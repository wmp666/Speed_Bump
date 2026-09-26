package com.wmp.speedbump.fluent.theme;

/**
 * Fluent Design 的尺寸、间距与圆角令牌。
 *
 * <p>数值对齐 WinUI 3 的 Fluent 2 规范：控件高度 32、圆角 4/8、控件内边距 11/5 等。</p>
 */
public final class FluentMetrics {

    private FluentMetrics() {
    }

    // ---- 圆角 ----
    /** 小圆角：按钮、文本框、开关轨道 */
    public static final int RADIUS_SMALL = 4;
    /** 中圆角：卡片、对话框 */
    public static final int RADIUS_MEDIUM = 8;
    /** 大圆角：浮出层、Flyout */
    public static final int RADIUS_LARGE = 12;
    /** 控件内的迷你圆角（选中指示条） */
    public static final int RADIUS_PILL = 99;

    // ---- 控件尺寸 ----
    /** 标准控件高度 */
    public static final int CONTROL_HEIGHT = 32;
    /** 紧凑控件高度（工具栏按钮） */
    public static final int CONTROL_HEIGHT_SMALL = 26;
    /** 大控件高度（主行动按钮） */
    public static final int CONTROL_HEIGHT_LARGE = 40;

    /** 文本框内部左右留白 */
    public static final int TEXT_PADDING_X = 11;
    /** 文本框内部上下留白 */
    public static final int TEXT_PADDING_Y = 5;

    /** 开关：轨道宽 / 高 */
    public static final int SWITCH_WIDTH = 40;
    public static final int SWITCH_HEIGHT = 20;
    /** 开关：滑块外径与内径 */
    public static final int SWITCH_KNOB = 12;
    public static final int SWITCH_KNOB_INNER = 6;

    /** 复选框边长 */
    public static final int CHECKBOX_SIZE = 20;
    /** 单选框直径 */
    public static final int RADIO_SIZE = 20;

    /** 标题栏高度 */
    public static final int TITLE_BAR_HEIGHT = 32;
    /** 标题栏按钮宽高 */
    public static final int CAPTION_BUTTON_WIDTH = 46;
    public static final int CAPTION_BUTTON_HEIGHT = 32;

    /** 导航栏展开宽度 */
    public static final int NAV_WIDTH_EXPANDED = 280;
    /** 导航栏折叠宽度 */
    public static final int NAV_WIDTH_COMPACT = 48;
    /** 导航项高度 */
    public static final int NAV_ITEM_HEIGHT = 36;
    /** 选中指示条宽高 */
    public static final int NAV_INDICATOR_WIDTH = 3;
    public static final int NAV_INDICATOR_HEIGHT = 16;

    // ---- 间距 ----
    public static final int SPACING_XXS = 2;
    public static final int SPACING_XS = 4;
    public static final int SPACING_S = 8;
    public static final int SPACING_M = 12;
    public static final int SPACING_L = 16;
    public static final int SPACING_XL = 24;
    public static final int SPACING_XXL = 36;

    /** 页面内容左右留白 */
    public static final int PAGE_PADDING = 36;
    /** 页面内容最大宽度（超宽屏下避免正文被拉成长条） */
    public static final int PAGE_MAX_WIDTH = 1080;

    /** 滚动条厚度 */
    public static final int SCROLLBAR_THICKNESS = 12;
    /** 滚动条滑块最小长度 */
    public static final int SCROLLBAR_MIN_THUMB = 24;

    /** 边框线宽 */
    public static final float BORDER = 1f;
    /** 焦点环宽度（Fluent 为控件外的 2px 描边） */
    public static final float FOCUS_RING = 2f;

    /** 动画时长（毫秒） */
    public static final int DURATION_FAST = 120;
    public static final int DURATION_NORMAL = 200;
    public static final int DURATION_SLOW = 300;
}
