<template>
  <div class="pdf-viewer">
    <div class="pdf-toolbar">
      <button 
        @click="prevPage" 
        :disabled="isPrevDisabled"
        :class="{ 'locked-btn': !props.isHost && props.pageLocks[currentPageNum.value - 1] }"
      >
        上一页
      </button>
      <span>{{ currentPageNum }} / {{ totalPagesNum }}</span>
      <button 
        @click="nextPage" 
        :disabled="isNextDisabled"
        :class="{ 'locked-btn': !props.isHost && props.pageLocks[currentPageNum.value + 1] }"
      >
        下一页
      </button>
    </div>
    
    <div class="pdf-canvas-container">
      <canvas id="pdf-canvas" ref="pdfCanvas"></canvas>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-if="error" class="error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, computed } from 'vue'
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf'
import pdfWorker from 'pdfjs-dist/legacy/build/pdf.worker?url'

// 设置 worker（Vite 官方推荐方式）
pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker

const props = defineProps({
  pdfUrl: String,
  currentPage: Number,
  hostPage: Number,
  isHost: Boolean,
  displayMode: Boolean,  // 大屏展示模式
  pageLocks: {  // 页面锁定状态
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['page-change', 'pages-loaded', 'update:currentPage'])

// ❗核心：PDF.js 对象用 let，不用 ref
let pdfDoc = null
let canvas = null
let isRendering = false
let currentRenderTask = null // 用于取消渲染任务

// 只有 UI 相关的状态用 ref
const pdfCanvas = ref(null)
const currentPageNum = ref(1)
const totalPagesNum = ref(0)
const loading = ref(false)
const error = ref('')

// 计算上一页/下一页按钮是否禁用
const isPrevDisabled = computed(() => {
  if (currentPageNum.value <= 1) return true
  // 如果是学生端，检查上一页是否被锁定
  if (!props.isHost && props.pageLocks[currentPageNum.value - 1]) {
    return true
  }
  return false
})

const isNextDisabled = computed(() => {
  if (currentPageNum.value >= totalPagesNum.value) return true
  // 如果是学生端，检查下一页是否被锁定
  if (!props.isHost && props.pageLocks[currentPageNum.value + 1]) {
    return true
  }
  return false
})

onMounted(() => {
  canvas = pdfCanvas.value
  
  if (props.pdfUrl) {
    loadPdf(props.pdfUrl)
  }
})

onUnmounted(() => {
  // 清理
  if (pdfDoc) {
    pdfDoc.destroy()
    pdfDoc = null
  }
})

watch(() => props.pdfUrl, (newUrl) => {
  if (newUrl) {
    loadPdf(newUrl)
  }
})

watch(() => props.currentPage, (newPage) => {
  if (newPage && newPage !== currentPageNum.value && !isRendering) {
    currentPageNum.value = newPage
    renderPage(newPage)
  }
})

async function loadPdf(url) {
  loading.value = true
  error.value = ''
  
  try {
    // 如果已有文档，先销毁
    if (pdfDoc) {
      pdfDoc.destroy()
    }
    
    // 加载 PDF
    const loadingTask = pdfjsLib.getDocument(url)
    pdfDoc = await loadingTask.promise
    
    totalPagesNum.value = pdfDoc.numPages
    
    // 通知父组件总页数
    emit('pages-loaded', pdfDoc.numPages)
    
    if (props.currentPage) {
      currentPageNum.value = props.currentPage
    }
    
    await renderPage(currentPageNum.value)
    loading.value = false
  } catch (err) {
    console.error('Failed to load PDF:', err)
    error.value = 'PDF 加载失败: ' + err.message
    loading.value = false
  }
}

async function renderPage(pageNum) {
  if (!pdfDoc || !canvas || isRendering) return
  
  // 取消之前的渲染任务（防抖）
  if (currentRenderTask) {
    currentRenderTask.cancel()
    currentRenderTask = null
  }
  
  isRendering = true
  
  try {
    // 获取页面（❗不要存储 page 对象）
    const page = await pdfDoc.getPage(pageNum)
    
    // ✅ 自适应容器宽度
    const container = canvas.parentElement
    const containerWidth = container?.clientWidth || 800
    const containerHeight = container?.clientHeight || 600
    
    // 获取原始页面尺寸
    const originalViewport = page.getViewport({ scale: 1 })
    
    // 计算自适应缩放比例
    let scaleX, scaleY, scale
    
    if (props.displayMode) {
      // 大屏展示模式：无边距，无最大缩放限制
      scaleX = containerWidth / originalViewport.width
      scaleY = containerHeight / originalViewport.height
      scale = Math.min(scaleX, scaleY)
    } else {
      // 普通模式：留出边距，限制最大缩放
      scaleX = (containerWidth - 32) / originalViewport.width  // 留16px边距
      scaleY = (containerHeight - 32) / originalViewport.height
      scale = Math.min(scaleX, scaleY, 2.5) // 最大不超过2.5倍
    }
    
    const dpr = window.devicePixelRatio || 1
    const viewport = page.getViewport({ scale })
    
    // ✅ Canvas 物理像素（高分辨率）
    canvas.width = Math.floor(viewport.width * dpr)
    canvas.height = Math.floor(viewport.height * dpr)
    
    // ✅ CSS 显示尺寸（逻辑像素）
    canvas.style.width = `${viewport.width}px`
    canvas.style.height = `${viewport.height}px`
    
    const ctx = canvas.getContext('2d')
    
    // ✅ 图像平滑优化
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    
    // ✅ 关键：缩放绘制上下文
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    
    // 渲染
    currentRenderTask = page.render({
      canvasContext: ctx,
      viewport: viewport
    })
    
    await currentRenderTask.promise
    currentRenderTask = null
    
    isRendering = false
  } catch (err) {
    if (err.name === 'RenderingCancelledException') {
      console.log('Rendering cancelled for page', pageNum)
    } else {
      console.error('Failed to render page:', err)
      error.value = '页面渲染失败: ' + err.message
    }
    isRendering = false
    currentRenderTask = null
  }
}

function prevPage() {
  if (currentPageNum.value > 1 && !isRendering) {
    const targetPage = currentPageNum.value - 1
    // 学生端检查页面是否被锁定
    if (!props.isHost && props.pageLocks[targetPage]) {
      console.log(`Page ${targetPage} is locked, cannot navigate`)
      return
    }
    currentPageNum.value = targetPage
    renderPage(currentPageNum.value)
    if (props.isHost) {
      emit('page-change', currentPageNum.value)
    } else {
      // 学生端也需要触发更新，以便缩略图显示蓝框
      emit('update:currentPage', currentPageNum.value)
    }
  }
}

function nextPage() {
  if (currentPageNum.value < totalPagesNum.value && !isRendering) {
    const targetPage = currentPageNum.value + 1
    // 学生端检查页面是否被锁定
    if (!props.isHost && props.pageLocks[targetPage]) {
      console.log(`Page ${targetPage} is locked, cannot navigate`)
      return
    }
    currentPageNum.value = targetPage
    renderPage(currentPageNum.value)
    if (props.isHost) {
      emit('page-change', currentPageNum.value)
    } else {
      // 学生端也需要触发更新，以便缩略图显示蓝框
      emit('update:currentPage', currentPageNum.value)
    }
  }
}

function jumpToHostPage() {
  if (!isRendering) {
    currentPageNum.value = props.hostPage
    renderPage(props.hostPage)
  }
}
</script>

<style scoped>
.pdf-viewer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.pdf-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border-bottom: 1px solid #ddd;
}

.pdf-toolbar button {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border-radius: 4px;
}

.pdf-toolbar button:disabled {
  background: #ccc;
  cursor: not-allowed;
  opacity: 0.5;
}

/* 锁定页面的按钮样式 */
.pdf-toolbar button.locked-btn {
  background: #999;
  cursor: not-allowed;
  opacity: 0.6;
}

.pdf-toolbar button.locked-btn::after {
  content: ' 🔒';
}

.pdf-toolbar span {
  color: #333;
  font-weight: 500;
}

.pdf-canvas-container {
  flex: 1;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  position: relative;
}

.pdf-canvas-container canvas {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  max-width: 100%;
  height: auto;
}

.loading, .error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 20px 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loading {
  color: #667eea;
}

.error {
  color: #f44336;
}
</style>

