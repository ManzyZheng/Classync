import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// 事件常量
export const WS_EVENTS = {
  CLASSROOM_STATE: 'CLASSROOM_STATE',
  PAGE_UPDATE: 'PAGE_UPDATE',
  QUESTION_OPENED: 'QUESTION_OPENED',
  QUESTION_CLOSED: 'QUESTION_CLOSED',
  QUESTION_FINISHED: 'QUESTION_FINISHED',
  ANSWER_SUBMITTED: 'ANSWER_SUBMITTED',
  DISCUSSION_NEW: 'DISCUSSION_NEW',
  DISPLAY_CODE_TOGGLE: 'DISPLAY_CODE_TOGGLE',
  PAGE_LOCK_UPDATE: 'PAGE_LOCK_UPDATE',
  PAGE_LOCK_BATCH: 'PAGE_LOCK_BATCH',
  PAGE_LOCK_BATCH_UNLOCK: 'PAGE_LOCK_BATCH_UNLOCK',
  DISPLAY_QUESTION: 'DISPLAY_QUESTION'
}

class WebSocketService {
  constructor() {
    this.client = null
    this.connected = false
    this.subscriptions = new Map()
    this.eventHandlers = new Map() // 事件处理器注册表
  }

  connect(classroomId, callbacks = {}) {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,

        onConnect: () => {
          console.log('WebSocket Connected')
          this.connected = true

          // 订阅课堂主题
          const subscription = this.client.subscribe(
            `/topic/classroom/${classroomId}`,
            (message) => {
              const data = JSON.parse(message.body)
              this.handleMessage(data, callbacks)
            }
          )

          this.subscriptions.set(classroomId, subscription)

          // 发送加入课堂消息
          this.send('/app/join_classroom', {
            classroomId: classroomId
          })

          resolve()
        },

        onStompError: (frame) => {
          console.error('STOMP Error:', frame)
          this.connected = false
          reject(frame)
        },

        onWebSocketClose: () => {
          console.log('WebSocket Closed')
          this.connected = false
        }
      })

      this.client.activate()
    })
  }

  handleMessage(message, callbacks) {
    const { event, payload } = message

    console.log(`[WebSocket] Event: ${event}`, payload)

    // 细粒度事件分发
    switch (event) {
      case WS_EVENTS.CLASSROOM_STATE:
        callbacks.onClassroomState?.(payload)
        break
      case WS_EVENTS.PAGE_UPDATE:
        callbacks.onPageUpdate?.(payload)
        break
      case WS_EVENTS.QUESTION_OPENED:
        callbacks.onQuestionOpened?.(payload)
        break
      case WS_EVENTS.QUESTION_CLOSED:
        callbacks.onQuestionClosed?.(payload)
        break
      case WS_EVENTS.QUESTION_FINISHED:
        callbacks.onQuestionFinished?.(payload)
        break
      case WS_EVENTS.ANSWER_SUBMITTED:
        callbacks.onAnswerSubmitted?.(payload)
        break
      case WS_EVENTS.DISCUSSION_NEW:
        callbacks.onDiscussionNew?.(payload)
        break
      case WS_EVENTS.DISPLAY_CODE_TOGGLE:
        callbacks.onDisplayCodeToggle?.(payload)
        break
      case WS_EVENTS.PAGE_LOCK_UPDATE:
        callbacks.onPageLockUpdate?.(payload)
        break
      case WS_EVENTS.PAGE_LOCK_BATCH:
        callbacks.onPageLockBatch?.(payload)
        break
      case WS_EVENTS.PAGE_LOCK_BATCH_UNLOCK:
        callbacks.onPageLockBatchUnlock?.(payload)
        break
      case WS_EVENTS.DISPLAY_QUESTION:
        callbacks.onDisplayQuestion?.(payload)
        break
      default:
        console.warn(`Unknown event type: ${event}`)
    }

    // 触发注册的事件处理器
    const handlers = this.eventHandlers.get(event)
    if (handlers) {
      handlers.forEach(handler => handler(payload))
    }
  }

  // 注册事件处理器（替代window.dispatchEvent）
  on(event, handler) {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, [])
    }
    this.eventHandlers.get(event).push(handler)
  }

  // 取消注册
  off(event, handler) {
    const handlers = this.eventHandlers.get(event)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  send(destination, data) {
    if (this.connected && this.client) {
      this.client.publish({
        destination,
        body: JSON.stringify(data)
      })
    } else {
      console.warn('WebSocket not connected, cannot send message')
    }
  }

  disconnect() {
    if (this.client) {
      this.subscriptions.forEach(sub => sub.unsubscribe())
      this.subscriptions.clear()
      this.eventHandlers.clear()
      this.client.deactivate()
      this.connected = false
    }
  }

  // 发送翻页消息
  sendPageChange(classroomId, pageNumber) {
    this.send('/app/page_change', {
      classroomId,
      data: { pageNumber }
    })
  }

  // 发送问题切换消息
  sendQuestionToggle(classroomId, questionId) {
    this.send('/app/question_toggle', {
      classroomId,
      data: { questionId }
    })
  }

  // 发送问题结束消息
  sendQuestionFinish(classroomId, questionId) {
    this.send('/app/question_finish', {
      classroomId,
      data: { questionId }
    })
  }

  // 发送答案提交消息
  sendAnswerSubmit(classroomId, questionId) {
    this.send('/app/answer_submit', {
      classroomId,
      data: { questionId }
    })
  }

  // 发送新讨论消息
  sendDiscussionNew(classroomId, discussion) {
    this.send('/app/discussion_new', {
      classroomId,
      data: discussion
    })
  }

  // 发送展示课堂码切换消息
  sendDisplayCodeToggle(classroomId, show) {
    this.send('/app/display_code_toggle', {
      classroomId,
      data: { show }
    })
  }

  // 发送页面锁定切换消息
  sendPageLockToggle(classroomId, pageNumber, isLocked) {
    this.send('/app/page_lock_toggle', {
      classroomId,
      data: { pageNumber, isLocked }
    })
  }

  // 发送批量锁定消息
  sendPageLockBatch(classroomId, fromPage) {
    this.send('/app/page_lock_batch', {
      classroomId,
      data: { fromPage }
    })
  }

  // 发送批量解锁消息
  sendPageLockBatchUnlock(classroomId, fromPage = 1) {
    this.send('/app/page_lock_batch_unlock', {
      classroomId,
      data: { fromPage }
    })
  }
  
  // 发送问题展示消息
  sendDisplayQuestion(classroomId, questionId, mode) {
    this.send('/app/display_question', {
      classroomId,
      data: {
        questionId,
        mode
      }
    })
  }
}

export default new WebSocketService()

