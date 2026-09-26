package com.wmp.downloader.tools.ui.fluent;

import com.formdev.flatlaf.FlatLaf;
import com.wmp.downloader.tools.file.DataControl;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.function.Supplier;

/**
 * Fluent 风格自绘组件使用的<b>语义颜色令牌</b>。
 *
 * <h3>为什么不写死两套（浅色/深色）色值表</h3>
 * <p>本项目已经有 9 套主题（Mac Light/Dark、Flat Light/Dark、Darcula、IntelliJ、System…），
 * 而且强调色由 {@code FlatLaf.setGlobalExtraDefaults("@accentColor")} 在运行时改写。
 * 如果自绘组件自己维护一张色表，就必然与主题系统脱节——切到 Darcula 时自绘控件还是原来的颜色。</p>
 *
 * <p>所以这里的每个令牌都<b>从当前 Look &amp; Feel 的 {@code UIManager} 取值再派生</b>：
 * 主题换了、强调色改了，自绘组件下一次重绘就自动跟上，零维护成本。
 * 取不到时逐级回退到 FlatLaf/系统默认值，最后才是写死的兜底色。</p>
 *
 * <h3>使用约定</h3>
 * <p>所有取值方法都必须在<b>绘制时</b>调用（不要缓存到字段里），否则主题切换后会拿到旧颜色。</p>
 */
public final class FluentColors {

    private FluentColors() {
    }

    // ==================================================================
    // 主题状态
    // ==================================================================

    /** 当前 Look &amp; Feel 是否为深色 */
    public static boolean isDark() {
        try {
            return FlatLaf.isLafDark();
        } catch (Throwable t) {
            // FlatLaf 不可用时的粗略判断：底色亮度
            Color bg = UIManager.getColor("Panel.background");
            return bg != null && luminance(bg) < 0.5;
        }
    }

    // ==================================================================
    // 强调色
    // ==================================================================

    /**
     * 强调色（主题色）。
     *
     * <p>取值顺序：FlatLaf 的 {@code Component.accentColor}（已包含用户的
     * {@code @accentColor} 设置）→ {@code Component.focusColor} → 项目配置
     * {@code accent_color} → Fluent 默认蓝。</p>
     */
    public static Color accent() {
        Color c = UIManager.getColor("Component.accentColor");
        if (c == null) {
            c = UIManager.getColor("Component.focusColor");
        }
        if (c == null) {
            c = fromConfig("accent_color");
        }
        return c != null ? c : new Color(0x00, 0x78, 0xD4);
    }

    /** 强调色悬停态（深色主题提亮，浅色主题压暗） */
    public static Color accentHover() {
        return shade(accent(), isDark() ? 0.14f : -0.10f);
    }

    /** 强调色按下态 */
    public static Color accentPressed() {
        return shade(accent(), isDark() ? -0.10f : -0.20f);
    }

    /** 主题色之上的文字色（黑或白，按对比度自动选） */
    public static Color onAccent() {
        return luminance(accent()) > 0.55 ? new Color(0x00, 0x00, 0x00) : Color.WHITE;
    }

    /** 强调色的低透明度填充（选中底、进度条轨道、开关轨道） */
    public static Color accentSubtle() {
        Color a = accent();
        return new Color(a.getRed(), a.getGreen(), a.getBlue(), isDark() ? 0x33 : 0x22);
    }

    // ==================================================================
    // 文字
    // ==================================================================

    /** 主文本 */
    public static Color text() {
        return firstNonNull(
                () -> UIManager.getColor("Label.foreground"),
                () -> UIManager.getColor("Panel.foreground"),
                () -> isDark() ? new Color(0xFF, 0xFF, 0xFF) : new Color(0x1A, 0x1A, 0x1A));
    }

    /** 次要文本（说明文字、数值单位） */
    public static Color textSecondary() {
        return alpha(text(), isDark() ? 0xC7 : 0x9C);
    }

    /** 禁用文本 */
    public static Color textDisabled() {
        return firstNonNull(
                () -> UIManager.getColor("Label.disabledForeground"),
                () -> UIManager.getColor("Button.disabledText"),
                () -> alpha(text(), 0x5A));
    }

    // ==================================================================
    // 表面与描边
    // ==================================================================

    /** 卡片/面板表面填充 */
    public static Color card() {
        return firstNonNull(
                () -> UIManager.getColor("Panel.background"),
                () -> UIManager.getColor("control"),
                () -> isDark() ? new Color(0x2B, 0x2B, 0x2B) : new Color(0xFF, 0xFF, 0xFF));
    }

    /** 卡片描边（很淡的一圈，用于在浅色主题下区分同色卡片） */
    public static Color cardStroke() {
        Color border = UIManager.getColor("Component.borderColor");
        if (border != null) {
            return alpha(border, isDark() ? 0x59 : 0x40);
        }
        return alpha(isDark() ? Color.WHITE : Color.BLACK, isDark() ? 0x14 : 0x12);
    }

    /** 控件填充（按钮、输入框的常态底色） */
    public static Color control() {
        return firstNonNull(
                () -> UIManager.getColor("Button.background"),
                () -> UIManager.getColor("TextField.background"),
                () -> isDark() ? new Color(0xFF, 0xFF, 0xFF, 0x14) : new Color(0xFF, 0xFF, 0xFF, 0xB3));
    }

    /** 控件悬停填充 */
    public static Color controlHover() {
        return firstNonNull(
                () -> UIManager.getColor("Button.hoverBackground"),
                () -> shade(control(), isDark() ? 0.12f : -0.05f),
                () -> control());
    }

    /** 控件按下填充 */
    public static Color controlPressed() {
        return firstNonNull(
                () -> UIManager.getColor("Button.pressedBackground"),
                () -> shade(control(), isDark() ? -0.06f : -0.10f));
    }

    /** 控件禁用填充 */
    public static Color controlDisabled() {
        return alpha(control(), isDark() ? 0x80 : 0xA6);
    }

    /** 通用描边 */
    public static Color stroke() {
        Color c = UIManager.getColor("Component.borderColor");
        return c != null ? c : alpha(isDark() ? Color.WHITE : Color.BLACK, 0x2E);
    }

    /** 分隔线 */
    public static Color divider() {
        Color c = UIManager.getColor("Separator.foreground");
        return c != null ? c : alpha(isDark() ? Color.WHITE : Color.BLACK, 0x1F);
    }

    /** 细微悬停底（透明按钮、导航项、列表行） */
    public static Color subtleHover() {
        return firstNonNull(
                () -> UIManager.getColor("List.hoverBackground"),
                () -> alpha(isDark() ? Color.WHITE : Color.BLACK, isDark() ? 0x14 : 0x0F));
    }

    /** 细微按下底 */
    public static Color subtlePressed() {
        return alpha(isDark() ? Color.WHITE : Color.BLACK, isDark() ? 0x0D : 0x0A);
    }

    /** 滚动条滑块常态色 */
    public static Color scrollThumb() {
        Color c = UIManager.getColor("ScrollBar.thumb");
        return c != null ? c : isDark() ? new Color(0x9A, 0x9A, 0x9A) : new Color(0x8C, 0x8C, 0x8C);
    }

    /** 滚动条滑块悬停/拖动色 */
    public static Color scrollThumbHover() {
        Color c = UIManager.getColor("ScrollBar.hoverThumbColor");
        if (c == null) {
            c = UIManager.getColor("ScrollBar.pressedThumbColor");
        }
        return c != null ? c : shade(scrollThumb(), isDark() ? 0.18f : -0.18f);
    }

    /** 进度条轨道色 */
    public static Color progressTrack() {
        return firstNonNull(
                () -> UIManager.getColor("ProgressBar.background"),
                () -> accentSubtle());
    }

    /** 模态遮罩 */
    public static Color smoke() {
        return alpha(Color.BLACK, isDark() ? 0x66 : 0x4D);
    }

    /** 焦点环颜色：与 WinUI 一致，追求强对比而非主题色 */
    public static Color focus() {
        return isDark() ? Color.WHITE : Color.BLACK;
    }

    // ==================================================================
    // 工具
    // ==================================================================

    /** 替换 alpha */
    public static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a));
    }

    /** 调整明度：{@code amount > 0} 变亮，{@code < 0} 变暗 */
    public static Color shade(Color c, float amount) {
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
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

    /** 两色插值，{@code t} 取 0..1 */
    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
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

    /** 逐级取第一个非空值 */
    @SafeVarargs
    private static Color firstNonNull(Supplier<Color>... suppliers) {
        for (Supplier<Color> s : suppliers) {
            try {
                Color c = s.get();
                if (c != null) {
                    return c;
                }
            } catch (Throwable ignored) {
                // 某一个来源不可用时继续尝试下一个
            }
        }
        return Color.GRAY;
    }

    /**
     * 从项目配置里取一个颜色。
     *
     * <p>用 try/catch 包住是必要的：{@link DataControl} 需要先 {@code load()}，
     * 而自绘组件可能在配置加载完成之前就被创建（例如启动画面）。</p>
     */
    private static Color fromConfig(String key) {
        try {
            // 用空串而不是 null 作默认值：DataControl.get 是泛型 <T> T get(String, T)，
            // 传 null 会让类型推断失败
            String value = DataControl.get(key, "");
            if (value.isBlank()) {
                return null;
            }
            String hex = value.startsWith("#") ? value.substring(1) : value;
            if (hex.length() == 6) {
                return new Color(Integer.parseInt(hex, 16) | 0xFF000000, false);
            }
            if (hex.length() == 8) {
                return new Color((int) Long.parseLong(hex, 16), true);
            }
        } catch (Throwable ignored) {
            // 配置未就绪或格式非法，走默认色
        }
        return null;
    }
}
