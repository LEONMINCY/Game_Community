// 文件说明：api/statistics.js，后台统计接口封装，集中请求首页图表和看板数据。
import { getRequest } from '@/utils/http'

export function getAdminDashboard() {
  return getRequest('/admin/statistics/dashboard')
}

export function getAdminStatistics() {
  return getRequest('/admin/statistics/')
}

export function getUserGrowthTrend() {
  return getRequest('/admin/statistics/user-growth')
}

export function getHotGames() {
  return getRequest('/admin/statistics/hot-games')
}

export function getGameTypes() {
  return getRequest('/admin/statistics/game-types')
}

export function getActivityAnalysis() {
  return getRequest('/admin/statistics/activity-analysis')
}

export function getGameSalesRanking() {
  return getRequest('/admin/statistics/game-sales-ranking')
}

export function getSalesOverview() {
  return getRequest('/admin/statistics/sales-overview')
}

export function getPostInteractionHeatmap() {
  return getRequest('/admin/statistics/post-interaction-heatmap')
}

export function getUserAgeDistribution() {
  return getRequest('/admin/statistics/user-age-distribution')
}

export function getNewsViews() {
  return getRequest('/admin/statistics/news-views')
}

export function getGameGoodRates() {
  return getRequest('/admin/statistics/game-good-rates')
}

export function getModeratorDashboard() {
  return getRequest('/admin/statistics/moderator-dashboard')
}
