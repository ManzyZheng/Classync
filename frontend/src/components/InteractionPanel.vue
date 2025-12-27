<template>
  <div class="interaction-panel">
    <div class="tabs">
      <button 
        :class="{ active: activeTab === 'interaction' }"
        @click="activeTab = 'interaction'"
      >
        互动
      </button>
      <button 
        :class="{ active: activeTab === 'discussion' }"
        @click="activeTab = 'discussion'"
      >
        讨论区
      </button>
    </div>
    
    <div class="tab-content">
      <InteractionTab 
        v-if="activeTab === 'interaction'" 
        :classroom-id="classroomId"
        :is-host="isHost"
      />
      <DiscussionTab 
        v-if="activeTab === 'discussion'" 
        :classroom-id="classroomId"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import InteractionTab from './InteractionTab.vue'
import DiscussionTab from './DiscussionTab.vue'

defineProps({
  classroomId: Number,
  isHost: Boolean
})

const activeTab = ref('interaction')
</script>

<style scoped>
.interaction-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: white;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #ddd;
}

.tabs button {
  flex: 1;
  padding: 12px;
  background: transparent;
  border: none;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tabs button.active {
  color: #667eea;
  border-bottom: 2px solid #667eea;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}
</style>

