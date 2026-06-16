<!-- 文件说明：views/admin/news.vue，后台新闻管理页面，维护新闻内容并查看评论互动。 -->
<template>
  <div class="news-management">
    <el-form :model="queryParams" label-width="70px" :inline="true">
      <el-form-item label="标题">
        <el-input v-model="queryParams.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button icon="Refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" size="mini" @click="handleAdd">新增</el-button>
      </el-col>
    </el-row>

    <el-table :data="news" style="width: 100%">
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ formatContent(scope.row.content) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="时间" width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="230">
        <template #default="scope">
          <div class="table-actions">
            <el-button type="primary" link @click="openDetailDialog(scope.row)">查看</el-button>
            <el-button type="warning" link @click="editNews(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="deleteNews(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="totalNews"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑新闻' : '新增新闻'" width="60%">
      <div class="dialog-body">
        <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
          <el-form-item label="标题" prop="title">
            <el-input v-model="form.title" placeholder="请输入标题" />
          </el-form-item>
          <el-form-item label="内容" prop="content">
            <div class="editor-container">
              <Toolbar style="border-bottom: 1px solid #ccc" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
              <Editor
                style="height: 400px"
                v-model="form.content"
                :defaultConfig="editorConfig"
                mode="default"
                @onCreated="handleCreated"
              />
            </div>
          </el-form-item>
          <el-form-item label="图片">
            <el-upload
              action="/api/file/upload"
              :headers="headers"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :limit="1"
            >
              <div class="iconWrapper" v-if="!imageUrl">
                <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
              </div>
              <div v-else class="image-preview">
                <img :src="imageUrl" class="preview-image" alt="" />
                <div class="image-actions">
                  <el-icon @click.stop="handleRemoveImage"><Delete /></el-icon>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="addNews">确定</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="新闻详情" width="780px">
      <div v-if="detailNews" class="admin-news-detail">
        <div class="detail-head">
          <h2>{{ detailNews.title }}</h2>
          <span>{{ detailNews.createTime || '-' }}</span>
        </div>
        <el-image
          v-if="detailCover"
          class="detail-cover"
          :src="detailCover"
          fit="cover"
          preview-teleported
          :preview-src-list="[detailCover]"
        />
        <div class="detail-content" v-html="detailNews.content"></div>

        <div class="comment-section">
          <div class="section-title">评论区</div>
          <el-empty v-if="detailComments.length === 0" description="暂无评论" />
          <div v-else class="comment-list">
            <div v-for="comment in detailComments" :key="comment.id" class="comment-card">
              <div class="comment-main">
                <el-avatar :size="32" :src="replaceURL(comment.avatar || '')" />
                <div class="comment-body">
                  <div class="comment-meta">
                    <span>{{ comment.nickname || ('用户' + comment.userId) }}</span>
                    <small>{{ comment.createTime || '-' }}</small>
                  </div>
                  <div class="comment-text">{{ comment.content || '-' }}</div>
                  <el-image
                    v-if="comment.imageUrl"
                    class="comment-image"
                    :src="replaceURL(comment.imageUrl)"
                    fit="cover"
                    preview-teleported
                    :preview-src-list="[replaceURL(comment.imageUrl)]"
                  />
                  <div v-if="comment.children && comment.children.length" class="reply-list">
                    <div v-for="reply in comment.children" :key="reply.id" class="reply-card">
                      <strong>{{ reply.nickname || ('用户' + reply.userId) }}：</strong>{{ reply.content || '-' }}
                      <el-image
                        v-if="reply.imageUrl"
                        class="reply-image"
                        :src="replaceURL(reply.imageUrl)"
                        fit="cover"
                        preview-teleported
                        :preview-src-list="[replaceURL(reply.imageUrl)]"
                      />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, shallowRef, onBeforeUnmount } from 'vue'
import { postRequest, putRequest, deleteRequest, getRequest } from '../../utils/http'
import { ElMessage } from 'element-plus'
import { replaceURL } from '../../utils/tools'
import { Plus, Delete } from '@element-plus/icons-vue'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const headers = ref({ Authorization: localStorage.getItem('token') })
const imageUrl = ref('')
const currentPage = ref(1)
const totalNews = ref(0)
const queryParams = ref({ pageNo: 1, pageSize: 10, title: null })
const form = ref({ title: null, content: '', mediaJson: null })
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailNews = ref(null)
const detailCover = ref('')
const detailComments = ref([])
const news = ref([])
const editorRef = shallowRef()

const toolbarConfig = { excludeKeys: [] }
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/file/upload',
      headers: { Authorization: localStorage.getItem('token') },
      fieldName: 'file',
      onBeforeUpload(file) {
        return file
      },
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data)
        } else {
          ElMessage.error('图片上传失败')
        }
      }
    }
  }
}

onMounted(() => {
  getNewsList()
})

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
  }
})

const handleCreated = (editor) => {
  editorRef.value = editor
}

const formatContent = (content) => {
  if (!content) return ''
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content
  const text = tempDiv.textContent || tempDiv.innerText || ''
  return text.length > 100 ? text.slice(0, 100) + '...' : text
}

const search = () => {
  queryParams.value.pageNo = 1
  getNewsList()
}

const getNewsList = () => {
  postRequest('/news/page', queryParams.value).then(res => {
    if (res.code === 200) {
      news.value = res.data.list
      totalNews.value = res.data.total
    }
  })
}

const resetQuery = () => {
  queryParams.value = { pageNo: 1, pageSize: 10, title: null }
  getNewsList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  queryParams.value.pageNo = page
  getNewsList()
}

const handleAdd = () => {
  dialogVisible.value = true
  resetForm()
}

const handleAvatarSuccess = (res) => {
  if (res.code === 200) {
    form.value.mediaJson = res.data
    imageUrl.value = replaceURL(res.data)
  }
}

const handleRemoveImage = () => {
  form.value.mediaJson = null
  imageUrl.value = ''
}

const addNews = () => {
  const request = form.value.id ? putRequest('/news/update', form.value) : postRequest('/news/add', form.value)
  request.then(res => {
    if (res.code === 200) {
      ElMessage.success(form.value.id ? '编辑成功' : '新增成功')
      resetForm()
      getNewsList()
      dialogVisible.value = false
    }
  })
}

const editNews = (newsItem) => {
  resetForm()
  dialogVisible.value = true
  form.value = { ...newsItem }
  if (form.value.mediaJson) {
    imageUrl.value = parseNewsCover(form.value.mediaJson)
  }
}

const deleteNews = (newsId) => {
  deleteRequest('/news/delete/' + newsId).then(res => {
    if (res.code === 200) {
      ElMessage.success('删除成功')
      getNewsList()
    }
  })
}

const openDetailDialog = (newsItem) => {
  detailNews.value = newsItem
  detailCover.value = parseNewsCover(newsItem.mediaJson)
  detailComments.value = []
  detailDialogVisible.value = true
  loadNewsComments(newsItem.id)
}

const parseNewsCover = (mediaJson) => {
  if (!mediaJson) return ''
  try {
    const parsed = JSON.parse(mediaJson)
    return replaceURL(Array.isArray(parsed) ? parsed[0] : parsed)
  } catch (error) {
    return replaceURL(mediaJson)
  }
}

const loadNewsComments = (newsId) => {
  postRequest('/news/comment/list', { newsId, pageNo: 1, pageSize: 100 }).then(res => {
    if (res.code === 200) {
      detailComments.value = (res.data.list || []).map(item => ({ ...item, children: item.children || [] }))
      detailComments.value.forEach(comment => loadNewsReplies(comment))
    }
  }).catch(() => {
    ElMessage.error('评论加载失败')
  })
}

const loadNewsReplies = (comment) => {
  getRequest(`/news/comment/reply/list/${comment.id}?pageNo=1&pageSize=50`).then(res => {
    if (res.code === 200) {
      comment.children = res.data.list || []
    }
  })
}

const resetForm = () => {
  form.value = { title: null, content: '', mediaJson: null }
  imageUrl.value = ''
  if (editorRef.value) {
    editorRef.value.clear()
  }
}
</script>

<style scoped>
.news-management {
  padding: 40px;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: end;
}

.table-actions {
  display: inline-flex;
  gap: 8px;
}

.dialog-body {
  padding: 30px;
}

.editor-container {
  border: 1px solid #ccc;
  z-index: 100;
  width: 100%;
  margin-bottom: 20px;
  border-radius: 4px;
  overflow: hidden;
  box-sizing: border-box;
}

:deep(.w-e-text-container) {
  background-color: #fff;
}

:deep(.w-e-toolbar) {
  border-bottom: 1px solid #eee;
  background-color: #fafafa;
}

.iconWrapper {
  width: 78px;
  height: 78px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px dashed #d9d9d9;
}

.iconWrapper:hover {
  border-color: #409eff;
}

.image-preview {
  position: relative;
  width: 178px;
  height: 178px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-actions {
  position: absolute;
  top: 0;
  right: 0;
  display: none;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px;
  border-radius: 0 0 0 4px;
}

.image-preview:hover .image-actions {
  display: block;
}

.image-actions .el-icon {
  color: #fff;
  font-size: 18px;
  cursor: pointer;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}

.admin-news-detail {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 8px;
}

.detail-head h2 {
  margin: 0 0 8px;
  color: #1f2d3d;
  font-size: 22px;
}

.detail-head span {
  color: #909399;
  font-size: 13px;
}

.detail-cover {
  width: 220px;
  height: 132px;
  margin-top: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.detail-content {
  margin-top: 16px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 8px;
  line-height: 1.8;
  color: #303133;
}

.comment-section {
  margin-top: 18px;
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.section-title {
  margin-bottom: 12px;
  font-weight: 700;
  color: #1f2d3d;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.comment-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fff;
}

.comment-main {
  display: flex;
  gap: 10px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #909399;
  font-size: 12px;
}

.comment-text {
  margin-top: 6px;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.comment-image,
.reply-image {
  width: 96px;
  height: 72px;
  margin-top: 8px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.reply-list {
  margin-top: 8px;
  padding: 8px;
  background: #f7f9fc;
  border-radius: 6px;
}

.reply-card {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
</style>
