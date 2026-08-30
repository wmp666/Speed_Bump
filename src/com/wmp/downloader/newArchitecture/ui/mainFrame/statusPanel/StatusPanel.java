package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;

import javax.swing.*;
import java.awt.*;

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

        MessageCenterLabel.addActionListener(e -> {
            var jDialog = new JDialog();
            var msgCenterPanel = new MsgCenterPanel();
            msgCenterPanel.loadMsg();
            jDialog.add(new JScrollPane(msgCenterPanel));
            jDialog.pack();
            jDialog.setVisible(true);
        });

        setFocusTraversalPolicy(new LayoutFocusTraversalPolicy());
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
