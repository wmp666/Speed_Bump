package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.ui.Downloader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * 状态栏：左下任务运行状态 / 右下工具箱与消息中心浮层入口。
 */
public class StatusPanel extends JPanel {

    //固定宽度（右侧浮层卡片）
    private static final int MSG_CARD_WIDTH = 360;
    private static final int TOOLS_CARD_WIDTH = 300;
    //工具箱卡片距窗口底部的高度（避免盖住状态栏上的按钮）
    private static final int BOTTOM_MARGIN = 44;

    private JPanel mainPanel;
    private JButton MessageCenterLabel;
    private JButton ToolsLabel;

    private final Downloader downloader;

    private JPopupMenu ToolsPopupMenu = null;

    // ---------- 左侧：任务运行状态 ----------
    private JLabel taskStatusLabel;   //复用表单里的问候语 JLabel，直接承载 图标+运行/总数 文本
    private String runStateIconKey = "statusbar.idle";
    private long lastRun = -1;
    private int lastTotal = -1;
    private final Timer taskStatusTimer;

    // ---------- 消息中心覆盖层 ----------
    private MsgCenterParts msgParts = null;
    private boolean msgVisible = false;

    // ---------- Tools 覆盖层 ----------
    private JPanel toolsTopPanel = null;
    private JPanel toolsBottom = null;   //SOUTH 停靠容器，承载卡片
    private JPanel toolsCard = null;
    private boolean toolsVisible = false;

    public StatusPanel(Downloader downloader) {
        this.downloader = downloader;

        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        ToolsLabel.setText("");
        MessageCenterLabel.setText("");

        //左侧：任务运行计数（替换表单里的“你好世界”）
        setupTaskStatusLabel();

        ToolsLabel.addActionListener(e -> toggleTools());
        MessageCenterLabel.addActionListener(e -> toggleMsgCenter());

        //覆盖层与窗口同步缩放
        downloader.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fitOverlays();
            }
        });

        taskStatusTimer = new Timer(300, e -> updateTaskStatus());
        taskStatusTimer.start();
        //立即渲染一次，避免短暂显示旧文案
        updateTaskStatus();

        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
    }

    // =====================================================================
    // 左侧任务运行状态（胶囊式状态格）
    // =====================================================================

    /**
     * 找到表单里仅有的问候语 JLabel，直接改造为“图标+运行/总数”状态胶囊。
     * 不向 Designer 的 Grid 容器新增组件（避免缺 GridConstraints 抛 NPE）。
     */
    private void setupTaskStatusLabel() {
        taskStatusLabel = findFirstTextLabel(mainPanel);
        if (taskStatusLabel == null) return;

        taskStatusLabel.setOpaque(false);
        taskStatusLabel.setIconTextGap(8);
        taskStatusLabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        //图标跟随主题与运行状态动态刷新
        IconControl.addInDynamicConverter(() ->
                taskStatusLabel.setIcon(IconControl.getIcon(runStateIconKey, statusIconSize())));
        taskStatusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        //点击跳转到“任务中心”页
        taskStatusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectMainTab(downloader.downloaderPanel);
            }
        });
    }

    private JLabel findFirstTextLabel(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JLabel l && l.getText() != null && !l.getText().isEmpty() && l.getIcon() == null) {
                return l;
            }
            if (child instanceof Container cc) {
                JLabel found = findFirstTextLabel(cc);
                if (found != null) return found;
            }
        }
        return null;
    }

    private int statusIconSize() {
        if (taskStatusLabel == null || taskStatusLabel.getFont() == null) return 14;
        return (int) Math.round(taskStatusLabel.getFont().getSize() * 1.35);
    }

    /**
     * 刷新“运行中 / 总数”。仅在计数变化时才更新文本与图标，降低开销。
     */
    private void updateTaskStatus() {
        if (taskStatusLabel == null) return;
        int total = downloader.taskList.size();
        long run = downloader.taskList.stream().filter(AbstractTask::isRunning).count();
        if (total == lastTotal && run == lastRun) return;
        lastTotal = total;
        lastRun = run;

        runStateIconKey = run > 0 ? "statusbar.running" : "statusbar.idle";
        String text = run > 0
                ? String.format(StringFormat.translate("statusbar.running_summary"), run, total)
                : String.format(StringFormat.translate("statusbar.idle_summary"), total);

        taskStatusLabel.setText(text);
        taskStatusLabel.setToolTipText(text);
        //立即重取图标（主题切换时 addInDynamicConverter 里的任务也会刷新）
        taskStatusLabel.setIcon(IconControl.getIcon(runStateIconKey, statusIconSize()));
    }

    // =====================================================================
    // Tools 浮层卡片（右上角）
    // =====================================================================

    private void toggleTools() {
        if (toolsVisible) hideTools();
        else showTools();
    }

    private void showTools() {
        if (msgVisible) hideMsgCenter();

        if (toolsTopPanel == null) {
            toolsTopPanel = new JPanel(new BorderLayout());
            toolsTopPanel.setOpaque(false);
            //点击卡片以外的空白区域即收起（类菜单交互）
            toolsTopPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    hideTools();
                }
            });
        }
        //若上一轮关闭时已从层级移除，这里重新加入
        if (toolsTopPanel.getParent() == null) {
            downloader.getLayeredPane().add(toolsTopPanel, JLayeredPane.MODAL_LAYER);
        }

        //卡片只构建一次，避免反复开关重复注册图标动态任务
        if (toolsCard == null) {
            toolsCard = buildToolsCard();
        }
        if (toolsBottom == null) {
            toolsBottom = new JPanel(new BorderLayout());
            toolsBottom.setOpaque(false);
            //贴合状态栏，从窗口底部右端往上弹出（预留状态栏高度，避免压在按钮上）
            toolsBottom.setBorder(BorderFactory.createEmptyBorder(0, 0, BOTTOM_MARGIN, 8));
            toolsBottom.add(toolsCard, BorderLayout.EAST);
        }
        if (toolsBottom.getParent() == null) {
            toolsTopPanel.add(toolsBottom, BorderLayout.SOUTH);
        }
        fitOverlay(toolsTopPanel);
        downloader.revalidate();
        downloader.repaint();
        toolsVisible = true;
    }

    private JPanel buildToolsCard() {
        JPanel card = newFloatingCard();
        card.setLayout(new BorderLayout());

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 8, 0, 8);

        //直接以原始菜单（含全部原菜单项）为来源构建，保持菜单栏内容完整
        if (ToolsPopupMenu != null) {
            appendMenuItems(ToolsPopupMenu.getComponents(), body, gbc, 0);
        }

        //底部留白占位，避免卡片被内容撑得过高时失衡
        gbc.weighty = 1.0;
        gbc.gridy++;
        body.add(Box.createVerticalGlue(), gbc);

        card.add(body, BorderLayout.CENTER);

        Dimension pref = card.getPreferredSize();
        card.setPreferredSize(new Dimension(Math.max(TOOLS_CARD_WIDTH, pref.width), pref.height));
        return card;
    }

    /**
     * 递归把 JPopupMenu/JMenu 里的子项转成卡片的纵向列表。
     * JMenu 作为分组标题，JMenuItem/JSeparator 原样展开，保证菜单项完整。
     */
    private void appendMenuItems(Component[] items, JPanel body, GridBagConstraints gbc, int depth) {
        for (Component c : items) {
            if (c instanceof JSeparator) {
                addSeparator(gbc, body);
            } else if (c instanceof JMenu sub) {
                addGroupHeader(gbc, body, sub.getText());
                appendMenuItems(sub.getMenuComponents(), body, gbc, depth + 1);
            } else if (c instanceof JMenuItem item) {
                String text = item.getText();
                if (text == null || text.isEmpty()) {
                    addSeparator(gbc, body);
                    continue;
                }
                if (item instanceof JCheckBoxMenuItem cb) {
                    text = (cb.isSelected() ? "\u2611  " : "\u2610  ") + text;
                }
                String label = text;
                Icon icon = item.getIcon() != null ? item.getIcon() : spacerIcon(16);
                gbc.gridy++;
                body.add(new LeafRow(label, icon, depth, () -> {
                    runMenuItem(item);
                }), gbc);
            }
        }
    }

    private void runMenuItem(JMenuItem item) {
        if (item instanceof JCheckBoxMenuItem cb) {
            cb.setSelected(!cb.isSelected());
        }
        for (java.awt.event.ActionListener al : item.getActionListeners()) {
            al.actionPerformed(new java.awt.event.ActionEvent(item,
                    java.awt.event.ActionEvent.ACTION_PERFORMED, null));
        }
        hideTools();
    }

    private void addGroupHeader(GridBagConstraints gbc, JPanel body, String text) {
        if (text == null || text.isEmpty()) return;
        gbc.gridy++;
        JLabel head = new JLabel(text);
        head.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        head.setBorder(BorderFactory.createEmptyBorder(10, 10, 2, 10));
        head.setOpaque(false);
        body.add(head, gbc);
    }

    private void addSeparator(GridBagConstraints gbc, JPanel body) {
        gbc.gridy++;
        body.add(new JSeparator(), gbc);
        gbc.gridy++;
    }

    /**
     * 等宽透明占位图标，让没有图标菜单项的文字起点与带图标行对齐。
     */
    private static ImageIcon spacerIcon(int size) {
        return new ImageIcon(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
    }

    private void hideTools() {
        if (toolsTopPanel != null) {
            downloader.getLayeredPane().remove(toolsTopPanel);
            downloader.revalidate();
            downloader.repaint();
        }
        toolsVisible = false;
    }

    // =====================================================================
    // 消息中心浮层（右上角卡片式）
    // =====================================================================

    private void toggleMsgCenter() {
        if (msgVisible) hideMsgCenter();
        else showMsgCenter();
    }

    private void showMsgCenter() {
        if (toolsVisible) hideTools();

        if (msgParts == null) {
            msgParts = buildMsgParts();
        } else {
            msgParts.msgCenterPanel.loadMsg();
            downloader.getLayeredPane().add(msgParts.topPanel, JLayeredPane.MODAL_LAYER);
        }
        scrollMsgTop();
        fitOverlay(msgParts.topPanel);
        downloader.revalidate();
        downloader.repaint();
        msgVisible = true;
    }

    private void hideMsgCenter() {
        if (msgParts != null) {
            downloader.getLayeredPane().remove(msgParts.topPanel);
            downloader.revalidate();
            downloader.repaint();
        }
        msgVisible = false;
    }

    /**
     * 供外部调用：刷新消息并回到顶部。downloader.StatusPanel.refreshMsgCenter();
     */
    public void refreshMsgCenter() {
        if (msgParts == null) return;
        msgParts.msgCenterPanel.loadMsg();
        if (msgVisible) scrollMsgTop();
    }

    private MsgCenterParts buildMsgParts() {
        var msgCenterPanel = new MsgCenterPanel();
        msgCenterPanel.loadMsg();

        //内容右侧固定留白，确保文本绝不会延伸到垂直滚动条下方
        JPanel padHost = new JPanel(new BorderLayout());
        padHost.setOpaque(false);
        padHost.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        padHost.add(msgCenterPanel, BorderLayout.CENTER);

        var scrollPane = new JScrollPane(padHost);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        JPanel card = newFloatingCard();
        card.setLayout(new BorderLayout());
        card.add(buildTitleBar("msg-center", StringFormat.translate("message_center.title"),
                this::hideMsgCenter), BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        //EAST 停靠：固定卡片宽度（高度会被 BorderLayout 撑满窗口可用区）
        card.setPreferredSize(new Dimension(MSG_CARD_WIDTH, MSG_CARD_WIDTH));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(card, BorderLayout.EAST);
        //点击卡片以外区域即收起
        top.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hideMsgCenter();
            }
        });

        downloader.getLayeredPane().add(top, JLayeredPane.MODAL_LAYER);
        return new MsgCenterParts(top, scrollPane, msgCenterPanel);
    }

    private void scrollMsgTop() {
        if (msgParts == null) return;
        var sp = msgParts.scrollPane;
        sp.getVerticalScrollBar().setValue(0);
        sp.getViewport().setViewPosition(new Point(0, 0));
        SwingUtilities.invokeLater(() -> {
            sp.getVerticalScrollBar().setValue(0);
            sp.getViewport().setViewPosition(new Point(0, 0));
        });
    }

    // =====================================================================
    // 通用浮层卡片组件
    // =====================================================================

    /**
     * 带主题化半透明圆角背景的浮动卡片容器。
     * 尺寸由调用方按布局需要设置（EAST 停靠需 setPreferredSize 宽度）。
     */
    private JPanel newFloatingCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color base = UIManager.getColor("Panel.background");
                boolean dark = "dark".equals(DataControl.get("theme_type", "light"));
                Color bg = new Color(base.getRed(), base.getGreen(), base.getBlue(),
                        dark ? 235 : 245);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                //细描边，让浮层与背景区分
                Color edge = dark ? ColorFunctions.lighten(base, 0.18f)
                        : ColorFunctions.darken(base, 0.12f);
                g2.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    /**
     * 卡片标题栏：图标 + 标题 + 关闭按钮。
     */
    private JPanel buildTitleBar(String iconKey, String title, Runnable onClose) {
        JLabel icon = new JLabel();
        IconControl.addInDynamicConverter(
                () -> icon.setIcon(IconControl.getIcon(iconKey, statusIconSize())));

        JLabel titleLabel = new JLabel(title);
        titleLabel.putClientProperty("FlatLaf.style", "font: bold $h3.font");
        titleLabel.setOpaque(false);

        JButton close = new StatusButton("close");
        close.setToolTipText("");

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(icon);
        left.add(titleLabel);

        bar.add(left, BorderLayout.CENTER);
        bar.add(close, BorderLayout.EAST);
        close.addActionListener(e -> onClose.run());
        return bar;
    }

    private void selectMainTab(Component comp) {
        if (comp == null || downloader.mainTabbedPane == null) return;
        int idx = downloader.mainTabbedPane.indexOfComponent(comp);
        if (idx >= 0) downloader.mainTabbedPane.setSelectedIndex(idx);
    }

    // =====================================================================
    // 覆盖层尺寸同步
    // =====================================================================

    private void fitOverlays() {
        if (msgVisible && msgParts != null) fitOverlay(msgParts.topPanel);
        if (toolsVisible && toolsTopPanel != null) fitOverlay(toolsTopPanel);
    }

    private void fitOverlay(JComponent top) {
        Rectangle bounds = downloader.getContentPane().getBounds();
        top.setBounds(0, 0, bounds.width, bounds.height);
    }

    // =====================================================================
    // 历史/系统接口
    // =====================================================================

    public void setPopupMenu(JPopupMenu popupMenu) {
        this.ToolsPopupMenu = popupMenu;
        ToolsLabel.setComponentPopupMenu(popupMenu);
    }

    private void createUIComponents() {
        ToolsLabel = new StatusButton("tool-kit");
        MessageCenterLabel = new StatusButton("msg-center");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Color base = UIManager.getColor("Panel.background");
        Color adjusted = DataControl.get("theme_type", "light").equals("dark")
                ? ColorFunctions.lighten(base, 0.1f)
                : ColorFunctions.darken(base, 0.1f);
        Color translucent = new Color(adjusted.getRed(), adjusted.getGreen(), adjusted.getBlue(), 180);
        g2.setColor(translucent);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    /**
     * 消息中心内部结构引用。
     */
    private record MsgCenterParts(JPanel topPanel, JScrollPane scrollPane, MsgCenterPanel msgCenterPanel) {
    }

    /**
     * 工具箱卡片里的可点击行：统一行高、左边距随层级缩进、悬停高亮。
     * 不用 JButton/半透明整块填充，避免相邻按钮互相压盖重叠。
     */
    private static final class LeafRow extends JLabel {
        private boolean hover = false;

        LeafRow(String text, Icon icon, int depth, Runnable action) {
            super(text);
            setIcon(icon);
            setIconTextGap(8);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(8, 8 + depth * 12, 8, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                Color base = UIManager.getColor("Panel.background");
                boolean dark = "dark".equals(DataControl.get("theme_type", "light"));
                Color c = dark ? ColorFunctions.lighten(base, 0.18f)
                        : ColorFunctions.darken(base, 0.10f);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
