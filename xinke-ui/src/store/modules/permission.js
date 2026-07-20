import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
  'permission',
  {
    state: () => ({
      routes: [],
      addRoutes: [],
      defaultRoutes: [],
      topbarRouters: [],
      sidebarRouters: []
    }),
    actions: {
      setRoutes(routes) {
        this.addRoutes = routes
        this.routes = constantRoutes.concat(routes)
      },
      setDefaultRoutes(routes) {
        this.defaultRoutes = constantRoutes.concat(routes)
      },
      setTopbarRoutes(routes) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes) {
        this.sidebarRouters = routes
      },
      generateRoutes(roles) {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            const sidebarRoutes = ensureUniqueRouteNames(filterAsyncRouter(sdata))
            const rewriteRoutes = ensureUniqueRouteNames(filterAsyncRouter(rdata, false, true))
            const defaultRoutes = ensureUniqueRouteNames(filterAsyncRouter(defaultData))
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view) => {
  let res
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

// Vue Router 会在注册同名路由时移除旧路由。菜单 SQL 即使遗漏 route_name，也不应导致已有业务页面变成 404。
function ensureUniqueRouteNames(routes, usedNames = new Set(), parentPath = '') {
  routes.forEach(route => {
    const fullPath = [parentPath, route.path].filter(Boolean).join('/').replace(/\/+/g, '/')
    if (route.name) {
      const originalName = route.name
      let candidate = originalName
      if (usedNames.has(candidate)) {
        const pathSuffix = fullPath
          .split('/')
          .filter(Boolean)
          .map(segment => segment.replace(/[^a-zA-Z0-9]/g, ''))
          .filter(Boolean)
          .map(segment => segment.charAt(0).toUpperCase() + segment.slice(1))
          .join('') || 'Route'
        candidate = `${originalName}${pathSuffix}`
        let sequence = 2
        while (usedNames.has(candidate)) {
          candidate = `${originalName}${pathSuffix}${sequence++}`
        }
        route.name = candidate
        console.warn(`[router] 路由名 ${originalName} 重复，已自动调整为 ${candidate}`)
      }
      usedNames.add(candidate)
    }
    if (route.children?.length) {
      ensureUniqueRouteNames(route.children, usedNames, fullPath)
    }
  })
  return routes
}

export default usePermissionStore
