// 文件说明：utils/auth.js，登录态工具，统一判断游客访问和需要登录的交互跳转。
import router from '@/router'
import { ElMessage } from 'element-plus'

const publicPostMatchers = [
  /^\/noLogin\//,
  /^\/game\/list$/,
  /^\/post\/page$/,
  /^\/comment\/list$/,
  /^\/news\/page$/,
  /^\/news\/comment\/list$/
]

const protectedRoutes = [
  '/admin',
  '/addForum',
  '/ai-assistant',
  '/chat',
  '/my-posts',
  '/wishlist',
  '/cart',
  '/my-orders',
  '/privacy-settings',
  '/paymentResults'
]

export const hasToken = () => Boolean(localStorage.getItem('token'))

export const isPublicPostUrl = (url = '') => {
  const cleanUrl = String(url).split('?')[0]
  return publicPostMatchers.some(matcher => matcher.test(cleanUrl))
}

export const isProtectedPath = (path = '', query = {}) => {
  if (path === '/userinfo' && !query.userId) {
    return true
  }
  return protectedRoutes.some(route => path === route || path.startsWith(`${route}/`))
}

export const buildLoginLocation = (redirect) => ({
  path: '/login',
  query: redirect && redirect !== '/login' ? { redirect } : {}
})

export const requireLogin = (redirect, message = '请先登录后再操作') => {
  if (hasToken()) {
    return true
  }
  if (message) {
    ElMessage.warning(message)
  }
  router.push(buildLoginLocation(redirect || router.currentRoute.value.fullPath))
  return false
}

