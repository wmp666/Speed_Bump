package com.wmp.speed_bump.platform.ui.swing.window;

/**
 * 设置页与欢迎页外壳之间的约定。
 *
 * <p>各设置页只负责「摆控件 + 把取值写进配置」，至于「重装外观 / 重刷全部文案 /
 * 页脚提示 / 是不是正在重建界面」这些<b>跨页</b>的事情，交回外壳统一处理——
 * 页与页之间因此不需要互相认识。</p>
 *
 * <p>页面在 {@code bind(host)} 里拿到外壳；那个时点外壳已经构造完毕，可以安全回调。</p>
 */
interface WelcomePageHost {

    /**
     * 重新套用当前外观。
     *
     * <p>主题、强调色、字体、字体大小、组件弧度这五项都是「写进 {@code UIManager}
     * 或 LAF 全局默认值」才生效的，改完必须重装外观才看得到。</p>
     */
    void reapplyTheme();

    /** 重刷所有页面的文案（含标签栏标题）——切换界面语言后调用 */
    void retranslateAll();

    /**
     * 在页脚显示一条提示。
     *
     * @param text    提示文字
     * @param isError true 用错误色，false 用次要文字色
     */
    void showHint(String text, boolean isError);

    /**
     * 是否正在重建界面。
     *
     * <p>{@code updateUI} 会顺带调整控件状态并可能补发事件，监听器要据此判断
     * 「这是用户操作的」还是「刷新捎带的」，见 {@code WelcomePage#refreshWindow()}。</p>
     */
    boolean isRebuilding();
}
