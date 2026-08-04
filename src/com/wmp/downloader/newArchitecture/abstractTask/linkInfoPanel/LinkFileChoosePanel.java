package com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel;

import com.wmp.downloader.tools.download.URLDownloadTool;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;

public class LinkFileChoosePanel extends JPanel {

    private String[] fileNames;
    private long[] fileSizes;
    private String[] fileTypes;

    private JButton selectionAllbutton;
    private JButton selectionNoneButton;
    private JButton selectionReverseButton;
    private JTable FilesTable;
    private JPanel mainPanel;

    public LinkFileChoosePanel(String[] fileNames, long[] fileSizes, String[] fileTypes, boolean[] chooseStatus) {
        this.setLayout(new BorderLayout());
        this.add(mainPanel, BorderLayout.CENTER);

        this.fileNames = fileNames;
        this.fileSizes = fileSizes;
        this.fileTypes = fileTypes;

        DefaultTableModel model = (DefaultTableModel) FilesTable.getModel();
        for (var i = 0; i < fileNames.length; i++) {
            boolean selected = i < chooseStatus.length && chooseStatus[i];
            String sizeStr = i < fileSizes.length ? URLDownloadTool.DownloadProgress.formatSize(fileSizes[i]) : "未知";
            String type = i < fileTypes.length ? fileTypes[i] : "";
            model.addRow(new Object[]{selected, fileNames[i], sizeStr, type});
        }

        selectionAllbutton.addActionListener(e -> {
            TableModel m = FilesTable.getModel();
            for (int i = 0; i < m.getRowCount(); i++) {
                m.setValueAt(true, i, 0);
            }
        });
        selectionNoneButton.addActionListener(e -> {
            TableModel m = FilesTable.getModel();
            for (int i = 0; i < m.getRowCount(); i++) {
                m.setValueAt(false, i, 0);
            }
        });
        selectionReverseButton.addActionListener(e -> {
            TableModel m = FilesTable.getModel();
            for (int i = 0; i < m.getRowCount(); i++) {
                m.setValueAt(!(boolean) m.getValueAt(i, 0), i, 0);
            }
        });
    }

    public long getSelectedFilesSize() {
        long size = 0;
        TableModel model = FilesTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((boolean) model.getValueAt(i, 0)) {
                size += fileSizes[i];
            }
        }
        return size;
    }

    public int[] getSelectedFilesIndex() {
        ArrayList<Integer> indexList = new ArrayList<>();
        TableModel model = FilesTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if ((boolean) model.getValueAt(i, 0)) {
                indexList.add(i);
            }
        }
        return indexList.stream().mapToInt(Integer::intValue).toArray();
    }

    private void createUIComponents() {
        DefaultTableModel model = new DefaultTableModel(new Object[][]{}, new Object[]{"选择", "文件名", "文件大小", "文件类型"}) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        FilesTable = new JTable(model);
    }

    public boolean[] getFileSelectionStatus() {
        TableModel model = FilesTable.getModel();
        boolean[] selectionStatus = new boolean[model.getRowCount()];
        for (int i = 0; i < model.getRowCount(); i++) {
            selectionStatus[i] = (boolean) model.getValueAt(i, 0);
        }
        return selectionStatus;
    }
}
