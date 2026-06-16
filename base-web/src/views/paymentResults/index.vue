<!-- 文件说明：views/paymentResults/index.vue，支付结果页，根据支付宝回跳订单号刷新并展示支付结果。 -->
<template>
  <div class="">恭喜购买成功!</div>
</template>
<script setup>
import {ref, onMounted} from "vue";
import { useRoute, useRouter} from "vue-router";
import { ElMessage } from 'element-plus'
import { getRequest   } from "@/utils/http.js";

const route = useRoute()
const router = useRouter()
const orderNo = ref(route.query.out_trade_no)
const total = ref(route.query.total_amount)
onMounted(() => {
  pay()
})

const pay = () => {
  console.log(orderNo.value)
  getRequest('/order/pay?orderNo=' + orderNo.value).then(res => {
    if (res.code === 200) {
      ElMessage.success('支付成功')
      setTimeout(() => {
        router.push('/')
      }, 3000)
    }
  })
}
</script>