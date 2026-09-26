package com.wmp.downloader.tools.ui.fluent;

import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.ThemeChanger;

import javax.swing.UIManager;

/**
 * Fluent 界面增强的统一安装入口。
 *
 * <h3>它做了什么</h3>
 * <ol>
 *   <li>把 {@code ScrollBarUI} 换成 {@link FluentScrollBarUI}（悬浮细滚动条）；</li>
 *   <li>把 {@code ProgressBarUI} 换成 {@link FluentProgressBarUI}（不确定态扫动动画）；</li>
 *   <li>把 {@code CheckBoxUI} 换成 {@link FluentSwitchUI}（WinUI 开关外观，
 *       表格 Boolean 渲染器自动回退为传统复选框）；</li>
 *   <li>挂接主题回调 {@link FluentThemeSupport}，让自绘组件跟随主题刷新。</li>
 * </ol>
 *
 * <h3>为什么必须在每次主题切换后重新灌入</h3>
 * <p>{@code FlatLaf.setup()} 会<b>重建整张 {@code UIManager} 默认值表</b>，
 * 我们塞进去的 {@code ScrollBarUI}/{@code ProgressBarUI}/{@code CheckBoxUI}
 * 会被一并抹掉。所以这里把「灌入默认值」注册成一个
 * {@link DynamicConverterTask}——本项目的 {@link ThemeChanger} 在每次主题刷新时
 * 都会调用它，而且时机正好在 {@code FlatLaf.setup()} 之后、
 * {@code updateComponentTreeUI} 之前，是唯一正确的窗口期。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 启动时（建议紧跟在设置外观之前）调用一次
 * FluentUi.install();
 * }</pre>
 */
public final class FluentUi {

    private FluentUi() {
    }

    private static boolean installed;

    /** 是否启用 WinUI 开关外观（关闭后复选框恢复传统方块） */
    private static boolean switchStyleEnabled = true;

    /** 是否启用悬浮细滚动条 */
    private static boolean fluentScrollBarEnabled = true;

    /** 是否启用 Fluent 进度条 */
    private static boolean fluentProgressBarEnabled = true;

    /** 是否给普通按钮安装揭示高亮光晕（仅 FlatLaf 下生效） */
    private static boolean revealForButtons = true;

    // ==================================================================
    // 安装
    // ==================================================================

    /** 安装全部 Fluent 界面增强。可重复调用。 */
    public static void install() {
        if (installed) {
            applyUiDefaults();
            return;
        }
        installed = true;

        // 1) 让自绘组件跟随主题
        FluentThemeSupport.install();

        // 2) 先灌一次默认值（此时可能还没有 L&F，但新组件的 updateUI 会立刻用上）
        applyUiDefaults();

        // 3) 注册成动态转换任务：每次 FlatLaf.setup() 重建默认值表后重新灌入
        try {
            ThemeChanger.addInDynamicConverter(FluentUi::applyUiDefaults);
        } catch (Throwable ignored) {
            // ThemeChanger 不可用时（例如单元测试）至少上面的直接调用已经生效
        }
    }

    /**
     * 把 Fluent 的 UI 类写进 {@code UIManager} 默认值。
     *
     * <p>必须在 {@code FlatLaf.setup()} 之后调用；由 {@link ThemeChanger}
     * 在每个主题刷新周期里自动调用。</p>
     */
    public static void applyUiDefaults() {
        try {
            apply("ScrollBarUI", FluentScrollBarUI.class.getName(), fluentScrollBarEnabled);
            if (fluentScrollBarEnabled) {
                UIManager.put("ScrollBar.showButtons", Boolean.FALSE);
            }

            apply("ProgressBarUI", FluentProgressBarUI.class.getName(), fluentProgressBarEnabled);
            apply("CheckBoxUI", FluentSwitchUI.class.getName(), switchStyleEnabled);

            // 按钮光晕只在 FlatLaf 下安装：RevealButtonUI 继承自 FlatButtonUI，
            // 换成 System / Metal / Windows Classic 外观后它读不到 FlatLaf 的默认值，
            // 轻则外观怪异、重则抛异常。宁可这类主题下没有光晕，也不能让按钮坏掉。
            apply("ButtonUI", RevealButtonUI.class.getName(), revealForButtons && isFlatLaf());
        } catch (Throwable ignored) {
            // 默认值写入失败不应影响启动
        }
    }

    /**
     * 安装或还原文案类 UI 默认值。
     *
     * <p>关闭某项时<b>不能简单地 {@code UIManager.remove(key)}</b>：
     * {@code UIManager.getDefaults()} 是「L&amp;F 默认值 + 用户覆盖」的合并表，
     * 删掉键会把 Look &amp; Feel 自己的实现一起删掉，之后
     * {@code UIDefaults.getUI()} 找不到类会返回 {@code null}，
     * {@code JButton.updateUI()} 里 {@code setUI(null)} 直接让按钮崩掉。
     * 正确做法是从 {@code getLookAndFeelDefaults()} 取回 L&amp;F 的原始值写回去。</p>
     *
     * @param key          默认值键（如 {@code "ButtonUI"}）
     * @param ourClassName 我们的实现类名
     * @param enable       是否启用我们的实现
     */
    private static void apply(String key, String ourClassName, boolean enable) {
        Object current = UIManager.get(key);
        boolean currentlyOurs = ourClassName.equals(current);
        if (enable) {
            UIManager.put(key, ourClassName);
        } else if (currentlyOurs) {
            Object lafValue = UIManager.getLookAndFeelDefaults().get(key);
            if (lafValue != null) {
                UIManager.put(key, lafValue);
            }
        }
    }

    /** 当前外观是否为 FlatLaf 系（Fluent 自绘代理的适用前提） */
    public static boolean isFlatLaf() {
        try {
            return UIManager.getLookAndFeel() instanceof com.formdev.flatlaf.FlatLaf;
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================================================================
    // 单项开关
    // ==================================================================

    public static boolean isInstalled() {
        return installed;
    }

    /** 揭示高亮光晕 */
    public static void setRevealEnabled(boolean enabled) {
        RevealEngine.setEnabled(enabled);
    }

    public static boolean isRevealEnabled() {
        return RevealEngine.isEnabled();
    }

    /**
     * 开关外观。
     *
     * <p>关闭后需要刷新界面才会生效（{@code SwingUtilities.updateComponentTreeUI}），
     * 调用 {@link #refreshUI()} 即可。</p>
     */
    public static void setSwitchStyleEnabled(boolean enabled) {
        switchStyleEnabled = enabled;
        applyUiDefaults();
    }

    public static boolean isSwitchStyleEnabled() {
        return switchStyleEnabled;
    }

    public static void setFluentScrollBarEnabled(boolean enabled) {
        fluentScrollBarEnabled = enabled;
        applyUiDefaults();
    }

    public static void setFluentProgressBarEnabled(boolean enabled) {
        fluentProgressBarEnabled = enabled;
        applyUiDefaults();
    }

    /** 刷新所有已显示窗口的 UI（切换单项开关后调用） */
    public static void refreshUI() {
        for (java.awt.Window window : java.awt.Window.getWindows()) {
            try {
                javax.swing.SwingUtilities.updateComponentTreeUI(window);
            } catch (Throwable ignored) {
                // 单个窗口刷新失败不影响其它窗口
            }
        }
    }

    /** 释放资源（应用退出前调用，主要是摘掉全局鼠标监听） */
    public static void dispose() {
        RevealEngine.dispose();
    }
}
