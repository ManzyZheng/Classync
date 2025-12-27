<template>
  <div class="discussion-tab">
    <div class="discussion-header">
      <h3>讨论区</h3>
    </div>
    
    <div class="discussion-list">
      <!-- 只显示顶级评论（非回复） -->
      <div 
        v-for="discussion in topLevelDiscussions"
        :key="discussion.id"
        class="discussion-item"
      >
        <div class="discussion-user">
          {{ discussion.isAnonymous ? '匿名用户' : discussion.userName }}
        </div>
        <div class="discussion-content">
          {{ discussion.content }}
        </div>
        <div class="discussion-footer">
          <span class="discussion-time">{{ formatTime(discussion.createdAt) }}</span>
          <button class="reply-btn" @click="startReply(discussion)">回复</button>
        </div>
        
        <!-- ✅ 回复嵌套显示在原评论下方 -->
        <div v-if="getReplies(discussion.id).length > 0" class="replies">
          <div 
            v-for="reply in getReplies(discussion.id)"
            :key="reply.id"
            class="reply-item"
          >
            <div class="reply-user">
              {{ reply.isAnonymous ? '匿名用户' : reply.userName }}
            </div>
            <div class="reply-content">
              <span class="reply-to">回复 {{ discussion.isAnonymous ? '匿名用户' : discussion.userName }}:</span>
              {{ reply.content }}
            </div>
            <div class="reply-time">{{ formatTime(reply.createdAt) }}</div>
          </div>
        </div>
      </div>
      
      <div v-if="topLevelDiscussions.length === 0" class="empty-state">
        <p>暂无讨论</p>
      </div>
    </div>
    
    <div class="discussion-input">
      <div v-if="replyTo" class="reply-indicator">
        回复 {{ replyTo.isAnonymous ? '匿名用户' : replyTo.userName }}
        <button @click="cancelReply">×</button>
      </div>
      <div class="input-row">
        <textarea 
          v-model="newContent"
          :placeholder="replyTo ? '输入回复内容...' : '输入评论内容...'"
          @keydown.enter.ctrl="sendDiscussion"
        ></textarea>
        <button class="send-btn" @click="sendDiscussion">发送</button>
      </div>
      <label class="anonymous-checkbox">
        <input type="checkbox" v-model="isAnonymous" />
        匿名发布
      </label>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../api'
import websocket from '../utils/websocket'

const props = defineProps({
  classroomId: Number
})

const userStore = useUserStore()

const discussions = ref([])
const newContent = ref('')
const isAnonymous = ref(false)
const replyTo = ref(null)

onMounted(() => {
  loadDiscussions()
  // ✅ 监听WebSocket讨论更新
  setupWebSocketListeners()
})

onUnmounted(() => {
  cleanupWebSocketListeners()
})

// ✅ 只获取顶级评论（没有replyToId的）
const topLevelDiscussions = computed(() => {
  return discussions.value.filter(d => !d.replyToId)
})

// ✅ WebSocket监听器
const setupWebSocketListeners = () => {
  websocket.on('DISCUSSION_NEW', handleDiscussionNew)
}

const cleanupWebSocketListeners = () => {
  websocket.off('DISCUSSION_NEW', handleDiscussionNew)
}

// ✅ 处理新讨论（实时更新，不重新请求）
const handleDiscussionNew = (payload) => {
  console.log('[DiscussionTab] New discussion:', payload)
  // 重新加载讨论列表以获取完整数据
  loadDiscussions()
}

const loadDiscussions = async () => {
  try {
    const data = await api.discussion.getByClassroom(props.classroomId)
    discussions.value = data
  } catch (error) {
    console.error('Failed to load discussions:', error)
  }
}

// 获取某条评论的所有回复
const getReplies = (discussionId) => {
  return discussions.value.filter(d => d.replyToId === discussionId)
}

const startReply = (discussion) => {
  replyTo.value = discussion
}

const cancelReply = () => {
  replyTo.value = null
}

const sendDiscussion = async () => {
  if (!newContent.value.trim()) return
  
  try {
    const data = {
      classroomId: props.classroomId,
      userId: userStore.currentUser.id,
      content: newContent.value,
      replyToId: replyTo.value?.id || null,
      isAnonymous: isAnonymous.value
    }
    
    await api.discussion.create(data)
    
    // ✅ 发送WebSocket通知
    websocket.sendDiscussionNew(props.classroomId, {
      userId: userStore.currentUser.id,
      userName: isAnonymous.value ? '匿名用户' : userStore.currentUser.name,
      isAnonymous: isAnonymous.value
    })
    
    newContent.value = ''
    replyTo.value = null
    isAnonymous.value = false
    
    // 重新加载以获取完整数据（包括ID和时间戳）
    loadDiscussions()
  } catch (error) {
    console.error('Failed to send discussion:', error)
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString()
}
</script>

<style scoped>
.discussion-tab {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.discussion-header {
  padding: 16px;
  border-bottom: 1px solid #ddd;
}

.discussion-header h3 {
  font-size: 18px;
  color: #333;
}

.discussion-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.discussion-item {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 6px;
}

.discussion-user {
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
  font-size: 14px;
}

.discussion-content {
  color: #333;
  margin-bottom: 8px;
  line-height: 1.5;
  font-size: 14px;
}

.reply-to {
  color: #667eea;
  margin-right: 4px;
  font-weight: 500;
}

.discussion-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.discussion-time {
  font-size: 12px;
  color: #999;
}

.reply-btn {
  padding: 4px 12px;
  background: transparent;
  color: #667eea;
  font-size: 12px;
}

.reply-btn:hover {
  background: #f0f4ff;
  border-radius: 4px;
}

/* ✅ 回复区样式优化 */
.replies {
  margin-top: 12px;
  padding-left: 16px;
  border-left: 3px solid #667eea;
}

.reply-item {
  margin-bottom: 8px;
  padding: 10px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.reply-user {
  font-weight: bold;
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
}

.reply-content {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
  line-height: 1.4;
}

.reply-time {
  font-size: 11px;
  color: #999;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.discussion-input {
  border-top: 1px solid #ddd;
  padding: 12px;
  background: white;
}

.reply-indicator {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f0f4ff;
  border-radius: 4px;
  margin-bottom: 8px;
  color: #667eea;
  font-size: 14px;
}

.reply-indicator button {
  background: transparent;
  color: #667eea;
  font-size: 18px;
  padding: 0 4px;
}

.input-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.input-row textarea {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  height: 60px;
  font-size: 14px;
}

.input-row textarea:focus {
  outline: none;
  border-color: #667eea;
}

.send-btn {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border-radius: 4px;
  font-size: 14px;
}

.send-btn:hover {
  background: #5568d3;
}

.anonymous-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.anonymous-checkbox input {
  cursor: pointer;
  width: 16px;
  height: 16px;
}
</style>

