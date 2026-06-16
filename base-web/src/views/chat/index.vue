<!-- 文件说明：views/chat/index.vue，前台私信页面，展示会话列表、聊天记录、表情、图片和表情包发送。 -->
<template>
  <div class="app-container">
    <div class="chat-layout">
      <!-- 左侧聊天列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <h3>聊天列表</h3>
        </div>
        <div class="chat-list-wrapper">
          <div v-for="(chat, index) in chatHistoryList" 
               :key="index"
               :class="['chat-list-item', { active: isActiveChat(chat.userId) }]"
               @click="switchChat(chat.userId)">
            <div class="chat-list-avatar" title="查看主页" @click.stop="viewUser(chat.userId)">
              <el-avatar :size="40" :src="chat.avatar || imageFallbackURL()" />
            </div>
            <div class="chat-list-info">
              <div class="chat-list-name">{{ chat.nickname || '用户' + chat.userId }}</div>
              <div class="chat-list-message">{{ chat.lastMessage }}</div>
            </div>
            <div class="chat-list-side">
              <div class="chat-list-time">{{ formatChatListTime(chat.lastTime) }}</div>
              <span v-if="Number(chat.unreadCount) > 0" class="chat-unread-count">
                {{ formatUnreadCount(chat.unreadCount) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <template v-if="otherUserId">
          <div class="headerWrapper">
            <div class="back" @click="goBack()">
              <el-icon><ArrowLeftBold /></el-icon>
            </div>
            <div class="theNameOfTheOtherParty">
              {{ otherUserName }}
            </div>
          </div>
          <div class="chatWrapper" ref="chatRef">
            <div class="chatList">
              <div v-for="(item, index) in chatList" :key="index + '1'">
                <div class="item" :class="item.myself ? 'myself' : ''">
                  <div class="itemAvatar">
                    <el-avatar :size="35" :src="(item.myself ? myAvatar : avatar) || imageFallbackURL()" />
                  </div>
                  <div class="itemContent">
                    <div class="itemInfo" :class="messageView(item.content).type">
                      <template v-if="messageView(item.content).type === 'image' || messageView(item.content).type === 'sticker'">
                        <el-image
                          :src="messageView(item.content).url"
                          :fit="messageView(item.content).type === 'sticker' ? 'contain' : 'cover'"
                          preview-teleported
                          :preview-src-list="[messageView(item.content).url]"
                          :class="messageView(item.content).type === 'sticker' ? 'sticker-img' : 'chat-img'"
                        >
                          <template #error>
                            <div class="chat-image-fallback">图片已失效</div>
                          </template>
                        </el-image>
                      </template>
                      <template v-else-if="messageView(item.content).type === 'post_share'">
                        <div class="post-share-card" @click="openPost(messageView(item.content).postId)">
                          <div class="post-share-icon">帖</div>
                          <div class="post-share-body">
                            <div class="post-share-title">{{ messageView(item.content).title }}</div>
                            <div class="post-share-meta">{{ messageView(item.content).gameName || '社区帖子' }}</div>
                          </div>
                        </div>
                      </template>
                      <template v-else>
                        {{ messageView(item.content).text }}
                      </template>
                    </div>
                    <div class="itemTime">
                      {{item.time}}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="inputWrapper">
            <div class="toolbar" @click.stop>
              <el-button text circle title="表情" @click="toggleEmojiPanel">
                <el-icon><Sunny /></el-icon>
              </el-button>
              <el-upload
                action="/api/file/upload"
                :headers="headers"
                :show-file-list="false"
                accept="image/*"
                :on-success="handleImageUploadSuccess"
              >
                <el-button text circle title="发送图片">
                  <el-icon><Picture /></el-icon>
                </el-button>
              </el-upload>
              <el-button text circle title="自定义表情包" @click="toggleStickerPanel">
                <el-icon><MagicStick /></el-icon>
              </el-button>
            </div>

            <div v-if="showEmojiPicker" class="emoji-panel" @click.stop>
              <span
                v-for="emoji in emojiList"
                :key="emoji"
                class="emoji-item"
                @click="insertEmoji(emoji)"
              >
                {{ emoji }}
              </span>
            </div>

            <div v-if="showStickerPanel" class="sticker-panel" @click.stop>
              <div class="sticker-grid">
                <div
                  v-for="sticker in customStickers"
                  :key="sticker"
                  class="sticker-item"
                  @click="sendMediaMessage('sticker', sticker)"
                >
                  <img :src="normalizeMediaUrl(sticker)" alt="sticker" @error="handleImageError" />
                  <el-icon class="remove-sticker" @click.stop="removeSticker(sticker)"><Close /></el-icon>
                </div>
                <el-upload
                  action="/api/file/upload"
                  :headers="headers"
                  :show-file-list="false"
                  accept="image/*"
                  :on-success="handleStickerUploadSuccess"
                >
                  <div class="sticker-add">
                    <el-icon><Plus /></el-icon>
                  </div>
                </el-upload>
              </div>
            </div>

            <textarea ref="textareaRef" :maxlength="messageMaxLength" @keydown.enter.exact.prevent="sendMessage" v-model="inputValue" @input="updateWordCount" placeholder="请输入..." />
            <div class="chatTool">
              <div class="wordCountDisplay">
                {{currentCount}} / {{messageMaxLength}}
              </div>
              <div class="send">
                <el-button type="success" size="small" @click="sendMessage">发 送</el-button>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="empty-chat">
            <el-empty description="请选择一个聊天" />
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onActivated, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store'
import { getRequest, postRequest } from '@/utils/http'
import { emojiList } from '@/utils/emoji'
import { handleImageError, imageFallbackURL, replaceURL } from '@/utils/tools'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const otherUserId = ref('')
const otherUserName = ref('')
const userId = ref('123')
const chatList = ref([])
const avatar = ref('')
const myAvatar = ref('')
// 定义输入内容的响应式变量
const inputValue = ref('');
// 定义当前字数的响应式变量
const currentCount = ref(0);
const textareaRef = ref(null);
const chatRef = ref(null);
const chatHistoryList = ref([])
const showEmojiPicker = ref(false)
const showStickerPanel = ref(false)
const customStickers = ref([])
const messageMaxLength = 2000
const headers = ref({
  Authorization: localStorage.getItem('token')
})

const getCurrentUserId = () => userStore.userInfo?.userId || localStorage.getItem('userId') || sessionStorage.getItem('userId') || ''
const stickerStorageKey = (id = userId.value) => `chat_custom_stickers_${id || 'default'}`

//设置socket
let ws = null

onMounted(() => {
  userId.value = getCurrentUserId()
  myAvatar.value = normalizeMediaUrl(userStore.userInfo?.avatar)
  loadCustomStickers()
  
  // 获取聊天列表
  getChatList()
  initWebSocket()
  document.addEventListener('click', closePanels)
})

onActivated(() => {
  userId.value = getCurrentUserId()
  myAvatar.value = normalizeMediaUrl(userStore.userInfo?.avatar)
  loadCustomStickers()
  getChatList()
  if (otherUserId.value) {
    getMessageHistory()
  }
})

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
  document.removeEventListener('click', closePanels)
})

// 初始化WebSocket连接
const initWebSocket = () => {
  ws = new WebSocket(buildWebSocketUrl(userId.value))

  ws.onopen = () => {
    console.log('连接成功')
  }

  ws.onmessage = (event) => {
    if (event.data === 'false') {
      ElMessage.error('当前用户不在线，无法收到消息')
      return
    }
    let data = null
    try {
      data = JSON.parse(event.data)
    } catch (error) {
      console.warn('invalid websocket message', event.data)
      return
    }
    if (data.type === 'notification') {
      return
    }
    const currentUserId = String(userId.value)
    const senderId = String(data.senderId)
    const targetUserId = senderId === currentUserId ? data.receiverId : data.senderId
    const time = data.createTime || new Date().toISOString()
    const isIncoming = senderId !== currentUserId
    const isCurrentChat = String(targetUserId) === String(otherUserId.value)
    updateChatPreview(targetUserId, data.content, time, isIncoming && !isCurrentChat)
    if (isCurrentChat) {
      chatList.value.push({
        userId: data.senderId,
        content: data.content,
        time,
        myself: senderId === currentUserId
      })
      if (isIncoming) {
        markConversationRead(targetUserId)
        clearChatUnread(targetUserId)
      }
      scrollToBottom()
    }
  }

  ws.onerror = () => {
    ElMessage.error('连接错误')
  }
}

// 初始化聊天
const initChat = () => {
  if (otherUserId.value) {
    getOtherUserInfo()
    getMessageHistory()
    if (textareaRef.value) {
      textareaRef.value.focus()
    }
  }
}

//返回
const goBack = () => {
  router.back()
}

const buildWebSocketUrl = (id) => {
  if (import.meta.env.VITE_WS_URL) {
    return `${import.meta.env.VITE_WS_URL}?userId=${id}`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.hostname || 'localhost'
  const port = window.location.port && window.location.port !== '9999' ? ':9999' : (window.location.port ? `:${window.location.port}` : '')
  return `${protocol}://${host}${port}/ws?userId=${id}`
}

const viewUser = (targetUserId) => {
  if (!targetUserId) {
    return
  }
  const path = String(targetUserId) === String(userId.value) ? '/userinfo' : `/userinfo?userId=${targetUserId}`
  router.push(path)
}

//查询他人用户信息
const getOtherUserInfo = () => {
  getRequest('/user-info/get-real/' + otherUserId.value).then(res => {
    if (res.code === 200) {
      console.log(res.data, 'getOtherUserInfo')
      avatar.value = normalizeMediaUrl(res.data.avatar)
      otherUserName.value = res.data.nickname
    }
  })
}


const getMessageHistory = () => {
  getRequest('/message/list/' + otherUserId.value).then(res => {
    console.log(res, 'getMessageHistory')
    if (res.code === 200) {
      chatList.value = res.data.map(item => ({
        userId: item.senderId,
        content: item.content,
        time: item.createTime,
        myself: String(item.senderId) === String(userId.value)
      }))
      markConversationRead(otherUserId.value)
      clearChatUnread(otherUserId.value)
      scrollToBottom()
    }
  })
}

const updateWordCount = () => {
  // 更新当前字数变量的值
  currentCount.value = inputValue.value.length
};

const sendMessage = () => {
  const content = inputValue.value.trim()
  if (content) {
    sendSocketMessage(content)
    inputValue.value = ''
    currentCount.value = 0
  }
  setTimeout(() => {
    textareaRef.value.selectionStart = 0
    textareaRef.value.selectionEnd = 0
    textareaRef.value.focus()
  }, 0)
};

const sendMediaMessage = (type, url) => {
  const mediaUrl = normalizeMediaUrl(url)
  if (!mediaUrl) {
    return
  }
  sendSocketMessage(JSON.stringify({ type, url: mediaUrl }))
  showEmojiPicker.value = false
  showStickerPanel.value = false
}

const sendSocketMessage = (content) => {
  if (!otherUserId.value) {
    ElMessage.warning('请选择聊天对象')
    return
  }
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    ElMessage.error('聊天连接未建立，请稍后再试')
    return
  }

  const time = new Date().toLocaleString()
  chatList.value.push({
    userId: userId.value,
    content,
    time,
    myself: true
  })
  ws.send(JSON.stringify({
    senderId: userId.value,
    content,
    receiverId: otherUserId.value
  }))
  updateChatPreview(otherUserId.value, content, time)
  scrollToBottom()
}

const normalizeMediaUrl = (url) => {
  if (!url) {
    return ''
  }
  let value = String(url).trim().replace(/\\/g, '/')
  if (value.startsWith('data:') || value.startsWith('blob:')) {
    return value
  }
  if (/^[a-zA-Z]:\//.test(value)) {
    const fileName = value.slice(value.lastIndexOf('/') + 1)
    return fileName ? `/noLogin/common/img/${encodeURIComponent(fileName)}` : ''
  }
  if (!value.includes('/') && /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(value)) {
    return `/noLogin/common/img/${encodeURIComponent(value)}`
  }
  return replaceURL(value)
}

const parseMessageContent = (content) => {
  if (!content) {
    return ''
  }
  if (typeof content === 'object') {
    return content
  }
  try {
    const parsed = JSON.parse(content)
    if (typeof parsed === 'string') {
      try {
        return JSON.parse(parsed)
      } catch (error) {
        return parsed
      }
    }
    return parsed
  } catch (error) {
    return content
  }
}

const looksLikeImageUrl = (value) => {
  const text = String(value || '')
  return text.startsWith('data:image/')
    || text.includes('/noLogin/common/img/')
    || text.includes('/files/')
    || text.includes('/uploads/')
    || /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(text)
}

const messageView = (content) => {
  if (!content) {
    return { type: 'text', text: '' }
  }
  const data = parseMessageContent(content)
  if (data && typeof data === 'object') {
    const mediaUrl = data.url || data.imageUrl || data.src || data.content
    if (mediaUrl && (data.type === 'image' || data.type === 'sticker' || looksLikeImageUrl(mediaUrl))) {
      return {
        type: data.type === 'sticker' ? 'sticker' : 'image',
        url: normalizeMediaUrl(mediaUrl)
      }
    }
    if (data.type === 'post_share' && data.postId) {
      return {
        type: 'post_share',
        postId: data.postId,
        title: data.title || '帖子',
        gameName: data.gameName || ''
      }
    }
    return { type: 'text', text: data.text || data.content || '' }
  }
  if (looksLikeImageUrl(data)) {
    return { type: 'image', url: normalizeMediaUrl(data) }
  }
  return { type: 'text', text: data }
}

const previewMessage = (content) => {
  const view = messageView(content)
  if (view.type === 'image') {
    return '[图片]'
  }
  if (view.type === 'sticker') {
    return '[表情包]'
  }
  if (view.type === 'post_share') {
    return `[帖子] ${view.title}`
  }
  return view.text
}

const formatUnreadCount = (count) => {
  const value = Number(count) || 0
  return value > 99 ? '99+' : String(value)
}

// 聊天列表只展示到分钟，避免 ISO 时间里的 T 和秒数挤压昵称。
const formatChatListTime = (value) => {
  if (!value) {
    return ''
  }
  const text = String(value).trim().replace('T', ' ')
  const matched = text.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})\s+(\d{1,2}):(\d{1,2})/)
  if (matched) {
    const [, year, month, day, hour, minute] = matched
    const monthDay = `${month.padStart(2, '0')}-${day.padStart(2, '0')}`
    const time = `${hour.padStart(2, '0')}:${minute.padStart(2, '0')}`
    return Number(year) === new Date().getFullYear() ? `${monthDay} ${time}` : `${year}-${monthDay}`
  }
  const date = new Date(text)
  if (!Number.isNaN(date.getTime())) {
    const pad = (num) => String(num).padStart(2, '0')
    const monthDay = `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`
    return date.getFullYear() === new Date().getFullYear() ? `${monthDay} ${time}` : `${date.getFullYear()}-${monthDay}`
  }
  return text.replace(/:\d{2}(\.\d+)?$/, '')
}

const openPost = (postId) => {
  if (postId) {
    router.push('/forum?id=' + postId)
  }
}

const scrollToBottom = () => {
  setTimeout(() => {
    if (chatRef.value) {
      chatRef.value.scrollTop = chatRef.value.scrollHeight
    }
  }, 0)
}

const loadChatUserInfo = (chat) => {
  if (!chat || !chat.userId) {
    return
  }
  getRequest('/user-info/get-real/' + chat.userId).then(userRes => {
    if (userRes.code === 200) {
      chat.nickname = userRes.data.nickname
      chat.avatar = normalizeMediaUrl(userRes.data.avatar)
    }
  })
}

const emitUnreadChange = (delta = 0) => {
  window.dispatchEvent(new CustomEvent('message-unread-change', {
    detail: { delta }
  }))
}

const clearChatUnread = (targetUserId) => {
  const chat = chatHistoryList.value.find(item => String(item.userId) === String(targetUserId))
  if (chat) {
    const previousCount = Number(chat.unreadCount) || 0
    chat.unreadCount = 0
    if (previousCount > 0) {
      emitUnreadChange(-previousCount)
    }
  }
}

const markConversationRead = (targetUserId) => {
  if (!targetUserId) {
    return
  }
  postRequest(`/message/read/${targetUserId}`, {}, { silentError: true })
    .then(() => emitUnreadChange())
    .catch(() => {})
}

const updateChatPreview = (targetUserId, content, time, increaseUnread = false) => {
  const chat = chatHistoryList.value.find(item => String(item.userId) === String(targetUserId))
  if (chat) {
    chat.lastMessage = previewMessage(content)
    chat.lastTime = time
    if (increaseUnread) {
      chat.unreadCount = (Number(chat.unreadCount) || 0) + 1
      emitUnreadChange(1)
    }
    chatHistoryList.value = [
      chat,
      ...chatHistoryList.value.filter(item => String(item.userId) !== String(targetUserId))
    ]
    return
  }

  const newChat = {
    userId: targetUserId,
    nickname: String(targetUserId) === String(otherUserId.value) ? otherUserName.value : '\u7528\u6237' + targetUserId,
    avatar: String(targetUserId) === String(otherUserId.value) ? avatar.value : '',
    lastMessage: previewMessage(content),
    lastTime: time,
    unreadCount: increaseUnread ? 1 : 0
  }
  chatHistoryList.value.unshift(newChat)
  loadChatUserInfo(newChat)
  if (increaseUnread) {
    emitUnreadChange(1)
  }
}

const closePanels = () => {
  showEmojiPicker.value = false
  showStickerPanel.value = false
}

const toggleEmojiPanel = () => {
  showEmojiPicker.value = !showEmojiPicker.value
  showStickerPanel.value = false
}

const toggleStickerPanel = () => {
  showStickerPanel.value = !showStickerPanel.value
  showEmojiPicker.value = false
}

const insertEmoji = (emoji) => {
  const textarea = textareaRef.value
  const start = textarea?.selectionStart ?? inputValue.value.length
  const end = textarea?.selectionEnd ?? inputValue.value.length
  inputValue.value = inputValue.value.slice(0, start) + emoji + inputValue.value.slice(end)
  updateWordCount()
  setTimeout(() => {
    if (textareaRef.value) {
      textareaRef.value.selectionStart = start + emoji.length
      textareaRef.value.selectionEnd = start + emoji.length
      textareaRef.value.focus()
    }
  }, 0)
}

const handleImageUploadSuccess = (response) => {
  if (response.code === 200 && response.data) {
    sendMediaMessage('image', normalizeMediaUrl(response.data))
    return
  }
  ElMessage.error(response.msg || '图片上传失败')
}

const handleStickerUploadSuccess = (response) => {
  if (response.code === 200 && response.data) {
    const stickerUrl = normalizeMediaUrl(response.data)
    if (!customStickers.value.includes(stickerUrl)) {
      customStickers.value.unshift(stickerUrl)
      saveCustomStickers()
    }
    ElMessage.success('表情包已添加')
    return
  }
  ElMessage.error(response.msg || '表情包上传失败')
}

const loadCustomStickers = () => {
  const keys = [
    stickerStorageKey(),
    'chat_custom_stickers_default',
    'chat_custom_stickers_undefined',
    'chat_custom_stickers_null',
    'chat_custom_stickers_'
  ]
  const stickers = []
  keys.forEach(key => {
    try {
      const rawValue = localStorage.getItem(key)
      const parsed = JSON.parse(rawValue || '[]')
      const list = Array.isArray(parsed) ? parsed : [parsed]
      list.forEach(item => {
        const url = normalizeMediaUrl(typeof item === 'string' ? item : item?.url || item?.src)
        if (url && !stickers.includes(url)) {
          stickers.push(url)
        }
      })
    } catch (error) {
      // 忽略旧版本写入的不规范表情缓存，避免影响聊天页加载。
    }
  })
  customStickers.value = stickers
  saveCustomStickers()
}

const saveCustomStickers = () => {
  localStorage.setItem(stickerStorageKey(), JSON.stringify(customStickers.value))
}

const removeSticker = (sticker) => {
  customStickers.value = customStickers.value.filter(item => item !== sticker)
  saveCustomStickers()
}

// 获取聊天列表
const getChatList = () => {
  getRequest('/message/chat-list').then(res => {
    if (res.code === 200) {
      chatHistoryList.value = res.data.map(chat => ({
        ...chat,
        lastMessage: previewMessage(chat.lastMessage),
        unreadCount: Number(chat.unreadCount) || 0
      }))
      // 获取每个用户的基本信息
      chatHistoryList.value.forEach(chat => {
        loadChatUserInfo(chat)
      })
      
      // 如果是从其他页面跳转过来的，设置当前选中的用户
      if (route.query.userId) {
        otherUserId.value = parseInt(route.query.userId)
        initChat()
      }
    }
  })
}

// 切换聊天对象
const isActiveChat = (chatUserId) => String(chatUserId) === String(otherUserId.value)

const switchChat = (userId) => {
  otherUserId.value = userId
  showEmojiPicker.value = false
  showStickerPanel.value = false
  initChat()
}

</script>

<style lang="less" scoped>
.app-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.chat-layout {
  display: flex;
  width: 100%;
  height: 100%;
}

.chat-sidebar {
  width: 300px;
  height: 100%;
  border-right: 1px solid #e5e5e5;
  background: #fff;

  .sidebar-header {
    height: 50px;
    padding: 0 20px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5e5e5;

    h3 {
      margin: 0;
      font-size: 16px;
      color: #333;
    }
  }

  .chat-list-wrapper {
    height: calc(100% - 50px);
    overflow-y: auto;

    &::-webkit-scrollbar {
      width: 0;
      height: 0;
    }
  }

  .sidebar-tabs {
    display: flex;
    gap: 6px;
    padding: 8px 12px;
    border-bottom: 1px solid #f0f2f5;

    button {
      flex: 1;
      height: 32px;
      border: 0;
      border-radius: 6px;
      background: #f5f7fa;
      color: #606266;
      cursor: pointer;

      &.active {
        background: #ecf5ff;
        color: #409eff;
        font-weight: 600;
      }
    }
  }

  .report-progress-wrapper {
    height: calc(100% - 98px);
    overflow-y: auto;
    padding: 12px;
    box-sizing: border-box;
    background: #f7f9fc;
  }

  .report-empty {
    padding-top: 60px;
  }

  .report-progress-card {
    padding: 12px;
    margin-bottom: 12px;
    border-radius: 8px;
    background: #fff;
    border: 1px solid #edf0f5;
    box-shadow: 0 4px 12px rgba(30, 42, 60, 0.04);
  }

  .report-card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    color: #9aa3af;
    font-size: 12px;
  }

  .report-card-title {
    margin-top: 10px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .report-card-comment,
  .report-card-reason,
  .report-card-reply {
    margin-top: 8px;
    color: #606266;
    font-size: 13px;
    line-height: 1.5;
  }

  .report-card-comment {
    padding: 8px;
    background: #f8fafc;
    border-radius: 6px;
    color: #4b5563;
  }

  .chat-list-item {
    padding: 15px 14px;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s;
    border-bottom: 1px solid #f5f5f5;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #ecf5ff;
    }

    .chat-list-avatar {
      flex: 0 0 40px;
      cursor: pointer;
      transition: transform 0.2s;

      &:hover {
        transform: scale(1.04);
      }
    }

    .chat-list-info {
      flex: 1;
      min-width: 0;
      margin: 0 10px;
      overflow: hidden;

      .chat-list-name {
        font-size: 14px;
        color: #333;
        margin-bottom: 5px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .chat-list-message {
        font-size: 12px;
        color: #999;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    .chat-list-time {
      font-size: 12px;
      color: #999;
      text-align: right;
      white-space: nowrap;
    }

    .chat-list-side {
      flex: 0 0 82px;
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      gap: 6px;
    }

    .chat-unread-count {
      min-width: 18px;
      height: 18px;
      padding: 0 5px;
      border-radius: 999px;
      background: #f56c6c;
      color: #fff;
      font-size: 12px;
      line-height: 18px;
      text-align: center;
      box-sizing: border-box;
      font-weight: 700;
    }
  }
}

.chat-main {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;

  .headerWrapper {
    background: #fff;
  }

  .chatWrapper {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    scrollbar-gutter: stable;
  }

  .inputWrapper {
    background: #fff;
  }
}

.app-container {
  width: 100%;
  height: 100%;
  position: relative;

  .headerWrapper {
    width: 100%;
    height: 50px;
    position: relative;
    display: flex;
    justify-content: center;
    align-items: center;
    //border-bottom: 1px solid #d1d1d1;

    .back {
      position: absolute;
      left: 15px;
      top: 25px;
      transform: translateY(-50%);
      cursor: pointer;
    }

  }


  .chatWrapper {
    width: 100%;
    height: calc(100% - 250px);
    background-color: #f7f8f9;
    border: 1px solid #e5e5e5;
    border-radius: 5px;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #c4ccd8 transparent;

    &::-webkit-scrollbar {
      width: 6px;
      height: 0;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: #c4ccd8;
      border-radius: 999px;
    }

    &::-webkit-scrollbar-thumb:hover {
      background: #9aa6b6;
    }


    .chatList {
      width: 100%;
      padding: 15px;
      box-sizing: border-box;

      .item {
        display: flex;
        margin-bottom: 15px;
      }

      .myself{
        flex-direction: row-reverse;

        .itemContent {
          margin-left: 0 !important;
          margin-right: 15px !important;
          align-items: flex-end;
        }
      }

      .itemContent {
        margin-left: 15px;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
      }

      .itemInfo {
        padding: 0 10px;
        background-color: #80b9f2;
        color: #fff;
        line-height: 35px;
        font-size: 14px;
        border-radius: 5px;
        max-width: 300px;
        word-break: break-all;

        &.image,
        &.sticker,
        &.post_share {
          padding: 0;
          background: transparent;
          color: inherit;
          line-height: 1;
          max-width: 220px;
          overflow: hidden;
        }

        &.post_share {
          max-width: 280px;
        }

        .chat-img {
          display: block;
          width: 180px;
          max-width: 100%;
          max-height: 180px;
          border-radius: 6px;
          background: #fff;
        }

        .sticker-img {
          display: block;
          width: 108px;
          height: 108px;
          border-radius: 6px;
          background: transparent;
        }

        .chat-image-fallback {
          width: 180px;
          height: 120px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 6px;
          background: #eef2f7;
          color: #8b95a5;
          font-size: 13px;
          border: 1px solid #dfe5ee;
          box-sizing: border-box;
        }

        .post-share-card {
          display: flex;
          align-items: center;
          gap: 10px;
          width: 260px;
          padding: 12px;
          border-radius: 8px;
          background: #fff;
          border: 1px solid #e5e7eb;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          cursor: pointer;
          box-sizing: border-box;

          &:hover {
            border-color: #409eff;
          }
        }

        .post-share-icon {
          width: 38px;
          height: 38px;
          border-radius: 8px;
          background: #ecf5ff;
          color: #409eff;
          display: flex;
          align-items: center;
          justify-content: center;
          font-weight: 700;
          flex-shrink: 0;
        }

        .post-share-body {
          min-width: 0;
          flex: 1;
        }

        .post-share-title {
          color: #303133;
          font-size: 14px;
          font-weight: 600;
          line-height: 1.4;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .post-share-meta {
          color: #909399;
          font-size: 12px;
          line-height: 1.4;
          margin-top: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }

      .itemTime {
        font-size: 10px;
        color: #999;
        margin-top: 5px;
      }
    }

  }

  .inputWrapper {
    width: 100%;
    height: 188px;
    margin-top: 10px;
    position: relative;
    border: 1px solid #e5e5e5;

    .chatTool {
      position: absolute;
      bottom: 10px;
      right: 15px;
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #666;

      .send {
        margin-left: 15px;
      }
    }

    .toolbar {
      height: 38px;
      padding: 0 10px;
      display: flex;
      align-items: center;
      gap: 4px;
      border-bottom: 1px solid #f0f0f0;
      box-sizing: border-box;

      :deep(.el-upload) {
        display: flex;
      }
    }

    .emoji-panel,
    .sticker-panel {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 100%;
      z-index: 10;
      margin-bottom: 6px;
      padding: 12px;
      background: #fff;
      border: 1px solid #e5e5e5;
      border-radius: 6px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
      box-sizing: border-box;
    }

    .emoji-panel {
      max-height: 188px;
      overflow-y: auto;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
      gap: 6px;
    }

    .emoji-item {
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 4px;
      cursor: pointer;
      font-size: 20px;

      &:hover {
        background: #f5f7fa;
      }
    }

    .sticker-panel {
      max-height: 220px;
      overflow-y: auto;
    }

    .sticker-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
      gap: 10px;
    }

    .sticker-item,
    .sticker-add {
      width: 72px;
      height: 72px;
      border-radius: 6px;
      border: 1px solid #edf0f5;
      background: #f8fafc;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-sizing: border-box;
    }

    .sticker-item {
      position: relative;

      img {
        max-width: 62px;
        max-height: 62px;
        object-fit: contain;
      }

      .remove-sticker {
        position: absolute;
        top: -6px;
        right: -6px;
        width: 18px;
        height: 18px;
        border-radius: 50%;
        background: #fff;
        color: #909399;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.14);
      }
    }

    textarea {
      border-radius: 5px;
      width: 100%;
      height: calc(100% - 38px);
      resize: none;
      border: none;
      outline: none;
      padding: 15px 15px 48px;
      box-sizing: border-box;
      font-size: 15px;
      //background-color: #f7f8f9;

      &::-webkit-scrollbar {
        width: 0;
        height: 0;
      }
    }
  }
}

.empty-chat {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f7f8f9;
  border: 1px solid #e5e5e5;
  border-radius: 5px;
}
</style>
