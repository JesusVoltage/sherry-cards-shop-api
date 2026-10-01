package com.sherrycardsshop.api.media.dto;

public record StorageUsageDto(boolean enabled, long usedBytes, long limitBytes) {
}
