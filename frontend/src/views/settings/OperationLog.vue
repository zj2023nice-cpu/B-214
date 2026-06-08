<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import dayjs from 'dayjs';

const logs = ref<any[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const filterUsername = ref('');
const filterOperationType = ref('');
const dateRange = ref<[string, string] | null>(null);

const operationTypes = [
  { label: '登录', value: 'LOGIN' },
  { label: '新增', value: 'CREATE' },
  { label: '修改', value: 'UPDATE' },
  { label: '删除', value: 'DELETE' },
  { label: '入库', value: 'INBOUND' },
  { label: '出库', value: 'OUTBOUND' },
  { label: '调拨', value: 'TRANSFER' },
  { label: '盘点', value: 'CHECK' },
  { label: '导入', value: 'IMPORT' }
];

const fetchLogs = async () => {
  const params: any = {
    page: currentPage.value - 1,
    size: pageSize.value
  };
  if (filterUsername.value) params.username = filterUsername.value;
  if (filterOperationType.value) params.operationType = filterOperationType.value;
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dayjs(dateRange.value[0]).startOf('day').format('YYYY-MM-DDTHH:mm:ss');
    params.endTime = dayjs(dateRange.value[1]).endOf('day').format('YYYY-MM-DDTHH:mm:ss');
  }
  const res: any = await axios.get('/operation-logs', { params });
  if (res.code === 200) {
    logs.value = res.data.content;
    total.value = res.data.totalElements;
  }
};

const handleSearch = () => {
  currentPage.value = 1;
  fetchLogs();
};

const handleReset = () => {
  filterUsername.value = '';
  filterOperationType.value = '';
  dateRange.value = null;
  currentPage.value = 1;
  fetchLogs();
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  fetchLogs();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  fetchLogs();
};

const formatDate = (date: string) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-';
};

const typeLabel = (type: string) => {
  const found = operationTypes.find(t => t.value === type);
  return found ? found.label : type;
};

const typeTagType = (type: string) => {
  switch (type) {
    case 'LOGIN': return '';
    case 'CREATE': return 'success';
    case 'UPDATE': return 'warning';
    case 'DELETE': return 'danger';
    case 'INBOUND': return 'success';
    case 'OUTBOUND': return 'warning';
    case 'TRANSFER': return '';
    case 'CHECK': return 'info';
    case 'IMPORT': return 'success';
    default: return 'info';
  }
};

const resultTagType = (result: string) => {
  return result === 'SUCCESS' ? 'success' : 'danger';
};

onMounted(() => {
  fetchLogs();
});
</script>

<template>
  <div class="operation-log-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
        </div>
      </template>

      <div class="filter-bar">
        <el-input
          v-model="filterUsername"
          placeholder="操作人"
          clearable
          style="width: 160px; margin-right: 12px;"
        />
        <el-select
          v-model="filterOperationType"
          placeholder="操作类型"
          clearable
          style="width: 140px; margin-right: 12px;"
        >
          <el-option
            v-for="t in operationTypes"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="margin-right: 12px;"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="logs" border style="width: 100%; margin-top: 16px;">
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column label="操作类型" width="100">
          <template #default="scope">
            <el-tag :type="typeTagType(scope.row.operationType)" size="small">
              {{ typeLabel(scope.row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" width="150" />
        <el-table-column prop="operationTarget" label="操作对象" width="100" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column label="操作时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.operationTime) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="scope">
            <el-tag :type="resultTagType(scope.row.result)" size="small">
              {{ scope.row.result === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="调用方法" min-width="200" show-overflow-tooltip />
        <el-table-column prop="errorMsg" label="错误信息" min-width="150" show-overflow-tooltip />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.operation-log-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
