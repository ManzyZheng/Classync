<template>
  <transition name="overlay-fade">
    <div v-if="visible" class="question-overlay">
      <div class="overlay-container">
        <!-- 问题标题 -->
        <div class="question-header">
          <h1 class="question-title">{{ question?.content }}</h1>
          <div class="question-type">
            {{ getQuestionTypeText(question?.type) }}
          </div>
        </div>

        <!-- 选项列表 -->
        <div v-if="question?.type !== 'ESSAY'" class="options-container">
          <div 
            v-for="(option, index) in options" 
            :key="option.id" 
            class="option-item"
            :class="{ 'has-result': showResults }"
          >
            <div class="option-content">
              <span class="option-label">{{ getOptionLabel(index) }}</span>
              <span class="option-text">{{ option.content }}</span>
            </div>
            
            <!-- 结果条形图 -->
            <div v-if="showResults && statistics" class="option-result">
              <div class="result-bar-container">
                <div 
                  class="result-bar" 
                  :style="{ width: getPercentage(option.content) + '%' }"
                  :class="{ 'is-correct': showAnswer && option.isCorrect }"
                >
                </div>
              </div>
              <div class="result-stats">
                <span class="result-count">{{ getCount(option.content) }} 人</span>
                <span class="result-percentage">{{ getPercentage(option.content) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 简答题提示 -->
        <div v-else class="essay-section">
          <!-- 显示答案模式：展示词云 -->
          <div v-if="showAnswer && wordFrequency.length > 0" class="essay-wordcloud">
            <div class="wordcloud-container">
              <span 
                v-for="(word, index) in wordFrequency" 
                :key="index"
                class="word-tag"
                :style="getWordStyleForDisplay(word, index)"
              >
                {{ word.word }}
              </span>
            </div>
            <p class="essay-count">基于 {{ essayAnswers?.length || 0 }} 份答案</p>
          </div>
          
          <!-- 问题模式或结果模式：显示提示 -->
          <div v-else class="essay-hint">
            <p>简答题正在收集中...</p>
            <p v-if="showResults" class="essay-count">已收到 {{ essayAnswers?.length || 0 }} 份答案</p>
          </div>
        </div>

        <!-- 底部提示 -->
        <div class="overlay-footer">
          <div v-if="!showResults" class="footer-hint">
            <span class="pulse-dot"></span>
            <span>学生正在作答...</span>
          </div>
          <div v-else-if="showAnswer" class="footer-hint">
            <span>✅</span>
            <span>正确答案</span>
          </div>
          <div v-else class="footer-hint">
            <span>📊</span>
            <span>实时统计结果</span>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { generateWordCloud, calculateWordPositions, getWordStyle } from '../utils/wordcloud'
import websocket, { WS_EVENTS } from '../utils/websocket'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  questionId: {
    type: Number,
    default: null
  },
  mode: {
    type: String,
    default: 'QUESTION_ONLY'  // QUESTION_ONLY, SHOW_RESULTS, SHOW_ANSWER
  }
})

const question = ref(null)
const options = ref([])
const statistics = ref(null)
const essayAnswers = ref([])
const wordFrequency = ref([])
const wordPositions = ref([])

const showResults = computed(() => props.mode === 'SHOW_RESULTS' || props.mode === 'SHOW_ANSWER')
const showAnswer = computed(() => props.mode === 'SHOW_ANSWER')

// 获取问题类型文本
const getQuestionTypeText = (type) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'ESSAY': '简答题',
    'QUIZ': '测验'
  }
  return typeMap[type] || '互动问题'
}

// 获取选项标签（A, B, C, D...）
const getOptionLabel = (index) => {
  return String.fromCharCode(65 + index)
}

// 获取统计数据中的计数
const getCount = (optionContent) => {
  if (!statistics.value) return 0
  const stat = statistics.value.find(s => s.optionContent === optionContent)
  return stat ? stat.count : 0
}

// 获取统计数据中的百分比
const getPercentage = (optionContent) => {
  if (!statistics.value) return 0
  const stat = statistics.value.find(s => s.optionContent === optionContent)
  return stat ? Math.round(stat.percentage) : 0
}

// 获取词云样式
const getWordStyleForDisplay = (word, index) => {
  // 使用更大的容器尺寸适应放映页
  return getWordStyle(word, index, wordFrequency.value, wordPositions.value, 1000, 600)
}

// 监听 questionId 变化，加载问题数据
watch(() => props.questionId, async (newId) => {
  if (!newId) {
    question.value = null
    options.value = []
    statistics.value = null
    essayAnswers.value = []
    wordFrequency.value = []
    wordPositions.value = []
    return
  }

  try {
    // 加载问题详情
    const response = await fetch(`/api/questions/${newId}`)
    const data = await response.json()
    console.log('[QuestionOverlay] Loaded question data:', data)
    
    // 后端返回的是 QuestionWithOptionsDTO，直接就是问题对象，不是嵌套的 { question, options }
    question.value = {
      id: data.id,
      classroomId: data.classroomId,
      type: data.type,
      content: data.content,
      questions: data.questions,
      isOpen: data.isOpen,
      isFinished: data.isFinished
    }
    options.value = data.options || []
    
    console.log('[QuestionOverlay] Question content:', question.value?.content)
    console.log('[QuestionOverlay] Options:', options.value)
    
    // 如果需要显示结果，加载统计数据
    if (showResults.value && question.value.type !== 'ESSAY') {
      const statsResponse = await fetch(`/api/answers/statistics/${newId}`)
      statistics.value = await statsResponse.json()
      console.log('[QuestionOverlay] Statistics:', statistics.value)
    }
    
    // 如果是简答题且需要显示结果或答案，加载答案数据
    if (showResults.value && question.value.type === 'ESSAY') {
      const answersResponse = await fetch(`/api/answers/question/${newId}`)
      essayAnswers.value = await answersResponse.json()
      console.log('[QuestionOverlay] Essay answers loaded:', essayAnswers.value.length)
      
      // 如果是显示答案模式，生成词云
      if (showAnswer.value && essayAnswers.value.length > 0) {
        console.log('[QuestionOverlay] Generating word cloud (initial load)...')
        wordFrequency.value = await generateWordCloud(essayAnswers.value)
        console.log('[QuestionOverlay] Word frequency:', wordFrequency.value.length, 'words')
        if (wordFrequency.value.length > 0) {
          wordPositions.value = await calculateWordPositions(wordFrequency.value, 1000, 600)
          console.log('[QuestionOverlay] Word positions calculated:', wordPositions.value.length)
        }
      }
    }
  } catch (error) {
    console.error('Failed to load question:', error)
  }
}, { immediate: true })

// 监听 mode 变化，重新加载统计数据
watch(() => props.mode, async (newMode) => {
  console.log('[QuestionOverlay] Mode changed to:', newMode)
  console.log('[QuestionOverlay] Question type:', question.value?.type)
  
  if ((newMode === 'SHOW_RESULTS' || newMode === 'SHOW_ANSWER') && props.questionId) {
    try {
      if (question.value?.type !== 'ESSAY') {
        const statsResponse = await fetch(`/api/answers/statistics/${props.questionId}`)
        statistics.value = await statsResponse.json()
      } else {
        // 简答题：加载所有答案
        const answersResponse = await fetch(`/api/answers/question/${props.questionId}`)
        essayAnswers.value = await answersResponse.json()
        console.log('[QuestionOverlay] Essay answers loaded:', essayAnswers.value.length)
        
        // 如果是显示答案模式，生成词云
        if (newMode === 'SHOW_ANSWER' && essayAnswers.value.length > 0) {
          await regenerateWordCloud()
        } else if (newMode === 'SHOW_ANSWER') {
          console.warn('[QuestionOverlay] No essay answers to generate word cloud from')
        }
      }
    } catch (error) {
      console.error('Failed to load statistics:', error)
    }
  } else if (newMode === 'QUESTION_ONLY') {
    statistics.value = null
    essayAnswers.value = []
    wordFrequency.value = []
    wordPositions.value = []
  }
})

// 重新生成词云
const regenerateWordCloud = async () => {
  if (!essayAnswers.value || essayAnswers.value.length === 0) {
    console.log('[QuestionOverlay] No essay answers to generate word cloud')
    wordFrequency.value = []
    wordPositions.value = []
    return
  }
  
  console.log('[QuestionOverlay] Regenerating word cloud with', essayAnswers.value.length, 'answers')
  try {
    wordFrequency.value = await generateWordCloud(essayAnswers.value)
    console.log('[QuestionOverlay] Generated word frequency:', wordFrequency.value.length, 'words')
    
    if (wordFrequency.value.length > 0) {
      wordPositions.value = await calculateWordPositions(wordFrequency.value, 1000, 600)
      console.log('[QuestionOverlay] Calculated word positions:', wordPositions.value.length)
    }
  } catch (error) {
    console.error('[QuestionOverlay] Failed to regenerate word cloud:', error)
  }
}

// WebSocket 监听：实时更新答案和统计
const handleAnswerSubmitted = async (payload) => {
  console.log('[QuestionOverlay] Answer submitted event:', payload)
  console.log('[QuestionOverlay] Current question type:', question.value?.type)
  console.log('[QuestionOverlay] Current mode:', props.mode)
  console.log('[QuestionOverlay] showAnswer:', showAnswer.value, 'showResults:', showResults.value)
  
  // 检查是否是当前问题的答案
  if (payload.questionId !== props.questionId) {
    console.log('[QuestionOverlay] Question ID mismatch, ignoring')
    return
  }
  
  // 如果是简答题且正在显示结果或答案，重新加载答案
  if (question.value?.type === 'ESSAY' && showResults.value) {
    try {
      const answersResponse = await fetch(`/api/answers/question/${props.questionId}`)
      essayAnswers.value = await answersResponse.json()
      console.log('[QuestionOverlay] Real-time: Essay answers updated to', essayAnswers.value.length)
      
      // 如果是显示答案模式，重新生成词云
      if (showAnswer.value) {
        console.log('[QuestionOverlay] Regenerating word cloud...')
        await regenerateWordCloud()
      }
    } catch (error) {
      console.error('[QuestionOverlay] Failed to reload essay answers:', error)
    }
  }
  // 如果是选择题且正在显示结果，更新统计数据
  else if (question.value?.type !== 'ESSAY' && showResults.value) {
    try {
      const statsResponse = await fetch(`/api/answers/statistics/${props.questionId}`)
      statistics.value = await statsResponse.json()
      console.log('[QuestionOverlay] Real-time: Statistics updated')
    } catch (error) {
      console.error('[QuestionOverlay] Failed to reload statistics:', error)
    }
  }
}

// 组件挂载时订阅 WebSocket 事件
onMounted(() => {
  websocket.on(WS_EVENTS.ANSWER_SUBMITTED, handleAnswerSubmitted)
  console.log('[QuestionOverlay] Subscribed to ANSWER_SUBMITTED event')
})

// 组件卸载时取消订阅
onUnmounted(() => {
  websocket.off(WS_EVENTS.ANSWER_SUBMITTED, handleAnswerSubmitted)
  console.log('[QuestionOverlay] Unsubscribed from ANSWER_SUBMITTED event')
})
</script>

<style scoped>
.question-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #ffffff;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.overlay-container {
  max-width: 1200px;
  width: 100%;
  background: #ffffff;
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.4s ease-out;
  max-height: 90vh;
  overflow-y: auto;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.question-header {
  text-align: center;
  margin-bottom: 32px;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 20px;
}

.question-title {
  font-size: clamp(24px, 5vw, 42px);
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 16px;
  line-height: 1.4;
  word-wrap: break-word;
}

.question-type {
  display: inline-block;
  padding: 6px 20px;
  background: #667eea;
  color: white;
  border-radius: 20px;
  font-size: clamp(14px, 2vw, 18px);
  font-weight: 600;
}

.options-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.option-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 8px 12px;
  transition: all 0.3s ease;
}

.option-item.has-result {
  background: white;
  border: 2px solid #e0e0e0;
}

.option-content {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-label {
  display: flex;
  align-items: center;
  justify-content: center;
  width: clamp(36px, 5vw, 42px);
  height: clamp(36px, 5vw, 42px);
  background: #667eea;
  color: white;
  border-radius: 50%;
  font-size: clamp(16px, 2.5vw, 20px);
  font-weight: 700;
  flex-shrink: 0;
}

.option-text {
  font-size: clamp(16px, 2.5vw, 22px);
  color: #2c3e50;
  font-weight: 500;
  word-wrap: break-word;
  flex: 1;
}

.option-result {
  margin-top: 8px;
}

.result-bar-container {
  height: clamp(20px, 3vw, 26px);
  background: #e9ecef;
  border-radius: 13px;
  overflow: hidden;
  margin-bottom: 6px;
}

.result-bar {
  height: 100%;
  background: #9ca3af;
  border-radius: 13px;
  transition: width 0.6s ease;
  position: relative;
}

.result-bar.is-correct {
  background: #22c55e;
}

.result-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 6px;
}

.result-count {
  font-size: clamp(14px, 2vw, 18px);
  color: #6c757d;
  font-weight: 500;
}

.result-percentage {
  font-size: clamp(16px, 2.5vw, 20px);
  color: #6c757d;
  font-weight: 700;
}

.essay-section {
  width: 100%;
}

.essay-wordcloud {
  padding: 40px 20px;
  text-align: center;
}

.wordcloud-container {
  position: relative;
  width: 100%;
  height: 500px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.word-tag {
  position: absolute;
  white-space: nowrap;
  font-weight: 600;
  cursor: default;
  transition: all 0.3s ease;
  animation: wordFadeIn 0.8s ease-out backwards;
  animation-delay: var(--animation-delay, 0s);
}

.word-tag:hover {
  transform: translate(-50%, -50%) scale(1.1) !important;
  z-index: 10;
}

@keyframes wordFadeIn {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.5);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

.essay-hint {
  text-align: center;
  padding: 40px 20px;
}

.essay-hint p {
  font-size: clamp(20px, 3vw, 28px);
  color: #6c757d;
  margin-bottom: 16px;
}

.essay-count {
  font-size: clamp(24px, 4vw, 32px);
  color: #667eea;
  font-weight: 700;
}

.overlay-footer {
  text-align: center;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 2px solid #e0e0e0;
}

.footer-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: clamp(16px, 2.5vw, 22px);
  color: #6c757d;
  font-weight: 500;
  flex-wrap: wrap;
}

.pulse-dot {
  width: 16px;
  height: 16px;
  background: #22c55e;
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.2);
  }
}

/* 过渡动画 */
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.3s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>

