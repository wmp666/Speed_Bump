package com.wmp.downloader;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.FunctionDialog;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;

public class Run {
    private static final Logger logger = Logger.getLogger(Run.class);

    static void main(String[] args) {
        DataControl.load();

        WebSetter.SSLControl(DataControl.get("isUseSSL", false));

        ThemeChanger.easyChanger();

        var downloader = new Downloader();

        ThemeChanger.easyChanger();

        //获取上次的版本
        var version = DataControl.get("last_version", "0.0.0");
        logger.info("Last version: " + version);
        if (!version.equals(DataControl.get("version", "0.0.0"))){
            //弹出更新日志
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea textArea = new JTextArea();
            textArea.setText("""
                    0.1.7
                    1. 更新功能性弹窗样式
                    
                    0.1.6
                    1. 提升功能性弹窗功能
                    2. 增加新的文本到语言文件
                    
                    0.0.1 > 0.1.5
                    1.更新内容遗失 :(
                    """);
            panel.add(textArea, BorderLayout.CENTER);

            FunctionDialog.showDialog(null, "更新日志 " + version + " > " + DataControl.get("version", "0.0.0"),
                    panel, null,
                    FunctionDialog.DEFAULT_BUTTONS, 0,
                    null, FunctionDialog.NORTH_DIRECTION_CENTER,
                    false, true);


            DataControl.putAndSave("last_version", DataControl.get("version", "0.0.0"));
        }

        downloader.setVisible(true);


    }
}
