<!-- 文件说明：views/admin/log.vue，后台日志管理页面，查询、导出和清理用户操作日志。 -->
<template>
  <div class="operation-log-page">
    <div class="filter-panel">
      <el-form :model="query" :inline="true" label-width="72px">
        <el-form-item label="用户">
          <el-input v-model="query.username" clearable placeholder="用户名/昵称" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" clearable placeholder="全部角色" style="width: 150px">
            <el-option label="管理员" :value="1" />
            <el-option label="普通用户" :value="2" />
            <el-option label="社区审核员" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" clearable placeholder="全部结果" style="width: 130px">
            <el-option label="成功" :value="true" />
            <el-option label="失败" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="操作/接口/参数" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="search">查询</el-button>
          <el-button icon="Refresh" @click="reset">重置</el-button>
          <el-button icon="Download" @click="exportLogs">导出</el-button>
          <el-button type="danger" plain icon="Delete" @click="clearLogs">清空时间段</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="logs" border stripe class="log-table">
      <el-table-column prop="createdTime" label="时间" width="170" />
      <el-table-column label="用户" min-width="140">
        <template #default="{ row }">
          <div class="user-cell">
            <span>{{ row.username || 'guest' }}</span>
            <el-tag size="small" effect="plain">{{ roleText(row.roleId) }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="operationName" label="操作" min-width="180" show-overflow-tooltip />
      <el-table-column label="接口" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <span class="method">{{ row.requestMethod }}</span>
          <span>{{ row.requestUri }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="costTime" label="耗时" width="95">
        <template #default="{ row }">{{ row.costTime || 0 }} ms</template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="query.pageNo"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="loadLogs"
      />
    </div>

    <el-drawer v-model="detailVisible" title="日志详情" size="520px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户">{{ currentLog.username || 'guest' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText(currentLog.roleId) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.operationName }}</el-descriptions-item>
        <el-descriptions-item label="接口">{{ currentLog.requestMethod }} {{ currentLog.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="控制器">{{ currentLog.controllerName }}#{{ currentLog.methodName }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ currentLog.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="参数">
          <pre class="detail-pre">{{ currentLog.requestParams || '无' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="!currentLog.success" label="异常">
          <pre class="detail-pre error">{{ currentLog.errorMessage || '未知异常' }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { postRequest } from '../../utils/http'

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  username: '',
  roleId: null,
  success: null,
  keyword: ''
})
const timeRange = ref([])
const logs = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref({})

onMounted(loadLogs)

function buildParams() {
  return {
    ...query,
    startTime: timeRange.value?.[0] || '',
    endTime: timeRange.value?.[1] || ''
  }
}

function loadLogs() {
  postRequest('/operation-log/page', buildParams()).then(res => {
    logs.value = res.data?.list || []
    total.value = res.data?.total || 0
  })
}

function search() {
  query.pageNo = 1
  loadLogs()
}

function reset() {
  query.pageNo = 1
  query.username = ''
  query.roleId = null
  query.success = null
  query.keyword = ''
  timeRange.value = []
  loadLogs()
}

function openDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

function roleText(roleId) {
  const map = {
    1: '管理员',
    2: '普通用户',
    3: '社区审核员'
  }
  return map[Number(roleId)] || '游客'
}

async function exportLogs() {
  const params = new URLSearchParams()
  Object.entries(buildParams()).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      params.append(key, value)
    }
  })
  const response = await fetch(`/api/operation-log/export?${params.toString()}`, {
    headers: {
      Authorization: localStorage.getItem('token') || ''
    }
  })
  if (!response.ok) {
    ElMessage.error('导出失败')
    return
  }
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `operation-log-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

function clearLogs() {
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请先选择要清空的时间段')
    return
  }
  ElMessageBox.confirm('确定清空所选时间段内的操作日志吗？该操作不可恢复。', '确认清空', {
    type: 'warning',
    confirmButtonText: '确定清空',
    cancelButtonText: '取消'
  }).then(() => {
    postRequest('/operation-log/clear', buildParams()).then(res => {
      ElMessage.success(`已清空 ${res.data || 0} 条日志`)
      loadLogs()
    })
  }).catch(() => {})
}
</script>

<style scoped>
.operation-log-page {
  padding: 24px;
  box-sizing: border-box;
}

.filter-panel {
  padding: 18px 18px 6px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  margin-bottom: 18px;
  background: #fff;
}

.log-table {
  width: 100%;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.method {
  display: inline-flex;
  min-width: 46px;
  height: 22px;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  border-radius: 4px;
  background: #eef6ff;
  color: #2d8cf0;
  font-size: 12px;
  font-weight: 600;
}

.pagination-wrapper {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.detail-pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 260px;
  overflow: auto;
  margin: 0;
  font-family: Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.detail-pre.error {
  color: #f56c6c;
}
</style>
