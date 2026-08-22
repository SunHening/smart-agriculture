<template>
  <div>
    <el-tabs v-model="activeTab" type="border-card">
      <!-- AI 模型参数 -->
      <el-tab-pane label="AI 模型参数" name="ai">
        <div class="tab-header">
          <span class="tab-title">大模型接入 / 参数配置</span>
          <el-button type="primary" @click="openAi()">新增配置</el-button>
        </div>
        <el-table :data="aiList" stripe v-loading="aiLoading">
          <el-table-column prop="provider" label="提供商" width="110">
            <template #default="{ row }"><el-tag>{{ row.provider }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="baseUrl" label="接口地址" min-width="200" show-overflow-tooltip />
          <el-table-column prop="modelName" label="模型名" min-width="140" show-overflow-tooltip />
          <el-table-column prop="temperature" label="温度" width="80" />
          <el-table-column prop="maxTokens" label="最大Token" width="100" />
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
              <el-button size="small" @click="openAi(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delAi(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 提示词模板 -->
      <el-tab-pane label="提示词模板" name="prompt">
        <div class="tab-header">
          <span class="tab-title">AI 提示词模板</span>
          <el-button type="primary" @click="openPrompt()">新增模板</el-button>
        </div>
        <el-table :data="promptList" stripe v-loading="promptLoading">
          <el-table-column prop="templateName" label="模板名称" width="140" />
          <el-table-column prop="templateCode" label="模板编码" width="140" />
          <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" @click="openPrompt(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="delPrompt(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- AI 配置弹窗 -->
    <el-dialog v-model="aiVisible" :title="aiForm.id ? '编辑 AI 配置' : '新增 AI 配置'" width="600px">
      <el-form :model="aiForm" label-width="100px">
        <el-form-item label="提供商">
          <el-select v-model="aiForm.provider" style="width:100%">
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="OpenAI" value="openai" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址"><el-input v-model="aiForm.baseUrl" placeholder="https://api.deepseek.com/v1" /></el-form-item>
        <el-form-item label="API Key"><el-input v-model="aiForm.apiKey" type="password" show-password placeholder="sk-..." /></el-form-item>
        <el-form-item label="模型名"><el-input v-model="aiForm.modelName" placeholder="deepseek-chat" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="温度"><el-input-number v-model="aiForm.temperature" :precision="2" :min="0" :max="2" :step="0.1" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大Token"><el-input-number v-model="aiForm.maxTokens" :min="0" style="width:100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="系统提示词"><el-input v-model="aiForm.systemPrompt" type="textarea" :rows="3" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="aiForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设为生效">
              <el-switch v-model="aiForm.isActive" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="aiVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAi">保存</el-button>
      </template>
    </el-dialog>

    <!-- 提示词弹窗 -->
    <el-dialog v-model="promptVisible" :title="promptForm.id ? '编辑模板' : '新增模板'" width="560px">
      <el-form :model="promptForm" label-width="90px">
        <el-form-item label="模板名称"><el-input v-model="promptForm.templateName" /></el-form-item>
        <el-form-item label="模板编码"><el-input v-model="promptForm.templateCode" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="promptForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="promptForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="promptForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promptVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrompt">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  getAiConfigList, addAiConfig, updateAiConfig, deleteAiConfig,
  getPromptTemplateList, addPromptTemplate, updatePromptTemplate, deletePromptTemplate
} from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('ai')

const aiList = ref([])
const aiLoading = ref(false)
const aiVisible = ref(false)
const aiForm = ref({})

const promptList = ref([])
const promptLoading = ref(false)
const promptVisible = ref(false)
const promptForm = ref({})

const loadAi = async () => {
  aiLoading.value = true
  const res = await getAiConfigList()
  aiList.value = res.data || []
  aiLoading.value = false
}

const loadPrompts = async () => {
  promptLoading.value = true
  const res = await getPromptTemplateList()
  promptList.value = res.data || []
  promptLoading.value = false
}

const openAi = (row) => {
  aiForm.value = row ? { ...row } : { provider: 'deepseek', status: 1, isActive: 0, temperature: 0.7, maxTokens: 2000 }
  aiVisible.value = true
}

const saveAi = async () => {
  if (aiForm.value.id) {
    await updateAiConfig(aiForm.value)
  } else {
    await addAiConfig(aiForm.value)
  }
  ElMessage.success('保存成功')
  aiVisible.value = false
  loadAi()
}

const delAi = async (id) => {
  await ElMessageBox.confirm('确定删除该 AI 配置？', '提示', { type: 'warning' })
  await deleteAiConfig(id)
  ElMessage.success('删除成功')
  loadAi()
}

const openPrompt = (row) => {
  promptForm.value = row ? { ...row } : { status: 1 }
  promptVisible.value = true
}

const savePrompt = async () => {
  if (promptForm.value.id) {
    await updatePromptTemplate(promptForm.value)
  } else {
    await addPromptTemplate(promptForm.value)
  }
  ElMessage.success('保存成功')
  promptVisible.value = false
  loadPrompts()
}

const delPrompt = async (id) => {
  await ElMessageBox.confirm('确定删除该模板？', '提示', { type: 'warning' })
  await deletePromptTemplate(id)
  ElMessage.success('删除成功')
  loadPrompts()
}

onMounted(() => {
  loadAi()
  loadPrompts()
})
</script>

<style scoped>
.tab-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.tab-title { font-weight: 600; }
</style>
