package com.wmp.downloader.tools.ui.fluent;

import javax.swing.JCheckBox;

/**
 * Fluent 开关组件（{@link JCheckBox} 的直接替代品）。
 *
 * <p>与普通 {@code JCheckBox} 完全同接口：{@code isSelected()}、
 * {@code addItemListener}、{@code setText} 行为一致，因此把某个字段的类型从
 * {@code JCheckBox} 换成它不需要改任何调用代码。</p>
 *
 * <p><b>什么时候需要它：</b>全局安装（{@link FluentUi#install()}）已经把
 * {@code CheckBoxUI} 换成了 {@link FluentSwitchUI}，所有复选框都会呈现开关外观。
 * 这个类用于两种场景：一是全局安装被关闭时仍想要单个开关；
 * 二是想在代码里显式表达「这里是一个开关，不是勾选框」。</p>
 */
public class FluentToggleSwitch extends JCheckBox {

    public FluentToggleSwitch() {
        super();
    }

    public FluentToggleSwitch(String text) {
        super(text);
    }

    public FluentToggleSwitch(String text, boolean selected) {
        super(text, selected);
    }

    /**
     * 始终使用开关外观。
     *
     * <p>覆写 {@code updateUI} 而不是在构造器里 {@code setUI}：主题切换时
     * {@code updateComponentTreeUI} 会重新调用 {@code updateUI}，
     * 在构造器里设置会被那一步覆盖掉。</p>
     */
    @Override
    public void updateUI() {
        setUI(new FluentSwitchUI());
    }

    /** 便捷：把本开关的标签设为 HTML 文本（{@code FluentSwitchUI} 的文本绘制支持 HTML） */
    public FluentToggleSwitch htmlText(String html) {
        setText(html);
        return this;
    }
}
