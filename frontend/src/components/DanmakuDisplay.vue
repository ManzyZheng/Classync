<template>
  <div class="danmaku-container">
    <!-- 调试信息 - 已注释
    <div style="position: fixed; top: 10px; left: 10px; background: rgba(255,0,0,0.7); color: white; padding: 10px; z-index: 10001; border-radius: 5px; font-size: 12px; max-width: 200px; pointer-events: auto;">
      <div>DanmakuDisplay Loaded</div>
      <div>ClassroomId: {{ classroomId }}</div>
      <div>Active: {{ activeDanmakus.length }}</div>
      <div>Ready: {{ isReady ? 'yes' : 'no' }}</div>
      <button @click="() => { console.log('Test clicked'); addDanmaku({userName: 'Test', content: 'Test message', isAnonymous: false}) }" style="margin-top: 5px; padding: 5px; cursor: pointer; background: white; color: red; border: 1px solid white;">Test</button>
    </div>
    -->
    
    <!-- 弹幕内容 -->
    <div v-for="danmaku in activeDanmakus" :key="danmaku.id" class="danmaku-item">
      <div class="danmaku-bubble" :class="{ anonymous: danmaku.isAnonymous }">
        <div class="danmaku-avatar">{{ getDanmakuAvatar(danmaku.userName) }}</div>
        <div class="danmaku-content-wrapper">
          <div class="danmaku-user">{{ danmaku.isAnonymous ? '匿名用户' : danmaku.userName }}</div>
          <div class="danmaku-content">{{ danmaku.content }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import api from '../api'
import websocket from '../utils/websocket'

const props = defineProps({
  classroomId: { type: Number, required: false, default: 0 },
  enabled: { type: Boolean, default: true }
})

const activeDanmakus = ref([])
const danmakuIdCounter = ref(0)
const timers = ref(new Map())
const isReady = ref(false)
const displayedDiscussionIds = ref(new Set())  // 跟踪已显示的讨论 ID

// 导出全局状态用于调试
window.__danmakuState = {
  activeDanmakus,
  isReady,
  displayedDiscussionIds,
  classroomId: () => props.classroomId
}

console.log('[DanmakuDisplay] Component script loaded, initial props:', { classroomId: props.classroomId, enabled: props.enabled })
console.log('[DanmakuDisplay] __danmakuState exported to window')

// 立即尝试初始化，不依赖 onMounted
console.log('[DanmakuDisplay] Attempting immediate initialization...')
// 直接调用，让 setupWebSocketListener 自己判断是否需要执行
setupWebSocketListener()

onMounted(() => {
  console.log('[DanmakuDisplay] onMounted called, classroomId:', props.classroomId)
  console.log('[DanmakuDisplay] Checking conditions - classroomId:', props.classroomId, 'enabled:', props.enabled)
  
  // 不管怎样都尝试设置
  if (props.enabled) {
    console.log('[DanmakuDisplay] Component enabled, setting up listener...')
    setupWebSocketListener()
  } else {
    console.warn('[DanmakuDisplay] Component disabled')
  }
})

function setupWebSocketListener() {
  // 如果已经准备过或条件不满足，直接返回
  if (isReady.value || !props.enabled) {
    return
  }
  
  // 如果 classroomId 还没有，稍后再试
  if (!props.classroomId || props.classroomId <= 0) {
    console.log('[DanmakuDisplay] ClassroomId not ready yet, will retry')
    // 设置一个重试
    setTimeout(() => setupWebSocketListener(), 500)
    return
  }
  
  isReady.value = true
  console.log('[DanmakuDisplay] Setting up WebSocket listener for classroom:', props.classroomId)
  
  const handleDiscussionNew = (payload) => {
    console.log('[DanmakuDisplay] *** DISCUSSION_NEW event fired ***', payload)
    loadAndShowLatestDiscussion()
  }
  
  try {
    websocket.on('DISCUSSION_NEW', handleDiscussionNew)
    console.log('[DanmakuDisplay] WebSocket listener registered successfully')
    
    // 测试：添加一条测试弹幕 - 已注释
    // console.log('[DanmakuDisplay] Adding test danmaku')
    // addDanmaku({
    //   userName: 'WebSocket Ready',
    //   content: 'Listener initialized',
    //   isAnonymous: false
    // })
  } catch (err) {
    console.error('[DanmakuDisplay] Error registering listener:', err)
  }
  
  window.__danmakuHandler = handleDiscussionNew
  console.log('[DanmakuDisplay] Handler saved to window.__danmakuHandler')
}

onUnmounted(() => {
  console.log('[DanmakuDisplay] onUnmounted called')
  if (window.__danmakuHandler) {
    websocket.off('DISCUSSION_NEW', window.__danmakuHandler)
  }
  timers.value.forEach(timer => clearTimeout(timer))
  timers.value.clear()
})

const loadAndShowLatestDiscussion = async () => {
  console.log('[DanmakuDisplay] >>> loadAndShowLatestDiscussion called')
  try {
    console.log('[DanmakuDisplay] >>> Fetching discussions for classroomId:', props.classroomId)
    
    const discussions = await api.discussion.getByClassroom(props.classroomId)
    console.log('[DanmakuDisplay] >>> Got discussions:', discussions?.length || 0, 'items')
    
    if (!discussions || discussions.length === 0) {
      console.log('[DanmakuDisplay] >>> No discussions')
      return
    }
    
    // 只过滤顶级讨论（没有 replyToId）
    const topLevelDiscussions = discussions.filter(d => !d.replyToId)
    console.log('[DanmakuDisplay] >>> Top level discussions:', topLevelDiscussions?.length || 0)
    
    if (topLevelDiscussions.length === 0) {
      console.log('[DanmakuDisplay] >>> No top-level discussions')
      return
    }
    
    // 获取最新的顶级讨论
    const latest = topLevelDiscussions[topLevelDiscussions.length - 1]
    console.log('[DanmakuDisplay] >>> Latest top-level discussion:', latest)
    
    // 检查这个讨论是否已经显示过
    if (displayedDiscussionIds.value.has(latest.id)) {
      console.log('[DanmakuDisplay] >>> Discussion already displayed:', latest.id)
      return
    }
    
    // 标记这个讨论为已显示
    displayedDiscussionIds.value.add(latest.id)
    console.log('[DanmakuDisplay] >>> Adding new top-level discussion:', latest.id)
    
    addDanmaku(latest)
  } catch (error) {
    console.error('[DanmakuDisplay] >>> Error:', error)
    console.error('[DanmakuDisplay] >>> Error stack:', error.stack)
  }
}

const addDanmaku = (discussion) => {
  const duration = 30000  // 30秒显示时间
  const danmaku = {
    id: danmakuIdCounter.value++,
    userName: discussion.userName || '匿名用户',
    content: discussion.content,
    isAnonymous: discussion.isAnonymous || false,
    createTime: Date.now()
  }
  
  console.log('[DanmakuDisplay] >>> ADDING DANMAKU:', danmaku)
  activeDanmakus.value.push(danmaku)
  console.log('[DanmakuDisplay] >>> Active count:', activeDanmakus.value.length)
  
  const timer = setTimeout(() => removeDanmaku(danmaku.id), duration)
  timers.value.set(danmaku.id, timer)
}

const removeDanmaku = (id) => {
  const index = activeDanmakus.value.findIndex(d => d.id === id)
  if (index > -1) {
    const danmaku = activeDanmakus.value[index]
    activeDanmakus.value.splice(index, 1)
    const timer = timers.value.get(id)
    if (timer) clearTimeout(timer)
    timers.value.delete(id)
    
    // 移除时也可以从已显示集合中删除，这样如果有重新发送相同讨论的情况可以再次显示
    // 但通常不需要这样做，因为同一个讨论不应该在同一时间显示多次
  }
}

const getDanmakuAvatar = (userName) => {
  if (!userName || userName === '匿名用户') return '?'
  return userName.charAt(0).toUpperCase()
}
</script>

<style scoped>
.danmaku-container {
  position: fixed;
  left: 20px;
  bottom: 20px;
  width: 500px;
  max-width: calc(100vw - 40px);
  pointer-events: none;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.danmaku-item {
  animation: slideUp 30s ease-in forwards;
}

.danmaku-item:nth-child(1) { animation-duration: 30s; }
.danmaku-item:nth-child(2) { animation-duration: 30s; animation-delay: 0.3s; }
.danmaku-item:nth-child(3) { animation-duration: 30s; animation-delay: 0.6s; }
.danmaku-item:nth-child(n+4) { animation-duration: 30s; animation-delay: 0.9s; }

@keyframes slideUp {
  0% { opacity: 1; transform: translateY(0); }
  85% { opacity: 1; transform: translateY(-80px); }
  100% { opacity: 0; transform: translateY(-100px); }
}

.danmaku-bubble {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.68);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  pointer-events: auto;
}

.danmaku-bubble:hover {
  background: rgba(0, 0, 0, 0.78);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
}

.danmaku-bubble.anonymous {
  background: rgba(50, 50, 50, 0.68);
}

.danmaku-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.danmaku-bubble.anonymous .danmaku-avatar {
  background: linear-gradient(135deg, #888888 0%, #555555 100%);
  box-shadow: 0 2px 8px rgba(80, 80, 80, 0.3);
}

.danmaku-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.danmaku-user {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.danmaku-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 400px;
}

@media (max-width: 768px) {
  .danmaku-container {
    left: 16px;
    bottom: 16px;
    width: calc(100vw - 32px);
  }
  .danmaku-bubble { padding: 8px 12px; border-radius: 16px; }
  .danmaku-avatar { width: 28px; height: 28px; font-size: 12px; }
  .danmaku-user { font-size: 11px; }
  .danmaku-content { font-size: 13px; max-width: 350px; }
}

@media (max-width: 480px) {
  .danmaku-container {
    left: 12px;
    bottom: 12px;
    width: calc(100vw - 24px);
  }
  .danmaku-bubble { padding: 7px 10px; border-radius: 14px; }
  .danmaku-avatar { width: 24px; height: 24px; font-size: 10px; }
  .danmaku-user { font-size: 10px; }
  .danmaku-content { font-size: 12px; max-width: 280px; }
}
</style>