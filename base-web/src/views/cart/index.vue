<!-- 文件说明：views/cart/index.vue，购物车页面，展示已加入游戏、选择结算并生成订单。 -->
<template>
  <div class="commerce-page">
    <div class="page-head">
      <div>
        <h2>购物车</h2>
        <p>勾选要购买的游戏后统一结算。</p>
      </div>
      <div class="checkout-box">
        <span>已选 {{ selectedIds.length }} 款</span>
        <strong>¥{{ totalPrice }}</strong>
        <el-button type="primary" :disabled="selectedIds.length === 0" @click="checkout">结算</el-button>
      </div>
    </div>

    <el-empty v-if="games.length === 0" description="购物车还是空的" />
    <div v-else class="cart-list">
      <div v-for="game in games" :key="game.gameId" class="cart-card">
        <el-checkbox
          v-model="selectedIds"
          :label="game.gameId"
          :disabled="game.pendingOrder"
        ><span></span></el-checkbox>
        <img :src="game.icon" alt="" @click="goDetail(game.gameId)" />
        <div class="game-info" @click="goDetail(game.gameId)">
          <h3>{{ game.name }}</h3>
          <span>{{ game.developer || '未设置开发商' }}</span>
          <div class="price-row">
            <strong>¥{{ game.finalPrice ?? game.price }}</strong>
            <del v-if="game.discount > 0">¥{{ game.price }}</del>
            <em v-if="game.discount > 0">-{{ game.discount }}%</em>
            <el-tag v-if="game.pendingOrder" type="warning" size="small">待支付</el-tag>
          </div>
          <p v-if="game.pendingOrder" class="pending-tip">已生成待支付订单，可到“我的订单”继续支付。</p>
        </div>
        <el-button type="danger" plain @click="removeItem(game.gameId)">移除</el-button>
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
import { computed, ref, onActivated, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRequest, postRequest, deleteRequest } from '@/utils/http'
import { replaceURL } from '@/utils/tools'
import AlipayQrDialog from '@/components/AlipayQrDialog.vue'

const router = useRouter()
const games = ref([])
const selectedIds = ref([])
const payDialogVisible = ref(false)
const currentPayment = ref(null)

const totalPrice = computed(() => {
  return games.value
    .filter(item => selectedIds.value.includes(item.gameId))
    .reduce((sum, item) => sum + Number(item.finalPrice ?? item.price ?? 0), 0)
    .toFixed(2)
})

onMounted(() => {
  loadCart()
})

onActivated(() => {
  loadCart()
})

const normalizeGame = (item) => {
  const price = Number(item.price ?? 0)
  const finalPrice = Number(item.finalPrice ?? price)
  const pendingValue = item.pendingOrder
  return {
    ...item,
    gameId: Number(item.gameId ?? item.id),
    price,
    finalPrice,
    discount: Number(item.discount ?? 0),
    pendingOrder: pendingValue === true || pendingValue === 1 || pendingValue === '1' || String(pendingValue).toLowerCase() === 'true',
    icon: item.icon ? replaceURL(item.icon) : ''
  }
}

const loadCart = () => {
  getRequest('/game-user/cart/list').then(res => {
    if (res.code === 200) {
      games.value = (res.data || []).map(normalizeGame)
      selectedIds.value = games.value.filter(item => !item.pendingOrder).map(item => item.gameId)
    }
  })
}

const removeItem = (gameId) => {
  deleteRequest(`/game-user/cart/${gameId}`).then(res => {
    if (res.code === 200) {
      ElMessage.success('已移出购物车')
      loadCart()
    }
  })
}

const checkout = () => {
  postRequest('/game-user/cart/checkout', {
    gameIds: selectedIds.value
  }).then(res => {
    if (res.code === 200) {
      currentPayment.value = res.data
      payDialogVisible.value = true
      loadCart()
    } else {
      ElMessage.error(res.msg || '结算失败')
    }
  })
}

const handlePaymentPaid = (orderNo) => {
  loadCart()
  router.push('/paymentResults?out_trade_no=' + encodeURIComponent(orderNo))
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
  gap: 20px;
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

.checkout-box {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;

  span {
    color: #7a8494;
  }

  strong {
    color: #f56c6c;
    font-size: 24px;
  }
}

.cart-list {
  display: grid;
  gap: 14px;
}

.cart-card {
  display: grid;
  grid-template-columns: auto 180px minmax(0, 1fr) auto;
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

.pending-tip {
  margin: 8px 0 0;
  color: #e6a23c;
  font-size: 13px;
}

@media (max-width: 820px) {
  .page-head,
  .checkout-box {
    align-items: flex-start;
  }

  .page-head,
  .cart-card {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .cart-card img {
    width: 100%;
  }
}
</style>
