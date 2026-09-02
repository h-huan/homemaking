<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="Order No"><el-input v-model="queryParams.orderNo" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="Mobile"><el-input v-model="queryParams.contactMobile" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="Order Status"><el-input v-model="queryParams.orderStatus" clearable style="width: 140px" /></el-form-item>
      <el-form-item label="Pay Status"><el-input v-model="queryParams.payStatus" clearable style="width: 140px" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">Search</el-button><el-button icon="Refresh" @click="resetQuery">Reset</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="Order No" prop="orderNo" min-width="180" />
      <el-table-column label="Service" prop="serviceName" min-width="160" />
      <el-table-column label="Contact" prop="contactName" min-width="120" />
      <el-table-column label="Mobile" prop="contactMobile" min-width="120" />
      <el-table-column label="Appointment" min-width="180"><template #default="scope">{{ parseTime(scope.row.appointmentDate, '{y}-{m}-{d}') }} {{ scope.row.appointmentTimeSlot }}</template></el-table-column>
      <el-table-column label="Amount" prop="payAmount" width="110" />
      <el-table-column label="Order Status" prop="orderStatus" width="120" />
      <el-table-column label="Pay Status" prop="payStatus" width="100" />
      <el-table-column label="Assign Status" prop="assignStatus" width="110" />
      <el-table-column label="Action" width="240" fixed="right"><template #default="scope"><el-button link type="primary" @click="handleDetail(scope.row)">Detail</el-button><el-button link type="success" @click="handleAssign(scope.row)" v-hasPermi="['hm:order:assign']">Assign</el-button><el-button link type="warning" @click="handleCancel(scope.row)" v-hasPermi="['hm:order:cancel']">Cancel</el-button><el-button link type="danger" @click="handleRefund(scope.row)" v-hasPermi="['hm:order:cancel']">Refund</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-drawer v-model="detailOpen" title="Order Detail" size="45%"><template v-if="detail.orderId"><el-descriptions :column="1" border><el-descriptions-item label="Order No">{{ detail.orderNo }}</el-descriptions-item><el-descriptions-item label="Service">{{ detail.serviceName }}</el-descriptions-item><el-descriptions-item label="Contact">{{ detail.contactName }} / {{ detail.contactMobile }}</el-descriptions-item><el-descriptions-item label="Address">{{ detail.serviceAddress }}</el-descriptions-item><el-descriptions-item label="Appointment">{{ parseTime(detail.appointmentDate, '{y}-{m}-{d}') }} {{ detail.appointmentTimeSlot }}</el-descriptions-item><el-descriptions-item label="Amount">{{ detail.payAmount }}</el-descriptions-item><el-descriptions-item label="Customer Remark">{{ detail.customerRemark }}</el-descriptions-item><el-descriptions-item label="Cancel Reason">{{ detail.cancelReason }}</el-descriptions-item></el-descriptions><el-divider>Items</el-divider><el-table :data="detail.itemList || []" border><el-table-column label="Name" prop="itemName" min-width="180" /><el-table-column label="Price" prop="itemPrice" width="120" /><el-table-column label="Qty" prop="quantity" width="90" /><el-table-column label="Amount" prop="itemAmount" width="120" /></el-table><el-divider>Operate Logs</el-divider><el-timeline><el-timeline-item v-for="item in detail.operateLogs || []" :key="item.logId" :timestamp="parseTime(item.createTime)">{{ item.operatorName }} - {{ item.actionType }} - {{ item.actionDesc }}</el-timeline-item></el-timeline></template></el-drawer>
    <el-dialog v-model="assignOpen" title="Assign Worker" width="520px"><el-form :model="assignForm" label-width="100px"><el-form-item label="Worker"><el-select v-model="assignForm.workerId" style="width: 100%"><el-option v-for="item in workerOptions" :key="item.workerProfileId" :label="`${item.workerName} (${item.mobile})`" :value="item.workerProfileId" /></el-select></el-form-item><el-form-item label="Remark"><el-input v-model="assignForm.remark" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button type="primary" @click="submitAssign">Confirm</el-button><el-button @click="assignOpen = false">Cancel</el-button></template></el-dialog>
    <el-dialog v-model="reasonOpen" :title="reasonTitle" width="520px"><el-form :model="reasonForm" label-width="100px"><el-form-item label="Reason"><el-input v-model="reasonForm.reason" type="textarea" :rows="4" /></el-form-item></el-form><template #footer><el-button type="primary" @click="submitReason">Confirm</el-button><el-button @click="reasonOpen = false">Cancel</el-button></template></el-dialog>
  </div>
</template>
<script setup name="HmOrder">
import { assignOrder, cancelOrder, getOrder, listOrder, refundOrder } from '@/api/hm/order'
import { listWorker } from '@/api/hm/worker'
const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const orderList = ref([])
const detailOpen = ref(false)
const detail = ref({})
const assignOpen = ref(false)
const workerOptions = ref([])
const assignForm = ref({ orderId: undefined, workerId: undefined, remark: '' })
const reasonOpen = ref(false)
const reasonType = ref('cancel')
const reasonTitle = ref('')
const reasonForm = ref({ orderId: undefined, reason: '' })
const queryParams = reactive({ pageNum: 1, pageSize: 10, orderNo: undefined, contactMobile: undefined, orderStatus: undefined, payStatus: undefined })
function loadWorkers() { listWorker({ pageNum: 1, pageSize: 1000 }).then(res => workerOptions.value = res.rows || []) }
function getList() { loading.value = true; listOrder(queryParams).then(res => { orderList.value = res.rows; total.value = res.total }).finally(() => loading.value = false) }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleDetail(row) { getOrder(row.orderId).then(res => { detail.value = res.data || {}; detailOpen.value = true }) }
function handleAssign(row) { assignForm.value = { orderId: row.orderId, workerId: undefined, remark: '' }; assignOpen.value = true }
function handleCancel(row) { reasonType.value = 'cancel'; reasonTitle.value = 'Cancel Order'; reasonForm.value = { orderId: row.orderId, reason: '' }; reasonOpen.value = true }
function handleRefund(row) { reasonType.value = 'refund'; reasonTitle.value = 'Refund Order'; reasonForm.value = { orderId: row.orderId, reason: '' }; reasonOpen.value = true }
function submitAssign() { assignOrder(assignForm.value).then(() => { proxy.$modal.msgSuccess('Assigned successfully'); assignOpen.value = false; getList() }) }
function submitReason() { const payload = reasonType.value === 'cancel' ? { orderId: reasonForm.value.orderId, cancelReason: reasonForm.value.reason } : { orderId: reasonForm.value.orderId, refundReason: reasonForm.value.reason }; const action = reasonType.value === 'cancel' ? cancelOrder : refundOrder; action(payload).then(() => { proxy.$modal.msgSuccess('Processed successfully'); reasonOpen.value = false; getList() }) }
loadWorkers(); getList()
</script>