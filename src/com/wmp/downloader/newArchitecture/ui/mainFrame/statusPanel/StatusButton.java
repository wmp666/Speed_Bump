package com.wmp.downloader.newArchitecture.ui.mainFrame.statusPanel;

import com.formdev.flatlaf.util.ColorFunctions;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.ui.IconControl;

import javax.swing.*;
import java.awt.*;

/**
 * 伪装成 JLabel 的按钮，内置交互反馈（悬停/按压/焦点），自动适配主题。
 */
public class StatusButton extends JButton {

    private String iconKey;          // 图标资源 key
    private Color baseColor;         // 默认背景色
    private Color hoverColor;        // 悬停/焦点时的背景色
    private Color pressedColor;      // 按下时的背景色

    public StatusButton() {
        this("null", null);
    }

    public StatusButton(String iconKey) {
        this(iconKey, null);
    }

    public StatusButton(String iconKey, String tooltip) {
        super();
        this.iconKey = iconKey;

        putClientProperty("FlatLaf.style", "font: $h2.font");

        // ---------- 外观初始化 ----------
        setBorderPainted(false);          // 无边框
        setContentAreaFilled(false);      // 不绘制默认背景（由我们自己绘制）
        setFocusPainted(false);           // 不绘制焦点虚线框
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ---------- 图标（动态转换） ----------
        IconControl.addInDynamicConverter(
                () -> setIcon(IconControl.getIcon(iconKey, getFont().getSize()))
        );

        // ---------- 文本 ----------
        setText("");                     // 纯图标，不显示文字（若需文字可另行设置）

        // ---------- 工具提示 ----------
        if (tooltip != null && !tooltip.isEmpty()) {
            setToolTipText(tooltip);
        }

        // ---------- 计算主题颜色（在绘制前获取，但实际在 paintComponent 中动态计算以支持实时切换） ----------
        // 但为了效率，我们可以在 paintComponent 中每次重新计算，或监听主题变化。
        // 这里我们采用在 paintComponent 中每次计算，因为主题切换后组件会重绘。
    }

    /**
     * 覆写 paintComponent，根据按钮状态绘制自定义背景。
     */
    @Override
    protected void paintComponent(Graphics g) {
        // 1. 获取当前主题颜色（每次都重新计算，以支持运行时切换主题）
        updateThemeColors();

        // 2. 绘制自定义背景
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // 根据按钮模型状态选择背景色
            ButtonModel model = getModel();
            Color bgColor = new Color(0, 0, 0, 0);
            if (model.isPressed()) {
                bgColor = pressedColor;
            } else if (model.isRollover() || model.isArmed()) {
                bgColor = hoverColor;
            } else if (hasFocus()) {
                // 焦点状态可与悬停一致或单独定义，这里沿用悬停色
                bgColor = hoverColor;
            }
            // 绘制背景
            g2.setColor(bgColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }

        // 3. 调用父类绘制图标和文本（注意：由于 contentAreaFilled=false，父类不会绘制背景，但会绘制图标、文本等）
        super.paintComponent(g);
    }

    /**
     * 根据当前主题更新颜色值。
     */
    private void updateThemeColors() {
        Color base = UIManager.getColor("Panel.background");
        if (base == null) {
            base = Color.WHITE; // 后备
        }
        base = new Color(base.getRed(), base.getGreen(), base.getBlue(), 180);

        boolean isDark = "dark".equals(DataControl.get("theme_type", "light"));

        this.baseColor = base;
        this.hoverColor = isDark ? ColorFunctions.lighten(base, 0.2f)
                : ColorFunctions.darken(base, 0.15f);
        this.pressedColor = isDark ? ColorFunctions.lighten(base, 0.4f)
                : ColorFunctions.darken(base, 0.3f);
    }

    // ---------- 可选的便捷方法 ----------
    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
        IconControl.addInDynamicConverter(
                () -> setIcon(IconControl.getIcon(iconKey, getFont().getSize()))
        );
        repaint();
    }

    public String getIconKey() {
        return iconKey;
    }

    // 如果希望点击时触发动作，外部直接添加 ActionListener 即可，无需额外修改。
}