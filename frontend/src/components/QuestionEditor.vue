<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content editor-modal" @click.stop>
      <h3>{{ question?.id ? '编辑' : '创建' }}{{ question?.type === 'CHOICE' ? '选择题' : '问答题' }}</h3>
      
      <div class="form-group">
        <label>题目内容</label>
        <textarea 
          v-model="formData.content" 
          placeholder="请输入题目内容"
        ></textarea>
      </div>
      
      <div v-if="formData.type === 'CHOICE'" class="options-section">
        <label>选项</label>
        <div 
          v-for="(option, index) in formData.options"
          :key="index"
          class="option-item"
        >
          <input 
            type="checkbox"
            v-model="option.isCorrect"
            title="标记为正确答案"
          />
          <input 
            type="text"
            v-model="option.content"
            placeholder="请输入选项内容"
          />
          <button class="delete-option-btn" @click="removeOption(index)">×</button>
        </div>
        <button class="add-option-btn" @click="addOption">+ 添加选项</button>
      </div>
      
      <div class="modal-actions">
        <button class="cancel-btn" @click="$emit('close')">取消</button>
        <button class="submit-btn" @click="saveQuestion">确定</button>
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
.editor-modal {
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
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

.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: vertical;
  min-height: 100px;
}

.options-section label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-item input[type="checkbox"] {
  width: 20px;
  height: 20px;
  cursor: pointer;
}

.option-item input[type="text"] {
  flex: 1;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.delete-option-btn {
  width: 32px;
  height: 32px;
  background: #f44336;
  color: white;
  border-radius: 4px;
  font-size: 20px;
}

.add-option-btn {
  width: 100%;
  padding: 8px;
  background: #f5f5f5;
  border: 1px dashed #ddd;
  border-radius: 4px;
  color: #667eea;
  margin-top: 8px;
}

.add-option-btn:hover {
  background: #e0e0e0;
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
}

.cancel-btn {
  background: #f5f5f5;
  color: #333;
}

.submit-btn {
  background: #667eea;
  color: white;
}
</style>

