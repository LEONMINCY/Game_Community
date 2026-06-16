<!-- 文件说明：views/myOrders/index.vue，我的订单页面，查询订单、继续支付、取消订单和申请退款。 -->
<template>
  <div class="orders-page">
    <div class="page-head">
      <div>
        <h2>我的订单</h2>
        <p>查询订单、删除订单，或对已购买游戏发起退款申请。</p>
      </div>
    </div>

    <el-form :model="queryParams" inline class="query-form">
      <el-form-item label="游戏名称">
        <el-input v-model="queryParams.gameName" clearable placeholder="请输入游戏名称" @keyup.enter="loadOrders" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="待支付" value="待支付" />
          <el-option label="购买成功" value="购买成功" />
          <el-option label="已取消" value="已取消" />
          <el-option label="退款审核中" value="退款审核中" />
          <el-option label="已退款" value="已退款" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="orders" class="orders-table" row-key="id">
      <el-table-column type="expand" width="42">
        <template #default="scope">
          <div class="order-expand">
            <div>
              <span>退款原因</span>
              <p>{{ scope.row.refundReason || '无' }}</p>
            </div>
            <div>
              <span>审核回复</span>
              <p>{{ scope.row.refundReply || '无' }}</p>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="订单编号" min-width="150" class-name="order-no-cell" />
      <el-table-column prop="gameName" label="游戏名称" min-width="150" show-overflow-tooltip />
      <el-table-column label="金额" width="90">
        <template #default="scope">¥{{ scope.row.totalPrice }}</template>
      </el-table-column>
      <el-table-column prop="status" label="订单状态" width="105" />
      <el-table-column label="退款状态" width="105">
        <template #default="scope">
          <el-tag :type="refundTagType(scope.row.refundStatus)">
            {{ refundText(scope.row.refundStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="150" />
      <el-table-column label="操作" min-width="170" class-name="operation-cell">
        <template #default="scope">
          <div class="order-actions">
            <el-button
              v-if="canApplyRefund(scope.row)"
              type="warning"
              link
              @click="openRefundDialog(scope.row)"
            >申请退款</el-button>
            <el-button
              v-if="canReview(scope.row)"
              type="primary"
              link
              @click="goReview(scope.row)"
            >去评价</el-button>
            <el-button
              v-if="scope.row.status === '待支付'"
              type="primary"
              link
              @click="continuePay(scope.row)"
            >去支付</el-button>
            <el-button
              v-if="scope.row.status === '待支付'"
              type="info"
              link
              @click="cancelOrder(scope.row)"
            >取消</el-button>
            <el-button type="danger" link @click="deleteOrder(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="520px">
      <el-input
        v-model="refundReason"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="请填写退款原因"
      />
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
    <AlipayQrDialog
      v-model="payDialogVisible"
      :payment="currentPayment"
      @paid="handlePaymentPaid"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onActivated, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { postRequest, putRequest, deleteRequest } from '@/utils/http'
import AlipayQrDialog from '@/components/AlipayQrDialog.vue'

const router = useRouter()
const orders = ref([])
const total = ref(0)
const refundDialogVisible = ref(false)
const currentOrder = ref(null)
const refundReason = ref('')
const payDialogVisible = ref(false)
const currentPayment = ref(null)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  gameName: '',
  status: ''
})

onMounted(() => {
  loadOrders()
})

onActivated(() => {
  loadOrders()
})

const loadOrders = () => {
  postRequest('/order/my/page', queryParams).then(res => {
    if (res.code === 200) {
      orders.value = res.data.list || []
      total.value = res.data.total || 0
    }
  })
}

const handleSearch = () => {
  queryParams.pageNo = 1
  loadOrders()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.gameName = ''
  queryParams.status = ''
  loadOrders()
}

const handleCurrentChange = (page) => {
  queryParams.pageNo = page
  loadOrders()
}

const canApplyRefund = (order) => {
  return order.status === '购买成功' && order.refundStatus !== 'PENDING' && order.refundStatus !== 'APPROVED'
}

const canReview = (order) => {
  return order.status === '购买成功' && order.gameId
}

const goReview = (order) => {
  router.push({ path: '/gameDetail', query: { id: order.gameId, review: '1' } })
}

const openRefundDialog = (order) => {
  currentOrder.value = order
  refundReason.value = ''
  refundDialogVisible.value = true
}

const submitRefund = () => {
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  putRequest(`/order/refund/apply/${currentOrder.value.id}`, {
    refundReason: refundReason.value.trim()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('退款申请已提交')
      refundDialogVisible.value = false
      loadOrders()
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  })
}

const continuePay = (order) => {
  postRequest('/order/continuePay', {
    id: order.id
  }).then(res => {
    if (res.code === 200) {
      currentPayment.value = {
        ...res.data,
        gameName: order.gameName
      }
      payDialogVisible.value = true
    }
  })
}

const cancelOrder = (order) => {
  ElMessageBox.confirm('确定取消这笔待支付订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    putRequest(`/order/cancel/${order.id}`, {}).then(res => {
      if (res.code === 200) {
        ElMessage.success('订单已取消')
        loadOrders()
      }
    })
  }).catch(() => {})
}

const handlePaymentPaid = () => {
  loadOrders()
}

const deleteOrder = (order) => {
  ElMessageBox.confirm('确定删除该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRequest(`/order/my/delete/${order.id}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadOrders()
      }
    })
  }).catch(() => {})
}

const refundText = (status) => {
  const map = {
    NONE: '无',
    PENDING: '待审核',
    APPROVED: '已同意',
    REJECTED: '已拒绝'
  }
  return map[status] || '无'
}

const refundTagType = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}
</script>

<style scoped lang="less">
.orders-page {
  width: 100%;
  min-height: 100%;
  padding: 24px;
  box-sizing: border-box;
  overflow-x: hidden;
}

.page-head {
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

.query-form {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 16px;

  :deep(.el-form-item) {
    margin-bottom: 8px;
  }
}

.orders-table {
  width: 100%;

  :deep(.el-table__body),
  :deep(.el-table__header) {
    width: 100% !important;
  }

  :deep(.order-no-cell .cell) {
    white-space: normal;
    line-height: 1.45;
    word-break: break-all;
  }

  :deep(.operation-cell .cell) {
    padding-right: 8px;
  }
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
  align-items: center;

  :deep(.el-button) {
    margin-left: 0;
  }
}

.order-expand {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  padding: 8px 42px 14px;
  color: #606266;

  span {
    display: inline-block;
    margin-bottom: 6px;
    color: #909399;
    font-size: 13px;
  }

  p {
    margin: 0;
    line-height: 1.6;
    word-break: break-word;
  }
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 920px) {
  .orders-page {
    padding: 16px;
  }

  .order-expand {
    grid-template-columns: 1fr;
    padding: 8px 18px 14px;
  }
}
</style>
