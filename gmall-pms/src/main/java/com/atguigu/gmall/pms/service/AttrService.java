package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import java.util.List;

/**
 * 商品属性
 *
 * @author xiaohuo
 * @email xiaohuobulauya@163.com
 * @date 2021-04-29 17:40:13
 */
public interface AttrService extends IService<AttrEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<AttrEntity> queryAttrsByCidAndTypeOrSearchType(Long cid, Integer type, Integer searchType);
}

