package com.wmp.speed_bump.platform;

import com.wmp.speed_bump.common.background.exception.NotFoundPlatformFunctionException;
import com.wmp.speed_bump.common.background.tools.Creator;
import com.wmp.speed_bump.common.background.tools.platform.GetPlatformName;
import org.jetbrains.annotations.NotNull;

public class PlatformClassControl {

    /**
     * 根据当前平台的状态匹配使用的对应类实现
     * @param className 类名（分平台的名称位置留空为%s）
     * @return 实现类
     */
    public static <T> @NotNull Creator<T> getCreator(String className, @NotNull Type type){
        String newClassName = "";
        switch (type) {
            case UI -> {
                String[] list = {"swing", "fx", "android"};
                String typeStr = GetPlatformName.getUIName();
                newClassName = String.format(className, typeStr);
            }
            case BACKGROUND -> {
                String[] list = {"win", "mac", "linux", "android"};
                newClassName = String.format(className, GetPlatformName.getOSName());

            }
            case BACKGROUND_SIMPLE -> {
                String[] list = {"pc", "phone"};
                newClassName = String.format(className, GetPlatformName.getEquipmentName());

            }
            default -> throw new NotFoundPlatformFunctionException("出现无法识别的类型：" + type);
        }
        if (newClassName.isBlank()) {
            throw new NotFoundPlatformFunctionException("找不到类:" + className + " 类型:" + type + "平台:" + GetPlatformName.getOSName());
        }else {
            try {
                var clazz = Class.forName(newClassName);
                return () -> {
                    try {
                        return (T) clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new NotFoundPlatformFunctionException("类加载异常：" + e.getMessage());
                    }
                };

            } catch (Exception e) {
                throw new NotFoundPlatformFunctionException("类加载异常：" + e.getMessage());
            }
        }
    }

    public enum Type{
        UI, BACKGROUND, BACKGROUND_SIMPLE
    }


}
