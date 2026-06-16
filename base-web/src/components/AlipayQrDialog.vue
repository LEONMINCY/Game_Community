<!-- 文件说明：components/AlipayQrDialog.vue，支付宝扫码弹窗组件，展示订单信息、二维码和支付状态刷新入口。 -->
<template>
  <el-dialog
    :model-value="modelValue"
    title="支付宝沙箱扫码支付"
    width="420px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="handleClosed"
  >
    <div class="qr-pay">
      <div class="order-summary">
        <div>
          <span>订单号</span>
          <strong>{{ paymentData?.orderNo || '-' }}</strong>
        </div>
        <div>
          <span>游戏</span>
          <strong>{{ paymentData?.gameName || paymentData?.subject || '游戏订单' }}</strong>
        </div>
      </div>
      <div class="amount">¥{{ paymentData?.amount || '0.00' }}</div>
      <div class="qr-box">
        <img v-if="qrImageUrl" :src="qrImageUrl" alt="支付宝支付二维码" />
        <div v-else-if="qrPending" class="qr-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>二维码生成中...</span>
        </div>
        <el-empty v-else description="二维码生成失败" />
      </div>
      <p>请使用支付宝沙箱版 App 扫码付款，支付成功后系统会自动更新订单。</p>
      <div class="status-line" :class="{ expired: isExpired }">
        {{ statusText }}
      </div>
    </div>
    <template #footer>
      <el-button type="primary" :loading="checking" @click="checkStatus">我已支付，刷新状态</el-button>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getRequest } from '@/utils/http'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  payment: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'paid'])
const checking = ref(false)
const remainingSeconds = ref(0)
const tradeStatus = ref('')
const statusMessage = ref('')
const hasChecked = ref(false)
const localPayment = ref(null)
let timer = null
let countdownTimer = null
let qrTimer = null
let statusDelayTimer = null

const paymentData = computed(() => localPayment.value || props.payment || {})

const qrPending = computed(() => {
  const status = paymentData.value?.qrStatus
  return !paymentData.value?.qrCode && (!status || status === 'PENDING')
})

const qrImageUrl = computed(() => {
  if (paymentData.value?.qrImage) return paymentData.value.qrImage
  if (!paymentData.value?.qrCode) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(paymentData.value.qrCode)}`
})

const isExpired = computed(() => remainingSeconds.value <= 0)

const statusText = computed(() => {
  if (isExpired.value) {
    return '二维码已超时，订单会保持“已取消”状态，可重新发起购买。'
  }
  if (tradeStatus.value === 'WAIT_BUYER_PAY') {
    return `等待支付，二维码剩余 ${formatRemaining(remainingSeconds.value)}`
  }
  if (!hasChecked.value || isWaitingForBuyer(statusMessage.value)) {
    return `等待扫码，二维码剩余 ${formatRemaining(remainingSeconds.value)}`
  }
  return statusMessage.value || `等待扫码，二维码剩余 ${formatRemaining(remainingSeconds.value)}`
})

watch(
  () => [props.modelValue, props.payment?.orderNo],
  ([visible, orderNo]) => {
    stopPolling()
    stopCountdown()
    stopQrPolling()
    localPayment.value = props.payment ? { ...props.payment } : null
    tradeStatus.value = ''
    statusMessage.value = ''
    hasChecked.value = false
    if (visible && orderNo) {
      remainingSeconds.value = Number(props.payment?.expireSeconds) || 30 * 60
      countdownTimer = window.setInterval(() => {
        remainingSeconds.value = Math.max(0, remainingSeconds.value - 1)
        if (remainingSeconds.value <= 0) {
          stopPolling()
          stopCountdown()
          checkStatus()
        }
      }, 1000)
      if (paymentData.value?.qrCode) {
        startStatusPolling()
      } else {
        startQrPolling()
      }
    }
  }
)

onBeforeUnmount(() => {
  handleClosed()
})

function handleClosed() {
  stopPolling()
  stopCountdown()
  stopQrPolling()
}

function stopPolling() {
  if (statusDelayTimer) {
    window.clearTimeout(statusDelayTimer)
    statusDelayTimer = null
  }
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
}

function startStatusPolling() {
  if (timer || statusDelayTimer || !paymentData.value?.qrCode) return
  statusDelayTimer = window.setTimeout(() => {
    statusDelayTimer = null
    checkStatus()
    timer = window.setInterval(checkStatus, 15000)
  }, 8000)
}

function stopCountdown() {
  if (countdownTimer) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function startQrPolling() {
  loadQrCode()
  qrTimer = window.setInterval(loadQrCode, 1000)
}

function stopQrPolling() {
  if (qrTimer) {
    window.clearInterval(qrTimer)
    qrTimer = null
  }
}

function loadQrCode() {
  if (!paymentData.value?.orderNo || paymentData.value?.qrCode) {
    stopQrPolling()
    return
  }
  getRequest('/order/pay/qr?orderNo=' + encodeURIComponent(paymentData.value.orderNo)).then(res => {
    if (res.code === 200 && res.data) {
      localPayment.value = { ...(localPayment.value || {}), ...res.data }
      statusMessage.value = res.data.message || statusMessage.value
      if (res.data.qrCode) {
        stopQrPolling()
        startStatusPolling()
      } else if (res.data.qrStatus === 'FAILED') {
        stopQrPolling()
      }
    }
  })
}

function checkStatus() {
  if (!paymentData.value?.orderNo || checking.value || !paymentData.value?.qrCode) return
  checking.value = true
  getRequest('/order/pay/status?orderNo=' + encodeURIComponent(paymentData.value.orderNo)).then(res => {
    if (res.code === 200) {
      hasChecked.value = true
      tradeStatus.value = res.data?.tradeStatus || ''
      statusMessage.value = res.data?.message || ''
    }
    if (res.code === 200 && res.data?.paid) {
      stopPolling()
      stopCountdown()
      ElMessage.success('支付成功')
      emit('update:modelValue', false)
      emit('paid', paymentData.value.orderNo)
    }
  }).finally(() => {
    checking.value = false
  })
}

function isWaitingForBuyer(message) {
  return !message || message.includes('交易不存在') || message.includes('订单不存在')
}

function formatRemaining(seconds) {
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return `${minutes}:${String(rest).padStart(2, '0')}`
}
</script>

<style scoped>
.qr-pay {
  text-align: center;
}

.amount {
  margin-top: 18px;
  color: #f56c6c;
  font-size: 34px;
  font-weight: 700;
}

.order-summary {
  display: grid;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  text-align: left;
}

.order-summary div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.order-summary span {
  color: #7a8494;
}

.order-summary strong {
  color: #1f2d3d;
  text-align: right;
  word-break: break-all;
}

.qr-box {
  width: 260px;
  height: 260px;
  margin: 22px auto 12px;
  display: grid;
  place-items: center;
  border: 1px solid #e6eaf0;
  border-radius: 8px;
  background: #fff;
}

.qr-box img {
  width: 240px;
  height: 240px;
}

.qr-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #7a8494;
  font-size: 14px;
}

.qr-loading .el-icon {
  color: #409eff;
  font-size: 28px;
}

p {
  margin: 0 0 8px;
  color: #7a8494;
}

.status-line {
  min-height: 22px;
  color: #409eff;
  font-size: 14px;
}

.status-line.expired {
  color: #f56c6c;
}
</style>
