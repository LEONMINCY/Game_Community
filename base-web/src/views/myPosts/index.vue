<!-- 文件说明：views/myPosts/index.vue，我的帖子页面，管理个人帖子、编辑删除和审核申诉。 -->
<template>
  <div class="my-posts">
    <div class="page-header">
      <h3>我的帖子</h3>
      <el-button type="primary" @click="router.push('/addForum')">发布帖子</el-button>
    </div>

    <el-form :model="queryParams" inline class="query-form">
      <el-form-item label="标题">
        <el-input v-model="queryParams.title" clearable placeholder="搜索帖子标题" @keyup.enter="getPostList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.auditStatus" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getPostList">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="posts" height="calc(100vh - 250px)" class="my-posts-table">
      <el-table-column type="expand">
        <template #default="scope">
          <div class="post-expand">
            <p><strong>拒绝原因：</strong>{{ scope.row.auditReason || '无' }}</p>
            <p><strong>申诉内容：</strong>{{ scope.row.appealContent || '无' }}</p>
            <p><strong>申诉回复：</strong>{{ scope.row.appealReply || '无' }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="gameName" label="游戏" min-width="120" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发布时间" width="145" show-overflow-tooltip />
      <el-table-column label="状态" width="150">
        <template #default="scope">
          <div class="status-cell">
            <el-tag :type="auditTagType(scope.row.auditStatus)" size="small">{{ auditText(scope.row.auditStatus) }}</el-tag>
            <el-tag :type="appealTagType(scope.row.appealStatus)" size="small">{{ appealText(scope.row.appealStatus) }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <div class="table-actions">
            <el-button type="primary" link @click="viewPost(scope.row)">查看</el-button>
            <el-button type="warning" link @click="editPost(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="deletePost(scope.row)">删除</el-button>
            <el-button
              v-if="canAppeal(scope.row)"
              type="primary"
              link
              @click="openAppealDialog(scope.row)"
            >申诉</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editDialogVisible" title="编辑帖子" width="760px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <div class="editor-container">
            <Toolbar
              style="border-bottom: 1px solid #ccc"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              mode="default"
            />
            <Editor
              style="height: 320px"
              v-model="form.content"
              :defaultConfig="editorConfig"
              mode="default"
              @onCreated="handleCreated"
            />
          </div>
        </el-form-item>
        <el-form-item label="游戏" prop="gameId">
          <el-select v-model="form.gameId" filterable placeholder="请选择游戏">
            <el-option v-for="game in gameList" :key="game.id" :label="game.name" :value="game.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面">
          <el-upload
            v-model:file-list="fileList"
            action="/api/file/upload"
            :headers="headers"
            list-type="picture-card"
            :limit="5"
            :on-success="handleImageSuccess"
            :on-remove="handleImageRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">提交审核</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="帖子详情" width="720px">
      <h3>{{ currentPost?.title }}</h3>
      <div class="detail-content" v-html="currentPost?.content"></div>
    </el-dialog>

    <el-dialog v-model="appealDialogVisible" title="帖子申诉" width="520px">
      <div class="reject-reason">拒绝原因：{{ currentPost?.auditReason || '无' }}</div>
      <el-input
        v-model="appealContent"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="请输入申诉说明"
      />
      <template #footer>
        <el-button @click="appealDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, shallowRef, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { getRequest, putRequest, deleteRequest, postRequest } from '@/utils/http'

const router = useRouter()
const posts = ref([])
const gameList = ref([])
const editDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const appealDialogVisible = ref(false)
const currentPost = ref(null)
const appealContent = ref('')
const formRef = ref(null)
const fileList = ref([])
const editorRef = shallowRef()
const headers = ref({
  Authorization: localStorage.getItem('token')
})

const queryParams = reactive({
  title: '',
  auditStatus: ''
})

const form = reactive({
  id: null,
  title: '',
  content: '',
  gameId: null,
  images: []
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  gameId: [{ required: true, message: '请选择游戏', trigger: 'change' }]
}

const toolbarConfig = { excludeKeys: [] }
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/file/upload',
      headers: {
        Authorization: localStorage.getItem('token')
      },
      fieldName: 'file',
      maxFileSize: 600 * 1024 * 1024,
      allowedFileTypes: ['image/*', 'image/webp'],
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data)
        } else {
          ElMessage.error('图片上传失败')
        }
      }
    },
    uploadVideo: {
      server: '/api/file/upload',
      headers: {
        Authorization: localStorage.getItem('token')
      },
      fieldName: 'file',
      maxFileSize: 600 * 1024 * 1024,
      allowedFileTypes: ['video/mp4', 'video/webm', 'video/ogg', 'image/webp'],
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data)
        } else {
          ElMessage.error('视频上传失败')
        }
      }
    }
  }
}

onMounted(() => {
  getPostList()
  getGameList()
})

onBeforeUnmount(() => {
  if (editorRef.value) {
    editorRef.value.destroy()
  }
})

const handleCreated = (editor) => {
  editorRef.value = editor
}

const getPostList = () => {
  const params = new URLSearchParams()
  if (queryParams.title) params.append('title', queryParams.title)
  if (queryParams.auditStatus) params.append('auditStatus', queryParams.auditStatus)
  const query = params.toString()
  getRequest('/post/my' + (query ? `?${query}` : '')).then(res => {
    if (res.code === 200) {
      posts.value = res.data || []
    }
  })
}

const getGameList = () => {
  getRequest('/game/listAll').then(res => {
    if (res.code === 200) {
      gameList.value = res.data
    }
  })
}

const resetQuery = () => {
  queryParams.title = ''
  queryParams.auditStatus = ''
  getPostList()
}

const editPost = (post) => {
  currentPost.value = post
  form.id = post.id
  form.title = post.title
  form.content = post.content
  form.gameId = post.gameId
  form.images = post.media ? JSON.parse(post.media) : []
  fileList.value = form.images.map(url => ({
    url,
    name: url.substring(url.lastIndexOf('/') + 1)
  }))
  editDialogVisible.value = true
}

const submitEdit = () => {
  formRef.value.validate(valid => {
    if (!valid) return
    putRequest('/post/my/update', {
      id: form.id,
      title: form.title,
      content: form.content,
      gameId: form.gameId,
      media: JSON.stringify(form.images)
    }).then(res => {
      if (res.code === 200) {
        ElMessage.success('已提交审核')
        editDialogVisible.value = false
        getPostList()
      }
    })
  })
}

const deletePost = (post) => {
  ElMessageBox.confirm('确认删除这篇帖子吗？', '提示', {
    type: 'warning'
  }).then(() => {
    deleteRequest('/post/my/delete/' + post.id).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        getPostList()
      }
    })
  }).catch(() => {})
}

const viewPost = (post) => {
  if ((post.auditStatus || 'approved') === 'approved') {
    router.push('/forum?id=' + post.id)
    return
  }
  currentPost.value = post
  detailDialogVisible.value = true
}

const canAppeal = (post) => {
  return post.auditStatus === 'rejected' && post.appealStatus !== 'pending'
}

const openAppealDialog = (post) => {
  currentPost.value = post
  appealContent.value = post.appealContent || ''
  appealDialogVisible.value = true
}

const submitAppeal = () => {
  if (!appealContent.value.trim()) {
    ElMessage.warning('请输入申诉说明')
    return
  }
  postRequest('/post/appeal/' + currentPost.value.id, {
    appealContent: appealContent.value
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('申诉已提交')
      appealDialogVisible.value = false
      getPostList()
    }
  })
}

const handleImageSuccess = (response, uploadFile) => {
  if (response.code === 200) {
    form.images.push(response.data)
    uploadFile.url = response.data
  }
}

const handleImageRemove = (file) => {
  form.images = form.images.filter(url => url !== file.url)
}

const auditText = (status) => ({
  pending: '待审核',
  approved: '已通过',
  rejected: '已拒绝'
}[status || 'approved'] || '已通过')

const auditTagType = (status) => ({
  pending: 'warning',
  approved: 'success',
  rejected: 'danger'
}[status || 'approved'] || 'success')

const appealText = (status) => ({
  none: '未申诉',
  pending: '申诉中',
  approved: '申诉通过',
  rejected: '申诉拒绝'
}[status || 'none'] || '未申诉')

const appealTagType = (status) => ({
  none: 'info',
  pending: 'warning',
  approved: 'success',
  rejected: 'danger'
}[status || 'none'] || 'info')
</script>

<style lang="less" scoped>
.my-posts {
  height: 100%;
  padding: 20px;
  box-sizing: border-box;

  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 18px;
    }
  }

  .query-form {
    margin-bottom: 8px;
  }

  .my-posts-table {
    width: 100%;
  }

  .status-cell {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-wrap: wrap;
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

  .post-expand,
  .reject-reason {
    color: #606266;
    line-height: 1.7;
  }

  .editor-container {
    width: 100%;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    overflow: hidden;
  }

  .detail-content {
    max-height: 520px;
    overflow-y: auto;
  }
}
</style>
