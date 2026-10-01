package com.wmp.speed_bump.common.ui;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tool.Creator;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;
import com.wmp.speed_bump.platform.PlatformClassControl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 初次使用欢迎页的抽象端。
 *
 * <h3>什么时候会出现</h3>
 * <p>满足下面任意一条时，会在<b>主界面构建之前</b>先显示欢迎页：</p>
 * <ul>
 *   <li>配置 {@value #KEY_IS_SHOW_WELCOME} 不为 {@code false}。新用户的配置文件里
 *       根本没有这个键，读出来的就是缺省值 {@code true}，于是「装完还没设置过 = 新用户」；</li>
 *   <li>启动参数里带 {@value #ARG_SHOW_WELCOME}（强制再看一次）。</li>
 * </ul>
 * <p>欢迎页关闭后会把 {@value #KEY_IS_SHOW_WELCOME} 写成 {@code false}，
 * 因此默认只出现一次；想再看一次加启动参数即可。</p>
 *
 * <h3>为什么判定逻辑放在这里</h3>
 * <p>「要不要显示」是与界面实现无关的策略，放在抽象端可以让各个平台实现共用同一套判断，
 * 也便于单独验证。界面相关的部分（{@link #showPage()}）由
 * {@code com.wmp.speed_bump.platform.ui.<平台>.window.WelcomePage} 实现——
 * 与 {@link PreLoadDialog} 是同一套「声明在 common、实现在 platform」的写法。</p>
 */
public interface WelcomePage {

    /**
     * 是否还需要展示欢迎页。
     *
     * <p>键不存在时按 {@code true} 处理：初次使用的用户根本还没写过这个键。
     * 欢迎页关闭时才会把它写成 {@code false}。</p>
     */
    String KEY_IS_SHOW_WELCOME = "is_show_welcome";

    /** 强制展示欢迎页的启动参数（与上面的配置键是「或」的关系）。 */
    String ARG_SHOW_WELCOME = "-showWelcome";

    Creator<WelcomePage> INSTANCE_CREATOR = PlatformClassControl.getCreator(
            "com.wmp.speed_bump.platform.ui.%s.window.WelcomePage", PlatformClassControl.Type.UI);

    /**
     * 本次启动是否需要展示欢迎页。
     *
     * @param argList 启动参数，允许为 {@code null}
     * @return 需要展示返回 {@code true}
     */
    static boolean shouldShow(List<String> argList) {
        if (argList != null && argList.contains(ARG_SHOW_WELCOME)) {
            return true;
        }
        return Boolean.TRUE.equals(DataControl.get(KEY_IS_SHOW_WELCOME, Boolean.TRUE));
    }

    /**
     * 按需展示欢迎页：「判定 → 创建 → 显示」一整套，且不把异常抛给启动流程。
     *
     * <p>欢迎页只是锦上添花，任何失败（类加载不到、界面异常、用户环境问题）
     * 都不应该阻止应用启动，因此这里兜住全部 {@link Throwable}，只记日志。</p>
     *
     * <p>调用方负责在界面线程上调用：本方法会创建并显示 Swing / FX 窗口。</p>
     *
     * @param argList 启动参数，允许为 {@code null}
     * @return 真的显示了欢迎页返回 {@code true}；不需要显示、或显示失败时返回 {@code false}
     */
    static boolean showIfNeeded(List<String> argList) {
        if (!shouldShow(argList)) {
            return false;
        }
        try {
            INSTANCE_CREATOR.create().showPage();
            return true;
        } catch (Throwable t) {
            Logger.getLogger(WelcomePage.class).error("欢迎页显示失败，已跳过", t);
            return false;
        }
    }

    /**
     * 模态展示欢迎页，并在关闭后落盘用户的选择。
     *
     * <p>方法返回时欢迎页已经关闭，用户的选择也已经生效（写入配置）。</p>
     */
    void showPage();

    /** 欢迎页的各设置页，顺序即标签栏顺序 */
    ArrayList<WelcomeSettingsPage> pages = new ArrayList<>();

    /**
     * 添加一个欢迎设置页面。
     *
     * <p>{@code pages} 是接口上的静态列表（接口字段只能是 static），一个进程内只会有一份。
     * 所以这里按 {@code translateKey} <b>替换</b>而不是追加：否则同一页被登记两次
     * （重新打开欢迎页、或者测试里连建两次）就会长出重复的标签页。</p>
     *
     * @param translateKey 标签栏标题的翻译键
     * @param page         该页的内容，面板由 {@link MultiplePanel#getPanel()} 取出
     */
    public default void addPage(String translateKey, MultiplePanel page) {
        pages.removeIf(existing -> existing.translateKey() != null
                && existing.translateKey().equals(translateKey));
        pages.add(new WelcomeSettingsPage(translateKey, page));
    }

    public record WelcomeSettingsPage(String translateKey, MultiplePanel page) {
    }
}
