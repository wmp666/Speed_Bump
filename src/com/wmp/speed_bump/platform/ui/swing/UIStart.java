package com.wmp.speed_bump.platform.ui.swing;


import com.wmp.downloader.Run;
import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.downloader.tools.devtools.StartupTrace;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.fluent.FluentUi;
import com.wmp.downloader.tools.web.TCPControl;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.List;

public class UIStart implements com.wmp.speed_bump.common.UIStart {


    private static final Logger logger = Logger.getLogger(UIStart.class);

    @Override
    public void show(List<String> argList, String linkPath) {
        //加载窗口（以及启动阶段可能出现的弹窗）在此之前就要用上系统外观，
        //否则它们会使用 Swing 默认的 Metal 外观，和主界面差别很大
        applySystemLookAndFeel();
        StartupTrace.step("切换到系统外观");

        //Fluent 界面增强（悬浮滚动条 / 进度条扫动动画 / 开关外观 / 揭示高亮）。
        //必须赶在任何界面组件创建之前装好：这样新组件的 updateUI 会直接拿到
        //ScrollBarUI / ProgressBarUI / CheckBoxUI；晚一步就会出现「新窗口是旧样式」的割裂。
        FluentUi.install();
        StartupTrace.step("安装 Fluent 界面增强");

        logger.info("开始加载");
        Downloader downloader = null;
        try {
            DataControl.load();
            StartupTrace.step("加载配置");

            ParserTaskInfo.loadParsers();
            StartupTrace.step("注册解析器");

            WebSetter.SSLControl(DataControl.get("isUseSSL", false));
            WebSetter.proxies(true);
            StartupTrace.step("初始化网络");


            ThemeChanger.easyChanger();
            StartupTrace.step("应用主题");

            downloader = new Downloader();
            StartupTrace.step("构建主窗口");

            ThemeChanger.easyChanger();
            StartupTrace.step("刷新主题后的组件");

            Thread.ofVirtual().start(() -> {
                try {
                    TCPControl.startServer();
                } catch (Exception ex) {
                    logger.error("服务端启动失败!");
                    JOptionPane.showMessageDialog(null, "服务端启动失败");
                }
            });
            StartupTrace.step("启动本地服务端（后台）");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "启动发生错误\n"+e);
            logger.error("启动发生错误", e);
            System.exit(0);
            throw new RuntimeException(e);

        }

        if (!argList.contains("-background")){
            downloader.setVisible(true);
            StartupTrace.step("显示主窗口");
        }

        if (linkPath != null) downloader.showLinkDetectedDialog(linkPath);
    }


    /**
     * 先把外观切到系统默认（Windows / macOS 原生外观）。
     *
     * <p>用户配置的主题要等 {@link DataControl#load()} 之后才知道，所以启动窗口只能用系统外观；
     * 这比 Swing 默认的 Metal 更接近最终界面，也避免了加载窗口与主界面观感割裂。
     * 读取配置后 {@link ThemeChanger#easyChanger()} 会再换成 FlatLaf 主题。</p>
     */
    private static void applySystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            //拿不到系统外观不是什么大问题，继续用默认外观即可
            logger.error("设置系统外观失败，将继续使用默认外观", e);
        }
    }
}
