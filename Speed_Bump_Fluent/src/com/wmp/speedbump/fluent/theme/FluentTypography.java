package com.wmp.speedbump.fluent.theme;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * WinUI 3 字体排印令牌。
 *
 * <h3>字体族的降级顺序</h3>
 * <ol>
 *   <li>Windows 11：{@code Segoe UI Variable Display} / {@code Segoe UI Variable Text}</li>
 *   <li>Windows 10：{@code Segoe UI}</li>
 *   <li>其它系统：{@code SansSerif} 逻辑字体（Swing 会映射到系统 UI 字体）</li>
 * </ol>
 *
 * <p>中文等 CJK 字形由 Java 的字体回退链自动处理，不需要显式指定「微软雅黑」。</p>
 *
 * <h3>为什么字号是整数</h3>
 * <p>WinUI 的字号阶梯是 12 / 14 / 20 / 28 / 40 / 68。Swing 的 {@link Font} 接受
 * 整数点值；用小数会让同一文字在不同 DPI 下取整不一致，反而模糊。</p>
 */
public final class FluentTypography {

    private FluentTypography() {
    }

    /** 标题/大字使用的字体族 */
    public static final String DISPLAY_FAMILY;
    /** 正文使用的字体族 */
    public static final String TEXT_FAMILY;
    /** Fluent 图标字体族（Segoe Fluent Icons / Segoe MDL2 Assets） */
    public static final String ICON_FAMILY;

    static {
        Set<String> families = availableFamilies();
        DISPLAY_FAMILY = pick(families,
                "Segoe UI Variable Display", "Segoe UI Variable Text", "Segoe UI", Font.SANS_SERIF);
        TEXT_FAMILY = pick(families,
                "Segoe UI Variable Text", "Segoe UI Variable Display", "Segoe UI", Font.SANS_SERIF);
        ICON_FAMILY = pick(families,
                "Segoe Fluent Icons", "Segoe MDL2 Assets", Font.SANS_SERIF);
    }

    // ---- 字号阶梯 ----
    public static final int SIZE_CAPTION = 12;
    public static final int SIZE_BODY = 14;
    public static final int SIZE_BODY_LARGE = 18;
    public static final int SIZE_SUBTITLE = 20;
    public static final int SIZE_TITLE = 28;
    public static final int SIZE_TITLE_LARGE = 40;
    public static final int SIZE_DISPLAY = 68;

    // ---- 字体实例缓存 ----
    //
    // 每次 new Font(...) 都会创建一个新对象；组件的 getPreferredSize() 普遍要取
    // FontMetrics，而 FontMetrics 缓存是以「字体 + 渲染上下文」为键的。
    // 大量重复构造字体不仅白费分配，还会降低缓存命中率——一个任务列表页里
    // 每个卡片都会调用 body()/bodyStrong()/caption()，累积起来相当可观。
    private static final Font CAPTION = new Font(TEXT_FAMILY, Font.PLAIN, SIZE_CAPTION);
    private static final Font BODY = new Font(TEXT_FAMILY, Font.PLAIN, SIZE_BODY);
    private static final Font BODY_STRONG = new Font(TEXT_FAMILY, Font.BOLD, SIZE_BODY);
    private static final Font BODY_LARGE = new Font(TEXT_FAMILY, Font.PLAIN, SIZE_BODY_LARGE);
    private static final Font SUBTITLE = new Font(DISPLAY_FAMILY, Font.PLAIN, SIZE_SUBTITLE);
    private static final Font TITLE = new Font(DISPLAY_FAMILY, Font.BOLD, SIZE_TITLE);
    private static final Font TITLE_LARGE = new Font(DISPLAY_FAMILY, Font.BOLD, SIZE_TITLE_LARGE);
    private static final java.util.Map<Integer, Font> ICON_FONTS = new java.util.concurrent.ConcurrentHashMap<>();

    public static Font caption() {
        return CAPTION;
    }

    public static Font body() {
        return BODY;
    }

    /** 正文加粗（用于卡片标题、导航项选中态） */
    public static Font bodyStrong() {
        return BODY_STRONG;
    }

    public static Font bodyLarge() {
        return BODY_LARGE;
    }

    public static Font subtitle() {
        return SUBTITLE;
    }

    /** 页面主标题 */
    public static Font title() {
        return TITLE;
    }

    public static Font titleLarge() {
        return TITLE_LARGE;
    }

    /** 图标字体，指定像素尺寸（按尺寸缓存） */
    public static Font icon(int size) {
        return ICON_FONTS.computeIfAbsent(size, s -> new Font(ICON_FAMILY, Font.PLAIN, s));
    }

    /** 图标字体是否可用（不可用时 UI 会退化为矢量绘制） */
    public static boolean iconFontAvailable() {
        return !Font.SANS_SERIF.equals(ICON_FAMILY);
    }

    private static String pick(Set<String> families, String... candidates) {
        for (String c : candidates) {
            if (families.contains(c.toLowerCase(Locale.ROOT))) {
                return c;
            }
        }
        return Font.SANS_SERIF;
    }

    private static Set<String> availableFamilies() {
        Set<String> result = new HashSet<>();
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return result;
            }
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                result.add(name.toLowerCase(Locale.ROOT));
            }
        } catch (Throwable ignored) {
            // 取不到字体列表时全部走逻辑字体
        }
        return result;
    }
}
