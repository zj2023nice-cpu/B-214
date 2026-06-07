package com.warehouse.config;

import com.warehouse.entity.*;
import com.warehouse.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShelfRepository shelfRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final InboundRecordRepository inboundRecordRepository;

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedWarehousesAndShelves();
        seedCategories();
        seedSuppliers();
        seedMaterialsAndStock();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("123456"); // In real app, hash this
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user");
            user.setPassword("123456");
            user.setRole("USER");
            userRepository.save(user);

            System.out.println("Seeded users");
        }
    }

    private void seedWarehousesAndShelves() {
        if (warehouseRepository.count() == 0) {
            Warehouse w1 = new Warehouse();
            w1.setCode("WH001");
            w1.setName("一号主仓库");
            w1.setAddress("物流大道123号");
            w1.setManager("张三");
            w1 = warehouseRepository.save(w1);

            Warehouse w2 = new Warehouse();
            w2.setCode("WH002");
            w2.setName("二号冷链库");
            w2.setAddress("工业园区88号");
            w2.setManager("李四");
            w2 = warehouseRepository.save(w2);

            System.out.println("Seeded warehouse data");

            // Seed Shelves
            if (shelfRepository.count() == 0) {
                Shelf s1 = new Shelf();
                s1.setCode("A-01");
                s1.setName("A区01货架");
                s1.setCapacity(100);
                s1.setWarehouse(w1);
                shelfRepository.save(s1);

                Shelf s2 = new Shelf();
                s2.setCode("A-02");
                s2.setName("A区02货架");
                s2.setCapacity(100);
                s2.setWarehouse(w1);
                shelfRepository.save(s2);

                Shelf s3 = new Shelf();
                s3.setCode("B-01");
                s3.setName("B区冷冻架");
                s3.setCapacity(50);
                s3.setWarehouse(w2);
                shelfRepository.save(s3);

                System.out.println("Seeded shelf data");
            }
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            Category c1 = new Category();
            c1.setName("电子产品");
            c1.setDescription("各类电子元器件及成品");
            categoryRepository.save(c1);

            Category c2 = new Category();
            c2.setName("办公用品");
            c2.setDescription("日常办公消耗品");
            categoryRepository.save(c2);

            System.out.println("Seeded category data");
        }
    }

    private void seedSuppliers() {
        if (supplierRepository.count() == 0) {
            Supplier s1 = new Supplier();
            s1.setName("科技先锋有限公司");
            s1.setContactPerson("王五");
            s1.setPhone("13800138000");
            s1.setAddress("科技园A栋");
            supplierRepository.save(s1);

            Supplier s2 = new Supplier();
            s2.setName("文具大王贸易");
            s2.setContactPerson("赵六");
            s2.setPhone("13900139000");
            s2.setAddress("商贸城B区");
            supplierRepository.save(s2);

            System.out.println("Seeded supplier data");
        }
    }

    private void seedMaterialsAndStock() {
        if (materialRepository.count() == 0) {
            List<Category> categories = categoryRepository.findAll();
            List<Supplier> suppliers = supplierRepository.findAll();

            if (categories.isEmpty() || suppliers.isEmpty()) return;

            Category electronics = categories.stream().filter(c -> c.getName().equals("电子产品")).findFirst().orElse(categories.get(0));
            Category office = categories.stream().filter(c -> c.getName().equals("办公用品")).findFirst().orElse(categories.get(0));
            Supplier techSupplier = suppliers.stream().filter(s -> s.getName().equals("科技先锋有限公司")).findFirst().orElse(suppliers.get(0));
            Supplier officeSupplier = suppliers.stream().filter(s -> s.getName().equals("文具大王贸易")).findFirst().orElse(suppliers.get(0));

            // Material 1: Laptop
            Material m1 = new Material();
            m1.setCode("MAT001");
            m1.setName("高性能笔记本");
            m1.setSpec("i7/16G/512G");
            m1.setUnit("台");
            m1.setPrice(5000.0);
            m1.setCategory(electronics);
            m1.setSupplier(techSupplier);
            m1.setAlertThreshold(10);
            m1.setStockQuantity(50); // Initial stock
            m1 = materialRepository.save(m1);

            // Inbound Record for M1
            InboundRecord in1 = new InboundRecord();
            in1.setSerialNo("IN" + System.currentTimeMillis() + "01");
            in1.setInboundTime(LocalDateTime.now().minusDays(5));
            in1.setQuantity(50);
            in1.setPrice(4800.0); // Purchase price
            in1.setMaterial(m1);
            in1.setSupplier(techSupplier);
            in1.setRemark("初始入库");
            inboundRecordRepository.save(in1);

            // Material 2: Mouse
            Material m2 = new Material();
            m2.setCode("MAT002");
            m2.setName("无线鼠标");
            m2.setSpec("蓝牙/2.4G");
            m2.setUnit("个");
            m2.setPrice(50.0);
            m2.setCategory(electronics);
            m2.setSupplier(techSupplier);
            m2.setAlertThreshold(20);
            m2.setStockQuantity(5); // Low stock warning test
            m2 = materialRepository.save(m2);

            // Inbound Record for M2
            InboundRecord in2 = new InboundRecord();
            in2.setSerialNo("IN" + System.currentTimeMillis() + "02");
            in2.setInboundTime(LocalDateTime.now().minusDays(3));
            in2.setQuantity(5);
            in2.setPrice(45.0);
            in2.setMaterial(m2);
            in2.setSupplier(techSupplier);
            in2.setRemark("少量补货");
            inboundRecordRepository.save(in2);

            // Material 3: A4 Paper
            Material m3 = new Material();
            m3.setCode("MAT003");
            m3.setName("A4打印纸");
            m3.setSpec("70g/500张");
            m3.setUnit("包");
            m3.setPrice(20.0);
            m3.setCategory(office);
            m3.setSupplier(officeSupplier);
            m3.setAlertThreshold(50);
            m3.setStockQuantity(200);
            m3 = materialRepository.save(m3);

            // Inbound Record for M3
            InboundRecord in3 = new InboundRecord();
            in3.setSerialNo("IN" + System.currentTimeMillis() + "03");
            in3.setInboundTime(LocalDateTime.now().minusDays(10));
            in3.setQuantity(200);
            in3.setPrice(18.0);
            in3.setMaterial(m3);
            in3.setSupplier(officeSupplier);
            in3.setRemark("季度采购");
            inboundRecordRepository.save(in3);

            System.out.println("Seeded materials and inbound records");
        }
    }
}
