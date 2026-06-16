<!-- 文件说明：views/rankings/index.vue，游戏榜单页面，展示热销、好评、期待、优惠和平台维度排行。 -->
<template>
  <div class="rankings-page">
    <div class="rankings-head">
      <div>
        <h2>游戏榜单</h2>
      </div>
      <div class="head-stats">
        <div>
          <strong>{{ displayTotal }}</strong>
          <span>{{ viewAll ? '全部结果' : '前100范围' }}</span>
        </div>
        <div>
          <strong>{{ activeTabLabel }}</strong>
          <span>当前榜单</span>
        </div>
      </div>
    </div>

    <div class="rank-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="rank-filters">
      <el-select v-model="filters.type" clearable placeholder="全部类型" @change="reload">
        <el-option v-for="type in gameTypes" :key="type" :label="type" :value="type" />
      </el-select>
      <el-select v-model="filters.platform" clearable placeholder="全部平台" @change="reload">
        <el-option v-for="platform in platforms" :key="platform" :label="platform" :value="platform" />
      </el-select>
      <el-segmented
        v-if="activeTab === 'sales' || activeTab === 'discounts'"
        v-model="filters.period"
        :options="periodOptions"
        @change="reload"
      />
      <el-select
        v-if="activeTab === 'reviews'"
        v-model="filters.gameId"
        clearable
        filterable
        placeholder="选择游戏查看评价占比"
        @change="reload"
      >
        <el-option v-for="game in gameOptions" :key="game.id" :label="game.name" :value="game.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="reload">刷新</el-button>
      <el-button icon="Refresh" @click="resetFilters">重置</el-button>
    </div>

    <section class="rank-list-panel">
      <div class="rank-section-title">
        <div>
          <h3>{{ activeTabLabel }}</h3>
          <p>{{ viewAll ? `共 ${rawTotal} 条，当前展示全部范围` : `默认展示前100，当前第 ${filters.pageNo} 页` }}</p>
        </div>
        <div class="rank-section-actions">
          <el-button v-if="rawTotal > 100 && !viewAll" text type="primary" @click="showAll">
            查看全部
          </el-button>
          <el-button v-if="viewAll" text type="primary" @click="backTop100">
            返回前100
          </el-button>
        </div>
      </div>

      <div class="rank-list-head">
        <span>排名</span>
        <span>游戏</span>
        <span>标签</span>
        <span>{{ metricTitle }}</span>
      </div>

      <div v-loading="loading" class="rank-list">
        <div
          v-for="row in rows"
          :key="rowKey(row)"
          class="rank-row"
          :class="{ clickable: row.id }"
          @click="row.id && goGame(row.id)"
        >
          <div class="rank-number">{{ row.rank }}</div>
          <img v-if="row.icon" class="rank-cover" :src="replaceURL(row.icon)" alt="" loading="lazy">
          <div v-else class="rank-platform">{{ shortPlatform(row.platform) }}</div>

          <div class="rank-info">
            <div class="rank-title">
              {{ row.name || row.platform }}
              <el-tag v-if="row.newListed" size="small" type="success">新上榜</el-tag>
            </div>
            <div class="rank-sub">
              <span v-if="row.finalPrice !== undefined">￥{{ row.finalPrice }}</span>
              <span v-if="row.discount > 0">-{{ row.discount }}%</span>
              <span v-if="row.sales !== undefined">销量 {{ row.sales || 0 }}</span>
            </div>
          </div>

          <div class="rank-tags">
            <span v-for="tag in displayTags(row)" :key="tag">{{ tag }}</span>
          </div>

          <div class="rank-metric">
            <strong>{{ formatMetric(row) }}</strong>
            <small v-if="activeTab === 'reviews'">好评 {{ row.goodCount || 0 }} / 差评 {{ row.badCount || 0 }}</small>
            <small v-else-if="activeTab === 'expected'">愿望单人数</small>
            <small v-else-if="activeTab === 'platforms'">销量 {{ row.sales || 0 }} / 游戏 {{ row.gameCount || 0 }}</small>
            <el-tooltip
              v-else
              :content="rankChangeTip(row)"
              placement="top"
            >
              <span class="rank-change" :class="rankChangeClass(row)">
                {{ rankChangeText(row) }}
              </span>
            </el-tooltip>
          </div>
        </div>
        <el-empty v-if="!loading && rows.length === 0" description="暂无榜单数据" />
      </div>

      <el-pagination
        v-if="displayTotal > filters.pageSize"
        class="rank-pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :total="displayTotal"
        :page-sizes="[10, 20]"
        :page-size="filters.pageSize"
        :current-page="filters.pageNo"
        @size-change="changeSize"
        @current-change="changePage"
      />
    </section>

    <section class="chart-section">
      <div class="chart-section-head">
        <h3>数据统计</h3>
      </div>
      <div class="rank-charts">
        <div v-for="(title, index) in chartTitles" :key="`${activeTab}-${title}`" class="chart-card" :class="{ wide: index === 0 }">
          <h4>{{ title }}</h4>
          <div :ref="setChartRef(index)" class="chart"></div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRequest } from '@/utils/http'
import { replaceURL } from '@/utils/tools'

const router = useRouter()
const loading = ref(false)
const activeTab = ref('sales')
const rows = ref([])
const rawTotal = ref(0)
const charts = ref({})
const gameTypes = ref([])
const platforms = ref([])
const gameOptions = ref([])
const viewAll = ref(false)
const chartRefs = ref([])
let chartInstances = []
let rankingRequestSeq = 0
let echartsRuntime = null

const tabs = [
  { key: 'sales', label: '热销游戏' },
  { key: 'reviews', label: '好评排行' },
  { key: 'expected', label: '期待榜' },
  { key: 'discounts', label: '优惠促销' },
  { key: 'platforms', label: '平台榜单' }
]

const periodOptions = [
  { label: '总榜', value: 'total' },
  { label: '年榜', value: 'year' },
  { label: '月榜', value: 'month' },
  { label: '周榜', value: 'week' }
]

const filters = ref({
  type: '',
  platform: '',
  period: 'total',
  gameId: null,
  pageNo: 1,
  pageSize: 10
})

const activeTabLabel = computed(() => tabs.find(tab => tab.key === activeTab.value)?.label || '')
const displayTotal = computed(() => viewAll.value ? rawTotal.value : Math.min(rawTotal.value, 100))
const metricTitle = computed(() => {
  const map = {
    sales: '收入 / 变更',
    reviews: '好评率',
    expected: '愿望单',
    discounts: '促销收入 / 变更',
    platforms: '平台收入'
  }
  return map[activeTab.value] || '指标'
})

const chartTitles = computed(() => {
  const map = {
    sales: ['收入TOP10', '类型收入占比', '价格区间分布', '价格与收入散点', '榜单综合雷达', '收入结构矩形图'],
    reviews: ['好评/差评占比', '好评率趋势', '榜单好评率TOP', '评价数与好评率散点', '评价表现雷达', '评价结构矩形图'],
    expected: ['最受期待Top10', '类型愿望单占比', '愿望单榜单柱状图', '价格与愿望单散点', '期待表现雷达', '期待结构矩形图'],
    discounts: ['促销收入TOP10', '折扣力度分布', '类型促销收入', '折扣与收入散点', '促销表现雷达', '促销结构矩形图'],
    platforms: ['平台销售收入', '平台游戏数量占比', '平台销量', '游戏数与收入散点', '平台表现雷达', '平台结构矩形图']
  }
  return map[activeTab.value] || []
})

onMounted(() => {
  loadOptions()
  loadRankings()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})

const setChartRef = index => el => {
  if (el) chartRefs.value[index] = el
}

const loadOptions = () => {
  getRequest('/game/types', { silentError: true }).then(res => {
    gameTypes.value = res.data || []
  }).catch(() => {})
  getRequest('/ranking/platform-options', { silentError: true }).then(res => {
    platforms.value = res.data || []
  }).catch(() => {})
  getRequest('/game/listAll?limit=300', { silentError: true }).then(res => {
    gameOptions.value = res.data || []
  }).catch(() => {})
}

const switchTab = key => {
  disposeCharts()
  chartRefs.value = []
  activeTab.value = key
  viewAll.value = false
  filters.value.pageNo = 1
  loadRankings()
}

const reload = () => {
  filters.value.pageNo = 1
  loadRankings()
}

const resetFilters = () => {
  viewAll.value = false
  filters.value = {
    type: '',
    platform: '',
    period: 'total',
    gameId: null,
    pageNo: 1,
    pageSize: 10
  }
  loadRankings()
}

const showAll = () => {
  viewAll.value = true
  filters.value.pageNo = 1
  loadRankings()
}

const backTop100 = () => {
  viewAll.value = false
  filters.value.pageNo = 1
  loadRankings()
}

const changePage = page => {
  filters.value.pageNo = page
  loadRankings()
}

const changeSize = size => {
  filters.value.pageSize = size
  filters.value.pageNo = 1
  loadRankings()
}

const loadRankings = () => {
  loading.value = true
  const currentRequest = ++rankingRequestSeq
  const endpointMap = {
    sales: '/ranking/sales',
    reviews: '/ranking/reviews',
    expected: '/ranking/expected',
    discounts: '/ranking/discounts',
    platforms: '/ranking/platforms'
  }
  const params = {
    type: filters.value.type || undefined,
    platform: activeTab.value === 'platforms' ? undefined : (filters.value.platform || undefined),
    period: filters.value.period,
    gameId: activeTab.value === 'reviews' ? filters.value.gameId || undefined : undefined,
    pageNo: filters.value.pageNo,
    pageSize: filters.value.pageSize
  }

  getRequest(endpointMap[activeTab.value], { params, silentError: true }).then(res => {
    if (currentRequest !== rankingRequestSeq) return
    const result = res.data || {}
    rawTotal.value = result.total || 0
    if (!viewAll.value && (filters.value.pageNo - 1) * filters.value.pageSize >= 100) {
      filters.value.pageNo = 1
      return loadRankings()
    }
    rows.value = result.list || []
    charts.value = result.charts || {}
    scheduleRenderCharts()
  }).catch(() => {
    if (currentRequest !== rankingRequestSeq) return
    rows.value = []
    rawTotal.value = 0
    charts.value = {}
    scheduleRenderCharts()
  }).finally(() => {
    if (currentRequest === rankingRequestSeq) loading.value = false
  })
}

const scheduleRenderCharts = () => {
  nextTick(() => {
    requestAnimationFrame(() => {
      renderCharts().then(() => requestAnimationFrame(resizeCharts))
    })
  })
}

const disposeCharts = () => {
  chartInstances.forEach(chart => chart && chart.dispose())
  chartInstances = []
}

const loadEcharts = async () => {
  if (!echartsRuntime) {
    echartsRuntime = await import('echarts')
  }
  return echartsRuntime
}

const renderCharts = async () => {
  const echarts = await loadEcharts()
  const options = buildChartOptions()
  chartRefs.value.forEach((el, index) => {
    if (!el) return
    const chart = chartInstances[index] || echarts.init(el)
    chartInstances[index] = chart
    chart.resize()
    chart.setOption(options[index] || emptyOption(), true)
  })
}

const buildChartOptions = () => {
  if (activeTab.value === 'sales') {
    const top = charts.value.topRevenue || []
    return [
      barOption(top, '收入', '#3b82f6'),
      pieOption(charts.value.typeRevenue || []),
      pieOption(charts.value.priceRanges || []),
      scatterOption(rows.value, 'finalPrice', 'revenue', '价格', '收入'),
      radarOption(['收入', '销量', '均价', '折扣', '游戏数'], radarValues(rows.value)),
      treemapOption(top)
    ]
  }
  if (activeTab.value === 'reviews') {
    const reviewTop = rows.value.slice(0, 10).map(row => ({ name: row.name, value: row.goodRate || 0 }))
    return [
      pieOption((charts.value.pie || []).map(item => ({ ...item, name: item.name === 'good' ? '好评' : '差评' }))),
      lineOption(charts.value.trend || [], 'month', 'goodRate', '好评率'),
      barOption(reviewTop, '好评率', '#22c55e'),
      scatterOption(rows.value, 'reviewCount', 'goodRate', '评价数', '好评率'),
      radarOption(['好评率', '评价数', '平均分', '好评数', '差评数'], reviewRadarValues(rows.value)),
      treemapOption(rows.value.slice(0, 12).map(row => ({ name: row.name, value: row.reviewCount || 0 })))
    ]
  }
  if (activeTab.value === 'expected') {
    const expectedTop = charts.value.topExpected || []
    return [
      horizontalBarOption(expectedTop, '愿望单'),
      pieOption(charts.value.typeWishlist || []),
      barOption(rows.value.slice(0, 10).map(row => ({ name: row.name, value: row.wishlistCount || 0 })), '愿望单', '#a855f7'),
      scatterOption(rows.value, 'finalPrice', 'wishlistCount', '价格', '愿望单'),
      radarOption(['愿望单', '价格', '折扣', '游戏数', '销量'], expectedRadarValues(rows.value)),
      treemapOption(expectedTop)
    ]
  }
  if (activeTab.value === 'discounts') {
    const top = charts.value.topRevenue || []
    return [
      barOption(top, '收入', '#f97316'),
      pieOption(objectToChartList(charts.value.discountRanges)),
      pieOption(charts.value.typeRevenue || []),
      scatterOption(rows.value, 'discount', 'revenue', '折扣', '收入'),
      radarOption(['促销收入', '销量', '折扣', '均价', '游戏数'], radarValues(rows.value)),
      treemapOption(top)
    ]
  }
  const platformData = charts.value.revenue || []
  return [
    barOption(platformData, '收入', '#14b8a6'),
    pieOption(platformData.map(item => ({ name: item.name, value: item.gameCount || 0 }))),
    barOption(platformData.map(item => ({ name: item.name, value: item.sales || 0 })), '销量', '#6366f1'),
    scatterOption(platformData, 'gameCount', 'value', '游戏数', '收入'),
    radarOption(['收入', '销量', '游戏数', '均收入', '平台数'], platformRadarValues(platformData)),
    treemapOption(platformData)
  ]
}

const resizeCharts = () => {
  chartInstances.forEach(chart => chart && chart.resize())
}

const emptyOption = () => ({
  title: { text: '暂无数据', left: 'center', top: 'center', textStyle: { color: '#9ca3af', fontSize: 14 } }
})

const compactAxisLabel = (value, max = 8) => {
  const text = String(value ?? '')
  return text.length > max ? `${text.slice(0, max)}...` : text
}

const barOption = (data, name, color) => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 24, top: 28, bottom: 74, containLabel: true },
  xAxis: {
    type: 'category',
    data: data.map(item => item.name),
    axisLabel: {
      interval: 0,
      rotate: 32,
      margin: 14,
      width: 78,
      overflow: 'truncate',
      formatter: value => compactAxisLabel(value, 8)
    }
  },
  yAxis: { type: 'value' },
  series: [{ name, type: 'bar', data: data.map(item => Number(item.value) || 0), itemStyle: { color, borderRadius: [6, 6, 0, 0] } }]
})

const horizontalBarOption = (data, name) => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 24, right: 24, top: 20, bottom: 20, containLabel: true },
  xAxis: { type: 'value' },
  yAxis: {
    type: 'category',
    data: [...data].reverse().map(item => item.name),
    axisLabel: {
      width: 150,
      overflow: 'truncate',
      formatter: value => compactAxisLabel(value, 12)
    }
  },
  series: [{ name, type: 'bar', data: [...data].reverse().map(item => Number(item.value) || 0), itemStyle: { color: '#3b82f6', borderRadius: [0, 6, 6, 0] } }]
})

const pieOption = data => ({
  tooltip: { trigger: 'item' },
  legend: {
    type: 'scroll',
    bottom: 0,
    itemWidth: 10,
    itemHeight: 10,
    pageIconSize: 10,
    textStyle: { overflow: 'truncate', width: 90 }
  },
  series: [{
    type: 'pie',
    radius: ['34%', '62%'],
    center: ['50%', '42%'],
    avoidLabelOverlap: true,
    minShowLabelAngle: 8,
    label: {
      show: true,
      formatter: '{b}',
      overflow: 'truncate',
      width: 84
    },
    labelLine: { show: true, length: 10, length2: 8 },
    labelLayout: { hideOverlap: true },
    data: data.map(item => ({ name: item.name, value: Number(item.value) || 0 }))
  }]
})

const lineOption = (data, xKey, yKey, name) => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 24, top: 28, bottom: 36 },
  xAxis: { type: 'category', data: data.map(item => item[xKey]) },
  yAxis: { type: 'value', max: 100 },
  series: [{ name, type: 'line', smooth: true, data: data.map(item => Number(item[yKey]) || 0), areaStyle: {}, itemStyle: { color: '#22c55e' } }]
})

const scatterOption = (data, xKey, yKey, xName, yName) => ({
  tooltip: {
    trigger: 'item',
    formatter: params => `${params.data[2]}<br>${xName}: ${params.data[0]}<br>${yName}: ${params.data[1]}`
  },
  grid: { left: 52, right: 24, top: 28, bottom: 44 },
  xAxis: { type: 'value', name: xName },
  yAxis: { type: 'value', name: yName },
  series: [{
    type: 'scatter',
    symbolSize: 13,
    data: data.slice(0, 60).map(row => [Number(row[xKey]) || 0, Number(row[yKey]) || 0, row.name || row.platform]),
    itemStyle: { color: '#f97316' }
  }]
})

const radarOption = (labels, values) => ({
  tooltip: {},
  radar: {
    radius: '62%',
    indicator: labels.map(label => ({ name: label, max: 100 }))
  },
  series: [{
    type: 'radar',
    data: [{ value: values, name: '综合表现', areaStyle: { opacity: 0.18 } }]
  }]
})

const treemapOption = data => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'treemap',
    roam: false,
    breadcrumb: { show: false },
    label: { show: true, formatter: '{b}' },
    data: data.map(item => ({ name: item.name, value: Number(item.value) || 0 }))
  }]
})

const objectToChartList = (value = {}) => Object.entries(value).map(([name, itemValue]) => ({ name, value: itemValue }))
const sum = (list, key) => list.reduce((total, item) => total + (Number(item[key]) || 0), 0)
const avg = (list, key) => list.length ? sum(list, key) / list.length : 0
const scale = (value, max) => Math.max(0, Math.min(100, max ? (value / max) * 100 : 0))

const radarValues = list => {
  const revenue = sum(list, 'revenue')
  const sales = sum(list, 'sales')
  const price = avg(list, 'finalPrice')
  const discount = avg(list, 'discount')
  return [scale(revenue, 10000), scale(sales, 500), scale(price, 500), scale(discount, 100), scale(list.length, 100)]
}

const reviewRadarValues = list => [
  scale(avg(list, 'goodRate'), 100),
  scale(avg(list, 'reviewCount'), 300),
  scale(avg(list, 'avgRating'), 10),
  scale(avg(list, 'goodCount'), 200),
  scale(avg(list, 'badCount'), 200)
]

const expectedRadarValues = list => [
  scale(avg(list, 'wishlistCount'), 300),
  scale(avg(list, 'finalPrice'), 500),
  scale(avg(list, 'discount'), 100),
  scale(list.length, 100),
  scale(avg(list, 'sales'), 300)
]

const platformRadarValues = list => [
  scale(avg(list, 'value'), 10000),
  scale(avg(list, 'sales'), 500),
  scale(avg(list, 'gameCount'), 100),
  scale(avg(list, 'value') / Math.max(avg(list, 'gameCount'), 1), 1000),
  scale(list.length, 20)
]

const displayTags = row => {
  const tags = [...(row.typeList || []), ...(row.platformList || [])]
  return tags.slice(0, 8)
}

const formatMetric = row => {
  if (activeTab.value === 'reviews') return `${row.goodRate || 0}%`
  if (activeTab.value === 'expected') return `${row.wishlistCount || 0}`
  if (activeTab.value === 'platforms') return `￥${Number(row.revenue || row.value || 0).toFixed(2)}`
  return `￥${Number(row.revenue || 0).toFixed(2)}`
}

const rankChangeText = row => {
  if (row.newListed) return '新上榜'
  if (row.rankChange === null || row.rankChange === undefined || row.rankChange === 0) return '持平'
  return row.rankChange > 0 ? `▲ ${row.rankChange}` : `▼ ${Math.abs(row.rankChange)}`
}

const rankChangeClass = row => {
  if (row.newListed) return 'new'
  if ((row.rankChange || 0) > 0) return 'up'
  if ((row.rankChange || 0) < 0) return 'down'
  return 'flat'
}

const rankChangeTip = row => {
  if (row.newListed) return '上一周期未进入榜单'
  if (!row.previousRank) return '总榜暂无上一周期对比'
  return `上一周期第 ${row.previousRank} 名，当前第 ${row.rank} 名`
}

const rowKey = row => row.id || row.platform
const shortPlatform = value => String(value || '?').slice(0, 2).toUpperCase()
const goGame = id => router.push('/gameDetail?id=' + id)
</script>

<style lang="less" scoped>
.rankings-page {
  min-height: 100%;
  padding: 18px;
  box-sizing: border-box;
  background: #f6f7fb;
  overflow-y: auto;
}

.rankings-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 22px;
  border-radius: 8px;
  background: #111827;
  color: #fff;

  h2 {
    margin: 0 0 8px;
    font-size: 26px;
  }

  p {
    margin: 0;
    color: #cbd5e1;
  }
}

.head-stats {
  display: flex;
  gap: 12px;

  div {
    min-width: 120px;
    padding: 12px;
    border-radius: 8px;
    background: rgba(255, 255, 255, 0.1);
  }

  strong,
  span {
    display: block;
  }

  strong {
    font-size: 22px;
  }

  span {
    color: #cbd5e1;
    margin-top: 4px;
  }
}

.rank-tabs,
.rank-filters,
.rank-list-panel,
.chart-section {
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.rank-tabs {
  display: flex;
  gap: 8px;
  padding: 10px;
  margin-top: 16px;

  button {
    border: 0;
    border-radius: 6px;
    padding: 10px 16px;
    background: transparent;
    cursor: pointer;
    color: #4b5563;

    &.active {
      background: #111827;
      color: #fff;
      font-weight: 700;
    }
  }
}

.rank-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-top: 14px;
}

.rank-list-panel {
  margin-top: 16px;
  padding: 18px;
}

.rank-section-title,
.chart-section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;

  h3 {
    margin: 0 0 6px;
    color: #111827;
    font-size: 20px;
  }

  p,
  span {
    margin: 0;
    color: #8a92a3;
    font-size: 13px;
  }
}

.rank-list-head,
.rank-row {
  display: grid;
  grid-template-columns: 58px 108px minmax(320px, 1.8fr) minmax(120px, 0.7fr) minmax(140px, 150px);
  align-items: center;
  gap: 12px;
}

.rank-list-head {
  margin-top: 16px;
  padding: 10px 4px;
  color: #6b7280;
  font-size: 13px;
  border-top: 1px solid #eef2f7;
  border-bottom: 1px solid #eef2f7;

  span:nth-child(2) {
    grid-column: 2 / 4;
  }

  span:last-child {
    grid-column: 5;
    text-align: right;
    white-space: nowrap;
  }
}

.rank-row {
  min-height: 88px;
  border-bottom: 1px solid #eef2f7;

  &.clickable {
    cursor: pointer;
  }

  &:hover {
    background: #f8fafc;
  }
}

.rank-number {
  font-size: 24px;
  font-weight: 800;
  color: #9ca3af;
  text-align: center;
}

.rank-row:nth-child(1) .rank-number { color: #f97316; }
.rank-row:nth-child(2) .rank-number { color: #a855f7; }
.rank-row:nth-child(3) .rank-number { color: #3b82f6; }

.rank-cover {
  width: 108px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  background: #eef2f7;
}

.rank-platform {
  width: 68px;
  height: 68px;
  border-radius: 8px;
  background: #111827;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.rank-info {
  min-width: 0;
}

.rank-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 800;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
  color: #8a92a3;
  font-size: 13px;
}

.rank-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  span {
    padding: 3px 9px;
    border-radius: 999px;
    background: #eef6ff;
    color: #2563eb;
    font-size: 12px;
  }
}

.rank-metric {
  text-align: right;
  min-width: 0;

  strong,
  span,
  small {
    display: block;
  }

  strong {
    color: #ef4444;
    font-size: 18px;
    white-space: nowrap;
  }

  small {
    margin-top: 5px;
    color: #8a92a3;
    font-size: 12px;
  }
}

.rank-change {
  margin-top: 6px;
  font-size: 13px;

  &.up {
    color: #22c55e;
  }

  &.down {
    color: #f59e0b;
  }

  &.new {
    color: #0284c7;
  }

  &.flat {
    color: #9ca3af;
  }
}

.rank-pagination {
  margin-top: 16px;
  justify-content: flex-end;
  flex-wrap: wrap;
  row-gap: 8px;
}

.chart-section {
  margin-top: 18px;
  padding: 18px;
}

.rank-charts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.chart-card {
  min-width: 0;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  padding: 14px;
  background: #fff;

  &.wide {
    grid-column: 1 / -1;
  }

  h4 {
    margin: 0 0 10px;
    color: #111827;
    font-size: 16px;
  }
}

.chart {
  height: 300px;
}

.chart-card.wide .chart {
  height: 340px;
}

@media (max-width: 900px) {
  .rank-list-head {
    display: none;
  }

  .rank-row {
    grid-template-columns: 52px 96px minmax(0, 1fr) 150px;
  }

  .rank-cover {
    width: 96px;
    height: 58px;
  }

  .rank-tags {
    grid-column: 3 / 5;
    margin-bottom: 10px;
  }
}

@media (max-width: 760px) {
  .rankings-head,
  .head-stats,
  .rank-section-title,
  .chart-section-head {
    flex-direction: column;
  }

  .rank-row {
    grid-template-columns: 42px 76px minmax(0, 1fr);
  }

  .rank-cover {
    width: 76px;
    height: 48px;
  }

  .rank-tags,
  .rank-metric {
    grid-column: 3;
    text-align: left;
  }

  .rank-charts {
    grid-template-columns: 1fr;
  }
}
</style>
