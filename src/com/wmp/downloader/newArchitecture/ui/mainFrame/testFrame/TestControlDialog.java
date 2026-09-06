package com.wmp.downloader.newArchitecture.ui.mainFrame.testFrame;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.TestFunctionControl;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class TestControlDialog{
    public JPanel contentPane;
    private JTable idTable;
    private JTable infoTable;

    public TestControlDialog() {

        // 遇到 ESCAPE 时调用 onCancel()
        contentPane.registerKeyboardAction(e -> {}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void onOK() {
        // 在此处添加您的代码
        var idTableModel = (DefaultTableModel) idTable.getModel();
        var enableIdList = new ArrayList<Integer>();
        for (var i = 0; i < idTableModel.getRowCount(); i++) {
            var aBoolean = Boolean.parseBoolean(idTableModel.getValueAt(i, 0).toString());
            if (aBoolean) enableIdList.add(Integer.valueOf(idTableModel.getValueAt(i,1).toString()));
        }
        System.err.println(enableIdList);
        TestFunctionControl.saveEnableList(enableIdList);


    }


    public static TestControlDialog getPanel() {
        return new TestControlDialog();
    }

    private HashMap<Integer, HashMap<Short, String>> allTip = new HashMap<>();

    public void load() {
        //开始加载数据
        allTip = TestFunctionControl.getAllTip();
        var enableIDList = TestFunctionControl.enableIDList();

        // infoTable 仅作展示，任何单元格都不可编辑
        infoTable.setModel(new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });

        //id
        {
            //0 - boolean 1 - id
            var objects = new Object[allTip.size()][2];

            var keyIterator = allTip.keySet().iterator();
            int count = 0;
            while (keyIterator.hasNext()) {
                var id = keyIterator.next();
                objects[count][0] = enableIDList.contains(id);
                objects[count][1] = id;

                count++;
            }

            var defaultTableModel = new DefaultTableModel(objects, new String[]{"可用状态", "编号"}) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 0) {
                        return Boolean.class;
                    }
                    return super.getColumnClass(columnIndex);
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    // 只允许点选“可用状态”复选框，禁止直接改写“编号”等文字
                    return columnIndex == 0;
                }
            };
            idTable.setModel(defaultTableModel);
            idTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            idTable.getSelectionModel().addListSelectionListener(e1 -> {
                // 忽略正在调整中的事件（例如用户拖动鼠标选择范围时）
                if (e1.getValueIsAdjusting()) {
                    return;
                }

                // 获取当前选中的行索引
                int selectedRow = idTable.getSelectedRow();
                if (selectedRow < 0) {
                    return;
                }

                var id = Integer.valueOf(idTable.getModel().getValueAt(selectedRow, 1).toString());

                var map = allTip.get(id);
                var model = (DefaultTableModel) infoTable.getModel();
                model.setDataVector(new Object[0][2], new String[]{"内部序列", "作用"});
                map.forEach((index, description) -> {
                    model.addRow(new Object[]{index, description});
                });
                fitColumnWidths(infoTable, true);
            });
        }

        //先让两侧表格按内容自适应，再调整 JSplitPane 的分割策略
        fitColumnWidths(idTable, false);
        tuneSplitPane();
    }

    /**
     * @param allowHorizontalScroll 内容超宽时是否允许出现横向滚动条：
     *                              - false：表格始终自动铺满所在视口（适合内容较短的 idTable，宽度富余时平均分摊给各列）；
     *                              - true：当内容总宽超过可视区时切到横向滚动，而不是把右侧内容裁剪/挤出可视区（适合 infoTable）。
     * 列的最小宽度仍按“表头/内容”实际需要计算，保证不会被压成截断。
     */
    private void fitColumnWidths(JTable table, boolean allowHorizontalScroll) {
        if (table == null || table.getColumnCount() == 0) return;
        if (allowHorizontalScroll) {
            // 关闭“自动填满视口宽度”，让 JTable 保持内容所需宽度，
            // 超宽时其外层 JScrollPane 会自动出现横向滚动条以查看完整内容
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
        table.setFillsViewportHeight(true);
        final int PADDING = 24;
        var header = table.getTableHeader();
        var headerRenderer = header.getDefaultRenderer();
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn column = table.getColumnModel().getColumn(col);
            int width = 0;
            // 表头宽度
            var headerComponent = headerRenderer.getTableCellRendererComponent(
                    table, column.getHeaderValue(), false, false, -1, col);
            width = Math.max(width, headerComponent.getPreferredSize().width);
            // 单元格内容宽度（逐行取最大）
            for (int row = 0; row < table.getRowCount(); row++) {
                var cell = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
                width = Math.max(width, cell.getPreferredSize().width);
            }
            width += PADDING;
            column.setMinWidth(width);
            column.setPreferredWidth(width);
            column.setWidth(width);
        }
        // 内容较长的一侧，确保横向滚动条按需出现
        if (allowHorizontalScroll && table.getParent() instanceof JViewport viewport
                && viewport.getParent() instanceof JScrollPane scrollPane) {
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        table.doLayout();
    }

    /**
     * 修正 contentPane 中 JSplitPane 的分割策略：
     * - 连续布局，拖拽时实时刷新两侧；
     * - 左右两侧按 0.5 的 resizeWeight 分配窗口 resize / 拖拽带来的空间，避免某一侧被无限压缩；
     * - 首次真正显示并拿到真实宽度后，把分割线放到中间，保证两侧（尤其 idTable 侧）有充足宽度铺满文字。
     */
    private void tuneSplitPane() {
        var split = findDescendant(contentPane, JSplitPane.class);
        if (split == null) return;
        split.setContinuousLayout(true);
        split.setResizeWeight(0.5);
        // 保证分割后两侧都不会被压缩成窄缝
        var left = split.getLeftComponent();
        var right = split.getRightComponent();
        if (left instanceof Component c) {
            var s = c.getMinimumSize();
            c.setMinimumSize(new Dimension(Math.max(s.width, 150), Math.max(s.height, 80)));
        }
        if (right instanceof Component c) {
            var s = c.getMinimumSize();
            c.setMinimumSize(new Dimension(Math.max(s.width, 160), Math.max(s.height, 80)));
        }
        // 首次可见时设置分割比例（此时才有真实尺寸），只执行一次，之后保留用户拖动结果
        split.addHierarchyListener(new HierarchyListener() {
            private boolean dividerApplied = false;

            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if (dividerApplied) return;
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        && split.isShowing() && split.getWidth() > 0) {
                    dividerApplied = true;
                    SwingUtilities.invokeLater(() -> {
                        if (split.getWidth() > 0) split.setDividerLocation(0.5);
                    });
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> T findDescendant(Component root, Class<T> type) {
        if (root == null) return null;
        if (type.isInstance(root)) return (T) root;
        if (root instanceof Container container) {
            for (var child : container.getComponents()) {
                var found = findDescendant(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void createUIComponents() {
    }
}
