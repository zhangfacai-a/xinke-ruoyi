set names utf8mb4;

-- 账簿页面菜单。凭证过账后在这里查询正式明细账和试算平衡。
set @finance_parent_id := (select menu_id from sys_menu where path = 'finance' and parent_id = 0 limit 1);

update sys_menu
set order_num = 11
where parent_id = @finance_parent_id and perms = 'finance:close:list';

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select
  '账簿中心', @finance_parent_id, 10, 'ledger', 'finance/ledger', null, 'FinanceLedger',
  1, 0, 'C', '0', '0', 'finance:ledger:list', 'documentation',
  'admin', sysdate(), '明细账和试算平衡'
where @finance_parent_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:ledger:list');

set @ledger_menu_id := (select menu_id from sys_menu where perms = 'finance:ledger:list' limit 1);

-- 已经拥有财务管理目录的角色，同步获得账簿中心菜单。
insert ignore into sys_role_menu(role_id, menu_id)
select distinct role_id, @ledger_menu_id
from sys_role_menu
where menu_id = @finance_parent_id
  and @ledger_menu_id is not null;

-- 补录历史已过账凭证。按凭证整单判断，重复执行不会重复写入。
insert into fin_ledger_entry(
  voucher_no, period_code, voucher_date, subject_code, subject_name, dc, amount,
  counterparty_name, source_type, source_no, create_time
)
select
  v.voucher_no, v.period_code, v.voucher_date, e.subject_code, e.subject_name,
  e.direction, e.amount, e.counterparty_name, v.source_type, v.source_no, sysdate()
from fin_voucher v
inner join fin_voucher_entry e on e.voucher_id = v.voucher_id
where v.voucher_status = 'posted'
  and not exists (
    select 1 from fin_ledger_entry l where l.voucher_no = v.voucher_no
  );

-- 依据正式明细账重建所有期间的试算平衡。
delete from fin_trial_balance;

insert into fin_trial_balance(
  period_code, subject_code, subject_name, begin_debit, begin_credit,
  current_debit, current_credit, end_debit, end_credit, create_time
)
select
  balances.periodCode, balances.subjectCode, balances.subjectName,
  greatest(balances.beginNet, 0.00), greatest(-balances.beginNet, 0.00),
  balances.currentDebit, balances.currentCredit,
  greatest(balances.beginNet + balances.currentDebit - balances.currentCredit, 0.00),
  greatest(-(balances.beginNet + balances.currentDebit - balances.currentCredit), 0.00),
  sysdate()
from (
  select
    periods.period_code periodCode,
    ledger.subject_code subjectCode,
    max(ledger.subject_name) subjectName,
    sum(case
      when ledger.period_code < periods.period_code
        then case when ledger.dc = 'debit' then ledger.amount else -ledger.amount end
      else 0
    end) beginNet,
    sum(case when ledger.period_code = periods.period_code and ledger.dc = 'debit' then ledger.amount else 0 end) currentDebit,
    sum(case when ledger.period_code = periods.period_code and ledger.dc = 'credit' then ledger.amount else 0 end) currentCredit
  from (select distinct period_code from fin_ledger_entry) periods
  inner join fin_ledger_entry ledger on ledger.period_code <= periods.period_code
  group by periods.period_code, ledger.subject_code
) balances
where balances.beginNet != 0
   or balances.currentDebit != 0
   or balances.currentCredit != 0;
