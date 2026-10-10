package com.wmp.speed_bump.platform.ui.swing.window;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.ThemeChanger;
import com.wmp.speed_bump.platform.ui.swing.tools.fluent.FluentColors;
import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 欢迎页的「外观」页：主题、主题色、字体、字体大小、使用方角组件。
 *
 * <p>这五项全部是即时预览的——改完立刻调用
 * {@link WelcomePageHost#reapplyTheme()}，用户不用靠想象。</p>
 */
final class AppearanceSettingsPage extends MultiplePanel implements SettingsPage {

    /** 强调色色块的边长 */
    private static final int SWATCH_SIZE = 26;

    /** 强调色预设（与配置里的 {@code accent_color} 同格式：不含 # 的 6 位十六进制） */
    private static final String[] ACCENT_PRESETS = {
            "05E666", "0078D4", "8B5CF6", "E91E63", "FF8C00", "E81123", "00B7C3",
    };

    /** 色块按钮上存放自己色值的键 */
    private static final String ACCENT_HEX_KEY = "WelcomePage.accentHex";

    // 字段一律不写初始化器：createPanel() 是在 MultiplePanel 构造器里被调用的，
    // 那时子类的字段初始化器还没执行，写了也只会是 null（见 MultiplePanel 的类注释）。
    private FormPanel panel;
    private JLabel themeLabel;
    private JLabel accentLabel;
    private JLabel fontLabel;
    private JLabel fontSizeLabel;
    private JComboBox<ThemeChanger.ThemeChoice> themeCombo;
    private JComboBox<String> fontCombo;
    private JSpinner fontSizeSpinner;
    private JCheckBox squareCheckBox;
    private JButton customAccentButton;
    private List<JButton> swatchButtons;

    private WelcomePageHost host;

    @Override
    protected void createPanel() {
        panel = new FormPanel();

        themeLabel = new JLabel();
        accentLabel = new JLabel();
        fontLabel = new JLabel();
        fontSizeLabel = new JLabel();

        themeCombo = new JComboBox<>();
        //三档主题的定义在 ThemeChanger 里，与设置页共用同一份，免得两处又各写一套
        for (ThemeChanger.ThemeChoice choice : ThemeChanger.ThemeChoice.values()) {
            themeCombo.addItem(choice);
        }

        fontCombo = new JComboBox<>();
        //字体名可以非常长，用原型值把下拉的显示宽度钉住，否则窗口会被最长的字体名撑开
        fontCombo.setPrototypeDisplayValue("Microsoft YaHei UI");
        //必须用英文名列表：中文系统下 getAvailableFontFamilyNames() 给的是「微软雅黑」，
        //而配置里存的是 "Microsoft YaHei"，两边对不上，下拉会悄悄退到列表第一项
        //（「Agency FB」），用户点一下「开始使用」就把字体改错了。
        //英文名与语言无关，也才是适合写进配置的那一份。
        for (String font : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Locale.ENGLISH)) {
            fontCombo.addItem(font);
        }

        fontSizeSpinner = new JSpinner(WelcomeSettings.fontSizeModel());

        squareCheckBox = new JCheckBox();
        squareCheckBox.setOpaque(false);

        panel.row(themeLabel, themeCombo);
        panel.row(accentLabel, buildAccentRow());
        panel.row(fontLabel, fontCombo);
        panel.row(fontSizeLabel, buildFontSizeRow());
        panel.wide(squareCheckBox);

        putPanel(panel);
    }

    /**
     * 接上外壳：填初值、挂监听。
     *
     * <p>分工是刻意的——{@link #createPanel()} 只管把控件搭出来（那时外壳还不存在），
     * 与外部打交道的部分全部放到这里，避免在构造期就去碰还没准备好的对象。</p>
     */
    void bind(WelcomePageHost host) {
        this.host = host;

        themeCombo.setSelectedItem(ThemeChanger.ThemeChoice.of(WelcomeSettings.theme()));
        selectFont(WelcomeSettings.font());
        fontSizeSpinner.setValue(WelcomeSettings.fontSize());
        squareCheckBox.setSelected(WelcomeSettings.squareCorners());
        refreshSwatches();

        themeCombo.addItemListener(e -> {
            if (host.isRebuilding() || e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            DataControl.put("theme", ((ThemeChanger.ThemeChoice) e.getItem()).themeName());
            host.reapplyTheme();
        });

        fontCombo.addItemListener(e -> {
            if (host.isRebuilding() || e.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            DataControl.put("Font", String.valueOf(e.getItem()));
            host.reapplyTheme();
        });

        fontSizeSpinner.addChangeListener(e -> {
            if (host.isRebuilding() || !(fontSizeSpinner.getValue() instanceof Number number)) {
                return;
            }
            DataControl.put("FontSize", number.intValue());
            host.reapplyTheme();
        });

        squareCheckBox.addActionListener(e -> {
            if (host.isRebuilding()) {
                return;
            }
            DataControl.put("is_use_square_component", squareCheckBox.isSelected());
            //组件弧度是在装外观时写进 UIManager 的，不重装外观不会变
            host.reapplyTheme();
        });

        customAccentButton.addActionListener(e -> chooseCustomAccent());
    }

    @Override
    public void retranslate() {
        themeLabel.setText(StringFormat.translate("welcome.theme"));
        accentLabel.setText(StringFormat.translate("welcome.accent"));
        fontLabel.setText(StringFormat.translate("welcome.font"));
        fontSizeLabel.setText(StringFormat.translate("welcome.font_size"));
        squareCheckBox.setText(StringFormat.translate("welcome.shape.square"));
        customAccentButton.setText(StringFormat.translate("welcome.accent.custom"));

        //下拉项的文字是渲染时现取的，但宽度可能随语言变化，需要重新布局
        panel.revalidate();
        panel.repaint();
    }

    // ==================================================================
    // 强调色
    // ==================================================================

    private JPanel buildAccentRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));

        swatchButtons = new ArrayList<>(ACCENT_PRESETS.length);
        for (String hex : ACCENT_PRESETS) {
            JButton swatch = createSwatch(hex);
            swatchButtons.add(swatch);
            row.add(swatch);
            row.add(Box.createHorizontalStrut(6));
        }

        customAccentButton = new JButton();
        row.add(Box.createHorizontalStrut(2));
        row.add(customAccentButton);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /**
     * 一个强调色色块。
     *
     * <p>做成「普通按钮 + 自绘图标」而不是自绘按钮：项目里 FluentUi 把
     * {@code ButtonUI} 换成了带揭示光晕的实现，自己重写 paintComponent 会把这套外观绕过去。
     * 只换图标就既能保留按钮的悬停/按下反馈，又能控制外观。</p>
     */
    private JButton createSwatch(String hex) {
        JButton button = new JButton(new SwatchIcon(hex, false));
        button.putClientProperty(ACCENT_HEX_KEY, hex);
        button.setToolTipText("#" + hex);
        button.setFocusable(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        button.setMaximumSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
        button.addActionListener(e -> applyAccent(hex));
        return button;
    }

    private void applyAccent(String hex) {
        DataControl.put("accent_color", hex);
        refreshSwatches();
        //强调色是作为 LAF 全局默认注入的（@accentColor），只有重装外观才生效
        host.reapplyTheme();
    }

    private void chooseCustomAccent() {
        Color initial = WelcomeSettings.parseColor(WelcomeSettings.accent(), FluentColors.accent());
        Color chosen = JColorChooser.showDialog(panel, StringFormat.translate("welcome.accent"), initial);
        if (chosen == null) {
            return;
        }
        applyAccent(String.format("%02X%02X%02X", chosen.getRed(), chosen.getGreen(), chosen.getBlue()));
    }

    /** 把色块按钮的选中态刷成当前强调色 */
    private void refreshSwatches() {
        String current = WelcomeSettings.normalizeHex(WelcomeSettings.accent());
        for (JButton button : swatchButtons) {
            Object hex = button.getClientProperty(ACCENT_HEX_KEY);
            if (!(hex instanceof String value)) {
                continue;
            }
            button.setIcon(new SwatchIcon(value, value.equalsIgnoreCase(current)));
            button.repaint();
        }
    }

    // ==================================================================
    // 字体
    // ==================================================================

    private JPanel buildFontSizeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(fontSizeSpinner);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    /**
     * 按字体族名选中下拉项。
     *
     * <p>找不到时（换过机器、字体被卸载）仍然把原名塞进选中位，让它照实显示出来，
     * <b>但不改配置</b>——静默换成「碰巧排在最前」的字体比显示一个不在列表里的名字更糟。
     * 真正保证「不会顺手写回配置」的是外壳重建期间的闸门。</p>
     */
    private void selectFont(String family) {
        if (family == null || family.isBlank()) {
            return;
        }
        for (int i = 0; i < fontCombo.getItemCount(); i++) {
            if (family.equalsIgnoreCase(fontCombo.getItemAt(i))) {
                fontCombo.setSelectedIndex(i);
                return;
            }
        }
        fontCombo.setSelectedItem(family);
    }

    /** 色块图标：一个圆点；选中时外面套一圈对比色描边 */
    private static final class SwatchIcon implements Icon {

        private final Color color;
        private final boolean selected;

        SwatchIcon(String hex, boolean selected) {
            this.color = WelcomeSettings.parseColor(hex, Color.GRAY);
            this.selected = selected;
        }

        @Override
        public int getIconWidth() {
            return SWATCH_SIZE;
        }

        @Override
        public int getIconHeight() {
            return SWATCH_SIZE;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //选中时把圆点缩小并往外让出一圈底色，再套一圈描边，
            //这样「选中的是谁」一眼就能看出来。
            int inset = selected ? 8 : 4;
            g2.setColor(color);
            g2.fillOval(x + inset, y + inset, SWATCH_SIZE - inset * 2, SWATCH_SIZE - inset * 2);
            //描边刻意不用 FluentColors.accent()：选中色块的色值就是强调色本身，
            //用强调色画圈等于「绿圈套绿点」，完全看不出来。改用与文字同源的对比色。
            g2.setColor(selected ? FluentColors.text() : FluentColors.cardStroke());
            g2.setStroke(new BasicStroke(selected ? 2f : 1f));
            int ring = selected ? 2 : 1;
            g2.drawOval(x + ring, y + ring, SWATCH_SIZE - ring * 2 - 1, SWATCH_SIZE - ring * 2 - 1);
            g2.dispose();
        }
    }
}
