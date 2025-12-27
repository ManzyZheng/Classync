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
    }
  },
  
  // 问题相关
  question: {
    getByClassroom: (classroomId) => api.get(`/questions/classroom/${classroomId}`),
    getById: (id) => api.get(`/questions/${id}`),
    create: (data) => api.post('/questions', data),
    update: (id, data) => api.put(`/questions/${id}`, data),
    delete: (id) => api.delete(`/questions/${id}`),
    toggle: (id) => api.post(`/questions/${id}/toggle`),
    finish: (id) => api.post(`/questions/${id}/finish`)
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
  }
}

