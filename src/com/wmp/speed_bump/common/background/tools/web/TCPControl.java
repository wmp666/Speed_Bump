package com.wmp.speed_bump.common.background.tools.web;

import com.wmp.speed_bump.platform.PlatformClassControl;

public interface TCPControl {
    TCPControl INSTANCE =
            PlatformClassControl.<TCPControl>getCreator("com.wmp.speed_bump.platform.background.%s.tools.web.TCPControl", PlatformClassControl.Type.BACKGROUND_SIMPLE).create();


    boolean isHasServer() throws Exception;
    void startServer() throws Exception;
    int sendToServer(String message) throws Exception;
}
