package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.FunctionDialog;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ToastMsgInfoPanel {
    private JPanel mainPanel;
    private JButton closeButton;
    private JTextArea msgTextArea;
    private JLabel iconLabel;
    private JLabel dateTimeLabel;

    private final long dateMs;
    private final DynamicConverterTask[] dynamicConverterTask;

    public ToastMsgInfoPanel(Date date, String msg, String iconKey) {
        dateMs = date.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yy.MM.dd HH:mm:ss");
        dateTimeLabel.setText(dateFormat.format(date));

        iconLabel.putClientProperty("FlatLaf.style", "font: $h1.font");
        iconLabel.setText("");

        dynamicConverterTask = ThemeChanger.addInDynamicConverter(
                () -> iconLabel.setIcon(IconControl.getIcon(iconKey, iconLabel.getFont().getSize())),
                () -> {
                    Color base = UIManager.getColor("Panel.background");

                    Color adjusted = DataControl.get("theme_type", "light").equals("dark")
                            ? ColorFunctions.lighten(base, 0.1f)
                            : ColorFunctions.darken(base, 0.1f);
                    mainPanel.setBackground(adjusted);
                }
        );
        msgTextArea.setOpaque(false);
        msgTextArea.setText(msg);
        msgTextArea.setLineWrap(true);
        msgTextArea.setRows(5);
        msgTextArea.setColumns(20);

        closeButton.addActionListener(e -> {
            close();
        });
    }

    public void close(){
        ThemeChanger.removeDynamicConverter(dynamicConverterTask);
        //从持久化数据中删除这则通知
        DataControl.deleteMsgInfo(dateMs);
        //从界面中移除这则通知
        Container parent = mainPanel.getParent();
        if (parent != null){
            parent.remove(mainPanel);
            parent.revalidate();
            parent.repaint();
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }


}
