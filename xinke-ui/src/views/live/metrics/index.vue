<template>
  <div class="handover-page">
    <header class="page-head"><div><h2>主播数据上报</h2><p>查看各直播间的主播上报进度和区间经营数据</p></div><el-button :icon="Refresh" circle title="刷新" :loading="loading" @click="load" /></header>
    <div class="toolbar"><el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索直播间或主播"/><span>共 {{ filtered.length }} 个直播间</span></div>
    <section v-loading="loading" class="room-list">
      <router-link v-for="room in filtered" :key="room.roomId" class="room-row" :to="`/live-ops/liveMetrics/room/${room.roomId}`">
        <div class="room-main"><strong>{{ room.roomName }}</strong><span>{{ room.anchorNames || '尚未配置主播' }}</span></div>
        <div class="room-data"><span>最新场次</span><b>{{ room.liveStartTime ? datetime(room.liveStartTime) : '暂无场次' }}</b></div>
        <div class="room-data"><span>最近上报</span><b>{{ room.lastSnapshotAt ? datetime(room.lastSnapshotAt) : '暂无上报' }}</b><small v-if="room.lastAnchorName">{{ room.lastAnchorName }}</small></div>
        <div class="room-progress"><b>{{ Number(room.latestSegmentCount || 0) }}</b><span>人已上报</span></div>
        <el-tag v-if="Number(room.exceptionCount)" type="warning" effect="plain">{{ room.exceptionCount }} 个待核对</el-tag><el-tag v-else-if="room.lastSnapshotAt" type="success" effect="plain">正常</el-tag><el-tag v-else type="info" effect="plain">未开始</el-tag>
        <el-icon class="room-arrow"><ArrowRight /></el-icon>
      </router-link><el-empty v-if="!loading&&!filtered.length" description="没有匹配的直播间"/>
    </section>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Refresh, Search } from '@element-plus/icons-vue'
import { listHandoverRooms } from '@/api/liveMetrics'
const rooms=ref([]),keyword=ref(''),loading=ref(false)
const filtered=computed(()=>{const key=keyword.value.trim().toLowerCase();return rooms.value.filter(room=>!key||`${room.roomName}${room.anchorNames||''}`.toLowerCase().includes(key))})
const datetime=value=>value?String(value).replace('T',' ').slice(5,16):'-'
async function load(){loading.value=true;try{rooms.value=(await listHandoverRooms()).data||[]}finally{loading.value=false}}
onMounted(load)
</script>
<style scoped>
.handover-page{max-width:1320px;margin:auto;padding:20px}.page-head,.toolbar,.room-row{display:flex;align-items:center}.page-head{justify-content:space-between;margin-bottom:18px}.page-head h2{margin:0;font-size:22px}.page-head p{margin:5px 0 0;color:#7b8493;font-size:13px}.toolbar{justify-content:space-between;padding:13px 0;border-block:1px solid #e7e9ee;color:#8a93a1;font-size:12px}.toolbar .el-input{width:320px}.room-list{min-height:300px;margin-top:8px}.room-row{min-height:82px;gap:22px;padding:12px 16px;border-bottom:1px solid #ebeef3;background:#fff;color:#242a33;text-decoration:none}.room-row:hover{background:#f8fafc}.room-main{display:flex;flex:1;min-width:260px;flex-direction:column;gap:6px}.room-main strong{font-size:15px}.room-main span,.room-data span,.room-data small,.room-progress span{color:#8a93a1;font-size:12px}.room-data{display:flex;width:150px;flex-direction:column;gap:4px}.room-data b{font-size:13px;font-weight:600}.room-progress{display:flex;width:80px;align-items:baseline;gap:4px}.room-progress b{color:#ef6723;font-size:20px}.room-progress span{white-space:nowrap}@media(max-width:900px){.room-row{align-items:flex-start;flex-wrap:wrap}.room-main{min-width:100%}.room-data{flex:1}.room-progress{width:auto}}@media(max-width:600px){.handover-page{padding:12px}.toolbar .el-input{width:75%}}
.room-arrow{color:#a6adb8;font-size:16px;transition:.15s}.room-row:hover .room-arrow{color:#ef6723;transform:translateX(2px)}
</style>
