package com.atguigu.sms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuSaleVo {
    private Long skuId;
    // 积分优惠信息
    private BigDecimal growBounds;
    private BigDecimal buyBounds;
    private List<Integer> work;
    // 打折优惠信息
    private Integer fullCount;
    private BigDecimal discount;
    private Integer ladderAddOther;
    // 满减优惠信息
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private Integer fullAddOther;
}
