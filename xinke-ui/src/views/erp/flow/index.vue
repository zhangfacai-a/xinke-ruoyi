<template>
  <div class="app-container flow-workbench">
    <section class="flow-header soft-card">
      <div>
        <p>Company Operation Flow</p>
        <h2>经营闭环工作台</h2>
      </div>
      <div class="flow-actions">
        <el-button plain icon="Sort" @click="openInventoryAction('in')">入库</el-button>
        <el-button plain icon="Upload" @click="openInventoryAction('out')">出库</el-button>
        <el-button plain icon="Switch" @click="openInventoryAction('transfer')">调拨</el-button>
        <el-button plain type="danger" icon="Warning" @click="openInventoryAction('loss')">报损</el-button>
      </div>
    </section>

    <el-tabs v-model="activeGroup" class="flow-tabs" @tab-change="handleGroupChange">
      <el-tab-pane v-for="group in groups" :key="group.name" :label="group.label" :name="group.name" />
    </el-tabs>

    <div class="flow-layout">
      <aside class="flow-side soft-card">
        <button
          v-for="item in currentGroup.children"
          :key="item.type"
          class="flow-nav-item"
          :class="{ active: activeType === item.type }"
          @click="setFlowType(item.type)"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
        </button>
      </aside>

      <main class="flow-main">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
          <el-form-item label="关键字" prop="keyword">
            <el-input v-model="queryParams.keyword" placeholder="输入单号/编码" clearable @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col v-if="!isReadOnly" :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:flow:add']">新增</el-button>
          </el-col>
          <el-col v-if="!isReadOnly" :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['erp:flow:edit']">修改</el-button>
          </el-col>
          <el-col v-if="!isReadOnly" :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['erp:flow:remove']">删除</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </el-row>

        <el-table v-loading="loading" :data="flowList" @selection-change="handleSelectionChange">
          <el-table-column v-if="!isReadOnly" type="selection" width="55" align="center" />
          <el-table-column v-for="col in currentConfig.columns" :key="col.prop" :label="col.label" :prop="col.prop" :min-width="col.width || 120" :align="col.align || 'center'" show-overflow-tooltip>
            <template #default="scope">
              <el-tag v-if="col.tag" :type="statusTag(scope.row[col.prop])" :title="formatValue(scope.row[col.prop])">{{ formatValue(scope.row[col.prop]) }}</el-tag>
              <span v-else :title="formatValue(scope.row[col.prop])">{{ formatValue(scope.row[col.prop]) }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="!isReadOnly" label="操作" width="240" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:flow:edit']">修改</el-button>
              <el-button v-if="currentConfig.status" link type="primary" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['erp:flow:audit']">审批</el-button>
              <el-button v-if="activeType === 'supplier-reconcile'" link type="primary" icon="Wallet" @click="handlePayable(scope.row)">生成应付</el-button>
              <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:flow:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </main>
    </div>

    <el-dialog :title="dialogTitle" v-model="open" width="720px" append-to-body>
      <el-form ref="flowRef" :model="form" label-width="120px">
        <el-row :gutter="16">
          <el-col v-for="field in currentConfig.fields" :key="field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.prop">
              <el-input-number v-if="field.type === 'number'" v-model="form[field.prop]" :min="0" :precision="field.precision ?? 0" controls-position="right" style="width: 100%" />
              <el-date-picker v-else-if="field.type === 'date'" v-model="form[field.prop]" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" style="width: 100%" />
              <el-date-picker v-else-if="field.type === 'datetime'" v-model="form[field.prop]" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择时间" style="width: 100%" />
              <el-select v-else-if="field.type === 'select'" v-model="form[field.prop]" clearable style="width: 100%">
                <el-option v-for="opt in field.options || statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-input v-else v-model="form[field.prop]" :type="field.type === 'textarea' ? 'textarea' : 'text'" :placeholder="`请输入${field.label}`" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog :title="inventoryTitle" v-model="inventoryOpen" width="620px" append-to-body>
      <el-form :model="inventoryForm" label-width="120px">
        <el-form-item v-if="inventoryAction === 'transfer'" label="调出仓库ID">
          <el-input-number v-model="inventoryForm.fromWarehouseId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="inventoryAction === 'transfer'" label="调入仓库ID">
          <el-input-number v-model="inventoryForm.toWarehouseId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="inventoryAction !== 'transfer'" label="仓库ID">
          <el-input-number v-model="inventoryForm.warehouseId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="SKU ID">
          <el-input-number v-model="inventoryForm.skuId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="SKU编码">
          <el-input v-model="inventoryForm.skuCode" />
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="inventoryForm.quantity" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="inventoryForm.costPrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="来源单号">
          <el-input v-model="inventoryForm.sourceNo" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inventoryForm.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitInventoryAction">确定</el-button>
          <el-button @click="inventoryOpen = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpFlowWorkbench">
import {
  listFlow,
  getFlow,
  addFlow,
  updateFlow,
  delFlow,
  approveFlow,
  inventoryIn,
  inventoryOut,
  inventoryTransfer,
  inventoryLoss,
  generateSupplierPayable
} from '@/api/erp/flow'

const { proxy } = getCurrentInstance()

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已审批', value: 'approved' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'done' },
  { label: '已关闭', value: 'closed' },
  { label: '启用', value: 'enabled' },
  { label: '停用', value: 'disabled' },
  { label: '待匹配', value: 'unmatched' },
  { label: '已匹配', value: 'matched' },
  { label: '打开', value: 'open' }
]

const baseAmount = [
  { prop: 'total_amount', label: '金额', type: 'number', precision: 2 },
  { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
]

const configs = {
  'purchase-request': cfg('采购申请', 'request_id', 'request_no', 'request_status', [
    text('request_no', '申请单号'), text('request_title', '申请标题'), text('supplier_name', '供应商'), num('total_amount', '金额', 2), status('request_status')
  ], [
    text('request_no', '申请单号'), text('request_title', '申请标题'), text('requester_name', '申请人'), text('department_name', '部门'), num('supplier_id', '供应商ID'), text('supplier_name', '供应商'), num('warehouse_id', '仓库ID'), date('expected_date', '期望到货'), ...baseAmount
  ]),
  'purchase-receipt': cfg('采购入库', 'receipt_id', 'receipt_no', 'receipt_status', [
    text('receipt_no', '入库单号'), text('purchase_no', '采购单号'), text('supplier_name', '供应商'), num('total_qty', '数量'), num('total_amount', '金额', 2), status('receipt_status')
  ], [
    text('receipt_no', '入库单号'), text('purchase_no', '采购单号'), num('supplier_id', '供应商ID'), text('supplier_name', '供应商'), num('warehouse_id', '仓库ID'), text('warehouse_name', '仓库'), date('receipt_date', '入库日期'), num('total_qty', '数量'), ...baseAmount
  ]),
  'purchase-return': cfg('采购退货', 'return_id', 'return_no', 'return_status', [
    text('return_no', '退货单号'), text('purchase_no', '采购单号'), text('supplier_name', '供应商'), num('total_qty', '数量'), num('total_amount', '金额', 2), status('return_status')
  ], [
    text('return_no', '退货单号'), text('purchase_no', '采购单号'), text('receipt_no', '入库单号'), num('supplier_id', '供应商ID'), text('supplier_name', '供应商'), num('warehouse_id', '仓库ID'), date('return_date', '退货日期'), num('total_qty', '数量'), text('return_reason', '原因'), ...baseAmount
  ]),
  'supplier-reconcile': cfg('供应商对账', 'reconcile_id', 'reconcile_no', 'reconcile_status', [
    text('reconcile_no', '对账单号'), text('supplier_name', '供应商'), text('period_code', '期间'), num('payable_amount', '应付金额', 2), num('diff_amount', '差异', 2), status('reconcile_status')
  ], [
    text('reconcile_no', '对账单号'), num('supplier_id', '供应商ID'), text('supplier_name', '供应商'), text('period_code', '期间'), num('purchase_amount', '采购金额', 2), num('return_amount', '退货金额', 2), num('payable_amount', '应付金额', 2), num('diff_amount', '差异', 2), text('remark', '备注', 'textarea', 24)
  ]),
  'supplier-quote': cfg('供应商报价', 'quote_id', 'quote_no', 'quote_status', [
    text('quote_no', '报价单号'), text('supplier_name', '供应商'), text('sku_code', 'SKU'), num('quote_price', '报价', 2), status('quote_status')
  ], [
    text('quote_no', '报价单号'), num('supplier_id', '供应商ID'), text('supplier_name', '供应商'), num('sku_id', 'SKU ID'), text('sku_code', 'SKU编码'), num('quote_price', '报价', 2), num('min_order_qty', '最小起订量'), date('effective_date', '生效日期'), date('expire_date', '失效日期'), text('remark', '备注', 'textarea', 24)
  ]),
  'inventory-transfer': cfg('库存调拨', 'transfer_id', 'transfer_no', 'transfer_status', [
    text('transfer_no', '调拨单号'), num('from_warehouse_id', '调出仓'), num('to_warehouse_id', '调入仓'), num('total_qty', '数量'), status('transfer_status')
  ], [
    text('transfer_no', '调拨单号'), num('from_warehouse_id', '调出仓库ID'), num('to_warehouse_id', '调入仓库ID'), date('transfer_date', '调拨日期'), num('total_qty', '数量'), text('remark', '备注', 'textarea', 24)
  ]),
  'inventory-stocktake': cfg('库存盘点', 'stocktake_id', 'stocktake_no', 'stocktake_status', [
    text('stocktake_no', '盘点单号'), num('warehouse_id', '仓库ID'), date('stocktake_date', '盘点日期'), num('profit_loss_amount', '盈亏金额', 2), status('stocktake_status')
  ], [
    text('stocktake_no', '盘点单号'), num('warehouse_id', '仓库ID'), text('warehouse_name', '仓库'), date('stocktake_date', '盘点日期'), num('total_sku_count', 'SKU数量'), num('profit_loss_amount', '盈亏金额', 2), text('remark', '备注', 'textarea', 24)
  ]),
  'inventory-loss': cfg('库存报损', 'loss_id', 'loss_no', 'loss_status', [
    text('loss_no', '报损单号'), num('warehouse_id', '仓库ID'), text('sku_code', 'SKU'), num('quantity', '数量'), num('loss_amount', '损失金额', 2), status('loss_status')
  ], [
    text('loss_no', '报损单号'), num('warehouse_id', '仓库ID'), num('sku_id', 'SKU ID'), text('sku_code', 'SKU编码'), text('sku_name', 'SKU名称'), num('quantity', '数量'), num('cost_price', '成本价', 2), num('loss_amount', '损失金额', 2), text('loss_reason', '报损原因'), text('remark', '备注', 'textarea', 24)
  ]),
  'inventory-warning': simpleCfg('库存预警', 'rule_id', 'status', ['warehouse_id', 'sku_code', 'min_qty', 'max_qty', 'warning_level', 'status']),
  'inventory-ageing': simpleCfg('库龄分析', 'snapshot_id', '', ['snapshot_date', 'warehouse_id', 'sku_code', 'qty_0_30', 'qty_31_60', 'qty_61_90', 'qty_90_plus']),
  shipment: simpleCfg('销售发货', 'shipment_id', 'shipment_status', ['shipment_no', 'sales_no', 'warehouse_id', 'logistics_company', 'logistics_no', 'shipment_status']),
  'after-sale': simpleCfg('售后单', 'after_sale_id', 'after_sale_status', ['after_sale_no', 'sales_no', 'after_sale_type', 'customer_name', 'amount', 'after_sale_status']),
  compensation: simpleCfg('补偿单', 'compensation_id', 'compensation_status', ['compensation_no', 'sales_no', 'shop_id', 'sku_id', 'compensation_amount', 'compensation_status']),
  'settlement-match': simpleCfg('结算匹配', 'match_id', 'match_status', ['match_no', 'settlement_no', 'sales_no', 'settlement_amount', 'order_amount', 'diff_amount', 'match_status']),
  'order-profit': simpleCfg('订单利润', 'profit_id', '', ['sales_no', 'shop_id', 'sku_id', 'channel_code', 'operator_name', 'pay_amount', 'product_cost', 'platform_fee', 'promotion_fee', 'profit_amount', 'share_profit_amount']),
  'operator-attribution': cfg('运营归因规则', 'rule_id', 'rule_code', 'status', [
    text('rule_code', '规则编码'), text('rule_name', '规则名称'), text('shop_name', '店铺'), text('sku_code', 'SKU'), text('channel_name', '渠道'), text('operator_name', '运营'), num('share_rate', '分成比例', 4), num('priority', '优先级'), status('status')
  ], [
    text('rule_code', '规则编码'), text('rule_name', '规则名称'), num('shop_id', '店铺ID'), text('shop_name', '店铺'), num('product_id', '商品ID'), text('product_name', '商品'), num('sku_id', 'SKU ID'), text('sku_code', 'SKU编码'), text('sku_name', 'SKU名称'), text('channel_code', '渠道编码'), text('channel_name', '渠道名称'), num('operator_id', '运营ID'), text('operator_name', '运营'), num('share_rate', '分成比例', 4), num('priority', '优先级'), date('effective_start', '生效日期'), date('effective_end', '失效日期'), text('remark', '备注', 'textarea', 24)
  ]),
  'operator-profit-day': simpleCfg('运营利润日报', 'summary_id', '', ['dt', 'period_code', 'shop_name', 'operator_name', 'channel_name', 'order_count', 'gmv', 'net_amount', 'product_cost', 'platform_fee', 'ad_cost', 'freight_fee', 'after_sale_cost', 'profit_amount', 'share_profit_amount']),
  'operator-settlement': cfg('运营分润结算', 'settlement_id', 'settlement_no', 'settlement_status', [
    text('settlement_no', '结算单号'), text('period_code', '期间'), text('operator_name', '运营'), text('shop_name', '店铺'), text('channel_name', '渠道'), num('share_profit_amount', '应分润', 2), num('paid_amount', '已付款', 2), num('remain_amount', '未付款', 2), status('settlement_status')
  ], [
    text('settlement_no', '结算单号'), text('period_code', '期间'), num('operator_id', '运营ID'), text('operator_name', '运营'), num('shop_id', '店铺ID'), text('shop_name', '店铺'), text('channel_code', '渠道编码'), text('channel_name', '渠道'), num('profit_amount', '利润', 2), num('share_profit_amount', '应分润', 2), num('paid_amount', '已付款', 2), num('remain_amount', '未付款', 2), text('remark', '备注', 'textarea', 24)
  ]),
  'sku-barcode': simpleCfg('SKU条码', 'barcode_id', 'status', ['sku_id', 'sku_code', 'barcode', 'barcode_type', 'status']),
  'platform-sku': simpleCfg('平台SKU映射', 'mapping_id', 'status', ['platform_code', 'shop_id', 'platform_sku_id', 'sku_id', 'sku_code', 'status']),
  bundle: simpleCfg('组合商品', 'bundle_id', 'bundle_status', ['bundle_sku_id', 'bundle_sku_code', 'bundle_name', 'bundle_status']),
  'cost-history': simpleCfg('成本价历史', 'cost_id', '', ['sku_id', 'sku_code', 'cost_price', 'effective_date', 'source_type', 'source_no']),
  'payment-request': simpleCfg('付款申请', 'payment_id', 'payment_status', ['payment_no', 'source_type', 'source_no', 'counterparty_name', 'payable_no', 'payment_amount', 'payment_status']),
  'payment-execute': simpleCfg('付款执行', 'execute_id', 'execute_status', ['execute_no', 'payment_no', 'bank_account_id', 'cash_flow_no', 'execute_amount', 'execute_status']),
  reimbursement: simpleCfg('员工报销', 'reimbursement_id', 'reimbursement_status', ['reimbursement_no', 'employee_name', 'department_name', 'reimbursement_title', 'expense_amount', 'invoice_amount', 'reimbursement_status']),
  contract: simpleCfg('合同台账', 'contract_id', 'contract_status', ['contract_no', 'contract_name', 'contract_type', 'counterparty_name', 'contract_amount', 'end_date', 'contract_status']),
  'contract-plan': simpleCfg('收付款计划', 'plan_id', 'plan_status', ['contract_no', 'plan_type', 'plan_date', 'plan_amount', 'actual_amount', 'plan_status']),
  'subject-mapping': simpleCfg('科目映射', 'mapping_id', 'status', ['biz_type', 'fee_type_code', 'source_type', 'debit_subject_code', 'credit_subject_code', 'status']),
  'voucher-template': simpleCfg('凭证模板', 'template_id', 'template_status', ['template_code', 'template_name', 'source_type', 'template_status']),
  'ledger-entry': simpleCfg('总账/明细账', 'ledger_id', '', ['voucher_no', 'period_code', 'voucher_date', 'subject_code', 'subject_name', 'dc', 'amount']),
  'trial-balance': simpleCfg('试算平衡', 'trial_id', '', ['period_code', 'subject_code', 'subject_name', 'current_debit', 'current_credit', 'end_debit', 'end_credit']),
  'profit-statement': simpleCfg('利润表', 'statement_id', '', ['period_code', 'revenue_amount', 'cost_amount', 'gross_profit', 'expense_amount', 'net_profit'])
}

const readOnlyFlowTypes = ['inventory-ageing', 'order-profit', 'operator-profit-day', 'ledger-entry', 'trial-balance', 'profit-statement']
readOnlyFlowTypes.forEach(type => {
  if (configs[type]) {
    configs[type].readOnly = true
  }
})

const groups = [
  { name: 'purchase', label: '采购闭环', children: [
    item('purchase-request', '采购申请', '申请-审批-转采购'),
    item('purchase-receipt', '采购入库', '到货入库'),
    item('purchase-return', '采购退货', '退货出库'),
    item('supplier-reconcile', '供应商对账', '对账生成应付'),
    item('supplier-quote', '供应商报价', '报价与成本依据')
  ] },
  { name: 'inventory', label: '库存闭环', children: [
    item('inventory-transfer', '调拨', '仓间流转'),
    item('inventory-stocktake', '盘点', '账实核对'),
    item('inventory-loss', '报损', '损耗处理'),
    item('inventory-warning', '预警', '安全库存'),
    item('inventory-ageing', '库龄', '库存沉淀')
  ] },
  { name: 'sales', label: '销售/订单', children: [
    item('shipment', '发货', '订单履约'),
    item('after-sale', '售后', '退款退货服务'),
    item('compensation', '补偿', '客户补偿'),
    item('settlement-match', '结算匹配', '平台结算核对'),
    item('order-profit', '订单利润', '单笔利润核算'),
    item('operator-attribution', '运营归因', '按商品/渠道归属运营'),
    item('operator-profit-day', '运营利润', '运营分润日报'),
    item('operator-settlement', '运营分润', '按运营生成结算')
  ] },
  { name: 'product', label: '商品/SKU', children: [
    item('sku-barcode', 'SKU条码', '条码管理'),
    item('platform-sku', '平台SKU映射', '平台拆分规则'),
    item('bundle', '组合商品', '套装拆分'),
    item('cost-history', '成本历史', '成本追踪')
  ] },
  { name: 'finance', label: '财务增强', children: [
    item('payment-request', '付款申请', '付款审批'),
    item('payment-execute', '付款执行', '流水关联'),
    item('reimbursement', '员工报销', '费用报销'),
    item('contract', '合同台账', '归档提醒'),
    item('contract-plan', '收付款计划', '计划跟踪'),
    item('subject-mapping', '科目映射', '自动凭证基础'),
    item('voucher-template', '凭证模板', '自动凭证规则'),
    item('ledger-entry', '总账/明细账', '账簿记录'),
    item('trial-balance', '试算平衡', '期末检查'),
    item('profit-statement', '利润表', '财务报表')
  ] }
]

const activeGroup = ref('purchase')
const activeType = ref('purchase-request')
const flowList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const selectedRows = ref([])
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const dialogTitle = ref('')
const form = ref({})
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: undefined, status: undefined })
const inventoryOpen = ref(false)
const inventoryAction = ref('in')
const inventoryForm = ref({})

const currentGroup = computed(() => groups.find(group => group.name === activeGroup.value))
const currentConfig = computed(() => configs[activeType.value])
const isReadOnly = computed(() => Boolean(currentConfig.value?.readOnly))
const inventoryTitle = computed(() => ({ in: '库存入库', out: '库存出库', transfer: '库存调拨', loss: '库存报损' }[inventoryAction.value]))

function cfg(label, pk, no, statusProp, columns, fields) {
  return { label, pk, no, status: statusProp, columns: columns.map(normalizeColumn), fields }
}
function simpleCfg(label, pk, statusProp, props) {
  const columns = props.map(prop => normalizeColumn({ prop, label: labelOf(prop), tag: prop === statusProp }))
  const fields = props.filter(prop => prop !== pk).map(prop => fieldByProp(prop))
  return { label, pk, no: props[0], status: statusProp, columns, fields }
}
function item(type, label, desc) { return { type, label, desc } }
function text(prop, label, type = 'text', span) { return { prop, label, type, span } }
function num(prop, label, precision = 0) { return { prop, label, type: 'number', precision } }
function date(prop, label) { return { prop, label, type: 'date' } }
function status(prop) { return { prop, label: '状态', tag: true } }
function normalizeColumn(col) { return { ...col, width: col.width || 130 } }
function labelOf(prop) {
  const dict = {
    request_no: '单号', receipt_no: '单号', return_no: '单号', reconcile_no: '单号',
    supplier_name: '供应商', sku_code: 'SKU', status: '状态', payment_amount: '付款金额',
    expense_amount: '报销金额', contract_amount: '合同金额', amount: '金额',
    dt: '日期', period_code: '期间', shop_name: '店铺', operator_name: '运营',
    channel_name: '渠道', order_count: '订单数', gmv: 'GMV', net_amount: '净成交额',
    product_cost: '商品成本', platform_fee: '平台扣点', ad_cost: '推广费',
    freight_fee: '运费', after_sale_cost: '售后成本', profit_amount: '利润',
    share_profit_amount: '应分润', paid_amount: '已付款', remain_amount: '未付款'
  }
  return dict[prop] || prop
}
function fieldByProp(prop) {
  if (prop.includes('amount') || prop.includes('price') || prop.includes('qty') || prop.endsWith('_id')) return num(prop, labelOf(prop), prop.includes('amount') || prop.includes('price') ? 2 : 0)
  if (prop.includes('date')) return date(prop, labelOf(prop))
  if (prop.includes('status')) return { prop, label: labelOf(prop), type: 'select' }
  return text(prop, labelOf(prop))
}
function statusTag(value) {
  if (['approved', 'done', 'enabled', 'matched', 'open'].includes(value)) return 'success'
  if (['processing', 'unmatched'].includes(value)) return 'warning'
  if (['closed', 'disabled'].includes(value)) return 'info'
  return 'primary'
}
function formatValue(value) { return value === null || value === undefined || value === '' ? '-' : value }
function handleGroupChange() {
  activeType.value = currentGroup.value.children[0].type
  clearSelectionState()
  handleQuery()
}
function setFlowType(type) {
  activeType.value = type
  clearSelectionState()
  handleQuery()
}
function getList() {
  loading.value = true
  listFlow(activeType.value, queryParams).then(response => {
    flowList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => { loading.value = false })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) {
  selectedRows.value = selection
  ids.value = selection.map(row => row[currentConfig.value.pk])
  single.value = selection.length !== 1
  multiple.value = !selection.length
}
function clearSelectionState() {
  selectedRows.value = []
  ids.value = []
  single.value = true
  multiple.value = true
}
function handleAdd() {
  if (isReadOnly.value) return
  form.value = {}
  open.value = true
  dialogTitle.value = `新增${currentConfig.value.label}`
}
function handleUpdate(row) {
  if (isReadOnly.value) return
  const id = row?.[currentConfig.value.pk] || ids.value[0]
  getFlow(activeType.value, id).then(response => {
    form.value = response.data || {}
    open.value = true
    dialogTitle.value = `修改${currentConfig.value.label}`
  })
}
function submitForm() {
  if (isReadOnly.value) return
  const id = form.value[currentConfig.value.pk]
  const request = id ? updateFlow(activeType.value, id, form.value) : addFlow(activeType.value, form.value)
  request.then(() => {
    proxy.$modal.msgSuccess('保存成功')
    open.value = false
    getList()
  })
}
function handleDelete(row) {
  if (isReadOnly.value) return
  const targetIds = row?.[currentConfig.value.pk] || ids.value
  proxy.$modal.confirm(`确认删除选中的${currentConfig.value.label}数据？`).then(() => delFlow(activeType.value, targetIds)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}
function handleApprove(row) {
  approveFlow(activeType.value, row[currentConfig.value.no]).then(() => {
    proxy.$modal.msgSuccess('审批成功')
    getList()
  })
}
function handlePayable(row) {
  generateSupplierPayable(row.reconcile_no).then(() => proxy.$modal.msgSuccess('应付单已生成'))
}
function openInventoryAction(action) {
  inventoryAction.value = action
  inventoryForm.value = { quantity: 1, costPrice: 0 }
  inventoryOpen.value = true
}
function submitInventoryAction() {
  const map = { in: inventoryIn, out: inventoryOut, transfer: inventoryTransfer, loss: inventoryLoss }
  map[inventoryAction.value](inventoryForm.value).then(() => {
    proxy.$modal.msgSuccess('库存处理成功')
    inventoryOpen.value = false
    getList()
  })
}

getList()
</script>

<style scoped lang="scss">
.flow-workbench {
  .flow-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 22px 24px;
    margin-bottom: 16px;

    p {
      margin: 0 0 6px;
      color: #7c8498;
      font-size: 12px;
      font-weight: 800;
      text-transform: uppercase;
    }

    h2 {
      margin: 0;
      color: #202437;
      font-size: 22px;
    }
  }

  .flow-actions {
    display: flex;
    gap: 10px;
  }

  .flow-layout {
    display: grid;
    grid-template-columns: 220px minmax(0, 1fr);
    gap: 16px;
  }

  .flow-side {
    padding: 10px;
    align-self: start;
  }

  .flow-nav-item {
    width: 100%;
    padding: 12px 14px;
    border: 0;
    border-radius: 14px;
    background: transparent;
    color: #5d667a;
    text-align: left;
    cursor: pointer;
    transition: all 0.18s ease;

    span {
      display: block;
      font-weight: 800;
    }

    small {
      display: block;
      margin-top: 4px;
      color: #98a1b4;
    }

    &:hover,
    &.active {
      background: rgba(108, 92, 231, 0.08);
      color: #6c5ce7;
    }
  }

  .flow-main {
    min-width: 0;
  }
}

@media (max-width: 1100px) {
  .flow-workbench {
    .flow-header,
    .flow-actions {
      flex-wrap: wrap;
    }

    .flow-layout {
      grid-template-columns: 1fr;
    }

    .flow-side {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 8px;
    }
  }
}
</style>
