package com.warehouse.exception;

import lombok.Getter;

@Getter
public class DeleteConflictException extends RuntimeException {
    private final String entityType;
    private final long count;

    public DeleteConflictException(String entityType, long count) {
        super(String.format("该%s下存在 %d 个物资，无法删除", entityType, count));
        this.entityType = entityType;
        this.count = count;
    }
}
