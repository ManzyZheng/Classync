<template>
  <div class="danmaku-container" v-if="shouldRender">
    <transition-group name="danmaku-fade" tag="div" class="danmaku-list">
      <div
        v-for="danmaku in activeDanmakus"
        :key="danmaku.id"
        class="danmaku-item"
        :style="{ animationDuration: danmaku.duration + 'ms' }"
      >
        <div class="danmaku-bubble" :class="{ anonymous: danmaku.isAnonymous }">
          <div class="danmaku-avatar">
            {{ getDanmakuAvatar(danmaku.userName) }}
          </div>
          <div class="danmaku-content-wrapper">
            <div class="danmaku-user">
              {{ danmaku.isAnonymous ? '匿名用户' : danmaku.userName }}
            </div>
            <div class="danmaku-content">
              {{ danmaku.content }}
            </div>
          </div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import api from '../api'
import websocket from '../utils/websocket'

const props = defineProps({
  classroomId: {
    type: Number,
    required: true
  },
  enabled: {
    type: Boolean,
    default: true
  }
})

const activeDanmakus = ref([])
const danmakuIdCounter = ref(0)
const maxDisplayTime = 8000 // 8秒，弹幕显示时长
const danmakuTimers = ref(new Map()) // 存储每条弹幕的自动删除计时器

// 计算属性：是否应该渲染
const shouldRender = computed(() => {
  const result = props.classroomId && props.classroomId > 0 && props.enabled
  console.log('[DanmakuDisplay] shouldRender check:', {
    classroomId: props.classroomId,
    enabled: props.enabled,
    result
  })
  return result
})

// 监听 props 变化，确保在 classroomId 加载后再初始化
watch(() => props.classroomId, (newId, oldId) => {
  console.log('[DanmakuDisplay] classroomId changed:', { oldId, newId })
  if (newId && newId > 0 && !oldId) {
    // 第一次有 classroomId 时初始化
    initializeComponent()
  }
}, { immediate: true })

watch(() => shouldRender.value, (should) => {
  console.log('[DanmakuDisplay] shouldRender changed:', should)
  if (should) {
    initializeComponent()
  }
}, { immediate: true })

onMounted(() => {
  console.log('[DanmakuDisplay] ========== Component mounted ==========')
  console.log('[DanmakuDisplay] Props:', {
    classroomId: props.classroomId,
    enabled: props.enabled
  })
  console.log('[DanmakuDisplay] shouldRender:', shouldRender.value)
  
  // 如果已经有 classroomId，立即初始化
  if (props.classroomId && props.classroomId > 0 && props.enabled) {
    console.log('[DanmakuDisplay] Initializing immediately...')
    initializeComponent()
  } else {
    console.log('[DanmakuDisplay] Waiting for classroomId...')
  }
})

const initializeComponent = () => {
  console.log('[DanmakuDisplay] ========== Initializing component ==========')
  console.log('[DanmakuDisplay] classroomId:', props.classroomId)
  console.log('[DanmakuDisplay] enabled:', props.enabled)
  
  if (!props.classroomId || props.classroomId <= 0) {
    console.error('[DanmakuDisplay] ERROR: Invalid classroomId!')
    return
  }
  
  if (!props.enabled) {
    console.warn('[DanmakuDisplay] Component is disabled')
    return
  }
  
  // 添加一个测试弹幕，确认组件能显示
  console.log('[DanmakuDisplay] Adding test danmaku...')
  addDanmaku({
    userName: '系统',
    content: '弹幕功能已启动',
    isAnonymous: false
  })
  
  // 加载讨论数据
  loadDiscussions()
  
  // 设置 WebSocket 监听器
  setupWebSocketListeners()
  
  console.log('[DanmakuDisplay] ✅ Initialization complete')
  console.log('[DanmakuDisplay] Active danmakus:', activeDanmakus.value.length)
}

onUnmounted(() => {
  cleanupWebSocketListeners()
  // 清除所有计时器
  danmakuTimers.value.forEach(timerId => {
    clearTimeout(timerId)
  })
  danmakuTimers.value.clear()
  // 清除所有弹幕
  activeDanmakus.value = []
})

const setupWebSocketListeners = () => {
  console.log('[DanmakuDisplay] ========== Setting up WebSocket listeners ==========')
  console.log('[DanmakuDisplay] WebSocket object:', websocket)
  console.log('[DanmakuDisplay] WebSocket connected:', websocket?.connected)
  
  // 直接注册监听器，不管是否已连接
  // 因为 websocket.on() 会在连接后自动工作
  try {
    websocket.on('DISCUSSION_NEW', handleDiscussionNew)
    console.log('[DanmakuDisplay] ✅ WebSocket listener registered successfully')
    console.log('[DanmakuDisplay] Handler function:', handleDiscussionNew)
    
    // 验证注册是否成功
    const handlers = websocket.eventHandlers?.get('DISCUSSION_NEW')
    console.log('[DanmakuDisplay] Registered handlers:', handlers)
    console.log('[DanmakuDisplay] Handlers count:', handlers?.length || 0)
  } catch (error) {
    console.error('[DanmakuDisplay] ❌ Failed to register WebSocket listener:', error)
  }
}

const cleanupWebSocketListeners = () => {
  console.log('[DanmakuDisplay] Cleaning up WebSocket listeners')
  websocket.off('DISCUSSION_NEW', handleDiscussionNew)
}

const handleDiscussionNew = async (payload) => {
  console.log('[DanmakuDisplay] ========== New discussion received ==========')
  console.log('[DanmakuDisplay] Payload:', payload)
  console.log('[DanmakuDisplay] Props enabled:', props.enabled)
  console.log('[DanmakuDisplay] Props classroomId:', props.classroomId)
  
  if (!props.enabled) {
    console.log('[DanmakuDisplay] Component disabled, ignoring new discussion')
    return
  }
  
  if (!props.classroomId) {
    console.warn('[DanmakuDisplay] No classroomId, cannot load latest discussion')
    return
  }
  
  console.log('[DanmakuDisplay] Loading discussions from API...')
  
  // 加载最新的讨论以获取完整内容
  try {
    const discussions = await api.discussion.getByClassroom(props.classroomId)
    console.log('[DanmakuDisplay] Loaded discussions after new message:', discussions)
    console.log('[DanmakuDisplay] Discussions count:', discussions?.length || 0)
    
    if (discussions && discussions.length > 0) {
      // 只显示顶级评论（非回复）
      const topLevelDiscussions = discussions.filter(d => !d.replyToId)
      console.log('[DanmakuDisplay] Top level discussions count:', topLevelDiscussions.length)
      
      if (topLevelDiscussions.length > 0) {
        const latestDiscussion = topLevelDiscussions[topLevelDiscussions.length - 1]
        console.log('[DanmakuDisplay] Latest discussion:', latestDiscussion)
        
        // 检查是否已经显示过这条弹幕（通过内容判断，简单去重）
        const alreadyExists = activeDanmakus.value.some(
          d => d.content === latestDiscussion.content && 
               (Date.now() - d.timestamp) < 5000 // 5秒内的相同内容认为是重复的
        )
        
        if (!alreadyExists) {
          console.log('[DanmakuDisplay] Adding latest discussion as danmaku')
          addDanmaku(latestDiscussion)
        } else {
          console.log('[DanmakuDisplay] Discussion already exists, skipping')
        }
      } else {
        console.log('[DanmakuDisplay] No top level discussion found')
      }
    } else {
      console.log('[DanmakuDisplay] No discussions found')
    }
  } catch (error) {
    console.error('[DanmakuDisplay] Failed to load latest discussion:', error)
    console.error('[DanmakuDisplay] Error details:', error.message, error.stack)
  }
}

const loadDiscussions = async () => {
  if (!props.enabled) {
    console.log('[DanmakuDisplay] Component disabled, skipping load')
    return
  }
  
  if (!props.classroomId) {
    console.warn('[DanmakuDisplay] No classroomId, cannot load discussions')
    return
  }
  
  console.log('[DanmakuDisplay] ========== Loading discussions ==========')
  console.log('[DanmakuDisplay] Classroom ID:', props.classroomId)
  
  try {
    const discussions = await api.discussion.getByClassroom(props.classroomId)
    console.log('[DanmakuDisplay] ✅ Loaded discussions:', discussions)
    console.log('[DanmakuDisplay] Discussions count:', discussions?.length || 0)
    
    if (discussions && discussions.length > 0) {
      // 只加载最近的非回复讨论（最多5条）
      const topLevelDiscussions = discussions
        .filter(d => !d.replyToId)
        .slice(-5)
      
      console.log('[DanmakuDisplay] Top level discussions count:', topLevelDiscussions.length)
      console.log('[DanmakuDisplay] Top level discussions:', topLevelDiscussions)
      
      if (topLevelDiscussions.length > 0) {
        // 立即显示所有弹幕，不延迟
        topLevelDiscussions.forEach((discussion, index) => {
          console.log(`[DanmakuDisplay] Adding discussion ${index + 1}/${topLevelDiscussions.length}:`, discussion)
          addDanmaku(discussion)
        })
      } else {
        console.log('[DanmakuDisplay] No top level discussions found')
      }
    } else {
      console.log('[DanmakuDisplay] No discussions found in response')
    }
  } catch (error) {
    console.error('[DanmakuDisplay] ❌ Failed to load discussions:', error)
    console.error('[DanmakuDisplay] Error stack:', error.stack)
  }
}

const addDanmaku = (discussion) => {
  if (!discussion || !discussion.content) {
    console.warn('[DanmakuDisplay] Invalid discussion data:', discussion)
    return
  }
  
  // 检查是否已经存在相同的弹幕（防止重复）
  const exists = activeDanmakus.value.some(
    d => d.content === discussion.content && 
         (Date.now() - d.timestamp) < 5000 // 5秒内的相同内容认为是重复的
  )
  
  if (exists) {
    console.log('[DanmakuDisplay] Duplicate danmaku detected, skipping')
    return
  }
  
  // 根据内容长度设置显示时长，确保用户有足够时间阅读
  const contentLength = discussion.content.length
  const baseDuration = 4000 // 基础显示时间 4 秒
  const additionalDuration = Math.min(contentLength * 50, 4000) // 每个字符增加 50ms，最多额外 4 秒
  const duration = baseDuration + additionalDuration
  
  const danmaku = {
    id: danmakuIdCounter.value++,
    userName: discussion.userName || '匿名用户',
    content: discussion.content,
    isAnonymous: discussion.isAnonymous || false,
    timestamp: Date.now(), // 记录创建时间
    duration: duration // 显示时长
  }
  
  console.log('[DanmakuDisplay] ✅ Adding danmaku:', danmaku)
  console.log('[DanmakuDisplay] Content length:', danmaku.content.length, 'Duration:', duration)
  
  // 新弹幕添加到数组末尾
  activeDanmakus.value.push(danmaku)
  
  console.log('[DanmakuDisplay] Active danmakus count:', activeDanmakus.value.length)
  
  // 设置自动删除计时器
  const timerId = setTimeout(() => {
    removeDanmaku(danmaku.id)
    danmakuTimers.value.delete(danmaku.id)
  }, duration)
  
  danmakuTimers.value.set(danmaku.id, timerId)
}
获取用户昵称的首字母作为头像
const getDanmakuAvatar = (userName) => {
  if (!userName || userName === '匿名用户') return '?'
  return userName.charAt(0).toUpperCase()
// 滚动到底部（显示最新弹幕）
const scrollToBottom = () => {
  if (scrollAreaRef.value) {
    // 使用 requestAnimationFrame 确保DOM更新后再滚动
    requestAnimationFrame(() => {
      scrollAreaRef.value.scrollTop = scrollAreaRef.value.scrollHeight
    })
  }
}
</script>

<style scoped>
.danmaku-container {
  position: fixed;
  left: 24px;
  bottom: 24px;
  width: 420px;
  max-width: calc(100vw - 48px);
  max-height: 400px;
  pointer-events: none;
  z-index: 1001; /* 确保在问题覆盖层之上 */
  /* 添加可见性，便于调试 */
  min-height: 50px;
  /* 临时添加背景色，确保可见 */
  background: rgba(255, 0, 0, 0.1);
  border:0px;
  bottom: 20px;
  width: calc(100% - 40px);
  max-width: 500px;
  pointer-events: none;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.danmaku-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.danmaku-item {
  animation: danmakuPush var(--danmaku-duration, 8s) ease-in forwards;
  width: fit-content;
  max-width: 100%;
}

.danmaku-bubble {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 20px;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  word-wrap: break-word;
  word-break: break-word;
  pointer-events: auto;
  cursor: default;
}

.danmaku-bubble:hover {
  background: rgba(0, 0, 0, 0.75);
  box-shadow: 
    0 6px 20px rgba(0, 0, 0, 0.45),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
}

.danmaku-bubble.anonymous {
  background: rgba(60, 60, 60, 0.65);
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
  background: linear-gradient(135deg, #999999 0%, #666666 100%);
  box-shadow: 0 2px 8px rgba(100, 100, 100, 0.3);
}

.danmaku-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.danmaku-user {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.danmaku-content {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  line-height: 1.4;
  word-wrap: break-word;
  word-break: break-word;
  max-width: 450px;
  font-weight: 400;
}

/* 弹幕上升和淡出的动画 */
@keyframes danmakuPush {
  0% {
    opacity: 1;
    transform: translateY(0);
  }
  85% {
    opacity: 1;
    transform: translateY(-100px);
  }
  100% {
    opacity: 0;
    transform: translateY(-120px);
  }
}

/* 新增动画：淡入淡出 */
.danmaku-fade-enter-active {
  animation: danmakuFadeIn 0.3s ease-out;
}

.danmaku-fade-leave-active {
  animation: danmakuFadeOut 0.3s ease-in;
}

@keyframes danmakuFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes danmakuFadeOut {
  from {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
  to {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .danmaku-container {
    left: 16px;
    bottom: 16px;
    max-width: calc(100vw - 32px);
  }
  
  .danmaku-bubble {
    padding: 9px 14px;
    border-radius: 18px;
  }
  
  .danmaku-avatar {
    width: 28px;
    height: 28px;
    font-size: 12px;
  }
  
  .danmaku-user {
    font-size: 11px;
  }
  
  .danmaku-content {
    font-size: 13px;
    max-width: 400px;
  }
}

@media (max-width: 480px) {
  .danmaku-container {
    left: 12px;
    bottom: 12px;
    max-width: calc(100vw - 24px);
  }
  
  .danmaku-bubble {
    padding: 8px 12px;
    border-radius: 16px;
  }
  
  .danmaku-avatar {
    width: 26px;
    height: 26px;
    font-size: 11px;
  }
  
  .danmaku-user {
    font-size: 10px;
  }
  
  .danmaku-content {
    font-size: 12px;
    max-width: 30