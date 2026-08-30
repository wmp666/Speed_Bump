package com.wmp.downloader.newArchitecture.ui.mainFrame;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel {
    private JButton button1;
    private JButton button2;
    private JPanel mainPanel;

    public StatusPanel() {

        this.setLayout(new BorderLayout());
        this.add(mainPanel);
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
}
