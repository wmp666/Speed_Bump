package com.wmp.downloader.tools.ui;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.EasterEggData;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class IconControl {
    private static final Logger logger = Logger.getLogger(IconControl.class);

    private static final List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();


    public static ImageIcon getIcon(String key) {


        Properties iconProperties = new Properties();
        try {
            iconProperties.load(IconControl.class.getResourceAsStream("/com/wmp/downloader/tools/ui/icons.properties"));
        } catch (IOException e) {
            logger.error("加载失败： icons.properties", e);
        }


        var iconPath = iconProperties.getProperty(key, "/icon/%theme_type%/12-misc/circle.png");
        iconPath = iconPath.replace("%theme_type%", DataControl.get("theme_type", "light"));
        return new ImageIcon(IconControl.class.getResource(iconPath));
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
        if (!EasterEggData.canUseIcon)
            return new ImageIcon(getIcon("null").getImage().getScaledInstance(1, 1, Image.SCALE_SMOOTH));
        else
            return new ImageIcon(getIcon(key).getImage().getScaledInstance(weight, height, Image.SCALE_SMOOTH));
    }

    public static Image getImage(String key, int weight, int height) {
        if (!EasterEggData.canUseIcon)
            return getIcon("null").getImage().getScaledInstance(1, 1, Image.SCALE_SMOOTH);
        else
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
