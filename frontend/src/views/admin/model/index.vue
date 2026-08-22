<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 检测模型配置 -->
      <el-tab-pane label="检测模型" name="model">
        <div class="tab-header">
          <span class="tab-title">检测模型 / 参数配置</span>
          <el-button type="primary" @click="openModel()">新增模型</el-button>
        </div>
        <el-table :data="modelList" stripe v-loading="modelLoading">
          <el-table-column prop="configName" label="配置名称" min-width="140" />
          <el-table-column prop="modelType" label="模型" width="100">
            <template #default="{ row }"><el-tag>{{ row.modelType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="device" label="设备" width="80" />
          <el-table-column prop="gsdDefault" label="默认GSD" width="90" />
          <el-table-column prop="minAreaThreshold" label="最小面积阈值" width="110" />
          <el-table-column prop="confidenceThreshold" label="置信度阈值" width="110" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生效" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.isActive === 1" type="warning" size="small">生效中</el-tag>
              <span v-else style="color:#ccc">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openModel(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delModel(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 数据集 -->
      <el-tab-pane label="数据集" name="dataset">
        <div class="tab-header">
          <span class="tab-title">遥感 / 检测数据集</span>
          <el-button type="primary" @click="openDataset()">新增数据集</el-button>
        </div>
        <el-table :data="datasetList" stripe v-loading="datasetLoading">
          <el-table-column prop="datasetName" label="数据集名称" min-width="140" />
          <el-table-column prop="datasetType" label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="row.datasetType === 'REMOTE_SENSING' ? 'primary' : 'success'" size="small">
                {{ row.datasetType === 'REMOTE_SENSING' ? '遥感影像' : '无人机航拍' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="imageCount" label="影像数量" width="100" />
          <el-table-column prop="storagePath" label="存储路径" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openDataset(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delDataset(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 模型弹窗 -->
    <el-dialog v-model="modelVisible" :title="modelForm.id ? '编辑模型' : '新增模型'" width="560px">
      <el-form :model="modelForm" label-width="120px">
        <el-form-item label="配置名称"><el-input v-model="modelForm.configName" placeholder="如：SAM vit_b CPU" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="模型">
              <el-select v-model="modelForm.modelType" style="width:100%">
                <el-option label="vit_b（375MB）" value="vit_b" />
                <el-option label="vit_l（1.2GB）" value="vit_l" />
                <el-option label="vit_h（2.5GB）" value="vit_h" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备">
              <el-select v-model="modelForm.device" style="width:100%">
                <el-option label="CPU" value="cpu" />
                <el-option label="CUDA (GPU)" value="cuda" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="权重路径"><el-input v-model="modelForm.checkpointPath" placeholder="留空则自动下载" /></el-form-item>
        <el-form-item label="模型参数(JSON)"><el-input v-model="modelForm.modelParams" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="默认GSD"><el-input-number v-model="modelForm.gsdDefault" :precision="2" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最小面积"><el-input-number v-model="modelForm.minAreaThreshold" :min="0" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置信度"><el-input-number v-model="modelForm.confidenceThreshold" :precision="2" :min="0" :max="1" :step="0.05" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="modelForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为生效">
              <el-switch v-model="modelForm.isActive" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="modelVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <!-- 数据集弹窗 -->
    <el-dialog v-model="datasetVisible" :title="datasetForm.id ? '编辑数据集' : '新增数据集'" width="520px">
      <el-form :model="datasetForm" label-width="90px">
        <el-form-item label="数据集名称"><el-input v-model="datasetForm.datasetName" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="datasetForm.datasetType">
            <el-radio label="REMOTE_SENSING">遥感影像</el-radio>
            <el-radio label="DRONE">无人机航拍</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="datasetForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="影像数量"><el-input-number v-model="datasetForm.imageCount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="存储路径"><el-input v-model="datasetForm.storagePath" placeholder="/data/datasets/..." /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="datasetForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="datasetVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDataset">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getModelConfigList, addModelConfig, updateModelConfig, deleteModelConfig,
  getDatasetList, addDataset, updateDataset, deleteDataset
} from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('model')

const modelList = ref([])
const modelLoading = ref(false)
const modelVisible = ref(false)
const modelForm = ref({})

const datasetList = ref([])
const datasetLoading = ref(false)
const datasetVisible = ref(false)
const datasetForm = ref({})

const loadModels = async () => {
  modelLoading.value = true
  const res = await getModelConfigList()
  modelList.value = res.data || []
  modelLoading.value = false
}

const loadDatasets = async () => {
  datasetLoading.value = true
  const res = await getDatasetList()
  datasetList.value = res.data || []
  datasetLoading.value = false
}

const openModel = (row) => {
  modelForm.value = row ? { ...row } : { modelType: 'vit_b', device: 'cpu', status: 1, isActive: 0, gsdDefault: 1.2, minAreaThreshold: 200, confidenceThreshold: 0.8 }
  modelVisible.value = true
}

const saveModel = async () => {
  if (modelForm.value.id) {
    await updateModelConfig(modelForm.value)
  } else {
    await addModelConfig(modelForm.value)
  }
  ElMessage.success('保存成功')
  modelVisible.value = false
  loadModels()
}

const delModel = async (id) => {
  await ElMessageBox.confirm('确定删除该模型配置？', '提示', { type: 'warning' })
  await deleteModelConfig(id)
  ElMessage.success('删除成功')
  loadModels()
}

const openDataset = (row) => {
  datasetForm.value = row ? { ...row } : { datasetType: 'REMOTE_SENSING', status: 1, imageCount: 0 }
  datasetVisible.value = true
}

const saveDataset = async () => {
  if (datasetForm.value.id) {
    await updateDataset(datasetForm.value)
  } else {
    await addDataset(datasetForm.value)
  }
  ElMessage.success('保存成功')
  datasetVisible.value = false
  loadDatasets()
}

const delDataset = async (id) => {
  await ElMessageBox.confirm('确定删除该数据集？', '提示', { type: 'warning' })
  await deleteDataset(id)
  ElMessage.success('删除成功')
  loadDatasets()
}

onMounted(() => {
  loadModels()
  loadDatasets()
})
</script>

<style scoped>
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.tab-title { font-weight: 600; }
</style>
