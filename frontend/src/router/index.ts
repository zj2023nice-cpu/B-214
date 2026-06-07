import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Layout from '../views/Layout.vue';
import Dashboard from '../views/Dashboard.vue';
import WarehouseList from '../views/warehouse/WarehouseList.vue';
import MaterialList from '../views/material/MaterialList.vue';
import Inbound from '../views/inventory/Inbound.vue';
import Outbound from '../views/inventory/Outbound.vue';
import InventoryRecords from '../views/inventory/InventoryRecords.vue';
import StockReport from '../views/report/StockReport.vue';
import SystemSettings from '../views/settings/SystemSettings.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '仪表盘' }
      },
      {
        path: 'warehouses',
        name: 'WarehouseList',
        component: WarehouseList,
        meta: { title: '仓库管理' }
      },
      {
        path: 'materials',
        name: 'MaterialList',
        component: MaterialList,
        meta: { title: '物资管理' }
      },
      {
        path: 'inventory/inbound',
        name: 'Inbound',
        component: Inbound,
        meta: { title: '物资入库' }
      },
      {
        path: 'inventory/outbound',
        name: 'Outbound',
        component: Outbound,
        meta: { title: '物资出库' }
      },
      {
        path: 'inventory/records',
        name: 'InventoryRecords',
        component: InventoryRecords,
        meta: { title: '出入库记录' }
      },
      {
        path: 'reports/stock',
        name: 'StockReport',
        component: StockReport,
        meta: { title: '库存报表' }
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: SystemSettings,
        meta: { title: '系统设置' }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user');
  if (to.name !== 'Login' && !user) {
    next({ name: 'Login' });
  } else {
    next();
  }
});

export default router;
