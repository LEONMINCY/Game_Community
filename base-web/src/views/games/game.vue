<!-- 文件说明：views/games/game.vue，游戏详情页，展示媒体预览、价格、购买、愿望单、购物车、统计和评价。 -->
<template>
  <div class="mainpage">
    <div class="game-detail-container">
      <div class="backWrapper">
        <span class="back" @click="router.back()">&lt; 返回</span>
      </div>

      <div class="game-header">
        <div class="game-title-section">
          <div>
            <div class="game-title">{{ gameData.title }}</div>
            <div class="game-tags" v-if="gameData.tags.length">
              <span class="tag" v-for="tag in gameData.tags" :key="tag">{{ tag }}</span>
            </div>
            <div class="subtitle">
              <span v-if="gameData.developer">{{ gameData.developer }}</span>
              <span v-if="gameData.developer && gameData.releaseDate" class="divider">·</span>
              <span v-if="gameData.releaseDate">{{ gameData.releaseDate }}</span>
            </div>
          </div>

          <div class="game-score">
            <div class="score">{{ gameData.rating }}</div>
            <span class="review-count">{{ gameData.reviewCount }} 人评价</span>
            <span class="review-type" :class="{ empty: !gameData.reviewCount }">{{ gameData.reviewType }}</span>
          </div>
        </div>
      </div>

      <div class="media-section">
        <div class="main-media">
          <video
            v-if="selectedMedia.type === 'video'"
            :src="selectedMedia.url"
            controls
            class="media-content"
          />
          <img
            v-else-if="selectedMedia.url"
            :src="selectedMedia.url"
            :alt="gameData.title"
            class="media-content"
          />
          <div v-else class="empty-media">暂无媒体内容</div>
        </div>
        <div class="media-thumb-shell" v-if="gameData.media.length">
          <div class="media-thumbnails" ref="thumbnailRef">
            <div
              v-for="(media, index) in gameData.media"
              :key="index"
              @click="selectMedia(media)"
              class="thumbnail"
              :class="{ active: selectedMedia === media }"
            >
              <video
                v-if="media.type === 'video'"
                :src="media.url"
                muted
                preload="metadata"
                class="thumb-video"
              />
              <img v-else :src="media.thumbnail" :alt="'预览图' + (index + 1)">
              <span v-if="media.type === 'video'" class="video-mark">▶</span>
            </div>
          </div>
        </div>
      </div>

      <div class="purchase-section">
        <div class="price-info">
          <div class="current-price">
            <span class="price" :class="{ free: isFreeGame }">{{ displayPrice }}</span>
            <span class="original-price" v-if="gameData.discount > 0 && !isFreeGame">¥{{ gameData.originalPrice }}</span>
            <span class="discount-tag" v-if="gameData.discount > 0 && !isFreeGame">-{{ gameData.discount }}%</span>
            <span class="price-mark" v-if="priceMarkText(gameData.priceMark)">{{ priceMarkText(gameData.priceMark) }}</span>
          </div>
        </div>
        <div class="purchase-buttons">
          <span v-if="gameData.isBuy" class="owned-button">已购买</span>
          <button v-else class="buy-button" @click="handlePurchase">{{ isFreeGame ? '免费开玩' : '立即购买' }}</button>
          <button v-if="!gameData.isBuy" class="wish-button" :class="{ active: inWishlist }" @click="toggleWishlist">
            {{ inWishlist ? '取消愿望单' : '加入愿望单' }}
          </button>
          <button v-if="!gameData.isBuy" class="cart-button" :class="{ active: inCart }" @click="toggleCart">
            {{ inCart ? '移出购物车' : '加入购物车' }}
          </button>
        </div>
      </div>

      <div class="info-tabs">
        <div class="tab-headers">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            @click="currentTab = tab.key"
            :class="{ active: currentTab === tab.key }"
            class="tab-header"
          >
            {{ tab.label }}
          </div>
        </div>

        <div v-if="currentTab === 'description'" class="tab-content">
          <div class="description" v-html="gameData.description"></div>
        </div>

        <div v-if="currentTab === 'requirements'" class="tab-content">
          <div class="requirements">
            <h3>最低配置</h3>
            <ul>
              <li v-for="(req, key) in gameData.requirements.minimum" :key="key">
                <span class="req-label">{{ key }}:</span> {{ req }}
              </li>
            </ul>
            <h3>推荐配置</h3>
            <ul>
              <li v-for="(req, key) in gameData.requirements.recommended" :key="key">
                <span class="req-label">{{ key }}:</span> {{ req }}
              </li>
            </ul>
          </div>
        </div>

        <div v-if="currentTab === 'stats'" class="tab-content stats-content">
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-value">{{ gameStats.ratingCount }}</div>
              <div class="stat-label">评分人数</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ gameStats.goodRate }}%</div>
              <div class="stat-label">好评率</div>
            </div>
            <div class="stat-card">
              <div class="stat-value">{{ gameStats.totalSales }}</div>
              <div class="stat-label">总销量</div>
            </div>
          </div>

          <div class="visual-grid">
            <div class="chart-panel">
              <h3>好评占比</h3>
              <div class="pie-chart" :style="goodRatePieStyle">
                <div class="pie-center">
                  <strong>{{ gameStats.goodRate }}%</strong>
                  <span>好评率</span>
                </div>
              </div>
              <div class="pie-legend">
                <span><i class="good"></i>好评</span>
                <span><i class="bad"></i>差评</span>
              </div>
            </div>

            <div class="chart-panel">
              <h3>评分分布</h3>
              <div class="score-bars">
                <div v-for="item in ratingDistribution" :key="item.score" class="score-row">
                  <span>{{ item.score }}分</span>
                  <div class="score-track">
                    <div class="score-fill" :style="{ width: scoreBarWidth(item.count) }"></div>
                  </div>
                  <em>{{ item.count }}</em>
                </div>
              </div>
            </div>
          </div>

          <div class="chart-section">
            <div class="chart-panel">
              <h3>评分趋势</h3>
              <div v-if="ratingPolylinePoints" class="line-chart">
                <svg viewBox="0 0 320 160" preserveAspectRatio="none">
                  <polyline class="line-grid" points="0,40 320,40" />
                  <polyline class="line-grid" points="0,80 320,80" />
                  <polyline class="line-grid" points="0,120 320,120" />
                  <polyline class="line" :points="ratingPolylinePoints" />
                </svg>
                <div class="line-labels">
                  <span v-for="item in gameStats.ratingTrend" :key="item.month">{{ item.month }}</span>
                </div>
              </div>
              <div v-else class="empty-chart">暂无评分趋势</div>
            </div>

            <div class="chart-panel">
              <h3>月销量趋势</h3>
              <div v-if="gameStats.monthlySalesTrend.length" class="bar-chart">
                <div v-for="item in gameStats.monthlySalesTrend" :key="item.month" class="bar-item">
                  <div class="bar-value">{{ item.sales }}</div>
                  <div class="bar-track">
                    <div class="bar-fill sales" :style="{ height: salesBarHeight(item.sales) }"></div>
                  </div>
                  <div class="bar-label">{{ item.month }}</div>
                </div>
              </div>
              <div v-else class="empty-chart">暂无销量趋势</div>
            </div>
          </div>

          <div class="chart-panel heatmap-panel">
            <h3>评分热力图</h3>
            <div v-if="gameStats.ratingTrend.length" class="heatmap">
              <div
                v-for="item in gameStats.ratingTrend"
                :key="item.month"
                class="heat-cell"
                :style="{ opacity: heatmapOpacity(item.count) }"
              >
                <strong>{{ item.count }}</strong>
                <span>{{ item.month }}</span>
              </div>
            </div>
            <div v-else class="empty-chart">暂无热力数据</div>
          </div>
        </div>

        <div v-if="currentTab === 'reviews'" ref="reviewSectionRef" class="tab-content">
          <div class="review-editor" v-if="gameData.isBuy">
            <div class="review-editor-row">
              <span class="review-editor-label">评分</span>
              <el-rate v-model="userRating" :max="10" show-score />
            </div>
            <div class="review-editor-row">
              <span class="review-editor-label">推荐</span>
              <div class="review-choice-group">
                <button
                  type="button"
                  class="review-choice recommend"
                  :class="{ active: reviewRecommend === true }"
                  @click="reviewRecommend = reviewRecommend === true ? null : true"
                >
                  推荐
                </button>
                <button
                  type="button"
                  class="review-choice not-recommend"
                  :class="{ active: reviewRecommend === false }"
                  @click="reviewRecommend = reviewRecommend === false ? null : false"
                >
                  不推荐
                </button>
                <span class="review-choice-tip">不选择则按中评发布</span>
              </div>
            </div>
            <el-input
              v-model="reviewContent"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="写下你的评价内容"
            />
            <div class="review-actions">
              <el-button type="primary" @click="submitRating">{{ hasRated ? '更新评价' : '发布评价' }}</el-button>
            </div>
          </div>
          <div class="review-login-tip" v-else>购买游戏后可以发布评价</div>

          <div class="review-filter">
            <el-radio-group v-model="reviewFilter" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="good">推荐</el-radio-button>
              <el-radio-button label="bad">不推荐</el-radio-button>
            </el-radio-group>
            <div class="review-date-filter">
              <el-select v-model="reviewDateMode" size="small" style="width: 112px" @change="handleReviewDateModeChange">
                <el-option label="全部时间" value="all" />
                <el-option label="近2周" value="recent2weeks" />
                <el-option label="按月筛选" value="month" />
                <el-option label="按年筛选" value="year" />
              </el-select>
              <el-date-picker
                v-if="reviewDateMode === 'month'"
                v-model="reviewMonth"
                type="month"
                size="small"
                placeholder="选择月份"
                style="width: 140px"
              />
              <el-date-picker
                v-if="reviewDateMode === 'year'"
                v-model="reviewYear"
                type="year"
                size="small"
                placeholder="选择年份"
                style="width: 120px"
              />
            </div>
          </div>

          <div class="rating-list-wrapper">
            <template v-if="filteredRatingList.length > 0">
              <div class="rating-item" v-for="item in filteredRatingList" :key="item.id">
                <div class="rating-header">
                  <div class="user-info">
                    <el-avatar :size="32" :src="item.userAvatar" />
                    <span class="username">{{ item.nickname }}</span>
                    <span class="recommend-tag" :class="recommendTagClass(item.recommend)">
                      {{ recommendText(item.recommend) }}
                    </span>
                  </div>
                  <div class="rating-score">{{ item.rating }} 分</div>
                </div>
                <div class="rating-content" v-if="item.content">{{ item.content }}</div>
                <div class="rating-time">{{ item.createTime }}</div>
              </div>
            </template>
            <div v-else class="no-rating">暂无评价</div>
          </div>
          <el-pagination
            v-if="ratingPage.total > 0"
            class="rating-pagination"
            :current-page="ratingPage.pageNo"
            :page-size="ratingPage.pageSize"
            :page-sizes="ratingPageSizes"
            :total="ratingPage.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleRatingSizeChange"
            @current-change="handleRatingPageChange"
          />
        </div>
      </div>
    </div>

    <AlipayQrDialog
      v-model="payDialogVisible"
      :payment="currentPayment"
      @paid="handlePaymentPaid"
    />
  </div>
</template>

<script setup>
import { ref, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRequest, postRequest } from '../../utils/http'
import { replaceURL } from '@/utils/tools'
import AlipayQrDialog from '@/components/AlipayQrDialog.vue'
import { hasToken, requireLogin } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const currentTab = ref('description')
const selectedMedia = ref({})
const thumbnailRef = ref(null)
const reviewSectionRef = ref(null)
const payDialogVisible = ref(false)
const currentPayment = ref(null)
const userRating = ref(0)
const reviewContent = ref('')
const reviewRecommend = ref(null)
const reviewFilter = ref('all')
const reviewDateMode = ref('all')
const reviewMonth = ref(null)
const reviewYear = ref(null)
const hasRated = ref(false)
const ratingList = ref([])
const ratingPageSizes = [20, 50, 100]
const ratingPage = ref({
  pageNo: 1,
  pageSize: 50,
  total: 0
})
const inWishlist = ref(false)
const inCart = ref(false)
const gameStats = ref({
  ratingCount: 0,
  goodRate: 0,
  reviewType: '暂无用户评测',
  totalSales: 0,
  ratingTrend: [],
  monthlySalesTrend: []
})

const tabs = [
  { key: 'description', label: '游戏简介' },
  { key: 'requirements', label: '系统需求' },
  { key: 'stats', label: '数据统计' },
  { key: 'reviews', label: '玩家评价' }
]

const gameData = ref({
  id: null,
  title: '',
  developer: '',
  releaseDate: '',
  price: null,
  originalPrice: null,
  discount: 0,
  priceMark: '',
  rating: '0.0',
  reviewCount: 0,
  reviewType: '暂无用户评测',
  description: '',
  media: [],
  requirements: {
    minimum: {},
    recommended: {}
  },
  tags: [],
  isBuy: false
})

const maxMonthlySales = computed(() => {
  return Math.max(1, ...gameStats.value.monthlySalesTrend.map(item => Number(item.sales) || 0))
})

const maxRatingTrendCount = computed(() => {
  return Math.max(1, ...gameStats.value.ratingTrend.map(item => Number(item.count) || 0))
})

const ratingDistribution = computed(() => {
  return Array.from({ length: 10 }, (_, index) => {
    const score = 10 - index
    return {
      score,
      count: ratingList.value.filter(item => Number(item.rating) === score).length
    }
  })
})

const maxRatingDistributionCount = computed(() => {
  return Math.max(1, ...ratingDistribution.value.map(item => item.count))
})

const filteredRatingList = computed(() => {
  // 评价列表已经由服务端按好评/差评分页筛选，这里只负责渲染当前页。
  return ratingList.value
})

const padDateNumber = (value) => String(value).padStart(2, '0')

const formatDateTimeParam = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  const year = date.getFullYear()
  const month = padDateNumber(date.getMonth() + 1)
  const day = padDateNumber(date.getDate())
  const hour = padDateNumber(date.getHours())
  const minute = padDateNumber(date.getMinutes())
  const second = padDateNumber(date.getSeconds())
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

const getRatingDateRange = () => {
  if (reviewDateMode.value === 'recent2weeks') {
    const end = new Date()
    end.setDate(end.getDate() + 1)
    end.setHours(0, 0, 0, 0)
    const start = new Date(end)
    start.setDate(start.getDate() - 14)
    start.setHours(0, 0, 0, 0)
    return { startTime: formatDateTimeParam(start), endTime: formatDateTimeParam(end) }
  }
  if (reviewDateMode.value === 'month' && reviewMonth.value) {
    const selected = new Date(reviewMonth.value)
    const start = new Date(selected.getFullYear(), selected.getMonth(), 1, 0, 0, 0)
    const end = new Date(selected.getFullYear(), selected.getMonth() + 1, 1, 0, 0, 0)
    return { startTime: formatDateTimeParam(start), endTime: formatDateTimeParam(end) }
  }
  if (reviewDateMode.value === 'year' && reviewYear.value) {
    const selected = new Date(reviewYear.value)
    const start = new Date(selected.getFullYear(), 0, 1, 0, 0, 0)
    const end = new Date(selected.getFullYear() + 1, 0, 1, 0, 0, 0)
    return { startTime: formatDateTimeParam(start), endTime: formatDateTimeParam(end) }
  }
  return {}
}

const scrollToReviewSection = () => {
  nextTick(() => {
    reviewSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const currentPrice = computed(() => {
  const value = gameData.value.price
  if (value === null || value === undefined || value === '') {
    return null
  }
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
})

const isFreeGame = computed(() => currentPrice.value !== null && currentPrice.value <= 0)

const displayPrice = computed(() => {
  if (currentPrice.value === null) {
    return '加载中'
  }
  return isFreeGame.value ? '免费开玩' : `¥${currentPrice.value}`
})

const goodRatePieStyle = computed(() => ({
  background: `conic-gradient(#67c23a 0 ${Number(gameStats.value.goodRate) || 0}%, #f56c6c ${Number(gameStats.value.goodRate) || 0}% 100%)`
}))

const ratingPolylinePoints = computed(() => {
  const list = gameStats.value.ratingTrend
  if (!list.length) return ''
  const maxIndex = Math.max(1, list.length - 1)
  return list.map((item, index) => {
    const x = (index / maxIndex) * 320
    const y = 150 - (Math.max(0, Math.min(Number(item.avgRating) || 0, 10)) / 10) * 140
    return `${x},${y}`
  }).join(' ')
})

const selectMedia = (media) => {
  selectedMedia.value = media
}

const safeJsonParse = (value, fallback) => {
  if (!value) return fallback
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return fallback
  }
}

const splitTypes = (type) => {
  if (!type) return []
  return String(type)
    .split(/[,，、/|]+/)
    .map(item => item.trim())
    .filter(Boolean)
}

const formatDate = (value) => {
  if (!value) return ''
  return String(value).slice(0, 10)
}

const priceMarkText = (value) => {
  if (value === 'historical_low') return '史低'
  if (value === 'tie_historical_low') return '平史低'
  return ''
}

const ratingBarHeight = (rating) => {
  const value = Math.max(0, Math.min(Number(rating) || 0, 10))
  return `${Math.max(8, value * 10)}%`
}

const salesBarHeight = (sales) => {
  const value = Number(sales) || 0
  return `${Math.max(8, (value / maxMonthlySales.value) * 100)}%`
}

const scoreBarWidth = (count) => {
  return `${Math.max(4, ((Number(count) || 0) / maxRatingDistributionCount.value) * 100)}%`
}

const heatmapOpacity = (count) => {
  return Math.max(0.18, (Number(count) || 0) / maxRatingTrendCount.value)
}

const isGameDetailRoute = () => route.path === '/gameDetail'

const normalizeGame = (item, avgRating, ratingCount) => {
  const screenshots = safeJsonParse(item.screenshots, [])
  const videos = safeJsonParse(item.video, [])
  const media = []
  const finalPrice = item.finalPrice ?? item.discountPrice ?? item.price ?? null

  videos.forEach(url => {
    media.push({
      type: 'video',
      url: replaceURL(url),
      thumbnail: ''
    })
  })
  screenshots.forEach(url => {
    media.push({
      type: 'image',
      url: replaceURL(url),
      thumbnail: replaceURL(url)
    })
  })

  return {
    ...item,
    title: item.name,
    releaseDate: formatDate(item.releaseDate),
    reviewCount: item.reviewCount ?? ratingCount ?? 0,
    reviewType: item.reviewType || calcReviewType(item.reviewCount ?? ratingCount ?? 0, item.goodRate),
    rating: (Number(avgRating) || 0).toFixed(1),
    requirements: safeJsonParse(item.systemRequirements, { minimum: {}, recommended: {} }),
    tags: [...splitTypes(item.type), ...splitTypes(item.platforms)],
    price: finalPrice,
    originalPrice: item.discount > 0 ? item.price : null,
    discount: item.discount || 0,
    priceMark: item.priceMark || '',
    isBuy: item.isBuy,
    media
  }
}

const refreshRatingSummary = async () => {
  const [ratingRes, countRes, statsRes] = await Promise.all([
    getRequest(`/gameRating/average-rating/${gameData.value.id}`),
    getRequest(`/gameRating/rating-count/${gameData.value.id}`),
    getRequest(`/game/stats/${gameData.value.id}`)
  ])

  if (ratingRes.code === 200) {
    gameData.value.rating = (Number(ratingRes.data) || 0).toFixed(1)
  }
  if (countRes.code === 200) {
    gameData.value.reviewCount = countRes.data || 0
  }
  if (statsRes.code === 200) {
    gameStats.value = {
      ratingCount: statsRes.data?.ratingCount || 0,
      goodRate: statsRes.data?.goodRate || 0,
      reviewType: statsRes.data?.reviewType || calcReviewType(statsRes.data?.ratingCount || 0, statsRes.data?.goodRate || 0),
      totalSales: statsRes.data?.totalSales || 0,
      ratingTrend: statsRes.data?.ratingTrend || [],
      monthlySalesTrend: statsRes.data?.monthlySalesTrend || []
    }
    gameData.value.reviewType = gameStats.value.reviewType
  }
}

const handlePurchase = () => {
  if (!requireLogin(route.fullPath)) return
  if (!gameData.value.id || currentPrice.value === null) {
    ElMessage.warning('游戏信息还没有加载完成，请稍后再试')
    return
  }
  postRequest('/order/add', {
    gameId: gameData.value.id,
    totalPrice: currentPrice.value
  }).then(res => {
    if (res.code === 200) {
      if (res.data?.free === 'true' || res.data?.paid === 'true' || res.data?.status === '购买成功') {
        gameData.value.isBuy = true
        inWishlist.value = false
        inCart.value = false
        ElMessage.success('购买成功，已加入游戏库')
        return
      }
      currentPayment.value = res.data
      payDialogVisible.value = true
    } else {
      ElMessage.error(res.msg || '创建订单失败')
    }
  }).catch(err => {
    ElMessage.error('支付发起失败')
    console.error(err)
  })
}

const handlePaymentPaid = (orderNo) => {
  gameData.value.isBuy = true
  router.push('/paymentResults?out_trade_no=' + encodeURIComponent(orderNo))
}

const getCommerceStatus = (id) => {
  if (!hasToken()) {
    inWishlist.value = false
    inCart.value = false
    return
  }
  Promise.all([
    getRequest(`/game-user/wishlist/status/${id}`),
    getRequest(`/game-user/cart/status/${id}`)
  ]).then(([wishlistRes, cartRes]) => {
    if (wishlistRes.code === 200) {
      inWishlist.value = !!wishlistRes.data?.active
    }
    if (cartRes.code === 200) {
      inCart.value = !!cartRes.data?.active
    }
  }).catch(err => {
    console.error('获取愿望单/购物车状态失败:', err)
  })
}

const toggleWishlist = () => {
  if (!gameData.value.id) return
  if (!requireLogin(route.fullPath)) return
  postRequest(`/game-user/wishlist/toggle/${gameData.value.id}`, {}).then(res => {
    if (res.code === 200) {
      inWishlist.value = !!res.data?.active
      ElMessage.success(inWishlist.value ? '已加入愿望单' : '已取消愿望单')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const toggleCart = () => {
  if (!gameData.value.id) return
  if (!requireLogin(route.fullPath)) return
  postRequest(`/game-user/cart/toggle/${gameData.value.id}`, {}).then(res => {
    if (res.code === 200) {
      inCart.value = !!res.data?.active
      ElMessage.success(inCart.value ? '已加入购物车' : '已移出购物车')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const submitRating = () => {
  if (!requireLogin(route.fullPath)) return
  if (!gameData.value.isBuy) {
    ElMessage.warning('购买游戏后才能评价')
    return
  }
  if (!userRating.value) {
    ElMessage.warning('请先选择评分')
    return
  }

  postRequest('/gameRating/rate', {
    gameId: gameData.value.id,
    rating: userRating.value,
    content: reviewContent.value,
    recommend: reviewRecommend.value
  }).then(async res => {
    if (res.code === 200) {
      ElMessage.success(hasRated.value ? '评价已更新' : '评价已发布')
      hasRated.value = true
      await refreshRatingSummary()
      ratingPage.value.pageNo = 1
      getRatingList(gameData.value.id)
    } else {
      ElMessage.error(res.msg || '评价失败')
      getUserRating()
    }
  }).catch(err => {
    console.error('评价失败:', err)
    ElMessage.error('评价失败，请稍后重试')
    getUserRating()
  })
}

const getUserRating = () => {
  if (!hasToken() || !gameData.value.isBuy) return

  getRequest(`/gameRating/user-rating/${gameData.value.id}`).then(res => {
    if (res.code === 200 && res.data) {
      userRating.value = res.data.rating
      reviewContent.value = res.data.content || ''
      reviewRecommend.value = res.data.recommend === true ? true : (res.data.recommend === false ? false : null)
      hasRated.value = true
    } else {
      userRating.value = 0
      reviewContent.value = ''
      reviewRecommend.value = null
      reviewFilter.value = 'all'
      hasRated.value = false
    }
  }).catch(err => {
    console.error('获取用户评价失败:', err)
  })
}

const getRatingList = (id) => {
  const params = {
    pageNo: ratingPage.value.pageNo,
    pageSize: ratingPage.value.pageSize
  }
  if (reviewFilter.value !== 'all') {
    params.recommend = reviewFilter.value === 'good'
  }
  const dateRange = getRatingDateRange()
  if (dateRange.startTime) {
    params.startTime = dateRange.startTime
  }
  if (dateRange.endTime) {
    params.endTime = dateRange.endTime
  }
  getRequest(`/gameRating/list/${id}`, { params }).then(res => {
    if (res.code === 200) {
      const pageData = Array.isArray(res.data)
        ? { list: res.data, total: res.data.length }
        : (res.data || { list: [], total: 0 })
      ratingPage.value.total = Number(pageData.total) || 0
      ratingList.value = (pageData.list || []).map(item => ({
        ...item,
        recommend: item.recommend === true ? true : (item.recommend === false ? false : null),
        userAvatar: item.userAvatar ? replaceURL(item.userAvatar) : ''
      }))
    }
  }).catch(err => {
    console.error('获取评价列表失败:', err)
  })
}

const handleReviewDateModeChange = () => {
  reviewMonth.value = null
  reviewYear.value = null
}

const handleRatingPageChange = (page) => {
  ratingPage.value.pageNo = page
  getRatingList(gameData.value.id)
}

const handleRatingSizeChange = (size) => {
  ratingPage.value.pageSize = size
  ratingPage.value.pageNo = 1
  getRatingList(gameData.value.id)
}

const getGameData = async (id) => {
  if (!id || id === 'undefined') {
    ElMessage.warning('游戏信息不存在，请返回列表重新打开')
    return
  }
  try {
    const gameRes = await getRequest('/game/get/' + id, { silentError: true })
    if (gameRes.code !== 200 || !gameRes.data) {
      return
    }

    const [ratingResult, countResult, statsResult] = await Promise.allSettled([
      getRequest(`/gameRating/average-rating/${id}`, { silentError: true }),
      getRequest(`/gameRating/rating-count/${id}`, { silentError: true }),
      getRequest(`/game/stats/${id}`, { silentError: true })
    ])
    const ratingRes = ratingResult.status === 'fulfilled' ? ratingResult.value : null
    const countRes = countResult.status === 'fulfilled' ? countResult.value : null
    const statsRes = statsResult.status === 'fulfilled' ? statsResult.value : null

    gameData.value = normalizeGame(
      gameRes.data,
      ratingRes?.code === 200 ? ratingRes.data : 0,
      countRes?.code === 200 ? countRes.data : 0
    )
    selectedMedia.value = gameData.value.media[0] || {}
    gameStats.value = {
      ratingCount: statsRes?.data?.ratingCount || 0,
      goodRate: statsRes?.data?.goodRate || 0,
      reviewType: statsRes?.data?.reviewType || calcReviewType(statsRes?.data?.ratingCount || countRes?.data || 0, statsRes?.data?.goodRate || 0),
      totalSales: statsRes?.data?.totalSales || 0,
      ratingTrend: statsRes?.data?.ratingTrend || [],
      monthlySalesTrend: statsRes?.data?.monthlySalesTrend || []
    }
    gameData.value.reviewType = gameStats.value.reviewType
    getCommerceStatus(id)
    getUserRating()
  } catch (err) {
    console.error('获取游戏数据失败:', err)
    ElMessage.error('游戏详情加载失败，请稍后再试')
  }
}

watch(
  () => [route.path, route.query.id, route.query.review],
  ([path, newId, reviewFlag], oldValue = []) => {
    const [oldPath, oldId] = oldValue
    if (path !== '/gameDetail') return
    if (!newId) return
    const openReviews = reviewFlag === '1'
    const sameGame = newId === oldId && path === oldPath
    if (!sameGame) {
      currentTab.value = openReviews ? 'reviews' : 'description'
      selectedMedia.value = {}
      userRating.value = 0
      reviewContent.value = ''
      reviewRecommend.value = null
      hasRated.value = false
      ratingList.value = []
      ratingPage.value.pageNo = 1
      ratingPage.value.total = 0
      inWishlist.value = false
      inCart.value = false
      getGameData(newId)
      getRatingList(newId)
    }
    if (openReviews) {
      currentTab.value = 'reviews'
      scrollToReviewSection()
    }
  },
  { immediate: true }
)

watch([reviewFilter, reviewDateMode, reviewMonth, reviewYear], () => {
  ratingPage.value.pageNo = 1
  if (gameData.value.id) {
    getRatingList(gameData.value.id)
  }
})

function calcReviewType(count, goodRate) {
  const total = Number(count) || 0
  const rate = Number(goodRate) || 0
  if (total <= 0) return '暂无用户评测'
  if (rate >= 95 && total >= 500) return '好评如潮'
  if ((rate >= 80 && rate <= 94 && total >= 50) || (rate >= 95 && total >= 50)) return '特别好评'
  if (rate >= 80) return '好评'
  if (rate >= 70 && total >= 10) return '多半好评'
  if (rate >= 40 && total >= 10) return '褒贬不一'
  if (rate >= 20 && total >= 10) return '多半差评'
  if (rate <= 19 && total >= 500) return '差评如潮'
  if (rate <= 19 && total >= 50) return '特别差评'
  if (rate <= 19) return '差评'
  return '褒贬不一'
}

const recommendText = (value) => {
  if (value === true) return '推荐'
  if (value === false) return '不推荐'
  return '中评'
}

const recommendTagClass = (value) => ({
  bad: value === false,
  neutral: value === null || value === undefined
})
</script>

<style lang="less" scoped>
.mainpage {
  width: 100%;
  height: 100%;
  overflow-y: auto;
}

.mainpage::-webkit-scrollbar {
  width: 0;
}

.game-detail-container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 18px;
}

.backWrapper {
  font-size: 14px;
  font-weight: 550;
  margin-bottom: 15px;

  .back {
    background-color: #f7f8f9;
    padding: 5px 10px;
    cursor: pointer;
    border-radius: 5px;
    display: inline-block;

    &:hover {
      background-color: #e7e8e9;
    }
  }
}

.game-header {
  margin-bottom: 20px;
}

.game-title-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.game-title {
  font-size: 32px;
  font-weight: 600;
  color: #1d1d1f;
}

.subtitle {
  font-size: 18px;
  color: #6e6e73;
  line-height: 1.4;
}

.divider {
  margin: 0 8px;
}

.game-tags {
  display: flex;
  gap: 8px;
  margin: 12px 0;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 12px;
  background: #f5f5f7;
  border-radius: 16px;
  font-size: 14px;
  color: #6e6e73;
}

.game-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 15px 20px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 12px;
  min-width: 150px;

  .score {
    font-size: 38px;
    font-weight: bold;
    color: #ff6b6b;
    line-height: 1;
  }

  .review-count {
    font-size: 13px;
    color: #666;
    margin-top: 8px;
  }

  .review-type {
    margin-top: 8px;
    padding: 2px 8px;
    border-radius: 999px;
    color: #1677ff;
    background: #eaf3ff;
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;

    &.empty {
      color: #909399;
      background: #f3f4f6;
    }
  }
}

.media-section {
  margin-bottom: 20px;
  height: 592px;
  width: 100%;
  background: #000;
  border-radius: 18px;
  overflow: hidden;
}

.main-media {
  width: 100%;
  height: 450px;
  background: #000;
  overflow: hidden;
}

.media-content {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.empty-media {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.media-thumb-shell {
  position: relative;
  margin-top: 18px;
  padding: 0 18px 16px;
}

.media-thumbnails {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 12px;
  scroll-behavior: smooth;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.55) rgba(255, 255, 255, 0.12);

  &::-webkit-scrollbar {
    display: block;
    height: 8px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(255, 255, 255, 0.12);
    border-radius: 999px;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.48);
    border-radius: 999px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: rgba(255, 255, 255, 0.68);
  }
}

.thumbnail {
  position: relative;
  flex: 0 0 auto;
  width: 140px;
  height: 80px;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.05);
  }

  img,
  .thumb-video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  &.active {
    box-shadow: 0 0 0 3px #0066cc;
  }
}

.video-mark {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.58);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transform: translate(-50%, -50%);
  font-size: 16px;
  pointer-events: none;
}

.purchase-section {
  background: rgba(0, 0, 0, 0.03);
  padding: 15px;
  border-radius: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 15px auto;
  max-width: 800px;
}

.current-price {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.price {
  font-size: 30px;
  font-weight: 600;
  color: #1d1d1f;

  &.free {
    color: #2f8f46;
  }
}

.original-price {
  text-decoration: line-through;
  color: #6e6e73;
  font-size: 16px;
}

.discount-tag {
  background: #ff6b6b;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
}

.price-mark {
  border: 1px solid #ff6b6b;
  color: #ff6b6b;
  background: #fff5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.purchase-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.buy-button,
.owned-button,
.wish-button,
.cart-button {
  border: none;
  padding: 10px 30px;
  border-radius: 980px;
  font-size: 15px;
}

.buy-button {
  background: #0071e3;
  color: white;
  cursor: pointer;

  &:hover {
    background: #0077ed;
  }
}

.owned-button {
  background: #f5f5f7;
  color: #1d1d1f;
}

.wish-button,
.cart-button {
  cursor: pointer;
  background: #fff;
  color: #1d1d1f;
  border: 1px solid #d2d2d7;

  &:hover {
    border-color: #0071e3;
    color: #0071e3;
  }

  &.active {
    background: #fff7e6;
    border-color: #ffb020;
    color: #c77800;
  }
}

.info-tabs {
  margin-top: 30px;
}

.tab-headers {
  display: flex;
  justify-content: flex-start;
  gap: 40px;
  border-bottom: 1px solid #d2d2d7;
  margin-bottom: 10px;
}

.tab-header {
  padding: 16px 8px;
  cursor: pointer;
  font-size: 17px;
  color: #1d1d1f;
  position: relative;

  &.active {
    color: #0066cc;
  }

  &.active::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    width: 100%;
    height: 2px;
    background: #0066cc;
  }
}

.tab-content {
  padding: 20px 0;
  max-width: 900px;
  margin: 0 auto;
  font-size: 14px;
  line-height: 1.6;
  color: #14191e;
}

.requirements ul {
  list-style: none;
  padding: 0;
}

.requirements li {
  margin: 10px 0;
}

.req-label {
  font-weight: bold;
  margin-right: 10px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card,
.chart-panel,
.review-editor,
.rating-list-wrapper,
.review-login-tip {
  background: #f8f9fb;
  border-radius: 8px;
  padding: 18px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1d1d1f;
}

.stat-label {
  color: #6e6e73;
  margin-top: 6px;
}

.chart-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.visual-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.chart-panel h3 {
  margin: 0 0 12px;
  font-size: 16px;
}

.pie-chart {
  width: 168px;
  height: 168px;
  border-radius: 50%;
  margin: 8px auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pie-center {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  strong {
    font-size: 24px;
    color: #303133;
  }

  span {
    color: #909399;
    font-size: 12px;
  }
}

.pie-legend {
  display: flex;
  justify-content: center;
  gap: 18px;
  color: #606266;
  font-size: 13px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  i {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;
  }

  .good {
    background: #67c23a;
  }

  .bad {
    background: #f56c6c;
  }
}

.score-bars {
  display: grid;
  gap: 8px;
}

.score-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 28px;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 12px;

  em {
    font-style: normal;
    text-align: right;
  }
}

.score-track {
  height: 10px;
  border-radius: 999px;
  background: #e9edf3;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #409eff, #67c23a);
}

.line-chart {
  height: 220px;
  display: flex;
  flex-direction: column;

  svg {
    width: 100%;
    height: 172px;
    overflow: visible;
  }
}

.line-grid {
  fill: none;
  stroke: #e9edf3;
  stroke-width: 1;
}

.line {
  fill: none;
  stroke: #409eff;
  stroke-width: 4;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-labels {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.bar-chart {
  height: 220px;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  overflow-x: auto;
  padding-top: 20px;
}

.bar-item {
  min-width: 58px;
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
}

.bar-value {
  font-size: 12px;
  color: #6e6e73;
  margin-bottom: 6px;
}

.bar-track {
  height: 150px;
  width: 24px;
  background: #e9edf3;
  border-radius: 6px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  border-radius: 6px 6px 0 0;

  &.rating {
    background: #409eff;
  }

  &.sales {
    background: #67c23a;
  }
}

.bar-label {
  margin-top: 8px;
  color: #6e6e73;
  font-size: 12px;
}

.empty-chart {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.heatmap-panel {
  margin-top: 16px;
}

.heatmap {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}

.heat-cell {
  height: 70px;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  strong {
    font-size: 18px;
  }

  span {
    font-size: 12px;
    margin-top: 4px;
  }
}

.review-editor {
  margin-bottom: 16px;
}

.review-editor-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.review-editor-label {
  width: 42px;
  color: #606266;
}

.review-choice-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.review-choice {
  border: 1px solid #d7e1ee;
  border-radius: 6px;
  background: #fff;
  color: #425466;
  padding: 7px 14px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  transition: all 0.16s ease;

  &.recommend.active {
    border-color: #409eff;
    background: #ecf5ff;
    color: #1677d2;
    font-weight: 600;
  }

  &.not-recommend.active {
    border-color: #f56c6c;
    background: #fff0f0;
    color: #d94848;
    font-weight: 600;
  }
}

.review-choice-tip {
  color: #909399;
  font-size: 12px;
}

.review-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.review-login-tip {
  color: #909399;
  margin-bottom: 16px;
}

.review-filter {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.review-date-filter {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.rating-item {
  padding: 16px 0;
  border-bottom: 1px solid #eee;

  &:last-child {
    border-bottom: none;
  }
}

.rating-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.recommend-tag {
  color: #67c23a;
  background: rgba(103, 194, 58, 0.12);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;

  &.bad {
    color: #f56c6c;
    background: rgba(245, 108, 108, 0.12);
  }

  &.neutral {
    color: #8a92a3;
    background: rgba(144, 147, 153, 0.14);
  }
}

.rating-score {
  font-size: 16px;
  font-weight: bold;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
}

.rating-content {
  color: #303133;
  margin: 8px 0;
  white-space: pre-wrap;
}

.rating-time {
  font-size: 12px;
  color: #999;
}

.no-rating {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;
}

.rating-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  .game-title-section,
  .purchase-section,
  .rating-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .game-score {
    align-items: flex-start;
    width: 100%;
  }

  .media-section {
    height: auto;
  }

  .main-media {
    height: 260px;
  }

  .tab-headers {
    gap: 16px;
    overflow-x: auto;
  }

  .stats-grid,
  .visual-grid,
  .chart-section {
    grid-template-columns: 1fr;
  }
}
</style>
