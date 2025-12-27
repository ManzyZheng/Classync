<template>
  <div class="interaction-tab">
    <!-- 主持人视角 -->
    <div v-if="isHost" class="host-view">
      <div v-if="!selectedQuestion" class="question-list">
        <div class="list-header">
          <h3>问题列表</h3>
          <button class="add-btn" @click="showQuestionTypeModal = true">+</button>
        </div>
        
        <div class="questions">
          <div 
            v-for="question in questions" 
            :key="question.id"
            class="question-item"
          >
            <div class="question-info" @click="selectQuestion(question)">
              <div class="question-content">{{ question.content }}</div>
              <div class="question-type">{{ question.type === 'CHOICE' ? '选择题' : '问答题' }}</div>
            </div>
            <div class="question-actions">
              <button 
                :class="['toggle-btn', { open: question.isOpen }]"
                @click="toggleQuestion(question.id)"
              >
                {{ question.isOpen ? '开放' : '关闭' }}
              </button>
              <div class="more-menu">
                <button class="more-btn" @click.stop="toggleMenu(question.id, $event)">⋯</button>
                <div v-if="activeMenu === question.id" class="menu-dropdown" @click.stop>
                  <button @click="editQuestion(question)">编辑</button>
                  <button @click="deleteQuestion(question.id)">删除</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 问题详情 -->
      <div v-else class="question-detail">
        <button class="back-btn" @click="selectedQuestion = null">← 返回</button>
        
        <div class="detail-header">
          <h3>{{ selectedQuestion.type === 'CHOICE' ? '选择题控制' : '问答题控制' }}</h3>
          <div class="status-control">
            <span>{{ selectedQuestion.isFinished ? '已结束' : '统计进行中' }}</span>
            <div 
              :class="['status-indicator', { open: selectedQuestion.isOpen }]"
              @click="toggleQuestion(selectedQuestion.id)"
            ></div>
          </div>
        </div>
        
        <div class="question-content-box">
          <p>{{ selectedQuestion.content }}</p>
        </div>
        
        <!-- 问答题统计 -->
        <div v-if="selectedQuestion.type === 'ESSAY'" class="essay-statistics">
          <!-- 词云区域（放在上面） -->
          <div class="wordcloud-section">
            <h4>词云统计</h4>
            <div class="wordcloud-container">
              <div v-if="wordFrequency.length > 0" class="word-tags">
                <span 
                  v-for="(word, index) in wordFrequency" 
                  :key="index"
                  class="word-tag"
                  :style="{ fontSize: getWordSize(word.count) + 'px' }"
                >
                  {{ word.word }} ({{ word.count }})
                </span>
              </div>
              <div v-else class="no-wordcloud">
                暂无数据
              </div>
            </div>
          </div>
          
          <!-- 学生回答列表 -->
          <div class="answers-section">
            <h4>学生回答 ({{ essayAnswers.length }})</h4>
            <div class="essay-answers-list">
              <div 
                v-for="(answer, index) in essayAnswers" 
                :key="index"
                class="essay-answer-item"
              >
                <div class="answer-content">{{ answer.content }}</div>
                <div class="answer-time">{{ formatTime(answer.createdAt) }}</div>
              </div>
              <div v-if="essayAnswers.length === 0" class="no-answers">
                暂无回答
              </div>
            </div>
          </div>
        </div>
        <div v-if="selectedQuestion.type === 'CHOICE'" class="choice-statistics">
          <div 
            v-for="stat in statistics" 
            :key="stat.optionContent"
            class="stat-item"
          >
            <div class="option-text">{{ stat.optionContent }}</div>
            <div class="stat-bar">
              <div 
                class="bar-fill"
                :style="{ width: stat.percentage + '%' }"
              ></div>
              <span class="stat-text">{{ stat.count }}人 ({{ stat.percentage.toFixed(1) }}%)</span>
            </div>
          </div>
        </div>
        
        <!-- 问答题词云 -->
        <div v-if="selectedQuestion.type === 'ESSAY'" class="essay-wordcloud">
          <canvas ref="wordcloudCanvas" width="400" height="300"></canvas>
        </div>
        
        <button 
          v-if="selectedQuestion.isOpen && !selectedQuestion.isFinished"
          class="finish-btn"
          @click="finishQuestion"
        >
          结束统计并展示结果
        </button>
      </div>
    </div>
    
    <!-- 观众视角 -->
    <div v-else class="viewer-view">
      <div v-if="openQuestions.length === 0" class="no-interaction">
        <div class="empty-box">当前无互动</div>
      </div>
      
      <div v-else class="interaction-active">
        <!-- 选择题 -->
        <div v-if="currentQuestion.type === 'CHOICE'" class="choice-question">
          <h3>{{ currentQuestion.content }}</h3>
          
          <div v-if="!currentQuestion.isFinished" class="options">
            <div 
              v-for="(option, index) in currentQuestion.options"
              :key="option.id"
              :class="['option', { selected: selectedOption === option.content }]"
              @click="selectOption(option.content)"
            >
              {{ option.content }}
            </div>
          </div>
          
          <button 
            v-if="!submitted && !currentQuestion.isFinished"
            class="submit-btn"
            :disabled="!selectedOption"
            @click="submitAnswer"
          >
            提交
          </button>
          
          <!-- 结果展示 -->
          <div v-if="currentQuestion.isFinished" class="results">
            <div 
              v-for="stat in viewerStatistics"
              :key="stat.optionContent"
              :class="['result-item', { 
                correct: stat.isCorrect, 
                myAnswer: stat.optionContent === myAnswer 
              }]"
            >
              <div class="option-text">{{ stat.optionContent }}</div>
              <div class="result-bar">
                <div 
                  :class="['bar-fill', { correct: stat.isCorrect }]"
                  :style="{ width: stat.percentage + '%' }"
                ></div>
                <span>{{ stat.percentage.toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 问答题 -->
        <div v-if="currentQuestion.type === 'ESSAY'" class="essay-question">
          <h3>{{ currentQuestion.content }}</h3>
          
          <div v-if="!currentQuestion.isFinished" class="essay-input">
            <textarea 
              v-model="essayAnswer"
              placeholder="请输入您的答案"
            ></textarea>
            <button @click="submitEssayAnswer">发送</button>
          </div>
          
          <div class="essay-wordcloud">
            <canvas ref="essayWordcloudCanvas" width="400" height="300"></canvas>
          </div>
        </div>
        
        <!-- 题目切换 -->
        <div v-if="openQuestions.length > 1" class="question-nav">
          <button @click="prevQuestion">‹</button>
          <span>{{ currentQuestionIndex + 1 }} / {{ openQuestions.length }}</span>
          <button @click="nextQuestion">›</button>
        </div>
      </div>
    </div>
    
    <!-- 问题类型选择弹窗 -->
    <div v-if="showQuestionTypeModal" class="modal-overlay" @click="showQuestionTypeModal = false">
      <div class="modal-content" @click.stop>
        <h3>选择问题类型</h3>
        <button class="type-btn" @click="createQuestion('CHOICE')">选择题</button>
        <button class="type-btn" @click="createQuestion('ESSAY')">问答题</button>
      </div>
    </div>
    
    <!-- 问题编辑弹窗 -->
    <QuestionEditor
      v-if="showQuestionEditor"
      :question="editingQuestion"
      :classroom-id="classroomId"
      @close="showQuestionEditor = false"
      @saved="handleQuestionSaved"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../api'
import websocket from '../utils/websocket'
import QuestionEditor from './QuestionEditor.vue'

const props = defineProps({
  classroomId: Number,
  isHost: Boolean
})

const userStore = useUserStore()

const questions = ref([])
const selectedQuestion = ref(null)
const statistics = ref([])
const essayAnswers = ref([])  // 问答题答案列表
const wordFrequency = ref([])  // 词频统计
const showQuestionTypeModal = ref(false)
const showQuestionEditor = ref(false)
const editingQuestion = ref(null)
const activeMenu = ref(null)

// 观众相关
const openQuestions = ref([])
const currentQuestionIndex = ref(0)
const currentQuestion = ref(null)
const selectedOption = ref(null)
const submitted = ref(false)
const myAnswer = ref(null)
const viewerStatistics = ref([])
const essayAnswer = ref('')
const wordcloudCanvas = ref(null)
const essayWordcloudCanvas = ref(null)

onMounted(() => {
  loadQuestions()
  // 添加全局点击事件监听
  document.addEventListener('click', handleClickOutside)
  
  // ✅ 设置WebSocket事件监听（主持人和学生都需要）
  setupWebSocketListeners()
})

onUnmounted(() => {
  // 移除监听
  document.removeEventListener('click', handleClickOutside)
  cleanupWebSocketListeners()
})

// ✅ WebSocket事件监听器（替代window.dispatchEvent反模式）
const setupWebSocketListeners = () => {
  // 监听问题开放事件
  websocket.on('QUESTION_OPENED', handleQuestionOpened)
  // 监听问题关闭事件
  websocket.on('QUESTION_CLOSED', handleQuestionClosed)
  // 监听问题结束事件
  websocket.on('QUESTION_FINISHED', handleQuestionFinished)
  // 监听答案提交事件（主持人端）
  if (props.isHost) {
    websocket.on('ANSWER_SUBMITTED', handleAnswerSubmitted)
  }
}

const cleanupWebSocketListeners = () => {
  websocket.off('QUESTION_OPENED', handleQuestionOpened)
  websocket.off('QUESTION_CLOSED', handleQuestionClosed)
  websocket.off('QUESTION_FINISHED', handleQuestionFinished)
  if (props.isHost) {
    websocket.off('ANSWER_SUBMITTED', handleAnswerSubmitted)
  }
}

// 处理答案提交（主持人端）
// ✅ 处理答案提交（主持人端实时更新）
const handleAnswerSubmitted = (payload) => {
  console.log('[InteractionTab] Answer submitted:', payload)
  
  if (!selectedQuestion.value) return
  
  const questionId = payload.questionId || selectedQuestion.value.id
  
  // 如果当前正在查看的问题有新答案提交
  if (selectedQuestion.value.id === questionId) {
    if (selectedQuestion.value.type === 'ESSAY') {
      // 问答题：刷新答案列表和词云
      console.log('[InteractionTab] Refreshing essay answers for question:', questionId)
      loadEssayAnswers(questionId)
    } else if (selectedQuestion.value.type === 'CHOICE') {
      // 选择题：刷新统计数据
      console.log('[InteractionTab] Refreshing choice statistics for question:', questionId)
      loadStatistics(questionId)
    }
  }
}

// ✅ 处理问题开放
const handleQuestionOpened = (payload) => {
  console.log('[InteractionTab] Question opened:', payload)
  updateQuestionState(payload.questionId, true)
}

// ✅ 处理问题关闭
const handleQuestionClosed = (payload) => {
  console.log('[InteractionTab] Question closed:', payload)
  updateQuestionState(payload.questionId, false)
}

// ✅ 统一的状态更新逻辑
const updateQuestionState = (questionId, isOpen) => {
  console.log('[InteractionTab] updateQuestionState:', { questionId, isOpen })
  
  // 1. 更新questions列表中的状态
  const questionIndex = questions.value.findIndex(q => q.id === questionId)
  if (questionIndex !== -1) {
    questions.value[questionIndex].isOpen = isOpen
  } else {
    console.warn('[InteractionTab] Question not found in local list:', questionId)
    // 如果本地没有这个问题，重新加载列表
    loadQuestions()
    return
  }
  
  // 2. 重新计算openQuestions
  openQuestions.value = questions.value.filter(q => q.isOpen)
  
  console.log('[InteractionTab] Open questions count:', openQuestions.value.length)
  
  // 3. 如果问题被开放了
  if (isOpen) {
    // 如果是当前显示的问题，更新它的状态
    if (currentQuestion.value?.id === questionId) {
      currentQuestion.value.isOpen = true
    } 
    // 如果当前没有显示问题，加载这个新开放的问题
    else if (!currentQuestion.value) {
      console.log('[InteractionTab] Loading newly opened question:', questionId)
      loadQuestionDetail(questionId)
    }
  }
  
  // 4. 如果当前问题被关闭了
  if (currentQuestion.value?.id === questionId && !isOpen) {
    if (openQuestions.value.length > 0) {
      // 切换到下一个开放问题
      const nextQuestion = openQuestions.value.find(q => q.id !== questionId)
      if (nextQuestion) {
        loadQuestionDetail(nextQuestion.id)
      } else {
        currentQuestion.value = null
      }
    } else {
      // 没有开放问题了，显示空状态
      currentQuestion.value = null
    }
  }
}

const handleQuestionFinished = (payload) => {
  console.log('[InteractionTab] Question finished:', payload)
  
  const { questionId } = payload
  
  // 更新问题状态
  const questionIndex = questions.value.findIndex(q => q.id === questionId)
  if (questionIndex !== -1) {
    questions.value[questionIndex].isFinished = true
  }
  
  // 如果是当前问题，标记为已结束
  if (currentQuestion.value?.id === questionId) {
    currentQuestion.value.isFinished = true
    // 重新加载统计数据（如果需要）
    if (currentQuestion.value.type === 'CHOICE') {
      loadViewerStatistics(questionId)
    }
  }
}

const loadQuestions = async () => {
  try {
    const data = await api.question.getByClassroom(props.classroomId)
    questions.value = data
    
    // 🔍 诊断日志
    console.log('[InteractionTab] Loaded questions:', data.map(q => ({
      id: q.id,
      content: q.content.substring(0, 20),
      isOpen: q.isOpen,
      isFinished: q.isFinished
    })))
    
    if (!props.isHost) {
      openQuestions.value = data.filter(q => q.isOpen)
      console.log('Open questions for viewer:', openQuestions.value)
      
      if (openQuestions.value.length > 0) {
        await loadQuestionDetail(openQuestions.value[0].id)
      } else {
        currentQuestion.value = null
        console.log('No open questions, showing empty state')
      }
    }
  } catch (error) {
    console.error('Failed to load questions:', error)
  }
}

const loadQuestionDetail = async (questionId) => {
  try {
    console.log('[InteractionTab] Loading question detail:', questionId)
    const data = await api.question.getById(questionId)
    currentQuestion.value = data
    console.log('[InteractionTab] Question detail loaded:', {
      id: data.id,
      type: data.type,
      isOpen: data.isOpen,
      isFinished: data.isFinished
    })
    
    if (data.isFinished && data.type === 'CHOICE') {
      loadViewerStatistics(questionId)
    }
  } catch (error) {
    console.error('Failed to load question detail:', error)
    currentQuestion.value = null
  }
}

const selectQuestion = async (question) => {
  selectedQuestion.value = question
  
  if (question.type === 'CHOICE') {
    loadStatistics(question.id)
  } else if (question.type === 'ESSAY') {
    loadEssayAnswers(question.id)
  }
}

const loadStatistics = async (questionId) => {
  try {
    const data = await api.answer.getStatistics(questionId)
    statistics.value = data
  } catch (error) {
    console.error('Failed to load statistics:', error)
  }
}

// ✅ 加载问答题答案
const loadEssayAnswers = async (questionId) => {
  try {
    const data = await api.answer.getEssayAnswers(questionId)
    essayAnswers.value = data
    generateWordCloud(data)
  } catch (error) {
    console.error('Failed to load essay answers:', error)
  }
}

// ✅ 生成词云数据（简单的词频统计）
const generateWordCloud = (answers) => {
  if (!answers || answers.length === 0) {
    wordFrequency.value = []
    return
  }
  
  // 合并所有答案文本
  const allText = answers.map(a => a.content).join(' ')
  
  // 简单的中文分词（按字符分割，过滤标点符号）
  const words = allText.split('').filter(char => {
    return char.match(/[\u4e00-\u9fa5a-zA-Z0-9]/)
  })
  
  // 统计词频（这里简化为单字统计，实际应该用分词库）
  const frequency = {}
  words.forEach(word => {
    frequency[word] = (frequency[word] || 0) + 1
  })
  
  // 转换为数组并排序
  wordFrequency.value = Object.entries(frequency)
    .map(([word, count]) => ({ word, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 30)  // 只取前30个高频词
}

// ✅ 根据词频计算字体大小
const getWordSize = (count) => {
  const maxCount = wordFrequency.value[0]?.count || 1
  const minSize = 12
  const maxSize = 32
  return minSize + (count / maxCount) * (maxSize - minSize)
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
  
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${date.toLocaleDateString()} ${hours}:${minutes}`
}

const loadViewerStatistics = async (questionId) => {
  try {
    const data = await api.answer.getStatistics(questionId)
    viewerStatistics.value = data
  } catch (error) {
    console.error('Failed to load viewer statistics:', error)
  }
}

const toggleQuestion = async (questionId) => {
  try {
    const question = questions.value.find(q => q.id === questionId)
    
    // 🔍 诊断日志
    console.log('[InteractionTab] ========== TOGGLE START ==========')
    console.log('[InteractionTab] Question before toggle:', {
      questionId,
      currentIsOpen: question?.isOpen,
      willBecomeIsOpen: !question?.isOpen
    })
    
    await api.question.toggle(questionId)
    
    console.log('[InteractionTab] API toggle completed, sending WebSocket...')
    websocket.sendQuestionToggle(props.classroomId, questionId)
    
    // 只更新当前问题的状态，不刷新整个列表
    if (question) {
      const oldState = question.isOpen
      question.isOpen = !question.isOpen
      console.log('[InteractionTab] Updated local state:', {
        from: oldState,
        to: question.isOpen
      })
    }
    
    // 如果在详情页，也更新详情
    if (selectedQuestion.value && selectedQuestion.value.id === questionId) {
      selectedQuestion.value.isOpen = !selectedQuestion.value.isOpen
    }
    
    console.log('[InteractionTab] ========== TOGGLE END ==========')
  } catch (error) {
    console.error('Failed to toggle question:', error)
  }
}

const finishQuestion = async () => {
  try {
    await api.question.finish(selectedQuestion.value.id)
    websocket.sendQuestionFinish(props.classroomId, selectedQuestion.value.id)
    selectedQuestion.value.isFinished = true
  } catch (error) {
    console.error('Failed to finish question:', error)
  }
}

const toggleMenu = (questionId, event) => {
  // 阻止事件冒泡
  if (event) {
    event.stopPropagation()
  }
  activeMenu.value = activeMenu.value === questionId ? null : questionId
}

// 点击外部关闭菜单
const handleClickOutside = () => {
  activeMenu.value = null
}

const createQuestion = (type) => {
  editingQuestion.value = { 
    type, 
    content: '', 
    options: [], 
    isOpen: false,  // 默认关闭
    isFinished: false 
  }
  showQuestionTypeModal.value = false
  showQuestionEditor.value = true
}

const editQuestion = (question) => {
  editingQuestion.value = { ...question }
  showQuestionEditor.value = true
  activeMenu.value = null
}

const deleteQuestion = async (questionId) => {
  if (!confirm('确定删除这个问题吗？')) return
  
  try {
    await api.question.delete(questionId)
    loadQuestions()
    activeMenu.value = null
  } catch (error) {
    console.error('Failed to delete question:', error)
  }
}

const handleQuestionSaved = () => {
  showQuestionEditor.value = false
  editingQuestion.value = null
  loadQuestions()
}

// 观众答题
const selectOption = (option) => {
  if (!submitted.value) {
    selectedOption.value = option
  }
}

const submitAnswer = async () => {
  if (!selectedOption.value) return
  
  if (!currentQuestion.value) {
    console.error('[InteractionTab] Cannot submit: currentQuestion is null')
    console.log('[InteractionTab] openQuestions:', openQuestions.value)
    alert('问题加载失败，请刷新页面')
    return
  }
  
  try {
    console.log('[InteractionTab] Submitting answer:', {
      questionId: currentQuestion.value.id,
      option: selectedOption.value
    })
    
    await api.answer.submit({
      questionId: currentQuestion.value.id,
      userId: userStore.currentUser.id,
      content: selectedOption.value
    })
    
    console.log('[InteractionTab] Answer submitted successfully')
    submitted.value = true
    myAnswer.value = selectedOption.value
    websocket.sendAnswerSubmit(props.classroomId, currentQuestion.value.id)
  } catch (error) {
    console.error('Failed to submit answer:', error)
    alert('提交失败，请重试')
  }
}

const submitEssayAnswer = async () => {
  if (!essayAnswer.value.trim()) return
  
  if (!currentQuestion.value) {
    console.error('[InteractionTab] Cannot submit: currentQuestion is null')
    console.log('[InteractionTab] openQuestions:', openQuestions.value)
    alert('问题加载失败，请刷新页面')
    return
  }
  
  try {
    console.log('[InteractionTab] Submitting essay answer:', {
      questionId: currentQuestion.value.id,
      content: essayAnswer.value
    })
    
    await api.answer.submit({
      questionId: currentQuestion.value.id,
      userId: userStore.currentUser.id,
      content: essayAnswer.value
    })
    
    console.log('[InteractionTab] Essay answer submitted successfully')
    essayAnswer.value = ''
    websocket.sendAnswerSubmit(props.classroomId, currentQuestion.value.id)
  } catch (error) {
    console.error('Failed to submit essay answer:', error)
    alert('提交失败，请重试')
  }
}

const prevQuestion = () => {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
    loadQuestionDetail(openQuestions.value[currentQuestionIndex.value].id)
    resetAnswerState()
  }
}

const nextQuestion = () => {
  if (currentQuestionIndex.value < openQuestions.value.length - 1) {
    currentQuestionIndex.value++
    loadQuestionDetail(openQuestions.value[currentQuestionIndex.value].id)
    resetAnswerState()
  }
}

const resetAnswerState = () => {
  selectedOption.value = null
  submitted.value = false
  myAnswer.value = null
  essayAnswer.value = ''
}
</script>

<style scoped>
.interaction-tab {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.host-view, .viewer-view {
  height: 100%;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  font-size: 18px;
  color: #333;
}

.add-btn {
  width: 32px;
  height: 32px;
  background: #667eea;
  color: white;
  border-radius: 50%;
  font-size: 20px;
}

.questions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-item {
  border: 1px solid #ddd;
  border-radius: 6px;
  overflow: visible;
  position: relative;
}

.question-info {
  padding: 12px;
  cursor: pointer;
}

.question-info:hover {
  background: #f5f5f5;
}

.question-content {
  margin-bottom: 4px;
  color: #333;
}

.question-type {
  font-size: 12px;
  color: #667eea;
}

.question-actions {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-top: 1px solid #ddd;
}

.toggle-btn {
  padding: 4px 12px;
  background: #9e9e9e;  /* 默认灰色（关闭状态） */
  color: white;
  border: 1px solid #9e9e9e;
  border-radius: 4px;
  font-size: 12px;
}

.toggle-btn.open {
  background: #4caf50;  /* isOpen=true时绿色（开放状态） */
  color: white;
  border-color: #4caf50;
}

.more-menu {
  position: relative;
  margin-left: auto;
}

.more-btn {
  padding: 4px 8px;
  background: transparent;
  font-size: 16px;
}

.menu-dropdown {
  position: absolute;
  right: 0;
  top: 100%;
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 100;
  min-width: 80px;
  margin-top: 4px;
}

.menu-dropdown button {
  display: block;
  width: 100%;
  padding: 8px 16px;
  text-align: left;
  background: white;
  border: none;
}

.menu-dropdown button:hover {
  background: #f5f5f5;
}

.question-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.back-btn {
  padding: 8px;
  background: transparent;
  color: #667eea;
  text-align: left;
  margin-bottom: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.status-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f44336;
  cursor: pointer;
}

.status-indicator.open {
  background: #4caf50;
}

.question-content-box {
  padding: 16px;
  background: #f5f5f5;
  border-radius: 6px;
  margin-bottom: 16px;
}

.choice-statistics {
  flex: 1;
  overflow-y: auto;
}

/* 问答题统计样式 */
.essay-statistics {
  padding: 16px;
}

.essay-statistics h4 {
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
}

.wordcloud-section {
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.wordcloud-container {
  min-height: 160px;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.word-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
  align-items: center;
}

.word-tag {
  display: inline-block;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.3s ease;
  cursor: default;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.word-tag:hover {
  transform: scale(1.1) translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.no-wordcloud {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 40px;
}

.answers-section {
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.essay-answers-list {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 12px;
}

.essay-answer-item {
  padding: 14px 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 10px;
  border-left: 4px solid #667eea;
  transition: all 0.2s;
}

.essay-answer-item:hover {
  background: #e9ecef;
  transform: translateX(4px);
}

.answer-content {
  color: #333;
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 8px;
  word-wrap: break-word;
}

.answer-time {
  font-size: 12px;
  color: #999;
}

.no-answers {
  text-align: center;
  padding: 60px;
  color: #999;
  font-size: 14px;
}

.stat-item {
  margin-bottom: 16px;
}

.option-text {
  margin-bottom: 4px;
  color: #333;
}

.stat-bar {
  position: relative;
  height: 32px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: #667eea;
  transition: width 0.3s;
}

.stat-text {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #333;
  font-size: 12px;
}

.finish-btn {
  padding: 12px;
  background: #667eea;
  color: white;
  border-radius: 4px;
  margin-top: 16px;
}

.no-interaction {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-box {
  padding: 40px;
  background: #f5f5f5;
  color: #999;
  border-radius: 6px;
}

.choice-question, .essay-question {
  padding: 16px;
}

.choice-question h3, .essay-question h3 {
  margin-bottom: 16px;
  color: #333;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.option {
  padding: 12px;
  border: 2px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.option:hover {
  border-color: #667eea;
}

.option.selected {
  border-color: #667eea;
  background: #f0f4ff;
}

.submit-btn {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border-radius: 4px;
}

.submit-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.results {
  margin-top: 16px;
}

.result-item {
  margin-bottom: 12px;
}

.result-item.myAnswer .option-text {
  font-weight: bold;
  font-size: 16px;
}

.result-bar {
  position: relative;
  height: 32px;
  background: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.result-bar .bar-fill {
  height: 100%;
  background: #ddd;
}

.result-bar .bar-fill.correct {
  background: #4caf50;
}

.result-bar span {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
}

.essay-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.essay-input textarea {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  min-height: 80px;
}

.essay-input button {
  align-self: flex-end;
  padding: 8px 24px;
  background: #667eea;
  color: white;
  border-radius: 4px;
}

.essay-wordcloud {
  margin-top: 16px;
}

.question-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}

.question-nav button {
  padding: 4px 12px;
  background: #667eea;
  color: white;
  border-radius: 4px;
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
  width: 400px;
}

.modal-content h3 {
  margin-bottom: 16px;
}

.type-btn {
  display: block;
  width: 100%;
  padding: 12px;
  margin-bottom: 8px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
}

.type-btn:hover {
  background: #667eea;
  color: white;
  border-color: #667eea;
}
</style>

