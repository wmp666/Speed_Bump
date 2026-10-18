package com.wmp.speed_bump.common.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;
import com.wmp.speed_bump.platform.PlatformClassControl;
import raven.modal.Toast;
import raven.modal.toast.option.ToastOption;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public interface ToastMessage {

    ToastMessage INSTANCE =
            PlatformClassControl.<ToastMessage>getCreator("com.wmp.speed_bump.platform.ui.%s.tools.ToastMessage", PlatformClassControl.Type.UI).create();

    Type DEFAULT = Type.DEFAULT;
    Type INFO = Type.INFO;
    Type SUCCESS = Type.SUCCESS;
    Type WARNING = Type.WARNING;
    Type ERROR = Type.ERROR;

    public enum Type {
        DEFAULT, SUCCESS, INFO, WARNING, ERROR
    }

    /**
     * 显示一个ToastMessage
     * @param c 主窗口
     * @param message 信息
     * @param type 类型
     * @param isUseHeavyWeight 是否使用重型通知
     */
    static void show(Object c, String message, Type type, boolean isUseHeavyWeight) {
        INSTANCE.showToastMessage(c, message, type, isUseHeavyWeight);
    }

    /**
     * @see ToastMessage#show(Object, String, Type, boolean)
     */
    static void show(Object c, String message, Type type) {
        show(c, message, type, false);
    }

    /**
     * @see ToastMessage#show(Object, String, Type, boolean)
     */
    static void show(String message, Type type) {
        show(null, message, type);
    }

    /**
     * @see ToastMessage#show(Object, String, Type, boolean)
     */
    static void show(String message) {
        show(message, Type.DEFAULT);
    }

    /**
     * 显示一个选择窗口
     * @param c 主窗口
     * @param message 信息
     * @param customButtons 按钮
     * @param onlyClickOnce 是否只允许按一下
     * @param isUseHeavyWeight 是否使用重型通知
     * @param toastFunction 按钮点击状态监听
     */
    static void showConfirm(Object c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, boolean isUseHeavyWeight, ToastFunction toastFunction) {
        INSTANCE.showConfirmToastMessage(c, message, customButtons, onlyClickOnce, isUseHeavyWeight, toastFunction);
    }

    /**
     * @see ToastMessage#showConfirm(Object, String, FunctionDialog.CustomButtons[], boolean, boolean, ToastFunction)
     */
    static void showConfirm(Object c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, ToastFunction toastFunction) {
        showConfirm(c, message, customButtons, onlyClickOnce, false, toastFunction);
    }

    /**
     * @see ToastMessage#showConfirm(Object, String, FunctionDialog.CustomButtons[], boolean, boolean, ToastFunction)
     */
    static void showConfirm(Object c, String message, FunctionDialog.CustomButtons[] customButtons, ToastFunction toastFunction) {
        showConfirm(c, message, customButtons, false, toastFunction);
    }

    /**
     * @see ToastMessage#showConfirm(Object, String, FunctionDialog.CustomButtons[], boolean, boolean, ToastFunction)
     */
    static void showConfirm(String message, FunctionDialog.CustomButtons[] customButtons, ToastFunction toastFunction) {
        showConfirm(null, message, customButtons, false, false, toastFunction);
    }

    /**
     * 显示特定组件的ToastMessage
     * @param c 主窗口
     * @param component 组件
     * @param isUseHeavyWeight 是否使用重型通知
     * @return ToastMessage ID
     */
    static String showComponent(Object c, MultiplePanel component, boolean isUseHeavyWeight) {
        return INSTANCE.showComponentToastMessage(c, component, isUseHeavyWeight);
    }

    static String showComponent(Object c, MultiplePanel component) {
        return showComponent(c, component, false);
    }

    static String showComponent(MultiplePanel component) {
        return showComponent(null, component);
    }

    private static JButton createCloseButton(ToastOption option, Runnable stop) {
        Icon icon = option.getStyle().getCloseIcon();
        if (icon == null) {
            icon = new FlatSVGIcon("icon/close.svg", 0.3f);
        }
        JButton buttonClose = new JButton(icon);
        buttonClose.setUI(new BasicButtonUI());
        buttonClose.setOpaque(false);
        buttonClose.setFocusable(false);
        buttonClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buttonClose.setBorder(null);
        buttonClose.setRolloverEnabled(false);
        buttonClose.addActionListener(e -> stop.run());
        return buttonClose;
    }

    //--------实现类----------

    void showToastMessage(Object c, String message, Type type, boolean isUseHeavyWeight);

    void showConfirmToastMessage(Object c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, boolean isUseHeavyWeight, ToastFunction toastFunction);

    String showComponentToastMessage(Object c, MultiplePanel component, boolean isUseHeavyWeight);

    interface ToastFunction {
        void run(int AllClickCount, int clickCount, int result);
    }

    /**
     * 用于便捷生成重复的通知
     */
    class Utils{
        public static void createSaveAndApplyMsg(){
            show(StringFormat.translate("settings.save_and_apply"), SUCCESS);
        }

        public static void createSaveAndApplyNextMsg(){
            show(StringFormat.translate("settings.web.port_settings.update_tip"), INFO);
        }
    }
}
