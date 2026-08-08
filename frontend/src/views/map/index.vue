<template>
  <div class="map-page">
    <!-- 左侧地图 -->
    <div class="map-area">
      <div id="tdt-map" ref="mapRef"></div>

      <!-- 工具栏 -->
      <div class="map-toolbar">
        <el-button-group size="small" style="margin-right:8px">
          <el-button :type="basemap === 'img' ? 'primary' : 'default'" @click="switchBasemap('img')">影像</el-button>
          <el-button :type="basemap === 'vec' ? 'primary' : 'default'" @click="switchBasemap('vec')">矢量</el-button>
        </el-button-group>
        <el-divider direction="vertical" />
        <el-button type="success" size="small" @click="startDraw" :disabled="drawing">框选区域</el-button>
        <el-button type="warning" size="small" @click="clearDraw" :disabled="!drawnLayer" style="margin:0 4px">清除</el-button>
        <el-button type="primary" size="small" @click="detectField" :disabled="!drawnLayer || detecting" :loading="detecting">
          {{ detecting ? '正在检测...' : '检测农田' }}
        </el-button>
      </div>
    </div>

    <!-- 右侧面板 -->
    <aside class="side-panel">
      <el-card>
        <template #header><span>📡 地图控制</span></template>
        <div class="panel-section">
          <div class="panel-label">点击位置</div>
          <div class="panel-value">{{ clickCoord || '点击地图获取坐标' }}</div>
        </div>
        <el-divider />
        <div class="panel-section">
          <div class="panel-label">快速定位</div>
          <el-select v-model="location" @change="goToLocation" placeholder="选择示例地点" style="width:100%">
            <el-option label="山西·石楼县" value="shilou" />
            <el-option label="山西·永和县" value="yonghe" />
          </el-select>
        </div>
        <el-divider />
        <div class="panel-section">
          <div class="panel-label">检测结果</div>
          <div v-if="!result" class="panel-value">框选区域后点击"检测农田"</div>
          <template v-else>
            <div class="result-grid">
              <div class="result-item">
                <span class="rl">面积</span>
                <span class="rv primary">{{ result.area }} 亩</span>
              </div>
              <div class="result-item">
                <span class="rl">置信度</span>
                <span class="rv success">{{ result.confidence }}%</span>
              </div>
              <div class="result-item">
                <span class="rl">地块数</span>
                <span class="rv">{{ result.fieldCount }}</span>
              </div>
              <div class="result-item">
                <span class="rl">耗时</span>
                <span class="rv">{{ result.time }}ms</span>
              </div>
            </div>
            <el-button type="success" size="small" style="margin-top:12px;width:100%" @click="saveResult">保存结果到农田</el-button>
          </template>
        </div>
      </el-card>
    </aside>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet-draw'
import 'leaflet/dist/leaflet.css'
import 'leaflet-draw/dist/leaflet.draw.css'
import { samGeoDetect } from '../../api'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')
const mapRef = ref(null)
const basemap = ref('img')
const clickCoord = ref('')
const location = ref('shilou')
const drawing = ref(false)
const detecting = ref(false)
const result = ref(null)

let map = null
let currentBase = null
let currentLabel = null
let drawnLayer = null
let drawnItems = null
let clickMarker = null

const TK = 'b2d9cc5faa19adaf7b5c80c90e61e5d7'
const SUBDOMAINS = ['0', '1', '2', '3', '4', '5', '6', '7']

const createLayer = (layer) =>
  L.tileLayer(`https://t{s}.tianditu.gov.cn/${layer}_w/wmts?service=wmts&request=getTile&version=1.0.0&layer=${layer}&style=default&tilematrixset=w&format=tiles&tilematrix={z}&tilerow={y}&tilecol={x}&tk=${TK}`, {
    subdomains: SUBDOMAINS, maxZoom: 18, minZoom: 3, tileSize: 256
  })

const baseLayers = { img: createLayer('img'), vec: createLayer('vec') }
const labelLayers = { img: createLayer('cia'), vec: createLayer('cva') }

const focusPoints = {
  shilou: { coord: [36.96311, 110.80496], zoom: 12, label: '山西石楼县' },
  yonghe: { coord: [36.70461, 110.71903], zoom: 13, label: '山西永和县' }
}

const switchBasemap = (type) => {
  if (currentBase) map.removeLayer(currentBase)
  if (currentLabel) map.removeLayer(currentLabel)
  currentBase = baseLayers[type]
  currentLabel = labelLayers[type]
  currentBase.addTo(map)
  currentLabel.addTo(map)
  basemap.value = type
}

const goToLocation = (key) => {
  const t = focusPoints[key]
  if (!t) return
  map.setView(t.coord, t.zoom)
}

// 框选工具
const startDraw = () => {
  if (!drawnItems) {
    drawnItems = new L.FeatureGroup()
    map.addLayer(drawnItems)
  }
  drawing.value = true
  const drawControl = new L.Control.Draw({
    position: 'topright',
    draw: {
      polygon: { allowIntersection: false, showArea: true, shapeOptions: { color: '#43a047' } },
      rectangle: { shapeOptions: { color: '#43a047' } },
      polyline: false, circle: false, circlemarker: false, marker: false
    },
    edit: false
  })
  drawControl.addTo(map)
  map.once(L.Draw.Event.CREATED, (e) => {
    drawnItems.clearLayers()
    drawnLayer = e.layer
    drawnItems.addLayer(drawnLayer)
    map.removeControl(drawControl)
    drawing.value = false
    ElMessage.success('区域已选定，点击"检测农田"开始分析')
  })
}

const clearDraw = () => {
  if (drawnItems) drawnItems.clearLayers()
  drawnLayer = null
  result.value = null
}

// 调用后端 SAM-Geo API 进行真实检测
const detectField = async () => {
  if (!drawnLayer) return
  detecting.value = true

  try {
    const bounds = drawnLayer.getBounds()
    const bbox = {
      north: bounds.getNorth(), south: bounds.getSouth(),
      east: bounds.getEast(), west: bounds.getWest()
    }

    const res = await samGeoDetect({ userId: user.id, bbox, zoom: map.getZoom() })
    const data = res.data

    // 在地图上绘制检测到的农田边界
    if (drawnItems) drawnItems.clearLayers()
    if (data.boundaries) {
      data.boundaries.forEach(b => {
        // GeoJSON 坐标是 [x, y] (像素坐标)，直接叠加在地图上
        const coords = b.geojson.coordinates[0].map(c => [c[1], c[0]]) // 交换为 [lat, lng]
        L.polygon(coords, {
          color: '#43a047', weight: 2, fillColor: '#43a047', fillOpacity: 0.25
        }).addTo(drawnItems)
      })
    }

    result.value = {
      area: data.totalAreaMu,
      confidence: data.boundaries?.[0]?.confidence ? (data.boundaries[0].confidence * 100).toFixed(1) : '-',
      fieldCount: data.boundaries?.length || 0,
      time: data.processingTimeMs
    }
    ElMessage.success(`检测完成：${data.boundaries?.length || 0} 个地块，总面积 ${data.totalAreaMu} 亩`)
  } catch (e) {
    ElMessage.error('检测失败：SAM-Geo 服务未启动或连接异常')
    console.error(e)
  } finally {
    detecting.value = false
  }
}

const saveResult = async () => {
  ElMessage.success('检测结果已自动保存到数据库')
}

onMounted(async () => {
  await nextTick()
  if (!mapRef.value) return

  map = L.map('tdt-map', {
    center: focusPoints.shilou.coord,
    zoom: 17,
    zoomControl: false,
    attributionControl: false
  })

  // 缩放控件放右下，避免与左上工具栏重叠
  L.control.zoom({ position: 'bottomright' }).addTo(map)
  L.control.attribution({ position: 'bottomleft', prefix: '天地图' }).addTo(map)

  switchBasemap('img')
  L.control.scale({ position: 'bottomleft', imperial: false }).addTo(map)

  // 点击获取坐标
  map.on('click', (e) => {
    const { lat, lng } = e.latlng
    clickCoord.value = `${lat.toFixed(5)}, ${lng.toFixed(5)}`
    if (!clickMarker) {
      clickMarker = L.circleMarker(e.latlng, { radius: 6, color: '#38bdf8', weight: 2, fillOpacity: 0.3 }).addTo(map)
    } else {
      clickMarker.setLatLng(e.latlng)
    }
  })
})

onUnmounted(() => {
  if (map) { map.remove(); map = null }
})
</script>

<style scoped>
.map-page { display: flex; height: calc(100vh - 100px); gap: 16px; }
.map-area { flex: 1; position: relative; border-radius: 12px; overflow: hidden; }
#tdt-map { width: 100%; height: 100%; }

.map-toolbar { position: absolute; top: 10px; left: 10px; z-index: 1000; display: flex; align-items: center; background: #fff; padding: 6px 10px; border-radius: 10px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }

.side-panel { width: 300px; flex-shrink: 0; }
.panel-section { margin: 8px 0; }
.panel-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.panel-value { font-size: 14px; color: #333; }

.result-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.result-item { text-align: center; padding: 8px; background: #f7f8fa; border-radius: 8px; }
.rl { font-size: 11px; color: #999; display: block; }
.rv { font-size: 18px; font-weight: 700; color: #1a1a1a; display: block; }
.rv.primary { color: #43a047; }
.rv.success { color: #67c23a; }
</style>
