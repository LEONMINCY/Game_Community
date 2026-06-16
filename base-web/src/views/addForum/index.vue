<!-- 文件说明：views/addForum/index.vue，发布内容页面，负责帖子标题、富文本、游戏标签、话题和封面提交。 -->
<template>
  <div class="add-forum-container">
    <el-form 
      :model="formData" 
      :rules="rules" 
      ref="formRef"
      label-width="96px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入标题" />
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
            style="height: 400px"
            v-model="formData.content"
            :defaultConfig="editorConfig"
            mode="default"
            @onCreated="handleCreated"
          />
        </div>
      </el-form-item>

      <el-form-item label="游戏标签" prop="gameTag">
        <el-select
          v-model="formData.gameTag"
          placeholder="搜索游戏"
          filterable
          remote
          :remote-method="searchGames"
          :loading="searchLoading"
          clearable
        >
          <el-option
            v-for="game in searchResults"
            :key="game.id"
            :label="game.name"
            :value="game.id"
          >
            <game-tag :src="game.icon" :name="game.name" />
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item label="话题">
        <el-select
          v-model="formData.topics"
          multiple
          filterable
          allow-create
          default-first-option
          clearable
          placeholder="输入 #话题 后回车，可添加多个（非必填）"
        >
          <el-option
            v-for="topic in topicSuggestions"
            :key="topic"
            :label="'#' + topic"
            :value="topic"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="封面">
        <el-upload
          v-model:file-list="fileList"
          action="/api/file/upload"
          :headers="headers"
          list-type="picture-card"
          :limit="5"
          :on-change="handleImageChange"
          :on-success="handleImageSuccess"
          :on-remove="handleImageRemove"
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitForm">发布</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, shallowRef, onBeforeUnmount } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import GameTag from '@/components/gameTag.vue'
import { getRequest, postRequest } from '@/utils/http'
import { useRouter } from 'vue-router'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'

const router = useRouter()
const formRef = ref(null)
const fileList = ref([])
const headers = ref({
  Authorization: localStorage.getItem('token')
})

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef()

// 工具栏配置
const toolbarConfig = {
  excludeKeys: []
}

// 编辑器配置
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/file/upload',
      headers: {
        Authorization: localStorage.getItem('token')
      },
      // 自定义上传参数
      fieldName: 'file',
      maxFileSize: 600 * 1024 * 1024,
      allowedFileTypes: ['image/*', 'image/webp'],
      // 上传之前触发
      onBeforeUpload(file) {
        return file
      },
      // 自定义上传图片的数据格式
      customInsert(res, insertFn) {
        // 服务端返回成功后，把图片地址插入富文本编辑器。
        if (res.code === 200) {
          const url = res.data
          insertFn(url)
          // 不再将富文本中的图片添加到 formData.images
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
      onBeforeUpload(file) {
        return file
      },
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

// 组件销毁时，也及时销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const handleCreated = (editor) => {
  editorRef.value = editor
}

// 表单数据
const formData = reactive({
  title: '',
  content: '',
  gameTag: '',
  topics: [],
  images: [] // 只存储封面图片
})

// 表单验证规则
const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度在2到50个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, message: '内容长度至少10个字符', trigger: 'blur' }
  ],
  gameTag: [
    { required: true, message: '请选择游戏标签', trigger: 'change' }
  ]
}

// 游戏列表数据（示例数据，实际应该从API获取）
const gameList = ref([])

// 在 script setup 中添加搜索相关的变量和方法
const searchLoading = ref(false)
const searchResults = ref([])
const topicSuggestions = ref([])

// 搜索游戏
const searchGames = async (query) => {
  if (query !== '') {
    searchLoading.value = true
    try {
      const res = await getRequest('/game/listAll?keyword=' + query, {
        keyword: query
      })
      if (res.code === 200) {
        searchResults.value = res.data
      }
    } catch (error) {
      console.error('搜索游戏失败:', error)
    } finally {
      searchLoading.value = false
    }
  } else {
    searchResults.value = gameList.value
  }
}

// 修改 onMounted，初始化时加载所有游戏
onMounted(() => {
  getGameList()
  getTopicSuggestions()
})

// 修改获取游戏列表方法
const getGameList = () => {
  getRequest('/game/listAll').then(res => {
    if (res.code === 200) {
      gameList.value = res.data
      searchResults.value = res.data // 初始化搜索结果为所有游戏
    }
  })
}

const getTopicSuggestions = () => {
  getRequest('/post/hotTopics?limit=20').then(res => {
    if (res.code === 200) {
      topicSuggestions.value = (res.data || []).map(item => item.topic)
    }
  })
}

const normalizeTopics = () => {
  return [...new Set((formData.topics || [])
    .map(topic => String(topic).replace(/^#+/, '').trim())
    .filter(Boolean))]
}

// 处理封面图片上传成功
const handleImageSuccess = (response, uploadFile) => {
  console.log(response)
  if (response.code === 200) {
    formData.images.push(response.data)
    // 更新上传文件的 url
    uploadFile.url = response.data
  }
}

// 处理封面图片删除
const handleImageRemove = (file) => {
  // 从 formData.images 中移除对应的图片URL
  const index = formData.images.indexOf(file.url)
  if (index > -1) {
    formData.images.splice(index, 1)
  }
}

// 处理封面图片变化
const handleImageChange = (uploadFile) => {
  if (fileList.value.length > 5) {
    ElMessage.warning('最多只能上传5张封面图片')
    return
  }
}

// 提交表单
const submitForm = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate((valid) => {
    if (valid) {
      // 调用接口提交表单数据。
      console.log('提交的表单数据：', formData)

      postRequest('/post/add', {
        title: formData.title,
        content: formData.content,
        gameId: formData.gameTag,
        topics: normalizeTopics().join(','),
        media: JSON.stringify(formData.images)
      }).then(res => {
        console.log(res)
        if (res.code === 200) {
            resetForm()
            router.push('/my-posts')
            ElMessage.success('提交成功，等待管理员审核')
        }
      }).catch(err => {
        console.log(err)
        resetForm()
      })
    }
  })
}

// 重置表单
const resetForm = () => {
  if (!formRef.value) return
  formRef.value.resetFields()
  fileList.value = []
  formData.topics = []
  formData.images = [] // 清空封面图片数组
}
</script>

<style lang="less" scoped>
.add-forum-container {
  max-width: 1080px;
  width: 95%;
  margin: 20px auto;
  padding: 24px 32px;
  background: #fff;
  border-radius: 8px;
  box-sizing: border-box;
  overflow: auto;
  height: inherit;

  :deep(.el-form) {
    width: 100%;
    box-sizing: border-box;
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;
    width: 100%;
    box-sizing: border-box;
  }

  :deep(.el-form-item__label) {
    width: 96px !important;
    padding-right: 14px;
    justify-content: flex-end;
    white-space: nowrap;
    word-break: keep-all;
    box-sizing: border-box;
  }

  :deep(.el-form-item__content) {
    margin-left: 96px !important;
    margin-right: 0;
    width: auto;
    min-width: 0;
    box-sizing: border-box;
  }

  :deep(.el-input) {
    width: 100%;
    max-width: 100%;
  }

  :deep(.el-select) {
    width: 100%;
    max-width: 100%;
  }

  :deep(.el-select-dropdown__item) {
    padding: 0 20px;
  }

  .editor-container {
    border: 1px solid #ccc;
    z-index: 100;
    width: 100%;
    margin-bottom: 20px;
    border-radius: 4px;
    overflow: hidden;
    box-sizing: border-box;

    :deep(.w-e-text-container) {
      background-color: #fff;
    }

    :deep(.w-e-toolbar) {
      border-bottom: 1px solid #eee;
      background-color: #fafafa;
    }

    :deep(.w-e-text-placeholder) {
      color: #999;
    }

    :deep(.w-e-text-container [data-slate-editor]) {
      padding: 0 10px;
    }
  }

  :deep(.el-upload--picture-card) {
    --el-upload-picture-card-size: 100px;
    margin: 0 8px 8px 0;
  }

  :deep(.el-upload-list--picture-card) {
    --el-upload-list-picture-card-size: 100px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    
    .el-upload-list__item {
      margin: 0;
    }
  }
}

@media screen and (max-width: 768px) {
  .add-forum-container {
    width: 100%;
    margin: 0;
    padding: 10px;
    border-radius: 0;

    :deep(.el-form-item__label) {
      width: 60px !important;
      padding-right: 8px;
    }

    :deep(.el-form-item__content) {
      margin-left: 60px !important;
    }

    .editor-container {
      margin: 0;
    }
  }
}
</style>
