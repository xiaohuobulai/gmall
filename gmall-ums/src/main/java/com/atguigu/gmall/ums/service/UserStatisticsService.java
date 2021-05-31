package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserStatisticsEntity;

import java.util.Map;

/**
 * 统计信息表
 *
 * @author 小伙 not bad
 * @email xiaohubulaiya@163.com
 * @date 2021-04-30 12:32:34
 */
public interface UserStatisticsService extends IService<UserStatisticsEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

