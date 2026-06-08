<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import dayjs from 'dayjs';
import { ElMessage, ElMessageBox } from 'element-plus';

const pendingRecords = ref([]);
const loading = ref(false);

const fetchPending = async () => {
  loading.value = true;
  try {
    const res: any = await axios.get('/inventory/outbound/pending');
    if (res.code === 200) pendingRecords.value = res.data;
  } finally {
    loading.value = false;
  }
};

const handleApprove = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认批准出库申请「${row.serialNo}」？批准后将立即扣减库存并完成出库。`,
      '审批确认',
      { confirmButtonText: '确认批准', cancelButtonText: '取消', type: 'warning' }
    );
    const res: any = await axios.post(`/inventory/outbound/approve/${row.id}`);
    if (res.code === 200) {
      ElMessage.success('审批通过，已完成出库并扣减库存');
      fetchPending();
    }
  } catch {}
};

const rejectDialogVisible = ref(false);
const rejectForm = ref({ id: null as number | null, rejectReason: '' });

const openRejectDialog = (row: any) => {
  rejectForm.value = { id: row.id, rejectReason: '' };
  rejectDialogVisible.value = true;
};

const handleReject = async () => {
  if (!rejectForm.value.rejectReason.trim()) {
    ElMessage.error('请填写拒绝原因');
    return;
  }
  const res: any = await axios.post(`/inventory/outbound/reject/${rejectForm.value.id}`, {
    rejectReason: rejectForm.value.rejectReason
  });
  if (res.code === 200) {
    ElMessage.success('已拒绝出库申请');
    rejectDialogVisible.value = false;
    fetchPending();
  }
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
};

onMounted(() => {
  fetchPending();
});
</script>

<template>
  <div class="approval-container">
    <el-card header="出库审批">
      <el-table :data="pendingRecords" border style="width: 100%" v-loading="loading">
        <el-table-column prop="serialNo" label="出库单号" width="180" />
        <el-table-column label="物资名称">
          <template #default="scope">
            {{ scope.row.material?.name }} ({{ scope.row.material?.code }})
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="receiver" label="领用人" width="120" />
        <el-table-column prop="department" label="领用部门" width="120" />
        <el-table-column prop="purpose" label="出库用途" min-width="150" />
        <el-table-column label="申请时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.outboundTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button type="success" size="small" @click="handleApprove(scope.row)">批准</el-button>
            <el-button type="danger" size="small" @click="openRejectDialog(scope.row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && pendingRecords.length === 0" description="暂无待审批的出库申请" />
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="拒绝出库申请" width="500px">
      <el-form label-width="100px">
        <el-form-item label="拒绝原因" required>
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="3"
            placeholder="请输入拒绝原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.approval-container {
  padding: 20px;
}
</style>
