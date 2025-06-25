package com.ai.plug.test.test.controller;

import com.ai.plug.autoconfigure.annotation.ToolNotScanForAuto;
import com.ai.plug.test.test.PoiData;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ToolNotScanForAuto
@RestController
public class AutoNotScanController {

    @GetMapping("/api/tool/auto_not_scan")
    @ApiOperation("tool不扫描")
    public List<PoiData> getPoiData() {
        return List.of(
                new PoiData("tool不扫描", 121.455415, 31.249743, "日杂店", 128)
        );
    }
}
