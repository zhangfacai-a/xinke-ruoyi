<template>
  <main class="live-home">
    <header class="page-head">
      <div>
        <h1>工作台</h1>
        <p>{{ today }}</p>
      </div>
    </header>

    <section class="entry-list" aria-label="常用功能">
      <button v-for="entry in entries" :key="entry.path" type="button" @click="router.push(entry.path)">
        <span class="entry-icon"><component :is="entry.icon" /></span>
        <span class="entry-copy">
          <strong>{{ entry.title }}</strong>
          <small>{{ entry.subtitle }}</small>
        </span>
        <el-icon class="entry-arrow"><ArrowRight /></el-icon>
      </button>
    </section>
  </main>
</template>

<script setup name="Index">
import { ArrowRight, Goods, Switch, User } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {computed} from 'vue'
import {checkPermi} from '@/utils/permission'

const router = useRouter()
const today = new Intl.DateTimeFormat('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const entries=computed(()=>[
  {title:'直播客户中心',subtitle:'客户领取、跟进与资料',path:'/ops-customers/customer-center',icon:User,perms:['live:customerCenter:list']},
  {title:'京东客户信息',subtitle:'RPA 导入、订单与联系方式',path:'/ops-customers/jd-customers',icon:User},
  {title:'挽单与转推中心',subtitle:'订单处理、跟进与经营看板',path:'/ops-customers/retention-center',icon:Switch,perms:['live:retention:access']},
  {title:'主播数据',subtitle:'主播交接、区间明细与业绩',path:'/ops-anchors/liveMetrics',icon:Switch,perms:['live:metrics:list']},
  {title:'礼品工作台',subtitle:'礼品登记、记录与汇总',path:'/ops-gifts/giftWorkbench',icon:Goods,perms:['live:gift:entry','live:gift:list']},
  {title:'运费征收',subtitle:'运费明细与经营看板',path:'/ops-freight/freight-collection',icon:Goods,perms:['live:freight:access','live:freight:viewAll','live:freight:entry']},
  {title:'人员与业务配置',subtitle:'业务分配、共享人员组与实际权限',path:'/ops-people/user',icon:User,perms:['system:user:list']}
].filter(entry=>!entry.perms||checkPermi(entry.perms)))
</script>

<style scoped lang="scss">
.live-home {
  min-height: calc(100vh - 84px);
  padding: 16px;
  background: #f5f6f8;
  color: #273142;
}

.page-head {
  display: flex;
  align-items: center;
  min-height: 66px;
  padding: 12px 16px;
  border: 1px solid #e4e7eb;
  border-left: 4px solid #f26b21;
  border-radius: 6px;
  background: #fff;
}

h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
}

.page-head p {
  margin: 5px 0 0;
  color: #7b8491;
  font-size: 13px;
}

.entry-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.entry-list button {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid #e4e7eb;
  border-radius: 6px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.entry-list button:hover {
  border-color: #f26b21;
  background: #fffaf7;
}

.entry-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 5px;
  background: #fff1e9;
  color: #e75e1b;
}

.entry-icon svg {
  width: 19px;
  height: 19px;
}

.entry-copy {
  min-width: 0;
  flex: 1;
}

.entry-copy strong,
.entry-copy small {
  display: block;
}

.entry-copy strong {
  font-size: 15px;
}

.entry-copy small {
  margin-top: 5px;
  color: #7b8491;
  font-size: 12px;
}

.entry-arrow {
  color: #a1a8b2;
}

@media (max-width: 720px) {
  .live-home { padding: 9px; }
  .entry-list { grid-template-columns: 1fr; }
}
</style>
