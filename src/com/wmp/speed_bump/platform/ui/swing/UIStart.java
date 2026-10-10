package com.wmp.speed_bump.platform.ui.swing;


import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.tools.WebSetter;
import com.wmp.speed_bump.common.background.tools.devtools.StartupTrace;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.FluentUi;
import com.wmp.speed_bump.platform.ui.swing.tools.swingx.LoadingBusyLabelUI;
import com.wmp.downloader.tools.web.TCPControl;
import com.wmp.downloader.ui.Downloader;
import com.wmp.speed_bump.common.ui.WelcomePage;
import com.wmp.speed_bump.common.background.tools.SBLogger;

import javax.swing.*;
import java.util.List;

public class UIStart implements com.wmp.speed_bump.common.UIStart {


    private static final SBLogger logger = SBLogger.getLogger(UIStart.class);

    /** 早期界面准备只做一次（{@link #prepareUi} 会被 {@code Run} 与 {@link #show} 各调一次）。 */
    private static boolean prepared;

    /**
     * {@inheritDoc}
     *
     * <p>顺序不能颠倒：先切外观，再装界面增强。因为 {@code FluentUi} 里
     * 「揭示高亮按钮」是否安装要看当前外观是不是 FlatLaf——
     * 启动阶段这里还是系统外观（Windows），那个条件不成立，
     * 等 {@code ThemeChanger.easyChanger()} 换成 FlatLaf 后由主题回调补装。</p>
     */
    @Override
    public void prepareUi() {
        if (prepared) {
            return;
        }
        prepared = true;

        //加载窗口（以及启动阶段可能出现的弹窗）在此之前就要用上系统外观，
        //否则它们会使用 Swing 默认的 Metal 外观，和主界面差别很大
        applySystemLookAndFeel();
        StartupTrace.step("切换到系统外观");

        //Fluent 界面增强（悬浮滚动条 / 进度条扫动动画 / 开关外观 / 揭示高亮）。
        //必须赶在任何界面组件创建之前装好：这样新组件的 updateUI 会直接拿到
        //ScrollBarUI / ProgressBarUI / CheckBoxUI；晚一步就会出现「新窗口是旧样式」的割裂。
        FluentUi.install();
        //加载窗里的 JXBusyLabel：把 SwingX 默认的旋转圆点换成滚动横杠加载图标。
        //同样必须早于加载窗的构造——Swing 组件的 UI 在构造时就取定了。
        LoadingBusyLabelUI.install();
        StartupTrace.step("安装 Fluent 界面增强");
    }

    @Override
    public void show(List<String> argList, String linkPath) {
        //早期窗口之前 {@code Run} 已经调用过；这里兜底一次，
        //让「只调 show」的调用方也不会缺了系统外观与界面增强。
        //prepareUi() 是幂等的，重复调用不会重复记启动耗时。
        prepareUi();

        logger.info("开始加载");
        Downloader downloader = null;
        //后台启动（开机自启）只留在托盘里，不该弹任何界面
        boolean showUi = !argList.contains("-background");
        try {

            ThemeChanger.easyChanger();
            StartupTrace.step("应用主题");

            //首次使用（或带 -showWelcome）时先展示欢迎页。
            //必须赶在主窗口构建之前：Downloader 在构造时就会读取主题色、组件弧度、背景等配置，
            //在这里改完，主界面第一次画出来就是用户选的样子。
            if (showUi && showWelcomePage(argList)) {
                StartupTrace.step("初次使用欢迎页");
            }

            DataControl.load();
            StartupTrace.step("加载配置");

            ParserTaskInfo.loadParsers();
            StartupTrace.step("注册解析器");

            WebSetter.SSLControl(DataControl.get("isUseSSL", false));
            WebSetter.proxies(true);
            StartupTrace.step("初始化网络");

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

        if (showUi){
            downloader.setVisible(true);
            StartupTrace.step("显示主窗口");
        }

        if (linkPath != null) downloader.showLinkDetectedDialog(linkPath);
    }


    /**
     * 按需展示初次使用欢迎页。
     *
     * <p>「要不要显示」由 {@link WelcomePage#shouldShow(List)} 判定（配置键
     * {@code is_show_welcome}，或启动参数 {@code -showWelcome}）；
     * 这里只负责把它放到界面线程上——{@code Run.main} 是主线程，
     * 而欢迎页会创建并显示 Swing 窗口，必须在 EDT 上构造。</p>
     *
     * <p>欢迎页本身已经兜住全部异常，这里再兜一层是为了「界面线程切换失败」
     * 这类问题也不至于带着整个启动流程一起挂掉。</p>
     *
     * @return 真的显示了欢迎页返回 {@code true}（供启动耗时日志区分这一步有没有发生）
     */
    private static boolean showWelcomePage(List<String> argList) {
        //判定必须在显示之前做：欢迎页关闭时会把 is_show_welcome 写成 false，
        //显示之后再问就永远是「不需要」了。
        if (!WelcomePage.shouldShow(argList)) {
            return false;
        }
        Runnable show = () -> WelcomePage.showIfNeeded(argList);
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                show.run();
            } else {
                SwingUtilities.invokeAndWait(show);
            }
        } catch (Throwable t) {
            logger.error("欢迎页显示失败，已跳过", t);
            return false;
        }
        return true;
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
