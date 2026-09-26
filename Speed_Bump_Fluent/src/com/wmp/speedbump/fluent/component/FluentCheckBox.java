package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Fluent 复选框。
 *
 * <p>选中时方框填充主题色并画一个白色对勾（用图标字体的 CheckMark 字形，与 WinUI 同源）。</p>
 */
public class FluentCheckBox extends JToggleButton implements FluentTheme.ThemeAware {

    /** 未选中：普通方框；选中：主题色实底 */
    private int boxSize = FluentMetrics.CHECKBOX_SIZE;

    public FluentCheckBox() {
        this(null, false);
    }

    public FluentCheckBox(String text) {
        this(text, false);
    }

    public FluentCheckBox(String text, boolean selected) {
        super(text, selected);
        setFont(FluentTypography.body());
        FluentPainting.makeTransparent(this);
        setRolloverEnabled(true);
        RevealEngine.register(this);
        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        repaint();
    }

    public FluentCheckBox tip(String tooltip) {
        setToolTipText(tooltip);
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : FluentTypography.body());
        int textWidth = getText() == null ? 0 : fm.stringWidth(getText());
        int gap = textWidth > 0 ? FluentMetrics.SPACING_S : 0;
        int width = boxSize + gap + textWidth;
        int height = Math.max(FluentMetrics.CONTROL_HEIGHT, fm.getHeight() + 8);
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            int h = getHeight();
            int y = (h - boxSize) / 2;
            boolean enabled = isEnabled();
            boolean checked = isSelected();

            Shape box = FluentPainting.roundRect(0.5, y + 0.5, boxSize - 1, boxSize - 1,
                    FluentMetrics.RADIUS_SMALL);
            Shape halo = FluentPainting.roundRect(0, 0, getWidth(), getHeight(), FluentMetrics.RADIUS_SMALL);
            RevealEngine.paint(this, g2, halo);

            Color fill;
            if (!enabled) {
                fill = FluentColors.controlDisabled();
            } else if (checked) {
                fill = getModel().isPressed() ? FluentColors.accentPressed()
                        : getModel().isRollover() ? FluentColors.accentHover() : FluentColors.accent();
            } else {
                fill = getModel().isPressed() ? FluentColors.controlPressed()
                        : getModel().isRollover() ? FluentColors.controlHover() : FluentColors.control();
            }
            FluentPainting.fill(g2, box, fill);
            if (!checked || !enabled) {
                FluentPainting.stroke(g2, box, FluentColors.stroke(), FluentMetrics.BORDER);
            }

            if (checked && enabled) {
                Color mark = FluentColors.onAccent();
                g2.setColor(mark);
                if (FluentTypography.iconFontAvailable()) {
                    var font = FluentIcons.font(15);
                    g2.setFont(font);
                    FontMetrics fm = g2.getFontMetrics(font);
                    String glyph = String.valueOf(FluentIcons.CHECK);
                    int gw = fm.stringWidth(glyph);
                    int baseline = y + (boxSize - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(glyph, (boxSize - gw) / 2f, baseline);
                } else {
                    // 无图标字体：画一条对勾折线
                    g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                            java.awt.BasicStroke.JOIN_ROUND));
                    g2.drawLine(5, y + boxSize / 2, boxSize / 2 - 1, y + boxSize - 6);
                    g2.drawLine(boxSize / 2 - 1, y + boxSize - 6, boxSize - 5, y + 5);
                }
            }

            String text = getText();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setFont(getFont());
                g2.setColor(enabled ? FluentColors.text() : FluentColors.textDisabled());
                g2.drawString(text, boxSize + FluentMetrics.SPACING_S, baseline);
            }

            if (isFocusOwner()) {
                FluentPainting.focusRing(g2, this, FluentMetrics.RADIUS_SMALL);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // 自绘
    }
}
