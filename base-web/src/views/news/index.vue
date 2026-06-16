<!-- 文件说明：views/news/index.vue，新闻中心页面，展示新闻列表、详情、评论、点赞、收藏和转发。 -->
<template>
  <div class="news-container">
    <div class="search-box">
      <el-input
        v-model="queryParams.title"
        placeholder="请输入关键字搜索"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch">
        <template #suffix>
          <el-icon class="search-icon" @click="handleSearch"><Search /></el-icon>
        </template>
      </el-input>
    </div>
    <div class="news-list">
      <div v-for="(item, index) in newsList"
           :key="item.id || index"
           class="news-item"
           @click="goToDetail(item)">
        <div class="news-content">
          <div class="news-info">
            <h3 class="news-title">{{ item.title }}</h3>
            <p class="news-desc">{{ item.description }}</p>
            <div class="news-meta">
              <span class="news-time">{{ item.publishTime }}</span>
              <span class="news-author">{{ item.author }}</span>
              <span class="news-views">
                <el-icon><View /></el-icon>
                {{ item.browCount }}
              </span>
              <span class="news-action" :class="{ active: item.isLikes }" @click.stop="handleNewsLike(item)">
                <el-icon><Pointer /></el-icon>
                {{ item.likes || 0 }}
              </span>
              <span class="news-action" :class="{ active: item.isFavorites }" @click.stop="handleNewsFavorite(item)">
                <el-icon><Star /></el-icon>
                {{ item.favorites || 0 }}
              </span>
              <span class="news-action" @click.stop="shareNews(item)">
                <el-icon><Share /></el-icon>
                {{ item.shareCount || 0 }}
              </span>
            </div>
          </div>
          <div class="news-image" v-if="item.coverImage">
            <el-image
              :src="item.coverImage"
              fit="cover"
              :preview-src-list="[item.coverImage]"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Pointer, Search, Share, Star, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { postRequest } from "@/utils/http.js";
import { replaceURL, removeTags  } from '@/utils/tools';

const router = useRouter()
const newsList = ref([])
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  title: ''
});

// 模拟新闻数据
const mockNews = () => {
  return {
    id: Date.now(),
    title: '《天国：拯救2》最新预告片发布，展示全新战斗系统',
    description: '开发商Warhorse Studios今日发布了《天国：拯救2》的最新预告片，展示了游戏全新的战斗系统和改进的AI。预告片中展示了更加流畅的战斗动作和更智能的敌人AI...',
    publishTime: '2024-03-21 14:30',
    author: '游戏社区',
    views: Math.floor(Math.random() * 10000),
    coverImage: 'https://cdn.akamai.steamstatic.com/steam/apps/1771300/header.jpg'
  }
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

const normalizeNewsItem = (item) => {
  const id = Number(item?.id)
  if (!Number.isInteger(id) || id <= 0) {
    return null
  }
  return {
    ...item,
    id,
    coverImage: parseNewsCover(item.mediaJson),
    description: removeTags(item.content),
    views: Math.floor(Math.random() * 10000),
    publishTime: item.createTime,
    author: '游戏社区',
    likes: item.likes || 0,
    favorites: item.favorites || 0,
    shareCount: item.shareCount || 0,
    isLikes: !!item.isLikes,
    isFavorites: !!item.isFavorites,
  }
}

const getNewsList = () => {
  postRequest('/news/page', queryParams.value).then(res => {
    if (res.code === 200) {
      newsList.value.push(
          ...(res.data.list || []).map(normalizeNewsItem).filter(Boolean)
      )

    }
  })
  console.log(newsList)
}

// 初始加载新闻
const loadNews = () => {
  // 模拟加载10条新
  queryParams.value.pageNo += 1
  getNewsList()
}

// 检查滚动位置并加载更多新闻
const checkScroll = () => {
  const scrollHeight = document.documentElement.scrollHeight
  const scrollTop = document.documentElement.scrollTop
  const clientHeight = document.documentElement.clientHeight

  // 当滚动到距离底部100px时加载更多
  if (scrollHeight - scrollTop - clientHeight < 100) {
    loadNews()
  }
}

// 跳转到新闻详情页
const goToDetail = (item) => {
  const id = Number(item?.id)
  if (!Number.isInteger(id) || id <= 0) {
    ElMessage.warning('新闻信息不存在')
    return
  }
  item.browCount = (item.browCount || 0) + 1
  router.push({
    path: '/news/detail',
    query: {
      id
    }
  })
}

// 处理搜索
const handleSearch = () => {
  queryParams.value.pageNo = 1
  newsList.value = []
  getNewsList()
}

const handleNewsLike = (item) => {
  const url = item.isLikes ? '/news/unlike/' : '/news/like/'
  postRequest(url + item.id).then(res => {
    if (res.code === 200) {
      item.likes = res.data.likes || 0
      item.isLikes = !!res.data.isLikes
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const handleNewsFavorite = (item) => {
  const url = item.isFavorites ? '/news/unFavorite/' : '/news/favorite/'
  postRequest(url + item.id).then(res => {
    if (res.code === 200) {
      item.favorites = res.data.favorites || 0
      item.isFavorites = !!res.data.isFavorites
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  })
}

const shareNews = (item) => {
  postRequest('/news/share/' + item.id).then(async res => {
    if (res.code === 200) {
      item.shareCount = res.data || 0
      const href = window.location.origin + router.resolve({ path: '/news/detail', query: { id: item.id } }).href
      try {
        await navigator.clipboard.writeText(href)
        ElMessage.success('链接已复制')
      } catch (error) {
        ElMessage.success('转发次数已更新')
      }
    }
  })
}

onMounted(() => {
  getNewsList()
  window.addEventListener('scroll', checkScroll)
})

</script>

<style lang="less" scoped>
.news-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
  padding: 15px;

  .search-box {
    margin-bottom: 20px;
    
    .search-icon {
      cursor: pointer;
      &:hover {
        color: #409EFF;
      }
    }
  }

  &::-webkit-scrollbar {
    display: none;
  }

  .news-list {
    .news-item {
      background: #fff;
      padding: 20px;
      margin-bottom: 15px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
      }

      .news-content {
        display: flex;
        justify-content: space-between;
        gap: 20px;

        .news-info {
          flex: 1;

          .news-title {
            font-size: 18px;
            font-weight: bold;
            color: #333;
            margin: 0 0 10px 0;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 1;
            overflow: hidden;
          }

          .news-desc {
            color: #666;
            font-size: 14px;
            line-height: 1.6;
            margin: 0 0 15px 0;
            display: -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 2;
            overflow: hidden;
          }

          .news-meta {
            color: #999;
            font-size: 12px;
            display: flex;
            align-items: center;
            gap: 15px;

            .news-views {
              display: flex;
              align-items: center;
              gap: 4px;
            }

            .news-action {
              display: inline-flex;
              align-items: center;
              gap: 4px;
              cursor: pointer;
              transition: color 0.2s;

              &:hover,
              &.active {
                color: #409eff;
              }
            }
          }
        }

        .news-image {
          width: 200px;
          height: 120px;
          border-radius: 4px;
          overflow: hidden;

          .el-image {
            width: 100%;
            height: 100%;
          }
        }
      }
    }
  }
}
</style>
