<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue';
import axios from '../api/axios';
import * as echarts from 'echarts';
import { Box, Warning, Money, House } from '@element-plus/icons-vue';

interface Warehouse {
  id: number;
  code: string;
  name: string;
  address: string;
  manager: string;
}

interface TrendItem {
  date: string;
  inboundQuantity: number;
  outboundQuantity: number;
}

const warehouses = ref<Warehouse[]>([]);
const materials = ref([]);
const pieChartRef = ref(null);
const lineChartRef = ref(null);
let pieChart: any = null;
let lineChart: any = null;

const trendLoading = ref(false);
const trendEmpty = ref(false);
const trendData = ref<TrendItem[]>([]);

const fetchData = async () => {
  try {
    const [wRes, mRes]: any = await Promise.all([
      axios.get('/warehouses'),
      axios.get('/materials')
    ]);
    
    if (wRes.code === 200) warehouses.value = wRes.data;
    if (mRes.code === 200) {
      materials.value = mRes.data;
      nextTick(() => initPieChart());
    }
  } catch (error) {
    console.error(error);
  }
};

const fetchTrendData = async (days: number = 7) => {
  trendLoading.value = true;
  trendEmpty.value = false;
  try {
    const res: any = await axios.get('/inventory/statistics/trend', { params: { days } });
    if (res.code === 200) {
      trendData.value = res.data;
      const hasData = trendData.value.some(
        (item) => item.inboundQuantity > 0 || item.outboundQuantity > 0
      );
      trendEmpty.value = !hasData;
      nextTick(() => initLineChart());
    }
  } catch (error) {
    console.error(error);
    trendEmpty.value = true;
  } finally {
    trendLoading.value = false;
  }
};

const stats = computed(() => {
  const totalStock = materials.value.reduce((sum, item: any) => sum + item.stockQuantity, 0);
  const totalValue = materials.value.reduce((sum, item: any) => sum + (item.stockQuantity * item.price), 0);
  const lowStock = materials.value.filter((item: any) => item.stockQuantity < item.alertThreshold).length;
  
  return [
    { title: '仓库总数', value: warehouses.value.length, icon: House, color: '#409EFF' },
    { title: '物资种类', value: materials.value.length, icon: Box, color: '#67C23A' },
    { title: '库存预警', value: lowStock, icon: Warning, color: '#F56C6C' },
    { title: '库存总值', value: `¥${totalValue.toLocaleString()}`, icon: Money, color: '#E6A23C' },
  ];
});

const initPieChart = () => {
  if (!pieChartRef.value) return;

  const categoryData: Record<string, number> = {};
  materials.value.forEach((item: any) => {
    const catName = item.category?.name || '未分类';
    categoryData[catName] = (categoryData[catName] || 0) + item.stockQuantity;
  });
  
  const pieData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  if (pieChart) pieChart.dispose();
  pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    title: { text: '库存分布', left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%' },
    series: [{
      name: '库存数量',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false, position: 'center' },
      emphasis: { label: { show: true, fontSize: '20', fontWeight: 'bold' } },
      data: pieData
    }]
  });
};

const initLineChart = () => {
  if (!lineChartRef.value || trendEmpty.value) return;

  if (lineChart) lineChart.dispose();
  lineChart = echarts.init(lineChartRef.value);
  lineChart.setOption({
    title: { text: '近7日出入库趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '出库'], bottom: '0%' },
    grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.map(item => item.date)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '入库',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.inboundQuantity),
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '出库',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.outboundQuantity),
        itemStyle: { color: '#F56C6C' }
      }
    ]
  });
};

onMounted(() => {
  fetchData();
  fetchTrendData(7);
  window.addEventListener('resize', () => {
    pieChart?.resize();
    lineChart?.resize();
  });
});
</script>

<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6" v-for="(stat, index) in stats" :key="index">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">{{ stat.title }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="pieChartRef" style="height: 350px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div v-loading="trendLoading" style="height: 350px; position: relative;">
            <div v-if="trendEmpty && !trendLoading" class="empty-trend">
              <el-empty description="暂无出入库趋势数据" />
            </div>
            <div v-show="!trendEmpty" ref="lineChartRef" style="height: 350px;"></div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="section-title">
      <h3>主要仓库概览</h3>
    </div>
    
    <el-row :gutter="20">
      <el-col :span="8" v-for="w in warehouses" :key="w.id">
        <el-card shadow="hover" class="warehouse-card">
          <template #header>
            <div class="card-header">
              <span class="wh-name">{{ w.name }}</span>
              <el-tag effect="dark" type="info">{{ w.code }}</el-tag>
            </div>
          </template>
          <div class="card-body">
            <p><span class="label">负责人:</span> {{ w.manager }}</p>
            <p><span class="label">地址:</span> {{ w.address }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container {
  padding-bottom: 20px;
}

.stat-card {
  margin-bottom: 20px;
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-right: 20px;
  color: white;
  font-size: 28px;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.chart-row {
  margin-bottom: 30px;
}

.empty-trend {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.section-title {
  margin-bottom: 20px;
  border-left: 5px solid #409EFF;
  padding-left: 15px;
}

.section-title h3 {
  margin: 0;
  color: #303133;
}

.warehouse-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.wh-name {
  font-weight: bold;
  font-size: 16px;
}

.card-body p {
  margin: 10px 0;
  color: #606266;
}

.label {
  color: #909399;
  margin-right: 10px;
}
</style>
