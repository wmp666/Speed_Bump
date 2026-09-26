package com.wmp.downloader.tools.ui.fluent;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.beans.PropertyChangeListener;

/**
 * 进度条增强：<b>只接管「不确定模式」的绘制，其余一切交给当前 Look &amp; Feel</b>。
 *
 * <h3>职责划分</h3>
 * <table border="1">
 *   <caption>谁画什么</caption>
 *   <tr><th>状态</th><th>由谁绘制</th></tr>
 *   <tr><td>{@code isIndeterminate() == true}</td>
 *       <td>本类：一条高亮带单向扫过（Fluent / WinUI 的不确定进度观感）</td></tr>
 *   <tr><td>确定进度（含文本、方向、边框、内边距、圆角、尺寸）</td>
 *       <td>当前 Look &amp; Feel 自己的 {@code ProgressBarUI}，逐像素保持一致</td></tr>
 * </table>
 *
 * <p>这样一来，确定态进度条的外观<b>完全跟随主题</b>——FlatLaf 的 Mac Light/Dark、
 * Darcula、IntelliJ，以及 Metal / System 等外观下都保持它们原本的样式，
 * 本类不会去「重新发明」一个进度条。被替换掉的只有原来那个「方块来回弹跳」的不确定态。</p>
 *
 * <h3>实现方式：装饰器</h3>
 * <p>本类继承 {@link ProgressBarUI}（不是 {@link BasicProgressBarUI}）——
 * 必须如此，因为 {@code JProgressBar.updateUI()} 里有
 * {@code setUI((ProgressBarUI) UIManager.getUI(this))} 的强制类型转换。</p>
 *
 * <p>它在 {@code installUI} 里从 {@code UIManager.getLookAndFeelDefaults()} 取一份
 * <b>当前 L&amp;F 自己的</b>进度条 UI 实例，对组件调用它的 {@code installUI}，
 * 之后所有生命周期与查询方法（首选尺寸、基线、辅助功能等）都转发给它。
 * 只有「确定态绘制」与「不确定态绘制」这两个入口按状态分流。</p>
 *
 * <p>用 {@code UIManager.getLookAndFeelDefaults().getUI(c)} 而不是自己反射构造：
 * 这条路是 JDK 内部机制，能正确实例化
 * {@code com.sun.java.swing.plaf.windows.*} 这类<b>未导出包</b>里的 L&amp;F 实现——
 * 自己反射会因模块封装而失败。</p>
 *
 * <h3>共享实例的保护</h3>
 * <p>部分 L&amp;F（尤其 Windows 系）的 {@code createUI} 会返回<b>单例</b>。
 * 对单例调用 {@code installUI} 会把它的内部状态指向新的组件，
 * 正在使用它的其它进度条会跟着画错。本类通过「连续取两次是否为同一对象」
 * 识别这种情况，命中时改用 {@link BasicProgressBarUI} 兜底：
 * 宁可确定态朴素一点，也不能把别的进度条画坏。</p>
 */
public class FluentProgressBarUI extends ProgressBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new FluentProgressBarUI();
    }

    /** 一次完整扫过的周期（毫秒） */
    private static final long SWEEP_PERIOD_MS = 1400L;

    /** FlatLaf 的「强制方角」客户端属性；用于让扫动带的形状与确定态一致 */
    private static final String CLIENT_PROPERTY_SQUARE = "JProgressBar.square";

    /** 目标进度条 */
    private JProgressBar bar;

    /** 当前 L&F 自己的进度条 UI（或兜底的 Basic 实现） */
    private ComponentUI delegate;

    private Timer sweepTimer;
    private long sweepStart = System.nanoTime();
    private PropertyChangeListener indeterminateListener;

    // ==================================================================
    // 取当前 L&F 自己的进度条 UI
    // ==================================================================

    /**
     * 取一份「当前 Look &amp; Feel 自己的」进度条 UI 实例。
     *
     * <p>只返回可以安全 {@code installUI} 的<b>非共享</b>实例；
     * 取不到、或发现是共享单例时返回 {@code null}，由调用方兜底。</p>
     *
     * <p>公开出来是为了让离屏自检能做「确定态绘制是否与 L&amp;F 完全一致」的逐像素对照。</p>
     */
    public static ComponentUI createLafUi(JComponent c) {
        try {
            UIDefaults lafDefaults = UIManager.getLookAndFeelDefaults();
            // 先判断有没有这个键：缺键时直接调 getUI 会打印一堆错误栈
            if (lafDefaults.get("ProgressBarUI") == null) {
                return null;
            }
            ComponentUI first = lafDefaults.getUI(c);
            if (first == null || first instanceof FluentProgressBarUI) {
                return null;
            }
            // 共享实例检测：再取一次，若仍是同一个对象，说明 L&F 复用了单例，
            // 此时不能对它 installUI
            ComponentUI second = lafDefaults.getUI(c);
            return first == second ? null : first;
        } catch (Throwable t) {
            return null;
        }
    }

    // ==================================================================
    // 生命周期：全部转发给 L&F 的 UI
    // ==================================================================

    @Override
    public void installUI(JComponent c) {
        if (!(c instanceof JProgressBar progressBar)) {
            return;
        }
        bar = progressBar;

        ComponentUI lafUi = createLafUi(c);
        if (lafUi == null) {
            // 兜底：L&F 用的是共享单例（或取不到），退化为 Swing 默认绘制，
            // 至少不会污染其它进度条
            lafUi = new BasicProgressBarUI();
        }
        try {
            lafUi.installUI(c);
            delegate = lafUi;
        } catch (Throwable t) {
            delegate = null;
        }

        indeterminateListener = evt -> updateSweepTimer();
        progressBar.addPropertyChangeListener("indeterminate", indeterminateListener);
        updateSweepTimer();
    }

    @Override
    public void uninstallUI(JComponent c) {
        stopSweepTimer();
        if (bar != null && indeterminateListener != null) {
            bar.removePropertyChangeListener("indeterminate", indeterminateListener);
        }
        indeterminateListener = null;
        if (delegate != null) {
            try {
                delegate.uninstallUI(c);
            } catch (Throwable ignored) {
                // 卸载失败不影响其它流程
            }
            delegate = null;
        }
        bar = null;
    }

    // ==================================================================
    // 绘制：按状态分流
    // ==================================================================

    @Override
    public void update(Graphics g, JComponent c) {
        if (isIndeterminate()) {
            // 不确定态：本类全权接管，不能给 L&F 机会去画它那套来回弹跳的方块
            paint(g, c);
            return;
        }
        if (delegate != null) {
            delegate.update(g, c);
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!isIndeterminate()) {
            // 确定态：完全交给当前 L&F
            if (delegate != null) {
                delegate.paint(g, c);
            }
            return;
        }
        // 组件在不可见时就被设成不确定态的情况：这里补一次动画启动
        if (bar != null && bar.isShowing() && (sweepTimer == null || !sweepTimer.isRunning())) {
            updateSweepTimer();
        }
        paintSweep(g, c);
    }

    private boolean isIndeterminate() {
        return bar != null && bar.isIndeterminate();
    }

    // ==================================================================
    // 扫动动画
    // ==================================================================

    private void updateSweepTimer() {
        if (isIndeterminate()) {
            if (sweepTimer == null) {
                sweepTimer = new Timer(FluentMetrics.TICK, e -> {
                    if (bar == null || !bar.isIndeterminate() || !bar.isShowing()) {
                        stopSweepTimer();
                        return;
                    }
                    bar.repaint();
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

    /**
     * 画不确定态的扫动带。
     *
     * <p>绘制区域跟随组件的 {@code Insets}（与 L&amp;F 的确定态一致）；
     * 内边距大到没有可画区域时退回整个组件，避免「什么都没画」。</p>
     *
     * <p>相位由真实时间推导，而不是「每次重绘累加一点」：定时器的触发间隔并不精确，
     * 累加式相位会让扫动速度随系统负载抖动。</p>
     */
    private void paintSweep(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);

            int x = 0;
            int y = 0;
            int width = c.getWidth();
            int height = c.getHeight();
            if (bar != null) {
                Insets insets = bar.getInsets();
                if (insets != null) {
                    int w = c.getWidth() - insets.left - insets.right;
                    int h = c.getHeight() - insets.top - insets.bottom;
                    if (w > 0 && h > 0) {
                        x = insets.left;
                        y = insets.top;
                        width = w;
                        height = h;
                    }
                }
            }
            if (width <= 0 || height <= 0) {
                return;
            }

            int radius = sweepRadius(c);

            // 轨道：优先用当前 L&F 的进度条底色，取不到才用强调色淡化
            FluentPainting.fill(g2, FluentPainting.roundRect(x, y, width, height, radius),
                    FluentColors.progressTrack());

            // 扫动带
            long elapsed = (System.nanoTime() - sweepStart) / 1_000_000L;
            float phase = (elapsed % SWEEP_PERIOD_MS) / (float) SWEEP_PERIOD_MS;
            float bandWidth = Math.max(height * 3f, width * FluentMetrics.PROGRESS_BAND_RATIO);
            float bandX = x - bandWidth + phase * (width + bandWidth);
            g2.setColor(bar != null && bar.isEnabled()
                    ? FluentColors.accent() : FluentColors.textDisabled());
            g2.fill(FluentPainting.roundRect(bandX, y, bandWidth, height, radius));

            // 不确定态仍然允许显示文本（原本由 BasicProgressBarUI 负责，不能被吞掉）
            if (bar != null && bar.isStringPainted()) {
                paintCenteredString(g2, x, y, width, height);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 扫动带的圆角：与确定态保持一致。
     *
     * <p>两个来源：项目的「方角组件」设置，以及 FlatLaf 的
     * {@code JProgressBar.square} 客户端属性（逐组件强制方角）。
     * 只看前者的话，用户单独把某个进度条设成方角时，
     * 会出现「确定态直角、不确定态胶囊」的割裂。</p>
     */
    private int sweepRadius(JComponent c) {
        if (Boolean.TRUE.equals(c.getClientProperty(CLIENT_PROPERTY_SQUARE))) {
            return 0;
        }
        return FluentMetrics.cornerRadius(FluentMetrics.RADIUS_PILL);
    }

    private void paintCenteredString(Graphics2D g2, int x, int y, int width, int height) {
        String text = bar.getString();
        if (text == null || text.isEmpty()) {
            return;
        }
        g2.setFont(bar.getFont());
        g2.setColor(bar.isEnabled() ? FluentColors.text() : FluentColors.textDisabled());
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (width - fm.stringWidth(text)) / 2;
        int ty = y + (height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    // ==================================================================
    // 查询方法：一律转发给 L&F 的 UI
    // ==================================================================

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return delegate != null ? delegate.getPreferredSize(c) : super.getPreferredSize(c);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return delegate != null ? delegate.getMinimumSize(c) : super.getMinimumSize(c);
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return delegate != null ? delegate.getMaximumSize(c) : super.getMaximumSize(c);
    }

    @Override
    public boolean contains(JComponent c, int x, int y) {
        return delegate != null ? delegate.contains(c, x, y) : super.contains(c, x, y);
    }

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        return delegate != null ? delegate.getBaseline(c, width, height)
                : super.getBaseline(c, width, height);
    }

    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        return delegate != null ? delegate.getBaselineResizeBehavior(c)
                : super.getBaselineResizeBehavior(c);
    }

    @Override
    public int getAccessibleChildrenCount(JComponent c) {
        return delegate != null ? delegate.getAccessibleChildrenCount(c)
                : super.getAccessibleChildrenCount(c);
    }

    @Override
    public javax.accessibility.Accessible getAccessibleChild(JComponent c, int i) {
        return delegate != null ? delegate.getAccessibleChild(c, i)
                : super.getAccessibleChild(c, i);
    }
}
