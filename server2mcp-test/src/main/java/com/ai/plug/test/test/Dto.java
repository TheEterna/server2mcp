package com.ai.plug.test.test;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
@Schema(description = "我是对象 666")
//@ApiModel
public class Dto {
//    @ApiModelProperty
    @ToolParam(description = "这个是id")
    private Integer id;
    @Schema(title = "666", description = "牛逼")
    private String score;

    public Dto(Integer id, String score) {
        this.id = id;
        this.score = score;
    }
}