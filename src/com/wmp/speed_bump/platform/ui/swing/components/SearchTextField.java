package com.wmp.speed_bump.platform.ui.swing.components;

import com.formdev.flatlaf.FlatClientProperties;
import com.wmp.speed_bump.common.ui.components.SearchHandler;
import org.jdesktop.swingx.JXSearchField;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * 搜索输入框组件。
 *
 * <p><b>当前只做外观，不做搜索。</b>触发时机、防抖、数据来源都还没定，
 * 因此这里仅提供输入框形态（占位提示、清除按钮、可选前导图标、透明背景），
 * 并预留 {@link SearchHandler} 作为将来挂载搜索行为的入口。</p>
 *
 * <p>典型的后续接入方式：监听回车/文本变化后调用 {@code searchHandler}，
 * 但具体策略留待实现搜索时再定。</p>
 */
public class SearchTextField extends JXSearchField {

    private static final long serialVersionUID = 1L;

    /** 未输入内容时显示的提示文本 */
    private String placeholderText = "搜索";

    /** 是否使用透明背景，默认开启 */
    private boolean transparentBackground = true;

    /** 搜索行为处理器：目前仅保存引用，不做任何调用 */
    private SearchHandler searchHandler;

    public SearchTextField() {
        this(16);
    }

    public SearchTextField(int columns) {
        super();
        applyFlatLafStyle();
        setTransparentBackground(true);

    }

    /** 应用 FlatLaf 提供的输入框形态；没有自定义绘制，主题切换时自动跟随 */
    private void applyFlatLafStyle() {
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholderText);
        // 有内容时显示清除按钮，避免再写一套清空逻辑
        putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
    }

    /**
     * 设置是否使用透明背景。
     *
     * <p>透明时<b>会把边框一并去掉</b>，这不是随手为之：FlatLaf 只在
     * 「边框不是 FlatBorder」且 {@code opaque=false} 时才真正跳过背景绘制
     * （见 {@code FlatTextFieldUI.paintBackground} 的前置判断），
     * 否则聚焦时仍会填一层 {@code focusedBackground}，透明就无从谈起。</p>
     *
     * @param transparent true 透明（默认）；false 恢复主题默认的输入框外观
     */
    public void setTransparentBackground(boolean transparent) {
        this.transparentBackground = transparent;
        if (transparent) {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        } else {
            setOpaque(true);
            Object border = UIManager.getBorder("TextField.border");
            setBorder(border instanceof Border b
                    ? b
                    : BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }
        repaint();
    }

    public boolean isTransparentBackground() {
        return transparentBackground;
    }

    public String getPlaceholderText() {
        return placeholderText;
    }

    public void setPlaceholderText(String placeholderText) {
        this.placeholderText = placeholderText;
        putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholderText);
    }

    /**
     * 设置输入框前端的图标（通常是放大镜）。
     * 图标资源由调用方提供，传 {@code null} 表示不显示。
     */
    public void setLeadingIcon(Icon icon) {
        putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, icon);
    }

    public SearchHandler getSearchHandler() {
        return searchHandler;
    }

    /**
     * 挂载搜索处理器。
     *
     * <p>搜索行为尚未实现，这里只保存引用——将来在合适的触发点调用它。</p>
     */
    public void setSearchHandler(SearchHandler searchHandler) {
        this.searchHandler = searchHandler;
    }
}
