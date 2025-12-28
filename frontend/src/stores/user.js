import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref(null)
  const isHost = ref(false)

  const setUser = (user) => {
    currentUser.value = user
    // 使用 sessionStorage 而不是 localStorage，这样每个标签页独立存储
    // 可以在不同标签页同时登录不同账号
    sessionStorage.setItem('user', JSON.stringify(user))
  }

  const setIsHost = (value) => {
    isHost.value = value
  }

  const loadUser = () => {
    // 优先从 sessionStorage 加载，如果没有则尝试从 localStorage 迁移（兼容旧数据）
    let savedUser = sessionStorage.getItem('user')
    if (!savedUser) {
      savedUser = localStorage.getItem('user')
      // 如果从 localStorage 找到，迁移到 sessionStorage 并清除 localStorage
      if (savedUser) {
        sessionStorage.setItem('user', savedUser)
        localStorage.removeItem('user')
      }
    }
    if (savedUser) {
      currentUser.value = JSON.parse(savedUser)
    }
  }

  const logout = () => {
    currentUser.value = null
    isHost.value = false
    sessionStorage.removeItem('user')
    // 同时清除 localStorage（如果有遗留数据）
    localStorage.removeItem('user')
  }

  return {
    currentUser,
    isHost,
    setUser,
    setIsHost,
    loadUser,
    logout
  }
})

