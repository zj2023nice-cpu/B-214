package com.warehouse.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.*;
import com.warehouse.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("InventoryController 集成测试")
class InventoryControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private InboundRecordRepository inboundRecordRepository;

    @Autowired
    private OutboundRecordRepository outboundRecordRepository;

    private final AtomicInteger serialCounter = new AtomicInteger(1);

    private Material testMaterial;

    private Warehouse testWarehouse;

    private Shelf testShelf;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("测试分类");
        category = categoryRepository.save(category);

        Supplier supplier = new Supplier();
        supplier.setName("测试供应商");
        supplier.setContactPerson("张三");
        supplier.setPhone("13800138000");
        supplier = supplierRepository.save(supplier);

        testMaterial = new Material();
        testMaterial.setCode("MAT-IT-" + serialCounter.get());
        testMaterial.setName("集成测试物资");
        testMaterial.setUnit("个");
        testMaterial.setPrice(100.0);
        testMaterial.setStockQuantity(100);
        testMaterial.setAlertThreshold(10);
        testMaterial.setCategory(category);
        testMaterial.setSupplier(supplier);
        testMaterial = materialRepository.save(testMaterial);

        testWarehouse = new Warehouse();
        testWarehouse.setCode("WH-IT-" + serialCounter.get());
        testWarehouse.setName("集成测试仓库");
        testWarehouse.setAddress("测试地址");
        testWarehouse = warehouseRepository.save(testWarehouse);

        testShelf = new Shelf();
        testShelf.setCode("SH-IT-" + serialCounter.get());
        testShelf.setName("集成测试货架");
        testShelf.setCapacity(200);
        testShelf.setCurrentLoad(10);
        testShelf.setWarehouse(testWarehouse);
        testShelf = shelfRepository.save(testShelf);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ApiResponse<Map<String, Object>> parseApiResponse(String body) throws Exception {
        return objectMapper.readValue(body, new TypeReference<ApiResponse<Map<String, Object>>>() {});
    }

    @Nested
    @DisplayName("POST /api/inventory/inbound 入库端点测试")
    class InboundEndpointTests {

        @Test
        @DisplayName("正常入库 - 返回200和入库记录")
        void inbound_ValidRequest_ReturnsOkWithRecord() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "IN-INT-%03d",
                    "quantity": 30,
                    "price": 100.0,
                    "material": { "id": %d },
                    "remark": "集成测试入库"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(200);
            assertThat(apiResponse.getData().get("serialNo")).isNotNull();
            assertThat(apiResponse.getData().get("quantity")).isEqualTo(30);
            assertThat(apiResponse.getData().get("inboundTime")).isNotNull();
        }

        @Test
        @DisplayName("正常入库 - 有仓库和货架")
        void inbound_WithWarehouseAndShelf_ReturnsOkWithRecord() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "IN-INT-%03d",
                    "quantity": 20,
                    "price": 80.0,
                    "material": { "id": %d },
                    "warehouse": { "id": %d },
                    "shelf": { "id": %d },
                    "remark": "带仓库和货架入库"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId(),
                    testWarehouse.getId(), testShelf.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("物资不存在返回400状态码")
        void inbound_MaterialNotFound_Returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "IN-INT-%03d",
                    "quantity": 10,
                    "material": { "id": 99999 }
                }
                """.formatted(serialCounter.getAndIncrement());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(400);
            assertThat(apiResponse.getMessage()).contains("Material not found");
        }

        @Test
        @DisplayName("无效JSON请求返回400状态码")
        void inbound_InvalidJson_Returns400() {
            // Given
            String invalidJson = "{ invalid json content }";

            HttpEntity<String> entity = new HttpEntity<>(invalidJson, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("仓库不存在返回400状态码")
        void inbound_WarehouseNotFound_Returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "IN-INT-%03d",
                    "quantity": 10,
                    "material": { "id": %d },
                    "warehouse": { "id": 99999 }
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("POST /api/inventory/outbound 出库端点测试")
    class OutboundEndpointTests {

        @Test
        @DisplayName("正常出库 - 返回200和待审批出库记录")
        void outbound_ValidRequest_ReturnsOkWithPendingRecord() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "OUT-INT-%03d",
                    "quantity": 20,
                    "material": { "id": %d },
                    "department": "技术部",
                    "remark": "集成测试出库"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(200);
            assertThat(apiResponse.getData().get("status")).isEqualTo("PENDING");
            assertThat(apiResponse.getData().get("outboundTime")).isNotNull();
            assertThat(apiResponse.getData().get("quantity")).isEqualTo(20);
        }

        @Test
        @DisplayName("正常出库 - 有仓库时设置仓库关联")
        void outbound_WithWarehouse_ReturnsOkWithWarehouse() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "OUT-INT-%03d",
                    "quantity": 10,
                    "material": { "id": %d },
                    "warehouse": { "id": %d },
                    "department": "研发部"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId(),
                    testWarehouse.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("物资不存在返回400状态码")
        void outbound_MaterialNotFound_Returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "OUT-INT-%03d",
                    "quantity": 10,
                    "material": { "id": 99999 }
                }
                """.formatted(serialCounter.getAndIncrement());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(400);
            assertThat(apiResponse.getMessage()).contains("Material not found");
        }

        @Test
        @DisplayName("无效JSON请求返回400状态码")
        void outbound_InvalidJson_Returns400() {
            // Given
            String invalidJson = "{ this is not valid }";

            HttpEntity<String> entity = new HttpEntity<>(invalidJson, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("仓库不存在返回400状态码")
        void outbound_WarehouseNotFound_Returns400() throws Exception {
            // Given
            String requestBody = """
                {
                    "serialNo": "OUT-INT-%03d",
                    "quantity": 10,
                    "material": { "id": %d },
                    "warehouse": { "id": 99999 }
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            ApiResponse<Map<String, Object>> apiResponse = parseApiResponse(response.getBody());
            assertThat(apiResponse.getCode()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("入库出库事务一致性测试")
    class TransactionConsistencyTests {

        @Test
        @DisplayName("入库后数据库中存在对应记录")
        void inbound_RecordPersistedInDatabase() throws Exception {
            // Given
            long countBefore = inboundRecordRepository.count();
            String requestBody = """
                {
                    "serialNo": "IN-TX-%03d",
                    "quantity": 25,
                    "price": 50.0,
                    "material": { "id": %d },
                    "remark": "事务测试入库"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            long countAfter = inboundRecordRepository.count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("出库后数据库中存在对应待审批记录")
        void outbound_RecordPersistedInDatabase() throws Exception {
            // Given
            long countBefore = outboundRecordRepository.count();
            String requestBody = """
                {
                    "serialNo": "OUT-TX-%03d",
                    "quantity": 15,
                    "material": { "id": %d },
                    "department": "财务部"
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/outbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            long countAfter = outboundRecordRepository.count();
            assertThat(countAfter).isEqualTo(countBefore + 1);
        }

        @Test
        @DisplayName("入库后物资库存数量增加")
        void inbound_StockQuantityIncreased() throws Exception {
            // Given
            int stockBefore = materialRepository.findById(testMaterial.getId()).get().getStockQuantity();
            String requestBody = """
                {
                    "serialNo": "IN-TX-STOCK-%03d",
                    "quantity": 30,
                    "price": 100.0,
                    "material": { "id": %d }
                }
                """.formatted(serialCounter.getAndIncrement(), testMaterial.getId());

            HttpEntity<String> entity = new HttpEntity<>(requestBody, jsonHeaders());

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    "/api/inventory/inbound", HttpMethod.POST, entity, String.class);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            int stockAfter = materialRepository.findById(testMaterial.getId()).get().getStockQuantity();
            assertThat(stockAfter).isEqualTo(stockBefore + 30);
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        @Order(1)
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .securityMatcher("/**")
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.disable())
                    .requestCache(cache -> cache.disable())
                    .anonymous(anonymous -> anonymous.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return web -> web.ignoring().requestMatchers("/**");
        }
    }
}
