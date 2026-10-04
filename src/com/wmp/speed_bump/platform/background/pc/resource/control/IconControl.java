package com.wmp.speed_bump.platform.background.pc.resource.control;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tools.SBLogger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

public class IconControl implements com.wmp.speed_bump.common.background.tools.resource.control.IconControl<ImageIcon> {
    private static final SBLogger logger = SBLogger.getLogger(IconControl.class);

    private static final Properties iconProperties = new Properties();

    static{
        try (var is = IconControl.class.getResourceAsStream("/com/wmp/speed_bump/common/background/resource/icons.properties")) {
            if (is == null) {
                logger.error("加载失败： icons.properties 未找到");
            } else {
                iconProperties.load(is);
            }
        } catch (IOException e) {
            logger.error("加载失败： icons.properties", e);
        }
    }

    public ImageIcon getIcon(String key) {
        return getIcon(DataControl.get("theme_type", "light"), key);
    }

    public ImageIcon getIcon(String type, String key) {
        logger.info("正在获取" + key + "的对应图标");
        var iconPath = iconProperties.getProperty(key, "/com/wmp/speed_bump/common/background/resource/icon/%theme_type%/12-misc/circle.png");
        iconPath = iconPath.replace("%theme_type%", type);
        logger.info(iconPath);

        URL iconUrl = IconControl.class.getResource(iconPath);
        if (iconUrl == null) {
            logger.error("图标资源未找到：" + iconPath + "（已跳过，使用空白占位图）");
            return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        }
        return new ImageIcon(iconUrl);
    }

    public ImageIcon getIcon(String key, int size) {
        return getIcon(key, size, size);
    }

    public ImageIcon getIcon(String key, int weight, int height) {
        return new ImageIcon(getIcon(key).getImage().getScaledInstance(weight, height, Image.SCALE_SMOOTH));
    }

}
