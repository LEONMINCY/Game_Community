<!-- 文件说明：views/aiAssistant/index.vue，AI助手页面，管理对话历史、图片分析、聊天导出和清空。 -->
<template>
  <div class="ai-page">
    <aside class="history-panel">
      <div class="history-header">
        <h2>AI助手</h2>
        <el-button type="primary" size="small" @click="createConversation">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>

      <div class="conversation-list">
        <div
          v-for="item in conversations"
          :key="item.id"
          :class="['conversation-item', { active: item.id === activeConversationId }]"
          @click="selectConversation(item.id)"
        >
          <div class="conversation-main">
            <div class="conversation-title">{{ item.title || '新对话' }}</div>
            <div class="conversation-time">{{ item.updateTime || item.createTime }}</div>
          </div>
          <el-button
            class="conversation-delete"
            link
            type="danger"
            :icon="Delete"
            @click.stop="deleteConversation(item)"
          />
        </div>
        <el-empty v-if="!conversations.length" description="暂无历史对话" :image-size="90" />
      </div>
    </aside>

    <section class="chat-panel">
      <div class="chat-header">
        <div>
          <h3>{{ activeConversation?.title || '新对话' }}</h3>
          <span>游戏攻略、帖子创作、图片分析、推荐和社区问题都可以问我</span>
        </div>
        <div class="chat-actions">
          <el-button :disabled="!activeConversationId" @click="exportMessages">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
          <el-button :disabled="!activeConversationId" @click="clearConversation">
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
        </div>
      </div>

      <div ref="messageRef" class="message-list" v-loading="loadingHistory">
        <div v-if="!messages.length" class="welcome">
          <div class="welcome-title">你好，我是游戏社区AI助手</div>
          <div class="welcome-subtitle">可以帮你分析图片、整理攻略、润色帖子、推荐游戏，也能回答本站游戏和帖子数据问题。</div>
        </div>

        <div
          v-for="message in messages"
          :key="message.id || message.createTime + message.role"
          :class="['message-item', message.role]"
        >
          <div class="message-avatar">{{ message.role === 'assistant' ? 'AI' : '我' }}</div>
          <div class="message-body">
            <div v-if="messageImages(message).length" class="message-images">
              <el-image
                v-for="(image, index) in messageImages(message)"
                :key="image + index"
                :src="image"
                fit="cover"
                :preview-src-list="messageImages(message)"
                preview-teleported
              />
            </div>
            <div v-if="message.content" class="message-content">{{ message.content }}</div>
            <div class="message-time">{{ message.createTime }}</div>
          </div>
        </div>

        <div v-if="sending" class="message-item assistant">
          <div class="message-avatar">AI</div>
          <div class="message-body">
            <div class="message-content loading-text">正在思考...</div>
          </div>
        </div>
      </div>

      <div class="input-panel">
        <div v-if="selectedImages.length" class="selected-images">
          <div v-for="image in selectedImages" :key="image.id" class="selected-image">
            <img :src="image.url" :alt="image.name">
            <el-button class="remove-image" circle size="small" :icon="Close" @click="removeImage(image.id)" />
          </div>
        </div>

        <el-input
          v-model="inputValue"
          type="textarea"
          :rows="4"
          resize="none"
          placeholder="输入你想问的问题，也可以直接发图片让我分析..."
          @keydown.enter.exact.prevent="sendMessage"
        />
        <input
          ref="imageInputRef"
          class="hidden-file"
          type="file"
          accept="image/*"
          multiple
          @change="handleImageChange"
        >
        <div class="input-actions">
          <el-button :icon="Picture" @click="triggerImageSelect">图片</el-button>
          <el-button @click="clearInput">清除输入</el-button>
          <el-button type="primary" :loading="sending" :disabled="!canSend" @click="sendMessage">
            发送
          </el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Delete, Download, Picture, Plus } from '@element-plus/icons-vue'
import { deleteRequest, getRequest, postRequest } from '@/utils/http'

const conversations = ref([])
const messages = ref([])
const activeConversationId = ref(null)
const inputValue = ref('')
const selectedImages = ref([])
const sending = ref(false)
const loadingHistory = ref(false)
const messageRef = ref(null)
const imageInputRef = ref(null)

const activeConversation = computed(() =>
  conversations.value.find(item => item.id === activeConversationId.value)
)
const canSend = computed(() =>
  !sending.value && (inputValue.value.trim().length > 0 || selectedImages.value.length > 0)
)

onMounted(() => {
  loadConversations()
})

const loadConversations = () => {
  getRequest('/ai-assistant/conversations').then(res => {
    conversations.value = res.data || []
    if (!activeConversationId.value && conversations.value.length) {
      selectConversation(conversations.value[0].id)
    }
  })
}

const createConversation = () => {
  postRequest('/ai-assistant/conversations', { title: '新对话' }).then(res => {
    const conversation = res.data
    conversations.value.unshift(conversation)
    activeConversationId.value = conversation.id
    messages.value = []
  })
}

const selectConversation = (id) => {
  activeConversationId.value = id
  loadingHistory.value = true
  getRequest(`/ai-assistant/conversations/${id}/messages`).then(res => {
    messages.value = res.data || []
    scrollToBottom()
  }).finally(() => {
    loadingHistory.value = false
  })
}

const sendMessage = () => {
  const imageUrls = selectedImages.value.map(item => item.url)
  const rawContent = inputValue.value.trim()
  const content = rawContent || (imageUrls.length ? '请分析我发送的图片。' : '')
  if ((!content && !imageUrls.length) || sending.value) {
    return
  }

  messages.value.push({
    id: `temp-${Date.now()}`,
    role: 'user',
    content,
    imageUrls: JSON.stringify(imageUrls),
    createTime: new Date().toLocaleString()
  })
  inputValue.value = ''
  selectedImages.value = []
  scrollToBottom()

  sending.value = true
  postRequest('/ai-assistant/chat', {
    conversationId: activeConversationId.value,
    content,
    imageUrls
  }).then(res => {
    const data = res.data
    activeConversationId.value = data.conversation.id
    messages.value = data.messages || []
    upsertConversation(data.conversation)
    scrollToBottom()
  }).catch(() => {
    messages.value.push({
      id: `failed-${Date.now()}`,
      role: 'assistant',
      content: '消息发送失败，请稍后重试。',
      createTime: new Date().toLocaleString()
    })
    scrollToBottom()
  }).finally(() => {
    sending.value = false
  })
}

const triggerImageSelect = () => {
  imageInputRef.value?.click()
}

const handleImageChange = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) return
  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      ElMessage.warning('只能选择图片文件')
      continue
    }
    if (file.size > 6 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过 6MB，已跳过`)
      continue
    }
    if (selectedImages.value.length >= 4) {
      ElMessage.warning('一次最多发送 4 张图片')
      break
    }
    const url = await readFileAsDataUrl(file)
    selectedImages.value.push({
      id: `${Date.now()}-${Math.random()}`,
      name: file.name,
      url
    })
  }
  event.target.value = ''
}

const readFileAsDataUrl = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const removeImage = (id) => {
  selectedImages.value = selectedImages.value.filter(item => item.id !== id)
}

const messageImages = (message) => {
  if (!message || !message.imageUrls) {
    return []
  }
  if (Array.isArray(message.imageUrls)) {
    return message.imageUrls
  }
  try {
    const images = JSON.parse(message.imageUrls)
    return Array.isArray(images) ? images : []
  } catch (e) {
    return []
  }
}

const deleteConversation = (item) => {
  ElMessageBox.confirm(`确定要删除「${item.title || '新对话'}」吗？删除后不可恢复。`, '删除对话', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRequest(`/ai-assistant/conversations/${item.id}`).then(() => {
      conversations.value = conversations.value.filter(conversation => conversation.id !== item.id)
      if (activeConversationId.value === item.id) {
        const nextConversation = conversations.value[0]
        if (nextConversation) {
          selectConversation(nextConversation.id)
        } else {
          activeConversationId.value = null
          messages.value = []
        }
      }
      ElMessage.success('对话已删除')
    })
  }).catch(() => {})
}

const clearConversation = () => {
  if (!activeConversationId.value) {
    return
  }
  ElMessageBox.confirm('确定清空当前对话记录吗？', '清空对话', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    postRequest(`/ai-assistant/conversations/${activeConversationId.value}/clear`, {}).then(res => {
      messages.value = res.data || []
      const current = conversations.value.find(item => item.id === activeConversationId.value)
      if (current) {
        current.title = '新对话'
        current.updateTime = new Date().toLocaleString()
      }
      ElMessage.success('已清空')
    })
  })
}

const exportMessages = () => {
  if (!messages.value.length) {
    ElMessage.warning('当前对话没有可导出的记录')
    return
  }
  const title = activeConversation.value?.title || 'AI助手对话'
  const lines = messages.value.map(item => {
    const role = item.role === 'assistant' ? 'AI助手' : '我'
    const images = messageImages(item)
    const imageText = images.length ? `\n[图片 ${images.length} 张]` : ''
    return `[${item.createTime || ''}] ${role}\n${item.content || ''}${imageText}\n`
  })
  const blob = new Blob([`${title}\n\n${lines.join('\n')}`], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${sanitizeFilename(title)}.txt`
  link.click()
  URL.revokeObjectURL(url)
}

const upsertConversation = (conversation) => {
  const index = conversations.value.findIndex(item => item.id === conversation.id)
  if (index >= 0) {
    conversations.value.splice(index, 1)
  }
  conversations.value.unshift(conversation)
}

const sanitizeFilename = (name) => {
  return String(name || 'AI助手对话').replace(/[\\/:*?"<>|]/g, '_')
}

const clearInput = () => {
  inputValue.value = ''
  selectedImages.value = []
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageRef.value) {
      messageRef.value.scrollTop = messageRef.value.scrollHeight
    }
  })
}
</script>

<style lang="less" scoped>
.ai-page {
  height: 100%;
  display: flex;
  gap: 12px;
  min-height: 0;
}

.history-panel {
  width: 260px;
  flex: 0 0 260px;
  border-right: 1px solid #edf0f5;
  padding-right: 12px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.history-header {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  h2 {
    margin: 0;
    font-size: 18px;
    color: #1f2937;
  }
}

.conversation-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.conversation-item {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;

  &:hover {
    background: #f5f7fa;
  }

  &.active {
    background: #ecf5ff;
    border-color: #b3d8ff;
  }
}

.conversation-main {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  color: #303133;
  font-weight: 600;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-delete {
  flex: 0 0 auto;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.conversation-item:hover .conversation-delete,
.conversation-item.active .conversation-delete {
  opacity: 1;
}

.conversation-time {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #edf0f5;
  padding-bottom: 10px;
  gap: 12px;

  h3 {
    margin: 0 0 6px;
    color: #1f2937;
    font-size: 18px;
  }

  span {
    color: #909399;
    font-size: 13px;
  }
}

.chat-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  margin: 12px 0;
}

.welcome {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
  text-align: center;
}

.welcome-title {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}

.welcome-subtitle {
  font-size: 14px;
  color: #909399;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.user {
    flex-direction: row-reverse;

    .message-body {
      align-items: flex-end;
    }

    .message-content {
      background: #409eff;
      color: #fff;
      border-color: #409eff;
    }
  }
}

.message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #1f2937;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 34px;
}

.message-body {
  max-width: min(720px, 78%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-content {
  background: #fff;
  color: #303133;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-image {
    width: 126px;
    height: 86px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #edf0f5;
    background: #fff;
  }
}

.message-time {
  color: #a8abb2;
  font-size: 12px;
  margin-top: 2px;
}

.loading-text {
  color: #909399;
}

.input-panel {
  border-top: 1px solid #edf0f5;
  padding-top: 10px;
}

.selected-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.selected-image {
  position: relative;
  width: 88px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.remove-image {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  min-height: 20px;
}

.hidden-file {
  display: none;
}

.input-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 900px) {
  .ai-page {
    flex-direction: column;
  }

  .history-panel {
    width: 100%;
    flex-basis: 180px;
    border-right: none;
    border-bottom: 1px solid #edf0f5;
    padding-right: 0;
    padding-bottom: 8px;
  }

  .message-body {
    max-width: 86%;
  }
}
</style>
