<template>
  <div>
    <div class="page-header">
      <h3>我的农田</h3>
      <el-button type="primary" @click="handleAdd">新增农田</el-button>
    </div>

    <div v-if="tableData.length === 0">
      <el-empty description="暂无农田数据，请先添加" />
    </div>

    <div v-else class="field-grid">
      <el-card v-for="field in tableData" :key="field.id" class="field-card" shadow="hover">
        <template #header>
          <div class="field-card-header">
            <span class="field-name">{{ field.fieldName }}</span>
            <el-tag :type="field.status === 1 ? 'success' : 'info'" size="small">{{ field.status === 1 ? '正常' : '闲置' }}</el-tag>
          </div>
        </template>
        <div class="field-info">
          <div class="info-row" v-if="field.detectedArea">
            <span class="info-label">检测面积</span>
            <span class="info-value detected">{{ field.detectedArea }} 亩</span>
          </div>
          <div class="info-row">
            <span class="info-label">位置</span>
            <span class="info-value">{{ field.location }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">土壤</span>
            <span class="info-value">{{ field.soilType }}</span>
          </div>
          <div class="info-row" v-if="field.longitude">
            <span class="info-label">坐标</span>
            <span class="info-value">{{ field.longitude?.toFixed(4) }}, {{ field.latitude?.toFixed(4) }}</span>
          </div>
        </div>
        <div class="field-actions">
          <el-button size="small" @click="handleEdit(field)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(field.id)">删除</el-button>
        </div>
      </el-card>
    </div>

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑农田' : '新增农田'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="农田名称"><el-input v-model="form.fieldName" placeholder="如：东区1号田" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" placeholder="如：村东200米" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="土壤类型">
          <el-select v-model="form.soilType" placeholder="请选择" style="width:100%">
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
import { getFieldPage, addField, updateField, deleteField } from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({})

const loadData = async () => {
  const res = await getFieldPage({ pageNum: 1, pageSize: 50 })
  tableData.value = res.data.records
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h3 { margin: 0; }
.field-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1400px) { .field-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 1100px) { .field-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 700px) { .field-grid { grid-template-columns: 1fr; } }
.field-card { transition: transform 0.2s; }
.field-card:hover { transform: translateY(-2px); }
.field-card-header { display: flex; justify-content: space-between; align-items: center; }
.field-name { font-weight: 600; }
.field-info { margin-bottom: 12px; }
.info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.info-label { color: #999; }
.info-value { color: #333; }
.info-value.detected { color: #67c23a; font-weight: 600; }
.field-actions { display: flex; gap: 8px; justify-content: flex-end; }
</style>
