<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';

const warehouses = ref([]);
const materials = ref([]);
const transfers = ref([]);

const form = ref({
  sourceWarehouse: { id: null as number | null },
  targetWarehouse: { id: null as number | null },
  material: { id: null as number | null },
  quantity: 1,
  remark: ''
});

const fetchWarehouses = async () => {
  const res: any = await axios.get('/warehouses');
  if (res.code === 200) warehouses.value = res.data;
};

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials');
  if (res.code === 200) materials.value = res.data;
};

const fetchTransfers = async () => {
  const res: any = await axios.get('/transfers');
  if (res.code === 200) transfers.value = res.data;
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
};

const statusTagType = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success';
    case 'PENDING': return 'warning';
    case 'CANCELLED': return 'info';
    default: return 'info';
  }
};

const statusLabel = (status: string) => {
  switch (status) {
    case 'COMPLETED': return '已完成';
    case 'PENDING': return '待处理';
    case 'CANCELLED': return '已取消';
    default: return status;
  }
};

const handleSubmit = async () => {
  if (!form.value.sourceWarehouse.id) {
    ElMessage.error('请选择源仓库');
    return;
  }
  if (!form.value.targetWarehouse.id) {
    ElMessage.error('请选择目标仓库');
    return;
  }
  if (form.value.sourceWarehouse.id === form.value.targetWarehouse.id) {
    ElMessage.error('源仓库和目标仓库不能相同');
    return;
  }
  if (!form.value.material.id) {
    ElMessage.error('请选择物资');
    return;
  }
  if (form.value.quantity < 1) {
    ElMessage.error('调拨数量必须大于0');
    return;
  }

  const res: any = await axios.post('/transfers', form.value);
  if (res.code === 200) {
    ElMessage.success('调拨成功');
    form.value = {
      sourceWarehouse: { id: null },
      targetWarehouse: { id: null },
      material: { id: null },
      quantity: 1,
      remark: ''
    };
    fetchTransfers();
  }
};

const handleCancel = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认取消该调拨记录？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res: any = await axios.put(`/transfers/${id}/cancel`);
    if (res.code === 200) {
      ElMessage.success('取消成功');
      fetchTransfers();
    }
  } catch {}
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除该调拨记录？此操作不可恢复。', '警告', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res: any = await axios.delete(`/transfers/${id}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchTransfers();
    }
  } catch {}
};

onMounted(() => {
  fetchWarehouses();
  fetchMaterials();
  fetchTransfers();
});
</script>

<template>
  <div class="transfer-container">
    <el-card header="物资调拨">
      <el-form :model="form" label-width="120px" style="max-width: 600px;">
        <el-form-item label="源仓库">
          <el-select v-model="form.sourceWarehouse.id" filterable placeholder="请选择源仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} - ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标仓库">
          <el-select v-model="form.targetWarehouse.id" filterable placeholder="请选择目标仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} - ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择物资">
          <el-select v-model="form.material.id" filterable placeholder="请选择物资" style="width: 100%">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.code} - ${m.name} (库存: ${m.stockQuantity})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨数量">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">确认调拨</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card header="调拨记录" style="margin-top: 20px;">
      <el-table :data="transfers" border style="width: 100%">
        <el-table-column prop="serialNo" label="调拨单号" width="180" />
        <el-table-column label="源仓库" width="150">
          <template #default="scope">{{ scope.row.sourceWarehouse?.name }}</template>
        </el-table-column>
        <el-table-column label="目标仓库" width="150">
          <template #default="scope">{{ scope.row.targetWarehouse?.name }}</template>
        </el-table-column>
        <el-table-column label="物资名称">
          <template #default="scope">{{ scope.row.material?.name }} ({{ scope.row.material?.code }})</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column label="调拨时间" width="180">
          <template #default="scope">{{ formatDate(scope.row.transferTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status !== 'CANCELLED'"
              type="warning"
              size="small"
              @click="handleCancel(scope.row.id)"
            >取消</el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row.id)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.transfer-container {
  padding: 20px;
}
</style>
