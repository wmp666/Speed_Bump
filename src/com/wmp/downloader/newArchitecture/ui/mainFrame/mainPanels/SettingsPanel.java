package com.wmp.downloader.newArchitecture.ui.mainFrame.mainPanels;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.platform.AutoStart;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.Downloader;
import com.wmp.downloader.ui.common.FileAssociationPanel;
import com.wmp.downloader.ui.common.PathSelectionPanel;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.color.EyeDropperColorChooserPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsPanel {

    private static final Logger logger = Logger.getLogger(SettingsPanel.class);

    public JPanel settingsPanel;
    private JButton refreshButton;
    private JButton saveButton;
    private JTabbedPane tabbedPane2;
    private JScrollPane personalizedSetsScrollPane;
    private JComboBox<String> themeComboBox;
    private JCheckBox isUseHeavyWeightToastCheckBox;
    private JTextField accentColorTextField;
    private JButton accentColorChooseButton;
    private JCheckBox IsUseSquareComponentCheckBox;
    private JComboBox FunctionDialogStyleComboBox;
    private JScrollPane platformSetsScrollPane;
    private JPanel platformSetsPanel;
    private JScrollPane downloadSetsScrollPane;
    private JTextField ThreadNumLabel;
    private JSlider ThreadNumSlider;
    private JCheckBox isUseClipBoardListenerCheckBox;
    private JCheckBox isUseSSLCheckBox;
    private JTextField portTextField;
    private JButton portSaveButton;
    private JButton PortDefaultButton;
    private JScrollPane DataControlSetsScrollPane;
    private JButton deleteTempFolderDataButton;
    private JButton dataPathButton;
    private JButton downloadFilesPathButton;
    private JPanel ThemeControlPanel;
    private JPanel BackgroundControlPanel;
    private JPanel TextUIControlPanel;
    private PathSelectionPanel backgroundSelectionPanel;
    private JComboBox<String> BackgroundModeComboBox;
    private JSlider alphaSlider;
    private JComboBox<String> FontListComboBox;
    private JSpinner fontSizeSpinner;
    private JComboBox<String> laugComboBox;
    private JCheckBox isStartCheckUpdateCheckBox;
    private FileAssociationPanel torrentFileAssociationPanel;
    private JCheckBox isAutoStartCheckBox;
    private PathSelectionPanel pathSelectionPanel;
    private PathSelectionPanel tempPathSelectionPanel;
    private JCheckBox isUseSystemMsgCheckBox;

    private final Downloader downloader;

    public SettingsPanel(Downloader downloader) {
        this.downloader = downloader;
    }

    private void createUIComponents() {
        backgroundSelectionPanel = new PathSelectionPanel(StringFormat.translate("settings", "settings.personalized.background_path"), new File(DataControl.get("background", "")), SystemFileChooser.FILES_ONLY);
        pathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "save_path"), DataControl.getDownloadFilePath());
        tempPathSelectionPanel = new PathSelectionPanel(StringFormat.translate("common", "temp_path"), new File(DataControl.get("TempFilePath", DataControl.getDefaultTempPath().getAbsolutePath())));

        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(DataControl.get("FontSize", 12).intValue(), 1, Integer.MAX_VALUE, 1));

        torrentFileAssociationPanel = new FileAssociationPanel("torrent", StringFormat.translate("file_association.torrent"), "/icon/file_assoication/torrent_file");

        platformSetsScrollPane = UITools.setScrollPaneUnOpaque(new JScrollPane(platformSetsPanel));
    }

    public void initSettingsComponents() {
        ThemeChanger.addInDynamicConverter(
                downloader::updateDefaultButton
        );

        UITools.setScrollPaneUnOpaque(downloadSetsScrollPane);
        UITools.setScrollPaneUnOpaque(personalizedSetsScrollPane);
        UITools.setScrollPaneUnOpaque(DataControlSetsScrollPane);

        downloadSetsScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
        isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
        ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
        ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
        alphaSlider.setValue((int) (DataControl.get("background_alpha", new java.math.BigDecimal("0.3")).floatValue() * 100));
        isStartCheckUpdateCheckBox.setSelected(DataControl.get("is_start_check_update", true));
        accentColorTextField.setText(DataControl.get("accent_color", "05E666"));
        IsUseSquareComponentCheckBox.setSelected(DataControl.get("is_use_square_component", true));
        portTextField.setText(String.valueOf(DataControl.get("port", 5465)));
        isAutoStartCheckBox.setSelected(AutoStart.isAutoStart());
        isUseSystemMsgCheckBox.setSelected(DataControl.get("is_use_system_msg", false));

        BackgroundModeComboBox.addItem("None");
        BackgroundModeComboBox.addItem("Image");

        BackgroundModeComboBox.setSelectedItem(DataControl.get("background_mode", "None"));

        backgroundSelectionPanel.setPath(DataControl.get("background", ""));
        //backgroundSelectionPanel.setVisible(false);
        if (Objects.equals(BackgroundModeComboBox.getSelectedItem(), "Image")) {
            backgroundSelectionPanel.setVisible(true);
        } else backgroundSelectionPanel.setVisible(false);

        //初始化语言设置项
        {
            String[] laugs = new String[]{
                    "简体中文(zh_cn)", "English(en_us)", "日本語(ja_JP)", "Русский язык(ru_RU)",
                    "繁體中文|臺灣(zh_TW)", "繁體中文|香港地區(zh_HK)"
            };

            var lauguage = DataControl.get("laug", "zh_cn");
            for (String laug : laugs) {
                laugComboBox.addItem(laug);

                Matcher matcher = Pattern.compile("\\((.+_.+)\\)").matcher(laug);
                if (matcher.find() && lauguage.equals(matcher.group(1))) {
                    lauguage = laug;
                }
            }

            laugComboBox.setSelectedItem(lauguage);
        }

        //初始化主题设置项
        {
            themeComboBox.addItem("System Theme Style");
            themeComboBox.addItem("Mac Dark");
            themeComboBox.addItem("Mac Light");
            themeComboBox.addItem("Dark");
            themeComboBox.addItem("Light");
            themeComboBox.addItem("Darcula");
            themeComboBox.addItem("IntelliJ");

            themeComboBox.setSelectedItem(DataControl.get("theme", "System Theme Style"));
        }

        //初始化字体设置项
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String font : fonts) FontListComboBox.addItem(font);
        FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));

        isUseHeavyWeightToastCheckBox.setSelected(DataControl.get("is_use_heavy_weight.toast", false));

        //初始化功能性窗口的样式数据
        FunctionDialogStyleComboBox.removeAllItems();
        FunctionDialogStyleComboBox.addItem(StringFormat.translate("settings.personalized.function_dialog_style.use_embed_dialog"));
        FunctionDialogStyleComboBox.addItem(StringFormat.translate("settings.personalized.function_dialog_style.use_local_embed_dialog"));
        FunctionDialogStyleComboBox.addItem(StringFormat.translate("settings.personalized.function_dialog_style.use_dialog"));
        FunctionDialogStyleComboBox.setSelectedIndex(DataControl.get("function_dialog.style", 0));

        //添加图标
        IconControl.addInDynamicConverter(
                () -> dataPathButton.setIcon(IconControl.getIcon("folder", dataPathButton.getFont().getSize())),
                () -> downloadFilesPathButton.setIcon(IconControl.getIcon("folder", downloadFilesPathButton.getFont().getSize())),
                () -> deleteTempFolderDataButton.setIcon(IconControl.getIcon("trash", deleteTempFolderDataButton.getFont().getSize())),
                () -> accentColorChooseButton.setIcon(IconControl.getIcon("eyedropper", accentColorChooseButton.getFont().getSize()))
        );
        IconControl.addInDynamicConverter(
                () -> refreshButton.setIcon(IconControl.getIcon("refresh", refreshButton.getFont().getSize())),
                () -> saveButton.setIcon(IconControl.getIcon("save", saveButton.getFont().getSize()))
        );

        //添加监听
        downloader.mainTabbedPane.addChangeListener(e -> downloader.updateDefaultButton());
        ThreadNumSlider.addChangeListener(e -> {
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            ThreadNumLabel.setSize(ThreadNumLabel.getPreferredSize());
        });


        //动态保存
        BackgroundModeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                DataControl.putAndSave("background_mode", e.getItem().toString());
                if (e.getItem().equals("Image")) {
                    backgroundSelectionPanel.setVisible(true);
                } else {
                    backgroundSelectionPanel.setVisible(false);
                }
                ToastMessage.Utils.createSaveAndApplyMsg();
            }

        });
        backgroundSelectionPanel.setPathChangeListener(path -> {
            DataControl.putAndSave("background", path);
            ToastMessage.Utils.createSaveAndApplyMsg();
        });
        alphaSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();

            // 关键判断：如果正在调整中（鼠标按下拖拽），则忽略，直接返回
            if (source.getValueIsAdjusting()) {
                return;
            }
            DataControl.putAndSave("background_alpha", (float) alphaSlider.getValue() / 100.0f);
            ToastMessage.Utils.createSaveAndApplyMsg();
        });
        themeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                var themeStr = e.getItem().toString();
                DataControl.putAndSave("theme", themeStr);
                ThemeChanger.easyChanger();
                ToastMessage.Utils.createSaveAndApplyMsg();
            }
        });
        FontListComboBox.addActionListener(e -> {
            var fontName = FontListComboBox.getSelectedItem().toString();
            DataControl.putAndSave("Font", fontName);
            ThemeChanger.easyChanger();
            ToastMessage.Utils.createSaveAndApplyMsg();
        });
        isAutoStartCheckBox.addActionListener(_ -> {
            var selected = isAutoStartCheckBox.isSelected();

            if (DataControl.getAppPath() == null) {
                isAutoStartCheckBox.setSelected(!selected);
                ToastMessage.show(StringFormat.translate("error"), ToastMessage.ERROR);
                return;
            }

            try {
                AutoStart.setAutoStart(selected);
            } catch (Exception e) {
                logger.error("发生错误", e);
                isAutoStartCheckBox.setSelected(!selected);
                ToastMessage.show("Exception: " + e.getMessage(), ToastMessage.ERROR);
                return;
            }
            ToastMessage.Utils.createSaveAndApplyMsg();
        });
        FunctionDialogStyleComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                DataControl.putAndSave("function_dialog.style", FunctionDialogStyleComboBox.getSelectedIndex());
                ToastMessage.Utils.createSaveAndApplyMsg();
            }
        });
        isUseSystemMsgCheckBox.addActionListener(e -> {
            var selected = isUseSystemMsgCheckBox.isSelected();
            DataControl.putAndSave("is_use_system_msg", selected);
            ToastMessage.Utils.createSaveAndApplyMsg();
        });

        //下次生效
        PortDefaultButton.addActionListener(_ -> {
            portTextField.setText("5465");
            DataControl.putAndSave("port", portTextField.getText());
            ToastMessage.Utils.createSaveAndApplyNextMsg();

        });
        portSaveButton.addActionListener(_ -> {
            //先判断是不是int
            try {
                Integer.parseInt(portTextField.getText());
            } catch (NumberFormatException e) {
                logger.error("错误的数字类型", e);
                ToastMessage.show(StringFormat.translate("settings.web.port_settings.num_err"), ToastMessage.ERROR);
                return;
            }
            DataControl.putAndSave("port", portTextField.getText());
            ToastMessage.Utils.createSaveAndApplyNextMsg();
        });
        laugComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                var lauguage = e.getItem().toString();
                Matcher matcher = Pattern.compile("\\((.+_.+)\\)").matcher(lauguage);
                if (matcher.find()) {
                    lauguage = matcher.group(1);
                }
                DataControl.putAndSave("laug", lauguage);
                ToastMessage.Utils.createSaveAndApplyNextMsg();
            }
        });

        //强制刷新与保存
        refreshButton.addActionListener(e -> {
            DataControl.load();
            isUseSSLCheckBox.setSelected(DataControl.get("isUseSSL", false));
            isUseClipBoardListenerCheckBox.setSelected(DataControl.get("isUseClipBoardListener", false));
            isUseHeavyWeightToastCheckBox.setSelected(DataControl.get("is_use_heavy_weight.toast", false));
            FunctionDialogStyleComboBox.setSelectedIndex(DataControl.get("function_dialog.style", 0));
            isStartCheckUpdateCheckBox.setSelected(DataControl.get("is_start_check_update", true));
            IsUseSquareComponentCheckBox.setSelected(DataControl.get("is_use_square_component", true));
            isUseSystemMsgCheckBox.setSelected(DataControl.get("is_use_system_msg", false));

            ThreadNumSlider.setValue(DataControl.get("ThreadNum", 64));
            ThreadNumLabel.setText(String.valueOf(ThreadNumSlider.getValue()));
            pathSelectionPanel.setPath(DataControl.getDownloadFilePath().getAbsolutePath());
            tempPathSelectionPanel.setPath(DataControl.get("TempFilePath", DataControl.getDataPath().getAbsolutePath()));
            FontListComboBox.setSelectedItem(DataControl.get("Font", "Microsoft YaHei"));
            fontSizeSpinner.setValue(DataControl.get("FontSize", 12));
            themeComboBox.setSelectedItem(DataControl.get("theme", "System Theme Style"));

            accentColorTextField.setText(DataControl.get("accent_color", "05E666"));

            isAutoStartCheckBox.setSelected(AutoStart.isAutoStart());

            downloader.updateBackground();
            downloader.updateChildBounds(); // FIX 使用统一方法

            ThemeChanger.easyChanger();
        });


        accentColorChooseButton.addActionListener(e -> {
            Color color = null;
            try {
                color = Color.decode("#" + accentColorTextField.getText());
            } catch (NumberFormatException ex) {
                color = new Color(0x29a5e3);
            }
            var colorChooser = new JColorChooser();
            colorChooser.addChooserPanel(new EyeDropperColorChooserPanel());


            var dialog = JColorChooser.createDialog(downloader, StringFormat.translate("settings.personalized.accent_color"), true, colorChooser,
                    e2 -> {
                        var result = colorChooser.getColor();

                        accentColorTextField.setText(String.format("%06X", result.getRGB() & 0x00FFFFFF));
                    },
                    e2 ->{}
            );
            dialog.setVisible(true);

        });

        dataPathButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DataControl.getDataPath());
            } catch (IOException ex) {
                logger.error("文件打开失败", ex);
            }
        });

        downloadFilesPathButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(DataControl.getDownloadFilePath());
            } catch (IOException ex) {
                logger.error("文件打开失败", ex);
            }
        });

        deleteTempFolderDataButton.addActionListener(e -> {
            var tempPath = DataControl.getTempPath();
            DataControl.deleteFolder(tempPath);
        });

        saveButton.addActionListener(e -> {
            DataControl.put("isUseSSL", isUseSSLCheckBox.isSelected());
            DataControl.put("isUseClipBoardListener", isUseClipBoardListenerCheckBox.isSelected());
            DataControl.put("ThreadNum", ThreadNumSlider.getValue());
            DataControl.put("DownloadFilePath", pathSelectionPanel.getPath());
            DataControl.put("TempFilePath", tempPathSelectionPanel.getPath());
            DataControl.put("FontSize", fontSizeSpinner.getValue());
            DataControl.put("is_use_heavy_weight.toast", isUseHeavyWeightToastCheckBox.isSelected());
            DataControl.put("is_start_check_update", isStartCheckUpdateCheckBox.isSelected());
            DataControl.put("accent_color", accentColorTextField.getText());
            DataControl.put("is_use_square_component", IsUseSquareComponentCheckBox.isSelected());

            DataControl.save();
            DataControl.load();

            downloader.updateBackground();
            downloader.updateChildBounds(); // FIX 使用统一方法

            ToastMessage.show(downloader, StringFormat.translate("settings", "settings.save.tip"), ToastMessage.SUCCESS);
            ThemeChanger.easyChanger();
        });
    }

    public JButton getSaveButton() {
        return saveButton;
    }
}
