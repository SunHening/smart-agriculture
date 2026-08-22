<template>
  <div>
    <el-row :gutter="20">
      <!-- 上传影像 -->
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header><span>📤 上传影像 · 检测结果</span></template>
          <el-form label-width="90px">
            <el-form-item label="关联农田">
              <el-select v-model="form.fieldId" placeholder="请选择（可选）" clearable style="width:100%">
                <el-option v-for="f in fieldList" :key="f.id" :label="f.fieldName" :value="f.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="影像文件">
              <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange" :on-remove="() => file = null"
                accept=".png,.jpg,.jpeg,.tif,.tiff" drag>
                <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                <div class="el-upload__text">拖拽影像到此处或 <em>点击上传</em></div>
                <template #tip><div class="el-upload__tip">支持 PNG / JPG / TIFF 格式</div></template>
              </el-upload>
            </el-form-item>
            <el-form-item label="分辨率(m/px)">
              <el-input-number v-model="form.gsd" :min="0.001" :precision="3" :step="0.1" style="width:200px" />
              <div class="gsd-tip">地面采样距离，用于像素面积换算实际面积</div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpload" :disabled="!file" :loading="uploading">
                {{ uploading ? '检测中...' : '开始检测' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 检测结果 -->
      <el-col :xs="24" :md="14">
        <el-card>
          <template #header><span>📊 检测结果</span></template>
          <el-empty v-if="!lastResult" description="上传影像后自动提取地块边界并测算面积" />
          <template v-else>
            <div class="result-grid">
              <div class="result-item"><span class="rl">总面积</span><span class="rv primary">{{ lastResult.totalAreaMu }} 亩</span></div>
              <div class="result-item"><span class="rl">地块数</span><span class="rv">{{ lastResult.fieldCount }}</span></div>
              <div class="result-item"><span class="rl">置信度</span><span class="rv success">{{ lastResult.confidence }}</span></div>
              <div class="result-item"><span class="rl">耗时</span><span class="rv">{{ lastResult.processingTimeMs }}ms</span></div>
            </div>
            <div v-if="lastResult.overlayBase64" class="overlay-box">
              <div class="overlay-label">分割叠加图（绿色为识别地块）</div>
              <img :src="lastResult.overlayBase64" class="overlay-img" alt="分割叠加图" />
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFieldList, samGeoDetectImage } from '../../../api'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')

const fieldList = ref([])
const form = ref({ gsd: 0.5 })
const file = ref(null)
const uploading = ref(false)
const lastResult = ref(null)

const loadFields = async () => {
  try {
    const res = await getFieldList()
    fieldList.value = res.data || []
  } catch { /* ignore */ }
}

const handleFileChange = (uploadFile) => { file.value = uploadFile.raw }

const readFileAsBase64 = (raw) => new Promise((resolve, reject) => {
  const reader = new FileReader()
  reader.onload = () => resolve(reader.result)
  reader.onerror = reject
  reader.readAsDataURL(raw)
})

const handleUpload = async () => {
  if (!file.value) return
  uploading.value = true
  try {
    const imageBase64 = await readFileAsBase64(file.value)
    const res = await samGeoDetectImage({
      userId: user.id,
      fieldId: form.value.fieldId,
      imageBase64,
      gsd: form.value.gsd || 0.5,
      imageType: 'DRONE',
      originalFilename: file.value.name
    })
    const data = res.data
    const avgConf = data.boundaries?.length
      ? data.boundaries.reduce((s, b) => s + (b.confidence || 0), 0) / data.boundaries.length
      : 0

    lastResult.value = {
      totalAreaMu: data.totalAreaMu,
      fieldCount: data.fieldCount || data.boundaries?.length || 0,
      confidence: (avgConf * 100).toFixed(1) + '%',
      processingTimeMs: data.processingTimeMs,
      overlayBase64: data.overlayBase64
    }
    ElMessage.success('检测完成')
  } catch (e) {
    ElMessage.error('检测失败：' + (e.message || 'SAM-Geo 服务未启动'))
  } finally {
    uploading.value = false
  }
}

onMounted(() => loadFields())
</script>

<style scoped>
.gsd-tip { font-size: 12px; color: #999; line-height: 1.4; margin-top: 4px; }
.result-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.result-item { text-align: center; padding: 16px 8px; background: #f7f8fa; border-radius: 10px; }
.rl { font-size: 12px; color: #999; display: block; }
.rv { font-size: 22px; font-weight: 700; color: #1a1a1a; display: block; margin-top: 6px; }
.rv.primary { color: #43a047; }
.rv.success { color: #67c23a; }
.overlay-box { margin-top: 16px; }
.overlay-label { font-size: 13px; color: #666; margin-bottom: 8px; }
.overlay-img { width: 100%; max-height: 420px; object-fit: contain; background: #f5f7fa; border-radius: 8px; border: 1px solid #ebeef5; }
@media (max-width: 1100px) { .result-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
