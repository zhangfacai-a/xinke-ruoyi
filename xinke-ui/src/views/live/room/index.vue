<template>
  <div class="app-container room-mapping-page">
    <header class="page-head">
      <div>
        <h2>直播间店铺配置</h2>
        <p>指定每个直播间由哪个店铺账号负责，未配置的直播间不会自动跟进用户。</p>
      </div>
      <div class="head-actions">
        <el-button icon="Shop" @click="openShopDialog">管理店铺账号</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['live:room:add']">新增直播间</el-button>
      </div>
    </header>

    <div class="summary-bar">
      <div><span>全部直播间</span><strong>{{ total }}</strong></div>
      <div><span>本页正常</span><strong class="success">{{ pageReady }}</strong></div>
      <div><span>本页待配置</span><strong class="warning">{{ pageUnmapped }}</strong></div>
      <div><span>可执行店铺</span><strong>{{ activeShops.length }}</strong></div>
    </div>

    <section class="workspace">
      <el-form :model="queryParams" ref="queryRef" :inline="true" class="filters">
        <el-form-item label="直播间"><el-input v-model="queryParams.roomName" placeholder="名称或直播间ID" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="配置状态">
          <el-segmented v-model="queryParams.mappingStatus" :options="mappingOptions" @change="handleQuery" />
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="queryParams.shopConfigId" clearable filterable placeholder="全部店铺" style="width:180px" @change="handleQuery">
            <el-option v-for="shop in activeShops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <div v-if="!activeShops.length" class="empty-config">
        <div><strong>还没有可执行的店铺</strong><span>先填写店铺名称、实际操作账号和私信内容，再为直播间指定店铺。</span></div>
        <el-button type="primary" @click="openShopDialog">立即配置</el-button>
      </div>

      <el-table v-loading="loading" :data="roomList" row-key="room_key" class="room-table">
        <el-table-column label="直播间" min-width="220" fixed>
          <template #default="scope"><div class="room-cell"><strong>{{ scope.row.room_name || scope.row.room_key }}</strong><small>{{ scope.row.room_key }}</small></div></template>
        </el-table-column>
        <el-table-column label="执行状态" width="140" align="center">
          <template #default="scope"><el-tag :type="roomStatus(scope.row).type" effect="light">{{ roomStatus(scope.row).text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="负责店铺" min-width="210">
          <template #default="scope">
            <div v-if="scope.row.shop_config_id" class="shop-cell"><strong>{{ scope.row.shop_name || currentShop(scope.row)?.shopName || '-' }}</strong><small>{{ scope.row.douyin_account_code || currentShop(scope.row)?.douyinAccountCode || '-' }}</small></div>
            <span v-else class="muted">尚未指定</span>
          </template>
        </el-table-column>
        <el-table-column label="配置完整度" width="150">
          <template #default="scope"><el-progress :percentage="shopCompletion(currentShop(scope.row))" :status="shopCompletion(currentShop(scope.row))===100?'success':undefined" /></template>
        </el-table-column>
        <el-table-column label="数据状态" width="100" align="center">
          <template #default="scope"><el-tag :type="scope.row.status === '1' ? 'info' : 'success'" effect="plain">{{ scope.row.status === '1' ? '停用' : '启用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最后上报" width="165"><template #default="scope">{{ formatTime(scope.row.last_report_time) }}</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openMapping(scope.row)">{{ scope.row.shop_config_id ? '更换店铺' : '指定店铺' }}</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['live:room:edit']">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['live:room:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog v-model="roomOpen" :title="roomTitle" width="520px" append-to-body>
      <el-form ref="roomRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="直播间识别码" prop="roomKey"><el-input v-model="form.roomKey" :disabled="isEdit" placeholder="通常由采集插件自动填写" /><div class="field-help">用于识别直播间，保存后不可修改。</div></el-form-item>
        <el-form-item label="直播间名称" prop="roomName"><el-input v-model="form.roomName" placeholder="例如：晚间好物专场" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roomOpen=false">取消</el-button><el-button type="primary" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="mappingOpen" :title="mappingForm.currentShopId ? '更换负责店铺' : '指定负责店铺'" width="560px" append-to-body>
      <div class="mapping-summary"><span>直播间</span><strong>{{ mappingForm.roomName }}</strong></div>
      <el-form label-width="100px">
        <el-form-item label="负责店铺" required>
          <el-select v-model="mappingForm.shopConfigId" filterable placeholder="请选择店铺" style="width:100%">
            <el-option v-for="shop in activeShops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId">
              <span>{{ shop.shopName }}</span><small class="shop-account">操作账号：{{ shop.douyinAccountCode }}</small>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert v-if="selectedMappingShop" :title="`确认后，该直播间的新用户将由 ${selectedMappingShop.shopName} 的账号处理。`" type="warning" :closable="false" show-icon />
      <div v-if="mappingForm.currentShopId" class="mapping-policy"><strong>历史任务不会改变</strong><span>已领取和已完成的任务仍保留原店铺，避免执行到错误账号。</span></div>
      <template #footer><el-button @click="mappingOpen=false">取消</el-button><el-button v-if="mappingForm.currentShopId" @click="removeMapping(mappingForm.row)">解除绑定</el-button><el-button type="primary" :loading="mappingSaving" @click="confirmMapping">确认并生效</el-button></template>
    </el-dialog>

    <el-dialog v-model="shopOpen" title="影刀店铺配置" width="1040px" append-to-body destroy-on-close :before-close="handleShopClose">
      <div class="shop-config-layout">
        <aside class="shop-list">
          <el-button type="primary" plain icon="Plus" @click="createShopDraft">新建店铺</el-button>
          <button v-for="shop in shops" :key="shop.shopConfigId" type="button" :class="{active:shopForm.shopConfigId===shop.shopConfigId}" @click="selectShop(shop)">
            <strong>{{ shop.shopName }}</strong><small>{{ shop.douyinAccountCode }}</small>
          </button>
        </aside>
        <div class="shop-editor">
          <el-tabs v-model="shopTab">
            <el-tab-pane label="基础信息" name="basic">
              <el-form :model="shopForm" label-width="110px" class="shop-form">
                <el-form-item label="店铺名称" required><el-input v-model="shopForm.shopName" placeholder="与抖店名称保持一致" /></el-form-item>
                <el-form-item label="操作抖音号" required><el-input v-model="shopForm.douyinAccountCode" placeholder="影刀浏览器实际登录的抖音号" /></el-form-item>
                <el-form-item label="配置状态"><el-switch v-model="shopForm.enabled" inline-prompt active-text="启用" inactive-text="停用" /></el-form-item>
                <el-form-item label="备注"><el-input v-model="shopForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="私信模板" name="templates">
              <div class="template-nav">
                <div class="template-nav-row"><span>发送场景</span><el-segmented v-model="templateScene" :options="templateSceneOptions" /></div>
                <div v-if="templateScene==='NO_PURCHASE'" class="template-nav-row"><span>文案分类</span><el-segmented v-model="templateGroup" :options="templateGroupOptions" /></div>
                <p>{{ templateScene==='NO_PURCHASE'?'评论匹配定向文案时使用定向内容，否则使用普通内容；任务始终同时携带退款关怀文案。':'影刀确认用户申请退款或已经退款后使用。' }}</p>
              </div>
              <div class="template-tools">
                <div><strong>{{ activeTemplateSection.label }}</strong><span>{{ activeTemplateSection.description }}</span><em v-if="templateIssueCount">{{ templateIssueCount }} 条需要完善</em></div>
                <div class="template-actions">
                  <el-button v-if="templateScene==='NO_PURCHASE'" icon="Search" @click="openTemplateTest">测试匹配</el-button>
                  <el-dropdown trigger="click" @command="handleTemplateCommand">
                    <el-button icon="MoreFilled" title="更多模板操作" />
                    <template #dropdown><el-dropdown-menu><el-dropdown-item command="enableAll">全部启用</el-dropdown-item><el-dropdown-item command="restore" divided>恢复推荐文案</el-dropdown-item></el-dropdown-menu></template>
                  </el-dropdown>
                  <el-button type="primary" icon="Plus" @click="addTemplate">新增一条</el-button>
                </div>
              </div>
              <div class="template-list">
                <div v-for="item in sceneTemplates" :key="item.templateKey" class="template-entry" :class="{'has-error':templateErrors(item).length,'is-open':expandedTemplateKey===item.templateKey}">
                  <div class="template-summary">
                    <button type="button" class="template-summary-main" @click="toggleTemplateEditor(item)">
                      <span class="summary-name"><strong>{{ item.templateName || '未命名文案' }}</strong><small v-if="item.keywordMode">{{ templateKeywordSummary(item) }}</small><small v-else>普通随机文案</small></span>
                      <span class="summary-copy">{{ renderTemplatePreview(item.content) || '还没有填写私信内容' }}</span>
                      <span v-if="templateErrors(item).length" class="summary-error">需要完善</span>
                      <el-icon><ArrowUp v-if="expandedTemplateKey===item.templateKey" /><ArrowDown v-else /></el-icon>
                    </button>
                    <span class="template-enabled" @click.stop><el-switch v-model="item.enabled" @change="value=>toggleTemplate(item,value)" /><em>{{ item.enabled?'已启用':'已停用' }}</em></span>
                    <el-button link type="danger" icon="Delete" title="删除这条文案" @click.stop="removeTemplate(item)" />
                  </div>
                  <div v-if="expandedTemplateKey===item.templateKey" class="template-editor-grid">
                    <div class="template-editor-form">
                      <label class="editor-field"><span>文案名称</span><el-input v-model="item.templateName" placeholder="例如：发货时间咨询" maxlength="100" /></label>
                      <div v-if="item.keywordMode" class="keyword-editor">
                        <div class="field-title"><strong>匹配内容</strong><span>用户评论包含任意一项时使用</span></div>
                        <div class="keyword-entry"><el-input v-model="keywordDrafts[item.templateKey]" clearable placeholder="输入后按回车，例如：什么时候发货" @keyup.enter="addKeyword(item)" /><el-button type="primary" plain @click="addKeyword(item)">添加</el-button></div>
                        <div class="keyword-tags"><el-tag v-for="keyword in item.keywords" :key="keyword" closable @close="removeKeyword(item,keyword)">{{ keyword }}</el-tag><small v-if="!(item.keywords||[]).length">还没有匹配内容，请先添加。</small></div>
                      </div>
                      <div class="message-editor-label"><span>私信内容</span><div class="variable-tools"><el-button v-for="variable in templateVariables" :key="variable.token" link type="primary" @click="insertVariable(item,variable)">+ {{ variable.label }}</el-button></div></div>
                      <el-input :ref="el=>setTemplateInputRef(item.templateKey,el)" :model-value="friendlyTemplateContent(item.content)" @input="value=>updateTemplateContent(item,value)" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="输入影刀最终发送给用户的私信内容" />
                      <div v-if="templateErrors(item).length || templateWarnings(item).length" class="template-feedback">
                        <span v-for="message in templateErrors(item)" :key="message" class="error-text">{{ message }}</span>
                        <span v-for="message in templateWarnings(item)" :key="message" class="warning-text">{{ message }}</span>
                      </div>
                    </div>
                    <aside class="message-preview">
                      <div class="message-preview-head"><strong>发送预览</strong><span>{{ shopForm.shopName || '示例店铺' }}</span></div>
                      <div class="preview-chat"><span class="preview-avatar">{{ shopPreviewInitial }}</span><p>{{ renderTemplatePreview(item.content) || '填写私信内容后，这里会实时显示。' }}</p></div>
                      <small>自动信息已使用示例内容替换</small>
                    </aside>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="执行限制" name="limits">
              <el-form :model="shopForm" label-width="130px" class="shop-form two-column">
                <el-form-item label="每日最多处理"><el-input-number v-model="shopForm.dailyLimit" :min="1" :max="10000" /></el-form-item>
                <el-form-item label="每小时最多处理"><el-input-number v-model="shopForm.hourlyLimit" :min="1" :max="1000" /></el-form-item>
                <el-form-item label="连续操作人数"><el-input-number v-model="shopForm.burstSize" :min="1" :max="100" /></el-form-item>
                <el-form-item label="批次休息分钟"><el-input-number v-model="shopForm.restMinutes" :min="1" :max="120" /></el-form-item>
                <el-form-item label="允许执行时段"><el-time-picker v-model="shopForm.allowedTimeRange" is-range value-format="HH:mm" format="HH:mm" range-separator="至" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
                <el-form-item label="连续失败暂停"><el-input-number v-model="shopForm.maxConsecutiveFailures" :min="1" :max="100" /></el-form-item>
                <el-form-item label="验证码处理"><el-switch v-model="shopForm.pauseOnCaptcha" active-text="立即暂停店铺" /></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="退款策略" name="refund">
              <el-alert title="首次查询到退款时可发送一次售后关怀；上报结果后停止后续自动营销，冷却结束仍需人工确认。" type="warning" :closable="false" class="mb12" />
              <el-form :model="shopForm" label-width="160px" class="shop-form">
                <el-form-item label="已退款冷却期"><el-input-number v-model="shopForm.refundCooldownDays" :min="1" :max="3650" /><span class="unit">天</span></el-form-item>
                <el-form-item label="取消未付款冷却期"><el-input-number v-model="shopForm.cancelledCooldownDays" :min="1" :max="365" /><span class="unit">天</span></el-form-item>
                <el-form-item label="退款处理中"><el-tag type="danger">禁止自动营销</el-tag></el-form-item>
                <el-form-item label="质量问题退款"><el-tag type="danger">永久抑制，转人工售后</el-tag></el-form-item>
                <el-form-item label="冷却结束"><el-tag type="warning">进入人工审核，不自动恢复</el-tag></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
      <template #footer><el-button @click="requestCloseShop">取消</el-button><el-button type="primary" :loading="shopSaving" @click="saveShop">保存店铺配置</el-button></template>
    </el-dialog>

    <el-dialog v-model="templateTestOpen" title="测试评论匹配" width="560px" append-to-body>
      <div class="template-test-dialog">
        <span>输入一条真实评论，查看系统会选择哪条未购买文案。</span>
        <div class="keyword-test"><el-input v-model="templateTestText" clearable placeholder="例如：什么时候发货" @keyup.enter="previewTemplateSelection" /><el-button type="primary" icon="Search" @click="previewTemplateSelection">测试</el-button></div>
        <div v-if="randomPreview" class="test-result"><small>{{ previewLabel }}</small><strong>{{ previewTemplateName }}</strong><p>{{ randomPreview }}</p></div>
      </div>
      <template #footer><el-button @click="templateTestOpen=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveRoomMapping">
import { addLiveRoom, addRpaShop, deleteLiveRoom, listLiveRooms, listRpaWorkbenchShops, mapLiveRoomShop, unmapLiveRoomShop, updateLiveRoom, updateRpaShop } from '@/api/live/viewer'
const { proxy } = getCurrentInstance()
const loading=ref(false), roomOpen=ref(false), shopOpen=ref(false), shopSaving=ref(false), mappingOpen=ref(false), mappingSaving=ref(false), isEdit=ref(false), roomTitle=ref(''), roomList=ref([]), shops=ref([]), total=ref(0), shopTab=ref('basic')
const templateScene=ref('NO_PURCHASE'), templateGroup=ref('GENERAL'), randomPreview=ref(''), previewLabel=ref(''), previewTemplateName=ref(''), templateTestText=ref(''), templateTestOpen=ref(false), expandedTemplateKey=ref(''), shopSnapshot=ref('')
const keywordDrafts=reactive({})
const templateInputRefs=new Map()
const mappingForm=reactive({row:null,roomKey:'',roomName:'',currentShopId:undefined,shopConfigId:undefined})
const mappingOptions=[{label:'全部',value:''},{label:'待映射',value:'UNMAPPED'},{label:'已映射',value:'MAPPED'}]
const queryParams=reactive({pageNum:1,pageSize:20,roomName:undefined,mappingStatus:'',shopConfigId:undefined})
const form=reactive({roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined})
const templateSet=(prefix,name,scene,contents,keywords,priority,defaultGroup=false)=>contents.map((content,index)=>({templateKey:`${prefix}_${String(index+1).padStart(2,'0')}`,templateName:`${name} ${index+1}`,scene,content,enabled:true,defaultTemplate:defaultGroup&&index===0,priority,keywords:[...(keywords||[])],keywordMode:Boolean(keywords?.length)}))
const keywordTemplate=(key,name,content,keywords)=>({templateKey:key,templateName:name,scene:'NO_PURCHASE',content,enabled:true,defaultTemplate:false,priority:10,keywords,keywordMode:true})
const defaultTemplates=()=>[
  ...templateSet('NO_PURCHASE','未购买用户','NO_PURCHASE',[
    '你好，看到你刚刚来过{{liveRoomName}}，想了解哪款产品呢？有问题可以直接告诉我。','你好，感谢关注{{shopName}}，产品功能、价格或活动方面有疑问都可以问我。','你好，刚才在直播间看到你啦，有哪款产品想进一步了解吗？','你好，感谢来到{{liveRoomName}}，如果还有没来得及问的问题，我可以继续帮你。','你好，直播间里的产品如果还没选好，我可以根据你的需求帮你看看。','你好，感谢你的关注，有产品方面的问题可以直接发给我。','你好，直播间里的内容如果有没听清的地方，我可以再帮你说明。','你好，看到你关注了我们的直播，需要我帮你找合适的产品吗？','你好，感谢来过{{liveRoomName}}，选购方面有疑问可以随时问我。','你好，这里是{{shopName}}，如果你还在比较产品，我可以帮你梳理一下。'
  ],'',10,true),
  keywordTemplate('NO_PURCHASE_KEYWORD_PRICE','价格与优惠咨询','你好，看到你比较关注价格和活动，需要我帮你确认当前到手价或可用优惠吗？',['多少钱','价格','优惠','国补','补贴','太贵','便宜','活动']),
  keywordTemplate('NO_PURCHASE_KEYWORD_GIFT','赠品咨询','你好，看到你在问赠品，需要我帮你确认当前套餐包含的赠品和领取条件吗？',['有什么赠品','送什么','送啥','赠品','礼品']),
  keywordTemplate('NO_PURCHASE_KEYWORD_COMPARE','型号对比','你好，看到你在比较不同款式，需要我根据你的使用需求帮你说明区别吗？',['有什么区别','什么区别','区别','对比','哪个好','哪款好','怎么选']),
  keywordTemplate('NO_PURCHASE_KEYWORD_SPEC','参数尺寸咨询','你好，看到你比较关注产品参数，需要我帮你确认具体尺寸、配置或容量吗？',['屏幕多大','多大屏幕','内存多大','屏幕','内存','尺寸','配置','参数','容量','公斤']),
  keywordTemplate('NO_PURCHASE_KEYWORD_SHIPPING','发货时效咨询','你好，看到你在关注发货时间，需要我帮你确认当前库存和预计发出时间吗？',['什么时候发货','延迟发货','几天到','多久到','发货']),
  keywordTemplate('NO_PURCHASE_KEYWORD_INSTALL','安装服务咨询','你好，看到你在咨询安装服务，需要我帮你确认是否包安装、服务范围和预约方式吗？',['包安装吗','包安装','上门安装','安装','上门']),
  keywordTemplate('NO_PURCHASE_KEYWORD_WARRANTY','售后保障咨询','你好，看到你比较关注售后保障，需要我帮你确认质保、退换或服务规则吗？',['质保多久','运费险','可以试用吗','质保','保修','试用','售后']),
  keywordTemplate('NO_PURCHASE_KEYWORD_LINK','购买链接咨询','你好，看到你在找对应商品，需要我帮你确认应该看哪个链接或哪款商品吗？',['几号链接','链接在哪','哪个链接','拍哪个','哪个款']),
  keywordTemplate('NO_PURCHASE_KEYWORD_AGE','年龄与使用场景','你好，看到你在确认适用阶段，可以告诉我使用者的年龄或主要需求，我帮你看看是否合适。',['几岁','幼儿园','小班','中班','大班','一年级','二年级','三年级']),
  keywordTemplate('NO_PURCHASE_KEYWORD_FUNCTION','具体功能咨询','你好，看到你比较关注具体功能，需要我结合你的使用需求帮你确认这款是否合适吗？',['不要烘干','带烘干','洗烘一体','单洗','洗烘','烘干','英语','动画片','功能']),
  ...templateSet('REFUND','退款关怀','REFUND',[
    '你好，看到你的订单正在申请退款，想确认一下是否遇到了什么问题？需要的话我可以帮你跟进。','你好，留意到你的订单有退款申请，如果是产品或服务方面的问题，可以告诉我，我来帮你处理。','你好，看到你提交了退款申请，给你带来不便很抱歉。方便说一下遇到的情况吗？','你好，你的订单退款情况我们已经关注到了，如需查询进度或协助处理，可以直接告诉我。','你好，看到订单进入了退款流程，如果还有未解决的问题，我可以继续帮你核实。','你好，关于这次退款，如果是使用、发货或商品方面的问题，可以告诉我具体情况。','你好，留意到你的售后申请了，需要协助确认退款进度或处理方案吗？','你好，很抱歉这次购物没有达到预期。退款过程中如果需要帮助，可以直接联系我。','你好，看到你的订单有售后记录，我来确认一下是否还需要我们协助处理。','你好，你的退款申请我们已经留意到了，有任何疑问都可以在这里告诉我，我会帮你跟进。'
  ],'',20)
]
const emptyShop=()=>({shopConfigId:undefined,shopName:'',douyinAccountCode:'',dailyLimit:100,hourlyLimit:15,burstSize:10,restMinutes:5,allowedTimeRange:['09:00','22:00'],refundCooldownDays:90,cancelledCooldownDays:7,pauseOnCaptcha:true,maxConsecutiveFailures:5,enabled:true,remark:'',messageTemplates:defaultTemplates()})
const shopForm=reactive(emptyShop())
const templateScenes=[
  {value:'NO_PURCHASE',label:'未购买跟进',description:'确认没有订单后使用',rule:'用户评论命中指定内容时优先使用；没有命中时，从普通文案中随机选择。'},
  {value:'REFUND',label:'退款关怀',description:'申请退款或已经退款时使用',rule:'影刀确认用户申请退款或已经退款后，从启用的关怀文案中随机选择。'}
]
const templateGroups=[
  {value:'GENERAL',label:'普通跟进',description:'评论没有匹配时随机使用'},
  {value:'KEYWORD',label:'评论定向',description:'评论说到指定内容时优先使用'}
]
const templateVariables=[
  {label:'用户称呼',placeholder:'【用户称呼】',token:'{{nickname}}'},
  {label:'店铺名称',placeholder:'【店铺名称】',token:'{{shopName}}'},
  {label:'直播间名称',placeholder:'【直播间名称】',token:'{{liveRoomName}}'}
]
const allowedTemplateTokens=new Set(templateVariables.map(item=>item.token))
const currentScene=computed(()=>templateScenes.find(item=>item.value===templateScene.value)||templateScenes[0])
const allSceneTemplates=computed(()=>shopForm.messageTemplates.filter(item=>item.scene===templateScene.value))
const sceneTemplates=computed(()=>templateScene.value!=='NO_PURCHASE'?allSceneTemplates.value:allSceneTemplates.value.filter(item=>templateGroup.value==='KEYWORD'?item.keywordMode:!item.keywordMode))
const activeTemplateSection=computed(()=>templateScene.value==='NO_PURCHASE'?(templateGroups.find(item=>item.value===templateGroup.value)||templateGroups[0]):currentScene.value)
const templateIssueCount=computed(()=>sceneTemplates.value.filter(item=>templateErrors(item).length).length)
const templateSceneOptions=computed(()=>templateScenes.map(scene=>({value:scene.value,label:scene.value==='NO_PURCHASE'?`未购买跟进 ${enabledTemplateCount(scene.value)}`:`退款关怀 ${enabledTemplateCount(scene.value)}`})))
const templateGroupOptions=computed(()=>templateGroups.map(group=>({value:group.value,label:`${group.label} ${enabledTemplateGroupCount(group.value)}`})))
const shopPreviewInitial=computed(()=>String(shopForm.shopName||'店').trim().slice(0,1)||'店')
const shopDirty=computed(()=>Boolean(shopSnapshot.value)&&shopSnapshot.value!==JSON.stringify(shopForm))
watch(templateScene,()=>{templateGroup.value='GENERAL'})
watch([templateScene,templateGroup],()=>{randomPreview.value='';previewLabel.value='';previewTemplateName.value='';nextTick(()=>{expandedTemplateKey.value=sceneTemplates.value[0]?.templateKey||''})})
const rules={roomKey:[{required:true,message:'直播间ID不能为空',trigger:'blur'}],roomName:[{required:true,message:'直播间名称不能为空',trigger:'blur'}]}
const activeShops=computed(()=>shops.value.filter(item=>item.status!=='1'))
const selectedMappingShop=computed(()=>activeShops.value.find(item=>String(item.shopConfigId)===String(mappingForm.shopConfigId)))
const pageMapped=computed(()=>roomList.value.filter(item=>item.shop_config_id).length)
const pageReady=computed(()=>roomList.value.filter(item=>roomStatus(item).text==='正常执行').length)
const pageUnmapped=computed(()=>roomList.value.length-pageMapped.value)
function getList(){loading.value=true;listLiveRooms(queryParams).then(res=>{roomList.value=res.rows||[];total.value=res.total||0}).finally(()=>loading.value=false)}
function loadShops(){return listRpaWorkbenchShops().then(res=>shops.value=res.data||[])}
function handleQuery(){queryParams.pageNum=1;getList()}
function resetQuery(){Object.assign(queryParams,{pageNum:1,roomName:undefined,mappingStatus:'',shopConfigId:undefined});getList()}
function resetRoom(){Object.assign(form,{roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined});proxy.resetForm('roomRef')}
function handleAdd(){resetRoom();isEdit.value=false;roomTitle.value='新增直播间';roomOpen.value=true}
function handleUpdate(row){resetRoom();isEdit.value=true;roomTitle.value='编辑直播间';Object.assign(form,{roomKey:row.room_key,roomName:row.room_name,source:row.source,status:row.status||'0',remark:row.remark});roomOpen.value=true}
function submitForm(){proxy.$refs.roomRef.validate(valid=>{if(!valid)return;const payload={...form};(isEdit.value?updateLiveRoom(form.roomKey,payload):addLiveRoom(payload)).then(()=>{proxy.$modal.msgSuccess('保存成功');roomOpen.value=false;getList()})})}
function currentShop(row){return shops.value.find(item=>String(item.shopConfigId)===String(row?.shop_config_id))}
function shopCompletion(shop){if(!shop)return 0;const checks=[shop.shopName,shop.douyinAccountCode,Number(shop.dailyLimit)>0,Number(shop.hourlyLimit)>0,shop.allowedStartTime,shop.allowedEndTime,(shop.messageTemplates||[]).some(item=>item.enabled&&String(item.content||'').trim())];return Math.round(checks.filter(Boolean).length/checks.length*100)}
function roomStatus(row){if(row.status==='1')return{text:'直播间已停用',type:'info'};if(!row.shop_config_id)return{text:'待指定店铺',type:'warning'};const shop=currentShop(row);if(!shop)return{text:'店铺已停用',type:'danger'};if(shopCompletion(shop)<100)return{text:'店铺配置不完整',type:'danger'};return{text:'正常执行',type:'success'} }
function openMapping(row){Object.assign(mappingForm,{row,roomKey:row.room_key,roomName:row.room_name||row.room_key,currentShopId:row.shop_config_id,shopConfigId:row.shop_config_id});mappingOpen.value=true}
function confirmMapping(){if(!mappingForm.shopConfigId)return proxy.$modal.msgWarning('请先选择负责店铺');if(String(mappingForm.shopConfigId)===String(mappingForm.currentShopId)){mappingOpen.value=false;return}mappingSaving.value=true;mapLiveRoomShop(mappingForm.roomKey,mappingForm.shopConfigId).then(()=>{proxy.$modal.msgSuccess('负责店铺已更新');mappingOpen.value=false;getList()}).finally(()=>mappingSaving.value=false)}
function removeMapping(row){proxy.$modal.confirm(`解除后，“${row.room_name||row.room_key}”的新用户不会进入自动跟进。确认解除吗？`).then(()=>unmapLiveRoomShop(row.room_key)).then(()=>{proxy.$modal.msgSuccess('已解除绑定，历史任务不受影响');mappingOpen.value=false;getList()}).catch(()=>{})}
function handleDelete(row){if(row.shop_config_id)return proxy.$modal.msgWarning('请先解除负责店铺，再删除直播间');proxy.$modal.confirm(`删除后不可恢复，但不会删除历史用户和执行记录。确认删除“${row.room_name||row.room_key}”吗？`).then(()=>deleteLiveRoom(row.room_key)).then(()=>{proxy.$modal.msgSuccess('直播间已删除');getList()}).catch(()=>{})}
function rememberShopState(){shopSnapshot.value=JSON.stringify(shopForm)}
function resetTemplateEditor(){randomPreview.value='';previewLabel.value='';previewTemplateName.value='';templateTestText.value='';nextTick(()=>{expandedTemplateKey.value=sceneTemplates.value[0]?.templateKey||''})}
function newShop(){Object.assign(shopForm,emptyShop());shopTab.value='basic';templateScene.value='NO_PURCHASE';templateGroup.value='GENERAL';resetTemplateEditor();rememberShopState()}
function editShop(shop){const oldPrefixes=['GENERAL','COMMENT','PRODUCT','PROMOTION','AFTER_SALES','FALLBACK'];const saved=shop.messageTemplates||[];const isOld=saved.length&&saved.length<=60&&saved.every(item=>{const key=String(item.templateKey||'');return oldPrefixes.includes(key)||oldPrefixes.some(prefix=>key.startsWith(`${prefix}_`))});const isPrevious=saved.length===20&&saved.every(item=>/^(NO_PURCHASE|REFUND)_\d{2}$/.test(String(item.templateKey||''))&&!(item.keywords||[]).length);const source=isOld||isPrevious?defaultTemplates():(saved.length?saved:defaultTemplates());const templates=source.map(item=>({...item,keywords:[...(item.keywords||[])],keywordMode:Boolean(item.keywords?.length)}));Object.assign(shopForm,emptyShop(),shop,{enabled:shop.status!=='1',allowedTimeRange:[shop.allowedStartTime||'09:00',shop.allowedEndTime||'22:00'],messageTemplates:templates});shopTab.value='basic';templateScene.value='NO_PURCHASE';templateGroup.value='GENERAL';resetTemplateEditor();rememberShopState()}
function confirmDiscard(action){if(!shopDirty.value)return action();proxy.$modal.confirm('当前店铺有尚未保存的修改，确定放弃这些修改吗？').then(action).catch(()=>{})}
function createShopDraft(){confirmDiscard(newShop)}
function selectShop(shop){if(String(shopForm.shopConfigId)===String(shop.shopConfigId))return;confirmDiscard(()=>editShop(shop))}
function openShopDialog(){if(shops.value.length)editShop(shops.value[0]);else newShop();shopOpen.value=true}
function enabledTemplateCount(scene){return shopForm.messageTemplates.filter(item=>item.scene===scene&&item.enabled&&String(item.content||'').trim()).length}
function enabledTemplateGroupCount(group){return shopForm.messageTemplates.filter(item=>item.scene==='NO_PURCHASE'&&item.enabled&&String(item.content||'').trim()&&(group==='KEYWORD'?item.keywordMode:!item.keywordMode)).length}
function toggleTemplateEditor(item){expandedTemplateKey.value=expandedTemplateKey.value===item.templateKey?'':item.templateKey}
function templateKeywordSummary(item){const keywords=item.keywords||[];if(!keywords.length)return '还没有匹配内容';return `${keywords.slice(0,3).join('、')}${keywords.length>3?` 等 ${keywords.length} 项`:''}`}
function openTemplateTest(){templateTestText.value='';randomPreview.value='';previewLabel.value='';previewTemplateName.value='';templateTestOpen.value=true}
function addTemplate(){const number=sceneTemplates.value.length+1;const keywordMode=templateScene.value==='NO_PURCHASE'&&templateGroup.value==='KEYWORD';const item={templateKey:`CUSTOM_${Date.now()}`,templateName:`${activeTemplateSection.value.label} ${number}`,scene:templateScene.value,content:'',enabled:false,defaultTemplate:false,priority:templateScene.value==='NO_PURCHASE'?10:20,keywords:[],keywordMode};shopForm.messageTemplates.push(item);expandedTemplateKey.value=item.templateKey;nextTick(()=>templateInputRefs.get(item.templateKey)?.textarea?.focus())}
function isRequiredLastTemplate(item){if(!item.enabled)return false;if(item.scene==='REFUND')return enabledTemplateCount('REFUND')<=1;return !item.keywordMode&&enabledTemplateGroupCount('GENERAL')<=1}
function removeTemplate(item){if(isRequiredLastTemplate(item))return proxy.$modal.msgWarning(item.scene==='REFUND'?'退款关怀至少要保留一条可用文案':'普通跟进至少要保留一条可用文案');const remove=()=>{const visible=[...sceneTemplates.value];const visibleIndex=visible.indexOf(item);shopForm.messageTemplates.splice(shopForm.messageTemplates.indexOf(item),1);if(expandedTemplateKey.value===item.templateKey)nextTick(()=>{expandedTemplateKey.value=sceneTemplates.value[visibleIndex]?.templateKey||sceneTemplates.value[visibleIndex-1]?.templateKey||''})};if(!String(item.content||'').trim())return remove();proxy.$modal.confirm(`删除“${item.templateName||'这条文案'}”后，保存配置才会正式生效。确定删除吗？`).then(remove).catch(()=>{})}
function toggleTemplate(item,value){if(!value&&isRequiredLastTemplate(item)){item.enabled=true;proxy.$modal.msgWarning(item.scene==='REFUND'?'退款关怀至少要保留一条可用文案':'普通跟进至少要保留一条可用文案');return}if(value&&!String(item.content||'').trim()){item.enabled=false;proxy.$modal.msgWarning('请先填写私信内容，再启用这条文案')}}
function addKeyword(item){const keyword=String(keywordDrafts[item.templateKey]||'').trim();if(!keyword)return proxy.$modal.msgWarning('请先输入需要匹配的内容');if(keyword.length<2||keyword.length>20)return proxy.$modal.msgWarning('匹配内容应为 2–20 个字');const keywords=item.keywords||(item.keywords=[]);if(keywords.length>=20)return proxy.$modal.msgWarning('每条文案最多添加 20 项');const normalized=keyword.toLowerCase();if(keywords.some(value=>String(value).trim().toLowerCase()===normalized))return proxy.$modal.msgWarning('这项内容已经添加过了');const usedBy=shopForm.messageTemplates.find(other=>other!==item&&other.scene===item.scene&&other.keywordMode&&(other.keywords||[]).some(value=>String(value).trim().toLowerCase()===normalized));if(usedBy)return proxy.$modal.msgWarning(`这项内容已经用于“${usedBy.templateName}”`);keywords.push(keyword);keywordDrafts[item.templateKey]=''}
function removeKeyword(item,keyword){item.keywords.splice(item.keywords.indexOf(keyword),1)}
function setTemplateInputRef(key,el){if(el)templateInputRefs.set(key,el);else templateInputRefs.delete(key)}
function friendlyTemplateContent(content){return templateVariables.reduce((text,variable)=>text.replaceAll(variable.token,variable.placeholder),String(content||''))}
function storedTemplateContent(content){return templateVariables.reduce((text,variable)=>text.replaceAll(variable.placeholder,variable.token),String(content||''))}
function updateTemplateContent(item,value){item.content=storedTemplateContent(value)}
function insertVariable(item,variable){const textarea=templateInputRefs.get(item.templateKey)?.textarea;const content=friendlyTemplateContent(item.content);const start=textarea?.selectionStart??content.length;const end=textarea?.selectionEnd??content.length;updateTemplateContent(item,`${content.slice(0,start)}${variable.placeholder}${content.slice(end)}`);nextTick(()=>{const cursor=start+variable.placeholder.length;textarea?.focus();textarea?.setSelectionRange(cursor,cursor)})}
function renderTemplatePreview(content){return String(content||'').replaceAll('{{nickname}}','小林').replaceAll('{{shopName}}',shopForm.shopName||'示例店铺').replaceAll('{{liveRoomName}}','新品直播间')}
function templateErrors(item){const errors=[];const name=String(item.templateName||'').trim(),content=String(item.content||'').trim(),keywords=(item.keywords||[]).map(value=>String(value).trim()).filter(Boolean);if(!name)errors.push('请填写模板名称');if(!content)errors.push('请填写私信内容');const tokens=content.match(/\{\{[^{}]*\}\}/g)||[];const unknown=tokens.filter(token=>!allowedTemplateTokens.has(token));const leftovers=content.replace(/\{\{[^{}]*\}\}/g,'');if(unknown.length||leftovers.includes('{{')||leftovers.includes('}}'))errors.push('包含无法识别的变量，请使用上方按钮插入');if(content&&shopForm.messageTemplates.some(other=>other!==item&&other.scene===item.scene&&String(other.content||'').trim()===content))errors.push('这条文案与同场景的其他文案重复');if(item.keywordMode&&!keywords.length)errors.push('关键词触发文案至少需要一个关键词');if(item.keywordMode&&keywords.length>20)errors.push('每条文案最多设置 20 个关键词');if(item.keywordMode&&keywords.some(keyword=>keyword.length<2||keyword.length>20))errors.push('关键词应为 2–20 个字');if(item.keywordMode&&keywords.some(keyword=>shopForm.messageTemplates.some(other=>other!==item&&other.scene===item.scene&&other.keywordMode&&(other.keywords||[]).some(value=>String(value).trim().toLowerCase()===keyword.toLowerCase()))))errors.push('存在已被同场景其他文案使用的关键词');return errors}
function templateWarnings(item){const warnings=[];const content=String(item.content||'').trim();if(content.length>200)warnings.push('文案较长，建议控制在 200 字以内');if(item.scene==='REFUND'&&/(优惠|下单|购买|活动|赠品|领券)/.test(content))warnings.push('退款关怀中包含营销用语，建议改为协助处理和进度关怀');return warnings}
function previewTemplateSelection(){const available=allSceneTemplates.value.filter(item=>item.enabled&&!templateErrors(item).length);if(!available.length)return proxy.$modal.msgWarning('当前场景没有可用文案');const comment=String(templateTestText.value||'').trim().toLowerCase();let longest=0,matches=[];for(const item of available.filter(value=>value.keywordMode)){const matched=(item.keywords||[]).map(String).filter(keyword=>comment.includes(keyword.toLowerCase())).sort((a,b)=>b.length-a.length)[0];if(!matched)continue;if(matched.length>longest){longest=matched.length;matches=[]}if(matched.length===longest)matches.push({item,matched})}let selected;if(matches.length){selected=matches[Math.floor(Math.random()*matches.length)];previewLabel.value=`命中评论定向“${selected.matched}” · 同时携带退款关怀`}else{const general=available.filter(item=>!item.keywordMode);if(!general.length)return proxy.$modal.msgWarning('评论没有命中定向文案，但当前没有可用的普通跟进文案');selected={item:general[Math.floor(Math.random()*general.length)]};previewLabel.value=comment?'未命中评论定向 · 普通跟进 + 退款关怀':'未输入评论 · 普通跟进 + 退款关怀'}previewTemplateName.value=selected.item.templateName||'未命名文案';randomPreview.value=renderTemplatePreview(selected.item.content)}
function handleTemplateCommand(command){if(command==='enableAll'){const invalid=sceneTemplates.value.find(item=>!String(item.content||'').trim()||(item.keywordMode&&!(item.keywords||[]).length));if(invalid)return proxy.$modal.msgWarning(invalid.keywordMode?'存在未填写匹配内容的文案，请完善或删除后再全部启用':'存在空文案，请填写或删除后再全部启用');sceneTemplates.value.forEach(item=>item.enabled=true);return}if(command==='restore'){const section=activeTemplateSection.value.label;proxy.$modal.confirm(`恢复后，${section}现有文案会替换为系统推荐的 10 条，确定继续吗？`).then(()=>{const isTarget=item=>item.scene===templateScene.value&&(templateScene.value!=='NO_PURCHASE'||(templateGroup.value==='KEYWORD'?item.keywordMode:!item.keywordMode));const restored=defaultTemplates().filter(isTarget);shopForm.messageTemplates.splice(0,shopForm.messageTemplates.length,...shopForm.messageTemplates.filter(item=>!isTarget(item)),...restored);randomPreview.value='';nextTick(()=>{expandedTemplateKey.value=sceneTemplates.value[0]?.templateKey||''});proxy.$modal.msgSuccess(`已恢复${section}推荐文案，保存后生效`)}).catch(()=>{})}}
function validateTemplates(){if(enabledTemplateGroupCount('GENERAL')===0){templateScene.value='NO_PURCHASE';templateGroup.value='GENERAL';return '普通跟进至少要保留一条可用文案'}if(enabledTemplateCount('REFUND')===0){templateScene.value='REFUND';return '退款关怀至少要保留一条可用文案'}for(const item of shopForm.messageTemplates){const error=templateErrors(item)[0];if(error){templateScene.value=item.scene;templateGroup.value=item.keywordMode?'KEYWORD':'GENERAL';return `${item.templateName||'未命名文案'}：${error}`}}return ''}
function handleShopClose(done){confirmDiscard(done)}
function requestCloseShop(){confirmDiscard(()=>{shopOpen.value=false})}
function saveShop(){if(!shopForm.shopName.trim()||!shopForm.douyinAccountCode.trim())return proxy.$modal.msgWarning('请填写店铺名称和操作抖音号');const templateError=validateTemplates();if(templateError)return proxy.$modal.msgWarning(templateError);const [allowedStartTime,allowedEndTime]=shopForm.allowedTimeRange||['09:00','22:00'];const data={...shopForm,status:shopForm.enabled?'0':'1',douyinShopName:shopForm.shopName,allowedStartTime,allowedEndTime,messageTemplates:shopForm.messageTemplates.map(({keywordMode,...item})=>({...item,keywords:keywordMode?[...new Set((item.keywords||[]).map(value=>String(value).trim()).filter(Boolean))]:[]}))};delete data.shopConfigId;delete data.enabled;delete data.allowedTimeRange;shopSaving.value=true;const request=shopForm.shopConfigId?updateRpaShop(shopForm.shopConfigId,data):addRpaShop(data);request.then(()=>{proxy.$modal.msgSuccess('店铺配置已保存');return loadShops()}).then(()=>{if(shopForm.shopConfigId){const current=shops.value.find(item=>item.shopConfigId===shopForm.shopConfigId);if(current)editShop(current)}else if(shops.value.length)editShop(shops.value[shops.value.length-1])}).finally(()=>shopSaving.value=false)}
function formatTime(value){return value?String(value).replace('T',' ').replace(/\.\d+$/,'').slice(0,19):'-'}
loadShops();getList()
</script>

<style scoped>
.room-mapping-page{min-height:calc(100vh - 84px);background:#f6f7f9}.page-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.page-head h2{margin:0 0 4px;font-size:22px;color:#202124}.page-head p{margin:0;color:#6b7280}.head-actions{display:flex;gap:8px}.summary-bar{display:grid;grid-template-columns:repeat(4,1fr);margin-bottom:14px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.summary-bar div{padding:12px 18px;border-right:1px solid #ebeef5}.summary-bar div:last-child{border-right:0}.summary-bar span{display:block;font-size:12px;color:#6b7280}.summary-bar strong{display:block;margin-top:3px;font-size:22px;color:#202124}.summary-bar .success{color:#16a34a}.summary-bar .warning{color:#f97316}.workspace{padding:14px 16px 16px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.filters{border-bottom:1px solid #ebeef5;margin-bottom:12px}.empty-config{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;margin-bottom:12px;border-left:3px solid #f97316;background:#fff7ed}.empty-config strong,.empty-config span{display:block}.empty-config span{margin-top:3px;font-size:13px;color:#6b7280}.room-cell{display:flex;flex-direction:column;line-height:1.5}.room-cell small{color:#909399}.shop-account{float:right;margin-left:18px;color:#909399}.room-table{width:100%}.shop-config-layout{display:grid;grid-template-columns:190px minmax(0,1fr);min-height:560px;border:1px solid #e4e7ed}.shop-list{padding:12px;border-right:1px solid #e4e7ed;background:#f7f8fa}.shop-list>.el-button{width:100%;margin-bottom:10px}.shop-list button{display:flex;flex-direction:column;width:100%;padding:10px;margin-bottom:5px;border:0;border-radius:4px;background:transparent;text-align:left;cursor:pointer}.shop-list button:hover,.shop-list button.active{background:#fff1e8;color:#ea580c}.shop-list small{margin-top:3px;color:#909399}.shop-editor{padding:0 18px 18px;min-width:0}.shop-form{max-width:650px;padding-top:12px}.two-column{display:grid;grid-template-columns:repeat(2,minmax(260px,1fr));max-width:none}.template-tools{display:flex;align-items:center;justify-content:space-between;margin:6px 0 12px;color:#6b7280}.template-list{max-height:470px;overflow:auto}.template-row{padding:12px 0;border-bottom:1px solid #ebeef5}.template-meta{display:grid;grid-template-columns:160px 140px 42px 70px 32px;gap:8px;align-items:center;margin-bottom:8px}.template-row small{display:block;margin-top:6px;color:#909399}.unit{margin-left:8px;color:#6b7280}.mb12{margin-bottom:12px}@media(max-width:900px){.summary-bar{grid-template-columns:repeat(2,1fr)}.page-head{align-items:flex-start}.page-head p{display:none}.head-actions{flex-wrap:wrap;justify-content:flex-end}.shop-config-layout{grid-template-columns:1fr}.shop-list{display:none}.two-column{grid-template-columns:1fr}}
.shop-cell{display:flex;flex-direction:column;line-height:1.45}.shop-cell small,.field-help,.muted{color:#909399;font-size:12px}.mapping-summary{display:flex;gap:12px;align-items:center;background:#fff7ed;border:1px solid #fed7aa;padding:12px 14px;margin-bottom:18px}.mapping-summary span{color:#9a3412}.mapping-policy{display:flex;flex-direction:column;gap:3px;margin-top:14px;padding:10px 12px;border-left:3px solid #f97316;background:#fff7ed}.mapping-policy span{color:#6b7280;font-size:12px}
.template-intro{display:flex;flex-direction:column;gap:4px;padding:12px 14px;margin:6px 0 12px;border-left:3px solid #f97316;background:#fff7ed}.template-intro strong{color:#292524}.template-intro span{color:#78716c;font-size:13px}.scene-switch{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));border:1px solid #dfe3e8;border-radius:6px;overflow:hidden}.scene-switch button{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:64px;padding:10px 14px;border:0;border-right:1px solid #dfe3e8;background:#fff;color:#374151;text-align:left;cursor:pointer}.scene-switch button:last-child{border-right:0}.scene-switch button.active{box-shadow:inset 0 -3px #f97316;background:#fff8f3;color:#c2410c}.scene-switch span{display:flex;flex-direction:column;gap:3px}.scene-switch small{color:#8a929f}.scene-switch b{font-size:12px;font-weight:500;white-space:nowrap}.template-tools{min-height:52px}.template-tools>div:first-child{display:flex;flex-direction:column;gap:2px}.template-tools strong{color:#303133}.template-tools span{font-size:12px}.template-actions{display:flex;align-items:center;gap:8px}.template-check{display:flex;align-items:center;gap:10px;padding:9px 12px;margin-bottom:2px;border:1px solid #dcfce7;background:#f0fdf4}.template-check.is-warning{border-color:#fed7aa;background:#fff7ed}.template-check .check-icon{display:grid;place-items:center;width:24px;height:24px;border-radius:50%;background:#16a34a;color:#fff;font-weight:700}.template-check.is-warning .check-icon{background:#f97316}.template-check div{display:flex;flex-direction:column;gap:2px}.template-check strong{font-size:13px;color:#166534}.template-check.is-warning strong{color:#9a3412}.template-check small{color:#6b7280}.random-preview{display:grid;grid-template-columns:100px minmax(0,1fr);align-items:start;padding:10px 12px;margin-bottom:4px;background:#f8fafc;border-left:3px solid #fb923c}.random-preview span{color:#6b7280;font-size:12px}.random-preview p{margin:0;color:#303133;line-height:1.6}.template-list{max-height:450px;padding-right:6px}.template-row{padding:16px 0}.template-row.has-error{border-left:3px solid #ef4444;padding-left:10px}.template-meta{display:grid;grid-template-columns:minmax(220px,320px) 130px 32px;gap:10px;align-items:end}.template-name{display:flex;flex-direction:column;gap:6px}.template-name>span,.message-editor-label>span{font-size:12px;font-weight:600;color:#4b5563}.template-name small{display:inline;margin-left:5px;font-weight:400;color:#9ca3af}.template-enabled{display:flex;align-items:center;gap:7px;height:32px}.template-enabled em{color:#6b7280;font-size:12px;font-style:normal;white-space:nowrap}.message-editor-label{display:flex;align-items:center;justify-content:space-between;margin-top:13px}.message-editor-label small{margin:0;color:#909399}.variable-tools{display:flex;align-items:center;gap:6px;min-height:40px}.variable-tools span{margin-right:2px;color:#6b7280;font-size:12px}.variable-tools .el-button{margin-left:0;color:#c2410c;border-color:#fed7aa;background:#fffaf6}.variable-tools .el-button:hover{color:#fff;border-color:#f97316;background:#f97316}.template-preview{display:grid;grid-template-columns:78px minmax(0,1fr);gap:8px;margin-top:8px;padding:9px 10px;background:#f8fafc}.template-preview span{color:#6b7280;font-size:12px;font-weight:600}.template-preview p{margin:0;color:#374151;font-size:13px;line-height:1.5}.template-feedback{display:flex;flex-wrap:wrap;gap:10px;margin-top:7px;font-size:12px}.error-text{color:#dc2626}.warning-text{color:#d97706}@media(max-width:760px){.scene-switch{grid-template-columns:1fr}.scene-switch button{border-right:0;border-bottom:1px solid #dfe3e8}.template-tools{align-items:flex-start;gap:8px}.template-actions{flex-wrap:wrap}.template-meta{grid-template-columns:minmax(0,1fr) 120px 32px}.random-preview,.template-preview{grid-template-columns:1fr}.message-editor-label{align-items:flex-start;flex-direction:column;gap:3px}}
.message-group-switch{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:12px}.message-group-switch button{display:grid;grid-template-columns:30px minmax(0,1fr) auto;align-items:center;gap:10px;padding:11px 12px;border:1px solid #dfe3e8;border-radius:5px;background:#fff;color:#374151;text-align:left;cursor:pointer}.message-group-switch button:hover{border-color:#fdba74}.message-group-switch button.active{border-color:#f97316;background:#fff8f3;box-shadow:inset 3px 0 #f97316}.message-group-switch span:not(.group-icon){display:flex;flex-direction:column;gap:2px}.message-group-switch small{color:#8a929f}.message-group-switch b{font-size:13px;color:#6b7280}.group-icon{display:grid;place-items:center;width:28px;height:28px;border-radius:4px;background:#f3f4f6;color:#6b7280;font-weight:700}.message-group-switch button.active .group-icon{background:#f97316;color:#fff}.delivery-rule{display:grid;grid-template-columns:minmax(180px,1fr) minmax(260px,1.3fr) minmax(260px,1.3fr);align-items:center;gap:8px;padding:9px 12px;margin-top:10px;border:1px solid #e5e7eb;background:#f9fafb}.delivery-rule>strong{font-size:12px;color:#4b5563}.delivery-rule>div{display:flex;align-items:center;gap:6px;font-size:12px}.delivery-rule span{color:#6b7280}.delivery-rule b{padding:3px 6px;border-radius:3px;background:#fff1e8;color:#c2410c;font-weight:500}.delivery-rule i{color:#9ca3af;font-style:normal}.template-type-note{display:flex;align-items:center;justify-content:space-between;min-height:32px;margin:9px 0;padding:6px 9px;background:#f8fafc;color:#6b7280;font-size:12px}.template-type-note .el-button{margin-left:8px}@media(max-width:900px){.delivery-rule{grid-template-columns:1fr}.message-group-switch{grid-template-columns:1fr}}@media(max-width:760px){.message-group-switch button{grid-template-columns:30px minmax(0,1fr) auto}}
.keyword-test{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px;padding:10px 0}.template-rule{display:flex;align-items:center;gap:10px;margin:12px 0}.template-rule>span,.keyword-editor>span{width:78px;color:#4b5563;font-size:12px;font-weight:600}.template-rule :deep(.el-radio-button__inner){padding-left:12px;padding-right:12px}.keyword-editor{display:grid;grid-template-columns:78px minmax(0,1fr);align-items:center;gap:8px;margin:8px 0 4px}.keyword-editor small{grid-column:2;margin-top:0;color:#909399;font-size:12px}.keyword-editor :deep(.el-select){width:100%}@media(max-width:760px){.keyword-test{grid-template-columns:1fr}.template-rule{align-items:flex-start;flex-wrap:wrap}.template-rule>span{width:100%}.keyword-editor{grid-template-columns:1fr}.keyword-editor small{grid-column:1}.variable-tools{align-items:flex-start;flex-wrap:wrap}}
.template-nav{padding:10px 0;border-bottom:1px solid #ebeef5}.template-nav-row{display:grid;grid-template-columns:72px minmax(0,460px);align-items:center;gap:10px;margin-bottom:8px}.template-nav-row>span{font-size:13px;font-weight:600;color:#4b5563}.template-nav-row :deep(.el-segmented){width:100%;--el-segmented-item-selected-bg-color:#fff;--el-segmented-item-selected-color:#ea580c}.template-nav p{margin:2px 0 0 82px;color:#7b8491;font-size:12px;line-height:1.5}.template-tools{margin:8px 0}.template-tools>div:first-child{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:baseline;column-gap:10px}.template-tools>div:first-child em{color:#dc2626;font-size:12px;font-style:normal}.keyword-editor{align-items:start;margin:10px 0}.keyword-editor>span{padding-top:8px}.keyword-entry{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px}.keyword-tags{grid-column:2;display:flex;flex-wrap:wrap;gap:6px;min-height:28px}.keyword-tags small{grid-column:auto;padding-top:4px}.keyword-tags .el-tag{max-width:100%}@media(max-width:760px){.template-nav-row{grid-template-columns:1fr}.template-nav p{margin-left:0}.template-tools>div:first-child{grid-template-columns:1fr}.keyword-entry{grid-template-columns:minmax(0,1fr) auto}.keyword-tags{grid-column:1}}
.template-row{padding:12px 0}.template-meta{align-items:center;margin-bottom:10px}.keyword-editor{display:flex;flex-direction:column;align-items:stretch;gap:7px;margin:8px 0 10px}.field-title{display:flex;align-items:baseline;gap:8px}.field-title strong{color:#4b5563;font-size:12px}.field-title span{color:#909399;font-size:12px}.keyword-tags{grid-column:auto;min-height:24px}.message-editor-label{margin:8px 0 5px}.message-editor-label .variable-tools{min-height:0;margin-left:auto}.message-editor-label .variable-tools .el-button{padding:2px 4px;border:0;background:transparent}.message-editor-label .variable-tools .el-button:hover{color:#ea580c;background:#fff7ed}@media(max-width:760px){.field-title{align-items:flex-start;flex-direction:column;gap:2px}.message-editor-label{align-items:flex-start}.message-editor-label .variable-tools{margin-left:0}}

/* Private-message editor: summary first, edit one item at a time. */
.template-list{max-height:450px;padding:0;overflow-y:auto;overflow-x:hidden;border-top:1px solid #e5e7eb}
.template-entry{border-bottom:1px solid #e5e7eb;background:#fff;transition:background-color .15s ease}
.template-entry.is-open{background:#fffaf6;box-shadow:inset 3px 0 #f97316}
.template-entry.has-error:not(.is-open){box-shadow:inset 3px 0 #ef4444}
.template-summary{display:grid;grid-template-columns:minmax(0,1fr) 92px 32px;align-items:center;gap:10px;min-height:66px;padding:7px 10px 7px 14px}
.template-summary-main{display:grid;grid-template-columns:minmax(150px,190px) minmax(120px,1fr) auto 18px;align-items:center;gap:14px;min-width:0;padding:7px 0;border:0;background:transparent;color:inherit;text-align:left;cursor:pointer}
.template-summary-main:focus-visible{outline:2px solid #fb923c;outline-offset:2px}
.summary-name{display:flex;min-width:0;flex-direction:column;gap:4px}
.summary-name strong,.summary-name small,.summary-copy{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.summary-name strong{color:#303133;font-size:14px}
.summary-name small{color:#8a929f;font-size:12px}
.summary-copy{min-width:0;color:#697180;font-size:13px}
.summary-error{color:#dc2626;font-size:12px;white-space:nowrap}
.template-summary-main>.el-icon{color:#9ca3af;font-size:15px}
.template-summary .template-enabled{justify-content:flex-start;height:auto}
.template-summary>.el-button{width:32px;height:32px;margin:0;padding:0}
.template-editor-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(220px,.8fr);gap:22px;padding:18px 20px 20px;border-top:1px solid #fed7aa}
.template-editor-form{display:flex;min-width:0;flex-direction:column;gap:11px}
.editor-field{display:flex;flex-direction:column;gap:6px}
.editor-field>span,.message-editor-label>span{color:#4b5563;font-size:12px;font-weight:600}
.template-editor-form .keyword-editor{display:flex;align-items:stretch;margin:0 0 2px}
.template-editor-form .keyword-tags{display:flex;grid-column:auto;flex-wrap:wrap;gap:6px;min-height:24px}
.template-editor-form .keyword-tags small{padding-top:4px;color:#909399;font-size:12px}
.template-editor-form .message-editor-label{display:flex;align-items:center;margin:0 0 -5px}
.template-editor-form .variable-tools{display:flex;min-height:0;flex-wrap:wrap;justify-content:flex-end}
.message-preview{align-self:stretch;min-width:0;padding:14px;background:#f4f5f7;border:1px solid #e5e7eb}
.message-preview-head{display:flex;align-items:center;justify-content:space-between;gap:12px;padding-bottom:11px;border-bottom:1px solid #dde1e6}
.message-preview-head strong{color:#303133;font-size:13px}
.message-preview-head span{overflow:hidden;color:#7b8491;font-size:12px;text-overflow:ellipsis;white-space:nowrap}
.preview-chat{display:flex;align-items:flex-start;gap:9px;padding:22px 0 18px}
.preview-avatar{display:grid;place-items:center;flex:0 0 32px;width:32px;height:32px;border-radius:4px;background:#f97316;color:#fff;font-size:13px;font-weight:700}
.preview-chat p{position:relative;max-width:calc(100% - 45px);margin:0;padding:9px 11px;border:1px solid #e5e7eb;border-radius:3px;background:#fff;color:#303133;font-size:13px;line-height:1.65;overflow-wrap:anywhere}
.preview-chat p::before{position:absolute;top:10px;left:-5px;width:8px;height:8px;border-left:1px solid #e5e7eb;border-bottom:1px solid #e5e7eb;background:#fff;content:'';transform:rotate(45deg)}
.message-preview>small{display:block;color:#9ca3af;font-size:11px;line-height:1.5}
.template-test-dialog>span{display:block;color:#6b7280;font-size:13px;line-height:1.6}
.template-test-dialog .keyword-test{padding:14px 0 4px}
.test-result{display:flex;flex-direction:column;gap:6px;margin-top:12px;padding:13px 14px;border-left:3px solid #f97316;background:#fff7ed}
.test-result small{color:#9a3412;font-size:12px}
.test-result strong{color:#303133;font-size:14px}
.test-result p{margin:2px 0 0;color:#4b5563;line-height:1.65;overflow-wrap:anywhere}
@media(max-width:900px){.template-editor-grid{grid-template-columns:1fr}.message-preview{min-height:180px}.template-summary-main{grid-template-columns:minmax(140px,190px) minmax(100px,1fr) auto 18px}}
@media(max-width:700px){.template-tools{flex-direction:column;align-items:stretch}.template-actions{justify-content:flex-end}.template-summary{grid-template-columns:minmax(0,1fr) 42px 32px}.template-summary-main{grid-template-columns:minmax(0,1fr) auto 18px;gap:8px}.summary-copy{display:none}.template-summary .template-enabled em{display:none}.template-editor-grid{gap:14px;padding:15px 12px 17px}.template-editor-form .message-editor-label{align-items:flex-start;flex-direction:column;gap:6px}.template-editor-form .variable-tools{justify-content:flex-start;margin-left:0}.keyword-entry{grid-template-columns:minmax(0,1fr) auto}}
</style>
