package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * Fluent 进度条。
 *
 * <p>WinUI 的进度条只有 4px 高、全圆角，不使用渐变，填充色就是主题色。
 * 这种「细、圆、纯色」的造型与 Material 的厚进度条完全不同，是辨识度很高的细节。</p>
 *
 * <p>不确定态（{@link #setIndeterminate(boolean)}）绘制一条来回扫动的高亮带，
 * 动画由组件自身驱动，仅在可见时才运行，避免后台无谓重绘。</p>
 */
public class FluentProgressBar extends JComponent {

    private static final int TICK = 16;

    private double value;
    private boolean indeterminate;
    private int barHeight = 4;
    private Timer timer;
    /** 不确定态相位，0..1 */
    private float phase;

    public FluentProgressBar() {
        this(0);
    }

    public FluentProgressBar(double value) {
        setValue(value);
        setOpaque(false);
    }

    // ==================================================================
    // 属性
    // ==================================================================

    /** 进度值，0..100 */
    public void setValue(double value) {
        this.value = Math.max(0, Math.min(100, value));
        repaint();
    }

    public double getValue() {
        return value;
    }

    public void setIndeterminate(boolean indeterminate) {
        if (this.indeterminate == indeterminate) {
            return;
        }
        this.indeterminate = indeterminate;
        if (indeterminate) {
            startTimer();
        } else {
            stopTimer();
        }
        repaint();
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public FluentProgressBar setBarHeight(int height) {
        this.barHeight = height;
        revalidate();
        repaint();
        return this;
    }

    // ==================================================================
    // 尺寸
    // ==================================================================

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, barHeight);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(24, barHeight);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, barHeight);
    }

    // ==================================================================
    // 动画
    // ==================================================================

    private void startTimer() {
        if (timer == null) {
            timer = new Timer(TICK, e -> {
                phase += 0.012f;
                if (phase > 1.4f) {
                    phase = -0.4f;
                }
                repaint();
            });
        }
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void stopTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (indeterminate) {
            startTimer();
        }
    }

    @Override
    public void removeNotify() {
        stopTimer();
        super.removeNotify();
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            int w = getWidth();
            int h = Math.min(getHeight(), Math.max(barHeight, 1));
            int y = (getHeight() - h) / 2;
            int radius = FluentMetrics.RADIUS_PILL;

            Shape track = FluentPainting.roundRect(0, y, w, h, radius);
            FluentPainting.fill(g2, track, FluentColors.accentSubtle());

            g2.setColor(FluentColors.accent());
            if (indeterminate) {
                // 高亮带宽度为整体的 35%，从左侧外扫到右侧外
                double bandWidth = w * 0.35;
                double x = -bandWidth + phase * (w + bandWidth);
                Shape band = FluentPainting.roundRect(x, y, bandWidth, h, radius);
                g2.fill(band);
            } else if (value > 0) {
                double filled = Math.max(h, w * value / 100.0);
                Shape fill = FluentPainting.roundRect(0, y, filled, h, radius);
                g2.fill(fill);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            stopTimer();
        }
        repaint();
    }
}
