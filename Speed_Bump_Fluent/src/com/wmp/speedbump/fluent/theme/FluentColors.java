package com.wmp.speedbump.fluent.theme;

import java.awt.Color;

/**
 * WinUI 3 / Fluent Design 颜色令牌。
 *
 * <p>命名与语义对齐 WinUI 3 的 brush 命名（Layer / Card / Control / Text / Accent / Stroke），
 * 每个令牌同时给出浅色与深色两组取值，由 {@link FluentTheme#isDark()} 决定当前返回哪一组。</p>
 *
 * <h3>关于 alpha</h3>
 * <p>Fluent 的材质层（Layer / Card / Control）本身就是<b>半透明</b>的——半透明不是装饰，
 * 而是让底层 Mica/Acrylic 材质透出来的唯一手段。因此这里的颜色大量带 alpha，
 * 绘制时必须使用 {@code AlphaComposite}（Swing 默认就是 SrcOver，直接 setColor 即可）。</p>
 *
 * <p>若运行环境没有原生材质（非 Windows、或窗口不是透明窗口），
 * 会由 {@link FluentTheme} 设置 {@link #setSolidFallback(boolean)}，
 * 此时所有令牌自动与不透明底色合成，保证文字对比度不塌陷。</p>
 */
public final class FluentColors {

    private FluentColors() {
    }

    /** 是否把半透明令牌预先与不透明底色合成（无原生材质时使用） */
    private static boolean solidFallback = false;

    /** 无材质时的不透明底色 */
    private static final Color L_SOLID = new Color(0xF3, 0xF3, 0xF3);
    private static final Color D_SOLID = new Color(0x20, 0x20, 0x20);

    /** 用户自定义主题色；为 {@code null} 时使用系统色/默认 Fluent 蓝 */
    private static Color accentOverride;

    // ==================================================================
    // 浅色令牌
    // ==================================================================

    private static final Color L_LAYER = new Color(0xFF, 0xFF, 0xFF, 0xB4);
    private static final Color L_CARD = new Color(0xFF, 0xFF, 0xFF, 0xCC);
    private static final Color L_CARD_HOVER = new Color(0xFF, 0xFF, 0xFF, 0xCC);
    private static final Color L_CARD_STROKE = new Color(0x00, 0x00, 0x00, 0x14);
    private static final Color L_CONTROL = new Color(0xFF, 0xFF, 0xFF, 0xB3);
    private static final Color L_CONTROL_HOVER = new Color(0xFF, 0xFF, 0xFF, 0xD6);
    private static final Color L_CONTROL_PRESSED = new Color(0xF9, 0xF9, 0xF9, 0x8C);
    private static final Color L_CONTROL_DISABLED = new Color(0xFF, 0xFF, 0xFF, 0x59);
    private static final Color L_STROKE = new Color(0x00, 0x00, 0x00, 0x10);
    private static final Color L_DIVIDER = new Color(0x00, 0x00, 0x00, 0x14);
    private static final Color L_TEXT = new Color(0x00, 0x00, 0x00, 0xE4);
    private static final Color L_TEXT_SECONDARY = new Color(0x00, 0x00, 0x00, 0x9C);
    private static final Color L_TEXT_DISABLED = new Color(0x00, 0x00, 0x00, 0x5A);
    private static final Color L_ACCENT = new Color(0x00, 0x78, 0xD4);
    private static final Color L_ACCENT_HOVER = new Color(0x1A, 0x86, 0xD9);
    private static final Color L_ACCENT_PRESSED = new Color(0x00, 0x67, 0xB8);
    private static final Color L_ON_ACCENT = Color.WHITE;
    private static final Color L_SUBTLE_HOVER = new Color(0x00, 0x00, 0x00, 0x0F);
    private static final Color L_SUBTLE_PRESSED = new Color(0x00, 0x00, 0x00, 0x0A);
    private static final Color L_SMOKE = new Color(0x00, 0x00, 0x00, 0x4D);

    // ==================================================================
    // 深色令牌
    // ==================================================================

    private static final Color D_LAYER = new Color(0x20, 0x20, 0x20, 0xB4);
    private static final Color D_CARD = new Color(0xFF, 0xFF, 0xFF, 0x0D);
    private static final Color D_CARD_HOVER = new Color(0xFF, 0xFF, 0xFF, 0x17);
    private static final Color D_CARD_STROKE = new Color(0xFF, 0xFF, 0xFF, 0x12);
    private static final Color D_CONTROL = new Color(0xFF, 0xFF, 0xFF, 0x0F);
    private static final Color D_CONTROL_HOVER = new Color(0xFF, 0xFF, 0xFF, 0x19);
    private static final Color D_CONTROL_PRESSED = new Color(0xFF, 0xFF, 0xFF, 0x0A);
    private static final Color D_CONTROL_DISABLED = new Color(0xFF, 0xFF, 0xFF, 0x0A);
    private static final Color D_STROKE = new Color(0xFF, 0xFF, 0xFF, 0x12);
    private static final Color D_DIVIDER = new Color(0xFF, 0xFF, 0xFF, 0x14);
    private static final Color D_TEXT = new Color(0xFF, 0xFF, 0xFF, 0xFF);
    private static final Color D_TEXT_SECONDARY = new Color(0xFF, 0xFF, 0xFF, 0xC7);
    private static final Color D_TEXT_DISABLED = new Color(0xFF, 0xFF, 0xFF, 0x5A);
    private static final Color D_ACCENT = new Color(0x60, 0xCD, 0xFF);
    private static final Color D_ACCENT_HOVER = new Color(0x7A, 0xD6, 0xFF);
    private static final Color D_ACCENT_PRESSED = new Color(0x4C, 0xC2, 0xFF);
    private static final Color D_ON_ACCENT = new Color(0x00, 0x00, 0x00);
    private static final Color D_SUBTLE_HOVER = new Color(0xFF, 0xFF, 0xFF, 0x0F);
    private static final Color D_SUBTLE_PRESSED = new Color(0xFF, 0xFF, 0xFF, 0x0A);
    private static final Color D_SMOKE = new Color(0x00, 0x00, 0x00, 0x66);

    private static boolean dark() {
        return FluentTheme.isDark();
    }

    /**
     * 令牌出口：若启用了「不透明回退」，把半透明色与不透明底色合成，
     * 使颜色在无材质环境下依然有正确的对比度。
     *
     * @param aboveLayer {@code true} 表示该表面是「叠在材质层之上」的（卡片、控件），
     *                   合成基准应当是已经合成过的材质层，而不是原始底色。
     *                   <p>这一点很容易搞错：如果把卡片和材质层都与同一个底色合成，
     *                   两者算出来会<b>完全相同</b>——界面上表现为「卡片和背景一样白，
     *                   只剩一圈描边」。真实渲染时卡片是画在材质层之上的，
     *                   这里的合成顺序必须与绘制顺序一致。</p>
     */
    private static Color resolve(Color c, boolean aboveLayer) {
        if (!solidFallback || c.getAlpha() == 255) {
            return c;
        }
        Color base = aboveLayer ? resolveLayer() : solidBase();
        float a = c.getAlpha() / 255f;
        return new Color(
                Math.round(c.getRed() * a + base.getRed() * (1 - a)),
                Math.round(c.getGreen() * a + base.getGreen() * (1 - a)),
                Math.round(c.getBlue() * a + base.getBlue() * (1 - a)));
    }

    private static Color resolve(Color c) {
        return resolve(c, true);
    }

    /** 已合成的材质层颜色（无材质时的基准） */
    private static Color resolveLayer() {
        Color c = dark() ? D_LAYER : L_LAYER;
        if (!solidFallback || c.getAlpha() == 255) {
            return c;
        }
        Color base = solidBase();
        float a = c.getAlpha() / 255f;
        return new Color(
                Math.round(c.getRed() * a + base.getRed() * (1 - a)),
                Math.round(c.getGreen() * a + base.getGreen() * (1 - a)),
                Math.round(c.getBlue() * a + base.getBlue() * (1 - a)));
    }

    private static Color solidBase() {
        return dark() ? D_SOLID : L_SOLID;
    }

    // ==================================================================
    // 公开令牌
    // ==================================================================

    /** 窗口最底层材质着色（画在 NativeBackdrop 之上、卡片之下） */
    public static Color layer() {
        return resolveLayer();
    }

    /** 无材质时的不透明底色 */
    public static Color solid() {
        return solidBase();
    }

    /** 卡片填充 */
    public static Color card() {
        return resolve(dark() ? D_CARD : L_CARD);
    }

    /** 卡片悬停填充 */
    public static Color cardHover() {
        return resolve(dark() ? D_CARD_HOVER : L_CARD_HOVER);
    }

    /** 卡片描边 */
    public static Color cardStroke() {
        return resolve(dark() ? D_CARD_STROKE : L_CARD_STROKE);
    }

    /**
     * 不透明卡面。
     *
     * <p>用于弹窗、Flyout、Tooltip —— 它们是独立的顶层窗口，没有原生材质垫底，
     * 用半透明色会让桌面直接透上来，文字不可读。</p>
     */
    public static Color solidCard() {
        return dark() ? new Color(0x2C, 0x2C, 0x2C) : new Color(0xFF, 0xFF, 0xFF);
    }

    /** 不透明浮出层表面（比 {@link #solidCard()} 略深，形成层次） */
    public static Color flyout() {
        return dark() ? new Color(0x2C, 0x2C, 0x2C) : new Color(0xF9, 0xF9, 0xF9);
    }

    /** 控件填充（标准按钮、文本框、下拉框） */
    public static Color control() {
        return resolve(dark() ? D_CONTROL : L_CONTROL);
    }

    /** 控件悬停填充 */
    public static Color controlHover() {
        return resolve(dark() ? D_CONTROL_HOVER : L_CONTROL_HOVER);
    }

    /** 控件按下填充 */
    public static Color controlPressed() {
        return resolve(dark() ? D_CONTROL_PRESSED : L_CONTROL_PRESSED);
    }

    /** 控件禁用填充 */
    public static Color controlDisabled() {
        return resolve(dark() ? D_CONTROL_DISABLED : L_CONTROL_DISABLED);
    }

    /** 通用描边（控件边框） */
    public static Color stroke() {
        return resolve(dark() ? D_STROKE : L_STROKE);
    }

    /** 分隔线 */
    public static Color divider() {
        return resolve(dark() ? D_DIVIDER : L_DIVIDER);
    }

    /** 主文本 */
    public static Color text() {
        return dark() ? D_TEXT : L_TEXT;
    }

    /** 次要文本 */
    public static Color textSecondary() {
        return dark() ? D_TEXT_SECONDARY : L_TEXT_SECONDARY;
    }

    /** 禁用文本 */
    public static Color textDisabled() {
        return dark() ? D_TEXT_DISABLED : L_TEXT_DISABLED;
    }

    /** 主题色（强调色） */
    public static Color accent() {
        if (accentOverride != null) {
            return accentOverride;
        }
        return dark() ? D_ACCENT : L_ACCENT;
    }

    /** 主题色悬停 */
    public static Color accentHover() {
        if (accentOverride != null) {
            return shade(accentOverride, dark() ? 0.14f : -0.08f);
        }
        return dark() ? D_ACCENT_HOVER : L_ACCENT_HOVER;
    }

    /** 主题色按下 */
    public static Color accentPressed() {
        if (accentOverride != null) {
            return shade(accentOverride, dark() ? -0.12f : -0.18f);
        }
        return dark() ? D_ACCENT_PRESSED : L_ACCENT_PRESSED;
    }

    /** 主题色之上的文字色 */
    public static Color onAccent() {
        if (accentOverride != null) {
            return luminance(accentOverride) > 0.55 ? new Color(0, 0, 0) : Color.WHITE;
        }
        return dark() ? D_ON_ACCENT : L_ON_ACCENT;
    }

    /** 主题色的低透明度填充（选中态底、开关轨道） */
    public static Color accentSubtle() {
        Color a = accent();
        return new Color(a.getRed(), a.getGreen(), a.getBlue(), dark() ? 0x33 : 0x22);
    }

    /** 细微悬停（透明按钮、导航项） */
    public static Color subtleHover() {
        return resolve(dark() ? D_SUBTLE_HOVER : L_SUBTLE_HOVER);
    }

    /** 细微按下 */
    public static Color subtlePressed() {
        return resolve(dark() ? D_SUBTLE_PRESSED : L_SUBTLE_PRESSED);
    }

    /** 模态遮罩 */
    public static Color smoke() {
        return dark() ? D_SMOKE : L_SMOKE;
    }

    /** 焦点环颜色 */
    public static Color focus() {
        return dark() ? new Color(0xFF, 0xFF, 0xFF) : new Color(0x00, 0x00, 0x00);
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** 调整明度：{@code amount > 0} 变亮，{@code < 0} 变暗 */
    public static Color shade(Color c, float amount) {
        int r = c.getRed(), g = c.getGreen(), b = c.getBlue();
        if (amount >= 0) {
            r = Math.round(r + (255 - r) * amount);
            g = Math.round(g + (255 - g) * amount);
            b = Math.round(b + (255 - b) * amount);
        } else {
            float k = 1 + amount;
            r = Math.round(r * k);
            g = Math.round(g * k);
            b = Math.round(b * k);
        }
        return new Color(clamp(r), clamp(g), clamp(b), c.getAlpha());
    }

    /** 替换 alpha */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a));
    }

    /** 在两个颜色之间插值，{@code t} 取 0..1 */
    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** 感知亮度，0..1 */
    public static double luminance(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ---- 配置 ----

    public static void setSolidFallback(boolean value) {
        solidFallback = value;
    }

    public static boolean isSolidFallback() {
        return solidFallback;
    }

    public static void setAccentOverride(Color color) {
        accentOverride = color;
    }

    public static Color accentOverride() {
        return accentOverride;
    }
}
