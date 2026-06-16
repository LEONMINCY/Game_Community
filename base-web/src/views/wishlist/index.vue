<!-- 文件说明：views/wishlist/index.vue，愿望单页面，展示用户收藏的想玩游戏并支持移除和跳转详情。 -->
<template>
  <div class="commerce-page">
    <div class="page-head">
      <div>
        <h2>愿望单</h2>
        <p>把想玩的游戏先放在这里，降价时再来看看。</p>
      </div>
    </div>

    <el-empty v-if="games.length === 0" description="愿望单还是空的" />
    <div v-else class="game-grid">
      <div v-for="game in games" :key="game.gameId" class="game-card">
        <img :src="game.icon" alt="" @click="goDetail(game.gameId)" />
        <div class="game-info" @click="goDetail(game.gameId)">
          <h3>{{ game.name }}</h3>
          <span>{{ game.developer || '未设置开发商' }}</span>
          <div class="price-row">
            <strong>¥{{ game.finalPrice ?? game.price }}</strong>
            <del v-if="game.discount > 0">¥{{ game.price }}</del>
            <em v-if="game.discount > 0">-{{ game.discount }}%</em>
          </div>
        </div>
        <div class="card-actions">
          <el-button
            :type="game.inCart ? 'success' : 'primary'"
            plain
            @click="toggleCart(game)"
          >
            {{ game.inCart ? '取消购物车' : '加入购物车' }}
          </el-button>
          <el-button type="danger" plain @click="removeItem(game.gameId)">移除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onActivated, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRequest, postRequest, deleteRequest } from '@/utils/http'
import { replaceURL } from '@/utils/tools'

const router = useRouter()
const games = ref([])

onMounted(() => {
  loadWishlist()
})

onActivated(() => {
  loadWishlist()
})

const normalizeGame = (item, cartIds = new Set()) => {
  const price = Number(item.price ?? 0)
  const gameId = Number(item.gameId ?? item.id)
  return {
    ...item,
    gameId,
    price,
    finalPrice: Number(item.finalPrice ?? price),
    discount: Number(item.discount ?? 0),
    icon: item.icon ? replaceURL(item.icon) : '',
    inCart: cartIds.has(gameId)
  }
}

const loadWishlist = () => {
  Promise.allSettled([
    getRequest('/game-user/wishlist/list'),
    getRequest('/game-user/cart/list')
  ]).then(([wishlistResult, cartResult]) => {
    const wishlistRes = wishlistResult.status === 'fulfilled' ? wishlistResult.value : null
    const cartRes = cartResult.status === 'fulfilled' ? cartResult.value : null
    if (wishlistRes?.code === 200) {
      const cartIds = new Set((cartRes?.data || []).map(item => Number(item.gameId ?? item.id)).filter(Boolean))
      games.value = (wishlistRes.data || []).map(item => normalizeGame(item, cartIds))
    }
  })
}

const removeItem = (gameId) => {
  deleteRequest(`/game-user/wishlist/${gameId}`).then(res => {
    if (res.code === 200) {
      ElMessage.success('已移出愿望单')
      loadWishlist()
    }
  })
}

const toggleCart = (game) => {
  if (!game?.gameId) return
  postRequest(`/game-user/cart/toggle/${game.gameId}`, {}).then(res => {
    if (res.code === 200) {
      game.inCart = !!res.data?.active
      ElMessage.success(game.inCart ? '已加入购物车' : '已取消购物车')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const goDetail = (id) => {
  router.push({ path: '/gameDetail', query: { id } })
}
</script>

<style scoped lang="less">
.commerce-page {
  width: 100%;
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  overflow-x: hidden;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0 0 8px;
    font-size: 24px;
  }

  p {
    margin: 0;
    color: #7a8494;
  }
}

.game-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.game-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  min-width: 0;

  img {
    width: 180px;
    aspect-ratio: 16 / 9;
    object-fit: cover;
    border-radius: 6px;
    background: #f2f4f7;
    cursor: pointer;
  }
}

.game-info {
  min-width: 0;
  cursor: pointer;

  h3 {
    margin: 0 0 8px;
    font-size: 18px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #6b7280;
    font-size: 14px;
  }
}

.card-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  min-width: 106px;
}

.price-row {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;

  strong {
    color: #f56c6c;
    font-size: 20px;
  }

  del {
    color: #9aa3af;
  }

  em {
    font-style: normal;
    color: #f56c6c;
    background: #fff1f0;
    padding: 2px 6px;
    border-radius: 4px;
  }
}

@media (max-width: 1100px) {
  .game-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .game-card {
    grid-template-columns: 1fr;

    img {
      width: 100%;
    }
  }
}
</style>
