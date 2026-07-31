package com.wmp.downloader.tools.ui;

import javax.swing.*;

public class UITools {
    public static JPanel createProgressBarPanel(JProgressBar... progressBar){
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (JProgressBar bar : progressBar) {
            panel.add(bar);
        }
        return panel;
    }

    public static JPanel createProgressBarsPanel(JPanel... progressBarsPanel){
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JPanel bar : progressBarsPanel) {
            panel.add(bar);
        }
        return panel;
    }
}
