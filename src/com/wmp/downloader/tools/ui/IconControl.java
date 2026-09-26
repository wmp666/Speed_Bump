package com.wmp.downloader.tools.ui;

import com.wmp.downloader.tools.file.DataControl;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class IconControl {
    private static final Logger logger = Logger.getLogger(IconControl.class);

    private static final List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();

    private static final Properties iconProperties = new Properties();

    static{
        try (var is = IconControl.class.getResourceAsStream("/com/wmp/downloader/tools/ui/icons.properties")) {
            if (is == null) {
                logger.error("加载失败： icons.properties 未找到");
            } else {
                iconProperties.load(is);
            }
        } catch (IOException e) {
            logger.error("加载失败： icons.properties", e);
        }
    }


    public static ImageIcon getIcon(String key) {
        logger.info("正在获取" + key + "的对应图标");
        var iconPath = iconProperties.getProperty(key, "/com/wmp/speed_bump/common/background/resource/icon/%theme_type%/12-misc/circle.png");
        iconPath = iconPath.replace("%theme_type%", DataControl.get("theme_type", "light"));
        logger.info(iconPath);

        URL iconUrl = IconControl.class.getResource(iconPath);
        if (iconUrl == null) {
            logger.error("图标资源未找到：" + iconPath + "（已跳过，使用空白占位图）");
            return new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        }
        return new ImageIcon(iconUrl);
    }

    public static Image getImage(String key) {
        return getIcon(key).getImage();
    }

    public static ImageIcon getIcon(String key, int size) {
        return getIcon(key, size, size);
    }

    public static Image getImage(String key, int size) {
        return getImage(key, size, size);
    }

    public static ImageIcon getIcon(String key, int weight, int height) {
        return new ImageIcon(getIcon(key).getImage().getScaledInstance(weight, height, Image.SCALE_SMOOTH));
    }

    public static Image getImage(String key, int weight, int height) {
        return getIcon(key).getImage().getScaledInstance(weight, height, Image.SCALE_SMOOTH);
    }

    /**
     * 添加动态转换图标任务
     *
     * @param tasks 图标转换任务，将设置图标的代码写在此处
     */
    public static DynamicConverterTask[] addInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.addAll(List.of(tasks));
        for (var task : tasks) {
            try {
                task.task();
            } catch (Exception _) {
            }
        }
        return tasks;
    }

    public static void removeInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.removeAll(List.of(tasks));
    }


    /**
     * 运行动态转换图标任务
     */
    public static void runDynamicConverters() {
        for (var task : dynamicConverterTasks) {
            try {
                task.task();
            } catch (Exception e) {
                logger.error("图标刷新失败", e);
            }
        }
    }
}
