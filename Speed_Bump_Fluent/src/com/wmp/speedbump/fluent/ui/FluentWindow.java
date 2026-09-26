package com.wmp.speedbump.fluent.ui;

import com.wmp.speedbump.fluent.backdrop.BackdropMaterial;
import com.wmp.speedbump.fluent.backdrop.NativeBackdrop;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

/**
 * Fluent 无边框窗口。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>把窗口做成<b>逐像素透明 + 无边框</b>，为原生材质（Mica / Acrylic / Blur）铺好前提；</li>
 *   <li>自绘材质之上的「材质层」（半透明表面 + 1px 描边）；</li>
 *   <li>提供无边框窗口缺失的三件事：边缘缩放、标题栏拖动、最大化/还原/最小化/关闭。</li>
 * </ul>
 *
 * <h3>为什么不自己画圆角</h3>
 * <p>看起来「自己把窗口四角画圆」很容易，但原生模糊（{@code SetWindowCompositionAttribute}）
 * 作用在<b>整个窗口矩形</b>上：即使应用把四角画成透明，那四块区域露出的仍是模糊层，
 * 结果就是「圆角外壳 + 方角模糊」，比直角更难看。所以圆角交给 DWM 的
 * {@code DWMWA_WINDOW_CORNER_PREFERENCE}（Windows 11 生效）；
 * Windows 10 上 DWM 不支持，窗口就是直角——这与 Windows 10 上其它 Fluent 应用的表现一致。</p>
 *
 * <h3>边缘缩放与「非客户区」的关系</h3>
 * <p>无边框窗口没有命中测试区，Swing 也不会把边缘的几个像素交给窗口管理器。
 * 这里用全局 {@link AWTEventListener} 监听鼠标移动/拖动，在距离边缘 {@link #RESIZE_MARGIN}
 * 像素内改光标并进入缩放模式。用全局监听而不是给每个子组件挂监听器的原因：
 * 鼠标事件由<b>最深的那个子组件</b>接收，而边缘区域往往被内容组件盖住，
 * 挂在窗口上的 {@code MouseMotionListener} 根本收不到。</p>
 */
public class FluentWindow extends JFrame {

    /** 边缘缩放的热区宽度（px） */
    public static final int RESIZE_MARGIN = 6;

    private static final int EDGE_NORTH = 1;
    private static final int EDGE_SOUTH = 2;
    private static final int EDGE_WEST = 4;
    private static final int EDGE_EAST = 8;

    private final FluentTitleBar titleBar;
    private final LayerPane layerPane;
    private final JPanel contentHost = new JPanel(new BorderLayout());

    /** 期望的材质（可能被系统降级，实际生效的见 {@link #appliedMaterial()}） */
    private BackdropMaterial requestedMaterial = BackdropMaterial.ACRYLIC;
    private BackdropMaterial appliedMaterial = BackdropMaterial.NONE;
    private boolean backdropPrepared;
    private boolean backdropApplied;
    /** 材质是否已真正生效（区别于「尝试过」） */
    private boolean backdropOk;

    // ---- 缩放状态 ----
    private int resizeEdges;
    private boolean resizing;
    private Point resizeStartScreen;
    private Rectangle resizeStartBounds;
    private AWTEventListener globalMouseListener;

    // ---- 最大化状态 ----
    private boolean maximized;
    private Rectangle restoreBounds;

    public FluentWindow(String title) {
        super(title);
        setUndecorated(true);
        setMinimumSize(new Dimension(760, 520));
        fitToWorkArea(1200, 780);
        centerOnScreen();

        contentHost.setOpaque(false);
        layerPane = new LayerPane();
        layerPane.setLayout(new BorderLayout());
        titleBar = new FluentTitleBar(this);
        titleBar.setTitle(title);
        layerPane.add(titleBar, BorderLayout.NORTH);
        layerPane.add(contentHost, BorderLayout.CENTER);
        setContentPane(layerPane);
    }

    // ==================================================================
    // 内容
    // ==================================================================

    /** 设置窗口主体内容（标题栏之下） */
    public FluentWindow setContent(JComponent content) {
        contentHost.removeAll();
        content.setOpaque(false);
        contentHost.add(content, BorderLayout.CENTER);
        contentHost.revalidate();
        contentHost.repaint();
        return this;
    }

    public JPanel contentHost() {
        return contentHost;
    }

    public FluentTitleBar titleBar() {
        return titleBar;
    }

    // ==================================================================
    // 显示与材质
    // ==================================================================

    @Override
    public void setVisible(boolean visible) {
        if (visible && !backdropPrepared) {
            // 透明必须在窗口「实现」之前设置，否则重设需要先 dispose 再显示
            backdropPrepared = NativeBackdrop.prepareTransparent(this);
        }
        super.setVisible(visible);
        if (visible && !backdropApplied) {
            applyBackdrop();
        }
    }

    /**
     * 应用（或重新应用）背景材质。
     *
     * <h3>为什么必须带重试</h3>
     * <p>原生材质的调用需要窗口的 Win32 句柄，而句柄是通过
     * {@code EnumWindows + IsWindowVisible} 枚举本进程窗口拿到的。
     * 在 {@code setVisible(true)} 返回后<b>立即</b>调用时，窗口还没进入可见状态，
     * 枚举结果为空 → 句柄为 0 → 判定「材质不可用」→ 永久退化成不透明背景。</p>
     *
     * <p>这个失败模式非常隐蔽：应用明明报告「原生材质可用」，界面上却完全没有材质效果。
     * 实测在 Windows 10 19044 上必然触发。因此这里改为「乐观 + 轮询重试」：
     * 先按透明渲染，每 {@code RETRY_DELAY_MS} 毫秒重试一次，最多 {@link #MAX_ATTEMPTS} 次；
     * 只有全部失败才真正退化为不透明。</p>
     *
     * @return 首次尝试是否成功（失败时会自动重试，最终结果见 {@link #isBackdropActive()}）
     */
    public boolean applyBackdrop() {
        backdropApplied = true;
        appliedMaterial = NativeBackdrop.resolve(requestedMaterial);

        if (!NativeBackdrop.isSupported()) {
            backdropOk = false;
            FluentColors.setSolidFallback(true);
            layerPane.repaint();
            return false;
        }

        // 乐观：先允许透明渲染，避免在重试的间隙里界面先变白再变透
        FluentColors.setSolidFallback(false);
        boolean ok = NativeBackdrop.apply(this, appliedMaterial, 0);
        backdropOk = ok;
        layerPane.repaint();
        if (!ok) {
            scheduleRetry(1);
        }
        return ok;
    }

    /** 材质是否已真正生效 */
    public boolean isBackdropActive() {
        return backdropOk;
    }

    private static final int MAX_ATTEMPTS = 25;
    private static final int RETRY_DELAY_MS = 60;

    private void scheduleRetry(int attempt) {
        if (attempt > MAX_ATTEMPTS) {
            // 重试用尽：确实拿不到句柄或系统确实不支持，退化为不透明以保证文字可读
            FluentColors.setSolidFallback(true);
            layerPane.repaint();
            System.out.println("[Speed_Bump_Fluent] 背景材质应用失败，已退化为不透明背景");
            return;
        }
        javax.swing.Timer timer = new javax.swing.Timer(RETRY_DELAY_MS, e -> {
            if (NativeBackdrop.apply(this, appliedMaterial, 0)) {
                backdropOk = true;
                FluentColors.setSolidFallback(false);
                layerPane.repaint();
                System.out.println("[Speed_Bump_Fluent] 背景材质已应用（第 " + attempt + " 次重试，HWND=0x"
                        + Long.toHexString(NativeBackdrop.hwndOf(this)) + "，材质=" + appliedMaterial + "）");
            } else {
                scheduleRetry(attempt + 1);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    /** 期望的材质 */
    public BackdropMaterial requestedMaterial() {
        return requestedMaterial;
    }

    /** 实际生效的材质（可能是降级结果） */
    public BackdropMaterial appliedMaterial() {
        return appliedMaterial;
    }

    /** 运行时切换材质 */
    public FluentWindow setBackdropMaterial(BackdropMaterial material) {
        this.requestedMaterial = material == null ? BackdropMaterial.ACRYLIC : material;
        applyBackdrop();
        return this;
    }

    // ==================================================================
    // 窗口动作（供标题栏调用）
    // ==================================================================

    /** 移动窗口（拖动标题栏时按屏幕坐标调用） */
    public void moveTo(int screenX, int screenY) {
        setLocation(screenX, screenY);
    }

    public boolean isMaximized() {
        return maximized;
    }

    /** 在工作区与还原尺寸之间切换 */
    public void toggleMaximize() {
        if (maximized) {
            restoreFromMaximize();
        } else {
            maximize();
        }
    }

    /**
     * 最大化到<b>工作区</b>。
     *
     * <p>刻意不用 {@code setExtendedState(MAXIMIZED_BOTH)}：无边框窗口在 Windows 上
     * 走那条路会把任务栏一起盖住。{@code GraphicsEnvironment.getMaximumWindowBounds()}
     * 返回的正是排除了任务栏的工作区，用它设置 bounds 更可靠。</p>
     */
    public void maximize() {
        if (maximized) {
            return;
        }
        restoreBounds = getBounds();
        Rectangle work = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setBounds(work);
        maximized = true;
        titleBar.updateMaximizeState(true);
        setCursor(Cursor.getDefaultCursor());
    }

    /** 从最大化还原 */
    public void restoreFromMaximize() {
        if (!maximized) {
            return;
        }
        if (restoreBounds != null) {
            setBounds(restoreBounds);
        }
        maximized = false;
        titleBar.updateMaximizeState(false);
    }

    /** 最小化 */
    public void setWindowMinimized() {
        try {
            setExtendedState(JFrame.ICONIFIED);
        } catch (Throwable t) {
            // 极少数平台上无边框 + 透明窗口无法图标化，忽略即可
        }
    }

    /** 关闭：先派发 WINDOW_CLOSING（可被监听器拦截，例如「还有任务在下载」确认框） */
    public void closeWindow() {
        processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void centerOnScreen() {
        try {
            Rectangle work = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int x = work.x + (work.width - getWidth()) / 2;
            int y = work.y + (work.height - getHeight()) / 2;
            setLocation(Math.max(work.x, x), Math.max(work.y, y));
        } catch (Throwable ignored) {
            setLocationRelativeTo(null);
        }
    }

    /**
     * 把初始尺寸收敛到「工作区减去一圈留白」以内。
     *
     * <p>必须做这一步：固定尺寸的窗口在小屏幕上会<b>直接超出屏幕</b>——
     * 实测在一台 1366×768 的机器上，1200×780 的默认尺寸比屏幕还高 12px，
     * 窗口底部的操作区会永久落到屏幕之外，用户既看不到也点不到。
     * {@code getMaximumWindowBounds()} 返回的是排除了任务栏的工作区，是最可靠的依据。</p>
     */
    private void fitToWorkArea(int preferredWidth, int preferredHeight) {
        int width = preferredWidth;
        int height = preferredHeight;
        try {
            Rectangle work = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            int margin = 48;
            width = Math.min(preferredWidth, Math.max(480, work.width - margin));
            height = Math.min(preferredHeight, Math.max(360, work.height - margin));
        } catch (Throwable ignored) {
            // 取不到工作区时退回首选尺寸
        }
        setSize(width, height);
    }

    // ==================================================================
    // 边缘缩放
    // ==================================================================

    /** 指定窗口内坐标是否落在缩放热区上 */
    public boolean isOnResizeEdge(Point windowPoint) {
        return edgesAt(windowPoint) != 0;
    }

    public boolean isResizing() {
        return resizing;
    }

    private int edgesAt(Point p) {
        if (maximized || p == null) {
            return 0;
        }
        int edges = 0;
        if (p.x < RESIZE_MARGIN) {
            edges |= EDGE_WEST;
        } else if (p.x >= getWidth() - RESIZE_MARGIN) {
            edges |= EDGE_EAST;
        }
        if (p.y < RESIZE_MARGIN) {
            edges |= EDGE_NORTH;
        } else if (p.y >= getHeight() - RESIZE_MARGIN) {
            edges |= EDGE_SOUTH;
        }
        return edges;
    }

    private void updateResizeCursor(Point p) {
        setCursor(cursorFor(edgesAt(p)));
    }

    private Cursor cursorFor(int edges) {
        return switch (edges) {
            case EDGE_NORTH, EDGE_SOUTH -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case EDGE_WEST, EDGE_EAST -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case EDGE_NORTH | EDGE_WEST -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case EDGE_NORTH | EDGE_EAST -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case EDGE_SOUTH | EDGE_WEST -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR);
            case EDGE_SOUTH | EDGE_EAST -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR);
            default -> Cursor.getDefaultCursor();
        };
    }

    @Override
    public void addNotify() {
        super.addNotify();
        installGlobalMouseListener();
    }

    @Override
    public void dispose() {
        uninstallGlobalMouseListener();
        NativeBackdrop.invalidate(this);
        super.dispose();
    }

    private void installGlobalMouseListener() {
        if (globalMouseListener != null) {
            return;
        }
        globalMouseListener = this::handleGlobalMouse;
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener,
                    AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } catch (Throwable t) {
            globalMouseListener = null;
        }
    }

    private void uninstallGlobalMouseListener() {
        if (globalMouseListener == null) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        } catch (Throwable ignored) {
            // 退出阶段失败可忽略
        }
        globalMouseListener = null;
    }

    /**
     * 全局鼠标处理：只关心落在本窗口内的移动与拖动，用于缩放热区与缩放过程。
     */
    private void handleGlobalMouse(AWTEvent event) {
        if (!(event instanceof MouseEvent e) || !isShowing()) {
            return;
        }
        Component source = e.getComponent();
        if (source == null) {
            return;
        }
        boolean inside = source == this || SwingUtilities.isDescendingFrom(source, this);
        if (!inside) {
            return;
        }
        Point p = SwingUtilities.convertPoint(source, e.getPoint(), this);

        switch (e.getID()) {
            case MouseEvent.MOUSE_MOVED -> {
                if (!resizing) {
                    updateResizeCursor(p);
                }
            }
            case MouseEvent.MOUSE_PRESSED -> {
                int edges = edgesAt(p);
                if (edges != 0) {
                    resizing = true;
                    resizeEdges = edges;
                    resizeStartScreen = e.getLocationOnScreen();
                    resizeStartBounds = getBounds();
                }
            }
            case MouseEvent.MOUSE_DRAGGED -> {
                if (resizing) {
                    performResize(e.getLocationOnScreen());
                }
            }
            case MouseEvent.MOUSE_RELEASED -> {
                if (resizing) {
                    resizing = false;
                    resizeEdges = 0;
                    resizeStartScreen = null;
                    resizeStartBounds = null;
                    updateResizeCursor(p);
                }
            }
            default -> {
                // 其它鼠标事件与缩放无关
            }
        }
    }

    private void performResize(Point screen) {
        if (resizeStartScreen == null || resizeStartBounds == null) {
            return;
        }
        int dx = screen.x - resizeStartScreen.x;
        int dy = screen.y - resizeStartScreen.y;
        int minWidth = Math.max(getMinimumSize().width, 320);
        int minHeight = Math.max(getMinimumSize().height, 240);

        Rectangle bounds = new Rectangle(resizeStartBounds);
        if ((resizeEdges & EDGE_EAST) != 0) {
            bounds.width = Math.max(minWidth, resizeStartBounds.width + dx);
        }
        if ((resizeEdges & EDGE_SOUTH) != 0) {
            bounds.height = Math.max(minHeight, resizeStartBounds.height + dy);
        }
        if ((resizeEdges & EDGE_WEST) != 0) {
            int width = Math.max(minWidth, resizeStartBounds.width - dx);
            bounds.x = resizeStartBounds.x + (resizeStartBounds.width - width);
            bounds.width = width;
        }
        if ((resizeEdges & EDGE_NORTH) != 0) {
            int height = Math.max(minHeight, resizeStartBounds.height - dy);
            bounds.y = resizeStartBounds.y + (resizeStartBounds.height - height);
            bounds.height = height;
        }
        setBounds(bounds);
    }

    // ==================================================================
    // 材质层
    // ==================================================================

    /**
     * 材质层：画在原生材质之上、所有内容之下。
     *
     * <p>它是 Fluent 层次感的关键——原生材质往往是「太透 / 太花」的，
     * 真正让文字可读的是这一层半透明表面。WinUI 里对应 {@code LayerFillColorDefaultBrush}，
     * 深色主题下是 80% 的黑、浅色主题下是 80% 的白加一点提亮。</p>
     */
    private class LayerPane extends JPanel {

        LayerPane() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                FluentPainting.fill(g2, new Rectangle(0, 0, getWidth(), getHeight()),
                        FluentColors.layer());
                // 一圈极淡的描边，让窗口在浅色桌面上也有边界
                Color stroke = FluentColors.cardStroke();
                if (stroke.getAlpha() > 0) {
                    g2.setColor(stroke);
                    g2.setStroke(new java.awt.BasicStroke(FluentMetrics.BORDER));
                    g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
