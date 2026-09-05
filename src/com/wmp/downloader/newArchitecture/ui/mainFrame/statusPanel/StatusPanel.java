package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StatusPanel extends JPanel {
    private JPanel mainPanel;
    private JButton MessageCenterLabel;
    private JButton ToolsLabel;

    private Downloader downloader;

    private JPopupMenu ToolsPopupMenu = null;

    public StatusPanel(Downloader downloader) {
        this.downloader = downloader;

        this.setLayout(new BorderLayout());
        this.add(mainPanel);

        ToolsLabel.setText("");
        MessageCenterLabel.setText("");

        ToolsLabel.addActionListener(e -> {
            if (ToolsPopupMenu != null) {
                ToolsPopupMenu.show(ToolsLabel, 0, -ToolsPopupMenu.getPreferredSize().height);
            }
        });

        AtomicInteger count = new AtomicInteger();
        AtomicBoolean isShow = new AtomicBoolean(false);
        AtomicReference<MsgCenterPanelControl> msgCenterPanelControl = new AtomicReference<>();
        MessageCenterLabel.addActionListener(e -> {
            if (!isShow.get()) {
                if (count.get() == 0) {
                    msgCenterPanelControl.set(loadMsgPanel(downloader));

                }else{
                    msgCenterPanelControl.get().msgCenterPanel.loadMsg();
                    downloader.getLayeredPane().add(msgCenterPanelControl.get().topPanel, JLayeredPane.MODAL_LAYER);
                }
                isShow.set(true);
                count.getAndIncrement();
            }else {
                downloader.getLayeredPane().remove(msgCenterPanelControl.get().topPanel);
                downloader.revalidate();
                downloader.repaint();
                isShow.set(false);
            }

        });

        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
    }

    private record MsgCenterPanelControl(JPanel topPanel, MsgCenterPanel msgCenterPanel){}

    private MsgCenterPanelControl loadMsgPanel(Downloader downloader) {
        JPanel temp = new JPanel(new BorderLayout()){
            @Override
            protected void paintComponent(Graphics g) {
                /*Graphics2D g2 = (Graphics2D) g.create();

                Color translucent = new Color(128, 128, 128, 200);


                g2.setColor(translucent);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();*/
            }
        };
        temp.setOpaque(false);

        var msgCenterPanel = new MsgCenterPanel();
        msgCenterPanel.loadMsg();
        var scrollPane = new JScrollPane(msgCenterPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        scrollPane.getVerticalScrollBar().setValue(0);
        temp.add(scrollPane, BorderLayout.EAST);

        downloader.getLayeredPane().add(temp, JLayeredPane.MODAL_LAYER);

        // ---------- 4. 关键：让 topPanel 与 ContentPane 保持同样大小 ----------
        downloader.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // 获取内容面板的尺寸（排除边框装饰）
                Rectangle bounds = downloader.getContentPane().getBounds();
                temp.setBounds(0, 0, bounds.width, bounds.height);
            }
        });
        // 首次显示时立即调整一次
        SwingUtilities.invokeLater(() -> {
            Rectangle bounds = downloader.getContentPane().getBounds();
            temp.setBounds(0, 0, bounds.width, bounds.height);
        });

        return new MsgCenterPanelControl(temp, msgCenterPanel);
    }

    private void createUIComponents(){
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

    public void setPopupMenu(JPopupMenu popupMenu){
        this.ToolsPopupMenu = popupMenu;
        ToolsLabel.setComponentPopupMenu(popupMenu);
    }
}
