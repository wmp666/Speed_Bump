package com.wmp.speed_bump.platform.ui.swing.tools;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;
import raven.modal.Toast;
import raven.modal.toast.option.ToastLayoutOption;
import raven.modal.toast.option.ToastLocation;
import raven.modal.toast.option.ToastOption;
import raven.modal.toast.option.ToastStyle;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ToastMessage implements com.wmp.speed_bump.common.ui.ToastMessage {
    @Override
    public void showToastMessage(Object c, String message, Type type, boolean isUseHeavyWeight) {
        if (c == null) c = Downloader.mainFrame;
        var toastOption = new ToastOption();
        toastOption.setDelay(2000)
                .setHeavyWeight(DataControl.get("is_use_heavy_weight.toast", false) || isUseHeavyWeight)
                .setLayoutOption(new ToastLayoutOption())
                .setHtmlEnabled(true);
        Toast.show((Component) c, switch (type) {
            case INFO -> Toast.Type.INFO;
            case SUCCESS -> Toast.Type.SUCCESS;
            case WARNING -> Toast.Type.WARNING;
            case ERROR -> Toast.Type.ERROR;
            default -> Toast.Type.DEFAULT;
        }, message, ToastLocation.BOTTOM_TRAILING, toastOption);

        var b = !Downloader.mainFrame.isActive();
        if (SystemTray.isSupported() && b && DataControl.get("is_use_system_msg", false)) {
            TrayIcon.MessageType messageType = switch (type) {
                case INFO, SUCCESS -> TrayIcon.MessageType.INFO;
                case WARNING -> TrayIcon.MessageType.WARNING;
                case ERROR -> TrayIcon.MessageType.ERROR;
                default -> TrayIcon.MessageType.NONE;
            };
            Downloader.trayIcon.displayMessage("", message, messageType);
        }
    }

    @Override
    public void showConfirmToastMessage(Object c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, boolean isUseHeavyWeight, ToastFunction toastFunction) {
        JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        functionPanel.setOpaque(false);


        JTextArea textArea = new JTextArea(message);
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        functionPanel.add(textArea);

        AtomicReference<String> id = new AtomicReference<>(null);
        AtomicInteger allClickCount = new AtomicInteger();
        HashMap<Integer, Integer> clickCountMap = new HashMap<>();
        for (var customButton : customButtons) {
            var button = new JButton(customButton.text());
            button.addActionListener(e -> {
                var clickCount = clickCountMap.getOrDefault(customButton.result(), 0);
                clickCountMap.put(customButton.result(), clickCount + 1);
                toastFunction.run(allClickCount.addAndGet(1), clickCount + 1, customButton.result());
                if (onlyClickOnce) {
                    Toast.close(id.get());
                }
            });
            functionPanel.add(button);
        }

        id.set(showComponentToastMessage(c, MultiplePanel.quicklyCreate(
                () -> new MultiplePanel.QuicklyCreatorInfo("swing", functionPanel)),
                isUseHeavyWeight));
    }

    @Override
    public String showComponentToastMessage(Object c, MultiplePanel component, boolean isUseHeavyWeight) {
        if (c == null) {
            c = Downloader.mainFrame;
        }
        var option = new ToastOption();
        option.setDelay(2000)
                .setHeavyWeight(DataControl.get("is_use_heavy_weight.toast", false) || isUseHeavyWeight)
                .setLayoutOption(new ToastLayoutOption()
                        .setLocation(ToastLocation.BOTTOM_TRAILING))
                .setHtmlEnabled(true)
                .setAutoClose(false)
                .setCloseOnClick(true)
                .setStyle(new ToastStyle().setShowCloseButton(true));
        var panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(component.getPanel(), BorderLayout.CENTER);
        AtomicReference<String> id = new AtomicReference<>();
        panel.add(createCloseButton(option, () -> Toast.close(id.get())), BorderLayout.EAST);
        id.set(Toast.showCustom((Component) c, panel, option));
        return id.get();
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

}
