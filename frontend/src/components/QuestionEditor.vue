<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content editor-modal" @click.stop>
      <div class="modal-header">
        <h3>{{ question?.id ? '编辑' : '创建' }}{{ question?.type === 'CHOICE' ? '选择题' : '问答题' }}</h3>
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
      
      <div v-if="formData.type === 'CHOICE'" class="options-section">
        <label class="form-label">
          <span class="label-icon">✓</span>
          选项
        </label>
        <div class="options-list">
          <div 
            v-for="(option, index) in formData.options"
            :key="index"
            class="option-item"
            :class="{ 'is-correct': option.isCorrect }"
          >
            <label class="checkbox-wrapper">
              <input 
                type="checkbox"
                v-model="option.isCorrect"
                title="标记为正确答案"
                class="correct-checkbox"
              />
              <span class="checkbox-label">正确答案</span>
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
import { ref, watch } from 'vue'
import api from '../api'

const props = defineProps({
  question: Object,
  classroomId: Number
})

const emit = defineEmits(['close', 'saved'])

const formData = ref({
  type: props.question?.type || 'CHOICE',
  content: props.question?.content || '',
  options: props.question?.options || []
})

watch(() => props.question, (newQuestion) => {
  if (newQuestion) {
    formData.value = {
      type: newQuestion.type,
      content: newQuestion.content,
      options: newQuestion.options ? [...newQuestion.options] : []
    }
  }
}, { immediate: true })

const addOption = () => {
  formData.value.options.push({
    content: '',
    isCorrect: false
  })
}

const removeOption = (index) => {
  formData.value.options.splice(index, 1)
}

const saveQuestion = async () => {
  if (!formData.value.content.trim()) {
    alert('请输入题目内容')
    return
  }
  
  if (formData.value.type === 'CHOICE' && formData.value.options.length === 0) {
    alert('请至少添加一个选项')
    return
  }
  
  try {
    const data = {
      classroomId: props.classroomId,
      type: formData.value.type,
      content: formData.value.content,
      options: formData.value.type === 'CHOICE' ? formData.value.options : [],
      isOpen: props.question?.isOpen ?? false,  // 保留原状态，新建时默认false
      isFinished: props.question?.isFinished ?? false
    }
    
    if (props.question?.id) {
      await api.question.update(props.question.id, data)
    } else {
      await api.question.create(data)
    }
    
    emit('saved')
  } catch (error) {
    console.error('Failed to save question:', error)
    alert('保存失败')
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

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}

.correct-checkbox {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #4caf50;
}

.checkbox-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
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
</style>

