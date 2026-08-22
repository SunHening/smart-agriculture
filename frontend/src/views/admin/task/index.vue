<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图像处理任务监控</span>
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
            <el-button @click="loadData">刷新</el-button>
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
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <el-button size="small" link @click="handleReDetect(row)" :disabled="row.status === 'PROCESSING'">重检</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

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
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDetectionPage, reDetect, deleteDetection } from '../../../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const filterImageType = ref('')
const filterStatus = ref('')

const drawerVisible = ref(false)
const detail = ref(null)

const statusTag = (s) => ({ PENDING: 'warning', PROCESSING: '', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)

const loadData = async () => {
  loading.value = true
  const res = await getDetectionPage({ pageNum: pageNum.value, pageSize: pageSize.value, imageType: filterImageType.value, status: filterStatus.value })
  tableData.value = res.data.records
  total.value = res.data.total
  loading.value = false
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
  drawerVisible.value = true
}

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
