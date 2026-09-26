package com.wmp.speedbump.fluent.ui;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Fluent 标题栏。
 *
 * <p>无边框窗口的标题栏需要自己实现三件事：拖动窗口、双击最大化/还原、以及右上角的
 * 三个标题栏按钮。这里全部由本类承担，窗口本身只提供 {@link FluentWindow} 的
 * {@code moveTo / toggleMaximize / minimize / close} 几个动作。</p>
 *
 * <h3>为什么按钮图标是矢量画的，而不是用图标字体</h3>
 * <p>WinUI 的最小化/最大化/关闭是极简的 1px 线条图形，用 {@code Segoe MDL2 Assets}
 * 的字形渲染出来反而比矢量线更粗、更糊（字形本身带 hinting）。
 * 直接用 {@code Graphics2D} 画 10×10 的线条，在任何 DPI 下都锐利，
 * 而且彻底摆脱「系统没有该字体就变成方块」的风险。</p>
 */
public class FluentTitleBar extends JPanel implements FluentTheme.ThemeAware {

    private final FluentWindow window;
    private final JLabel titleLabel = new JLabel();
    private final JLabel iconLabel = new JLabel();
    private final CaptionButton minimizeButton;
    private final CaptionButton maximizeButton;
    private final CaptionButton closeButton;

    /** 拖动状态 */
    private Point dragOffset;
    private boolean dragging;

    public FluentTitleBar(FluentWindow window) {
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(100, FluentMetrics.TITLE_BAR_HEIGHT));

        // ---- 左侧：图标 + 标题 ----
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,
                FluentMetrics.SPACING_M, 0));
        left.setOpaque(false);
        left.setBorder(new javax.swing.border.EmptyBorder(
                (FluentMetrics.TITLE_BAR_HEIGHT - 16) / 2, FluentMetrics.SPACING_S, 0, 0));
        iconLabel.setPreferredSize(new Dimension(16, 16));
        left.add(iconLabel);
        titleLabel.setFont(FluentTypography.caption());
        titleLabel.setForeground(FluentColors.text());
        left.add(titleLabel);
        add(left, BorderLayout.WEST);

        // ---- 右侧：标题栏按钮 ----
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        minimizeButton = new CaptionButton(CaptionButton.Type.MINIMIZE);
        maximizeButton = new CaptionButton(CaptionButton.Type.MAXIMIZE);
        closeButton = new CaptionButton(CaptionButton.Type.CLOSE);
        right.add(minimizeButton);
        right.add(maximizeButton);
        right.add(closeButton);
        add(right, BorderLayout.EAST);

        // ---- 拖动 / 双击最大化 ----
        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 落在窗口边缘时不拖动（那是缩放手势）
                if (window.isResizing() || window.isOnResizeEdge(e.getPoint())) {
                    return;
                }
                dragOffset = e.getPoint();
                dragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !window.isOnResizeEdge(e.getPoint())) {
                    window.toggleMaximize();
                }
            }
        };
        addMouseListener(dragHandler);
        left.addMouseListener(dragHandler);
        titleLabel.addMouseListener(dragHandler);
        iconLabel.addMouseListener(dragHandler);

        MouseAdapter dragMotion = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging || dragOffset == null) {
                    return;
                }
                // 最大化状态下拖动标题栏：WinUI 会先还原窗口再跟随指针
                if (window.isMaximized()) {
                    double ratio = e.getPoint().x / (double) Math.max(1, getWidth());
                    window.restoreFromMaximize();
                    dragOffset = new Point((int) (window.getWidth() * ratio), e.getPoint().y);
                }
                Point screen = e.getLocationOnScreen();
                window.moveTo(screen.x - dragOffset.x, screen.y - dragOffset.y);
            }
        };
        addMouseMotionListener(dragMotion);
        left.addMouseMotionListener(dragMotion);
        titleLabel.addMouseMotionListener(dragMotion);
        iconLabel.addMouseMotionListener(dragMotion);

        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        titleLabel.setForeground(FluentColors.text());
    }

    // ==================================================================
    // 内容
    // ==================================================================

    public void setTitle(String text) {
        titleLabel.setText(text == null ? "" : text);
    }

    /** 设置 16×16 应用图标；传 {@code null} 时显示一个占位方块 */
    public void setAppIcon(BufferedImage image) {
        if (image == null) {
            iconLabel.setIcon(null);
            iconLabel.repaint();
            return;
        }
        iconLabel.setIcon(new javax.swing.ImageIcon(
                image.getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH)));
    }

    /** 最大化状态变化时切换按钮图形 */
    public void updateMaximizeState(boolean maximized) {
        maximizeButton.setType(maximized ? CaptionButton.Type.RESTORE : CaptionButton.Type.MAXIMIZE);
    }

    /** 让标题栏按钮获得焦点能力（键盘可访问性） */
    public void focusCloseButton() {
        closeButton.requestFocusInWindow();
    }

    // ==================================================================
    // 标题栏按钮
    // ==================================================================

    /**
     * 标题栏按钮：46×32，悬停时出现底色；关闭按钮悬停变红。
     */
    public class CaptionButton extends JComponent {

        /** 按钮图形类型 */
        public enum Type {
            MINIMIZE, MAXIMIZE, RESTORE, CLOSE
        }

        private Type type;
        private boolean hover;
        private boolean pressed;

        CaptionButton(Type type) {
            this.type = type;
            setPreferredSize(new Dimension(FluentMetrics.CAPTION_BUTTON_WIDTH,
                    FluentMetrics.CAPTION_BUTTON_HEIGHT));
            setFocusable(true);
            setCursor(Cursor.getDefaultCursor());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                    if (hover && contains(e.getPoint())) {
                        perform();
                    }
                }
            });
        }

        void setType(Type type) {
            this.type = type;
            repaint();
        }

        private void perform() {
            switch (type) {
                case MINIMIZE -> window.setWindowMinimized();
                case MAXIMIZE -> window.toggleMaximize();
                case RESTORE -> window.restoreFromMaximize();
                case CLOSE -> window.closeWindow();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                Color background = null;
                Color glyph = FluentColors.text();
                if (type == Type.CLOSE && (hover || pressed)) {
                    // Windows 的关闭按钮悬停是固定的红色，不跟随主题色
                    background = pressed ? new Color(0xB3, 0x22, 0x1C) : new Color(0xC4, 0x2B, 0x1C);
                    glyph = Color.WHITE;
                } else if (pressed) {
                    background = FluentColors.subtlePressed();
                } else if (hover) {
                    background = FluentColors.subtleHover();
                }
                if (background != null) {
                    g2.setColor(background);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                paintGlyph(g2, glyph);
            } finally {
                g2.dispose();
            }
        }

        /** 画 10×10 的线条图形 */
        private void paintGlyph(Graphics2D g2, Color color) {
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                    java.awt.BasicStroke.JOIN_MITER));
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int half = 5;
            int left = cx - half;
            int top = cy - half;
            int right = cx + half;
            int bottom = cy + half;
            switch (type) {
                case MINIMIZE -> g2.drawLine(left, cy, right, cy);
                case MAXIMIZE -> g2.drawRect(left, top, half * 2, half * 2);
                case RESTORE -> {
                    // 后窗只画上边与右边，前窗画完整方框并向下偏移
                    g2.drawLine(left + 2, top, right, top);
                    g2.drawLine(right, top, right, bottom - 2);
                    g2.drawRect(left, top + 2, half * 2 - 2, half * 2 - 2);
                }
                case CLOSE -> {
                    g2.drawLine(left, top, right, bottom);
                    g2.drawLine(left, bottom, right, top);
                }
            }
        }
    }

    /** 便捷：创建并设置标题 */
    public static FluentTitleBar of(FluentWindow window, String title) {
        FluentTitleBar bar = new FluentTitleBar(window);
        bar.setTitle(title);
        return bar;
    }
}
