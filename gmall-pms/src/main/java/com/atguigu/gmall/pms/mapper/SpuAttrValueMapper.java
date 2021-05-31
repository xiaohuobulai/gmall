package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * spu属性值
 * 
 * @author xiaohuo
 * @email xiaohuobulauya@163.com
 * @date 2021-04-29 17:40:13
 */
@Mapper
public interface SpuAttrValueMapper extends BaseMapper<SpuAttrValueEntity> {

    List<SpuAttrValueEntity> querySearchAttrValueBySpuId(Long spuId);
}
