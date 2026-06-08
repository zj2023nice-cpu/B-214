<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from '../../api/axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import type { UploadProps } from 'element-plus';

const materials = ref([]);
const categories = ref([]);
const suppliers = ref([]);
const dialogVisible = ref(false);
const importResultVisible = ref(false);
const importResult = ref<any>(null);
const uploading = ref(false);
const editingId = ref<number | null>(null);
const imageList = ref<any[]>([]);
const previewVisible = ref(false);
const previewUrl = ref('');
const form = ref<any>({
  code: '',
  name: '',
  spec: '',
  unit: '',
  price: 0,
  stockQuantity: 0,
  alertThreshold: 10,
  category: { id: null },
  supplier: { id: null }
});

const fetchMaterials = async () => {
  const res: any = await axios.get('/materials');
  if (res.code === 200) {
    materials.value = res.data;
  }
};

const fetchBasicData = async () => {
  const catRes: any = await axios.get('/categories');
  if (catRes.code === 200) categories.value = catRes.data;
  
  const supRes: any = await axios.get('/suppliers');
  if (supRes.code === 200) suppliers.value = supRes.data;
};

const parseImageUrls = (images: string | null | undefined): string[] => {
  if (!images) return [];
  return images.split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0);
};

const handleAdd = () => {
  editingId.value = null;
  form.value = {
    code: '',
    name: '',
    spec: '',
    unit: '',
    price: 0,
    stockQuantity: 0,
    alertThreshold: 10,
    category: { id: null },
    supplier: { id: null }
  };
  imageList.value = [];
  if (categories.value.length > 0) form.value.category.id = categories.value[0].id;
  if (suppliers.value.length > 0) form.value.supplier.id = suppliers.value[0].id;
  dialogVisible.value = true;
};

const handleEdit = (row: any) => {
  editingId.value = row.id;
  form.value = {
    code: row.code,
    name: row.name,
    spec: row.spec,
    unit: row.unit,
    price: row.price,
    stockQuantity: row.stockQuantity,
    alertThreshold: row.alertThreshold,
    category: row.category ? { id: row.category.id } : { id: null },
    supplier: row.supplier ? { id: row.supplier.id } : { id: null }
  };
  const urls = parseImageUrls(row.images);
  imageList.value = urls.map((url: string, index: number) => ({
    name: `image-${index}`,
    url: url,
    status: 'success'
  }));
  dialogVisible.value = true;
};

const handleSave = async () => {
  if (!form.value.category.id) form.value.category = null;
  if (!form.value.supplier.id) form.value.supplier = null;

  const urls = imageList.value
    .filter((item: any) => item.status === 'success' && item.url)
    .map((item: any) => item.url);
  form.value.images = urls.length > 0 ? urls.join(',') : null;

  let res: any;
  if (editingId.value) {
    res = await axios.put(`/materials/${editingId.value}`, form.value);
  } else {
    res = await axios.post('/materials', form.value);
  }
  if (res.code === 200) {
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    fetchMaterials();
  }
};

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定删除该物资吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    const res: any = await axios.delete(`/materials/${id}`);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchMaterials();
    }
  });
};

const handleImportUpload = async (options: any) => {
  const formData = new FormData();
  formData.append('file', options.file);
  uploading.value = true;
  try {
    const res: any = await axios.post('/materials/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    });
    if (res.code === 200) {
      importResult.value = res.data;
      importResultVisible.value = true;
      fetchMaterials();
      if (res.data.failureCount === 0) {
        ElMessage.success(`导入成功，共导入 ${res.data.successCount} 条记录`);
      } else {
        ElMessage.warning(`导入完成，成功 ${res.data.successCount} 条，失败 ${res.data.failureCount} 条`);
      }
    } else {
      ElMessage.error(res.message || '导入失败');
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '导入失败');
  } finally {
    uploading.value = false;
  }
};

const handleDownloadTemplate = () => {
  window.open('/api/materials/template', '_blank');
};

const beforeImageUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (!['image/jpeg', 'image/png'].includes(rawFile.type)) {
    ElMessage.error('图片格式仅支持 JPG/PNG');
    return false;
  }
  if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('图片大小不能超过 2MB');
    return false;
  }
  return true;
};

const handleImageUpload = async (options: any) => {
  const formData = new FormData();
  formData.append('files', options.file);
  try {
    const res: any = await axios.post('/upload/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 30000,
    });
    if (res.code === 200 && res.data && res.data.length > 0) {
      const url = res.data[0];
      const file = imageList.value.find((f: any) => f.uid === options.file.uid);
      if (file) {
        file.url = url;
        file.status = 'success';
      }
      ElMessage.success('图片上传成功');
    } else {
      ElMessage.error(res.message || '图片上传失败');
      imageList.value = imageList.value.filter((f: any) => f.uid !== options.file.uid);
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '图片上传失败');
    imageList.value = imageList.value.filter((f: any) => f.uid !== options.file.uid);
  }
};

const handleImageRemove = (file: any) => {
  imageList.value = imageList.value.filter((item: any) => item.uid !== file.uid);
};

const handleImagePreview = (file: any) => {
  previewUrl.value = file.url;
  previewVisible.value = true;
};

const handleExceed = () => {
  ElMessage.warning('最多上传5张图片');
};

const tableRowClassName = ({ row }: { row: any }) => {
  if (row.stockQuantity < row.alertThreshold) {
    return 'warning-row';
  }
  return '';
};

onMounted(() => {
  fetchMaterials();
  fetchBasicData();
});
</script>

<template>
  <div class="material-container">
    <div class="header-actions">
      <el-button type="primary" @click="handleAdd">新增物资</el-button>
      <el-upload
        :show-file-list="false"
        :http-request="handleImportUpload"
        accept=".xlsx,.xls,.csv"
        style="display: inline-block; margin-left: 12px;"
      >
        <el-button type="success" :loading="uploading">批量导入</el-button>
      </el-upload>
      <el-button @click="handleDownloadTemplate" style="margin-left: 12px;">下载导入模板</el-button>
    </div>

    <el-table 
      :data="materials" 
      style="width: 100%; margin-top: 20px;" 
      border 
      :row-class-name="tableRowClassName"
    >
      <el-table-column prop="code" label="物资编号" width="120" />
      <el-table-column prop="name" label="物资名称" width="150" />
      <el-table-column label="图片" width="100">
        <template #default="scope">
          <div v-if="parseImageUrls(scope.row.images).length > 0" class="thumbnail-wrapper">
            <el-image
              :src="parseImageUrls(scope.row.images)[0]"
              :preview-src-list="parseImageUrls(scope.row.images)"
              fit="cover"
              class="thumbnail-img"
              :z-index="9999"
            />
            <span v-if="parseImageUrls(scope.row.images).length > 1" class="image-count">
              +{{ parseImageUrls(scope.row.images).length - 1 }}
            </span>
          </div>
          <span v-else style="color: #c0c4cc;">暂无</span>
        </template>
      </el-table-column>
      <el-table-column prop="spec" label="规格型号" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="price" label="单价" width="100" />
      <el-table-column label="分类" width="120">
        <template #default="scope">
          {{ scope.row.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="供应商" width="150">
        <template #default="scope">
          {{ scope.row.supplier?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="stockQuantity" label="当前库存" width="100">
         <template #default="scope">
           <span :style="scope.row.stockQuantity < scope.row.alertThreshold ? 'color: red; font-weight: bold' : ''">
             {{ scope.row.stockQuantity }}
           </span>
         </template>
      </el-table-column>
      <el-table-column prop="alertThreshold" label="预警值" width="100" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑物资' : '新增物资'" width="650px">
      <el-form :model="form" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="物资编号">
              <el-input v-model="form.code" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物资名称">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="规格型号">
              <el-input v-model="form.spec" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位">
              <el-input v-model="form.unit" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单价">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
             <el-form-item label="初始库存">
              <el-input-number v-model="form.stockQuantity" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="库存预警">
              <el-input-number v-model="form.alertThreshold" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分类">
          <el-select v-model="form.category.id" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.supplier.id" placeholder="请选择供应商" style="width: 100%">
            <el-option v-for="item in suppliers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="物资图片">
          <el-upload
            v-model:file-list="imageList"
            :http-request="handleImageUpload"
            :before-upload="beforeImageUpload"
            :on-remove="handleImageRemove"
            :on-preview="handleImagePreview"
            :on-exceed="handleExceed"
            accept=".jpg,.jpeg,.png"
            :limit="5"
            list-type="picture-card"
            multiple
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="previewVisible" title="图片预览" width="700px">
      <img :src="previewUrl" alt="预览" style="width: 100%;" />
    </el-dialog>

    <el-dialog v-model="importResultVisible" title="导入结果" width="650px">
      <div v-if="importResult" class="import-result">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="总计">{{ importResult.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="成功">
            <span style="color: #67c23a; font-weight: bold;">{{ importResult.successCount }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <span :style="importResult.failureCount > 0 ? 'color: #f56c6c; font-weight: bold;' : ''">{{ importResult.failureCount }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="importResult.failures && importResult.failures.length > 0" style="margin-top: 16px;">
          <h4 style="margin-bottom: 8px;">失败详情：</h4>
          <el-table :data="importResult.failures" border max-height="300" style="width: 100%;">
            <el-table-column prop="rowIndex" label="行号" width="80" />
            <el-table-column prop="materialCode" label="物资编号" width="120" />
            <el-table-column prop="reason" label="失败原因" />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.material-container {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
}
.header-actions {
  display: flex;
  align-items: center;
}
.warning-row {
  --el-table-tr-bg-color: var(--el-color-warning-light-9);
}
.thumbnail-wrapper {
  position: relative;
  display: inline-block;
}
.thumbnail-img {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  cursor: pointer;
}
.image-count {
  position: absolute;
  bottom: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 4px 0 4px 0;
  line-height: 16px;
}
</style>
