<template>
  <div class="home-container">
    <div class="header">
      <h1>Classync</h1>
      <div class="user-info">
        <span>欢迎, {{ userStore.currentUser?.name }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </div>
    
    <div class="content">
      <div class="section join-section">
        <h2>加入课堂</h2>
        <div class="join-form">
          <input 
            v-model="classCode" 
            type="text" 
            placeholder="请输入课堂码"
            @keyup.enter="joinClassroom"
          />
          <button @click="joinClassroom">加入</button>
        </div>
      </div>
      
      <div class="section create-section">
        <div class="section-header">
          <h2>我创建的课堂</h2>
          <button class="create-btn" @click="showCreateModal = true">
            + 创建课堂
          </button>
        </div>
        
        <div class="classroom-list">
          <div 
            v-for="classroom in myClassrooms" 
            :key="classroom.id"
            class="classroom-item"
            @click="enterClassroom(classroom)"
          >
            <div class="classroom-info">
              <h3>{{ classroom.name }}</h3>
              <p class="classroom-time">
                {{ formatTime(classroom.startTime) }} - {{ formatTime(classroom.endTime) }}
              </p>
            </div>
            <div class="classroom-code">
              课堂码: <strong>{{ classroom.classCode }}</strong>
            </div>
          </div>
          
          <div v-if="myClassrooms.length === 0" class="empty-state">
            <p>暂无创建的课堂</p>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 创建课堂弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="closeCreateModal">
      <div class="modal-content create-modal" @click.stop>
        <h3>创建课堂</h3>
        
        <div class="form-group">
          <label>课堂名称</label>
          <input v-model="newClassroom.name" type="text" placeholder="请输入课堂名称" />
        </div>
        
        <div class="form-group">
          <label>开始时间</label>
          <input v-model="newClassroom.startTime" type="datetime-local" />
        </div>
        
        <div class="form-group">
          <label>结束时间</label>
          <input v-model="newClassroom.endTime" type="datetime-local" />
        </div>
        
        <div class="form-group">
          <label>上传 PDF</label>
          <input type="file" accept=".pdf" @change="handleFileSelect" />
          <p v-if="selectedFile" class="file-name">{{ selectedFile.name }}</p>
        </div>
        
        <div class="modal-actions">
          <button class="cancel-btn" @click="closeCreateModal">取消</button>
          <button class="submit-btn" @click="createClassroom">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const classCode = ref('')
const myClassrooms = ref([])
const showCreateModal = ref(false)
const selectedFile = ref(null)

const newClassroom = ref({
  name: '',
  startTime: '',
  endTime: ''
})

onMounted(() => {
  userStore.loadUser()
  if (!userStore.currentUser) {
    router.push('/login')
    return
  }
  loadMyClassrooms()
})

const loadMyClassrooms = async () => {
  try {
    const classrooms = await api.classroom.getByHostUser(userStore.currentUser.id)
    myClassrooms.value = classrooms
  } catch (error) {
    console.error('Failed to load classrooms:', error)
  }
}

const joinClassroom = async () => {
  if (!classCode.value) {
    alert('请输入课堂码')
    return
  }
  
  try {
    const classroom = await api.classroom.getByCode(classCode.value)
    router.push(`/classroom/viewer/${classroom.id}`)
  } catch (error) {
    console.error('Failed to join classroom:', error)
    alert('课堂码无效或课堂不存在')
  }
}

const enterClassroom = (classroom) => {
  router.push(`/classroom/host/${classroom.id}`)
}

const handleFileSelect = (event) => {
  selectedFile.value = event.target.files[0]
}

const createClassroom = async () => {
  if (!newClassroom.value.name || !newClassroom.value.startTime || !newClassroom.value.endTime) {
    alert('请填写完整信息')
    return
  }
  
  try {
    const classroom = await api.classroom.create({
      name: newClassroom.value.name,
      startTime: newClassroom.value.startTime,
      endTime: newClassroom.value.endTime,
      hostUserId: userStore.currentUser.id
    })
    
    // 上传 PDF
    if (selectedFile.value) {
      await api.classroom.uploadPdf(classroom.id, selectedFile.value)
    }
    
    alert('课堂创建成功！课堂码：' + classroom.classCode)
    closeCreateModal()
    loadMyClassrooms()
  } catch (error) {
    console.error('Failed to create classroom:', error)
    alert('创建课堂失败')
  }
}

const closeCreateModal = () => {
  showCreateModal.value = false
  newClassroom.value = { name: '', startTime: '', endTime: '' }
  selectedFile.value = null
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.home-container {
  width: 100%;
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: white;
  padding: 16px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h1 {
  font-size: 24px;
  color: #667eea;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info span {
  color: #333;
}

.logout-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  color: #333;
}

.logout-btn:hover {
  background: #e0e0e0;
}

.content {
  max-width: 1200px;
  margin: 32px auto;
  padding: 0 32px;
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 24px;
}

.section {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.section h2 {
  font-size: 20px;
  margin-bottom: 16px;
  color: #333;
}

.join-form {
  display: flex;
  gap: 8px;
}

.join-form input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.join-form button {
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border-radius: 4px;
}

.join-form button:hover {
  background: #5568d3;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.create-btn {
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border-radius: 4px;
  font-size: 14px;
}

.create-btn:hover {
  background: #5568d3;
}

.classroom-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.classroom-item {
  padding: 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.classroom-item:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.classroom-info h3 {
  font-size: 16px;
  margin-bottom: 4px;
  color: #333;
}

.classroom-time {
  font-size: 14px;
  color: #666;
}

.classroom-code {
  font-size: 14px;
  color: #666;
}

.classroom-code strong {
  color: #667eea;
  font-size: 16px;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-content h3 {
  margin-bottom: 20px;
  color: #333;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.file-name {
  margin-top: 8px;
  color: #667eea;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.cancel-btn, .submit-btn {
  flex: 1;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
}

.cancel-btn {
  background: #f5f5f5;
  color: #333;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.submit-btn {
  background: #667eea;
  color: white;
}

.submit-btn:hover {
  background: #5568d3;
}

.create-section {
  grid-column: span 1;
}
</style>

