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
            <div v-if="!result && !areaResult && !autoResult" class="panel-value">
              请选择 Point、Box 或 Automatic 开始分割
            </div>

            <template v-else>
              <div class="result-grid">

                <!-- 面积 -->
                <div class="result-item">
                  <span class="rl">面积</span>
                  <span class="rv primary">
                    {{
                      areaResult
                        ? areaResult.areaMu.toFixed(2)
                        : (autoResult?.area ?? result?.area ?? '--')
                    }} 亩
                  </span>
                </div>

                <!-- 置信度 -->
                <div class="result-item">
                  <span class="rl">置信度</span>
                  <span class="rv success">
                    {{ autoResult?.confidence ?? result?.confidence ?? '--' }}%
                  </span>
                </div>

                <!-- 地块数 -->
                <div class="result-item">
                  <span class="rl">地块数</span>
                  <span class="rv">
                    {{ autoResult?.fieldCount ?? result?.fieldCount ?? '--' }}
                  </span>
                </div>

                <!-- 耗时 -->
                <div class="result-item">
                  <span class="rl">耗时</span>
                  <span class="rv">
                    {{ autoResult?.time ?? result?.time ?? '--' }}ms
                  </span>
                </div>

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
import axios from 'axios'
import { ElMessage } from 'element-plus'

const user = JSON.parse(localStorage.getItem('user') || '{}')

const mapRef = ref(null)
const basemap = ref('img')

const drawing = ref(false)
const pointMode = ref(false)
const promptPoints = ref([])

// SAM Prompt结果(Point/Box)
const samResult = ref(null)

const fieldList = ref([])
const fieldId = ref(null)
const result = ref(null)

// Box面积结果
const areaResult = ref(null)

// ============================================================
// SAM Point Prompt 状态
// ============================================================

// 当前 Prompt 模式：
// positive = +
// negative = -
const promptMode = ref(null)

// Hover 预览是否正在计算
const hoverLoading = ref(false)

// ============================================================
// SAM Session
// ============================================================

// 当前地图截图对应的 SAM Session
const samSessionId = ref(null)

// 当前 SAM 图片对应的地图范围
let samImageBounds = null

// 当前正式 SAM Mask 图层
let samMaskLayer = null

// Point 模式下固定使用的“干净地图截图”
let promptImageBase64 = null

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

// Automatic农田边界图层
let autoPolygonLayer = null


// Box临时绘制图层
let drawnItems = null
let drawStartLatLng = null
let tempRect = null
let isDrawing = false
let pointLayer = null

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

    // 记录这张截图对应的地图范围
    samImageBounds = map.getBounds()

    console.log(
      'Point Prompt 图像准备完成'
    )

    ElMessage.info(
      '正在创建 SAM Session，请稍候...'
    )

    await createSamSession()

    console.log(
      'SAM Session 创建完成:',
      samSessionId.value
    )

  } catch (error) {

    console.error(
      'SAM Session 创建失败:',
      error
    )

    ElMessage.error(
      'SAM Session 创建失败：' +
      (error.message || '未知错误')
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
    // map.on('mousemove', onPointMouseMove)

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
  if (!pointMode.value) return
  if (!promptImageBase64) return
  if (!samSessionId.value) return

  const mapEl = document.getElementById('tdt-map')
  if (!mapEl) return

  const mapWidth = mapEl.clientWidth
  const mapHeight = mapEl.clientHeight

  const imageWidth = promptImageWidth.value
  const imageHeight = promptImageHeight.value

  const x =
    e.containerPoint.x * imageWidth / mapWidth

  const y =
    e.containerPoint.y * imageHeight / mapHeight

  const requestId = ++hoverRequestId

  hoverLoading.value = true

  try {
    const response = await axios.post(
      `/api/sam/session/${samSessionId.value}/prediction`,
      {
        points: [
          {
            x,
            y,
            label: 1
          }
        ],
        multimask_output: true
      },
      {
        responseType: 'blob',
        timeout: 120000
      }
    )

    if (requestId !== hoverRequestId) {
      return
    }

    const maskBlob = response.data

    if (!(maskBlob instanceof Blob)) {
      throw new Error('SAM Hover 返回结果不是 Blob')
    }

    if (hoverLayer) {
      map.removeLayer(hoverLayer)
      hoverLayer = null
    }

    const maskUrl = URL.createObjectURL(maskBlob)

    hoverLayer = L.imageOverlay(
      maskUrl,
      samImageBounds,
      {
        opacity: 0.25,
        interactive: false
      }
    ).addTo(map)

  } catch (error) {
    if (requestId === hoverRequestId) {
      console.debug(
        'Hover prediction failed:',
        error.message
      )
    }
  } finally {
    if (requestId === hoverRequestId) {
      hoverLoading.value = false
    }
  }
}

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

    if (!samSessionId.value) {
      throw new Error('SAM Session 不存在')
    }

    const response = await axios.post(
      `/api/sam/session/${samSessionId.value}/prediction`,
      {
        points: promptPoints.value,
        multimask_output: true
      },
      {
        responseType: 'blob',
        timeout: 120000
      }
    )

    const maskBlob = response.data

    if (!(maskBlob instanceof Blob)) {
      throw new Error('SAM 返回结果不是 Blob')
    }

    console.log('SAM Mask:', {
      type: maskBlob.type,
      size: maskBlob.size
    })

    const maskUrl = URL.createObjectURL(maskBlob)

    if (samMaskLayer) {
      map.removeLayer(samMaskLayer)
      samMaskLayer = null
    }

    samMaskLayer = L.imageOverlay(
      maskUrl,
      samImageBounds,
      {
        opacity: 0.45,
        interactive: false
      }
    ).addTo(map)

    // ----------------------------------------------------
    // 9. 更新右侧结果
    // ----------------------------------------------------

    const area = await calculateMaskArea(
      maskBlob,
      promptZoom.value
    )

    areaResult.value = area

    result.value = {
      area: area.areaMu,
      confidence: '-',
      fieldCount: 1,
      time: '-'
    }

    ElMessage.success('Point 分割完成')

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

const startBoxPrompt = async () => {
  if (!map) return

  // 退出 Point 模式
  pointMode.value = false
  promptMode.value = 'box'

  if (!promptImageBase64) {
    promptImageBase64 = await capturePromptImage()
    samImageBounds = map.getBounds()
    await createSamSession()
  }

  hoverRequestId++

  if (hoverTimer) {
    clearTimeout(hoverTimer)
    hoverTimer = null
  }

  if (hoverLayer) {
    map.removeLayer(hoverLayer)
    hoverLayer = null
  }

  map.off('click', onPointClick)
  map.off('mousemove', onPointMouseMove)

  if (pointLayer) {
    map.removeLayer(pointLayer)
    pointLayer = null
  }

  promptPoints.value = []

  // 进入框选模式
  drawing.value = true
  map.dragging.disable()

  map.off('mousedown', onDrawMouseDown)
  map.on('mousedown', onDrawMouseDown)

  ElMessage.info('请在地图上拖拽框选目标区域')
}

const onDrawMouseDown = (e) => {
  console.log('[BOX] mousedown', e.latlng)
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

const onDrawMouseUp = async (e) => {
  console.log('[AREA] current zoom:', map.getZoom())
  console.log('[BOX] mouseup', e.clientX, e.clientY)

  if (!isDrawing) return

  isDrawing = false
  map.off('mousemove', onDrawMouseMove)
  document.removeEventListener('mouseup', onDrawMouseUp)

  const c = map.getContainer()
  const r = c.getBoundingClientRect()

  const endLatLng = map.containerPointToLatLng(L.point(e.clientX - r.left, e.clientY - r.top))

  if (tempRect) { map.removeLayer(tempRect); tempRect = null }

  const bounds = L.latLngBounds(drawStartLatLng, endLatLng)

  const startPoint = map.latLngToContainerPoint(drawStartLatLng)
  const endPoint = map.latLngToContainerPoint(endLatLng)

  const imageWidth = promptImageWidth.value
  const imageHeight = promptImageHeight.value

  const box = [
    Math.max(0, Math.min(startPoint.x, endPoint.x)),
    Math.max(0, Math.min(startPoint.y, endPoint.y)),
    Math.min(imageWidth - 1, Math.max(startPoint.x, endPoint.x)),
    Math.min(imageHeight - 1, Math.max(startPoint.y, endPoint.y))
  ]
  console.log('[BOX] SAM box:', box)

  if (
    box[2] - box[0] < 10 ||
    box[3] - box[1] < 10
  ) {
    ElMessage.warning('框选区域太小')
    return
  }

  if (!samSessionId.value) {
    throw new Error('SAM Session 不存在')
  }

  const response = await axios.post(
    `/api/sam/session/${samSessionId.value}/box`,
    {
      box,
      multimask_output: true
    },
    {
      responseType: 'blob',
      timeout: 120000
    }
  )

  const maskBlob = response.data

  console.log('[BOX] maskBlob:', maskBlob, maskBlob.type, maskBlob.size)

  const samScore = response.headers['x-sam-score']

  const confidence = samScore
    ? Number(samScore) * 100
    : null

  console.log('[BOX] SAM confidence:', confidence)

  result.value = {
    ...(result.value || {}),
    confidence
  }

  console.log('[AREA] start calculating')

  const area = await calculateMaskArea(
    maskBlob,
    promptZoom.value
  )

  console.log('[BOX] area:', area)

  areaResult.value = area

  if (!(maskBlob instanceof Blob)) {
    throw new Error('SAM Box 返回结果不是 Blob')
  }

  const maskUrl = URL.createObjectURL(maskBlob)

  if (samMaskLayer) {
    map.removeLayer(samMaskLayer)
    samMaskLayer = null
  }

  samMaskLayer = L.imageOverlay(
    maskUrl,
    samImageBounds,
    {
      opacity: 0.45,
      interactive: false
    }
  ).addTo(map)

  ElMessage.success('Box 分割完成')

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

  result.value = null
  autoResult.value = null
}

const promptZoom = ref(null)
const promptImageWidth = ref(0)
const promptImageHeight = ref(0)

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

  promptImageWidth.value = canvas.width
  promptImageHeight.value = canvas.height

  promptZoom.value = map.getZoom()

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

const createSamSession = async () => {
  if (!promptImageBase64) {
    throw new Error('SAM 图像不存在')
  }

  // base64 → Blob
  const response = await fetch(promptImageBase64)
  const blob = await response.blob()

  const formData = new FormData()
  formData.append('image', blob, 'sam-image.png')

  const res = await axios.post(
    '/api/sam/session',
    formData,
    {
      timeout: 120000
    }
  )

  samSessionId.value = res.data.session_id

  console.log('SAM Session 创建成功:', res.data)

  return res.data
}

const calculateMaskArea = (maskBlob, zoom) => {
  return new Promise((resolve, reject) => {
    const image = new Image()
    const url = URL.createObjectURL(maskBlob)

    image.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        canvas.width = image.width
        canvas.height = image.height

        const ctx = canvas.getContext('2d')
        ctx.drawImage(image, 0, 0)

        const imageData = ctx.getImageData(
          0,
          0,
          image.width,
          image.height
        )

        let foregroundPixels = 0

        for (let i = 0; i < imageData.data.length; i += 4) {
          const r = imageData.data[i]
          const g = imageData.data[i + 1]
          const b = imageData.data[i + 2]

          if (r === 255 && g === 255 && b === 255) {
            foregroundPixels++
          }
        }

        const gsdMap = {
          13: 19.2,
          14: 9.6,
          15: 4.8,
          16: 2.4,
          17: 1.2,
          18: 0.6,
          19: 0.3
        }

        const gsd = gsdMap[zoom] || 1.2

        const areaM2 = foregroundPixels * gsd * gsd
        const areaMu = areaM2 / 666.6667

        URL.revokeObjectURL(url)

        resolve({
          foregroundPixels,
          gsd,
          areaM2,
          areaMu
        })
      } catch (error) {
        URL.revokeObjectURL(url)
        reject(error)
      }
    }

    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('无法读取 SAM Mask'))
    }

    image.src = url
  })
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

  drawnItems = L.layerGroup().addTo(map)

  // Automatic检测结果
  autoPolygonLayer = L.layerGroup().addTo(map)
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
