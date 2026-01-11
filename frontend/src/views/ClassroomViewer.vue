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
          :page-locks="pageLocks"
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
          :page-locks="pageLocks"
          @page-change="handlePageChange"
          @pages-loaded="totalPages = $event"
          @update:currentPage="currentPage = $event"
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
const pageLocks = ref({})  // 页面锁定状态

onMounted(async () => {
  await loadClassroom()
  await connectWebSocket()
})

onUnmounted(() => {
  websocket.disconnect()
})

const loadClassroom = async () => {
  try {
    // 先检查课堂状态
    const statusData = await api.classroom.getStatus(classroomId.value)
    console.log('[ClassroomViewer] Classroom status:', statusData.status)
    
    if (statusData.status === 'NOT_STARTED') {
      alert('课堂尚未开始，请稍后再来')
      router.push('/home')
      return
    } else if (statusData.status === 'ENDED') {
      alert('课堂已结束')
      router.push('/home')
      return
    }
    
    // 状态正常，加载课堂数据
    const data = await api.classroom.getById(classroomId.value)
    classroom.value = data
    classroomStore.setClassroom(data)
    classroomStore.setIsHost(false)
    
    currentPage.value = data.currentPage || 1
    hostPage.value = data.currentPage || 1
    
    if (data.pdfPath) {
      pdfUrl.value = `/uploads/${data.pdfPath}`
    }
    
    // 加载页面锁定状态（如果失败则使用空对象）
    try {
      const locks = await api.pageLock.getByClassroom(classroomId.value)
      pageLocks.value = locks || {}
    } catch (lockError) {
      console.warn('Failed to load page locks, using empty state:', lockError)
      pageLocks.value = {}
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
        // 加载页面锁定状态
        if (payload.pageLocks) {
          pageLocks.value = payload.pageLocks
        }
      },
      onPageUpdate: (payload) => {
        console.log('Page updated:', payload)
        const pageNumber = payload.pageNumber
        hostPage.value = pageNumber
        // 自动跟随主持人翻页
        currentPage.value = pageNumber
      },
      // ✅ 页面锁定更新
      onPageLockUpdate: (payload) => {
        console.log('[Viewer] Page lock updated:', payload)
        const { pageNumber, isLocked } = payload
        pageLocks.value[pageNumber] = isLocked
      },
      // ✅ 批量锁定
      onPageLockBatch: (payload) => {
        console.log('[Viewer] Batch lock:', payload)
        const { fromPage } = payload
        for (let page = fromPage; page <= totalPages.value; page++) {
          pageLocks.value[page] = true
        }
      },
      // ✅ 批量解锁（瞬间完成）
      onPageLockBatchUnlock: (payload) => {
        console.log('[Viewer]  Batch unlock:', payload)
        const { fromPage } = payload
        // 从 fromPage 开始解锁所有页面
        for (let page = fromPage; page <= totalPages.value; page++) {
          pageLocks.value[page] = false
        }
        console.log('[Viewer]  All pages unlocked instantly!')
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
  // 检查页面是否被锁定
  if (pageLocks.value[page]) {
    console.log(`Cannot navigate to locked page ${page}`)
    return
  }
  currentPage.value = page
}

const jumpToHostPage = () => {
  // 跳转到主持人页面时也要检查锁定状态
  if (!pageLocks.value[hostPage.value]) {
    currentPage.value = hostPage.value
  }
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

