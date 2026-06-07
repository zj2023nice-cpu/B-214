<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import dayjs from 'dayjs';

const activeTab = ref('inbound');
const inboundRecords = ref([]);
const outboundRecords = ref([]);

const fetchInbound = async () => {
  const res: any = await axios.get('/inventory/records/inbound');
  if (res.code === 200) inboundRecords.value = res.data;
};

const fetchOutbound = async () => {
  const res: any = await axios.get('/inventory/records/outbound');
  if (res.code === 200) outboundRecords.value = res.data;
};

const formatDate = (date: string) => {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
};

const handleTabChange = () => {
  if (activeTab.value === 'inbound') fetchInbound();
  else fetchOutbound();
};

onMounted(() => {
  fetchInbound();
});
</script>

<template>
  <div class="records-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="入库记录" name="inbound">
        <el-table :data="inboundRecords" border style="width: 100%">
          <el-table-column prop="serialNo" label="入库单号" width="180" />
          <el-table-column label="物资名称">
            <template #default="scope">
              {{ scope.row.material?.name }} ({{ scope.row.material?.code }})
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="price" label="单价" width="100" />
          <el-table-column label="入库时间" width="180">
            <template #default="scope">{{ formatDate(scope.row.inboundTime) }}</template>
          </el-table-column>
          <el-table-column label="供应商">
            <template #default="scope">{{ scope.row.supplier?.name || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="出库记录" name="outbound">
        <el-table :data="outboundRecords" border style="width: 100%">
          <el-table-column prop="serialNo" label="出库单号" width="180" />
          <el-table-column label="物资名称">
            <template #default="scope">
              {{ scope.row.material?.name }} ({{ scope.row.material?.code }})
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column label="出库时间" width="180">
             <template #default="scope">{{ formatDate(scope.row.outboundTime) }}</template>
          </el-table-column>
          <el-table-column prop="department" label="领用部门" width="150" />
          <el-table-column prop="remark" label="备注" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.records-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}
</style>
