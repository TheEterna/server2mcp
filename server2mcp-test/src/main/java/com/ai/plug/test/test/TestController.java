package com.ai.plug.test.test;

import com.logaritex.mcp.annotation.McpArg;
import com.logaritex.mcp.annotation.McpComplete;
import com.logaritex.mcp.annotation.McpPrompt;
import com.logaritex.mcp.annotation.McpResource;
import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.annotations.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * @author 韩
 * time: 2024/2/16 18:06
 */
@RestController
//@Tag(name = "UserController",description = "注册功能实现")
@Api(tags="用户模块")
public class TestController {

    private final Map<String, List<String>> cityDatabase = new HashMap<>();

    private final Map<String, List<String>> countryDatabase = new HashMap<>();

    private final Map<String, List<String>> languageDatabase = new HashMap<>();

    public TestController() {
        // Initialize with some sample data
        cityDatabase.put("a", List.of("Amsterdam", "Athens", "Atlanta", "Austin"));
        cityDatabase.put("b", List.of("Barcelona", "Berlin", "Boston", "Brussels"));
        cityDatabase.put("c", List.of("Cairo", "Calgary", "Cape Town", "Chicago"));
        cityDatabase.put("l", List.of("Lagos", "Lima", "Lisbon", "London", "Los Angeles"));
        cityDatabase.put("n", List.of("Nairobi", "Nashville", "New Delhi", "New York"));
        cityDatabase.put("p", List.of("Paris", "Perth", "Phoenix", "Prague"));
        cityDatabase.put("s",
                List.of("San Francisco", "Santiago", "Seattle", "Seoul", "Shanghai", "Singapore", "Sydney"));
        cityDatabase.put("t", List.of("Taipei", "Tokyo", "Toronto"));

        countryDatabase.put("a", List.of("Afghanistan", "Albania", "Algeria", "Argentina", "Australia", "Austria"));
        countryDatabase.put("b", List.of("Bahamas", "Belgium", "Brazil", "Bulgaria"));
        countryDatabase.put("c", List.of("Canada", "Chile", "China", "Colombia", "Croatia"));
        countryDatabase.put("f", List.of("Finland", "France"));
        countryDatabase.put("g", List.of("Germany", "Greece"));
        countryDatabase.put("i", List.of("Iceland", "India", "Indonesia", "Ireland", "Italy"));
        countryDatabase.put("j", List.of("Japan"));
        countryDatabase.put("u", List.of("Uganda", "Ukraine", "United Kingdom", "United States"));

        languageDatabase.put("e", List.of("English"));
        languageDatabase.put("f", List.of("French"));
        languageDatabase.put(" ", List.of("German"));
        languageDatabase.put("i", List.of("Italian"));
        languageDatabase.put("j", List.of("Japanese"));
        languageDatabase.put("m", List.of("Mandarin"));
        languageDatabase.put("p", List.of("Portuguese"));
        languageDatabase.put("r", List.of("Russian"));
        languageDatabase.put("s", List.of("Spanish", "Swedish"));
    }
    @McpResource(uri = "user-status://{username}",
            name = "User Status",
            description = "Provides the current status for a specific user")
    public String getUserStatus(String username) {
        return "OK";
    }


    @McpComplete(prompt = "personalized-message")
    public McpSchema.CompleteResult completeCountryName(McpSchema.CompleteRequest request) {
        String prefix = request.argument().value().toLowerCase();
        if (prefix.isEmpty()) {
            return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(List.of("Enter a country name"), 1, false));
        }

        String firstLetter = prefix.substring(0, 1);
        List<String> countries = countryDatabase.getOrDefault(firstLetter, List.of());

        List<String> matches = countries.stream()
                .filter(country -> country.toLowerCase().startsWith(prefix))
                .toList();

        return new McpSchema.CompleteResult(new McpSchema.CompleteResult.CompleteCompletion(matches, matches.size(), false));
    }
    @McpPrompt(name = "personalized-message",
            description = "Generates a personalized message based on user information")
    public McpSchema.GetPromptResult personalizedMessage(
            @McpArg(name = "name", description = "The user's name", required = true) String name,
            @McpArg(name = "age", description = "The user's age", required = true) Integer age,
            @McpArg(name = "interests", description = "The user's interests", required = true) String interests) {



        StringBuilder message = new StringBuilder();
        message.append("Hello, ").append(name).append("!\n\n");

        if (age != null) {
            message.append("At ").append(age).append(" years old, you have ");
            if (age < 30) {
                message.append("so much ahead of you.\n\n");
            }
            else if (age < 60) {
                message.append("gained valuable life experience.\n\n");
            }
            else {
                message.append("accumulated wisdom to share with others.\n\n");
            }
        }

        if (interests != null && !interests.isEmpty()) {
            message.append("Your interest in ")
                    .append(interests)
                    .append(" shows your curiosity and passion for learning.\n\n");
        }

        message
                .append("I'm here to assist you with any questions you might have about the Model Context Protocol.");

        return new McpSchema.GetPromptResult("Personalized Message",
                List.of(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(message.toString()))));
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
    @GetMapping("/test")
    public Mono<String> test() {
        return Mono.just("OK");
    }
    @GetMapping("/com/ai/plug/test")
    @ApiOperation("普通测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name="filePath", value="读取文件路径")
    })
    public Object test(@RequestParam(value = "filePath", defaultValue = "image.jpg") String filePath) {

        return Map.of("num", "111");
    }

    // 预定义的真实上海超市数据（名称、地址、经纬度）
    private static final List<Map<String, Object>> REAL_SUPERMARKETS = Arrays.asList(
            createSupermarket("大润发杨浦店", "上海市杨浦区黄兴路2228号", 31.301572, 121.520087),
            createSupermarket("家乐福古北店", "上海市长宁区水城南路268号", 31.216111, 121.416389),
            createSupermarket("沃尔玛南浦大桥店", "上海市浦东新区浦东南路2101号", 31.205722, 121.507778),
            createSupermarket("世纪联华徐汇店", "上海市徐汇区宜山路455号", 31.191389, 121.433611),
            createSupermarket("盒马鲜生大宁店", "上海市静安区万荣路777号", 31.264444, 121.454167),
            createSupermarket("永辉超市张江店", "上海市浦东新区博云路2号", 31.201389, 121.591111),
            createSupermarket("华润万家静安店", "上海市静安区南京西路1618号", 31.223889, 121.455),
            createSupermarket("山姆会员商店青浦店", "上海市青浦区赵巷镇嘉松中路5555号", 31.157778, 121.1975),
            createSupermarket("Costco闵行店", "上海市闵行区朱建路235号", 31.125, 121.391111),
            createSupermarket("联华超市淮海店", "上海市黄浦区淮海中路651号", 31.220278, 121.470278)
    );

    private static Map<String, Object> createSupermarket(String name, String address, double lat, double lng) {
        Map<String, Object> supermarket = new HashMap<>();
        supermarket.put("name", name);
        supermarket.put("address", address);
        supermarket.put("latitude", lat);
        supermarket.put("longitude", lng);
        return supermarket;
    }

    @GetMapping("/oreo-sales/shanghai")
    @ApiOperation("获取上海超市的奥利奥(OREO)销量")
    public List<Map<String, Object>> getShanghaiOreoSales() {
        List<Map<String, Object>> salesList = new ArrayList<>();
        Random random = new Random();

        // 使用真实超市数据生成10条模拟销量记录
        for (Map<String, Object> supermarket : REAL_SUPERMARKETS) {
            Map<String, Object> saleItem = new LinkedHashMap<>();
            saleItem.put("supermarketName", supermarket.get("name"));
            saleItem.put("address", supermarket.get("address"));
            saleItem.put("location", new LinkedHashMap<String, Double>() {{
                put("latitude", (Double) supermarket.get("latitude"));
                put("longitude", (Double) supermarket.get("longitude"));
            }});
            saleItem.put("oreoSales", random.nextInt(500) + 100); // 销量范围100-599
            saleItem.put("reportDate", "2025-06-" + String.format("%02d", random.nextInt(15) + 1));
            saleItem.put("categoryRank", random.nextInt(10) + 1); // 品类排名1-10
            salesList.add(saleItem);
        }

        return salesList;
    }
}
