<template>
  <div class="join-container">
    <div class="join-card">
      <div class="loading" v-if="loading">
        <div class="spinner"></div>
        <p>正在加入课堂...</p>
      </div>
      <div class="error" v-else-if="error">
        <p>{{ error }}</p>
        <button @click="retry">重试</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const classCode = ref(route.params.classCode)

onMounted(async () => {
  await joinClassroom()
})

const joinClassroom = async () => {
  loading.value = true
  error.value = ''
  
  try {
    // 1. 根据 classCode 查询课堂信息
    const classroom = await api.classroom.getByCode(classCode.value)
    
    if (!classroom || !classroom.id) {
      error.value = '课堂不存在或已结束'
      loading.value = false
      return
    }
    
    // 2. 检查用户是否已登录
    if (!userStore.currentUser) {
      // 未登录，跳转到登录页，并设置重定向
      router.push({
        path: '/login',
        query: { redirect: `/interaction/${classroom.id}` }
      })
      return
    }
    
    // 3. 已登录，记录参与者并跳转到互动页面
    try {
      await api.classroom.recordParticipant(classroom.id, userStore.currentUser.id)
    } catch (err) {
      console.warn('Failed to record participant:', err)
      // 即使记录失败也继续跳转
    }
    
    // 4. 跳转到互动专用页面
    router.push(`/interaction/${classroom.id}`)
    
  } catch (err) {
    console.error('Failed to join classroom:', err)
    error.value = '加入课堂失败，请检查课堂码是否正确'
    loading.value = false
  }
}

const retry = () => {
  joinClassroom()
}
</script>

<style scoped>
.join-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.join-card {
  background: white;
  border-radius: 12px;
  padding: 48px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  min-width: 320px;
  text-align: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading p {
  color: #666;
  font-size: 16px;
}

.error {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.error p {
  color: #e53e3e;
  font-size: 16px;
  margin: 0;
}

.error button {
  padding: 10px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.error button:hover {
  background: #5568d3;
}
</style>

