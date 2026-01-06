<template>
  <div class="interaction-only-container">
    <div class="mobile-header">
      <div class="classroom-info">
        <h2>{{ classroom?.name }}</h2>
        <div class="classroom-code">课堂码: {{ classroom?.classCode }}</div>
      </div>
    </div>
    
    <div class="interaction-content">
      <InteractionPanel 
        v-if="classroomId"
        :classroom-id="classroomId"
        :is-host="false"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'
import websocket from '../utils/websocket'
import InteractionPanel from '../components/InteractionPanel.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const classroomId = ref(parseInt(route.params.id))
const classroom = ref(null)

onMounted(async () => {
  // 检查是否已登录
  if (!userStore.currentUser) {
    router.push({
      path: '/login',
      query: { redirect: `/interaction/${classroomId.value}` }
    })
    return
  }
  
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
      // ✅ 问题开放：触发WebSocket内部事件系统
      onQuestionOpened: (payload) => {
        console.log('Question opened:', payload)
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

// 移除 exitClassroom 函数，用户使用手机浏览器的返回按钮退出
</script>

<style scoped>
.interaction-only-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.classroom-info {
  text-align: center;
  width: 100%;
}

.classroom-info h2 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.classroom-code {
  font-size: 12px;
  color: #667eea;
  font-weight: 500;
}

.interaction-content {
  flex: 1;
  overflow: hidden;
  background: white;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .mobile-header {
    padding: 10px 12px;
  }
  
  .classroom-info h2 {
    font-size: 15px;
  }
  
  .classroom-code {
    font-size: 11px;
  }
}
</style>

