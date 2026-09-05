package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTitledPanel;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MsgCenterPanel extends JPanel {

    public MsgCenterPanel() {
        //从上到下排列，子组件水平方向填充
        this.setLayout(new GridBagLayout());
    }



    public void loadMsg(){
        this.removeAll();

        GridBagConstraints gbc = createConstraints();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yy.MM.dd");
        String lastDate = "";
        JXTaskPane lastTaskPane = new JXTaskPane();

        for (var o : DataControl.getMsgInfo()) {
            if (o instanceof JSONObject jsonObject){
                var date = new Date(jsonObject.getLongValue("date"));
                var msg = jsonObject.getString("msg");
                //-1-Default 0-Info 1-Warn 2-Error
                var infoStyle = jsonObject.getIntValue("style", -1);

                var newDate = dateFormat.format(date);
                if (!newDate.equals(lastDate)) {
                    lastDate = newDate;
                    lastTaskPane = new JXTaskPane(lastDate);
                    lastTaskPane.setAnimated(false);
                    //同一天，不折叠
                    lastTaskPane.setCollapsed(!dateFormat.format(new Date()).equals(lastDate));

                    gbc.gridy += 1;
                    this.add(lastTaskPane, gbc);
                }



                var panel = new ToastMsgInfoPanel(date, msg,
                        switch (infoStyle){
                            case 0 -> "info";
                            case 1 -> "warn";
                            case 2 -> "error";
                            default -> "null";
                        }).getMainPanel();
                //水平上填充
                panel.setAlignmentX(Component.LEFT_ALIGNMENT);
                //垂直上设置最小高度和最大高度
                panel.setMinimumSize(new Dimension(0, (int) panel.getPreferredSize().getHeight()));
                panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, (int) panel.getPreferredSize().getHeight()));

                lastTaskPane.add(panel);
                //this.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }


    }

    // 创建并返回配置好的约束
    private static GridBagConstraints createConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 10, 5, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        return gbc;
    }
    }
