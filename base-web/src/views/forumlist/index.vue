<!-- 文件说明：views/forumlist/index.vue，社区标签帖子列表，展示指定游戏或话题下的帖子。 -->
<template>
  <div class="forumlist-main">
    <div class="gameListWrapper">
      <div class="gameList" ref="gameListRef">
        <div class="gameItem" :class="{ active: !selectedGameId && !selectedTopic }" @click="selectGame(null)">
          <el-image class="gameIcon" src="/logo.png" fit="cover" />
          <span>推荐</span>
        </div>
        <div
          v-for="game in gameList"
          :key="game.id"
          class="gameItem"
          :class="{ active: selectedGameId === game.id }"
          @click="selectGame(game.id)"
        >
          <el-image class="gameIcon" :src="thumbnailURL(game.icon, 120)" fit="cover" lazy>
            <template #error>
              <img class="gameIconFallback" :src="imageFallbackURL()" alt="">
            </template>
          </el-image>
          <span>{{ game.name }}</span>
        </div>
      </div>
    </div>

    <el-input
      v-model="searchValue"
      placeholder="搜索内容、话题或游戏关键词"
      clearable
      class="searchInput"
      @clear="submitSearch"
      @keyup.enter="submitSearch"
    />

    <div class="listWrapper" :class="{ loading: listLoading }">
      <div v-if="listLoading" class="list-loading">正在加载帖子...</div>
      <div v-if="!listLoading && forumList.length === 0" class="empty-state">
        <el-empty description="暂无帖子" />
      </div>

      <article class="listItem" v-for="item in forumList" :key="item.id" @click="findOutMore(item.id)">
        <div class="userInfo" @click.stop="viewUsers(item.userId)">
          <el-avatar style="cursor: pointer" :size="25" :src="item.userAvatar || imageFallbackURL()" />
          <div class="name">{{ item.nickname }}</div>
          <div class="level">Lv{{ item.userLevel || 0 }}</div>
        </div>

        <div class="metaLine">
          <div v-if="item.gameName" class="gameTag" @click.stop="goGameCommunity(item.gameId)">{{ item.gameName }}</div>
          <div class="topicWrapper" v-if="getTopics(item).length">
            <span
              v-for="topic in getTopics(item)"
              :key="topic"
              class="topicTag"
              @click.stop="goTopic(topic)"
            >
              #{{ topic }}
            </span>
          </div>
        </div>

        <div class="titleWrapper">{{ item.title }}</div>
        <div class="contentWrapper">{{ formatPostContent(item.content) }}</div>

        <div class="imgWrapper" v-if="item.images && item.images.length">
          <div class="imgCard" v-for="(img, index) in visibleImages(item)" :key="img + index">
            <img
              class="postThumb"
              :src="thumbnailURL(img, 420)"
              loading="lazy"
              decoding="async"
              alt=""
              @error="useOriginalImage($event, img)"
            >
            <span v-if="index === 2 && item.images.length > 3" class="moreMask">+{{ item.images.length - 3 }}</span>
          </div>
        </div>

        <div class="actionWrapper">
          <div class="actionItem" @click.stop="handleLike(item)">
            <img v-if="item.isLikes" src="@/assets/imgs/likeB.png" alt="like">
            <img v-else src="@/assets/imgs/likeW.png" alt="like">
            <span class="count">{{ item.likes }}</span>
          </div>
          <div class="actionItem" @click.stop="handleFavorite(item)">
            <img v-if="item.isFavorites" src="@/assets/imgs/FavoriteB.png" alt="favorite">
            <img v-else src="@/assets/imgs/FavoritW.png" alt="favorite">
            <span class="count">{{ item.favorites }}</span>
          </div>
          <div class="actionItem" @click.stop="openShare(item)">
            <el-icon><Share /></el-icon>
            <span class="count">{{ item.shareCount || 0 }}</span>
          </div>
        </div>
      </article>
    </div>

    <PostShareDialog v-model="shareDialogVisible" :post="shareTarget" @shared="handleShared" />
  </div>
</template>

<script setup>
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "../../store/index.js";
import { ref, onMounted, watch } from "vue";
import { getRequest, postRequest } from '@/utils/http'
import { handleImageError, imageFallbackURL, replaceURL, thumbnailURL } from '@/utils/tools'
import { Share } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { hasToken, requireLogin } from '@/utils/auth'
import PostShareDialog from '@/components/PostShareDialog.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const forumList = ref([])
const gameList = ref([])
const selectedGameId = ref(null)
const selectedTopic = ref('')
const searchValue = ref('')
const shareDialogVisible = ref(false)
const shareTarget = ref(null)
const listLoading = ref(false)
const forumCache = new Map()
let requestSeq = 0

onMounted(() => {
  getGameList()
  syncRouteQuery()
})

watch(
  () => route.query,
  () => {
    syncRouteQuery()
  }
)

const getGameList = () => {
  getRequest('/game/hotCommunities?limit=10', { silentError: true }).then(res => {
    if (res.code === 200) {
      gameList.value = (res.data || []).map(game => ({
        ...game,
        icon: replaceURL(game.icon)
      }))
    }
  }).catch(() => {
    gameList.value = []
  })
}

const selectGame = (gameId) => {
  const nextGameId = gameId ? Number(gameId) : null
  if (selectedGameId.value === nextGameId && !searchValue.value.trim() && !selectedTopic.value.trim()) {
    return
  }
  router.push({
    path: '/forumlist',
    query: nextGameId ? { gameId: nextGameId } : {}
  })
}

const syncRouteQuery = () => {
  selectedGameId.value = route.query.gameId ? Number(route.query.gameId) : null
  searchValue.value = route.query.keyword ? String(route.query.keyword) : ''
  selectedTopic.value = route.query.topic ? String(route.query.topic) : ''
  getForumList()
}

const submitSearch = () => {
  const keyword = searchValue.value.trim()
  const query = {}
  if (keyword) query.keyword = keyword
  if (selectedGameId.value) query.gameId = selectedGameId.value
  router.push({ path: '/forumlist', query })
}

const buildListCacheKey = () => {
  return JSON.stringify({
    gameId: selectedGameId.value || '',
    keyword: searchValue.value.trim(),
    topic: selectedTopic.value.trim()
  })
}

const fetchPostList = async () => {
  const keyword = searchValue.value.trim() || selectedTopic.value.trim()
  if (keyword || selectedGameId.value) {
    const params = []
    if (keyword) params.push(`keyword=${encodeURIComponent(keyword)}`)
    if (selectedGameId.value) params.push(`gameId=${selectedGameId.value}`)
    return getRequest(`/post/search?${params.join('&')}`, { silentError: true })
  }
  return getRequest('/post/recommend', { silentError: true })
}

const getForumList = async () => {
  const cacheKey = buildListCacheKey()
  const currentSeq = ++requestSeq
  if (forumCache.has(cacheKey)) {
    forumList.value = forumCache.get(cacheKey)
  } else {
    listLoading.value = true
  }

  try {
    const res = await fetchPostList()
    if (currentSeq !== requestSeq) return
    if (res.code === 200) {
      const list = (res.data || []).map(normalizePost)
      forumCache.set(cacheKey, list)
      forumList.value = list
    } else {
      ElMessage.error(res.msg || '加载帖子失败')
    }
  } catch (error) {
    if (currentSeq === requestSeq) {
      forumList.value = forumCache.get(cacheKey) || []
      ElMessage.error('加载帖子失败，请稍后重试')
    }
  } finally {
    if (currentSeq === requestSeq) {
      listLoading.value = false
    }
  }
}

const parseMediaImages = (media) => {
  try {
    const value = JSON.parse(media || '[]')
    return Array.isArray(value) ? value : []
  } catch (error) {
    return []
  }
}

const normalizePost = (item) => {
  const mediaImages = parseMediaImages(item.media)
  return {
    ...item,
    images: mediaImages.map(img => replaceURL(img)),
    userAvatar: replaceURL(item.userAvatar),
    isLikes: item.isLikes || item.isLiked || false,
    isFavorites: item.isFavorites || item.isFavorited || false,
    likes: item.likes || 0,
    favorites: item.favorites || 0,
    shareCount: item.shareCount || 0
  }
}

const formatPostContent = (content) => {
  const tempDiv = document.createElement('div')
  tempDiv.innerHTML = content || ''
  const text = tempDiv.textContent || tempDiv.innerText || ''
  return text.length > 100 ? text.slice(0, 100) + '...' : text
}

const visibleImages = (item) => (item.images || []).slice(0, 3)

const useOriginalImage = (event, originalUrl) => {
  const image = event.target
  if (!image) return
  if (image.dataset.fallback === '1') {
    handleImageError(event)
    return
  }
  image.dataset.fallback = '1'
  image.src = originalUrl
}

const getTopics = (item) => {
  return String(item.topics || '')
    .split(',')
    .map(topic => topic.trim())
    .filter(Boolean)
}

const goTopic = (topic) => {
  router.push({ path: '/forumlist', query: { topic } })
}

const goGameCommunity = (gameId) => {
  if (gameId) {
    router.push({ path: '/forumlist', query: { gameId } })
  }
}

const findOutMore = (id) => {
  router.push('/forum?id=' + id)
}

const viewUsers = (userId) => {
  const currentUserId = userStore.userInfo?.userId || sessionStorage.getItem('userId')
  if (String(currentUserId || '') === String(userId || '')) {
    router.push('/userinfo')
  } else {
    router.push('/userinfo?userId=' + userId)
  }
}

const handleLike = async (item) => {
  if (!hasToken() && !requireLogin(router.currentRoute.value.fullPath)) return
  const url = item.isLikes ? '/post/unlike/' : '/post/like/'
  const res = await postRequest(url + item.id)
  if (res.code === 200) {
    item.isLikes = !item.isLikes
    item.likes = item.isLikes ? (item.likes || 0) + 1 : Math.max((item.likes || 0) - 1, 0)
  }
}

const handleFavorite = async (item) => {
  if (!hasToken() && !requireLogin(router.currentRoute.value.fullPath)) return
  const url = item.isFavorites ? '/post/unFavorite/' : '/post/favorite/'
  const res = await postRequest(url + item.id)
  if (res.code === 200) {
    item.isFavorites = !item.isFavorites
    item.favorites = item.isFavorites ? (item.favorites || 0) + 1 : Math.max((item.favorites || 0) - 1, 0)
  }
}

const openShare = (item) => {
  if (!hasToken() && !requireLogin(router.currentRoute.value.fullPath)) return
  shareTarget.value = item
  shareDialogVisible.value = true
}

const handleShared = (shareCount) => {
  if (shareTarget.value) {
    shareTarget.value.shareCount = shareCount
  }
}
</script>

<style lang="less" scoped>
.forumlist-main {
  width: 100%;
  height: 100%;
  position: relative;
  overflow-y: auto;
  -ms-overflow-style: none;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.gameListWrapper {
  position: sticky;
  top: 0;
  z-index: 10;
  width: 100%;
  box-sizing: border-box;
  padding: 10px 8px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.gameList {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(11, minmax(56px, 1fr));
  gap: clamp(6px, 0.7vw, 12px);
  overflow: hidden;
}

.gameItem {
  min-width: 0;
  height: 74px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 6px 4px;
  border-radius: 8px;
  cursor: pointer;
  box-sizing: border-box;
  transition: background-color 0.2s ease;

  .gameIcon {
    width: 30px;
    height: 30px;
    flex-shrink: 0;
    border-radius: 4px;
  }

  .gameIconFallback {
    width: 30px;
    height: 30px;
    display: block;
    object-fit: cover;
    border-radius: 4px;
  }

  span {
    width: 100%;
    color: #606266;
    font-size: 12px;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.active,
  &:hover {
    background-color: #f2f3f5;
  }

  &.active span {
    color: #303133;
    font-weight: 600;
  }
}

.searchInput {
  margin: 10px 0 8px;
}

.listWrapper {
  position: relative;
  width: 100%;
  min-height: 260px;

  &.loading {
    opacity: 0.76;
    pointer-events: none;
  }
}

.list-loading {
  position: sticky;
  top: 86px;
  z-index: 8;
  width: fit-content;
  margin: 10px auto;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
  font-size: 13px;
  backdrop-filter: blur(6px);
}

.listItem {
  width: 100%;
  margin-bottom: 10px;
  padding: 15px;
  box-sizing: border-box;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 8px;

  &:hover {
    background-color: #f7f8fa;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }
}

.userInfo {
  display: flex;
  align-items: center;

  .name {
    margin-left: 6px;
    color: #64696e;
    font-size: 12px;
  }

  .level {
    background-color: #329afe;
    color: #fff;
    font-size: 10px;
    margin-left: 8px;
    height: 14px;
    line-height: 14px;
    padding: 0 3px;
    border-radius: 3px;
  }
}

.metaLine {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.gameTag,
.topicTag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.gameTag {
  background-color: #e6f3ff;
  color: #329afe;

  &:hover {
    background-color: #d7ebff;
  }
}

.topicWrapper {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.topicTag {
  background: #f4f6f8;
  color: #606266;

  &:hover {
    color: #409eff;
    background: #ecf5ff;
  }
}

.titleWrapper {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 700;
  color: #0d2538;
}

.contentWrapper {
  margin-top: 6px;
  width: 100%;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.6;
  color: #666;
  font-size: 14px;
}

.imgWrapper {
  margin-top: 10px;
  width: min(100%, 540px);
  display: flex;
  gap: 8px;
}

.imgCard {
  position: relative;
  width: 164px;
  aspect-ratio: 16 / 10;
  flex: 0 1 164px;
  border-radius: 8px;
  overflow: hidden;
  background: #f3f6fa;
}

.postThumb {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.moreMask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.actionWrapper {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  gap: 20px;
}

.actionItem {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: #666;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    background-color: #f0f0f0;
  }

  img {
    width: 20px;
    height: 20px;
    transition: transform 0.2s;
  }

  &:hover img {
    transform: scale(1.1);
  }

  .count {
    font-size: 14px;
    color: #666;
    margin-left: 4px;
  }
}

@media (max-width: 1500px) {
  .gameList {
    grid-template-columns: repeat(11, minmax(48px, 1fr));
  }

  .gameItem {
    height: 70px;
  }
}
</style>
