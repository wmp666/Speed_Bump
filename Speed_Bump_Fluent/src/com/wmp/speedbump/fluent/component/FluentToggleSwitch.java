package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JToggleButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Fluent 开关（ToggleSwitch）。
 *
 * <p>布局与 WinUI 一致：<b>文字在左、开关在右</b>。开关本体是一个 40×20 的胶囊轨道，
 * 内部 12px 滑块；开时轨道填充主题色、滑块为黑/白反色，关时轨道为半透明控件底、滑块为次要文字色。</p>
 *
 * <p>滑块位置做了 150ms 的位移动画（WinUI 的开关手感来自这个位移，不是瞬间跳变）。</p>
 */
public class FluentToggleSwitch extends JToggleButton implements FluentTheme.ThemeAware {

    private static final int ANIMATION_MS = 150;
    private static final int TICK = 16;

    /** 滑块归一化位置：0 = 关，1 = 开 */
    private float knobPosition;
    private Timer animation;

    public FluentToggleSwitch() {
        this(null, false);
    }

    public FluentToggleSwitch(String text) {
        this(text, false);
    }

    public FluentToggleSwitch(String text, boolean selected) {
        super(text, selected);
        setFont(FluentTypography.body());
        FluentPainting.makeTransparent(this);
        setRolloverEnabled(true);
        knobPosition = selected ? 1f : 0f;
        RevealEngine.register(this);
        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        repaint();
        addItemListener(e -> animateTo(isSelected() ? 1f : 0f));
    }

    /** 链式：设置提示 */
    public FluentToggleSwitch tip(String tooltip) {
        setToolTipText(tooltip);
        return this;
    }

    private void animateTo(float target) {
        if (animation != null && animation.isRunning()) {
            animation.stop();
        }
        animation = new Timer(TICK, null);
        animation.addActionListener(e -> {
            float delta = target - knobPosition;
            if (Math.abs(delta) < 0.02f) {
                knobPosition = target;
                animation.stop();
            } else {
                knobPosition += delta * 0.35f;
            }
            repaint();
        });
        animation.start();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : FluentTypography.body());
        int textWidth = getText() == null ? 0 : fm.stringWidth(getText());
        int gap = textWidth > 0 ? FluentMetrics.SPACING_M : 0;
        int width = textWidth + gap + FluentMetrics.SWITCH_WIDTH;
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
            int trackW = FluentMetrics.SWITCH_WIDTH;
            int trackH = FluentMetrics.SWITCH_HEIGHT;
            int trackX = getWidth() - trackW;
            int trackY = (h - trackH) / 2;

            boolean enabled = isEnabled();
            boolean on = isSelected();

            // ---- 文字 ----
            String text = getText();
            if (text != null && !text.isEmpty()) {
                FontMetrics fm = g2.getFontMetrics(getFont());
                int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setFont(getFont());
                g2.setColor(enabled ? FluentColors.text() : FluentColors.textDisabled());
                g2.drawString(text, 0, baseline);
            }

            // ---- 轨道 ----
            Shape track = FluentPainting.roundRect(trackX, trackY, trackW, trackH,
                    FluentMetrics.RADIUS_PILL);
            Color trackColor;
            if (!enabled) {
                trackColor = FluentColors.controlDisabled();
            } else if (on) {
                trackColor = getModel().isRollover() || getModel().isPressed()
                        ? FluentColors.accentHover() : FluentColors.accent();
            } else {
                trackColor = getModel().isRollover() || getModel().isPressed()
                        ? FluentColors.controlPressed() : FluentColors.control();
            }
            FluentPainting.fill(g2, track, trackColor);
            if (!on || !enabled) {
                FluentPainting.stroke(g2, FluentPainting.roundRect(trackX + 0.5, trackY + 0.5,
                        trackW - 1, trackH - 1, FluentMetrics.RADIUS_PILL),
                        FluentColors.stroke(), FluentMetrics.BORDER);
            }

            // ---- 滑块 ----
            int knobOuter = FluentMetrics.SWITCH_KNOB;
            int knobInner = FluentMetrics.SWITCH_KNOB_INNER;
            float travel = trackW - knobOuter - 8; // 左右各留 4px
            float knobX = trackX + 4 + travel * knobPosition;
            float knobY = trackY + (trackH - knobOuter) / 2f;

            Color knobColor;
            if (!enabled) {
                knobColor = FluentColors.textDisabled();
            } else if (on) {
                // 开：滑块为黑或白，与外层主题色形成反色对比（WinUI 规则）
                knobColor = FluentColors.onAccent();
                if (FluentColors.luminance(knobColor) < 0.5) {
                    knobColor = Color.BLACK;
                }
            } else {
                knobColor = FluentColors.textSecondary();
                if (getModel().isRollover() || getModel().isPressed()) {
                    knobColor = FluentColors.text();
                }
            }

            // 外层实心圆
            g2.setColor(knobColor);
            g2.fillOval(Math.round(knobX), Math.round(knobY), knobOuter, knobOuter);

            // 内层小圆：关态用一个「空洞」造型，开态直接实心
            if (!on && enabled) {
                g2.setColor(FluentColors.solid());
                float innerX = knobX + (knobOuter - knobInner) / 2f;
                float innerY = knobY + (knobOuter - knobInner) / 2f;
                g2.fillOval(Math.round(innerX), Math.round(innerY), knobInner, knobInner);
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
