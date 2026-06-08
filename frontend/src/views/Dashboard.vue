<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed, nextTick, type Ref } from 'vue';
import axios from '../api/axios';
import * as echarts from 'echarts';
import { Box, Warning, Money, House, Download, Upload, TrendCharts } from '@element-plus/icons-vue';

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

interface DashboardStats {
  todayInboundTotal: number;
  todayOutboundTotal: number;
  monthlyStockValueChange: number;
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

const dashboardStats = ref<DashboardStats>({
  todayInboundTotal: 0,
  todayOutboundTotal: 0,
  monthlyStockValueChange: 0,
});
const dashboardStatsLoading = ref(true);

const animatedTodayInbound = ref(0);
const animatedTodayOutbound = ref(0);
const animatedMonthlyChange = ref(0);

let pollTimer: ReturnType<typeof setInterval> | null = null;
let isPageVisible = ref(true);

function animateNumber(target: Ref<number>, to: number, duration: number = 800) {
  const from = target.value;
  const diff = to - from;
  if (diff === 0) {
    target.value = to;
    return;
  }
  const startTime = performance.now();
  function step(currentTime: number) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    target.value = Math.round((from + diff * eased) * 100) / 100;
    if (progress < 1) {
      requestAnimationFrame(step);
    }
  }
  requestAnimationFrame(step);
}

const fetchDashboardStats = async () => {
  try {
    const res: any = await axios.get('/inventory/statistics/dashboard');
    if (res.code === 200 && res.data) {
      dashboardStats.value = res.data;
      animateNumber(animatedTodayInbound, res.data.todayInboundTotal);
      animateNumber(animatedTodayOutbound, res.data.todayOutboundTotal);
      animateNumber(animatedMonthlyChange, res.data.monthlyStockValueChange);
    }
  } catch (error) {
    console.error(error);
  } finally {
    dashboardStatsLoading.value = false;
  }
};

const fetchData = async () => {
  try {
    const [wRes, mRes]: any = await Promise.all([
      axios.get('/warehouses'),
      axios.get('/materials/all')
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

const refreshAll = async () => {
  await Promise.all([
    fetchData(),
    fetchDashboardStats(),
    fetchTrendData(7),
  ]);
};

const startPolling = () => {
  stopPolling();
  pollTimer = setInterval(() => {
    refreshAll();
  }, 30000);
};

const stopPolling = () => {
  if (pollTimer !== null) {
    clearInterval(pollTimer);
    pollTimer = null;
  }
};

const handleVisibilityChange = () => {
  if (document.hidden) {
    isPageVisible.value = false;
    stopPolling();
  } else {
    isPageVisible.value = true;
    refreshAll();
    startPolling();
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

const extraStats = computed(() => {
  const changeVal = animatedMonthlyChange.value;
  const changeStr = changeVal >= 0
    ? `+¥${changeVal.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
    : `-¥${Math.abs(changeVal).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  return [
    { title: '今日入库总量', value: animatedTodayInbound.value, icon: Download, color: '#36D399' },
    { title: '今日出库总量', value: animatedTodayOutbound.value, icon: Upload, color: '#FB7185' },
    { title: '本月库存总值变化', value: changeStr, icon: TrendCharts, color: changeVal >= 0 ? '#67C23A' : '#F56C6C' },
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
    title: { text: '近7日实际出入库趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库', '实际出库'], bottom: '0%' },
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
        name: '实际出库',
        type: 'line',
        smooth: true,
        data: trendData.value.map(item => item.outboundQuantity),
        itemStyle: { color: '#F56C6C' }
      }
    ]
  });
};

onMounted(() => {
  refreshAll();
  startPolling();
  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener('resize', () => {
    pieChart?.resize();
    lineChart?.resize();
  });
});

onBeforeUnmount(() => {
  stopPolling();
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (pieChart) pieChart.dispose();
  if (lineChart) lineChart.dispose();
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

    <el-row :gutter="20" class="extra-stats-row">
      <el-col :span="8" v-for="(stat, index) in extraStats" :key="'extra-' + index">
        <el-card shadow="hover" class="stat-card extra-stat-card">
          <div v-if="dashboardStatsLoading" class="skeleton-wrapper">
            <div class="skeleton-icon"></div>
            <div class="skeleton-text-group">
              <div class="skeleton-title"></div>
              <div class="skeleton-value"></div>
            </div>
          </div>
          <div v-else class="stat-content">
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

    <div class="poll-indicator">
      <span class="poll-dot" :class="{ active: isPageVisible }"></span>
      {{ isPageVisible ? '实时刷新中 (30s)' : '刷新已暂停' }}
    </div>
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
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
  min-width: 0;
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

.extra-stats-row {
  margin-bottom: 0;
}

.extra-stat-card {
  height: 100%;
}

.skeleton-wrapper {
  display: flex;
  align-items: center;
}

.skeleton-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  margin-right: 20px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-text-group {
  flex: 1;
  min-width: 0;
}

.skeleton-title {
  height: 16px;
  width: 60%;
  border-radius: 4px;
  margin-bottom: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}

.skeleton-value {
  height: 28px;
  width: 80%;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
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

.poll-indicator {
  position: fixed;
  bottom: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;
  background: #fff;
  padding: 6px 12px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.poll-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  transition: background 0.3s;
}

.poll-dot.active {
  background: #67C23A;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
