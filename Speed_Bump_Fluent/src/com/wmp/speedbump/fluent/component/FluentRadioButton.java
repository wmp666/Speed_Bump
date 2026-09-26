package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
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
 * Fluent 单选框。
 *
 * <p>选中时外圈填充主题色，内部一个反色实心圆点。多个组件用
 * {@link javax.swing.ButtonGroup} 组合即可获得互斥行为——{@link JToggleButton} 天然支持。</p>
 */
public class FluentRadioButton extends JToggleButton implements FluentTheme.ThemeAware {

    private int size = FluentMetrics.RADIO_SIZE;

    public FluentRadioButton() {
        this(null, false);
    }

    public FluentRadioButton(String text) {
        this(text, false);
    }

    public FluentRadioButton(String text, boolean selected) {
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

    public FluentRadioButton tip(String tooltip) {
        setToolTipText(tooltip);
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : FluentTypography.body());
        int textWidth = getText() == null ? 0 : fm.stringWidth(getText());
        int gap = textWidth > 0 ? FluentMetrics.SPACING_S : 0;
        return new Dimension(size + gap + textWidth,
                Math.max(FluentMetrics.CONTROL_HEIGHT, fm.getHeight() + 8));
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
            int y = (h - size) / 2;
            boolean enabled = isEnabled();
            boolean checked = isSelected();

            RevealEngine.paint(this, g2, FluentPainting.roundRect(0, 0, getWidth(), getHeight(),
                    FluentMetrics.RADIUS_SMALL));

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

            // 外圈：圆形用足够大的圆角矩形退化实现，比 fillOval 更容易与描边对齐
            Shape outer = FluentPainting.roundRect(0.5, y + 0.5, size - 1, size - 1, FluentMetrics.RADIUS_PILL);
            FluentPainting.fill(g2, outer, fill);
            if (!checked || !enabled) {
                FluentPainting.stroke(g2, outer, FluentColors.stroke(), FluentMetrics.BORDER);
            } else {
                FluentPainting.stroke(g2, outer, FluentColors.solid(), FluentMetrics.BORDER);
            }

            if (checked && enabled) {
                int dot = 8;
                Color dotColor = FluentColors.onAccent();
                g2.setColor(dotColor);
                float dx = (size - dot) / 2f;
                float dy = y + (size - dot) / 2f;
                g2.fillOval(Math.round(dx), Math.round(dy), dot, dot);
            }

            String text = getText();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setFont(getFont());
                g2.setColor(enabled ? FluentColors.text() : FluentColors.textDisabled());
                g2.drawString(text, size + FluentMetrics.SPACING_S, baseline);
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
