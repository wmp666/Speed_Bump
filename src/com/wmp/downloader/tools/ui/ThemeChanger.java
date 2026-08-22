package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.swingx.FlatSwingXDefaultsAddon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.EasterEggData;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXColorSelectionButton;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThemeChanger {

    private static final List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(ThemeChanger.class);
    private static final Timer timer = new Timer(1000, e -> easyThemeRefresh());
    private static String theme = "";
    private static String themeStyle = "";

    static {
        timer.start();

        //执行仅需执行一次的UI相关操作
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

        //颜色更新
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#" + DataControl.get("accent_color", "05E666")));

        ThemeRefresh(newTheme, false, true);


        //字体更新
        UIManager.put("defaultFont", new Font(DataControl.get("Font", "Microsoft YaHei"), Font.PLAIN, DataControl.get("FontSize", 12)));

        for (var window : JWindow.getOwnerlessWindows()) {
            try {
                SwingUtilities.updateComponentTreeUI(window);
            } catch (Exception e) {
                logger.error("窗口刷新失败", e);
            }
        }

        FlatAnimatedLafChange.hideSnapshotWithAnimation();
    }


    private static void ThemeRefresh(Object newTheme, boolean isUseSnapshot){
        ThemeRefresh(newTheme, isUseSnapshot, false);
    }

    private static void ThemeRefresh(Object newTheme, boolean isUseSnapshot, boolean forcedRefresh) {
        //主题更新，如果前后主题相同就跳过
        if (!forcedRefresh && isSameTheme(newTheme)) return;

        if (isUseSnapshot) FlatAnimatedLafChange.showSnapshot();

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
        UIManager.put("FlatLaf.addon.swingx", new FlatSwingXDefaultsAddon());
        UIManager.put("TabbedPane.tabsOpaque", false);
        //UIManager.put("TabbedPane.background", new Color(0, 0, 0, 100));
        UIManager.put("TabbedPane.contentOpaque", false);
        FlatLaf.setUseNativeWindowDecorations(true);

        int arc = DataControl.get("is_use_square_component", true)?0:10;
        UIManager.put("Button.arc", arc);
        UIManager.put("Component.arc", arc);   // 影响 ComboBox, Spinner 等
        UIManager.put("CheckBox.arc", arc);
        UIManager.put("ProgressBar.arc", arc);
        UIManager.put("TextComponent.arc", arc);


        //组件更新
        runDynamicConverters();

        //图标更新
        IconControl.runDynamicConverters();

        if (isUseSnapshot) {
            for (var window : JWindow.getOwnerlessWindows()) {
                try {
                    SwingUtilities.updateComponentTreeUI(window);
                } catch (Exception e) {
                    logger.error("窗口刷新失败", e);
                }
            }

            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    private static boolean isSameTheme(Object newTheme) {
        if (newTheme instanceof LookAndFeel lookAndFeel) {
            if (!themeStyle.equals("LookAndFeel")) {
                themeStyle = "LookAndFeel";
                return false;
            } else {
                var newThemeStr = lookAndFeel.getClass().getName();
                if (newThemeStr.equals(theme)) {
                    return true;
                } else {
                    theme = newThemeStr;
                    return false;
                }
            }
        } else if (newTheme instanceof String string) {
            if (!themeStyle.equals("String")) {
                themeStyle = "String";
                return false;
            } else {
                if (string.equals(theme)) {
                    return true;
                } else {
                    theme = string;
                    return false;
                }
            }
        } else {
            var typeName = newTheme.getClass().getTypeName();
            if (!themeStyle.equals(typeName)) {
                themeStyle = typeName;
                return false;
            } else {
                var newThemeStr = newTheme.toString();
                if (newThemeStr.equals(theme)) {
                    return true;
                } else {
                    theme = newThemeStr;
                    return false;
                }
            }
        }
    }

    /**
     * 简单主题切换
     */
    public static void easyChanger() {
        easyChanger(DataControl.get("theme", "System Theme Style"));
    }

    public static void easyThemeRefresh() {
        var newTheme = DataControl.get("theme", "System Theme Style");
        if (newTheme.equals("System Theme Style")) {
            newTheme = SystemThemeDetector.isDarkMode() ? "Mac Dark" : "Mac Light";
        }


        ThemeRefresh(switch (newTheme) {
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
        }, true);


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
