package com.wmp.downloader.test;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.swingx.FlatSwingXDefaultsAddon;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import org.jdesktop.swingx.JXTitledPanel;

import javax.swing.*;
import java.awt.*;

/**
 * 测试 JXTaskPaneContainer 的演示窗口
 * 集成 FlatLaf 与 flatlaf-swingx
 */
public class TaskPaneContainerDemo {

    public static void main(String[] args) throws ClassNotFoundException {
        //System.out.println(Class.forName("org.jdesktop.swingx.painter.Painter"));
        // 1. 设置 FlatLaf 主题
        FlatLightLaf.setup();  // 也可以换成 FlatDarkLaf.setup()

        // 2. 【关键】注册 SwingX 集成插件，使 JXTaskPane 等组件样式统一
        UIManager.put("FlatLaf.addon.swingx", new FlatSwingXDefaultsAddon());
        UIManager.put("TaskPane.animate", Boolean.FALSE);

        // 3. 在 EDT 中创建 UI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JXTaskPaneContainer 测试窗口");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 500);
            frame.setLocationRelativeTo(null);

            // 创建任务面板容器
            JXTaskPaneContainer container = new JXTaskPaneContainer();
            container.setBackground(Color.GREEN);

            // ----- 任务面板 1：文件操作 -----
            JXTaskPane pane1 = new JXTaskPane();
            pane1.setTitle("文件操作");
            pane1.setIcon(UIManager.getIcon("FileView.fileIcon")); // 可选图标
            pane1.add(new JButton("新建文件"));
            pane1.add(new JButton("打开文件"));
            pane1.add(new JButton("保存文件"));
            pane1.add(new JCheckBox("自动保存"));
            pane1.setOpaque(false);
            ((JPanel)pane1.getContentPane()).setOpaque(false);

            // ----- 任务面板 2：视图设置 -----
            JXTaskPane pane2 = new JXTaskPane();
            pane2.setTitle("视图设置");
            pane2.setIcon(UIManager.getIcon("FileView.directoryIcon"));
            pane2.add(new JCheckBox("显示工具栏"));
            pane2.add(new JCheckBox("显示状态栏"));
            pane2.add(new JLabel("缩放比例："));
            pane2.add(new JSlider(JSlider.HORIZONTAL, 50, 200, 100));

            // ----- 任务面板 3：高级选项 -----
            JXTaskPane pane3 = new JXTaskPane();
            pane3.setTitle("高级选项");
            pane3.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
            JPanel advancedPanel = new JPanel(new GridLayout(2, 2, 5, 5));
            advancedPanel.add(new JLabel("缓存大小："));
            advancedPanel.add(new JTextField("512 MB"));
            advancedPanel.add(new JLabel("日志级别："));
            advancedPanel.add(new JComboBox<>(new String[]{"DEBUG", "INFO", "WARN", "ERROR"}));
            pane3.add(advancedPanel);

            // 将所有任务面板添加到容器
            container.add(pane1);
            container.add(pane2);
            var comp = new JXTitledPanel("111", pane3);
            container.add(comp);

            // 将容器放入滚动窗格（内容过多时可滚动）
            JScrollPane scrollPane = new JScrollPane(container);
            scrollPane.setBorder(null);  // 去掉边框，更干净

            // 将滚动窗格添加到窗口中央
            frame.add(scrollPane, BorderLayout.CENTER);

            // 添加状态栏（可选）
            JLabel statusBar = new JLabel("就绪 | 共 " + container.getComponentCount() + " 个任务面板");
            statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            frame.add(statusBar, BorderLayout.SOUTH);

            frame.setVisible(true);
        });
    }
}
