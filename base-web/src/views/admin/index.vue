<!-- 文件说明：views/admin/index.vue，后台布局页面，负责侧边栏菜单、顶部账号信息和子页面出口。 -->
<template>
    <div class="mainPage">
        <div class="menu">
            <div class="logo">
                <img src="../../assets/imgs/logo.png"
               alt=""
               style="width: 60px;height: 60px">
                <span>游戏社区</span>
            </div>
            <div class="menuList">
                <el-menu
                    active-text-color="#45a1ff"
                    @select="handleSelect"
                    :default-active="defaultActive"
                >
                    <el-menu-item
                        v-for="item in visibleMenus"
                        :key="item.index"
                        :index="item.index"
                    >
                        <el-icon><component :is="item.icon" /></el-icon>
                        <span class="menu-title">
                            <span>{{ item.title }}</span>
                            <span
                                v-if="item.index === '/admin/report' && reportPendingCount > 0"
                                class="menu-red-badge"
                            >
                                {{ reportPendingText }}
                            </span>
                        </span>
                    </el-menu-item>
                </el-menu>
            </div>
        </div>
        <div class="content">
            <div class="header">

                <div style="display: flex; align-items: center">
                    <el-dropdown>
                        <el-avatar style="cursor: pointer" :size="30" :src="userInfo.avatar" />
                        <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item @click="openProfileDialog">修改资料</el-dropdown-item>
                            <el-dropdown-item divided @click="openPasswordDialog">修改密码</el-dropdown-item>
                            <el-dropdown-item divided @click="signOut">退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                        </template>
                    </el-dropdown>

                    <span style="margin-left: 8px;font-size: 14px">{{ userInfo.nickname || userInfo.username }}</span>
                </div>
            </div>
            <div class="view">
                <router-view v-slot="{ Component, route }">
                    <KeepAlive :max="6">
                        <component :is="Component" :key="route.path" />
                    </KeepAlive>
                </router-view>
            </div>
        </div>

        <el-dialog v-model="profileDialogVisible" title="修改资料" width="520px" :close-on-click-modal="false">
            <div class="profile-dialog">
                <el-upload
                    class="admin-avatar-uploader"
                    action="/api/file/upload"
                    :headers="headers"
                    :show-file-list="false"
                    accept="image/*"
                    :on-success="handleAvatarSuccess"
                >
                    <el-avatar :size="76" :src="profileAvatarPreview" />
                    <span>更换头像</span>
                </el-upload>
                <el-form label-width="86px">
                    <el-form-item label="昵称">
                        <el-input v-model="profileForm.nickname" maxlength="16" placeholder="请输入昵称" />
                    </el-form-item>
                    <el-form-item label="手机号">
                        <el-input v-model="profileForm.phone" maxlength="20" placeholder="请输入手机号" />
                    </el-form-item>
                    <el-form-item label="个性签名">
                        <el-input
                            v-model="profileForm.signature"
                            type="textarea"
                            :rows="3"
                            maxlength="120"
                            show-word-limit
                            placeholder="写一句个性签名"
                        />
                    </el-form-item>
                </el-form>
            </div>
            <template #footer>
                <el-button @click="profileDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="saveProfile">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="passwordDialogVisible" title="修改密码" width="480px" :close-on-click-modal="false">
            <el-form label-width="86px">
                <el-form-item label="原密码">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>
                <el-form-item label="新密码">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
                <el-form-item label="确认密码">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="passwordDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="savePassword">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { useRoute, useRouter } from "vue-router";
import { computed, reactive, ref, onMounted, onUnmounted, watch } from "vue";
import { ElMessage } from 'element-plus';
import { getRequest, postRequest, putRequest } from '../../utils/http';
import { useUserStore } from '@/store';
import { replaceURL } from '@/utils/tools';
const router = useRouter();
const route = useRoute();
const userStore = useUserStore()
const userInfo = ref({
    username: '',
    avatar: '',
    rawAvatar: '',
    nickname: '',
    phone: '',
    signature: '',
    roleId: null,
    permissions: [],
});
const headers = ref({
    Authorization: localStorage.getItem('token')
})
const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const profileForm = reactive({
    nickname: '',
    phone: '',
    signature: '',
    avatar: ''
})
const mobilePattern = /^1[3-9]\d{9}$/
const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})
const profileAvatarPreview = computed(() => profileForm.avatar ? replaceURL(profileForm.avatar) : userInfo.value.avatar)
const reportPendingCount = ref(0)
let reportPendingTimer = null
const reportPendingText = computed(() => reportPendingCount.value > 99 ? '99+' : reportPendingCount.value)
const moderatorMenuIndexes = ['/admin/index', '/admin/adminNews', '/admin/post', '/admin/report', '/admin/order', '/admin/message']
const menuItems = [
    { index: '/admin/index', icon: 'Histogram', title: '首页' },
    { index: '/admin/game', icon: 'Platform', title: '游戏管理' },
    { index: '/admin/order', icon: 'Document', title: '订单管理' },
    { index: '/admin/adminNews', icon: 'TrendCharts', title: '新闻管理' },
    { index: '/admin/post', icon: 'Promotion', title: '帖子管理' },
    { index: '/admin/report', icon: 'Warning', title: '举报管理' },
    { index: '/admin/message', icon: 'Message', title: '站内私信' },
    { index: '/admin/user', icon: 'UserFilled', title: '用户管理' }
    , { index: '/admin/log', icon: 'Memo', title: '日志管理' }
]
const visibleMenus = computed(() => {
    const permissions = userInfo.value.permissions || []
    const isAdmin = Number(userInfo.value.roleId) === 1
        || permissions.includes('admin')
        || permissions.includes('管理员')
    const isModerator = Number(userInfo.value.roleId) === 3
        || permissions.includes('moderator')
        || permissions.includes('社区审核员')
    if (isAdmin) {
        return menuItems
    }
    if (isModerator) {
        return menuItems.filter(item => moderatorMenuIndexes.includes(item.index))
    }
    return []
})
const defaultActive = computed(() => route.path);

const handleSelect = (index) => {
    if (!visibleMenus.value.some(item => item.index === index)) {
        return
    }
    router.push(index);
};

onMounted(() => {
    getUserInfo()
    loadReportPendingCount()
    reportPendingTimer = setInterval(loadReportPendingCount, 5000)
})

onUnmounted(() => {
    if (reportPendingTimer) {
        clearInterval(reportPendingTimer)
        reportPendingTimer = null
    }
})

/**
 * 后台举报提醒数量包含新举报和待审核申诉，用轮询保证侧边栏红点及时更新。
 */
const loadReportPendingCount = () => {
    getRequest('/report/pending-count', { silentError: true }).then(res => {
        if (res.code === 200) {
            reportPendingCount.value = Number(res.data || 0)
        }
    }).catch(() => {})
}
const getUserInfo = () => {
  getRequest('/user-info/get-real').then(res => {
    if (res.code === 200) {
      userStore.setUserInfo(res.data)
      userInfo.value.username = res.data.username || ''
      userInfo.value.avatar = replaceURL(res.data.avatar)
      userInfo.value.rawAvatar = res.data.avatar || ''
      userInfo.value.nickname = res.data.nickname || ''
      userInfo.value.phone = res.data.phone || ''
      userInfo.value.signature = res.data.signature || ''
      userInfo.value.roleId = res.data.roleId
      userInfo.value.permissions = res.data.permissions || []
      guardRoute()
    }
  })
}

const guardRoute = () => {
    const allowed = visibleMenus.value.some(item => item.index === route.path)
    if (!visibleMenus.value.length) {
        ElMessage.warning('当前账号没有后台权限')
        router.replace('/')
        return
    }
    if (!allowed) {
        router.replace(visibleMenus.value[0].index)
    }
}

watch(() => route.path, () => {
    if (userInfo.value.roleId) {
        guardRoute()
    }
    if (route.path === '/admin/report') {
        loadReportPendingCount()
    }
})


const signOut = () => {
    localStorage.removeItem('token')
    router.push('/login')
}

const openProfileDialog = () => {
    profileForm.nickname = userInfo.value.nickname || userInfo.value.username || ''
    profileForm.phone = userInfo.value.phone || ''
    profileForm.signature = userInfo.value.signature || ''
    profileForm.avatar = userInfo.value.rawAvatar || ''
    profileDialogVisible.value = true
}

const handleAvatarSuccess = (res) => {
    if (res.code === 200) {
        profileForm.avatar = res.data
    } else {
        ElMessage.error(res.msg || '头像上传失败')
    }
}

const saveProfile = () => {
    if (!profileForm.nickname.trim()) {
        ElMessage.warning('昵称不能为空')
        return
    }
    const phone = String(profileForm.phone || '').trim()
    if (phone && !mobilePattern.test(phone)) {
        ElMessage.warning('请输入合法的11位手机号')
        return
    }
    putRequest('/user-info/update', {
        nickname: profileForm.nickname.trim(),
        phone,
        signature: profileForm.signature,
        avatar: profileForm.avatar
    }).then(res => {
        if (res.code === 200) {
            ElMessage.success('资料已更新')
            profileDialogVisible.value = false
            getUserInfo()
        }
    })
}

const openPasswordDialog = () => {
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
}

const savePassword = () => {
    if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
        ElMessage.warning('请填写完整的密码信息')
        return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致')
        return
    }
    if (passwordForm.newPassword.length < 6) {
        ElMessage.warning('新密码长度不能小于6位')
        return
    }
    postRequest('/update-password', {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
    }).then(res => {
        if (res.code === 200) {
            ElMessage.success('密码修改成功')
            passwordDialogVisible.value = false
        }
    })
}

</script>

<style lang="less" scoped>
.mainPage {
    width: 100vw;
    height: 100vh;
    display: flex;
    box-sizing: border-box;

    .menu {
        width: 240px;
        flex: 0 0 240px;
        height: 100%;
        box-shadow: 2px 0 6px rgba(0, 21, 41, .35);
        z-index: 100;
        // background-color: #545c64;
        .logo {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 60px;

        }
        .menuList {
            width: 100%;
            height: calc(100% - 60px);

        }
    }
    .content {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        .header {
            width: 100%;
            height: 59px;
            background-color: #fff;
            box-shadow: 0 2px 4px rgba(0, 21, 41, .35);
            border-bottom: 1px solid #bdbdbd;
            z-index: 100;
            display: flex;
            align-items: center;
            justify-content: end;
            padding-right: 20px;
            box-sizing: border-box;
        }

        .view {
            width: 100%;
            min-width: 0;
            height: calc(100% - 60px);
            padding: 20px;
            box-sizing: border-box;
            background-color: #fff;
            overflow: auto;
        }
    }
}

.menu-title {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.menu-red-badge {
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: 9px;
    background: #f56c6c;
    color: #fff;
    font-size: 12px;
    line-height: 18px;
    text-align: center;
    font-weight: 600;
}

.profile-dialog {
    display: grid;
    grid-template-columns: 110px 1fr;
    gap: 20px;
    align-items: start;
}

.admin-avatar-uploader {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    color: #409eff;
    cursor: pointer;

    span {
        font-size: 13px;
    }
}
</style>
