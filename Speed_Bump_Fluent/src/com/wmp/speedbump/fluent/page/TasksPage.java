package com.wmp.speedbump.fluent.page;

import com.wmp.speedbump.fluent.component.FluentButton;
import com.wmp.speedbump.fluent.component.FluentCard;
import com.wmp.speedbump.fluent.component.FluentProgressBar;
import com.wmp.speedbump.fluent.component.FluentTextBox;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;
import com.wmp.speedbump.fluent.ui.FluentPage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务列表页。
 *
 * <h3>这是原型，不是最终实现</h3>
 * <p>任务数据是页内造的，用来说明「Fluent 版的任务卡片长什么样、层次怎么排」，
 * 以及验证组件库在真实组合下的表现。接入真实下载引擎时，
 * 只要把 {@link TaskCard#setProgress} 之类的入口接到下载任务对象上即可，
 * 视图层不需要改动——这正是把 UI 与逻辑分离的收益。</p>
 */
public class TasksPage extends FluentPage {

    /**
     * 是否播放演示动画。
     *
     * <p>关掉它主要为了两种场景：一是做「同一画面不同材质」的截图对照时，
     * 进度条每 400ms 变一次会让两张图的差异无法归因；
     * 二是低性能机器上不需要无谓的定时重绘。</p>
     */
    public static boolean demoAnimationEnabled = true;

    private final List<TaskCard> cards = new ArrayList<>();
    private final JLabel summaryLabel = new JLabel();
    private Timer demoTimer;

    public TasksPage() {
        super("任务", "全部下载任务");

        add(buildToolbar());
        gap(FluentMetrics.SPACING_L);
        add(buildSummary());
        gap(FluentMetrics.SPACING_L);

        addTask("ubuntu-24.04-desktop-amd64.iso", "正在下载", 62.4, 3.2, "1.02 GB / 1.63 GB");
        addTask("Spring Boot 实战（第 3 版）.pdf", "正在下载", 28.9, 1.4, "18.4 MB / 63.7 MB");
        addTask("bilibili_4K_HDR_测试片源.mp4", "等待中", 0, 0, "0 B / 412 MB");
        addTask("jdk-25_windows-x64_bin.exe", "已完成", 100, 0, "186 MB / 186 MB");
    }

    // ==================================================================
    // 顶部工具栏
    // ==================================================================

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(FluentMetrics.SPACING_M, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, FluentMetrics.CONTROL_HEIGHT_LARGE));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, FluentMetrics.SPACING_S, 0));
        left.setOpaque(false);
        FluentButton create = FluentButton.accent("创建任务", FluentIcons.ADD);
        create.tip("新建下载任务（Ctrl+N）");
        FluentButton startAll = new FluentButton("全部开始", FluentIcons.PLAY);
        FluentButton pauseAll = new FluentButton("全部暂停", FluentIcons.PAUSE);
        FluentButton refresh = FluentButton.icon(FluentIcons.REFRESH, FluentButton.Style.SUBTLE);
        refresh.tip("刷新");
        left.add(create);
        left.add(startAll);
        left.add(pauseAll);
        left.add(refresh);
        bar.add(left, BorderLayout.WEST);

        FluentTextBox search = new FluentTextBox("按文件名搜索", FluentIcons.SEARCH);
        search.setPreferredSize(new Dimension(280, FluentMetrics.CONTROL_HEIGHT));
        search.setMaximumSize(new Dimension(280, FluentMetrics.CONTROL_HEIGHT));
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(search);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ==================================================================
    // 概览卡片
    // ==================================================================

    private JComponent buildSummary() {
        FluentCard card = new FluentCard();
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPadding(FluentMetrics.SPACING_M);

        JPanel grid = new JPanel(new GridLayout(1, 4, FluentMetrics.SPACING_S, 0));
        grid.setOpaque(false);
        grid.add(statCell("正在下载", "2", FluentColors.accent()));
        grid.add(statCell("等待中", "1", FluentColors.text()));
        grid.add(statCell("已完成", "12", color(0x0F, 0x7B, 0x0F)));
        grid.add(statCell("总速度", "4.6 MB/s", FluentColors.text()));

        card.addContent(grid);
        card.addContent(Box.createVerticalStrut(FluentMetrics.SPACING_S));
        summaryLabel.setText("下载目录：C:\\Users\\Public\\Downloads　·　"
                + "后台运行时关闭窗口不会中断任务");
        summaryLabel.setFont(FluentTypography.caption());
        summaryLabel.setForeground(FluentColors.textSecondary());
        summaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.addContent(summaryLabel);
        return card;
    }

    private JComponent statCell(String caption, String value, Color valueColor) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(BorderFactory.createEmptyBorder(
                FluentMetrics.SPACING_S, FluentMetrics.SPACING_M,
                FluentMetrics.SPACING_S, FluentMetrics.SPACING_M));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(FluentTypography.subtitle());
        valueLabel.setForeground(valueColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(FluentTypography.caption());
        captionLabel.setForeground(FluentColors.textSecondary());
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        cell.add(valueLabel);
        cell.add(captionLabel);
        return cell;
    }

    // ==================================================================
    // 任务卡片
    // ==================================================================

    private TaskCard addTask(String name, String status, double progress, double speedMbps, String sizeText) {
        TaskCard card = new TaskCard(name, status, progress, speedMbps, sizeText);
        cards.add(card);
        add(card);
        gap(FluentMetrics.SPACING_M);
        return card;
    }

    /**
     * 单条任务卡片：文件名 + 状态徽标 → 进度条 → 进度文字 + 操作按钮。
     *
     * <p>层次刻意压得很扁：Fluent 不用卡片阴影表达层级，所以这里靠「一行标题、一条进度、
     * 一行说明」三段式来控制信息密度，避免卡片在列表里显得笨重。</p>
     */
    public static class TaskCard extends FluentCard {

        private final JLabel nameLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final FluentProgressBar progressBar = new FluentProgressBar();
        private final JLabel detailLabel = new JLabel();
        private final FluentButton actionButton;

        private double progress;
        private double speedMbps;
        private String sizeText;
        private String status;

        TaskCard(String name, String status, double progress, double speedMbps, String sizeText) {
            super();
            this.progress = progress;
            this.speedMbps = speedMbps;
            this.sizeText = sizeText;
            this.status = status;

            setPadding(FluentMetrics.SPACING_L);

            // ---- 第一行：文件名 + 状态 ----
            JPanel titleRow = new JPanel(new BorderLayout(FluentMetrics.SPACING_M, 0));
            titleRow.setOpaque(false);
            nameLabel.setText(name);
            nameLabel.setFont(FluentTypography.bodyStrong());
            nameLabel.setForeground(FluentColors.text());
            titleRow.add(nameLabel, BorderLayout.CENTER);

            statusLabel.setFont(FluentTypography.caption());
            statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            titleRow.add(statusLabel, BorderLayout.EAST);

            // ---- 第二行：进度条 ----
            progressBar.setBarHeight(4);

            // ---- 第三行：详情 + 操作 ----
            JPanel detailRow = new JPanel(new BorderLayout(FluentMetrics.SPACING_M, 0));
            detailRow.setOpaque(false);
            detailLabel.setFont(FluentTypography.caption());
            detailLabel.setForeground(FluentColors.textSecondary());
            detailRow.add(detailLabel, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, FluentMetrics.SPACING_XS, 0));
            actions.setOpaque(false);
            actionButton = FluentButton.icon(FluentIcons.PAUSE, FluentButton.Style.SUBTLE);
            actionButton.tip("暂停 / 继续");
            FluentButton folder = FluentButton.icon(FluentIcons.FOLDER, FluentButton.Style.SUBTLE);
            folder.tip("打开所在文件夹");
            FluentButton open = FluentButton.icon(FluentIcons.OPEN_FILE, FluentButton.Style.SUBTLE);
            open.tip("打开文件");
            FluentButton remove = FluentButton.icon(FluentIcons.DELETE, FluentButton.Style.SUBTLE);
            remove.tip("删除任务");
            actions.add(actionButton);
            actions.add(folder);
            actions.add(open);
            actions.add(remove);
            detailRow.add(actions, BorderLayout.EAST);

            addContent(titleRow);
            addContent(Box.createVerticalStrut(FluentMetrics.SPACING_M));
            addContent(progressBar);
            addContent(Box.createVerticalStrut(FluentMetrics.SPACING_S));
            addContent(detailRow);

            refresh();
            // 主题监听由 FluentCard 注册，这里覆写 onThemeChanged 追加自身刷新
        }

        /** 更新进度并刷新所有派生文本 */
        public void setProgress(double progress, double speedMbps, String sizeText) {
            this.progress = progress;
            this.speedMbps = speedMbps;
            this.sizeText = sizeText;
            refresh();
        }

        public void setStatus(String status) {
            this.status = status;
            refresh();
        }

        private void refresh() {
            progressBar.setIndeterminate("等待中".equals(status));
            progressBar.setValue(progress);
            nameLabel.setForeground(FluentColors.text());

            Color statusColor = switch (status) {
                case "正在下载" -> FluentColors.accent();
                case "已完成" -> color(0x0F, 0x7B, 0x0F);
                case "等待中" -> FluentColors.textSecondary();
                default -> FluentColors.textSecondary();
            };
            statusLabel.setText(status);
            statusLabel.setForeground(statusColor);

            String detail = sizeText;
            if (speedMbps > 0) {
                detail += "　·　" + String.format("%.1f MB/s", speedMbps)
                        + "　·　" + String.format("%.1f%%", progress);
            }
            detailLabel.setText(detail);
            actionButton.setGlyph("正在下载".equals(status) ? FluentIcons.PAUSE : FluentIcons.PLAY);
            repaint();
        }

        @Override
        public void onThemeChanged() {
            super.onThemeChanged();
            refresh();
        }

        public FluentProgressBar progressBar() {
            return progressBar;
        }

        public FluentButton actionButton() {
            return actionButton;
        }
    }

    private static Color color(int r, int g, int b) {
        return new Color(r, g, b);
    }

    // ==================================================================
    // 演示动画：让正在下载的任务动起来
    // ==================================================================

    @Override
    public void addNotify() {
        super.addNotify();
        if (!demoAnimationEnabled) {
            return;
        }
        if (demoTimer == null) {
            demoTimer = new Timer(400, e -> tick());
        }
        demoTimer.start();
    }

    @Override
    public void removeNotify() {
        if (demoTimer != null) {
            demoTimer.stop();
        }
        super.removeNotify();
    }

    private void tick() {
        for (TaskCard card : cards) {
            if ("正在下载".equals(card.status)) {
                double next = card.progress + Math.random() * 0.6;
                if (next > 100) {
                    next = 100;
                    card.setStatus("已完成");
                    card.setProgress(100, 0, card.sizeText);
                } else {
                    card.setProgress(next, card.speedMbps * (0.9 + Math.random() * 0.2), card.sizeText);
                }
            }
        }
    }

    /** 供页面外调用（例如标题栏按钮） */
    public List<TaskCard> taskCards() {
        return cards;
    }
}
