// 文件说明：main.js，前端启动入口，挂载Vue应用、Element Plus、Pinia和全局图标。
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router/index.js'
import './style.css'
import App from './App.vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import { buildLoginLocation, hasToken, isProtectedPath } from '@/utils/auth'

const app = createApp(App)
const pinia = createPinia()
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
//路由拦截
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requireAuth) || isProtectedPath(to.path, to.query)
  if (requiresAuth && !hasToken()) {
    next(buildLoginLocation(to.fullPath))
    return
  }
  next()
})
app.use(ElementPlus, {
  locale: zhCn,
});

app.mount('#app')
