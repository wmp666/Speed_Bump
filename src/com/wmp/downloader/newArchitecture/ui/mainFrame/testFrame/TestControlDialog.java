package com.wmp.downloader.newArchitecture.ui.mainFrame.testFrame;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.TestFunctionControl;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
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
        System.err.println(enableIDList);

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

                var id = Integer.valueOf(idTable.getModel().getValueAt(selectedRow, 1).toString());

                var map = allTip.get(id);
                var model = (DefaultTableModel) infoTable.getModel();
                model.setDataVector(new Object[0][2], new String[]{"内部序列", "作用"});
                map.forEach((index, description) -> {
                    model.addRow(new Object[]{index, description});
                });


            });
        }

    }


    private void createUIComponents() {
    }
}
