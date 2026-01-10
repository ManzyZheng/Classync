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

        <!-- 测验子问题列表 -->
        <div v-if="question?.type === 'QUIZ' && subQuestions.length > 0" class="quiz-wrapper">
          <!-- 返回按钮（当选中某个子问题时显示） -->
          <div v-if="selectedSubQuestionIndex !== null" class="back-to-list-btn-container">
            <button class="back-to-list-btn" @click="selectedSubQuestionIndex = null">
              ← 返回问题列表
            </button>
          </div>
          
          <div :class="['quiz-subquestions-container', { 'single-view': selectedSubQuestionIndex !== null }]">
          <!-- 显示所有子问题（当没有选中特定子问题时） -->
          <template v-if="selectedSubQuestionIndex === null">
            <div 
              v-for="(subQuestion, subIndex) in subQuestions"
              :key="subIndex"
              class="quiz-subquestion-card"
            >
              <div class="subquestion-header">
                <span class="subquestion-number">问题 {{ subIndex + 1 }}</span>
                <span class="subquestion-type-badge">{{ getQuestionTypeText(subQuestion.type) }}</span>
              </div>
              <div class="subquestion-content-text">{{ subQuestion.content }}</div>
              
              <!-- 选择题子问题的选项和统计 -->
              <div v-if="(subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE' || subQuestion.type === 'CHOICE') && subQuestion.options && subQuestion.options.length > 0" class="subquestion-options-container">
                <div 
                  v-for="(option, optIndex) in subQuestion.options"
                  :key="optIndex"
                  class="option-item"
                  :class="{ 'has-result': showResults }"
                >
                  <div class="option-content">
                    <span class="option-label">{{ getOptionLabel(optIndex) }}</span>
                    <span class="option-text">{{ option.content }}</span>
                    <span v-if="showAnswer && option.isCorrect" class="correct-mark">✓</span>
                  </div>
                  
                  <!-- 结果条形图 -->
                  <div v-if="showResults && subQuestionStatistics[subIndex]" class="option-result">
                    <div class="result-bar-container">
                      <div 
                        class="result-bar" 
                        :style="{ width: getSubQuestionPercentage(subIndex, option.content) + '%' }"
                        :class="{ 'is-correct': showAnswer && option.isCorrect }"
                      >
                      </div>
                    </div>
                    <div class="result-stats">
                      <span class="result-count">{{ getSubQuestionCount(subIndex, option.content) }} 人</span>
                      <span class="result-percentage">{{ getSubQuestionPercentage(subIndex, option.content) }}%</span>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 问答题子问题 -->
              <div v-else-if="subQuestion.type === 'ESSAY'" class="subquestion-essay-section">
                <div v-if="showAnswer && subQuestionWordFrequency[subIndex] && subQuestionWordFrequency[subIndex].length > 0" class="essay-wordcloud">
                  <WordCloudIframe
                    :keywords="getSubQuestionWordCloudKeywords(subIndex)"
                    bg-color="white"
                    class="wordcloud-iframe-wrapper"
                  />
                  <p class="essay-count">基于 {{ getSubQuestionEssayCount(subIndex) }} 份答案</p>
                </div>
                <div v-else class="essay-hint">
                  <p>简答题正在收集中...</p>
                  <p v-if="showResults" class="essay-count">已收到 {{ getSubQuestionEssayCount(subIndex) }} 份答案</p>
                </div>
              </div>
              
              <!-- 查看详情按钮 -->
              <div v-if="showResults" class="subquestion-actions">
                <button class="view-detail-btn" @click="selectedSubQuestionIndex = subIndex">
                  单独显示
                </button>
              </div>
            </div>
          </template>
          
          <!-- 显示单个子问题详情（当选中某个子问题时） -->
          <div v-else-if="selectedSubQuestionIndex !== null && subQuestions[selectedSubQuestionIndex]" class="quiz-subquestion-card single-subquestion-view">
            <div class="subquestion-header">
              <span class="subquestion-number">问题 {{ selectedSubQuestionIndex + 1 }}</span>
              <span class="subquestion-type-badge">{{ getQuestionTypeText(subQuestions[selectedSubQuestionIndex].type) }}</span>
            </div>
            <div class="subquestion-content-text">{{ subQuestions[selectedSubQuestionIndex].content }}</div>
            
            <!-- 选择题子问题的选项和统计 -->
            <div v-if="(subQuestions[selectedSubQuestionIndex].type === 'SINGLE_CHOICE' || subQuestions[selectedSubQuestionIndex].type === 'MULTIPLE_CHOICE' || subQuestions[selectedSubQuestionIndex].type === 'CHOICE') && subQuestions[selectedSubQuestionIndex].options && subQuestions[selectedSubQuestionIndex].options.length > 0" class="subquestion-options-container">
              <div 
                v-for="(option, optIndex) in subQuestions[selectedSubQuestionIndex].options"
                :key="optIndex"
                class="option-item"
                :class="{ 'has-result': showResults }"
              >
                <div class="option-content">
                  <span class="option-label">{{ getOptionLabel(optIndex) }}</span>
                  <span class="option-text">{{ option.content }}</span>
                  <span v-if="showAnswer && option.isCorrect" class="correct-mark">✓</span>
                </div>
                
                <!-- 结果条形图 -->
                <div v-if="showResults && subQuestionStatistics[selectedSubQuestionIndex]" class="option-result">
                  <div class="result-bar-container">
                    <div 
                      class="result-bar" 
                      :style="{ width: getSubQuestionPercentage(selectedSubQuestionIndex, option.content) + '%' }"
                      :class="{ 'is-correct': showAnswer && option.isCorrect }"
                    >
                    </div>
                  </div>
                  <div class="result-stats">
                    <span class="result-count">{{ getSubQuestionCount(selectedSubQuestionIndex, option.content) }} 人</span>
                    <span class="result-percentage">{{ getSubQuestionPercentage(selectedSubQuestionIndex, option.content) }}%</span>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 问答题子问题 -->
            <div v-else-if="subQuestions[selectedSubQuestionIndex].type === 'ESSAY'" class="subquestion-essay-section">
              <div v-if="showAnswer && subQuestionWordFrequency[selectedSubQuestionIndex] && subQuestionWordFrequency[selectedSubQuestionIndex].length > 0" class="essay-wordcloud">
                <WordCloudIframe
                  :keywords="getSubQuestionWordCloudKeywords(selectedSubQuestionIndex)"
                  bg-color="white"
                  class="wordcloud-iframe-wrapper"
                />
                <p class="essay-count">基于 {{ getSubQuestionEssayCount(selectedSubQuestionIndex) }} 份答案</p>
              </div>
              <div v-else class="essay-hint">
                <p>简答题正在收集中...</p>
                <p v-if="showResults" class="essay-count">已收到 {{ getSubQuestionEssayCount(selectedSubQuestionIndex) }} 份答案</p>
              </div>
            </div>
          </div>
          </div>
        </div>

        <!-- 选项列表（普通问题） -->
        <div v-else-if="question?.type !== 'ESSAY' && question?.type !== 'QUIZ'" class="options-container">
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
        <div v-else-if="question?.type === 'ESSAY'" class="essay-section">
          <!-- 显示答案模式：展示词云 -->
          <div v-if="showAnswer && wordFrequency.length > 0" class="essay-wordcloud">
            <WordCloudIframe
              :keywords="wordCloudKeywords"
              bg-color="white"
              class="wordcloud-iframe-wrapper"
            />
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
import WordCloudIframe from './WordCloudIframe.vue'

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
  },
  subQuestionIndex: {
    type: Number,
    default: null  // 子问题索引，如果提供则只显示该子问题
  }
})

const question = ref(null)
const options = ref([])
const statistics = ref(null)
const essayAnswers = ref([])
const wordFrequency = ref([])
const wordPositions = ref([])
// 测验相关
const subQuestions = ref([])
const subQuestionStatistics = ref({}) // { subIndex: statistics[] }
const subQuestionEssayAnswers = ref({}) // { subIndex: answers[] }
const subQuestionWordFrequency = ref({}) // { subIndex: wordFrequency[] }
const subQuestionWordPositions = ref({}) // { subIndex: wordPositions[] }
const selectedSubQuestionIndex = ref(null) // 当前选中的子问题索引，null 表示显示所有子问题

const showResults = computed(() => props.mode === 'SHOW_RESULTS' || props.mode === 'SHOW_ANSWER')
const showAnswer = computed(() => props.mode === 'SHOW_ANSWER')

// 获取问题类型文本
const getQuestionTypeText = (type) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'ESSAY': '简答题',
    'QUIZ': '测验',
    'CHOICE': '选择题' // 兼容旧数据
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

// 将wordFrequency转换为WordCloudIframe需要的keywords格式
const wordCloudKeywords = computed(() => {
  if (!wordFrequency.value || wordFrequency.value.length === 0) {
    return {}
  }
  const keywords = {}
  wordFrequency.value.forEach(word => {
    keywords[word.word] = word.count
  })
  return keywords
})

// 获取子问题的词云keywords
const getSubQuestionWordCloudKeywords = (subIndex) => {
  const freq = subQuestionWordFrequency.value[subIndex] || []
  if (freq.length === 0) {
    return {}
  }
  const keywords = {}
  freq.forEach(word => {
    keywords[word.word] = word.count
  })
  return keywords
}

// 获取子问题的统计计数
const getSubQuestionCount = (subIndex, optionContent) => {
  const stats = subQuestionStatistics.value[subIndex]
  if (!stats || !Array.isArray(stats)) return 0
  const stat = stats.find(s => s.optionContent === optionContent)
  return stat ? stat.count : 0
}

// 获取子问题的统计百分比
const getSubQuestionPercentage = (subIndex, optionContent) => {
  const stats = subQuestionStatistics.value[subIndex]
  if (!stats || !Array.isArray(stats)) return 0
  const stat = stats.find(s => s.optionContent === optionContent)
  return stat ? Math.round(stat.percentage) : 0
}

// 获取子问题的问答题答案数量
const getSubQuestionEssayCount = (subIndex) => {
  return subQuestionEssayAnswers.value[subIndex]?.length || 0
}

// 加载测验子问题的统计数据
const loadQuizSubQuestionStatistics = async (quizId, subIndex, subQuestion) => {
  try {
    // 获取整张测验题的所有回答
    const allAnswersResponse = await fetch(`/api/answers/question/${quizId}`)
    const allAnswers = await allAnswersResponse.json()
    
    if (subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE' || subQuestion.type === 'CHOICE') {
      const optionCountMap = {}
      let totalSelections = 0
      
      allAnswers.forEach(ans => {
        try {
          const payload = JSON.parse(ans.content)
          if (!payload.answers || !Array.isArray(payload.answers)) return
          payload.answers
            .filter(a => a.subQuestionIndex === subIndex)
            .forEach(a => {
              if (!a.content) return
              const parts = subQuestion.type === 'MULTIPLE_CHOICE'
                ? a.content.split(',').map(p => p.trim()).filter(Boolean)
                : [a.content.trim()]
              parts.forEach(p => {
                optionCountMap[p] = (optionCountMap[p] || 0) + 1
                totalSelections++
              })
            })
        } catch (e) {
          console.warn('[QuestionOverlay] Failed to parse quiz answer JSON:', e)
        }
      })
      
      const opts = subQuestion.options || []
      subQuestionStatistics.value[subIndex] = opts.map(opt => {
        const count = optionCountMap[opt.content] || 0
        return {
          optionContent: opt.content,
          count,
          percentage: totalSelections > 0 ? (count * 100.0 / totalSelections) : 0,
          isCorrect: !!opt.isCorrect
        }
      })
    } else if (subQuestion.type === 'ESSAY') {
      const subAnswers = []
      allAnswers.forEach(ans => {
        try {
          const payload = JSON.parse(ans.content)
          if (!payload.answers || !Array.isArray(payload.answers)) return
          payload.answers
            .filter(a => a.subQuestionIndex === subIndex && a.content)
            .forEach(a => {
              subAnswers.push({
                content: a.content,
                createdAt: ans.createdAt
              })
            })
        } catch (e) {
          console.warn('[QuestionOverlay] Failed to parse quiz answer JSON for essay:', e)
        }
      })
      
      subQuestionEssayAnswers.value[subIndex] = subAnswers
      
      // 如果是显示答案模式，生成词云
      if (showAnswer.value && subAnswers.length > 0) {
        try {
          const freq = await generateWordCloud(subAnswers)
          subQuestionWordFrequency.value[subIndex] = freq
          if (freq.length > 0) {
            const pos = await calculateWordPositions(freq, 1000, 400)
            subQuestionWordPositions.value[subIndex] = pos
          }
        } catch (error) {
          console.error('[QuestionOverlay] Failed to generate word cloud for sub-question:', error)
        }
      }
    }
  } catch (error) {
    console.error('[QuestionOverlay] Failed to load quiz sub-question statistics:', error)
  }
}

// 监听 subQuestionIndex 变化，自动设置选中状态
watch(() => props.subQuestionIndex, (newIndex) => {
  if (question.value?.type === 'QUIZ') {
    if (newIndex !== null && newIndex !== undefined) {
      selectedSubQuestionIndex.value = newIndex
      console.log('[QuestionOverlay] Sub-question index changed to:', newIndex)
    } else {
      selectedSubQuestionIndex.value = null
      console.log('[QuestionOverlay] Sub-question index cleared, showing all sub-questions')
    }
  }
}, { immediate: true })

// 监听 questionId 变化，加载问题数据
watch(() => props.questionId, async (newId) => {
  if (!newId) {
    question.value = null
    options.value = []
    statistics.value = null
    essayAnswers.value = []
    wordFrequency.value = []
    wordPositions.value = []
    subQuestions.value = []
    subQuestionStatistics.value = {}
    subQuestionEssayAnswers.value = {}
    subQuestionWordFrequency.value = {}
    subQuestionWordPositions.value = {}
    selectedSubQuestionIndex.value = null
    subQuestions.value = []
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
    
    // 解析测验子问题
    if (question.value.type === 'QUIZ') {
      let questionsData = data.questions
      if (typeof questionsData === 'string') {
        try {
          questionsData = JSON.parse(questionsData)
        } catch (e) {
          console.error('[QuestionOverlay] Failed to parse questions JSON:', e)
          questionsData = []
        }
      }
      subQuestions.value = Array.isArray(questionsData) ? questionsData : []
      console.log('[QuestionOverlay] Quiz sub-questions loaded:', subQuestions.value.length)
      
      // 根据 props.subQuestionIndex 设置选中状态
      if (props.subQuestionIndex !== null && props.subQuestionIndex !== undefined) {
        selectedSubQuestionIndex.value = props.subQuestionIndex
        console.log('[QuestionOverlay] Auto-selected sub-question index:', props.subQuestionIndex)
      } else {
        // 如果没有提供子问题索引，确保显示所有子问题
        selectedSubQuestionIndex.value = null
        console.log('[QuestionOverlay] No sub-question index provided, showing all sub-questions')
      }
      
      // 如果需要显示结果，加载每个子问题的统计数据
      if (showResults.value) {
        for (let i = 0; i < subQuestions.value.length; i++) {
          await loadQuizSubQuestionStatistics(newId, i, subQuestions.value[i])
        }
      }
    }
    
    console.log('[QuestionOverlay] Question content:', question.value?.content)
    console.log('[QuestionOverlay] Options:', options.value)
    
    // 如果需要显示结果，加载统计数据（普通问题）
    if (showResults.value && question.value.type !== 'ESSAY' && question.value.type !== 'QUIZ') {
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
      if (question.value?.type === 'QUIZ') {
        // 测验：加载每个子问题的统计数据
        for (let i = 0; i < subQuestions.value.length; i++) {
          await loadQuizSubQuestionStatistics(props.questionId, i, subQuestions.value[i])
        }
      } else if (question.value?.type !== 'ESSAY') {
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
    subQuestionStatistics.value = {}
    subQuestionEssayAnswers.value = {}
    subQuestionWordFrequency.value = {}
    subQuestionWordPositions.value = {}
    // 只有在没有提供子问题索引时才重置，否则保持选中状态
    if (props.subQuestionIndex === null || props.subQuestionIndex === undefined) {
      selectedSubQuestionIndex.value = null
    }
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
  
  // 如果是测验且正在显示结果，重新加载所有子问题的统计数据
  if (question.value?.type === 'QUIZ' && showResults.value) {
    try {
      for (let i = 0; i < subQuestions.value.length; i++) {
        await loadQuizSubQuestionStatistics(props.questionId, i, subQuestions.value[i])
      }
      console.log('[QuestionOverlay] Real-time: Quiz sub-question statistics updated')
    } catch (error) {
      console.error('[QuestionOverlay] Failed to reload quiz statistics:', error)
    }
  }
  // 如果是简答题且正在显示结果或答案，重新加载答案
  else if (question.value?.type === 'ESSAY' && showResults.value) {
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
  else if (question.value?.type !== 'ESSAY' && question.value?.type !== 'QUIZ' && showResults.value) {
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
  align-items: flex-start;
  justify-content: center;
  padding: 15px;
  overflow-y: auto;
}

.overlay-container {
  max-width: 98vw;
  width: 100%;
  background: #ffffff;
  border-radius: 24px;
  padding: 25px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.4s ease-out;
  margin-top: 10px;
  margin-bottom: 10px;
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
  margin-bottom: 24px;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 16px;
}

.question-title {
  font-size: clamp(20px, 4vw, 36px);
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 12px;
  line-height: 1.3;
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
  padding: 10px 14px;
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
  margin-bottom: 8px;
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

.correct-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #22c55e;
  color: white;
  border-radius: 50%;
  font-size: 18px;
  font-weight: 700;
  margin-left: 12px;
  flex-shrink: 0;
}

.quiz-subquestions-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 18px;
  margin-bottom: 20px;
}

/* 响应式：小屏幕时使用单列 */
@media (max-width: 1000px) {
  .quiz-subquestions-container {
    grid-template-columns: 1fr;
  }
}

/* 当选中单个子问题时，使用单列布局并居中 */
.quiz-subquestions-container.single-view {
  grid-template-columns: 1fr;
  max-width: 90%;
  margin: 0 auto;
}

.quiz-subquestion-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  border: 2px solid #e0e0e0;
  transition: all 0.3s ease;
  min-width: 0; /* 防止内容溢出 */
}

.quiz-subquestion-card:hover {
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.subquestion-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.subquestion-number {
  font-size: clamp(18px, 3vw, 24px);
  font-weight: 700;
  color: #2c3e50;
}

.subquestion-type-badge {
  display: inline-block;
  padding: 4px 12px;
  background: #667eea;
  color: white;
  border-radius: 12px;
  font-size: clamp(12px, 1.8vw, 16px);
  font-weight: 600;
}

.subquestion-content-text {
  font-size: clamp(16px, 2.5vw, 22px);
  color: #2c3e50;
  font-weight: 500;
  line-height: 1.5;
  margin-bottom: 16px;
  word-wrap: break-word;
}

.subquestion-options-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.subquestion-essay-section {
  margin-top: 12px;
}

.subquestion-actions {
  margin-top: 16px;
  text-align: center;
}

.view-detail-btn {
  padding: 12px 24px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: clamp(14px, 2vw, 18px);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-detail-btn:hover {
  background: #5568d3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.view-detail-btn:active {
  transform: translateY(0);
}

.quiz-wrapper {
  width: 100%;
}

.back-to-list-btn-container {
  margin-bottom: 16px;
  text-align: left;
}

.back-to-list-btn {
  padding: 10px 20px;
  background: #6c757d;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: clamp(14px, 2vw, 18px);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.back-to-list-btn:hover {
  background: #5a6268;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(108, 117, 125, 0.3);
}

.back-to-list-btn:active {
  transform: translateY(0);
}

.single-subquestion-view {
  max-width: 90%;
  margin: 0 auto;
  width: 100%;
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

.wordcloud-iframe-wrapper {
  width: 100%;
  height: 500px;
  margin: 0 auto 24px;
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

