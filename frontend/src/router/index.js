import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import ClassroomHost from '../views/ClassroomHost.vue'
import ClassroomViewer from '../views/ClassroomViewer.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/home',
    name: 'Home',
    component: Home
  },
  {
    path: '/classroom/host/:id',
    name: 'ClassroomHost',
    component: ClassroomHost
  },
  {
    path: '/classroom/viewer/:id',
    name: 'ClassroomViewer',
    component: ClassroomViewer
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

