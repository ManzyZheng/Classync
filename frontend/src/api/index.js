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
     * 生成词云URL
     * @param {Object} keywords - 词云权重对象，如 {"词1": 12, "词2": 1}
     * @param {String} bgColor - 背景色，支持英文颜色名或rgb/rgba，如 "black", "red", "rgb(255,0,0)"
     * @param {String} maskImage - 遮罩图片URL（可选）
     * @returns {String} 词云页面URL
     */
    generateUrl: (keywords, bgColor = 'black', maskImage = null) => {
      const baseUrl = 'http://niningqi.com/api/wordcloud'
      const params = new URLSearchParams()

      // 将keywords对象转换为JSON字符串并编码
      const keywordsJson = JSON.stringify(keywords)
      params.append('keywords', keywordsJson)

      // 添加背景色
      if (bgColor) {
        params.append('bgColor', bgColor)
      }

      // 添加遮罩图片（如果提供）
      if (maskImage) {
        params.append('maskImage', maskImage)
      }

      return `${baseUrl}?${params.toString()}`
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

