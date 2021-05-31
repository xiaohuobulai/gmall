package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.HomeSubjectEntity;

import java.util.Map;

/**
 * 首页专题表【jd首页下面很多专题，每个专题链接新的页面，展示专题商品信息】
 *
 * @author 小伙 not bad
 * @email xiaohubulaiya@163.com
 * @date 2021-04-30 12:00:24
 */
public interface HomeSubjectService extends IService<HomeSubjectEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

