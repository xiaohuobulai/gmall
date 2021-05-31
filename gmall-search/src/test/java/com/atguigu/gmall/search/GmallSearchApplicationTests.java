package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Test
    void contextLoads() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);

        Integer pageNum = 1;
        Integer pageSize = 100;
        do {
            // 分页查询sku
            PageParamVo pageParamVo = new PageParamVo(pageNum, pageSize, null);
            ResponseVo<List<SpuEntity>> listResponseVo = this.pmsClient.querySpuByPageJson(pageParamVo);
            List<SpuEntity> spuEntities = listResponseVo.getData();
            if (CollectionUtils.isEmpty(spuEntities)) {
                return;
            }
            // 遍历spu，查询每个spu下的所有sku
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkuInfoBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                if (!CollectionUtils.isEmpty(skuEntities)) {
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        // sku相关信息
                        goods.setSkuId(skuEntity.getId());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setPrice(skuEntity.getPrice().doubleValue());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setSubTitle(skuEntity.getSubtitle());
                        // 获取创建时间
                        goods.setCreateTime(spuEntity.getCreateTime());
                        // 获取库存：销量和是否有货
                        ResponseVo<List<WareSkuEntity>> wareBySkuId = this.wmsClient.querySkuWareBySkuId(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareBySkuId.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get().longValue());
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                        }
                        // 品牌
                        ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                        BrandEntity brandEntity = brandEntityResponseVo.getData();
                        if (brandEntity != null) {
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }

                        // 分类
                        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCategoryId());
                        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                        if (categoryEntity != null) {
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }


                        // 检索spu参数
                        ResponseVo<List<SpuAttrValueEntity>> attrValueResp = this.pmsClient.querySearchAttrValueBySpuId(spuEntity.getId());
                        List<SpuAttrValueEntity> attrValueEntities = attrValueResp.getData();
                        List<SearchAttrValue> searchAttrValues = new ArrayList<>();
                        if (!CollectionUtils.isEmpty(attrValueEntities)) {
                            searchAttrValues = attrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                searchAttrValue.setAttrId(spuAttrValueEntity.getAttrId());
                                searchAttrValue.setAttrName(spuAttrValueEntity.getAttrName());
                                searchAttrValue.setAttrValue(spuAttrValueEntity.getAttrValue());
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                        }

                        // 检索sku参数
                        ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResp = this.pmsClient.querySearchAttrValueBySkuId(spuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResp.getData();
                        List<SearchAttrValue> searchSkuAttrValues = new ArrayList<>();
                        if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                            searchSkuAttrValues = skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValue searchAttrValue = new SearchAttrValue();
                                searchAttrValue.setAttrId(skuAttrValueEntity.getAttrId());
                                searchAttrValue.setAttrName(skuAttrValueEntity.getAttrName());
                                searchAttrValue.setAttrValue(skuAttrValueEntity.getAttrValue());
                                return searchAttrValue;
                            }).collect(Collectors.toList());
                        }
                        searchAttrValues.addAll(searchSkuAttrValues);
                        goods.setSearchAttrs(searchAttrValues);
                        return goods;
                    }).collect(Collectors.toList());
                    this.repository.saveAll(goodsList);
                }
            });


            pageSize = spuEntities.size();
            pageNum++;
        } while (pageSize == 100);
    }


}
