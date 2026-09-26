package com.wmp.downloader.tools.ui.fluent;

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
}
