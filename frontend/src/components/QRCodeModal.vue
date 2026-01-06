<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>课堂二维码</h3>
        <button class="close-btn" @click="$emit('close')">×</button>
      </div>
      
      <div class="modal-body">
        <div class="qrcode-container">
          <canvas ref="qrcodeCanvas"></canvas>
        </div>
        <div class="qrcode-info">
          <p class="info-text">扫描二维码加入课堂</p>
          <div class="class-code">
            <span class="label">课堂码:</span>
            <span class="code">{{ classCode }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import QRCode from 'qrcode'

const props = defineProps({
  classCode: {
    type: String,
    required: true
  }
})

defineEmits(['close'])

const qrcodeCanvas = ref(null)

const generateQRCode = async () => {
  if (!qrcodeCanvas.value || !props.classCode) return
  
  try {
    // 生成二维码内容：完整的 URL
    const qrContent = `${window.location.origin}/join/${props.classCode}`
    
    await QRCode.toCanvas(qrcodeCanvas.value, qrContent, {
      width: 280,
      margin: 2,
      color: {
        dark: '#000000',
        light: '#FFFFFF'
      }
    })
  } catch (error) {
    console.error('Failed to generate QR code:', error)
  }
}

onMounted(() => {
  generateQRCode()
})

watch(() => props.classCode, () => {
  generateQRCode()
})
</script>

<style scoped>
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
  z-index: 2000;
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

.modal-content {
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 400px;
  max-width: 90vw;
  animation: slideUp 0.3s ease-out;
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
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: #666;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #f5f5f5;
  color: #333;
}

.modal-body {
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-container {
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.qrcode-container canvas {
  display: block;
  border-radius: 8px;
}

.qrcode-info {
  text-align: center;
  width: 100%;
}

.info-text {
  font-size: 14px;
  color: #666;
  margin: 0 0 16px 0;
}

.class-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.class-code .label {
  font-size: 14px;
  color: #666;
}

.class-code .code {
  font-size: 20px;
  font-weight: 600;
  color: #667eea;
  letter-spacing: 2px;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .modal-content {
    width: 340px;
  }
  
  .qrcode-container {
    padding: 12px;
  }
  
  .modal-body {
    padding: 24px 16px;
  }
}
</style>

