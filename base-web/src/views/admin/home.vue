<!-- 文件说明：views/admin/home.vue，后台首页看板，展示用户、帖子、订单、销量和社区运营图表。 -->
<template>
  <div class="home-container" v-loading="loading">
    <el-row :gutter="16" class="data-overview">
      <el-col v-for="item in activeStatistics" :key="item.label" :xs="24" :sm="12" :md="6">
        <el-card shadow="hover" class="data-card">
          <div class="card-content">
            <el-icon :size="34"><component :is="item.icon" /></el-icon>
            <div class="card-right">
              <div class="card-value">{{ item.value }}</div>
              <div class="card-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <section class="dashboard-grid">
      <el-card
        v-for="chart in activeChartCards"
        :key="chart.key"
        shadow="hover"
        class="chart-card"
        :class="{ wide: chart.wide }"
      >
        <template #header>
          <div class="card-header">
            <span>{{ chart.title }}</span>
            <el-button size="small" text type="primary" @click="exportChart(chart.key, chart.title)">
              导出
            </el-button>
          </div>
        </template>
        <div :ref="setChartRef(chart.key)" class="chart"></div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRequest } from '@/utils/http'
import {
  getAdminDashboard,
  getModeratorDashboard,
} from '@/api/statistics'

const loading = ref(false)
const isModerator = ref(false)
const chartRefs = ref({})
const chartInstances = new Map()
const adminDashboardData = ref({})
const moderatorDashboardData = ref({})
const adminStatRaw = ref({})
let echartsRuntime = null

const statisticsData = ref([
  { label: '总用户数', value: '0', icon: 'User' },
  { label: '今日活跃', value: '0', icon: 'View' },
  { label: '总帖子数', value: '0', icon: 'Document' },
  { label: '本月新增', value: '0', icon: 'TrendCharts' }
])

const moderatorStatistics = ref([
  { label: '本月已审核', value: '0', icon: 'Checked' },
  { label: '待审核帖子', value: '0', icon: 'Clock' },
  { label: '待处理举报', value: '0', icon: 'Warning' },
  { label: '本月已处理举报', value: '0', icon: 'CircleCheck' }
])

const adminChartCards = [
  { key: 'userGrowth', title: '用户增长趋势' },
  { key: 'hotPosts', title: '热门游戏类帖子TOP5' },
  { key: 'gameType', title: '游戏分类占比', wide: true },
  { key: 'activity', title: '活跃度分析' },
  { key: 'salesRanking', title: '游戏销量排名' },
  { key: 'salesOverview', title: '平台游戏销售总额统计' },
  { key: 'postHeatmap', title: '帖子评论和点赞热力图' },
  { key: 'age', title: '用户年龄占比' },
  { key: 'newsViews', title: '新闻浏览量统计' },
  { key: 'goodRate', title: '游戏好评率统计' },
  { key: 'operationFunnel', title: '内容运营漏斗' },
  { key: 'typeTreemap', title: '游戏分类矩形树图' },
  { key: 'newsRose', title: '新闻浏览玫瑰图' },
  { key: 'goodRateGauge', title: '平均好评率仪表盘' },
  { key: 'salesScatter', title: '销量分布散点图' }
]

const moderatorChartCards = [
  { key: 'modGamePost', title: '关联游戏发帖数统计' },
  { key: 'modPostView', title: '帖子浏览量统计' },
  { key: 'modTypeLike', title: '各类游戏帖子点赞数统计' },
  { key: 'modTypeFavorite', title: '各类游戏帖子收藏数统计' },
  { key: 'modReview', title: '当月审核与待审核' },
  { key: 'modGamePostPie', title: '关联游戏发帖占比' },
  { key: 'modReviewFunnel', title: '审核处理漏斗' },
  { key: 'modInteractionRadar', title: '社区审核互动雷达' }
]

const activeStatistics = computed(() => isModerator.value ? moderatorStatistics.value : statisticsData.value)
const activeChartCards = computed(() => isModerator.value ? moderatorChartCards : adminChartCards)

const formatNumber = value => Number(value || 0).toLocaleString()
const numberValue = value => Number(String(value ?? 0).replace(/,/g, '')) || 0
const mapList = value => Array.isArray(value) ? value : []
const sum = (rows, key = 'value') => mapList(rows).reduce((total, item) => total + numberValue(item[key]), 0)
const avg = (rows, key = 'value') => {
  const list = mapList(rows)
  return list.length ? sum(list, key) / list.length : 0
}
const scale = (value, max) => Math.max(0, Math.min(100, max ? (numberValue(value) / max) * 100 : 0))

const setChartRef = key => el => {
  if (el) chartRefs.value[key] = el
}

const loadRole = async () => {
  try {
    const res = await getRequest('/user-info/get-real')
    const permissions = res.data?.permissions || []
    isModerator.value = Number(res.data?.roleId) === 3 || permissions.includes('moderator') || permissions.includes('社区审核员')
  } catch (error) {
    isModerator.value = false
  }
}

const loadDashboard = async () => {
  loading.value = true
  try {
    if (isModerator.value) {
      await loadModeratorDashboard()
    } else {
      await loadAdminDashboard()
    }
    scheduleRenderCharts()
  } finally {
    loading.value = false
  }
}

const loadAdminDashboard = async () => {
  const res = await getAdminDashboard()
  const data = res.data || {}

  adminStatRaw.value = data.statistics || {}
  statisticsData.value = [
    { label: '总用户数', value: formatNumber(adminStatRaw.value.totalUsers), icon: 'User' },
    { label: '今日活跃', value: formatNumber(adminStatRaw.value.activeUsers), icon: 'View' },
    { label: '总帖子数', value: formatNumber(adminStatRaw.value.totalPosts), icon: 'Document' },
    { label: '本月新增', value: formatNumber(adminStatRaw.value.newUsers), icon: 'TrendCharts' }
  ]

  adminDashboardData.value = {
    userGrowth: data.userGrowth || {},
    hotGames: data.hotGames || [],
    gameTypes: data.gameTypes || [],
    activity: data.activity || {},
    salesRanking: data.salesRanking || [],
    salesOverview: data.salesOverview || {},
    postHeatmap: data.postHeatmap || {},
    ageDistribution: data.ageDistribution || [],
    newsViews: data.newsViews || [],
    goodRates: data.goodRates || []
  }
}

const loadModeratorDashboard = async () => {
  const res = await getModeratorDashboard()
  const data = res.data || {}
  const reviewSummary = data.reviewSummary || {}
  moderatorStatistics.value = [
    { label: '本月已审核', value: formatNumber(reviewSummary.reviewedThisMonth), icon: 'Checked' },
    { label: '待审核帖子', value: formatNumber(reviewSummary.pendingPosts), icon: 'Clock' },
    { label: '待处理举报', value: formatNumber(reviewSummary.pendingReports), icon: 'Warning' },
    { label: '本月已处理举报', value: formatNumber(reviewSummary.handledReportsThisMonth), icon: 'CircleCheck' }
  ]
  moderatorDashboardData.value = data
}

const scheduleRenderCharts = () => {
  nextTick(() => {
    requestAnimationFrame(() => {
      renderCharts().then(() => requestAnimationFrame(resizeCharts))
    })
  })
}

const loadEcharts = async () => {
  if (!echartsRuntime) {
    echartsRuntime = await import('echarts')
  }
  return echartsRuntime
}

const renderCharts = async () => {
  const echarts = await loadEcharts()
  const options = isModerator.value ? buildModeratorOptions() : buildAdminOptions()
  activeChartCards.value.forEach(chartMeta => {
    const el = chartRefs.value[chartMeta.key]
    if (!el) return
    const chart = chartInstances.get(chartMeta.key) || echarts.init(el)
    chartInstances.set(chartMeta.key, chart)
    chart.resize()
    chart.setOption(options[chartMeta.key] || emptyOption(), true)
  })
}

const resizeCharts = () => {
  chartInstances.forEach(chart => chart && chart.resize())
}

const disposeCharts = () => {
  chartInstances.forEach(chart => chart && chart.dispose())
  chartInstances.clear()
}

const exportChart = (key, title) => {
  const chart = chartInstances.get(key)
  if (!chart) {
    ElMessage.warning('图表还未加载完成')
    return
  }
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
  const link = document.createElement('a')
  link.href = url
  link.download = `${String(title).replace(/[\\/:*?"<>|]/g, '_')}.png`
  link.click()
}

const buildAdminOptions = () => {
  const data = adminDashboardData.value
  const goodRateAvg = Math.round(avg(data.goodRates))
  return {
    userGrowth: lineOption(data.userGrowth.months || [], data.userGrowth.values || [], '新增用户'),
    hotPosts: horizontalBarOption((data.hotGames || []).slice(0, 10), '帖子数', '#3b82f6'),
    gameType: pieOption(data.gameTypes || [], true),
    activity: radarOption(data.activity.indicator || [], data.activity.value || [], '活跃度'),
    salesRanking: horizontalBarOption((data.salesRanking || []).slice(0, 10), '销量', '#22c55e'),
    salesOverview: salesOverviewOption(data.salesOverview || {}),
    postHeatmap: heatmapOption(data.postHeatmap || {}),
    age: pieOption(data.ageDistribution || []),
    newsViews: barOption((data.newsViews || []).slice(0, 12), '浏览量', '#f97316'),
    goodRate: barOption((data.goodRates || []).slice(0, 12), '好评率', '#22c55e', { max: 100, suffix: '%' }),
    operationFunnel: funnelOption([
      { name: '总用户数', value: adminStatRaw.value.totalUsers || 0 },
      { name: '今日活跃', value: adminStatRaw.value.activeUsers || 0 },
      { name: '总帖子数', value: adminStatRaw.value.totalPosts || 0 },
      { name: '本月新增', value: adminStatRaw.value.newUsers || 0 }
    ]),
    typeTreemap: treemapOption(data.gameTypes || []),
    newsRose: rosePieOption((data.newsViews || []).slice(0, 10)),
    goodRateGauge: gaugeOption(goodRateAvg, '平均好评率'),
    salesScatter: scatterOption((data.salesRanking || []).slice(0, 30), '排名', '销量')
  }
}

const buildModeratorOptions = () => {
  const data = moderatorDashboardData.value || {}
  const reviewSummary = data.reviewSummary || {}
  const typeLikes = data.typeLikes || []
  const typeFavorites = data.typeFavorites || []
  return {
    modGamePost: horizontalBarOption(data.gamePostCounts || [], '帖子数', '#3b82f6'),
    modPostView: barOption(data.postViews || [], '浏览量', '#6366f1'),
    modTypeLike: barOption(typeLikes, '点赞数', '#f97316'),
    modTypeFavorite: barOption(typeFavorites, '收藏数', '#a855f7'),
    modReview: pieOption([
      { name: '本月已审核', value: reviewSummary.reviewedThisMonth || 0 },
      { name: '待审核', value: reviewSummary.pendingPosts || 0 }
    ]),
    modGamePostPie: pieOption(data.gamePostCounts || []),
    modReviewFunnel: funnelOption([
      { name: '待审核帖子', value: reviewSummary.pendingPosts || 0 },
      { name: '本月已审核', value: reviewSummary.reviewedThisMonth || 0 },
      { name: '待处理举报', value: reviewSummary.pendingReports || 0 },
      { name: '已处理举报', value: reviewSummary.handledReportsThisMonth || 0 }
    ]),
    modInteractionRadar: radarOption(
      ['点赞', '收藏', '浏览', '已审核', '待处理'],
      [
        scale(sum(typeLikes), 500),
        scale(sum(typeFavorites), 500),
        scale(sum(data.postViews || []), 5000),
        scale(reviewSummary.reviewedThisMonth, 100),
        scale((reviewSummary.pendingPosts || 0) + (reviewSummary.pendingReports || 0), 100)
      ],
      '审核概览'
    )
  }
}

const emptyOption = () => ({
  title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#9ca3af', fontSize: 14 } }
})

const commonGrid = { left: 28, right: 24, top: 32, bottom: 48, containLabel: true }

const barOption = (rows, name, color = '#3b82f6', extra = {}) => ({
  tooltip: { trigger: 'axis', valueFormatter: value => `${value}${extra.suffix || ''}` },
  grid: commonGrid,
  xAxis: {
    type: 'category',
    data: mapList(rows).map(item => item.name),
    axisLabel: { interval: 0, rotate: mapList(rows).length > 6 ? 28 : 0, overflow: 'truncate', width: 88 }
  },
  yAxis: { type: 'value', max: extra.max },
  series: [{ name, type: 'bar', data: mapList(rows).map(item => numberValue(item.value)), barWidth: 18, itemStyle: { color, borderRadius: [5, 5, 0, 0] } }]
})

const horizontalBarOption = (rows, name, color = '#3b82f6') => {
  const list = [...mapList(rows)].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 24, top: 28, bottom: 28, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: list.map(item => item.name), axisLabel: { overflow: 'truncate', width: 110 } },
    series: [{ name, type: 'bar', data: list.map(item => numberValue(item.value)), barWidth: 16, itemStyle: { color, borderRadius: [0, 5, 5, 0] } }]
  }
}

const lineOption = (labels, values, name) => ({
  tooltip: { trigger: 'axis' },
  grid: commonGrid,
  xAxis: { type: 'category', boundaryGap: false, data: labels },
  yAxis: { type: 'value' },
  series: [{ name, data: values, type: 'line', smooth: true, areaStyle: { opacity: 0.16 }, itemStyle: { color: '#2563eb' } }]
})

const pieOption = (rows, spacious = false) => ({
  tooltip: { trigger: 'item' },
  legend: {
    type: 'scroll',
    orient: spacious ? 'vertical' : 'horizontal',
    right: spacious ? 8 : 'auto',
    top: spacious ? 20 : 'auto',
    bottom: spacious ? 'auto' : 0,
    itemWidth: 10,
    itemHeight: 10,
    pageIconSize: 10,
    textStyle: { overflow: 'truncate', width: spacious ? 120 : 90 }
  },
  series: [{
    type: 'pie',
    radius: ['34%', '62%'],
    center: spacious ? ['42%', '50%'] : ['50%', '42%'],
    avoidLabelOverlap: true,
    minShowLabelAngle: 8,
    label: {
      show: true,
      formatter: '{b}\n{d}%',
      overflow: 'truncate',
      width: spacious ? 110 : 80
    },
    labelLine: { show: true, length: 10, length2: 8 },
    labelLayout: { hideOverlap: true },
    data: mapList(rows).map(item => ({ name: item.name, value: numberValue(item.value) }))
  }]
})

const rosePieOption = rows => ({
  tooltip: { trigger: 'item' },
  legend: { type: 'scroll', bottom: 0 },
  series: [{
    type: 'pie',
    roseType: 'radius',
    radius: [20, 105],
    center: ['50%', '43%'],
    avoidLabelOverlap: true,
    label: { show: true, formatter: '{b}', overflow: 'truncate', width: 90 },
    labelLayout: { hideOverlap: true },
    data: mapList(rows).map(item => ({ name: item.name, value: numberValue(item.value) }))
  }]
})

const radarOption = (labels, values, name) => ({
  tooltip: {},
  radar: {
    radius: '62%',
    indicator: mapList(labels).map(label => ({ name: label, max: 100 }))
  },
  series: [{ type: 'radar', data: [{ value: mapList(values).map(item => numberValue(item)), name, areaStyle: { opacity: 0.18 } }] }]
})

const salesOverviewOption = data => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 28, right: 28, top: 48, bottom: 36, containLabel: true },
  xAxis: { type: 'category', data: data.months || [] },
  yAxis: [{ type: 'value', name: '销售额' }, { type: 'value', name: '订单' }],
  series: [
    { name: '销售额', type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: data.amounts || [] },
    { name: '订单数', type: 'bar', yAxisIndex: 1, data: data.counts || [], barWidth: 16 }
  ]
})

const heatmapOption = data => ({
  tooltip: { position: 'top' },
  grid: { top: 35, left: 70, right: 20, bottom: 48 },
  xAxis: { type: 'category', data: data.days || [] },
  yAxis: { type: 'category', data: data.hours || [] },
  visualMap: { min: 0, max: data.max || 10, calculable: true, orient: 'horizontal', left: 'center', bottom: 0 },
  series: [{ type: 'heatmap', data: data.data || [], label: { show: false } }]
})

const funnelOption = rows => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'funnel',
    left: '10%',
    top: 24,
    bottom: 20,
    width: '80%',
    sort: 'descending',
    label: { formatter: '{b}: {c}' },
    data: mapList(rows).map(item => ({ name: item.name, value: numberValue(item.value) }))
  }]
})

const treemapOption = rows => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'treemap',
    roam: false,
    breadcrumb: { show: false },
    label: { show: true, formatter: '{b}', overflow: 'truncate' },
    data: mapList(rows).map(item => ({ name: item.name, value: numberValue(item.value) }))
  }]
})

const gaugeOption = (value, name) => ({
  tooltip: { formatter: '{b}: {c}%' },
  series: [{
    type: 'gauge',
    min: 0,
    max: 100,
    progress: { show: true, width: 14 },
    axisLine: { lineStyle: { width: 14 } },
    detail: { valueAnimation: true, formatter: '{value}%' },
    data: [{ value, name }]
  }]
})

const scatterOption = (rows, xName, yName) => ({
  tooltip: {
    trigger: 'item',
    formatter: params => `${params.data[2]}<br>${xName}: ${params.data[0]}<br>${yName}: ${params.data[1]}`
  },
  grid: commonGrid,
  xAxis: { type: 'value', name: xName },
  yAxis: { type: 'value', name: yName },
  series: [{
    type: 'scatter',
    symbolSize: value => Math.max(8, Math.min(26, numberValue(value[1]) / 5 + 8)),
    data: mapList(rows).map((item, index) => [index + 1, numberValue(item.value), item.name]),
    itemStyle: { color: '#14b8a6' }
  }]
})

onMounted(async () => {
  await loadRole()
  await nextTick()
  await loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style lang="less" scoped>
.home-container {
  padding: 20px;
  min-width: 0;
}

.data-overview {
  margin-bottom: 16px;
}

.data-card {
  margin-bottom: 16px;

  .card-content {
    min-height: 82px;
    display: flex;
    align-items: center;
    gap: 16px;
  }
}

.card-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.card-label {
  margin-top: 5px;
  font-size: 14px;
  color: #909399;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.chart-card {
  min-width: 0;

  &.wide {
    grid-column: 1 / -1;
  }

  :deep(.el-card__body) {
    padding: 0 16px 16px;
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.chart {
  height: 320px;
  width: 100%;
}

.chart-card.wide .chart {
  height: 360px;
}

@media (max-width: 760px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: auto;
  }
}
</style>
