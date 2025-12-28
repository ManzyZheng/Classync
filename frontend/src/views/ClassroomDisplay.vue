<template>
  <div class="display-container">
    <div class="pdf-display">
      <PdfViewer 
        v-if="pdfUrl"
        :pdf-url="pdfUrl"
        :current-page="currentPage"
        :is-host="false"
        :display-mode="true"
        @pages-loaded="totalPages = $event"
      />
      <div v-else class="no-pdf">
        <p>未上传 PDF 文件</p>
      </div>
    </div>
    
    <!-- 课堂码浮层 -->
    <transition name="fade">
      <div v-if="showClassroomCode && classroomCode" class="classroom-code-overlay">
        <div class="code-label">课堂码</div>
        <div class="code-value">{{ classroomCode }}</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import websocket from '../utils/websocket'
import PdfViewer from '../components/PdfViewer.vue'

const route = useRoute()
const router = useRouter()

const classroomId = ref(parseInt(route.params.id))
const pdfUrl = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const showClassroomCode = ref(false)  // 展示课堂码开关状态
const classroomCode = ref('')  // 课堂码

onMounted(async () => {
  await loadClassroom()
  await connectWebSocket()
})

onUnmounted(() => {
  websocket.disconnect()
})

const loadClassroom = async () => {
  try {
    const data = await api.classroom.getById(classroomId.value)
    
    // 获取初始页码
    currentPage.value = data.currentPage || 1
    
    // 保存课堂码
    classroomCode.value = data.classCode || ''
    
    // 加载 PDF
    if (data.pdfPath) {
      pdfUrl.value = `/uploads/${data.pdfPath}`
    }
  } catch (error) {
    console.error('Failed to load classroom:', error)
    alert('加载课堂失败')
    router.push('/home')
  }
}

const connectWebSocket = async () => {
  try {
    await websocket.connect(classroomId.value, {
      // ✅ 订阅 CLASSROOM_STATE 事件
      onClassroomState: (payload) => {
        console.log('[Display] Classroom state:', payload)
        if (payload.currentPage) {
          currentPage.value = payload.currentPage
        }
      },
      // ✅ 订阅 PAGE_UPDATE 事件
      onPageUpdate: (payload) => {
        console.log('[Display] Page updated:', payload)
        if (payload.pageNumber) {
          currentPage.value = payload.pageNumber
        }
      },
      // ✅ 订阅 DISPLAY_CODE_TOGGLE 事件
      onDisplayCodeToggle: (payload) => {
        console.log('[Display] Display code toggle:', payload)
        if (payload.show !== undefined) {
          showClassroomCode.value = payload.show
        }
      }
    })
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
  }
}
</script>

<style scoped>
.display-container {
  width: 100vw;
  height: 100vh;
  background: #1a1a1a;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
}

.pdf-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 覆盖 PdfViewer 的样式，隐藏工具栏 */
.pdf-display :deep(.pdf-toolbar) {
  display: none;
}

/* 调整 PDF 容器样式，填满整个屏幕 */
.pdf-display :deep(.pdf-viewer) {
  width: 100%;
  height: 100%;
  background: #1a1a1a;
}

.pdf-display :deep(.pdf-canvas-container) {
  width: 100%;
  height: 100%;
  padding: 0;
  margin: 0;
  background: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 让 Canvas 尽可能大地填满屏幕 */
.pdf-display :deep(.pdf-canvas-container canvas) {
  max-width: 100vw !important;
  max-height: 100vh !important;
  width: auto !important;
  height: auto !important;
  box-shadow: none;
  object-fit: contain;
}

.no-pdf {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #888;
  font-size: 24px;
}

/* 课堂码浮层 */
.classroom-code-overlay {
  position: fixed;
  top: 24px;
  right: 24px;
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(10px);
  padding: 20px 32px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  pointer-events: none;
}

.code-label {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 8px;
  text-align: center;
  font-weight: 400;
  letter-spacing: 1px;
}

.code-value {
  font-size: 48px;
  color: #ffffff;
  font-weight: 700;
  text-align: center;
  letter-spacing: 4px;
  font-family: 'Courier New', monospace;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

