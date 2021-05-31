package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(paramVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            SearchResponseVo searchResponseVo = this.parseResult(response);
            searchResponseVo.setPageNum(paramVo.getPageNum());
            searchResponseVo.setPageSize(paramVo.getPageSize());
            return searchResponseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        // 解析hits
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits());

        SearchHit[] hitsHits = hits.getHits();
        List<Goods> goodsList = Stream.of(hitsHits).map(hitsHit -> {
            String json = hitsHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            // 获取高亮标题，覆盖_source普通标题
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField highlightTitle = highlightFields.get("title");
                goods.setTitle(highlightTitle.getFragments()[0].string());
            }

            return goods;
        }).collect(Collectors.toList());
        responseVo.setGoodsList(goodsList);

        // 解析aggregations
        Map<String, Aggregation> aggregationMap = response.getAggregations().asMap();
        // 获取品牌
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandIdAgg");
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)) {
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                // 获取桶中的子聚合
                Map<String, Aggregation> brandSubAggMap = bucket.getAggregations().asMap();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandSubAggMap.get("brandNameAgg");
                brandEntity.setName(brandNameAgg.getBuckets().get(0).getKeyAsString());

                ParsedStringTerms logoAgg = (ParsedStringTerms) brandSubAggMap.get("LogoAgg");
                brandEntity.setLogo(logoAgg.getBuckets().get(0).getKeyAsString());
                return brandEntity;
            }).collect(Collectors.toList()));
        }

        // 获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)) {
            List<CategoryEntity> categoryEntities = categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                ParsedStringTerms categoryNameAgg = (ParsedStringTerms) bucket.getAggregations().get("categoryNameAgg");
                categoryEntity.setName(categoryNameAgg.getBuckets().get(0).getKeyAsString());
                return categoryEntity;
            }).collect(Collectors.toList());
            responseVo.setCategories(categoryEntities);
        }

        // 获取规格参数
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(attrIdAggBuckets)) {
            List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());

                Map<String, Aggregation> attrSubAgg = bucket.getAggregations().asMap();
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) attrSubAgg.get("attrNameAgg");
                searchResponseAttrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());

                ParsedStringTerms attrValueAgg = (ParsedStringTerms) attrSubAgg.get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(valueBuckets)) {
                    searchResponseAttrVo.setAttrValues(valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(searchResponseAttrVos);
        }
        return responseVo;
    }

    private SearchSourceBuilder buildDsl(SearchParamVo paramVo) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)) {
            return sourceBuilder;
        }
        // 1.构建检索条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1 构建匹配搜索条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        // 1.2 构建过滤条件
        // 1.2.1 构建品牌过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }

        // 1.2.2 构建分类过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }

        // 1.2.3 构建价格区间过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }

        // 1.2.4 构建是否有货过滤
        Boolean store = paramVo.getStore();
        if (store != null && store) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }

        // 1.2.5 构建规格参数嵌套过滤
        List<String> props = paramVo.getProps();
        if (!CollectionUtils.isEmpty(props)) {
            props.forEach(prop -> { // 每一个prop就会对应一个嵌套过滤
                // 分隔字符串  4:8G-12G
                String[] attr = StringUtils.split(prop, ":");
                if (attr != null && attr.length == 2) {
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    String[] attrValues = StringUtils.split(attr[1], "-");
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", attrValues));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }

        // 2.排序
        Integer sort = paramVo.getSort();
        switch (sort) {
            case 1:
                sourceBuilder.sort("price", SortOrder.DESC);
                break;
            case 2:
                sourceBuilder.sort("price", SortOrder.ASC);
                break;
            case 3:
                sourceBuilder.sort("sales", SortOrder.DESC);
                break;
            case 4:
                sourceBuilder.sort("createTime", SortOrder.DESC);
                break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC);
        }

        // 3.构建分页条件
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        // 4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<font style='color:red;'>").postTags("</font>"));

        // 5.构建聚合
        // 5.1 构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("LogoAgg").field("logo"))
        );

        // 5.2 构建分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"))
        );

        // 5.3 构建规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue")))
        );

        // 6. 构建结果集过滤，过滤出来需要响应给前端的数据
        sourceBuilder.fetchSource(new String[]{"skuId", "defaultImage", "price", "title", "subTitle"}, null);
        return sourceBuilder;
    }
}
