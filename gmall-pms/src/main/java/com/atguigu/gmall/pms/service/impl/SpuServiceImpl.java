package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.*;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrsValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import org.springframework.util.CollectionUtils;

@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescService spuDescService;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient gmallSmsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuInfo(PageParamVo pageParamVo, Long cid) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        // 如果cid != 0 则说明是查本类
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }
        String key = pageParamVo.getKey();
        if (StringUtils.isNotBlank(key)) {
            wrapper.and(t -> t.like("name", key).or().like("id", key));
        }
        return new PageResultVo(this.page(pageParamVo.getPage(), wrapper));
    }

    @Override
    @GlobalTransactional
    public void bigSave(SpuVo spuVo) {
        // 1、保存spu相关信息
        // 1.1 保存spu基本信息
        Long spuId = saveSpuInfo(spuVo);
        // 1.2 保存spu描述信息
        this.spuDescService.saveSpuDesc(spuVo, spuId);
        // 1.3 保存spu规格参数
        saveBaseAttrs(spuVo, spuId);
        // 2. 保存sku相关信息
        saveSkuInfo(spuVo, spuId);
        this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE", "item.insert", spuId);
    }

    public Long saveSpuInfo(SpuVo spuVo) {
        // 1.1 保存spu基本信息
        spuVo.setPublishStatus(1);// 默认是已上架
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());// 新增时，更新时间和创建时间一致
        this.save(spuVo);
        // 获取spuId
        return spuVo.getId();
    }

    public void saveBaseAttrs(SpuVo spuVo, Long spuId) {
        // 1.3 保存spu规格参数
        List<SpuAttrsValueVo> baseAttrs = spuVo.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(spuAttrsValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(spuAttrsValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                spuAttrValueEntity.setSort(0);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            // mapper里面没有批量保存的方法，service里面有
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
        }
    }

    public void saveSkuInfo(SpuVo spuVo, Long spuId) {
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        skus.forEach(skuVo -> {
            // 2.1. 保存sku基本信息
            skuVo.setSpuId(spuId);
            skuVo.setBrandId(spuVo.getBrandId());
            skuVo.setCategoryId(spuVo.getCategoryId());
            // 设置默认图片
            List<String> images = skuVo.getImages();
            if (!CollectionUtils.isEmpty(images)) {
                skuVo.setDefaultImage(StringUtils.isNotBlank(skuVo.getDefaultImage()) ? skuVo.getDefaultImage() : images.get(0));
            }
            this.skuMapper.insert(skuVo);

            Long skuId = skuVo.getId();
            // 2.2. 保存sku图片信息
            if (!CollectionUtils.isEmpty(images)) {
                this.skuImagesService.saveBatch(images.stream().map(image -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVo.getDefaultImage(), image) ? 1 : 0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            // 2.3. 保存sku的规格参数（销售属性）
            List<SkuAttrValueEntity> saleAttrs = skuVo.getSaleAttrs();
            if (!CollectionUtils.isEmpty(saleAttrs)) {
                saleAttrs.forEach(skuAttrValueEntity -> {
                    skuAttrValueEntity.setSkuId(skuId);
                });
                this.skuAttrValueService.saveBatch(saleAttrs);
            }
            // 3. 保存营销相关信息，需要远程调用gmall-sms
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVo, skuSaleVo);
            skuSaleVo.setSkuId(skuId);
            this.gmallSmsClient.saveSales(skuSaleVo);
        });
    }


}