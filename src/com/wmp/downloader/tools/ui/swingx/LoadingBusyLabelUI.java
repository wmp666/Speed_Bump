package com.wmp.downloader.tools.ui.swingx;

import com.wmp.downloader.tools.file.DataControl;
import org.jdesktop.swingx.painter.BusyPainter;
import org.jdesktop.swingx.plaf.BusyLabelUI;

import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLabelUI;
import java.awt.Dimension;
/**
 * 把「滚动横杠」加载图标接入 SwingX 的 {@code JXBusyLabel}。
 *
 * <p>{@link BusyLabelUI} 的契约只有两个成员，本类就只适配这两个：</p>
 * <table border="1">
 *   <caption>BusyLabelUI 契约与适配方式</caption>
 *   <tr><th>契约</th><th>本类的做法</th></tr>
 *   <tr>
 *     <td>{@code BusyPainter getBusyPainter(Dimension dim)}</td>
 *     <td>返回 {@link LoadingBusyPainter}。{@code dim} 是标签的首选尺寸（可能为 {@code null}）；
 *         真正的绘制尺寸由 SwingX 的 {@code PainterIcon} 在绘制时给出，所以 {@code dim}
 *         只用来做「太小就少画几根横杠」的降级判断。</td>
 *   </tr>
 *   <tr>
 *     <td>{@code int getDelay()}</td>
 *     <td>按「周期 ÷ 帧数」换算成每帧间隔。{@code JXBusyLabel} 会用它建一个 {@code Timer}，
 *         每 tick 把 {@code frame} 加一并重绘——<b>动画是由标签驱动的，painter 不需要自己的定时器</b>。</td>
 *   </tr>
 * </table>
 *
 * <p>其余参数（横杠数量、朝向、圆角、间距、留白、颜色）都不在 {@code BusyLabelUI} 契约里，
 * 因此由 {@link LoadingBusyPainter} 内的默认值给出，也可以拿到 painter 引用后单独调整。</p>
 *
 * <h3>帧数默认值是怎么定的（与项目里既有的泵帧方式对齐）</h3>
 * <p>{@code Downloader.createLazyLoadPanelInMainFrame} 里已经有一段「空白页居中显示等待动画」的代码，
 * 它<b>不依赖标签自带的 Timer</b>，而是自己用一个 <b>80ms</b> 的 {@code Timer}
 * 每拍把 {@code frame} 加一（作者注释说标签自带的 Timer 在 FlatLaf 下推进重绘不可靠）。
 * 于是实际周期 = {@code points × 80ms}：SwingX 默认 8 点 ≈ 0.64 秒，
 * 而如果用 60 点就会变成 <b>4.8 秒转一圈</b>，慢得像卡住。</p>
 *
 * <p>所以默认帧数取 <b>20</b>，周期 1600ms —— 两个驱动方式的时间刚好一致：</p>
 * <ul>
 *   <li>外部 80ms 泵帧：{@code 20 × 80ms = 1600ms}</li>
 *   <li>标签自带 Timer：{@code getDelay() = 1600 / 20 = 80ms}，同样是 1600ms</li>
 * </ul>
 * <p>无论用哪种方式驱动，速度都一样，不需要为驱动方式单独调参。
 * 想让手动泵帧更顺滑，把泵帧间隔降到 30ms 左右、并相应地用
 * {@code new LoadingBusyLabelUI(60, 1600)} 即可。</p>
 *
 * <h3>安装</h3>
 * <pre>{@code
 * LoadingBusyLabelUI.install();          // 启动时调用一次
 * JXBusyLabel label = new JXBusyLabel(new Dimension(48, 48));
 * label.setBusy(true);
 * }</pre>
 */
public class LoadingBusyLabelUI extends BasicLabelUI implements BusyLabelUI {

    /** {@code JXBusyLabel.uiClassID} 的值，也就是 {@code UIManager} 里的键名。 */
    public static final String UI_CLASS_ID = "BusyLabelUI";

    /**
     * 默认一个循环的帧数。
     *
     * <p>取 20 是为了与项目里既有的「80ms 手动泵帧」对齐：{@code 20 × 80ms = 1600ms} 一圈，
     * 而标签自带 Timer 的 {@code getDelay()} 也正好是 80ms，两种驱动方式速度一致。</p>
     */
    public static final int DEFAULT_FRAME_COUNT = 20;
    /** 默认一个循环的时长（毫秒）。 */
    public static final double DEFAULT_PERIOD_MILLIS = 300.0;
    /** 低于该像素尺寸就认为图标太小，减少横杠数量以免糊成一团。 */
    private static final int TINY_SIZE = 24;

    private final int frameCount;
    private final double periodMillis;

    public LoadingBusyLabelUI() {
        this(DEFAULT_FRAME_COUNT, DEFAULT_PERIOD_MILLIS);
    }

    /**
     * @param frameCount   一个循环的帧数（≥2），同时决定 painter 的 {@code points}
     * @param periodMillis 一个循环的时长（毫秒），越小越快
     */
    public LoadingBusyLabelUI(int frameCount, double periodMillis) {
        this.frameCount = Math.max(2, frameCount);
        this.periodMillis = Math.max(120.0, periodMillis);
    }

    /**
     * SwingX 通过 {@code UIDefaults.getUI()} 反射调用<b>静态</b> {@code createUI}，
     * 缺省会继承父类的实现并静默返回错误的 UI。详见 {@link com.wmp.downloader.tools.ui.fluent.FluentScrollBarUI}。
     */
    public static ComponentUI createUI(JComponent c) {
        return new LoadingBusyLabelUI();
    }

    // ==================================================================
    // BusyLabelUI 契约
    // ==================================================================

    @Override
    public BusyPainter getBusyPainter(Dimension dim) {
        LoadingBusyPainter painter = new LoadingBusyPainter(frameCount);
        if (dim != null && Math.min(dim.width, dim.height) < TINY_SIZE) {
            // 图标很小时三根柱 + 多根横杠会糊成一片，降到 1 根更清楚
            painter.setBarCount(1);
        }
        painter.setCornerRadius(DataControl.get("is_use_square_component", true)?0:0.5);
        painter.setDirection(BusyPainter.Direction.LEFT);
        painter.setOrientation(LoadingBusyPainter.Orientation.HORIZONTAL);
        return painter;
    }

    @Override
    public int getDelay() {
        return Math.max(1, (int) Math.round(periodMillis / frameCount));
    }

    // ==================================================================
    // 安装
    // ==================================================================

    /**
     * 把本 UI 装到 {@code UIManager}。
     *
     * <pre>{@code
     * LoadingBusyLabelUI.install();   // 启动时调用一次即可
     * }</pre>
     *
     * <h3>不需要额外维护默认值（实测结论）</h3>
     * <p>SwingX 的 {@code BusyLabelAddon} 会在 {@code JXBusyLabel} 类初始化时往
     * {@code UIManager} 写 {@code BusyLabelUI = BasicBusyLabelUI}。
     * 曾担心这会覆盖我们写入的值，于是加过「先强制类初始化再写入」之类的动作——
     * <b>实测是多余的</b>：{@code UIManager.put} 写的是「用户默认值」，
     * 优先级高于 addon 写进 L&amp;F 默认表的值，无论先 put 还是先加载类都生效；
     * 而且 {@code UIManager.setLookAndFeel(...)} 会<b>保留</b>用户默认值，
     * 切换主题（含跨 L&amp;F 家族）之后新建的标签依然拿到本 UI。
     * 所以这里只做一次 put，不挂任何主题回调。</p>
     */
    public static void install() {
        UIManager.put(UI_CLASS_ID, LoadingBusyLabelUI.class.getName());
    }
}
