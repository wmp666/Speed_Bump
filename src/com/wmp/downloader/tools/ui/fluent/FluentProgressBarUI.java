package com.wmp.downloader.tools.ui.fluent;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.beans.PropertyChangeListener;

/**
 * Fluent 进度条。
 *
 * <h3>相比项目默认进度条的变化</h3>
 * <ol>
 *   <li><b>不确定态有扫动动画</b>：默认实现是 {@code BasicProgressBarUI} 的
 *       「方块来回弹跳」，这里换成一条高亮带单向扫过并首尾衔接，即 WinUI 的观感。
 *       项目里有 5 处真实使用 {@code setIndeterminate(true)}
 *       （拓展安装、Gopeed 文件大小未知、FFmpeg 合并、HTTP 长度探测），不是为好看而好看。</li>
 *   <li><b>颜色跟随主题</b>：填充取 {@link FluentColors#accent()}，轨道取
 *       {@link FluentColors#progressTrack()}，切主题/改强调色自动跟上。</li>
 *   <li><b>形状服从项目既有偏好</b>：{@code is_use_square_component} 为真时画直角条，
 *       为假时画胶囊条——不会出现「按钮直角、进度条圆角」的自相矛盾。</li>
 * </ol>
 *
 * <h3>两个刻意的实现决定</h3>
 * <p><b>1）绘制区域是整个组件，不做「细条居中」。</b>
 * 细条居中意味着组件有一半区域是透明的，而透明区域必须依赖父容器正确重绘，
 * 在本项目这种「根面板画背景图 + 大量透明子面板」的结构里很容易出现残影。
 * 所以细度交给布局解决：{@link com.wmp.downloader.tools.ui.UITools#createProgressBarPanel}
 * 把bar 高度从 12px 改成 6px，UI 只管把整个组件画满。</p>
 *
 * <p><b>2）自带动画定时器，不依赖基类的。</b>
 * 因为本类接管了 {@code paint}，基类在 {@code paint} 里启动动画定时器的那段逻辑不会执行——
 * 如果继续依赖它，扫动动画会「画一次就不动了」。这个坑必须在接管 paint 时一并处理。</p>
 */
public class FluentProgressBarUI extends BasicProgressBarUI {

    /**
     * 必须显式定义：{@code UIDefaults.getUI()} 调用的是静态 {@code createUI}，
     * 缺省会继承 {@link BasicProgressBarUI#createUI} 并返回普通进度条 UI，
     * 于是 {@code UIManager} 里的配置形同虚设且不报错。详见 {@link FluentScrollBarUI#createUI}。
     */
    public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
        return new FluentProgressBarUI();
    }

    /** 一次完整扫过的周期（毫秒） */
    private static final long SWEEP_PERIOD_MS = 1400L;

    private Timer sweepTimer;
    private long sweepStart = System.nanoTime();
    private PropertyChangeListener indeterminateListener;

    // ==================================================================
    // 动画驱动
    // ==================================================================

    @Override
    protected void installListeners() {
        super.installListeners();
        // indeterminate 变化时立即启停动画，不等到下一次 paint
        indeterminateListener = evt -> updateSweepTimer();
        if (progressBar != null) {
            progressBar.addPropertyChangeListener("indeterminate", indeterminateListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (progressBar != null && indeterminateListener != null) {
            progressBar.removePropertyChangeListener("indeterminate", indeterminateListener);
        }
        stopSweepTimer();
        super.uninstallListeners();
    }

    @Override
    public void uninstallUI(JComponent c) {
        stopSweepTimer();
        super.uninstallUI(c);
    }

    private void updateSweepTimer() {
        if (progressBar != null && progressBar.isIndeterminate()) {
            if (sweepTimer == null) {
                sweepTimer = new Timer(FluentMetrics.TICK, e -> {
                    if (progressBar == null || !progressBar.isIndeterminate() || !progressBar.isShowing()) {
                        stopSweepTimer();
                        return;
                    }
                    progressBar.repaint();
                });
                sweepTimer.setCoalesce(true);
            }
            if (!sweepTimer.isRunning()) {
                sweepStart = System.nanoTime();
                sweepTimer.start();
            }
        } else {
            stopSweepTimer();
        }
    }

    private void stopSweepTimer() {
        if (sweepTimer != null && sweepTimer.isRunning()) {
            sweepTimer.stop();
        }
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        // 由 paint 统一接管
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        // 由 paint 统一接管
    }

    /**
     * 跳过 {@code ComponentUI.update} 的背景填充，直接绘制，避免与自绘的圆角冲突。
     */
    @Override
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!(c instanceof JProgressBar bar)) {
            super.paint(g, c);
            return;
        }
        // 万一 indeterminate 是在组件不可见时设置的，这里补一次启停
        if (bar.isIndeterminate() && (sweepTimer == null || !sweepTimer.isRunning()) && bar.isShowing()) {
            updateSweepTimer();
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);

            int width = bar.getWidth();
            int height = bar.getHeight();
            int radius = FluentMetrics.cornerRadius(FluentMetrics.RADIUS_PILL);
            Shape area = FluentPainting.roundRect(0, 0, width, height, radius);

            // 轨道：铺满整个组件
            FluentPainting.fill(g2, area, FluentColors.progressTrack());

            boolean enabled = bar.isEnabled();
            Color fillColor = enabled ? FluentColors.accent() : FluentColors.textDisabled();
            g2.setColor(fillColor);

            if (bar.isIndeterminate()) {
                paintSweep(g2, width, height, radius);
            } else {
                int span = Math.max(1, bar.getMaximum() - bar.getMinimum());
                double ratio = Math.max(0, Math.min(1,
                        (bar.getValue() - bar.getMinimum()) / (double) span));
                if (ratio > 0) {
                    double filled = Math.max(height, width * ratio);
                    g2.fill(FluentPainting.roundRect(0, 0, filled, height, radius));
                }
            }

            if (bar.isStringPainted()) {
                paintString(g2, 0, 0, width, height, 0, bar.getInsets());
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 画不确定态的扫动高亮带。
     *
     * <p>相位由真实时间推导，而不是「每次重绘累加一点」：定时器的触发间隔并不精确，
     * 累加式相位会让扫动速度随系统负载抖动。</p>
     */
    private void paintSweep(Graphics2D g2, int width, int height, int radius) {
        long elapsed = (System.nanoTime() - sweepStart) / 1_000_000L;
        float phase = (elapsed % SWEEP_PERIOD_MS) / (float) SWEEP_PERIOD_MS;
        float bandWidth = Math.max(height * 3f, width * FluentMetrics.PROGRESS_BAND_RATIO);
        // 从完全在左侧之外扫到完全在右侧之外
        float x = -bandWidth + phase * (width + bandWidth);
        g2.fill(FluentPainting.roundRect(x, 0, bandWidth, height, radius));
    }

    /** 文字绘制：居中显示 {@code JProgressBar} 的 string */
    @Override
    protected void paintString(Graphics g, int x, int y, int width, int height, int amountFull, Insets b) {
        JProgressBar bar = progressBar;
        if (bar == null || !bar.isStringPainted()) {
            return;
        }
        String text = bar.getString();
        if (text == null || text.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            g2.setFont(bar.getFont());
            g2.setColor(bar.isEnabled() ? FluentColors.text() : FluentColors.textDisabled());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int tx = x + (width - textWidth) / 2;
            int ty = y + (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }
}
