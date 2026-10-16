<template>
  <div>
    <el-card>
      <template #header>
        <div class="map-header">
          <span>🗺️ 天地图 · 检测结果</span>
          <span class="map-tip">
            ＋选择目标区域　－排除区域　Box框选　Automatic自动分割
          </span>
        </div>
      </template>

      <div class="map-wrap">
        <div class="map-area">
          <div id="tdt-map" ref="mapRef"></div>
          
          <div class="map-toolbar">

  <el-button-group size="small" style="margin-right:8px">
    <el-button
      :type="basemap === 'img' ? 'primary' : 'default'"
      @click="switchBasemap('img')"
    >
      影像
    </el-button>

    <el-button
      :type="basemap === 'vec' ? 'primary' : 'default'"
      @click="switchBasemap('vec')"
    >
      矢量
    </el-button>
  </el-button-group>

  <el-divider direction="vertical" />

  <!-- Point + -->
  <el-button
    type="primary"
    size="small"
    :plain="promptMode !== 'positive'"
    @click="startPointPrompt('positive')"
  >
    ＋ Point
  </el-button>

  <!-- Point - -->
  <el-button
    type="danger"
    size="small"
    :plain="promptMode !== 'negative'"
    @click="startPointPrompt('negative')"
  >
    － Point
  </el-button>

  <!-- Box -->
  <el-button
    type="success"
    size="small"
    :plain="promptMode !== 'box'"
    @click="startBoxPrompt"
  >
    Box
  </el-button>

  <!-- Automatic -->
  <el-button
    type="warning"
    size="small"
    @click="automaticSegment"
    :loading="automaticLoading"
  >
    {{ automaticLoading ? '分析中...' : 'Automatic' }}
  </el-button>

  <!-- Clear -->
  <el-button
    type="info"
    size="small"
    @click="clearAll"
  >
    清除
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
            <div v-if="!result" class="panel-value">
              请选择 Point、Box 或 Automatic 开始分割
            </div>
            <template v-else>
              <div class="result-grid">
                <div class="result-item"><span class="rl">面积</span><span class="rv primary">{{ result.area }} 亩</span></div>
                <div class="result-item"><span class="rl">置信度</span><span class="rv success">{{ result.confidence }}%</span></div>
                <div class="result-item"><span class="rl">地块数</span><span class="rv">{{ result.fieldCount }}</span></div>
                <div class="result-item"><span class="rl">耗时</span><span class="rv">{{ result.time }}ms</span></div>
              </div>
            </template>
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
import axios from 'axios'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')

const mapRef = ref(null)
const basemap = ref('img')

const drawing = ref(false)
const pointMode = ref(false)
const promptPoints = ref([])

const result = ref(null)
const fieldList = ref([])
const fieldId = ref(null)

// ============================================================
// SAM Point Prompt 状态
// ============================================================

// 当前 Prompt 模式：
// positive = +
// negative = -
const promptMode = ref(null)

// Hover 预览是否正在计算
const hoverLoading = ref(false)

// Point 模式下固定使用的“干净地图截图”
let promptImageBase64 = null
let samSessionId = null
let samSessionPromise = null

// Hover 预览图层
let hoverLayer = null

// 正式 Prompt 点图层
let promptLayer = null

// 当前 Hover 请求序号
let hoverRequestId = 0

// 防止鼠标移动过于频繁
let hoverTimer = null

let map = null
let currentBase = null
let currentLabel = null

let promptPointMarkers = []

let drawnLayer = null
let drawnItems = null
let drawStartLatLng = null
let tempRect = null
let isDrawing = false
let pointLayer = null
let pointResultLayer = null

const TK = 'b2d9cc5faa19adaf7b5c80c90e61e5d7'
const SUBDOMAINS = ['0', '1', '2', '3', '4', '5', '6', '7']

const createLayer = (layer) =>
  L.tileLayer(`https://t{s}.tianditu.gov.cn/${layer}_w/wmts?service=wmts&request=getTile&version=1.0.0&layer=${layer}&style=default&tilematrixset=w&format=tiles&tilematrix={z}&tilerow={y}&tilecol={x}&tk=${TK}`, {
    subdomains: SUBDOMAINS, maxZoom: 18, minZoom: 3, tileSize: 256
  })

const baseLayers = { img: createLayer('img'), vec: createLayer('vec') }
const labelLayers = { img: createLayer('cia'), vec: createLayer('cva') }

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

const loadFields = async () => {
  try {
    const res = await getFieldList()
    fieldList.value = res.data || []
  } catch { /* ignore */ }
}

const startPointPrompt = async (mode) => {
  if (!map) return

  // ----------------------------------------------------
  // 1. 退出框选模式
  // ----------------------------------------------------

  if (isDrawing) {
    isDrawing = false

    map.off('mousedown', onDrawMouseDown)
    map.off('mousemove', onDrawMouseMove)
    document.removeEventListener('mouseup', onDrawMouseUp)

    if (tempRect) {
      map.removeLayer(tempRect)
      tempRect = null
    }

    map.dragging.enable()
    drawing.value = false
  }

  // ----------------------------------------------------
  // 2. 设置 Point 模式
  // ----------------------------------------------------

  pointMode.value = true
  promptMode.value = mode

  console.log(
    '进入 Point 模式:',
    mode
  )

  // ----------------------------------------------------
  // 3. 创建点图层
  // ----------------------------------------------------

  if (!pointLayer) {
    pointLayer = L.layerGroup().addTo(map)
  }

  // ----------------------------------------------------
  // 4. 第一次进入 Point 模式时生成干净截图
  // ----------------------------------------------------

  if (!promptImageBase64) {

    try {

      ElMessage.info(
        '正在准备 SAM 图像，请稍候...'
      )

      promptImageBase64 =
        await capturePromptImage()

      console.log(
        'Point Prompt 图像准备完成'
      )

    } catch (error) {

      console.error(
        'Prompt 图像生成失败:',
        error
      )

      ElMessage.error(
        '无法获取地图图像'
      )

      return
    }
  }

  // ----------------------------------------------------
  // 5. 禁止地图拖动
  // ----------------------------------------------------

  map.dragging.disable()

  // ----------------------------------------------------
  // 6. 注册 Point 点击
  // ----------------------------------------------------

  map.off('click', onPointClick)
  map.on('click', onPointClick)

    // ----------------------------------------------------
    // 7. 注册 Hover
    // ----------------------------------------------------

    map.off('mousemove', onPointMouseMove)
    map.on('mousemove', onPointMouseMove)

    console.log(
      'Hover mousemove 已注册'
    )

  ElMessage.info(
    mode === 'positive'
      ? '＋ 前景点：点击目标地块'
      : '－ 背景点：点击需要排除的区域'
  )
}

const predictHoverPoint = async (e) => {

  if (!pointMode.value) {
    return
  }

  if (!promptImageBase64) {
    return
  }

  const mapEl = document.getElementById('tdt-map')

  if (!mapEl) {
    return
  }

  // ----------------------------------------------------
  // 1. 当前鼠标位置
  // ----------------------------------------------------

  const mapWidth = mapEl.clientWidth
  const mapHeight = mapEl.clientHeight

  const imageWidth = promptImageWidth.value
  const imageHeight = promptImageHeight.value

  const containerPoint = e.containerPoint

  // ----------------------------------------------------
  // 2. Leaflet 坐标 → SAM 图片像素
  // ----------------------------------------------------

  const x =
    e.containerPoint.x *
    imageWidth /
    mapWidth

  const y =
    e.containerPoint.y *
    imageHeight /
    mapHeight

  console.log(
    'Hover 坐标:',
    'map =',
    containerPoint.x,
    containerPoint.y,
    'image =',
    x,
    y
  )
  // ----------------------------------------------------
  // 3. 防止鼠标跑到地图外
  // ----------------------------------------------------

  if (
    x < 0 ||
    y < 0 ||
    x > imageWidth ||
    y > imageHeight
  ) {
    return
  }

  // ----------------------------------------------------
  // 4. 请求序号
  //
  // 非常重要：
  //
  // 鼠标可能快速移动：
  //
  // A 请求
  // B 请求
  // C 请求
  //
  // 如果 A 最后才返回，
  // 不能让 A 覆盖 C 的结果。
  // ----------------------------------------------------

  const requestId = ++hoverRequestId

  hoverLoading.value = true

  try {

    const response = await axios.post(
      'http://127.0.0.1:5000/predict-point',
      {
        image_base64: promptImageBase64,
        session_id: await ensureSamSession(),
        x,
        y,
        label: 1
      },
      {
        timeout: 120000
      }
    )

    // ----------------------------------------------------
    // 5. 如果这不是最新请求，直接丢弃
    // ----------------------------------------------------

    if (requestId !== hoverRequestId) {
      return
    }

    const data = response.data

    if (!data.success) {
      return
    }

    // ----------------------------------------------------
    // 6. 删除旧 Hover
    // ----------------------------------------------------

    if (hoverLayer) {
      map.removeLayer(hoverLayer)
      hoverLayer = null
    }

    hoverLayer = L.layerGroup().addTo(map)

    // ----------------------------------------------------
    // 7. SAM 边界 → Leaflet
    // ----------------------------------------------------

    if (data.boundaries) {

      data.boundaries.forEach((boundary) => {

        const coords =
          boundary.geojson?.coordinates?.[0]

        if (!coords || coords.length < 3) {
          return
        }

        const latlngs = coords.map((coord) => {

          const pixelX = coord[0]
          const pixelY = coord[1]

          const containerX =
            pixelX *
            mapWidth /
            imageWidth

          const containerY =
            pixelY *
            mapHeight /
            imageHeight

          return map.containerPointToLatLng(
            L.point(
              containerX,
              containerY
            )
          )
        })
        

        if (latlngs.length < 3) {
          return
        }

        // ------------------------------------------------
        // Hover 使用较淡的显示
        // ------------------------------------------------

        L.polygon(
          latlngs,
          {
            color: '#409eff',
            weight: 2,
            fillColor: '#409eff',
            fillOpacity: 0.18,
            interactive: false
          }
        ).addTo(hoverLayer)
      })
    }

  } catch (error) {

    // Hover 本身不是关键操作
    // 所以不要每次失败都弹 Element Message

    console.debug(
      'Hover prediction failed:',
      error.message
    )

  } finally {

    if (requestId === hoverRequestId) {
      hoverLoading.value = false
    }
  }
}

// ============================================================
// Point Hover Preview
// ============================================================

const onPointMouseMove = (e) => {

  console.log('Hover:', e.containerPoint.x, e.containerPoint.y)

  if (!pointMode.value) {
    return
  }

  if (!promptImageBase64) {
    return
  }

  // ----------------------------------------------------
  // 防止鼠标移动过快产生大量请求
  // ----------------------------------------------------

  if (hoverTimer) {
    clearTimeout(hoverTimer)
  }

  hoverTimer = setTimeout(() => {
    predictHoverPoint(e)
  }, 150)
}

const onPointClick = async (e) => {
  
  if (!pointMode.value) return

  console.log('========== POINT DEBUG ==========')

  const mapEl = document.getElementById('tdt-map')

  console.log('DOM client:', {
    width: mapEl?.clientWidth,
    height: mapEl?.clientHeight
  })

  console.log('DOM rect:', {
    width: mapEl?.getBoundingClientRect().width,
    height: mapEl?.getBoundingClientRect().height
  })  

  console.log('Leaflet map:', {
    width: map.getSize().x,
    height: map.getSize().y
  })

  console.log('containerPoint:', {
    x: e.containerPoint.x,
    y: e.containerPoint.y
  })

  console.log('promptImage:', {
    width: promptImageWidth.value,
    height: promptImageHeight.value
  })

  console.log('devicePixelRatio:', window.devicePixelRatio)

  console.log('=================================')

  // ----------------------------------------------------
  // 1. 取消当前 Hover 请求
  // ----------------------------------------------------

  hoverRequestId++

  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  // 清除 Hover 预览
  if (hoverLayer) {
    map.removeLayer(hoverLayer)
    hoverLayer = null
  }

  try {
    // ----------------------------------------------------
    // 2. Point 模式必须使用进入 Point 模式时保存的干净截图
    // ----------------------------------------------------

    if (!promptImageBase64) {
      throw new Error('Point Prompt 图像不存在')
    }

    const mapWidth = mapEl.clientWidth
    const mapHeight = mapEl.clientHeight

    // 当前点击位置
    // 因为 promptImageBase64 是进入 Point 模式时保存的
    // 干净地图截图，所以这里直接使用地图容器坐标
    const imageWidth = promptImageWidth.value
    const imageHeight = promptImageHeight.value

    const x =
      e.containerPoint.x *
      imageWidth /
      mapWidth

    const y =
      e.containerPoint.y *
      imageHeight /
      mapHeight

    // ----------------------------------------------------
    // 3. 判断是 + 点还是 - 点
    // ----------------------------------------------------

    const label =
      promptMode.value === 'negative'
        ? 0
        : 1

    console.log('正式 Point:', {
      mode: promptMode.value,
      x,
      y,
      label
    })

    // ----------------------------------------------------
    // 4. 显示用户点击的点
    // ----------------------------------------------------

    const marker = L.circleMarker(e.latlng, {
      radius: 6,
      color: '#ffffff',
      weight: 2,

      // +
      fillColor: label === 1
        ? '#43a047'
        : '#f56c6c',

      fillOpacity: 1,
      interactive: false
    })

    marker.addTo(pointLayer)

    // ----------------------------------------------------
    // 5. 保存 Prompt 点
    // ----------------------------------------------------

    promptPoints.value.push({
      latlng: e.latlng,
      x,
      y,
      label
    })

    console.log(
      '当前 Prompt 点:',
      promptPoints.value
    )

    // ----------------------------------------------------
    // 6. 调用 Python SAM
    // ----------------------------------------------------

    ElMessage.info(
      label === 1
        ? '正在使用前景点进行分割...'
        : '正在使用背景点排除区域...'
    )

    const response = await axios.post(
      'http://127.0.0.1:5000/predict-point',
      {
        image_base64: promptImageBase64,
        session_id: await ensureSamSession(),
        x,
        y,
        label
      },
      {
        timeout: 120000
      }
    )

    const data = response.data

    console.log('SAM Point result:', data)

    if (!data.success) {
      throw new Error(
        data.error || 'Point 分割失败'
      )
    }

    // ----------------------------------------------------
    // 7. 清除上一次 SAM 结果
    // ----------------------------------------------------

    if (pointResultLayer) {
      map.removeLayer(pointResultLayer)
      pointResultLayer = null
    }

    pointResultLayer = L.layerGroup().addTo(map)

    // ----------------------------------------------------
    // 8. SAM 像素坐标 → Leaflet 坐标
    // ----------------------------------------------------

    if (data.boundaries) {

      data.boundaries.forEach((boundary) => {

        const coords =
          boundary.geojson?.coordinates?.[0]

        if (!coords || coords.length < 3) {
          return
        }

        const latlngs = coords.map((coord) => {
          const pixelX = coord[0]
          const pixelY = coord[1]

          const containerX =
            pixelX *
            mapWidth /
            imageWidth

          const containerY =
            pixelY *
            mapHeight /
            imageHeight

          return map.containerPointToLatLng(
            L.point(
              containerX,
              containerY
            )
          )
        })

        L.polygon(
          latlngs,
          {
            color: '#43a047',
            weight: 2,
            fillColor: '#43a047',
            fillOpacity: 0.30,
            interactive: false
          }
        )
          .addTo(pointResultLayer)
          .bindPopup(
            `SAM Confidence: ${
              (data.confidence * 100).toFixed(1)
            }%`
          )
      })
    }

    // ----------------------------------------------------
    // 9. 更新右侧结果
    // ----------------------------------------------------

    result.value = {
      area: '-',
      confidence: (
        data.confidence * 100
      ).toFixed(1),
      fieldCount:
        data.boundaries?.length || 0,
      time:
        data.processing_time_ms
    }

    ElMessage.success(
      `Point 分割完成，Confidence ${
        (data.confidence * 100).toFixed(1)
      }%`
    )

  } catch (error) {

    console.error(
      'Point prediction error:',
      error
    )

    ElMessage.error(
      'Point 分割失败：' +
      (
        error.response?.data?.error ||
        error.message ||
        'SAM 服务异常'
      )
    )
  }
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
  // 退出 Point 模式
  if (pointMode.value) {
    pointMode.value = false
    promptPoints.value = []

    if (map) {
      map.off('click', onPointClick)
      map.off('mousemove', onPointMouseMove)
      map.dragging.enable()
    }

    if (pointLayer) {
      map.removeLayer(pointLayer)
      pointLayer = null
    }

    if (pointResultLayer) {
      map.removeLayer(pointResultLayer)
      pointResultLayer = null
    }

    if (hoverLayer) {
      map.removeLayer(hoverLayer)
      hoverLayer = null
    }
  }

  // 退出框选模式
  if (isDrawing) {
    isDrawing = false

    map.off('mousedown', onDrawMouseDown)
    map.off('mousemove', onDrawMouseMove)

    document.removeEventListener('mouseup', onDrawMouseUp)

    if (tempRect) {
      map.removeLayer(tempRect)
      tempRect = null
    }

    map.dragging.enable()
    drawing.value = false
  }

  // 清除框选结果
  if (drawnItems) {
    drawnItems.clearLayers()
  }

  drawnLayer = null
  result.value = null
}

const promptImageWidth = ref(0)
const promptImageHeight = ref(0)

const ensureSamSession = async () => {
  if (samSessionId) return samSessionId
  if (samSessionPromise) return samSessionPromise
  if (!promptImageBase64) throw new Error('SAM 截图尚未准备好')

  samSessionPromise = (async () => {
    const imageBlob = await fetch(promptImageBase64).then(response => {
      if (!response.ok) throw new Error('无法读取 SAM 截图')
      return response.blob()
    })
    const formData = new FormData()
    formData.append('image', imageBlob, 'sam-prompt.png')
    const response = await axios.post(
      'http://127.0.0.1:5000/sam/sessions',
      formData,
      { timeout: 120000 }
    )
    samSessionId = response.data.session_id
    return samSessionId
  })()

  try {
    return await samSessionPromise
  } catch (error) {
    samSessionPromise = null
    throw error
  }
}

const capturePromptImage = async () => {
  const mapEl = document.getElementById('tdt-map')

  if (!mapEl) {
    throw new Error('地图容器不存在')
  }

  const mapWidth = mapEl.clientWidth
  const mapHeight = mapEl.clientHeight

  const canvas = await html2canvas(mapEl, {
    useCORS: true,
    allowTaint: true,

    // 非常重要：
    // 强制截图与 Leaflet CSS 像素保持 1:1
    scale: 1,

    width: mapWidth,
    height: mapHeight
  })

  // 截图内容或视口变化后，旧 embedding 不再适用。
  samSessionId = null
  samSessionPromise = null
  promptImageWidth.value = canvas.width
  promptImageHeight.value = canvas.height

  console.log('========== SAM Prompt Image ==========')

  console.log('Leaflet CSS:', {
    width: mapWidth,
    height: mapHeight
  })

  console.log('Canvas:', {
    width: canvas.width,
    height: canvas.height
  })

  console.log('=======================================')

  return canvas.toDataURL('image/png')
}

const detectField = async () => {
  if (!drawnLayer) return
  detecting.value = true
  try {
    const mapEl = document.getElementById('tdt-map')
    const canvas = await html2canvas(mapEl, { useCORS: true, allowTaint: true, scale: 1 })
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
    center: [36.96311, 110.80496],
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
    document.removeEventListener(
      'mouseup',
      onDrawMouseUp
    )
  }

  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  if (map) {
    map.off('mousemove', onPointMouseMove)
    map.off('click', onPointClick)
    map.remove()
    map = null
  }
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
