<template>
  <div class="app-container gift-summary">
    <div class="page-head"><div><h2>礼品成本汇总</h2><p>按订单日期统计礼品数量和成本，点击汇总行可查看对应记录。</p></div><el-button icon="Refresh" :loading="loading" title="刷新" @click="load" /></div>
    <div class="toolbar">
      <el-button-group><el-button :type="preset==='today'?'primary':''" @click="setPreset('today')">今天</el-button><el-button :type="preset==='week'?'primary':''" @click="setPreset('week')">近7天</el-button><el-button :type="preset==='month'?'primary':''" @click="setPreset('month')">本月</el-button><el-button :type="preset==='all'?'primary':''" @click="setPreset('all')">全部</el-button></el-button-group>
      <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" @change="preset='';load()" />
      <el-select v-model="query.shopName" clearable filterable placeholder="全部店铺" @change="load"><el-option v-for="item in shops" :key="item.shopId" :label="item.shopName" :value="item.shopName" /></el-select>
      <el-segmented v-model="query.groupBy" :options="groupOptions" @change="load" />
    </div>

    <div class="stats" v-loading="loading">
      <div><span>已处理订单</span><strong>{{ integer(summary.handledOrders) }}</strong><small>所有已登记状态</small></div>
      <div><span>有礼品订单</span><strong>{{ integer(summary.giftOrders) }}</strong><small>占比 {{ giftRate }}%</small></div>
      <div><span>礼品总件数</span><strong>{{ integer(summary.giftQuantity) }}</strong><small>平均 {{ averageQuantity }} 件/单</small></div>
      <div class="main-stat"><span>礼品总成本</span><strong>¥{{ money(summary.giftCost) }}</strong><small>平均 ¥{{ averageCost }}/单</small></div>
    </div>

    <div class="table-head"><strong>{{ groupTitle }}</strong><span>共 {{ summary.groups?.length || 0 }} 项，按礼品成本从高到低排列</span></div>
    <el-table v-loading="loading" :data="summary.groups || []" stripe @row-click="drillDown">
      <el-table-column type="index" label="#" width="55" />
      <el-table-column prop="groupName" :label="groupTitle" min-width="220"><template #default="{ row }"><strong>{{ row.groupName || '未归类' }}</strong></template></el-table-column>
      <el-table-column prop="orderCount" label="订单数" width="120" align="right" />
      <el-table-column prop="giftQuantity" label="礼品件数" width="120" align="right" />
      <el-table-column label="平均成本/单" width="145" align="right"><template #default="{ row }">¥{{ money(Number(row.giftCost||0)/Math.max(Number(row.orderCount||0),1)) }}</template></el-table-column>
      <el-table-column label="礼品成本" width="150" align="right"><template #default="{ row }"><strong class="cost">¥{{ money(row.giftCost) }}</strong></template></el-table-column>
      <el-table-column label="成本占比" min-width="170"><template #default="{ row }"><div class="share"><el-progress :percentage="costShare(row.giftCost)" :stroke-width="8" :show-text="false" /><span>{{ costShare(row.giftCost) }}%</span></div></template></el-table-column>
      <el-table-column label="操作" width="105"><template #default><el-button link type="primary" icon="ArrowRight">查看记录</el-button></template></el-table-column>
      <template #empty><el-empty description="当前范围没有礼品成本数据" /></template>
    </el-table>
  </div>
</template>

<script setup name="GiftSummary">
import { getGiftSummary, listGiftShopOptions } from '@/api/live/gift'
import { useRouter } from 'vue-router'
const router=useRouter(),today=new Date().toISOString().slice(0,10),monthStart=new Date(new Date().getFullYear(),new Date().getMonth(),1).toISOString().slice(0,10)
const groupOptions=[{label:'按店铺',value:'shop'},{label:'按日期',value:'date'},{label:'按礼品',value:'gift'}]
const summary=ref({}),shops=ref([]),dates=ref([monthStart,today]),query=ref({groupBy:'shop',shopName:''}),preset=ref('month'),loading=ref(false)
const groupTitle=computed(()=>({shop:'店铺',date:'订单日期',gift:'礼品'}[query.value.groupBy]||'汇总对象'))
const giftRate=computed(()=>ratio(summary.value.giftOrders,summary.value.handledOrders)),averageQuantity=computed(()=>money(Number(summary.value.giftQuantity||0)/Math.max(Number(summary.value.giftOrders||0),1))),averageCost=computed(()=>money(Number(summary.value.giftCost||0)/Math.max(Number(summary.value.giftOrders||0),1)))
const money=value=>Number(value||0).toFixed(2),integer=value=>Number(value||0).toLocaleString('zh-CN'),ratio=(value,total)=>total?Math.round(Number(value||0)*1000/Number(total))/10:0
const costShare=value=>ratio(value,summary.value.giftCost)
function setPreset(value){preset.value=value;if(value==='today')dates.value=[today,today];if(value==='week')dates.value=[new Date(Date.now()-6*86400000).toISOString().slice(0,10),today];if(value==='month')dates.value=[monthStart,today];if(value==='all')dates.value=[];load()}
async function load(){loading.value=true;try{summary.value=(await getGiftSummary({groupBy:query.value.groupBy,shopName:query.value.shopName,beginDate:dates.value?.[0],endDate:dates.value?.[1]})).data||{}}finally{loading.value=false}}
function drillDown(row){const target={beginDate:dates.value?.[0],endDate:dates.value?.[1]};if(query.value.shopName)target.shopName=query.value.shopName;if(query.value.groupBy==='shop')target.shopName=row.groupName;if(query.value.groupBy==='date'){target.beginDate=row.groupName;target.endDate=row.groupName}if(query.value.groupBy==='gift')target.giftName=row.groupName;router.push({path:'/live-ops/giftLedger',query:target})}
onMounted(async()=>{shops.value=(await listGiftShopOptions()).data||[];await load()})
</script>

<style scoped>
.gift-summary{max-width:1500px}.page-head,.toolbar,.table-head,.share{display:flex;align-items:center}.page-head{justify-content:space-between}.page-head h2{margin:0 0 4px;font-size:22px}.page-head p{margin:0;color:#6b7280}.toolbar{flex-wrap:wrap;gap:10px;padding:18px 0}.toolbar>.el-select{width:220px}.stats{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #e5e7eb}.stats>div{display:flex;flex-direction:column;gap:5px;padding:18px;border-right:1px solid #e5e7eb}.stats>div:last-child{border-right:0}.stats span,.stats small,.table-head span{color:#6b7280}.stats strong{font-size:27px;font-weight:650}.stats .main-stat{background:#fff7ed}.stats .main-stat strong,.cost{color:#c2410c}.table-head{justify-content:space-between;padding:24px 0 10px}.table-head span{font-size:13px}.share{gap:10px}.share .el-progress{flex:1}.share span{width:48px;text-align:right;color:#6b7280}
@media(max-width:800px){.toolbar>*{width:100%!important}.stats{grid-template-columns:repeat(2,1fr)}.stats>div:nth-child(2){border-right:0}.stats>div:nth-child(-n+2){border-bottom:1px solid #e5e7eb}}
</style>
