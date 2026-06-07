<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';

const warehouses = ref([]);
const dialogVisible = ref(false);
const shelfDialogVisible = ref(false);
const currentWarehouseId = ref<number | null>(null);
const shelves = ref([]);
const form = ref({
  code: '',
  name: '',
  address: '',
  manager: ''
});
const shelfForm = ref({
  code: '',
  name: '',
  capacity: 0,
  warehouse: { id: 0 }
});
const isEdit = ref(false);

const fetchWarehouses = async () => {
  const res: any = await axios.get('/warehouses');
  if (res.code === 200) {
    warehouses.value = res.data;
  }
};

const handleAdd = () => {
  isEdit.value = false;
  form.value = { code: '', name: '', address: '', manager: '' };
  dialogVisible.value = true;
};

const handleSave = async () => {
  const res: any = await axios.post('/warehouses', form.value);
  if (res.code === 200) {
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    fetchWarehouses();
  }
};

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该仓库吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res: any = await axios.delete(`/warehouses/${id}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchWarehouses();
    }
  });
};

// Shelf Logic
const handleManageShelves = async (warehouse: any) => {
  currentWarehouseId.value = warehouse.id;
  shelfDialogVisible.value = true;
  fetchShelves(warehouse.id);
};

const fetchShelves = async (warehouseId: number) => {
  const res: any = await axios.get(`/warehouses/${warehouseId}/shelves`);
  if (res.code === 200) {
    shelves.value = res.data;
  }
};

const handleAddShelf = () => {
  if (!currentWarehouseId.value) return;
  shelfForm.value = { 
    code: '', 
    name: '', 
    capacity: 100, 
    warehouse: { id: currentWarehouseId.value } 
  };
  saveShelf(); // Simplification: directly save or open another dialog? Let's use prompt or simple inline add.
  // Actually, let's just use a simple form inside the dialog above the table
};

const newShelfCode = ref('');
const newShelfName = ref('');
const newShelfCapacity = ref(100);

const handleQuickAddShelf = async () => {
  if (!currentWarehouseId.value) return;
  const payload = {
    code: newShelfCode.value,
    name: newShelfName.value,
    capacity: newShelfCapacity.value,
    warehouse: { id: currentWarehouseId.value }
  };
  const res: any = await axios.post('/warehouses/shelves', payload);
  if (res.code === 200) {
    ElMessage.success('添加货架成功');
    newShelfCode.value = '';
    newShelfName.value = '';
    fetchShelves(currentWarehouseId.value);
  }
};

const handleDeleteShelf = (id: number) => {
   ElMessageBox.confirm('确定删除该货架吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res: any = await axios.delete(`/warehouses/shelves/${id}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      if (currentWarehouseId.value) fetchShelves(currentWarehouseId.value);
    }
  });
};

onMounted(() => {
  fetchWarehouses();
});
</script>

<template>
  <div class="warehouse-container">
    <div class="header-actions">
      <el-button type="primary" @click="handleAdd">新增仓库</el-button>
    </div>

    <el-table :data="warehouses" style="width: 100%; margin-top: 20px;" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="code" label="仓库编号" />
      <el-table-column prop="name" label="仓库名称" />
      <el-table-column prop="address" label="地址" />
      <el-table-column prop="manager" label="负责人" />
      <el-table-column label="操作" width="250">
        <template #default="scope">
          <el-button size="small" @click="handleManageShelves(scope.row)">管理货架</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Warehouse Dialog -->
    <el-dialog v-model="dialogVisible" title="仓库信息" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库编号">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="仓库名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Shelf Dialog -->
    <el-dialog v-model="shelfDialogVisible" title="货架管理" width="800px">
      <div style="margin-bottom: 20px; display: flex; gap: 10px;">
        <el-input v-model="newShelfCode" placeholder="货架编号" style="width: 150px" />
        <el-input v-model="newShelfName" placeholder="货架名称" style="width: 150px" />
        <el-input-number v-model="newShelfCapacity" placeholder="容量" :min="1" />
        <el-button type="primary" @click="handleQuickAddShelf">添加货架</el-button>
      </div>
      
      <el-table :data="shelves" border height="400">
        <el-table-column prop="code" label="货架编号" />
        <el-table-column prop="name" label="货架名称" />
        <el-table-column prop="capacity" label="容量" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button size="small" type="danger" @click="handleDeleteShelf(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<style scoped>
.warehouse-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}
</style>
