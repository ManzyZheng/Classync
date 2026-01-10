<template>
  <div class="home-container">
    <!-- 顶部导航栏 -->
    <header class="header">
      <h1 class="logo">Classync</h1>
      <div class="user-info">
        <span class="username">欢迎, {{ userStore.currentUser?.name }}</span>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </header>
    
    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 左侧栏：观众相关 -->
      <aside class="left-sidebar">
        <!-- 模块 ①：加入课堂 -->
        <section class="card join-card">
          <h2 class="card-title">加入课堂</h2>
          <div class="join-form">
            <input 
              v-model="classCode" 
              type="text" 
              class="input-field"
              placeholder="请输入课堂码"
              @keyup.enter="joinClassroom"
            />
            <button class="btn btn-primary" @click="joinClassroom">
              加入课堂
            </button>
          </div>
          <p v-if="joinError" class="error-message">{{ joinError }}</p>
        </section>
        
        <!-- 模块 ②：我作为观众加入过的课堂 -->
        <section class="card history-card">
          <h2 class="card-title">我加入过的课堂</h2>
          <div class="classroom-list">
            <div 
              v-for="classroom in participantClassrooms" 
              :key="classroom.id"
              class="classroom-item-compact"
              @click="enterClassroomAsViewer(classroom)"
            >
              <div class="classroom-main">
                <h3 class="classroom-name">{{ classroom.name }}</h3>
                <p class="classroom-time">
                  {{ formatTime(classroom.startTime) }} - {{ formatTime(classroom.endTime) }}
                </p>
              </div>
              <div class="classroom-item-right">
                <div class="classroom-meta">
                  <span class="badge badge-viewer">观众</span>
                  <span :class="['badge', `badge-${getClassroomStatus(classroom)}`]">
                    {{ getClassroomStatusText(classroom) }}
                  </span>
                </div>
                <button 
                  class="delete-btn"
                  @click.stop="deleteParticipantHistory(classroom)"
                  title="删除历史记录"
                >
                  🗑️
                </button>
              </div>
            </div>
            
            <div v-if="participantClassrooms.length === 0" class="empty-state">
              <p>你还没有作为观众加入任何课堂</p>
            </div>
          </div>
        </section>
      </aside>
      
      <!-- 右侧主内容区：主持人相关 -->
      <section class="right-main">
        <!-- 模块 ③：我创建的课堂 -->
        <div class="card host-card">
          <div class="card-header">
            <h2 class="card-title">我创建的课堂</h2>
            <button class="btn btn-primary" @click="showCreateModal = true">
              + 创建课堂
            </button>
          </div>
          
          <div class="classroom-list-main">
            <div 
              v-for="classroom in myClassrooms" 
              :key="classroom.id"
              class="classroom-row"
              @click="enterClassroomAsHost(classroom)"
            >
              <div class="classroom-left">
                <h3 class="classroom-name-large">{{ classroom.name }}</h3>
                <div class="classroom-details">
                  <span class="detail-item">课堂码: <strong>{{ classroom.classCode }}</strong></span>
                  <span class="detail-item">{{ formatTimeRange(classroom.startTime, classroom.endTime) }}</span>
                </div>
              </div>
              
              <div class="classroom-right">
                <span :class="['status-badge', `status-${getClassroomStatus(classroom)}`]">
                  {{ getClassroomStatusIcon(classroom) }} {{ getClassroomStatusText(classroom) }}
                </span>
                <button 
                  class="btn-icon"
                  @click.stop="showMoreActions(classroom, $event)"
                  title="更多操作"
                  ref="menuButton"
                >
                  ⋯
                </button>
              </div>
            </div>
            
            <div v-if="myClassrooms.length === 0" class="empty-state">
              <p>暂无创建的课堂</p>
              <p class="empty-hint">点击右上角"+ 创建课堂"开始</p>
            </div>
          </div>
        </div>
      </section>
    </main>
    
    <!-- 创建课堂弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click="closeCreateModal">
      <div class="modal-content create-modal" @click.stop>
        <h3 class="modal-title">创建课堂</h3>
        
        <!-- 创建方式选择 -->
        <div class="form-group">
          <label class="form-label">创建方式</label>
          <div class="create-mode-selector">
            <button 
              class="mode-btn"
              :class="{ active: createMode === 'new' }"
              @click="createMode = 'new'"
            >
              <span class="mode-icon">➕</span>
              <span>新建课堂</span>
            </button>
            <button 
              class="mode-btn"
              :class="{ active: createMode === 'copy' }"
              @click="createMode = 'copy'"
            >
              <span class="mode-icon">📋</span>
              <span>复制已有课堂</span>
            </button>
          </div>
        </div>
        
        <!-- 新建课堂表单 -->
        <template v-if="createMode === 'new'">
          <div class="form-group">
            <label class="form-label">课堂名称</label>
            <input 
              v-model="newClassroom.name" 
              type="text" 
              class="input-field"
              placeholder="请输入课堂名称" 
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">开始时间</label>
            <input 
              v-model="newClassroom.startTime" 
              type="datetime-local" 
              class="input-field"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">结束时间</label>
            <input 
              v-model="newClassroom.endTime" 
              type="datetime-local" 
              class="input-field"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">上传 PDF</label>
            <input 
              type="file" 
              accept=".pdf" 
              class="input-file"
              @change="handleFileSelect" 
            />
            <p v-if="selectedFile" class="file-name">{{ selectedFile.name }}</p>
          </div>
        </template>
        
        <!-- 复制已有课堂表单 -->
        <template v-if="createMode === 'copy'">
          <!-- 选择源课堂 -->
          <div class="form-group">
            <label class="form-label">选择要复制的课堂</label>
            <select 
              v-model="copySourceClassroomId" 
              class="input-field"
              @change="handleCopySourceChange"
            >
              <option value="">请选择课堂</option>
              <option 
                v-for="classroom in myClassrooms" 
                :key="classroom.id" 
                :value="classroom.id"
              >
                {{ classroom.name }} ({{ formatTimeRange(classroom.startTime, classroom.endTime) }})
              </option>
            </select>
          </div>
          
          <!-- 复制选项 -->
            <div v-if="copySourceClassroomId" class="form-group">
            <label class="form-label">复制选项</label>
            <div class="copy-options">
              <label 
                class="copy-option-item" 
                :class="{ active: copyOption === 'entire' }"
              >
                <input 
                  type="radio" 
                  v-model="copyOption" 
                  value="entire"
                  class="copy-radio"
                />
                <div class="copy-option-content">
                  <span class="copy-option-title">复制整个课堂</span>
                  <span class="copy-option-desc">复制除了课堂时间和观众提交结果的整个课堂（包括文件、所有问题）</span>
                </div>
              </label>
              
              <label 
                class="copy-option-item"
                :class="{ active: copyOption === 'file' }"
              >
                <input 
                  type="radio" 
                  v-model="copyOption" 
                  value="file"
                  class="copy-radio"
                />
                <div class="copy-option-content">
                  <span class="copy-option-title">仅复制文件</span>
                  <span class="copy-option-desc">只复制课堂的PDF文件</span>
                </div>
              </label>
              
              <label 
                class="copy-option-item"
                :class="{ active: copyOption === 'questions' }"
              >
                <input 
                  type="radio" 
                  v-model="copyOption" 
                  value="questions"
                  class="copy-radio"
                />
                <div class="copy-option-content">
                  <span class="copy-option-title">复制所有问题</span>
                  <span class="copy-option-desc">复制课堂中的所有问题（不含提交结果）</span>
                </div>
              </label>
              
              <label 
                class="copy-option-item"
                :class="{ active: copyOption === 'single-question' }"
              >
                <input 
                  type="radio" 
                  v-model="copyOption" 
                  value="single-question"
                  class="copy-radio"
                />
                <div class="copy-option-content">
                  <span class="copy-option-title">复制单个问题</span>
                  <span class="copy-option-desc">选择并复制课堂中的某个特定问题（包括选项）</span>
                </div>
              </label>
            </div>
          </div>
          
          <!-- 选择单个问题 -->
          <div v-if="copyOption === 'single-question' && copySourceQuestions.length > 0" class="form-group">
            <label class="form-label">选择要复制的问题</label>
            <div class="custom-select-wrapper">
              <div 
                class="custom-select"
                :class="{ open: showQuestionDropdown, focused: showQuestionDropdown }"
                @click="toggleQuestionDropdown"
              >
                <div class="custom-select-selected">
                  <span v-if="copySelectedQuestionId">
                    {{ getSelectedQuestionText(copySelectedQuestionId) }}
                  </span>
                  <span v-else class="placeholder">请选择问题</span>
                </div>
                <span class="custom-select-arrow" :class="{ rotated: showQuestionDropdown }">▼</span>
              </div>
              <transition name="dropdown">
                <div v-if="showQuestionDropdown" class="custom-select-options">
                  <div 
                    v-for="question in copySourceQuestions" 
                    :key="question.id"
                    class="custom-select-option"
                    :class="{ selected: copySelectedQuestionId === question.id.toString() }"
                    @click="selectQuestion(question.id)"
                  >
                    <span class="question-type-icon">{{ getQuestionTypeIcon(question.type) }}</span>
                    <span class="question-type-text">{{ getQuestionTypeText(question.type) }}</span>
                    <span class="question-content">{{ question.content.substring(0, 60) }}{{ question.content.length > 60 ? '...' : '' }}</span>
                  </div>
                  <div 
                    v-if="copySourceQuestions.length === 0"
                    class="custom-select-option empty"
                  >
                    暂无问题
                  </div>
                </div>
              </transition>
            </div>
          </div>
          
          <!-- 新建课堂信息 -->
          <div class="form-group">
            <label class="form-label">课堂名称</label>
            <input 
              v-model="newClassroom.name" 
              type="text" 
              class="input-field"
              placeholder="请输入新课堂名称" 
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">开始时间</label>
            <input 
              v-model="newClassroom.startTime" 
              type="datetime-local" 
              class="input-field"
            />
          </div>
          
          <div class="form-group">
            <label class="form-label">结束时间</label>
            <input 
              v-model="newClassroom.endTime" 
              type="datetime-local" 
              class="input-field"
            />
          </div>
          
          <div v-if="copyOption === 'file' || copyOption === 'entire'" class="form-group">
            <label class="form-label">PDF 文件</label>
            <div class="copy-file-info">
              <span class="copy-file-indicator">📄 将从源课堂复制文件</span>
            </div>
          </div>
          
          <div v-if="copyOption !== 'file' && copyOption !== 'entire'" class="form-group">
            <label class="form-label">上传 PDF（可选）</label>
            <input 
              type="file" 
              accept=".pdf" 
              class="input-file"
              @change="handleFileSelect" 
            />
            <p v-if="selectedFile" class="file-name">{{ selectedFile.name }}</p>
          </div>
        </template>
        
        <div class="modal-actions">
          <button class="btn btn-secondary" @click="closeCreateModal">取消</button>
          <button class="btn btn-primary" @click="createClassroom" :disabled="isCreating">
            <span v-if="isCreating">创建中...</span>
            <span v-else>确定</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 更多操作菜单 -->
    <div v-if="showMoreActionsMenu" class="menu-overlay" @click="closeMoreActionsMenu">
      <div class="more-actions-menu" @click.stop :style="{ position: 'fixed' }">
        <div class="menu-header">
          <h3 class="menu-title">{{ selectedClassroomForMenu?.name }}</h3>
          <span :class="['menu-status-badge', `status-${getClassroomStatus(selectedClassroomForMenu)}`]">
            {{ getClassroomStatusIcon(selectedClassroomForMenu) }} {{ getClassroomStatusText(selectedClassroomForMenu) }}
          </span>
        </div>
        
        <div class="menu-info">
          <div class="menu-info-item">
            <span class="menu-icon">📅</span>
            <span>{{ formatTimeRange(selectedClassroomForMenu?.startTime, selectedClassroomForMenu?.endTime) }}</span>
          </div>
          <div class="menu-info-item">
            <span class="menu-icon">👤</span>
            <span>{{ userStore.currentUser?.name }} (您)</span>
          </div>
        </div>
        
        <div class="menu-actions">
          <button class="menu-action-btn primary" @click="copyClassCode($event)">
            <span class="menu-action-icon">#</span>
            <span>复制课堂码</span>
          </button>
          <button class="menu-action-btn danger" @click="deleteClassroom">
            <span class="menu-action-icon">🗑️</span>
            <span>删除</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 复制提示弹窗 -->
    <transition name="toast">
      <div v-if="showCopyToast" class="copy-toast" :style="{ top: copyToastPosition.top + 'px', left: copyToastPosition.left + 'px' }">
        <span class="toast-icon">✓</span>
        <span>已复制</span>
      </div>
    </transition>
    
    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteConfirm" class="modal-overlay" @click="closeDeleteConfirm">
      <div class="confirm-modal" @click.stop>
        <div class="confirm-icon">🗑️</div>
        <h3 class="confirm-title">确认删除</h3>
        <p class="confirm-message">
          确定要删除"<strong>{{ classroomToDelete?.name }}</strong>"的课堂吗？
        </p>
        <p class="confirm-hint">此操作不可恢复</p>
        <div class="confirm-actions">
          <button class="btn btn-secondary" @click="closeDeleteConfirm">取消</button>
          <button class="btn btn-danger" @click="confirmDelete">确定删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

const classCode = ref('')
const joinError = ref('')
const myClassrooms = ref([])
const participantClassrooms = ref([])
const showCreateModal = ref(false)
const selectedFile = ref(null)
const showDeleteConfirm = ref(false)
const classroomToDelete = ref(null)
const showMoreActionsMenu = ref(false)
const selectedClassroomForMenu = ref(null)
const menuButtonRef = ref(null)
const showCopyToast = ref(false)
const copyToastPosition = ref({ top: 0, left: 0 })

// 创建课堂相关
const createMode = ref('new') // 'new' 或 'copy'
const copySourceClassroomId = ref('')
const copyOption = ref('entire') // 'entire', 'file', 'questions', 'single-question'
const copySelectedQuestionId = ref('')
const copySourceQuestions = ref([])
const isCreating = ref(false)
const showQuestionDropdown = ref(false)

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
  loadParticipantClassrooms()
  
  // 点击外部关闭下拉列表
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const handleClickOutside = (event) => {
  const selectWrapper = document.querySelector('.custom-select-wrapper')
  if (selectWrapper && !selectWrapper.contains(event.target)) {
    showQuestionDropdown.value = false
  }
}

const loadMyClassrooms = async () => {
  try {
    const classrooms = await api.classroom.getByHostUser(userStore.currentUser.id)
    myClassrooms.value = classrooms
  } catch (error) {
    console.error('Failed to load classrooms:', error)
  }
}

const loadParticipantClassrooms = async () => {
  try {
    const classrooms = await api.classroom.getByParticipantUser(userStore.currentUser.id)
    participantClassrooms.value = classrooms
  } catch (error) {
    console.error('Failed to load participant classrooms:', error)
  }
}

const joinClassroom = async () => {
  joinError.value = ''
  
  if (!classCode.value) {
    joinError.value = '请输入课堂码'
    return
  }
  
  try {
    const classroom = await api.classroom.getByCode(classCode.value)
    // 记录用户参与
    await api.classroom.recordParticipant(classroom.id, userStore.currentUser.id)
    router.push(`/classroom/viewer/${classroom.id}`)
  } catch (error) {
    console.error('Failed to join classroom:', error)
    joinError.value = '课堂码无效或课堂不存在'
  }
}

const enterClassroomAsHost = (classroom) => {
  router.push(`/classroom/host/${classroom.id}`)
}

const enterClassroomAsViewer = (classroom) => {
  router.push(`/classroom/viewer/${classroom.id}`)
}

const deleteParticipantHistory = (classroom) => {
  classroomToDelete.value = classroom
  showDeleteConfirm.value = true
}

const confirmDelete = async () => {
  if (!classroomToDelete.value) return
  
  try {
    await api.classroom.delete(classroomToDelete.value.id)
    // 重新加载课堂列表
    loadMyClassrooms()
    closeDeleteConfirm()
  } catch (error) {
    console.error('Failed to delete classroom:', error)
    alert('删除失败，请重试')
    closeDeleteConfirm()
  }
}

const closeDeleteConfirm = () => {
  showDeleteConfirm.value = false
  classroomToDelete.value = null
}

const handleFileSelect = (event) => {
  selectedFile.value = event.target.files[0]
}

const createClassroom = async () => {
  if (!newClassroom.value.name || !newClassroom.value.startTime || !newClassroom.value.endTime) {
    alert('请填写完整信息')
    return
  }
  
  if (createMode.value === 'copy' && !copySourceClassroomId.value) {
    alert('请选择要复制的课堂')
    return
  }
  
  if (createMode.value === 'copy' && copyOption.value === 'single-question' && !copySelectedQuestionId.value) {
    alert('请选择要复制的问题')
    return
  }
  
  isCreating.value = true
  
  try {
    let classroom = null
    
    if (createMode.value === 'new') {
      // 新建课堂
      classroom = await api.classroom.create({
        name: newClassroom.value.name,
        startTime: newClassroom.value.startTime,
        endTime: newClassroom.value.endTime,
        hostUserId: userStore.currentUser.id
      })
      
      // 上传 PDF
      if (selectedFile.value) {
        await api.classroom.uploadPdf(classroom.id, selectedFile.value)
      }
    } else {
      // 复制课堂
      const sourceId = parseInt(copySourceClassroomId.value)
      
      if (copyOption.value === 'entire') {
        // 复制整个课堂
        classroom = await api.classroom.copyEntireClassroom(sourceId, {
          name: newClassroom.value.name,
          startTime: newClassroom.value.startTime,
          endTime: newClassroom.value.endTime,
          hostUserId: userStore.currentUser.id
        })
        
        // 复制文件（从源课堂下载并上传到新课堂）
        try {
          const fileInfo = await api.classroom.getFileToCopy(sourceId)
          if (fileInfo && fileInfo.pdfPath) {
            // 从服务器下载文件并上传到新课堂
            const response = await fetch(`/uploads/${fileInfo.pdfPath}`)
            if (response.ok) {
              const blob = await response.blob()
              const file = new File([blob], fileInfo.pdfPath.split('/').pop(), { type: 'application/pdf' })
              await api.classroom.uploadPdf(classroom.id, file)
            }
          }
        } catch (fileError) {
          console.warn('Failed to copy file:', fileError)
          // 文件复制失败不影响课堂创建
        }
      } else if (copyOption.value === 'file') {
        // 仅复制文件
        classroom = await api.classroom.create({
          name: newClassroom.value.name,
          startTime: newClassroom.value.startTime,
          endTime: newClassroom.value.endTime,
          hostUserId: userStore.currentUser.id
        })
        
        // 复制文件
        try {
          const fileInfo = await api.classroom.getFileToCopy(sourceId)
          if (fileInfo && fileInfo.pdfPath) {
            const response = await fetch(`/uploads/${fileInfo.pdfPath}`)
            if (response.ok) {
              const blob = await response.blob()
              const file = new File([blob], fileInfo.pdfPath.split('/').pop(), { type: 'application/pdf' })
              await api.classroom.uploadPdf(classroom.id, file)
            }
          }
        } catch (fileError) {
          console.warn('Failed to copy file:', fileError)
        }
      } else if (copyOption.value === 'questions') {
        // 复制所有问题
        classroom = await api.classroom.create({
          name: newClassroom.value.name,
          startTime: newClassroom.value.startTime,
          endTime: newClassroom.value.endTime,
          hostUserId: userStore.currentUser.id
        })
        
        // 上传 PDF（如果选择了）
        if (selectedFile.value) {
          await api.classroom.uploadPdf(classroom.id, selectedFile.value)
        }
        
        // 复制所有问题
        await api.classroom.copyQuestionsToClassroom(sourceId, classroom.id)
      } else if (copyOption.value === 'single-question') {
        // 复制单个问题
        classroom = await api.classroom.create({
          name: newClassroom.value.name,
          startTime: newClassroom.value.startTime,
          endTime: newClassroom.value.endTime,
          hostUserId: userStore.currentUser.id
        })
        
        // 上传 PDF（如果选择了）
        if (selectedFile.value) {
          await api.classroom.uploadPdf(classroom.id, selectedFile.value)
        }
        
        // 复制单个问题
        await api.question.copyQuestion(parseInt(copySelectedQuestionId.value), classroom.id)
      }
    }
    
    alert('课堂创建成功！课堂码：' + classroom.classCode)
    closeCreateModal()
    loadMyClassrooms()
  } catch (error) {
    console.error('Failed to create classroom:', error)
    alert('创建课堂失败：' + (error.message || '未知错误'))
  } finally {
    isCreating.value = false
  }
}

const closeCreateModal = () => {
  showCreateModal.value = false
  newClassroom.value = { name: '', startTime: '', endTime: '' }
  selectedFile.value = null
  createMode.value = 'new'
  copySourceClassroomId.value = ''
  copyOption.value = 'entire'
  copySelectedQuestionId.value = ''
  copySourceQuestions.value = []
  isCreating.value = false
  showQuestionDropdown.value = false
}

const handleCopySourceChange = async () => {
  copySelectedQuestionId.value = ''
  copySourceQuestions.value = []
  showQuestionDropdown.value = false
  
  if (copySourceClassroomId.value) {
    try {
      // 加载源课堂的问题列表
      const questions = await api.question.getByClassroom(parseInt(copySourceClassroomId.value))
      copySourceQuestions.value = questions || []
    } catch (error) {
      console.error('Failed to load source questions:', error)
      copySourceQuestions.value = []
    }
  }
}

const toggleQuestionDropdown = () => {
  showQuestionDropdown.value = !showQuestionDropdown.value
}

const selectQuestion = (questionId) => {
  copySelectedQuestionId.value = questionId.toString()
  showQuestionDropdown.value = false
}

const getSelectedQuestionText = (questionId) => {
  const question = copySourceQuestions.value.find(q => q.id.toString() === questionId.toString())
  if (!question) return ''
  return `${getQuestionTypeText(question.type)}: ${question.content.substring(0, 50)}${question.content.length > 50 ? '...' : ''}`
}

const getQuestionTypeText = (type) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'CHOICE': '选择题',
    'ESSAY': '问答题',
    'QUIZ': '测验'
  }
  return typeMap[type] || '未知类型'
}

const getQuestionTypeIcon = (type) => {
  const iconMap = {
    'SINGLE_CHOICE': '📝',
    'MULTIPLE_CHOICE': '📝',
    'CHOICE': '📝',
    'ESSAY': '✍️',
    'QUIZ': '📋'
  }
  return iconMap[type] || '❓'
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const showMoreActions = async (classroom, event) => {
  selectedClassroomForMenu.value = classroom
  showMoreActionsMenu.value = true
  // 保存按钮位置，用于定位弹窗
  if (event) {
    menuButtonRef.value = event.target
    // 使用 nextTick 确保 DOM 已更新
    await nextTick()
    positionMenu(event.target)
  }
}

const positionMenu = (button) => {
  const menu = document.querySelector('.more-actions-menu')
  if (!menu || !button) return
  
  const rect = button.getBoundingClientRect()
  const menuWidth = 320 // 固定宽度
  const menuHeight = menu.offsetHeight || 250
  
  // 计算位置：按钮右侧，垂直对齐到按钮顶部
  let left = rect.right + 8
  let top = rect.top
  
  // 边界检查：如果超出右边界，显示在按钮左侧
  if (left + menuWidth > window.innerWidth - 20) {
    left = rect.left - menuWidth - 8
  }
  
  // 边界检查：如果超出下边界，向上调整
  if (top + menuHeight > window.innerHeight - 20) {
    top = window.innerHeight - menuHeight - 20
  }
  
  // 边界检查：如果超出上边界，对齐到顶部
  if (top < 20) {
    top = 20
  }
  
  menu.style.left = `${left}px`
  menu.style.top = `${top}px`
}

const closeMoreActionsMenu = () => {
  showMoreActionsMenu.value = false
  selectedClassroomForMenu.value = null
  menuButtonRef.value = null
}

const copyClassCode = async (event) => {
  if (!selectedClassroomForMenu.value) return
  
  try {
    await navigator.clipboard.writeText(selectedClassroomForMenu.value.classCode)
    // 显示提示
    if (event) {
      const button = event.target.closest('.menu-action-btn')
      if (button) {
        const rect = button.getBoundingClientRect()
        copyToastPosition.value = {
          top: rect.top - 40,
          left: rect.left + (rect.width / 2)
        }
      }
    }
    showCopyToast.value = true
    // 1秒后自动消失
    setTimeout(() => {
      showCopyToast.value = false
    }, 1000)
  } catch (error) {
    console.error('Failed to copy:', error)
    alert('复制失败，请手动复制')
  }
}

const deleteClassroom = () => {
  if (!selectedClassroomForMenu.value) return
  classroomToDelete.value = selectedClassroomForMenu.value
  closeMoreActionsMenu()
  showDeleteConfirm.value = true
}

// 计算课堂状态
const getClassroomStatus = (classroom) => {
  const now = new Date()
  const start = new Date(classroom.startTime)
  const end = new Date(classroom.endTime)
  
  if (now < start) return 'pending'
  if (now > end) return 'finished'
  return 'ongoing'
}

const getClassroomStatusText = (classroom) => {
  const status = getClassroomStatus(classroom)
  const statusMap = {
    'ongoing': '进行中',
    'pending': '未开始',
    'finished': '已结束'
  }
  return statusMap[status]
}

const getClassroomStatusIcon = (classroom) => {
  const status = getClassroomStatus(classroom)
  const iconMap = {
    'ongoing': '🟢',
    'pending': '⏳',
    'finished': '⚪'
  }
  return iconMap[status]
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTimeRange = (start, end) => {
  const startDate = new Date(start)
  const endDate = new Date(end)
  
  const dateStr = startDate.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
  
  const startTime = startDate.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
  
  const endTime = endDate.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
  
  return `${dateStr} ${startTime} - ${endTime}`
}

</script>

<style scoped>
/* ========== 全局样式 ========== */
.home-container {
  width: 100%;
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* ========== 顶部导航栏 ========== */
.header {
  background: white;
  padding: 16px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  font-size: 24px;
  font-weight: 600;
  color: #667eea;
  margin: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  color: #333;
  font-size: 14px;
}

.logout-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
  color: #333;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #e0e0e0;
}

/* ========== 主内容区布局 ========== */
.main-content {
  max-width: 1400px;
  width: 100%;
  margin: 0 auto;
  padding: 32px;
  display: grid;
  grid-template-columns: 2fr 3fr;
  gap: 24px;
  flex: 1;
}

/* ========== 卡片通用样式 ========== */
.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 20px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card-header .card-title {
  margin: 0;
}

/* ========== 左侧栏 ========== */
.left-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 加入课堂卡片 */
.join-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.join-card .card-title {
  color: white;
  font-size: 20px;
}

.join-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.input-field {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
  box-sizing: border-box;
}

.input-field:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.join-card .input-field {
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.95);
}

.join-card .input-field:focus {
  border-color: white;
  background: white;
}

.error-message {
  color: #ffeb3b;
  font-size: 13px;
  margin: 8px 0 0 0;
}

/* 历史课堂卡片 */
.history-card {
  flex: 1;
}

.classroom-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.classroom-item-compact {
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.classroom-item-compact:hover {
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
  transform: translateY(-1px);
}

.classroom-main {
  flex: 1;
  margin-bottom: 0;
}

.classroom-item-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.classroom-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px 0;
}

.classroom-time {
  font-size: 13px;
  color: #666;
  margin: 0;
}

.classroom-meta {
  display: flex;
  gap: 8px;
}

.badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge-viewer {
  background: #e3f2fd;
  color: #1976d2;
}

.badge-ongoing {
  background: #e8f5e9;
  color: #388e3c;
}

.badge-pending {
  background: #fff3e0;
  color: #f57c00;
}

.badge-finished {
  background: #f5f5f5;
  color: #757575;
}

.delete-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
  flex-shrink: 0;
}

.classroom-item-compact:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #f5f5f5;
  transform: scale(1.2);
  opacity: 1;
}

.delete-btn:active {
  transform: scale(1.1);
}

/* ========== 右侧主内容区 ========== */
.right-main {
  display: flex;
  flex-direction: column;
}

.host-card {
  flex: 1;
}

.classroom-list-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 课堂行（Slido 风格） */
.classroom-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.classroom-row:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
  transform: translateY(-2px);
}

.classroom-left {
  flex: 1;
}

.classroom-name-large {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.classroom-details {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.detail-item {
  font-size: 13px;
  color: #666;
}

.detail-item strong {
  color: #667eea;
  font-weight: 600;
}

.classroom-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-badge {
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.status-ongoing {
  background: #e8f5e9;
  color: #388e3c;
}

.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-finished {
  background: #f5f5f5;
  color: #757575;
}

/* ========== 按钮样式 ========== */
.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-primary {
  background: #667eea;
  color: white;
}

.btn-primary:hover {
  background: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.3);
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
  border: 1px solid #ddd;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

.btn-icon {
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  opacity: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon:hover {
  background: #f5f5f5;
  color: #333;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: #999;
}

.empty-state p {
  margin: 8px 0;
  font-size: 14px;
}

.empty-hint {
  font-size: 13px;
  color: #bbb;
}

/* ========== 弹窗样式 ========== */
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
  backdrop-filter: blur(4px);
}

.modal-content {
  background: white;
  padding: 32px;
  border-radius: 16px;
  width: 500px;
  max-width: 90vw;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.create-modal {
  width: 600px;
}

.modal-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px 0;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

.input-file {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.file-name {
  margin-top: 8px;
  color: #667eea;
  font-size: 13px;
  font-weight: 500;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
}

.modal-actions .btn {
  flex: 1;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== 创建方式选择器样式 ========== */
.create-mode-selector {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.mode-btn {
  flex: 1;
  padding: 14px 20px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  background: white;
  color: #666;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.mode-btn:hover {
  border-color: #667eea;
  background: #f8f9ff;
  color: #667eea;
}

.mode-btn.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.mode-icon {
  font-size: 24px;
}

/* ========== 复制选项样式 ========== */
.copy-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.copy-option-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  background: #fafafa;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.copy-option-item:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.copy-radio {
  accent-color: #667eea;
  margin-top: 2px;
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
}

.copy-radio {
  accent-color: #667eea;
  margin-top: 2px;
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
}

.copy-option-item.active {
  border-color: #667eea;
  background: #f0f2ff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
}

.copy-option-item.active .copy-option-title {
  color: #667eea;
  font-weight: 600;
}

.copy-option-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.copy-option-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.copy-option-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.copy-file-info {
  padding: 12px 16px;
  background: #f0f2ff;
  border: 1px solid #d0d5ff;
  border-radius: 8px;
}

.copy-file-indicator {
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

/* ========== 自定义下拉选择器样式 ========== */
.custom-select-wrapper {
  position: relative;
  width: 100%;
}

.custom-select {
  position: relative;
  width: 100%;
  padding: 12px 40px 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.custom-select:hover {
  border-color: #ccc;
  background: #fafafa;
}

.custom-select.focused {
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.custom-select.open {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  border-bottom-left-radius: 0;
  border-bottom-right-radius: 0;
}

.custom-select-selected {
  flex: 1;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  min-height: 24px;
}

.custom-select-selected .placeholder {
  color: #999;
}

.custom-select-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
  font-size: 12px;
  transition: transform 0.2s;
  pointer-events: none;
}

.custom-select-arrow.rotated {
  transform: translateY(-50%) rotate(180deg);
}

.custom-select-options {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 2px solid #667eea;
  border-top: none;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-height: 300px;
  overflow-y: auto;
  z-index: 1000;
  margin-top: -2px;
}

.custom-select-option {
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.15s;
  display: flex;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.custom-select-option:last-child {
  border-bottom: none;
}

.custom-select-option:hover {
  background: #f8f9ff;
}

.custom-select-option.selected {
  background: #f0f2ff;
  border-left: 3px solid #667eea;
  padding-left: 13px;
}

.custom-select-option.empty {
  color: #999;
  justify-content: center;
  cursor: default;
}

.custom-select-option.empty:hover {
  background: white;
}

.question-type-icon {
  font-size: 18px;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}

.question-type-text {
  font-size: 13px;
  font-weight: 600;
  color: #667eea;
  min-width: 60px;
  flex-shrink: 0;
}

.question-content {
  font-size: 14px;
  color: #333;
  flex: 1;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 下拉动画 */
.dropdown-enter-active {
  transition: all 0.2s ease-out;
}

.dropdown-leave-active {
  transition: all 0.15s ease-in;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.dropdown-enter-to {
  opacity: 1;
  transform: translateY(0);
}

.dropdown-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* 滚动条样式 */
.custom-select-options::-webkit-scrollbar {
  width: 6px;
}

.custom-select-options::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.custom-select-options::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 3px;
}

.custom-select-options::-webkit-scrollbar-thumb:hover {
  background: #999;
}

/* ========== 删除确认弹窗样式 ========== */
.confirm-modal {
  background: white;
  padding: 32px;
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  text-align: center;
  animation: modalSlideIn 0.3s ease-out;
}

@keyframes modalSlideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.confirm-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.8;
}

.confirm-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.confirm-message {
  font-size: 15px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.6;
}

.confirm-message strong {
  color: #333;
  font-weight: 600;
}

.confirm-hint {
  font-size: 13px;
  color: #999;
  margin: 0 0 24px 0;
}

.confirm-actions {
  display: flex;
  gap: 12px;
}

.confirm-actions .btn {
  flex: 1;
}

.btn-danger {
  background: #f44336;
  color: white;
}

.btn-danger:hover {
  background: #d32f2f;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(244, 67, 54, 0.3);
}

/* ========== 更多操作菜单样式 ========== */
.menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  z-index: 999;
}

.more-actions-menu {
  position: fixed;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  padding: 20px;
  min-width: 320px;
  max-width: 400px;
  z-index: 1000;
  animation: menuSlideIn 0.2s ease-out;
  transform-origin: top right;
}

@keyframes menuSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.menu-header {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.menu-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
}

.menu-status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.menu-status-badge.status-ongoing {
  background: #e8f5e9;
  color: #388e3c;
}

.menu-status-badge.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.menu-status-badge.status-finished {
  background: #f5f5f5;
  color: #757575;
}

.menu-info {
  margin-bottom: 20px;
}

.menu-info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 14px;
  color: #666;
}

.menu-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.menu-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-action-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: white;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.menu-action-btn:hover {
  background: #f5f5f5;
  border-color: #ddd;
}

.menu-action-btn.primary {
  border-color: #667eea;
  color: #667eea;
}

.menu-action-btn.primary:hover {
  background: #f0f2ff;
}

.menu-action-btn.danger {
  border-color: #f44336;
  color: #f44336;
}

.menu-action-btn.danger:hover {
  background: #ffebee;
}

.menu-action-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

/* ========== 复制提示弹窗样式 ========== */
.copy-toast {
  position: fixed;
  background: #333;
  color: white;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  pointer-events: none;
  transform: translateX(-50%);
}

.toast-icon {
  font-size: 16px;
  color: #4caf50;
}

/* 提示弹窗动画 */
.toast-enter-active {
  transition: all 0.3s ease-out;
}

.toast-leave-active {
  transition: all 0.2s ease-in;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

.toast-enter-to {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.toast-leave-from {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-10px);
}

/* ========== 响应式设计 ========== */

/* 平板 (768px - 1023px) */
@media (max-width: 1023px) {
  .main-content {
    grid-template-columns: 1fr;
    padding: 24px 16px;
  }
  
  .left-sidebar {
    order: 1;
  }
  
  .right-main {
    order: 2;
  }
  
  .classroom-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .classroom-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .btn-icon {
    opacity: 1;
  }
}

/* 移动端 (≤767px) */
@media (max-width: 767px) {
  .header {
    padding: 12px 16px;
  }
  
  .logo {
    font-size: 20px;
  }
  
  .username {
    display: none;
  }
  
  .main-content {
    padding: 16px;
    gap: 16px;
  }
  
  .card {
    padding: 20px;
    border-radius: 10px;
  }
  
  .card-title {
    font-size: 16px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .card-header .btn {
    width: 100%;
  }
  
  .join-form {
    gap: 10px;
  }
  
  .classroom-row {
    padding: 16px;
  }
  
  .classroom-name-large {
    font-size: 16px;
  }
  
  .classroom-details {
    flex-direction: column;
    gap: 6px;
  }
  
  .classroom-right {
    flex-wrap: wrap;
  }
  
  .classroom-item-compact {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .classroom-item-right {
    width: 100%;
    justify-content: space-between;
  }
  
  .delete-btn {
    opacity: 1;
  }
  
  .status-badge {
    order: -1;
    width: 100%;
    text-align: center;
  }
  
  .modal-content {
    padding: 24px;
    width: 100%;
    max-width: calc(100vw - 32px);
  }
  
  .modal-actions {
    flex-direction: column;
  }
  
  .confirm-modal {
    padding: 24px;
    width: calc(100vw - 32px);
  }
  
  .confirm-icon {
    font-size: 40px;
  }
  
  .confirm-title {
    font-size: 18px;
  }
  
  .confirm-message {
    font-size: 14px;
  }
  
  .confirm-actions {
    flex-direction: column;
  }
}

/* 大屏幕优化 (≥1400px) */
@media (min-width: 1400px) {
  .main-content {
    max-width: 1600px;
    grid-template-columns: 1fr 2fr;
  }
}
</style>
