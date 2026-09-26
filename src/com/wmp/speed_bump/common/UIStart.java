package com.wmp.speed_bump.common;

import com.wmp.speed_bump.common.background.tool.Creator;
import com.wmp.speed_bump.common.ui.PreLoadDialog;
import com.wmp.speed_bump.platform.PlatformClassControl;

import java.util.List;

public interface UIStart {
    UIStart INSTANCE =
            (UIStart) PlatformClassControl.getCreator("com.wmp.speed_bump.platform.ui.%s.UIStart", PlatformClassControl.Type.UI).create();

    void show(List<String> argList, String linkPath);
}
