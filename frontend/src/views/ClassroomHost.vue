<template>
  <div class="classroom-container">
    <div class="classroom-header">
      <button class="back-btn" @click="exitClassroom" title="返回">
        <span class="back-icon">←</span>
      </button>
      <h2 class="classroom-title">{{ classroom?.name }}</h2>
      <div class="header-info">
        <div class="info-item">
          <span class="info-icon">📅</span>
          <span>{{ formatTimeRange(classroom?.startTime, classroom?.endTime) }}</span>
        </div>
        <div class="info-separator">|</div>
        <div class="info-item">
          <span class="info-icon">#</span>
          <span>{{ classroom?.classCode }}</span>
        </div>
      </div>
      <button class="present-btn" @click="handlePresent" title="放映">
        <span class="present-icon">▶</span>
        <span>放映</span>
      </button>
    </div>
    
    <div class="classroom-content">
      <div class="left-panel">
        <PdfThumbnails 
          :pdf-url="pdfUrl"
          :total-pages="totalPages"
          :current-page="currentPage"
          :host-page="currentPage"
          :is-host="true"
          @page-select="handlePageSelect"
        />
      </div>
      
      <div class="center-panel">
        <PdfViewer 
          v-if="pdfUrl"
          :pdf-url="pdfUrl"
          :current-page="currentPage"
          :is-host="true"
          @page-change="handlePageChange"
          @pages-loaded="totalPages = $event"
        />
        <div v-else class="no-pdf">
          <p>未上传 PDF 文件</p>
        </div>
      </div>
      
      <div class="right-panel">
        <InteractionPanel 
          :classroom-id="classroomId"
          :is-host="true"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useClassroomStore } from '../stores/classroom'
import api from '../api'
import websocket from '../utils/websocket'
import PdfThumbnails from '../components/PdfThumbnails.vue'
import PdfViewer from '../components/PdfViewer.vue'
import InteractionPanel from '../components/InteractionPanel.vue'

const route = useRoute()
const router = useRouter()
const classroomStore = useClassroomStore()

const classroomId = ref(parseInt(route.params.id))
const classroom = ref(null)
const pdfUrl = ref('')
const currentPage = ref(1)
const totalPages = ref(0)

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
    classroom.value = data
    classroomStore.setClassroom(data)
    classroomStore.setIsHost(true)
    currentPage.value = data.currentPage || 1
    
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
      onPageUpdate: (payload) => {
        console.log('Page updated:', payload)
      },
      onQuestionOpened: (payload) => {
        console.log('Question opened:', payload)
        const handlers = websocket.eventHandlers.get('QUESTION_OPENED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      onQuestionClosed: (payload) => {
        console.log('Question closed:', payload)
        const handlers = websocket.eventHandlers.get('QUESTION_CLOSED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      onQuestionFinished: (payload) => {
        console.log('Question finished:', payload)
        const handlers = websocket.eventHandlers.get('QUESTION_FINISHED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      onAnswerSubmitted: (payload) => {
        console.log('[ClassroomHost] Answer submitted:', payload)
        console.log('[ClassroomHost] Payload details:', JSON.stringify(payload))
        const handlers = websocket.eventHandlers.get('ANSWER_SUBMITTED')
        console.log('[ClassroomHost] Handlers count:', handlers ? handlers.size : 0)
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      // ✅ 讨论更新
      onDiscussionNew: (payload) => {
        console.log('Discussion new:', payload)
        const handlers = websocket.eventHandlers.get('DISCUSSION_NEW')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      }
    })
  } catch (error) {
    console.error('Failed to connect WebSocket:', error)
  }
}

const handlePageChange = (page) => {
  currentPage.value = page
  websocket.sendPageChange(classroomId.value, page)
}

const handlePageSelect = (page) => {
  currentPage.value = page
  websocket.sendPageChange(classroomId.value, page)
}

const exitClassroom = () => {
  websocket.disconnect()
  classroomStore.reset()
  router.push('/home')
}

const handlePresent = () => {
  // 放映功能暂未实现
  console.log('Present button clicked')
}

const formatTimeRange = (start, end) => {
  if (!start || !end) return ''
  const startDate = new Date(start)
  const endDate = new Date(end)
  
  const dateStr = startDate.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
  
  const endDateStr = endDate.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    year: endDate.getFullYear() !== startDate.getFullYear() ? 'numeric' : undefined
  })
  
  return `${dateStr} - ${endDateStr}`
}
</script>

<style scoped>
.classroom-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.classroom-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.back-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.back-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.back-icon {
  font-size: 20px;
  font-weight: 600;
}

.classroom-title {
  font-size: 18px;
  font-weight: 500;
  color: #333;
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
  margin-right: 16px;
  font-size: 14px;
  color: #666;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-icon {
  font-size: 16px;
  opacity: 0.7;
}

.info-separator {
  color: #ddd;
  font-size: 14px;
}

.present-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.present-btn:hover {
  background: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.present-icon {
  font-size: 14px;
}

.classroom-content {
  flex: 1;
  display: grid;
  grid-template-columns: 12% 60% 28%;
  gap: 0;
  overflow: hidden;
}

.left-panel, .center-panel, .right-panel {
  height: 100%;
  overflow: hidden;
}

.left-panel {
  border-right: 1px solid #ddd;
}

.right-panel {
  border-left: 1px solid #ddd;
}

.no-pdf {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: white;
}

.no-pdf p {
  color: #999;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .classroom-header {
    padding: 10px 16px;
    gap: 12px;
  }
  
  .classroom-title {
    font-size: 16px;
  }
  
  .header-info {
    display: none;
  }
  
  .present-btn {
    padding: 8px 16px;
    font-size: 13px;
  }
  
  .present-btn span:not(.present-icon) {
    display: none;
  }
  
  .present-icon {
    font-size: 16px;
  }
}

@media (max-width: 1024px) {
  .header-info {
    gap: 8px;
    font-size: 13px;
  }
  
  .info-item {
    gap: 4px;
  }
}
</style>

