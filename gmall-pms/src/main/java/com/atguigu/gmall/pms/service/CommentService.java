package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.entity.CommentEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * 商品评价
 *
 * @author xiaohuo
 * @email xiaohuobulauya@163.com
 * @date 2021-04-29 17:40:13
 */
public interface CommentService extends IService<CommentEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

