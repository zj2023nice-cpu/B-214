<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onBeforeUnmount } from 'vue';
import axios from '../../api/axios';
import * as echarts from 'echarts';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { ElMessage } from 'element-plus';
import { ArrowDown } from '@element-plus/icons-vue';

const materials = ref([]);
const pieChartRef = ref(null);
const barChartRef = ref(null);
let pieChart: any = null;
let barChart: any = null;

const turnoverData = ref<any[]>([]);
const turnoverChartRef = ref(null);
let turnoverChart: any = null;
const selectedMonth = ref('');
const exporting = ref(false);
const reportContainerRef = ref<HTMLElement | null>(null);
const pdfExporting = ref(false);
const exportProgress = ref(0);
const exportProgressText = ref('');
const showExportProgress = ref(false);
let originalChartSizes: Record<string, { width: string; height: string }> = {};

const getDefaultMonth = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  return `${y}-${m}`;
};

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials/all');
  if (res.code === 200) {
    materials.value = res.data;
    nextTick(() => {
      initCharts();
    });
  }
};

const fetchTurnoverRate = async () => {
  const month = selectedMonth.value || getDefaultMonth();
  const res: any = await axios.get('/inventory/turnover-rate', { params: { month } });
  if (res.code === 200) {
    turnoverData.value = res.data;
    nextTick(() => {
      initTurnoverChart();
    });
  }
};

const exportExcel = async () => {
  const month = selectedMonth.value || getDefaultMonth();
  exporting.value = true;
  try {
    const response = await fetch(`/api/inventory/turnover-rate/export?month=${month}`);
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'turnover_rate_report.xlsx';
    link.click();
    window.URL.revokeObjectURL(url);
  } finally {
    exporting.value = false;
  }
};

const totalMaterials = computed(() => materials.value.length);
const totalStock = computed(() => materials.value.reduce((sum, item: any) => sum + item.stockQuantity, 0));
const totalValue = computed(() => materials.value.reduce((sum, item: any) => sum + (item.stockQuantity * item.price), 0).toFixed(2));
const lowStockCount = computed(() => materials.value.filter((item: any) => item.stockQuantity < item.alertThreshold).length);

const initCharts = () => {
  if (!pieChartRef.value || !barChartRef.value) return;

  const categoryData: Record<string, number> = {};
  materials.value.forEach((item: any) => {
    const catName = item.category?.name || '未分类';
    categoryData[catName] = (categoryData[catName] || 0) + item.stockQuantity;
  });
  
  const pieData = Object.keys(categoryData).map(key => ({
    name: key,
    value: categoryData[key]
  }));

  const sortedByValue = [...materials.value].map((item: any) => ({
    name: item.name,
    value: item.stockQuantity * item.price
  })).sort((a, b) => b.value - a.value).slice(0, 5);

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

const initTurnoverChart = () => {
  if (!turnoverChartRef.value) return;

  const top10 = turnoverData.value.slice(0, 10);

  if (turnoverChart) turnoverChart.dispose();
  turnoverChart = echarts.init(turnoverChartRef.value);
  turnoverChart.setOption({
    title: { text: '库存周转率 Top 10', left: 'center' },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const item = params[0];
        return `${item.name}<br/>周转率: ${item.value}`;
      }
    },
    xAxis: {
      type: 'category',
      data: top10.map((i: any) => i.name),
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value', name: '周转率' },
    series: [
      {
        data: top10.map((i: any) => i.turnoverRate),
        type: 'bar',
        showBackground: true,
        backgroundStyle: { color: 'rgba(180, 180, 180, 0.2)' },
        itemStyle: { color: '#409EFF' }
      }
    ]
  });
};

const getDateString = () => {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, '0');
  const d = String(now.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const saveChartSizes = () => {
  const chartRefs: Record<string, any> = {
    pieChartRef,
    barChartRef,
    turnoverChartRef
  };
  Object.entries(chartRefs).forEach(([key, chartRef]) => {
    const el = chartRef.value as HTMLElement | null;
    if (el) {
      originalChartSizes[key] = {
        width: el.style.width || '',
        height: el.style.height || ''
      };
    }
  });
};

const resizeChartsForPrint = () => {
  const printWidth = 340;
  const printHeight = 280;
  const turnoverHeight = 300;

  if (pieChartRef.value) {
    const el = pieChartRef.value as HTMLElement;
    el.style.width = `${printWidth}px`;
    el.style.height = `${printHeight}px`;
  }
  if (barChartRef.value) {
    const el = barChartRef.value as HTMLElement;
    el.style.width = `${printWidth}px`;
    el.style.height = `${printHeight}px`;
  }
  if (turnoverChartRef.value) {
    const el = turnoverChartRef.value as HTMLElement;
    el.style.width = `${printWidth * 2 + 20}px`;
    el.style.height = `${turnoverHeight}px`;
  }
  pieChart?.resize();
  barChart?.resize();
  turnoverChart?.resize();
};

const restoreChartSizes = () => {
  const chartRefs: Record<string, any> = {
    pieChartRef,
    barChartRef,
    turnoverChartRef
  };
  Object.entries(chartRefs).forEach(([key, chartRef]) => {
    const el = chartRef.value as HTMLElement | null;
    if (el && originalChartSizes[key]) {
      el.style.width = originalChartSizes[key].width;
      el.style.height = originalChartSizes[key].height;
    }
  });
  pieChart?.resize();
  barChart?.resize();
  turnoverChart?.resize();
};

const handleExportCommand = (command: string) => {
  const exportAll = command === 'all';
  exportToPDF(exportAll);
};

const exportToPDF = async (exportAll: boolean) => {
  if (!reportContainerRef.value) return;
  pdfExporting.value = true;
  showExportProgress.value = true;
  exportProgress.value = 0;
  exportProgressText.value = '正在准备导出...';

  try {
    exportProgress.value = 10;
    exportProgressText.value = '调整图表尺寸...';

    saveChartSizes();
    resizeChartsForPrint();
    await nextTick();
    await new Promise(resolve => setTimeout(resolve, 600));

    exportProgress.value = 30;
    exportProgressText.value = '正在捕获页面内容...';

    const canvas = await html2canvas(reportContainerRef.value!, {
      scale: 2,
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      onclone: (_doc, clonedEl) => {
        const noPrintElements = clonedEl.querySelectorAll('.no-print');
        noPrintElements.forEach((el) => el.remove());

        if (exportAll) {
          const tableBodyWrappers = clonedEl.querySelectorAll('.el-table__body-wrapper');
          tableBodyWrappers.forEach((el) => {
            (el as HTMLElement).style.maxHeight = 'none';
            (el as HTMLElement).style.overflow = 'visible';
            (el as HTMLElement).style.height = 'auto';
          });
          const fixedWrappers = clonedEl.querySelectorAll('.el-table__fixed, .el-table__fixed-right');
          fixedWrappers.forEach((el) => {
            (el as HTMLElement).style.height = 'auto';
          });
          const tableRoots = clonedEl.querySelectorAll('.el-table');
          tableRoots.forEach((el) => {
            (el as HTMLElement).style.height = 'auto';
          });
        }
      }
    });

    exportProgress.value = 60;
    exportProgressText.value = '正在生成 PDF...';

    const imgData = canvas.toDataURL('image/jpeg', 0.92);
    const pdf = new jsPDF('p', 'mm', 'a4');

    const pageWidth = 210;
    const pageHeight = 297;
    const margin = 10;
    const contentWidth = pageWidth - margin * 2;
    const contentHeight = pageHeight - margin * 2;

    const imgWidth = canvas.width;
    const imgHeight = canvas.height;
    const ratio = contentWidth / imgWidth;
    const scaledHeight = imgHeight * ratio;

    let heightLeft = scaledHeight;
    let position = margin;
    let pageIndex = 0;

    pdf.addImage(imgData, 'JPEG', margin, position, contentWidth, scaledHeight);
    heightLeft -= contentHeight;
    pageIndex++;

    while (heightLeft > 0) {
      position = margin - contentHeight * pageIndex;
      pdf.addPage();
      pdf.addImage(imgData, 'JPEG', margin, position, contentWidth, scaledHeight);
      heightLeft -= contentHeight;
      pageIndex++;
    }

    exportProgress.value = 90;
    exportProgressText.value = '正在保存文件...';

    const fileName = `库存报表_${getDateString()}.pdf`;
    pdf.save(fileName);

    exportProgress.value = 100;
    exportProgressText.value = '导出完成！';

    restoreChartSizes();
    await nextTick();

    ElMessage.success('PDF 导出成功');
  } catch (error) {
    console.error('PDF export failed:', error);
    restoreChartSizes();
    ElMessage.error('PDF 导出失败，请重试');
  } finally {
    setTimeout(() => {
      pdfExporting.value = false;
      showExportProgress.value = false;
    }, 800);
  }
};

onMounted(() => {
  selectedMonth.value = getDefaultMonth();
  fetchMaterials();
  fetchTurnoverRate();
  const resizeHandler = () => {
    pieChart?.resize();
    barChart?.resize();
    turnoverChart?.resize();
  };
  window.addEventListener('resize', resizeHandler);
  onBeforeUnmount(() => {
    window.removeEventListener('resize', resizeHandler);
  });
});
</script>

<template>
  <div class="report-container" ref="reportContainerRef">
    <div class="report-header no-print">
      <h2 class="report-title">库存报表</h2>
      <el-dropdown trigger="click" @command="handleExportCommand">
        <el-button type="primary" :loading="pdfExporting">
          导出 PDF
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="current">导出当前页</el-dropdown-item>
            <el-dropdown-item command="all">导出全部数据</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

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

    <el-table :data="materials" border style="width: 100%; margin-bottom: 20px;" height="400">
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

    <el-card shadow="hover" style="margin-bottom: 20px;">
      <template #header>
        <div class="turnover-header">
          <span>库存周转率报表（仅统计实际出库）</span>
          <div class="turnover-actions no-print">
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              placeholder="选择月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              style="width: 180px; margin-right: 12px;"
              @change="fetchTurnoverRate"
            />
            <el-button type="success" :loading="exporting" @click="exportExcel">导出 Excel</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="20" style="margin-bottom: 20px;">
        <el-col :span="24">
          <div ref="turnoverChartRef" style="height: 350px;"></div>
        </el-col>
      </el-row>

      <el-table :data="turnoverData" border style="width: 100%;" height="400">
        <el-table-column prop="code" label="物资编号" />
        <el-table-column prop="name" label="物资名称" />
        <el-table-column prop="outboundTotal" label="实际出库总量" sortable />
        <el-table-column prop="avgStock" label="平均库存量" sortable />
        <el-table-column prop="turnoverRate" label="周转率" sortable />
      </el-table>
    </el-card>

    <el-dialog
      v-model="showExportProgress"
      title="PDF 导出进度"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="no-print"
    >
      <div class="export-progress-content">
        <el-progress :percentage="exportProgress" :stroke-width="20" :text-inside="true" status="success" />
        <p class="export-progress-text">{{ exportProgressText }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.report-container {
  padding: 20px;
}
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.report-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.card-value {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
}
.turnover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.turnover-actions {
  display: flex;
  align-items: center;
}
.export-progress-content {
  padding: 10px 0;
}
.export-progress-text {
  text-align: center;
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
}
</style>

<style>
@media print {
  .no-print {
    display: none !important;
  }
  .report-container {
    padding: 0 !important;
  }
  .el-card {
    box-shadow: none !important;
    border: 1px solid #dcdfe6 !important;
    break-inside: avoid;
  }
  .el-table {
    font-size: 10px !important;
  }
  .el-table th {
    padding: 4px 0 !important;
    font-size: 10px !important;
  }
  .el-table td {
    padding: 4px 0 !important;
    font-size: 10px !important;
  }
  .el-table__body-wrapper {
    overflow: visible !important;
    max-height: none !important;
    height: auto !important;
  }
  .el-table {
    height: auto !important;
  }
  .report-title {
    display: block !important;
    text-align: center;
    font-size: 18px !important;
    margin-bottom: 10px !important;
  }
  .card-value {
    font-size: 18px !important;
  }
  .el-card__header {
    padding: 8px 15px !important;
    font-size: 12px !important;
  }
  .el-col {
    break-inside: avoid;
  }
  .el-row {
    break-inside: avoid;
  }
  @page {
    size: A4;
    margin: 15mm;
  }
}
</style>
