<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/field')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#e8f5e9"><span>🌾</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.fieldCount || 0 }}</div>
              <div class="wc-label">我的农田</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/detection')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#e8f5e9"><span>✅</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.completedDetections || 0 }}</div>
              <div class="wc-label">已完成检测</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/detection')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#fff3e0"><span>📷</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.totalDetections || 0 }}</div>
              <div class="wc-label">检测总次数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 + 最近动态 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span>快速操作</span></template>
          <div class="quick-actions">
            <div class="action-card" @click="router.push('/home/detection')">
              <span class="action-icon">📤</span>
              <div class="action-info">
                <span class="action-title">上传影像</span>
                <span class="action-desc">上传遥感或无人机影像进行农田检测</span>
              </div>
            </div>
            <div class="action-card" @click="router.push('/home/field')">
              <span class="action-icon">🗺️</span>
              <div class="action-info">
                <span class="action-title">农田管理</span>
                <span class="action-desc">查看和管理农田信息与检测结果</span>
              </div>
            </div>
            <div class="action-card" @click="router.push('/home/customer-service')">
              <span class="action-icon">💬</span>
              <div class="action-info">
                <span class="action-title">智能客服</span>
                <span class="action-desc">AI助手解答农田检测相关问题</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span>系统简介</span></template>
          <div class="intro-list">
            <div class="intro-item">
              <span class="intro-dot">🛰️</span>
              <span>支持卫星遥感影像与无人机航拍图像上传分析</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">🧠</span>
              <span>基于 U-Net / DeepLabV3+ 深度学习模型自动识别农田</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">📐</span>
              <span>精准边界提取与面积计算，GeoJSON 格式输出</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">📊</span>
              <span>可视化检测结果叠加展示，多时相对比分析</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardOverview } from '../../../api'

const router = useRouter()

const overview = ref({})

onMounted(async () => {
  try {
    const res = await getDashboardOverview()
    overview.value = res.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.stat-row { margin-bottom: 0; }

.work-card { cursor: pointer; }
.wc-inner { display: flex; align-items: center; gap: 16px; }
.wc-icon-box { width: 52px; height: 52px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; flex-shrink: 0; }
.wc-body { flex: 1; }
.wc-num { font-size: 28px; font-weight: 700; color: #1a1a1a; }
.wc-label { font-size: 13px; color: #999; margin-top: 2px; }

.quick-actions { display: flex; flex-direction: column; gap: 12px; }
.action-card { display: flex; align-items: center; gap: 14px; padding: 14px 16px; border-radius: 10px; cursor: pointer; transition: background 0.2s; border: 1px solid transparent; }
.action-card:hover { background: #f9fdf9; border-color: #e8f5e9; }
.action-icon { font-size: 28px; flex-shrink: 0; }
.action-info { display: flex; flex-direction: column; gap: 2px; }
.action-title { font-size: 14px; font-weight: 500; color: #333; }
.action-desc { font-size: 12px; color: #999; }

.intro-list { display: flex; flex-direction: column; gap: 14px; }
.intro-item { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #555; }
.intro-dot { flex-shrink: 0; font-size: 18px; }
</style>
