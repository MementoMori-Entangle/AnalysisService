package com.entangle.analysis.util;

import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component("templateImageInfoUtil")
public class TemplateImageInfoUtil {
    public Map<String, Object> getInfo(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            Map<String, Object> info = new HashMap<>();
            info.put("size", file.length());
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                info.put("width", img.getWidth());
                info.put("height", img.getHeight());
            } else {
                info.put("width", "?");
                info.put("height", "?");
            }
            return info;
        } catch (Exception e) {
            return null;
        }
    }
}
