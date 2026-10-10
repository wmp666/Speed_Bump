package com.wmp.speed_bump.platform.ui.swing.tools;

import com.wmp.speed_bump.common.background.tools.StringFormat;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.painter.BusyPainter;

import javax.swing.*;
import java.awt.*;

/**
 * 懒加载标签页的「加载中」遮罩：把一个 {@link JXBusyLabel} 转圈动画严格居中地
 * 盖在空白容器上，内容就绪后再把它取下。
 *
 * <p>这里最关键的是 <b>调用时机</b>，而不是绘制本身：</p>
 * <ol>
 *   <li>{@link #show()} 必须在后台构建任务 <b>开始之前</b>、在 EDT 上调用，
 *       否则动画永远没有机会被看到；</li>
 *   <li>{@link #hide(boolean)} 必须在内容构建 <b>完成之后</b>、在 EDT 上调用
 *       （{@link SwingWorker#done()} 正好满足这两个条件）。</li>
 * </ol>
 *
 * <p>把这两步单独抽出来还有一个好处：它们不依赖主窗口，可以脱离整个应用被直接测试
 * （用一个 {@code JPanel} + {@code JFrame} 即可驱动完整流程，并断言后台任务运行期间
 * 动画确实显示且 {@code BusyPainter} 的帧在推进）。</p>
 *
 * <p>每个被懒加载的容器对应一个实例，构造后不能跨容器复用。</p>
 */
public class LazyLoadOverlay {

    /** 转圈动画的首选尺寸（正方形） */
    private static final int BUSY_SIZE = 64;

    private final JPanel panel;
    private final JXBusyLabel busyLabel;
    private final JPanel busyHost;

    /**
     * @param panel 内容为空的容器，必须使用 {@link BorderLayout}
     */
    public LazyLoadOverlay(JPanel panel) {
        this.panel = panel;
        this.busyLabel = createBusyLabel();
        this.busyHost = createBusyHost(this.busyLabel);
    }

    /**
     * 创建等待动画组件。必须在 EDT 上创建。
     */
    private static JXBusyLabel createBusyLabel() {
        JXBusyLabel label = new JXBusyLabel();
        label.setPreferredSize(new Dimension(BUSY_SIZE, BUSY_SIZE));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        // 确保 BusyPainter 被初始化：JXBusyLabel 内部推进帧的 Timer 依赖它，
        // 而 SwingX 只有在部分代码路径上才会自动创建它。
        BusyPainter busyPainter = label.getBusyPainter();
        if (busyPainter != null) {
            busyPainter.setPaintCentered(true);
        }
        return label;
    }

    /**
     * 用 GridBagLayout 包一层，让动画居中显示而不是被拉伸铺满整个容器。
     * 容器保持透明，以免遮盖标签页自身的背景与圆角。
     */
    private static JPanel createBusyHost(JXBusyLabel busyLabel) {
        JPanel host = new JPanel(new GridBagLayout());
        host.setOpaque(false);
        host.add(busyLabel);
        return host;
    }

    /**
     * 挂上遮罩并启动动画。必须在 EDT 上调用，且必须在后台任务开始之前调用。
     */
    public void show() {
        panel.removeAll();
        panel.add(busyHost, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
        // 必须先让组件进入显示层级再启动：JXBusyLabel 靠内部 Timer 推进帧并 repaint，
        // 未显示时拿不到有效的绘制上下文，动画不会推进。
        busyLabel.setBusy(true);
        busyLabel.repaint();
    }

    /**
     * 取下遮罩，露出内容构建任务已经放进容器里的组件。必须在 EDT 上调用。
     *
     * <p>这里刻意 <b>不用</b> {@code panel.removeAll()}：内容是由调用方
     * {@code add} 进同一个容器的，removeAll 会把刚构建好的内容一起清掉
     * ——这正是之前动画和内容互相覆盖的根源。</p>
     *
     * @param failed 内容构建是否失败；失败时容器里通常什么都没有，
     *               这时给一句提示，而不是留一片空白
     */
    public void hide(boolean failed) {
        busyLabel.setBusy(false); // 停掉 JXBusyLabel 内部的动画 Timer
        panel.remove(busyHost);
        if (failed && panel.getComponentCount() == 0) {
            JLabel failedLabel = new JLabel(
                    StringFormat.translate("common", "common.load_failed"), SwingConstants.CENTER);
            failedLabel.setEnabled(false);
            panel.add(failedLabel, BorderLayout.CENTER);
        }
        panel.revalidate();
        panel.repaint();
    }

    /** 遮罩当前是否显示在容器上（主要供测试与调试使用） */
    public boolean isShowing() {
        return busyHost.getParent() == panel;
    }

    /** 等待动画当前是否在运行（主要供测试与调试使用） */
    public boolean isBusy() {
        return busyLabel.isBusy();
    }
}
