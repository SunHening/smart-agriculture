<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-input v-model="searchUsername" placeholder="搜索用户名" style="width: 160px; margin-right: 10px;" clearable @clear="loadData" />
            <el-select v-model="searchRole" placeholder="角色" style="width: 120px; margin-right: 10px;" clearable @change="loadData">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账号状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row.id)" :disabled="row.role === 'ADMIN'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserPage, updateUserStatus, deleteUser } from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchUsername = ref('')
const searchRole = ref('')

const loadData = async () => {
  const res = await getUserPage({ pageNum: pageNum.value, pageSize: pageSize.value, username: searchUsername.value, role: searchRole.value })
  tableData.value = res.data.records
  total.value = res.data.total
}

const handleStatusChange = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await updateUserStatus(row.id, newStatus)
  ElMessage.success('状态更新成功')
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该用户？', '提示', { type: 'warning' })
  await deleteUser(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
