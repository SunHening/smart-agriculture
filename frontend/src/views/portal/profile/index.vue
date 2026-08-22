<template>
  <div>
    <h3 style="margin-bottom:20px">个人中心</h3>

    <el-row :gutter="20">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span>基本信息</span></template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="用户名"><el-input v-model="form.username" disabled /></el-form-item>
            <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span>修改密码</span></template>
          <el-form :model="pwdForm" label-width="80px">
            <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleChangePwd">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserInfo, updateUser, updatePassword } from '../../../api'
import { ElMessage } from 'element-plus'

const form = ref({})
const pwdForm = ref({})

onMounted(async () => {
  try {
    const res = await getUserInfo()
    form.value = res.data
  } catch { /* ignore */ }
})

const handleSave = async () => {
  await updateUser(form.value)
  ElMessage.success('保存成功')
}

const handleChangePwd = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning('请填写完整密码信息')
    return
  }
  await updatePassword(pwdForm.value)
  ElMessage.success('密码修改成功')
  pwdForm.value = {}
}
</script>
