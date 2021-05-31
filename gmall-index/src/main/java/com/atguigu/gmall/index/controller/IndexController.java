package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model) {

        // 获取一级分类
        List<CategoryEntity> categoryEntities = this.indexService.queryLevel1Categories();
        model.addAttribute("categories", categoryEntities);

        // TODO: 获取广告信息

        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLevel2CategoriesWithSubsByPid(@PathVariable Long pid) {
        List<CategoryEntity> categoryEntities = this.indexService.queryLevel2CategoriesWithSubsByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }


}