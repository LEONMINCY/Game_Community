<!-- 文件说明：components/PostShareDialog.vue，帖子转发弹窗组件，支持转发给关注用户和复制帖子链接。 -->
<template>
  <el-dialog v-model="dialogVisible" title="转发帖子" width="520px">
    <div class="share-preview">
      <div class="share-title">{{ post?.title || '帖子' }}</div>
      <div class="share-link">{{ shareLink }}</div>
    </div>

    <div class="share-actions">
      <el-button type="primary" plain @click="copyLink">复制链接</el-button>
    </div>

    <el-divider>转发给关注的人</el-divider>

    <el-input
      v-model="keyword"
      placeholder="搜索昵称或用户名"
      clearable
      class="share-search"
    />

    <div class="following-list" v-loading="loading">
      <el-empty v-if="filteredFollowing.length === 0" description="暂无可转发的关注用户" />
      <el-checkbox-group v-else v-model="selectedUserIds">
        <div v-for="user in filteredFollowing" :key="user.userId" class="following-item">
          <el-checkbox :label="user.userId">
            <div class="following-info">
              <el-avatar :size="32" :src="normalizeAvatar(user.avatar)" />
              <span>{{ user.nickname || user.username }}</span>
            </div>
          </el-checkbox>
        </div>
      </el-checkbox-group>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :disabled="selectedUserIds.length === 0" @click="forwardPost">
        转发
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRequest, postRequest } from '@/utils/http'
import { replaceURL } from '@/utils/tools'
import { useUserStore } from '@/store/index.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  post: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'shared'])
const router = useRouter()
const userStore = useUserStore()
const followingList = ref([])
const selectedUserIds = ref([])
const keyword = ref('')
const loading = ref(false)

const dialogVisible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const shareLink = computed(() => {
  if (!props.post?.id) return ''
  const href = router.resolve({ path: '/forum', query: { id: props.post.id } }).href
  return `${window.location.origin}${href}`
})

const filteredFollowing = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return followingList.value
  return followingList.value.filter(user =>
    `${user.nickname || ''}${user.username || ''}`.toLowerCase().includes(value)
  )
})

watch(
  () => props.modelValue,
  visible => {
    if (visible) {
      selectedUserIds.value = []
      keyword.value = ''
      getFollowingList()
    }
  }
)

const normalizeAvatar = avatar => avatar ? replaceURL(avatar) : ''

const getFollowingList = () => {
  const userId = userStore.userInfo?.userId
  if (!userId) return
  loading.value = true
  getRequest(`/userRelation/following/${userId}`).then(res => {
    if (res.code === 200) {
      followingList.value = res.data || []
    }
  }).finally(() => {
    loading.value = false
  })
}

const recordShare = async () => {
  if (!props.post?.id) return
  const res = await postRequest(`/post/share/${props.post.id}`, {})
  if (res.code === 200) {
    emit('shared', res.data)
  }
}

const copyLink = async () => {
  if (!shareLink.value) return
  try {
    await navigator.clipboard.writeText(shareLink.value)
    ElMessage.success('链接已复制')
  } catch (e) {
    const textarea = document.createElement('textarea')
    textarea.value = shareLink.value
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('链接已复制')
  }
  recordShare()
}

const forwardPost = async () => {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要转发的用户')
    return
  }
  const content = JSON.stringify({
    type: 'post_share',
    postId: props.post?.id,
    title: props.post?.title || '帖子',
    gameName: props.post?.gameName || '',
    cover: Array.isArray(props.post?.images) && props.post.images.length > 0 ? props.post.images[0] : ''
  })
  const results = await Promise.all(selectedUserIds.value.map(receiverId =>
    postRequest('/message/add', {
      receiverId,
      content
    }).catch(() => ({ code: 500 }))
  ))
  const failed = results.filter(item => item.code !== 200).length
  if (failed > 0) {
    ElMessage.error(`有 ${failed} 个用户转发失败`)
    return
  }
  await recordShare()
  ElMessage.success('转发成功')
  dialogVisible.value = false
}
</script>

<style scoped>
.share-preview {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f7f8fa;
}

.share-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.share-link {
  color: #606266;
  font-size: 13px;
  word-break: break-all;
}

.share-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.share-search {
  margin-bottom: 12px;
}

.following-list {
  max-height: 280px;
  overflow-y: auto;
}

.following-item {
  padding: 8px 0;
}

.following-info {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}
</style>
