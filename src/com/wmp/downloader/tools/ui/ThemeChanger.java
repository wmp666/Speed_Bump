package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.swingx.FlatSwingXDefaultsAddon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import org.apache.log4j.Logger;

import javax.swing.*;
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

    /**
     * 界面上给用户选的「三档主题」。
     *
     * <p>{@link #easyChanger} 其实认识十套外观（还有 {@code Darcula} / {@code IntelliJ} /
     * {@code Metal} / {@code Windows Classic} / {@code System}），但全摊在下拉里对普通用户是负担。
     * 所以界面只给三档，配置文件里仍然可以写任意一套——{@link #of(String)} 会把它们
     * 归到最近的一档显示，并<b>不会</b>因此改写配置：使用方按「选中档 == 当前主题所在档」
     * 判断后直接跳过保存即可（{@code SettingsPanel} 就是这么做的）。</p>
     *
     * <p>{@link #toString()} 是渲染时现取的翻译，所以切语言后下拉项会自己跟着变，
     * 不需要重建列表。</p>
     */
    public enum ThemeChoice {

        /** 跟随系统深浅色 */
        SYSTEM("theme.choice.system", "System Theme Style"),
        LIGHT("theme.choice.light", "Mac Light"),
        DARK("theme.choice.dark", "Mac Dark");

        private final String labelKey;
        private final String themeName;

        ThemeChoice(String labelKey, String themeName) {
            this.labelKey = labelKey;
            this.themeName = themeName;
        }

        /** 写进配置的主题名 */
        public String themeName() {
            return themeName;
        }

        /**
         * 把配置里的任意主题名归到三档之一。
         *
         * <p>认不出来的（{@code Metal} / {@code Windows Classic} / {@code System} 等）
         * 一律当作浅色——它们要么本来就是浅色外观，要么跟着系统走，
         * 归到浅色比归到深色更不容易看错。</p>
         */
        public static ThemeChoice of(String theme) {
            return switch (theme == null ? "" : theme) {
                case "System Theme Style" -> SYSTEM;
                case "Mac Dark", "Dark", "Darcula" -> DARK;
                default -> LIGHT;
            };
        }

        @Override
        public String toString() {
            return StringFormat.translate(labelKey);
        }
    }

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
        StringFormat.refreshLocal();

        for (var window : JWindow.getOwnerlessWindows()) {
            try {
                SwingUtilities.updateComponentTreeUI(window);
            } catch (Exception e) {
                logger.error("窗口刷新失败", e);
            }
        }

        try {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        } catch (Exception _) {

        }
    }


    private static void ThemeRefresh(Object newTheme, boolean isUseSnapshot){
        ThemeRefresh(newTheme, isUseSnapshot, false);
    }

    private static void ThemeRefresh(Object newTheme, boolean isUseSnapshot, boolean forcedRefresh) {
        //主题更新，如果前后主题相同就跳过
        if (!forcedRefresh && isSameTheme(newTheme)) return;

        DataControl.refresh();

        if (isUseSnapshot) FlatAnimatedLafChange.showSnapshot();

        if (newTheme instanceof LookAndFeel laf)
            FlatLaf.setup(laf);
        else if (newTheme instanceof String className) {
            try {
                UIManager.setLookAndFeel(className);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        //主体部分数据更新
        UIManager.put("FlatLaf.addon.swingx", new FlatSwingXDefaultsAddon());
        UIManager.put("TabbedPane.tabsOpaque", true);
        UIManager.put("TabbedPane.contentOpaque", false);
        System.setProperty("flatlaf.useFullWindowContent", "true");
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
        //部分内置图标刷新
        var icon = UIManager.getIcon("OptionPane.informationIcon");

        UIManager.put("OptionPane.errorIcon", IconControl.getIcon("error", icon.getIconWidth(), icon.getIconHeight()));
        UIManager.put("OptionPane.informationIcon", IconControl.getIcon("info", icon.getIconWidth(), icon.getIconHeight()));
        UIManager.put("OptionPane.warningIcon", IconControl.getIcon("warn", icon.getIconWidth(), icon.getIconHeight()));
        UIManager.put("OptionPane.questionIcon", IconControl.getIcon("question", icon.getIconWidth(), icon.getIconHeight()));

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
        easyChanger(DataControl.get("theme", "Mac Light"));
    }

    public static void easyThemeRefresh() {
        var newTheme = DataControl.get("theme", "Mac Light");
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
