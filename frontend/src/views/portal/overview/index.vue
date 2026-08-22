<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/field')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#e8f5e9"><span>🌾</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.fieldCount }}</div>
              <div class="wc-label">我的农田</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/field')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#e8f5e9"><span>📐</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.detectedArea }} <small>亩</small></div>
              <div class="wc-label">已检测面积</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="work-card" @click="router.push('/home/record')" shadow="hover">
          <div class="wc-inner">
            <div class="wc-icon-box" style="background:#fff3e0"><span>📷</span></div>
            <div class="wc-body">
              <div class="wc-num">{{ overview.detectionCount }}</div>
              <div class="wc-label">检测记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 + 系统简介 -->
    <el-row :gutter="20" style="margin-top:20px">
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header><span>快速操作</span></template>
          <div class="quick-actions">
            <div class="action-card" @click="router.push('/home/tianditu')">
              <span class="action-icon">🗺️</span>
              <div class="action-info">
                <span class="action-title">天地图检测</span>
                <span class="action-desc">框选区域，自动提取地块边界与面积</span>
              </div>
            </div>
            <div class="action-card" @click="router.push('/home/upload')">
              <span class="action-icon">📤</span>
              <div class="action-info">
                <span class="action-title">上传影像</span>
                <span class="action-desc">上传无人机航拍影像，自动测算农田面积</span>
              </div>
            </div>
            <div class="action-card" @click="router.push('/home/field')">
              <span class="action-icon">🌾</span>
              <div class="action-info">
                <span class="action-title">地块管理</span>
                <span class="action-desc">维护农田档案信息，新增 / 编辑 / 删除地块</span>
              </div>
            </div>
            <div class="action-card" @click="router.push('/home/chat')">
              <span class="action-icon">💬</span>
              <div class="action-info">
                <span class="action-title">AI 问答</span>
                <span class="action-desc">大模型交互问答，解答农田检测相关问题</span>
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
              <span class="intro-dot">🧠</span>
              <span>基于 SAM-Geo（Segment Anything Model）自动识别农田边界</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">📐</span>
              <span>精准边界提取与面积计算，GeoJSON 格式输出</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">🌱</span>
              <span>精准分割地块边界，自动测算农田面积（亩）</span>
            </div>
            <div class="intro-item">
              <span class="intro-dot">🤖</span>
              <span>接入 DeepSeek 大模型，支持上下文管理的智能问答</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getFieldPage, getDetectionPage } from '../../../api'

const router = useRouter()

const overview = ref({ fieldCount: 0, detectedArea: 0, detectionCount: 0 })

onMounted(async () => {
  try {
    const [fieldRes, detectionRes] = await Promise.all([
      getFieldPage({ pageNum: 1, pageSize: 200 }),
      getDetectionPage({ pageNum: 1, pageSize: 1 })
    ])
    const fields = fieldRes.data?.records || []
    const detectedArea = fields.reduce((sum, f) => sum + (Number(f.detectedArea) || 0), 0)
    overview.value = {
      fieldCount: fields.length,
      detectedArea: detectedArea.toFixed(1),
      detectionCount: detectionRes.data?.total || 0
    }
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
.wc-num small { font-size: 14px; font-weight: 500; color: #999; }
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
