package com.warehouse.repository;

import com.warehouse.entity.TransferRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRecordRepository extends JpaRepository<TransferRecord, Long> {
}
