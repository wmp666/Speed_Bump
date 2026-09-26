package com.wmp.speedbump.fluent.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.wmp.speedbump.fluent.backdrop.NativeBackdrop;
import com.wmp.speedbump.fluent.backdrop.WindowsRegistry;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Fluent 主题中枢：明暗模式、主题色、基础 Look &amp; Feel 装配与变更广播。
 *
 * <h3>分层</h3>
 * <ul>
 *   <li><b>底座</b>：FlatLaf 提供 {@code JComboBox} / {@code JPopupMenu} / {@code JTextArea} /
 *       {@code JFileChooser} 等「非核心」组件的渲染，避免为每个冷门组件重写 UI 代理。</li>
 *   <li><b>上层</b>：{@code com.wmp.speedbump.fluent.component} 里的自绘组件完全不依赖 FlatLaf，
 *       只读 {@link FluentColors} 令牌。</li>
 * </ul>
 *
 * <p>这样既保住了 Fluent 的视觉一致性，又不用把 Swing 的每个组件都重造一遍。</p>
 */
public final class FluentTheme {

    private FluentTheme() {
    }

    /** 主题模式 */
    public enum Mode {
        LIGHT, DARK, SYSTEM
    }

    private static Mode mode = Mode.LIGHT;
    private static boolean systemDark = false;
    private static boolean installed = false;

    /**
     * 关心主题变化的组件。
     *
     * <h3>为什么是「弱键 + 无值」而不是监听器列表</h3>
     * <p>最初的设计是 {@code addListener(this::repaint)} 把回调塞进一个强引用列表。
     * 这有两个坑：一是组件一旦被销毁，它的回调仍留在列表里（反复开关对话框会持续泄漏）；
     * 二是改成弱引用的回调对象会立刻被 GC——{@code this::repaint} 每次调用都新建一个对象，
     * 除了这份列表没有任何强引用持有它，监听器会「静默失效」。</p>
     *
     * <p>解法是把<b>组件本身</b>当作弱键放进 {@link WeakHashMap}，值用共享常量
     * {@link Boolean#TRUE}。这样值不会反向强引用键，条目能在组件被回收后自动过期。</p>
     */
    public interface ThemeAware {
        /** 主题或强调色变化时被调用（实现里通常只需 {@code repaint()}） */
        void onThemeChanged();
    }

    private static final java.util.Map<ThemeAware, Boolean> THEME_AWARE = new java.util.WeakHashMap<>();

    // ==================================================================
    // 状态查询
    // ==================================================================

    /** 当前是否为深色（SYSTEM 模式下跟随系统） */
    public static boolean isDark() {
        return mode == Mode.DARK || (mode == Mode.SYSTEM && systemDark);
    }

    public static Mode mode() {
        return mode;
    }

    /** 注册一个关心主题变化的组件（弱引用，无需手动注销） */
    public static void register(ThemeAware aware) {
        if (aware == null) {
            return;
        }
        synchronized (THEME_AWARE) {
            THEME_AWARE.put(aware, Boolean.TRUE);
        }
    }

    /** 立即注销（通常不需要，组件被回收时会自动过期） */
    public static void unregister(ThemeAware aware) {
        if (aware == null) {
            return;
        }
        synchronized (THEME_AWARE) {
            THEME_AWARE.remove(aware);
        }
    }

    // ==================================================================
    // 模式切换
    // ==================================================================

    /**
     * 设置主题模式。必须在 EDT 上调用（会重建 UIManager 默认值并刷新所有窗口）。
     */
    public static void setMode(Mode newMode) {
        if (newMode == null || newMode == mode) {
            return;
        }
        mode = newMode;
        if (newMode == Mode.SYSTEM) {
            systemDark = detectSystemDark();
        }
        refresh();
    }

    /** 在浅色 / 深色之间切换（SYSTEM 模式下切到与当前观感相反的一侧） */
    public static void toggle() {
        setMode(isDark() ? Mode.LIGHT : Mode.DARK);
    }

    /** 若处于 SYSTEM 模式且系统主题已变化，则重新应用 */
    public static void syncSystemTheme() {
        if (mode != Mode.SYSTEM) {
            return;
        }
        boolean now = detectSystemDark();
        if (now != systemDark) {
            systemDark = now;
            refresh();
        }
    }

    // ==================================================================
    // 装配
    // ==================================================================

    /**
     * 安装基础 Look &amp; Feel 与默认值。应在创建任何 Swing 组件之前调用一次。
     */
    public static void install() {
        if (installed) {
            return;
        }
        installed = true;
        if (mode == Mode.SYSTEM) {
            systemDark = detectSystemDark();
        }
        // 让 backdrop 层能读到当前明暗而不用反向依赖 theme 包
        NativeBackdrop.setDarkThemeSupplier(FluentTheme::isDark);
        appliedDark = null;
        setupLookAndFeel();
        publishDefaults();
    }

    /** 上一次真正安装 Look &amp; Feel 时所用的明暗（{@code null} 表示还没装过） */
    private static Boolean appliedDark;

    /**
     * 安装 FlatLaf 的浅色或深色 L&amp;F。
     *
     * <p>实测 {@code FlatLaf.setup()} 本身要 1–2 秒（要解析并装载整套 UI 默认值），
     * 而 {@link #refresh()} 原本每次都会调用它——包括「只换了强调色」和
     * {@code setMode(SYSTEM)} 但明暗其实没变的情况，纯属浪费。
     * 这里记住上次安装的明暗，明暗没变就直接跳过。</p>
     */
    private static void setupLookAndFeel() {
        boolean dark = isDark();
        if (appliedDark != null && appliedDark == dark) {
            return;
        }
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
        } catch (Throwable t) {
            // FlatLaf 不可用（未打包进 classpath）时退回系统 L&F，界面仍可运行
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 保持当前 L&F
            }
        }
        appliedDark = dark;
    }

    /**
     * 把 Fluent 令牌喂给 FlatLaf 的 UI 默认值表，让「非核心」组件也随主题走。
     */
    private static void publishDefaults() {
        Color accent = FluentColors.accent();
        Color text = FluentColors.text();

        UIManager.put("Component.accentColor", accent);
        UIManager.put("Component.focusColor", accent);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 1);
        UIManager.put("Component.arc", FluentMetrics.RADIUS_SMALL);
        UIManager.put("Component.focusCellOutlineWidth", 0);

        UIManager.put("Button.arc", FluentMetrics.RADIUS_SMALL);
        UIManager.put("TextComponent.arc", FluentMetrics.RADIUS_SMALL);
        UIManager.put("CheckBox.arc", 4);
        UIManager.put("ProgressBar.arc", FluentMetrics.RADIUS_SMALL);

        UIManager.put("ScrollBar.width", FluentMetrics.SCROLLBAR_THICKNESS);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumb", FluentColors.textSecondary());
        UIManager.put("ScrollBar.track", new Color(0, 0, 0, 0));

        UIManager.put("PopupMenu.borderColor", FluentColors.cardStroke());
        UIManager.put("PopupMenu.background", FluentColors.flyout());
        UIManager.put("PopupMenu.foreground", text);
        UIManager.put("MenuItem.selectionBackground", FluentColors.subtleHover());
        UIManager.put("MenuItem.selectionForeground", text);

        UIManager.put("ToolTip.background", FluentColors.flyout());
        UIManager.put("ToolTip.foreground", text);
        UIManager.put("ToolTip.border", new javax.swing.border.EmptyBorder(6, 10, 6, 10));

        UIManager.put("Panel.background", FluentColors.solid());
        UIManager.put("OptionPane.background", FluentColors.flyout());
        UIManager.put("OptionPane.messageForeground", text);

        UIManager.put("TextField.background", FluentColors.control());
        UIManager.put("TextField.foreground", text);
        UIManager.put("TextField.inactiveForeground", FluentColors.textDisabled());
        UIManager.put("TextField.caretForeground", text);
        UIManager.put("TextField.selectionBackground", FluentColors.accentSubtle());
        UIManager.put("TextField.selectionForeground", text);

        UIManager.put("ComboBox.background", FluentColors.control());
        UIManager.put("ComboBox.foreground", text);
        UIManager.put("ComboBox.selectionBackground", FluentColors.subtleHover());
        UIManager.put("ComboBox.selectionForeground", text);
        UIManager.put("ComboBox.buttonBackground", new Color(0, 0, 0, 0));
        UIManager.put("ComboBox.buttonArrowColor", FluentColors.textSecondary());
        UIManager.put("ComboBox.buttonEditableBackground", FluentColors.control());

        UIManager.put("List.background", new Color(0, 0, 0, 0));
        UIManager.put("List.foreground", text);
        UIManager.put("List.selectionBackground", FluentColors.accentSubtle());
        UIManager.put("List.selectionForeground", text);

        UIManager.put("ScrollPane.background", new Color(0, 0, 0, 0));
        UIManager.put("Viewport.background", new Color(0, 0, 0, 0));
    }

    /**
     * 主题变更后刷新：重装 L&amp;F、重发默认值、重画所有窗口、通知监听者。
     */
    public static void refresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(FluentTheme::refresh);
            return;
        }
        setupLookAndFeel();
        publishDefaults();
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            // 标题栏的深色标志位由 DWM 决定，主题切换后需要重新下发
            NativeBackdrop.applyImmersiveDarkMode(w, isDark());
            w.repaint();
        }
        for (Runnable listener : listenersSnapshot()) {
            try {
                listener.run();
            } catch (Throwable ignored) {
                // 单个监听器出错不影响其他监听器
            }
        }
    }

    /** 取一份监听者快照（避免在通知过程中修改表结构） */
    private static List<Runnable> listenersSnapshot() {
        List<ThemeAware> aware;
        synchronized (THEME_AWARE) {
            aware = new java.util.ArrayList<>(THEME_AWARE.keySet());
        }
        List<Runnable> snapshot = new java.util.ArrayList<>(aware.size());
        for (ThemeAware a : aware) {
            if (a != null) {
                snapshot.add(a::onThemeChanged);
            }
        }
        return snapshot;
    }

    /** 主题色变更入口（会触发 {@link #refresh()}） */
    public static void setAccent(Color color) {
        FluentColors.setAccentOverride(color);
        refresh();
    }

    /** 重新向所有窗口下发 DWM 深色标志（窗口刚创建或重建 hwnd 后调用） */
    public static void applyTo(Window window) {
        NativeBackdrop.applyImmersiveDarkMode(window, isDark());
    }

    // ==================================================================
    // 系统主题探测
    // ==================================================================

    /**
     * 读注册表判断系统应用主题是否为深色。
     *
     * <p>{@code AppsUseLightTheme} 为 0 表示深色。非 Windows 或读取失败时返回 {@code false}。</p>
     *
     * <p><b>只走 FFM 直读</b>：早期版本用 {@code reg query} 子进程，实测一次要 11 秒，
     * 而设置页的「跟随系统」会在 EDT 上同步调用本方法——界面会毫无提示地冻结十几秒。
     * 现在通过 {@link WindowsRegistry} 直调 {@code RegGetValueW}，耗时不到 1 毫秒。</p>
     */
    public static boolean detectSystemDark() {
        int value = WindowsRegistry.readInt(WindowsRegistry.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "AppsUseLightTheme");
        return value == 0;
    }

    /** 便捷：组件重画请求（主题监听器里常用） */
    public static void repaint(JComponent component) {
        if (component != null) {
            component.repaint();
        }
    }
}
