package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Fluent 下拉选择框。
 *
 * <h3>为什么这个组件几乎不改绘制</h3>
 * <p>实测 FlatLaf 的 {@code FlatComboBoxUI} 会<b>无条件</b>绘制自己的圆角背景
 * （源码：{@code if (paintBackground || c.isOpaque())}，其中 {@code paintBackground} 默认为 {@code true}），
 * 因此只要把 {@code ComboBox.background} 等 UI 默认值设成 Fluent 令牌，
 * 它画出来的就已经是「半透明圆角底 + 右侧箭头」——正好是目标效果。
 * 强行自绘反而会与之打架，所以这里只做三件事：设置字体与令牌、
 * 补一条 WinUI 特有的底线、统一下拉列表的渲染器。</p>
 *
 * <p>底线画在 {@link #paint(Graphics)} 的最后（即所有子组件之上），
 * 这样它不会被箭头按钮的绘制盖住。</p>
 */
public class FluentComboBox<E> extends JComboBox<E> implements FluentTheme.ThemeAware {

    private boolean underline = true;

    public FluentComboBox() {
        super();
        init();
    }

    public FluentComboBox(E[] items) {
        super(items);
        init();
    }

    public FluentComboBox(javax.swing.ComboBoxModel<E> model) {
        super(model);
        init();
    }

    private void init() {
        setFont(FluentTypography.body());
        setForeground(FluentColors.text());
        setBackground(FluentColors.control());
        setRequestFocusEnabled(true);
        // 与文本框一致的圆角
        putClientProperty("JComponent.roundRect", Boolean.TRUE);
        // FlatLaf：把箭头换成 WinUI 风格的下拉箭头，并去掉默认的方形外观
        putClientProperty("JComboBox.isSquare", Boolean.FALSE);
        putClientProperty("JComboBox.buttonEmbedded", Boolean.FALSE);
        setRenderer(new FluentRenderer());
        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        setForeground(FluentColors.text());
        setBackground(FluentColors.control());
        repaint();
    }

    /** 关闭底部的 2px 主题色下划线 */
    public FluentComboBox<E> setUnderline(boolean underline) {
        this.underline = underline;
        repaint();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 120), FluentMetrics.CONTROL_HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, FluentMetrics.CONTROL_HEIGHT);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (!underline) {
            return;
        }
        var g2 = (java.awt.Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            // 4px 内缩，避免压在圆角外侧
            if (isFocusOwner() || isPopupVisible()) {
                g2.setColor(FluentColors.accent());
                g2.fillRect(4, h - 2, w - 8, 2);
            } else {
                Color line = FluentColors.textSecondary();
                g2.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), 0x59));
                g2.fillRect(4, h - 1, w - 8, 1);
            }
        } finally {
            g2.dispose();
        }
    }

    /** 下拉列表项渲染器：跟随 Fluent 令牌，禁用项用弱化色 */
    private static class FluentRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JComponent c = (JComponent) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            c.setFont(FluentTypography.body());
            c.setBorder(new javax.swing.border.EmptyBorder(4, 8, 4, 8));
            if (isSelected) {
                c.setBackground(FluentColors.accentSubtle());
                c.setForeground(FluentColors.text());
            } else {
                c.setBackground(FluentColors.flyout());
                c.setForeground(FluentColors.text());
            }
            c.setOpaque(true);
            return c;
        }
    }
}
