package com.wmp.downloader.tools.ui;

import com.wmp.downloader.tools.TestFunctionControl;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

/**
 * 对话框的「原生背景材质（Mica / 模糊）」支持层。
 *
 * <p>本类是作为 {@code TestFunctionControl} 的一个测试项（mainId
 * {@value #TEST_FUNCTION_MAIN_ID}）引入的实验性功能，
 * <b>整个功能可以零残留地移除</b>：</p>
 *
 * <h3>如何移除该功能</h3>
 * <ol>
 *   <li>把使用方（{@code PreloadDialog}、{@code FunctionDialog}、{@code FlyoutMenu}）中标注为
 *       {@code [BACKDROP-START] ... [BACKDROP-END]} 的代码块整段删除；</li>
 *   <li>删掉 {@code TestFunctionControl} 静态块中 {@code 1004} 的
 *       {@code applies(...)} 参数与 {@code register(1004, ...)} 一行；</li>
 *   <li>删掉本文件（{@code DialogBackdrop.java}）以及
 *       {@code com.wmp.downloader.tools.ui.WindowBackdrop}（若没有别处使用）。</li>
 * </ol>
 *
 * <h3>启用方式</h3>
 * <p>默认关闭。在「测试功能」对话框里勾选 mainId {@value #TEST_FUNCTION_MAIN_ID} 后生效；
 * 对话框类窗口（加载窗、功能弹窗）在创建时读取该开关，因此需要重启应用，
 * 而自绘弹出菜单每次弹出都会重新读取。</p>
 * <p>代码里也可以用 {@link #setEnabled(Boolean)} 强制开关。</p>
 *
 * <h3>支持哪些窗口</h3>
 * <p>主体 API 面向 {@link JDialog}（{@link #install} / {@link #installBorderless}），
 * 另外提供两个通用入口给任意 {@link Window}：
 * {@link #prepareBackdropWindow(Window)}（显示前透明化）与 {@link #activate(Window)}（显示后应用材质），
 * 自绘弹出菜单 {@code FlyoutMenu} 就是通过它们接入的。</p>
 *
 * <h3>为什么必须去掉原生标题栏</h3>
 * <p>实测（JDK 25 / Windows 10）：带装饰的窗口设置透明背景会抛
 * {@code IllegalComponentStateException: The dialog is decorated}。
 * 而 per-pixel 透明是看到原生材质的必要条件（DWM 把材质绘制在窗口内容之下，
 * 只要客户区是不透明像素就会被完全遮住）。因此 {@link #install} 会去掉原生装饰并自绘标题栏，
 * 而 {@link #installBorderless} 面向本来就无边框的窗口，不改动装饰状态。</p>
 */
public final class DialogBackdrop {

    private DialogBackdrop() {
    }

    // ==================================================================
    // 可调参数
    // ==================================================================

    /** 内容区不透明度的默认值（0 全透明 ~ 255 全不透明）。数值越小材质越明显、文字越难读 */
    public static final int CONTENT_ALPHA = 155;

    /** 按钮栏的不透明度，稍高一点以保留视觉分区 */
    public static final int BUTTONS_ALPHA = 190;

    /** 传给 Win10 blur/acrylic 的着色（ABGR），仅部分模式使用 */
    public static final int TINT_ABGR = 0x66202020;

    /**
     * 实际使用的材质。<b>{@code null}（默认）表示按系统版本自动选择</b>：
     * <ul>
     *   <li>Windows 7 及更早 → {@code AERO}：{@code DwmEnableBlurBehindWindow}，
     *       Aero Glass 在这些系统上是原生能力</li>
     *   <li>Windows 8 及以后 → {@code BLUR}：{@code SetWindowCompositionAttribute}，
     *       因为微软在 Win8 移除了 Aero Glass，该 API 之后返回成功但无效果</li>
     * </ul>
     *
     * <p>需要手工指定时改这一个字段即可，可选值：{@code BLUR} / {@code AERO} /
     * {@code MICA} / {@code ACRYLIC} / {@code TABBED} / {@code NONE}。</p>
     */
    public static WindowBackdrop.Material MATERIAL = null;

    /**
     * 是否允许通过自绘标题栏拖动窗口。
     *
     * <p><b>默认 {@code false}：应用背景材质后窗口不可拖动。</b></p>
     *
     * <p>无边框窗口只有标题栏能发起拖动，所以关闭后对话框会固定在使用方给定的位置
     * （{@code FunctionDialog} 由 {@code packTimer} 持续居中）。</p>
     *
     * <p>另需注意：拖动支持原本还承担「用户拖动后停掉调用方的自动居中定时器」这一职责，
     * 关闭拖动后该副作用一并消失，{@code install(...)} 的 {@code autoCenterTimer} 参数不再起作用。</p>
     */
    public static boolean enableDragToMove = false;

    /** 标题栏高度 */
    private static final int TITLE_BAR_HEIGHT = 34;

    /** {@link #installBorderless} 生成的半透明层圆角半径；0 表示直角 */
    public static int CORNER_ARC = 12;

    /** 激活失败时的重试次数与间隔。窗口刚显示时 HWND 可能还没被枚举到，需要多试几次 */
    private static final int ACTIVATE_MAX_ATTEMPTS = 10;
    private static final int ACTIVATE_RETRY_DELAY_MS = 100;

    // ==================================================================
    // 开关
    // ==================================================================

    /**
     * 本功能在 {@code TestFunctionControl} 中登记的 mainId。
     * 描述见 {@code TestFunctionControl} 的静态块。
     */
    public static final int TEST_FUNCTION_MAIN_ID = 1004;

    /** 手动覆盖开关：null 表示未启用（默认）。测试项会在窗口创建前调用 {@link #applyTestFunctionSwitch()} 设置它 */
    private static Boolean enabledOverride = null;

    /** 测试项开关是否已读取过：进程内只读一次，避免每次弹窗都去读文件 */
    private static boolean switchLoaded = false;

    /** 手动覆盖开关；传 null 恢复默认（关闭） */
    public static void setEnabled(Boolean enabled) {
        enabledOverride = enabled;
    }

    /**
     * 是否启用。
     *
     * <p><b>默认关闭</b>——本功能登记为 {@code TestFunctionControl} 的测试项，
     * 只有测试功能开关启用后才生效。这样即使有人误删了开关逻辑，
     * 功能也不会在正式流程里悄悄打开。</p>
     */
    public static boolean isEnabled() {
        return enabledOverride != null && enabledOverride;
    }

    /**
     * 读取测试项开关并据此设置启用状态。在使用本功能的窗口构造之前调用一次即可。
     *
     * <p><b>这里刻意不走 {@code TestFunctionControl.run(...)}</b>：run 在功能被启用时会调用
     * {@code ToastMessage.show(...)}，而它的实现里会访问 {@code Downloader.mainFrame.isActive()}，
     * 启动早期该字段仍为 null，会直接抛 NPE。因此这里改为直接查询启用列表。</p>
     *
     * <p>{@code TestFunctionControl.load()} 是幂等的（清空后重新读取），
     * 之后 {@code DataControl.load()} 再次调用它也不会产生副作用。</p>
     */
    public static void applyTestFunctionSwitch() {
        if (switchLoaded) {
            // 进程内只读一次：测试项的改动按设计需要重启才生效
            return;
        }
        try {
            TestFunctionControl.load();
            setEnabled(TestFunctionControl.enableIDList().contains(TEST_FUNCTION_MAIN_ID));
        } catch (Throwable t) {
            // 测试项列表不可用时保持关闭，绝不影响启动流程
            setEnabled(Boolean.FALSE);
        }
        switchLoaded = true;
    }

    // ==================================================================
    // 显示前：无边框 + 透明 + 自绘标题栏
    // ==================================================================

    /**
     * 把对话框改造成可承载原生背景材质的透明窗口。
     *
     * <p><b>必须在 {@code setVisible(true)} 之前调用</b>（更准确地说，在窗口变为
     * displayable 之前，即 {@code pack()} 之前）。若窗口已经显示，本方法直接返回
     * {@code false} 且不做任何改动。</p>
     *
     * @param dialog 目标对话框
     * @param title  自绘标题栏上的标题文本
     * @return 是否改造成功
     */
    public static boolean install(JDialog dialog, String title) {
        return install(dialog, title, null);
    }

    /**
     * @param autoCenterTimer 调用方用于周期性重新居中的定时器（如 FunctionDialog 的 packTimer）。
     *                        用户拖动窗口后会停止它，否则窗口会被不断拉回原位。
     *                        传 {@code null} 表示不需要处理。
     */
    public static boolean install(JDialog dialog, String title, Timer autoCenterTimer) {
        if (dialog == null || !isEnabled() || dialog.isUndecorated()) {
            return false;
        }
        try {
            dialog.setUndecorated(true);
        } catch (Throwable t) {
            // 窗口已经 displayable，无法再改变装饰状态
            return false;
        }
        try {
            dialog.setBackground(new Color(0, 0, 0, 0));
        } catch (Throwable t) {
            // 透明设置失败则回滚，保持原有外观
            try {
                dialog.setUndecorated(false);
            } catch (Throwable ignored) {
                // 回滚失败也不应影响主流程
            }
            return false;
        }

        // 根面板与默认 contentPane 都不能是不透明背景，否则会盖住材质
        JRootPane rootPane = dialog.getRootPane();
        if (rootPane != null) {
            rootPane.setOpaque(false);
            rootPane.setBackground(new Color(0, 0, 0, 0));
        }
        Container oldContent = dialog.getContentPane();
        if (oldContent instanceof JComponent oldComponent) {
            oldComponent.setOpaque(false);
        }

        // 把原有内容整体包进一层，上方放自绘标题栏
        JPanel wrapper = new BackdropLayer();
        wrapper.add(createTitleBar(dialog, title, autoCenterTimer), BorderLayout.NORTH);
        if (oldContent != null) {
            wrapper.add(oldContent, BorderLayout.CENTER);
        }
        dialog.setContentPane(wrapper);

        // 自绘标题栏会占掉窗口高度，补偿最小尺寸，保证内容区不小于调用方设定的值
        Dimension min = dialog.getMinimumSize();
        if (min != null && min.height > 0) {
            dialog.setMinimumSize(new Dimension(min.width, min.height + TITLE_BAR_HEIGHT));
        }

        // 缩放后强制整窗重绘：透明缓冲里可能仍留着旧尺寸的画面
        dialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                dialog.repaint();
            }
        });
        return true;
    }

    /**
     * 透明窗口的内容层：自身不绘制任何背景，让材质直接透出。
     *
     * <p>这里刻意<b>不做</b>手工清屏：JDK 的 {@code RepaintManager} 对 per-pixel 透明窗口
     * 本来就会用 {@code AlphaComposite} 清除脏区，手工再 Clear 一次属于重复操作，
     * 反而可能干扰 Swing 的双缓冲，出现内容缺失或闪烁。</p>
     */
    private static final class BackdropLayer extends JPanel {

        BackdropLayer() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    /**
     * 面向<b>本来就无边框</b>的窗口（如启动加载窗）的一行式入口：
     * 透明化窗口 + 给内容套一层自绘半透明底色（带圆角），并让内容面板保持透明。
     *
     * <p>不添加标题栏，也不修改窗口的装饰状态，因此不会改变这类窗口的既有外观结构。</p>
     *
     * <p>半透明层在每次重绘时实时读取主题色，并且强制 {@code isOpaque() == false}，
     * 因此主题切换（{@code updateComponentTreeUI}）不会把它重置回不透明，
     * 不需要注册 {@code ThemeChanger} 的动态转换任务。</p>
     *
     * <p><b>必须在 {@code pack()} / {@code setVisible(true)} 之前调用。</b></p>
     *
     * @param dialog       目标窗口，须已经 {@code setUndecorated(true)}
     * @param contentPanel 内容根面板，会被设为透明，其直接子组件也会被设为透明
     * @return 是否改造成功
     */
    public static boolean installBorderless(JDialog dialog, JComponent contentPanel) {
        if (dialog == null || contentPanel == null || !isEnabled()) {
            return false;
        }
        if (!dialog.isUndecorated()) {
            // 带装饰的窗口无法透明；这里也不应擅自去掉调用方的标题栏
            return false;
        }
        try {
            dialog.setBackground(new Color(0, 0, 0, 0));
        } catch (Throwable t) {
            return false;
        }

        JRootPane rootPane = dialog.getRootPane();
        if (rootPane != null) {
            rootPane.setOpaque(false);
            rootPane.setBackground(new Color(0, 0, 0, 0));
        }

        Container currentContent = dialog.getContentPane();
        if (currentContent instanceof JComponent current) {
            current.setOpaque(false);
        }
        contentPanel.setOpaque(false);
        makeChildrenTransparent(contentPanel);

        // 把现有内容整体套进自绘半透明层
        JPanel layer = new TranslucentLayer(CONTENT_ALPHA, CORNER_ARC);
        layer.setLayout(new BorderLayout());
        if (currentContent != null) {
            layer.add(currentContent, BorderLayout.CENTER);
        }
        dialog.setContentPane(layer);
        return true;
    }

    /**
     * 把容器的所有<b>直接</b>子组件设为透明。
     * 仅适合加载窗这类只含标签/指示器的简单窗口——对 JButton 等有实体外观的组件会去掉其背景。
     */
    public static void makeChildrenTransparent(Container container) {
        if (container == null) {
            return;
        }
        for (Component child : container.getComponents()) {
            if (child instanceof JComponent jc) {
                jc.setOpaque(false);
            }
        }
    }

    /**
     * 递归把容器内部的「纯容器」组件设为透明。
     *
     * <p>{@link #makeTransparent} 只处理传入的组件本身，而调用方塞进对话框的功能面板
     * （{@code FunctionDialog} 的 {@code functionPanel}）内部往往还有若干层容器——
     * 滚动面板、视口、嵌套面板、标签页面板——它们默认不透明，会一层层盖住背景材质。
     * 这个方法负责把整棵容器树里的这类组件都清掉，因此所有调用点都无需各自改动。</p>
     *
     * <p><b>只处理容器类组件</b>：按钮、输入框、复选框等有实体外观的组件会保留背景，
     * 避免被误伤成"看不见的控件"。</p>
     */
    public static void makeTreeTransparent(Container container) {
        if (!isEnabled() || container == null) {
            return;
        }
        for (Component child : container.getComponents()) {
            if (!(child instanceof JComponent jc) || !isPureContainer(jc)) {
                continue;
            }
            jc.setOpaque(false);
            if (child instanceof JScrollPane scrollPane) {
                // 视口是独立组件，不会被上面那句 setOpaque 覆盖
                scrollPane.getViewport().setOpaque(false);
            }
            if (child instanceof Container nested) {
                makeTreeTransparent(nested);
            }
        }
    }

    /** 是否属于「纯容器」——透明化它们不会破坏控件的实体外观 */
    private static boolean isPureContainer(JComponent component) {
        return component instanceof JPanel
                || component instanceof JScrollPane
                || component instanceof JViewport
                || component instanceof JSplitPane
                || component instanceof JTabbedPane;
    }

    /**
     * 自绘半透明（可选圆角）背景层。
     *
     * <p>颜色不在初始化时固定，而是每次重绘时从 {@code UIManager} 实时读取，
     * 因此主题切换后自动跟随；同时重写 {@link #isOpaque()} 恒定返回 {@code false}，
     * 避免 {@code LookAndFeel.installProperty(..., "opaque", TRUE)} 在主题刷新时把它改回不透明。</p>
     */
    private static final class TranslucentLayer extends JPanel {

        private final int alpha;
        private final int arc;

        TranslucentLayer(int alpha, int arc) {
            this.alpha = clampAlpha(alpha);
            this.arc = Math.max(0, arc);
            super.setOpaque(false);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color base = themePanelBackground();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
            int w = getWidth();
            int h = getHeight();
            if (arc > 0) {
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }

    /** 显示前调用：让承载材质的主面板呈现半透明底色（材质透出 + 文字可读） */
    public static void makeTranslucent(JComponent... panels) {
        makeTranslucent(CONTENT_ALPHA, panels);
    }

    /** 同上，自定义不透明度 */
    public static void makeTranslucent(int alpha, JComponent... panels) {
        if (!isEnabled() || panels == null) {
            return;
        }
        Color base = themePanelBackground();
        for (JComponent panel : panels) {
            if (panel == null) {
                continue;
            }
            panel.setOpaque(true);
            panel.setBackground(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                    clampAlpha(alpha)));
        }
    }

    /** 显示前调用：让面板完全透明，露出下方材质或父面板底色 */
    public static void makeTransparent(JComponent... panels) {
        if (!isEnabled() || panels == null) {
            return;
        }
        for (JComponent panel : panels) {
            if (panel != null) {
                panel.setOpaque(false);
            }
        }
    }

    // ==================================================================
    // 标签页尺寸跟随
    // ==================================================================

    /**
     * 让窗口尺寸跟随 {@link JTabbedPane} 的「当前选中页」，而不是所有页的最大值。
     *
     * <p><b>为什么需要它：</b>{@code JTabbedPane.getPreferredSize()} 由
     * {@code BasicTabbedPaneUI.calculateSize()} 计算，而它的逻辑是
     * <i>“Determine minimum size required to display largest child in each dimension”</i>
     * ——遍历<b>所有</b>标签页取最大尺寸。所以切换标签页时 {@code preferredSize} 恒定不变，
     * 依赖它的 {@code pack()} 自然也不会调整窗口大小。这是 JDK 的既定行为，与本功能无关。</p>
     *
     * <p>这里通过给 tabbedPane 显式设置 {@code preferredSize}（= 当前页尺寸 + 标签栏高度）
     * 来绕过该行为，使原本的 {@code pack()} 逻辑自动生效。</p>
     *
     * <p>必须在窗口已经显示、且 tabbedPane 完成过一次布局之后调用效果最好；
     * 若首次调用时标签栏高度尚未测出，会在后续布局中自动补上。</p>
     *
     * @param dialog 承载该 tabbedPane 的窗口
     * @param tabs   需要跟随的标签页面板
     */
    /** 窗口 -> 需要跟随尺寸的 tabbedPane 的绑定键 */
    private static final String TAB_BINDING_KEY = "DialogBackdrop.tabbedPane";
    /** tabbedPane -> 标签栏（tab 行 + insets）高度缓存键 */
    private static final String TAB_CHROME_KEY = "DialogBackdrop.tabChromeHeight";

    public static void bindTabbedPaneResize(JDialog dialog, JTabbedPane tabs) {
        if (dialog == null || tabs == null || !isEnabled()) {
            return;
        }
        // 记录绑定：refreshTabbedPaneSize() 需要通过窗口找回这个 tabbedPane
        // （JDialog 不是 JComponent，client property 挂在 rootPane 上）
        JRootPane rootPane = dialog.getRootPane();
        if (rootPane == null) {
            return;
        }
        rootPane.putClientProperty(TAB_BINDING_KEY, tabs);

        ComponentAdapter tracker = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                rememberChromeHeight(tabs);
                refreshTabbedPaneSize(dialog);
            }
        };
        tabs.addComponentListener(tracker);
        dialog.addComponentListener(tracker);

        tabs.addChangeListener(e -> SwingUtilities.invokeLater(() -> {
            rememberChromeHeight(tabs);
            refreshTabbedPaneSize(dialog);
        }));
        SwingUtilities.invokeLater(() -> {
            rememberChromeHeight(tabs);
            refreshTabbedPaneSize(dialog);
        });
    }

    /**
     * 让 tabbedPane 的 preferredSize 重新等于「当前选中页尺寸 + 标签栏高度」。
     *
     * <p><b>调用方应在每次 {@code pack()} 之前调用它。</b>只监听标签页切换是不够的——
     * 当前页自身的内容尺寸发生变化时（异步加载、展开折叠、列表增删等）同样需要刷新，
     * 否则 preferredSize 会停留在上一次的快照上，窗口大小就不再跟随。
     * 这正是「与标签页切换无关的大小失效」的成因。</p>
     */
    public static void refreshTabbedPaneSize(JDialog dialog) {
        if (dialog == null || !isEnabled()) {
            return;
        }
        JRootPane rootPane = dialog.getRootPane();
        Object binding = (rootPane != null) ? rootPane.getClientProperty(TAB_BINDING_KEY) : null;
        if (!(binding instanceof JTabbedPane tabs)) {
            return;
        }
        int chromeHeight = chromeHeightOf(tabs);
        if (chromeHeight <= 0) {
            return;
        }
        Component page = tabs.getSelectedComponent();
        if (page == null) {
            return;
        }
        Dimension pageSize = page.getPreferredSize();
        Dimension target = new Dimension(pageSize.width, pageSize.height + chromeHeight);
        if (!target.equals(tabs.getPreferredSize())) {
            tabs.setPreferredSize(target);
            tabs.revalidate();
        }
    }

    /**
     * 记录标签栏（tab 行 + insets）占用的高度：由「容器高度 − 当前页实际高度」反推。
     *
     * <p>只记录一次——之后窗口可能被最小/最大尺寸约束，那时反推出的值不再可靠。</p>
     */
    private static void rememberChromeHeight(JTabbedPane tabs) {
        if (chromeHeightOf(tabs) > 0) {
            return;
        }
        Component page = tabs.getSelectedComponent();
        if (page == null) {
            return;
        }
        int tabHeight = tabs.getHeight();
        int pageHeight = page.getHeight();
        if (tabHeight > 0 && pageHeight > 0 && tabHeight > pageHeight) {
            tabs.putClientProperty(TAB_CHROME_KEY, tabHeight - pageHeight);
        }
    }

    private static int chromeHeightOf(JTabbedPane tabs) {
        Object value = tabs.getClientProperty(TAB_CHROME_KEY);
        return (value instanceof Integer i) ? i : 0;
    }

    /**
     * 在容器树里查找第一个 {@link JTabbedPane}（含递归）。
     * 供调用方在只拿到外层面板引用时使用。
     */
    public static JTabbedPane findTabbedPane(Container container) {
        if (container == null) {
            return null;
        }
        for (Component child : container.getComponents()) {
            if (child instanceof JTabbedPane tabs) {
                return tabs;
            }
            if (child instanceof Container nested) {
                JTabbedPane found = findTabbedPane(nested);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ==================================================================
    // 显示后：应用原生材质
    // ==================================================================

    /**
     * 通用入口：把<b>任意窗口</b>（例如自绘弹出菜单使用的 {@link javax.swing.JWindow}）
     * 改造成可以承载原生材质的透明窗口。
     *
     * <p>必须在 {@code setVisible(true)} 之前调用。测试项未启用时直接返回 {@code false}，
     * 窗口保持原有外观。</p>
     *
     * @return 是否成功设置为 per-pixel 透明窗口
     */
    public static boolean prepareBackdropWindow(Window window) {
        if (window == null || !isEnabled()) {
            return false;
        }
        try {
            return WindowBackdrop.prepareTransparent(window);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 应用原生背景材质。窗口显示之后调用（需要真实的 HWND）。
     *
     * @return 是否有原生调用成功；平台不支持时返回 false，不影响窗口正常显示
     */
    public static boolean activate(JDialog dialog) {
        return activate((Window) dialog);
    }

    /**
     * 通用入口：给任意窗口应用原生背景材质（{@link #activate(JDialog)} 的通用版本）。
     *
     * @return 是否有原生调用成功；平台不支持时返回 false，不影响窗口正常显示
     */
    public static boolean activate(Window window) {
        if (window == null || !isEnabled() || !WindowBackdrop.isSupported()) {
            // 平台不支持时不做无谓的重试
            return false;
        }
        return activateWithRetry(window, 0);
    }

    /**
     * 解析实际要用的材质：{@link #MATERIAL} 有值时优先，否则按系统版本自动选择。
     *
     * <p>Windows 7 及更早用 Aero Glass；Windows 8 起该能力被移除，
     * 改用 {@code SetWindowCompositionAttribute} 模糊。</p>
     */
    private static WindowBackdrop.Material resolveMaterial() {
        if (MATERIAL != null) {
            return MATERIAL;
        }
        int build = WindowBackdrop.windowsBuild();
        // Windows 7 = 7601、Vista = 6002、XP = 2600；Windows 8 = 9200
        if (build > 0 && build < 8000) {
            return WindowBackdrop.Material.AERO;
        }
        return WindowBackdrop.Material.BLUR;
    }

    /**
     * 应用材质并重试：刚显示时窗口可能还没被 {@code EnumWindows} 枚举到
     * （只有可见窗口才会被枚举），因此失败后延迟重试若干次。
     */
    private static boolean activateWithRetry(Window window, int attempt) {
        boolean ok = false;
        try {
            ok = WindowBackdrop.apply(window, resolveMaterial(), TINT_ABGR);
        } catch (Throwable t) {
            ok = false;
        }
        if (!ok && attempt < ACTIVATE_MAX_ATTEMPTS) {
            Timer retry = new Timer(ACTIVATE_RETRY_DELAY_MS, e -> activateWithRetry(window, attempt + 1));
            retry.setRepeats(false);
            retry.start();
        }
        return ok;
    }

    /** 关闭材质（保留无边框外观） */
    public static boolean deactivate(JDialog dialog) {
        if (dialog == null) {
            return false;
        }
        try {
            return WindowBackdrop.apply(dialog, WindowBackdrop.Material.NONE);
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================================================================
    // 自绘标题栏
    // ==================================================================

    private static JComponent createTitleBar(JDialog dialog, String title, Timer autoCenterTimer) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(true);
        Color base = themePanelBackground();
        bar.setBackground(new Color(base.getRed(), base.getGreen(), base.getBlue(),
                clampAlpha(CONTENT_ALPHA)));
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 8));
        bar.setPreferredSize(new Dimension(0, TITLE_BAR_HEIGHT));

        JLabel label = new JLabel(title == null ? "" : title);
        label.setForeground(themeForeground());
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 1f));
        bar.add(label, BorderLayout.WEST);

        JButton close = createCloseButton(dialog);
        JPanel east = new JPanel(new BorderLayout());
        east.setOpaque(false);
        east.add(close, BorderLayout.EAST);
        bar.add(east, BorderLayout.EAST);

        if (enableDragToMove) {
            installDragSupport(bar, dialog, autoCenterTimer);
        }
        return bar;
    }

    private static JButton createCloseButton(JDialog dialog) {
        JButton close = new JButton();
        close.setFocusable(false);
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setOpaque(false);
        close.setForeground(themeForeground());
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("关闭");
        close.setMargin(new java.awt.Insets(0, 0, 0, 0));
        close.setPreferredSize(new Dimension(30, TITLE_BAR_HEIGHT - 12));

        // 优先用主题自带的窗口关闭图标；
        // 取不到时退回 "×"（U+00D7 乘号）——不要用 "✕"（U+2715），
        // 微软雅黑等中文字体里没有该字形，会渲染成空白，看起来就是「按钮没有文字」。
        Icon closeIcon = UIManager.getIcon("InternalFrame.closeIcon");
        if (closeIcon != null) {
            close.setIcon(closeIcon);
        } else {
            close.setText("\u00d7");
            close.setFont(close.getFont().deriveFont(Font.BOLD,
                    close.getFont().getSize2D() + 4f));
        }
        // 与原 DISPOSE_ON_CLOSE 行为一致：关闭后 result 保持 RESULT_EXIT
        close.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(close);
            if (w != null) {
                w.dispose();
            } else {
                dialog.dispose();
            }
        });
        return close;
    }

    /** 无边框窗口必须自己实现拖动 */
    private static void installDragSupport(JComponent handle, JDialog dialog, Timer autoCenterTimer) {
        Point[] origin = new Point[1];
        handle.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin[0] = e.getPoint();
                // 用户开始拖动后，停止调用方的周期性重新居中，否则窗口会被立刻拉回
                if (autoCenterTimer != null && autoCenterTimer.isRunning()) {
                    autoCenterTimer.stop();
                }
            }
        });
        handle.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) {
                    return;
                }
                Point location = dialog.getLocation();
                dialog.setLocation(location.x + e.getX() - origin[0].x,
                        location.y + e.getY() - origin[0].y);
            }
        });
    }

    // ==================================================================
    // 主题色
    // ==================================================================

    private static Color themePanelBackground() {
        Color c = UIManager.getColor("Panel.background");
        if (c == null) {
            c = UIManager.getColor("control");
        }
        return c != null ? c : new Color(43, 43, 43);
    }

    private static Color themeForeground() {
        Color c = UIManager.getColor("Label.foreground");
        if (c == null) {
            c = UIManager.getColor("Panel.foreground");
        }
        return c != null ? c : Color.WHITE;
    }

    private static int clampAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    /** 供调试：打印本功能在当前环境的可用性 */
    public static void printSupportInfo() {
        System.out.println("[DialogBackdrop] enabled=" + isEnabled()
                + ", native=" + WindowBackdrop.isSupported()
                + ", windowsBuild=" + WindowBackdrop.windowsBuild()
                + ", windowsMajor=" + WindowBackdrop.windowsMajorVersion());
    }

    /** 未使用的占位，保持与 AWT 容器的兼容性检查 */
    @SuppressWarnings("unused")
    private static boolean hasAncestor(Component c, Class<?> type) {
        return type.isInstance(c);
    }
}
