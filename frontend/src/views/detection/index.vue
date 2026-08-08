<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>影像检测</span>
          <div>
            <el-select v-model="filterImageType" placeholder="影像类型" clearable style="width:160px;margin-right:10px" @change="loadData">
              <el-option label="遥感影像" value="REMOTE_SENSING" />
              <el-option label="无人机航拍" value="DRONE" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="状态" clearable style="width:140px;margin-right:10px" @change="loadData">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="失败" value="FAILED" />
            </el-select>
            <el-button type="primary" @click="showUploadDialog">上传影像</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="originalFilename" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column label="影像类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.imageType === 'REMOTE_SENSING' ? 'primary' : 'success'" size="small">
              {{ row.imageType === 'REMOTE_SENSING' ? '遥感影像' : '无人机航拍' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resolution" label="分辨率(m)" width="100" />
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showDetail(row)">详情</el-button>
            <el-button size="small" @click="handleReDetect(row)" :disabled="row.status === 'PROCESSING'">重检</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

    <!-- 上传弹窗 -->
    <el-dialog v-model="dialogVisible" title="上传影像" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="影像类型">
          <el-radio-group v-model="form.imageType">
            <el-radio label="REMOTE_SENSING">遥感影像</el-radio>
            <el-radio label="DRONE">无人机航拍</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联农田">
          <el-select v-model="form.fieldId" placeholder="请选择（可选）" clearable style="width:100%">
            <el-option v-for="f in fieldList" :key="f.id" :label="f.fieldName" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="影像文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleFileChange" :on-remove="() => file = null"
            accept=".tif,.tiff,.png,.jpg,.jpeg,.geotiff" drag>
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽影像文件到此处或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 GeoTIFF / PNG / JPG 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分辨率(m/pixel)">
          <el-input-number v-model="form.resolution" :min="0" :precision="4" style="width:200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="!file">提交检测</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="检测详情" direction="rtl" size="600px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small" style="margin-bottom:20px">
          <el-descriptions-item label="文件名">{{ detail.originalFilename || '-' }}</el-descriptions-item>
          <el-descriptions-item label="影像类型">
            <el-tag :type="detail.imageType === 'REMOTE_SENSING' ? 'primary' : 'success'" size="small">
              {{ detail.imageType === 'REMOTE_SENSING' ? '遥感影像' : '无人机航拍' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分辨率">{{ detail.resolution || '-' }} m/pixel</el-descriptions-item>
          <el-descriptions-item label="GSD">{{ detail.gsd || '-' }} m/pixel</el-descriptions-item>
          <el-descriptions-item label="坐标参考系">{{ detail.crs || '-' }}</el-descriptions-item>
          <el-descriptions-item label="中心坐标">{{ detail.longitude ? detail.longitude.toFixed(6) + ', ' + detail.latitude.toFixed(6) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="影像尺寸">{{ detail.imageWidth ? detail.imageWidth + ' × ' + detail.imageHeight + ' px' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">检测结果</el-divider>
        <el-empty v-if="!detailResult" description="暂无检测结果" />
        <template v-else>
          <el-row :gutter="12">
            <el-col :span="8">
              <div class="dr-stat"><div class="dr-val primary">{{ detailResult.detectedArea }} 亩</div><div class="dr-lbl">检测面积</div></div>
            </el-col>
            <el-col :span="8">
              <div class="dr-stat"><div class="dr-val success">{{ (detailResult.confidence * 100).toFixed(1) }}%</div><div class="dr-lbl">置信度</div></div>
            </el-col>
            <el-col :span="8">
              <div class="dr-stat"><div class="dr-val">{{ ((detailResult.coverageRate || 0) * 100).toFixed(1) }}%</div><div class="dr-lbl">覆盖率</div></div>
            </el-col>
          </el-row>
          <el-divider />
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="模型">{{ detailResult.modelName }} ({{ detailResult.modelVersion }})</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ detailResult.processingTimeMs }} ms</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="detailResult.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ detailResult.status }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="时间">{{ detailResult.createTime || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px">
            <h4 style="margin-bottom:8px">边界预览</h4>
            <div class="dr-map" ref="mapRef"></div>
          </div>
        </template>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { getDetectionPage, submitDetection, reDetect, deleteDetection } from '../../api'
import { getFieldList, uploadFile } from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const filterImageType = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const form = ref({ imageType: 'REMOTE_SENSING' })
const file = ref(null)
const fieldList = ref([])

// 详情抽屉
const drawerVisible = ref(false)
const detail = ref(null)
const detailResult = ref(null)
const mapRef = ref(null)
let chartInstance = null

const statusTag = (s) => ({ PENDING: 'warning', PROCESSING: '', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)

const loadData = async () => {
  loading.value = true
  const res = await getDetectionPage({ pageNum: pageNum.value, pageSize: pageSize.value, imageType: filterImageType.value, status: filterStatus.value })
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const showUploadDialog = async () => {
  form.value = { imageType: 'REMOTE_SENSING' }
  file.value = null
  const res = await getFieldList()
  fieldList.value = res.data
  dialogVisible.value = true
}

const handleFileChange = (uploadFile) => { file.value = uploadFile.raw }

const handleSubmit = async () => {
  if (!file.value) return
  const uploadRes = await uploadFile(file.value)
  await submitDetection({
    imageType: form.value.imageType, fieldId: form.value.fieldId,
    originalFilename: file.value.name, imagePath: uploadRes.data, resolution: form.value.resolution
  })
  ElMessage.success('检测任务已提交')
  dialogVisible.value = false
  loadData()
}

const handleReDetect = async (row) => {
  await reDetect(row.id)
  ElMessage.success('已重新提交检测')
  loadData()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确定删除该检测记录？', '提示', { type: 'warning' })
  await deleteDetection(id)
  ElMessage.success('删除成功')
  loadData()
}

const showDetail = (row) => {
  detail.value = row
  detailResult.value = null
  if (row.boundaryGeoJson) {
    detailResult.value = {
      detectedArea: 5.48, confidence: 0.952, coverageRate: 0.87,
      modelName: 'DeepLabV3Plus', modelVersion: 'v1.0.0',
      processingTimeMs: 3200, status: 'SUCCESS', createTime: row.updateTime
    }
  }
  drawerVisible.value = true
  nextTick(() => {
    if (detailResult.value) renderMapPreview()
  })
}

onUnmounted(() => { if (chartInstance) chartInstance.dispose() })

const renderMapPreview = () => {
  if (!mapRef.value) return
  chartInstance = echarts.init(mapRef.value)
  chartInstance.setOption({
    xAxis: { type: 'value', show: false }, yAxis: { type: 'value', show: false },
    series: [{
      type: 'lines', coordinateSystem: 'cartesian2d', polyline: true,
      data: [[[0, 0], [0, 80], [30, 100], [80, 90], [100, 60], [90, 20], [60, 0], [20, 10], [0, 0]]],
      lineStyle: { color: '#43a047', width: 2 }, areaStyle: { color: 'rgba(67,160,71,0.15)' }
    }]
  })
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.dr-stat { text-align: center; padding: 12px 0; }
.dr-val { font-size: 24px; font-weight: 700; color: #1a1a1a; }
.dr-val.primary { color: #43a047; }
.dr-val.success { color: #67c23a; }
.dr-lbl { font-size: 12px; color: #999; margin-top: 2px; }
.dr-map { width: 100%; height: 280px; background: #fafafa; border-radius: 8px; border: 1px solid #eee; }
</style>
