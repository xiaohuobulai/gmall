package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author 小伙 not bad
 * @email xiaohubulaiya@163.com
 * @date 2021-04-30 12:32:34
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
