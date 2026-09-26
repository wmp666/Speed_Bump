package com.wmp.speedbump.fluent.ui;

import com.wmp.speedbump.fluent.component.FluentButton;
import com.wmp.speedbump.fluent.component.FluentScrollPane;
import com.wmp.speedbump.fluent.component.FluentTextBox;
import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Fluent 导航视图（NavigationView）。
 *
 * <p>左侧是导航窗格，右侧是页面宿主（{@link CardLayout} 切换）。导航项带
 * <b>WinUI 的选中指示条</b>：窗格最左侧一条 3×16 的主题色圆角竖条，
 * 加上项目本体的圆角底色。这个组合是 Fluent 导航最具辨识度的部分。</p>
 *
 * <p>窗格支持折叠：展开 280px（图标 + 文字 + 顶部搜索框），折叠 48px（只有图标）。
 * 折叠动画刻意不做——Swing 上做宽度动画会引发整棵组件树反复 revalidate，
 * 得不偿失，WinUI 本身在展开/折叠时也是瞬时的。</p>
 */
public class FluentNavigationView extends JPanel implements FluentTheme.ThemeAware {

    private final JPanel navPane = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                // 导航窗格比窗口底色略亮一档，形成「两个平面」的层次（WinUI 的
                // NavigationViewDefaultPaneBackground 语义）
                FluentPainting.fill(g2, new java.awt.Rectangle(0, 0, getWidth(), getHeight()),
                        FluentColors.card());
                g2.setColor(FluentColors.divider());
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
            } finally {
                g2.dispose();
            }
        }
    };
    private final JPanel header = new JPanel(new BorderLayout());
    private final JPanel itemContainer = new JPanel();
    private final JPanel footerContainer = new JPanel();
    private final JPanel contentHost = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    private final FluentButton hamburger;
    private final FluentTextBox searchBox;

    private final List<NavItem> items = new ArrayList<>();
    private final List<NavItem> footerItems = new ArrayList<>();
    private NavItem selected;
    private boolean compact;

    public FluentNavigationView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // ---- 导航窗格 ----
        navPane.setOpaque(false);
        navPane.setPreferredSize(new Dimension(FluentMetrics.NAV_WIDTH_EXPANDED, 0));

        hamburger = FluentButton.subtle(FluentIcons.GLOBAL_NAV);
        hamburger.setPreferredSize(new Dimension(32, 32));
        hamburger.tip("折叠 / 展开导航");
        hamburger.addActionListener(e -> setCompact(!compact));

        searchBox = new FluentTextBox("搜索", FluentIcons.SEARCH);

        header.setOpaque(false);
        header.setBorder(new javax.swing.border.EmptyBorder(
                FluentMetrics.SPACING_XS, FluentMetrics.SPACING_XS,
                FluentMetrics.SPACING_XS, FluentMetrics.SPACING_XS));
        JPanel headerLeft = new JPanel(new BorderLayout());
        headerLeft.setOpaque(false);
        headerLeft.add(hamburger, BorderLayout.WEST);
        headerLeft.add(searchBox, BorderLayout.CENTER);
        header.add(headerLeft, BorderLayout.CENTER);
        navPane.add(header, BorderLayout.NORTH);

        itemContainer.setOpaque(false);
        itemContainer.setLayout(new BoxLayout(itemContainer, BoxLayout.Y_AXIS));
        JPanel itemWrapper = new JPanel(new BorderLayout());
        itemWrapper.setOpaque(false);
        itemWrapper.add(itemContainer, BorderLayout.NORTH);

        footerContainer.setOpaque(false);
        footerContainer.setLayout(new BoxLayout(footerContainer, BoxLayout.Y_AXIS));

        JPanel scrollContent = new JPanel(new BorderLayout());
        scrollContent.setOpaque(false);
        scrollContent.add(itemWrapper, BorderLayout.NORTH);
        scrollContent.add(footerContainer, BorderLayout.SOUTH);

        FluentScrollPane itemScroll = new FluentScrollPane(scrollContent);
        navPane.add(itemScroll, BorderLayout.CENTER);
        add(navPane, BorderLayout.WEST);

        // ---- 页面宿主 ----
        contentHost.setLayout(cardLayout);
        contentHost.setOpaque(false);
        add(contentHost, BorderLayout.CENTER);

        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        navPane.repaint();
        repaint();
    }

    // ==================================================================
    // 页面注册
    // ==================================================================

    /** 注册一个主页面（显示在导航列表里），立即构建 */
    public FluentNavigationView addPage(String title, char glyph, JComponent page) {
        return addPage(title, glyph, () -> page);
    }

    /** 注册一个固定在底部的页面（例如「设置」），立即构建 */
    public FluentNavigationView addFooterPage(String title, char glyph, JComponent page) {
        return addFooterPage(title, glyph, () -> page);
    }

    /**
     * 注册一个<b>延迟构建</b>的主页面。
     *
     * <p>页面直到第一次被选中才构造。实测三个页面在启动时全部构建要接近 2 秒
     * （组件构造、字体度量、首次布局都在里面），而用户启动后只看得到第一个页面——
     * 另外两个的构建时间纯属白等。</p>
     */
    public FluentNavigationView addPage(String title, char glyph,
                                        java.util.function.Supplier<JComponent> pageSupplier) {
        return addPageInternal(title, glyph, false, pageSupplier);
    }

    /** 注册一个延迟构建的底部页面 */
    public FluentNavigationView addFooterPage(String title, char glyph,
                                              java.util.function.Supplier<JComponent> pageSupplier) {
        return addPageInternal(title, glyph, true, pageSupplier);
    }

    private FluentNavigationView addPageInternal(String title, char glyph, boolean footer,
                                                 java.util.function.Supplier<JComponent> pageSupplier) {
        NavItem item = new NavItem(title, glyph, footer);
        item.pageSupplier = pageSupplier;
        if (footer) {
            footerItems.add(item);
            installItem(item, footerContainer);
        } else {
            items.add(item);
            installItem(item, itemContainer);
        }
        // 第一个注册的页面立即构建，避免启动后是空白
        if (selected == null) {
            select(item);
        }
        return this;
    }

    private void installItem(NavItem item, JPanel container) {
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, FluentMetrics.NAV_ITEM_HEIGHT));
        container.add(item);
        container.add(Box.createVerticalStrut(FluentMetrics.SPACING_XXS));
    }

    /** 确保页面已构建（懒加载在这里发生） */
    private JComponent ensurePage(NavItem item) {
        if (item.page == null && item.pageSupplier != null) {
            JComponent page = item.pageSupplier.get();
            item.page = page;
            item.pageSupplier = null;   // 只构建一次，同时释放对闭包的引用
            page.setOpaque(false);
            contentHost.add(page, item.pageKey);
        }
        return item.page;
    }

    /** 选中第一个页面 */
    public void selectFirst() {
        if (!items.isEmpty()) {
            select(items.get(0));
        } else if (!footerItems.isEmpty()) {
            select(footerItems.get(0));
        }
    }

    /** 选中某个页面（页面尚未构建时先构建） */
    public void select(NavItem item) {
        if (item == null) {
            return;
        }
        ensurePage(item);
        selected = item;
        for (NavItem i : allItems()) {
            i.setSelected(i == item);
        }
        cardLayout.show(contentHost, item.pageKey);
        navPane.repaint();
    }

    /** 按标题选中 */
    public void selectByTitle(String title) {
        for (NavItem item : allItems()) {
            if (item.title.equals(title)) {
                select(item);
                return;
            }
        }
    }

    public NavItem selectedItem() {
        return selected;
    }

    /** 全部导航项（主列表 + 底部列表），顺序与导航栏一致 */
    public List<NavItem> pages() {
        return allItems();
    }

    private List<NavItem> allItems() {
        List<NavItem> all = new ArrayList<>(items);
        all.addAll(footerItems);
        return all;
    }

    // ==================================================================
    // 折叠
    // ==================================================================

    public boolean isCompact() {
        return compact;
    }

    public void setCompact(boolean compact) {
        if (this.compact == compact) {
            return;
        }
        this.compact = compact;
        navPane.setPreferredSize(new Dimension(
                compact ? FluentMetrics.NAV_WIDTH_COMPACT : FluentMetrics.NAV_WIDTH_EXPANDED, 0));
        searchBox.setVisible(!compact);
        for (NavItem item : allItems()) {
            item.setCompact(compact);
        }
        navPane.revalidate();
        navPane.repaint();
        revalidate();
        repaint();
    }

    // ==================================================================
    // 导航项
    // ==================================================================

    /**
     * 单个导航项。
     *
     * <p>没有用 {@code JButton}：导航项需要「左对齐图标 + 文字、最左侧还有一条独立于项目之外的
     * 指示条」，用按钮做会一直在跟 {@code getPreferredSize} 与居中逻辑较劲。</p>
     */
    public class NavItem extends JComponent {

        private final String title;
        private final char glyph;
        private final boolean footer;
        private final String pageKey = "page-" + System.identityHashCode(this);
        private JComponent page;
        /** 延迟构建用的工厂，页面构建后置空 */
        private java.util.function.Supplier<JComponent> pageSupplier;

        private boolean hover;
        private boolean pressed;
        private boolean selected;

        NavItem(String title, char glyph, boolean footer) {
            this.title = title;
            this.glyph = glyph;
            this.footer = footer;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            RevealEngine.register(this);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    pressed = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                    if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
                        select(NavItem.this);
                    }
                }
            });
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        void setCompact(boolean compact) {
            revalidate();
            repaint();
        }

        public String title() {
            return title;
        }

        public JComponent page() {
            return page;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(compact ? FluentMetrics.NAV_WIDTH_COMPACT : 240,
                    FluentMetrics.NAV_ITEM_HEIGHT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, FluentMetrics.NAV_ITEM_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                int w = getWidth();
                int h = getHeight();
                int indent = 6;
                Shape body = FluentPainting.roundRect(indent, 2, Math.max(0, w - indent - 6), h - 4,
                        FluentMetrics.RADIUS_SMALL);

                // 底色：选中 > 按下 > 悬停
                Color fill = null;
                if (selected) {
                    fill = FluentColors.subtleHover();
                } else if (pressed) {
                    fill = FluentColors.subtlePressed();
                } else if (hover) {
                    fill = FluentColors.subtleHover();
                }
                FluentPainting.fill(g2, body, fill);
                RevealEngine.paint(this, g2, body);

                // 选中指示条：窗格最左侧的 3×16 主题色圆角竖条
                if (selected) {
                    Shape indicator = FluentPainting.roundRect(
                            0, (h - FluentMetrics.NAV_INDICATOR_HEIGHT) / 2.0,
                            FluentMetrics.NAV_INDICATOR_WIDTH, FluentMetrics.NAV_INDICATOR_HEIGHT,
                            FluentMetrics.RADIUS_PILL);
                    FluentPainting.fill(g2, indicator, FluentColors.accent());
                }

                // 图标
                Color foreground = selected ? FluentColors.accent() : FluentColors.text();
                int iconSize = 16;
                int iconX = indent + (compact ? (w - indent - 6 - iconSize) / 2 : FluentMetrics.SPACING_M);
                int iconY = (h - iconSize) / 2;
                paintGlyph(g2, iconX, iconY, iconSize, foreground);

                // 文字
                if (!compact) {
                    FontMetrics fm = g2.getFontMetrics(FluentTypography.body());
                    int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.setFont(selected ? FluentTypography.bodyStrong() : FluentTypography.body());
                    g2.setColor(foreground);
                    g2.drawString(title, iconX + iconSize + FluentMetrics.SPACING_M, baseline);
                }

                if (isFocusOwner()) {
                    FluentPainting.focusRing(g2, this, FluentMetrics.RADIUS_SMALL);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintGlyph(Graphics2D g2, int x, int y, int size, Color color) {
            g2.setColor(color);
            if (!FluentTypography.iconFontAvailable()) {
                g2.fillRoundRect(x, y, size, size, 4, 4);
                return;
            }
            var font = FluentIcons.font(size);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);
            String s = String.valueOf(glyph);
            int gw = fm.stringWidth(s);
            int baseline = y + (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(s, x + (size - gw) / 2f, baseline);
        }

        @Override
        public String toString() {
            return "NavItem[" + title + (footer ? ", footer" : "") + "]";
        }
    }

    /** 便捷：给页面宿主加一个分隔线容器 */
    public static JComponent divider() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(FluentColors.divider());
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, 1));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return p;
    }

    /** 便捷：标题装饰（未使用，保留给分组标题） */
    static JLabel groupLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FluentTypography.caption());
        label.setForeground(FluentColors.textSecondary());
        return label;
    }
}
