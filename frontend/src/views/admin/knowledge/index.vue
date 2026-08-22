<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>知识库 / 常见问题</span>
          <div>
            <el-input v-model="keyword" placeholder="搜索问题/答案/分类" style="width: 220px; margin-right: 10px;" clearable @keyup.enter="loadData" @clear="loadData" />
            <el-button type="primary" @click="loadData">搜索</el-button>
            <el-button type="success" @click="openEdit()">新增条目</el-button>
          </div>
        </div>
      </template>
      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.category }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="answer" label="回答" min-width="300" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑条目' : '新增条目'" width="600px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width:100%" allow-create filterable>
            <el-option label="面积检测" value="面积检测" />
            <el-option label="病虫害" value="病虫害" />
            <el-option label="遥感影像" value="遥感影像" />
            <el-option label="无人机" value="无人机" />
            <el-option label="通用" value="通用" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题"><el-input v-model="form.question" /></el-form-item>
        <el-form-item label="回答"><el-input v-model="form.answer" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKnowledgeList, searchKnowledge, addKnowledge, updateKnowledge, deleteKnowledge } from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const form = ref({})

const loadData = async () => {
  loading.value = true
  let res
  if (keyword.value && keyword.value.trim()) {
    res = await searchKnowledge(keyword.value.trim())
    tableData.value = res.data || []
  } else {
    res = await getKnowledgeList()
    tableData.value = res.data || []
  }
  loading.value = false
}

const openEdit = (row) => {
  form.value = row ? { ...row } : { category: '通用', status: 1 }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (form.value.id) {
    await updateKnowledge(form.value)
  } else {
    await addKnowledge(form.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该条目？', '提示', { type: 'warning' })
  await deleteKnowledge(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
