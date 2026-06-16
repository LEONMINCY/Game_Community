<!-- 文件说明：views/forgotPassword/index.vue，邮箱验证码找回密码页面，完成验证码发送和密码重置。 -->
<template>
  <div class="forgot-page">
    <div class="forgot-card">
      <div class="forgot-header">
        <h1>找回密码</h1>
        <p>请输入账号和已绑定邮箱，验证通过后即可重置密码。</p>
      </div>

      <el-form label-position="top" class="forgot-form">
        <el-form-item label="账号">
          <el-input
            v-model="form.identifier"
            clearable
            placeholder="用户名 / 手机号 / 邮箱"
            @keyup.enter="resetPassword"
          />
        </el-form-item>
        <el-form-item label="绑定邮箱">
          <el-input
            v-model="form.email"
            clearable
            placeholder="请输入账号已绑定的邮箱"
            @keyup.enter="resetPassword"
          />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="code-row">
            <el-input
              v-model="form.code"
              maxlength="6"
              clearable
              placeholder="6位邮箱验证码"
              @keyup.enter="resetPassword"
            />
            <el-button :disabled="sending || countdown > 0" @click="sendCode">
              {{ countdown > 0 ? countdown + 's后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入6-32位新密码"
            @keyup.enter="resetPassword"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            @keyup.enter="resetPassword"
          />
        </el-form-item>

        <el-button type="primary" class="submit-button" :loading="resetting" @click="resetPassword">
          重置密码
        </el-button>
        <el-button class="back-button" @click="goLogin">返回登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { postRequest } from '@/utils/http'

const router = useRouter()
const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const form = reactive({
  identifier: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const sending = ref(false)
const resetting = ref(false)
const countdown = ref(0)
let timer = null

// 校验账号和邮箱，避免无效请求进入后端发送逻辑。
const validateAccountAndEmail = () => {
  if (!form.identifier.trim()) {
    ElMessage.warning('请输入用户名、手机号或邮箱')
    return false
  }
  if (!emailPattern.test(form.email.trim())) {
    ElMessage.warning('请输入合法的绑定邮箱')
    return false
  }
  return true
}

// 启动前端倒计时，配合后端发送频率限制减少重复点击。
const startCountdown = () => {
  countdown.value = 60
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(timer)
      timer = null
    }
  }, 1000)
}

// 请求后端发送邮箱验证码。
const sendCode = () => {
  if (!validateAccountAndEmail()) return
  sending.value = true
  postRequest('/noLogin/password-reset/send-code', {
    identifier: form.identifier.trim(),
    email: form.email.trim()
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('如果账号和邮箱匹配，验证码会发送到该邮箱')
      startCountdown()
    } else {
      ElMessage.error(res.msg || '验证码发送失败')
    }
  }).catch(() => {
    // 统一错误拦截器已经展示后端返回的具体原因，这里避免重复弹窗。
  }).finally(() => {
    sending.value = false
  })
}

// 校验验证码和新密码，成功后跳回登录页。
const resetPassword = () => {
  if (!validateAccountAndEmail()) return
  if (!form.code.trim()) {
    ElMessage.warning('请输入邮箱验证码')
    return
  }
  if (!form.newPassword || form.newPassword.length < 6 || form.newPassword.length > 32) {
    ElMessage.warning('新密码长度为6-32位')
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  resetting.value = true
  postRequest('/noLogin/password-reset/reset', {
    identifier: form.identifier.trim(),
    email: form.email.trim(),
    code: form.code.trim(),
    newPassword: form.newPassword,
    confirmPassword: form.confirmPassword
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('密码已重置，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(res.msg || '密码重置失败')
    }
  }).catch(() => {
    // 统一错误拦截器已经展示后端返回的具体原因，这里避免重复弹窗。
  }).finally(() => {
    resetting.value = false
  })
}

const goLogin = () => {
  router.push('/login')
}

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<style scoped lang="less">
.forgot-page {
  width: 100vw;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f6f8fb 0%, #edf3ff 100%);
  padding: 24px;
  box-sizing: border-box;
}

.forgot-card {
  width: 420px;
  padding: 34px 38px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 18px 48px rgba(16, 35, 68, 0.12);
}

.forgot-header {
  margin-bottom: 24px;

  h1 {
    margin: 0 0 10px;
    font-size: 26px;
    color: #10233f;
  }

  p {
    margin: 0;
    line-height: 1.7;
    color: #6f7f95;
  }
}

.forgot-form {
  :deep(.el-form-item__label) {
    font-weight: 600;
    color: #26364d;
  }
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 128px;
  gap: 10px;
}

.submit-button,
.back-button {
  width: 100%;
  margin-left: 0;
}

.back-button {
  margin-top: 12px;
}
</style>
