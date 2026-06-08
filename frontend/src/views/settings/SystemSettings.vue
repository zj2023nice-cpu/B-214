<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';

const router = useRouter();
const activeTab = ref('category');
const categories = ref([]);
const suppliers = ref([]);

const catDialogVisible = ref(false);
const supDialogVisible = ref(false);

const catForm = ref({ name: '', description: '' });
const supForm = ref({ name: '', contactPerson: '', phone: '', address: '' });

const fetchCategories = async () => {
  const res: any = await axios.get('/categories');
  if (res.code === 200) categories.value = res.data;
};

const fetchSuppliers = async () => {
  const res: any = await axios.get('/supplier-ratings/suppliers');
  if (res.code === 200) suppliers.value = res.data;
};

const handleAddCat = () => {
  catForm.value = { name: '', description: '' };
  catDialogVisible.value = true;
};

const handleSaveCat = async () => {
  const res: any = await axios.post('/categories', catForm.value);
  if (res.code === 200) {
    ElMessage.success('保存成功');
    catDialogVisible.value = false;
    fetchCategories();
  }
};

const handleDeleteCat = (id: number) => {
   ElMessageBox.confirm('确定删除该分类吗?', '提示', { type: 'warning' })
    .then(async () => {
      const res: any = await axios.delete(`/categories/${id}`);
      if (res.code === 200) {
        ElMessage.success('删除成功');
        fetchCategories();
      }
    });
};

const handleAddSup = () => {
  supForm.value = { name: '', contactPerson: '', phone: '', address: '' };
  supDialogVisible.value = true;
};

const handleSaveSup = async () => {
  const res: any = await axios.post('/suppliers', supForm.value);
  if (res.code === 200) {
    ElMessage.success('保存成功');
    supDialogVisible.value = false;
    fetchSuppliers();
  }
};

const handleDeleteSup = (id: number) => {
   ElMessageBox.confirm('确定删除该供应商吗?', '提示', { type: 'warning' })
    .then(async () => {
      const res: any = await axios.delete(`/suppliers/${id}`);
      if (res.code === 200) {
        ElMessage.success('删除成功');
        fetchSuppliers();
      }
    });
};

onMounted(() => {
  fetchCategories();
  fetchSuppliers();
});
</script>

<template>
  <div class="settings-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="物资分类管理" name="category">
        <el-button type="primary" @click="handleAddCat" style="margin-bottom: 20px;">新增分类</el-button>
        <el-table :data="categories" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="分类名称" />
          <el-table-column prop="description" label="描述" />
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button size="small" type="danger" @click="handleDeleteCat(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      
      <el-tab-pane label="供应商管理" name="supplier">
        <el-button type="primary" @click="handleAddSup" style="margin-bottom: 20px;">新增供应商</el-button>
        <el-button @click="router.push('/supplier-rating')" style="margin-bottom: 20px;">供应商评价</el-button>
        <el-table :data="suppliers" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="供应商名称" width="160" />
          <el-table-column prop="contactPerson" label="联系人" width="90" />
          <el-table-column prop="phone" label="电话" width="130" />
          <el-table-column label="平均评分" width="180">
            <template #default="scope">
              <div style="display: flex; align-items: center; gap: 6px;">
                <el-rate :model-value="scope.row.averageRating" disabled show-score text-color="#ff9900" score-template="{value}" />
                <span style="color: #909399; font-size: 12px;">({{ scope.row.ratingCount }}条)</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="最新评价摘要" min-width="200">
            <template #default="scope">
              <div v-if="scope.row.latestRatings && scope.row.latestRatings.length > 0">
                <div v-for="(r, idx) in scope.row.latestRatings" :key="idx" class="rating-summary-item">
                  <el-rate :model-value="r.rating" disabled :size="12" style="display: inline-flex;" />
                  <span class="rating-summary-text">{{ r.content ? (r.content.length > 15 ? r.content.substring(0, 15) + '...' : r.content) : '无内容' }}</span>
                </div>
              </div>
              <span v-else style="color: #c0c4cc;">暂无评价</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button size="small" type="danger" @click="handleDeleteSup(scope.row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- Category Dialog -->
    <el-dialog v-model="catDialogVisible" title="分类信息" width="400px">
      <el-form :model="catForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="catForm.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="catForm.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveCat">确定</el-button>
      </template>
    </el-dialog>

    <!-- Supplier Dialog -->
    <el-dialog v-model="supDialogVisible" title="供应商信息" width="500px">
      <el-form :model="supForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="supForm.name" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="supForm.contactPerson" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="supForm.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="supForm.address" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveSup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.settings-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}

.rating-summary-item {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
}

.rating-summary-text {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 120px;
}
</style>
