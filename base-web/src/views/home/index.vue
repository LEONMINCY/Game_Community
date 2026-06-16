<!-- 文件说明：views/home/index.vue，前台首页布局，组合左侧导航、全局搜索、主内容和右侧排行榜。 -->
<template>
  <div class="home-main">
    <header class="header">
      <div class="header-content">
        <div class="brand" @click="goPage('/forumlist')">
          <img src="../../assets/imgs/logo.png" alt="" class="brand-logo">
          <span>游戏社区</span>
        </div>

        <div class="global-search-shell" ref="globalSearchRef">
          <el-input
            v-model="globalSearchKeyword"
            class="global-search-input"
            size="large"
            clearable
            placeholder="搜索内容、游戏、用户或话题"
            @focus="openGlobalSearch"
            @input="handleGlobalSearchInput"
            @keyup.enter="submitGlobalSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <div v-if="globalSearchOpen" class="global-search-panel" @mousedown.stop>
            <div class="search-panel-head">
              <strong>搜索历史</strong>
              <button type="button" :disabled="searchHistory.length === 0" @click="clearSearchHistory">
                <el-icon><Delete /></el-icon>
                清空
              </button>
            </div>

            <div class="search-history">
              <button
                v-for="keyword in searchHistory"
                :key="keyword"
                type="button"
                @click="useSearchKeyword(keyword)"
              >
                {{ keyword }}
              </button>
              <span v-if="searchHistory.length === 0" class="search-history-empty">暂无搜索历史</span>
            </div>

            <div class="search-tabs discover-tabs">
              <button
                v-for="tab in discoverTabs"
                :key="tab.key"
                type="button"
                :class="{ active: activeDiscoverTab === tab.key }"
                @click="activeDiscoverTab = tab.key"
              >
                {{ tab.label }}
              </button>
            </div>

            <div class="search-results">
              <template v-if="activeDiscoverTab === 'communities'">
                <div
                  v-for="(game, index) in searchHotCommunities"
                  :key="game.id"
                  class="search-result-row"
                  @click="goPage(`/forumlist?gameId=${game.id}`)"
                >
                  <span class="search-rank">{{ index + 1 }}</span>
                  <img v-if="game.cover" class="search-thumb" :src="thumbnailURL(game.cover, 120)" alt="" loading="lazy" decoding="async" @error="handleImageError">
                  <span class="search-title">{{ game.name }}</span>
                  <small>{{ game.postCount || 0 }}帖</small>
                </div>
              </template>

              <template v-else-if="activeDiscoverTab === 'games'">
                <div
                  v-for="(game, index) in hotGames"
                  :key="game.id"
                  class="search-result-row"
                  @click="goGame(game.id)"
                >
                  <span class="search-rank">{{ index + 1 }}</span>
                  <img v-if="game.cover" class="search-thumb" :src="thumbnailURL(game.cover, 120)" alt="" loading="lazy" decoding="async" @error="handleImageError">
                  <span class="search-title">{{ game.name }}</span>
                  <small>销量{{ game.salesCount || 0 }} · 好评率{{ game.goodRate || 0 }}%</small>
                </div>
              </template>

              <template v-else>
                <div
                  v-for="(topic, index) in hotTopics"
                  :key="topic.topic"
                  class="search-result-row"
                  @click="goTopic(topic.topic)"
                >
                  <span class="search-rank">{{ index + 1 }}</span>
                  <span class="search-topic-icon">#</span>
                  <span class="search-title">{{ topic.topic }}</span>
                  <small>{{ topic.count || 0 }}帖</small>
                </div>
              </template>

              <el-empty v-if="currentDiscoverEmpty" description="暂无热榜内容" :image-size="60" />
            </div>
          </div>
        </div>

        <div class="header-actions">
          <el-badge
            v-if="isLoggedIn"
            :value="totalNotifyCount"
            :hidden="totalNotifyCount === 0"
            class="notify-badge"
          >
            <el-button text circle class="notify-button" title="通知" @click="openNotificationDialog">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>

          <el-dialog
            v-model="notificationDialogVisible"
            width="760px"
            append-to-body
            :show-close="false"
            class="notification-center-dialog"
          >
            <div class="notification-center">
              <aside class="notification-tabs">
                <button
                  v-for="tab in notificationTabs"
                  :key="tab.key"
                  type="button"
                  :class="{ active: activeNotificationTab === tab.key }"
                  @click="activeNotificationTab = tab.key"
                >
                  <span class="tab-icon">{{ tab.icon }}</span>
                  <span>{{ tab.label }}</span>
                  <i v-if="notificationTabCount(tab.key)">{{ notificationTabCount(tab.key) }}</i>
                </button>
              </aside>
              <section class="notification-body">
                <div class="notification-head">
                  <h3>{{ currentNotificationTab.label }}</h3>
                  <button type="button" @click="notificationDialogVisible = false">×</button>
                </div>
                <div v-if="activeNotificationTab === 'comments'" class="comment-notification-tabs">
                  <button
                    type="button"
                    :class="{ active: activeCommentNotificationTab === 'toMe' }"
                    @click="activeCommentNotificationTab = 'toMe'"
                  >
                    评论我的
                  </button>
                  <button
                    type="button"
                    :class="{ active: activeCommentNotificationTab === 'mine' }"
                    @click="activeCommentNotificationTab = 'mine'"
                  >
                    我的评论
                  </button>
                </div>
                <el-empty
                  v-if="currentNotificationItems.length === 0"
                  :description="`暂无${currentNotificationTab.label}`"
                  :image-size="86"
                />
                <div v-else class="notification-list">
                  <div
                    v-for="item in currentNotificationItems"
                    :key="`${activeNotificationTab}-${item.id}`"
                    class="notification-card"
                    :class="{ unread: isUnreadNotification(item) }"
                    @click="openNotificationItem(item)"
                  >
                    <template v-if="activeNotificationTab === 'reports'">
                      <template v-if="item.notificationSource === 'punish'">
                        <div class="notification-card-top">
                          <strong>{{ item.title || '举报处理通知' }}</strong>
                          <el-tag size="small" :type="item.readFlag ? 'info' : 'success'">
                            {{ item.readFlag ? '已读' : '未读' }}
                          </el-tag>
                        </div>
                        <p>{{ item.content || '点击查看申诉入口' }}</p>
                        <small>{{ item.createTime || '-' }}</small>
                      </template>
                      <template v-else>
                        <div class="notification-card-top">
                          <strong>{{ reportCardTitle(item) }}</strong>
                          <el-tag size="small" :type="reportStatusType(item.status)">
                            {{ reportStatusText(item.status) }}
                          </el-tag>
                        </div>
                        <p>举报原因：{{ item.reason || '-' }}</p>
                        <small>{{ item.createTime || '-' }}</small>
                      </template>
                    </template>
                    <template v-else-if="activeNotificationTab === 'follows'">
                      <div class="notification-follow-row">
                        <button type="button" class="follow-user-info" @click.stop="openNotificationUser(item)">
                          <el-avatar :size="54" :src="notificationActorAvatar(item)" />
                          <span>
                            <strong>{{ notificationActorName(item) }}</strong>
                            <em v-if="notificationActorLevel(item) !== null">Lv{{ notificationActorLevel(item) }}</em>
                            <small>关注了你</small>
                          </span>
                        </button>
                        <button
                          type="button"
                          class="follow-action"
                          :class="{ followed: item.actorFollowed }"
                          @click.stop="followBack(item)"
                        >
                          {{ item.actorFollowed ? '互相关注' : '回关' }}
                        </button>
                      </div>
                    </template>
                    <template v-else-if="activeNotificationTab === 'comments'">
                      <div v-if="item.notificationSource !== 'my-comment' && item.actorId" class="notification-actor">
                        <el-avatar :size="34" :src="notificationActorAvatar(item)" />
                        <div class="notification-actor-meta">
                          <strong>{{ notificationActorName(item) }}</strong>
                          <span v-if="notificationActorLevel(item) !== null">Lv{{ notificationActorLevel(item) }}</span>
                        </div>
                      </div>
                      <div class="notification-card-top">
                        <strong>{{ item.notificationSource === 'my-comment' ? (item.postTitle || '我的评论') : (item.title || '评论提醒') }}</strong>
                        <el-tag v-if="item.notificationSource !== 'my-comment'" size="small" :type="item.readFlag ? 'info' : 'success'">
                          {{ item.readFlag ? '已读' : '未读' }}
                        </el-tag>
                      </div>
                      <p>{{ item.content || '点击查看评论详情' }}</p>
                      <div v-if="item.notificationSource === 'my-comment' && (item.postTitle || notificationCommentCover(item))" class="notification-comment-preview">
                        <img v-if="notificationCommentCover(item)" :src="notificationCommentCover(item)" alt="" loading="lazy" decoding="async" @error="handleImageError" />
                        <span>{{ item.postTitle || '相关帖子' }}</span>
                      </div>
                      <small>{{ item.createTime || '-' }}</small>
                    </template>
                    <template v-else>
                      <div v-if="item.actorId" class="notification-actor">
                        <el-avatar :size="34" :src="notificationActorAvatar(item)" />
                        <div class="notification-actor-meta">
                          <strong>{{ notificationActorName(item) }}</strong>
                          <span v-if="notificationActorLevel(item) !== null">Lv{{ notificationActorLevel(item) }}</span>
                        </div>
                      </div>
                      <div class="notification-card-top">
                        <strong>{{ item.title || currentNotificationTab.label }}</strong>
                        <el-tag size="small" :type="item.readFlag ? 'info' : 'success'">
                          {{ item.readFlag ? '已读' : '未读' }}
                        </el-tag>
                      </div>
                      <p>{{ item.content || '点击查看详情' }}</p>
                      <small>{{ item.createTime || '-' }}</small>
                    </template>
                  </div>
                </div>
              </section>
            </div>
          </el-dialog>

          <el-dialog v-model="reportDetailVisible" title="举报进度详情" width="520px" append-to-body>
            <div v-if="selectedReport" class="report-detail">
              <div><span>处理状态</span><strong>{{ reportStatusText(selectedReport.status) }}</strong></div>
              <div><span>举报对象</span><strong>{{ reportCardTitle(selectedReport) }}</strong></div>
              <div><span>举报原因</span><p>{{ selectedReport.reason || '-' }}</p></div>
              <div><span>处理回复</span><p>{{ selectedReport.reply || defaultReportReply(selectedReport.status) }}</p></div>
              <div v-if="selectedReport.appealStatus"><span>申诉状态</span><strong>{{ selectedReport.appealStatus }}</strong></div>
              <div v-if="selectedReport.appealReply"><span>申诉回复</span><p>{{ selectedReport.appealReply }}</p></div>
            </div>
          </el-dialog>

          <el-dialog v-model="appealDialogVisible" title="举报处罚申诉" width="460px" append-to-body>
            <el-input
              v-model="appealForm.content"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="请说明你的申诉理由，审核员会在后台同步处理进度"
            />
            <template #footer>
              <el-button @click="appealDialogVisible = false">取消</el-button>
              <el-button type="primary" @click="submitReportAppeal">提交申诉</el-button>
            </template>
          </el-dialog>

          <el-button v-if="!isLoggedIn" text circle class="notify-button" title="登录后查看通知" @click="goLogin">
            <el-icon><Bell /></el-icon>
          </el-button>

          <el-dropdown v-if="isLoggedIn">
            <el-avatar style="cursor: pointer" :size="30" :src="userInfo.avatar || imageFallbackURL()" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item><div @click="goPage('/userinfo')">个人中心</div></el-dropdown-item>
                <el-dropdown-item><div @click="goPage('/my-posts')">我的帖子</div></el-dropdown-item>
                <el-dropdown-item><div @click="goPage('/wishlist')">愿望单</div></el-dropdown-item>
                <el-dropdown-item><div @click="goPage('/cart')">购物车</div></el-dropdown-item>
                <el-dropdown-item><div @click="goPage('/my-orders')">我的订单</div></el-dropdown-item>
                <el-dropdown-item><div @click="goPage('/privacy-settings')">隐私设置</div></el-dropdown-item>
                <el-dropdown-item><div @click="signOut">退出登录</div></el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-else type="primary" size="small" @click="goLogin">登录</el-button>
          <span class="header-username">{{ userInfo.nickname || userInfo.username }}</span>
        </div>
      </div>
    </header>

    <main class="content-main">
      <div class="content">
        <aside class="left">
          <div class="letf-content">
            <div class="left-content-item" @click="goPage('/forumlist')">
              <el-icon size="18"><Platform /></el-icon>
              <div class="left-content-item-text">社区中心</div>
            </div>
            <div class="left-content-item" @click="goPage('/games')">
              <el-icon size="18"><SwitchFilled /></el-icon>
              <div class="left-content-item-text">游戏中心</div>
            </div>
            <div class="left-content-item" @click="goPage('/rankings')">
              <el-icon size="18"><Histogram /></el-icon>
              <div class="left-content-item-text">榜单</div>
            </div>
            <div class="left-content-item" @click="goPage('/news')">
              <el-icon size="18"><TrendCharts /></el-icon>
              <div class="left-content-item-text">新闻中心</div>
            </div>
            <div class="left-content-item" @click="goPage('/ai-assistant')">
              <el-icon size="18"><ChatDotRound /></el-icon>
              <div class="left-content-item-text">AI助手</div>
            </div>
            <div class="left-content-item" @click="goPage('/chat')">
              <el-icon size="18"><Message /></el-icon>
              <div class="left-content-item-text with-badge">
                消息中心
                <span v-if="unreadMessageCount > 0" class="nav-unread-badge">
                  {{ formatUnreadCount(unreadMessageCount) }}
                </span>
              </div>
            </div>
            <div class="left-content-item" @click="goPage('/userinfo')">
              <el-icon size="18"><Avatar /></el-icon>
              <div class="left-content-item-text">个人中心</div>
            </div>
          </div>
          <el-button color="#24292e" size="large" class="publish-button" @click="goPage('/addForum')">
            + 发布内容
          </el-button>
        </aside>

        <section class="center">
          <router-view v-slot="{ Component, route }">
            <KeepAlive :max="8">
              <component :is="Component" :key="['/forumlist', '/search'].includes(route.path) ? route.path : route.fullPath" />
            </KeepAlive>
          </router-view>
        </section>

        <aside class="right">
          <div class="right-content">
            <section class="hot-communities">
              <h3>热门社区TOP5</h3>
              <div class="game-list">
                <div
                  v-for="(game, index) in hotCommunities"
                  :key="game.id"
                  class="game-item"
                  @click="goPage(`/forumlist?gameId=${game.id}`)"
                >
                  <div class="rank">TOP{{ index + 1 }}</div>
                  <img :src="thumbnailURL(game.cover, 180)" :alt="game.name" loading="lazy" decoding="async" @error="handleImageError">
                  <div class="game-meta">
                    <span>{{ game.name }}</span>
                    <small>{{ game.postCount || 0 }}帖</small>
                  </div>
                </div>
              </div>
            </section>

            <section class="hot-games">
              <h3>热门游戏TOP10</h3>
              <div class="game-list">
                <div
                  v-for="(game, index) in hotGames"
                  :key="game.id"
                  class="game-item"
                  @click="goPage(`/gameDetail?id=${game.id}`)"
                >
                  <div class="rank">TOP{{ index + 1 }}</div>
                  <img :src="thumbnailURL(game.cover, 180)" :alt="game.name" loading="lazy" decoding="async" @error="handleImageError">
                  <div class="game-meta">
                    <span>{{ game.name }}</span>
                    <small>销量{{ game.salesCount || 0 }} · 好评率{{ game.goodRate || 0 }}%</small>
                  </div>
                </div>
              </div>
            </section>

            <section class="friend-links">
              <h3>友情链接</h3>
              <div class="link-list">
                <a v-for="link in friendLinks" :key="link.id" :href="link.url" target="_blank">{{ link.name }}</a>
              </div>
            </section>
          </div>
        </aside>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from "vue-router";
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { getRequest, postRequest, putRequest } from "../../utils/http";
import { useUserStore } from '@/store'
import { handleImageError, imageFallbackURL, replaceURL, thumbnailURL } from '@/utils/tools'
import { hasToken, isProtectedPath, requireLogin } from '@/utils/auth'
import { Bell, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter();
const route = useRoute();
const userStore = useUserStore()

const userInfo = ref({
  username: '游客',
  nickname: '',
  avatar: '',
})
const isLoggedIn = ref(hasToken())

const hotGames = ref([])
const hotCommunities = ref([])
const searchHotCommunities = ref([])
const hotTopics = ref([])
const globalSearchKeyword = ref('')
const globalSearchOpen = ref(false)
const globalSearchRef = ref(null)
const activeDiscoverTab = ref('communities')
const searchHistory = ref([])
const reportProgressList = ref([])
const mentionNotifications = ref([])
const myCommentList = ref([])
const unreadMessageCount = ref(0)
const notificationDialogVisible = ref(false)
const activeNotificationTab = ref('likes')
const activeCommentNotificationTab = ref('toMe')
const reportDetailVisible = ref(false)
const selectedReport = ref(null)
const appealDialogVisible = ref(false)
const appealForm = ref({
  id: null,
  content: ''
})
let notificationWs = null
let unreadRefreshTimer = null

const discoverTabs = [
  { key: 'communities', label: '社区热榜' },
  { key: 'games', label: '热门游戏' },
  { key: 'topics', label: '热门话题' }
]

const notificationTabs = [
  { key: 'likes', label: '点赞', icon: '赞' },
  { key: 'replies', label: '回复', icon: '回' },
  { key: 'mentions', label: '@我的', icon: '@' },
  { key: 'follows', label: '关注', icon: '关' },
  { key: 'comments', label: '所有评论', icon: '评' },
  { key: 'reports', label: '举报进度', icon: '报' }
]

const handleMessageUnreadChange = (event) => {
  const delta = Number(event?.detail?.delta)
  if (!Number.isNaN(delta) && delta !== 0) {
    unreadMessageCount.value = Math.max(0, unreadMessageCount.value + delta)
  }
  if (isLoggedIn.value) {
    loadUnreadMessageCount()
  }
}

const pendingReportCount = computed(() => {
  return reportProgressList.value.filter(item => !item.status || item.status === 'PENDING').length
})

const mentionUnreadCount = computed(() => {
  return mentionNotifications.value.filter(item => !item.readFlag).length
})

const totalNotifyCount = computed(() => pendingReportCount.value + mentionUnreadCount.value)

const likeNotifications = computed(() => {
  return mentionNotifications.value.filter(item => String(item.type || '').includes('LIKE'))
})

const replyNotifications = computed(() => {
  return mentionNotifications.value.filter(item => String(item.type || '').includes('REPLY'))
})

const atNotifications = computed(() => {
  return mentionNotifications.value.filter(item => String(item.type || '') === 'MENTION')
})

const followNotifications = computed(() => {
  return mentionNotifications.value.filter(item => String(item.type || '') === 'FOLLOW')
})

const commentToMeNotifications = computed(() => {
  return mentionNotifications.value.filter(item => {
    const type = String(item.type || '')
    return type === 'POST_COMMENT' || type === 'COMMENT_REPLY'
  })
})

const myCommentItems = computed(() => {
  return myCommentList.value.map(item => ({
    ...item,
    id: `my-comment-${item.id}`,
    notificationSource: 'my-comment',
    title: item.postTitle || '我的评论',
    content: item.content,
    createTime: item.createTime,
    targetUrl: `/forum?id=${item.postId}&commentId=${item.id}`
  }))
})

const commentNotificationItems = computed(() => {
  return activeCommentNotificationTab.value === 'mine'
    ? myCommentItems.value
    : commentToMeNotifications.value
})

const reportPunishNotifications = computed(() => {
  return mentionNotifications.value
    .filter(item => String(item.type || '').startsWith('REPORT'))
    .map(item => ({ ...item, notificationSource: 'punish' }))
})

const reportNotificationItems = computed(() => {
  const progressItems = reportProgressList.value.map(item => ({ ...item, notificationSource: 'progress' }))
  return [...reportPunishNotifications.value, ...progressItems]
})

const currentNotificationTab = computed(() => {
  return notificationTabs.find(item => item.key === activeNotificationTab.value) || notificationTabs[0]
})

const currentNotificationItems = computed(() => {
  if (activeNotificationTab.value === 'likes') return likeNotifications.value
  if (activeNotificationTab.value === 'replies') return replyNotifications.value
  if (activeNotificationTab.value === 'mentions') return atNotifications.value
  if (activeNotificationTab.value === 'follows') return followNotifications.value
  if (activeNotificationTab.value === 'comments') return commentNotificationItems.value
  return reportNotificationItems.value
})

const currentDiscoverEmpty = computed(() => {
  if (activeDiscoverTab.value === 'communities') return searchHotCommunities.value.length === 0
  if (activeDiscoverTab.value === 'games') return hotGames.value.length === 0
  return hotTopics.value.length === 0
})

const friendLinks = ref([
  { id: 1, name: '米哈游', url: 'https://www.mihoyo.com/' },
  { id: 2, name: '腾讯游戏', url: 'https://game.qq.com/' },
  { id: 3, name: '网易游戏', url: 'https://game.163.com/' },
  { id: 4, name: 'Steam', url: 'https://store.steampowered.com/' },
  { id: 5, name: 'Epic Games', url: 'https://www.epicgames.com/' },
  { id: 6, name: 'PlayStation', url: 'https://www.playstation.com/' },
  { id: 7, name: 'Xbox', url: 'https://www.xbox.com/' },
  { id: 8, name: 'Nintendo', url: 'https://www.nintendo.com/' },
  { id: 9, name: 'Ubisoft', url: 'https://www.ubisoft.com/' }
])

onMounted(() => {
  if (isLoggedIn.value) {
    getUserInfo()
    loadAllNotifications()
    loadUnreadMessageCount()
    initNotificationSocket()
    startUnreadRefreshTimer()
  }
  window.addEventListener('message-unread-change', handleMessageUnreadChange)
  syncSearchKeywordFromRoute()
  loadSearchHistory()
  getHotGames()
  getHotCommunities()
  getSearchHotCommunities()
  getHotTopics()
  document.addEventListener('click', handleGlobalSearchOutsideClick)
})

onUnmounted(() => {
  closeNotificationSocket()
  stopUnreadRefreshTimer()
  window.removeEventListener('message-unread-change', handleMessageUnreadChange)
  document.removeEventListener('click', handleGlobalSearchOutsideClick)
})

watch(
  () => route.fullPath,
  () => {
    syncSearchKeywordFromRoute()
    if (isLoggedIn.value) {
      loadUnreadMessageCount()
    }
  }
)

const syncSearchKeywordFromRoute = () => {
  if (route.path === '/search') {
    globalSearchKeyword.value = String(route.query.keyword || '')
  }
}

const getHotGames = () => {
  getRequest('/game/listHot', { silentError: true }).then(res => {
    if (res.code === 200) {
      hotGames.value = (res.data || []).slice(0, 10).map(game => ({
        id: game.id,
        name: game.name,
        cover: replaceURL(game.icon),
        salesCount: game.salesCount,
        goodRate: game.goodRate
      }))
    }
  }).catch(() => {
    hotGames.value = []
  })
}

const getHotCommunities = () => {
  getRequest('/game/hotCommunities?limit=5', { silentError: true }).then(res => {
    if (res.code === 200) {
      hotCommunities.value = (res.data || []).map(game => ({
        id: game.id,
        name: game.name,
        cover: replaceURL(game.icon),
        postCount: game.postCount
      }))
    }
  }).catch(() => {
    hotCommunities.value = []
  })
}

const getSearchHotCommunities = () => {
  getRequest('/game/hotCommunities?limit=10', { silentError: true }).then(res => {
    if (res.code === 200) {
      searchHotCommunities.value = (res.data || []).map(game => ({
        id: game.id,
        name: game.name,
        cover: replaceURL(game.icon),
        postCount: game.postCount
      }))
    }
  }).catch(() => {
    searchHotCommunities.value = []
  })
}

const getHotTopics = () => {
  getRequest('/post/hotTopics?limit=10', { silentError: true }).then(res => {
    if (res.code === 200) {
      hotTopics.value = res.data || []
    }
  }).catch(() => {
    hotTopics.value = []
  })
}

const openGlobalSearch = () => {
  globalSearchOpen.value = true
}

const handleGlobalSearchInput = () => {
  globalSearchOpen.value = true
}

const submitGlobalSearch = () => {
  const keyword = globalSearchKeyword.value.trim()
  if (!keyword) {
    globalSearchOpen.value = true
    return
  }
  saveSearchHistory(keyword)
  globalSearchOpen.value = false
  router.push({ path: '/search', query: { keyword, tab: 'posts' } })
}

const useSearchKeyword = (keyword) => {
  globalSearchKeyword.value = keyword
  submitGlobalSearch()
}

const searchHistoryKey = () => {
  return `game-community-search-history:${sessionStorage.getItem('userId') || 'guest'}`
}

const loadSearchHistory = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(searchHistoryKey()) || '[]')
    searchHistory.value = Array.isArray(stored) ? stored.filter(Boolean).slice(0, 10) : []
  } catch (error) {
    searchHistory.value = []
  }
}

const saveSearchHistory = (keyword) => {
  const value = keyword.trim()
  if (!value) return
  const next = [value, ...searchHistory.value.filter(item => item !== value)].slice(0, 10)
  searchHistory.value = next
  localStorage.setItem(searchHistoryKey(), JSON.stringify(next))
}

const clearSearchHistory = () => {
  searchHistory.value = []
  localStorage.removeItem(searchHistoryKey())
}

const goGame = (id) => {
  globalSearchOpen.value = false
  router.push('/gameDetail?id=' + id)
}

const goTopic = (topic) => {
  globalSearchOpen.value = false
  router.push({ path: '/forumlist', query: { topic } })
}

const handleGlobalSearchOutsideClick = (event) => {
  if (globalSearchRef.value && !globalSearchRef.value.contains(event.target)) {
    globalSearchOpen.value = false
  }
}

const loadReportProgress = () => {
  if (!hasToken()) {
    reportProgressList.value = []
    return
  }
  postRequest('/report/my/page', {
    pageNo: 1,
    pageSize: 20
  }, {
    silentError: true
  }).then(res => {
    if (res.code === 200) {
      reportProgressList.value = res.data.list || []
    }
  }).catch(() => {
    reportProgressList.value = []
  })
}

const loadMentionNotifications = () => {
  if (!hasToken()) {
    mentionNotifications.value = []
    return
  }
  getRequest('/notification/my?limit=100', { silentError: true }).then(res => {
    if (res.code === 200) {
      mentionNotifications.value = res.data || []
    }
  }).catch(() => {
    mentionNotifications.value = []
  })
}

const loadMyCommentList = () => {
  if (!hasToken()) {
    myCommentList.value = []
    return
  }
  getRequest('/comment/my/list?pageNo=1&pageSize=30', { silentError: true }).then(res => {
    if (res.code === 200) {
      myCommentList.value = res.data?.list || []
    }
  }).catch(() => {
    myCommentList.value = []
  })
}

const loadAllNotifications = () => {
  loadMentionNotifications()
  loadReportProgress()
  loadMyCommentList()
}

const openNotificationDialog = () => {
  activeNotificationTab.value = 'likes'
  notificationDialogVisible.value = true
  loadAllNotifications()
}

const notificationTabCount = (key) => {
  if (key === 'likes') return likeNotifications.value.filter(item => !item.readFlag).length
  if (key === 'replies') return replyNotifications.value.filter(item => !item.readFlag).length
  if (key === 'mentions') return atNotifications.value.filter(item => !item.readFlag).length
  if (key === 'follows') return followNotifications.value.filter(item => !item.readFlag).length
  if (key === 'comments') return commentToMeNotifications.value.filter(item => !item.readFlag).length
  return pendingReportCount.value + reportPunishNotifications.value.filter(item => !item.readFlag).length
}

const loadUnreadMessageCount = () => {
  if (!hasToken()) {
    unreadMessageCount.value = 0
    return
  }
  getRequest('/message/unread-count', { silentError: true }).then(res => {
    if (res.code === 200) {
      unreadMessageCount.value = Number(res.data) || 0
    }
  }).catch(() => {
    unreadMessageCount.value = 0
  })
}

const startUnreadRefreshTimer = () => {
  stopUnreadRefreshTimer()
  unreadRefreshTimer = window.setInterval(() => {
    if (isLoggedIn.value) {
      loadUnreadMessageCount()
    }
  }, 30000)
}

const stopUnreadRefreshTimer = () => {
  if (unreadRefreshTimer) {
    window.clearInterval(unreadRefreshTimer)
    unreadRefreshTimer = null
  }
}

const initNotificationSocket = () => {
  const userId = userStore.userInfo?.userId || sessionStorage.getItem('userId')
  if (!userId || notificationWs) {
    return
  }
  notificationWs = new WebSocket(buildWebSocketUrl(userId))
  notificationWs.onmessage = event => {
    let payload = null
    try {
      payload = JSON.parse(event.data)
    } catch (error) {
      return
    }
    if (payload?.type === 'notification' && payload.data) {
      upsertNotification(payload.data)
      return
    }
    if (payload?.senderId && String(payload.receiverId) === String(userId)) {
      window.setTimeout(loadUnreadMessageCount, route.path === '/chat' ? 350 : 0)
    }
  }
  notificationWs.onclose = () => {
    notificationWs = null
  }
}

const closeNotificationSocket = () => {
  if (notificationWs) {
    notificationWs.close()
    notificationWs = null
  }
}

const buildWebSocketUrl = (userId) => {
  if (import.meta.env.VITE_WS_URL) {
    return `${import.meta.env.VITE_WS_URL}?userId=${userId}`
  }
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const host = window.location.hostname || 'localhost'
  const port = window.location.port && window.location.port !== '9999' ? ':9999' : (window.location.port ? `:${window.location.port}` : '')
  return `${protocol}://${host}${port}/ws?userId=${userId}`
}

const upsertNotification = (notice) => {
  const exists = mentionNotifications.value.some(item => String(item.id) === String(notice.id))
  if (!exists) {
    mentionNotifications.value = [notice, ...mentionNotifications.value].slice(0, 30)
  }
  if (String(notice.type || '').startsWith('REPORT')) {
    loadReportProgress()
  }
}

const isUnreadNotification = (item) => {
  if (item?.notificationSource === 'punish') {
    return !item.readFlag
  }
  return activeNotificationTab.value === 'reports'
    ? (!item.status || item.status === 'PENDING')
    : !item.readFlag
}

const openNotificationItem = (item) => {
  if (activeNotificationTab.value === 'reports') {
    if (item?.notificationSource === 'punish') {
      openMentionNotification(item)
      return
    }
    selectedReport.value = item
    reportDetailVisible.value = true
    return
  }
  if (item?.notificationSource === 'my-comment') {
    openNotificationTarget(item.targetUrl)
    return
  }
  openMentionNotification(item)
}

const openNotificationTarget = (targetUrl) => {
  if (!targetUrl) {
    return
  }
  globalSearchOpen.value = false
  notificationDialogVisible.value = false
  router.push(targetUrl)
}

const openNotificationUser = (item) => {
  if (!item?.actorId) {
    return
  }
  openNotificationTarget(`/userinfo?userId=${item.actorId}`)
}

const followBack = (item) => {
  if (!item?.actorId || item.actorFollowed) {
    return
  }
  postRequest(`/userRelation/follow/${item.actorId}`, {}, { silentError: true }).then(res => {
    if (res.code === 200) {
      item.actorFollowed = true
      ElMessage.success('已回关')
      return
    }
    ElMessage.warning(res.msg || '回关失败')
  }).catch(error => {
    const message = error?.msg || error?.message || ''
    if (/已关注|已经关注/.test(message)) {
      item.actorFollowed = true
      return
    }
    ElMessage.warning('回关失败，请稍后再试')
  })
}

const formatUnreadCount = (count) => {
  const value = Number(count) || 0
  return value > 99 ? '99+' : String(value)
}

const notificationActorName = (item) => {
  return item?.actorNickname || item?.actorName || '未知用户'
}

const notificationActorAvatar = (item) => {
  return replaceURL(item?.actorAvatar || '')
}

const notificationActorLevel = (item) => {
  const level = Number(item?.actorLevel)
  return Number.isFinite(level) ? level : null
}

const notificationCommentCover = (item) => {
  const media = item?.postCover || item?.media || ''
  if (!media) {
    return ''
  }
  try {
    const list = JSON.parse(media)
    if (Array.isArray(list) && list.length > 0) {
      return thumbnailURL(list[0], 120)
    }
  } catch (error) {
    return thumbnailURL(media, 120)
  }
  return ''
}

const openMentionNotification = (notice) => {
  if (!notice) {
    return
  }
  if (!notice.readFlag) {
    postRequest(`/notification/read/${notice.id}`, {}, { silentError: true }).then(() => {
      notice.readFlag = true
    })
  }
  if (notice.targetType === 'REPORT' && notice.targetId && notice.type === 'REPORT_PUNISH' && !String(notice.title || '').includes('申诉进度')) {
    appealForm.value = {
      id: notice.targetId,
      content: ''
    }
    appealDialogVisible.value = true
    return
  }
  if (notice.targetUrl) {
    openNotificationTarget(notice.targetUrl)
  }
}

const reportCardTitle = (report) => {
  if (!report) return '举报记录'
  if (report.reportType === 'USER') return `用户举报：${report.reportedNickname || '未知用户'}`
  return report.commentContent || report.postTitle || report.newsTitle || '评论举报'
}

const submitReportAppeal = () => {
  if (!appealForm.value.id) {
    return
  }
  if (!appealForm.value.content.trim()) {
    ElMessage.warning('请填写申诉理由')
    return
  }
  putRequest('/report/appeal', {
    id: appealForm.value.id,
    appealContent: appealForm.value.content.trim()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('申诉已提交，请等待审核')
      appealDialogVisible.value = false
      loadAllNotifications()
    } else {
      ElMessage.error(res.msg || '申诉提交失败')
    }
  })
}

const reportStatusText = (status) => {
  if (!status || status === 'PENDING') return '待处理'
  if (String(status).startsWith('MUTE:')) return '已禁言'
  if (String(status).startsWith('BAN:')) return '已封禁'
  const map = {
    BANNED: '已封禁',
    UNBANNED: '已解封',
    REJECTED: '已拒绝'
  }
  return map[status] || '已处理'
}

const reportStatusType = (status) => {
  if (!status || status === 'PENDING') return 'warning'
  if (String(status).startsWith('MUTE:')) return 'success'
  if (String(status).startsWith('BAN:') || status === 'BANNED') return 'danger'
  if (status === 'REJECTED') return 'info'
  return 'info'
}

const defaultReportReply = (status) => {
  return (!status || status === 'PENDING') ? '审核员还没有处理，请稍后查看' : '审核员已处理该举报'
}

const goPage = (path = '/forumlist') => {
  globalSearchOpen.value = false
  if (path.includes('?')) {
    const [basePath, queryString] = path.split('?')
    const query = {}
    queryString.split('&').forEach(param => {
      const [key, value] = param.split('=')
      query[key] = value
    })
    if (isProtectedPath(basePath, query) && !requireLogin(path)) {
      return
    }
    router.push({ path: basePath, query })
    return
  }
  if (isProtectedPath(path) && !requireLogin(path)) {
    return
  }
  router.push({ path })
}

const signOut = () => {
  localStorage.removeItem('token')
  sessionStorage.removeItem('userId')
  userStore.setUserInfo({})
  userInfo.value.username = '游客'
  userInfo.value.nickname = ''
  userInfo.value.avatar = ''
  isLoggedIn.value = false
  reportProgressList.value = []
  mentionNotifications.value = []
  myCommentList.value = []
  unreadMessageCount.value = 0
  stopUnreadRefreshTimer()
  loadSearchHistory()
  router.push('/forumlist')
}

const goLogin = () => {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

const getUserInfo = () => {
  if (!hasToken()) {
    userInfo.value.username = '游客'
    userInfo.value.nickname = ''
    userInfo.value.avatar = ''
    return
  }
  getRequest('/user-info/get-real').then(res => {
    if (res.code === 200) {
      userStore.setUserInfo(res.data)
      sessionStorage.setItem('userId', res.data.userId)
      userInfo.value.username = res.data.username
      userInfo.value.nickname = res.data.nickname || ''
      userInfo.value.avatar = replaceURL(res.data.avatar)
      initNotificationSocket()
      loadUnreadMessageCount()
      startUnreadRefreshTimer()
      loadSearchHistory()
    }
  })
}
</script>

<style lang="less" scoped>
.home-main {
  width: 100%;
  min-height: 100%;
  background-color: #f5f6f8;
}

.header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: 70px;
  background-color: #fff;
  border-bottom: 1px solid #ebeef5;
}

.header-content {
  width: min(96vw, 1780px);
  height: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 210px minmax(360px, 860px) minmax(220px, auto);
  align-items: center;
  gap: 24px;
  font-weight: 600;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  color: #09213a;
  font-size: 18px;

  .brand-logo {
    width: 50px;
    height: 50px;
    object-fit: contain;
  }
}

.global-search-shell {
  position: relative;
  width: 100%;
}

.global-search-input {
  width: 100%;
}

.global-search-panel {
  position: absolute;
  top: calc(100% + 12px);
  left: 0;
  right: 0;
  z-index: 30;
  max-height: 72vh;
  overflow-y: auto;
  padding: 22px 26px 24px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.16);
}

.search-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  button {
    border: 0;
    background: transparent;
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    cursor: pointer;

    &:disabled {
      color: #c0c4cc;
      cursor: default;
    }
  }
}

.search-history {
  min-height: 72px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 40px;
  margin-bottom: 18px;

  button {
    border: 0;
    background: transparent;
    text-align: left;
    font-size: 15px;
    cursor: pointer;
    color: #111827;

    &:hover {
      color: #409eff;
    }
  }
}

.search-history-empty {
  color: #9aa1ad;
  font-weight: 400;
}

.search-tabs {
  display: flex;
  gap: 26px;
  margin-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;

  button {
    position: relative;
    border: 0;
    background: transparent;
    padding: 0 0 10px;
    color: #606266;
    font-size: 15px;
    cursor: pointer;

    &::after {
      content: '';
      position: absolute;
      left: 2px;
      right: 2px;
      bottom: -1px;
      height: 2px;
      border-radius: 999px;
      background: transparent;
    }

    &.active {
      color: #14191e;
      font-weight: 700;
    }

    &.active::after {
      background: #14191e;
    }
  }
}

.discover-tabs {
  margin-top: 6px;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.search-result-row {
  min-height: 38px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border-radius: 6px;
  padding: 4px 6px;

  &:hover {
    background: #f6f8fb;
  }

  small {
    color: #8a92a3;
    font-weight: 400;
    white-space: nowrap;
  }
}

.search-rank {
  width: 22px;
  color: #8a92a3;
  font-weight: 700;
}

.search-result-row:nth-child(1) .search-rank { color: #ff7a00; }
.search-result-row:nth-child(2) .search-rank { color: #d946ef; }
.search-result-row:nth-child(3) .search-rank { color: #409eff; }

.search-topic-icon {
  width: 34px;
  height: 24px;
  border-radius: 6px;
  background: #f1f3f6;
  color: #6b7280;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.search-thumb {
  width: 42px;
  height: 28px;
  object-fit: cover;
  border-radius: 4px;
}

.search-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.header-username {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.notify-button {
  color: #1f2d3d;
}

:deep(.notification-center-dialog .el-dialog__body) {
  padding: 0;
}

.notification-center {
  height: min(76vh, 680px);
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  background: #f5f7fb;
  border-radius: 8px;
  overflow: hidden;
}

.notification-tabs {
  padding: 18px 12px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 14px;

  button {
    position: relative;
    border: 0;
    border-radius: 8px;
    background: transparent;
    min-height: 74px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: #7a8495;
    cursor: pointer;
    font-weight: 600;

    &.active {
      background: #f0f6ff;
      color: #1e6bcb;
    }

    i {
      position: absolute;
      top: 8px;
      right: 10px;
      min-width: 18px;
      height: 18px;
      padding: 0 5px;
      border-radius: 999px;
      background: #f56c6c;
      color: #fff;
      font-size: 12px;
      line-height: 18px;
      font-style: normal;
    }
  }
}

.tab-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #e8f1ff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  font-weight: 800;
}

.notification-body {
  min-width: 0;
  padding: 26px 28px;
  overflow-y: auto;
}

.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;

  h3 {
    margin: 0;
    font-size: 20px;
    color: #111827;
  }

  button {
    border: 0;
    background: transparent;
    font-size: 32px;
    line-height: 1;
    cursor: pointer;
    color: #111827;
  }
}

.comment-notification-tabs {
  display: inline-flex;
  gap: 24px;
  margin: -4px 0 18px;
  border-bottom: 1px solid #e5e7eb;
  width: 100%;

  button {
    position: relative;
    border: 0;
    background: transparent;
    padding: 0 0 10px;
    color: #6b7280;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;

    &.active {
      color: #111827;

      &::after {
        content: "";
        position: absolute;
        left: 0;
        right: 0;
        bottom: -1px;
        height: 3px;
        border-radius: 999px;
        background: #111827;
      }
    }
  }
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.notification-card {
  border-radius: 10px;
  background: #fff;
  padding: 16px 18px;
  cursor: pointer;
  border: 1px solid transparent;

  &:hover {
    border-color: #c6dcff;
  }

  &.unread {
    border-color: #dbeafe;
    background: #f8fbff;
  }

  p {
    margin: 8px 0;
    color: #4b5563;
    line-height: 1.6;
  }

  small {
    color: #8a92a3;
  }
}

.notification-follow-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.follow-user-info {
  min-width: 0;
  border: 0;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0;
  cursor: pointer;
  text-align: left;

  span {
    min-width: 0;
    display: grid;
    grid-template-columns: minmax(0, auto) auto;
    align-items: center;
    column-gap: 6px;
  }

  strong {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #111827;
    font-size: 16px;
  }

  em {
    border-radius: 4px;
    padding: 1px 5px;
    background: #329afe;
    color: #fff;
    font-size: 11px;
    font-style: normal;
    line-height: 16px;
  }

  small {
    grid-column: 1 / -1;
    margin-top: 4px;
    color: #8a92a3;
  }
}

.follow-action {
  flex: 0 0 auto;
  border: 0;
  border-radius: 3px;
  min-width: 88px;
  height: 36px;
  padding: 0 16px;
  background: #111827;
  color: #fff;
  cursor: pointer;
  font-weight: 700;

  &.followed {
    background: #f0f2f5;
    color: #7a8495;
    cursor: default;
  }
}

.notification-comment-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0;
  padding: 10px;
  border-radius: 8px;
  background: #f5f7fb;
  color: #6b7280;

  img {
    width: 54px;
    height: 40px;
    border-radius: 5px;
    object-fit: cover;
    background: #eef1f5;
  }

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.notification-actor {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.notification-actor-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;

  strong {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #1f2d3d;
  }

  span {
    padding: 1px 5px;
    border-radius: 4px;
    background: #329afe;
    color: #fff;
    font-size: 11px;
    line-height: 16px;
  }
}

.notification-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  strong {
    min-width: 0;
    color: #111827;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.report-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;

  div {
    display: grid;
    grid-template-columns: 86px minmax(0, 1fr);
    gap: 14px;
  }

  span {
    color: #8a92a3;
  }

  strong,
  p {
    margin: 0;
    color: #1f2d3d;
    line-height: 1.6;
  }
}

.content-main {
  height: calc(100vh - 70px);
  width: 100%;
  overflow: hidden;
}

.content {
  width: min(98.5vw, 1880px);
  height: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr) 300px;
  gap: 12px;
  padding: 24px 0;
  box-sizing: border-box;
  min-height: 0;
}

.left {
  min-height: 0;

  .letf-content {
    width: 100%;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: rgba(149, 157, 165, 0.16) 0 8px 24px;
    overflow: hidden;
  }

  .left-content-item {
    display: flex;
    align-items: center;
    font-size: 15px;
    font-weight: 700;
    padding: 17px 30px;
    cursor: pointer;
    box-sizing: border-box;
    color: #0d2538;

    &:hover {
      background-color: #f3f5f8;
    }

    .left-content-item-text {
      margin-left: 12px;

      &.with-badge {
        display: inline-flex;
        align-items: center;
        gap: 8px;
      }
    }

    .nav-unread-badge {
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

.publish-button {
  width: 100%;
  margin: 20px 0 0;
}

.center {
  min-width: 0;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: rgba(149, 157, 165, 0.16) 0 8px 24px;
  padding: 12px;
  box-sizing: border-box;
  overflow-y: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  :deep(> *) {
    flex: 1;
    min-height: 0;
  }
}

.right {
  min-width: 0;
  min-height: 0;
  height: 100%;
  overflow-y: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.right-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hot-communities,
.hot-games,
.friend-links {
  background: #fff;
  border-radius: 8px;
  box-shadow: rgba(149, 157, 165, 0.16) 0 8px 24px;
  padding: 14px;
}

.hot-communities h3,
.hot-games h3,
.friend-links h3 {
  margin: 0 0 14px;
  color: #0d2538;
  font-size: 18px;
}

.game-list {
  display: flex;
  flex-direction: column;
}

.game-item {
  display: grid;
  grid-template-columns: 44px 50px minmax(0, 1fr);
  gap: 9px;
  align-items: center;
  min-height: 68px;
  border-bottom: 1px solid #eef1f5;
  cursor: pointer;

  &:last-child {
    border-bottom: 0;
  }

  &:hover .game-meta span {
    color: #409eff;
  }

  .rank {
    color: #409eff;
    font-size: 14px;
    font-weight: 800;
  }

  img {
    width: 50px;
    height: 50px;
    object-fit: cover;
    border-radius: 6px;
    background: #f2f4f7;
  }

  .game-meta {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 5px;

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: #0d2538;
      font-weight: 600;
    }

    small {
      color: #8a92a3;
      font-weight: 400;
    }
  }
}

.link-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  a {
    color: #4b5563;
    text-decoration: none;
    background: #f5f6f8;
    border-radius: 6px;
    padding: 8px 12px;

    &:hover {
      color: #409eff;
    }
  }
}

@media (max-width: 1280px) {
  .header-content {
    grid-template-columns: 190px minmax(260px, 1fr) 210px;
    gap: 16px;
  }

  .content {
    grid-template-columns: 210px minmax(0, 1fr);
  }

  .right {
    display: none;
  }
}
</style>
