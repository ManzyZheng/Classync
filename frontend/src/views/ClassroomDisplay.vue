<template>
  <div class="display-container">
    <!-- PDF 展示 -->
    <div class="pdf-display" :class="{ 'hidden-by-overlay': showQuestionOverlay }">
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
    
    <!-- 问题覆盖层 -->
    <QuestionOverlay 
      :visible="showQuestionOverlay"
      :question-id="displayQuestionId"
      :mode="displayQuestionMode"
      :sub-question-index="displaySubQuestionIndex"
    />
    
    <!-- 课堂码和二维码浮层 -->
    <transition name="fade">
      <div v-if="showClassroomCode && classroomCode" class="classroom-code-overlay">
        <div class="code-label">课堂码</div>
        <div class="code-value">{{ classroomCode }}</div>
        <div class="qrcode-container">
          <canvas ref="qrcodeCanvas"></canvas>
        </div>
      </div>
    </transition>
    
    <!-- 弹幕显示 -->
    <DanmakuDisplay 
      :classroom-id="classroomId || 0"
      :enabled="true"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import websocket from '../utils/websocket'
import PdfViewer from '../components/PdfViewer.vue'
import QuestionOverlay from '../components/QuestionOverlay.vue'
import DanmakuDisplay from '../components/DanmakuDisplay.vue'
import QRCode from 'qrcode'

const route = useRoute()
const router = useRouter()

const classroomId = ref(null) // 初始化为 null，等待从路由参数加载
const pdfUrl = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const showClassroomCode = ref(false)  // 展示课堂码开关状态
const classroomCode = ref('')  // 课堂码
const qrcodeCanvas = ref(null)  // 二维码 canvas 引用

// 问题展示状态
const showQuestionOverlay = ref(false)
const displayQuestionId = ref(null)
const displayQuestionMode = ref('QUESTION_ONLY')
const displaySubQuestionIndex = ref(null)

onMounted(async () => {
  // 先从路由参数获取 classroomId
  const idFromRoute = parseInt(route.params.id)
  if (idFromRoute && !isNaN(idFromRoute)) {
    classroomId.value = idFromRoute
    console.log('[Display] Classroom ID from route:', classroomId.value)
  }
  
  console.log('[Display] onMounted - classroomId before loadClassroom:', classroomId.value)
  
  await loadClassroom()
  
  console.log('[Display] onMounted - classroomId after loadClassroom:', classroomId.value)
  
  await connectWebSocket()
  
  console.log('[Display] onMounted - classroomId after connectWebSocket:', classroomId.value)
})

onUnmounted(() => {
  websocket.disconnect()
})

const loadClassroom = async () => {
  // 如果还没有 classroomId，从路由获取
  if (!classroomId.value) {
    const idFromRoute = parseInt(route.params.id)
    if (idFromRoute && !isNaN(idFromRoute)) {
      classroomId.value = idFromRoute
    }
  }
  
  if (!classroomId.value) {
    console.error('[Display] No classroom ID available')
    alert('无效的课堂ID')
    router.push('/home')
    return
  }
  
  try {
    const data = await api.classroom.getById(classroomId.value)
    console.log('[Display] Classroom data loaded:', data)
    
    // 获取初始页码
    currentPage.value = data.currentPage || 1
    
    // 保存课堂码
    classroomCode.value = data.classCode || ''
    console.log('[Display] Classroom code set to:', classroomCode.value)
    
    // 加载 PDF
    if (data.pdfPath) {
      pdfUrl.value = `/uploads/${data.pdfPath}`
    }
    
    // 初始化问题展示状态
    if (data.displayQuestionId) {
      displayQuestionId.value = data.displayQuestionId
      displayQuestionMode.value = data.displayQuestionMode || 'QUESTION_ONLY'
      showQuestionOverlay.value = true
      console.log('[Display] Initial question display:', displayQuestionId.value, displayQuestionMode.value)
    }
  } catch (error) {
    console.error('Failed to load classroom:', error)
    alert('加载课堂失败')
    router.push('/home')
  }
}

const connectWebSocket = async () => {
  if (!classroomId.value) {
    console.error('[Display] Cannot connect WebSocket: no classroom ID')
    return
  }
  
  try {
    await websocket.connect(classroomId.value, {
      // ✅ 订阅 CLASSROOM_STATE 事件
      onClassroomState: (payload) => {
        console.log('[Display] Classroom state:', payload)
        if (payload.currentPage) {
          currentPage.value = payload.currentPage
        }
        // 恢复问题展示状态
        if (payload.displayQuestionId) {
          displayQuestionId.value = payload.displayQuestionId
          displayQuestionMode.value = payload.displayQuestionMode || 'QUESTION_ONLY'
          showQuestionOverlay.value = true
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
        console.log('[Display] Display code toggle received:', payload)
        console.log('[Display] Current classroomCode:', classroomCode.value)
        console.log('[Display] Current showClassroomCode:', showClassroomCode.value)
        
        if (payload.show !== undefined) {
          showClassroomCode.value = payload.show
          console.log('[Display] Updated showClassroomCode to:', showClassroomCode.value)
          
          // 当显示时，等待 DOM 更新后生成二维码
          if (payload.show && classroomCode.value) {
            nextTick(() => {
              console.log('[Display] Generating QR code after nextTick')
              console.log('[Display] Canvas element:', qrcodeCanvas.value)
              generateQRCode()
            })
          }
        }
      },
      // ✅ 订阅 DISPLAY_QUESTION 事件
      onDisplayQuestion: (payload) => {
        console.log('[Display] Display question event:', payload)
        if (payload.questionId) {
          displayQuestionId.value = payload.questionId
          displayQuestionMode.value = payload.mode || 'QUESTION_ONLY'
          displaySubQuestionIndex.value = payload.subQuestionIndex ?? null
          showQuestionOverlay.value = true
        } else {
          // 清除问题展示
          showQuestionOverlay.value = false
          displayQuestionId.value = null
          displayQuestionMode.value = 'QUESTION_ONLY'
          displaySubQuestionIndex.value = null
        }
      },
      // ✅ 订阅 DISCUSSION_NEW 事件（用于弹幕显示）
      onDiscussionNew: (payload) => {
        console.log('[Display] Discussion new event received (for danmaku):', payload)
        // 这个回调会触发弹幕组件的监听器
      }
    })
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
  }
}

// 生成二维码
const generateQRCode = async () => {
  console.log('[Display] generateQRCode called')
  console.log('[Display] Canvas ref:', qrcodeCanvas.value)
  console.log('[Display] Classroom code:', classroomCode.value)
  
  if (!qrcodeCanvas.value || !classroomCode.value) {
    console.warn('[Display] Cannot generate QR code - canvas or code missing')
    return
  }
  
  try {
    // 生成二维码内容：完整的 URL
    // 开发环境：使用局域网 IP（手机可以扫码访问）
    // 生产环境：使用当前域名
    let baseUrl
    if (import.meta.env.DEV) {
      // 开发环境：使用 localhost（仅电脑本地测试）
      baseUrl = 'http://localhost:5173'
      // 或者使用局域网 IP（手机可以扫码访问）
      // baseUrl = 'http://172.29.5.89:5173'
    } else {
      // 生产环境：使用当前访问的域名
      baseUrl = window.location.origin
    }
    
    const qrContent = `${baseUrl}/join/${classroomCode.value}`
    console.log('[Display] QR code content:', qrContent)
    
    await QRCode.toCanvas(qrcodeCanvas.value, qrContent, {
      width: 200,
      margin: 2,
      color: {
        dark: '#000000',
        light: '#FFFFFF'
      }
    })
    console.log('[Display] QR code generated successfully')
  } catch (error) {
    console.error('[Display] Failed to generate QR code:', error)
  }
}

// 监听课堂码变化，自动生成二维码
watch([classroomCode, showClassroomCode], ([code, show]) => {
  if (show && code) {
    // 延迟一下确保 canvas 已经渲染
    setTimeout(() => {
      generateQRCode()
    }, 100)
  }
})
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
  position: fixed;
  top: 0;
  left: 0;
}

.pdf-display {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.3s ease;
}

.pdf-display.hidden-by-overlay {
  opacity: 0;
  pointer-events: none;
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

/* 课堂码和二维码浮层 */
.classroom-code-overlay {
  position: fixed;
  top: 24px;
  right: 24px;
  background: rgba(0, 0, 0, 0.85);
  backdrop-filter: blur(10px);
  padding: 24px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  z-index: 10000;  /* 高于 QuestionOverlay (9999)，确保始终显示在最上层 */
  pointer-events: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  min-width: 240px;
}

.code-label {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  font-weight: 500;
  letter-spacing: 1px;
  margin: 0;
}

.code-value {
  font-size: 36px;
  color: #ffffff;
  font-weight: 700;
  text-align: center;
  letter-spacing: 3px;
  font-family: 'Courier New', monospace;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
  margin: 0;
}

.qrcode-container {
  background: white;
  padding: 12px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  margin-top: 8px;
}

.qrcode-container canvas {
  display: block;
  border-radius: 4px;
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

