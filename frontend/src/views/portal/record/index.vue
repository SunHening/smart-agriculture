<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>检测结果记录</span>
          <div>
            <el-select v-model="filterImageType" placeholder="影像类型" clearable style="width:160px;margin-right:10px" @change="loadData">
              <el-option label="天地图遥感" value="REMOTE_SENSING" />
              <el-option label="上传影像" value="DRONE" />
            </el-select>
            <el-button @click="loadData">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" stripe v-loading="loading">
        <el-table-column prop="originalFilename" label="文件名" min-width="160" show-overflow-tooltip />
        <el-table-column label="影像类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.imageType === 'REMOTE_SENSING' ? 'primary' : 'success'" size="small">
              {{ row.imageType === 'REMOTE_SENSING' ? '天地图遥感' : '上传影像' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联农田" min-width="110">
          <template #default="{ row }">{{ row.fieldName || '-' }}</template>
        </el-table-column>
        <el-table-column label="面积(亩)" width="110">
          <template #default="{ row }"><span class="area">{{ row.totalAreaMu }}</span></template>
        </el-table-column>
        <el-table-column prop="fieldCount" label="地块数" width="80" />
        <el-table-column label="置信度" width="100">
          <template #default="{ row }">{{ (Number(row.confidence) * 100).toFixed(1) }}%</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="检测时间" width="170" />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" v-model:current-page="pageNum" v-model:page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="检测详情" direction="rtl" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border size="small" style="margin-bottom:20px">
          <el-descriptions-item label="文件名">{{ detail.originalFilename || '-' }}</el-descriptions-item>
          <el-descriptions-item label="影像类型">
            <el-tag :type="detail.imageType === 'REMOTE_SENSING' ? 'primary' : 'success'" size="small">
              {{ detail.imageType === 'REMOTE_SENSING' ? '天地图遥感' : '上传影像' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联农田">{{ detail.fieldName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="GSD">{{ detail.gsd || '-' }} m/pixel</el-descriptions-item>
          <el-descriptions-item label="总面积">{{ detail.totalAreaMu }} 亩</el-descriptions-item>
          <el-descriptions-item label="地块数">{{ detail.fieldCount }}</el-descriptions-item>
          <el-descriptions-item label="平均置信度">{{ (Number(detail.confidence) * 100).toFixed(1) }}%</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTag(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="检测时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="boundary-title">边界可视化</div>
        <svg v-if="boundaryShapes.shapes.length" :viewBox="boundaryShapes.viewBox" class="boundary-svg" preserveAspectRatio="xMidYMid meet">
          <polygon v-for="(s, i) in boundaryShapes.shapes" :key="i"
            :points="s.points.map((p) => p.join(',')).join(' ')"
            fill="rgba(67,160,71,0.35)" stroke="#43a047" stroke-width="2" vector-effect="non-scaling-stroke" />
        </svg>
        <el-empty v-else description="无边界数据" :image-size="60" />

        <div class="boundary-title">分割地块明细（{{ detail.boundaries?.length || 0 }}）</div>
        <el-table :data="detail.boundaries || []" size="small" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="面积(亩)" width="120">
            <template #default="{ row }">{{ row.detectedArea }}</template>
          </el-table-column>
          <el-table-column label="置信度" width="110">
            <template #default="{ row }">{{ (Number(row.confidence) * 100).toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column prop="modelName" label="模型" />
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getDetectionRecords } from '../../../api'

const user = JSON.parse(localStorage.getItem('user') || '{}')

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const filterImageType = ref('')

const drawerVisible = ref(false)
const detail = ref(null)

const statusTag = (s) => ({ PENDING: 'warning', PROCESSING: '', COMPLETED: 'success', FAILED: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ PENDING: '待处理', PROCESSING: '处理中', COMPLETED: '已完成', FAILED: '失败' }[s] || s)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDetectionRecords({
      userId: user.id,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      imageType: filterImageType.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

const showDetail = (row) => {
  detail.value = row
  drawerVisible.value = true
}

// 解析每条边界的 GeoJSON，聚合为 SVG 多边形与视口
const boundaryShapes = computed(() => {
  const shapes = []
  const pts = []
  ;(detail.value?.boundaries || []).forEach((b) => {
    let geo = null
    try { geo = typeof b.geojson === 'string' ? JSON.parse(b.geojson) : b.geojson } catch (e) { geo = null }
    if (!geo || geo.type !== 'Polygon' || !Array.isArray(geo.coordinates)) return
    geo.coordinates.forEach((ring) => {
      const ringPts = ring.map((c) => [Number(c[0]), Number(c[1])])
      if (ringPts.length < 3) return
      pts.push(...ringPts)
      shapes.push({ points: ringPts })
    })
  })
  if (!pts.length) return { shapes: [], viewBox: '0 0 100 100' }
  const xs = pts.map((p) => p[0])
  const ys = pts.map((p) => p[1])
  const minX = Math.min(...xs)
  const maxX = Math.max(...xs)
  const minY = Math.min(...ys)
  const maxY = Math.max(...ys)
  const w = maxX - minX || 1
  const h = maxY - minY || 1
  const pad = Math.max(w, h) * 0.05
  return { shapes, viewBox: `${minX - pad} ${minY - pad} ${w + pad * 2} ${h + pad * 2}` }
})

onMounted(() => loadData())
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.area { color: #43a047; font-weight: 600; }
.boundary-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }
.boundary-svg { width: 100%; height: 300px; background: #f5f7fa; border-radius: 8px; border: 1px solid #ebeef5; display: block; }
</style>
