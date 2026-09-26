package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.FlatClientProperties;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import raven.modal.utils.FlatLafStyleUtils;

import javax.swing.*;
import java.awt.*;

public class UITools {
    public static JPanel createProgressBarPanel(JProgressBar... progressBar) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        // 6px：Fluent 的进度条是「细条 + 上下留白」。
        // 原来是 12px 且条形填满整个高度，显得笨重。
        // 注意细度由这里的高度决定，而不是由 UI 代理去画一条细线——
        // 让组件一部分区域保持透明，会依赖父容器正确重绘，
        // 在本项目「根面板画背景图 + 大量透明子面板」的结构里容易出残影。
        int barHeight = 6;

        for (JProgressBar bar : progressBar) {
            // 这里原本强制 "arc: 0"（方角）。现在不再逐组件锁定形状，
            // 而是统一服从「方角组件」这个全局设置，避免出现
            // 「按钮直角、进度条圆角」这种自相矛盾的界面。
            // 形状由 FluentProgressBarUI 按 is_use_square_component 决定。
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
