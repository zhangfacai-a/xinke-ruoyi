<template>
  <div class="app-container gift-preference">
    <div class="page-head">
      <el-button icon="Refresh" title="刷新" @click="load" />
    </div>
    <div class="toolbar">
      <el-input v-model.trim="keyword" clearable placeholder="搜索礼品名称、编码或拼音简写" @keyup.enter="load"><template #prefix><el-icon><Search /></el-icon></template></el-input>
      <el-segmented v-model="view" :options="viewOptions" />
      <span class="count">显示 {{ visibleRows.length }} 个，已隐藏 {{ hiddenCount }} 个</span>
    </div>
    <div v-loading="loading" class="gift-grid">
      <article v-for="row in visibleRows" :key="row.giftId" :class="['gift-card',{hidden:isOn(row.personalHidden)}]">
        <div class="gift-head"><div><strong>{{ row.giftName }}</strong><span>{{ row.giftCode }}</span></div><el-tag v-if="isOn(row.personalPinned)" type="warning" effect="light">已置顶</el-tag></div>
        <div class="gift-info"><span>当前成本</span><strong v-if="row.currentCost!=null">¥{{ money(row.currentCost) }}</strong><em v-else>未设置</em></div>
        <div v-if="row.aliases" class="aliases">别名：{{ row.aliases }}</div>
        <div class="actions"><el-button link :loading="row.preferenceSaving" :type="isOn(row.personalPinned) ? 'warning' : 'info'" icon="Top" @click="toggle(row,'pinned')">{{ isOn(row.personalPinned) ? '取消置顶' : '置顶' }}</el-button><el-button link :loading="row.preferenceSaving" :type="isOn(row.personalHidden) ? 'success' : 'info'" :icon="isOn(row.personalHidden) ? 'View' : 'Hide'" @click="toggle(row,'hidden')">{{ isOn(row.personalHidden) ? '恢复显示' : '隐藏' }}</el-button></div>
      </article>
      <el-empty v-if="!loading&&!visibleRows.length" description="没有符合条件的礼品" class="grid-empty" />
    </div>
  </div>
</template>

<script setup name="GiftPreference">
import { listGifts, saveGiftPreference } from '@/api/live/gift'

const { proxy } = getCurrentInstance()
const rows = ref([]), loading = ref(false), keyword = ref(''), view = ref('all')
const viewOptions = [{ label: '全部', value: 'all' }, { label: '已显示', value: 'shown' }, { label: '已隐藏', value: 'hidden' }]
const money = value => Number(value || 0).toFixed(2)
const isOn = value => value === true || value === 1 || value === '1' || value === 'true' || value === '是'
const hiddenCount = computed(() => rows.value.filter(item => isOn(item.personalHidden)).length)
const visibleRows = computed(() => rows.value.filter(item => view.value === 'hidden' ? isOn(item.personalHidden) : view.value === 'shown' ? !isOn(item.personalHidden) : true))
let searchTimer
watch(keyword, () => { clearTimeout(searchTimer); searchTimer = setTimeout(load, 250) })
async function load() {
  loading.value = true
  try { rows.value = (await listGifts({ status: '0', includeHidden: '1', keyword: keyword.value })).data || [] } finally { loading.value = false }
}
async function toggle(row, field) {
  const key = `personal${field[0].toUpperCase()}${field.slice(1)}`
  const old = isOn(row[key])
  row[key] = !old
  row.preferenceSaving = true
  try {
    await saveGiftPreference({ giftId: row.giftId, hidden: field === 'hidden' ? row[key] : isOn(row.personalHidden), pinned: field === 'pinned' ? row[key] : isOn(row.personalPinned) })
    proxy.$modal.msgSuccess(field === 'hidden' ? (row[key] ? '礼品已隐藏' : '礼品已恢复显示') : (row[key] ? '礼品已置顶' : '已取消置顶'))
    await load()
  } catch (error) { row[key] = old; throw error } finally { row.preferenceSaving = false }
}
load()
</script>

<style scoped>
.gift-preference{max-width:1600px}.page-head,.toolbar,.gift-head,.actions{display:flex;align-items:center}.page-head{justify-content:space-between;gap:16px;margin-bottom:18px}.page-head h2{margin:0 0 4px;font-size:22px}.page-head p{margin:0;color:#6b7280}.toolbar{gap:10px;padding:12px 0;border-bottom:1px solid #ebeef5}.toolbar>.el-input{width:380px}.count{margin-left:auto;color:#6b7280;font-size:13px}.gift-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(285px,1fr));gap:12px;min-height:180px;padding-top:14px}.gift-card{display:flex;min-width:0;flex-direction:column;padding:14px;border:1px solid #dfe3e8;border-radius:6px;background:#fff;transition:border-color .15s,box-shadow .15s}.gift-card:hover{border-color:#f26b21;box-shadow:0 4px 14px rgba(23,32,51,.08)}.gift-card.hidden{background:#f7f8fa;opacity:.75}.gift-head{justify-content:space-between;gap:12px}.gift-head>div{min-width:0}.gift-head strong,.gift-head span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.gift-head strong{font-size:15px}.gift-head span{margin-top:4px;color:#8a949f;font:11px Consolas,monospace}.gift-info{display:flex;align-items:baseline;justify-content:space-between;margin-top:14px;padding:12px 0;border-top:1px solid #eef0f2;border-bottom:1px solid #eef0f2}.gift-info span,.gift-info em{color:#909399;font-size:12px}.gift-info strong{color:#c2410c;font-size:18px}.aliases{overflow:hidden;margin-top:9px;color:#7a8591;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.actions{justify-content:flex-end;gap:8px;margin-top:auto;padding-top:10px}.grid-empty{grid-column:1/-1}@media(max-width:720px){.page-head{align-items:flex-start;flex-direction:column}.toolbar{align-items:stretch;flex-wrap:wrap}.toolbar>.el-input{width:100%}.count{margin-left:0}.gift-grid{grid-template-columns:1fr}}
</style>
