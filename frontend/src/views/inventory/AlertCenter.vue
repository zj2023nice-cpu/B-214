<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from '../../api/axios';
import { Warning, ShoppingTrolley } from '@element-plus/icons-vue';

const router = useRouter();
const alertMaterials = ref<any[]>([]);
const categories = ref<any[]>([]);
const filterCategoryId = ref<number | null>(null);
const loading = ref(false);
const restockDialogVisible = ref(false);
const restockSuggestion = ref<any[]>([]);

const fetchAlertMaterials = async () => {
  loading.value = true;
  try {
    const params: any = {};
    if (filterCategoryId.value != null) params.categoryId = filterCategoryId.value;
    const res: any = await axios.get('/materials/alert-center', { params });
    if (res.code === 200) {
      alertMaterials.value = res.data;
    }
  } finally {
    loading.value = false;
  }
};

const fetchCategories = async () => {
  const res: any = await axios.get('/categories');
  if (res.code === 200) categories.value = res.data;
};

const getUrgencyLevel = (item: any) => {
  const gap = item.alertThreshold - item.stockQuantity;
  if (gap >= item.alertThreshold) return 'critical';
  if (gap >= item.alertThreshold * 0.5) return 'high';
  return 'medium';
};

const getUrgencyTagType = (level: string) => {
  if (level === 'critical') return 'danger';
  if (level === 'high') return 'warning';
  return 'info';
};

const getUrgencyLabel = (level: string) => {
  if (level === 'critical') return '紧急';
  if (level === 'high') return '高';
  return '中等';
};

const getSuggestedQuantity = (item: any) => {
  return Math.max(0, item.alertThreshold * 2 - item.stockQuantity);
};

const handleQuickInbound = (item: any) => {
  router.push({
    path: '/inventory/inbound',
    query: {
      materialId: String(item.id),
      materialCode: item.code,
      materialName: item.name,
      suggestedQty: String(getSuggestedQuantity(item)),
      price: String(item.price || 0),
      supplierId: item.supplier?.id ? String(item.supplier.id) : ''
    }
  });
};

const handleGenerateRestockSuggestions = () => {
  restockSuggestion.value = alertMaterials.value.map(item => ({
    ...item,
    suggestedQuantity: getSuggestedQuantity(item),
    urgencyLevel: getUrgencyLevel(item)
  }));
  restockDialogVisible.value = true;
};

const handleFilterChange = () => {
  fetchAlertMaterials();
};

const alertCount = computed(() => alertMaterials.value.length);

const criticalCount = computed(() =>
  alertMaterials.value.filter(m => getUrgencyLevel(m) === 'critical').length
);

const highCount = computed(() =>
  alertMaterials.value.filter(m => getUrgencyLevel(m) === 'high').length
);

onMounted(() => {
  fetchAlertMaterials();
  fetchCategories();
});
</script>

<template>
  <div class="alert-center-container">
    <div class="summary-cards">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-card shadow="hover" class="summary-card total-card">
            <div class="summary-content">
              <div class="summary-icon" style="background-color: #F56C6C;">
                <el-icon :size="28"><Warning /></el-icon>
              </div>
              <div class="summary-info">
                <div class="summary-title">预警物资总数</div>
                <div class="summary-value">{{ alertCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="summary-card critical-card">
            <div class="summary-content">
              <div class="summary-icon" style="background-color: #E6A23C;">
                <el-icon :size="28"><Warning /></el-icon>
              </div>
              <div class="summary-info">
                <div class="summary-title">紧急程度：紧急</div>
                <div class="summary-value">{{ criticalCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="summary-card high-card">
            <div class="summary-content">
              <div class="summary-icon" style="background-color: #E6A23C;">
                <el-icon :size="28"><Warning /></el-icon>
              </div>
              <div class="summary-info">
                <div class="summary-title">紧急程度：高</div>
                <div class="summary-value">{{ highCount }}</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="filterCategoryId"
        placeholder="按分类筛选"
        clearable
        style="width: 200px;"
        @change="handleFilterChange"
      >
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <div style="flex: 1;" />
      <el-button type="warning" @click="handleGenerateRestockSuggestions">
        <el-icon><ShoppingTrolley /></el-icon>
        生成补货建议
      </el-button>
    </div>

    <el-table
      :data="alertMaterials"
      v-loading="loading"
      style="width: 100%; margin-top: 16px;"
      border
      :row-class-name="({ row }: any) => {
        const level = getUrgencyLevel(row);
        if (level === 'critical') return 'critical-row';
        if (level === 'high') return 'high-row';
        return 'medium-row';
      }"
    >
      <el-table-column prop="code" label="物资编号" width="120" />
      <el-table-column prop="name" label="物资名称" width="150" />
      <el-table-column prop="spec" label="规格型号" width="120" />
      <el-table-column label="分类" width="120">
        <template #default="scope">
          {{ scope.row.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="stockQuantity" label="当前库存" width="100" align="center">
        <template #default="scope">
          <span style="color: #F56C6C; font-weight: bold;">{{ scope.row.stockQuantity }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="alertThreshold" label="预警阈值" width="100" align="center" />
      <el-table-column label="差距" width="100" align="center">
        <template #default="scope">
          <span style="color: #E6A23C; font-weight: bold;">
            {{ scope.row.alertThreshold - scope.row.stockQuantity }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="紧急程度" width="100" align="center">
        <template #default="scope">
          <el-tag :type="getUrgencyTagType(getUrgencyLevel(scope.row))" size="small">
            {{ getUrgencyLabel(getUrgencyLevel(scope.row)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="建议补货量" width="120" align="center">
        <template #default="scope">
          <span style="color: #67C23A; font-weight: bold;">{{ getSuggestedQuantity(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" align="center">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleQuickInbound(scope.row)">
            快速入库
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && alertMaterials.length === 0" description="暂无库存预警物资" />

    <el-dialog v-model="restockDialogVisible" title="补货建议" width="700px">
      <el-table :data="restockSuggestion" border max-height="500">
        <el-table-column prop="code" label="物资编号" width="110" />
        <el-table-column prop="name" label="物资名称" width="130" />
        <el-table-column label="当前库存" width="90" align="center">
          <template #default="scope">
            <span style="color: #F56C6C;">{{ scope.row.stockQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="alertThreshold" label="预警阈值" width="90" align="center" />
        <el-table-column label="紧急程度" width="90" align="center">
          <template #default="scope">
            <el-tag :type="getUrgencyTagType(scope.row.urgencyLevel)" size="small">
              {{ getUrgencyLabel(scope.row.urgencyLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建议补货量" width="100" align="center">
          <template #default="scope">
            <span style="color: #67C23A; font-weight: bold;">{{ scope.row.suggestedQuantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleQuickInbound(scope.row)">
              入库
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="restockDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.alert-center-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}

.summary-cards {
  margin-bottom: 20px;
}

.summary-card {
  margin-bottom: 0;
}

.summary-content {
  display: flex;
  align-items: center;
}

.summary-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 16px;
  color: white;
}

.summary-info {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.filter-bar {
  display: flex;
  align-items: center;
}

:deep(.critical-row) {
  --el-table-tr-bg-color: #fef0f0;
}

:deep(.critical-row td.el-table__cell) {
  color: #f56c6c !important;
}

:deep(.high-row) {
  --el-table-tr-bg-color: #fdf6ec;
}

:deep(.high-row td.el-table__cell) {
  color: #e6a23c !important;
}

:deep(.medium-row) {
  --el-table-tr-bg-color: #f4f4f5;
}

:deep(.medium-row td.el-table__cell) {
  color: #909399 !important;
}
</style>
