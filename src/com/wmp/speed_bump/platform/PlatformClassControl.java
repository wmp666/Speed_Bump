package com.wmp.speed_bump.platform;

import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.StatusTipPanel;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.exception.PlatformCLassFindException;
import com.wmp.speed_bump.common.background.tool.Creator;
import com.wmp.speed_bump.common.background.tool.platform.GetPlatformName;

import java.lang.reflect.InvocationTargetException;

public class PlatformClassControl {

    /**
     * 根据当前平台的状态匹配使用的对应类实现
     * @param className 类名（分平台的名称位置留空为%s）
     * @return 实现类
     */
    public static <T> Creator<T> getCreator(String className, Type type){
        String newClassName = "";
        switch (type) {
            case UI -> {
                String[] list = {"swing", "fx", "android"};
                String typeStr = GetPlatformName.isAndroid()?"android":DataControl.get("ui.style", "swing");
                newClassName = String.format(className, typeStr);
            }
            case BACKGROUND -> {
                String[] list = {"win", "mac", "linux", "android"};
                newClassName = String.format(className, GetPlatformName.getOSName());

            }
            case BACKGROUND_SIMPLE -> {
                String[] list = {"pc", "phone"};
                newClassName = String.format(className, GetPlatformName.isAndroid()?list[1]:list[0]);

            }
            default -> throw new PlatformCLassFindException("出现无法识别的类型：" + type);
        }
        if (newClassName.isBlank()) {
            throw new PlatformCLassFindException("找不到类:" + className + " 类型:" + type + "平台:" + GetPlatformName.getOSName());
        }else {
            try {
                var clazz = Class.forName(newClassName);
                return () -> {
                    try {
                        return (T) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new PlatformCLassFindException("类加载异常：" + e.getMessage());
                    }
                };

            } catch (Exception e) {
                throw new PlatformCLassFindException("类加载异常：" + e.getMessage());
            }
        }
    }

    public enum Type{
        UI, BACKGROUND, BACKGROUND_SIMPLE
    }


}
