<!-- 文件说明：views/login/index.vue，登录注册页面，处理账号注册、用户名或手机号登录和登录态写入。 -->
<template>
  <div class="login">
    <div class="vido-box">
      <video src="../../assets/login.mp4"
             autoplay
             loop
             muted
             poster="https://cdn.max-c.com/heybox/dailynews/img/7a4d601b5f9797705f12a895c9a8e2d0.jpg"></video>
    </div>
    <div class="login-box">
      <div class="title">游戏社区</div>
      <el-form v-if="isLogin"
               style="width: 300px;">
        <el-form-item>
          <el-input v-model="username"
                    placeholder="请输入用户名或手机号"></el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password"
                    placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary"
                     style="width: 100%;"
                     @click="login">登 录</el-button>
        </el-form-item>
        <el-form-item>
          <el-button link
                     type="primary"
                     style="width: 100%;"
                     @click="goForgotPassword">忘记密码？</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="default"
                     style="width: 100%;"
                     @click="goRegister">去 注 册</el-button>
        </el-form-item>
      </el-form>
      <el-form v-else
               style="width: 300px;">
        <el-form-item>
          <el-input v-model="username"
                    placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="phone"
                    maxlength="11"
                    placeholder="请输入手机号"></el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="email"
                    placeholder="邮箱（选填，用于找回密码）"></el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="gender"
                     placeholder="请选择性别"
                     style="width: 100%;">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
            <el-option label="不方便透露" value="不方便透露" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input-number v-model="age"
                           :min="1"
                           :max="120"
                           placeholder="年龄（选填）"
                           style="width: 100%;" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password"
                    placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="password2"
                    placeholder="请再次输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary"
                     style="width: 100%;"
                     @click="register">注 册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="default"
                     style="width: 100%;"
                     @click="goLogin">去 登 录</el-button>
        </el-form-item>
      </el-form>

    </div>
    <el-dialog
      v-model="banDialogVisible"
      title="账号处罚申诉"
      width="520px"
      append-to-body
    >
      <div class="ban-dialog">
        <p class="ban-summary">
          当前账号暂时无法登录。你可以先查看处罚原因，如果认为处理有误，可在这里提交申诉，后台举报管理会同步更新申诉进度。
        </p>
        <div class="ban-info-row">
          <span>处理状态</span>
          <strong>{{ reportStatusText(banDetail.status) }}</strong>
        </div>
        <div class="ban-info-row">
          <span>举报原因</span>
          <strong>{{ banDetail.reason || '-' }}</strong>
        </div>
        <div class="ban-info-row">
          <span>处理回复</span>
          <strong>{{ banDetail.reply || banFallbackMessage || '-' }}</strong>
        </div>
        <div v-if="banDetail.banEndTime || banDetail.muteEndTime" class="ban-info-row">
          <span>截止时间</span>
          <strong>{{ banDetail.banEndTime || banDetail.muteEndTime }}</strong>
        </div>
        <el-input
          v-model="banAppealContent"
          type="textarea"
          :rows="4"
          maxlength="300"
          show-word-limit
          placeholder="请说明申诉理由，后台审核员或管理员会在举报管理中处理"
        />
      </div>
      <template #footer>
        <el-button @click="banDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="banAppealSubmitting" @click="submitBanAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { postRequest } from '../../utils/http.js'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()
const route = useRoute()
const isLogin = ref(true)
const username = ref('')
const password = ref('')
const password2 = ref('')
const phone = ref('')
const email = ref('')
const gender = ref('')
const age = ref(null)
const mobilePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const banDialogVisible = ref(false)
const banDetail = ref({})
const banAppealContent = ref('')
const banAppealSubmitting = ref(false)
const banFallbackMessage = ref('')

const goRegister = () => {
  isLogin.value = false
}
const goLogin = () => { 
  isLogin.value = true
}
const goForgotPassword = () => {
  router.push('/forgot-password')
}

const isForbiddenLoginMessage = (message = '') => {
  return /禁止登录|封禁|被封禁|禁用|账号.*禁/.test(String(message))
}

const reportStatusText = (status = '') => {
  const value = String(status || '')
  if (value === 'BANNED' || value.startsWith('BAN')) return '已封禁'
  if (value.startsWith('MUTE')) return '已禁言'
  if (value === 'REJECTED') return '已拒绝'
  if (value === 'RESOLVED') return '已处理'
  return value || '处罚中'
}

const showBanDialog = (message) => {
  banFallbackMessage.value = message || ''
  banDetail.value = {}
  banAppealContent.value = ''
  postRequest('/noLogin/ban-info', {
    username: username.value.trim()
  }, { silentError: true }).then(res => {
    if (res.code === 200 && res.data) {
      banDetail.value = res.data
    }
  }).catch(() => {
    banDetail.value = {}
  }).finally(() => {
    banDialogVisible.value = true
  })
}

const submitBanAppeal = () => {
  const content = banAppealContent.value.trim()
  if (!content) {
    ElMessage.warning('请填写申诉理由')
    return
  }
  banAppealSubmitting.value = true
  postRequest('/noLogin/ban-appeal', {
    reportedKeyword: username.value.trim(),
    appealContent: content
  }, { silentError: true }).then(res => {
    if (res.code === 200) {
      ElMessage.success('申诉已提交，请等待后台处理')
      banDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '申诉提交失败')
    }
  }).finally(() => {
    banAppealSubmitting.value = false
  })
}

const login = () => {
  if (username.value === '' || password.value === '') { 
    ElMessage.error('请输入用户名/手机号和密码')
    return
  }
  postRequest('/noLogin/login', {
    username: username.value,
    password: password.value,
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('登录成功')
      localStorage.setItem('token', res.data.token)
      const roleId = Number(res.data.roleId)
      const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
      const isBackOfficeRole = roleId === 1 || roleId === 3
      if (isBackOfficeRole) {
        router.push(redirect && redirect.startsWith('/admin') ? redirect : '/admin/index')
      } else if (redirect && redirect !== '/login' && !redirect.startsWith('/admin')) {
        router.push(redirect)
      } else {
        router.push('/')
      }
    } else {
      if (isForbiddenLoginMessage(res.msg)) {
        showBanDialog(res.msg)
        return
      }
      ElMessage.error(res.msg)
    }
  }).catch(err => {
    const message = err?.msg || err?.message || ''
    if (isForbiddenLoginMessage(message)) {
      showBanDialog(message)
      return
    }
  })
}
const register = () => {
  if (!username.value.trim()) {
    ElMessage.error('请输入用户名')
    return
  }
  if (!mobilePattern.test(phone.value.trim())) {
    ElMessage.error('请输入合法的11位手机号')
    return
  }
  if (!gender.value) {
    ElMessage.error('请选择性别')
    return
  }
  if (email.value.trim() && !emailPattern.test(email.value.trim())) {
    ElMessage.error('请输入合法的邮箱地址')
    return
  }
  if (password.value !== password2.value) {
    ElMessage.error('两次密码不一致')
    return
  }
  postRequest('/noLogin/register', {
    username: username.value.trim(),
    phone: phone.value.trim(),
    email: email.value.trim() || null,
    gender: gender.value,
    age: age.value || null,
    password: password.value,
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('注册成功')
      username.value = ''
      phone.value = ''
      email.value = ''
      gender.value = ''
      age.value = null
      password.value = ''
      password2.value = ''
      isLogin.value = true
    } else {
      ElMessage.error(res.msg)
    }
  })
}


</script>
<style lang="less" scoped>
.login {
  width: 100vw;
  height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;

  .vido-box {
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;

    video {
      object-fit: cover;
      width: 100%;
      height: 100%;
    }

  }

  .login-box {
    width: 19rem;
    min-height: 20rem;
    height: auto;
    background-color: rgba(255, 255, 255, 0.3); /* 背景颜色和透明度 */
    backdrop-filter: blur(10px); /* 模糊效果，可调整模糊半径 */
    margin: 0 auto;
    transform: translateY(40px);
    z-index: 999;
    border-radius: 1rem;
    padding: 2rem 5rem;
    // box-sizing: border-box;

    .title {
      width: 100%;
      text-align: center;
      font-size: 22px;
      font-weight: 600;
      padding: 0.5rem 0 1.5rem 0;
    }
  }
}

.ban-dialog {
  color: #303133;

  .ban-summary {
    margin: 0 0 16px;
    color: #606266;
    line-height: 1.7;
  }

  .ban-info-row {
    display: grid;
    grid-template-columns: 86px 1fr;
    gap: 12px;
    padding: 9px 0;
    border-bottom: 1px solid #f0f2f5;

    span {
      color: #909399;
    }

    strong {
      color: #303133;
      font-weight: 500;
      word-break: break-word;
    }
  }

  :deep(.el-textarea) {
    margin-top: 16px;
  }
}
</style>
