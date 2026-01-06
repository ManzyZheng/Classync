<template>
  <div class="login-container">
    <div class="login-box">
      <h1 class="title">Classync</h1>
      <p class="subtitle">课堂互动系统</p>
      
      <div class="form-group">
        <label>账号</label>
        <input 
          v-model="account" 
          type="text" 
          placeholder="请输入账号"
          @keyup.enter="handleLogin"
        />
      </div>
      
      <div class="form-group">
        <label>姓名</label>
        <input 
          v-model="name" 
          type="text" 
          placeholder="请输入姓名"
          @keyup.enter="handleLogin"
        />
      </div>
      
      <button class="login-btn" @click="handleLogin">
        登录
      </button>
    </div>
    
    <!-- 模拟认证弹窗 -->
    <div v-if="showAuthModal" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <h3>模拟身份认证</h3>
        <p>请确认您的身份信息：</p>
        <div class="auth-info">
          <p><strong>账号：</strong>{{ account }}</p>
          <p><strong>姓名：</strong>{{ name }}</p>
        </div>
        <button class="confirm-btn" @click="completeAuth">
          已完成认证
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const account = ref('')
const name = ref('')
const showAuthModal = ref(false)

const handleLogin = () => {
  if (!account.value || !name.value) {
    alert('请输入账号和姓名')
    return
  }
  showAuthModal.value = true
}

const closeModal = () => {
  showAuthModal.value = false
}

const completeAuth = async () => {
  try {
    const user = await api.auth.login({
      account: account.value,
      name: name.value
    })
    
    userStore.setUser(user)
    showAuthModal.value = false
    
    // 检查是否有重定向参数
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
    router.push('/home')
    }
  } catch (error) {
    console.error('Login failed:', error)
    alert('登录失败，请重试')
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  width: 400px;
}

.title {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  text-align: center;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 16px;
  color: #666;
  text-align: center;
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.login-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s;
}

.login-btn:hover {
  transform: translateY(-2px);
}

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
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-content h3 {
  margin-bottom: 16px;
  color: #333;
}

.modal-content p {
  color: #666;
  margin-bottom: 16px;
}

.auth-info {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 20px;
}

.auth-info p {
  margin: 8px 0;
  color: #333;
}

.confirm-btn {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
}

.confirm-btn:hover {
  background: #5568d3;
}
</style>

