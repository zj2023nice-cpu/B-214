<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';

const warehouses = ref([]);
const dialogVisible = ref(false);
const shelfDialogVisible = ref(false);
const currentWarehouseId = ref<number | null>(null);
const shelves = ref([]);
const warehouseTableRef = ref();
const selectedWarehouses = ref<any[]>([]);
const batchDeleting = ref(false);
const deleteResultVisible = ref(false);
const deleteResult = ref<any>(null);
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

const handleSelectionChange = (selection: any[]) => {
  selectedWarehouses.value = selection;
};

const showBatchDeleteResult = (result: any) => {
  deleteResult.value = result;
  deleteResultVisible.value = true;
  if (result.allSucceeded) {
    ElMessage.success(result.summaryMessage || `删除成功，共删除 ${result.successCount} 条记录`);
  } else {
    ElMessage.warning(result.summaryMessage || '批量删除未执行，请查看详情');
  }
};

const handleBatchDelete = () => {
  if (selectedWarehouses.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录');
    return;
  }
  ElMessageBox.confirm(`确定删除选中的 ${selectedWarehouses.value.length} 条记录吗？`, '批量删除', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    batchDeleting.value = true;
    try {
      const ids = selectedWarehouses.value.map((item: any) => item.id);
      const res: any = await axios.delete('/warehouses/batch', { data: ids });
      if (res.code === 200) {
        const result = res.data;
        showBatchDeleteResult(result);
        if (result.allSucceeded) {
          selectedWarehouses.value = [];
          warehouseTableRef.value?.clearSelection();
          fetchWarehouses();
        }
      }
    } catch {
      ElMessage.error('批量删除失败');
    } finally {
      batchDeleting.value = false;
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

const getLoadRate = (shelf: any) => {
  if (!shelf.capacity || shelf.capacity === 0) return 0;
  return Math.round((shelf.currentLoad || 0) / shelf.capacity * 100);
};

const isOverloaded = (shelf: any) => {
  return getLoadRate(shelf) > 90;
};

const getWarningText = (shelf: any) => {
  const rate = getLoadRate(shelf);
  if (rate > 90) return '容量预警';
  return '';
};

onMounted(() => {
  fetchWarehouses();
});
</script>

<template>
  <div class="warehouse-container">
    <div class="header-actions">
      <el-button type="primary" @click="handleAdd">新增仓库</el-button>
      <el-button type="danger" :disabled="selectedWarehouses.length === 0" :loading="batchDeleting" @click="handleBatchDelete">
        批量删除{{ selectedWarehouses.length > 0 ? `(${selectedWarehouses.length})` : '' }}
      </el-button>
    </div>

    <el-table ref="warehouseTableRef" :data="warehouses" style="width: 100%; margin-top: 20px;" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
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

    <el-dialog v-model="deleteResultVisible" title="批量删除结果" width="720px">
      <div v-if="deleteResult">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="总计">{{ deleteResult.totalCount }}</el-descriptions-item>
          <el-descriptions-item :label="deleteResult.allSucceeded ? '成功' : '校验通过'">
            <span style="color: #67c23a; font-weight: bold;">{{ deleteResult.successCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <span :style="deleteResult.failureCount > 0 ? 'color: #f56c6c; font-weight: bold;' : ''">{{ deleteResult.failureCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="执行结果">
            <span :style="deleteResult.allSucceeded ? 'color: #67c23a; font-weight: bold;' : 'color: #e6a23c; font-weight: bold;'">
              {{ deleteResult.allSucceeded ? '已删除' : '整批未执行' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 12px; color: #606266;">{{ deleteResult.summaryMessage }}</div>
        <el-table :data="deleteResult.items || []" border max-height="320" style="width: 100%; margin-top: 16px;">
          <el-table-column prop="id" label="仓库ID" width="100" />
          <el-table-column label="结果" width="110">
            <template #default="scope">
              <el-tag :type="scope.row.success ? 'success' : 'danger'">
                {{ scope.row.success ? '通过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="320" />
        </el-table>
      </div>
      <template #footer>
        <el-button type="primary" @click="deleteResultVisible = false">确定</el-button>
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
        <el-table-column label="承载量">
          <template #default="scope">
            {{ scope.row.currentLoad || 0 }} / {{ scope.row.capacity || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="使用率" width="180">
          <template #default="scope">
            <div :style="{ color: isOverloaded(scope.row) ? '#F56C6C' : '', fontWeight: isOverloaded(scope.row) ? 'bold' : 'normal' }">
              <el-progress
                :percentage="getLoadRate(scope.row)"
                :color="isOverloaded(scope.row) ? '#F56C6C' : '#409EFF'"
                :stroke-width="16"
                :text-inside="true"
                style="width: 150px"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预警" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="isOverloaded(scope.row)" type="warning" size="small">{{ getWarningText(scope.row) }}</el-tag>
          </template>
        </el-table-column>
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
