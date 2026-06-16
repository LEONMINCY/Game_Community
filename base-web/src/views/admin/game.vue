<!-- 文件说明：views/admin/game.vue，后台游戏管理页面，维护游戏资料、类型、平台、截图、视频、价格和折扣。 -->
<template>
  <div class="game-management">
    <el-form :model="queryParams" label-width="70px" :inline="true">
      <el-form-item label="游戏名称">
        <el-input v-model="queryParams.name" placeholder="请输入游戏名称" clearable />
      </el-form-item>
      <el-form-item label="游戏类型">
        <el-select
          v-model="queryParams.type"
          placeholder="请选择或输入游戏类型"
          style="width: 220px"
          clearable
          filterable
          allow-create
          default-first-option
        >
          <el-option v-for="type in gameTypes" :key="type" :label="type" :value="type" />
        </el-select>
      </el-form-item>
      <el-form-item label="开发商">
        <el-input v-model="queryParams.developer" placeholder="请输入开发商" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="search">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="toolbar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <el-table :data="games" class="game-table" style="width: 100%" table-layout="fixed">
      <el-table-column prop="platforms" label="平台" min-width="130" show-overflow-tooltip />
      <el-table-column prop="name" label="游戏名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" min-width="130" show-overflow-tooltip />
      <el-table-column prop="developer" label="开发商" min-width="145" show-overflow-tooltip />
      <el-table-column prop="releaseDate" label="发售日期" width="112">
        <template #default="scope">{{ formatDate(scope.row.releaseDate) || '未设置' }}</template>
      </el-table-column>
      <el-table-column prop="price" label="原价" width="82" />
      <el-table-column label="折扣" width="86">
        <template #default="scope">
          <span v-if="scope.row.discount > 0">-{{ scope.row.discount }}%</span>
          <span v-else>无</span>
        </template>
      </el-table-column>
      <el-table-column label="价格标记" width="96">
        <template #default="scope">
          <el-tag v-if="priceMarkText(scope.row.priceMark)" size="small" type="danger">
            {{ priceMarkText(scope.row.priceMark) }}
          </el-tag>
          <span v-else>正常价</span>
        </template>
      </el-table-column>
      <el-table-column label="折后价" width="96">
        <template #default="scope">
          ¥{{ scope.row.finalPrice ?? scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="166" fixed="right">
        <template #default="scope">
          <el-button type="warning" @click="editGame(scope.row)">编辑</el-button>
          <el-button type="danger" @click="deleteGame(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="totalGames"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑游戏' : '新增游戏'" width="720px">
      <div class="dialog-body">
        <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
          <el-form-item label="支持平台">
            <el-select
              v-model="form.platforms"
              placeholder="选择或输入平台，可多选"
              style="width: 320px"
              filterable
              multiple
              allow-create
              default-first-option
            >
              <el-option v-for="platform in platformOptions" :key="platform" :label="platform" :value="platform" />
            </el-select>
          </el-form-item>
          <el-form-item label="游戏名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入游戏名称" />
          </el-form-item>
          <el-form-item label="游戏类型" prop="type">
            <el-select
              v-model="form.type"
              placeholder="请选择或输入游戏类型，可多选"
              style="width: 260px"
              filterable
              multiple
              allow-create
              default-first-option
            >
              <el-option v-for="type in gameTypes" :key="type" :label="type" :value="type" />
            </el-select>
          </el-form-item>
          <el-form-item label="开发商" prop="developer">
            <el-input v-model="form.developer" placeholder="请输入开发商" />
          </el-form-item>
          <el-form-item label="发售日期">
            <el-date-picker
              v-model="form.releaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择发售日期"
              style="width: 220px"
            />
          </el-form-item>
          <el-form-item label="游戏价格" prop="price">
            <el-input-number v-model="form.price" :min="0" :precision="0" placeholder="输入价格" />
            <span class="unit">元</span>
          </el-form-item>
          <el-form-item label="折扣">
            <el-input-number v-model="form.discount" :min="0" :max="100" :precision="0" />
            <span class="unit">%（0 表示不打折）</span>
          </el-form-item>
          <el-form-item label="价格标记">
            <el-radio-group v-model="form.priceMark">
              <el-radio-button label="">正常价</el-radio-button>
              <el-radio-button label="historical_low">史低</el-radio-button>
              <el-radio-button label="tie_historical_low">平史低</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="系统需求" prop="systemRequirements">
            <div class="requirements-editor">
              <div class="requirement-block" v-for="section in requirementSections" :key="section.key">
                <h3>{{ section.label }}</h3>
                <el-form-item v-for="field in requirementFields" :key="field.key" :label="field.label">
                  <el-input v-model="form.systemRequirements[section.key][field.key]" :placeholder="`请输入${field.label}`" />
                </el-form-item>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="游戏简介" prop="description">
            <el-input type="textarea" v-model="form.description" placeholder="请输入游戏简介" />
          </el-form-item>
          <el-form-item label="游戏封面" prop="icon">
            <el-upload
              class="avatar-uploader"
              action="/api/file/upload"
              v-model:file-list="fileList"
              :headers="headers"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-remove="handleIconRemove"
            >
              <div class="iconWrapper">
                <img v-if="imageUrl" :src="imageUrl" alt="游戏封面" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="游戏截图">
            <el-upload
              style="width: 600px"
              v-model:file-list="fileList1"
              action="/api/file/upload"
              :headers="headers"
              multiple
              :on-success="handleAvatarSuccessImage"
              :on-preview="handlePreview"
              :on-remove="handleScreenshotRemove"
            >
              <el-button type="primary" icon="Plus">上传游戏截图</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="游戏视频">
            <el-upload
              style="width: 600px"
              v-model:file-list="fileList2"
              action="/api/file/upload"
              :headers="headers"
              multiple
              accept="video/mp4,video/webm,video/ogg,image/webp,.mp4,.webm,.ogg,.webp"
              :on-success="handleAvatarSuccessVideo"
              :on-preview="handlePreview"
              :on-remove="handleVideoRemove"
            >
              <el-button type="primary" icon="Plus">上传游戏视频</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="addGame">{{ isEdit ? '保存' : '新增' }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="文件预览" width="800px">
      <div class="preview-body">
        <button
          v-if="canSwitchPreview"
          class="preview-switch left"
          type="button"
          title="上一张"
          @click="switchPreview(-1)"
        >
          ‹
        </button>
        <img v-if="previewType === 'image'" :src="previewUrl" />
        <video v-else-if="previewType === 'video'" :src="previewUrl" controls>
          您的浏览器不支持视频播放
        </video>
        <button
          v-if="canSwitchPreview"
          class="preview-switch right"
          type="button"
          title="下一张"
          @click="switchPreview(1)"
        >
          ›
        </button>
        <div v-if="canSwitchPreview" class="preview-counter">
          {{ previewIndex + 1 }} / {{ previewFiles.length }}
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { deleteRequest, getRequest, postRequest } from '@/utils/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import { replaceURL } from '@/utils/tools'

const headers = ref({
  Authorization: localStorage.getItem('token')
})
const fileList = ref([])
const fileList1 = ref([])
const fileList2 = ref([])
const imageUrl = ref('')
const games = ref([])
const totalGames = ref(0)
const gameTypes = ref([])
const platformOptions = ref(['PC', 'PS5', 'Switch', 'Xbox'])
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  name: '',
  type: '',
  developer: ''
})
const dialogVisible = ref(false)
const isEdit = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewType = ref('')
const previewIndex = ref(0)

const requirementSections = [
  { key: 'minimum', label: '最低配置要求' },
  { key: 'recommended', label: '推荐配置要求' }
]
const requirementFields = [
  { key: '操作系统', label: '操作系统' },
  { key: '处理器', label: '处理器' },
  { key: '内存', label: '内存' },
  { key: '显卡', label: '显卡' },
  { key: '存储空间', label: '存储空间' }
]

const previewFiles = computed(() => [...fileList1.value, ...fileList2.value])

const canSwitchPreview = computed(() => previewFiles.value.length > 1)

const createEmptyRequirements = () => ({
  minimum: Object.fromEntries(requirementFields.map(field => [field.key, ''])),
  recommended: Object.fromEntries(requirementFields.map(field => [field.key, '']))
})

const createEmptyForm = () => ({
  id: null,
  name: null,
  type: [],
  platforms: [],
  developer: null,
  rating: null,
  screenshots: [],
  video: [],
  price: null,
  discount: 0,
  priceMark: '',
  releaseDate: '',
  icon: null,
  systemRequirements: createEmptyRequirements(),
  description: null
})

const form = ref(createEmptyForm())
const rules = ref({
  name: [{ required: true, message: '请输入游戏名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择或输入游戏类型', trigger: 'blur' }],
  developer: [{ required: true, message: '请输入开发商', trigger: 'blur' }]
})

onMounted(() => {
  getGameTypes()
  getPlatformOptions()
  getGameList()
})

const safeJsonParse = (value, fallback) => {
  if (!value) return fallback
  if (typeof value !== 'string') return value
  try {
    return JSON.parse(value)
  } catch (e) {
    return fallback
  }
}

const splitTypes = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value
  return String(value)
    .split(/[,，、/|]+/)
    .map(item => item.trim())
    .filter(Boolean)
}

const normalizeTypeValue = (value) => splitTypes(value).join(',')
const normalizePlatformValue = (value) => splitTypes(value).join(',')

const priceMarkText = (value) => {
  if (value === 'historical_low') return '史低'
  if (value === 'tie_historical_low') return '平史低'
  return ''
}

const formatDate = (value) => {
  if (!value) return ''
  return String(value).slice(0, 10)
}

const getGameTypes = () => {
  getRequest('/game/types').then(res => {
    if (res.code === 200) {
      gameTypes.value = res.data || []
    }
  })
}

const getPlatformOptions = () => {
  getRequest('/ranking/platform-options', { silentError: true }).then(res => {
    if (res.code === 200) {
      platformOptions.value = Array.from(new Set([...platformOptions.value, ...(res.data || [])]))
    }
  }).catch(() => {})
}

const search = () => {
  queryParams.value.pageNo = 1
  getGameList()
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    name: '',
    type: '',
    developer: ''
  }
  getGameList()
}

const getGameList = () => {
  postRequest('/game/list', queryParams.value).then(res => {
    if (res.code === 200) {
      games.value = (res.data.list || []).map(item => ({
        ...item,
        systemRequirements: safeJsonParse(item.systemRequirements, createEmptyRequirements())
      }))
      totalGames.value = res.data.total
    }
  })
}

const handleIconRemove = () => {
  form.value.icon = null
  imageUrl.value = ''
}

const removeStoredUrl = (list, file) => {
  const fileUrl = file.response?.data || file.url
  return list.filter(url => url !== fileUrl && replaceURL(url) !== fileUrl)
}

const handleScreenshotRemove = (file) => {
  form.value.screenshots = removeStoredUrl(form.value.screenshots, file)
}

const handleVideoRemove = (file) => {
  form.value.video = removeStoredUrl(form.value.video, file)
}

const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    form.value.icon = res.data
    imageUrl.value = replaceURL(res.data)
  }
}

const handleAvatarSuccessImage = (res, uploadFile) => {
  if (res.code === 200) {
    form.value.screenshots.push(res.data)
    uploadFile.url = replaceURL(res.data)
    ElMessage.success('上传成功')
  }
}

const handleAvatarSuccessVideo = (res, uploadFile) => {
  if (res.code === 200) {
    form.value.video.push(res.data)
    uploadFile.url = replaceURL(res.data)
    ElMessage.success('上传成功')
  }
}

const handleCurrentChange = (page) => {
  queryParams.value.pageNo = page
  getGameList()
}

const handleAdd = () => {
  dialogVisible.value = true
  isEdit.value = false
  resetForm()
}

const addGame = () => {
  const formData = {
    ...form.value,
    type: normalizeTypeValue(form.value.type),
    platforms: normalizePlatformValue(form.value.platforms),
    discount: form.value.discount || 0,
    priceMark: form.value.priceMark || null,
    releaseDate: form.value.releaseDate || null,
    screenshots: JSON.stringify(form.value.screenshots),
    video: JSON.stringify(form.value.video),
    systemRequirements: JSON.stringify(form.value.systemRequirements)
  }

  const url = isEdit.value ? '/game/update' : '/game/add'
  postRequest(url, formData).then(res => {
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '编辑游戏成功' : '新增游戏成功')
      dialogVisible.value = false
      getGameList()
      getGameTypes()
      getPlatformOptions()
    }
  })
}

const editGame = (game) => {
  resetForm()
  isEdit.value = true
  dialogVisible.value = true
  const screenshots = safeJsonParse(game.screenshots, [])
  const videos = safeJsonParse(game.video, [])
  form.value = {
    ...game,
    type: splitTypes(game.type),
    platforms: splitTypes(game.platforms),
    discount: game.discount || 0,
    priceMark: game.priceMark || '',
    releaseDate: formatDate(game.releaseDate),
    screenshots,
    video: videos,
    systemRequirements: game.systemRequirements || createEmptyRequirements()
  }
  imageUrl.value = game.icon ? replaceURL(game.icon) : ''
  fileList1.value = screenshots.map(url => ({
    name: url.split('/').pop(),
    url: replaceURL(url)
  }))
  fileList2.value = videos.map(url => ({
    name: url.split('/').pop(),
    url: replaceURL(url)
  }))
}

const deleteGame = (gameId) => {
  ElMessageBox.confirm('确认要删除该游戏吗？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRequest(`/game/delete/${gameId}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getGameList()
        getGameTypes()
      }
    })
  }).catch(() => {})
}

const resetForm = () => {
  form.value = createEmptyForm()
  imageUrl.value = ''
  fileList.value = []
  fileList1.value = []
  fileList2.value = []
  previewIndex.value = 0
}

const getPreviewType = (file) => {
  const rawValue = file?.name || file?.url || ''
  const fileType = rawValue.split('.').pop().toLowerCase()
  return ['mp4', 'webm', 'ogg'].includes(fileType) ? 'video' : 'image'
}

const openPreviewByIndex = (index) => {
  const files = previewFiles.value
  if (!files.length) {
    return
  }
  const safeIndex = (index + files.length) % files.length
  const file = files[safeIndex]
  previewIndex.value = safeIndex
  previewUrl.value = file.url
  previewType.value = getPreviewType(file)
}

const handlePreview = (file) => {
  const index = previewFiles.value.findIndex(item => item.uid === file.uid || item.url === file.url || item.name === file.name)
  openPreviewByIndex(index >= 0 ? index : 0)
  previewVisible.value = true
}

const switchPreview = (step) => {
  openPreviewByIndex(previewIndex.value + step)
}
</script>

<style lang="less" scoped>
.game-management {
  padding: 28px;
}

.game-table {
  :deep(.el-table__cell) {
    padding: 10px 0;
  }

  :deep(.cell) {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 24px;
  }

  :deep(.el-button + .el-button) {
    margin-left: 8px;
  }
}

.toolbar {
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-body {
  padding: 16px 24px;
}

.unit {
  margin-left: 10px;
  color: #606266;
}

.requirements-editor {
  width: 100%;
}

.requirement-block {
  border: 1px solid #dcdfe6;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.requirement-block h3 {
  margin: 0 0 14px;
  font-size: 15px;
}

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.iconWrapper {
  width: 78px;
  height: 78px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
}

.iconWrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.iconWrapper:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 78px;
  height: 78px;
  text-align: center;
}

.preview-body {
  position: relative;
  min-height: 220px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.preview-body img,
.preview-body video {
  max-width: 100%;
  max-height: 70vh;
}

.preview-switch {
  position: absolute;
  top: 50%;
  width: 42px;
  height: 42px;
  border: 0;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.55);
  color: #fff;
  font-size: 30px;
  line-height: 1;
  transform: translateY(-50%);
  cursor: pointer;
  z-index: 2;

  &:hover {
    background: rgba(17, 24, 39, 0.72);
  }

  &.left {
    left: 12px;
  }

  &.right {
    right: 12px;
  }
}

.preview-counter {
  position: absolute;
  left: 50%;
  bottom: 10px;
  transform: translateX(-50%);
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.62);
  color: #fff;
  font-size: 12px;
}
</style>
