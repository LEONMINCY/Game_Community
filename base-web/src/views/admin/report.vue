<!-- 文件说明：views/admin/report.vue，后台举报管理页面，处理评论、新闻评论和用户举报。 -->
<template>
  <div class="report-management">
    <el-form :model="queryParams" :inline="true" label-width="84px" class="query-form">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 150px">
          <el-option :label="pendingStatusLabel" value="PENDING" />
          <el-option label="已禁言" value="MUTED" />
          <el-option label="已解除禁言" value="UNMUTED" />
          <el-option label="已封禁" value="BANNED" />
          <el-option label="已解封" value="UNBANNED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="queryParams.reportType" clearable placeholder="全部类型" style="width: 150px">
          <el-option label="评论举报" value="COMMENT" />
          <el-option label="用户举报" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item label="被举报用户">
        <el-input
          v-model="queryParams.reportedKeyword"
          clearable
          placeholder="昵称或账号"
          style="width: 180px"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div v-if="pendingReportCount > 0" class="pending-reminder" @click="showPendingReports">
      <span class="pending-dot"></span>
      <span>有 {{ pendingReportCount }} 条举报或申诉待处理</span>
      <small>点击查看</small>
    </div>

    <el-table :data="reports" class="reports-table" row-key="id" table-layout="fixed">
      <el-table-column label="类型" width="116">
        <template #default="scope">
          <el-tag :type="reportTypeTag(scope.row.reportType)">
            {{ reportTypeText(scope.row.reportType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="举报关系" min-width="148" show-overflow-tooltip>
        <template #default="scope">
          <div class="relation-cell">
            <span>{{ scope.row.reporterNickname || '-' }}</span>
            <small>举报 {{ scope.row.reportedNickname || '-' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="postTitle" label="所属帖子" min-width="118" show-overflow-tooltip />
      <el-table-column label="内容/原因" min-width="170" show-overflow-tooltip>
        <template #default="scope">
          <div class="stack-cell">
            <span>{{ scope.row.reportType === 'USER' ? (scope.row.evidenceText || scope.row.reason || '-') : (scope.row.commentContent || '-') }}</span>
            <small>原因：{{ scope.row.reason || '-' }}</small>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row)">{{ formatStatus(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="举报时间" width="150" show-overflow-tooltip />
      <el-table-column label="账号" width="68">
        <template #default="scope">
          <el-tag :type="scope.row.reportedEnableFlag ? 'success' : 'danger'">
            {{ scope.row.reportedEnableFlag ? '正常' : '封禁' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="226">
        <template #default="scope">
          <div class="table-actions">
            <el-button type="primary" link @click="openDetail(scope.row)">详情</el-button>
            <el-button type="primary" link @click="goPrivateMessage(scope.row)">私信</el-button>
            <el-button v-if="isPending(scope.row)" type="warning" link @click="openMute(scope.row)">禁言</el-button>
            <el-button v-if="isMuted(scope.row)" type="success" link @click="unmute(scope.row)">解除</el-button>
            <el-button v-if="isAppealPending(scope.row)" type="primary" link @click="openAppealReview(scope.row)">处理申诉</el-button>
            <el-button
              v-if="isPending(scope.row) && scope.row.reportedEnableFlag"
              type="danger"
              link
              @click="openBan(scope.row)"
            >封禁</el-button>
            <el-button
              v-if="isBanned(scope.row) || scope.row.reportedEnableFlag === false"
              type="success"
              link
              @click="updateUserStatus(scope.row, true)"
            >解封</el-button>
            <el-button v-if="isPending(scope.row)" type="info" link @click="openReject(scope.row)">拒绝</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="detailDialogVisible" title="举报详情" width="720px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="举报类型">{{ reportTypeText(detail.reportType) }}</el-descriptions-item>
        <el-descriptions-item label="举报用户">{{ detail.reporterNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被举报用户">
          {{ detail.reportedNickname || '-' }}（{{ detail.reportedUsername || '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag :type="detail.reportedEnableFlag ? 'success' : 'danger'">
            {{ detail.reportedEnableFlag ? '正常' : '封禁' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所属帖子">{{ detail.postTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评论内容">
          <div class="detail-content">{{ detail.commentContent || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="举报原因">{{ detail.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文字证据">
          <div class="detail-content">{{ detail.evidenceText || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="图片证据">
          <div v-if="evidenceImages(detail).length" class="evidence-images">
            <el-image
              v-for="img in evidenceImages(detail)"
              :key="img"
              :src="img"
              fit="cover"
              preview-teleported
              :preview-src-list="evidenceImages(detail)"
            />
          </div>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ formatStatus(detail) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.muteEndTime" label="禁言截止">{{ detail.muteEndTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.banEndTime" label="封禁截止">{{ detail.banEndTime }}</el-descriptions-item>
        <el-descriptions-item label="处理回复">{{ detail.reply || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申诉状态">{{ appealStatusText(detail.appealStatus) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.appealContent" label="申诉内容">
          <div class="detail-content">{{ detail.appealContent }}</div>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.appealReply" label="申诉回复">{{ detail.appealReply }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button v-if="isPending(detail)" type="warning" @click="openMute(detail)">禁言</el-button>
        <el-button v-if="isMuted(detail)" type="success" @click="unmute(detail)">解除禁言</el-button>
        <el-button v-if="isPending(detail) && detail.reportedEnableFlag" type="danger" @click="openBan(detail)">封禁账号</el-button>
        <el-button type="primary" @click="goPrivateMessage(detail)">私信用户</el-button>
        <el-button v-if="isBanned(detail) || detail.reportedEnableFlag === false" type="success" @click="updateUserStatus(detail, true)">解封账号</el-button>
        <el-button v-if="isPending(detail)" type="info" @click="openReject(detail)">拒绝处理</el-button>
        <el-button v-if="isAppealPending(detail)" type="primary" @click="openAppealReview(detail)">处理申诉</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="muteDialogVisible" title="设置禁言" width="360px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ muteTarget?.reportedNickname || '-' }}</span>
        </el-form-item>
        <el-form-item label="天数">
          <el-input-number v-model="muteDays" :min="1" :max="365" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="muteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMute">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="banDialogVisible" title="封禁账号" width="380px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ banTarget?.reportedNickname || '-' }}</span>
        </el-form-item>
        <el-form-item label="永久封禁">
          <el-switch v-model="permanentBan" />
        </el-form-item>
        <el-form-item v-if="!permanentBan" label="封禁天数">
          <el-input-number v-model="banDays" :min="1" :max="3650" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="banDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitBan">确认封禁</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="拒绝处理" width="420px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        maxlength="300"
        show-word-limit
        placeholder="请填写拒绝原因，用户端会收到该回复"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="appealReviewDialogVisible" title="处理举报申诉" width="460px">
      <el-form label-width="84px">
        <el-form-item label="处理结果">
          <el-radio-group v-model="appealReviewForm.appealStatus">
            <el-radio-button label="APPROVED">通过</el-radio-button>
            <el-radio-button label="REJECTED">驳回</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input
            v-model="appealReviewForm.appealReply"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="请填写给用户的申诉处理回复"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appealReviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppealReview">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getRequest, postRequest, putRequest } from '../../utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { replaceURL } from '@/utils/tools'

const router = useRouter()
const reports = ref([])
const total = ref(0)
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  reportType: '',
  status: '',
  reportedKeyword: ''
})

const detailDialogVisible = ref(false)
const detail = ref({})
const muteDialogVisible = ref(false)
const muteTarget = ref(null)
const muteDays = ref(1)
const banDialogVisible = ref(false)
const banTarget = ref(null)
const permanentBan = ref(true)
const banDays = ref(7)
const rejectDialogVisible = ref(false)
const rejectTarget = ref(null)
const rejectReason = ref('')
const appealReviewDialogVisible = ref(false)
const appealReviewForm = ref({
  id: null,
  appealStatus: 'REJECTED',
  appealReply: ''
})
const pendingReportCount = ref(0)
const pendingStatusLabel = computed(() => pendingReportCount.value > 0 ? `待处理（${pendingReportCount.value}）` : '待处理')
let pendingCountTimer = null

onMounted(() => {
  getReportList()
  loadPendingCount()
  pendingCountTimer = setInterval(loadPendingCount, 5000)
})

onUnmounted(() => {
  if (pendingCountTimer) {
    clearInterval(pendingCountTimer)
    pendingCountTimer = null
  }
})

const loadPendingCount = () => {
  getRequest('/report/pending-count', { silentError: true }).then(res => {
    if (res.code === 200) {
      const nextCount = Number(res.data || 0)
      const countChanged = nextCount !== pendingReportCount.value
      pendingReportCount.value = nextCount
      if (countChanged && queryParams.value.status === 'PENDING') {
        getReportList()
      }
    }
  }).catch(() => {})
}

const refreshReportData = () => {
  getReportList()
  loadPendingCount()
}

const showPendingReports = () => {
  queryParams.value.status = 'PENDING'
  queryParams.value.pageNo = 1
  refreshReportData()
}

const getReportList = () => {
  postRequest('/report/page', queryParams.value).then(res => {
    if (res.code === 200) {
      reports.value = res.data.list || []
      total.value = res.data.total
    }
  })
}

const search = () => {
  queryParams.value.pageNo = 1
  getReportList()
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    reportType: '',
    status: '',
    reportedKeyword: ''
  }
  getReportList()
}

const handleCurrentChange = (page) => {
  queryParams.value.pageNo = page
  getReportList()
}

const openDetail = (row) => {
  if (!row?.id) {
    detail.value = row || {}
    detailDialogVisible.value = true
    return
  }
  getRequest(`/report/detail/${row.id}`, { silentError: true }).then(res => {
    if (res.code === 200) {
      detail.value = res.data
      detailDialogVisible.value = true
    } else {
      detail.value = row
      detailDialogVisible.value = true
    }
  }).catch(() => {
    detail.value = row
    detailDialogVisible.value = true
  })
}

const openMute = (row) => {
  muteTarget.value = row
  muteDays.value = 1
  muteDialogVisible.value = true
}

const submitMute = () => {
  putRequest('/report/mute', {
    id: muteTarget.value.id,
    muteDays: muteDays.value
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('禁言设置成功')
      muteDialogVisible.value = false
      detailDialogVisible.value = false
      refreshReportData()
    } else {
      ElMessage.error(res.msg || '禁言设置失败')
    }
  })
}

const unmute = (row) => {
  ElMessageBox.confirm(`确定要解除 ${row.reportedNickname || '该用户'} 的禁言吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    putRequest('/report/unmute', {
      id: row.id
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success('已解除禁言')
        detailDialogVisible.value = false
        refreshReportData()
      } else {
        ElMessage.error(res.msg || '解除禁言失败')
      }
    })
  }).catch(() => {})
}

const openBan = (row) => {
  banTarget.value = row
  permanentBan.value = true
  banDays.value = 7
  banDialogVisible.value = true
}

const submitBan = () => {
  updateUserStatus(banTarget.value, false, permanentBan.value ? null : banDays.value)
  banDialogVisible.value = false
}

const updateUserStatus = (row, enableFlag, days = null) => {
  const actionText = enableFlag ? '解封' : '封禁'
  ElMessageBox.confirm(`确定要${actionText}该用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    putRequest('/report/user-status', {
      id: row.id,
      enableFlag,
      banDays: days
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success(`${actionText}成功`)
        detailDialogVisible.value = false
        refreshReportData()
      } else {
        ElMessage.error(res.msg || `${actionText}失败`)
      }
    })
  }).catch(() => {})
}

const openReject = (row) => {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const submitReject = () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  putRequest('/report/reject', {
    id: rejectTarget.value.id,
    reply: rejectReason.value.trim()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('已拒绝处理')
      rejectDialogVisible.value = false
      detailDialogVisible.value = false
      refreshReportData()
    } else {
      ElMessage.error(res.msg || '拒绝处理失败')
    }
  })
}

const goPrivateMessage = (row) => {
  if (!row?.reportedUserId) {
    ElMessage.warning('未找到被举报用户')
    return
  }
  router.push({
    path: '/admin/message',
    query: {
      userId: row.reportedUserId,
      nickname: row.reportedNickname || row.reportedUsername || ''
    }
  })
}

const openAppealReview = (row) => {
  appealReviewForm.value = {
    id: row.id,
    appealStatus: 'REJECTED',
    appealReply: ''
  }
  appealReviewDialogVisible.value = true
}

const submitAppealReview = () => {
  if (!appealReviewForm.value.appealReply.trim()) {
    ElMessage.warning('请填写申诉处理回复')
    return
  }
  putRequest('/report/appeal/review', appealReviewForm.value).then(res => {
    if (res.code === 200) {
      ElMessage.success('申诉处理已更新')
      appealReviewDialogVisible.value = false
      detailDialogVisible.value = false
      refreshReportData()
    } else {
      ElMessage.error(res.msg || '申诉处理失败')
    }
  })
}

const isPending = (row) => !row?.status || row.status === 'PENDING'

const isMuted = (row) => row?.status && String(row.status).startsWith('MUTE:')

const isBanned = (row) => {
  const status = String(row?.status || '')
  return status === 'BANNED' || status.startsWith('BAN:')
}

const isAppealPending = (row) => row?.appealStatus === 'PENDING'

const appealStatusText = (status) => {
  const map = {
    PENDING: '待处理',
    APPROVED: '已通过',
    REJECTED: '已驳回'
  }
  return map[status] || '暂无申诉'
}

const reportTypeTag = (type) => {
  if (type === 'USER') return 'danger'
  if (type === 'NEWS_COMMENT') return 'warning'
  return 'info'
}

const reportTypeText = (type) => {
  if (type === 'NEWS_COMMENT') return '新闻评论举报'
  if (type === 'COMMENT') return '帖子评论举报'
  return type === 'USER' ? '用户举报' : '评论举报'
}

const formatStatus = (rowOrStatus) => {
  const row = typeof rowOrStatus === 'object' ? rowOrStatus : null
  const status = row ? row.status : rowOrStatus
  if (row && isAppealPending(row)) return '申诉待处理'
  if (!status || status === 'PENDING') return '待处理'
  if (String(status).startsWith('MUTE:')) return '已禁言'
  if (String(status).startsWith('BAN:')) return '已封禁'
  const map = {
    BANNED: '已封禁',
    UNBANNED: '已解封',
    UNMUTED: '已解除禁言',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const statusTagType = (rowOrStatus) => {
  const row = typeof rowOrStatus === 'object' ? rowOrStatus : null
  const status = row ? row.status : rowOrStatus
  if (row && isAppealPending(row)) return 'danger'
  if (!status || status === 'PENDING') return 'info'
  if (String(status).startsWith('MUTE:')) return 'warning'
  if (String(status).startsWith('BAN:')) return 'danger'
  const map = {
    BANNED: 'danger',
    UNBANNED: 'success',
    UNMUTED: 'success',
    REJECTED: 'info'
  }
  return map[status] || 'info'
}

const evidenceImages = (row) => {
  if (!row?.evidenceImages) return []
  try {
    const parsed = JSON.parse(row.evidenceImages)
    return Array.isArray(parsed) ? parsed.map(img => replaceURL(img)) : []
  } catch (error) {
    return []
  }
}
</script>

<style scoped>
.report-management {
  padding: 28px 24px;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow-x: hidden;
}

.query-form {
  margin-bottom: 12px;
}

.pending-reminder {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid #ffd6d6;
  border-radius: 6px;
  background: #fff7f7;
  color: #f56c6c;
  cursor: pointer;
  font-size: 14px;
}

.pending-reminder small {
  color: #909399;
}

.pending-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  box-shadow: 0 0 0 4px rgba(245, 108, 108, .12);
}

.reports-table,
:deep(.el-table) {
  width: 100%;
}

.reports-table {
  max-width: 100%;
}

.reports-table :deep(.el-table__inner-wrapper),
.reports-table :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.reports-table :deep(.el-scrollbar__bar.is-horizontal) {
  display: none;
}

.reports-table :deep(.el-table__body),
.reports-table :deep(.el-table__header) {
  width: 100% !important;
}

.reports-table :deep(.el-table__cell) {
  box-sizing: border-box;
}

.relation-cell,
.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.relation-cell span,
.stack-cell span,
.relation-cell small,
.stack-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-cell small,
.stack-cell small {
  color: #909399;
}

.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: end;
}

.detail-content {
  max-height: 160px;
  overflow-y: auto;
  line-height: 1.7;
  white-space: pre-wrap;
}

.evidence-images {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.evidence-images :deep(.el-image) {
  width: 88px;
  height: 88px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}
</style>
