<!-- 文件说明：views/detail/index.vue，新闻详情页面，展示正文、评论、回复、点赞、收藏、转发和举报。 -->
<template>
  <div class="detail-container">
    <div class="detail-content">
      <div class="back-row">
        <el-button text @click="goBack">&lt; 返回</el-button>
      </div>

      <template v-if="!detailMissing">
        <div class="article-header">
          <h1 class="article-title">{{ newsDetail.title }}</h1>
          <div class="article-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ newsDetail.author }}
            </span>
            <span class="meta-item">
              <el-icon><Timer /></el-icon>
              {{ newsDetail.publishTime }}
            </span>
            <span class="meta-item">
              <el-icon><View /></el-icon>
              {{ newsDetail.browCount || 0 }}
            </span>
          </div>
        </div>

        <div class="article-cover" v-if="newsDetail.coverImage">
          <el-image
            :src="newsDetail.coverImage"
            fit="cover"
            :preview-src-list="[newsDetail.coverImage]"
            preview-teleported
          />
        </div>

        <div class="article-content" v-html="newsDetail.content"></div>

        <div class="article-tags" v-if="newsDetail.tags && newsDetail.tags.length">
          <el-tag
            v-for="tag in newsDetail.tags"
            :key="tag"
            class="tag-item"
            size="small"
          >
            {{ tag }}
          </el-tag>
        </div>

        <div class="article-actions">
          <button class="article-action" :class="{ active: newsDetail.isLikes }" @click="handleNewsLike">
            <el-icon><Pointer /></el-icon>
            <span>点赞 {{ newsDetail.likes || 0 }}</span>
          </button>
          <button class="article-action" :class="{ active: newsDetail.isFavorites }" @click="handleNewsFavorite">
            <el-icon><Star /></el-icon>
            <span>收藏 {{ newsDetail.favorites || 0 }}</span>
          </button>
          <button class="article-action" @click="shareNews">
            <el-icon><Share /></el-icon>
            <span>{{ newsDetail.shareCount || 0 }}</span>
          </button>
        </div>

        <div class="comments-wrapper">
          <div class="comments-title">全部评论 {{ comments.length }}</div>
          <div class="comments-list">
            <div
              v-for="comment in comments"
              :id="`comment-${comment.id}`"
              :key="comment.id"
              class="comment-item"
              :class="{ highlighted: String(route.query.commentId || '') === String(comment.id) }"
            >
              <div class="comment-header">
                <div class="user-info" @click="viewUser(comment.userId)">
                  <el-avatar :size="30" :src="comment.avatar" />
                  <span class="username">{{ comment.nickname || '用户' + comment.userId }}</span>
                  <span class="level">Lv{{ comment.userLevel ?? 0 }}</span>
                </div>
                <span class="time">{{ comment.createTime }}</span>
              </div>
              <div class="comment-content">
                <template v-if="comment.replyUserNickname">
                  <span class="reply-to">回复 <span @click="viewUser(comment.replyUserId)">@{{ comment.replyUserNickname }}</span>：</span>
                </template>
                {{ comment.content }}
                <div v-if="comment.imageUrl" class="comment-image-wrap">
                  <el-image
                    class="comment-image"
                    :src="comment.imageUrl"
                    :preview-src-list="[comment.imageUrl]"
                    preview-teleported
                    fit="cover"
                  />
                </div>
              </div>
              <div class="comment-actions">
                <span v-if="canReplyComment(comment)" class="action" @click="replyToComment(comment)">
                  <el-icon><ChatLineRound /></el-icon>
                  回复
                </span>
                <span class="action report" v-if="canReportComment(comment)" @click="openReportDialog(comment)">
                  <el-icon><Warning /></el-icon>
                  举报
                </span>
                <span class="action delete" v-if="comment.userId === userStore.userInfo.userId" @click="deleteComment(comment)">
                  <el-icon><Delete /></el-icon>
                  删除
                </span>
                <span v-if="comment.replyCount > 0" class="reply-count">{{ comment.replyCount }}条回复</span>
              </div>

              <div v-if="comment.children && comment.children.length > 0" class="reply-list">
                <div
                  v-for="reply in comment.children"
                  :id="`comment-${reply.id}`"
                  :key="reply.id"
                  class="reply-item"
                  :class="{ highlighted: String(route.query.commentId || '') === String(reply.id) }"
                >
                  <div class="comment-header">
                    <div class="user-info" @click="viewUser(reply.userId)">
                      <el-avatar :size="24" :src="reply.avatar" />
                      <span class="username">{{ reply.nickname || '用户' + reply.userId }}</span>
                      <span class="level">Lv{{ reply.userLevel ?? 0 }}</span>
                    </div>
                    <span class="time">{{ reply.createTime }}</span>
                  </div>
                  <div class="comment-content">
                    <template v-if="reply.replyUserNickname">
                      <span class="reply-to">回复 <span @click="viewUser(reply.replyUserId)">@{{ reply.replyUserNickname }}</span>：</span>
                    </template>
                    {{ reply.content }}
                    <div v-if="reply.imageUrl" class="comment-image-wrap">
                      <el-image
                        class="comment-image reply-image"
                        :src="reply.imageUrl"
                        :preview-src-list="[reply.imageUrl]"
                        preview-teleported
                        fit="cover"
                      />
                    </div>
                  </div>
                  <div class="comment-actions">
                    <span v-if="canReplyComment(reply)" class="action" @click="replyToComment(reply)">
                      <el-icon><ChatLineRound /></el-icon>
                      回复
                    </span>
                    <span class="action report" v-if="canReportComment(reply)" @click="openReportDialog(reply)">
                      <el-icon><Warning /></el-icon>
                      举报
                    </span>
                    <span class="action delete" v-if="reply.userId === userStore.userInfo.userId" @click="deleteComment(reply)">
                      <el-icon><Delete /></el-icon>
                      删除
                    </span>
                  </div>
                </div>
                <div v-if="comment.replyCount > comment.children.length" class="view-more" @click="loadMoreReplies(comment)">
                  查看更多回复
                </div>
              </div>
            </div>
            <el-empty v-if="comments.length === 0 && !loading" description="暂无评论" />
            <div class="load-more" v-if="!finished && comments.length > 0" @click="getComments">加载更多</div>
          </div>
        </div>
      </template>
      <el-empty v-else class="detail-empty" description="新闻不存在或已被删除" />
    </div>

    <el-dialog v-model="reportDialogVisible" title="举报评论" width="420px" :close-on-click-modal="false">
      <div class="report-dialog-content">
        <div class="report-target">
          <span>{{ reportTarget?.nickname || ('用户' + reportTarget?.userId) }}</span>
          <div class="report-preview">{{ reportTarget?.content || '图片评论' }}</div>
        </div>
        <el-input
          v-model="reportReason"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          placeholder="请输入举报原因"
        />
      </div>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <div v-if="!detailMissing" class="fixed-comment-input">
      <div class="reply-info" v-if="replyTo">
        回复 @{{ replyTo.nickname }}
        <el-icon class="close-reply" @click="cancelReply"><Close /></el-icon>
      </div>
      <div class="input-wrapper">
        <div class="emoji-trigger" @click.stop="toggleEmojiPanel">😊</div>
        <el-button text circle class="sticker-trigger" title="自定义表情包" @click.stop="toggleStickerPanel">
          <el-icon><MagicStick /></el-icon>
        </el-button>
        <el-upload
          class="comment-image-upload"
          action="/api/file/upload"
          :headers="headers"
          :show-file-list="false"
          accept="image/*"
          :on-success="handleCommentImageSuccess"
        >
          <el-button text circle title="发送图片">
            <el-icon><Picture /></el-icon>
          </el-button>
        </el-upload>
        <el-input
          v-model="newComment"
          type="textarea"
          :rows="3"
          :placeholder="replyTo ? '写下你的回复...' : '写下你的评论...'"
          resize="none"
          @focus="loadMentionUsers"
          @input="handleMentionInput"
        />
        <div v-if="showEmojiPicker" class="emoji-picker" @click.stop>
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
              @click="selectCommentSticker(sticker)"
            >
              <img :src="sticker" alt="sticker" />
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
          <div v-if="customStickers.length === 0" class="sticker-empty">暂无表情包，点击 + 添加</div>
        </div>
        <div v-if="showMentionPicker" class="mention-picker" @click.stop>
          <div
            v-for="option in mentionOptions"
            :key="option.key"
            class="mention-option"
            @click="insertMention(option)"
          >
            <el-avatar :size="28" :src="option.avatar" />
            <div class="mention-meta">
              <span>{{ option.nickname }}</span>
              <small>{{ option.isAi ? '评论区 AI 自动回复' : '已关注用户' }}</small>
            </div>
          </div>
          <div v-if="mentionOptions.length === 0" class="mention-empty">没有匹配的关注用户</div>
        </div>
      </div>
      <div v-if="commentImageUrl" class="comment-image-preview">
        <el-image :src="commentImageUrl" fit="cover" />
        <el-icon class="remove-image" @click="commentImageUrl = ''"><Close /></el-icon>
      </div>
      <div class="submit-wrapper">
        <el-button type="primary" @click="submitComment">{{ replyTo ? '回复' : '发表评论' }}</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref, onActivated, onDeactivated, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatLineRound, Close, Delete, MagicStick, Picture, Plus, Pointer, Share, Star, Timer, User, View, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequest, postRequest, deleteRequest } from '@/utils/http.js'
import { emojiList } from '@/utils/emoji'
import { replaceURL } from '@/utils/tools'
import { useUserStore } from '@/store'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const newsDetail = ref({})
const comments = ref([])
const loading = ref(false)
const finished = ref(false)
const detailMissing = ref(false)
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  newsId: null
})
const newComment = ref('')
const replyTo = ref(null)
const showEmojiPicker = ref(false)
const showStickerPanel = ref(false)
const customStickers = ref([])
const commentImageUrl = ref('')
const headers = ref({
  Authorization: localStorage.getItem('token')
})
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportReason = ref('')
const followingMentionUsers = ref([])
const showMentionPicker = ref(false)
const mentionKeyword = ref('')
const pageActive = ref(false)

const aiMentionOption = {
  key: 'ai-assistant',
  userId: null,
  nickname: 'AI助手',
  avatar: '',
  isAi: true
}

const parseNewsCover = (mediaJson) => {
  if (!mediaJson) return ''
  try {
    const parsed = JSON.parse(mediaJson)
    if (Array.isArray(parsed)) {
      return parsed[0] ? replaceURL(parsed[0]) : ''
    }
    return parsed ? replaceURL(parsed) : ''
  } catch (error) {
    return replaceURL(mediaJson)
  }
}

const applyNewsDetail = (item) => {
  newsDetail.value = {
    ...newsDetail.value,
    ...item,
    author: '游戏社区',
    publishTime: item.createTime,
    coverImage: parseNewsCover(item.mediaJson),
    tags: ['游戏新闻'],
    likes: item.likes || 0,
    favorites: item.favorites || 0,
    shareCount: item.shareCount || 0,
    isLikes: !!item.isLikes,
    isFavorites: !!item.isFavorites
  }
}

const resolveNewsId = () => {
  const candidate = route.query.id ?? route.query.newsId
  const raw = Array.isArray(candidate) ? candidate[0] : candidate
  const id = Number(raw)
  return Number.isInteger(id) && id > 0 ? id : null
}

const isNewsDetailRoute = () => route.path === '/news/detail'

const normalizeComment = (item) => ({
  ...item,
  avatar: item.avatar ? replaceURL(item.avatar) : '',
  imageUrl: item.imageUrl ? replaceURL(item.imageUrl) : '',
  children: (item.children || []).map(child => ({
    ...child,
    avatar: child.avatar ? replaceURL(child.avatar) : '',
    imageUrl: child.imageUrl ? replaceURL(child.imageUrl) : ''
  }))
})

const getNewsDetail = async (id) => {
  if (!id) {
    detailMissing.value = true
    ElMessage.warning('新闻信息不存在，请返回列表重新打开')
    return false
  }
  try {
    const res = await getRequest('/news/get/' + id, { silentError: true })
    if (res.code === 200) {
      applyNewsDetail(res.data)
      detailMissing.value = false
      return true
    }
    detailMissing.value = true
    ElMessage.warning(res.msg || '新闻不存在或已被删除')
  } catch (error) {
    detailMissing.value = true
    ElMessage.warning('新闻不存在或已被删除')
  }
  return false
}

const handleNewsLike = () => {
  if (!newsDetail.value.id) return
  const url = newsDetail.value.isLikes ? '/news/unlike/' : '/news/like/'
  postRequest(url + newsDetail.value.id).then(res => {
    if (res.code === 200) {
      applyNewsDetail(res.data)
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const handleNewsFavorite = () => {
  if (!newsDetail.value.id) return
  const url = newsDetail.value.isFavorites ? '/news/unFavorite/' : '/news/favorite/'
  postRequest(url + newsDetail.value.id).then(res => {
    if (res.code === 200) {
      applyNewsDetail(res.data)
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const shareNews = () => {
  if (!newsDetail.value.id) return
  postRequest('/news/share/' + newsDetail.value.id).then(async res => {
    if (res.code === 200) {
      newsDetail.value.shareCount = res.data || 0
      try {
        await navigator.clipboard.writeText(window.location.href)
        ElMessage.success('链接已复制')
      } catch (error) {
        ElMessage.success('转发次数已更新')
      }
    }
  })
}

const getComments = () => {
  if (!queryParams.value.newsId || loading.value || finished.value) return
  loading.value = true
  postRequest('/news/comment/list', queryParams.value).then(res => {
    if (res.code === 200) {
      const list = (res.data.list || []).map(normalizeComment)
      if (queryParams.value.pageNo === 1) {
        comments.value = list
        scrollToTargetComment()
      } else {
        comments.value = [...comments.value, ...list]
      }
      if (list.length < queryParams.value.pageSize) {
        finished.value = true
      }
      queryParams.value.pageNo += 1
    }
  }).finally(() => {
    loading.value = false
  })
}

const resetComments = () => {
  queryParams.value.pageNo = 1
  finished.value = false
  comments.value = []
  getComments()
}

const replyToComment = (comment) => {
  if (!canReplyComment(comment)) {
    ElMessage.warning('不能回复自己的评论')
    return
  }
  replyTo.value = comment.parentId ? { ...comment, id: comment.parentId } : comment
}

const cancelReply = () => {
  replyTo.value = null
  newComment.value = ''
}

const submitComment = () => {
  if (!queryParams.value.newsId) {
    ElMessage.warning('新闻信息不存在，请返回列表重新打开')
    return
  }
  if (!newComment.value.trim() && !commentImageUrl.value) {
    ElMessage.warning('评论内容或图片不能为空')
    return
  }
  postRequest('/news/comment/add', {
    newsId: queryParams.value.newsId,
    content: newComment.value.trim(),
    imageUrl: commentImageUrl.value,
    parentId: replyTo.value?.id || null,
    replyUserId: replyTo.value?.userId || null,
    mentionUserIds: collectMentionUserIds(),
    mentionAi: hasAiMention()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success(replyTo.value ? '回复成功' : '评论成功')
      newComment.value = ''
      commentImageUrl.value = ''
      replyTo.value = null
      resetComments()
    } else {
      ElMessage.error(res.msg || '评论失败')
    }
  })
}

const loadMoreReplies = async (comment) => {
  const res = await getRequest(`/news/comment/reply/list/${comment.id}?pageNo=1&pageSize=999`)
  if (res.code === 200) {
    comment.children = (res.data.list || []).map(normalizeComment)
  }
}

const canReportComment = (comment) => {
  return Number(comment?.userId) !== Number(userStore.userInfo?.userId) && !isAiUser(comment?.userId)
}

const canReplyComment = (comment) => {
  return Number(comment?.userId) !== Number(userStore.userInfo?.userId)
}

const isAiUser = (userId) => {
  return userId !== null && userId !== undefined && Number(userId) <= 0
}

const openReportDialog = (comment) => {
  reportTarget.value = comment
  reportReason.value = ''
  reportDialogVisible.value = true
}

const submitReport = () => {
  if (!reportReason.value.trim()) {
    ElMessage.warning('请输入举报原因')
    return
  }
  postRequest('/report/add', {
    reportedId: reportTarget.value.id,
    reportType: 'NEWS_COMMENT',
    reason: reportReason.value.trim()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('举报已提交')
      reportDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '举报提交失败')
    }
  }).catch(() => {
    ElMessage.error('举报提交失败')
  })
}

const deleteComment = (comment) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteRequest('/news/comment/delete/' + comment.id).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        resetComments()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  }).catch(() => {})
}

const handleCommentImageSuccess = (res) => {
  if (res.code === 200) {
    commentImageUrl.value = replaceURL(res.data)
    ElMessage.success('图片已添加')
  } else {
    ElMessage.error(res.msg || '图片上传失败')
  }
}

const getCurrentUserId = () => userStore.userInfo?.userId || localStorage.getItem('userId') || sessionStorage.getItem('userId') || ''

const stickerStorageKey = () => `chat_custom_stickers_${getCurrentUserId() || 'default'}`

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
      const parsed = JSON.parse(localStorage.getItem(key) || '[]')
      const list = Array.isArray(parsed) ? parsed : [parsed]
      list.forEach(item => {
        const url = replaceURL(typeof item === 'string' ? item : item?.url || item?.src)
        if (url && !stickers.includes(url)) {
          stickers.push(url)
        }
      })
    } catch (error) {
      // 忽略旧版本写入的不规范表情缓存，避免影响评论区加载。
    }
  })
  customStickers.value = stickers
  saveCustomStickers()
}

const saveCustomStickers = () => {
  localStorage.setItem(stickerStorageKey(), JSON.stringify(customStickers.value))
}

const toggleEmojiPanel = () => {
  showEmojiPicker.value = !showEmojiPicker.value
  showStickerPanel.value = false
}

const toggleStickerPanel = () => {
  showStickerPanel.value = !showStickerPanel.value
  showEmojiPicker.value = false
  loadCustomStickers()
}

const selectCommentSticker = (sticker) => {
  commentImageUrl.value = replaceURL(sticker)
  showStickerPanel.value = false
}

const handleStickerUploadSuccess = (res) => {
  if (res.code === 200 && res.data) {
    const stickerUrl = replaceURL(res.data)
    if (!customStickers.value.includes(stickerUrl)) {
      customStickers.value.unshift(stickerUrl)
      saveCustomStickers()
    }
    commentImageUrl.value = stickerUrl
    showStickerPanel.value = false
    ElMessage.success('表情包已添加')
  } else {
    ElMessage.error(res.msg || '表情包上传失败')
  }
}

const removeSticker = (sticker) => {
  customStickers.value = customStickers.value.filter(item => item !== sticker)
  saveCustomStickers()
  if (commentImageUrl.value === sticker) {
    commentImageUrl.value = ''
  }
}

const loadMentionUsers = () => {
  const currentUserId = userStore.userInfo?.userId
  if (!currentUserId || followingMentionUsers.value.length > 0) {
    return
  }
  getRequest(`/userRelation/following/${currentUserId}`, { silentError: true }).then(res => {
    if (res.code === 200) {
      followingMentionUsers.value = (res.data || []).map(user => ({
        key: `user-${user.userId}`,
        userId: user.userId,
        nickname: user.nickname || `用户${user.userId}`,
        avatar: replaceURL(user.avatar),
        isAi: false
      }))
    }
  }).catch(() => {
    followingMentionUsers.value = []
  })
}

const handleMentionInput = () => {
  const textarea = document.querySelector('.fixed-comment-input textarea')
  const cursor = textarea ? textarea.selectionStart : newComment.value.length
  const beforeCursor = newComment.value.slice(0, cursor)
  const match = beforeCursor.match(/@([^\s@，,：:]*)$/)
  if (!match) {
    showMentionPicker.value = false
    return
  }
  mentionKeyword.value = match[1] || ''
  showMentionPicker.value = true
  loadMentionUsers()
}

const mentionOptions = computed(() => {
  const keyword = mentionKeyword.value.trim().toLowerCase()
  const users = followingMentionUsers.value.filter(user => !keyword || user.nickname.toLowerCase().includes(keyword))
  const aiVisible = !keyword || aiMentionOption.nickname.toLowerCase().includes(keyword) || 'ai'.includes(keyword)
  return aiVisible ? [aiMentionOption, ...users] : users
})

const insertMention = (option) => {
  const textarea = document.querySelector('.fixed-comment-input textarea')
  const cursor = textarea ? textarea.selectionStart : newComment.value.length
  const beforeCursor = newComment.value.slice(0, cursor)
  const afterCursor = newComment.value.slice(cursor)
  const nextBefore = beforeCursor.replace(/@([^\s@，,：:]*)$/, `@${option.nickname} `)
  newComment.value = nextBefore + afterCursor
  showMentionPicker.value = false
  requestAnimationFrame(() => {
    const nextCursor = nextBefore.length
    const input = document.querySelector('.fixed-comment-input textarea')
    input?.focus()
    input?.setSelectionRange(nextCursor, nextCursor)
  })
}

const collectMentionUserIds = () => {
  return followingMentionUsers.value
    .filter(user => newComment.value.includes(`@${user.nickname}`))
    .map(user => user.userId)
}

const hasAiMention = () => {
  return newComment.value.includes('@AI助手') || /@AI(\s|$)/i.test(newComment.value)
}

const insertEmoji = (emoji) => {
  newComment.value += emoji
  showEmojiPicker.value = false
  showStickerPanel.value = false
}

const closeEmojiPicker = (event) => {
  const target = event?.target
  const emojiPicker = document.querySelector('.emoji-picker')
  const emojiTrigger = document.querySelector('.emoji-trigger')
  const stickerPanel = document.querySelector('.sticker-panel')
  const stickerTrigger = document.querySelector('.sticker-trigger')
  const mentionPicker = document.querySelector('.mention-picker')
  if (!target || (emojiPicker && !emojiPicker.contains(target) && emojiTrigger && !emojiTrigger.contains(target))) {
    showEmojiPicker.value = false
  }
  if (!target || (stickerPanel && !stickerPanel.contains(target) && stickerTrigger && !stickerTrigger.contains(target))) {
    showStickerPanel.value = false
  }
  if (!target || (mentionPicker && !mentionPicker.contains(target))) {
    showMentionPicker.value = false
  }
}

const viewUser = (userId) => {
  if (!userId || Number(userId) <= 0) {
    return
  }
  if (String(userId) === String(userStore.userInfo.userId)) {
    router.push('/userinfo')
  } else {
    router.push('/userinfo?userId=' + userId)
  }
}

const goBack = () => {
  router.back()
}

const scrollToTargetComment = () => {
  const commentId = route.query.commentId
  if (!commentId) return
  nextTick(() => {
    const target = document.getElementById(`comment-${commentId}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

const loadCurrentNews = async () => {
  if (!isNewsDetailRoute()) {
    return
  }
  const newsId = resolveNewsId()
  if (!newsId) {
    detailMissing.value = true
    newsDetail.value = {}
    comments.value = []
    return
  }
  detailMissing.value = false
  newsDetail.value = {}
  comments.value = []
  finished.value = false
  loading.value = false
  queryParams.value.pageNo = 1
  queryParams.value.newsId = newsId
  const detailLoaded = await getNewsDetail(newsId)
  if (detailLoaded) {
    getComments()
  }
}

watch(() => route.fullPath, (path, oldPath) => {
  if (pageActive.value && isNewsDetailRoute() && path !== oldPath) {
    loadCurrentNews()
  }
})

onMounted(() => {
  pageActive.value = true
  loadCurrentNews()
  loadCustomStickers()
  document.addEventListener('click', closeEmojiPicker)
})

onActivated(() => {
  if (pageActive.value) {
    return
  }
  pageActive.value = true
  loadCurrentNews()
})

onDeactivated(() => {
  pageActive.value = false
})

onUnmounted(() => {
  pageActive.value = false
  document.removeEventListener('click', closeEmojiPicker)
})
</script>

<style lang="less" scoped>
.detail-container {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 0;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;

  .detail-content {
    flex: 1;
    min-height: 0;
    max-width: 900px;
    width: 100%;
    margin: 0 auto;
    background: #fff;
    border-radius: 8px;
    padding: 24px 30px;
    box-sizing: border-box;
    overflow-y: auto;
    scrollbar-width: thin;
    scrollbar-color: #cfd6e4 transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background: #cfd6e4;
      border-radius: 999px;
    }
  }
}

.back-row {
  margin-bottom: 12px;
}

.detail-empty {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-header {
  margin-bottom: 24px;

  .article-title {
    font-size: 28px;
    font-weight: 700;
    color: #303133;
    margin: 0 0 16px;
    line-height: 1.4;
  }

  .article-meta {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 18px;
    color: #606266;
    font-size: 14px;
  }

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.article-cover {
  margin-bottom: 24px;
  border-radius: 8px;
  overflow: hidden;

  .el-image {
    width: 100%;
    height: 400px;
  }
}

.article-content {
  color: #333;
  font-size: 16px;
  line-height: 1.8;

  :deep(img) {
    max-width: 100%;
    height: auto;
  }

  :deep(p) {
    margin: 10px 0;
  }
}

.article-tags {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  background: #f0f2f5;
  border: none;
  color: #606266;
}

.article-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 18px 0;
  margin-top: 18px;
  border-top: 1px solid #f0f2f5;
  border-bottom: 1px solid #f0f2f5;
}

.article-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  padding: 7px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover,
  &.active {
    color: #409eff;
    border-color: #409eff;
    background: #ecf5ff;
  }
}

.comments-wrapper {
  padding-top: 18px;
  min-width: 0;
  overflow-x: hidden;
}

.comments-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  padding-bottom: 10px;
}

.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f2f5;
  min-width: 0;

  &.highlighted {
    border-radius: 8px;
    background: #f4f9ff;
    box-shadow: inset 3px 0 0 #409eff;
    padding-left: 10px;
    padding-right: 10px;
  }
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  min-width: 0;
}

.username {
  font-weight: 600;
  color: #303133;
}

.level {
  background: #329afe;
  color: #fff;
  font-size: 11px;
  padding: 1px 4px;
  border-radius: 3px;
}

.time {
  color: #909399;
  font-size: 12px;
  white-space: nowrap;
}

.comment-content {
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
}

.reply-to {
  color: #909399;

  span {
    color: #409eff;
    cursor: pointer;
  }
}

.comment-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;

  &:hover {
    color: #409eff;
  }

  &.delete:hover {
    color: #f56c6c;
  }

  &.report:hover {
    color: #e6a23c;
  }
}

.reply-count {
  display: inline-flex;
  align-items: center;
  height: 18px;
  color: #909399;
  font-size: 13px;
  line-height: 18px;
}

.reply-list {
  margin: 12px 0 0 38px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f7f9fc;
  max-width: calc(100% - 38px);
  box-sizing: border-box;
  overflow-x: hidden;
}

.reply-item {
  padding: 10px 0;
  border-bottom: 1px solid #edf0f5;

  &.highlighted {
    border-radius: 6px;
    background: #ecf5ff;
    padding-left: 8px;
    padding-right: 8px;
  }

  &:last-child {
    border-bottom: none;
  }
}

.view-more,
.load-more {
  text-align: center;
  color: #409eff;
  cursor: pointer;
  padding: 10px 0 0;
  font-size: 13px;
}

.comment-image-wrap {
  margin-top: 8px;
}

.comment-image {
  width: 180px;
  max-width: 100%;
  height: 120px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.reply-image {
  width: 150px;
  height: 100px;
}

.fixed-comment-input {
  position: relative;
  flex: 0 0 auto;
  width: min(900px, calc(100% - 20px));
  margin: 12px auto 0;
  min-height: 145px;
  box-sizing: border-box;
  background: #fff;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  box-shadow: 0 -8px 24px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.reply-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #606266;
  font-size: 14px;
  padding-bottom: 8px;
}

.close-reply {
  cursor: pointer;

  &:hover {
    color: #409eff;
  }
}

.input-wrapper {
  position: relative;
}

.emoji-trigger {
  position: absolute;
  right: 10px;
  top: 10px;
  cursor: pointer;
  z-index: 2;
  font-size: 20px;
}

.comment-image-upload {
  position: absolute;
  right: 42px;
  top: 6px;
  z-index: 2;
}

.sticker-trigger {
  position: absolute;
  right: 74px;
  top: 6px;
  z-index: 2;
}

.emoji-picker {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 100%;
  margin-bottom: 8px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(32px, 1fr));
  gap: 6px;
  z-index: 20;
}

.sticker-panel {
  position: absolute;
  right: 0;
  bottom: 100%;
  width: 360px;
  max-width: 100%;
  max-height: 230px;
  margin-bottom: 8px;
  padding: 12px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  box-sizing: border-box;
  z-index: 20;
}

.sticker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(66px, 1fr));
  gap: 10px;
}

.sticker-item,
.sticker-add {
  width: 66px;
  height: 66px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #edf0f5;
  border-radius: 6px;
  background: #f8fafc;
  box-sizing: border-box;
  cursor: pointer;
}

.sticker-item {
  position: relative;

  img {
    max-width: 56px;
    max-height: 56px;
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

.sticker-empty {
  padding-top: 8px;
  color: #909399;
  font-size: 12px;
  text-align: center;
}

.mention-picker {
  position: absolute;
  left: 0;
  bottom: 100%;
  width: 320px;
  max-height: 260px;
  overflow-y: auto;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
  z-index: 21;
}

.mention-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    background: #f5f7fa;
  }
}

.mention-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  span {
    color: #303133;
    font-size: 14px;
    font-weight: 600;
  }

  small {
    color: #909399;
    font-size: 12px;
  }
}

.mention-empty {
  padding: 12px;
  text-align: center;
  color: #909399;
  font-size: 13px;
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

.comment-image-preview {
  position: relative;
  width: 86px;
  height: 64px;
  margin-top: 8px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;

  .el-image {
    width: 100%;
    height: 100%;
  }
}

.remove-image {
  position: absolute;
  right: 4px;
  top: 4px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  cursor: pointer;
}

.submit-wrapper {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.report-dialog-content {
  display: grid;
  gap: 14px;
}

.report-target {
  color: #303133;
  font-size: 14px;
}

.report-preview {
  margin-top: 8px;
  padding: 10px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  line-height: 1.6;
  max-height: 84px;
  overflow-y: auto;
  word-break: break-word;
}
</style>
