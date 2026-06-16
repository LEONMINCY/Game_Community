<!-- 文件说明：views/admin/message.vue，后台站内私信页面，供管理员和审核员与用户沟通。 -->
<template>
  <div class="staff-message-page">
    <aside class="contact-panel">
      <div class="panel-title">站内私信</div>
      <el-input v-model="keyword" clearable placeholder="筛选已有会话" />
      <el-scrollbar class="contact-list">
        <button
          v-for="contact in filteredContacts"
          :key="contact.userId"
          type="button"
          class="contact-item"
          :class="{ active: activeContact?.userId === contact.userId }"
          @click="selectContact(contact)"
        >
          <el-avatar :size="38" :src="contact.avatar" />
          <span>
            <strong>{{ contact.nickname || contact.username || `用户${contact.userId}` }}</strong>
            <small>{{ previewMessage(contact.lastMessage) || contact.username || '已有会话' }}</small>
          </span>
          <em>{{ formatContactTime(contact.lastTime) }}</em>
        </button>
        <el-empty v-if="!filteredContacts.length" description="暂无会话记录" :image-size="72" />
      </el-scrollbar>
    </aside>

    <section class="chat-panel">
      <div v-if="activeContact" class="chat-head">
        <el-avatar :size="34" :src="activeContact.avatar" />
        <div>
          <strong>{{ activeContact.nickname || activeContact.username || `用户${activeContact.userId}` }}</strong>
          <small>审核沟通私信，管理员和审核员不受关注关系限制</small>
        </div>
      </div>
      <el-empty v-else class="empty-chat" description="从左侧选择已有会话，或在举报管理中点击私信" :image-size="100" />

      <el-scrollbar v-if="activeContact" ref="messageScrollbar" class="message-list">
        <div
          v-for="message in messages"
          :key="message.id || `${message.senderId}-${message.createTime}`"
          class="message-row"
          :class="{ mine: Number(message.senderId) === Number(currentUserId) }"
        >
          <div class="message-bubble" :class="messageView(message.content).type">
            <template v-if="['image', 'sticker'].includes(messageView(message.content).type)">
              <el-image
                :src="messageView(message.content).url"
                :fit="messageView(message.content).type === 'sticker' ? 'contain' : 'cover'"
                preview-teleported
                :preview-src-list="[messageView(message.content).url]"
              />
            </template>
            <template v-else>
              <div>{{ messageView(message.content).text }}</div>
            </template>
            <small>{{ message.createTime }}</small>
          </div>
        </div>
      </el-scrollbar>

      <div v-if="activeContact" class="message-composer" @click.stop>
        <div class="composer-toolbar">
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
          <span v-for="emoji in emojiList" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">
            {{ emoji }}
          </span>
        </div>

        <div v-if="showStickerPanel" class="sticker-panel" @click.stop>
          <div class="sticker-grid">
            <div v-for="sticker in customStickers" :key="sticker" class="sticker-item" @click="sendMediaMessage('sticker', sticker)">
              <img :src="normalizeMediaUrl(sticker)" alt="sticker" />
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

        <el-input
          ref="composerInput"
          v-model="draft"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="输入要发送的社区沟通信息"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :disabled="!draft.trim()" @click="sendMessage">发送</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close, MagicStick, Picture, Plus, Sunny } from '@element-plus/icons-vue'
import { getRequest, postRequest } from '@/utils/http'
import { emojiList } from '@/utils/emoji'
import { replaceURL } from '@/utils/tools'
import { useUserStore } from '@/store'

const route = useRoute()
const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId || localStorage.getItem('userId'))
const keyword = ref('')
const contacts = ref([])
const activeContact = ref(null)
const messages = ref([])
const draft = ref('')
const messageScrollbar = ref(null)
const composerInput = ref(null)
const showEmojiPicker = ref(false)
const showStickerPanel = ref(false)
const customStickers = ref([])
const headers = ref({ Authorization: localStorage.getItem('token') })
let ws = null
let reconnectTimer = null
let allowReconnect = true

const stickerStorageKey = computed(() => `chat_custom_stickers_${currentUserId.value || 'default'}`)

const filteredContacts = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return contacts.value
  return contacts.value.filter(contact => {
    return String(contact.nickname || '').toLowerCase().includes(kw)
      || String(contact.username || '').toLowerCase().includes(kw)
      || String(contact.userId || '').includes(kw)
  })
})

onMounted(() => {
  loadCustomStickers()
  loadChatContacts().then(openRouteContact)
  initWebSocket()
  document.addEventListener('click', closePanels)
})

onUnmounted(() => {
  allowReconnect = false
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  if (ws) {
    ws.close()
  }
  document.removeEventListener('click', closePanels)
})

watch(() => route.query.userId, () => openRouteContact())
watch(currentUserId, () => initWebSocket())

const initWebSocket = () => {
  const id = currentUserId.value
  if (!id || (ws && [WebSocket.OPEN, WebSocket.CONNECTING].includes(ws.readyState))) {
    return
  }
  ws = new WebSocket(buildWebSocketUrl(id))
  ws.onmessage = handleSocketMessage
  ws.onclose = () => {
    ws = null
    if (allowReconnect) {
      reconnectTimer = setTimeout(initWebSocket, 3000)
    }
  }
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

const handleSocketMessage = (event) => {
  let data = null
  try {
    data = JSON.parse(event.data)
  } catch (error) {
    return
  }
  if (!data || data.type === 'notification') {
    return
  }
  const currentId = String(currentUserId.value || '')
  const senderId = String(data.senderId || '')
  const receiverId = String(data.receiverId || '')
  if (!currentId || (senderId !== currentId && receiverId !== currentId)) {
    return
  }
  const otherUserId = senderId === currentId ? data.receiverId : data.senderId
  const time = data.createTime || new Date().toISOString()
  updatePreview(data.content, time, otherUserId)
  if (activeContact.value && String(activeContact.value.userId) === String(otherUserId)) {
    const duplicated = data.id && messages.value.some(message => String(message.id) === String(data.id))
    if (!duplicated) {
      messages.value.push({
        id: data.id,
        senderId: data.senderId,
        receiverId: data.receiverId,
        content: data.content,
        createTime: time
      })
      scrollToBottom()
    }
  }
}

const loadChatContacts = async () => {
  const res = await getRequest('/message/chat-list', { silentError: true })
  if (res.code === 200) {
    contacts.value = (res.data || []).map(chat => ({
      ...chat,
      avatar: '',
      nickname: '',
      username: '',
      lastMessage: chat.lastMessage,
      lastTime: chat.lastTime
    }))
    contacts.value.forEach(loadContactInfo)
  }
}

const loadContactInfo = (contact) => {
  if (!contact?.userId) return
  getRequest('/user-info/get-real/' + contact.userId, { silentError: true }).then(res => {
    if (res.code === 200) {
      contact.nickname = res.data.nickname
      contact.username = res.data.username
      contact.avatar = normalizeMediaUrl(res.data.avatar)
    }
  })
}

const openRouteContact = () => {
  const userId = route.query.userId
  if (!userId) return
  let contact = contacts.value.find(item => String(item.userId) === String(userId))
  if (!contact) {
    contact = {
      userId: Number(userId),
      nickname: String(route.query.nickname || ''),
      username: '',
      avatar: '',
      lastMessage: '尚未开始会话',
      lastTime: ''
    }
    contacts.value.unshift(contact)
    loadContactInfo(contact)
  }
  selectContact(contact)
}

const selectContact = (contact) => {
  activeContact.value = contact
  draft.value = ''
  closePanels()
  loadMessages()
}

const loadMessages = () => {
  if (!activeContact.value?.userId) return
  getRequest(`/message/list/${activeContact.value.userId}`, { silentError: true }).then(res => {
    if (res.code === 200) {
      messages.value = res.data || []
      scrollToBottom()
    }
  })
}

const sendMessage = () => {
  const content = draft.value.trim()
  if (content) {
    sendContent(content)
    draft.value = ''
  }
}

const sendMediaMessage = (type, url) => {
  const mediaUrl = normalizeMediaUrl(url)
  if (!mediaUrl) return
  sendContent(JSON.stringify({ type, url: mediaUrl }))
  closePanels()
}

const sendContent = (content) => {
  if (!activeContact.value?.userId) return
  postRequest('/message/add', {
    receiverId: activeContact.value.userId,
    content
  }).then(res => {
    if (res.code === 200) {
      const time = new Date().toLocaleString()
      messages.value.push({
        senderId: currentUserId.value,
        receiverId: activeContact.value.userId,
        content,
        createTime: time
      })
      updatePreview(content, time)
      scrollToBottom()
    } else {
      ElMessage.error(res.msg || '发送失败')
    }
  })
}

const updatePreview = (content, time, targetUserId = activeContact.value?.userId) => {
  if (!targetUserId) return
  const userId = targetUserId
  let contact = contacts.value.find(item => String(item.userId) === String(userId))
  if (!contact) {
    contact = {
      userId: Number(userId),
      nickname: '',
      username: '',
      avatar: '',
      lastMessage: content,
      lastTime: time
    }
    contacts.value = [contact, ...contacts.value]
    loadContactInfo(contact)
    return
  }
  contact.lastMessage = content
  contact.lastTime = time
  contacts.value = [contact, ...contacts.value.filter(item => String(item.userId) !== String(userId))]
  if (activeContact.value && String(activeContact.value.userId) === String(userId)) {
    activeContact.value = contact
  }
}

const normalizeMediaUrl = (url) => {
  if (!url) return ''
  const value = String(url).trim().replace(/\\/g, '/')
  if (value.startsWith('data:') || value.startsWith('blob:')) return value
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
  if (!content || typeof content === 'object') return content || ''
  try {
    const parsed = JSON.parse(content)
    return typeof parsed === 'string' ? JSON.parse(parsed) : parsed
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
  const data = parseMessageContent(content)
  if (data && typeof data === 'object') {
    const mediaUrl = data.url || data.imageUrl || data.src || data.content
    if (mediaUrl && (data.type === 'image' || data.type === 'sticker' || looksLikeImageUrl(mediaUrl))) {
      return { type: data.type === 'sticker' ? 'sticker' : 'image', url: normalizeMediaUrl(mediaUrl) }
    }
    return { type: 'text', text: data.text || data.content || '' }
  }
  if (looksLikeImageUrl(data)) return { type: 'image', url: normalizeMediaUrl(data) }
  return { type: 'text', text: data || '' }
}

const previewMessage = (content) => {
  const view = messageView(content)
  if (view.type === 'image') return '[图片]'
  if (view.type === 'sticker') return '[表情包]'
  return view.text
}

const formatContactTime = (value) => {
  if (!value) return ''
  const date = new Date(String(value).replace('T', ' '))
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const currentYear = new Date().getFullYear()
  return date.getFullYear() === currentYear ? `${month}-${day}` : `${date.getFullYear()}-${month}-${day}`
}

const toggleEmojiPanel = () => {
  showEmojiPicker.value = !showEmojiPicker.value
  showStickerPanel.value = false
}

const toggleStickerPanel = () => {
  showStickerPanel.value = !showStickerPanel.value
  showEmojiPicker.value = false
}

const closePanels = () => {
  showEmojiPicker.value = false
  showStickerPanel.value = false
}

const insertEmoji = (emoji) => {
  draft.value += emoji
  nextTick(() => composerInput.value?.focus?.())
}

const handleImageUploadSuccess = (response) => {
  if (response.code === 200 && response.data) {
    sendMediaMessage('image', response.data)
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
  try {
    const parsed = JSON.parse(localStorage.getItem(stickerStorageKey.value) || '[]')
    customStickers.value = Array.isArray(parsed) ? parsed.map(normalizeMediaUrl).filter(Boolean) : []
  } catch (error) {
    customStickers.value = []
  }
}

const saveCustomStickers = () => {
  localStorage.setItem(stickerStorageKey.value, JSON.stringify(customStickers.value))
}

const removeSticker = (sticker) => {
  customStickers.value = customStickers.value.filter(item => item !== sticker)
  saveCustomStickers()
}

const scrollToBottom = () => {
  nextTick(() => {
    const wrap = messageScrollbar.value?.wrapRef
    if (wrap) wrap.scrollTop = wrap.scrollHeight
  })
}
</script>

<style scoped>
.staff-message-page {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  height: calc(100vh - 120px);
  padding: 24px;
  box-sizing: border-box;
}

.contact-panel,
.chat-panel {
  min-width: 0;
  border: 1px solid #e9edf3;
  border-radius: 8px;
  background: #fff;
}

.contact-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.panel-title {
  font-size: 18px;
  font-weight: 700;
}

.contact-list {
  flex: 1;
}

.contact-item {
  width: 100%;
  border: 0;
  border-radius: 6px;
  background: transparent;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 10px;
  padding: 10px;
  cursor: pointer;
  text-align: left;
}

.contact-item.active,
.contact-item:hover {
  background: #f0f6ff;
}

.contact-item span {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.contact-item strong,
.contact-item small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-item em {
  justify-self: end;
  color: #a8b1c0;
  font-style: normal;
  font-size: 12px;
  white-space: nowrap;
}

.contact-item small,
.chat-head small,
.message-bubble small {
  color: #8b95a5;
}

.chat-panel {
  display: flex;
  flex-direction: column;
}

.chat-head {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  border-bottom: 1px solid #edf1f7;
}

.chat-head div {
  display: flex;
  flex-direction: column;
}

.empty-chat {
  flex: 1;
}

.message-list {
  flex: 1;
  padding: 18px;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.mine {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 66%;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f3f5f8;
  line-height: 1.6;
  word-break: break-word;
}

.message-row.mine .message-bubble {
  color: #fff;
  background: #409eff;
}

.message-row.mine .message-bubble small {
  color: rgba(255, 255, 255, 0.82);
}

.message-bubble.image,
.message-bubble.sticker {
  background: transparent;
  padding: 0;
}

.message-bubble :deep(.el-image) {
  max-width: 220px;
  max-height: 180px;
  border-radius: 8px;
  overflow: hidden;
}

.message-bubble.sticker :deep(.el-image) {
  width: 120px;
  height: 120px;
}

.message-composer {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 188px;
  padding: 0 14px 12px;
  border-top: 1px solid #edf1f7;
  background: #fff;
}

.composer-toolbar {
  height: 38px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-bottom: 1px solid #f0f2f5;
}

.composer-toolbar :deep(.el-upload) {
  display: flex;
}

.message-composer :deep(.el-textarea) {
  flex: 1;
  min-height: 94px;
}

.message-composer :deep(.el-textarea__inner) {
  min-height: 94px !important;
  resize: none;
  border-radius: 6px;
}

.message-composer > .el-button {
  align-self: flex-end;
  width: 96px;
}

.emoji-panel,
.sticker-panel {
  position: absolute;
  left: 14px;
  bottom: 100%;
  z-index: 10;
  width: 320px;
  max-height: 220px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 30px rgba(20, 32, 50, 0.12);
}

.emoji-item {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 20px;
}

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.sticker-item,
.sticker-add {
  position: relative;
  width: 66px;
  height: 66px;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  cursor: pointer;
}

.sticker-item img {
  max-width: 58px;
  max-height: 58px;
}

.remove-sticker {
  position: absolute;
  top: -6px;
  right: -6px;
  color: #f56c6c;
  background: #fff;
  border-radius: 50%;
}
</style>
