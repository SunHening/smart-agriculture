<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>🌾</span></div>
            <div class="stat-body">
              <div class="stat-value">{{ overview.fieldCount }}</div>
              <div class="stat-label">农田总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>📊</span></div>
            <div class="stat-body">
              <div class="stat-value">{{ overview.totalDetections }}</div>
              <div class="stat-label">检测总次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#e8f5e9"><span>✅</span></div>
            <div class="stat-body">
              <div class="stat-value success">{{ overview.completedDetections }}</div>
              <div class="stat-label">检测成功</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-inner">
            <div class="stat-icon" style="background:#fce4ec"><span>❌</span></div>
            <div class="stat-body">
              <div class="stat-value danger">{{ overview.failedDetections }}</div>
              <div class="stat-label">检测失败</div>
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
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { getDashboardOverview } from '../../api'
import * as echarts from 'echarts'

const overview = ref({})
const pieChartRef = ref(null)
const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 50 },
  { color: '#67c23a', percentage: 100 }
]

const loadOverview = async () => {
  const res = await getDashboardOverview()
  overview.value = res.data
  nextTick(() => renderPie())
}

const renderPie = () => {
  if (!pieChartRef.value) return
  const chart = echarts.init(pieChartRef.value)
  chart.setOption({
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
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => loadOverview())
</script>

<style scoped>
.stat-row { margin-top: 0; }
.stat-card { cursor: default; }
.stat-inner { display: flex; align-items: center; gap: 16px; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 22px; }
.stat-body { flex: 1; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; }
.stat-value.success { color: #67c23a; }
.stat-value.danger { color: #f56c6c; }
.stat-label { font-size: 13px; color: #999; margin-top: 2px; }
.percent-text { font-size: 24px; font-weight: 700; color: #1a1a1a; }
</style>
