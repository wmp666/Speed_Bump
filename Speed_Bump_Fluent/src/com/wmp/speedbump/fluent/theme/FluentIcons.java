package com.wmp.speedbump.fluent.theme;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Supplier;

/**
 * Fluent 图标。
 *
 * <p>WinUI 自身就是用 {@code Segoe Fluent Icons} / {@code Segoe MDL2 Assets} 字体里的
 * 私有区码位渲染图标的。这里沿用同一方案：零图片资源、任意缩放清晰、
 * 颜色随主题变化——比打包一堆 PNG/SVG 更贴近原生，也更省体积。</p>
 *
 * <p>若运行环境没有图标字体（Linux 常见），{@link #icon} 会退化为一个中性色圆点占位，
 * 界面结构不受影响。</p>
 */
public final class FluentIcons {

    private FluentIcons() {
    }

    // ---- 导航与结构 ----
    public static final char GLOBAL_NAV = '\uE700';
    public static final char HOME = '\uE80F';
    public static final char RECENT = '\uE81C';
    public static final char LIBRARY = '\uE8F1';
    public static final char PAGE = '\uE7C3';
    public static final char CALENDAR = '\uE787';
    public static final char GLOBE = '\uE774';
    public static final char NETWORK = '\uE968';

    // ---- 操作 ----
    public static final char ADD = '\uE710';
    public static final char CANCEL = '\uE711';
    public static final char MORE = '\uE712';
    public static final char SETTINGS = '\uE713';
    public static final char SEARCH = '\uE721';
    public static final char REFRESH = '\uE72C';
    public static final char BACK = '\uE72B';
    public static final char SHARE = '\uE72D';
    public static final char SAVE = '\uE74E';
    public static final char DELETE = '\uE74D';
    public static final char COPY = '\uE8C8';
    public static final char SYNC = '\uE895';
    public static final char EDIT = '\uE70F';
    public static final char LINK = '\uE71B';

    // ---- 媒体 / 传输 ----
    public static final char PLAY = '\uE768';
    public static final char PAUSE = '\uE769';
    public static final char DOWNLOAD = '\uE896';
    public static final char UPLOAD = '\uE898';
    public static final char FOLDER = '\uE8B7';
    public static final char OPEN_FILE = '\uE8E5';

    // ---- 状态 ----
    public static final char CHECK = '\uE73E';
    public static final char INFO = '\uE946';
    public static final char STAR = '\uE734';
    public static final char STAR_FILL = '\uE735';
    public static final char BRIGHTNESS = '\uE706';

    // ---- 方向 ----
    public static final char CHEVRON_DOWN = '\uE70D';
    public static final char CHEVRON_UP = '\uE70E';
    public static final char CHEVRON_LEFT = '\uE76B';
    public static final char CHEVRON_RIGHT = '\uE76C';

    // ---- 标题栏（多半用矢量绘制，这里保留码位备用） ----
    public static final char CHROME_MINIMIZE = '\uE921';
    public static final char CHROME_MAXIMIZE = '\uE922';
    public static final char CHROME_RESTORE = '\uE923';
    public static final char CHROME_CLOSE = '\uE8BB';

    /** 取图标字体 */
    public static Font font(int size) {
        return FluentTypography.icon(size);
    }

    /** 固定颜色的图标 */
    public static Icon icon(char glyph, int size, Color color) {
        return new GlyphIcon(glyph, size, () -> color);
    }

    /** 颜色随主题变化的图标（推荐：主题切换时图标自动跟随） */
    public static Icon themedIcon(char glyph, int size, Supplier<Color> colorSupplier) {
        return new GlyphIcon(glyph, size, colorSupplier);
    }

    /** 取 GlyphIcon 的字符（供自绘组件直接画字） */
    public static String glyph(char c) {
        return String.valueOf(c);
    }

    /**
     * 用图标字体绘制一个字形，返回是否成功。
     *
     * <p>组件内部自绘时用这个而不是 {@link javax.swing.Icon#paintIcon}，
     * 免得为每个绘制点都构造 Icon 对象。</p>
     */
    public static boolean paintGlyph(Graphics2D g, char glyph, int size, float x, float y) {
        if (!FluentTypography.iconFontAvailable()) {
            return false;
        }
        g.setFont(font(size));
        g.drawString(glyph(glyph), x, y);
        return true;
    }

    /**
     * 图标字形在指定字号下的绘制基线偏移，便于垂直居中。
     *
     * @return {@code [ascent, descent]}，字体不可用时返回 {@code null}
     */
    public static int[] metrics(Component c, int size) {
        if (!FluentTypography.iconFontAvailable()) {
            return null;
        }
        FontMetrics fm = c.getFontMetrics(font(size));
        return new int[]{fm.getAscent(), fm.getDescent()};
    }

    /** 基于字体的图标实现 */
    private static final class GlyphIcon implements Icon {
        private final char glyph;
        private final int size;
        private final Supplier<Color> color;

        GlyphIcon(char glyph, int size, Supplier<Color> color) {
            this.glyph = glyph;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                if (!FluentTypography.iconFontAvailable()) {
                    // 无图标字体：画一个中性圆点占位，保持布局不变
                    g2.setColor(FluentColors.textDisabled());
                    int d = Math.max(4, size / 3);
                    g2.fillOval(x + (size - d) / 2, y + (size - d) / 2, d, d);
                    return;
                }
                g2.setFont(font(size));
                g2.setColor(color.get());
                FontMetrics fm = g2.getFontMetrics();
                int baseline = y + (size - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(glyph(glyph), x, baseline);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
