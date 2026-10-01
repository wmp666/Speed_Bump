package com.wmp.speed_bump.platform.ui.swing.window;

import com.formdev.flatlaf.util.SystemFileChooser;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tool.platform.AutoStart;
import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.speed_bump.common.background.tool.StringFormat;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;
import org.apache.log4j.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;

/**
 * 欢迎页的「语言与启动」页：界面语言、下载目录、开机自启动、启动时检查更新。
 *
 * <p>这一页里有三个「不能只管写配置」的选项，各自都有附加动作：</p>
 * <ul>
 *   <li><b>界面语言</b>：写配置后要重装外观（内部会 {@code StringFormat.refreshLocal()}），
 *       再让外壳把<b>所有</b>页面的文案重刷一遍——语言是跨页生效的；</li>
 *   <li><b>开机自启动</b>：它是本页唯一写到 {@code DataControl} 之外的选项
 *       （注册表 / LaunchAgent / desktop 文件），失败时没有「稍后保存」这一说，
 *       只能当场把开关拨回去并说明原因；</li>
 *   <li><b>下载目录</b>：边输入边校验，输入框短暂为空时不能往配置里写。</li>
 * </ul>
 */
final class GeneralSettingsPage extends MultiplePanel implements SettingsPage {

    private static final Logger logger = Logger.getLogger(GeneralSettingsPage.class);

    /** 界面语言候选，与设置页保持一致 */
    private static final String[][] LANGUAGES = {
            {"简体中文(zh_cn)", "zh_cn"},
            {"English(en_us)", "en_us"},
            {"日本語(ja_JP)", "ja_JP"},
            {"Русский язык(ru_RU)", "ru_RU"},
            {"繁體中文|臺灣(zh_TW)", "zh_TW"},
            {"繁體中文|香港地區(zh_HK)", "zh_HK"},
    };

    // 字段一律不写初始化器，理由见 AppearanceSettingsPage 的同名注释
    private FormPanel panel;
    private JLabel languageLabel;
    private JLabel pathLabel;
    private JComboBox<LanguageItem> languageCombo;
    private JTextField pathField;
    private JButton browseButton;
    private JCheckBox autoStartCheckBox;
    private JCheckBox checkUpdateCheckBox;

    /** 注册给 IconControl 的动态图标任务，窗口销毁时要摘掉，避免任务列表越积越多 */
    private DynamicConverterTask[] iconTasks;

    private WelcomePageHost host;

    @Override
    protected void createPanel() {
        panel = new FormPanel();

        languageLabel = new JLabel();
        pathLabel = new JLabel();

        languageCombo = new JComboBox<>();
        for (String[] language : LANGUAGES) {
            languageCombo.addItem(new LanguageItem(language[0], language[1]));
        }

        pathField = new JTextField(24);

        browseButton = new JButton();

        autoStartCheckBox = new JCheckBox();
        checkUpdateCheckBox = new JCheckBox();
        autoStartCheckBox.setOpaque(false);
        checkUpdateCheckBox.setOpaque(false);

        panel.row(languageLabel, languageCombo);
        panel.row(pathLabel, buildPathRow());
        panel.wide(autoStartCheckBox);
        panel.wide(checkUpdateCheckBox);

        putPanel(panel);
    }

    /** 接上外壳：填初值、挂监听（与 {@link #createPanel()} 的分工见该类注释） */
    void bind(WelcomePageHost host) {
        this.host = host;

        languageCombo.setSelectedItem(languageItemOf(WelcomeSettings.languageCode()));
        pathField.setText(WelcomeSettings.downloadPath());
        checkUpdateCheckBox.setSelected(WelcomeSettings.checkUpdateOnStart());

        //开机自启动必须由打包后的程序设置（依赖 jpackage 写入的 jpackage.app-path），
        //开发环境里读不到，此时把开关置灰而不是让它点了没反应
        boolean autoStartUsable = WelcomeSettings.autoStartUsable();
        autoStartCheckBox.setEnabled(autoStartUsable);
        autoStartCheckBox.setSelected(autoStartUsable && WelcomeSettings.autoStartEnabled());
        autoStartCheckBox.setToolTipText(autoStartUsable ? null
                : StringFormat.translate("welcome.auto_start.unavailable"));

        iconTasks = IconControl.addInDynamicConverter(
                () -> browseButton.setIcon(IconControl.getIcon("folder", browseButton.getFont().getSize())));

        languageCombo.addItemListener(e -> {
            if (host.isRebuilding() || e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            DataControl.put("laug", ((LanguageItem) e.getItem()).code());
            //重装外观内部会 StringFormat.refreshLocal()，之后重刷全部文案才是新语言
            host.reapplyTheme();
            host.retranslateAll();
        });

        checkUpdateCheckBox.addActionListener(e -> {
            if (host.isRebuilding()) {
                return;
            }
            DataControl.put("is_start_check_update", checkUpdateCheckBox.isSelected());
        });

        autoStartCheckBox.addActionListener(e -> {
            if (host.isRebuilding()) {
                return;
            }
            applyAutoStart(autoStartCheckBox.isSelected());
        });

        browseButton.addActionListener(e -> chooseDownloadPath());
        pathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyPath();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyPath();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyPath();
            }
        });
    }

    @Override
    public void retranslate() {
        languageLabel.setText(StringFormat.translate("welcome.laug"));
        pathLabel.setText(StringFormat.translate("welcome.download_path"));
        autoStartCheckBox.setText(StringFormat.translate("welcome.auto_start"));
        checkUpdateCheckBox.setText(StringFormat.translate("welcome.check_update"));
        browseButton.setText(StringFormat.translate("welcome.browse"));
        panel.revalidate();
        panel.repaint();
    }

    @Override
    public void dispose() {
        if (iconTasks != null) {
            IconControl.removeInDynamicConverter(iconTasks);
            iconTasks = null;
        }
    }

    // ==================================================================
    // 下载目录
    // ==================================================================

    private JPanel buildPathRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(pathField, BorderLayout.CENTER);
        row.add(browseButton, BorderLayout.EAST);
        return row;
    }

    private void chooseDownloadPath() {
        var path = DataControl.getPath(panel, SystemFileChooser.OPEN_DIALOG, SystemFileChooser.DIRECTORIES_ONLY);
        if (path == null) {
            return;
        }
        //setText 会顺带触发 document 监听，把新路径写进配置
        pathField.setText(path.getAbsolutePath());
    }

    private void applyPath() {
        String text = pathField.getText().trim();
        //输入过程中会短暂为空（全选删除），此时不要写进配置，免得主界面拿到空目录
        if (text.isEmpty()) {
            return;
        }
        DataControl.put("DownloadFilePath", text);
    }

    // ==================================================================
    // 开机自启动
    // ==================================================================

    /**
     * 立即应用开机自启动。
     *
     * <p>这里刻意不用 {@code ToastMessage}：欢迎页跑在启动阶段，主窗口还不存在，
     * 它拿不到挂载的父窗口。提示改走页脚。</p>
     */
    private void applyAutoStart(boolean wanted) {
        if (!WelcomeSettings.autoStartUsable()) {
            autoStartCheckBox.setSelected(WelcomeSettings.autoStartEnabled());
            host.showHint(StringFormat.translate("welcome.auto_start.unavailable"), true);
            return;
        }
        try {
            AutoStart.setAutoStart(wanted);
            host.showHint(StringFormat.translate("welcome.hint"), false);
        } catch (Throwable t) {
            logger.error("设置开机自启动失败", t);
            autoStartCheckBox.setSelected(WelcomeSettings.autoStartEnabled());
            host.showHint(StringFormat.translate("welcome.auto_start.failed"), true);
        }
    }

    // ==================================================================
    // 语言
    // ==================================================================

    private LanguageItem languageItemOf(String code) {
        for (int i = 0; i < languageCombo.getItemCount(); i++) {
            LanguageItem item = languageCombo.getItemAt(i);
            if (item.code().equalsIgnoreCase(code)) {
                return item;
            }
        }
        //认不出来（例如系统语言不在候选里）就退回第一项，至少不会空着
        return languageCombo.getItemAt(0);
    }

    /** 语言下拉项：语言名本身不翻译（用户要在看不懂当前语言时也能认出自己的母语） */
    private record LanguageItem(String display, String code) {
        @Override
        public String toString() {
            return display;
        }
    }
}
