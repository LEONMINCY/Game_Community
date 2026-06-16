<!-- 文件说明：views/admin/order.vue，后台订单管理页面，查询订单、审核退款并处理支付状态。 -->
<template>
  <div class="order-management">
    <el-form :model="queryParams" inline class="query-form">
      <el-form-item label="订单编号">
        <el-input v-model="queryParams.orderNo" clearable placeholder="请输入订单编号" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="用户ID">
        <el-input-number v-model="queryParams.userId" :min="1" :controls="false" placeholder="用户ID" />
      </el-form-item>
      <el-form-item label="游戏名称">
        <el-input v-model="queryParams.gameName" clearable placeholder="请输入游戏名称" @keyup.enter="handleSearch" />
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
      <el-form-item label="退款状态">
        <el-select v-model="queryParams.refundStatus" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="无" value="NONE" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已同意" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="orders" class="orders-table">
      <el-table-column label="订单信息" min-width="220" show-overflow-tooltip>
        <template #default="scope">
          <div class="stack-cell">
            <span>{{ scope.row.orderNo }}</span>
            <small>{{ scope.row.createTime || '-' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="82" />
      <el-table-column prop="gameName" label="游戏名称" min-width="145" show-overflow-tooltip />
      <el-table-column label="金额" width="86">
        <template #default="scope">¥{{ scope.row.totalPrice }}</template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="scope">
          <div class="status-cell">
            <span>{{ scope.row.status }}</span>
            <el-tag :type="refundTagType(scope.row.refundStatus)" size="small">
              {{ refundText(scope.row.refundStatus) }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="退款信息" min-width="210" show-overflow-tooltip>
        <template #default="scope">
          <div class="stack-cell">
            <span>原因：{{ scope.row.refundReason || '-' }}</span>
            <small>回复：{{ scope.row.refundReply || '-' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="scope">
          <div class="table-actions">
            <el-button
              v-if="scope.row.refundStatus === 'PENDING'"
              type="success"
              link
              @click="reviewRefund(scope.row, 'APPROVED')"
            >同意</el-button>
            <el-button
              v-if="scope.row.refundStatus === 'PENDING'"
              type="warning"
              link
              @click="reviewRefund(scope.row, 'REJECTED')"
            >拒绝</el-button>
            <el-button type="danger" link @click="handleDelete(scope.row)">删除</el-button>
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
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { postRequest, putRequest, deleteRequest } from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const total = ref(0)
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  orderNo: '',
  userId: null,
  gameName: '',
  status: '',
  refundStatus: ''
})

onMounted(() => {
  getOrderList()
})

const getOrderList = () => {
  postRequest('/order/page', queryParams).then(res => {
    if (res.code === 200) {
      orders.value = res.data.list || []
      total.value = res.data.total || 0
    }
  })
}

const handleSearch = () => {
  queryParams.pageNo = 1
  getOrderList()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.orderNo = ''
  queryParams.userId = null
  queryParams.gameName = ''
  queryParams.status = ''
  queryParams.refundStatus = ''
  getOrderList()
}

const reviewRefund = (order, decision) => {
  if (decision === 'APPROVED') {
    ElMessageBox.confirm('确定同意该退款申请吗？同意后会调用支付宝退款。', '退款审核', {
      confirmButtonText: '同意',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      submitReview(order, decision, '退款申请已通过')
    }).catch(() => {})
    return
  }

  ElMessageBox.prompt('请输入拒绝原因', '退款审核', {
    confirmButtonText: '拒绝退款',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请填写给用户看的审核回复',
    inputValidator: value => !!String(value || '').trim(),
    inputErrorMessage: '请填写拒绝原因'
  }).then(({ value }) => {
    submitReview(order, decision, value.trim())
  }).catch(() => {})
}

const submitReview = (order, decision, reply) => {
  putRequest('/order/refund/review', {
    id: order.id,
    refundStatus: decision,
    refundReply: reply
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(decision === 'APPROVED' ? '已同意退款' : '已拒绝退款')
      getOrderList()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  })
}

const handleDelete = (order) => {
  ElMessageBox.confirm('确定要删除该订单吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRequest(`/order/delete/${order.id}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getOrderList()
      }
    })
  }).catch(() => {})
}

const handleCurrentChange = (page) => {
  queryParams.pageNo = page
  getOrderList()
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
.order-management {
  width: 100%;
  min-width: 0;
  padding: 20px;
  box-sizing: border-box;
}

.query-form {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 16px;
}

.orders-table {
  width: 100%;
}

.stack-cell,
.status-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.stack-cell span,
.stack-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stack-cell small {
  color: #909399;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
