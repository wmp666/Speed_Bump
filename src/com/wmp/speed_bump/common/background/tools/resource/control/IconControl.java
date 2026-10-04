package com.wmp.speed_bump.common.background.tools.resource.control;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tools.DynamicConverterTask;
import com.wmp.speed_bump.common.background.tools.SBLogger;
import com.wmp.speed_bump.platform.PlatformClassControl;

import javax.swing.ImageIcon;
import java.util.ArrayList;
import java.util.List;

public interface IconControl <T>{

    /**
     * 当前平台的图标实现。
     * <p>
     * 泛型参数在这里就固定成 {@link ImageIcon}，所以
     * {@code IconControl.INSTANCE.getIcon(...)} 拿到的直接就是 ImageIcon；
     * 若字段声明成裸类型 {@code IconControl}，成员会被类型擦除，调用方只会拿到 Object。
     */
    IconControl<ImageIcon> INSTANCE = PlatformClassControl.<IconControl<ImageIcon>>getCreator(
            "com.wmp.speed_bump.platform.background.%s.resource.control.IconControl", PlatformClassControl.Type.BACKGROUND_SIMPLE).create();


    List<DynamicConverterTask> dynamicConverterTasks = new ArrayList<>();

    default T getIcon(String key) {
        return getIcon(DataControl.get("theme_type", "light"), key);
    }

    T getIcon(String type, String key);

    default T getIcon(String key, int size) {
        return getIcon(key, size, size);
    }

    T getIcon(String key, int weight, int height);


    /**
     * 添加动态转换图标任务
     *
     * @param tasks 图标转换任务，将设置图标的代码写在此处
     */
    default DynamicConverterTask[] addInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.addAll(java.util.List.of(tasks));
        for (var task : tasks) {
            try {
                task.task();
            } catch (Exception _) {
            }
        }
        return tasks;
    }

    default void removeInDynamicConverter(DynamicConverterTask... tasks) {
        dynamicConverterTasks.removeAll(List.of(tasks));
    }


    /**
     * 运行动态转换图标任务
     */
    default void runDynamicConverters() {
        for (var task : dynamicConverterTasks) {
            try {
                task.task();
            } catch (Exception e) {
                SBLogger.getLogger(IconControl.class).error("图标刷新失败", e);
            }
        }
    }
}
