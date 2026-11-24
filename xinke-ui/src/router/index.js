import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/ops-people/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/system/user',
    redirect: to => ({path:'/ops-people/user',query:to.query}),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  // Keep legacy live-ops links working after the menu is grouped into folders.
  ...['giftCatalog', 'giftInventory'].map(path => ({
    path: `/live-ops/${path}`,
    redirect: `/live-ops/gift/${path}`,
    hidden: true
  })),
  {
    path: '/live-ops/liveSubject',
    component: Layout,
    hidden: true,
    children: [{path:'',component:()=>import('@/views/live/liveSubject/index.vue'),name:'BusinessConfigurationLegacy',meta:{title:'直播间与店铺配置'}}]
  },
  {
    path: '/live-ops/giftAdmin',
    redirect: to => to.query.section === 'inventory'
      ? '/live-ops/giftMaterial?section=inventory'
      : '/live-ops/giftMaterial',
    hidden: true
  },
  {
    path: '/live-ops/gift/giftAdmin',
    redirect: to => to.query.section === 'inventory'
      ? '/live-ops/giftMaterial?section=inventory'
      : '/live-ops/giftMaterial',
    hidden: true
  },
  // The old all-in-one customer workbench is frozen. Keep existing
  // bookmarks useful by sending them to the new customer center while the
  // database menu migration is rolled out.
  {
    path: '/live-ops/customer-workbench',
    redirect: to => ({ path: '/live-ops/customer-center', query: to.query }),
    hidden: true
  },
  {
    path: '/live-ops/live/customer-workbench',
    redirect: to => ({ path: '/live-ops/customer-center', query: to.query }),
    hidden: true
  },
  // Keep the new center reachable while the backend menu migration or
  // dynamic-menu cache is being rolled out. Visibility and permissions for
  // the sidebar remain controlled by the backend menu tree.
  {
    path: '/live-ops/customer-center',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/liveCustomer/center/index.vue'),
        name: 'LiveCustomerCenterDirect',
        meta: { title: '直播客户中心', icon: 'user' }
      }
    ]
  },
  {
    path: '/live-ops/liveMetrics',
    component: Layout,
    hidden: true,
    children: [
      { path: '', component: () => import('@/views/live/metrics/index.vue'), name: 'LiveMetricsDirect', meta: { title: '主播数据上报', icon: 'switch' } },
      { path: 'room/:roomId', component: () => import('@/views/live/metrics/room.vue'), name: 'LiveMetricsRoom', meta: { title: '直播间交接记录', activeMenu: '/ops-anchors/liveMetrics' } }
    ]
  },
  {
    path: '/live-ops/liveMetricsReport',
    component: Layout,
    hidden: true,
    redirect: '/live-ops/liveMetrics',
    children: []
  },
  {
    path: '/live-ops/anchor-releases',
    component: Layout,
    hidden: true,
    children: [{path:'',component:()=>import('@/views/live/anchorReleases/index.vue'),name:'AnchorReleaseLegacy',meta:{title:'App 版本发布'}}]
  },
  {
    path: '/live-ops/retention-center',
    component: Layout,
    hidden: true,
    children: [{ path: '', component: () => import('@/views/live/retention/index.vue'), name: 'RetentionCenterDirect', meta: { title: '挽单与转推中心', icon: 'documentation' } }]
  },
  {
    path: '/live-ops/freight-collection',
    component: Layout,
    hidden: true,
    children: [{ path: '', component: () => import('@/views/live/freight/index.vue'), name: 'FreightCollectionDirect', meta: { title: '运费征收明细', icon: 'money' } }]
  },
  {
    path: '/live-ops/jd-customers',
    component: Layout,
    hidden: true,
    children: [{ path: '', component: () => import('@/views/live/jd-customers/index.vue'), name: 'JdCustomersDirect', meta: { title: '京东客户信息', icon: 'peoples' } }]
  },
  ...[
    ['giftLedger', 'ledger'],
    ['giftPreference', 'preference'],
    ['giftSummary', 'bi']
  ].flatMap(([path, section]) => [
    {
      path: `/live-ops/${path}`,
      redirect: to => ({ path: '/live-ops/giftWorkbench', query: { ...to.query, section } }),
      hidden: true
    },
    {
      path: `/live-ops/gift/${path}`,
      redirect: to => ({ path: '/live-ops/giftWorkbench', query: { ...to.query, section } }),
      hidden: true
    }
  ]),
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
]

// 动态路由，基于用户权限动态去加载
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/ops-people/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
