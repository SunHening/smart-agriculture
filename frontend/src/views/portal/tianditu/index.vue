<template>
  <div>
    <el-card>
      <template #header>
        <div class="map-header">
          <span>🗺️ 天地图 · 检测结果</span>
          <span class="map-tip">框选区域后点击「检测农田」，自动提取地块边界与面积</span>
        </div>
      </template>

      <div class="map-wrap">
        <div class="map-area">
          <div id="tdt-map" ref="mapRef"></div>
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

        <aside class="side-panel">
          <div class="panel-section">
            <div class="panel-label">关联农田</div>
            <el-select v-model="fieldId" placeholder="请选择（可选）" clearable style="width:100%">
              <el-option v-for="f in fieldList" :key="f.id" :label="f.fieldName" :value="f.id" />
            </el-select>
          </div>
          <el-divider />
          <div class="panel-section">
            <div class="panel-label">检测结果</div>
            <div v-if="!result" class="panel-value">框选区域后点击"检测农田"</div>
            <template v-else>
              <div class="result-grid">
                <div class="result-item"><span class="rl">面积</span><span class="rv primary">{{ result.area }} 亩</span></div>
                <div class="result-item"><span class="rl">置信度</span><span class="rv success">{{ result.confidence }}%</span></div>
                <div class="result-item"><span class="rl">地块数</span><span class="rv">{{ result.fieldCount }}</span></div>
                <div class="result-item"><span class="rl">耗时</span><span class="rv">{{ result.time }}ms</span></div>
              </div>
            </template>
          </div>
          <el-divider />
          <div class="panel-section">
            <div class="panel-label">快速定位</div>
            <el-select v-model="location" @change="goToLocation" placeholder="选择示例地点" style="width:100%">
              <el-option label="山西·石楼县" value="shilou" />
              <el-option label="山西·永和县" value="yonghe" />
            </el-select>
          </div>
        </aside>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import html2canvas from 'html2canvas'
import { getFieldList, samGeoDetectImage } from '../../../api'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')

const mapRef = ref(null)
const basemap = ref('img')
const location = ref('shilou')
const drawing = ref(false)
const detecting = ref(false)
const result = ref(null)
const fieldList = ref([])
const fieldId = ref(null)

let map = null
let currentBase = null
let currentLabel = null
let drawnLayer = null
let drawnItems = null
let drawStartLatLng = null
let tempRect = null
let isDrawing = false

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
  if (!map) return
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
  if (!t || !map) return
  map.setView(t.coord, t.zoom)
}

const loadFields = async () => {
  try {
    const res = await getFieldList()
    fieldList.value = res.data || []
  } catch { /* ignore */ }
}

const startDraw = () => {
  if (!map) return
  if (!drawnItems) {
    drawnItems = new L.FeatureGroup()
    map.addLayer(drawnItems)
  }
  // 清除上一次的框，避免叠加
  if (drawnLayer) {
    drawnItems.clearLayers()
    drawnLayer = null
  }
  drawing.value = true
  ElMessage.info('按住鼠标左键在地图上拖拽，拉出任意大小的框')
  map.dragging.disable()
  map.on('mousedown', onDrawMouseDown)
}

const onDrawMouseDown = (e) => {
  drawStartLatLng = e.latlng
  isDrawing = true
  tempRect = L.rectangle([drawStartLatLng, drawStartLatLng], {
    color: '#43a047', weight: 2, fillColor: '#43a047', fillOpacity: 0.15, interactive: false
  }).addTo(map)
  map.on('mousemove', onDrawMouseMove)
  document.addEventListener('mouseup', onDrawMouseUp)
}

const onDrawMouseMove = (e) => {
  if (!isDrawing || !tempRect) return
  tempRect.setBounds(L.latLngBounds(drawStartLatLng, e.latlng))
}

const onDrawMouseUp = (e) => {
  if (!isDrawing) return
  isDrawing = false
  map.off('mousemove', onDrawMouseMove)
  document.removeEventListener('mouseup', onDrawMouseUp)

  // document 的 mouseup 是 DOM 事件（无 latlng），用容器坐标反算结束点
  const c = map.getContainer()
  const r = c.getBoundingClientRect()
  const endLatLng = map.containerPointToLatLng(L.point(e.clientX - r.left, e.clientY - r.top))

  if (tempRect) { map.removeLayer(tempRect); tempRect = null }

  const bounds = L.latLngBounds(drawStartLatLng, endLatLng)
  const tiny = 0.00005
  if (bounds.getNorth() - bounds.getSouth() < tiny || bounds.getEast() - bounds.getWest() < tiny) {
    ElMessage.warning('框选区域过小，请重新拖拽一个更大的框')
  } else {
    drawnItems.clearLayers()
    drawnLayer = L.rectangle(bounds, {
      color: '#43a047', weight: 2, fillColor: '#43a047', fillOpacity: 0.15, interactive: false
    })
    drawnItems.addLayer(drawnLayer)
    ElMessage.success('区域已选定，点击"检测农田"开始分析')
  }

  map.off('mousedown', onDrawMouseDown)
  map.dragging.enable()
  drawing.value = false
}

const clearDraw = () => {
  if (isDrawing) {
    isDrawing = false
    map.off('mousedown', onDrawMouseDown)
    map.off('mousemove', onDrawMouseMove)
    document.removeEventListener('mouseup', onDrawMouseUp)
    if (tempRect) { map.removeLayer(tempRect); tempRect = null }
    map.dragging.enable()
    drawing.value = false
  }
  if (drawnItems) drawnItems.clearLayers()
  drawnLayer = null
  result.value = null
}

const detectField = async () => {
  if (!drawnLayer) return
  detecting.value = true
  try {
    const mapEl = document.getElementById('tdt-map')
    const canvas = await html2canvas(mapEl, { useCORS: true, allowTaint: true })
    const imgBase64 = canvas.toDataURL('image/png')

    const zoom = map.getZoom()
    const gsdMap = { 13: 19.2, 14: 9.6, 15: 4.8, 16: 2.4, 17: 1.2, 18: 0.6, 19: 0.3 }
    const gsd = gsdMap[zoom] || 1.2

    const res = await samGeoDetectImage({ userId: user.id, fieldId: fieldId.value, imageBase64: imgBase64, gsd })
    const data = res.data

    // 在地图上绘制检测到的边界
    if (drawnItems) drawnItems.clearLayers()
    if (data.boundaries) {
      const mapBounds = map.getBounds()
      data.boundaries.forEach((b) => {
        const coords = b.geojson.coordinates[0]
        const latlngs = coords.map((c) => {
          const lng = mapBounds.getWest() + (c[0] / canvas.width) * (mapBounds.getEast() - mapBounds.getWest())
          const lat = mapBounds.getNorth() - (c[1] / canvas.height) * (mapBounds.getNorth() - mapBounds.getSouth())
          return [lat, lng]
        })
        if (latlngs.length > 2) {
          L.polygon(latlngs, { color: '#43a047', weight: 2, fillColor: '#43a047', fillOpacity: 0.25 })
            .addTo(drawnItems).bindPopup(`${b.area_mu} 亩`)
        }
      })
    }

    result.value = {
      area: data.totalAreaMu,
      confidence: data.boundaries?.[0]?.confidence ? (data.boundaries[0].confidence * 100).toFixed(1) : '-',
      fieldCount: data.fieldCount || data.boundaries?.length || 0,
      time: data.processingTimeMs
    }
    ElMessage.success(`检测完成：${data.fieldCount || data.boundaries?.length || 0} 个地块，总面积 ${data.totalAreaMu} 亩`)
  } catch (e) {
    ElMessage.error('检测失败：' + (e.message || 'SAM-Geo 服务未启动'))
  } finally {
    detecting.value = false
  }
}

onMounted(async () => {
  loadFields()
  await nextTick()
  if (!mapRef.value) return
  map = L.map('tdt-map', {
    center: focusPoints.shilou.coord,
    zoom: 17,
    zoomControl: false,
    attributionControl: false
  })
  L.control.zoom({ position: 'bottomright' }).addTo(map)
  L.control.attribution({ position: 'bottomleft', prefix: '天地图' }).addTo(map)
  switchBasemap('img')
  L.control.scale({ position: 'bottomleft', imperial: false }).addTo(map)
})

onUnmounted(() => {
  if (isDrawing) {
    document.removeEventListener('mouseup', onDrawMouseUp)
  }
  if (map) { map.remove(); map = null }
})
</script>

<style scoped>
.map-header { display: flex; justify-content: space-between; align-items: center; }
.map-tip { font-size: 12px; color: #999; }

.map-wrap { display: flex; gap: 16px; }
.map-area { flex: 1; position: relative; border-radius: 12px; overflow: hidden; height: 520px; }
#tdt-map { width: 100%; height: 100%; }
.map-toolbar { position: absolute; top: 10px; left: 10px; z-index: 1000; display: flex; align-items: center; background: #fff; padding: 6px 10px; border-radius: 10px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); }

.side-panel { width: 280px; flex-shrink: 0; }
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
