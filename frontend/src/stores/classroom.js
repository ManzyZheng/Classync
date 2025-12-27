import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useClassroomStore = defineStore('classroom', () => {
  const currentClassroom = ref(null)
  const currentPage = ref(1)
  const isHost = ref(false)
  
  const setClassroom = (classroom) => {
    currentClassroom.value = classroom
    currentPage.value = classroom.currentPage || 1
  }
  
  const setCurrentPage = (page) => {
    currentPage.value = page
  }
  
  const setIsHost = (value) => {
    isHost.value = value
  }
  
  const reset = () => {
    currentClassroom.value = null
    currentPage.value = 1
    isHost.value = false
  }
  
  return {
    currentClassroom,
    currentPage,
    isHost,
    setClassroom,
    setCurrentPage,
    setIsHost,
    reset
  }
})

