<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import axios from '../../api/axios';
import { ElMessage } from 'element-plus';

const materials = ref([]);
const warehouses = ref([]);
const form = ref({
  serialNo: '',
  warehouse: { id: null as number | null },
  material: { id: null as number | null },
  quantity: 1,
  department: '',
  remark: ''
});
const currentStock = ref(0);

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials');
  if (res.code === 200) materials.value = res.data;
};

const fetchWarehouses = async () => {
  const res: any = await axios.get('/warehouses');
  if (res.code === 200) warehouses.value = res.data;
};

const generateSerialNo = () => {
  const now = new Date();
  const timestamp = now.getFullYear().toString() +
    (now.getMonth() + 1).toString().padStart(2, '0') +
    now.getDate().toString().padStart(2, '0') +
    now.getHours().toString().padStart(2, '0') +
    now.getMinutes().toString().padStart(2, '0') +
    now.getSeconds().toString().padStart(2, '0');
  form.value.serialNo = `OUT-${timestamp}`;
};

const warehouseInventory = ref<any[]>([]);

const fetchWarehouseInventory = async (warehouseId: number) => {
  if (!warehouseId) {
    warehouseInventory.value = [];
    currentStock.value = 0;
    return;
  }
  const res: any = await axios.get(`/transfers/warehouse-inventory/${warehouseId}`);
  if (res.code === 200) {
    warehouseInventory.value = res.data;
    updateCurrentStock();
  }
};

const updateCurrentStock = () => {
  if (form.value.warehouse.id && form.value.material.id) {
    const inv = warehouseInventory.value.find(
      (wi: any) => wi.material?.id === form.value.material.id
    );
    currentStock.value = inv?.quantity || 0;
  } else {
    const material: any = materials.value.find((m: any) => m.id === form.value.material.id);
    currentStock.value = material?.stockQuantity || 0;
  }
};

watch(() => form.value.warehouse.id, (newVal) => {
  if (newVal) {
    fetchWarehouseInventory(newVal);
  } else {
    warehouseInventory.value = [];
    updateCurrentStock();
  }
});

watch(() => form.value.material.id, () => {
  updateCurrentStock();
});

const handleSubmit = async () => {
  if (!form.value.material.id) {
    ElMessage.error('请选择物资');
    return;
  }
  if (form.value.quantity > currentStock.value) {
    ElMessage.error(`库存不足，当前库存: ${currentStock.value}`);
    return;
  }
  
  const payload = { ...form.value };
  if (!payload.warehouse.id) payload.warehouse = null;

  const res: any = await axios.post('/inventory/outbound', payload);
  if (res.code === 200) {
    ElMessage.success('出库成功');
    form.value = {
      serialNo: '',
      warehouse: { id: null },
      material: { id: null },
      quantity: 1,
      department: '',
      remark: ''
    };
    generateSerialNo();
    currentStock.value = 0;
    fetchMaterials();
  }
};

onMounted(() => {
  fetchMaterials();
  fetchWarehouses();
  generateSerialNo();
});
</script>

<template>
  <div class="outbound-container">
    <el-card header="物资出库">
      <el-form :model="form" label-width="120px" style="max-width: 600px;">
        <el-form-item label="出库单号">
          <el-input v-model="form.serialNo" disabled>
             <template #append>
               <el-button @click="generateSerialNo">刷新</el-button>
             </template>
          </el-input>
        </el-form-item>
        <el-form-item label="出库仓库">
          <el-select v-model="form.warehouse.id" filterable placeholder="请选择出库仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} - ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择物资">
          <el-select v-model="form.material.id" filterable placeholder="请选择物资" style="width: 100%">
            <el-option v-for="item in materials" :key="item.id" :label="`${item.code} - ${item.name} (库存: ${item.stockQuantity})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量">
          <el-input-number v-model="form.quantity" :min="1" :max="currentStock > 0 ? currentStock : 1" style="width: 100%" />
          <div style="font-size: 12px; color: #999; margin-left: 10px;">当前库存: {{ currentStock }}</div>
        </el-form-item>
        <el-form-item label="领用部门">
          <el-input v-model="form.department" placeholder="请输入领用部门" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">确认出库</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.outbound-container {
  padding: 20px;
}
</style>
