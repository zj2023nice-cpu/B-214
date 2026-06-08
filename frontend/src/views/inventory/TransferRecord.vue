<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import dayjs from 'dayjs';

const warehouses = ref([]);
const materials = ref([]);
const transfers = ref([]);
const sourceInventory = ref<any[]>([]);

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
  const res: any = await axios.get('/materials/all');
  if (res.code === 200) materials.value = res.data;
};

const fetchTransfers = async () => {
  const res: any = await axios.get('/transfers');
  if (res.code === 200) transfers.value = res.data;
};

const fetchSourceInventory = async (warehouseId: number) => {
  if (!warehouseId) {
    sourceInventory.value = [];
    return;
  }
  const res: any = await axios.get(`/transfers/warehouse-inventory/${warehouseId}`);
  if (res.code === 200) {
    sourceInventory.value = res.data;
  }
};

const filteredMaterials = ref<any[]>([]);

const updateFilteredMaterials = () => {
  if (form.value.sourceWarehouse.id && sourceInventory.value.length > 0) {
    const invMaterialIds = sourceInventory.value.map((wi: any) => wi.material?.id).filter(Boolean);
    filteredMaterials.value = materials.value.filter((m: any) => invMaterialIds.includes(m.id));
  } else {
    filteredMaterials.value = materials.value;
  }
};

const getMaterialStockInSource = (materialId: number | null) => {
  if (!materialId || !form.value.sourceWarehouse.id) return 0;
  const inv = sourceInventory.value.find((wi: any) => wi.material?.id === materialId);
  return inv?.quantity || 0;
};

watch(() => form.value.sourceWarehouse.id, (newVal) => {
  form.value.material.id = null;
  if (newVal) {
    fetchSourceInventory(newVal);
  } else {
    sourceInventory.value = [];
    updateFilteredMaterials();
  }
});

watch(sourceInventory, () => {
  updateFilteredMaterials();
});

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
    ElMessage.success('调拨申请已创建，状态为待处理');
    form.value = {
      sourceWarehouse: { id: null },
      targetWarehouse: { id: null },
      material: { id: null },
      quantity: 1,
      remark: ''
    };
    sourceInventory.value = [];
    filteredMaterials.value = [];
    fetchTransfers();
  }
};

const handleConfirm = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认执行该调拨？确认后将执行库存变动。', '确认调拨', {
      confirmButtonText: '确认执行',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res: any = await axios.put(`/transfers/${id}/confirm`);
    if (res.code === 200) {
      ElMessage.success('调拨已完成，库存已变动');
      fetchTransfers();
    }
  } catch {}
};

const handleCancel = async (id: number, status: string) => {
  const msg = status === 'COMPLETED'
    ? '确认取消该调拨记录？取消后库存将回退至原仓库。'
    : '确认取消该待处理的调拨记录？';
  try {
    await ElMessageBox.confirm(msg, '提示', {
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

const handleDelete = async (id: number, status: string) => {
  const msg = status === 'COMPLETED'
    ? '确认删除该调拨记录？删除后库存将回退至原仓库，此操作不可恢复。'
    : '确认删除该调拨记录？此操作不可恢复。';
  try {
    await ElMessageBox.confirm(msg, '警告', {
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
      <el-alert
        title="调拨流程：创建调拨单（待处理） → 确认执行（库存变动） → 完成"
        type="info"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      />
      <el-form :model="form" label-width="120px" style="max-width: 600px;">
        <el-form-item label="源仓库">
          <el-select v-model="form.sourceWarehouse.id" filterable placeholder="请选择源仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} - ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标仓库">
          <el-select v-model="form.targetWarehouse.id" filterable placeholder="请选择目标仓库" style="width: 100%">
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="`${w.code} - ${w.name}`"
              :value="w.id"
              :disabled="w.id === form.sourceWarehouse.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择物资">
          <el-select v-model="form.material.id" filterable placeholder="请选择物资" style="width: 100%">
            <el-option
              v-for="m in filteredMaterials"
              :key="m.id"
              :label="form.sourceWarehouse.id
                ? `${m.code} - ${m.name} (仓库库存: ${getMaterialStockInSource(m.id)})`
                : `${m.code} - ${m.name} (总库存: ${m.stockQuantity})`"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调拨数量">
          <el-input-number
            v-model="form.quantity"
            :min="1"
            :max="getMaterialStockInSource(form.material.id) || 9999"
            style="width: 100%"
          />
          <div v-if="form.sourceWarehouse.id && form.material.id" style="font-size: 12px; color: #999; margin-left: 10px;">
            源仓库库存: {{ getMaterialStockInSource(form.material.id) }}
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">提交调拨申请</el-button>
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="success"
              size="small"
              @click="handleConfirm(scope.row.id)"
            >确认执行</el-button>
            <el-button
              v-if="scope.row.status !== 'CANCELLED'"
              type="warning"
              size="small"
              @click="handleCancel(scope.row.id, scope.row.status)"
            >取消</el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(scope.row.id, scope.row.status)"
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
