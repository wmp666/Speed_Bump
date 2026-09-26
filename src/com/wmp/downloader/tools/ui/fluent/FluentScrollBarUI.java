package com.wmp.downloader.tools.ui.fluent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Fluent 悬浮式滚动条。
 *
 * <h3>与 Swing / FlatLaf 默认滚动条的差别</h3>
 * <ul>
 *   <li>去掉两端的箭头按钮（Win10/11 的滚动条没有箭头按钮）；</li>
 *   <li>不画轨道——滚动区域的内容直接延伸到底，视觉上更干净；</li>
 *   <li>滑块常态只有 3px 细条，指针进入滚动区域后展开为 8px 并加深，可拖动区域一直是整条宽度；</li>
 *   <li>滑块为全圆角胶囊形。</li>
 * </ul>
 *
 * <p>继承 {@link BasicScrollBarUI} 而不是从零实现：拖动、键盘、鼠标滚轮、
 * 点击轨道翻页这些交互它都已经处理好，重造没有意义。这里只换绘制。</p>
 *
 * <p>颜色全部来自 {@link FluentColors}（即当前 Look &amp; Feel），
 * 所以主题切换后不需要额外处理。</p>
 */
public class FluentScrollBarUI extends BasicScrollBarUI {

    /**
     * Swing 通过 {@code UIDefaults.getUI()} 反射调用<b>静态</b>方法
     * {@code createUI(JComponent)} 来创建 UI 实例，<b>不是</b>调用无参构造器。
     *
     * <p>这里必须显式定义它。否则会「继承」父类 {@link BasicScrollBarUI#createUI}，
     * 而那个方法返回的是 {@code new BasicScrollBarUI()}——于是 {@code UIManager} 里
     * 明明写着 {@code FluentScrollBarUI}，实际装上的却是普通滚动条，
     * 而且<b>不报任何错</b>。这个坑排查起来非常费劲，务必保留此方法。</p>
     */
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent c) {
        return new FluentScrollBarUI();
    }

    private boolean hover;

    // ==================================================================
    // 安装
    // ==================================================================

    @Override
    protected void installDefaults() {
        super.installDefaults();
        // 不画轨道，因此自身必须透明，否则会盖住下层内容（本项目大量面板是透明的）
        scrollbar.setOpaque(false);
        scrollbar.setBorder(null);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                scrollbar.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                scrollbar.repaint();
            }
        });
    }

    // ==================================================================
    // 按钮：完全去掉
    // ==================================================================

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

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // 悬浮式滚动条不画轨道
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);

            boolean vertical = scrollbar.getOrientation() == JScrollBar.VERTICAL;
            int trackThickness = vertical ? scrollbar.getWidth() : scrollbar.getHeight();
            boolean active = hover || isDragging;

            // 常态 3px 细条，悬停/拖动时展开到 8px；始终不超过轨道厚度
            int wanted = active ? FluentMetrics.SCROLLBAR_THICK : FluentMetrics.SCROLLBAR_THIN;
            int thickness = Math.max(2, Math.min(wanted, trackThickness - 2));

            Color base = active ? FluentColors.scrollThumbHover() : FluentColors.scrollThumb();
            Color color = FluentColors.alpha(base, active ? 0xC0 : 0x66);

            int x;
            int y;
            int w;
            int h;
            if (vertical) {
                x = thumbBounds.x + (thumbBounds.width - thickness) / 2;
                y = thumbBounds.y;
                w = thickness;
                h = Math.max(thumbBounds.height, FluentMetrics.SCROLLBAR_MIN_THUMB);
            } else {
                x = thumbBounds.x;
                y = thumbBounds.y + (thumbBounds.height - thickness) / 2;
                w = Math.max(thumbBounds.width, FluentMetrics.SCROLLBAR_MIN_THUMB);
                h = thickness;
            }
            FluentPainting.fill(g2, FluentPainting.roundRect(x, y, w, h, FluentMetrics.RADIUS_PILL), color);
        } finally {
            g2.dispose();
        }
    }

    /** 让不确定态/悬停的重绘及时生效 */
    @Override
    protected void paintDecreaseHighlight(Graphics g) {
        // 点击轨道时的「翻页高亮」也用主题色淡化块替代默认的实心填充
    }

    @Override
    protected void paintIncreaseHighlight(Graphics g) {
        // 同上
    }
}
