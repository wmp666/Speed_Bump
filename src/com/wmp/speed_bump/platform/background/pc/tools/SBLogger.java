package com.wmp.speed_bump.platform.background.pc.tools;

import com.wmp.speed_bump.common.background.exception.NotFoundPlatformFunctionException;
import org.apache.log4j.Logger;

public class SBLogger implements com.wmp.speed_bump.common.background.tools.SBLogger {
    private Logger logger = null;

    @Override
    public void init(Class<?> clazz) throws NotFoundPlatformFunctionException {
        try {
            logger = Logger.getLogger(clazz);
        } catch (Exception e) {
            throw new NotFoundPlatformFunctionException("未找到可用的日志记录器");
        }
    }

    @Override
    public void info(Object message) {
        logger.info(message);
    }

    @Override
    public void info(Object message, Throwable t) {
        logger.info(message, t);
    }

    @Override
    public void error(Object message) {
        logger.error(message);
    }

    @Override
    public void error(Object message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void warn(Object message) {
        logger.warn(message);
    }

    @Override
    public void warn(Object message, Throwable t) {
        logger.warn(message, t);
    }

    @Override
    public void debug(Object message) {
        logger.debug(message);
    }

    @Override
    public void debug(Object message, Throwable t) {
        logger.debug(message, t);
    }
}
