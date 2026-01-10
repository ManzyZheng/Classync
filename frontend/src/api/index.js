import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default {
  // 用户相关
  auth: {
    login: (data) => api.post('/auth/login', data),
    getUser: (id) => api.get(`/users/${id}`)
  },

  // 课堂相关
  classroom: {
    create: (data) => api.post('/classrooms', data),
    getById: (id) => api.get(`/classrooms/${id}`),
    getByCode: (code) => api.get(`/classrooms/code/${code}`),
    getByHostUser: (userId) => api.get(`/classrooms/host/${userId}`),
    getByParticipantUser: (userId) => api.get(`/classrooms/participant/${userId}`),
    recordParticipant: (classroomId, userId) => api.post(`/classrooms/${classroomId}/participant/${userId}`),
    removeParticipant: (classroomId, userId) => api.delete(`/classrooms/${classroomId}/participant/${userId}`),
    delete: (id) => api.delete(`/classrooms/${id}`),
    uploadPdf: (id, file) => {
      const formData = new FormData()
      formData.append('file', file)
      return api.post(`/classrooms/${id}/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    },
    setDisplayQuestion: (classroomId, questionId, mode) => api.post(`/classrooms/${classroomId}/display-question`, {
      questionId,
      mode
    }),
    // 复制相关
    getFileToCopy: (id) => api.get(`/classrooms/${id}/copy-file`),
    copyQuestionsToClassroom: (sourceId, targetId) => api.post(`/classrooms/${sourceId}/copy-questions-to/${targetId}`),
    copyEntireClassroom: (sourceId, data) => api.post(`/classrooms/${sourceId}/copy`, data)
  },

  // 问题相关
  question: {
    getByClassroom: (classroomId) => api.get(`/questions/classroom/${classroomId}`),
    getById: (id) => api.get(`/questions/${id}`),
    create: (data) => api.post('/questions', data),
    update: (id, data) => api.put(`/questions/${id}`, data),
    delete: (id) => api.delete(`/questions/${id}`),
    toggle: (id) => api.post(`/questions/${id}/toggle`),
    finish: (id) => api.post(`/questions/${id}/finish`),
    // 复制相关
    copyQuestion: (sourceId, targetClassroomId) => api.post(`/questions/${sourceId}/copy-to/${targetClassroomId}`)
  },

  // 答案相关
  answer: {
    submit: (data) => api.post('/answers', data),
    getByQuestion: (questionId) => api.get(`/answers/question/${questionId}`),
    getStatistics: (questionId) => api.get(`/answers/statistics/${questionId}`),
    getEssayAnswers: (questionId) => api.get(`/answers/essay/${questionId}`)
  },

  // 讨论相关
  discussion: {
    getByClassroom: (classroomId) => api.get(`/discussions/classroom/${classroomId}`),
    create: (data) => api.post('/discussions', data)
  },

  // 词云相关
  wordcloud: {
    /**
     * 生成词云URL（使用与 lddgo.net 相同的 ECharts 技术）
     * 由于 lddgo.net 没有API且存在跨域限制，我们使用ECharts直接在本地生成词云
     * 这样可以获得相同的视觉效果，同时避免跨域问题
     * @param {Object} keywords - 词云权重对象，如 {"词1": 12, "词2": 1}
     * @param {String} bgColor - 背景色，支持英文颜色名或rgb/rgba，如 "white", "black"
     * @param {String} maskImage - 遮罩图片URL（可选，暂未实现）
     * @returns {String} 词云页面URL（blob URL）
     */
    generateUrl: (keywords, bgColor = 'white', maskImage = null) => {
      // 将keywords对象转换为数据数组格式：[[word, weight], ...]
      const data = Object.entries(keywords).map(([word, weight]) => [word, Number(weight)])

      // 创建包含ECharts词云的HTML页面
      // 使用与 lddgo.net 相同的 ECharts 和 echarts-wordcloud 库
      const htmlContent = `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>词云生成</title>
  <style>
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
    }
    body {
      width: 100%;
      height: 100vh;
      background: ${bgColor};
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    #wordcloud-container {
      width: 100%;
      height: 100%;
    }
  </style>
  <!-- 使用与 lddgo.net 相同的 ECharts 库 -->
  <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/echarts-wordcloud@2.1.0/dist/echarts-wordcloud.min.js"></script>
</head>
<body>
  <div id="wordcloud-container"></div>
  <script>
    (function() {
      const data = ${JSON.stringify(data)};
      const container = document.getElementById('wordcloud-container');
      const chart = echarts.init(container);
      
      // 生成颜色函数（使用丰富的颜色方案）
      const colors = [
        '#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe',
        '#43e97b', '#38f9d7', '#fa709a', '#fee140', '#30cfd0',
        '#a8edea', '#fed6e3', '#ff9a9e', '#fecfef', '#fec163',
        '#667eea', '#764ba2', '#f093fb', '#4facfe', '#00f2fe'
      ];
      let colorIndex = 0;
      const getColor = function() {
        const color = colors[colorIndex % colors.length];
        colorIndex++;
        return color;
      };
      
      // 使用与 lddgo.net 相同的 ECharts 配置格式
      // 优化参数让词云更大更饱满
      const option = {
        backgroundColor: '${bgColor}',
        series: [{
          type: 'wordCloud',
          gridSize: 6,  // 减小网格大小，让词更密集
          sizeRange: [20, 80],  // 增大字体范围
          rotationRange: [-90, 90],
          rotationStep: 45,
          shape: 'circle',
          width: '100%',
          height: '100%',
          drawOutOfBound: false,
          textStyle: {
            fontFamily: 'Microsoft YaHei, Arial, sans-serif',
            fontWeight: 'bold',
            color: getColor
          },
          emphasis: {
            focus: 'self',
            textStyle: {
              shadowBlur: 10,
              shadowColor: '#333'
            }
          },
          data: data.map(([word, value]) => ({
            name: word,
            value: value
          }))
        }]
      };
      
      chart.setOption(option);
      
      // 响应窗口大小变化
      window.addEventListener('resize', function() {
        chart.resize();
      });
    })();
  </script>
</body>
</html>
      `

      // 创建Blob URL
      const blob = new Blob([htmlContent], { type: 'text/html;charset=utf-8' })
      return URL.createObjectURL(blob)
    }
  },

  // 页面锁定相关
  pageLock: {
    getByClassroom: (classroomId) => api.get(`/page-locks/classroom/${classroomId}`),
    toggle: (classroomId, pageNumber) => api.post('/page-locks/toggle', { classroomId, pageNumber }),
    lockFrom: (classroomId, fromPage, totalPages) => api.post('/page-locks/lock-from', { classroomId, fromPage, totalPages }),
    unlockFrom: (classroomId, fromPage) => api.post('/page-locks/unlock-from', { classroomId, fromPage }),
    unlockAll: (classroomId) => api.post('/page-locks/unlock-all', { classroomId })
  }
}

