package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Fluent 多行文本框。
 *
 * <p>与 {@link FluentTextBox} 同样的「外壳 + 内层文本组件」结构；内层是
 * {@link JTextArea}，外面套一个只有纵向滚动条的 {@link JScrollPane}。</p>
 *
 * <p>占位提示由本类<b>手工绘制</b>，而不是依赖 Look&amp;Feel 的
 * {@code placeholderText} 客户端属性——FlatLaf 对 {@code JTextArea} 的支持取决于版本，
 * 自己画三行代码就没有不确定性。</p>
 */
public class FluentTextArea extends JPanel implements FluentTheme.ThemeAware {

    private final JTextArea area = new JTextArea();
    private final FluentScrollPane scroll;
    private String placeholder;
    private final int radius = FluentMetrics.RADIUS_SMALL;

    public FluentTextArea() {
        this(null, 4);
    }

    public FluentTextArea(String placeholder) {
        this(placeholder, 4);
    }

    public FluentTextArea(String placeholder, int rows) {
        this.placeholder = placeholder;
        setLayout(new BorderLayout());
        setOpaque(false);

        area.setOpaque(false);
        area.setBackground(new Color(0, 0, 0, 0));
        area.setFont(FluentTypography.body());
        area.setForeground(FluentColors.text());
        area.setCaretColor(FluentColors.text());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(rows);
        area.setBorder(new javax.swing.border.EmptyBorder(
                FluentMetrics.TEXT_PADDING_Y + 4, FluentMetrics.TEXT_PADDING_X,
                FluentMetrics.TEXT_PADDING_Y, FluentMetrics.TEXT_PADDING_X));

        scroll = new FluentScrollPane(area);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        area.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                repaint();
            }
        });
        area.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                repaint();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                repaint();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                repaint();
            }
        });

        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        area.setForeground(FluentColors.text());
        area.setCaretColor(FluentColors.text());
        repaint();
    }

    public String getText() {
        return area.getText();
    }

    public void setText(String text) {
        area.setText(text);
    }

    public void setPlaceholder(String text) {
        this.placeholder = text;
        repaint();
    }

    public JTextArea area() {
        return area;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 200), Math.max(d.height, 96));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();
            boolean focused = area.isFocusOwner();

            Shape body = FluentPainting.roundRect(0.5, 0.5, w - 1, h - 1, radius);
            FluentPainting.fill(g2, body, enabled ? FluentColors.control() : FluentColors.controlDisabled());
            FluentPainting.stroke(g2, body, FluentColors.stroke(), FluentMetrics.BORDER);

            if (enabled) {
                if (focused) {
                    g2.setColor(FluentColors.accent());
                    g2.fillRect(1, h - 2, w - 2, 2);
                } else {
                    Color line = FluentColors.textSecondary();
                    g2.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(), 0x59));
                    g2.fillRect(1, h - 1, w - 2, 1);
                }
            }

            // 占位提示
            if (placeholder != null && area.getText().isEmpty()) {
                g2.setFont(FluentTypography.body());
                g2.setColor(FluentColors.textDisabled());
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int baseline = FluentMetrics.TEXT_PADDING_Y + 4 + fm.getAscent();
                g2.drawString(placeholder, FluentMetrics.TEXT_PADDING_X, baseline);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        return area.requestFocusInWindow();
    }

    @Override
    public void requestFocus() {
        area.requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        area.setEnabled(enabled);
        repaint();
    }
}
