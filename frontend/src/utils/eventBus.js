// 简单的事件总线
import { ref } from 'vue'

const listeners = new Map()

export const eventBus = {
  on(event, callback) {
    if (!listeners.has(event)) {
      listeners.set(event, [])
    }
    listeners.get(event).push(callback)
  },
  
  off(event, callback) {
    if (!listeners.has(event)) return
    const callbacks = listeners.get(event)
    const index = callbacks.indexOf(callback)
    if (index > -1) {
      callbacks.splice(index, 1)
    }
  },
  
  emit(event, data) {
    if (!listeners.has(event)) return
    listeners.get(event).forEach(callback => callback(data))
  }
}

