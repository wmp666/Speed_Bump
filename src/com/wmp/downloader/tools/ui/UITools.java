package com.wmp.downloader.tools.ui;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

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
        }

        // 移除最后一个多余间隔（可选）
        // 或者直接返回
        return panel;
    }

    public static JPanel createProgressBarsPanel(JPanel... progressBarsPanel) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for (JPanel bar : progressBarsPanel) {
            panel.add(bar);
        }
        return panel;
    }

    public static JScrollPane setScrollPaneUnOpaque(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        return scrollPane;
    }

    public static JEditorPane createMarkdownPane(String markdownText){
        if (markdownText == null) {
            var jEditorPane = new JEditorPane();
            jEditorPane.setOpaque(false);
            jEditorPane.setEditable(false);
            return jEditorPane;
        }

        // 1. 使用 commonmark 将 Markdown 转换为 HTML
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(parser.parse(markdownText));

        // 2. 在 Swing 的 JEditorPane 中显示 HTML
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setOpaque(false);
        editorPane.setEditable(false);

        return editorPane;
    }
}
