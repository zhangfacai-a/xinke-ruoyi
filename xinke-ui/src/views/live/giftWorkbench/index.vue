<template>
  <div class="app-container gift-workbench-page">
    <nav class="section-nav" aria-label="礼品工作台页面">
      <div class="section-tabs">
        <button v-for="item in availableSections" :key="item.value" :class="{ active: activeSection === item.value }"
          type="button" @click="switchSection(item.value)">{{ item.label }}</button>
      </div>
    </nav>

    <GiftLedger v-if="activeSection === 'ledger'" />
    <GiftPreference v-else-if="activeSection === 'preference'" />
    <GiftSummary v-else-if="activeSection === 'bi'" />
    <QuickTemplate v-else-if="activeSection === 'template'" />
    <el-empty v-else description="当前账号没有礼品工作台权限，请联系管理员授权" />
  </div>
</template>

<script setup name="GiftWorkbench">
import { useRoute, useRouter } from 'vue-router'
import { checkPermi } from '@/utils/permission'
import GiftLedger from '@/views/live/giftLedger/index.vue'
import GiftPreference from '@/views/live/giftPreference/index.vue'
import GiftSummary from '@/views/live/giftSummary/index.vue'
import QuickTemplate from '@/views/live/quickTemplate/index.vue'

const route = useRoute()
const router = useRouter()
const sections = [
  { label: '礼品记录', value: 'ledger', permission: 'live:gift:ledger' },
  { label: '我的礼品设置', value: 'preference', permission: 'live:gift:preference' },
  { label: '礼品经营 BI', value: 'bi', permission: 'live:gift:summary' },
  { label: '快捷模板', value: 'template', permission: 'live:gift:template' }
]
const availableSections = sections.filter(item => checkPermi([item.permission]))
const requestedSection = () => availableSections.some(item => item.value === route.query.section)
  ? route.query.section
  : availableSections[0]?.value || ''
const activeSection = ref(requestedSection())

watch(() => route.query.section, () => { activeSection.value = requestedSection() })

function switchSection(section) {
  if (section === activeSection.value) return
  activeSection.value = section
  router.replace({ query: { ...route.query, section } })
}
</script>

<style scoped>
.gift-workbench-page{
  --gift-line:#e3e7eb;
  --gift-ink:#202631;
  --gift-muted:#747e8c;
  --gift-orange:#ed6a2c;
  min-height:calc(100vh - 84px);
  padding:16px 22px 30px;
  color:var(--gift-ink);
  background:#f4f6f8;
  position:relative;
}
.section-nav{
  display:flex;
  width:100%;
  max-width:1600px;
  min-height:49px;
  align-items:flex-end;
  margin:0 auto;
  padding:0 250px 0 17px;
  border:1px solid var(--gift-line);
  border-bottom:0;
  border-radius:6px 6px 0 0;
  background:#fff;
}
.section-tabs{display:flex;align-items:flex-end;gap:25px;overflow-x:auto;overflow-y:hidden;scrollbar-width:none}
.section-tabs::-webkit-scrollbar{display:none}
.section-tabs button{
  position:relative;
  height:48px;
  flex:none;
  padding:0 2px;
  border:0;
  color:#667085;
  background:transparent;
  font-size:13px;
  cursor:pointer;
}
.section-tabs button:hover{color:var(--gift-ink)}
.section-tabs button.active{color:var(--gift-ink);font-weight:650}
.section-tabs button.active::after{position:absolute;right:0;bottom:-1px;left:0;height:2px;background:var(--gift-orange);content:""}
.gift-workbench-page :deep(.gift-ledger),
.gift-workbench-page :deep(.gift-preference),
.gift-workbench-page :deep(.gift-bi),
.gift-workbench-page :deep(.material-center){
  width:100%;
  max-width:1600px;
  min-height:420px;
  margin:0 auto;
  padding:16px;
  border:1px solid var(--gift-line);
  background:#fff;
}
.gift-workbench-page :deep(.page-head){
  position:absolute;
  z-index:2;
  top:22px;
  right:max(39px, calc((100% - 1556px) / 2));
  min-height:32px;
  align-items:center;
  justify-content:flex-end;
  margin:0;
  padding:0;
  border:0;
}
.gift-workbench-page :deep(.room-context){margin-top:0}
@media(max-width:760px){
  .gift-workbench-page{padding:8px 7px 18px}
  .section-nav{padding:0 12px}
  .section-tabs{gap:18px}
  .gift-workbench-page :deep(.gift-ledger),
  .gift-workbench-page :deep(.gift-preference),
  .gift-workbench-page :deep(.gift-bi),
  .gift-workbench-page :deep(.material-center){padding:10px}
  .gift-workbench-page :deep(.page-head){position:static;min-height:44px;margin-bottom:8px;border-bottom:1px solid var(--gift-line)}
}
</style>
