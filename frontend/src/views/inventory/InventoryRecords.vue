<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import dayjs from 'dayjs';
import { ElMessage, ElMessageBox } from 'element-plus';

const activeTab = ref('inbound');
const inboundRecords = ref([]);
const outboundRecords = ref([]);
const dateRange = ref<[string, string] | null>(null);
const exporting = ref(false);
const inboundTableRef = ref();
const outboundTableRef = ref();
const selectedInbound = ref<any[]>([]);
const selectedOutbound = ref<any[]>([]);
const batchDeleting = ref(false);

const fetchInbound = async () => {
  const params: any = {};
  if (dateRange.value) {
    params.startDate = dateRange.value[0];
    params.endDate = dateRange.value[1];
  }
  const res: any = await axios.get('/inventory/records/inbound', { params });
  if (res.code === 200) inboundRecords.value = res.data;
};

const fetchOutbound = async () => {
  const params: any = {};
  if (dateRange.value) {
    params.startDate = dateRange.value[0];
    params.endDate = dateRange.value[1];
  }
  const res: any = await axios.get('/inventory/records/outbound', { params });
  if (res.code === 200) outboundRecords.value = res.data;
};

const handleSearch = () => {
  if (activeTab.value === 'inbound') fetchInbound();
  else fetchOutbound();
};

const handleReset = () => {
  dateRange.value = null;
  if (activeTab.value === 'inbound') fetchInbound();
  else fetchOutbound();
};

const handleExport = async () => {
  if (exporting.value) return;
  exporting.value = true;
  try {
    const params: any = { type: activeTab.value };
    if (dateRange.value) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }
    const response = await fetch(`/api/inventory/records/export?${new URLSearchParams(params).toString()}`);
    if (!response.ok) throw new Error('Export failed');
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    const disposition = response.headers.get('Content-Disposition');
    const match = disposition?.match(/filename=(.+)/);
    link.download = match ? match[1] : `${activeTab.value}_records.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch {
    ElMessage.error('导出失败');
  } finally {
    exporting.value = false;
  }
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
};

const handleInboundSelectionChange = (selection: any[]) => {
  selectedInbound.value = selection;
};

const handleOutboundSelectionChange = (selection: any[]) => {
  selectedOutbound.value = selection;
};

const handleBatchDeleteInbound = () => {
  if (selectedInbound.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${selectedInbound.value.length} 条记录吗？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    batchDeleting.value = true;
    try {
      const ids = selectedInbound.value.map((item: any) => item.id);
      const res: any = await axios.delete('/inventory/records/inbound/batch', { data: ids });
      if (res.code === 200) {
        const result = res.data;
        if (result.failureCount === 0) {
          ElMessage.success(`删除成功，共删除 ${result.successCount} 条记录`);
        } else {
          ElMessage.warning(`操作完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`);
        }
        selectedInbound.value = [];
        inboundTableRef.value?.clearSelection();
        fetchInbound();
      }
    } catch {
      ElMessage.error('批量删除失败');
    } finally {
      batchDeleting.value = false;
    }
  });
};

const handleBatchDeleteOutbound = () => {
  if (selectedOutbound.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${selectedOutbound.value.length} 条记录吗？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    batchDeleting.value = true;
    try {
      const ids = selectedOutbound.value.map((item: any) => item.id);
      const res: any = await axios.delete('/inventory/records/outbound/batch', { data: ids });
      if (res.code === 200) {
        const result = res.data;
        if (result.failureCount === 0) {
          ElMessage.success(`删除成功，共删除 ${result.successCount} 条记录`);
        } else {
          ElMessage.warning(`操作完成，成功 ${result.successCount} 条，失败 ${result.failureCount} 条`);
        }
        selectedOutbound.value = [];
        outboundTableRef.value?.clearSelection();
        fetchOutbound();
      }
    } catch {
      ElMessage.error('批量删除失败');
    } finally {
      batchDeleting.value = false;
    }
  });
};

const handleTabChange = () => {
  if (activeTab.value === 'inbound') fetchInbound();
  else fetchOutbound();
};

onMounted(() => {
  fetchInbound();
});
</script>

<template>
  <div class="records-container">
    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="margin-right: 12px"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="success" :loading="exporting" :disabled="exporting" @click="handleExport">
        {{ exporting ? '导出中...' : '导出' }}
      </el-button>
      <el-button
        v-if="activeTab === 'inbound'"
        type="danger"
        :disabled="selectedInbound.length === 0"
        :loading="batchDeleting"
        @click="handleBatchDeleteInbound"
      >
        批量删除{{ selectedInbound.length > 0 ? `(${selectedInbound.length})` : '' }}
      </el-button>
      <el-button
        v-else
        type="danger"
        :disabled="selectedOutbound.length === 0"
        :loading="batchDeleting"
        @click="handleBatchDeleteOutbound"
      >
        批量删除{{ selectedOutbound.length > 0 ? `(${selectedOutbound.length})` : '' }}
      </el-button>
    </div>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="入库记录" name="inbound">
        <el-table ref="inboundTableRef" :data="inboundRecords" border style="width: 100%" @selection-change="handleInboundSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="serialNo" label="入库单号" width="180" />
          <el-table-column label="物资名称">
            <template #default="scope">
              {{ scope.row.material?.name }} ({{ scope.row.material?.code }})
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="price" label="单价" width="100" />
          <el-table-column label="入库时间" width="180">
            <template #default="scope">{{ formatDate(scope.row.inboundTime) }}</template>
          </el-table-column>
          <el-table-column label="供应商">
            <template #default="scope">{{ scope.row.supplier?.name || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="出库记录" name="outbound">
        <el-table ref="outboundTableRef" :data="outboundRecords" border style="width: 100%" @selection-change="handleOutboundSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="serialNo" label="出库单号" width="180" />
          <el-table-column label="物资名称">
            <template #default="scope">
              {{ scope.row.material?.name }} ({{ scope.row.material?.code }})
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag
                :type="
                  scope.row.status === 'PENDING' ? 'warning' :
                  scope.row.status === 'APPROVED' || scope.row.status === 'COMPLETED' ? 'success' :
                  scope.row.status === 'REJECTED' ? 'danger' : 'info'
                "
              >
                {{
                  scope.row.status === 'PENDING' ? '待审批' :
                  scope.row.status === 'APPROVED' || scope.row.status === 'COMPLETED' ? '已出库' :
                  scope.row.status === 'REJECTED' ? '已拒绝' : scope.row.status
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="receiver" label="领用人" width="100" />
          <el-table-column prop="purpose" label="出库用途" min-width="120" />
          <el-table-column label="出库时间" width="180">
             <template #default="scope">{{ formatDate(scope.row.outboundTime) }}</template>
          </el-table-column>
          <el-table-column prop="department" label="领用部门" width="120" />
          <el-table-column prop="rejectReason" label="拒绝原因" width="150">
            <template #default="scope">
              {{ scope.row.rejectReason || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.records-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}
.filter-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>
