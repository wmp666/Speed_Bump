package com.wmp.speedbump.fluent.page;

import com.wmp.speedbump.fluent.backdrop.BackdropMaterial;
import com.wmp.speedbump.fluent.backdrop.NativeBackdrop;
import com.wmp.speedbump.fluent.component.FluentButton;
import com.wmp.speedbump.fluent.component.FluentCard;
import com.wmp.speedbump.fluent.component.FluentComboBox;
import com.wmp.speedbump.fluent.component.FluentToggleSwitch;
import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;
import com.wmp.speedbump.fluent.ui.FluentPage;
import com.wmp.speedbump.fluent.ui.FluentWindow;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 设置页。
 *
 * <p>这里的开关<b>不是装饰</b>：主题、材质、主题色、光晕动画都会真实改变界面，
 * 这样第一阶段的原型就不再是「静态截图」，而是一个能验证组件库在明暗两种主题、
 * 材质可用与不可用两种环境下都正确的测试台。</p>
 */
public class SettingsPage extends FluentPage {

    private final FluentWindow window;
    private final JLabel diagnosticsLabel = new JLabel();
    private final JLabel materialStateLabel = new JLabel();

    public SettingsPage(FluentWindow window) {
        super("设置", "外观、下载与系统集成");
        this.window = window;

        add(buildAppearanceCard());
        gap(FluentMetrics.SPACING_L);
        add(buildDownloadCard());
        gap(FluentMetrics.SPACING_L);
        add(buildSystemCard());
        gap(FluentMetrics.SPACING_L);
        add(buildDiagnosticsCard());

        // 主题监听由 FluentPage 统一注册，这里通过覆写 onThemeChanged 追加诊断刷新
        refreshDiagnostics();
    }

    // ==================================================================
    // 外观
    // ==================================================================

    private JComponent buildAppearanceCard() {
        FluentCard card = new FluentCard("外观", "主题、材质与强调色");

        FluentComboBox<String> themeBox = new FluentComboBox<>(
                new String[]{"浅色", "深色", "跟随系统"});
        themeBox.setSelectedIndex(FluentTheme.isDark() ? 1 : 0);
        themeBox.addActionListener(e -> FluentTheme.setMode(switch (themeBox.getSelectedIndex()) {
            case 1 -> FluentTheme.Mode.DARK;
            case 2 -> FluentTheme.Mode.SYSTEM;
            default -> FluentTheme.Mode.LIGHT;
        }));
        card.addRow("应用主题", "深色主题会让材质层与文字整体反转", themeBox);

        FluentComboBox<String> materialBox = new FluentComboBox<>(
                new String[]{"亚克力 Acrylic", "云母 Mica", "模糊 Blur", "无（不透明）"});
        materialBox.setSelectedIndex(0);
        materialBox.addActionListener(e -> {
            BackdropMaterial material = switch (materialBox.getSelectedIndex()) {
                case 1 -> BackdropMaterial.MICA;
                case 2 -> BackdropMaterial.BLUR;
                case 3 -> BackdropMaterial.NONE;
                default -> BackdropMaterial.ACRYLIC;
            };
            if (window != null) {
                window.setBackdropMaterial(material);
            }
            refreshDiagnostics();
        });
        card.addRow("窗口材质", "Windows 10 只能真正实现模糊；Mica 需要 Windows 11 22H2", materialBox);

        card.addRow("强调色", "会同步到按钮、开关、进度条与导航指示条", buildAccentPicker());

        FluentToggleSwitch revealSwitch = new FluentToggleSwitch("揭示高亮光晕", RevealEngine.enabled);
        revealSwitch.addActionListener(e -> {
            RevealEngine.enabled = revealSwitch.isSelected();
            if (window != null) {
                window.repaint();
            }
        });
        card.addRow("鼠标光晕", "Fluent 的 Reveal Highlight，关闭后控件不再跟随指针发光", revealSwitch);
        return card;
    }

    /** 强调色色板 */
    private JComponent buildAccentPicker() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, FluentMetrics.SPACING_S, 0));
        row.setOpaque(false);
        Color[] palette = {
                new Color(0x00, 0x78, 0xD4),
                new Color(0x8B, 0x5C, 0xF6),
                new Color(0x0F, 0x7B, 0x0F),
                new Color(0xF7, 0x63, 0x0C),
                new Color(0xC4, 0x2B, 0x1C),
        };
        for (Color color : palette) {
            row.add(new ColorSwatch(color));
        }
        FluentButton reset = new FluentButton("恢复默认", FluentButton.Style.SUBTLE);
        reset.addActionListener(e -> {
            FluentTheme.setAccent(null);
            refreshDiagnostics();
        });
        row.add(reset);
        return row;
    }

    /** 可点击的色块 */
    private static class ColorSwatch extends JComponent {

        private final Color color;
        private boolean hover;

        ColorSwatch(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(FluentMetrics.CONTROL_HEIGHT, FluentMetrics.CONTROL_HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    FluentTheme.setAccent(color);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                FluentPainting.antialias(g2);
                // 只把色块画成中央的圆，四周留白，点击热区更大
                int size = Math.min(20, Math.min(getWidth(), getHeight()) - 4);
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                java.awt.Shape circle = FluentPainting.roundRect(x, y, size, size, FluentMetrics.RADIUS_PILL);
                FluentPainting.fill(g2, circle, color);
                if (hover) {
                    FluentPainting.stroke(g2,
                            FluentPainting.roundRect(x - 2, y - 2, size + 4, size + 4, FluentMetrics.RADIUS_PILL),
                            color, 2f);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ==================================================================
    // 下载
    // ==================================================================

    private JComponent buildDownloadCard() {
        FluentCard card = new FluentCard("下载", "默认行为与限速");

        FluentComboBox<String> threadBox = new FluentComboBox<>(new String[]{"1", "4", "8", "16", "32"});
        threadBox.setSelectedItem("8");
        card.addRow("默认线程数", "分段数过大会被部分服务器拒绝", threadBox);

        card.addRow("全局限速", "限制所有任务的总带宽（尚未实现）",
                new FluentToggleSwitch("启用", false));

        card.addRow("保留未完成任务", "关闭后，退出时丢弃等待中的任务", new FluentToggleSwitch("启用", true));

        FluentComboBox<String> ffmpegBox = new FluentComboBox<>(new String[]{
                "自动探测", "强制软件编码", "关闭硬件加速"});
        card.addRow("FFmpeg 硬件加速", "探测失败时自动回退软件编码", ffmpegBox);
        return card;
    }

    // ==================================================================
    // 系统集成
    // ==================================================================

    private JComponent buildSystemCard() {
        FluentCard card = new FluentCard("系统", "与操作系统的集成");
        card.addRow("开机自启动", "登录后自动运行减速带", new FluentToggleSwitch("启用", false));
        card.addRow("种子文件关联", "双击 .torrent 交给减速带打开", new FluentToggleSwitch("启用", true));
        card.addRow("剪切板监听", "复制链接后自动提示创建任务", new FluentToggleSwitch("启用", false));
        card.addRow("失焦系统通知", "窗口不在前台时用系统通知提醒", new FluentToggleSwitch("启用", true));
        card.addRow("单实例", "再次启动时把链接传给已运行的实例", new FluentToggleSwitch("启用", true));
        return card;
    }

    // ==================================================================
    // 环境诊断
    // ==================================================================

    /**
     * 环境诊断卡片。
     *
     * <p>把「材质能不能生效」的原因直接摊在界面上：Swing 的透明窗口是 layered 窗口，
     * DWM 的 Mica 在 layered 窗口上会返回成功却不渲染，这是本项目最容易踩的坑。
     * 与其让使用者对着「开了 Mica 没效果」发懵，不如把判定结果写出来。</p>
     */
    private JComponent buildDiagnosticsCard() {
        FluentCard card = new FluentCard("环境诊断", "排查材质与字体是否就绪");

        diagnosticsLabel.setFont(FluentTypography.caption());
        diagnosticsLabel.setForeground(FluentColors.textSecondary());
        diagnosticsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.addContent(diagnosticsLabel);

        card.addContent(Box.createVerticalStrut(FluentMetrics.SPACING_S));
        materialStateLabel.setFont(FluentTypography.caption());
        materialStateLabel.setForeground(FluentColors.textSecondary());
        materialStateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.addContent(materialStateLabel);

        card.addContent(Box.createVerticalStrut(FluentMetrics.SPACING_M));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        FluentButton refresh = new FluentButton("重新探测", FluentIcons.REFRESH);
        refresh.addActionListener(e -> refreshDiagnostics());
        FluentButton theme = FluentButton.icon(FluentIcons.BRIGHTNESS, FluentButton.Style.SUBTLE);
        theme.tip("切换明暗主题");
        theme.addActionListener(e -> FluentTheme.toggle());
        actions.add(refresh);
        actions.add(theme);
        card.addContent(actions);
        return card;
    }

    @Override
    public void onThemeChanged() {
        super.onThemeChanged();
        refreshDiagnostics();
    }

    private void refreshDiagnostics() {
        String accent = FluentColors.accentOverride() == null
                ? "跟随系统 / 默认 Fluent 蓝" : "自定义";
        diagnosticsLabel.setText("<html>"
                + "操作系统：" + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + "（build " + NativeBackdrop.windowsBuild() + "）<br>"
                + "原生材质可用：" + (NativeBackdrop.isSupported() ? "是" : "否")
                + "　·　实际生效路径：" + NativeBackdrop.resolve(BackdropMaterial.MICA)
                + "<br>"
                + "主题色：" + accent + "　·　当前主题：" + (FluentTheme.isDark() ? "深色" : "浅色")
                + "<br>"
                + "UI 字体：" + FluentTypography.TEXT_FAMILY
                + "　·　图标字体：" + FluentTypography.ICON_FAMILY
                + (FluentTypography.iconFontAvailable() ? "" : "（不可用，图标退化为占位图形）")
                + "</html>");

        String material = window == null ? "未知" : String.valueOf(window.appliedMaterial());
        String note = FluentColors.isSolidFallback()
                ? "　·　当前已退化为不透明背景（文字对比度优先）"
                : "　·　窗口为逐像素透明，材质可透出";
        materialStateLabel.setText("<html>请求的材质：" + material + note
                + "<br>说明：Swing 的透明窗口是 layered 窗口，"
                + "DWM 的 Mica 在其上<b>会返回成功但不渲染</b>，"
                + "因此本项目默认强制走 SetWindowCompositionAttribute 的模糊路径。</html>");
    }
}
