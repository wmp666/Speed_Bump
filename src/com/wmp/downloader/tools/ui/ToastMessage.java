package com.wmp.downloader.tools.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.FunctionDialog;
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

public class ToastMessage {

    public static final Toast.Type DEFAULT = Toast.Type.DEFAULT;
    public static final Toast.Type INFO = Toast.Type.INFO;
    public static final Toast.Type SUCCESS = Toast.Type.SUCCESS;
    public static final Toast.Type WARNING = Toast.Type.WARNING;
    public static final Toast.Type ERROR = Toast.Type.ERROR;

    public static void show(Component c, String message, Toast.Type type, boolean isUseHeavyWeight) {
        if (c == null) c = Downloader.mainFrame;
        var toastOption = new ToastOption();
        toastOption.setDelay(2000)
                .setHeavyWeight(DataControl.get("is_use_heavy_weight.toast", false) || isUseHeavyWeight)
                .setLayoutOption(new ToastLayoutOption())
                .setHtmlEnabled(true);
        Toast.show(c, type, message, ToastLocation.BOTTOM_TRAILING, toastOption);

    }

    public static void show(Component c, String message, Toast.Type type) {
        show(c, message, type, false);
    }

    public static void show(String message, Toast.Type type) {
        show(null, message, type);
    }

    public static void show(String message) {
        show(message, Toast.Type.DEFAULT);
    }

    public static void showConfirm(Component c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, boolean isUseHeavyWeight, ToastFunction toastFunction) {
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

        id.set(showComponent(c, functionPanel, isUseHeavyWeight));
    }

    public static void showConfirm(Component c, String message, FunctionDialog.CustomButtons[] customButtons, boolean onlyClickOnce, ToastFunction toastFunction) {
        showConfirm(c, message, customButtons, onlyClickOnce, false, toastFunction);
    }

    public static void showConfirm(Component c, String message, FunctionDialog.CustomButtons[] customButtons, ToastFunction toastFunction) {
        showConfirm(c, message, customButtons, false, toastFunction);
    }


    public static void showConfirm(String message, FunctionDialog.CustomButtons[] customButtons, ToastFunction toastFunction) {
        showConfirm(null, message, customButtons, false, false, toastFunction);
    }

    public static String showComponent(Component c, Component component, boolean isUseHeavyWeight) {
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
        panel.add(component, BorderLayout.CENTER);
        AtomicReference<String> id = new AtomicReference<>();
        panel.add(createCloseButton(option, () -> Toast.close(id.get())), BorderLayout.EAST);
        id.set(Toast.showCustom(c, panel, option));
        return id.get();

    }

    public static String showComponent(Component c, Component component) {
        return showComponent(c, component, false);
    }

    public static String showComponent(Component component) {
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

    static void main() {
        FlatMacLightLaf.setup();

        JFrame frame = new JFrame();
        frame.setBackground(Color.PINK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setVisible(true);

        JButton button = new JButton("Show Toast Heavy");
        button.addActionListener(e -> {
            ToastMessage.show(frame, "超长！！！------------------------------------------------------------------------------------------------------", ToastMessage.INFO, true);
            ToastMessage.show(frame, "This is a toast message2", ToastMessage.ERROR, true);
        });

        frame.add(button, BorderLayout.SOUTH);

        JButton button2 = new JButton("Show Toast");
        button2.addActionListener(e -> {
            ToastMessage.show(frame, "超长！！！------------------------------------------------------------------------------------------------------", ToastMessage.INFO);
            ToastMessage.show(frame, "This is a toast message1", ToastMessage.ERROR);
        });

        frame.add(button2, BorderLayout.NORTH);

        frame.add(createCloseButton(new ToastOption(), () -> {
            System.out.println(1);
        }), BorderLayout.CENTER);
    }

    public interface ToastFunction {
        void run(int AllClickCount, int clickCount, int result);
    }
}
