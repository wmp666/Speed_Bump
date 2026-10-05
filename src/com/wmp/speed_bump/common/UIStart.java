package com.wmp.speed_bump.common;

import com.wmp.speed_bump.platform.PlatformClassControl;

import java.util.List;

public interface UIStart {
    UIStart INSTANCE =
            (UIStart) PlatformClassControl.getCreator("com.wmp.speed_bump.platform.ui.%s.UIStart", PlatformClassControl.Type.UI).create();

    /**
     * 在任何窗口（含 {@code PreLoadDialog} 这类启动早期窗口）创建之前完成界面准备：
     * 切换系统外观、安装界面增强（Fluent 组件代理 + 忙碌标签加载动画）。
     *
     * <p><b>为什么必须早于早期窗口的构造：</b>加载窗的 {@code PreloadDialog.form} 里有一个
     * {@code JXBusyLabel}，而 Swing 组件的 UI 是在<b>构造时</b>就从 {@code UIManager} 取定的
     * （{@code JLabel} 的构造器会调用 {@code updateUI()}）。等窗口建好再安装，
     * 加载动画就还是 SwingX 默认的旋转圆点，窗口也已经按 Metal 外观 {@code pack()} 过一次了。</p>
     *
     * <p>实现必须是幂等的：{@link #show} 内部还会兜底调用一次，
     * 以便「只调 show」的调用方也不会缺了外观与增强。</p>
     */
    void prepareUi();

    void show(List<String> argList, String linkPath);
}
