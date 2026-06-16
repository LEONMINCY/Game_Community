<!-- 文件说明：views/privacySettings/index.vue，隐私设置页面，维护资料可见性、关注粉丝可见性和黑名单。 -->
<template>
  <div class="privacy-page">
    <div class="privacy-head">
      <h2>隐私设置</h2>
      <p>控制其他玩家在访问你主页时能看到哪些信息。</p>
    </div>

    <div class="privacy-list">
      <div class="privacy-item">
        <div>
          <h3>隐藏个人资料</h3>
          <p>其他玩家看不到你的性别、手机号、地址和年龄。</p>
        </div>
        <el-switch v-model="form.privacyProfile" />
      </div>
      <div class="privacy-item">
        <div>
          <h3>隐藏关注列表</h3>
          <p>其他玩家不能查看你关注了哪些用户。</p>
        </div>
        <el-switch v-model="form.privacyFollow" />
      </div>
      <div class="privacy-item">
        <div>
          <h3>隐藏粉丝列表</h3>
          <p>其他玩家不能查看你的粉丝列表。</p>
        </div>
        <el-switch v-model="form.privacyFans" />
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" @click="savePrivacy">保存设置</el-button>
    </div>

    <div class="blacklist-section">
      <div class="section-title">
        <h3>黑名单管理</h3>
        <p>被拉黑的玩家无法再给你发送私信，可以在这里随时取消拉黑。</p>
      </div>
      <el-empty v-if="blacklist.length === 0" description="暂无拉黑用户" />
      <div v-else class="blacklist-list">
        <div v-for="user in blacklist" :key="user.blockedUserId" class="blacklist-user">
          <div class="blacklist-user-main">
            <el-avatar :size="42" :src="avatarSrc(user.avatar)" />
            <div>
              <div class="blacklist-name">{{ user.nickname || ('用户' + user.blockedUserId) }}</div>
              <div class="blacklist-time">拉黑时间：{{ user.createTime || '-' }}</div>
            </div>
          </div>
          <el-button size="small" @click="unblockUser(user)">取消拉黑</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRequest, putRequest, deleteRequest } from '@/utils/http'
import { replaceURL } from '@/utils/tools'

const form = reactive({
  privacyProfile: false,
  privacyFollow: false,
  privacyFans: false
})

const blacklist = ref([])

onMounted(() => {
  loadPrivacy()
  loadBlacklist()
})

const loadPrivacy = () => {
  getRequest('/user-info/get-real').then(res => {
    if (res.code === 200) {
      form.privacyProfile = !!res.data.privacyProfile
      form.privacyFollow = !!res.data.privacyFollow
      form.privacyFans = !!res.data.privacyFans
    }
  })
}

const loadBlacklist = () => {
  getRequest('/blacklist/list').then(res => {
    if (res.code === 200) {
      blacklist.value = res.data || []
    }
  })
}

const avatarSrc = (avatar) => {
  return avatar ? replaceURL(avatar) : '/default-avatar.png'
}

const unblockUser = (user) => {
  ElMessageBox.confirm(
    `确定要取消拉黑 ${user.nickname || '该用户'} 吗？`,
    '取消拉黑',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteRequest('/blacklist/unblock/' + user.blockedUserId).then(res => {
      if (res.code === 200) {
        ElMessage.success('已取消拉黑')
        loadBlacklist()
      } else {
        ElMessage.error(res.msg || '取消拉黑失败')
      }
    })
  }).catch(() => {})
}

const savePrivacy = () => {
  putRequest('/user-info/update', {
    privacyProfile: form.privacyProfile,
    privacyFollow: form.privacyFollow,
    privacyFans: form.privacyFans
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('隐私设置已保存')
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  })
}
</script>

<style scoped lang="less">
.privacy-page {
  width: 100%;
  min-height: 100%;
  padding: 28px;
  box-sizing: border-box;
}

.privacy-head {
  margin-bottom: 20px;

  h2 {
    margin: 0 0 8px;
    font-size: 24px;
  }

  p {
    margin: 0;
    color: #7a8494;
  }
}

.privacy-list {
  display: grid;
  gap: 14px;
  max-width: 760px;
}

.privacy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;

  h3 {
    margin: 0 0 6px;
    font-size: 17px;
  }

  p {
    margin: 0;
    color: #7a8494;
    line-height: 1.5;
  }
}

.actions {
  margin-top: 22px;
}

.blacklist-section {
  max-width: 760px;
  margin-top: 28px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.section-title {
  margin-bottom: 12px;

  h3 {
    margin: 0 0 6px;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: #7a8494;
    line-height: 1.5;
  }
}

.blacklist-list {
  display: grid;
  gap: 10px;
}

.blacklist-user {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fc;
}

.blacklist-user-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.blacklist-name {
  font-weight: 600;
  color: #303133;
}

.blacklist-time {
  margin-top: 3px;
  font-size: 12px;
  color: #909399;
}
</style>
