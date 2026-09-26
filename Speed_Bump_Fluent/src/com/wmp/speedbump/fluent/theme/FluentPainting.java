package com.wmp.speedbump.fluent.theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * 绘制工具：圆角矩形、描边、焦点环。
 *
 * <p>Swing 默认不抗锯齿、也没有「圆角矩形填充」的现成封装，这里集中处理，
 * 免得每个组件都重复一遍 {@code RenderingHints} 与 {@link BasicStroke} 的样板代码。</p>
 */
public final class FluentPainting {

    private FluentPainting() {
    }

    /** 打开文本与图形抗锯齿（务必只对副本 {@code Graphics2D} 调用） */
    public static void antialias(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /**
     * 构造圆角矩形。
     *
     * <p>半径会被自动限制到不超过短边的一半，避免组件被压扁时出现自相交的怪异图形。</p>
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

    /** 填充形状 */
    public static void fill(Graphics2D g, Shape shape, Color color) {
        if (color == null || color.getAlpha() == 0) {
            return;
        }
        g.setColor(color);
        g.fill(shape);
    }

    /** 描边形状 */
    public static void stroke(Graphics2D g, Shape shape, Color color, float width) {
        if (color == null || color.getAlpha() == 0 || width <= 0) {
            return;
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(shape);
    }

    /**
     * 画焦点环。
     *
     * <p>Fluent 的焦点环是控件<b>外沿</b>的一圈 2px 实线，而不是控件内部的虚线框——
     * Swing 默认的 {@code setFocusPainted(true)} 画的是内嵌虚线，风格完全不对，
     * 所以本项目所有自绘组件都关掉默认焦点绘制，改调这里。</p>
     *
     * <p>为了让外沿那一圈可见，形状需要向内收 {@code width/2}。</p>
     */
    public static void focusRing(Graphics2D g, Component c, int radius) {
        float w = FluentMetrics.FOCUS_RING;
        float inset = w / 2f;
        Shape ring = roundRect(inset, inset,
                Math.max(0, c.getWidth() - w),
                Math.max(0, c.getHeight() - w),
                radius);
        // 浅色主题用黑、深色主题用白：与 WinUI 一致，焦点环要「强对比」而不是「主题色」
        stroke(g, ring, FluentColors.focus(), w);
    }

    /** 画 1px 分隔线（水平） */
    public static void horizontalDivider(Graphics2D g, int x1, int x2, int y) {
        g.setColor(FluentColors.divider());
        g.setStroke(new BasicStroke(FluentMetrics.BORDER));
        g.drawLine(x1, y, x2, y);
    }

    /**
     * 把颜色按给定系数向目标色混合，用于状态叠加（悬停 / 按下）。
     *
     * @param t 0..1
     */
    public static Color overlay(Color base, Color layer, float t) {
        return FluentColors.mix(base, layer, t);
    }

    /** 组件是否处于「可用」状态（含祖先） */
    public static boolean effectivelyEnabled(Component c) {
        return c != null && c.isEnabled() && c.isDisplayable()
                ? isEnabledInHierarchy(c)
                : c != null && c.isEnabled();
    }

    private static boolean isEnabledInHierarchy(Component c) {
        Component current = c;
        while (current != null) {
            if (!current.isEnabled()) {
                return false;
            }
            current = current.getParent();
        }
        return true;
    }

    /**
     * 便捷：给按钮族组件标记为「自绘」，统一关闭 Swing 默认的背景、边框与焦点绘制。
     *
     * <p>参数类型是 {@link javax.swing.AbstractButton} 而不是 {@link JComponent}：
     * {@code setBorderPainted} / {@code setFocusPainted} 只存在于按钮族。</p>
     */
    public static void makeTransparent(javax.swing.AbstractButton c) {
        c.setOpaque(false);
        c.setBorderPainted(false);
        c.setFocusPainted(false);
        c.setContentAreaFilled(false);
    }
}
