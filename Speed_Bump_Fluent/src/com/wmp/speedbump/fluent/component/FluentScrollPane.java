package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Fluent 滚动容器。
 *
 * <p>关键差异在滚动条：Windows 的滚动条是<b>悬浮细条</b>——平时只有一个 2px 的淡色指示条，
 * 指针进入滚动区域后才展开成可拖动的滑块。Swing 默认的滚动条带箭头按钮、有边框、常驻显示，
 * 放在 Fluent 界面里非常突兀，所以这里整条替换掉。</p>
 *
 * <p>水平滚动条默认关闭（Fluent 页面是纵向滚动的），需要时用
 * {@link #setHorizontalScrollBarPolicy(int)} 打开，样式同样生效。</p>
 */
public class FluentScrollPane extends JScrollPane {

    public FluentScrollPane() {
        this(null);
    }

    public FluentScrollPane(Component view) {
        super(view);
        setBorder(null);
        setOpaque(false);
        setViewportBorder(null);
        getViewport().setOpaque(false);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBar(newBar(JScrollBar.VERTICAL));
        setHorizontalScrollBar(newBar(JScrollBar.HORIZONTAL));
        getVerticalScrollBar().setUnitIncrement(18);
        getHorizontalScrollBar().setUnitIncrement(18);
        setWheelScrollingEnabled(true);
    }

    private JScrollBar newBar(int orientation) {
        JScrollBar bar = new JScrollBar(orientation);
        bar.setOpaque(false);
        bar.setUnitIncrement(18);
        bar.setUI(new FluentScrollBarUI());
        return bar;
    }

    /** 便捷：设置视口视图 */
    public FluentScrollPane view(Component view) {
        getViewport().setView(view);
        return this;
    }

    @Override
    public void setViewportView(Component view) {
        if (view instanceof JComponent jc) {
            jc.setOpaque(false);
        }
        super.setViewportView(view);
        if (getViewport() != null) {
            getViewport().setOpaque(false);
            // 必须用 SIMPLE_SCROLL_MODE，不能用 JViewport 的默认值 BLIT_SCROLL_MODE。
            // BLIT 滚动是「把已有像素块搬移」，只有视口不透明、父级背景不因滚动而需要
            // 重画时才安全。本项目的视口叠在半透明材质层之上，滚动后暴露出来的区域
            // 必须由下层材质重新绘制——BLIT 不会触发这个重绘，结果是滚动拖影。
            getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        }
    }

    /**
     * 悬浮式滚动条 UI。
     *
     * <p>继承 {@link BasicScrollBarUI} 是因为它已经处理好了拖动、键盘、鼠标滚轮与
     * 「点击轨道翻页」的全部交互逻辑，只需要把绘制换掉——重造这些交互没有意义。</p>
     */
    public static class FluentScrollBarUI extends BasicScrollBarUI {

        private boolean hover;

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton b = new JButton();
            Dimension zero = new Dimension(0, 0);
            b.setPreferredSize(zero);
            b.setMinimumSize(zero);
            b.setMaximumSize(zero);
            b.setFocusable(false);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // 悬浮式滚动条不画轨道
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !c.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                boolean vertical = c.getWidth() <= c.getHeight();
                int thickness = vertical ? c.getWidth() : c.getHeight();
                boolean active = hover || isDragging;
                // 常态 3px 细条，悬停/拖动时展开到 8px，并加深颜色
                int visual = active ? Math.min(8, thickness - 2) : Math.min(3, thickness - 2);
                visual = Math.max(2, visual);

                Color base = FluentColors.textSecondary();
                Color color = active
                        ? new Color(base.getRed(), base.getGreen(), base.getBlue(), 0xB0)
                        : new Color(base.getRed(), base.getGreen(), base.getBlue(), 0x59);

                int x, y, w, h;
                if (vertical) {
                    x = (thumbBounds.x * 2 + thumbBounds.width) / 2 - visual / 2;
                    y = thumbBounds.y;
                    w = visual;
                    h = thumbBounds.height;
                } else {
                    x = thumbBounds.x;
                    y = (thumbBounds.y * 2 + thumbBounds.height) / 2 - visual / 2;
                    w = thumbBounds.width;
                    h = visual;
                }
                FluentPainting.fill(g2,
                        FluentPainting.roundRect(x, y, w, h, FluentMetrics.RADIUS_PILL), color);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    scrollbar.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    scrollbar.repaint();
                }
            });
        }
    }
}
