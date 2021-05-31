package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import java.util.List;

/**
 * 商品三级分类
 *
 * @author xiaohuo
 * @email xiaohuobulauya@163.com
 * @date 2021-04-29 17:40:13
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<CategoryEntity> queryCategories(Long parentId);

    List<CategoryEntity> queryLevel2CatesWithSubsPid(Long pid);
}

