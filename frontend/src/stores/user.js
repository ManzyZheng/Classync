import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const currentUser = ref(null)
  const isHost = ref(false)
  
  const setUser = (user) => {
    currentUser.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }
  
  const setIsHost = (value) => {
    isHost.value = value
  }
  
  const loadUser = () => {
    const savedUser = localStorage.getItem('user')
    if (savedUser) {
      currentUser.value = JSON.parse(savedUser)
    }
  }
  
  const logout = () => {
    currentUser.value = null
    isHost.value = false
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

