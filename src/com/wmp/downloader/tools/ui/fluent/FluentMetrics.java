package com.wmp.downloader.tools.ui.fluent;

/**
 * Fluent 风格自绘组件的尺寸与圆角令牌。
 *
 * <p>数值对齐 WinUI 3 的 Fluent 2 规范。注意与项目已有的
 * {@code is_use_square_component} 配置保持一致的取舍：
 * 那个配置控制的是 FlatLaf 组件的圆角（0 或 10），这里是<b>自绘</b>组件自己的圆角——
 * 两者互不干扰，但如果用户明确选择了「方角组件」，
 * 自绘组件应当也用 0（见 {@link #cornerRadius(int)}）。</p>
 */
public final class FluentMetrics {

    private FluentMetrics() {
    }

    // ---- 圆角 ----
    /** 小圆角：按钮、输入框、开关轨道 */
    public static final int RADIUS_SMALL = 4;
    /** 中圆角：卡片、对话框 */
    public static final int RADIUS_MEDIUM = 8;
    /** 胶囊圆角：滚动条滑块、进度条、指示条 */
    public static final int RADIUS_PILL = 99;

    /**
     * 按项目「方角组件」配置折算圆角。
     *
     * @param radius 期望圆角
     * @return 若项目配置为方角组件则返回 0，否则返回期望值
     */
    public static int cornerRadius(int radius) {
        return squareComponents() ? 0 : radius;
    }

    /** 项目配置是否使用方角组件（{@code is_use_square_component}） */
    public static boolean squareComponents() {
        try {
            return com.wmp.downloader.tools.file.DataControl.get("is_use_square_component", Boolean.TRUE);
        } catch (Throwable t) {
            // 配置未加载时与项目默认值保持一致
            return true;
        }
    }

    // ---- 控件尺寸 ----
    public static final int CONTROL_HEIGHT = 32;
    public static final int CONTROL_HEIGHT_SMALL = 26;
    public static final int TEXT_PADDING_X = 11;
    public static final int TEXT_PADDING_Y = 5;

    /** 开关：轨道宽 / 高 */
    public static final int SWITCH_WIDTH = 40;
    public static final int SWITCH_HEIGHT = 20;
    /** 开关：滑块外径 / 内径 */
    public static final int SWITCH_KNOB = 12;
    public static final int SWITCH_KNOB_INNER = 6;
    /** 开关：文字与轨道之间的间距 */
    public static final int SWITCH_GAP = 8;

    /** 复选框边长（非开关样式时） */
    public static final int CHECKBOX_SIZE = 20;

    // ---- 滚动条 ----
    /** 滚动条常态厚度 */
    public static final int SCROLLBAR_THIN = 3;
    /** 滚动条悬停/拖动时的展开厚度 */
    public static final int SCROLLBAR_THICK = 8;
    /** 滚动条滑块最小长度 */
    public static final int SCROLLBAR_MIN_THUMB = 24;

    // ---- 进度条 ----
    /** 进度条高度（Fluent 只有 4px） */
    public static final int PROGRESS_HEIGHT = 4;
    /** 不确定态扫动带占整体的比例 */
    public static final float PROGRESS_BAND_RATIO = 0.35f;

    // ---- 动画 ----
    /** 动画帧间隔，约 60FPS */
    public static final int TICK = 16;
    /** 快速动画时长 */
    public static final int DURATION_FAST = 120;
    /** 常规动画时长 */
    public static final int DURATION_NORMAL = 200;

    // ---- 绘制 ----
    /** 边框线宽 */
    public static final float BORDER = 1f;
    /** 焦点环宽度 */
    public static final float FOCUS_RING = 2f;
}
