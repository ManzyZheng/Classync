import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Home from '../views/Home.vue'
import ClassroomHost from '../views/ClassroomHost.vue'
import ClassroomViewer from '../views/ClassroomViewer.vue'
import ClassroomDisplay from '../views/ClassroomDisplay.vue'
import JoinClassroom from '../views/JoinClassroom.vue'
import InteractionOnly from '../views/InteractionOnly.vue'

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
  },
  {
    path: '/classroom/display/:id',
    name: 'ClassroomDisplay',
    component: ClassroomDisplay
  },
  {
    path: '/join/:classCode',
    name: 'JoinClassroom',
    component: JoinClassroom
  },
  {
    path: '/interaction/:id',
    name: 'InteractionOnly',
    component: InteractionOnly
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

