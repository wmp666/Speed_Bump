package com.wmp.downloader.tools.ui;

import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 自绘的浮出菜单窗口（Flyout Menu）。
 *
 * <p>相比 {@link JPopupMenu}，它把窗口和绘制都握在自己手里，因此能做到两件事：</p>
 * <ul>
 *   <li><b>菜单项的划入/划出动画</b>：鼠标进入、离开时高亮色按帧过渡
 *       （{@link Item} 内部的 {@code hover} 值从 0 过渡到 1），而不是瞬间跳变；</li>
 *   <li><b>窗口背景材质</b>：窗口是 per-pixel 透明的 {@link JWindow}，可以交给
 *       {@link DialogBackdrop} / {@link WindowBackdrop} 应用 Mica（Win10 上自动回退为模糊）。</li>
 * </ul>
 *
 * <p><b>关于材质：</b>相关调用被包裹在
 * {@code [BACKDROP-START] ... [BACKDROP-END]} 标记块中，整块删除即可让菜单回到
 * 普通的不透明外观；材质由 {@code TestFunctionControl} 的测试项 1004 控制，
 * 未启用时 {@link DialogBackdrop#isEnabled()} 为 {@code false}，这些调用会直接返回，
 * 菜单表现为一个普通的圆角不透明菜单。</p>
 *
 * <p>窗口不抢占焦点（{@link #setFocusableWindowState} 为 {@code false}），
 * 关闭依靠全局鼠标监听：点击菜单以外的任意位置即收起。</p>
 */
public class FlyoutMenu extends JWindow {

    private static final Logger logger = Logger.getLogger(FlyoutMenu.class);

    // ---- 尺寸与动画参数 ----
    /** 菜单项图标尺寸 */
    private static final int ICON_SIZE = 16;
    /** 头部图标尺寸 */
    private static final int HEADER_ICON_SIZE = 28;
    /** 菜单项高度 */
    private static final int ROW_HEIGHT = 32;
    /** 菜单左右内边距 */
    private static final int H_PADDING = 12;
    /** 图标与文字的间距 */
    private static final int ICON_TEXT_GAP = 10;
    /** 菜单最小宽度 */
    private static final int MIN_WIDTH = 200;
    /** 菜单项高亮的圆角 */
    private static final int ROW_ARC = 8;
    /** 菜单整体的圆角 */
    private static final int MENU_ARC = 10;
    /** 勾选标记的尺寸 */
    private static final int CHECK_SIZE = 12;
    /** 动画帧间隔与每帧步长：约 80ms 完成一次过渡 */
    private static final int HOVER_FRAME_MS = 15;
    private static final float HOVER_STEP = 0.19f;
    /** 过渡完成后仍保留的余量，避免停在 0.99 之类的值上 */
    private static final float HOVER_EPSILON = 0.02f;

    private final Surface surface = new Surface();
    private final Header header = new Header();
    private final List<Item> items = new ArrayList<>();

    /** 窗口是否已被改造成透明窗口（材质可用的前提） */
    private boolean backdropReady = false;

    /**
     * 材质是否确实应用成功。
     *
     * <p>失败的场景是真实存在的：窗口刚显示时 HWND 可能还没被 {@code EnumWindows} 枚举到。
     * 这时窗口虽然是透明的，背后却没有模糊，半透明的菜单底板会让文字直接压在桌面内容上、
     * 难以阅读。所以底板的不透明度按<b>实际结果</b>决定：应用成功才半透明，
     * 否则退回不透明（下一次弹出时 HWND 已被缓存，通常就能成功）。</p>
     */
    private boolean backdropApplied = false;

    /** 点击菜单之外时收起菜单 */
    private final AWTEventListener outsideClickListener = event -> {
        if (!isVisible() || !(event instanceof MouseEvent mouseEvent)) {
            return;
        }
        if (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED) {
            return;
        }
        if (windowOf(mouseEvent.getComponent()) == this) {
            return;
        }
        hideMenu();
    };

    public FlyoutMenu() {
        setAlwaysOnTop(true);
        // 不抢焦点：避免主窗口失活闪动，关闭改由全局鼠标监听负责
        setFocusableWindowState(false);

        // ===== [BACKDROP-START] =====
        // 让窗口成为 per-pixel 透明窗口——这是看到原生材质的必要条件。
        // 测试项未启用时 prepareBackdropWindow 直接返回 false，窗口保持不透明。
        backdropReady = DialogBackdrop.prepareBackdropWindow(this);
        // ===== [BACKDROP-END] =====

        surface.setLayout(new BoxLayout(surface, BoxLayout.Y_AXIS));
        surface.add(header);
        setContentPane(surface);
    }

    // ==================================================================
    // 内容构建
    // ==================================================================

    /** 添加一个普通菜单项 */
    public Item addItem(String iconKey, String text, Runnable action) {
        Item item = new Item(action, false);
        item.setIconKey(iconKey);
        item.setText(text);
        addToSurface(item);
        return item;
    }

    /**
     * 添加一个可勾选的菜单项。
     *
     * @param onToggle 点击后的回调，参数为切换后的勾选状态
     */
    public Item addCheckItem(String text, Consumer<Boolean> onToggle) {
        Item item = new Item(null, true);
        item.setText(text);
        item.onToggle = onToggle;
        addToSurface(item);
        return item;
    }

    /** 添加一条分隔线 */
    public void addSeparator() {
        Separator separator = new Separator();
        surface.add(separator);
        items.add(null); // 占位，保证 items 与 surface 顺序一致
    }

    private void addToSurface(Item item) {
        surface.add(item);
        items.add(item);
    }

    /** 设置头部标题（默认取应用名） */
    public void setHeaderTitle(String title) {
        header.title.setText(title == null ? "" : title);
    }

    /** 设置头部摘要文字（如「正在运行 2 个任务 · 共 5 个任务」） */
    public void setHeaderSubtitle(String subtitle) {
        header.subtitle.setText(subtitle == null ? "" : subtitle);
        header.subtitle.setVisible(subtitle != null && !subtitle.isEmpty());
    }

    /** 设置头部图标 */
    public void setHeaderIcon(Icon icon) {
        header.icon.setIcon(icon);
    }

    /** 按图标键设置头部图标（便于注册到 {@link IconControl} 的主题刷新） */
    public void setHeaderIconKey(String iconKey) {
        setHeaderIcon(iconKey == null ? null : IconControl.getIcon(iconKey, HEADER_ICON_SIZE));
    }

    /** 当前菜单项列表（分隔线以 {@code null} 占位，与添加顺序一致），供调试/验证使用 */
    public List<Item> items() {
        return items;
    }

    // ==================================================================
    // 显示与隐藏
    // ==================================================================

    /**
     * 在指定的屏幕坐标弹出菜单。
     *
     * @param screenLocation 触发点（通常是鼠标的屏幕坐标）
     */
    public void showAt(Point screenLocation) {
        if (screenLocation == null) {
            return;
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showAt(screenLocation));
            return;
        }

        layoutMenu();
        applyTheme();
        placeWindow(screenLocation);
        setVisible(true);
    }

    /** 收起菜单（窗口保留以便复用） */
    public void hideMenu() {
        if (isVisible()) {
            setVisible(false);
        } else {
            resetItems();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        boolean changed = visible != isVisible();
        super.setVisible(visible);
        if (!changed) {
            return;
        }
        if (visible) {
            Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
            // ===== [BACKDROP-START] =====
            // 窗口显示后才有 HWND，此时应用材质；返回的是首次尝试的结果，
            // 失败会自动重试（重试成功的情况由下一次弹出时命中的 HWND 缓存兜住）
            setBackdropApplied(DialogBackdrop.activate(this));
            // ===== [BACKDROP-END] =====
        } else {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            resetItems();
        }
    }

    /** 材质结果变化时切换底板的不透明度并重绘 */
    private void setBackdropApplied(boolean applied) {
        if (backdropApplied == applied) {
            return;
        }
        backdropApplied = applied;
        surface.repaint();
    }

    /** 供调试/验证：材质当前是否已应用 */
    public boolean isBackdropApplied() {
        return backdropApplied;
    }

    @Override
    public void dispose() {
        resetItems();
        Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
        super.dispose();
    }

    /**
     * 按内容计算窗口尺寸。
     *
     * <p>宽度取「最长的一项」与最小宽度中的较大者，高度是各部分的自然高度之和——
     * 这些由 {@link BoxLayout} 的 {@code preferredLayoutSize} 给出，
     * 各项在窗口内会被自动拉伸到统一宽度。</p>
     *
     * <p>这里刻意<b>只读取</b> {@code getPreferredSize()} 而不写回：
     * 一旦把 {@code preferredSize} 固定成快照，后续内容变化就再也不会反映到窗口尺寸上
     * （这正是「大小有时候不刷新」的成因）。</p>
     */
    private void layoutMenu() {
        setSize(surface.getPreferredSize());
    }

    /** 从当前主题刷新字体与前景色（不放在 paint 里，避免 setFont 触发重绘循环） */
    private void applyTheme() {
        Font font = uiFont();
        for (Item item : items) {
            if (item != null) {
                item.setFont(font);
            }
        }
        header.applyTheme(font);
    }

    private void placeWindow(Point location) {
        Dimension size = getSize();
        Rectangle screen = screenBoundsAt(location.x, location.y);

        // 托盘一般贴着屏幕边缘：越界时向内收；底部托盘上的菜单会自动向上展开
        int x = Math.min(location.x, screen.x + screen.width - size.width - 2);
        int y = Math.min(location.y, screen.y + screen.height - size.height - 2);
        x = Math.max(x, screen.x + 2);
        y = Math.max(y, screen.y + 2);
        setLocation(x, y);
    }

    private void resetItems() {
        for (Item item : items) {
            if (item != null) {
                item.resetInteraction();
            }
        }
    }

    private static Window windowOf(Component component) {
        if (component instanceof Window window) {
            return window;
        }
        return component == null ? null : SwingUtilities.getWindowAncestor(component);
    }

    private static Rectangle screenBoundsAt(int x, int y) {
        try {
            GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice device : environment.getScreenDevices()) {
                Rectangle bounds = device.getDefaultConfiguration().getBounds();
                if (bounds.contains(x, y)) {
                    return bounds;
                }
            }
            return environment.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        } catch (Exception e) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, screenSize.width, screenSize.height);
        }
    }

    // ==================================================================
    // 外观
    // ==================================================================

    /** 菜单底板：圆角矩形，材质可用时半透明，否则用主题底色 */
    private final class Surface extends JPanel {

        @Override
        public boolean isOpaque() {
            // 恒定返回 false：既让材质透出，也避免主题刷新（updateComponentTreeUI）
            // 通过 installProperty(..., "opaque", TRUE) 把它改回不透明
            return false;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(size.width, MIN_WIDTH), size.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color base = themeColor("Panel.background", new Color(43, 43, 43));
            // 只有「窗口已透明」且「材质确实应用成功」时才半透明，
            // 否则退回不透明底板，避免文字压在桌面内容上
            boolean translucent = backdropReady && backdropApplied;
            int alpha = translucent ? DialogBackdrop.CONTENT_ALPHA : 255;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, MENU_ARC, MENU_ARC);

            if (!translucent) {
                // 没有材质时需要一圈描边，否则圆角边界与桌面混在一起
                g2.setColor(themeColor("Component.borderColor", new Color(90, 90, 90)));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, MENU_ARC, MENU_ARC);
            }
            g2.dispose();
        }
    }

    /** 头部：图标 + 应用名 + 状态摘要 */
    private final class Header extends JPanel {

        private final JLabel icon = new JLabel();
        private final JLabel title = new JLabel();
        private final JLabel subtitle = new JLabel();

        Header() {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 12, 9, 16));

            title.putClientProperty("FlatLaf.style", "font: $h4.font");
            subtitle.setVisible(false);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(title);
            text.add(Box.createVerticalStrut(2));
            text.add(subtitle);

            icon.setPreferredSize(new Dimension(HEADER_ICON_SIZE, HEADER_ICON_SIZE));
            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        /** 主题切换后刷新字体与前景色 */
        void applyTheme(Font base) {
            title.setForeground(themeColor("Label.foreground", Color.WHITE));
            subtitle.setForeground(themeColor("Label.disabledForeground", Color.GRAY));
            subtitle.setFont(smallFont(base));
            revalidate();
            repaint();
        }
    }

    /** 分隔线 */
    private static final class Separator extends JComponent {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, 9);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 9);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Color color = themeColor("Menu.separatorColor", null);
            if (color == null) {
                Color fg = themeColor("Label.foreground", Color.GRAY);
                color = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 60);
            }
            int y = getHeight() / 2;
            g.setColor(color);
            g.drawLine(H_PADDING, y, getWidth() - H_PADDING, y);
        }
    }

    // ==================================================================
    // 菜单项
    // ==================================================================

    /**
     * 自绘菜单项。
     *
     * <p>划入/划出的效果就是高亮色的透明度过渡：鼠标进入时 {@code hover} 由 0 递增到 1，
     * 离开时递减回 0，过渡期间逐帧重绘，因此看起来是「颜色渐变」而不是瞬间点亮。
     * 文字与图标的前景色也按同一比例在「普通色」和「选中色」之间插值。</p>
     */
    public final class Item extends JComponent {

        private String iconKey;
        private Icon icon;
        private String text = "";
        private boolean checkable;
        private boolean checked;
        private Runnable action;
        private Consumer<Boolean> onToggle;

        /** 划入/划出动画的当前值与目标值（0~1） */
        private float hover;
        private float hoverTarget;
        private final Timer animator;

        private boolean pressed;

        Item(Runnable action, boolean checkable) {
            this.action = action;
            this.checkable = checkable;
            setOpaque(false);
            setFont(uiFont());

            this.animator = new Timer(HOVER_FRAME_MS, e -> tickHover());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setHoverTarget(isEnabled() ? 1f : 0f);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setHoverTarget(0f);
                    pressed = false;
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                        pressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    boolean fire = pressed && isEnabled() && contains(e.getPoint());
                    pressed = false;
                    setHoverTarget(contains(e.getPoint()) && isEnabled() ? 1f : 0f);
                    if (fire) {
                        trigger();
                    }
                }
            });
        }

        // ---- 属性 ----

        public void setIconKey(String iconKey) {
            this.iconKey = iconKey;
            this.icon = iconKey == null ? null : IconControl.getIcon(iconKey, ICON_SIZE);
            repaint();
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
            repaint();
        }

        public void setText(String text) {
            this.text = text == null ? "" : text;
            revalidate();
            repaint();
        }

        public void setChecked(boolean checked) {
            if (this.checked != checked) {
                this.checked = checked;
                repaint();
            }
        }

        public boolean isChecked() {
            return checked;
        }

        public void setAction(Runnable action) {
            this.action = action;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            if (!enabled) {
                setHoverTarget(0f);
            }
            repaint();
        }

        /** 供调试/验证：当前划入程度（0 = 未划入，1 = 完全划入） */
        public float hoverLevel() {
            return hover;
        }

        private void trigger() {
            if (checkable) {
                checked = !checked;
                repaint();
                hideMenu();
                if (onToggle != null) {
                    Boolean value = checked;
                    SwingUtilities.invokeLater(() -> onToggle.accept(value));
                }
                return;
            }
            hideMenu();
            if (action != null) {
                // 先让菜单收起，再执行动作（动作可能弹出对话框）
                SwingUtilities.invokeLater(action);
            }
        }

        // ---- 动画 ----

        private void setHoverTarget(float target) {
            if (Math.abs(hoverTarget - target) < HOVER_EPSILON && Math.abs(hover - target) < HOVER_EPSILON) {
                return;
            }
            hoverTarget = target;
            if (!animator.isRunning()) {
                animator.start();
            }
        }

        private void tickHover() {
            float delta = hoverTarget - hover;
            if (Math.abs(delta) <= HOVER_STEP + HOVER_EPSILON) {
                hover = hoverTarget;
                animator.stop();
            } else {
                hover += Math.signum(delta) * HOVER_STEP;
            }
            repaint();
        }

        void resetInteraction() {
            animator.stop();
            hover = 0f;
            hoverTarget = 0f;
            pressed = false;
            repaint();
        }

        // ---- 布局与绘制 ----

        @Override
        public Dimension getPreferredSize() {
            if (text.isEmpty() && icon == null) {
                return new Dimension(0, ROW_HEIGHT);
            }
            int width = H_PADDING + (icon != null ? ICON_SIZE + ICON_TEXT_GAP : 0)
                    + textWidth() + H_PADDING;
            if (checkable) {
                width += CHECK_SIZE + 8;
            }
            return new Dimension(width, ROW_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, ROW_HEIGHT);
        }

        private int textWidth() {
            FontMetrics metrics = getFontMetrics(getFont());
            return metrics.stringWidth(text);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean on = isEnabled();
            float level = on ? hover : 0f;
            if (on && pressed) {
                level = Math.min(1f, level + 0.25f);
            }

            // 划入高亮：透明度按 level 过渡
            if (level > HOVER_EPSILON) {
                Color selection = themeColor("MenuItem.selectionBackground", new Color(70, 100, 160));
                g2.setColor(withAlpha(selection, level));
                g2.fillRoundRect(4, 2, getWidth() - 8, getHeight() - 4, ROW_ARC, ROW_ARC);
            }

            int x = H_PADDING;
            if (icon != null) {
                if (!on) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
                }
                icon.paintIcon(this, g2, x, (getHeight() - ICON_SIZE) / 2);
                g2.setComposite(AlphaComposite.SrcOver);
                x += ICON_SIZE + ICON_TEXT_GAP;
            }

            Color foreground = on
                    ? blend(themeColor("MenuItem.foreground", Color.WHITE),
                    themeColor("MenuItem.selectionForeground", themeColor("MenuItem.foreground", Color.WHITE)),
                    level)
                    : themeColor("MenuItem.disabledForeground", Color.GRAY);

            Font font = getFont();
            g2.setFont(font);
            g2.setColor(foreground);
            FontMetrics metrics = g2.getFontMetrics(font);
            int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(text, x, baseline);

            if (checkable && checked) {
                drawCheck(g2, foreground);
            }
            g2.dispose();
        }

        /** 自绘勾选标记：不依赖字体的字形覆盖（中文字体常缺 ✓ 之类的符号） */
        private void drawCheck(Graphics2D g2, Color color) {
            int size = CHECK_SIZE;
            int left = getWidth() - H_PADDING - size;
            int top = (getHeight() - size) / 2;
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(color);
            g2.drawLine(left, top + size * 5 / 12, left + size * 4 / 12, top + size * 9 / 12);
            g2.drawLine(left + size * 4 / 12, top + size * 9 / 12, left + size, top + size * 2 / 12);
            g2.setStroke(old);
        }
    }

    // ==================================================================
    // 主题工具
    // ==================================================================

    private static Font uiFont() {
        Font font = UIManager.getFont("MenuItem.font");
        return font != null ? font : new JMenuItem().getFont();
    }

    private static Font smallFont(Font base) {
        if (base == null) {
            return null;
        }
        return base.deriveFont(Math.max(11f, base.getSize2D() - 1f));
    }

    private static Color themeColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    /** 按比例把 alpha 乘上 given 程度，用于「划入」高亮的淡入淡出 */
    private static Color withAlpha(Color color, float level) {
        int alpha = Math.max(0, Math.min(255, Math.round(255 * level)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** 在两个颜色之间线性插值 */
    private static Color blend(Color from, Color to, float level) {
        if (level <= 0f) {
            return from;
        }
        if (level >= 1f) {
            return to;
        }
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * level);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * level);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * level);
        return new Color(r, g, b);
    }

    /** 供调试：打印材质可用性 */
    public void printBackdropInfo() {
        logger.info("FlyoutMenu backdropReady=" + backdropReady
                + ", enabled=" + DialogBackdrop.isEnabled()
                + ", native=" + WindowBackdrop.isSupported()
                + ", build=" + WindowBackdrop.windowsBuild());
    }
}
