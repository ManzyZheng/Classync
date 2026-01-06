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
      <button 
        class="show-code-toggle" 
        @click="toggleShowClassroomCode" 
        :class="{ active: showClassroomCode }"
        title="展示课堂码和二维码"
      >
        <span class="toggle-icon">{{ showClassroomCode ? '👁️' : '👁️‍🗨️' }}</span>
        <span class="toggle-text">课堂码</span>
      </button>
      <button class="present-btn" @click="handlePresent" title="放映">
        <span class="present-icon">▶</span>
        <span>放映</span>
      </button>
    </div>
    
    <div class="classroom-content">
      <div class="left-panel">
        <!-- 批量锁定开关 -->
        <div class="lock-control-bar">
          <div class="lock-toggle-control">
            <span class="lock-label">锁定后续页面</span>
            <label class="switch">
              <input 
                type="checkbox" 
                v-model="lockFollowingPages"
                @change="toggleLockFollowingPages"
              >
              <span class="slider"></span>
            </label>
          </div>
        </div>
        
        <PdfThumbnails 
          :pdf-url="pdfUrl"
          :total-pages="totalPages"
          :current-page="currentPage"
          :host-page="currentPage"
          :is-host="true"
          :page-locks="pageLocks"
          @page-select="handlePageSelect"
          @lock-toggle="handleLockToggle"
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
const showClassroomCode = ref(false)  // 展示课堂码开关状态
const pageLocks = ref({})  // 页面锁定状态 { pageNumber: isLocked }
const lockFollowingPages = ref(false)  // 锁定后续页面开关状态

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
    
    // 加载页面锁定状态（如果失败则使用空对象）
    try {
      const locks = await api.pageLock.getByClassroom(classroomId.value)
      pageLocks.value = locks || {}
    } catch (lockError) {
      console.warn('Failed to load page locks, using empty state:', lockError)
      pageLocks.value = {}
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
  // 检查是否需要自动解锁
  if (pageLocks.value[page]) {
    handleAutoUnlock(page)
  }
  
  currentPage.value = page
  websocket.sendPageChange(classroomId.value, page)
}

const handlePageSelect = (page) => {
  // 检查是否需要自动解锁
  if (pageLocks.value[page]) {
    handleAutoUnlock(page)
  }
  
  currentPage.value = page
  websocket.sendPageChange(classroomId.value, page)
}

// 切换单个页面的锁定状态
const handleLockToggle = async (pageNumber) => {
  try {
    const result = await api.pageLock.toggle(classroomId.value, pageNumber)
    pageLocks.value[pageNumber] = result.isLocked
    
    // 广播锁定状态变化
    websocket.sendPageLockToggle(classroomId.value, pageNumber, result.isLocked)
    console.log(`Page ${pageNumber} lock toggled:`, result.isLocked)
  } catch (error) {
    console.error('Failed to toggle page lock:', error)
  }
}

// 切换锁定后续页面开关
const toggleLockFollowingPages = async () => {
  if (lockFollowingPages.value) {
    // 开启：锁定当前页之后的所有页面
    if (currentPage.value >= totalPages.value) {
      lockFollowingPages.value = false
      alert('当前已是最后一页')
      return
    }
    
    const fromPage = currentPage.value + 1
    
    try {
      await api.pageLock.lockFrom(classroomId.value, fromPage, totalPages.value)
      
      // 更新本地状态
      for (let page = fromPage; page <= totalPages.value; page++) {
        pageLocks.value[page] = true
      }
      
      // 广播批量锁定
      websocket.sendPageLockBatch(classroomId.value, fromPage)
      console.log(`Locked pages from ${fromPage} to ${totalPages.value}`)
    } catch (error) {
      console.error('Failed to lock pages:', error)
      lockFollowingPages.value = false
      alert('批量锁定失败')
    }
  } else {
    // 关闭：批量解锁所有页面（一次 HTTP + 一次 WebSocket）🚀
    try {
      console.log('🚀 Batch unlocking all pages...')
      
      // ✅ 一次 HTTP 请求，一次 SQL
      const result = await api.pageLock.unlockAll(classroomId.value)
      console.log(`✅ Unlocked ${result.count} pages in one request`)
      
      // ✅ 本地状态批量更新
      Object.keys(pageLocks.value).forEach(page => {
        pageLocks.value[page] = false
      })
      
      // ✅ 一次 WebSocket 广播
      websocket.sendPageLockBatchUnlock(classroomId.value, 1)
      
      console.log('🎉 Batch unlock completed instantly!')
    } catch (error) {
      console.error('Failed to unlock pages:', error)
      lockFollowingPages.value = true
      alert('解锁失败')
    }
  }
}

// 自动解锁（教师翻到锁定页时）
const handleAutoUnlock = async (pageNumber) => {
  try {
    await api.pageLock.toggle(classroomId.value, pageNumber)
    pageLocks.value[pageNumber] = false
    
    // 广播解锁
    websocket.sendPageLockToggle(classroomId.value, pageNumber, false)
    console.log(`Page ${pageNumber} auto-unlocked`)
  } catch (error) {
    console.error('Failed to auto-unlock page:', error)
  }
}

const exitClassroom = () => {
  websocket.disconnect()
  classroomStore.reset()
  router.push('/home')
}

const handlePresent = () => {
  // 在新窗口中打开展示页面
  const displayUrl = `/classroom/display/${classroomId.value}`
  window.open(displayUrl, '_blank', 'fullscreen=yes')
  console.log('Opening display view:', displayUrl)
}

const toggleShowClassroomCode = () => {
  showClassroomCode.value = !showClassroomCode.value
  console.log('[Host] Toggle classroom code display:', showClassroomCode.value)
  console.log('[Host] Classroom ID:', classroomId.value)
  console.log('[Host] Classroom code:', classroom.value?.classCode)
  
  // 通过 WebSocket 广播状态变化
  websocket.sendDisplayCodeToggle(classroomId.value, showClassroomCode.value)
  console.log('[Host] WebSocket message sent')
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

.show-code-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #f5f5f5;
  color: #666;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.show-code-toggle:hover {
  background: #ebebeb;
  border-color: #d0d0d0;
  color: #333;
}

.show-code-toggle.active {
  background: #e8f0fe;
  border-color: #667eea;
  color: #667eea;
}

.show-code-toggle.active:hover {
  background: #d2e3fc;
  border-color: #5568d3;
}

.toggle-icon {
  font-size: 16px;
}

.toggle-text {
  font-size: 13px;
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
  display: flex;
  flex-direction: column;
}

.lock-control-bar {
  padding: 12px 8px;
  background: #f9f9f9;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.lock-toggle-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.lock-label {
  font-size: 12px;
  color: #333;
  font-weight: 500;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  flex-shrink: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 24px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

input:checked + .slider {
  background-color: #ff9800;
}

input:focus + .slider {
  box-shadow: 0 0 1px #ff9800;
}

input:checked + .slider:before {
  transform: translateX(20px);
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

