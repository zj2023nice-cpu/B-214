package com.warehouse.service;

import com.warehouse.dto.SupplierRatingDTO;
import com.warehouse.dto.SupplierWithRatingDTO;
import com.warehouse.entity.Supplier;
import com.warehouse.entity.SupplierRating;
import com.warehouse.entity.User;
import com.warehouse.repository.SupplierRatingRepository;
import com.warehouse.repository.SupplierRepository;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierRatingService {

    private final SupplierRatingRepository ratingRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupplierRatingDTO createRating(Long userId, Long supplierId, Integer rating, String content) {
        if (ratingRepository.existsByUserIdAndSupplierId(userId, supplierId)) {
            throw new RuntimeException("您已评价过该供应商，不能重复评价");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1-5之间");
        }

        SupplierRating supplierRating = new SupplierRating();
        supplierRating.setRating(rating);
        supplierRating.setContent(content);
        supplierRating.setRatingTime(LocalDateTime.now());
        supplierRating.setSupplier(supplier);
        supplierRating.setUser(user);

        SupplierRating saved = ratingRepository.save(supplierRating);
        return toDTO(saved);
    }

    @Transactional
    public SupplierRatingDTO updateRating(Long ratingId, Long userId, Integer rating, String content) {
        SupplierRating existing = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        if (!existing.getUser().getId().equals(userId)) {
            throw new RuntimeException("只能修改自己的评价");
        }

        if (rating != null) {
            if (rating < 1 || rating > 5) {
                throw new RuntimeException("评分必须在1-5之间");
            }
            existing.setRating(rating);
        }
        if (content != null) {
            existing.setContent(content);
        }
        existing.setRatingTime(LocalDateTime.now());

        SupplierRating saved = ratingRepository.save(existing);
        return toDTO(saved);
    }

    @Transactional
    public void deleteRating(Long ratingId, Long userId) {
        SupplierRating existing = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));
        if (!existing.getUser().getId().equals(userId)) {
            throw new RuntimeException("只能删除自己的评价");
        }
        ratingRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<SupplierRatingDTO> getRatingsBySupplier(Long supplierId) {
        return ratingRepository.findBySupplierIdOrderByRatingTimeDesc(supplierId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        return roundRating(supplier.getAverageRating());
    }

    @Transactional(readOnly = true)
    public List<SupplierRatingDTO> getLatestRatings(Long supplierId, int count) {
        return ratingRepository.findTop3BySupplierIdOrderByRatingTimeDesc(supplierId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean hasUserRated(Long userId, Long supplierId) {
        return ratingRepository.existsByUserIdAndSupplierId(userId, supplierId);
    }

    @Transactional(readOnly = true)
    public List<SupplierWithRatingDTO> getAllSuppliersWithRating() {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<Long> supplierIds = suppliers.stream().map(Supplier::getId).collect(Collectors.toList());

        Map<Long, Long> countMap = batchCountBySupplier(supplierIds);
        Map<Long, List<SupplierRating>> ratingsMap = batchLatestRatingsBySupplier(supplierIds);

        return suppliers.stream().map(supplier -> {
            SupplierWithRatingDTO dto = new SupplierWithRatingDTO();
            dto.setId(supplier.getId());
            dto.setName(supplier.getName());
            dto.setContactPerson(supplier.getContactPerson());
            dto.setPhone(supplier.getPhone());
            dto.setAddress(supplier.getAddress());
            dto.setAverageRating(roundRating(supplier.getAverageRating()));
            dto.setRatingCount(countMap.getOrDefault(supplier.getId(), 0L));
            dto.setLatestRatings(
                    ratingsMap.getOrDefault(supplier.getId(), Collections.emptyList()).stream()
                            .limit(3)
                            .map(this::toDTO)
                            .collect(Collectors.toList())
            );
            return dto;
        }).collect(Collectors.toList());
    }

    private Map<Long, Long> batchCountBySupplier(List<Long> supplierIds) {
        if (supplierIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : ratingRepository.countBySupplierIdIn(supplierIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    private Map<Long, List<SupplierRating>> batchLatestRatingsBySupplier(List<Long> supplierIds) {
        if (supplierIds.isEmpty()) return Collections.emptyMap();
        return ratingRepository.findBySupplierIdInOrderByRatingTimeDesc(supplierIds).stream()
                .collect(Collectors.groupingBy(r -> r.getSupplier().getId()));
    }

    private Double roundRating(Double value) {
        return value != null ? Math.round(value * 10.0) / 10.0 : 0.0;
    }

    private SupplierRatingDTO toDTO(SupplierRating entity) {
        SupplierRatingDTO dto = new SupplierRatingDTO();
        dto.setId(entity.getId());
        dto.setRating(entity.getRating());
        dto.setContent(entity.getContent());
        dto.setRatingTime(entity.getRatingTime());
        dto.setSupplierId(entity.getSupplier().getId());
        dto.setSupplierName(entity.getSupplier().getName());
        dto.setUserId(entity.getUser().getId());
        dto.setUsername(entity.getUser().getUsername());
        return dto;
    }
}
