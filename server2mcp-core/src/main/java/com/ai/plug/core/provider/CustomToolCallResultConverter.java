package com.ai.plug.core.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Base64;


/**
 * @author 韩
 * time: 2025/5/24 17:00
 */
@Slf4j
public class CustomToolCallResultConverter implements ToolCallResultConverter {
    @Override
    public String convert(Object result, Type returnType) {
        if (returnType == Void.TYPE) {
            log.debug("The tool has no return type. Converting to conventional response.");
            return JsonParser.toJson("Done");
        }
        if (result instanceof RenderedImage) {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream(1024 * 4);
            try {
                ImageIO.write((RenderedImage) result, "PNG", buf);
            }
            catch (IOException e) {
                return "Failed to convert tool result to a base64 image: " + e.getMessage();
            }
            return Base64.getEncoder().encodeToString(buf.toByteArray());
        }
        else {
            log.debug("Converting tool result to JSON.");
            return JsonParser.toJson(result);
        }

    }
}
