<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue';
import axios from '../../api/axios';
import * as echarts from 'echarts';

const materials = ref([]);
const pieChartRef = ref(null);
const barChartRef = ref(null);
let pieChart: any = null;
let barChart: any = null;

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials');
  if (res.code === 200) {
    materials.value = res.data;
    nextTick(() => {
      initCharts();
    });
  }
};

const totalMaterials = computed(() => materials.value.length);
const totalStock = computed(() => materials.value.reduce((sum, item: any) => sum + item.stockQuantity, 0));
const totalValue = computed(() => materials.value.reduce((sum, item: any) => sum + (item.stockQuantity * item.price), 0).toFixed(2));
const lowStockCount = computed(() => materials.value.filter((item: any) => item.stockQuantity < item.alertThreshold).length);

const initCharts = () => {
  if (!pieChartRef.value || !barChartRef.value) return;

  // Pie Chart Data: Stock by Category
  const categoryData: Record<string, number> = {};
  materials.value.forEach((item: any) => {
    const catName = item.category?.name || '未分类';
    categoryData[catName] = (categoryData[catName] || 0) + item.stockQuantity;
  });
  
  const pieData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  // Bar Chart Data: Top 5 Value
  const sortedByValue = [...materials.value].map((item: any) => ({
    name: item.name,
    value: item.stockQuantity * item.price
  })).sort((a, b) => b.value - a.value).slice(0, 5);

  // Init Pie Chart
  if (pieChart) pieChart.dispose();
  pieChart = echarts.init(pieChartRef.value);
  pieChart.setOption({
    title: { text: '库存分类分布', left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '库存数量',
        type: 'pie',
        radius: '50%',
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  });

  // Init Bar Chart
  if (barChart) barChart.dispose();
  barChart = echarts.init(barChartRef.value);
  barChart.setOption({
    title: { text: '库存价值 Top 5', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: sortedByValue.map(i => i.name) },
    yAxis: { type: 'value' },
    series: [
      {
        data: sortedByValue.map(i => i.value),
        type: 'bar',
        showBackground: true,
        backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' }
      }
    ]
  });
};

onMounted(() => {
  fetchMaterials();
  window.addEventListener('resize', () => {
    pieChart?.resize();
    barChart?.resize();
  });
});
</script>

<template>
  <div class="report-container">
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>物资总数</template>
          <div class="card-value">{{ totalMaterials }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>库存总量</template>
          <div class="card-value">{{ totalStock }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>库存总价值</template>
          <div class="card-value">￥{{ totalValue }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>预警物资</template>
          <div class="card-value" style="color: red">{{ lowStockCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="pieChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="barChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-table :data="materials" border style="width: 100%" height="400">
      <el-table-column prop="code" label="物资编号" />
      <el-table-column prop="name" label="物资名称" />
      <el-table-column prop="category.name" label="分类" />
      <el-table-column prop="stockQuantity" label="当前库存" sortable />
      <el-table-column prop="unit" label="单位" />
      <el-table-column prop="price" label="单价" />
      <el-table-column label="总价">
        <template #default="scope">
          {{ (scope.row.price * scope.row.stockQuantity).toFixed(2) }}
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.report-container {
  padding: 20px;
}
.card-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
}
</style>
