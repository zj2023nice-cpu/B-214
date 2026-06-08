import axios from './axios';

export interface NotificationItem {
  id: number;
  title: string;
  content: string;
  type: string;
  read: boolean;
  createdAt: string;
  link: string | null;
  user: { id: number; username: string };
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const getNotifications = (page: number, size: number = 10) => {
  return axios.get<any, { code: number; data: NotificationPage }>('/notifications', {
    params: { page, size }
  });
};

export const getUnreadCount = () => {
  return axios.get<any, { code: number; data: number }>('/notifications/unread-count');
};

export const markAsRead = (id: number) => {
  return axios.put<any, { code: number; data: NotificationItem }>(`/notifications/${id}/read`);
};

export const markAllAsRead = () => {
  return axios.put<any, { code: number; data: null }>('/notifications/read-all');
};
