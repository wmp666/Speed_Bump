package com.wmp.downloader.tools.ui;

import com.wmp.downloader.tools.TestFunctionControl;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
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
 *   <li>把使用方（当前是 {@code PreloadDialog}）中标注为
 *       {@code [BACKDROP-START] ... [BACKDROP-END]} 的代码块整段删除；</li>
 *   <li>删掉 {@code TestFunctionControl} 静态块中 {@code 1004} 的
 *       {@code applies(...)} 参数与 {@code register(1004, ...)} 一行；</li>
 *   <li>删掉本文件（{@code DialogBackdrop.java}）以及
 *       {@code com.wmp.downloader.tools.ui.WindowBackdrop}（若没有别处使用）。</li>
 * </ol>
 *
 * <h3>启用方式</h3>
 * <p>默认关闭。在「测试功能」对话框里勾选 mainId {@value #TEST_FUNCTION_MAIN_ID} 后
 * <b>重启应用</b>生效（加载窗只在启动时创建一次）。</p>
 * <p>代码里也可以用 {@link #setEnabled(Boolean)} 强制开关。</p>
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

    /** 标题栏高度 */
    private static final int TITLE_BAR_HEIGHT = 34;

    /** {@link #installBorderless} 生成的半透明层圆角半径；0 表示直角 */
    public static int CORNER_ARC = 12;

    /** 激活失败时的重试次数与间隔 */
    private static final int ACTIVATE_MAX_ATTEMPTS = 6;
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
        try {
            TestFunctionControl.load();
            setEnabled(TestFunctionControl.enableIDList().contains(TEST_FUNCTION_MAIN_ID));
        } catch (Throwable t) {
            // 测试项列表不可用时保持关闭，绝不影响启动流程
            setEnabled(Boolean.FALSE);
        }
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
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(createTitleBar(dialog, title, autoCenterTimer), BorderLayout.NORTH);
        if (oldContent != null) {
            wrapper.add(oldContent, BorderLayout.CENTER);
        }
        dialog.setContentPane(wrapper);
        return true;
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
    // 显示后：应用原生材质
    // ==================================================================

    /**
     * 应用原生背景材质。窗口显示之后调用（需要真实的 HWND）。
     *
     * @return 是否有原生调用成功；平台不支持时返回 false，不影响窗口正常显示
     */
    public static boolean activate(JDialog dialog) {
        if (dialog == null || !isEnabled()) {
            return false;
        }
        return activateWithRetry(dialog, 0);
    }

    /**
     * 应用材质并重试：刚显示时窗口可能还没被 {@code EnumWindows} 枚举到
     * （只有可见窗口才会被枚举），因此失败后延迟重试若干次。
     */
    private static boolean activateWithRetry(JDialog dialog, int attempt) {
        boolean ok = false;
        try {
            ok = WindowBackdrop.apply(dialog, WindowBackdrop.Material.MICA, TINT_ABGR);
        } catch (Throwable t) {
            ok = false;
        }
        if (!ok && attempt < ACTIVATE_MAX_ATTEMPTS) {
            Timer retry = new Timer(ACTIVATE_RETRY_DELAY_MS, e -> activateWithRetry(dialog, attempt + 1));
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

        installDragSupport(bar, dialog, autoCenterTimer);
        return bar;
    }

    private static JButton createCloseButton(JDialog dialog) {
        JButton close = new JButton("✕");
        close.setFocusable(false);
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setOpaque(false);
        close.setForeground(themeForeground());
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setToolTipText("关闭");
        close.setMargin(new java.awt.Insets(0, 0, 0, 0));
        close.setPreferredSize(new Dimension(30, TITLE_BAR_HEIGHT - 12));
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
