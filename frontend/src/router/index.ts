import { createRouter, createWebHistory } from 'vue-router';
import Login from '../views/Login.vue';
import Layout from '../views/Layout.vue';
import Dashboard from '../views/Dashboard.vue';
import WarehouseList from '../views/warehouse/WarehouseList.vue';
import MaterialList from '../views/material/MaterialList.vue';
import Inbound from '../views/inventory/Inbound.vue';
import Outbound from '../views/inventory/Outbound.vue';
import InventoryRecords from '../views/inventory/InventoryRecords.vue';
import InventoryCheck from '../views/inventory/InventoryCheck.vue';
import TransferRecord from '../views/inventory/TransferRecord.vue';
import AlertCenter from '../views/inventory/AlertCenter.vue';
import StockReport from '../views/report/StockReport.vue';
import SystemSettings from '../views/settings/SystemSettings.vue';
import OperationLog from '../views/settings/OperationLog.vue';
import SupplierRating from '../views/supplier/SupplierRating.vue';
import Forbidden from '../views/Forbidden.vue';

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: Forbidden,
    meta: { title: '访问被拒绝' }
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
        meta: { title: '仪表盘', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'warehouses',
        name: 'WarehouseList',
        component: WarehouseList,
        meta: { title: '仓库管理', roles: ['ADMIN'] }
      },
      {
        path: 'materials',
        name: 'MaterialList',
        component: MaterialList,
        meta: { title: '物资管理', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'inventory/inbound',
        name: 'Inbound',
        component: Inbound,
        meta: { title: '物资入库', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'inventory/outbound',
        name: 'Outbound',
        component: Outbound,
        meta: { title: '物资出库', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'inventory/records',
        name: 'InventoryRecords',
        component: InventoryRecords,
        meta: { title: '出入库记录', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'inventory/check',
        name: 'InventoryCheck',
        component: InventoryCheck,
        meta: { title: '库存盘点', roles: ['ADMIN'] }
      },
      {
        path: 'inventory/transfer',
        name: 'TransferRecord',
        component: TransferRecord,
        meta: { title: '物资调拨', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'inventory/alert-center',
        name: 'AlertCenter',
        component: AlertCenter,
        meta: { title: '库存预警中心', roles: ['ADMIN', 'USER'] }
      },
      {
        path: 'reports/stock',
        name: 'StockReport',
        component: StockReport,
        meta: { title: '库存报表', roles: ['ADMIN'] }
      },
      {
        path: 'settings',
        name: 'SystemSettings',
        component: SystemSettings,
        meta: { title: '系统设置', roles: ['ADMIN'] }
      },
      {
        path: 'settings/operation-log',
        name: 'OperationLog',
        component: OperationLog,
        meta: { title: '操作日志', roles: ['ADMIN'] }
      },
      {
        path: 'supplier-rating',
        name: 'SupplierRating',
        component: SupplierRating,
        meta: { title: '供应商评价', roles: ['ADMIN', 'USER'] }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('user');

  if (to.name === 'Login') {
    next();
    return;
  }

  if (!userStr) {
    next({ name: 'Login' });
    return;
  }

  if (to.name === 'Forbidden') {
    next();
    return;
  }

  const allowedRoles = to.meta?.roles as string[] | undefined;
  if (allowedRoles && allowedRoles.length > 0) {
    try {
      const user = JSON.parse(userStr);
      const userRole = user.role || 'USER';
      if (!allowedRoles.includes(userRole)) {
        next({ name: 'Forbidden' });
        return;
      }
    } catch {
      next({ name: 'Login' });
      return;
    }
  }

  next();
});

export default router;
