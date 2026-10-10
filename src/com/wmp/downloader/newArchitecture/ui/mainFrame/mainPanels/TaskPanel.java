package com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels;

import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.Downloader;
import com.wmp.speed_bump.common.background.tools.resource.control.IconControl;

import javax.swing.*;
import java.awt.*;

/**
 * 任务页（主界面第一个标签页）。
 *
 * <p>与 {@code SettingsPanel} / {@code SpecialSettingsPanel} / {@code PluginParserPanel} /
 * {@code AboutPanel} 一样，本页的结构与初始化都放在自己的类与 {@code TaskPanel.form} 里，
 * {@code 下载器.form} 只保留一个空白容器 {@code downloaderPanel}，
 * 等到标签页首次被选中时再由 {@link Downloader} 通过 {@link #initTaskComponents()} 装配。</p>
 *
 * <p>任务的<strong>运行状态</strong>仍由 {@link Downloader} 持有（{@code taskList} 与其定时器），
 * 本类只负责任务页的界面：按钮栏、任务列表，以及把任务组件挂进列表。</p>
 */
public class TaskPanel {

    /**
     * 任务页的根面板，对应 {@code TaskPanel.form} 的根节点。
     * 由 {@link Downloader} 在初始化时加进标签页容器。
     */
    public JPanel taskPanel;

    public JButton createTaskButton;
    public JButton allStartButton;
    public JButton allPauseButton;

    public JPanel TaskButtonPanel;
    public JScrollPane TasksScrollPane;
    public JPanel TasksPanel;

    private final Downloader downloader;

    public TaskPanel(Downloader downloader) {
        this.downloader = downloader;
    }

    /**
     * 创建表单无法直接生成、需要自行构造的组件（{@code custom-create} 绑定的那些）。
     *
     * <p>由 Designer 生成的 {@code $$$setupUI$$$()} 在构造期回调本方法，
     * 因此这里必须先于表单把它俩建好。</p>
     */
    private void createUIComponents() {
        TasksPanel = new JPanel(new GridBagLayout());
        TasksPanel.setOpaque(false);

        TasksScrollPane = new JScrollPane(TasksPanel);
        TasksScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动
        TasksScrollPane.getViewport().setLayout(new ViewportLayout()); // 默认布局，会拉伸组件
        UITools.setScrollPaneUnOpaque(TasksScrollPane);
    }

    /**
     * 任务页的懒加载初始化：首次选中任务页时才由 {@link Downloader} 调用。
     *
     * <p>只做接线工作（字体 / 图标 / 监听器），界面结构由表单负责。
     * 跑在工作线程上，因此这里没有任何 Swing 组件树的改动。</p>
     */
    public void initTaskComponents() {
        // 任务页也有输入焦点时需要用到的默认按钮，随主题变化重算一次
        ThemeChanger.addInDynamicConverter(
                downloader::updateDefaultButton
        );

        createTaskButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allStartButton.putClientProperty("FlatLaf.style", "font: $h2.font");
        allPauseButton.putClientProperty("FlatLaf.style", "font: $h2.font");

        IconControl.INSTANCE.addInDynamicConverter(
                () -> createTaskButton.setIcon(IconControl.INSTANCE.getIcon("new", createTaskButton.getFont().getSize())),
                () -> allStartButton.setIcon(IconControl.INSTANCE.getIcon("start", allStartButton.getFont().getSize())),
                () -> allPauseButton.setIcon(IconControl.INSTANCE.getIcon("pause", allPauseButton.getFont().getSize()))
        );

        createTaskButton.addActionListener(e ->
                downloader.createDownloadTask(null));

        allStartButton.addActionListener(e -> {
            for (var task : downloader.taskList) {
                if (!task.isFinally()) task.start();
            }
        });

        allPauseButton.addActionListener(e -> {
            for (var task : downloader.taskList) {
                if (!task.isFinally()) task.stop();
            }
        });
    }

    /**
     * 把任务组件挂到任务列表上。
     *
     * <p>调用方（{@code Downloader.addDownloadTask}）跑在虚拟线程里，
     * 而这里是 Swing 组件树的改动，所以切回 EDT 执行。</p>
     */
    public void addTask(AbstractTask task) {
        if (TasksPanel == null) return;
        SwingUtilities.invokeLater(() -> {
            task.setAlignmentX(Component.LEFT_ALIGNMENT);
            TasksPanel.add(task, downloader.gbc);
            TasksPanel.revalidate();
            TasksPanel.repaint();
        });
    }

    /** 从任务列表上移除一个任务组件（同样切回 EDT） */
    public void removeTask(AbstractTask task) {
        if (TasksPanel == null) return;
        SwingUtilities.invokeLater(() -> {
            TasksPanel.remove(task);
            TasksPanel.revalidate();
            TasksPanel.repaint();
        });
    }

    /** 供 {@link Downloader#updateDefaultButton()} 取默认按钮；任务页未加载时返回 null */
    public JButton getCreateTaskButton() {
        return createTaskButton;
    }
}
