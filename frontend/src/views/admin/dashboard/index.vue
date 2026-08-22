<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>👥</span></div>
            <div class="stat-body">
              <div class="stat-value">{{ overview.userCount ?? 0 }}</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>🌾</span></div>
            <div class="stat-body">
              <div class="stat-value">{{ overview.fieldCount ?? 0 }}</div>
              <div class="stat-label">农田总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>📐</span></div>
            <div class="stat-body">
              <div class="stat-value success">{{ overview.totalDetectedArea ?? 0 }} <small>亩</small></div>
              <div class="stat-label">检测总面积</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>📊</span></div>
            <div class="stat-body">
              <div class="stat-value">{{ overview.totalDetections ?? 0 }}</div>
              <div class="stat-label">检测总次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 成功率 + 状态分布 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header><span>检测成功率</span></template>
          <div style="text-align:center;padding:20px">
            <el-progress type="dashboard" :percentage="Number(overview.successRate || 0)" :color="colors">
              <template #default="{ percentage }">
                <span class="percent-text">{{ percentage }}%</span>
              </template>
            </el-progress>
            <p style="color:#999;margin-top:12px">基于 {{ overview.totalDetections }} 次检测统计</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>检测状态分布</span></template>
          <div style="height:250px" ref="pieChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 实时任务看板 -->
    <el-card style="margin-top:16px">
      <template #header>
        <div class="task-header">
          <span>实时任务看板</span>
          <span class="task-refresh">每 5 秒自动刷新</span>
        </div>
      </template>

      <div class="task-status">
        <el-tag type="warning">待处理 {{ tasks.pending ?? 0 }}</el-tag>
        <el-tag>处理中 {{ tasks.processing ?? 0 }}</el-tag>
        <el-tag type="success">已完成 {{ tasks.completed ?? 0 }}</el-tag>
        <el-tag type="danger">失败 {{ tasks.failed ?? 0 }}</el-tag>
        <el-tag type="info">总计 {{ tasks.total ?? 0 }}</el-tag>
      </div>

      <el-table :data="tasks.recent || []" stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="originalFilename" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="110">
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
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getDashboardOverview, getDashboardTasks } from '../../../api'
import * as echarts from 'echarts'

const overview = ref({})
const tasks = ref({})
const pieChartRef = ref(null)
const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 50 },
  { color: '#67c23a', percentage: 100 }
]

let timer = null
let pieChart = null

const statusTag = (s) => ({ PENDING: 'warning', PROCESSING: '', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)

const loadOverview = async () => {
  const res = await getDashboardOverview()
  overview.value = res.data
  nextTick(() => renderPie())
}

const loadTasks = async () => {
  try {
    const res = await getDashboardTasks()
    tasks.value = res.data
  } catch { /* ignore */ }
}

const renderPie = () => {
  if (!pieChartRef.value) return
  if (!pieChart) pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['50%', '75%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 3 },
      label: { show: false },
      data: [
        { value: overview.value.completedDetections || 0, name: '成功', itemStyle: { color: '#67c23a' } },
        { value: overview.value.pendingDetections || 0, name: '待处理', itemStyle: { color: '#e6a23c' } },
        { value: overview.value.failedDetections || 0, name: '失败', itemStyle: { color: '#f56c6c' } }
      ]
    }]
  })
}

const handleResize = () => pieChart?.resize()

onMounted(() => {
  loadOverview()
  loadTasks()
  timer = setInterval(loadTasks, 5000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  if (pieChart) { pieChart.dispose(); pieChart = null }
})
</script>

<style scoped>
.stat-row { margin-top: 0; }
.stat-card { cursor: default; }
.stat-inner { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.stat-body { flex: 1; }
.stat-value { font-size: 26px; font-weight: 700; color: #1a1a1a; }
.stat-value small { font-size: 13px; font-weight: 500; color: #999; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }
.percent-text { font-size: 24px; font-weight: 700; color: #1a1a1a; }

.task-header { display: flex; justify-content: space-between; align-items: center; }
.task-refresh { font-size: 12px; color: #999; }
.task-status { display: flex; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
</style>
