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

    public static final String VERSION = "0.2.2";

    static void main(String[] args) {
        DataControl.load();

        WebSetter.SSLControl(DataControl.get("isUseSSL", false));
        WebSetter.proxies(true);


        ThemeChanger.easyChanger();

        var downloader = new Downloader();

        ThemeChanger.easyChanger();

        //获取上次的版本
        var version = DataControl.get("last_version", "0.0.0");
        logger.info("Last version: " + version);
        if (!version.equals(DataControl.get("version", "0.0.0"))) {
            //弹出更新日志
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea textArea = new JTextArea();
            textArea.setText("""
                    0.2.2
                    1.增加新的设置项，个性化开放度更高
                    2.整顿部分UI
                    3.提升性能
                    
                    0.2.1
                    1.更新创建链接功能，更加智能
                    2.添加ED2K链接下载（依赖gopeed）
                    3.支持将下载链接强制挂载到gopeed
                    4.新增下载成功的系统提示
                    
                    0.2.0
                    1.使修改种子、磁力文件/文件夹名称功能可用
                    
                    0.1.9
                    1.增加种子、磁力下载
                    2.优化功能性弹窗（内嵌式），点击四周会关闭的问题
                    3.新增拖入文本域添加文件路径
                    4.修改创建任务的解析链接功能的异常抛出条件
                    
                    0.1.8
                    1.考虑到大小问题，这个版本删除了JavaCV
                    
                    0.1.7
                    1. 更新功能性弹窗样式
                    2. 更新通知样式
                    
                    0.1.6
                    1. 提升功能性弹窗功能
                    2. 增加新的文本到语言文件
                    
                    0.0.1 > 0.1.5
                    1.更新内容遗失 :(
                    """);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setRows(15);
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
