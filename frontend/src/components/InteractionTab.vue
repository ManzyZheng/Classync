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
              <div class="question-type">{{ getQuestionTypeName(question.type) }}</div>
            </div>
            <div class="question-actions">
              <div class="toggle-wrapper">
                <span class="toggle-label">{{ question.isOpen ? '开放' : '关闭' }}</span>
                <label class="toggle-switch">
                  <input 
                    type="checkbox" 
                    :checked="question.isOpen"
                    @change="toggleQuestion(question.id)"
                  />
                  <span class="toggle-slider"></span>
                </label>
              </div>
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
          <h3>{{ getQuestionTypeName(selectedQuestion.type) }}控制</h3>
          <div class="status-control">
            <span>{{ selectedQuestion.isFinished ? '已结束' : '统计进行中' }}</span>
            <div class="timer-display">
              <svg class="timer-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                <path d="M12 6V12L16 14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              <span class="timer-text">
                <span v-if="selectedQuestion.isOpen && questionTimers[selectedQuestion.id]">
                  {{ formatTimer(questionTimers[selectedQuestion.id]) }}
                </span>
                <span v-else class="timer-not-started">未开始计时</span>
              </span>
            </div>
          </div>
        </div>
        
        <div class="question-content-box">
          <p>{{ selectedQuestion.content }}</p>
        </div>
        
        <!-- 问答题统计 -->
        <!-- 测验子问题列表 -->
        <div v-if="selectedQuestion.type === 'QUIZ'" class="quiz-subquestions-section">
          <h4>测验问题列表 ({{ selectedQuestion.questions?.length || 0 }})</h4>
          <div class="quiz-subquestions-list">
            <div 
              v-for="(subQuestion, index) in selectedQuestion.questions"
              :key="index"
              class="quiz-subquestion-item"
            >
              <div class="quiz-subquestion-header">
                <span class="quiz-subquestion-number">问题 {{ index + 1 }}</span>
                <span class="quiz-subquestion-type">{{ getQuestionTypeName(subQuestion.type) }}</span>
                <span class="quiz-subquestion-status" :class="{ 'open': subQuestion.isOpen }">
                  {{ subQuestion.isOpen ? '已开放' : '未开放' }}
                </span>
              </div>
              <div class="quiz-subquestion-content">{{ subQuestion.content }}</div>
              <div v-if="subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE' || subQuestion.type === 'CHOICE'" class="quiz-subquestion-stats">
                <button class="view-stats-btn" @click="viewSubQuestionStats(subQuestion)">查看统计</button>
              </div>
            </div>
            <div v-if="!selectedQuestion.questions || selectedQuestion.questions.length === 0" class="no-subquestions">
              暂无子问题
            </div>
          </div>
        </div>
        
        <div v-if="selectedQuestion.type === 'ESSAY' || (selectedQuestion.type === 'QUIZ' && viewingSubQuestion)" class="essay-statistics">
          <!-- 词云区域（放在上面） -->
          <div class="wordcloud-section">
            <div class="wordcloud-header">
              <h4>词云统计</h4>
              <div class="wordcloud-controls">
                <div class="wordcloud-toggle">
                  <label class="toggle-label-small">
                    <input 
                      type="checkbox" 
                      v-model="showWordCloud"
                    />
                    <span>显示词云</span>
                  </label>
                </div>
                <div v-if="showWordCloud" class="wordcloud-toggle">
                  <label class="toggle-label-small">
                    <input 
                      type="checkbox" 
                      v-model="useExternalWordCloud"
                    />
                    <span>使用外部API词云</span>
                  </label>
                </div>
              </div>
            </div>
            <div v-if="showWordCloud" class="wordcloud-container">
              <!-- 外部API词云 -->
              <WordCloudIframe
                v-if="useExternalWordCloud"
                :keywords="wordCloudKeywords"
                bg-color="white"
                class="external-wordcloud"
              />
              <!-- 原有词云 -->
              <template v-else>
                <div v-if="wordFrequency.length > 0" class="word-tags">
                  <span 
                    v-for="(word, index) in wordFrequency" 
                    :key="index"
                    class="word-tag"
                    :class="getWordClass(word.count, word.normalizedFrequency)"
                    :style="{ ...getWordStyle(word, index), color: getWordColor(word.count, index, word.normalizedFrequency) }"
                  >
                    {{ word.word }}
                  </span>
                </div>
                <div v-else class="no-wordcloud">
                  暂无数据
                </div>
              </template>
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
        <div v-if="selectedQuestion.type === 'SINGLE_CHOICE' || selectedQuestion.type === 'MULTIPLE_CHOICE' || selectedQuestion.type === 'CHOICE'" class="choice-statistics">
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
        <!-- 题目切换 -->
        <div v-if="openQuestions.length > 1" class="question-nav">
          <button 
            @click="prevQuestion" 
            :disabled="currentQuestionIndex === 0"
            :class="{ disabled: currentQuestionIndex === 0 }"
          >
            ‹
          </button>
          <span>{{ currentQuestionIndex + 1 }} / {{ openQuestions.length }}</span>
          <button 
            @click="nextQuestion" 
            :disabled="currentQuestionIndex === openQuestions.length - 1"
            :class="{ disabled: currentQuestionIndex === openQuestions.length - 1 }"
          >
            ›
          </button>
        </div>
        
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
      </div>
    </div>
    
    <!-- 问题类型选择弹窗 -->
    <div v-if="showQuestionTypeModal" class="modal-overlay" @click="showQuestionTypeModal = false">
      <div class="modal-content type-select-modal" @click.stop>
        <div class="modal-header">
          <h3>选择问题类型</h3>
          <button class="close-btn" @click="showQuestionTypeModal = false">×</button>
        </div>
        <div class="type-grid">
          <div class="type-card" @click="createQuestion('SINGLE_CHOICE')">
            <div class="type-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                <circle cx="8" cy="8" r="1.5" fill="currentColor"/>
                <path d="M12 8H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <circle cx="8" cy="12" r="1.5" fill="currentColor"/>
                <path d="M12 12H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <circle cx="8" cy="16" r="1.5" fill="currentColor"/>
                <path d="M12 16H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="type-title">单选题</div>
            <div class="type-desc">只能选择一个正确答案</div>
          </div>
          
          <div class="type-card" @click="createQuestion('MULTIPLE_CHOICE')">
            <div class="type-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
                <rect x="5" y="6" width="3" height="3" rx="0.5" fill="currentColor"/>
                <path d="M11 7.5H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <rect x="5" y="11" width="3" height="3" rx="0.5" fill="currentColor"/>
                <path d="M11 12.5H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <rect x="5" y="16" width="3" height="3" rx="0.5" fill="currentColor"/>
                <path d="M11 17.5H20" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="type-title">多选题</div>
            <div class="type-desc">可以选择多个正确答案</div>
          </div>
          
          <div class="type-card" @click="createQuestion('ESSAY')">
            <div class="type-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M14 2H6C5.46957 2 4.96086 2.21071 4.58579 2.58579C4.21071 2.96086 4 3.46957 4 4V20C4 20.5304 4.21071 21.0391 4.58579 21.4142C4.96086 21.7893 5.46957 22 6 22H18C18.5304 22 19.0391 21.7893 19.4142 21.4142C19.7893 21.0391 20 20.5304 20 20V8L14 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14 2V8H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M8 12H16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <path d="M8 16H16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </div>
            <div class="type-title">开放问题</div>
            <div class="type-desc">学生自由回答</div>
          </div>
          
          <div class="type-card" @click="createQuestion('QUIZ')">
            <div class="type-icon">
              <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 11L12 14L22 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M21 12V19C21 19.5304 20.7893 20.0391 20.4142 20.4142C20.0391 20.7893 19.5304 21 19 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H16" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div class="type-title">测验</div>
            <div class="type-desc">包含多个问题统一管理</div>
          </div>
        </div>
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
    
    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteConfirm" class="modal-overlay delete-overlay" @click="cancelDelete">
      <div class="modal-content delete-modal" @click.stop>
        <div class="modal-header">
          <h3>删除确认</h3>
          <button class="close-btn" @click="cancelDelete">×</button>
        </div>
        <div class="delete-content">
          <div class="delete-icon-wrapper">
            <div class="delete-icon-bg"></div>
            <svg class="delete-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M19 7L18.1327 19.1425C18.0579 20.1891 17.187 21 16.1378 21H7.86224C6.81296 21 5.94208 20.1891 5.86732 19.1425L5 7M10 11V17M14 11V17M15 7V4C15 3.44772 14.5523 3 14 3H10C9.44772 3 9 3.44772 9 4V7M4 7H20" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <div class="delete-text-content">
            <p class="delete-message">确定要删除这个问题吗？</p>
            <p class="delete-warning">删除后无法恢复，请谨慎操作</p>
          </div>
        </div>
        <div class="modal-actions">
          <button class="cancel-btn" @click="cancelDelete">取消</button>
          <button class="delete-confirm-btn" @click="confirmDelete">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style="margin-right: 6px;">
              <path d="M4 4L12 12M4 12L12 4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            确认删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted, computed } from 'vue'
import { useUserStore } from '../stores/user'
import api from '../api'
import websocket from '../utils/websocket'
import QuestionEditor from './QuestionEditor.vue'
import WordCloudIframe from './WordCloudIframe.vue'
import { filterStopwords, isStopword } from '../utils/stopwords'

// 动态导入 jieba-wasm 以避免 Vite 预构建问题
let jiebaModule = null
let jiebaInitialized = false
let jiebaInitPromise = null

const initJiebaOnce = async () => {
  if (jiebaInitialized && jiebaModule) {
    return
  }
  
  // 如果正在初始化，等待初始化完成
  if (jiebaInitPromise) {
    return jiebaInitPromise
  }
  
  jiebaInitPromise = (async () => {
    try {
      console.log('[WordCloud] Initializing jieba-wasm...')
      // 动态导入 jieba-wasm
      jiebaModule = await import('jieba-wasm')
      console.log('[WordCloud] Jieba module loaded, initializing...')
      await jiebaModule.default()
      jiebaInitialized = true
      console.log('[WordCloud] Jieba initialized successfully')
    } catch (error) {
      console.error('[WordCloud] Failed to initialize jieba:', error)
      jiebaInitPromise = null
      throw error
    }
  })()
  
  return jiebaInitPromise
}

const cutText = (text, useHMM = true) => {
  if (!jiebaModule || !jiebaInitialized) {
    throw new Error('Jieba not initialized')
  }
  if (!jiebaModule.cut) {
    throw new Error('Jieba cut function not available')
  }
  return jiebaModule.cut(text, useHMM)
}

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
const wordPositions = ref([])  // 词云位置数据
const showQuestionTypeModal = ref(false)
const showQuestionEditor = ref(false)
const editingQuestion = ref(null)
const activeMenu = ref(null)
const showDeleteConfirm = ref(false)
const deletingQuestionId = ref(null)

// 计时器相关
const questionTimers = ref({}) // 存储每个问题的开始时间 { questionId: startTime }
let timerInterval = null // 定时器

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
const showWordCloud = ref(false) // 默认不显示词云
const useExternalWordCloud = ref(true) // 默认使用外部API词云
const viewingSubQuestion = ref(null) // 当前查看的测验子问题

onMounted(() => {
  // 确保用户信息已加载
  userStore.loadUser()
  
  loadQuestions()
  // 添加全局点击事件监听
  document.addEventListener('click', handleClickOutside)
  
  // ✅ 设置WebSocket事件监听（主持人和学生都需要）
  setupWebSocketListeners()
  
  // 启动计时器更新
  startTimer()
})

onUnmounted(() => {
  // 移除监听
  document.removeEventListener('click', handleClickOutside)
  cleanupWebSocketListeners()
  
  // 清除定时器
  if (timerInterval) {
    clearInterval(timerInterval)
  }
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
  
  // 2. 处理计时器：每次开放时重新开始计时，关闭时清除计时器
  if (isOpen) {
    // 开放时重新开始计时（每次开放都重置）
    questionTimers.value[questionId] = Date.now()
  } else {
    // 关闭时清除计时器
    delete questionTimers.value[questionId]
  }
  
  // 3. 重新计算openQuestions
  openQuestions.value = questions.value.filter(q => q.isOpen)
  
  console.log('[InteractionTab] Open questions count:', openQuestions.value.length)
  
  // 4. 如果问题被开放了
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

// 获取问题类型名称
const getQuestionTypeName = (type) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'ESSAY': '开放问题',
    'QUIZ': '测验',
    'CHOICE': '选择题' // 兼容旧数据
  }
  return typeMap[type] || '问题'
}

const loadQuestions = async () => {
  try {
    const data = await api.question.getByClassroom(props.classroomId)
    // 处理每个问题的 questions 字段（如果是 JSON 字符串，需要解析）
    data.forEach(question => {
      if (question.questions && typeof question.questions === 'string') {
        try {
          question.questions = JSON.parse(question.questions)
        } catch (e) {
          console.error('[InteractionTab] Failed to parse questions JSON for question', question.id, ':', e)
          question.questions = []
        }
      } else if (!question.questions) {
        question.questions = []
      }
    })
    questions.value = data
    
    // 初始化计时器：为已开放的问题设置开始时间
    data.forEach(question => {
      if (question.isOpen) {
        // 如果问题已开放，重新开始计时（每次加载都重置）
        questionTimers.value[question.id] = Date.now()
      } else {
        // 如果问题未开放，清除计时器
        delete questionTimers.value[question.id]
      }
    })
    
    // 🔍 诊断日志
    console.log('[InteractionTab] Loaded questions:', data.map(q => ({
      id: q.id,
      type: q.type,
      content: q.content ? q.content.substring(0, 20) : '',
      isOpen: q.isOpen,
      isFinished: q.isFinished,
      questionsRaw: q.questions,
      questionsParsed: Array.isArray(q.questions) ? q.questions.length : 0
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

// 查看测验子问题的统计
const viewSubQuestionStats = async (subQuestion) => {
  viewingSubQuestion.value = subQuestion
  // 如果是选择题类型，加载统计
  if (subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE' || subQuestion.type === 'CHOICE') {
    // 注意：这里需要根据实际的API来加载子问题的统计
    // 如果子问题有独立的ID，可以使用 loadStatistics(subQuestion.id)
    // 如果没有，可能需要从测验的答案中筛选
    console.log('Viewing sub-question stats:', subQuestion)
  } else if (subQuestion.type === 'ESSAY') {
    // 如果是问答题，加载答案和词云
    // loadEssayAnswers(subQuestion.id)
    console.log('Viewing sub-question essay answers:', subQuestion)
  }
}

const selectQuestion = async (question) => {
  viewingSubQuestion.value = null // 清除子问题查看状态
  
  // 如果问题有 questions 字段（JSON字符串），需要解析
  if (question.questions && typeof question.questions === 'string') {
    try {
      question.questions = JSON.parse(question.questions)
      console.log('[InteractionTab] Parsed questions from JSON:', question.questions)
    } catch (e) {
      console.error('[InteractionTab] Failed to parse questions JSON:', e)
      question.questions = []
    }
  } else if (!question.questions) {
    question.questions = []
  }
  
  selectedQuestion.value = question
  console.log('[InteractionTab] Selected question:', {
    id: question.id,
    type: question.type,
    questionsCount: Array.isArray(question.questions) ? question.questions.length : 0,
    questions: question.questions
  })
  
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
    console.log('[WordCloud] Loaded essay answers:', data.length, 'answers')
    essayAnswers.value = data
    await generateWordCloud(data)
  } catch (error) {
    console.error('Failed to load essay answers:', error)
    wordFrequency.value = []
  }
}

// ✅ 生成词云数据（参考wordcloud库实现，使用jieba分词和停用词过滤）
const generateWordCloud = async (answers) => {
  if (!answers || answers.length === 0) {
    wordFrequency.value = []
    return
  }
  
  try {
    console.log('[WordCloud] Starting word cloud generation with', answers.length, 'answers')
    
    // 确保 jieba 已初始化
    await initJiebaOnce()
    
    // 合并所有答案文本（参考wordcloud_cn.py的实现）
    const allText = answers.map(a => a.content).join(' ')
    console.log('[WordCloud] Combined text length:', allText.length)
    
    // 配置参数（参考wordcloud库的参数）
    const config = {
      maxWords: 200,           // 最大词数（参考wordcloud默认值）
      minWordLength: 1,        // 最小词长度
      minFrequency: 1,         // 最小出现次数
      includeNumbers: false,   // 是否包含数字
      normalizePlurals: false  // 是否归一化复数（中文不需要）
    }
    
    // 使用jieba进行中文分词（参考wordcloud_cn.py的jieba_processing_txt函数）
    const words = []
    
    // 提取所有中文文本
    const chineseText = allText.replace(/[^\u4e00-\u9fa5a-zA-Z0-9\s]/g, ' ')
    
    try {
      // 使用jieba精确模式分词（cut_all=False，参考wordcloud_cn.py）
      const segments = cutText(chineseText, true)
      words.push(...segments.filter(s => s && s.trim().length > 0))
    } catch (error) {
      console.warn('Jieba segmentation failed, using fallback:', error)
      // 如果jieba失败，使用简单的字符分割作为后备方案
      const fallbackWords = chineseText.split(/\s+/).filter(s => s && s.trim().length > 0)
      words.push(...fallbackWords)
    }
    
    // 提取英文单词（参考wordcloud的regexp处理）
    const englishWords = allText.match(/[a-zA-Z]{2,}/g) || []
    englishWords.forEach(word => {
      words.push(word.toLowerCase())
    })
    
    // 过滤和统计词频（参考wordcloud的process_tokens逻辑）
    const frequency = {}
    
    words.forEach(word => {
      if (!word || word.trim().length === 0) {
        return
      }
      
      const trimmedWord = word.trim()
      
      // 最小词长度过滤（参考wordcloud的min_word_length参数）
      if (trimmedWord.length < config.minWordLength) {
        return
      }
      
      // 英文单词至少2个字符
      if (/^[a-zA-Z]+$/.test(trimmedWord) && trimmedWord.length < 2) {
        return
      }
      
      // 过滤停用词（参考wordcloud的stopwords处理）
      if (isStopword(trimmedWord)) {
        return
      }
      
      // 过滤纯数字（参考wordcloud的include_numbers参数）
      if (!config.includeNumbers && /^\d+$/.test(trimmedWord)) {
        return
      }
      
      // 过滤单个标点符号
      if (/^[^\u4e00-\u9fa5a-zA-Z0-9]+$/.test(trimmedWord)) {
        return
      }
      
      // 统计词频（参考wordcloud的generate_from_frequencies）
      const normalizedWord = trimmedWord.toLowerCase()
      frequency[normalizedWord] = (frequency[normalizedWord] || 0) + 1
    })
    
    // 转换为数组并排序（参考wordcloud的generate_from_frequencies排序逻辑）
    const sortedFrequencies = Object.entries(frequency)
      .filter(([word, count]) => count >= config.minFrequency)
      .sort((a, b) => b[1] - a[1])  // 按频率降序排序
      .slice(0, config.maxWords)     // 限制最大词数
    
    if (sortedFrequencies.length === 0) {
      console.warn('[WordCloud] No valid words found after filtering')
      wordFrequency.value = []
      return
    }
    
    // 归一化频率（参考wordcloud的频率归一化，最大频率为1）
    const maxFrequency = sortedFrequencies[0][1]
    wordFrequency.value = sortedFrequencies.map(([word, count]) => ({
      word,
      count,
      normalizedFrequency: count / maxFrequency  // 归一化频率，用于后续字体大小计算
    }))
    
    // 计算词云位置（避免重叠）
    await calculateWordPositions()
    
    console.log('[WordCloud] Generated', wordFrequency.value.length, 'words')
    console.log('[WordCloud] Top words:', wordFrequency.value.slice(0, 10))
    console.log('[WordCloud] Frequency range:', {
      min: wordFrequency.value[wordFrequency.value.length - 1]?.count || 0,
      max: wordFrequency.value[0]?.count || 0
    })
  } catch (error) {
    console.error('[WordCloud] Failed to generate word cloud:', error)
    console.error('[WordCloud] Error details:', error.stack)
    wordFrequency.value = []
  }
}

// ✅ 根据词频计算字体大小（参考wordcloud的字体大小计算，使用归一化频率）
const getWordSize = (count, normalizedFrequency) => {
  // 使用归一化频率（如果可用），否则使用count计算
  const ratio = normalizedFrequency !== undefined ? normalizedFrequency : (count / (wordFrequency.value[0]?.count || 1))
  const minSize = 20
  const maxSize = 56
  // 使用相对缩放（参考wordcloud的relative_scaling参数，默认0.5）
  const relativeScaling = 0.5
  return minSize + ratio * relativeScaling * (maxSize - minSize) + (1 - relativeScaling) * (maxSize - minSize) * (1 - ratio)
}

// ✅ 根据词频获取样式类和颜色（使用归一化频率）
const getWordClass = (count, normalizedFrequency) => {
  // 使用归一化频率（如果可用），否则使用count计算
  const ratio = normalizedFrequency !== undefined ? normalizedFrequency : (count / (wordFrequency.value[0]?.count || 1))
  
  if (ratio >= 0.8) {
    return 'word-tag-large'
  } else if (ratio >= 0.5) {
    return 'word-tag-medium'
  } else {
    return 'word-tag-small'
  }
}

// ✅ 根据词频获取颜色（使用协调的渐变色方案，参考wordcloud的颜色函数）
const getWordColor = (count, index, normalizedFrequency) => {
  // 使用归一化频率（如果可用），否则使用count计算
  const ratio = normalizedFrequency !== undefined ? normalizedFrequency : (count / (wordFrequency.value[0]?.count || 1))
  
  // 使用协调的渐变色方案（参考 simple-word-cloud）

const colorPalette = [
  // 高频词 - 深灰（带微调）
  '#121212', // 纯黑（有层次）
  '#2A2A2A', // 深炭灰
  '#3D3D3D', // 炭灰
  '#4F4F4F', // 中深灰
  
  // 中频词 - 中灰
  '#696969', // 暗灰
  '#808080', // 标准灰
  '#A9A9A9', // 暗灰2
  '#C0C0C0', // 银灰
  
  // 低频词 - 浅灰
  '#D3D3D3', // 浅灰
  '#DCDCDC', // 更浅灰
  '#E8E8E8', // 淡灰
  '#F0F0F0'  // 非常淡灰
]
  
  // 根据频率和索引选择颜色，确保颜色分布均匀
  if (ratio >= 0.8) {
    // 高频词使用深色，更醒目
    return colorPalette[index % 4]
  } else if (ratio >= 0.5) {
    // 中频词使用中等色
    return colorPalette[4 + (index % 4)]
  } else {
    // 低频词使用浅色
    return colorPalette[8 + (index % 4)]
  }
}

// ✅ 测量文本尺寸
let measureCanvas = null
let measureCtx = null

const measureTextSize = (text, fontSize) => {
  if (!measureCanvas) {
    measureCanvas = document.createElement('canvas')
    measureCtx = measureCanvas.getContext('2d')
  }
  measureCtx.font = `${fontSize}px sans-serif`
  const metrics = measureCtx.measureText(text)
  return {
    width: Math.ceil(metrics.width),
    height: Math.ceil(metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent)
  }
}

// ✅ 计算旋转后的包围框
const getRotatedBounds = (width, height, rotation) => {
  if (rotation === 0) {
    return { width, height }
  }
  const rad = (rotation * Math.PI) / 180
  const w = width * Math.abs(Math.cos(rad)) + height * Math.abs(Math.sin(rad))
  const h = width * Math.abs(Math.sin(rad)) + height * Math.abs(Math.cos(rad))
  return {
    width: Math.ceil(w),
    height: Math.ceil(h)
  }
}

// ✅ 基于 simple-word-cloud 算法的矩形螺旋扩散位置计算
const getSpiralPosition = (curWordItem, elWidth, elHeight, centerX, centerY, margin, placedWords) => {
  let startX, endX, startY, endY
  // 第一个文本的中心点
  startX = endX = centerX
  startY = endY = centerY

  // 根据容器的宽高来计算扩散步长（参考 simple-word-cloud）
  let stepLeft = 1
  let stepTop = 1
  if (elWidth > elHeight) {
    stepLeft = 1
    stepTop = elHeight / elWidth
  } else if (elHeight > elWidth) {
    stepTop = 1
    stepLeft = elWidth / elHeight
  }

  // 检查中心点是否可以放置
  if (canFitWord(curWordItem, [startX, startY], placedWords, margin, elWidth, elHeight)) {
    return [startX, startY]
  }

  // 依次扩散遍历每个像素点（矩形螺旋）
  while (true) {
    const curStartX = Math.floor(startX)
    const curStartY = Math.floor(startY)
    const curEndX = Math.floor(endX)
    const curEndY = Math.floor(endY)

    // 遍历矩形右侧的边
    for (let top = curStartY; top < curEndY; ++top) {
      const value = [curEndX, top]
      if (canFitWord(curWordItem, value, placedWords, margin, elWidth, elHeight)) {
        return value
      }
    }
    // 遍历矩形下面的边
    for (let left = curEndX; left > curStartX; --left) {
      const value = [left, curEndY]
      if (canFitWord(curWordItem, value, placedWords, margin, elWidth, elHeight)) {
        return value
      }
    }
    // 遍历矩形左侧的边
    for (let top = curEndY; top > curStartY; --top) {
      const value = [curStartX, top]
      if (canFitWord(curWordItem, value, placedWords, margin, elWidth, elHeight)) {
        return value
      }
    }
    // 遍历矩形上面的边
    for (let left = curStartX; left < curEndX; ++left) {
      const value = [left, curStartY]
      if (canFitWord(curWordItem, value, placedWords, margin, elWidth, elHeight)) {
        return value
      }
    }
    // 向四周扩散
    startX -= stepLeft
    endX += stepLeft
    startY -= stepTop
    endY += stepTop

    // 防止无限循环
    if (startX < -elWidth || endX > elWidth * 2 || startY < -elHeight || endY > elHeight * 2) {
      break
    }
  }
  
  // 如果找不到位置，返回中心点（作为后备）
  return [centerX, centerY]
}

// ✅ 检查位置是否可以放置词（改进的碰撞检测）
const canFitWord = (curWordItem, [cx, cy], placedWords, margin, containerWidth, containerHeight) => {
  const halfWidth = curWordItem.width / 2
  const halfHeight = curWordItem.height / 2
  
  // 检查是否在容器边界内
  if (cx - halfWidth < margin || cx + halfWidth > containerWidth - margin ||
      cy - halfHeight < margin || cy + halfHeight > containerHeight - margin) {
    return false
  }
  
  // 检查是否与已放置的词重叠（使用更严格的间距）
  const minSpacing = 20 // 最小间距
  for (const placed of placedWords) {
    const dx = Math.abs(cx - placed.x)
    const dy = Math.abs(cy - placed.y)
    const distance = Math.sqrt(dx * dx + dy * dy)
    
    // 计算两个词的边界框半径
    const currentRadius = Math.sqrt(curWordItem.width * curWordItem.width + curWordItem.height * curWordItem.height) / 2
    const existingRadius = Math.sqrt(placed.width * placed.width + placed.height * placed.height) / 2
    
    // 如果距离小于两个半径之和加上最小间距，则重叠
    if (distance < currentRadius + existingRadius + minSpacing) {
      return false
    }
  }
  
  return true
}

// ✅ 计算词云位置（使用矩形螺旋扩散算法）
const calculateWordPositions = async () => {
  if (wordFrequency.value.length === 0) {
    wordPositions.value = []
    return
  }
  
  const placedWords = []
  // 使用相对尺寸而不是固定尺寸，增大容器以提供更多空间
  const containerWidth = 800
  const containerHeight = 500
  const centerX = containerWidth / 2
  const centerY = containerHeight / 2
  const margin = 40 // 边距
  
  // 第一个词放在中心
  const firstWord = wordFrequency.value[0]
  const firstFontSize = getWordSize(firstWord.count, firstWord.normalizedFrequency)
  const firstSize = measureTextSize(firstWord.word, firstFontSize)
  const firstBounds = getRotatedBounds(firstSize.width, firstSize.height, 0)
  
  const firstWordItem = {
    x: centerX,
    y: centerY,
    width: firstBounds.width,
    height: firstBounds.height,
    rotation: 0
  }
  
  placedWords.push(firstWordItem)
  wordPositions.value = [{
    x: centerX,
    y: centerY,
    rotation: 0
  }]
  
  // 从中心向四周扩散，为其他词找位置（使用矩形螺旋算法）
  for (let i = 1; i < wordFrequency.value.length; i++) {
    const word = wordFrequency.value[i]
    const fontSize = getWordSize(word.count, word.normalizedFrequency)
    const rotation = (Math.sin(i * 2.3) * 10) // -10到10度
    const textSize = measureTextSize(word.word, fontSize)
    const bounds = getRotatedBounds(textSize.width, textSize.height, rotation)
    
    const curWordItem = {
      width: bounds.width,
      height: bounds.height,
      rotation
    }
    
    // 使用矩形螺旋算法找到位置
    const [x, y] = getSpiralPosition(
      curWordItem,
      containerWidth,
      containerHeight,
      centerX,
      centerY,
      margin,
      placedWords
    )
    
    // 确保在容器内
    const halfWidth = bounds.width / 2
    const halfHeight = bounds.height / 2
    const finalX = Math.max(margin + halfWidth, Math.min(containerWidth - margin - halfWidth, x))
    const finalY = Math.max(margin + halfHeight, Math.min(containerHeight - margin - halfHeight, y))
    
    placedWords.push({
      x: finalX,
      y: finalY,
      width: bounds.width,
      height: bounds.height,
      rotation
    })
    
    wordPositions.value.push({
      x: finalX,
      y: finalY,
      rotation
    })
  }
  
  // ✅ 计算词云整体边界，然后自适应缩放和居中（参考 simple-word-cloud 的 fitContainer）
  let minX = Infinity, maxX = -Infinity
  let minY = Infinity, maxY = -Infinity
  
  wordPositions.value.forEach((pos, index) => {
    const word = wordFrequency.value[index]
    if (!word) return
    
    const fontSize = getWordSize(word.count, word.normalizedFrequency)
    const textSize = measureTextSize(word.word, fontSize)
    const bounds = getRotatedBounds(textSize.width, textSize.height, pos.rotation)
    
    const halfWidth = bounds.width / 2
    const halfHeight = bounds.height / 2
    
    minX = Math.min(minX, pos.x - halfWidth)
    maxX = Math.max(maxX, pos.x + halfWidth)
    minY = Math.min(minY, pos.y - halfHeight)
    maxY = Math.max(maxY, pos.y + halfHeight)
  })
  
  // 计算词云的实际宽高和中心
  const wordCloudWidth = maxX - minX
  const wordCloudHeight = maxY - minY
  const wordCloudLeft = minX
  const wordCloudTop = minY
  
  // 计算容器的可用空间（考虑边距）
  const availableWidth = containerWidth - margin * 2
  const availableHeight = containerHeight - margin * 2
  
  // 计算宽高比（参考 simple-word-cloud）
  const elRatio = availableWidth / availableHeight
  const wordCloudRatio = wordCloudWidth / wordCloudHeight
  
  let w, h
  let offsetX = 0
  let offsetY = 0
  
  if (elRatio > wordCloudRatio) {
    // 词云高度以容器高度为准，宽度根据原比例进行缩放
    h = availableHeight
    w = wordCloudRatio * availableHeight
  } else {
    // 词云宽度以容器宽度为准，高度根据原比例进行缩放
    w = availableWidth
    h = availableWidth / wordCloudRatio
  }
  
  const scale = w / wordCloudWidth
  
  // 将词云移动到容器中间（参考 simple-word-cloud）
  const scaledLeft = wordCloudLeft * scale
  const scaledTop = wordCloudTop * scale
  
  if (elRatio > wordCloudRatio) {
    offsetY = -scaledTop
    offsetX = -scaledLeft + (availableWidth - w) / 2 + margin
  } else {
    offsetX = -scaledLeft
    offsetY = -scaledTop + (availableHeight - h) / 2 + margin
  }
  
  // 应用缩放和偏移
  wordPositions.value.forEach((pos, index) => {
    const word = wordFrequency.value[index]
    if (!word) return
    
    // 缩放位置
    pos.x = pos.x * scale + offsetX
    pos.y = pos.y * scale + offsetY
    
    // 保存缩放比例，用于字体大小调整
    pos.scale = scale
    
    // 修正超出容器的词（参考 simple-word-cloud）
    const fontSize = getWordSize(word.count, word.normalizedFrequency)
    const textSize = measureTextSize(word.word, fontSize * scale)
    const bounds = getRotatedBounds(textSize.width, textSize.height, pos.rotation)
    
    if (pos.x + bounds.width / 2 > containerWidth - margin) {
      pos.x = containerWidth - margin - bounds.width / 2
    }
    if (pos.x - bounds.width / 2 < margin) {
      pos.x = margin + bounds.width / 2
    }
    if (pos.y + bounds.height / 2 > containerHeight - margin) {
      pos.y = containerHeight - margin - bounds.height / 2
    }
    if (pos.y - bounds.height / 2 < margin) {
      pos.y = margin + bounds.height / 2
    }
  })
  
}

// ✅ 获取词的样式
const getWordStyle = (word, index) => {
  const baseFontSize = getWordSize(word.count, word.normalizedFrequency)
  const pos = wordPositions.value[index] || { x: 0, y: 0, rotation: 0, scale: 1 }
  
  // 应用缩放比例到字体大小
  const fontSize = baseFontSize * (pos.scale || 1)
  
  const animationDelay = index * 0.05
  
  // 计算相对于容器的百分比位置（与 calculateWordPositions 中保持一致）
  const containerWidth = 800
  const containerHeight = 500
  
  // 将绝对位置转换为百分比
  const percentX = (pos.x / containerWidth) * 100
  const percentY = (pos.y / containerHeight) * 100
  
  return {
    fontSize: fontSize + 'px',
    left: percentX + '%',  // 使用百分比定位
    top: percentY + '%',   // 使用百分比定位
    transform: `translate(-50%, -50%) rotate(${pos.rotation || 0}deg)`,
    '--word-index': index,
    '--animation-delay': animationDelay + 's'
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

const editQuestion = async (question) => {
  try {
    // 重新加载完整的问题数据，确保包含所有选项
    const fullQuestion = await api.question.getById(question.id)
    
    // 处理 questions 字段（如果是 JSON 字符串，需要解析）
    if (fullQuestion.questions && typeof fullQuestion.questions === 'string') {
      try {
        fullQuestion.questions = JSON.parse(fullQuestion.questions)
        console.log('[InteractionTab] Parsed questions for editing:', fullQuestion.questions)
      } catch (e) {
        console.error('[InteractionTab] Failed to parse questions JSON:', e)
        fullQuestion.questions = []
      }
    } else if (!fullQuestion.questions) {
      fullQuestion.questions = []
    }
    
    editingQuestion.value = fullQuestion
    showQuestionEditor.value = true
    activeMenu.value = null
    
    console.log('[InteractionTab] Editing question:', {
      id: fullQuestion.id,
      type: fullQuestion.type,
      questionsCount: Array.isArray(fullQuestion.questions) ? fullQuestion.questions.length : 0,
      questions: fullQuestion.questions
    })
  } catch (error) {
    console.error('Failed to load question for editing:', error)
    // 如果加载失败，使用原始数据作为后备
    editingQuestion.value = { ...question }
    showQuestionEditor.value = true
    activeMenu.value = null
  }
}

const deleteQuestion = (questionId) => {
  deletingQuestionId.value = questionId
  showDeleteConfirm.value = true
  activeMenu.value = null
}

const confirmDelete = async () => {
  if (!deletingQuestionId.value) return
  
  try {
    await api.question.delete(deletingQuestionId.value)
    loadQuestions()
    showDeleteConfirm.value = false
    deletingQuestionId.value = null
  } catch (error) {
    console.error('Failed to delete question:', error)
    alert('删除失败，请重试')
  }
}

const cancelDelete = () => {
  showDeleteConfirm.value = false
  deletingQuestionId.value = null
}

// 计时器相关函数
const startTimer = () => {
  // 每秒更新一次计时器显示
  // 使用响应式变量来触发更新
  timerInterval = setInterval(() => {
    // 通过更新对象引用来触发Vue的响应式更新
    questionTimers.value = { ...questionTimers.value }
  }, 1000)
}

// 格式化计时器显示（HH:MM:SS）
const formatTimer = (startTime) => {
  if (!startTime) return '00:00:00'
  
  const now = Date.now()
  const elapsed = Math.floor((now - startTime) / 1000) // 经过的秒数
  
  const hours = Math.floor(elapsed / 3600)
  const minutes = Math.floor((elapsed % 3600) / 60)
  const seconds = elapsed % 60
  
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
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
  
  if (!userStore.currentUser || !userStore.currentUser.id) {
    console.error('[InteractionTab] Cannot submit: currentUser is null or missing id')
    alert('用户信息未加载，请刷新页面重新登录')
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
  
  if (!userStore.currentUser || !userStore.currentUser.id) {
    console.error('[InteractionTab] Cannot submit: currentUser is null or missing id')
    alert('用户信息未加载，请刷新页面重新登录')
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

// 将wordFrequency转换为外部API需要的keywords格式
const wordCloudKeywords = computed(() => {
  if (!wordFrequency.value || wordFrequency.value.length === 0) {
    console.log('[WordCloud] wordFrequency is empty, returning empty keywords')
    return {}
  }
  const keywords = {}
  wordFrequency.value.forEach(item => {
    keywords[item.word] = item.count.toString()
  })
  console.log('[WordCloud] Generated keywords:', Object.keys(keywords).length, 'words')
  return keywords
})
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
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: #f5f5f5;
  border-top: 1px solid #ddd;
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
  gap: 16px;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%);
  border-radius: 8px;
  border: 1px solid rgba(102, 126, 234, 0.2);
}

.timer-icon {
  width: 20px;
  height: 20px;
  color: #667eea;
  flex-shrink: 0;
}

.timer-text {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.5px;
}

.timer-not-started {
  color: #999;
  font-weight: 500;
}

/* 开关包装器样式 */
.toggle-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toggle-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  min-width: 32px;
  text-align: right;
}

/* 滑动开关样式 */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 52px;
  height: 28px;
  vertical-align: middle;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 28px;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

.toggle-slider:before {
  position: absolute;
  content: "";
  height: 22px;
  width: 22px;
  left: 3px;
  bottom: 3px;
  background: linear-gradient(135deg, #ffffff 0%, #f7fafc 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 50%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2), 0 1px 2px rgba(0, 0, 0, 0.1);
}

.toggle-switch input:checked + .toggle-slider {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.toggle-switch input:checked + .toggle-slider:before {
  transform: translateX(24px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15), 0 1px 3px rgba(0, 0, 0, 0.1);
}

.toggle-switch input:focus + .toggle-slider {
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 3px rgba(76, 175, 80, 0.2);
}

.toggle-switch:hover .toggle-slider {
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 3px rgba(76, 175, 80, 0.15);
}

.toggle-switch:hover input:checked + .toggle-slider {
  background: linear-gradient(135deg, #5cbf60 0%, #4caf50 100%);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1), 0 0 0 3px rgba(76, 175, 80, 0.2);
}

.toggle-switch:active .toggle-slider:before {
  transform: scale(0.95);
}

.toggle-switch input:checked:active + .toggle-slider:before {
  transform: translateX(24px) scale(0.95);
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

/* 测验子问题样式 */
.quiz-subquestions-section {
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 20px;
}

.quiz-subquestions-section h4 {
  margin-bottom: 16px;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.quiz-subquestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quiz-subquestion-item {
  padding: 16px;
  background: white;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  transition: all 0.2s;
}

.quiz-subquestion-item:hover {
  border-color: #d0d0d0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.quiz-subquestion-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.quiz-subquestion-number {
  font-weight: 600;
  color: #667eea;
  font-size: 14px;
}

.quiz-subquestion-type {
  padding: 4px 10px;
  background: #e8f0ff;
  color: #667eea;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.quiz-subquestion-status {
  margin-left: auto;
  padding: 4px 10px;
  background: #f5f5f5;
  color: #999;
  border-radius: 6px;
  font-size: 12px;
}

.quiz-subquestion-status.open {
  background: #e8f5e9;
  color: #4caf50;
}

.quiz-subquestion-content {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 8px;
}

.quiz-subquestion-stats {
  margin-top: 8px;
}

.view-stats-btn {
  padding: 6px 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.view-stats-btn:hover {
  background: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.no-subquestions {
  text-align: center;
  padding: 40px;
  color: #999;
  font-size: 14px;
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
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  /* 确保词云容器居中 */
  display: flex;
  flex-direction: column;
  align-items: center;
}

.wordcloud-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  margin-bottom: 16px;
}

.wordcloud-section h4 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.wordcloud-controls {
  display: flex;
  align-items: center;
  gap: 16px;
}

.wordcloud-toggle {
  display: flex;
  align-items: center;
}

.toggle-label-small {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.toggle-label-small input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.external-wordcloud {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.wordcloud-container {
  /* 去掉固定宽高，改为相对单位 */
  width: 100%;
  min-height: 400px;
  padding: 30px; /* 减小内边距 */
  background: white;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  position: relative;
  overflow: hidden;
  /* 添加flex布局使内容居中 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.wordcloud-container::before {
  display: none; /* 隐藏装饰背景 */
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.5;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.word-tags {
  /* 使用固定宽高比，与计算逻辑保持一致，同时支持响应式 */
  position: relative;
  width: 800px;
  height: 500px;
  max-width: calc(100% - 60px); /* 考虑父容器的padding */
  max-height: calc(100% - 60px);
  aspect-ratio: 800 / 500; /* 保持宽高比 */
  margin: 0 auto;
  z-index: 1;
  /* 确保内容居中 */
  display: flex;
  justify-content: center;
  align-items: center;
}

.word-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: default;
  position: absolute;
  overflow: hidden;
  white-space: nowrap;
  animation: fadeInScale 0.6s ease-out var(--animation-delay, 0s) both;
  transform-origin: center center;
  line-height: 1.2;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.3) rotate(0deg);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.word-tag-large {
  background: transparent;
  font-weight: 700;
  padding: 4px 8px;
  letter-spacing: 0.5px;
}

.word-tag-medium {
  background: transparent;
  font-weight: 600;
  padding: 4px 8px;
  letter-spacing: 0.3px;
}

.word-tag-small {
  background: transparent;
  font-weight: 500;
  padding: 4px 8px;
  letter-spacing: 0.2px;
}

.word-tag:hover {
  transform: translate(-50%, -50%) scale(1.08) !important;
  z-index: 10;
  opacity: 0.9;
}

.word-count {
  font-size: 0.65em;
  opacity: 0.7;
  margin-left: 4px;
  font-weight: 400;
}

.no-wordcloud {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 60px 40px;
  position: relative;
  z-index: 1;
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
  margin-bottom: 20px;
}

.question-nav button {
  padding: 6px 10px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 24px;
  font-weight: 300;
  cursor: pointer;
  transition: background-color 0.2s;
  min-width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.question-nav button:hover:not(.disabled):not(:disabled) {
  background: #5568d3;
}

.question-nav button:active:not(.disabled):not(:disabled) {
  background: #4a5bc4;
}

.question-nav button.disabled,
.question-nav button:disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
  opacity: 0.6;
}

.question-nav button.disabled:hover,
.question-nav button:disabled:hover {
  background: #e0e0e0;
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

/* 问题类型选择弹窗样式 */
.type-select-modal {
  max-width: 600px;
  width: 90%;
  animation: slideUp 0.3s ease-out;
}

.type-select-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 24px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;
}

.type-select-modal .modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.3px;
}

.type-select-modal .close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  line-height: 1;
}

.type-select-modal .close-btn:hover {
  background: #e8e8e8;
  color: #333;
  transform: rotate(90deg);
}

.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.type-card {
  padding: 24px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border: 2px solid #e8f0ff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.type-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%);
}

.type-card:active {
  transform: translateY(-2px);
}

.type-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-icon svg {
  width: 100%;
  height: 100%;
}

.type-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.type-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
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

/* 删除确认弹窗样式 */
.delete-overlay {
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.delete-modal {
  max-width: 480px;
  width: 90%;
  animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

@keyframes slideUp {
  from {
    transform: translateY(30px) scale(0.95);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.delete-modal .modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid #f0f0f0;
}

.delete-modal .modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.3px;
}

.delete-modal .close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 8px;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  line-height: 1;
}

.delete-modal .close-btn:hover {
  background: #e8e8e8;
  color: #333;
  transform: rotate(90deg);
}

.delete-content {
  padding: 32px 28px;
  text-align: center;
}

.delete-icon-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.delete-icon-bg {
  position: absolute;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
}

.delete-icon {
  position: relative;
  width: 48px;
  height: 48px;
  color: #667eea;
  z-index: 1;
}

.delete-text-content {
  margin-top: 8px;
}

.delete-message {
  font-size: 18px;
  color: #1a1a1a;
  font-weight: 600;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.delete-warning {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.6;
}

.delete-modal .modal-actions {
  display: flex;
  gap: 12px;
  padding: 20px 28px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}

.delete-modal .cancel-btn,
.delete-modal .delete-confirm-btn {
  flex: 1;
  padding: 14px 24px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-modal .cancel-btn {
  background: white;
  color: #666;
  border: 2px solid #e8e8e8;
}

.delete-modal .cancel-btn:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
  color: #333;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.delete-modal .delete-confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.delete-modal .delete-confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  background: linear-gradient(135deg, #5568d3 0%, #6a4c93 100%);
}

.delete-modal .delete-confirm-btn:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}
</style>

