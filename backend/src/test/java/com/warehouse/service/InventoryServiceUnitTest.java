package com.warehouse.service;

import com.warehouse.entity.*;
import com.warehouse.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryService 出入库核心业务逻辑单元测试")
class InventoryServiceUnitTest {

    @InjectMocks
    private InventoryService inventoryService;

    @Mock
    private InboundRecordRepository inboundRecordRepository;

    @Mock
    private OutboundRecordRepository outboundRecordRepository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private WarehouseInventoryService warehouseInventoryService;

    @Mock
    private NotificationService notificationService;

    private Material testMaterial;
    private Warehouse testWarehouse;
    private Shelf testShelf;

    @BeforeEach
    void setUp() {
        testMaterial = new Material();
        testMaterial.setId(1L);
        testMaterial.setCode("MAT001");
        testMaterial.setName("测试物资");
        testMaterial.setStockQuantity(100);
        testMaterial.setAlertThreshold(10);
        testMaterial.setAlertSent(false);

        testWarehouse = new Warehouse();
        testWarehouse.setId(1L);
        testWarehouse.setCode("WH001");
        testWarehouse.setName("一号仓库");

        testShelf = new Shelf();
        testShelf.setId(1L);
        testShelf.setCode("A-01");
        testShelf.setName("A区01货架");
        testShelf.setCapacity(200);
        testShelf.setCurrentLoad(50);
        testShelf.setWarehouse(testWarehouse);
    }

    @Nested
    @DisplayName("processInbound 入库业务逻辑测试")
    class ProcessInboundTests {

        @Test
        @DisplayName("正常入库 - 无仓库时直接增加物资库存")
        void processInbound_WithoutWarehouse_IncreasesMaterialStock() {
            // Given
            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-001");
            record.setQuantity(50);
            record.setPrice(100.0);
            record.setMaterial(testMaterial);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            InboundRecord result = inventoryService.processInbound(record);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getInboundTime()).isNotNull();
            assertThat(result.getMaterial()).isEqualTo(testMaterial);
            assertThat(testMaterial.getStockQuantity()).isEqualTo(150);
            verify(materialRepository).save(testMaterial);
            verify(inboundRecordRepository).save(record);
            verify(warehouseInventoryService, never()).addToWarehouse(anyLong(), anyLong(), anyInt());
        }

        @Test
        @DisplayName("正常入库 - 有仓库时调用仓库库存服务")
        void processInbound_WithWarehouse_CallsWarehouseInventoryService() {
            // Given
            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-002");
            record.setQuantity(30);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            InboundRecord result = inventoryService.processInbound(record);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getWarehouse()).isEqualTo(testWarehouse);
            verify(warehouseInventoryService).addToWarehouse(1L, 1L, 30);
            verify(materialRepository, never()).save(any(Material.class));
        }

        @Test
        @DisplayName("正常入库 - 有仓库和货架时更新货架承载量")
        void processInbound_WithWarehouseAndShelf_UpdatesShelfLoad() {
            // Given
            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-003");
            record.setQuantity(20);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setShelf(testShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(shelfRepository.findById(1L)).thenReturn(Optional.of(testShelf));
            when(shelfRepository.save(any(Shelf.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            InboundRecord result = inventoryService.processInbound(record);

            // Then
            assertThat(result).isNotNull();
            assertThat(testShelf.getCurrentLoad()).isEqualTo(70);
            verify(shelfRepository).save(testShelf);
        }

        @Test
        @DisplayName("物资不存在时抛出异常")
        void processInbound_MaterialNotFound_ThrowsException() {
            // Given
            Material nonExistentMaterial = new Material();
            nonExistentMaterial.setId(999L);

            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-004");
            record.setQuantity(10);
            record.setMaterial(nonExistentMaterial);

            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.processInbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Material not found");

            verify(inboundRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("仓库不存在时抛出异常")
        void processInbound_WarehouseNotFound_ThrowsException() {
            // Given
            Warehouse nonExistentWarehouse = new Warehouse();
            nonExistentWarehouse.setId(999L);

            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-005");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(nonExistentWarehouse);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.processInbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Warehouse not found");
        }

        @Test
        @DisplayName("未选择仓库时指定货架抛出异常")
        void processInbound_ShelfWithoutWarehouse_ThrowsException() {
            // Given
            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-006");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setShelf(testShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

            // When & Then
            assertThatThrownBy(() -> inventoryService.processInbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("未选择仓库时不能指定货架");
        }

        @Test
        @DisplayName("货架不属于当前仓库时抛出异常")
        void processInbound_ShelfBelongsToDifferentWarehouse_ThrowsException() {
            // Given
            Warehouse anotherWarehouse = new Warehouse();
            anotherWarehouse.setId(2L);
            anotherWarehouse.setCode("WH002");

            Shelf wrongShelf = new Shelf();
            wrongShelf.setId(2L);
            wrongShelf.setCode("B-01");
            wrongShelf.setWarehouse(anotherWarehouse);

            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-007");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setShelf(wrongShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(shelfRepository.findById(2L)).thenReturn(Optional.of(wrongShelf));

            // When & Then
            assertThatThrownBy(() -> inventoryService.processInbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("所选货架不属于当前仓库");
        }

        @Test
        @DisplayName("货架超载时抛出异常")
        void processInbound_ShelfOverloaded_ThrowsException() {
            // Given
            Shelf smallShelf = new Shelf();
            smallShelf.setId(3L);
            smallShelf.setCode("C-01");
            smallShelf.setCapacity(60);
            smallShelf.setCurrentLoad(50);
            smallShelf.setWarehouse(testWarehouse);

            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-008");
            record.setQuantity(20);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setShelf(smallShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(shelfRepository.findById(3L)).thenReturn(Optional.of(smallShelf));

            // When & Then
            assertThatThrownBy(() -> inventoryService.processInbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("货架超载");
        }

        @Test
        @DisplayName("入库成功后发送通知")
        void processInbound_Success_SendsNotification() {
            // Given
            InboundRecord record = new InboundRecord();
            record.setSerialNo("IN-TEST-009");
            record.setQuantity(50);
            record.setMaterial(testMaterial);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));
            when(inboundRecordRepository.save(any(InboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            inventoryService.processInbound(record);

            // Then
            verify(notificationService).sendNotificationToAllUsersAsync(
                    eq("入库完成"),
                    contains("测试物资"),
                    eq("SUCCESS"),
                    eq("/inventory/records"));
        }
    }

    @Nested
    @DisplayName("processOutbound 出库业务逻辑测试")
    class ProcessOutboundTests {

        @Test
        @DisplayName("正常出库 - 创建待审批出库记录")
        void processOutbound_Normal_CreatesPendingRecord() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-001");
            record.setQuantity(20);
            record.setMaterial(testMaterial);
            record.setDepartment("技术部");

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> {
                OutboundRecord saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            OutboundRecord result = inventoryService.processOutbound(record);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(OutboundStatus.PENDING);
            assertThat(result.getOutboundTime()).isNotNull();
            assertThat(result.getMaterial()).isEqualTo(testMaterial);
            verify(outboundRecordRepository).save(record);
        }

        @Test
        @DisplayName("正常出库 - 有仓库时设置仓库关联")
        void processOutbound_WithWarehouse_SetsWarehouseAssociation() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-002");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            OutboundRecord result = inventoryService.processOutbound(record);

            // Then
            assertThat(result.getWarehouse()).isEqualTo(testWarehouse);
            verify(outboundRecordRepository).save(record);
        }

        @Test
        @DisplayName("正常出库 - 有仓库和货架时设置货架关联")
        void processOutbound_WithWarehouseAndShelf_SetsShelfAssociation() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-003");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setShelf(testShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(shelfRepository.findById(1L)).thenReturn(Optional.of(testShelf));
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            OutboundRecord result = inventoryService.processOutbound(record);

            // Then
            assertThat(result.getShelf()).isEqualTo(testShelf);
        }

        @Test
        @DisplayName("物资不存在时抛出异常")
        void processOutbound_MaterialNotFound_ThrowsException() {
            // Given
            Material nonExistentMaterial = new Material();
            nonExistentMaterial.setId(999L);

            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-004");
            record.setQuantity(10);
            record.setMaterial(nonExistentMaterial);

            when(materialRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.processOutbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Material not found");

            verify(outboundRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("仓库不存在时抛出异常")
        void processOutbound_WarehouseNotFound_ThrowsException() {
            // Given
            Warehouse nonExistentWarehouse = new Warehouse();
            nonExistentWarehouse.setId(999L);

            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-005");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(nonExistentWarehouse);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.processOutbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Warehouse not found");
        }

        @Test
        @DisplayName("未选择仓库时指定货架抛出异常")
        void processOutbound_ShelfWithoutWarehouse_ThrowsException() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-006");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setShelf(testShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));

            // When & Then
            assertThatThrownBy(() -> inventoryService.processOutbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("未选择仓库时不能指定货架");
        }

        @Test
        @DisplayName("货架不属于当前仓库时抛出异常")
        void processOutbound_ShelfBelongsToDifferentWarehouse_ThrowsException() {
            // Given
            Warehouse anotherWarehouse = new Warehouse();
            anotherWarehouse.setId(2L);

            Shelf wrongShelf = new Shelf();
            wrongShelf.setId(2L);
            wrongShelf.setWarehouse(anotherWarehouse);

            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-007");
            record.setQuantity(10);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setShelf(wrongShelf);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
            when(shelfRepository.findById(2L)).thenReturn(Optional.of(wrongShelf));

            // When & Then
            assertThatThrownBy(() -> inventoryService.processOutbound(record))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("所选货架不属于当前仓库");
        }

        @Test
        @DisplayName("出库申请成功后发送通知")
        void processOutbound_Success_SendsNotification() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setSerialNo("OUT-TEST-008");
            record.setQuantity(20);
            record.setMaterial(testMaterial);

            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            inventoryService.processOutbound(record);

            // Then
            verify(notificationService).sendNotificationToAllUsersAsync(
                    eq("出库申请"),
                    contains("测试物资"),
                    eq("INFO"),
                    eq("/inventory/outbound-approval"));
        }
    }

    @Nested
    @DisplayName("approveOutbound 审批出库业务逻辑测试")
    class ApproveOutboundTests {

        @Test
        @DisplayName("审批通过 - 库存充足时扣减库存并更新状态")
        void approveOutbound_SufficientStock_DeductsAndCompletes() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setId(1L);
            record.setSerialNo("OUT-APPROVE-001");
            record.setQuantity(30);
            record.setMaterial(testMaterial);
            record.setStatus(OutboundStatus.PENDING);

            when(outboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));

            // When
            OutboundRecord result = inventoryService.approveOutbound(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo(OutboundStatus.COMPLETED);
            assertThat(testMaterial.getStockQuantity()).isEqualTo(70);
            verify(materialRepository).save(testMaterial);
            verify(outboundRecordRepository).save(record);
        }

        @Test
        @DisplayName("审批通过 - 有仓库时调用仓库库存扣减服务")
        void approveOutbound_WithWarehouse_CallsWarehouseDeduction() {
            // Given
            OutboundRecord record = new OutboundRecord();
            record.setId(1L);
            record.setSerialNo("OUT-APPROVE-002");
            record.setQuantity(20);
            record.setMaterial(testMaterial);
            record.setWarehouse(testWarehouse);
            record.setStatus(OutboundStatus.PENDING);

            when(outboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));
            when(warehouseInventoryService.deductFromWarehouse(1L, 1L, 20)).thenReturn(null);
            when(outboundRecordRepository.save(any(OutboundRecord.class))).thenAnswer(inv -> inv.getArgument(0));
            when(materialRepository.findById(1L)).thenReturn(Optional.of(testMaterial));

            // When
            OutboundRecord result = inventoryService.approveOutbound(1L);

            // Then
            assertThat(result.getStatus()).isEqualTo(OutboundStatus.COMPLETED);
            verify(warehouseInventoryService).deductFromWarehouse(1L, 1L, 20);
            verify(materialRepository, never()).save(any(Material.class));
        }

        @Test
        @DisplayName("库存不足出库抛出异常")
        void approveOutbound_InsufficientStock_ThrowsException() {
            // Given
            Material lowStockMaterial = new Material();
            lowStockMaterial.setId(2L);
            lowStockMaterial.setName("低库存物资");
            lowStockMaterial.setStockQuantity(5);

            OutboundRecord record = new OutboundRecord();
            record.setId(1L);
            record.setSerialNo("OUT-APPROVE-003");
            record.setQuantity(20);
            record.setMaterial(lowStockMaterial);
            record.setStatus(OutboundStatus.PENDING);

            when(outboundRecordRepository.findById(1L)).thenReturn(Optional.of(record));

            // When & Then
            assertThatThrownBy(() -> inventoryService.approveOutbound(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Insufficient stock");

            assertThat(lowStockMaterial.getStockQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("出库记录不存在时抛出异常")
        void approveOutbound_RecordNotFound_ThrowsException() {
            // Given
            when(outboundRecordRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> inventoryService.approveOutbound(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Outbound record not found");
        }

        @Test
        @DisplayName("非待审批状态出库申请抛出异常")
        void approveOutbound_NotPendingStatus_ThrowsException() {
            // Given
            OutboundRecord completedRecord = new OutboundRecord();
            completedRecord.setId(1L);
            completedRecord.setQuantity(10);
            completedRecord.setMaterial(testMaterial);
            completedRecord.setStatus(OutboundStatus.COMPLETED);

            when(outboundRecordRepository.findById(1L)).thenReturn(Optional.of(completedRecord));

            // When & Then
            assertThatThrownBy(() -> inventoryService.approveOutbound(1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("只能审批待审批状态的出库申请");
        }
    }
}
