package com.sherrycardsshop.api.admin.dto;

import com.sherrycardsshop.api.media.dto.StorageUsageDto;

public record DashboardDto(ProductCounts products, long categories, long users, long admins, StorageUsageDto storage) {

    public record ProductCounts(long total, long active, long draft, long archived) {
    }
}
