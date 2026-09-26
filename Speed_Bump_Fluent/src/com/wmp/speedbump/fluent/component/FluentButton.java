package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Fluent 按钮。
 *
 * <h3>四种样式（对应 WinUI 3 的按钮规格）</h3>
 * <ul>
 *   <li>{@link Style#ACCENT} —— 强调按钮，主题色实底 + 反色文字。一个页面只应有一个主行动按钮。</li>
 *   <li>{@link Style#STANDARD} —— 标准按钮，半透明控件底 + 1px 描边 + Reveal 光晕。</li>
 *   <li>{@link Style#SUBTLE} —— 透明按钮，仅在悬停/按下时出现底色，用于工具栏。</li>
 *   <li>{@link Style#HYPERLINK} —— 超链接按钮，无底色，文字为主题色。</li>
 * </ul>
 *
 * <p>文字与图标由本类<b>手工绘制</b>而非交给 UI 代理，原因有三：
 * 一是要精确控制「图标 + 8px 间距 + 文字」的整体居中（Swing 默认按组件盒模型布局，做不出这种紧凑居中）；
 * 二是按下时 WinUI 会把文字整体下移 1px（微交互），需要通过 {@code g.translate} 实现；
 * 三是默认 L&amp;F 会在按钮上叠一层不透明底，破坏材质层。</p>
 */
public class FluentButton extends JButton implements FluentTheme.ThemeAware {

    /** 按钮样式 */
    public enum Style {
        STANDARD, ACCENT, SUBTLE, HYPERLINK
    }

    private Style style = Style.STANDARD;
    /** 图标字形码位，{@code 0} 表示无图标 */
    private char glyph;
    /** 图标字号 */
    private int glyphSize = 16;
    private int radius = FluentMetrics.RADIUS_SMALL;
    private int paddingX = 12;
    private int contentGap = 8;

    // ==================================================================
    // 构造
    // ==================================================================

    public FluentButton() {
        this(null, (char) 0, Style.STANDARD);
    }

    public FluentButton(String text) {
        this(text, (char) 0, Style.STANDARD);
    }

    public FluentButton(String text, char glyph) {
        this(text, glyph, Style.STANDARD);
    }

    /** 只有文字、但指定样式 */
    public FluentButton(String text, Style style) {
        this(text, (char) 0, style);
    }

    public FluentButton(String text, char glyph, Style style) {
        super(text);
        this.glyph = glyph;
        this.style = style != null ? style : Style.STANDARD;
        setFont(FluentTypography.body());
        setForeground(FluentColors.text());
        FluentPainting.makeTransparent(this);
        setRolloverEnabled(true);
        setMargin(new java.awt.Insets(0, 0, 0, 0));
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
        RevealEngine.register(this);
        FluentTheme.register(this);
    }

    // ---- 便捷工厂 ----

    public static FluentButton accent(String text) {
        return new FluentButton(text, (char) 0, Style.ACCENT);
    }

    public static FluentButton accent(String text, char glyph) {
        return new FluentButton(text, glyph, Style.ACCENT);
    }

    public static FluentButton subtle(char glyph) {
        return icon(glyph, Style.SUBTLE);
    }

    public static FluentButton icon(char glyph) {
        return icon(glyph, Style.STANDARD);
    }

    public static FluentButton icon(char glyph, Style style) {
        FluentButton b = new FluentButton(null, glyph, style);
        b.paddingX = 8;
        return b;
    }

    // ==================================================================
    // 配置
    // ==================================================================

    public Style getStyle() {
        return style;
    }

    public FluentButton setStyle(Style style) {
        this.style = style != null ? style : Style.STANDARD;
        revalidate();
        repaint();
        return this;
    }

    public char getGlyph() {
        return glyph;
    }

    public FluentButton setGlyph(char glyph) {
        this.glyph = glyph;
        revalidate();
        repaint();
        return this;
    }

    public FluentButton setGlyphSize(int size) {
        this.glyphSize = size;
        revalidate();
        repaint();
        return this;
    }

    public FluentButton setRadius(int radius) {
        this.radius = radius;
        repaint();
        return this;
    }

    /** 链式：设置提示文本 */
    public FluentButton tip(String tooltip) {
        setToolTipText(tooltip);
        return this;
    }

    /** 链式：设置是否可用 */
    public FluentButton disabled(boolean value) {
        setEnabled(!value);
        return this;
    }

    @Override
    public void onThemeChanged() {
        setForeground(FluentColors.text());
        repaint();
    }

    /** 主题切换时重画（供外部批量调用） */
    public void refreshTheme() {
        onThemeChanged();
    }

    // ==================================================================
    // 尺寸
    // ==================================================================

    @Override
    public Dimension getPreferredSize() {
        int height = glyph != 0 && (getText() == null || getText().isEmpty())
                ? FluentMetrics.CONTROL_HEIGHT
                : FluentMetrics.CONTROL_HEIGHT;

        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : FluentTypography.body());
        int textWidth = getText() == null ? 0 : fm.stringWidth(getText());
        int iconWidth = glyph != 0 ? glyphSize : 0;
        int gap = (glyph != 0 && textWidth > 0) ? contentGap : 0;
        int width = paddingX * 2 + iconWidth + gap + textWidth;
        if (iconWidth == 0 && textWidth == 0) {
            width = height; // 空按钮退化为正方形，避免宽度塌成 0
        }
        return new Dimension(Math.max(width, 24), height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            boolean enabled = isEnabled();
            Shape shape = FluentPainting.roundRect(this, 0, radius);

            Color fill = fillColor(enabled);
            FluentPainting.fill(g2, shape, fill);

            // 光晕画在背景之上、文字之下
            RevealEngine.paint(this, g2, shape);

            // 标准按钮带 1px 描边，让它在同色背景上也有边界。
            // 向内缩 0.5px 是为了让 1px 描边落在像素中心，否则线条会被反锯齿糊成 2px 灰边
            if (style == Style.STANDARD && enabled) {
                Shape inner = FluentPainting.roundRect(0.5, 0.5,
                        Math.max(0, getWidth() - 1), Math.max(0, getHeight() - 1), radius);
                FluentPainting.stroke(g2, inner, FluentColors.stroke(), FluentMetrics.BORDER);
            }

            if (isFocusOwner()) {
                FluentPainting.focusRing(g2, this, radius);
            }

            // WinUI 微交互：按下时内容整体下移 1px
            if (getModel().isPressed() && enabled) {
                g2.translate(0, 1);
            }
            paintContent(g2, enabled);
        } finally {
            g2.dispose();
        }
    }

    private Color fillColor(boolean enabled) {
        if (!enabled) {
            return style == Style.ACCENT ? FluentColors.controlDisabled() : FluentColors.controlDisabled();
        }
        boolean pressed = getModel().isPressed() || getModel().isArmed() && getModel().isPressed();
        boolean hover = getModel().isRollover();
        return switch (style) {
            case ACCENT -> pressed ? FluentColors.accentPressed()
                    : hover ? FluentColors.accentHover() : FluentColors.accent();
            case STANDARD -> pressed ? FluentColors.controlPressed()
                    : hover ? FluentColors.controlHover() : FluentColors.control();
            case SUBTLE, HYPERLINK -> pressed ? FluentColors.subtlePressed()
                    : hover ? FluentColors.subtleHover() : null;
        };
    }

    private Color contentColor(boolean enabled) {
        if (!enabled) {
            return FluentColors.textDisabled();
        }
        return switch (style) {
            case ACCENT -> FluentColors.onAccent();
            case HYPERLINK -> FluentColors.accent();
            default -> FluentColors.text();
        };
    }

    private void paintContent(Graphics2D g2, boolean enabled) {
        Color color = contentColor(enabled);
        g2.setColor(color);

        String text = getText();
        boolean hasText = text != null && !text.isEmpty();
        boolean hasGlyph = glyph != 0;
        if (!hasText && !hasGlyph) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        FontMetrics textMetrics = g2.getFontMetrics(getFont() != null ? getFont() : FluentTypography.body());
        int textWidth = hasText ? textMetrics.stringWidth(text) : 0;
        int gap = (hasGlyph && hasText) ? contentGap : 0;
        int iconWidth = hasGlyph ? glyphSize : 0;
        int total = iconWidth + gap + textWidth;

        float x = (width - total) / 2f;
        int baseline = (height - textMetrics.getHeight()) / 2 + textMetrics.getAscent();

        if (hasGlyph) {
            paintGlyph(g2, x, width, height);
            x += iconWidth + gap;
        }
        if (hasText) {
            g2.setFont(getFont());
            g2.setColor(color);
            // 强制整数坐标，避免文字落在半像素上发虚
            g2.drawString(text, Math.round(x), baseline);
        }
    }

    /** 用图标字体绘制字形；字体不可用时画一个中性方块占位 */
    private void paintGlyph(Graphics2D g2, float x, int componentWidth, int componentHeight) {
        if (!FluentTypography.iconFontAvailable()) {
            g2.fillRoundRect(Math.round(x), (componentHeight - glyphSize) / 2,
                    glyphSize, glyphSize, 4, 4);
            return;
        }
        var font = FluentIcons.font(glyphSize);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics(font);
        String s = String.valueOf(glyph);
        int glyphAdvance = fm.stringWidth(s);
        int baseline = (componentHeight - fm.getHeight()) / 2 + fm.getAscent();
        // 图标字形左右留白不对称，按实际宽度再居中一次
        float gx = x + (glyphSize - glyphAdvance) / 2f;
        g2.drawString(s, Math.round(gx), baseline);
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        revalidate();
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    @Override
    protected void paintBorder(Graphics g) {
        // 边框由 paintComponent 自绘
    }
}
