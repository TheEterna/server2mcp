package com.ai.plug.test.test.mapper;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author han
 * @time 2025/7/3 13:13
 */
@Data
@TableName("full_store_data")
public class Yizi {
    private String storeCode;
    private String regionGroup;
    private String region;
    private String subRegion;
    private String city;
    private String district;
    private String town;
    private String customerCode;
    private String customerName;
    private String channelType;
    private String storeType;
    private String storeSubtype;
    private String storeName;
    private Long assignedSalesmanCount;
    private Long plannedVisits;
    private Long actualVisits;
    private Double visitAchievementRate;
    private Long unvisitedStores;
    private Long plannedStoreCount;
    private Long actualStoreCount;
    private Double storeCoverageRate;
    private Long visitedDaysBlock;
    private Double visitDurationHours;
    private Boolean isValid;
    private String address;
    private Double lng;
    private Double lat;

}
