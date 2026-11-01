package com.wmp.speed_bump.common.background.tools.resource.control;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tools.DynamicConverterTask;
import com.wmp.speed_bump.common.background.tools.SBLogger;
import com.wmp.speed_bump.common.ui.components.MultipleIcon;
import com.wmp.speed_bump.platform.PlatformClassControl;

import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;

public interface IconControl<T>{

    /**
     * 当前平台的图标实现。
     * <p>
     * 泛型参数在这里就固定成 {@link ImageIcon}，所以
     * {@code INSTANCE.getIconInstance(...)} 拿到的直接就是 ImageIcon；
     * 若字段声明成裸类型 {@code IconControl}，成员会被类型擦除，调用方只会拿到 Object。
     * <p>
     * 调用方一般直接用静态方法 {@code IconControl.getIcon(...)}，其返回类型由目标类型推断；
     * 推断不出目标类型时（例如紧接其后调用 {@code .getImage()}）需显式写出类型实参：
     * {@code IconControl.<ImageIcon>getIcon("icon")}。
     */
    IconControl INSTANCE = PlatformClassControl.<IconControl>getCreator(
            "com.wmp.speed_bump.platform.background.%s.resource.control.IconControl", PlatformClassControl.Type.BACKGROUND_SIMPLE).create();


    List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();

    static <K> K getIcon(String key) {
        return INSTANCE.getIconInstance(key).getIcon();
    }

    static <K> K getIcon(String type, String key){
        return INSTANCE.getIconInstance(type, key).getIcon();
    }

    static <K> K getIcon(String key, int size) {
        return INSTANCE.getIconInstance(key, size).getIcon();
    }

    static <K> K getIcon(String key, int weight, int height){
        return INSTANCE.getIconInstance(key, weight, height).getIcon();
    }


    default MultipleIcon getIconInstance(String key) {
        return getIcon(DataControl.get("theme_type", "light"), key);
    }

    MultipleIcon getIconInstance(String type, String key);

    default MultipleIcon getIconInstance(String key, int size) {
        return getIcon(key, size, size);
    }

    MultipleIcon getIconInstance(String key, int weight, int height);


    /**
     * 添加动态转换图标任务
     *
     * @param tasks 图标转换任务，将设置图标的代码写在此处
     */
    static DynamicConverterTask[] addInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.addAll(java.util.List.of(tasks));
        for (var task : tasks) {
            try {
                task.task();
            } catch (Exception _) {
            }
        }
        return tasks;
    }

    static void removeInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.removeAll(List.of(tasks));
    }


    /**
     * 运行动态转换图标任务
     */
    static void runDynamicConverters() {
        for (var task : dynamicConverterTasks) {
            try {
                task.task();
            } catch (Exception e) {
                SBLogger.getLogger(IconControl.class).error("图标刷新失败", e);
            }
        }
    }
}
