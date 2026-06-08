<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Bell, WarningFilled, SuccessFilled } from '@element-plus/icons-vue';
import { getNotifications, getUnreadCount, markAsRead, markAllAsRead, type NotificationItem, type NotificationPage } from '../api/notification';

const router = useRouter();
const popoverVisible = ref(false);
const notifications = ref<NotificationItem[]>([]);
const unreadCount = ref(0);
const currentPage = ref(0);
const totalPages = ref(0);
const loading = ref(false);

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount();
    unreadCount.value = res.data;
  } catch {
    unreadCount.value = 0;
  }
};

const fetchNotifications = async () => {
  loading.value = true;
  try {
    const res = await getNotifications(currentPage.value, 8);
    const page: NotificationPage = res.data;
    notifications.value = page.content;
    totalPages.value = page.totalPages;
  } catch {
    notifications.value = [];
  } finally {
    loading.value = false;
  }
};

const handlePopoverShow = () => {
  currentPage.value = 0;
  fetchNotifications();
  fetchUnreadCount();
};

const handleMarkAsRead = async (item: NotificationItem) => {
  if (!item.read) {
    try {
      await markAsRead(item.id);
      item.read = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    } catch {}
  }
  if (item.link) {
    popoverVisible.value = false;
    router.push(item.link);
  }
};

const handleMarkAllAsRead = async () => {
  try {
    await markAllAsRead();
    notifications.value.forEach(n => n.read = true);
    unreadCount.value = 0;
  } catch {}
};

const handlePageChange = (page: number) => {
  currentPage.value = page - 1;
  fetchNotifications();
};

const getTypeIcon = (type: string) => {
  return type === 'WARNING' ? WarningFilled : SuccessFilled;
};

const getTypeColor = (type: string) => {
  return type === 'WARNING' ? '#F56C6C' : '#67C23A';
};

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return '刚刚';
  if (minutes < 60) return `${minutes}分钟前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}天前`;
  return date.toLocaleDateString('zh-CN');
};

const badgeDisplay = () => {
  if (unreadCount.value > 99) return '99+';
  if (unreadCount.value > 0) return String(unreadCount.value);
  return '';
};

onMounted(() => {
  fetchUnreadCount();
  setInterval(fetchUnreadCount, 30000);
});

defineExpose({ fetchUnreadCount });
</script>

<template>
  <el-popover
    :visible="popoverVisible"
    placement="bottom-end"
    :width="380"
    trigger="click"
    @show="handlePopoverShow"
  >
    <template #reference>
      <div class="notification-bell" @click="popoverVisible = !popoverVisible">
        <el-badge :value="badgeDisplay()" :hidden="unreadCount === 0" :max="99">
          <el-icon :size="20" class="bell-icon">
            <Bell />
          </el-icon>
        </el-badge>
      </div>
    </template>

    <div class="notification-panel">
      <div class="notification-header">
        <span class="notification-title">消息通知</span>
        <el-button
          v-if="unreadCount > 0"
          type="primary"
          link
          size="small"
          @click="handleMarkAllAsRead"
        >
          全部已读
        </el-button>
      </div>

      <div class="notification-list" v-loading="loading">
        <div v-if="notifications.length === 0" class="notification-empty">
          暂无消息
        </div>
        <div
          v-for="item in notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.read }"
          @click="handleMarkAsRead(item)"
        >
          <div class="notification-item-icon">
            <el-icon :size="20" :color="getTypeColor(item.type)">
              <component :is="getTypeIcon(item.type)" />
            </el-icon>
          </div>
          <div class="notification-item-content">
            <div class="notification-item-title">{{ item.title }}</div>
            <div class="notification-item-text">{{ item.content }}</div>
            <div class="notification-item-time">{{ formatTime(item.createdAt) }}</div>
          </div>
          <div v-if="!item.read" class="notification-item-dot"></div>
        </div>
      </div>

      <div v-if="totalPages > 1" class="notification-pagination">
        <el-pagination
          small
          layout="prev, pager, next"
          :total="totalPages * 8"
          :page-size="8"
          :current-page="currentPage + 1"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </el-popover>
</template>

<style scoped>
.notification-bell {
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 0 8px;
  height: 100%;
  transition: background 0.3s;
}

.notification-bell:hover {
  background: rgba(0, 0, 0, 0.025);
}

.bell-icon {
  color: #606266;
}

.notification-panel {
  margin: -12px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.notification-empty {
  padding: 40px 0;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.notification-item:hover {
  background: #f5f7fa;
}

.notification-item.unread {
  background: #ecf5ff;
}

.notification-item.unread:hover {
  background: #d9ecff;
}

.notification-item-icon {
  flex-shrink: 0;
  margin-right: 12px;
  margin-top: 2px;
}

.notification-item-content {
  flex: 1;
  min-width: 0;
}

.notification-item-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.notification-item-text {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 4px;
  word-break: break-all;
}

.notification-item-time {
  font-size: 11px;
  color: #909399;
}

.notification-item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409EFF;
  flex-shrink: 0;
  margin-top: 6px;
  margin-left: 8px;
}

.notification-pagination {
  display: flex;
  justify-content: center;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
}
</style>
