package com.wmp.speed_bump.common.background.tools;

import com.wmp.speed_bump.common.background.exception.NotFoundPlatformFunctionException;
import com.wmp.speed_bump.platform.PlatformClassControl;

/**
 * 平台无关的日志端。
 * <p>
 * 使用方式：{@code private static final SBLogger logger = SBLogger.getLogger(XXX.class);}
 */
public interface SBLogger {
    static SBLogger getLogger(Class<?> clazz){
        var sbLogger = PlatformClassControl.<SBLogger>getCreator(
                "com.wmp.speed_bump.platform.background.%s.tools.SBLogger", PlatformClassControl.Type.BACKGROUND_SIMPLE).create();
        try {
            sbLogger.init(clazz);
        } catch (NotFoundPlatformFunctionException e) {
            throw new RuntimeException(e);
        }
        return sbLogger;
    }

    void init(Class<?> clazz) throws NotFoundPlatformFunctionException;

    void info(Object message);
    void info(Object message, Throwable t);

    void error(Object message);
    void error(Object message, Throwable t);

    void warn(Object message);
    void warn(Object message, Throwable t);

    void debug(Object message);
    void debug(Object message, Throwable t);
}
