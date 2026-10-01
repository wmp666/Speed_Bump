package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.wmp.speed_bump.common.ui.components.SBProgressBar;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import raven.modal.utils.FlatLafStyleUtils;

import javax.swing.*;
import java.awt.*;

public class UITools {
    /**
     * 把通用进度条装进一个横向面板。
     * <p>
     * 参数用通用端 {@link SBProgressBar}（调用方只需通用能力），
     * 具体的外观样式只有落到 Swing 实现上时才需要设置。
     */
    public static JPanel createProgressBarPanel(SBProgressBar... progressBar) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        // 6px：Fluent 的进度条是「细条 + 上下留白」。

        int barHeight = 6;

        for (SBProgressBar bar : progressBar) {
            if (!(bar instanceof JProgressBar jProgressBar)) {
                // 非 Swing 实现没有这段共享外观逻辑，交由该平台自己处理
                continue;
            }
            jProgressBar.putClientProperty(FlatClientProperties.STYLE, "arc: 0");

            // 形状由 FluentProgressBarUI 按 is_use_square_component 决定。
            jProgressBar.setPreferredSize(new Dimension(0, barHeight));   // 宽度0表示由布局决定
            jProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, barHeight));
            // 可选：缩小字体，使百分比文本更紧凑
            jProgressBar.setFont(jProgressBar.getFont().deriveFont(10f));
            // 可选：去掉百分比文本（若想节省空间）:
            jProgressBar.setStringPainted(false);

            panel.add(jProgressBar);
        }

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
