package com.wmp.downloader.tools.ui;

import javax.swing.*;
import java.awt.*;

public class UITools {
    public static JPanel createProgressBarPanel(JProgressBar... progressBar) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        int barHeight = 12; // 固定高度，可调整

        for (JProgressBar bar : progressBar) {
            // 设置高度固定，宽度填满（通过设置最大宽度为 Integer.MAX_VALUE）
            bar.setPreferredSize(new Dimension(0, barHeight));   // 宽度0表示由布局决定
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, barHeight));
            // 可选：缩小字体，使百分比文本更紧凑
            bar.setFont(bar.getFont().deriveFont(10f));
            // 可选：去掉百分比文本（若想节省空间）:
            bar.setStringPainted(false);

            panel.add(bar);
            // 可添加小间距
            panel.add(Box.createHorizontalStrut(2));
        }

        // 移除最后一个多余间隔（可选）
        // 或者直接返回
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

    public static JScrollPane setScrollPaneUnOpaque(JScrollPane scrollPane){
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        return scrollPane;
    }
}
