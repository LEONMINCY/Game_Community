<!-- 文件说明：views/search/index.vue，全局搜索结果页，按内容、游戏、用户和话题展示搜索结果。 -->
<template>
  <div class="search-page">
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="result-body" v-loading="loading">
      <template v-if="activeTab === 'posts'">
        <article
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          @click="goPost(post.id)"
        >
          <div class="author-line">
            <el-avatar :size="28" :src="replaceURL(post.userAvatar) || imageFallbackURL()" />
            <span>{{ post.nickname || '社区玩家' }}</span>
            <el-tag size="small">Lv{{ post.userLevel || 0 }}</el-tag>
          </div>
          <h3 v-html="highlight(post.title)"></h3>
          <p v-html="highlight(post.plainContent)"></p>
          <div v-if="post.images && post.images.length" class="post-images">
            <img v-for="img in post.images" :key="img" :src="img" alt="" loading="lazy" @error="handleImageError">
          </div>
          <div class="post-footer">
            <span>{{ post.createTime || '' }}</span>
            <span>点赞 {{ post.likes || 0 }}</span>
            <span>收藏 {{ post.favorites || 0 }}</span>
          </div>
        </article>
      </template>

      <template v-else-if="activeTab === 'games'">
        <div
          v-for="game in games"
          :key="game.id"
          class="game-card"
          @click="goGame(game.id)"
        >
          <img :src="replaceURL(game.icon)" alt="" loading="lazy" @error="handleImageError">
          <div class="game-info">
            <h3 v-html="highlight(game.name)"></h3>
            <p>{{ game.developer || game.type || '游戏详情' }}</p>
          </div>
        </div>
      </template>

      <template v-else-if="activeTab === 'users'">
        <div
          v-for="user in users"
          :key="user.userId"
          class="user-result-card"
          @click="goUser(user.userId)"
        >
          <el-avatar :size="64" :src="replaceURL(user.avatar) || imageFallbackURL()" />
          <div class="user-result-main">
            <div class="user-name-row">
              <h3 v-html="highlight(user.nickname || user.username || '社区玩家')"></h3>
              <el-tag size="small" effect="dark">Lv{{ user.ex2 || 0 }}</el-tag>
            </div>
            <p>粉丝{{ user.fansCount || 0 }} · 内容{{ user.postCount || 0 }}</p>
          </div>
          <button
            v-if="!isSelf(user.userId)"
            type="button"
            class="follow-button"
            :class="{ followed: user.follow }"
            @click.stop="followUser(user)"
          >
            {{ user.follow ? '已关注' : '+ 关注' }}
          </button>
        </div>
      </template>

      <template v-else>
        <div
          v-for="topic in topics"
          :key="topic.topic"
          class="topic-card"
          @click="goTopic(topic.topic)"
        >
          <span class="topic-icon">#</span>
          <div>
            <h3 v-html="highlight(topic.topic)"></h3>
            <p>{{ topic.count || 0 }} 个帖子正在讨论</p>
          </div>
        </div>
      </template>

      <el-empty v-if="!loading && currentEmpty" :description="emptyDescription" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { deleteRequest, getRequest, postRequest } from '@/utils/http'
import { handleImageError, imageFallbackURL, replaceURL, removeTags } from '@/utils/tools'
import { requireLogin } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const tabs = [
  { key: 'posts', label: '内容' },
  { key: 'games', label: '游戏' },
  { key: 'users', label: '用户' },
  { key: 'topics', label: '话题' }
]

const activeTab = ref('posts')
const keyword = ref('')
const loading = ref(false)
const posts = ref([])
const games = ref([])
const users = ref([])
const topics = ref([])
const loaded = ref({})
const lastKeyword = ref('')
const errorText = ref('')
let requestSeq = 0

const currentEmpty = computed(() => {
  if (activeTab.value === 'posts') return posts.value.length === 0
  if (activeTab.value === 'games') return games.value.length === 0
  if (activeTab.value === 'users') return users.value.length === 0
  return topics.value.length === 0
})

const emptyDescription = computed(() => errorText.value || '暂无搜索结果')

onMounted(() => {
  syncFromRoute()
  searchActiveTab()
})

watch(
  () => route.fullPath,
  () => {
    const keywordChanged = syncFromRoute()
    searchActiveTab(keywordChanged)
  }
)

const syncFromRoute = () => {
  const nextKeyword = String(route.query.keyword || '').trim()
  const keywordChanged = nextKeyword !== lastKeyword.value
  keyword.value = nextKeyword
  if (keywordChanged) {
    loaded.value = {}
    lastKeyword.value = nextKeyword
    errorText.value = ''
  }
  const tab = String(route.query.tab || 'posts')
  activeTab.value = tabs.some(item => item.key === tab) ? tab : 'posts'
  return keywordChanged
}

const switchTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  router.replace({
    path: '/search',
    query: {
      keyword: keyword.value,
      tab
    }
  })
}

const searchActiveTab = async (force = false) => {
  if (!keyword.value) {
    posts.value = []
    games.value = []
    users.value = []
    topics.value = []
    loaded.value = {}
    return
  }
  const tab = activeTab.value
  if (!force && loaded.value[tab]) {
    return
  }

  const seq = ++requestSeq
  loading.value = true
  errorText.value = ''
  try {
    let success = false
    if (tab === 'posts') {
      success = await searchPosts()
    } else if (tab === 'games') {
      success = await searchGames()
    } else if (tab === 'users') {
      success = await searchUsers()
    } else {
      success = await searchTopics()
    }
    if (seq === requestSeq && success) {
      loaded.value[tab] = true
    }
  } finally {
    if (seq === requestSeq) {
      loading.value = false
    }
  }
}

const searchPosts = async () => {
  const res = await getRequest(`/post/search?keyword=${encodeURIComponent(keyword.value)}&limit=20`, { silentError: true }).catch(() => null)
  posts.value = res && res.code === 200 ? (res.data || []).map(normalizePost) : []
  if (!res || res.code !== 200) {
    errorText.value = '搜索服务暂时不可用，请稍后再试'
    return false
  }
  return true
}

const searchGames = async () => {
  const res = await getRequest(`/game/listAll?keyword=${encodeURIComponent(keyword.value)}&limit=30`, { silentError: true }).catch(() => null)
  games.value = res && res.code === 200 ? (res.data || []) : []
  if (!res || res.code !== 200) {
    errorText.value = '搜索服务暂时不可用，请稍后再试'
    return false
  }
  return true
}

const searchUsers = async () => {
  const url = `/noLogin/user-info/search?keyword=${encodeURIComponent(keyword.value)}&limit=30`
  const res = await getRequest(url, { silentError: true }).catch(() => null)
  users.value = res && res.code === 200 ? (res.data || []) : []
  if (!res || res.code !== 200) {
    errorText.value = '用户搜索服务暂时不可用，请稍后再试'
    return false
  }
  return true
}

const searchTopics = async () => {
  const res = await getRequest('/post/hotTopics?limit=200', { silentError: true }).catch(() => null)
  const list = res && res.code === 200 ? (res.data || []) : []
  const lower = keyword.value.toLowerCase()
  topics.value = list.filter(item => String(item.topic || '').toLowerCase().includes(lower))
  if (!res || res.code !== 200) {
    errorText.value = '话题搜索服务暂时不可用，请稍后再试'
    return false
  }
  return true
}

const formatContent = (content) => {
  const text = removeTags(content || '')
  return text.length > 180 ? text.slice(0, 180) + '...' : text
}

const normalizePost = (post) => {
  return {
    ...post,
    plainContent: formatContent(post.content),
    images: getImages(post).slice(0, 3).map(img => replaceURL(img))
  }
}

const getImages = (post) => {
  try {
    const media = JSON.parse(post.media || '[]')
    return Array.isArray(media) ? media : []
  } catch (error) {
    return []
  }
}

const escapeHtml = (value) => {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const highlight = (value) => {
  const safeValue = escapeHtml(value)
  if (!keyword.value) {
    return safeValue
  }
  const escapedKeyword = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safeValue.replace(new RegExp(escapedKeyword, 'gi'), match => `<mark>${match}</mark>`)
}

const isSelf = (userId) => {
  return Number(sessionStorage.getItem('userId') || 0) === Number(userId)
}

const followUser = async (user) => {
  if (!requireLogin(route.fullPath)) {
    return
  }
  if (user.follow) {
    const res = await deleteRequest(`/userRelation/unfollow/${user.userId}`, { silentError: true }).catch(() => null)
    if (res && res.code === 200) {
      user.follow = false
      user.fansCount = Math.max((user.fansCount || 0) - 1, 0)
      ElMessage.success('已取消关注')
    } else {
      ElMessage.warning('取消关注失败，请稍后再试')
    }
  } else {
    const res = await postRequest(`/userRelation/follow/${user.userId}`, {}, { silentError: true }).catch(() => null)
    if (res && res.code === 200) {
      user.follow = true
      user.fansCount = (user.fansCount || 0) + 1
      ElMessage.success('关注成功')
    } else {
      ElMessage.warning('关注失败，请稍后再试')
    }
  }
}

const goPost = (id) => {
  router.push('/forum?id=' + id)
}

const goGame = (id) => {
  router.push('/gameDetail?id=' + id)
}

const goUser = (userId) => {
  router.push({ path: '/userinfo', query: { userId } })
}

const goTopic = (topic) => {
  router.push({ path: '/forumlist', query: { topic } })
}
</script>

<style scoped lang="less">
.search-page {
  height: 100%;
  min-height: 0;
  overflow-y: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tabs {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 4px 10px 0;
  border-bottom: 1px solid #edf0f4;

  button {
    position: relative;
    border: 0;
    background: transparent;
    padding: 12px 0;
    color: #526070;
    font-size: 16px;
    cursor: pointer;

    &::after {
      content: '';
      position: absolute;
      left: 2px;
      right: 2px;
      bottom: -1px;
      height: 3px;
      border-radius: 999px;
      background: transparent;
    }

    &.active {
      color: #111827;
      font-weight: 700;
    }

    &.active::after {
      background: #111827;
    }
  }
}

.result-body {
  padding: 18px 10px 6px;
}

.post-card,
.game-card,
.topic-card {
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    background: #f7f9fc;
    box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
  }
}

.post-card {
  padding: 18px 16px;
  border-bottom: 1px solid #edf0f4;

  h3 {
    margin: 12px 0 8px;
    color: #0d2538;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: #4b5563;
    line-height: 1.7;
  }
}

.author-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #526070;
  font-size: 13px;
}

.post-images {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  max-width: 640px;

  img {
    width: 100%;
    aspect-ratio: 16 / 10;
    object-fit: cover;
    border-radius: 6px;
    background: #f3f5f8;
  }
}

.post-footer {
  margin-top: 12px;
  display: flex;
  gap: 18px;
  color: #8a92a3;
  font-size: 13px;
}

.game-card,
.topic-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f4;
}

.game-card img {
  width: 82px;
  height: 58px;
  object-fit: cover;
  border-radius: 6px;
  background: #f3f5f8;
}

.game-info,
.topic-card > div {
  min-width: 0;

  h3 {
    margin: 0 0 6px;
    color: #0d2538;
    font-size: 17px;
  }

  p {
    margin: 0;
    color: #8a92a3;
    font-size: 13px;
  }
}

.user-result-card {
  min-height: 94px;
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 90px;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f4;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background: #f8fafc;
  }
}

.user-result-main {
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: 6px;

  h3 {
    max-width: min(420px, 70vw);
    margin: 0;
    color: #14191e;
    font-size: 18px;
    font-weight: 800;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.user-result-main p {
  margin: 7px 0 0;
  color: #7b8494;
  font-size: 14px;
}

.follow-button {
  justify-self: end;
  border: 0;
  border-radius: 4px;
  background: #24292e;
  color: #fff;
  height: 34px;
  min-width: 74px;
  padding: 0 12px;
  font-size: 15px;
  cursor: pointer;

  &.followed {
    background: #eef1f5;
    color: #606a78;
    cursor: pointer;
  }
}

.topic-icon {
  width: 46px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f1f3f6;
  color: #5f6b7a;
  font-size: 18px;
  font-weight: 800;
}

:deep(mark) {
  background: #dbeafe;
  color: #1769e0;
  border-radius: 3px;
  padding: 0 2px;
}

@media (max-width: 720px) {
  .user-result-card {
    grid-template-columns: 52px minmax(0, 1fr);

    .el-avatar {
      width: 52px !important;
      height: 52px !important;
    }
  }

  .follow-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
