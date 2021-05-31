package com.atguigu.sms.api;

import com.atguigu.gmall.common.bean.ResponseVo;

import com.atguigu.sms.vo.SkuSaleVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface GmallSmsApi {
    @PostMapping("/sms/skubounds/save")
    public ResponseVo<SkuSaleVo> saveSales(@RequestBody SkuSaleVo skuSaleVo);
}
