package com.wmp.speed_bump.platform.ui.swing.tools.fluent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * 自绘组件的绘制工具集。
 *
 * <p>Swing 默认不抗锯齿，也没有「圆角矩形填充」的现成封装。这里集中处理，
 * 免得每个自绘组件都重复一遍 {@link RenderingHints} 与 {@link BasicStroke} 的样板代码。</p>
 */
public final class FluentPainting {

    private FluentPainting() {
    }

    /** 打开抗锯齿（只对 {@code Graphics2D} 的副本调用） */
    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * 构造圆角矩形。
     *
     * <p>半径会被限制到不超过短边的一半，避免组件被压扁时出现自相交的怪异图形。</p>
     */
    public static Shape roundRect(double x, double y, double w, double h, int radius) {
        double r = Math.max(0, Math.min(radius, Math.min(w, h) / 2.0));
        return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
    }

    /** 按组件尺寸构造圆角矩形，{@code inset} 为向内收缩的像素数 */
    public static Shape roundRect(Component c, int inset, int radius) {
        return roundRect(inset, inset,
                Math.max(0, c.getWidth() - inset * 2.0),
                Math.max(0, c.getHeight() - inset * 2.0),
                radius);
    }

    /** 填充形状（颜色为 {@code null} 或全透明时跳过） */
    public static void fill(Graphics2D g, Shape shape, Color color) {
        if (color == null || color.getAlpha() == 0 || shape == null) {
            return;
        }
        g.setColor(color);
        g.fill(shape);
    }

    /** 描边形状 */
    public static void stroke(Graphics2D g, Shape shape, Color color, float width) {
        if (color == null || color.getAlpha() == 0 || width <= 0 || shape == null) {
            return;
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    /**
     * 画焦点环。
     *
     * <p>Fluent 的焦点环是控件<b>外沿</b>的一圈 2px 实线，颜色追求强对比（浅色主题黑、深色主题白），
     * 而不是主题色。形状需要向内收 {@code width/2}，那一圈才完整可见。</p>
     */
    public static void focusRing(Graphics2D g, Component c, int radius) {
        float w = FluentMetrics.FOCUS_RING;
        float inset = w / 2f;
        Shape ring = roundRect(inset, inset,
                Math.max(0, c.getWidth() - w),
                Math.max(0, c.getHeight() - w),
                radius);
        stroke(g, ring, FluentColors.focus(), w);
    }

    /** 水平方向画一条分隔线 */
    public static void horizontalDivider(Graphics2D g, int x1, int x2, int y) {
        g.setColor(FluentColors.divider());
        g.setStroke(new BasicStroke(FluentMetrics.BORDER));
        g.drawLine(x1, y, x2, y);
    }

    /** 把组件标记为「自绘」：关闭 Swing 默认的背景、边框与焦点绘制 */
    public static void makeTransparent(javax.swing.AbstractButton b) {
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setContentAreaFilled(false);
    }

    // ==================================================================
    // 按钮族文字绘制
    // ==================================================================

    /**
     * 按钮族（复选框 / 开关 / 单选框 / 按钮）文字的目标颜色。
     *
     * <p>禁用态优先取主题自己的禁用文字色（FlatLaf 用的是
     * {@code CheckBox.disabledText} 这类键），取不到才退化为与主文字同源的淡化色。</p>
     */
    public static Color labelColor(javax.swing.AbstractButton b) {
        if (b.isEnabled()) {
            Color foreground = b.getForeground();
            return foreground != null ? foreground : FluentColors.text();
        }
        Color disabled = firstNonNullColor("CheckBox.disabledText", "Button.disabledText",
                "Label.disabledForeground");
        return disabled != null ? disabled : FluentColors.textDisabled();
    }

    /**
     * 绘制按钮族文字，自动处理三件事：禁用色、助记符下划线、HTML 文本。
     *
     * <h3>为什么不能交给 {@code BasicButtonUI.paintText}</h3>
     * <p>JDK 里那个方法的禁用态分支是<b>老式浮雕画法</b>：用
     * {@code background.brighter()} 先在原位画一遍，再用 {@code background.darker()}
     * 向左上偏移 1px 画第二遍。它<b>完全不读主题的禁用文字色</b>——
     * 在 FlatLaf 这类把控件背景设成深色的外观下，{@code darker()} 出来的结果近乎纯黑，
     * 于是「禁用组件的文字反而显示为黑色」。自绘组件必须自己选色。</p>
     *
     * <h3>HTML 文本必须走 View</h3>
     * <p>{@code BasicButtonUI} 在安装时会用 {@code BasicHTML.updateRenderer} 给含 HTML 的
     * 文本挂一个 {@code View}，真正的 HTML 绘制由该 View 完成。
     * 如果无条件调用 {@code paintText}，HTML 文本会被当成普通字符串，
     * 界面上会直接显示出一串标签原文（{@code <html>…}）。
     * 而 View 取色用的是组件前景色，它并不知道「禁用态该用哪个颜色」，
     * 所以绘制期间临时替换前景色，画完立刻还原。</p>
     *
     * @param g        图形上下文
     * @param b        目标按钮
     * @param text     文本（非 HTML 时使用）
     * @param textArea 可供文字使用的区域（通常是图标右侧的整块区域，整高）
     */
    public static void paintLabel(Graphics2D g, javax.swing.AbstractButton b, String text,
                                  java.awt.Rectangle textArea) {
        if (textArea == null || textArea.width <= 0 || textArea.height <= 0) {
            return;
        }
        if (text == null || text.isEmpty()) {
            return;
        }
        Color color = labelColor(b);

        javax.swing.text.View htmlView =
                (javax.swing.text.View) b.getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);
        if (htmlView != null) {
            Color old = b.getForeground();
            try {
                b.setForeground(color);
                float viewHeight = htmlView.getPreferredSpan(javax.swing.text.View.Y_AXIS);
                java.awt.Rectangle rect = new java.awt.Rectangle(
                        textArea.x,
                        textArea.y + Math.max(0, Math.round((textArea.height - viewHeight) / 2f)),
                        textArea.width,
                        Math.max(1, Math.round(viewHeight)));
                htmlView.paint(g, rect);
            } catch (Throwable ignored) {
                // HTML 视图绘制失败时退回普通文本，至少内容不丢
                g.setColor(color);
                g.drawString(text, textArea.x, textArea.y + g.getFontMetrics().getAscent());
            } finally {
                b.setForeground(old);
            }
            return;
        }

        java.awt.Font font = b.getFont();
        if (font != null) {
            g.setFont(font);
        }
        g.setColor(color);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int baseline = textArea.y + (textArea.height - fm.getHeight()) / 2 + fm.getAscent();
        javax.swing.plaf.basic.BasicGraphicsUtils.drawStringUnderlineCharAt(
                g, text, mnemonicIndex(b), textArea.x, baseline);
    }

    /**
     * 助记符下划线位置。
     *
     * <p>FlatLaf 默认「按 Alt 才显示助记符」，直接取
     * {@code getDisplayedMnemonicIndex()} 会一直画下划线，与主题行为不一致，
     * 所以先问一下 FlatLaf；非 FlatLaf 外观则沿用 Swing 自己的判断。</p>
     */
    private static int mnemonicIndex(javax.swing.AbstractButton b) {
        try {
            if (!com.formdev.flatlaf.FlatLaf.isShowMnemonics()) {
                return -1;
            }
        } catch (Throwable ignored) {
            // 非 FlatLaf：走下面的通用逻辑
        }
        return b.getDisplayedMnemonicIndex();
    }

    private static Color firstNonNullColor(String... keys) {
        for (String key : keys) {
            try {
                Color c = javax.swing.UIManager.getColor(key);
                if (c != null) {
                    return c;
                }
            } catch (Throwable ignored) {
                // 忽略单个键的取值失败
            }
        }
        return null;
    }
}
