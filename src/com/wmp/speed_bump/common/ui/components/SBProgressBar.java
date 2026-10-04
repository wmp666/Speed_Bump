package com.wmp.speed_bump.common.ui.components;

import com.wmp.speed_bump.common.background.tools.Creator;
import com.wmp.speed_bump.platform.PlatformClassControl;

/**
 * 进度条的通用端（平台无关）。
 * <p>
 * 凡是只需要「设置/读取进度」这类通用能力的位置，参数与字段一律用本接口，
 * 由 {@link #INSTANCE_CREATOR} 在运行时按平台创建真正的组件实现。
 */
public interface SBProgressBar {
    Creator<SBProgressBar> INSTANCE_CREATOR =
            PlatformClassControl.getCreator("com.wmp.speed_bump.platform.ui.%s.components.SBProgressBar", PlatformClassControl.Type.UI);


    void setProgressValue(int value);

    void setProgressMinValue(int minValue);
    void setProgressMaxValue(int maxValue);

    void setProgressIndeterminate(boolean indeterminacy);
    boolean ProgressIsIndeterminate();

    /** 设置进度条上显示的文字（如 "正在合并..." / "100%"） */
    void setProgressString(String text);

    /** 是否在进度条上绘制文字 */
    void setProgressStringPainted(boolean painted);
}
