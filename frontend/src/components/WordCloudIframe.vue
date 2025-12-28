<template>
  <div class="wordcloud-iframe-container">
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>正在生成词云...</p>
    </div>
    <div v-if="error" class="error">
      <p>{{ error }}</p>
    </div>
    <iframe
      v-if="wordcloudUrl && !error"
      ref="wordcloudIframe"
      :src="wordcloudUrl"
      class="wordcloud-iframe"
      frameborder="0"
      @load="handleLoad"
      @error="handleError"
    ></iframe>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import api from '../api'

const props = defineProps({
  // 词云权重对象，如 {"词1": 12, "词2": 1}
  keywords: {
    type: Object,
    required: true,
    default: () => ({})
  },
  // 背景色，支持英文颜色名或rgb/rgba
  bgColor: {
    type: String,
    default: 'white'
  },
  // 遮罩图片URL（可选）
  maskImage: {
    type: String,
    default: null
  }
})

const wordcloudUrl = ref('')
const loading = ref(false)
const error = ref(null)
const wordcloudIframe = ref(null)

// 生成词云URL
const generateWordCloudUrl = () => {
  try {
    error.value = null
    loading.value = false
    
    // 检查keywords是否为空
    if (!props.keywords || Object.keys(props.keywords).length === 0) {
      console.log('[WordCloudIframe] Keywords is empty, showing no data message')
      error.value = '暂无数据（请等待学生提交答案或检查答案内容）'
      wordcloudUrl.value = ''
      loading.value = false
      return
    }
    
    console.log('[WordCloudIframe] Generating word cloud URL with', Object.keys(props.keywords).length, 'keywords')
    loading.value = true
    wordcloudUrl.value = api.wordcloud.generateUrl(
      props.keywords,
      props.bgColor,
      props.maskImage
    )
    console.log('[WordCloudIframe] Generated URL:', wordcloudUrl.value)
  } catch (err) {
    console.error('生成词云URL失败:', err)
    error.value = '生成词云URL失败: ' + err.message
    loading.value = false
  }
}

// iframe加载完成
const handleLoad = () => {
  loading.value = false
  error.value = null
}

// iframe加载错误
const handleError = () => {
  loading.value = false
  error.value = '词云加载失败，请检查网络连接'
}

// 监听props变化，重新生成URL
watch(
  () => [props.keywords, props.bgColor, props.maskImage],
  () => {
    generateWordCloudUrl()
  },
  { deep: true, immediate: true }
)

onMounted(() => {
  generateWordCloudUrl()
})
</script>

<style scoped>
.wordcloud-iframe-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
  position: relative;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.wordcloud-iframe {
  width: 200%;
  height: 200%;
  min-height: 600px;
  border: none;
  display: block;
  /* 使用transform放大，使字体更大 */
  transform: scale(3);
  transform-origin: center center;
}

.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  z-index: 10;
  color: #e74c3c;
  font-size: 14px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.error p {
  margin: 0;
}
</style>

