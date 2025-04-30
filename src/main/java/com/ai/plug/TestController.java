package com.ai.plug;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @ author 韩
 * time: 2024/2/16 18:06
 */
@RestController
//@Tag(name = "UserController",description = "注册功能实现")
@Api(tags="用户模块")
public class TestController {



    @GetMapping("/test")
    @ApiOperation("获取用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType="body", name="dto", dataType="Object", required=true, value="用户Id")
    })
    public String test(@RequestBody Dto dto) {
//    public String test(String dto) {
        return "666";
    }
    @GetMapping("/test1")
    @ApiOperation("获取用户信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name="id", value="用户Id")
    })
    public String test1(@RequestParam String id) {
//    public String test(String dto) {
        return "666";
    }

//
//    @Operation(summary = "登录接口 没有进行加盐处理")
//    @GetMapping("/test")
//    @Parameters({@Parameter(name = "username",description = "usernameDes", in = ParameterIn.COOKIE),
//            @Parameter(name = "groupId",description = "groupIdDes", in = ParameterIn.DEFAULT)})
//    public String test(Dto dto) {
//        return "666";
//    }
//



}
