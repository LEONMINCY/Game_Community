<!-- 文件说明：views/forum/index.vue，社区中心首页，展示游戏标签、帖子流、热榜和社区搜索。 -->
<template>
  <div class="app-container">
    <div class="backWrapper">
      <span  class="back" @click="router.back()">&lt; 返回</span>
    </div>

    <div class="carouselWrapper" v-if="hasImages">
      <el-carousel indicator-position="outside">
        <el-carousel-item v-for="item in forumInfo.images" :key="item">
          <el-image fit="cover" preview-teleported style="width: 100%; height: 100%" :preview-src-list="forumInfo.images" :src="item"/>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="userinfoWrapper">
      <div class="userinfo" @click="viewUsers(forumInfo.userId)" style="cursor: pointer;">
        <el-avatar :size="35" :src="forumInfo.userAvatar" />
        <div class="name">{{ forumInfo.nickname }}</div>
        <div class="leave" style="font-size: 5px">Lv{{ forumInfo.userLevel }}</div>
      </div>
<!--      <div class="concern">-->
<!--        <el-button color="#24292e" icon="Plus" size="small" @click="handleConcern">关注</el-button>-->
<!--        <span>已关注</span>-->
<!--      </div>-->
    </div>

    <div class="contentWrapper">
      <div class="title">
        {{ forumInfo.title }}
      </div>
      <div class="content" v-html="forumInfo.content">
      </div>
      <div class="gameTag" @click="goGameCommunity">
        <GameTag :src="gameTag.src" :name="gameTag.name"></GameTag>
      </div>
      <div class="topicWrapper" v-if="getTopics().length">
        <span v-for="topic in getTopics()" :key="topic" class="topicTag" @click="goTopic(topic)">#{{ topic }}</span>
      </div>
    </div>

    <div class="postMetaTime" v-if="postDisplayTime">
      {{ postDisplayTime }}
    </div>

    <div class="postActions">
      <button class="postAction" :class="{ active: forumInfo.isLikes }" @click="handlePostLike">
        <el-icon><Pointer /></el-icon>
        <span>点赞 {{ forumInfo.likes || 0 }}</span>
      </button>
      <button class="postAction" :class="{ active: forumInfo.isFavorites }" @click="handlePostFavorite">
        <el-icon><Star /></el-icon>
        <span>收藏 {{ forumInfo.favorites || 0 }}</span>
      </button>
      <button class="postAction" @click="openShare(forumInfo)">
        <el-icon><Share /></el-icon>
        <span>转发 {{ forumInfo.shareCount || 0 }}</span>
      </button>
    </div>

    <div class="commentsWrapper">
      <div class="commentsHeaderBar">
        <span class="commentsTitle">
          全部评论 {{ comments.length }}
        </span>
        <el-popover
          placement="bottom-start"
          trigger="click"
          width="170"
          popper-class="comment-sort-popover"
        >
          <div class="comment-sort-menu">
            <button
              v-for="option in commentSortOptions"
              :key="option.value"
              type="button"
              class="comment-sort-option"
              :class="{ active: commentSortType === option.value }"
              @click="commentSortType = option.value"
            >
              <span>{{ option.label }}</span>
              <span v-if="commentSortType === option.value" class="sort-check">✓</span>
            </button>
          </div>
          <template #reference>
            <button type="button" class="commentSortTrigger" title="评论排序">
              <i></i>
              <i></i>
              <i></i>
            </button>
          </template>
        </el-popover>
      </div>

      <!-- 修改评论列表部分 -->
      <div class="commentsList" v-infinite-scroll="loadMore" infinite-scroll-distance="10" infinite-scroll-disabled="disabled"
      infinite-scroll-immediate="false">
        <div
          v-for="comment in sortedComments"
          :id="`comment-${comment.id}`"
          :key="comment.id"
          class="commentItem"
          :class="{ highlighted: String(route.query.commentId || '') === String(comment.id) }"
        >
          <!-- 主评论 -->
          <div class="commentHeader">
            <div class="userInfo" @click="viewUsers(comment.userId)" style="cursor: pointer;">
              <div v-if="isAiUser(comment.userId)" class="ai-avatar">AI</div>
              <el-avatar v-else :size="30" :src="comment.avatar" />
              <span class="username">{{ comment.nickname }}</span>
              <span v-if="!isAiUser(comment.userId)" class="level">Lv{{ comment.userLevel ?? comment.level ?? 0 }}</span>
            </div>
            <div class="time">{{ comment.createTime }}</div>
          </div>
          <div class="commentContent">
            <template v-if="comment.replyUserNickname">
              <span class="reply-to">回复 <span class="reply-name" @click="viewUsers(comment.replyUserId)">@{{ comment.replyUserNickname }}</span>：</span>
            </template>
            {{ comment.content }}
            <div v-if="comment.imageUrl" class="comment-image-wrap">
              <el-image
                :src="comment.imageUrl"
                :preview-src-list="[comment.imageUrl]"
                preview-teleported
                class="comment-image"
                fit="cover"
              />
            </div>
          </div>
          <div class="commentActions">
            <span v-if="canLikeComment(comment)" class="action like" :class="{ active: comment.isLiked }" @click="toggleCommentLike(comment)">
              <el-icon><Pointer /></el-icon>
              点赞 {{ comment.likes || 0 }}
            </span>
            <span v-if="canReplyComment(comment)" class="action" @click="replyToComment(comment)">
              <el-icon><ChatLineRound /></el-icon>
              回复
            </span>
            <span class="action report" v-if="canReportComment(comment)" @click="openReportDialog(comment)">
              <el-icon><Warning /></el-icon>
              举报
            </span>
            <!-- 添加删除评论按钮，仅当评论是当前用户发表的才显示 -->
            <span class="action delete" v-if="comment.userId === userStore.userInfo.userId" @click="deleteComment(comment)">
              <el-icon><Delete /></el-icon>
              删除
            </span>
            <span v-if="comment.replyCount > 0" class="reply-count">
              {{ comment.replyCount }}条回复
            </span>
          </div>

          <!-- 子评论列表 -->
          <div v-if="comment.children && comment.children.length > 0" class="replyList">
            <div
              v-for="reply in comment.children"
              :id="`comment-${reply.id}`"
              :key="reply.id"
              class="replyItem"
              :class="{ highlighted: String(route.query.commentId || '') === String(reply.id) }"
            >
              <div class="commentHeader">
                <div class="userInfo" @click="viewUsers(reply.userId)" style="cursor: pointer;">
                  <div v-if="isAiUser(reply.userId)" class="ai-avatar small">AI</div>
                  <el-avatar v-else :size="24" :src="reply.avatar" />
                  <span class="username">{{ reply.nickname }}</span>
                  <span v-if="!isAiUser(reply.userId)" class="level">Lv{{ reply.userLevel ?? reply.level ?? 0 }}</span>
                </div>
                <div class="time">{{ reply.createTime }}</div>
              </div>
              <div class="commentContent">
                <template v-if="reply.replyUserNickname">
                  <span class="reply-to">回复 <span class="reply-name" @click="viewUsers(reply.replyUserId)">@{{ reply.replyUserNickname }}</span>：</span>
                </template>
                {{ reply.content }}
                <div v-if="reply.imageUrl" class="comment-image-wrap">
                  <el-image
                    :src="reply.imageUrl"
                    :preview-src-list="[reply.imageUrl]"
                    preview-teleported
                    class="comment-image reply-image"
                    fit="cover"
                  />
                </div>
              </div>
              <div class="commentActions">
                <span v-if="canLikeComment(reply)" class="action like" :class="{ active: reply.isLiked }" @click="toggleCommentLike(reply)">
                  <el-icon><Pointer /></el-icon>
                  点赞 {{ reply.likes || 0 }}
                </span>
                <span v-if="canReplyComment(reply)" class="action" @click="replyToComment(reply)">
                  <el-icon><ChatLineRound /></el-icon>
                  回复
                </span>
                <span class="action report" v-if="canReportComment(reply)" @click="openReportDialog(reply)">
                  <el-icon><Warning /></el-icon>
                  举报
                </span>
                <!-- 添加删除子评论按钮，仅当评论是当前用户发表的才显示 -->
                <span class="action delete" v-if="reply.userId === userStore.userInfo.userId" @click="deleteComment(reply)">
                  <el-icon><Delete /></el-icon>
                  删除
                </span>
              </div>
            </div>

            <!-- 查看更多回复 -->
            <div v-if="comment.replyCount > comment.children.length" class="view-more" @click="loadMoreReplies(comment)">
              查看更多回复
            </div>
          </div>
        </div>

        <!-- 添加加载状态提示 -->
        <div class="loading-status" v-if="loading || finished">
          <p v-if="loading">加载中...</p>
          <p v-if="finished">没有更多评论了</p>
        </div>
      </div>
    </div>

    <el-dialog v-model="reportDialogVisible" title="举报评论" width="420px">
      <el-form label-width="70px">
        <el-form-item label="用户">
          <span>{{ reportTarget?.nickname }}</span>
        </el-form-item>
        <el-form-item label="评论">
          <div class="report-preview">{{ reportTarget?.content }}</div>
        </el-form-item>
        <el-form-item label="原因">
          <el-input
            v-model="reportReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入举报原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <PostShareDialog v-model="shareDialogVisible" :post="shareTarget" @shared="handleShared" />

    <!-- 固定在底部的评论输入框 -->
    <div class="fixedCommentInput">
      <div class="reply-info" v-if="replyTo">
        回复 @{{ replyTo.nickname }}：
        <el-icon class="close-reply" @click="cancelReply"><Close /></el-icon>
      </div>
      <div class="input-wrapper">
        <div class="emoji-trigger" @click="toggleEmojiPanel">
          😊
        </div>
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
          @keydown="handleMentionKeydown"
        />
        <!-- 表情选择面板 -->
        <div v-if="showEmojiPicker" class="emoji-picker">
          <div class="emoji-list">
            <span
              v-for="emoji in emojiList"
              :key="emoji"
              class="emoji-item"
              @click="insertEmoji(emoji)"
            >
              {{ emoji }}
            </span>
          </div>
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
            v-for="(option, index) in mentionOptions"
            :key="option.key"
            class="mention-option"
            :class="{ active: index === mentionActiveIndex }"
            @mouseenter="mentionActiveIndex = index"
            @click="insertMention(option)"
          >
            <div v-if="option.isAi" class="ai-avatar mention">AI</div>
            <el-avatar v-else :size="28" :src="option.avatar" />
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
      <div class="submitWrapper">
        <el-button type="primary" :loading="commentSubmitting" @click="submitComment">
          {{ commentSubmitting ? (submittingAiReply ? 'AI回复中...' : '提交中...') : (replyTo ? '回复' : '发表评论') }}
        </el-button>
      </div>
    </div>
  </div>
</template>
<script setup>
import GameTag from "../../components/gameTag.vue";
import { computed, nextTick, ref, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getRequest, postRequest, deleteRequest } from '@/utils/http'
import { emojiList } from '@/utils/emoji'
import { replaceURL } from '@/utils/tools'
import { useUserStore } from "@/store/index.js";
import { tr } from "element-plus/es/locales.mjs";
import { ElInfiniteScroll } from 'element-plus'
import { ChatLineRound, Close, Delete, MagicStick, Picture, Plus, Pointer, Share, Star, Warning } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import PostShareDialog from '@/components/PostShareDialog.vue'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const forumInfo = ref({})
const hasImages = ref(true)
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  postId: route.query.id
})
const loading = ref(false)
const finished = ref(false)
const comments = ref([])
const targetCommentLocating = ref(false)
const reportDialogVisible = ref(false)
const reportTarget = ref(null)
const reportReason = ref('')
const shareDialogVisible = ref(false)
const shareTarget = ref(null)
const commentImageUrl = ref('')
const followingMentionUsers = ref([])
const showMentionPicker = ref(false)
const mentionKeyword = ref('')
const mentionActiveIndex = ref(0)
const commentSubmitting = ref(false)
const submittingAiReply = ref(false)
const headers = ref({
  Authorization: localStorage.getItem('token')
})

const imgUrls = ref([ ])

const gameTag = ref({
  id: "",
  src: "",
  name: ""
})

const newComment = ref('')

const replyTo = ref(null) // 当前回复的评论对象

const showEmojiPicker = ref(false)
const showStickerPanel = ref(false)
const customStickers = ref([])
const commentSortType = ref('default')
const commentSortOptions = [
  { value: 'default', label: '默认' },
  { value: 'latest', label: '最新' },
  { value: 'likes', label: '最多点赞' }
]

const aiMentionOption = {
  key: 'ai-assistant',
  userId: null,
  nickname: 'AI助手',
  avatar: '',
  isAi: true
}

const sortedComments = computed(() => {
  const list = [...comments.value]
  if (commentSortType.value === 'latest') {
    return list.sort((left, right) => parseDateValue(right.createTime) - parseDateValue(left.createTime))
  }
  if (commentSortType.value === 'likes') {
    return list.sort((left, right) => {
      const likeDelta = Number(right.likes || 0) - Number(left.likes || 0)
      return likeDelta !== 0 ? likeDelta : parseDateValue(right.createTime) - parseDateValue(left.createTime)
    })
  }
  return list
})

const postDisplayTime = computed(() => {
  const created = forumInfo.value.createTime
  const updated = forumInfo.value.updateTime
  if (isEditedPost(created, updated)) {
    return `编辑于 ${formatPostTimeline(updated)}`
  }
  return formatPostTimeline(created)
})

const parseDateValue = (value) => {
  const date = parseDate(value)
  return date ? date.getTime() : 0
}

const parseDate = (value) => {
  if (!value) return null
  if (value instanceof Date) return value
  const normalized = String(value).replace(/-/g, '/').replace('T', ' ')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

const isEditedPost = (created, updated) => {
  const createdDate = parseDate(created)
  const updatedDate = parseDate(updated)
  if (!createdDate || !updatedDate) return false
  return updatedDate.getTime() - createdDate.getTime() > 60 * 1000
}

const formatPostTimeline = (value) => {
  const date = parseDate(value)
  if (!date) return ''
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const startOfTarget = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()
  const dayDelta = Math.floor((startOfToday - startOfTarget) / 86400000)
  const timeText = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  if (dayDelta === 0) {
    return `今天 ${timeText}`
  }
  if (dayDelta === 1) {
    return `昨天 ${timeText}`
  }
  const monthDay = `${date.getMonth() + 1}-${String(date.getDate()).padStart(2, '0')}`
  return date.getFullYear() === now.getFullYear() ? monthDay : `${date.getFullYear()}-${monthDay}`
}

const isForumDetailRoute = () => route.path === '/forum'

// 注册 infinite-scroll 指令
const vInfiniteScroll = ElInfiniteScroll

onMounted(() => {
  if (!isForumDetailRoute()) {
    return
  }
  const id = route.query.id
  getForumInfo(id)
  getComments() // 初始加载评论
  loadCustomStickers()
  document.addEventListener('click', closeEmojiPicker)
})


const getForumInfo = (id) => {
  getRequest('/post/get/' + id, {
    id: id.value
  }).then(res => {
    if (res.code === 200) {
      forumInfo.value = res.data
      forumInfo.value.likes = res.data.likes || 0
      forumInfo.value.favorites = res.data.favorites || 0
      forumInfo.value.shareCount = res.data.shareCount || 0
      forumInfo.value.isLikes = res.data.isLikes || false
      forumInfo.value.isFavorites = res.data.isFavorites || false
      // 只将media作为轮播图的图片
      forumInfo.value.images = JSON.parse(res.data.media || '[]')
      forumInfo.value.images = forumInfo.value.images.length > 0 ? forumInfo.value.images.map(img => replaceURL(img)) : []
      forumInfo.value.userAvatar = replaceURL(res.data.userAvatar)
      // 只有当封面图片存在时才显示轮播图
      hasImages.value = forumInfo.value.images && forumInfo.value.images.length > 0
      gameTag.value = {
        id: res.data.gameId,
        src: res.data.gameIcon,
        name: res.data.gameName
      }
    }
  })
}

const handlePostLike = () => {
  if (!forumInfo.value.id) return
  const url = forumInfo.value.isLikes ? '/post/unlike/' : '/post/like/'
  postRequest(url + forumInfo.value.id).then(res => {
    if (res.code === 200) {
      forumInfo.value.isLikes = !forumInfo.value.isLikes
      forumInfo.value.likes = Math.max(0, (forumInfo.value.likes || 0) + (forumInfo.value.isLikes ? 1 : -1))
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const handlePostFavorite = () => {
  if (!forumInfo.value.id) return
  const url = forumInfo.value.isFavorites ? '/post/unFavorite/' : '/post/favorite/'
  postRequest(url + forumInfo.value.id).then(res => {
    if (res.code === 200) {
      forumInfo.value.isFavorites = !forumInfo.value.isFavorites
      forumInfo.value.favorites = Math.max(0, (forumInfo.value.favorites || 0) + (forumInfo.value.isFavorites ? 1 : -1))
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const openShare = (post) => {
  shareTarget.value = post
  shareDialogVisible.value = true
}

const handleShared = (shareCount) => {
  forumInfo.value.shareCount = shareCount
}

const getTopics = () => {
  return String(forumInfo.value.topics || '')
    .split(',')
    .map(topic => topic.trim())
    .filter(Boolean)
}

const goTopic = (topic) => {
  router.push({ path: '/forumlist', query: { topic } })
}

const goGameCommunity = () => {
  if (gameTag.value.id) {
    router.push({ path: '/forumlist', query: { gameId: gameTag.value.id } })
  }
}

const getComments = (options = {}) => {
  if (loading.value || finished.value) return Promise.resolve()

  loading.value = true
  return postRequest('/comment/list', queryParams.value).then(res => {
    if (res.code === 200) {
      const data = res.data.list.map(item => {
        return {
          ...item,
          avatar: replaceURL(item.avatar),
          imageUrl: item.imageUrl ? replaceURL(item.imageUrl) : '',
          likes: Number(item.likes || 0),
          isLiked: item.isLiked === true || item.isLiked === 1 || item.isLiked === '1'
        }
      })
      const newComments = data || []
      if (queryParams.value.pageNo === 1) {
        comments.value = newComments
      } else {
        comments.value = [...comments.value, ...newComments]
      }

      // 判断是否还有更多数据
      if (newComments.length < queryParams.value.pageSize) {
        finished.value = true
      }
      queryParams.value.pageNo++
    }
  }).finally(() => {
    loading.value = false
    if (options.locateTarget !== false) {
      locateTargetComment()
    }
  })
}

// 回复评论
const replyToComment = (comment) => {
  if (!canReplyComment(comment)) {
    ElMessage.warning('不能回复自己的评论')
    return
  }
  // 如果是回复子评论，需要保持原有的父评论ID
  if (comment.parentId) {
    replyTo.value = {
      ...comment,
      id: comment.parentId // 使用原始父评论的ID作为parentId
    }
  } else {
    replyTo.value = comment
  }
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
  newComment.value = ''
}

// 修改提交评论方法
const submitComment = () => {
  if (commentSubmitting.value) {
    return
  }
  if (!newComment.value.trim() && !commentImageUrl.value) {
    ElMessage.warning('评论内容或图片不能都为空')
    return
  }

  const shouldTriggerAi = hasAiMention()
  const commentData = {
    postId: route.query.id,
    content: newComment.value,
    imageUrl: commentImageUrl.value,
    parentId: replyTo.value?.id || null,
    replyUserId: replyTo.value?.userId || null,
    mentionUserIds: collectMentionUserIds(),
    mentionAi: shouldTriggerAi
  }

  commentSubmitting.value = true
  submittingAiReply.value = shouldTriggerAi
  postRequest('/comment/add', commentData).then(res => {
    if (res.code === 200) {
      // 构建新评论对象
      const newCommentObj = {
        id: res.data || comments.value.length + 1,
        nickname: userStore.userInfo.nickname,
        avatar: replaceURL(userStore.userInfo.avatar),
        userLevel: userStore.userInfo.ex2 || userStore.userInfo.level || 0,
        content: newComment.value,
        imageUrl: commentImageUrl.value,
        createTime: new Date().toLocaleString(),
        userId: userStore.userInfo.userId,
        parentId: replyTo.value?.id || null,
        replyUserNickname: replyTo.value?.nickname,
        replyUserId: replyTo.value?.userId,
        likes: 0,
        isLiked: false
      }

      // 回复子评论时仍然回显在所属主评论的回复列表里，避免用户发完后需要刷新页面。
      if (replyTo.value?.parentId) {
        const parentComment = comments.value.find(c => c.id === replyTo.value.parentId)
        if (parentComment) {
          if (!parentComment.children) {
            parentComment.children = []
          }
          parentComment.children.push(newCommentObj)
          parentComment.replyCount = (parentComment.replyCount || 0) + 1
        }
      } else {
        if (replyTo.value) {
          // 回复主评论
          const parentComment = comments.value.find(c => c.id === replyTo.value.id)
          if (parentComment) {
            if (!parentComment.children) {
              parentComment.children = []
            }
            parentComment.children.push(newCommentObj)
            parentComment.replyCount = (parentComment.replyCount || 0) + 1
          }
        } else {
          // 新评论
          comments.value.unshift(newCommentObj)
        }
      }

      newComment.value = ''
      commentImageUrl.value = ''
      replyTo.value = null
      if (commentData.mentionAi) {
        resetComments()
      }
      ElMessage.success('回复成功')
    }
  }).catch(err => {
    ElMessage.error('评论失败')
  }).finally(() => {
    commentSubmitting.value = false
    submittingAiReply.value = false
  })
}

const canLikeComment = (comment) => {
  const currentUserId = Number(userStore.userInfo?.userId)
  return Number.isFinite(currentUserId) && currentUserId > 0 && Number(comment?.userId) !== currentUserId && !isAiUser(comment?.userId)
}

// 评论点赞需要落库并触发通知，前端只在接口成功后更新本地数量。
const toggleCommentLike = (comment) => {
  if (!canLikeComment(comment)) {
    return
  }
  const liked = comment.isLiked === true
  const requestUrl = liked ? `/comment/unlike/${comment.id}` : `/comment/like/${comment.id}`
  postRequest(requestUrl).then(res => {
    if (res.code === 200) {
      comment.isLiked = !liked
      comment.likes = Number(res.data ?? 0)
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  }).catch(() => {
    ElMessage.error('操作失败')
  })
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
    reportType: 'COMMENT',
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

// 添加加载更多方法
const loadMore = () => {
  console.log('loadMore')
  getComments()
}

// 添加 viewUsers 方法
const viewUsers = (userId) => {
  if (!userId || Number(userId) <= 0) {
    return
  }
  if (userStore.userInfo.userId === userId) {
    router.push('/userinfo')
  } else {
    router.push('/userinfo?userId=' + userId)
  }
}

// 加载更多回复
const loadMoreReplies = async (comment) => {
  const res = await getRequest(`/comment/reply/list/${comment.id}?pageNo=1&pageSize=999`)
  if (res.code === 200) {
    comment.children = (res.data.list || []).map(item => ({
      ...item,
      avatar: replaceURL(item.avatar),
      imageUrl: item.imageUrl ? replaceURL(item.imageUrl) : '',
      likes: Number(item.likes || 0),
      isLiked: item.isLiked === true || item.isLiked === 1 || item.isLiked === '1'
    }))
  }
}

const canReplyComment = (comment) => {
  return Number(comment?.userId) !== Number(userStore.userInfo?.userId)
}

const canReportComment = (comment) => {
  return Number(comment?.userId) !== Number(userStore.userInfo?.userId) && !isAiUser(comment?.userId)
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

const handleCommentImageSuccess = (res) => {
  if (res.code === 200) {
    commentImageUrl.value = replaceURL(res.data)
    ElMessage.success('图片已添加')
  } else {
    ElMessage.error(res.msg || '图片上传失败')
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
  const textarea = document.querySelector('.fixedCommentInput textarea')
  const cursor = textarea ? textarea.selectionStart : newComment.value.length
  const beforeCursor = newComment.value.slice(0, cursor)
  const match = beforeCursor.match(/@([^\s@，,：:]*)$/)
  if (!match) {
    showMentionPicker.value = false
    return
  }
  mentionKeyword.value = match[1] || ''
  showMentionPicker.value = true
  mentionActiveIndex.value = 0
  loadMentionUsers()
}

const mentionOptions = computed(() => {
  const keyword = mentionKeyword.value.trim().toLowerCase()
  const users = followingMentionUsers.value.filter(user => {
    const nickname = String(user.nickname || '').toLowerCase()
    return !keyword || nickname.includes(keyword)
  })
  const aiVisible = !keyword || aiMentionOption.nickname.toLowerCase().includes(keyword) || 'ai'.includes(keyword)
  return aiVisible ? [aiMentionOption, ...users] : users
})

const handleMentionKeydown = (event) => {
  if (!showMentionPicker.value) {
    return
  }
  const options = mentionOptions.value
  if (event.key === 'Escape') {
    showMentionPicker.value = false
    return
  }
  if (!options.length || !['ArrowDown', 'ArrowUp', 'Enter', 'Tab'].includes(event.key)) {
    return
  }
  event.preventDefault()
  if (event.key === 'ArrowDown') {
    mentionActiveIndex.value = (mentionActiveIndex.value + 1) % options.length
    return
  }
  if (event.key === 'ArrowUp') {
    mentionActiveIndex.value = (mentionActiveIndex.value - 1 + options.length) % options.length
    return
  }
  insertMention(options[mentionActiveIndex.value] || options[0])
}

const insertMention = (option) => {
  const textarea = document.querySelector('.fixedCommentInput textarea')
  const cursor = textarea ? textarea.selectionStart : newComment.value.length
  const beforeCursor = newComment.value.slice(0, cursor)
  const afterCursor = newComment.value.slice(cursor)
  const nextBefore = beforeCursor.replace(/@([^\s@，,：:]*)$/, `@${option.nickname} `)
  newComment.value = nextBefore + afterCursor
  showMentionPicker.value = false
  requestAnimationFrame(() => {
    const nextCursor = nextBefore.length
    const input = document.querySelector('.fixedCommentInput textarea')
    input?.focus()
    input?.setSelectionRange(nextCursor, nextCursor)
  })
}

const collectMentionUserIds = () => {
  return followingMentionUsers.value
    .filter(user => newComment.value.includes(`@${user.nickname}`))
    .map(user => user.userId)
}

const isAiUser = (userId) => {
  return userId !== null && userId !== undefined && Number(userId) <= 0
}

const hasAiMention = () => {
  return isAiUser(replyTo.value?.userId) || newComment.value.includes('@AI助手') || /@AI(\s|$)/i.test(newComment.value)
}

// 插入表情
const insertEmoji = (emoji) => {
  const textarea = document.querySelector('.fixedCommentInput textarea')
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const text = newComment.value
  newComment.value = text.substring(0, start) + emoji + text.substring(end)
  // 关闭表情选择器
  showEmojiPicker.value = false
  showStickerPanel.value = false
}

// 点击其他地方关闭表情选择器
const closeEmojiPicker = (e) => {
  const picker = document.querySelector('.emoji-picker')
  const trigger = document.querySelector('.emoji-trigger')
  if (picker && !picker.contains(e.target) && !trigger.contains(e.target)) {
    showEmojiPicker.value = false
  }
  const stickerPanel = document.querySelector('.sticker-panel')
  const stickerTrigger = document.querySelector('.sticker-trigger')
  if (stickerPanel && !stickerPanel.contains(e.target) && stickerTrigger && !stickerTrigger.contains(e.target)) {
    showStickerPanel.value = false
  }
  const mentionPicker = document.querySelector('.mention-picker')
  if (mentionPicker && !mentionPicker.contains(e.target)) {
    showMentionPicker.value = false
  }
}

// 组件卸载时移除监听
onUnmounted(() => {
  document.removeEventListener('click', closeEmojiPicker)
})

// 添加删除评论的方法
const deleteComment = (comment) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    // 调用删除评论的API
    deleteRequest(`/comment/delete/${comment.id}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功')
        
        // 更新本地评论列表
        if (comment.parentId) {
          // 如果是子评论，从父评论的children数组中移除
          const parentComment = comments.value.find(c => c.id === comment.parentId)
          if (parentComment && parentComment.children) {
            const index = parentComment.children.findIndex(c => c.id === comment.id)
            if (index !== -1) {
              parentComment.children.splice(index, 1)
              parentComment.replyCount = (parentComment.replyCount || 1) - 1
            }
          }
        } else {
          // 如果是主评论，直接从评论列表中移除
          const index = comments.value.findIndex(c => c.id === comment.id)
          if (index !== -1) {
            comments.value.splice(index, 1)
          }
        }
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    }).catch(err => {
      ElMessage.error('删除失败')
    })
  }).catch(() => {
    // 用户取消删除操作
  })
}

const resetComments = () => {
  queryParams.value.pageNo = 1
  finished.value = false
  comments.value = []
  getComments()
}

const findLoadedComment = (commentId) => {
  const targetId = String(commentId || '')
  for (const comment of comments.value) {
    if (String(comment.id) === targetId) {
      return { comment, parent: null }
    }
    const child = (comment.children || []).find(reply => String(reply.id) === targetId)
    if (child) {
      return { comment: child, parent: comment }
    }
  }
  return null
}

const findRootComment = (commentId) => {
  const targetId = String(commentId || '')
  return comments.value.find(comment => String(comment.id) === targetId)
}

const locateTargetComment = async () => {
  const commentId = route.query.commentId
  if (!commentId || targetCommentLocating.value) return
  targetCommentLocating.value = true
  try {
    let loaded = findLoadedComment(commentId)
    if (!loaded) {
      const detail = await getRequest(`/comment/get/${commentId}`, { silentError: true }).catch(() => null)
      const target = detail?.code === 200 ? detail.data : null
      const rootId = target?.parentId || target?.id || commentId

      for (let i = 0; i < 30 && !findRootComment(rootId) && !finished.value; i++) {
        await getComments({ locateTarget: false })
      }

      const rootComment = findRootComment(rootId)
      if (rootComment && target?.parentId) {
        await loadMoreReplies(rootComment)
      }
      loaded = findLoadedComment(commentId)
    }
    if (loaded) {
      scrollToTargetComment()
    }
  } finally {
    targetCommentLocating.value = false
  }
}

const scrollToTargetComment = () => {
  const commentId = route.query.commentId
  if (!commentId) return
  nextTick(() => {
    const target = document.getElementById(`comment-${commentId}`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

watch(
  () => [route.path, route.query.id, route.query.commentId],
  ([path, postId, commentId], oldValue = []) => {
    const [oldPath, oldPostId] = oldValue
    if (path !== '/forum') return
    if (!postId) return
    if (String(postId) !== String(oldPostId || '') || path !== oldPath) {
      queryParams.value.postId = postId
      queryParams.value.pageNo = 1
      finished.value = false
      comments.value = []
      getForumInfo(postId)
      getComments()
      return
    }
    if (commentId) {
      locateTargetComment()
    }
  }
)
</script>
<style lang="less" scoped>
.app-container {
  width: 100%;
  height: calc(100vh - 110px);
  position: relative;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;

  .ai-avatar {
    width: 30px;
    height: 30px;
    flex: 0 0 30px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #fff;
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0;
    background: linear-gradient(135deg, #2563eb 0%, #8b5cf6 55%, #14b8a6 100%);
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.22);

    &.small {
      width: 24px;
      height: 24px;
      flex-basis: 24px;
      font-size: 10px;
    }

    &.mention {
      width: 28px;
      height: 28px;
      flex-basis: 28px;
      font-size: 11px;
    }
  }

  .backWrapper {
    font-size: 14px;
    font-weight: 550;
    // position: absolute;
    // left: 15px;
    // top: 15px;

    .back {
      background-color: #f7f8f9;
      padding: 5px 10px;
      cursor: pointer;
      border-radius: 5px;
    }
  }

  .carouselWrapper {
    box-sizing: border-box;
    padding: 20px 15px 0 15px ;
    width: 100%;
    height: 400px;
  }

  .userinfoWrapper {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 15px;
    box-sizing: border-box;

    .userinfo {
      display: flex;
      align-items: center;
      box-sizing: border-box;

      .name {
        margin-left: 6px;
        color: rgb(100, 105, 110);
        font-size: 14px;
        font-weight: 600;
      }
      .leave {
        background-color: #329afe;
        color: #fff;
        font-size: 10px !important;
        margin-left: 8px;
        height: 12px;
        line-height: 12px;
        padding: 1px 2px;
        border-radius: 3px;

      }

    }
  }

  .contentWrapper {
    padding: 15px;
    width: 100%;
    box-sizing: border-box;
    border-bottom: 4px solid #f7f8f9;
    .title {
      font-size: 20px;
      font-weight: 600;
      margin-bottom: 10px;
    }
    .content {
      font-size: 16px;
      color: rgb(100, 105, 110);
      margin-bottom: 10px;
      line-height: 1.6;

      :deep(img) {
        max-width: 100%;
        height: auto;
      }

      :deep(p) {
        margin: 10px 0;
      }

      :deep(a) {
        color: #409eff;
        text-decoration: none;
        &:hover {
          text-decoration: underline;
        }
      }
    }

    .gameTag {
      margin-top: 10px;
      display: inline-block;
      cursor: pointer;
    }

    .topicWrapper {
      display: inline-flex;
      flex-wrap: wrap;
      gap: 8px;
      margin: 10px 0 0 10px;
    }

    .topicTag {
      padding: 6px 10px;
      border-radius: 5px;
      background: #f4f6f8;
      color: #606266;
      font-size: 12px;
      cursor: pointer;

      &:hover {
        color: #409eff;
        background: #ecf5ff;
      }
    }
  }

  .postActions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding: 12px 15px;
    border-bottom: 4px solid #f7f8f9;
    box-sizing: border-box;

    .postAction {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      border: 1px solid #e4e7ed;
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
  }

  .postMetaTime {
    padding: 12px 15px 0;
    color: #909399;
    font-size: 13px;
    line-height: 20px;
  }

  .commentsWrapper {
    padding: 15px;
    width: 100%;
    min-width: 0;
    box-sizing: border-box;
    overflow-x: hidden;

    .commentsHeaderBar {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      min-height: 38px;

      .commentsTitle {
        font-size: 14px;
        font-weight: 700;
        border-bottom: 2px solid #000;
        padding: 8px 0;
      }

      .commentSortTrigger {
        width: 24px;
        height: 24px;
        display: inline-flex;
        flex-direction: column;
        justify-content: center;
        gap: 3px;
        border: 0;
        background: transparent;
        padding: 0;
        cursor: pointer;

        i {
          display: block;
          height: 2px;
          border-radius: 999px;
          background: #8f98a8;
        }

        i:nth-child(1) {
          width: 18px;
        }

        i:nth-child(2) {
          width: 13px;
        }

        i:nth-child(3) {
          width: 8px;
        }

        &:hover i {
          background: #303133;
        }
      }
    }

    .commentsList {
      margin-top: 15px;
      padding-bottom: 20px;
      min-width: 0;
      overflow-x: hidden;
      .commentItem {
        padding: 15px 0;
        border-bottom: 1px solid #f0f0f0;
        min-width: 0;

        &.highlighted {
          border-radius: 8px;
          background: #f4f9ff;
          box-shadow: inset 3px 0 0 #409eff;
          padding-left: 10px;
          padding-right: 10px;
        }

        .commentHeader {
          display: flex;
          justify-content: space-between;
          align-items: center;
          gap: 12px;
          margin-bottom: 8px;

          .userInfo {
            display: flex;
            align-items: center;
            min-width: 0;
            flex: 1;

            .username {
              margin: 0 8px;
              font-weight: 500;
              color: #333;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .level {
              background-color: #329afe;
              color: #fff;
              font-size: 12px;
              padding: 1px 4px;
              border-radius: 3px;
            }
          }

          .time {
            color: #999;
            font-size: 12px;
            flex: 0 0 auto;
            white-space: nowrap;
          }
        }

        .commentContent {
          margin: 8px 0;
          color: #666;
          font-size: 14px;
          line-height: 1.6;

          .reply-to {
            color: #999;
            margin-right: 5px;

            .reply-name {
              color: #409eff;
              cursor: pointer;

              &:hover {
                text-decoration: underline;
              }
            }
          }
        }

        .commentActions {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 15px;

          .action {
            display: flex;
            align-items: center;
            gap: 4px;
            color: #999;
            cursor: pointer;
            font-size: 13px;

            &:hover {
              color: #409eff;
            }

            &.delete {
              &:hover {
                color: #f56c6c;
              }
            }

            &.report {
              &:hover {
                color: #e6a23c;
              }
            }

            .el-icon {
              font-size: 16px;
            }
          }
        }
      }
    }

    .loading-status {
      text-align: center;
      padding: 10px 0;
      color: #999;
      font-size: 14px;
    }
  }

  .fixedCommentInput {
    position: sticky;
    bottom: 0;
    left: 0;
    right: 0;
    width: 100%;
    min-height: 145px;
    box-sizing: border-box;
    background-color: #fff;
    padding: 15px;
    z-index: 100;
    margin-top: auto;

    .submitWrapper {
      margin-top: 10px;
      display: flex;
      justify-content: flex-end;
    }

    .reply-info {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 0;
      color: #666;
      font-size: 14px;

      .close-reply {
        cursor: pointer;
        &:hover {
          color: #409eff;
        }
      }
    }

    .input-wrapper {
      position: relative;

      .emoji-trigger {
        position: absolute;
        right: 10px;
        top: 10px;
        cursor: pointer;
        z-index: 1;
        font-size: 20px;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;

        &:hover {
          transform: scale(1.1);
        }
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
        bottom: 100%;
        left: 0;
        width: 100%;
        max-height: 200px;
        background: #fff;
        border: 1px solid #eee;
        border-radius: 8px;
        box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
        padding: 10px;
        margin-bottom: 10px;
        overflow-y: auto;
        z-index: 1000;

        .emoji-list {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(30px, 1fr));
          gap: 5px;

          .emoji-item {
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            padding: 5px;
            border-radius: 4px;
            transition: all 0.3s;
            font-size: 20px;

            &:hover {
              background-color: #f5f7fa;
            }
          }
        }

        &::-webkit-scrollbar {
          width: 6px;
        }

        &::-webkit-scrollbar-thumb {
          background-color: #ddd;
          border-radius: 3px;
        }
      }

      .sticker-panel {
        position: absolute;
        right: 0;
        bottom: 100%;
        width: 360px;
        max-width: 100%;
        max-height: 230px;
        margin-bottom: 10px;
        padding: 12px;
        overflow-y: auto;
        border: 1px solid #ebeef5;
        border-radius: 8px;
        background: #fff;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        box-sizing: border-box;
        z-index: 1000;
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
        margin-bottom: 10px;
        padding: 8px;
        border-radius: 8px;
        border: 1px solid #ebeef5;
        background: #fff;
        box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
        z-index: 1001;
      }

      .mention-option {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px;
        border-radius: 6px;
        cursor: pointer;

        &:hover,
        &.active {
          background: #f5f7fa;
        }
      }

      .mention-meta {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;

        span {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
        }

        small {
          font-size: 12px;
          color: #909399;
        }
      }

      .mention-empty {
        padding: 12px;
        color: #909399;
        font-size: 13px;
        text-align: center;
      }
    }
  }
}

.commentContent {
  margin: 8px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;

  .reply-to {
    color: #999;
    margin-right: 5px;

    .reply-name {
      color: #409eff;
      cursor: pointer;

      &:hover {
        text-decoration: underline;
      }
    }
  }
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
}

.reply-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
  font-size: 14px;

  .close-reply {
    cursor: pointer;
    &:hover {
      color: #409eff;
    }
  }
}

.commentActions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 8px;

  .action {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #999;
    cursor: pointer;
    font-size: 13px;

    &:hover {
      color: #409eff;
    }

    &.delete {
      &:hover {
        color: #f56c6c;
      }
    }

    &.report {
      &:hover {
        color: #e6a23c;
      }
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

.replyList {
  margin-left: 40px;
  margin-top: 10px;
  padding: 10px;
  background-color: #f7f8f9;
  border-radius: 8px;
  max-width: calc(100% - 40px);
  box-sizing: border-box;
  overflow-x: hidden;

  .replyItem {
    padding: 10px 0;
    border-bottom: 1px solid #eee;

    &.highlighted {
      border-radius: 6px;
      background: #ecf5ff;
      padding-left: 8px;
      padding-right: 8px;
    }

    &:last-child {
      border-bottom: none;
    }

    .commentHeader {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;

      .userInfo {
        min-width: 0;
        flex: 1;

        .el-avatar {
          width: 24px;
          height: 24px;
        }

        .username {
          font-size: 13px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .level {
          font-size: 11px;
          padding: 0 3px;
        }
      }

      .time {
        flex: 0 0 auto;
        white-space: nowrap;
        font-size: 12px;
      }
    }

    .commentContent {
      font-size: 14px;
    }

    .commentActions {
      margin-top: 5px;

      .action {
        font-size: 13px;
      }
    }
  }

  .view-more {
    text-align: center;
    padding: 10px 0 0;
    color: #409eff;
    cursor: pointer;
    font-size: 13px;

    &:hover {
      text-decoration: underline;
    }
  }
}

.reply-count {
  display: inline-flex;
  align-items: center;
  height: 18px;
  color: #999;
  font-size: 13px;
  line-height: 18px;
  margin-left: 0;
}

.commentActions .action.active {
  color: #409eff;
  font-weight: 600;
}

.report-preview {
  width: 100%;
  max-height: 110px;
  overflow-y: auto;
  line-height: 1.6;
  color: #606266;
  background-color: #f7f8f9;
  border-radius: 6px;
  padding: 8px 10px;
  box-sizing: border-box;
}

:global(.comment-sort-popover) {
  padding: 10px 0 !important;
  border-radius: 10px !important;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14) !important;
}

:global(.comment-sort-menu) {
  display: flex;
  flex-direction: column;
}

:global(.comment-sort-option) {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: #fff;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  text-align: left;
  padding: 12px 22px;
  cursor: pointer;
}

:global(.comment-sort-option:hover) {
  background: #f5f7fa;
}

:global(.comment-sort-option .sort-check) {
  color: #f43f5e;
  font-size: 18px;
}
</style>
