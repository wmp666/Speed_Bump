package com.wmp.speed_bump.platform.ui.swing.window;

import com.wmp.speed_bump.common.background.tools.StringFormat;
import com.wmp.speed_bump.common.ui.components.MultiplePanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;

/**
 * 欢迎页的「隐私」页：用户隐私保护协议（承诺不收集用户数据）。
 *
 * <h3>为什么是第一页</h3>
 * <p>协议要在用户真正开始使用之前就被看到，而标签栏顺序就是 {@code addPage} 的顺序，
 * 所以外壳先登记本页。本页没有任何可修改的选项，
 * 因此「跳过就整体回滚」那套语义与它无关。</p>
 *
 * <h3>为什么正文用 HTML 的 {@link JLabel}</h3>
 * <p>协议是一段长文本，用标签的 HTML 视图可以让它按 {@link #BODY_WIDTH} 自动折行；
 * 换成 {@code JTextArea} 就得自己算行数，窗口每次 {@code pack()} 尺寸都会抖一下。
 * 颜色/字号一律走 {@code FlatLaf.style}，主题切换时由外观重新套用，
 * 不需要像自绘色那样再手动压一遍。</p>
 */
final class PrivacySettingsPage extends MultiplePanel implements SettingsPage {

    /** 正文折行宽度（像素） */
    private static final int BODY_WIDTH = 380;

    /** 正文条目对应的翻译键，顺序即显示顺序 */
    private static final String[] ITEM_KEYS = {
            "welcome.privacy.item.local",
            "welcome.privacy.item.no_tracking",
            "welcome.privacy.item.network",
            "welcome.privacy.item.plugin",
    };

    // 字段一律不写初始化器，理由见 AppearanceSettingsPage 的同名注释
    private JLabel titleLabel;
    private JLabel summaryLabel;
    private JLabel[] itemLabels;
    private JLabel footerLabel;

    @Override
    protected void createPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        titleLabel = label("font: bold $h3.font");
        summaryLabel = label("font: bold $default.font");
        footerLabel = label("foreground: $Label.disabledForeground");

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(summaryLabel);
        panel.add(Box.createVerticalStrut(10));

        itemLabels = new JLabel[ITEM_KEYS.length];
        for (int i = 0; i < ITEM_KEYS.length; i++) {
            itemLabels[i] = label("foreground: $Label.disabledForeground");
            panel.add(itemLabels[i]);
            panel.add(Box.createVerticalStrut(6));
        }

        panel.add(Box.createVerticalStrut(4));
        panel.add(footerLabel);

        putPanel(panel);
    }

    @Override
    public void retranslate() {
        titleLabel.setText(StringFormat.translate("welcome.privacy.title"));
        summaryLabel.setText(paragraph("welcome.privacy.summary"));
        for (int i = 0; i < itemLabels.length; i++) {
            itemLabels[i].setText(paragraph(ITEM_KEYS[i]));
        }
        footerLabel.setText(paragraph("welcome.privacy.footer"));
    }

    // ==================================================================
    // 小工具
    // ==================================================================

    private static JLabel label(String flatLafStyle) {
        JLabel label = new JLabel();
        label.putClientProperty("FlatLaf.style", flatLafStyle);
        //BoxLayout 的默认对齐是居中，左对齐才和其余设置页一致
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * 把一条翻译包成定宽折行的 HTML 段落。
     *
     * <p>属性文件里写成 {@code \n} 的换行在 HTML 中要变成 {@code <br>}，
     * 而文案里的 {@code & < >} 必须先转义，否则整条会被当成标签吞掉。</p>
     */
    private static String paragraph(String translateKey) {
        String text = StringFormat.translate(translateKey)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replaceAll("\\R", "<br>");
        return "<html><body style='width:" + BODY_WIDTH + "px'>" + text + "</body></html>";
    }
}
