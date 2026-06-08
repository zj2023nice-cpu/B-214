<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';

const checks = ref([]);
const loading = ref(false);
const detailDialogVisible = ref(false);
const createDialogVisible = ref(false);
const currentCheck = ref<any>(null);
const detailRows = ref<any[]>([]);

const createForm = ref({
  remark: ''
});

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待盘点', type: 'info' },
  IN_PROGRESS: { label: '盘点中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' }
};

const fetchChecks = async () => {
  loading.value = true;
  try {
    const res: any = await axios.get('/inventory-checks');
    if (res.code === 200) checks.value = res.data;
  } finally {
    loading.value = false;
  }
};

const handleCreate = async () => {
  const res: any = await axios.post('/inventory-checks', createForm.value);
  if (res.code === 200) {
    ElMessage.success('盘点单已创建');
    createDialogVisible.value = false;
    createForm.value.remark = '';
    fetchChecks();
  }
};

const openDetail = async (row: any) => {
  const res: any = await axios.get(`/inventory-checks/${row.id}`);
  if (res.code === 200) {
    currentCheck.value = res.data;
    detailRows.value = res.data.details.map((d: any) => ({
      ...d,
      actualQuantity: d.actualQuantity ?? d.systemQuantity
    }));
    detailDialogVisible.value = true;
  }
};

const handleSubmit = async () => {
  const hasEmpty = detailRows.value.some(d => d.actualQuantity === null || d.actualQuantity === undefined);
  if (hasEmpty) {
    ElMessage.warning('请填写所有物资的实际盘点数量');
    return;
  }

  await ElMessageBox.confirm('提交盘点结果后，若所有物资均已盘点，系统库存将按实际数量调整，是否继续？', '确认提交');

  const payload = detailRows.value.map(d => ({
    id: d.id,
    actualQuantity: d.actualQuantity,
    remark: d.remark
  }));

  const res: any = await axios.post(`/inventory-checks/${currentCheck.value.id}/submit`, payload);
  if (res.code === 200) {
    ElMessage.success('盘点结果已提交');
    detailDialogVisible.value = false;
    fetchChecks();
  }
};

const formatDate = (date: string) => {
  if (!date) return '';
  const d = new Date(date);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
};

const differenceSummary = computed(() => {
  if (!detailRows.value.length) return { profit: 0, loss: 0 };
  let profit = 0, loss = 0;
  detailRows.value.forEach(d => {
    const diff = (d.actualQuantity ?? 0) - d.systemQuantity;
    if (diff > 0) profit += diff;
    else if (diff < 0) loss += diff;
  });
  return { profit, loss };
});

onMounted(() => {
  fetchChecks();
});
</script>

<template>
  <div class="inventory-check-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存盘点</span>
          <el-button type="primary" @click="createDialogVisible = true">发起盘点</el-button>
        </div>
      </template>

      <el-table :data="checks" border style="width: 100%" v-loading="loading">
        <el-table-column prop="serialNo" label="盘点单号" width="200" />
        <el-table-column label="盘点时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.checkTime) }}</template>
        </el-table-column>
        <el-table-column label="盘点状态" width="120">
          <template #default="scope">
            <el-tag :type="statusMap[scope.row.status]?.type">
              {{ statusMap[scope.row.status]?.label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="物资数" width="100">
          <template #default="scope">{{ scope.row.details?.length || 0 }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="openDetail(scope.row)">
              {{ scope.row.status === 'COMPLETED' ? '查看' : '盘点' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="createDialogVisible" title="发起盘点" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认发起</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" :title="`盘点明细 - ${currentCheck?.serialNo || ''}`" width="900px" top="5vh">
      <div v-if="currentCheck" class="detail-header">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="盘点单号">{{ currentCheck.serialNo }}</el-descriptions-item>
          <el-descriptions-item label="盘点时间">{{ formatDate(currentCheck.checkTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusMap[currentCheck.status]?.type" size="small">
              {{ statusMap[currentCheck.status]?.label }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="currentCheck.status !== 'PENDING' || detailRows.some(d => d.actualQuantity !== null && d.actualQuantity !== undefined)" class="summary-bar">
          <el-tag type="success" size="large">盘盈合计: +{{ differenceSummary.profit }}</el-tag>
          <el-tag type="danger" size="large">盘亏合计: {{ differenceSummary.loss }}</el-tag>
        </div>
      </div>

      <el-table :data="detailRows" border style="width: 100%" max-height="450">
        <el-table-column label="物资编码" width="120">
          <template #default="scope">{{ scope.row.material?.code }}</template>
        </el-table-column>
        <el-table-column label="物资名称" width="140">
          <template #default="scope">{{ scope.row.material?.name }}</template>
        </el-table-column>
        <el-table-column label="规格型号" width="100">
          <template #default="scope">{{ scope.row.material?.spec || '-' }}</template>
        </el-table-column>
        <el-table-column prop="systemQuantity" label="系统库存" width="100" align="center" />
        <el-table-column label="实际盘点" width="140" align="center">
          <template #default="scope">
            <el-input-number
              v-if="currentCheck?.status !== 'COMPLETED'"
              v-model="scope.row.actualQuantity"
              :min="0"
              size="small"
              controls-position="right"
            />
            <span v-else>{{ scope.row.actualQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="100" align="center">
          <template #default="scope">
            <span
              :class="{
                'diff-positive': (scope.row.actualQuantity ?? 0) - scope.row.systemQuantity > 0,
                'diff-negative': (scope.row.actualQuantity ?? 0) - scope.row.systemQuantity < 0,
                'diff-zero': (scope.row.actualQuantity ?? 0) - scope.row.systemQuantity === 0
              }"
            >
              {{ scope.row.actualQuantity !== null && scope.row.actualQuantity !== undefined
                ? ((scope.row.actualQuantity - scope.row.systemQuantity) > 0 ? '+' : '') + (scope.row.actualQuantity - scope.row.systemQuantity)
                : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="120">
          <template #default="scope">
            <el-input
              v-if="currentCheck?.status !== 'COMPLETED'"
              v-model="scope.row.remark"
              size="small"
              placeholder="选填"
            />
            <span v-else>{{ scope.row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer v-if="currentCheck?.status !== 'COMPLETED'">
        <el-button @click="detailDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交盘点结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.inventory-check-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header {
  margin-bottom: 16px;
}

.summary-bar {
  margin-top: 12px;
  display: flex;
  gap: 16px;
}

.diff-positive {
  color: #67c23a;
  font-weight: bold;
}

.diff-negative {
  color: #f56c6c;
  font-weight: bold;
}

.diff-zero {
  color: #909399;
}
</style>
