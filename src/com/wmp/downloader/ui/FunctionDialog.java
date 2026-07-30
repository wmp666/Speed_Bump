package com.wmp.downloader.ui;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.ThemeChanger;
import raven.modal.ModalDialog;
import raven.modal.component.SimpleModalBorder;
import raven.modal.option.ModalBorderOption;
import raven.modal.option.Option;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FunctionDialog extends JDialog {
    public static final int RESULT_OK = 0;
    public static final int RESULT_CANCEL = 1;
    public static final int RESULT_EXIT = 2;
    public static final int RESULT_SAVE = 3;
    public static final int NORTH_DIRECTION_LEFT = 0;
    public static final int NORTH_DIRECTION_CENTER = 1;
    public static final int NORTH_DIRECTION_RIGHT = 2;
    public static final CustomButtons OK_BUTTON = new CustomButtons(StringFormat.translate("common", "enter"), RESULT_OK);
    public static final CustomButtons[] DEFAULT_BUTTONS = {OK_BUTTON};
    public static final CustomButtons SAVE_BUTTON = new CustomButtons(StringFormat.translate("common", "save"), RESULT_SAVE);
    public static final CustomButtons CANCEL_BUTTON = new CustomButtons(StringFormat.translate("common", "cancel"), RESULT_CANCEL);
    public static final CustomButtons[] OK_CANCEL_BUTTONS = {OK_BUTTON, CANCEL_BUTTON};
    public static final CustomButtons[] SAVE_CANCEL_BUTTONS = {SAVE_BUTTON, CANCEL_BUTTON};

    private Component parent = null;

    private int result = RESULT_EXIT;

    private final Timer packTimer = new Timer(50, e -> {
        this.revalidate();
        this.repaint();
        this.pack();
        if (parent != null)
            this.setLocationRelativeTo(parent);
    });

    private JPanel UIPanel;
    private JPanel taskPanel;
    private JPanel ButtonsPanel;
    private final DynamicConverterTask task = () -> {
        if (DataControl.get("theme_type", "light").equals("dark"))
            ButtonsPanel.setBackground(ColorFunctions.lighten(UIManager.getColor("Panel.background"), 0.1f));
        else
            ButtonsPanel.setBackground(ColorFunctions.darken(UIManager.getColor("Panel.background"), 0.1f));
    };
    private JPanel northButtonPanel;

    /**
     * @param c                     父组件
     * @param title                 标题
     * @param functionPanel         功能面板
     * @param resultCallback        结果回调
     * @param buttons               按钮列表
     * @param defaultButtonIndex    默认按钮索引
     * @param northButtons          上方部按钮列表
     * @param northButtonsDirection 上方按钮方向，0表示居左，1表示居中，2表示居右
     */
    private FunctionDialog(Component c, String title,
                           JPanel functionPanel, ResultCallback resultCallback,
                           CustomButtons[] buttons, int defaultButtonIndex,
                           JButton[] northButtons, int northButtonsDirection,
                           boolean isAlwaysTop) {
        this(c, title,
                functionPanel, resultCallback,
                buttons, defaultButtonIndex,
                northButtons, northButtonsDirection,
                isAlwaysTop, false);
    }


    /**
     * @param c                     父组件
     * @param title                 标题
     * @param functionPanel         功能面板
     * @param resultCallback        结果回调
     * @param buttons               按钮列表
     * @param defaultButtonIndex    默认按钮索引
     * @param northButtons          上方部按钮列表
     * @param northButtonsDirection 上方按钮方向，0表示居左，1表示居中，2表示居右
     * @param isAlwaysTop           是否置顶
     * @param isUseScrollPane       是否使用滚动条
     */
    private FunctionDialog(Component c, String title,
                           JPanel functionPanel, ResultCallback resultCallback,
                           CustomButtons[] buttons, int defaultButtonIndex,
                           JButton[] northButtons, int northButtonsDirection,
                           boolean isAlwaysTop, boolean isUseScrollPane) {
        AtomicInteger result = new AtomicInteger(RESULT_EXIT);

        //判断组件c所在的窗体或c是窗体本身是否置顶


        // 添加上方按钮
        if (northButtons != null) {
            for (var jButton : northButtons) {
                jButton.putClientProperty("FlatLaf.style", "font: $h3.font");
                northButtonPanel.add(jButton);
            }
            switch (northButtonsDirection) {
                case NORTH_DIRECTION_LEFT -> northButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                case NORTH_DIRECTION_CENTER -> northButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
                case NORTH_DIRECTION_RIGHT -> northButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            }

        }

        // 添加功能面板
        if (functionPanel != null) {
            if (isUseScrollPane) {
                JScrollPane scrollPane = new JScrollPane(functionPanel);
                taskPanel.add(scrollPane, BorderLayout.CENTER);
            } else taskPanel.add(functionPanel, BorderLayout.CENTER);
        }
        // 添加按钮
        if (buttons.length == 0) {
            buttons = OK_CANCEL_BUTTONS;
        }


        //使用Dialog

        if (c == null) {

            if (c instanceof JFrame frame) {
                this.setAlwaysOnTop(isAlwaysTop || frame.isAlwaysOnTop());
            } else if (c instanceof JDialog dialog) {
                this.setAlwaysOnTop(isAlwaysTop || dialog.isAlwaysOnTop());
            } else {
                this.setAlwaysOnTop(isAlwaysTop);
            }
            this.setResizable(false);
            this.setTitle(title);
            this.setMinimumSize(new Dimension(400, 300));
            if (c instanceof JFrame frame) {
                this.setMaximumSize(frame.getSize());
            } else if (c instanceof JDialog dialog) {
                this.setMaximumSize(dialog.getSize());
            } else {
                this.setMaximumSize(Toolkit.getDefaultToolkit().getScreenSize());
            }
            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setModal(true);
            this.add(UIPanel);

            for (var i = 0; i < buttons.length; i++) {
                var button = buttons[i];
                JButton jButton = new JButton(button.text());
                jButton.putClientProperty("FlatLaf.style", "font: bold $h2.font");
                jButton.addActionListener(e -> {
                    this.setVisible(false);
                    result.set(button.result());
                    this.result = button.result;
                });
                if (i == defaultButtonIndex) {
                    var rootPane1 = ButtonsPanel.getRootPane();
                    if (rootPane1 != null) rootPane1.setDefaultButton(jButton);
                }
                ButtonsPanel.add(jButton);
            }

            this.pack();
            this.parent = c;
            this.setLocationRelativeTo(c);

            packTimer.start();

            this.requestFocus();
            this.setVisible(true);

            if (resultCallback != null)
                resultCallback.onResult(result.get());

            packTimer.stop();

            this.dispose();
        } else {
            var options = new SimpleModalBorder.Option[buttons.length];
            for (var i = 0; i < buttons.length; i++) {
                var button = buttons[i];
                options[i] = new SimpleModalBorder.Option(button.text(), button.result());
            }

            var modalBorderOption = new ModalBorderOption();
            modalBorderOption.setPadding(10, 10, 10, 10)
                    .setUseScroll(true);
            CustomButtons[] finalButtons = buttons;
            SimpleModalBorder simpleModalBorder = new SimpleModalBorder(
                    UIPanel, title, modalBorderOption,
                    options, (action, data) -> {
                if (data == SimpleModalBorder.OPENED) {
                    return;
                }
                if (resultCallback != null) {
                    resultCallback.onResult(data);
                }
            }
            ) {
                @Override
                protected JScrollPane createContentScroll() {
                    var contentScroll = super.createContentScroll();
                    contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                    contentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                    return contentScroll;
                }

                @Override
                protected JButton createButtonOption(Option option) {
                    JButton button = new JButton(option.getText()){
                        @Override
                        public boolean isDefaultButton() {
                            return finalButtons[defaultButtonIndex].result == option.getType();
                        }
                    };
                    button.addActionListener(_ -> doAction(option.getType()));
                    return button;
                }
            };
            ModalDialog.showModal(c, simpleModalBorder,
                    new Option()
                            .setBackgroundClickType(Option.BackgroundClickType.BLOCK)
                            .setHeavyWeight(DataControl.get("is_use_heavy_weight.function_dialog", false)));
        }
    }

    /**
     * @see #FunctionDialog(Component, String, JPanel, ResultCallback, CustomButtons[], int, JButton[], int, boolean)
     */
    public static int showDialog(Component c, String title, JPanel functionPanel, ResultCallback resultCallback, CustomButtons[] buttons, int defaultButtonIndex, JButton[] northButtons, int northButtonsDirection) {
        return new FunctionDialog(c, title,
                functionPanel, resultCallback,
                buttons, defaultButtonIndex,
                northButtons, northButtonsDirection,
                false, false).getResult();
    }

    /**
     * @see #FunctionDialog(Component, String, JPanel, ResultCallback, CustomButtons[], int, JButton[], int, boolean)
     */
    public static int showDialog(Component c, String title, JPanel functionPanel, ResultCallback resultCallback, CustomButtons[] buttons, int defaultButtonIndex, JButton[] northButtons, int northButtonsDirection, boolean isAlwaysTop) {
        return new FunctionDialog(c, title,
                functionPanel, resultCallback,
                buttons, defaultButtonIndex,
                northButtons, northButtonsDirection,
                isAlwaysTop, false).getResult();
    }

    /**
     * @see #FunctionDialog(Component, String, JPanel, ResultCallback, CustomButtons[], int, JButton[], int, boolean)
     */
    public static int showDialog(Component c, String title, JPanel functionPanel, ResultCallback resultCallback, CustomButtons[] buttons, int defaultButtonIndex, JButton[] northButtons, int northButtonsDirection, boolean isAlwaysTop, boolean isUseScrollPane) {
        return new FunctionDialog(c, title,
                functionPanel, resultCallback,
                buttons, defaultButtonIndex,
                northButtons, northButtonsDirection,
                isAlwaysTop, isUseScrollPane).getResult();
    }

    public int getResult(){
        return this.result;
    }
    static void main() {
        FlatMacDarkLaf.setup();

        JFrame frame = new JFrame();
        frame.setBackground(Color.PINK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);

        showDialog(frame, "提示", new JPanel(), result -> {
            System.out.println("Result: " + result);
        }, new CustomButtons[]{new CustomButtons("确定", RESULT_OK), new CustomButtons("保存", 4), new CustomButtons("取消", RESULT_CANCEL)}, 1, new JButton[]{new JButton("一个按钮")}, NORTH_DIRECTION_RIGHT);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        ButtonsPanel = new JPanel();
        ButtonsPanel.setLayout(new GridLayout(1, 0, 5, 5));
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (!b) {
            ThemeChanger.removeDynamicConverter(task);
        }
    }

    @Override
    public void pack() {
        //判断大小
        var preferredSize = this.getPreferredSize();
        var minimumSize = this.getMinimumSize();
        var maximumSize = this.getMaximumSize();

        var size = new Dimension(preferredSize);
        if (preferredSize.width < minimumSize.width) {
            size.width = minimumSize.width;
        } else if (preferredSize.width > maximumSize.width) {
            size.width = maximumSize.width;
        }

        if (preferredSize.height < minimumSize.height) {
            size.height = minimumSize.height;
        } else if (preferredSize.height > maximumSize.height) {
            size.height = maximumSize.height;
        }

        this.setSize(size);
    }

    public interface ResultCallback {
        void onResult(int result);
    }

    public record CustomButtons(String text, int result) {
    }
}


