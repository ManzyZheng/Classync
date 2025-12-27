<template>
  <div class="classroom-container">
    <div class="classroom-header">
      <h2>{{ classroom?.name }}</h2>
      <div class="classroom-code">课堂码: {{ classroom?.classCode }}</div>
      <button class="exit-btn" @click="exitClassroom">退出课堂</button>
    </div>
    
    <div class="classroom-content">
      <div class="left-panel">
        <PdfThumbnails 
          :pdf-url="pdfUrl"
          :total-pages="totalPages"
          :current-page="currentPage"
          :host-page="hostPage"
          :is-host="false"
          @page-select="handlePageSelect"
          @jump-to-host="jumpToHostPage"
        />
      </div>
      
      <div class="center-panel">
        <PdfViewer 
          v-if="pdfUrl"
          :pdf-url="pdfUrl"
          :current-page="currentPage"
          :host-page="hostPage"
          :is-host="false"
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
          :is-host="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useClassroomStore } from '../stores/classroom'
import { useUserStore } from '../stores/user'
import api from '../api'
import websocket from '../utils/websocket'
import PdfThumbnails from '../components/PdfThumbnails.vue'
import PdfViewer from '../components/PdfViewer.vue'
import InteractionPanel from '../components/InteractionPanel.vue'

const route = useRoute()
const router = useRouter()
const classroomStore = useClassroomStore()
const userStore = useUserStore()

const classroomId = ref(parseInt(route.params.id))
const classroom = ref(null)
const pdfUrl = ref('')
const currentPage = ref(1)
const hostPage = ref(1)
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
    classroomStore.setIsHost(false)
    
    currentPage.value = data.currentPage || 1
    hostPage.value = data.currentPage || 1
    
    if (data.pdfPath) {
      pdfUrl.value = `/uploads/${data.pdfPath}`
    }
    
    // 记录用户参与（观众身份）
    if (userStore.currentUser) {
      try {
        await api.classroom.recordParticipant(classroomId.value, userStore.currentUser.id)
      } catch (error) {
        console.error('Failed to record participant:', error)
      }
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
      onClassroomState: (payload) => {
        console.log('Classroom state:', payload)
        if (payload.currentPage) {
          hostPage.value = payload.currentPage
        }
      },
      onPageUpdate: (payload) => {
        console.log('Page updated:', payload)
        const pageNumber = payload.pageNumber
        hostPage.value = pageNumber
        // 自动跟随主持人翻页
        currentPage.value = pageNumber
      },
      // ✅ 问题开放：触发WebSocket内部事件系统
      onQuestionOpened: (payload) => {
        console.log('Question opened:', payload)
        // 触发注册的处理器
        const handlers = websocket.eventHandlers.get('QUESTION_OPENED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      // ✅ 问题关闭：触发WebSocket内部事件系统
      onQuestionClosed: (payload) => {
        console.log('Question closed:', payload)
        const handlers = websocket.eventHandlers.get('QUESTION_CLOSED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      // ✅ 问题结束
      onQuestionFinished: (payload) => {
        console.log('Question finished:', payload)
        const handlers = websocket.eventHandlers.get('QUESTION_FINISHED')
        if (handlers) {
          handlers.forEach(h => h(payload))
        }
      },
      // 答案提交
      onAnswerSubmitted: (payload) => {
        console.log('Answer submitted:', payload)
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

const handlePageSelect = (page) => {
  currentPage.value = page
}

const jumpToHostPage = () => {
  currentPage.value = hostPage.value
}

const exitClassroom = () => {
  websocket.disconnect()
  classroomStore.reset()
  router.push('/home')
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
  gap: 24px;
  padding: 16px 32px;
  background: white;
  border-bottom: 1px solid #ddd;
}

.classroom-header h2 {
  font-size: 20px;
  color: #333;
  flex: 1;
}

.classroom-code {
  font-size: 14px;
  color: #667eea;
  font-weight: bold;
}

.exit-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #333;
}

.exit-btn:hover {
  background: #e0e0e0;
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
</style>

