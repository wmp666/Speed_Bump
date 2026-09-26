package com.wmp.speedbump.fluent.backdrop;

/**
 * Fluent 窗口背景材质类型。
 *
 * <p>取值对齐 WinUI 3 的 {@code SystemBackdrop} 家族。注意这些是<b>意图</b>，
 * 最终能否生效由系统版本决定，见 {@link NativeBackdrop} 的降级表。</p>
 */
public enum BackdropMaterial {

    /**
     * 云母：取桌面壁纸的主色做底，再叠加一层柔和噪声。
     * Windows 11 22H2+ 独有。
     */
    MICA("云母 Mica", 2, -1),

    /**
     * 云母 Alt：比 Mica 更暗的变体，适合长时间阅读的界面。
     * Windows 11 22H2+ 独有。
     */
    MICA_ALT("云母 Mica Alt", 4, -1),

    /**
     * 亚克力：对窗口背后的内容做实时模糊并着色。
     * Windows 11 22H2+ 原生支持；Windows 10 1803+ 由 {@code SetWindowCompositionAttribute} 近似实现。
     */
    ACRYLIC("亚克力 Acrylic", 3, 4),

    /**
     * 纯模糊：只模糊不额外着色。
     * Windows 10 1803+ 上这是<b>唯一</b>真正产生模糊效果的路径。
     */
    BLUR("模糊 Blur", -1, 3),

    /** 关闭原生材质，由应用自绘不透明背景 */
    NONE("无", 1, 0);

    private final String displayName;
    /** {@code DWMWA_SYSTEMBACKDROP_TYPE} 取值，{@code -1} 表示不使用 DWM 路径 */
    final int dwmBackdropType;
    /** {@code ACCENTPOLICY.nAccentState} 取值，{@code -1} 表示不使用该回退路径 */
    final int accentState;

    BackdropMaterial(String displayName, int dwmBackdropType, int accentState) {
        this.displayName = displayName;
        this.dwmBackdropType = dwmBackdropType;
        this.accentState = accentState;
    }

    public String displayName() {
        return displayName;
    }

    /** 该材质的最低系统要求描述 */
    public String requirement() {
        return switch (this) {
            case MICA, MICA_ALT -> "Windows 11 22H2 (build 22621) 及以上";
            case ACRYLIC -> "Windows 11 22H2；Windows 10 1803+ 为近似效果";
            case BLUR -> "Windows 10 1803 (build 17134) 及以上";
            case NONE -> "全平台";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
