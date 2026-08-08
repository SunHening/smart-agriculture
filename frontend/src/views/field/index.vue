<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>农田管理</span>
          <div>
            <el-input v-model="searchName" placeholder="搜索农田名称" style="width: 200px; margin-right: 10px;" clearable @clear="loadData" />
            <el-button type="primary" @click="loadData">搜索</el-button>
            <el-button type="success" @click="handleAdd">新增农田</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe>
        <el-table-column prop="fieldName" label="农田名称" />
        <el-table-column label="检测面积(亩)" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.detectedArea ? '#67c23a' : '#ccc', fontWeight: row.detectedArea ? '600' : '400' }">{{ row.detectedArea || '未检测' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="soilType" label="土壤类型" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '闲置' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑农田' : '新增农田'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="农田名称"><el-input v-model="form.fieldName" placeholder="如：东区1号田" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" placeholder="如：村东200米" /></el-form-item>
        <el-form-item label="土壤类型">
          <el-select v-model="form.soilType" placeholder="请选择">
            <el-option label="黏土" value="黏土" />
            <el-option label="沙土" value="沙土" />
            <el-option label="壤土" value="壤土" />
            <el-option label="砂壤土" value="砂壤土" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">闲置</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" /></el-form-item>
        <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFieldPage, addField, updateField, deleteField } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})

const loadData = async () => {
  const res = await getFieldPage({ pageNum: pageNum.value, pageSize: pageSize.value, fieldName: searchName.value })
  tableData.value = res.data.records
  total.value = res.data.total
}

const handleAdd = () => {
  form.value = { status: 1 }
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  form.value = { ...row }
  isEdit.value = true
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (isEdit.value) {
    await updateField(form.value)
  } else {
    await addField(form.value)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该农田？', '提示', { type: 'warning' })
  await deleteField(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
