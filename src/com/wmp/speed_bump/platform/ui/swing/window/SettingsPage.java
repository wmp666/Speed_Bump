package com.wmp.speed_bump.platform.ui.swing.window;

/**
 * 一个设置页。
 *
 * <p>内容面板本身由 {@code MultiplePanel#getPanel()} 提供，本接口只声明
 * 「语言变了要能自己重刷页内文案」这一件事——外壳不认识具体的页面类，
 * 只按这个接口逐个通知。</p>
 */
interface SettingsPage {

    /** 按当前语言重刷本页所有文案 */
    void retranslate();

    /**
     * 释放本页注册到全局列表里的动态任务（窗口销毁时由外壳调用）。
     *
     * <p>{@code IconControl} / {@code ThemeChanger} 的任务列表是强引用的，
     * 页面用完不摘掉就会一直留在里面。</p>
     */
    default void dispose() {
    }
}
