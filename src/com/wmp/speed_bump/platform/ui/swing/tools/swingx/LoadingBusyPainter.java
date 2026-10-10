package com.wmp.speed_bump.platform.ui.swing.tools.swingx;

import org.jdesktop.swingx.painter.BusyPainter;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * 把「滚动横杠」加载图标的绘制搬进 SwingX 的 {@link BusyPainter}。
 *
 * <p>配合 {@link LoadingBusyLabelUI} 使用：把它交给 {@code JXBusyLabel}，
 * 标签的忙碌动画就会变成这个图标，而<b>动画驱动仍然由 {@code JXBusyLabel} 负责</b>
 * （它自带一个 {@code Timer}，每次把 {@code frame} 加一并重绘）。</p>
 *
 * <h3>与 SwingX 自带 {@code BusyPainter} 的关系</h3>
 * <p>{@code AbstractPainter.paint(...)} 是 {@code final} 的，唯一的绘制扩展点就是
 * {@code protected void doPaint(...)}，因此本类覆写它，完全替换掉「旋转圆点」的画法。
 * 尺寸来自 {@code PainterIcon} 传入的 icon 宽高，
 * 相位来自 {@link #getFrame()} / {@link #getPoints()}——
 * 这两条是 {@code BusyLabelUI} 契约里唯一能被适配的输入。</p>
 *
 * <h3>与原始独立组件的三处刻意差异</h3>
 * <ol>
 *   <li><b>不再自己开 Timer。</b>原始素材是个 {@code JComponent}，自带 16ms 定时器与 {@code phase} 字段；
 *       而 {@code JXBusyLabel} 已经按 {@code BusyLabelUI.getDelay()} 驱动帧了，
 *       两个定时器会互相打乱。这里改为「帧 → 相位」，见 {@link #phase()}。</li>
 *   <li><b>不再用背景色抠槽。</b>原实现把中间柱画成实心，再用背景色把横杠画上去「抠」出槽位——
 *       这要求背景必须是不透明纯色。作为标签图标，背景往往是透明的或带背景图，
 *       那样会画出一条突兀的色带。这里改用 {@link Area} 布尔减：把槽位从墨色区域里减掉，
 *       槽位自然透出下层，在任何背景上都成立，视觉与原实现一致。</li>
 *   <li><b>所有参数从静态字段改为实例字段。</b>原素材把 {@code cornerRadius}、{@code SIDE_BARS}
 *       等放在 {@code static} 上（它只被一个演示窗口使用）。同一个 painter 类会被多个标签共用，
 *       静态共享会让「两个标签设不同圆角」互相覆盖。这里全部改为实例状态。</li>
 * </ol>
 *
 * <h3>颜色</h3>
 * <p>默认墨色取<b>标签当前的前景色</b>（{@code target.getForeground()}），因此会跟随主题切换；
 * 默认不绘制背景（{@code backgroundColor == null}），不会在标签上盖一块底色。</p>
 *
 * <p>刻意<b>不</b>读取 SwingX 的 {@code JXBusyLabel.baseColor} / {@code highlightColor}：
 * 那两个值在 L&amp;F 安装时就被固化进 {@code UIManager}（{@code highlightColor} 甚至是当时
 * 抓取的 {@code Label.foreground}），启动后切主题不会更新，作为长期运行的加载指示反而会串色。</p>
 */
public class LoadingBusyPainter extends BusyPainter {

    // ================================================================ 设计坐标系
    // 全部使用 256x256 设计坐标（沿用原始素材的轮廓），绘制时等比缩放到 icon 尺寸。

    private static final double DESIGN = 256.0;

    /** 左右两侧细柱：宽度、上下端 y、中心 x。 */
    private static final double SIDE_BAR_W = 25.5;
    private static final double SIDE_BAR_TOP = 35.5;
    private static final double SIDE_BAR_BOTTOM = 219.5;
    private static final double SIDE_BAR_CX_LEFT = 63.0;
    private static final double SIDE_BAR_CX_RIGHT = 192.5;

    /** 中间柱柱身。 */
    private static final double MID_LEFT = 91.0;
    private static final double MID_RIGHT = 164.0;
    private static final double MID_TOP = 36.0;
    private static final double MID_BOTTOM = 220.0;

    /** 中间柱边界（与圆角无关，决定横杠活动范围）。 */
    private static final Rectangle2D MID_BOUNDS =
            new Rectangle2D.Double(MID_LEFT, MID_TOP, MID_RIGHT - MID_LEFT, MID_BOTTOM - MID_TOP);

    /** 四周留白比例：绘制时留出 6% 边距。 */
    private static final double MARGIN_FACTOR = 0.94;

    /** 墨色兜底值（取不到组件前景色时）。 */
    private static final Color DEFAULT_INK = new Color(0x11, 0x11, 0x11);

    /**
     * 图标的自然尺寸（像素）。
     *
     * <p>取 64 是为了与项目既有约定一致：{@code Downloader} 里那段等待动画
     * 显式给 {@code JXBusyLabel} 设的就是 {@code 64×64}。
     * 标签自己显式设过尺寸时以标签为准（如 {@code Downloader} 那样），本值只在标签没设时生效。</p>
     */
    public static final int DEFAULT_ICON_SIZE = 64;

    // ================================================================ 参数（代码内默认值）

    /**
     * 动画模式。
     *
     * <ul>
     *   <li>{@link Mode#FLOW}（默认）：横杠按方向匀速滚动，出了柱身从另一端进来。</li>
     *   <li>{@link Mode#YOYO}：横杠在柱内上下往复，到两端自然减速再折返。</li>
     * </ul>
     */
    public enum Mode {
        /** 单向流动，循环滚动。 */
        FLOW,
        /** 上下往复。 */
        YOYO
    }

    /** 横杠的滚动方向。 */
    public enum Direction {
        /** 向下 / 向右滚动（默认）。 */
        DOWN,
        /** 向上 / 向左滚动。 */
        UP
    }

    /** 整幅图形的朝向。 */
    public enum Orientation {
        /** 柱身竖直，横杠水平，上下滚动（默认）。 */
        VERTICAL,
        /** 整幅图形旋转 90°，柱身水平，横杠竖直，左右滚动。 */
        HORIZONTAL
    }

    private Mode mode = Mode.FLOW;
    private Orientation orientation = Orientation.VERTICAL;
    /** 横杠数量（1..12），默认 3。 */
    private int barCount = 3;
    /** 端头间隙系数（0.02..0.45）：横杠端头到柱身边框的留白占柱身高度比例，默认 0.1。 */
    private double endGapRatio = 0.1;
    /** 横向间隙系数（0..0.45）：横杠左右到柱身边框的留白占柱身宽度比例，默认 0.15。 */
    private double sideGapRatio = 0.15;
    /** 横杠间距倍率：间距 = 横杠长度 × 该倍率，默认 1.6。 */
    private double spacingFactor = 1.6;
    /** 圆角程度（0..1），默认 0.5；0 = 全直角，1 = 最圆。 */
    private double cornerRadius = 0.5;

    /** 墨色；{@code null} 表示跟随组件前景色。 */
    private Color inkColor;
    /** 背景色；{@code null} 表示不绘制背景（默认）。 */
    private Color backgroundColor;

    // ---- 随圆角重建的图形（实例状态，不是 static） ----
    private Area sideBars;
    private Area midBar;

    // ================================================================ 构造

    public LoadingBusyPainter() {
        this(20);
    }

    /**
     * @param frameCount 一个循环的帧数；应与 {@link LoadingBusyLabelUI#getDelay()} 一起决定动画快慢
     */
    public LoadingBusyPainter(int frameCount) {
        super();
        setPoints(Math.max(2, frameCount));
        // 动画每一帧都不同，缓存会让画面停住。
        // JXBusyLabel.initPainter 也会关掉它，但本类可能被用在别处，自己关一次更稳妥。
        setCacheable(false);
        declareNaturalSize(DEFAULT_ICON_SIZE);
        rebuildShapes();
    }

    /**
     * 声明图标的「自然尺寸」，供没有显式设置尺寸的 {@code JXBusyLabel} 使用。
     *
     * <h3>为什么必须声明</h3>
     * <p>{@code JXBusyLabel.getBusyPainter()} 对一个没设过首选尺寸的标签是这样算尺寸的：</p>
     * <pre>
     *   Dimension prefSize = getPreferredSize();                     // (0,0)
     *   busyPainter = createBusyPainter(null);
     *   if (!isPreferredSizeSet() &amp;&amp; prefSize 为空) {
     *       Rectangle rt = busyPainter.getTrajectory().getBounds();
     *       Rectangle rp = busyPainter.getPointShape().getBounds();
     *       int max = Math.max(rp.width, rp.height);
     *       prefSize = new Dimension(rt.width + max, rt.height + max);
     *   }
     * </pre>
     * <p>注意它<b>不看</b> {@code painter.getPreferredSize()}，只看轨迹形状与点形状的边界。
     * 而父类的默认构造函数 {@code BusyPainter()} 会把这两个形状按 <b>26px</b> 缩放，
     * 于是像 {@code PreloadDialog.form} 里那种「{@code new JXBusyLabel()} 且不设尺寸」的标签
     * 只会被撑到 <b>26×26</b>——图标小到几乎看不清，窗口 {@code pack()} 也跟着缩得很窄。</p>
     *
     * <h3>怎么声明</h3>
     * <p>本类的绘制完全不用这两个形状（{@link #doPaint} 按传入的宽高自行缩放），
     * 所以这里把它们纯粹当「尺寸声明」用：轨迹设成 {@code size × size}，
     * 点形状设成<b>空</b>矩形，于是 {@code prefSize} 恰好等于 {@code size × size}。
     * 颜色参数与绘制无关（墨色实际取自标签前景色），沿用父类的中性值。</p>
     */
    private void declareNaturalSize(int size) {
        int s = Math.max(8, size);
        init(new java.awt.geom.Rectangle2D.Float(0, 0, 0, 0),
                new java.awt.geom.Rectangle2D.Float(0, 0, s, s),
                Color.LIGHT_GRAY, Color.BLACK);
    }

    // ================================================================ 绘制

    @Override
    protected void doPaint(Graphics2D g, Object target, int width, int height) {
        if (width <= 0 || height <= 0) {
            // 标签还没有尺寸（JXBusyLabel 在首选尺寸为 0 时会把 icon 尺寸设成 0），直接不画
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            if (backgroundColor != null) {
                g2.setColor(backgroundColor);
                g2.fillRect(0, 0, width, height);
            }

            // 等比缩放到 256 设计坐标系并居中
            double scale = Math.min(width, height) / DESIGN * MARGIN_FACTOR;
            g2.translate((width - DESIGN * scale) / 2.0, (height - DESIGN * scale) / 2.0);
            g2.scale(scale, scale);

            // 横向模式：整幅图形（三根柱 + 滚动方向）一起旋转 90°
            if (orientation == Orientation.HORIZONTAL) {
                g2.rotate(Math.PI / 2.0, DESIGN / 2.0, DESIGN / 2.0);
            }

            g2.setColor(resolveInk(target));
            g2.fill(inkArea());
        } finally {
            g2.dispose();
        }
    }

    /**
     * 当前帧要涂成墨色的区域：三根柱子（含中间柱）减去柱内滚动的槽位。
     *
     * <p>这就是不用背景色抠槽的做法——槽位根本不被涂色，于是自然透出下层背景，
     * 在透明背景、背景图、任意主题色上都成立。</p>
     */
    private Area inkArea() {
        Area area = new Area(sideBars);
        area.add(midBar);
        area.subtract(slotArea());
        return area;
    }

    /** 当前帧的槽位（横杠）集合，已由中间柱边界裁掉超出部分。 */
    private Area slotArea() {
        double h = barLength();
        double pitch = basePitch();
        double offset = scrollOffset(pitch) * (scrollDirection() == Direction.UP ? -1.0 : 1.0);

        double top = MID_BOUNDS.getMinY();
        double bottom = MID_BOUNDS.getMaxY();
        double first = top + h / 2.0;          // 相位 0 时第一根横杠正好贴住柱身顶端
        // 从头铺到尾，两端各多铺一根，保证滚动过程中始终有杠可画
        int n = (int) Math.ceil((bottom - top) / pitch) + 2;

        Area slots = new Area();
        double barWidth = fixedBarWidth();
        double cx = (MID_BOUNDS.getMinX() + MID_BOUNDS.getMaxX()) / 2.0;
        for (int i = 0; i < n; i++) {
            double cy = first + i * pitch + offset;
            slots.add(new Area(roundRectPath(cx - barWidth / 2.0, cy - h / 2.0, barWidth, h)));
        }
        // 超出柱身的横杠直接裁掉（滚出柱身时表现为被两端裁断，而不是缩小）
        slots.intersect(midBar);
        return slots;
    }

    /** 动画相位 0..1，由 SwingX 的帧计数推导。 */
    private double phase() {
        int frameCount = Math.max(1, getPoints());
        // stopAnimation 会把 frame 设为 -1，必须能处理负数（floorMod 保证结果非负）
        return Math.floorMod(getFrame(), frameCount) / (double) frameCount;
    }

    /**
     * 横杠的滚动方向。
     *
     * <p>适配 SwingX 的 {@code setDirection}：它的 {@code RIGHT}（默认）/ {@code LEFT}
     * 原本表示旋转方向，这里映射为横杠的滚动方向，这样 {@code label.setDirection(...)}
     * 依然是有意义的；若真的不需要，用 {@link #setMode} 等方法更直接。</p>
     */
    private Direction scrollDirection() {
        return getDirection() == BusyPainter.Direction.LEFT ? Direction.UP : Direction.DOWN;
    }

    /** 墨色：优先显式设置，其次组件前景色（跟随主题），最后兜底。 */
    private Color resolveInk(Object target) {
        if (inkColor != null) {
            return inkColor;
        }
        if (target instanceof Component component) {
            Color foreground = component.getForeground();
            if (foreground != null) {
                return foreground;
            }
        }
        return DEFAULT_INK;
    }

    // ================================================================ 几何

    /** 圆角程度变化后重建受影响的图形。 */
    private void rebuildShapes() {
        double h = SIDE_BAR_BOTTOM - SIDE_BAR_TOP;
        Area bars = new Area(roundRectPath(SIDE_BAR_CX_LEFT - SIDE_BAR_W / 2.0, SIDE_BAR_TOP, SIDE_BAR_W, h));
        bars.add(new Area(roundRectPath(SIDE_BAR_CX_RIGHT - SIDE_BAR_W / 2.0, SIDE_BAR_TOP, SIDE_BAR_W, h)));
        sideBars = bars;
        midBar = new Area(roundRectPath(MID_LEFT, MID_TOP, MID_RIGHT - MID_LEFT, MID_BOTTOM - MID_TOP));
    }

    /** 按当前圆角程度换算实际圆角半径。 */
    private double cornerRadiusPx(double maxRadius) {
        double f = Math.clamp(cornerRadius, 0.0, 1.0);
        return Math.max(0.0, Math.min(maxRadius * f, maxRadius));
    }

    /** 按当前圆角程度生成圆角矩形路径（r = 0 时即直角矩形）。 */
    private Path2D.Double roundRectPath(double x, double y, double w, double h) {
        double r = cornerRadiusPx(Math.min(w, h) / 2.0);
        Path2D.Double p = new Path2D.Double(Path2D.WIND_NON_ZERO);
        p.moveTo(x + r, y);
        p.lineTo(x + w - r, y);
        if (r > 0) {
            p.quadTo(x + w, y, x + w, y + r);
        }
        p.lineTo(x + w, y + h - r);
        if (r > 0) {
            p.quadTo(x + w, y + h, x + w - r, y + h);
        }
        p.lineTo(x + r, y + h);
        if (r > 0) {
            p.quadTo(x, y + h, x, y + h - r);
        }
        p.lineTo(x, y + r);
        if (r > 0) {
            p.quadTo(x, y, x + r, y);
        }
        p.closePath();
        return p;
    }

    /** 端头间隙（设计坐标）：横杠端头到柱身边框的留白。 */
    private double endGap() {
        return Math.max(1.0, MID_BOUNDS.getHeight() * endGapRatio);
    }

    /**
     * 按「n 根横杠 + 两端各留端头间隙、间距 = 横杠长度 × 间距倍率」解出横杠长度。
     *
     * <p>几何关系：{@code n*L + (n-1)*(pitch-L) + 2*endGap = 柱身高}，
     * 其中 {@code pitch = L * spacingFactor}，所以</p>
     * <pre>L = (柱身高 - 2*端头间隙) / (n + (n-1)*(间距倍率 - 1))</pre>
     */
    private double barLength() {
        double g = endGap();
        double usable = Math.max(4.0, MID_BOUNDS.getHeight() - 2.0 * g);
        int n = Math.max(1, barCount);
        double denom = n + (n - 1) * (spacingFactor - 1.0);
        return Math.max(4.0, usable / Math.max(1.0, denom));
    }

    /** 滚动的基准间距。 */
    private double basePitch() {
        return Math.max(barLength() * spacingFactor, barLength() * 1.05);
    }

    /** 一个周期内滚动的距离（等于一个间距，滚动一个间距后图案自身重合）。 */
    private double scrollOffset(double pitch) {
        double phase = phase();
        if (mode == Mode.YOYO) {
            return pitch * 0.5 * (1.0 - Math.cos(2 * Math.PI * phase));
        }
        return pitch * phase;
    }

    /** 横杠长度方向上的宽度：柱身宽度减去两侧横向间隙（固定，滚动时边缘不跳变）。 */
    private double fixedBarWidth() {
        double span = MID_BOUNDS.getWidth();
        double usable = span * (1.0 - Math.max(0.0, Math.min(0.45, sideGapRatio)) * 2.0);
        return Math.max(4.0, Math.min(span, usable));
    }

    // ================================================================ 设置

    public Mode getMode() {
        return mode;
    }

    /** 设置动画模式，默认 {@link Mode#FLOW}。 */
    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.FLOW : mode;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    /** 设置整幅图形朝向，默认 {@link Orientation#VERTICAL}。 */
    public void setOrientation(Orientation orientation) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
    }

    public int getBarCount() {
        return barCount;
    }

    /** 设置横杠数量，取值 1..12；默认 3。 */
    public void setBarCount(int barCount) {
        this.barCount = Math.max(1, Math.min(12, barCount));
    }

    public double getCornerRadius() {
        return cornerRadius;
    }

    /** 设置圆角程度 0..1（0 = 全直角，1 = 最圆），默认 0.5。 */
    public void setCornerRadius(double cornerRadius) {
        this.cornerRadius = Math.clamp(cornerRadius, 0.0, 1.0);
        rebuildShapes();
    }

    public double getEndGapRatio() {
        return endGapRatio;
    }

    /** 设置端头间隙系数 0.02..0.45，默认 0.1（越大横杠越短、黑色越多）。 */
    public void setEndGapRatio(double endGapRatio) {
        this.endGapRatio = Math.max(0.02, Math.min(0.45, endGapRatio));
    }

    public double getSideGapRatio() {
        return sideGapRatio;
    }

    /** 设置横向间隙系数 0..0.45，默认 0.15（横杠左右各留出该比例的留白）。 */
    public void setSideGapRatio(double sideGapRatio) {
        this.sideGapRatio = Math.max(0.0, Math.min(0.45, sideGapRatio));
    }

    public double getSpacingFactor() {
        return spacingFactor;
    }

    /** 设置横杠间距倍率 1.0..5.0，默认 1.6。 */
    public void setSpacingFactor(double spacingFactor) {
        this.spacingFactor = Math.max(1.0, Math.min(5.0, spacingFactor));
    }

    public Color getInkColor() {
        return inkColor;
    }

    /** 显式指定墨色；传 {@code null} 表示跟随组件前景色（默认）。 */
    public void setInkColor(Color inkColor) {
        this.inkColor = inkColor;
    }

    public Color getLoadingBackground() {
        return backgroundColor;
    }

    /** 绘制背景色；传 {@code null}（默认）表示不绘制背景。 */
    public void setLoadingBackground(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    // ---- 供自检读取的内部量 ----

    /** 当前横杠长度（设计坐标），供自检使用。 */
    public double barLengthForTest() {
        return barLength();
    }

    /** 当前横杠宽度（设计坐标），供自检使用。 */
    public double barWidthForTest() {
        return fixedBarWidth();
    }

    /** 当前相位 0..1，供自检使用。 */
    public double phaseForTest() {
        return phase();
    }
}
