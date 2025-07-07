package com.ai.plug.test.test.controller;

import com.ai.plug.test.test.mapper.Yizi;
import com.ai.plug.test.test.mapper.YiziMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author han
 * @time 2025/7/3 13:05
 */

@RestController
@RequestMapping("/yizi/shanghai")
public class YiziController {
    @Autowired
    private YiziMapper yiziMapper;

    @GetMapping("/count/county")
    public List<Map<String, Object>> listCountryBySupplier(@RequestParam(value = "supplier", required = false) String supplier) {

        QueryWrapper<Yizi> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(supplier)) {
            queryWrapper.eq("customer_name", supplier);
        }

        queryWrapper.groupBy("district");
        queryWrapper.select("district as name", "count(*) as value");

        return yiziMapper.selectMaps(queryWrapper);
    }


    @GetMapping("/store")
    public List<Map<String, Object>> listStoreBySupplier(@RequestParam(value = "supplier", required = false) String supplier) {
        QueryWrapper<Yizi> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(supplier)) {
            queryWrapper.eq("customer_name", supplier);
        }
        // 指定查询字段
        queryWrapper.select("lng, lat , district");
        // 执行查询，返回键值对集合
        return yiziMapper.selectMaps(queryWrapper);
    }

}
