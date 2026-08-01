package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.EasterEggData;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ThemeChanger {

    private static final List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();

    private static String theme = "";

    private static final Timer timer = new Timer(1000, e -> easyChanger());

    static {
        timer.start();
    }

    /**
     * 动态转换部分组件在不同主题下的状态
     *
     * @param tasks 动态转换任务
     */

    public static DynamicConverterTask[] addInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.addAll(List.of(tasks));
        for (var t : tasks)
            try {
                t.task();
            } catch (Exception e) {
            }
        return tasks;
    }

    public static void removeDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.removeAll(List.of(tasks));
    }


    /**
     * 运行动态转换部分组件在不同主题下的状态
     */
    public static void runDynamicConverters() {
        for (var task : dynamicConverterTasks) task.task();
    }

    private static void changer(Object newTheme) {
        if (newTheme == null)
            return;
        DataControl.refresh();

        FlatAnimatedLafChange.showSnapshot();


        //主题更新
            if (!EasterEggData.canUseFlatLaf) {


                if (!(newTheme instanceof FlatLaf)) {
                    if (newTheme instanceof LookAndFeel laf)
                        FlatLaf.setup(laf);
                    else if (newTheme instanceof String className) {
                        try {
                            UIManager.setLookAndFeel(className);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                } else {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                if (newTheme instanceof LookAndFeel laf)
                    FlatLaf.setup(laf);
                else if (newTheme instanceof String className) {
                    try {
                        UIManager.setLookAndFeel(className);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        //主体部分数据更新
        UIManager.put("TabbedPane.tabsOpaque", false);
        UIManager.put("TabbedPane.contentOpaque", false);
        FlatLaf.setUseNativeWindowDecorations(true);
        //UIManager.put("TitlePane.unifiedBackground", true);
        //UIManager.put("TitlePane.unifiedBackground", false);


        //组件更新
        runDynamicConverters();

        //图标更新
        IconControl.runDynamicConverters();

        //字体更新
        UIManager.put("defaultFont", new Font(DataControl.get("Font", "Microsoft YaHei"), Font.PLAIN, DataControl.get("FontSize", 12)));

        for (var window : JWindow.getOwnerlessWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }

        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }

    /**
     * 简单主题切换
     */
    public static void easyChanger() {
        easyChanger(DataControl.get("theme", "System Theme Style"));
    }

    /**
     * 简单主题切换
     *
     * @param newTheme 新主题
     */
    public static void easyChanger(String newTheme) {
        if (newTheme.equals("System Theme Style")) {
            newTheme = SystemThemeDetector.isDarkMode() ? "Mac Dark" : "Mac Light";
        }
        if(newTheme.equals(theme)) return;
        theme = newTheme;
        changer(switch (newTheme) {
            case "Mac Dark" -> new FlatMacDarkLaf();
            case "Mac Light" -> new FlatMacLightLaf();
            case "Dark" -> new FlatDarkLaf();
            case "Light" -> new FlatLightLaf();
            case "Darcula" -> new FlatDarculaLaf();
            case "IntelliJ" -> new FlatIntelliJLaf();
            case "System" -> UIManager.getSystemLookAndFeelClassName();
            case "Windows Classic" -> "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
            case "Metal" -> "javax.swing.plaf.metal.MetalLookAndFeel";
            default -> null;
        });
    }
}
