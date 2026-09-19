package com.wmp.speed_bump.common.ui;

import com.wmp.speed_bump.common.background.tool.Creator;
import com.wmp.speed_bump.platform.PlatformClassControl;

/**
 * 预加载窗口的抽象端
 */
public interface PreLoadDialog {
    Creator<PreLoadDialog> INSTANCE_CREATOR =
            PlatformClassControl.getCreator("com.wmp.speed_bump.platform.ui.%s.window.PreloadDialog", PlatformClassControl.Type.UI);

    void showDialog();
    void disposeDialog();
}
