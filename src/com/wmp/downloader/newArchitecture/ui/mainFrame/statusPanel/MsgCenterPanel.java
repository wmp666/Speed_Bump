package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

public class MsgCenterPanel extends JPanel {

    public MsgCenterPanel() {
        //从上到下排列，子组件水平方向填充
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    public void loadMsg(){
        this.removeAll();
        for (var o : DataControl.getMsgInfo()) {
            if (o instanceof JSONObject jsonObject){
                var date = new Date(jsonObject.getLongValue("date"));
                var msg = jsonObject.getString("msg");
                //-1-Default 0-Info 1-Warn 2-Error
                var infoStyle = jsonObject.getIntValue("style", -1);
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

                this.add(panel);
                //this.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        }
    }
}
