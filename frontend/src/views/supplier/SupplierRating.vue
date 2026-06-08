<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { WarningFilled } from '@element-plus/icons-vue';
import { getUser } from '../../api/auth';

const suppliers = ref<any[]>([]);
const ratingDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const editDialogVisible = ref(false);
const currentSupplier = ref<any>(null);
const detailRatings = ref<any[]>([]);
const ratingForm = ref({ rating: 0, content: '' });
const editForm = ref({ id: 0, rating: 0, content: '' });
const hasRated = ref(false);
const user = getUser() || {};

const fetchSuppliers = async () => {
  const res: any = await axios.get('/supplier-ratings/suppliers');
  if (res.code === 200) {
    suppliers.value = res.data;
  }
};

const openRatingDialog = async (supplier: any) => {
  currentSupplier.value = supplier;
  ratingForm.value = { rating: 0, content: '' };
  try {
    const res: any = await axios.get(`/supplier-ratings/supplier/${supplier.id}/has-rated`);
    if (res.code === 200) {
      hasRated.value = res.data;
    }
  } catch {
    hasRated.value = false;
  }
  ratingDialogVisible.value = true;
};

const handleSubmitRating = async () => {
  if (ratingForm.value.rating === 0) {
    ElMessage.warning('请选择评分');
    return;
  }
  try {
    const res: any = await axios.post('/supplier-ratings', {
      supplierId: currentSupplier.value.id,
      rating: ratingForm.value.rating,
      content: ratingForm.value.content
    });
    if (res.code === 200) {
      ElMessage.success('评价成功');
      ratingDialogVisible.value = false;
      fetchSuppliers();
    }
  } catch (error: any) {
    const msg = error.response?.data?.message || '评价失败';
    ElMessage.error(msg);
  }
};

const openDetailDialog = async (supplier: any) => {
  currentSupplier.value = supplier;
  const res: any = await axios.get(`/supplier-ratings/supplier/${supplier.id}`);
  if (res.code === 200) {
    detailRatings.value = res.data;
  }
  detailDialogVisible.value = true;
};

const handleEditRating = (row: any) => {
  editForm.value = { id: row.id, rating: row.rating, content: row.content || '' };
  editDialogVisible.value = true;
};

const handleUpdateRating = async () => {
  if (editForm.value.rating === 0) {
    ElMessage.warning('请选择评分');
    return;
  }
  try {
    const res: any = await axios.put(`/supplier-ratings/${editForm.value.id}`, {
      rating: editForm.value.rating,
      content: editForm.value.content
    });
    if (res.code === 200) {
      ElMessage.success('修改成功');
      editDialogVisible.value = false;
      const detailRes: any = await axios.get(`/supplier-ratings/supplier/${currentSupplier.value.id}`);
      if (detailRes.code === 200) {
        detailRatings.value = detailRes.data;
      }
      fetchSuppliers();
    }
  } catch (error: any) {
    const msg = error.response?.data?.message || '修改失败';
    ElMessage.error(msg);
  }
};

const handleDeleteRating = (ratingId: number) => {
  ElMessageBox.confirm('确定删除该评价吗?', '提示', { type: 'warning' })
    .then(async () => {
      const res: any = await axios.delete(`/supplier-ratings/${ratingId}`);
      if (res.code === 200) {
        ElMessage.success('删除成功');
        const detailRes: any = await axios.get(`/supplier-ratings/supplier/${currentSupplier.value.id}`);
        if (detailRes.code === 200) {
          detailRatings.value = detailRes.data;
        }
        fetchSuppliers();
      }
    });
};

const formatTime = (time: string) => {
  if (!time) return '';
  return time.replace('T', ' ').substring(0, 19);
};

onMounted(() => {
  fetchSuppliers();
});
</script>

<template>
  <div class="supplier-rating-container">
    <div class="page-header">
      <h2>供应商评价</h2>
    </div>

    <el-table :data="suppliers" border style="width: 100%; margin-top: 20px;">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="供应商名称" width="180" />
      <el-table-column prop="contactPerson" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column label="平均评分" width="200">
        <template #default="scope">
          <div style="display: flex; align-items: center; gap: 8px;">
            <el-rate
              :model-value="scope.row.averageRating"
              disabled
              show-score
              text-color="#ff9900"
              score-template="{value}"
            />
            <span style="color: #909399; font-size: 12px;">({{ scope.row.ratingCount }}条评价)</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="最新评价摘要" min-width="260">
        <template #default="scope">
          <div v-if="scope.row.latestRatings && scope.row.latestRatings.length > 0">
            <div v-for="(r, idx) in scope.row.latestRatings" :key="idx" class="rating-summary-item">
              <el-rate :model-value="r.rating" disabled :size="12" style="display: inline-flex;" />
              <span class="rating-summary-text">{{ r.content ? (r.content.length > 20 ? r.content.substring(0, 20) + '...' : r.content) : '无评价内容' }}</span>
            </div>
          </div>
          <span v-else style="color: #c0c4cc;">暂无评价</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openRatingDialog(scope.row)">评价</el-button>
          <el-button size="small" @click="openDetailDialog(scope.row)">查看评价</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="ratingDialogVisible" title="供应商评价" width="500px">
      <div v-if="currentSupplier" style="margin-bottom: 16px;">
        <strong>供应商：</strong>{{ currentSupplier.name }}
      </div>
      <div v-if="hasRated" style="text-align: center; padding: 20px 0;">
        <el-icon style="font-size: 40px; color: #E6A23C;"><WarningFilled /></el-icon>
        <p style="margin-top: 12px; color: #909399;">您已评价过该供应商，不能重复评价</p>
      </div>
      <el-form v-else :model="ratingForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="ratingForm.rating" :texts="['很差', '较差', '一般', '较好', '很好']" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="ratingForm.content" type="textarea" :rows="4" placeholder="请输入评价内容（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ratingDialogVisible = false">关闭</el-button>
        <el-button v-if="!hasRated" type="primary" @click="handleSubmitRating">提交评价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="评价详情" width="700px">
      <div v-if="currentSupplier" style="margin-bottom: 16px;">
        <strong>供应商：</strong>{{ currentSupplier.name }}
        <span style="margin-left: 16px;">
          <strong>平均评分：</strong>
          <el-rate :model-value="currentSupplier.averageRating" disabled show-score text-color="#ff9900" />
        </span>
      </div>
      <el-table :data="detailRatings" border max-height="400">
        <el-table-column label="评分" width="180">
          <template #default="scope">
            <el-rate :model-value="scope.row.rating" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="200">
          <template #default="scope">
            {{ scope.row.content || '无评价内容' }}
          </template>
        </el-table-column>
        <el-table-column prop="username" label="评价人" width="100" />
        <el-table-column label="评价时间" width="170">
          <template #default="scope">
            {{ formatTime(scope.row.ratingTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button
              v-if="scope.row.userId === user.id"
              size="small"
              type="primary"
              @click="handleEditRating(scope.row)"
            >编辑</el-button>
            <el-button
              v-if="scope.row.userId === user.id"
              size="small"
              type="danger"
              @click="handleDeleteRating(scope.row.id)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑评价" width="500px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="editForm.rating" :texts="['很差', '较差', '一般', '较好', '很好']" show-text />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="editForm.content" type="textarea" :rows="4" placeholder="请输入评价内容（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateRating">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.supplier-rating-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.rating-summary-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 2px;
}

.rating-summary-text {
  font-size: 12px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}
</style>
