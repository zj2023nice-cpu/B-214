<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage } from 'element-plus';

const materials = ref([]);
const suppliers = ref([]);
const warehouses = ref([]);
const form = ref({
  serialNo: '',
  warehouse: { id: null as number | null },
  material: { id: null as number | null },
  quantity: 1,
  price: 0,
  supplier: { id: null as number | null },
  remark: ''
});

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials/all');
  if (res.code === 200) materials.value = res.data;
};

const fetchSuppliers = async () => {
  const res: any = await axios.get('/suppliers');
  if (res.code === 200) suppliers.value = res.data;
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
  form.value.serialNo = `IN-${timestamp}`;
};

const handleSubmit = async () => {
  if (!form.value.material.id) {
    ElMessage.error('请选择物资');
    return;
  }
  
  const payload = { ...form.value };
  if (!payload.supplier.id) payload.supplier = null;
  if (!payload.warehouse.id) payload.warehouse = null;
  
  const res: any = await axios.post('/inventory/inbound', payload);
  if (res.code === 200) {
    ElMessage.success('入库成功');
    form.value = {
      serialNo: '',
      warehouse: { id: null },
      material: { id: null },
      quantity: 1,
      price: 0,
      supplier: { id: null },
      remark: ''
    };
    generateSerialNo();
  }
};

const handleMaterialChange = (val: number) => {
  const material: any = materials.value.find((m: any) => m.id === val);
  if (material) {
    form.value.price = material.price || 0;
    if (material.supplier) {
        form.value.supplier.id = material.supplier.id;
    }
  }
};

onMounted(() => {
  fetchMaterials();
  fetchSuppliers();
  fetchWarehouses();
  generateSerialNo();
});
</script>

<template>
  <div class="inbound-container">
    <el-card header="物资入库">
      <el-form :model="form" label-width="120px" style="max-width: 600px;">
        <el-form-item label="入库单号">
          <el-input v-model="form.serialNo" disabled>
             <template #append>
               <el-button @click="generateSerialNo">刷新</el-button>
             </template>
          </el-input>
        </el-form-item>
        <el-form-item label="入库仓库">
          <el-select v-model="form.warehouse.id" filterable placeholder="请选择入库仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.code} - ${w.name}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择物资">
          <el-select v-model="form.material.id" filterable placeholder="请选择物资" style="width: 100%" @change="handleMaterialChange">
            <el-option v-for="item in materials" :key="item.id" :label="`${item.code} - ${item.name}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库单价">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplier.id" placeholder="请选择供应商" style="width: 100%">
            <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">确认入库</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.inbound-container {
  padding: 20px;
}
</style>
