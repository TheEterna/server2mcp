package com.ai.plug;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @ author 韩
 * time: 2024/2/16 18:06
 */
@RestController
@Tag(name = "UserController",description = "注册功能实现")
public class TestController {


    @Operation(summary = "登录接口 没有进行加盐处理")
    @GetMapping("/test")
    @Parameters({@Parameter(name = "username",description = "usernameDes", in = ParameterIn.COOKIE),
            @Parameter(name = "groupId",description = "groupIdDes", in = ParameterIn.DEFAULT)})
    @Parameter(name = "username",description = "as", in = ParameterIn.DEFAULT)
    public String test(String username) {
        return "666";
    }

}
