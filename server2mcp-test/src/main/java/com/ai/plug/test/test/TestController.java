package com.ai.plug.test.test;

import com.logaritex.mcp.annotation.McpResource;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;


/**
 * @author 韩
 * time: 2024/2/16 18:06
 */
@RestController
//@Tag(name = "UserController",description = "注册功能实现")
@Api(tags="用户模块")
public class TestController {


    @McpResource(uri = "user-status://{username}",
            name = "User Status",
            description = "Provides the current status for a specific user")
    public String getUserStatus(String username) {
        return "OK";
    }

    @GetMapping("/test2")
    @ApiOperation("读取JPG文件, 返回图片")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath", value="读取文件路径")
    })
//    @Tool(name = "testTool")
    public RenderedImage readJpegImage(@RequestParam(value = "filePath", defaultValue = "image.jpg") String filePath) {
        try {
            // 创建一个 File 对象，表示要读取的图像文件
            File file = new File(filePath);
            if (file.exists()) {
                System.out.println("文件存在");
            } else {
                System.out.println("文件不存在，请检查路径：" + filePath);
            }
            // 使用 ImageIO.read 方法读取图像文件并将其转换为 RenderedImage 对象
            return ImageIO.read(file);
        } catch (IOException e) {
            // 处理读取文件时可能出现的异常
            System.err.println("读取图像文件时出错: " + e.getMessage());
            return null;
        }
    }
    @GetMapping("/com/ai/plug/test")
    @ApiOperation("普通测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath", value="读取文件路径")
    })
    public Object test(@RequestParam(value = "filePath", defaultValue = "image.jpg") String filePath) {

        return Map.of("num", "111");
    }
}
