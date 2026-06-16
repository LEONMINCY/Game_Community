<!-- 文件说明：views/games/index.vue，游戏中心页面，提供游戏搜索、类型筛选、评分筛选和列表展示。 -->
<template>
  <div class="app-container">
    <div class="search-container">
      <el-form :model="queryParams" label-width="70px" :inline="true">
        <el-form-item label="游戏名称">
          <el-input v-model="queryParams.name" placeholder="请输入游戏名称" clearable />
        </el-form-item>
        <el-form-item label="游戏类型">
          <el-select v-model="queryParams.type" placeholder="请选择游戏类型" clearable filterable style="width: 200px" @change="selectType">
            <el-option v-for="type in gameTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="开发商">
          <el-input v-model="queryParams.developer" placeholder="请输入开发商" clearable />
        </el-form-item>
        <el-form-item label="评分" label-width="50px">
          <div class="rating-range">
            <el-input-number v-model="queryParams.minRating" :min="0" :max="10" :precision="1" :step="0.5" />
            <span>至</span>
            <el-input-number v-model="queryParams.maxRating" :min="0" :max="10" :precision="1" :step="0.5" />
          </div>
        </el-form-item>
        <el-form-item label="评价类型">
          <el-select v-model="queryParams.reviewType" placeholder="请选择评价类型" clearable style="width: 180px">
            <el-option v-for="item in reviewTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="success" icon="Star" @click="handleRecommend">热门推荐</el-button>
          <el-button type="danger" :plain="!showDiscountOnly" icon="PriceTag" @click="showDiscountGames">
            限时优惠
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="gamesWrapper">
      <div v-if="showDiscountOnly && games.length === 0" class="empty-state">
        <el-empty description="暂无限时优惠游戏" />
      </div>
      <div class="games-list">
        <el-card v-for="game in games" :key="game.id" class="game-card">
          <div class="game-content" @click="handleGameClick(game.id)">
            <div class="game-image">
              <el-image :src="thumbnailURL(game.icon, 360)" fit="cover" lazy>
                <template #error>
                  <img class="game-image-fallback" :src="imageFallbackURL()" alt="">
                </template>
              </el-image>
              <div class="game-rating">{{ game.rating || '0.0' }}分</div>
            </div>
            <div class="game-info">
              <h3 class="game-title">{{ game.name }}</h3>
              <div class="game-meta">
                <span class="game-developer">开发商：{{ game.developer || '未设置' }}</span>
              </div>
              <div class="game-review">
                <span class="review-type" :class="{ empty: !game.reviewCount }">{{ game.reviewType || '暂无用户评测' }}</span>
                <span>{{ game.reviewCount || 0 }} 条评价</span>
                <span v-if="game.reviewCount">好评率 {{ formatPercent(game.goodRate) }}%</span>
              </div>
              <div class="game-price">
                <span class="final-price">¥{{ game.finalPrice ?? game.price }}</span>
                <span class="origin-price" v-if="game.discount > 0">¥{{ game.price }}</span>
                <span class="discount-badge" v-if="game.discount > 0">-{{ game.discount }}%</span>
                <span class="price-mark" v-if="priceMarkText(game.priceMark)">{{ priceMarkText(game.priceMark) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-if="!showDiscountOnly"
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="totalGames"
        layout="total, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { postRequest, getRequest } from '@/utils/http'
import { imageFallbackURL, replaceURL, thumbnailURL } from '@/utils/tools'

const router = useRouter()
const totalGames = ref(0)
const games = ref([])
const gameTypes = ref([])
const discountGames = ref([])
const showDiscountOnly = ref(false)
const reviewTypeOptions = [
  '好评如潮',
  '特别好评',
  '好评',
  '多半好评',
  '褒贬不一',
  '多半差评',
  '差评',
  '特别差评',
  '差评如潮',
  '暂无用户评测'
]
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  name: '',
  type: '',
  developer: '',
  minRating: null,
  maxRating: null,
  reviewType: ''
})

onMounted(() => {
  getGameTypes()
  getDiscountGames()
  getGames()
})

const normalizeGame = (item) => ({
  ...item,
  icon: item.icon ? replaceURL(item.icon) : '',
  systemRequirements: item.systemRequirements ? safeJsonParse(item.systemRequirements, null) : null
})

const safeJsonParse = (value, fallback) => {
  try {
    return JSON.parse(value)
  } catch (e) {
    return fallback
  }
}

const priceMarkText = (value) => {
  if (value === 'historical_low') return '史低'
  if (value === 'tie_historical_low') return '平史低'
  return ''
}

const buildSearchParams = () => {
  const params = { ...queryParams.value }
  params.name = params.name ? String(params.name).trim() : ''
  params.type = params.type ? String(params.type).trim() : ''
  params.developer = params.developer ? String(params.developer).trim() : ''
  params.reviewType = params.reviewType ? String(params.reviewType).trim() : ''
  if (params.minRating !== null || params.maxRating !== null) {
    params.ratingRange = `${params.minRating ?? ''},${params.maxRating ?? ''}`
  }
  delete params.minRating
  delete params.maxRating
  return params
}

const formatPercent = (value) => {
  const number = Number(value || 0)
  return Number.isInteger(number) ? number : number.toFixed(1)
}

const getGameTypes = () => {
  getRequest('/game/types').then(res => {
    if (res.code === 200) {
      gameTypes.value = res.data || []
    }
  })
}

const getDiscountGames = () => {
  getRequest('/game/discounts').then(res => {
    if (res.code === 200) {
      discountGames.value = (res.data || []).map(normalizeGame)
      if (showDiscountOnly.value) {
        games.value = discountGames.value
        totalGames.value = discountGames.value.length
      }
    }
  })
}

const getGames = () => {
  showDiscountOnly.value = false
  postRequest('/game/list', buildSearchParams()).then(res => {
    if (res.code === 200) {
      games.value = (res.data.list || []).map(normalizeGame)
      totalGames.value = res.data.total || 0
    }
  })
}

const handleRecommend = () => {
  showDiscountOnly.value = false
  getRequest('/game/recommend').then(res => {
    if (res.code === 200) {
      games.value = (res.data || []).map(normalizeGame)
    }
  })
}

const selectType = (type) => {
  showDiscountOnly.value = false
  queryParams.value.type = type
  queryParams.value.pageNo = 1
  getGames()
}

const handleGameClick = (id) => {
  router.push({
    path: '/gameDetail',
    query: { id }
  })
}

const handleSearch = () => {
  showDiscountOnly.value = false
  queryParams.value.pageNo = 1
  getGames()
}

const showDiscountGames = () => {
  showDiscountOnly.value = true
  games.value = discountGames.value
  totalGames.value = discountGames.value.length
}

const handleCurrentChange = (val) => {
  queryParams.value.pageNo = val
  getGames()
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    name: '',
    type: '',
    developer: '',
    minRating: null,
    maxRating: null,
    reviewType: ''
  }
  getGames()
}
</script>

<style lang="less" scoped>
.app-container {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  padding: 16px;
  box-sizing: border-box;
  overflow-x: hidden;
}

.search-container {
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.18);

  .el-form-item {
    margin-bottom: 16px;
    margin-right: 16px;
  }
}

.rating-range {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-input-number {
    width: 120px;
  }
}

.game-price {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  line-height: 1.2;
}

.final-price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
}

.origin-price {
  color: #909399;
  text-decoration: line-through;
  white-space: nowrap;
}

.discount-badge {
  background: #f56c6c;
  color: #fff;
  border-radius: 4px;
  padding: 3px 7px;
  font-size: 12px;
  white-space: nowrap;
}

.price-mark {
  border: 1px solid #f56c6c;
  color: #f56c6c;
  background: #fff5f5;
  border-radius: 4px;
  padding: 2px 7px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.gamesWrapper {
  min-height: 300px;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  padding: 0;
  margin: 0;
  position: relative;
  background: transparent;
  border-radius: 8px;
  box-shadow: none;

}

.games-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 0 2px 2px;
  width: 100%;
  box-sizing: border-box;
  min-height: 100%;
  align-items: stretch;
}

.empty-state {
  padding: 40px 0;
}

.game-card {
  transition: all 0.3s;
  cursor: pointer;
  width: 100%;
  min-width: 0;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;

  :deep(.el-card__body) {
    height: 100%;
    padding: 18px 22px;
    box-sizing: border-box;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  }
}

.game-content {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
  width: 100%;
  height: 100%;
}

.game-image {
  position: relative;
  width: clamp(140px, 32%, 180px);
  aspect-ratio: 16 / 9;
  height: auto;
  flex-shrink: 0;

  .el-image {
    width: 100%;
    height: 100%;
    border-radius: 4px;
  }

  .game-image-fallback {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
    border-radius: 4px;
  }
}

.game-rating {
  position: absolute;
  right: 8px;
  top: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}

.game-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.game-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: bold;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.game-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  flex-wrap: wrap;
  color: #666;
  font-size: 13px;
  min-width: 0;
}

.game-review {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  min-width: 0;
  color: #8492a6;
  font-size: 12px;
  flex-wrap: wrap;
}

.review-type {
  padding: 2px 8px;
  border-radius: 999px;
  color: #1677ff;
  background: #eaf3ff;
  font-weight: 600;
  white-space: nowrap;
}

.review-type.empty {
  color: #909399;
  background: #f3f4f6;
}

.game-developer {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .games-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .game-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .game-image {
    width: 100%;
    height: auto;
  }

  .game-meta {
    gap: 8px;
  }

}
</style>
