package com.wmp.downloader.tools;

import com.wmp.downloader.newArchitecture.exception.TestFunctionException;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.ToastMessage;

import java.util.*;

public class TestFunctionControl {

    private static ArrayList<Integer> ids = null;

    private static final HashSet<Integer> enableIds = new HashSet<>();

    //mainId -> (id -> description)
    private static final HashMap<Integer, HashMap<Short, String>> tipMap = new HashMap<>();
    //用于管理已注册的测试项启用/禁用方法
    private static final HashMap<Integer, HashMap<Short, TestFunctionUnit>> testFunctionUnitHashMap = new HashMap<>();

    public static void applies(int... ids){
        if (ids != null && TestFunctionControl.ids == null) {
            TestFunctionControl.ids = new ArrayList<>();
            for (var id : ids) {
                TestFunctionControl.ids.add(id);
            }

        }
    }

    /**
     * 加载需要被启用的功能的ID，仅在注册这些功能前最有效
     */
    public static void load(){
        //从Data\TestEnableList.json获取
        enableIds.clear();
        enableIds.addAll(DataControl.getTestEnableSet());
    }

    public static void register(int mainId, short id, String description){
        //检查mainId
        if (!ids.contains(mainId)) {
            throw new TestFunctionException("没有被注册的mainID: " + mainId);
        }

        //存储描述
        var map = tipMap.getOrDefault(mainId, new HashMap<>());
        map.put(id, description);
        tipMap.put(mainId, map);

    }

    public static void run(int mainId, int id, Runnable enableRunnable, Runnable disableRunnable){
        var shortId = Short.MAX_VALUE < id ? Short.MAX_VALUE : (short) id;

        //检查mainId id
        if (!ids.contains(mainId)) {
            throw new TestFunctionException("没有被注册的mainID: " + mainId);
        }else {

            if (!tipMap.get(mainId).containsKey(shortId)){
                throw new TestFunctionException("没有被注册的id: " + mainId + ">" + id);
            }
        }

        //存储
        var map = testFunctionUnitHashMap.getOrDefault(mainId, new HashMap<>());
        map.put(shortId, new TestFunctionUnit(enableRunnable, disableRunnable));
        testFunctionUnitHashMap.put(mainId, map);

        //判断能否运行
        if (enableIds.contains(mainId)) {
            enableRunnable.run();
            ToastMessage.show(
                    String.format(StringFormat.translate("test_function_control.enable_function"),
                            mainId, id), ToastMessage.INFO);
        } else {
            disableRunnable.run();
        }


    }

    static{
        applies(1000, 1001, 1002, 1003);
        register(1000, (short) 1, "显示使窗口重新加载的按钮");
        register(1001, (short) 1, "更新详情翻译按钮");
        register(1002, (short) 1, "设置主窗口置顶，这会导致许多界面异常！");
        register(1003, (short) 1, "显示剪切板监听设置");
    }

    public static HashMap<Integer, HashMap<Short, String>> getAllTip(){
        HashMap<Integer, HashMap<Short, String>> allTipMap = new HashMap<>();
        for (var id : ids) {
            HashMap<Short, String> defaultDescriptionMap = new HashMap<>();
            defaultDescriptionMap.put((short) 0, "昔人已乘黄鹤去，此地空余黄鹤楼。");
            allTipMap.put(id, tipMap.getOrDefault(id, defaultDescriptionMap));
        }
        return allTipMap;
    }

    public static Set<Integer> enableIDList(){
        return enableIds;
    }

    public static void saveEnableList(List<Integer> idList){
        DataControl.saveEnableTestFunctionList(idList);
    }

    record TestFunctionUnit(Runnable enableRunnable, Runnable disableRunnable){}
}
