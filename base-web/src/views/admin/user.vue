<!-- 文件说明：views/admin/user.vue，后台用户管理页面，维护用户、审核员和管理员账号。 -->
<template>
  <div class="user-management">
    <el-form :model="queryParams" label-width="70px" :inline="true">
      <el-form-item label="昵称">
        <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="queryParams.roleId" placeholder="请选择角色" clearable style="width: 200px;">
          <el-option v-for="role in userRoles" :key="role.value" :label="role.label" :value="role.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd">新增账号</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="users" style="width: 100%">
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="nickname" label="昵称" min-width="130" />
      <el-table-column label="角色" width="170">
        <template #default="scope">
          <el-select v-model="scope.row.roleId" size="small" @change="handleRoleChange(scope.row)">
            <el-option v-for="role in userRoles" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column prop="avatar" label="头像" width="90">
        <template #default="scope">
          <img :src="scope.row.avatar" alt="头像" class="avatar">
        </template>
      </el-table-column>
      <el-table-column prop="userLevel" label="等级" width="90">
        <template #default="scope">
          <el-tag>Lv{{ scope.row.userLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLogin" label="最后登录时间" min-width="170" />
      <el-table-column label="启用" width="90">
        <template #default="scope">
          <el-switch v-model="scope.row.enable" @change="handleEnable(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="scope">
          <el-button type="primary" link @click="handleResetPassword(scope.row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNo"
        :page-size="queryParams.pageSize"
        :total="totalUsers"
        layout="total, prev, pager, next, jumper"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新增账号" width="460px">
      <el-form :model="form" label-width="90px" :rules="rules" ref="formRef">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="newPassword">
          <el-input v-model="form.newPassword" placeholder="默认 123456" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%;">
            <el-option v-for="role in addFormRoles" :key="role.value" :label="role.label" :value="role.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addUser">新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { postRequest, putRequest } from '../../utils/http'

const totalUsers = ref(0)
const userRoles = ref([
  { label: '管理员', value: 1 },
  { label: '普通用户', value: 2 },
  { label: '社区审核员', value: 3 }
])
const addFormRoles = ref(userRoles.value.filter(role => role.value !== 1))
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
  nickname: null,
  roleId: null
})
const form = ref({})
const formRef = ref(null)
const dialogVisible = ref(false)
const users = ref([])

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 16, message: '用户名长度为 4-16 位', trigger: 'blur' }
  ],
  newPassword: [
    { min: 6, max: 16, message: '密码长度为 6-16 位', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  roleId: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

onMounted(() => {
  getUserList()
})

const search = () => {
  queryParams.value.pageNo = 1
  getUserList()
}

const getUserList = () => {
  postRequest('/user/page', queryParams.value).then(res => {
    if (res.code === 200) {
      users.value = res.data.list || []
      totalUsers.value = res.data.total || 0
    }
  })
}

const resetQuery = () => {
  queryParams.value = {
    pageNo: 1,
    pageSize: 10,
    nickname: null,
    roleId: null
  }
  getUserList()
}

const handleEnable = (user) => {
  putRequest('/user/enable', {
    id: user.id,
    enableFlag: user.enable
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('操作成功')
      getUserList()
    }
  })
}

const handleRoleChange = (user) => {
  putRequest('/user/role', {
    id: user.id,
    roleId: user.roleId
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('角色已更新')
      getUserList()
    }
  })
}

const handleCurrentChange = (page) => {
  queryParams.value.pageNo = page
  getUserList()
}

const handleAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const addUser = () => {
  formRef.value.validate(valid => {
    if (!valid) return
    postRequest('/user/admin/add', form.value).then(res => {
      if (res.code === 200) {
        ElMessage.success('账号新增成功')
        dialogVisible.value = false
        getUserList()
      }
    })
  })
}

const handleResetPassword = (user) => {
  ElMessageBox.confirm('确定要将该用户密码重置为123456吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    postRequest(`/user/reset-password/${user.id}`).then(res => {
      if (res.code === 200) {
        ElMessage.success('密码重置成功')
      }
    })
  }).catch(() => {})
}

const resetForm = () => {
  form.value = {
    username: '',
    newPassword: '',
    nickname: '',
    phone: '',
    roleId: 2
  }
}
</script>

<style scoped>
.user-management {
  padding: 40px;
  width: 100%;
  box-sizing: border-box;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: end;
}
</style>
