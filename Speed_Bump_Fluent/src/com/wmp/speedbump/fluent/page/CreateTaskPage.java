package com.wmp.speedbump.fluent.page;

import com.wmp.speedbump.fluent.component.FluentButton;
import com.wmp.speedbump.fluent.component.FluentCard;
import com.wmp.speedbump.fluent.component.FluentCheckBox;
import com.wmp.speedbump.fluent.component.FluentComboBox;
import com.wmp.speedbump.fluent.component.FluentRadioButton;
import com.wmp.speedbump.fluent.component.FluentTextArea;
import com.wmp.speedbump.fluent.component.FluentTextBox;
import com.wmp.speedbump.fluent.component.FluentToggleSwitch;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentTypography;
import com.wmp.speedbump.fluent.ui.FluentPage;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * 创建任务页。
 *
 * <p>用来验证「多行输入 + 选择器 + 开关 + 单选」这一整套控件在真实表单里的组合效果。
 * 表单的组织方式沿用 WinUI 设置页的两条规则：</p>
 * <ol>
 *   <li>一张卡片只放一组相关设置，标题在卡片内、说明在标题下；</li>
 *   <li>每个设置项独占一行，「标签在左、控件在右」，行之间用 1px 分隔线。</li>
 * </ol>
 */
public class CreateTaskPage extends FluentPage {

    private final FluentTextArea linkInput =
            new FluentTextArea("每行一个链接，支持 HTTP / HTTPS / 磁力链接 / 种子文件路径；也可以直接把文件拖进来", 6);
    private final FluentTextBox folderInput = new FluentTextBox("下载保存目录");
    private FluentToggleSwitch resumeSwitch;
    private FluentCheckBox autoOpenBox;
    private FluentComboBox<String> protocolBox;
    private FluentComboBox<String> threadBox;

    public CreateTaskPage() {
        super("创建任务", "粘贴链接或拖入种子文件");

        add(buildLinkCard());
        gap(FluentMetrics.SPACING_L);
        add(buildLocationCard());
        gap(FluentMetrics.SPACING_L);
        add(buildOptionsCard());
        gap(FluentMetrics.SPACING_L);
        add(buildActions());
    }

    // ==================================================================
    // 下载链接
    // ==================================================================

    private JComponent buildLinkCard() {
        FluentCard card = new FluentCard("下载链接", "支持多行批量添加");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        linkInput.setPreferredSize(new Dimension(600, 132));
        linkInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 132));
        linkInput.setText("https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/24.04/"
                + "ubuntu-24.04.2-desktop-amd64.iso\n"
                + "magnet:?xt=urn:btih:2f1c9a1f0d3e7b5c8a4d6e2f9b0c1a3d5e7f9b0c");
        card.addContent(linkInput);
        card.addContent(javax.swing.Box.createVerticalStrut(FluentMetrics.SPACING_S));

        JLabel parsed = new JLabel("已识别 2 个链接：HTTP 多线程分段 · BitTorrent（磁力）");
        parsed.setFont(FluentTypography.caption());
        parsed.setForeground(FluentColors.textSecondary());
        parsed.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.addContent(parsed);
        return card;
    }

    // ==================================================================
    // 保存位置
    // ==================================================================

    private JComponent buildLocationCard() {
        FluentCard card = new FluentCard("保存位置");
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        folderInput.setText(System.getProperty("user.home") + "\\Downloads");
        JPanel row = new JPanel(new BorderLayout(FluentMetrics.SPACING_S, 0));
        row.setOpaque(false);
        row.add(folderInput, BorderLayout.CENTER);

        FluentButton browse = new FluentButton("浏览…", FluentIcons.FOLDER);
        browse.tip("选择保存目录");
        row.add(browse, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, FluentMetrics.CONTROL_HEIGHT));
        card.addContent(row);

        card.addContent(javax.swing.Box.createVerticalStrut(FluentMetrics.SPACING_M));
        resumeSwitch = new FluentToggleSwitch("断点续传", true);
        resumeSwitch.tip("中断后再次开始时可从已下载部分继续");
        card.addContent(resumeSwitch);
        return card;
    }

    // ==================================================================
    // 任务选项
    // ==================================================================

    private JComponent buildOptionsCard() {
        FluentCard card = new FluentCard("任务选项", "仅对本次任务生效");

        protocolBox = new FluentComboBox<>(new String[]{
                "自动识别", "HTTP / HTTPS", "BitTorrent", "交由 Gopeed 处理"});
        card.addRow("下载协议", "自动识别会根据链接与文件类型选择解析器", protocolBox);

        threadBox = new FluentComboBox<>(new String[]{"1", "4", "8", "16", "32"});
        threadBox.setSelectedItem("8");
        card.addRow("下载线程数", "服务器不支持分段时自动回落单线程", threadBox);

        autoOpenBox = new FluentCheckBox("下载完成后打开所在文件夹", true);
        card.addRow("完成后行为", "任务进入「已完成」时触发", autoOpenBox);

        card.addRow("下载后处理", "流拷贝不重新编码，速度最快", buildPostProcessOptions());

        card.addRow("代理", "使用系统代理设置（跟随 IE 代理配置）", new FluentToggleSwitch("跟随系统"));
        return card;
    }

    /** 单选组：三个互斥选项 */
    private JComponent buildPostProcessOptions() {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.RIGHT, FluentMetrics.SPACING_M, 0));
        group.setOpaque(false);
        ButtonGroup buttonGroup = new ButtonGroup();

        FluentRadioButton none = new FluentRadioButton("不处理", true);
        FluentRadioButton remux = new FluentRadioButton("流拷贝合并");
        FluentRadioButton transcode = new FluentRadioButton("转码");
        for (FluentRadioButton button : new FluentRadioButton[]{none, remux, transcode}) {
            buttonGroup.add(button);
            group.add(button);
        }
        return group;
    }

    // ==================================================================
    // 底部动作
    // ==================================================================

    private JComponent buildActions() {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, FluentMetrics.CONTROL_HEIGHT_LARGE));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, FluentMetrics.SPACING_S, 0));
        left.setOpaque(false);
        // 一个页面只应有一个强调按钮
        FluentButton start = FluentButton.accent("开始下载", FluentIcons.DOWNLOAD);
        FluentButton clear = new FluentButton("清空", FluentIcons.CANCEL);
        clear.addActionListener(e -> linkInput.setText(""));
        left.add(start);
        left.add(clear);
        row.add(left, BorderLayout.WEST);
        return row;
    }
}
