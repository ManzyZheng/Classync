<template>
  <div class="pdf-thumbnails">
    <h3>缩略图</h3>
    <div class="thumbnails-list" v-if="totalPages > 0">
      <div 
        v-for="page in totalPages"
        :key="page"
        :class="['thumbnail-item', { 
          active: page === currentPage,
          host: !isHost && page === hostPage 
        }]"
        @click="$emit('page-select', page)"
      >
        <div class="thumbnail-wrapper">
          <div class="page-number-badge">{{ page }}</div>
          <canvas 
            :ref="el => setCanvasRef(el, page)"
            :id="`thumbnail-${page}`"
            class="thumbnail-canvas"
          ></canvas>
          <span v-if="!isHost && page === hostPage" class="host-indicator">★</span>
        </div>
      </div>
    </div>
    <div v-else class="loading-thumbnails">
      <p>加载中...</p>
    </div>
    <button 
      v-if="!isHost && currentPage !== hostPage"
      class="jump-btn"
      @click="$emit('jump-to-host')"
    >
      跳转到当前页面
    </button>
  </div>
</template>

<script setup>
import { onMounted, watch, onUnmounted, nextTick } from 'vue'
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf'
import pdfWorker from 'pdfjs-dist/legacy/build/pdf.worker?url'

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker

const props = defineProps({
  pdfUrl: String,
  totalPages: Number,
  currentPage: Number,
  hostPage: Number,
  isHost: Boolean
})

defineEmits(['page-select', 'jump-to-host'])

// ❗使用 let 存储 PDF 对象
let pdfDoc = null
let canvasRefs = {}
let isLoading = false

onMounted(() => {
  if (props.pdfUrl) {
    loadThumbnails()
  }
})

onUnmounted(() => {
  if (pdfDoc) {
    pdfDoc.destroy()
    pdfDoc = null
  }
  canvasRefs = {}
})

watch(() => props.pdfUrl, (newUrl) => {
  if (newUrl && !isLoading) {
    loadThumbnails()
  }
})

watch(() => props.totalPages, (newTotal) => {
  // 当总页数更新时，如果 PDF 已加载但缩略图未渲染，则渲染
  if (newTotal && pdfDoc && !isLoading) {
    nextTick(() => {
      renderAllThumbnails()
    })
  }
})

function setCanvasRef(el, pageNum) {
  if (el) {
    canvasRefs[pageNum] = el
    
    // 如果 PDF 已加载，立即渲染这个缩略图
    if (pdfDoc && !isLoading) {
      nextTick(() => {
        renderThumbnail(pageNum)
      })
    }
  }
}

async function loadThumbnails() {
  if (!props.pdfUrl || isLoading) return
  
  isLoading = true
  
  try {
    if (pdfDoc) {
      pdfDoc.destroy()
    }
    
    const loadingTask = pdfjsLib.getDocument(props.pdfUrl)
    pdfDoc = await loadingTask.promise
    
    // 等待 DOM 更新后再渲染
    await nextTick()
    
    // 渲染所有缩略图
    await renderAllThumbnails()
    
    isLoading = false
  } catch (err) {
    console.error('Failed to load thumbnails:', err)
    isLoading = false
  }
}

async function renderAllThumbnails() {
  const totalPages = props.totalPages || pdfDoc?.numPages || 0
  
  for (let pageNum = 1; pageNum <= totalPages; pageNum++) {
    await renderThumbnail(pageNum)
  }
}

async function renderThumbnail(pageNum) {
  if (!pdfDoc) return
  
  const canvas = canvasRefs[pageNum]
  if (!canvas) {
    console.log(`Canvas for page ${pageNum} not found yet`)
    return
  }
  
  try {
    const page = await pdfDoc.getPage(pageNum)
    
    // ✅ 缩略图固定宽度（自适应高度）
    const thumbnailWidth = 140  // 固定缩略图宽度
    const originalViewport = page.getViewport({ scale: 1 })
    const scale = thumbnailWidth / originalViewport.width
    
    const dpr = window.devicePixelRatio || 1
    const viewport = page.getViewport({ scale })
    
    // ✅ Canvas 物理像素
    canvas.width = Math.floor(viewport.width * dpr)
    canvas.height = Math.floor(viewport.height * dpr)
    
    // ✅ CSS 显示尺寸
    canvas.style.width = `${viewport.width}px`
    canvas.style.height = `${viewport.height}px`
    
    const ctx = canvas.getContext('2d')
    
    // ✅ 图像平滑（缩略图用medium即可）
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'medium'
    
    // ✅ 缩放绘制上下文
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    
    await page.render({
      canvasContext: ctx,
      viewport: viewport
    }).promise
    
    console.log(`Thumbnail ${pageNum} rendered successfully`)
  } catch (err) {
    console.error(`Failed to render thumbnail ${pageNum}:`, err)
  }
}
</script>

<style scoped>
.pdf-thumbnails {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
  border-right: 1px solid #ddd;
  padding: 12px 8px;
}

.pdf-thumbnails h3 {
  font-size: 14px;
  margin-bottom: 12px;
  color: #333;
  padding: 0 8px;
}

.thumbnails-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 0 4px;
}

.loading-thumbnails {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
}

.thumbnail-item {
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 4px;
  flex-shrink: 0;
}

.thumbnail-wrapper {
  position: relative;
  padding: 4px;
  border: 2px solid #ddd;
  border-radius: 4px;
  background: white;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-item:hover .thumbnail-wrapper {
  border-color: #667eea;
  background: #f0f4ff;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
}

.thumbnail-item.active .thumbnail-wrapper {
  border-color: #667eea;
  border-width: 3px;
  background: #f0f4ff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.thumbnail-item.host .thumbnail-wrapper {
  border-color: #ff9800;
  background: #fff8f0;
}

.page-number-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 500;
  z-index: 2;
  line-height: 1.4;
}

.thumbnail-canvas {
  display: block;
  border-radius: 2px;
  max-width: 100%;
  height: auto;
}

.host-indicator {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #ff9800;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.jump-btn {
  margin-top: 12px;
  padding: 8px;
  background: #ff9800;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  width: calc(100% - 8px);
  margin-left: 4px;
  margin-right: 4px;
  font-weight: 500;
  flex-shrink: 0;
}

.jump-btn:hover {
  background: #f57c00;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* 滚动条美化 */
.thumbnails-list::-webkit-scrollbar {
  width: 6px;
}

.thumbnails-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.thumbnails-list::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.thumbnails-list::-webkit-scrollbar-thumb:hover {
  background: #999;
}
</style>

