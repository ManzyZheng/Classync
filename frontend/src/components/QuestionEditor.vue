<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content editor-modal" @click.stop>
      <div class="modal-header">
        <h3>{{ question?.id ? '编辑' : '创建' }}{{ getQuestionTypeName(question?.type || formData.type) }}</h3>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>
      
      <div class="form-group">
        <label class="form-label">
          题目内容
        </label>
        <textarea 
          v-model="formData.content" 
          placeholder="请输入题目内容..."
          class="form-textarea"
        ></textarea>
      </div>
      
      <!-- 单选题和多选题 -->
      <div v-if="formData.type === 'SINGLE_CHOICE' || formData.type === 'MULTIPLE_CHOICE'" class="options-section">
        <label class="form-label">
          <span class="label-icon">✓</span>
          选项
          <span class="type-hint">
            {{ formData.type === 'SINGLE_CHOICE' ? '（只能选择一个正确答案）' : '（可以选择多个正确答案）' }}
          </span>
        </label>
        <div class="options-list">
          <div 
            v-for="(option, index) in formData.options"
            :key="index"
            class="option-item"
            :class="{ 'is-correct': option.isCorrect }"
          >
            <label class="answer-wrapper">
              <!-- 单选题使用radio，多选题使用checkbox -->
              <input 
                :type="formData.type === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'"
                :name="formData.type === 'SINGLE_CHOICE' ? 'correct-answer' : undefined"
                :checked="option.isCorrect"
                @change="handleAnswerChange(index, $event)"
                :title="formData.type === 'SINGLE_CHOICE' ? '标记为正确答案（单选）' : '标记为正确答案（多选）'"
                class="correct-input"
              />
              <span class="answer-label">正确答案</span>
            </label>
            <input 
              type="text"
              v-model="option.content"
              placeholder="请输入选项内容..."
              class="option-input"
            />
            <button class="delete-option-btn" @click="removeOption(index)" title="删除选项">
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>
        <button class="add-option-btn" @click="addOption">
          <span class="add-icon">+</span>
          添加选项
        </button>
      </div>
      
      <!-- 测验类型：可以添加多个子问题 -->
      <div v-if="formData.type === 'QUIZ'" class="quiz-section">
        <label class="form-label">
          测验问题
        </label>
        <div class="quiz-questions-list">
          <div 
            v-for="(subQuestion, index) in formData.questions"
            :key="`quiz-question-${index}-${subQuestion.type}-${subQuestion.content?.substring(0, 10) || 'empty'}`"
            class="quiz-question-item"
            :class="{ 'editing': editingQuizQuestionIndex === index }"
          >
            <div class="quiz-question-header">
              <span class="quiz-question-number">问题 {{ index + 1 }}</span>
              <span class="quiz-question-type">{{ getQuestionTypeName(subQuestion.type) }}</span>
              <div class="quiz-question-actions">
                <button class="edit-quiz-question-btn" @click="editQuizQuestion(index)">编辑</button>
                <button class="delete-quiz-question-btn" @click="removeQuizQuestion(index)">删除</button>
              </div>
            </div>
            
            <!-- 编辑模式 -->
            <div v-if="editingQuizQuestionIndex === index" class="quiz-question-editor">
              <div class="form-group">
                <label class="form-label">题目内容</label>
                <textarea 
                  v-model="subQuestion.content"
                  placeholder="请输入题目内容..."
                  class="form-textarea"
                  rows="3"
                ></textarea>
              </div>
              
              <!-- 单选题和多选题的选项 -->
              <div v-if="subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE'" class="options-section">
                <label class="form-label">
                  <span class="label-icon">✓</span>
                  选项
                  <span class="type-hint">
                    {{ subQuestion.type === 'SINGLE_CHOICE' ? '（只能选择一个正确答案）' : '（可以选择多个正确答案）' }}
                  </span>
                </label>
                <div class="options-list">
                  <div 
                    v-for="(option, optIndex) in subQuestion.options"
                    :key="optIndex"
                    class="option-item"
                    :class="{ 'is-correct': option.isCorrect }"
                  >
                    <label class="answer-wrapper">
                      <input 
                        :type="subQuestion.type === 'SINGLE_CHOICE' ? 'radio' : 'checkbox'"
                        :name="`quiz-${index}-correct-answer`"
                        :checked="option.isCorrect"
                        @change="handleQuizAnswerChange(index, optIndex, $event)"
                        class="correct-input"
                      />
                      <span class="answer-label">正确答案</span>
                    </label>
                    <input 
                      type="text"
                      v-model="option.content"
                      placeholder="请输入选项内容..."
                      class="option-input"
                    />
                    <button class="delete-option-btn" @click="removeQuizOption(index, optIndex)" title="删除选项">
                      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                        <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      </svg>
                    </button>
                  </div>
                </div>
                <button class="add-option-btn" @click="addQuizOption(index)">
                  <span class="add-icon">+</span>
                  添加选项
                </button>
              </div>
              
              <div class="quiz-question-editor-actions">
                <button class="cancel-btn-small" @click="cancelEditQuizQuestion">取消</button>
                <button class="save-btn-small" @click="saveQuizQuestion(index)">保存</button>
              </div>
            </div>
            
            <!-- 显示模式 -->
            <div v-else class="quiz-question-content">
              <div class="quiz-question-text">{{ subQuestion.content || '（未设置）' }}</div>
              <div v-if="subQuestion.options && subQuestion.options.length > 0" class="quiz-question-options-preview">
                <div 
                  v-for="(option, optIndex) in subQuestion.options"
                  :key="optIndex"
                  class="quiz-option-preview"
                  :class="{ 'is-correct': option.isCorrect }"
                >
                  {{ option.content }}
                  <span v-if="option.isCorrect" class="correct-badge">✓</span>
                </div>
              </div>
            </div>
          </div>
          <div class="add-quiz-question-wrapper">
            <select 
              :value="selectedQuizQuestionType"
              class="quiz-question-type-select"
              @change="handleQuizQuestionTypeSelect"
            >
              <option value="">选择问题类型</option>
              <option value="SINGLE_CHOICE">单选题</option>
              <option value="MULTIPLE_CHOICE">多选题</option>
              <option value="ESSAY">开放问题</option>
            </select>
          </div>
        </div>
      </div>
      
      <div class="modal-actions">
        <button class="cancel-btn" @click="$emit('close')">取消</button>
        <button class="submit-btn" @click="saveQuestion">
          <span v-if="question?.id">保存</span>
          <span v-else>创建</span>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import api from '../api'

const props = defineProps({
  question: Object,
  classroomId: Number
})

const emit = defineEmits(['close', 'saved'])

// 测验相关状态（需要在 watch 之前定义，避免未初始化错误）
const editingQuizQuestionIndex = ref(-1)
const selectedQuizQuestionType = ref('')

const formData = ref({
  type: props.question?.type || 'SINGLE_CHOICE',
  content: props.question?.content || '',
  options: props.question?.options || [],
  questions: props.question?.questions || [] // 测验的子问题
})

watch(() => props.question, (newQuestion) => {
  if (newQuestion) {
    // 处理questions字段：如果是JSON字符串，需要解析；如果是数组，直接使用
    let questionsData = []
    if (newQuestion.questions) {
      if (typeof newQuestion.questions === 'string') {
        try {
          questionsData = JSON.parse(newQuestion.questions)
        } catch (e) {
          console.error('Failed to parse questions JSON:', e)
          questionsData = []
        }
      } else if (Array.isArray(newQuestion.questions)) {
        questionsData = [...newQuestion.questions]
      }
    }
    
    formData.value = {
      type: newQuestion.type,
      content: newQuestion.content,
      options: newQuestion.options ? [...newQuestion.options] : [],
      questions: questionsData
    }
    // 重置编辑状态
    editingQuizQuestionIndex.value = -1
    selectedQuizQuestionType.value = ''
    console.log('QuestionEditor: Loaded question data:', {
      type: newQuestion.type,
      questionsCount: formData.value.questions.length,
      questions: formData.value.questions,
      rawQuestions: newQuestion.questions
    })
  } else {
    // 如果没有问题数据，重置表单
    formData.value = {
      type: 'SINGLE_CHOICE',
      content: '',
      options: [],
      questions: []
    }
    editingQuizQuestionIndex.value = -1
    selectedQuizQuestionType.value = ''
  }
}, { immediate: true })

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

const addOption = () => {
  formData.value.options.push({
    content: '',
    isCorrect: false
  })
}

const removeOption = (index) => {
  formData.value.options.splice(index, 1)
}

// 处理单选题和多选题的答案选择
const handleAnswerChange = (index, event) => {
  if (formData.value.type === 'SINGLE_CHOICE') {
    // 单选题：只能选一个，先清除所有，再设置当前
    formData.value.options.forEach((opt, i) => {
      opt.isCorrect = i === index
    })
  } else {
    // 多选题：可以选多个
    formData.value.options[index].isCorrect = event.target.checked
  }
}

const handleQuizQuestionTypeSelect = async (event) => {
  const type = event.target.value
  if (type) {
    await addQuizQuestion(type)
    selectedQuizQuestionType.value = '' // 重置选择
  }
}

const addQuizQuestion = async (type) => {
  // 确保 questions 数组存在
  if (!formData.value.questions) {
    formData.value.questions = []
  }
  
  const newQuestion = {
    type,
    content: '',
    options: type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE' ? [] : undefined
  }
  
  // 添加新问题（使用响应式方式）
  formData.value.questions = [...formData.value.questions, newQuestion]
  
  // 等待DOM更新后再进入编辑模式
  await nextTick()
  
  // 设置编辑索引为新添加的问题
  const newIndex = formData.value.questions.length - 1
  editingQuizQuestionIndex.value = newIndex
  
  console.log('Added quiz question, editing index:', editingQuizQuestionIndex.value, 'Total questions:', formData.value.questions.length)
}

const removeQuizQuestion = (index) => {
  if (confirm('确定要删除这个问题吗？')) {
    // 确保 questions 数组存在
    if (!formData.value.questions || !Array.isArray(formData.value.questions)) {
      console.error('Questions array is not valid')
      return
    }
    
    // 使用响应式方式删除
    formData.value.questions = formData.value.questions.filter((_, i) => i !== index)
    
    // 更新编辑索引
    if (editingQuizQuestionIndex.value === index) {
      editingQuizQuestionIndex.value = -1
    } else if (editingQuizQuestionIndex.value > index) {
      editingQuizQuestionIndex.value--
    }
    
    console.log('Removed quiz question at index:', index, 'Remaining questions:', formData.value.questions.length)
  }
}

const editQuizQuestion = (index) => {
  editingQuizQuestionIndex.value = index
}

const cancelEditQuizQuestion = () => {
  editingQuizQuestionIndex.value = -1
}

const saveQuizQuestion = (index) => {
  const subQuestion = formData.value.questions[index]
  if (!subQuestion.content.trim()) {
    alert('请输入题目内容')
    return
  }
  if ((subQuestion.type === 'SINGLE_CHOICE' || subQuestion.type === 'MULTIPLE_CHOICE') && (!subQuestion.options || subQuestion.options.length === 0)) {
    alert('请至少添加一个选项')
    return
  }
  editingQuizQuestionIndex.value = -1
}

const addQuizOption = (questionIndex) => {
  if (!formData.value.questions[questionIndex].options) {
    formData.value.questions[questionIndex].options = []
  }
  formData.value.questions[questionIndex].options.push({
    content: '',
    isCorrect: false
  })
}

const removeQuizOption = (questionIndex, optionIndex) => {
  formData.value.questions[questionIndex].options.splice(optionIndex, 1)
}

const handleQuizAnswerChange = (questionIndex, optionIndex, event) => {
  const subQuestion = formData.value.questions[questionIndex]
  if (subQuestion.type === 'SINGLE_CHOICE') {
    // 单选题：只能选一个
    subQuestion.options.forEach((opt, i) => {
      opt.isCorrect = i === optionIndex
    })
  } else {
    // 多选题：可以选多个
    subQuestion.options[optionIndex].isCorrect = event.target.checked
  }
}

const saveQuestion = async () => {
  if (!formData.value.content.trim()) {
    alert('请输入题目内容')
    return
  }
  
  // 验证单选题和多选题
  if ((formData.value.type === 'SINGLE_CHOICE' || formData.value.type === 'MULTIPLE_CHOICE') && formData.value.options.length === 0) {
    alert('请至少添加一个选项')
    return
  }
  
  // 验证测验
  if (formData.value.type === 'QUIZ' && formData.value.questions.length === 0) {
    alert('请至少添加一个问题到测验中')
    return
  }
  
  try {
    // 准备保存的数据
    const data = {
      classroomId: props.classroomId,
      type: formData.value.type,
      content: formData.value.content,
      options: (formData.value.type === 'SINGLE_CHOICE' || formData.value.type === 'MULTIPLE_CHOICE') ? formData.value.options : [],
      questions: formData.value.type === 'QUIZ' ? formData.value.questions : [],
      isOpen: props.question?.isOpen ?? false,  // 保留原状态，新建时默认false
      isFinished: props.question?.isFinished ?? false
    }
    
    console.log('QuestionEditor: Saving question data:', {
      type: data.type,
      questionsCount: data.questions ? data.questions.length : 0,
      questions: data.questions,
      rawData: JSON.stringify(data, null, 2)
    })
    
    if (props.question?.id) {
      await api.question.update(props.question.id, data)
    } else {
      await api.question.create(data)
    }
    
    emit('saved')
  } catch (error) {
    console.error('Failed to save question:', error)
    alert('保存失败: ' + (error.message || '未知错误'))
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.editor-modal {
  max-width: 650px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease-out;
  position: relative;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.3px;
}

.close-btn {
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

.close-btn:hover {
  background: #e8e8e8;
  color: #333;
  transform: rotate(90deg);
}

.form-group {
  margin-bottom: 24px;
  padding: 0 28px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #333;
  font-weight: 600;
  font-size: 14px;
}

.label-icon {
  font-size: 16px;
}

.form-textarea {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  resize: vertical;
  min-height: 120px;
  font-size: 15px;
  font-family: inherit;
  color: #333;
  transition: all 0.2s;
  background: #fafafa;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-textarea::placeholder {
  color: #999;
}

.options-section {
  padding: 0 28px;
  margin-bottom: 24px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: #d0d0d0;
  background: #f5f5f5;
}

.option-item.is-correct {
  border-color: #4caf50;
  background: #f1f8f4;
}

.answer-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.correct-input {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #4caf50;
}

.answer-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.type-hint {
  font-size: 12px;
  color: #999;
  font-weight: normal;
  margin-left: 8px;
}

.option-input {
  flex: 1;
  padding: 10px 14px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  background: white;
  color: #333;
  transition: all 0.2s;
}

.option-input:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
}

.option-input::placeholder {
  color: #bbb;
}

.delete-option-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: #fee;
  color: #f44336;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.delete-option-btn:hover {
  background: #f44336;
  color: white;
  transform: scale(1.1);
}

.add-option-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #f8f9ff 0%, #f0f4ff 100%);
  border: 2px dashed #667eea;
  border-radius: 10px;
  color: #667eea;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
}

.add-option-btn:hover {
  background: linear-gradient(135deg, #f0f4ff 0%, #e8f0ff 100%);
  border-color: #5568d3;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.add-icon {
  font-size: 20px;
  font-weight: 300;
  line-height: 1;
}

.modal-actions {
  display: flex;
  gap: 12px;
  padding: 20px 28px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  border-radius: 0 0 16px 16px;
}

.cancel-btn, .submit-btn {
  flex: 1;
  padding: 14px 24px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.cancel-btn {
  background: white;
  color: #666;
  border: 2px solid #e8e8e8;
}

.cancel-btn:hover {
  background: #f5f5f5;
  border-color: #d0d0d0;
  color: #333;
}

.submit-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.submit-btn:active {
  transform: translateY(0);
}

/* 滚动条样式 */
.editor-modal::-webkit-scrollbar {
  width: 8px;
}

.editor-modal::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 4px;
}

.editor-modal::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

.editor-modal::-webkit-scrollbar-thumb:hover {
  background: #aaa;
}

/* 测验相关样式 */
.quiz-section {
  padding: 0 28px;
  margin-bottom: 24px;
}

.quiz-questions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quiz-question-item {
  padding: 16px;
  background: #fafafa;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  transition: all 0.2s;
  margin-bottom: 12px;
}

.quiz-question-item:hover {
  border-color: #d0d0d0;
  background: #f5f5f5;
}

.quiz-question-item.editing {
  border-color: #667eea;
  background: #f8f9ff;
}

.quiz-question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.quiz-question-number {
  font-weight: 600;
  color: #667eea;
  font-size: 14px;
}

.quiz-question-type {
  padding: 4px 10px;
  background: #e8f0ff;
  color: #667eea;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.quiz-question-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.edit-quiz-question-btn {
  padding: 6px 12px;
  background: #e8f0ff;
  color: #667eea;
  border: 1px solid #d0d9ff;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-quiz-question-btn:hover {
  background: #d0d9ff;
  border-color: #b8c5ff;
}

.delete-quiz-question-btn {
  padding: 6px 12px;
  background: #fee;
  color: #e53e3e;
  border: 1px solid #fcc;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.delete-quiz-question-btn:hover {
  background: #fdd;
  border-color: #faa;
}

.quiz-question-content {
  color: #666;
  font-size: 14px;
  line-height: 1.5;
}

.quiz-question-text {
  margin-bottom: 8px;
}

.quiz-question-options-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}

.quiz-option-preview {
  padding: 8px 12px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.quiz-option-preview.is-correct {
  border-color: #4caf50;
  background: #f1f8f4;
  color: #2e7d32;
}

.correct-badge {
  color: #4caf50;
  font-weight: bold;
  font-size: 14px;
}

.quiz-question-editor {
  padding-top: 12px;
  border-top: 1px solid #e8e8e8;
}

.quiz-question-editor .form-group {
  padding: 0;
  margin-bottom: 16px;
}

.quiz-question-editor .options-section {
  padding: 0;
  margin-bottom: 16px;
}

.quiz-question-editor-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 16px;
}

.cancel-btn-small,
.save-btn-small {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn-small {
  background: #f5f5f5;
  color: #666;
  border: 1px solid #e8e8e8;
}

.cancel-btn-small:hover {
  background: #e8e8e8;
}

.save-btn-small {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.save-btn-small:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.add-quiz-question-wrapper {
  margin-top: 12px;
}

.quiz-question-type-select {
  width: 100%;
  padding: 12px 16px;
  background: white;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  font-size: 15px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23667eea' d='M6 9L1 4h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 40px;
}

.quiz-question-type-select:hover {
  border-color: #d0d0d0;
}

.quiz-question-type-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.quiz-question-type-select option {
  padding: 10px;
  background: white;
  color: #333;
}
</style>

