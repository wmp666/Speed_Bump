package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentIcons;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Fluent 文本框。
 *
 * <h3>为什么是「外壳 + 内部 JTextField」而不是继承 JTextField</h3>
 * <p>WinUI 的 TextBox 需要三样 Swing 原生给不了的东西：半透明圆角底、
 * 底部一条会变主题色的 2px 底线、以及可选的左侧引导图标。
 * 直接继承 {@link JTextField} 就必须和 Look&amp;Feel 的 {@code paintBackground}
 * 抢绘制权（FlatLaf 的 {@code FlatTextFieldUI} 会在非 opaque 时也要画自己的底），
 * 结果通常是「圆角底上又叠了一个方角底」。外壳方案把职责分干净：
 * 外壳负责视觉，内层负责文本、光标、选区、输入法与剪切板——这些是最不该重造的部分。</p>
 */
public class FluentTextBox extends JPanel implements FluentTheme.ThemeAware {

    private final JTextField field = new JTextField();
    private final JLabel leadingIcon = new JLabel();
    private final int radius = FluentMetrics.RADIUS_SMALL;

    public FluentTextBox() {
        this(null, (char) 0);
    }

    public FluentTextBox(String placeholder) {
        this(placeholder, (char) 0);
    }

    /**
     * @param placeholder 占位文本（由 FlatLaf 原生支持，无输入时自动显示）
     * @param leadingGlyph 左侧引导图标码位，{@code 0} 表示不显示
     */
    public FluentTextBox(String placeholder, char leadingGlyph) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setFont(FluentTypography.body());

        field.setOpaque(false);
        // 双保险：即便 Look&Feel 仍然画了背景，也是全透明的，不会把外壳的半透明底叠成两层
        field.setBackground(new Color(0, 0, 0, 0));
        field.setFont(FluentTypography.body());
        field.setForeground(FluentColors.text());
        field.setCaretColor(FluentColors.text());
        field.setBorder(new javax.swing.border.EmptyBorder(
                FluentMetrics.TEXT_PADDING_Y, FluentMetrics.TEXT_PADDING_X,
                FluentMetrics.TEXT_PADDING_Y, FluentMetrics.TEXT_PADDING_X));
        if (placeholder != null) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }

        if (leadingGlyph != 0) {
            leadingIcon.setIcon(FluentIcons.themedIcon(leadingGlyph, 16, FluentColors::textSecondary));
            leadingIcon.setBorder(new javax.swing.border.EmptyBorder(
                    0, FluentMetrics.SPACING_M, 0, 0));
            add(leadingIcon, BorderLayout.WEST);
        }
        add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });

        // 有文字时底线颜色更实，与 WinUI 的空/非空状态区分一致
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                repaint();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                repaint();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                repaint();
            }
        });

        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        field.setForeground(FluentColors.text());
        field.setCaretColor(FluentColors.text());
        repaint();
    }

    // ==================================================================
    // 尺寸
    // ==================================================================

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int height = Math.max(FluentMetrics.CONTROL_HEIGHT, d.height);
        return new Dimension(Math.max(d.width, 120), height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            int w = getWidth();
            int h = getHeight();
            boolean enabled = isEnabled();
            boolean focused = field.isFocusOwner();

            // 底色：控件填充
            java.awt.Shape body = FluentPainting.roundRect(0.5, 0.5, w - 1, h - 1, radius);
            Color fill = enabled ? FluentColors.control() : FluentColors.controlDisabled();
            FluentPainting.fill(g2, body, fill);
            FluentPainting.stroke(g2, body, FluentColors.stroke(), FluentMetrics.BORDER);

            // 底部线条：常态 1px 次要色，聚焦 2px 主题色
            if (enabled) {
                if (focused) {
                    g2.setColor(FluentColors.accent());
                    g2.fillRect(1, h - 2, w - 2, 2);
                } else {
                    Color line = FluentColors.textSecondary();
                    g2.setColor(new Color(line.getRed(), line.getGreen(), line.getBlue(),
                            field.getText() != null && !field.getText().isEmpty() ? 0x8C : 0x59));
                    g2.fillRect(1, h - 1, w - 2, 1);
                }
            }
        } finally {
            g2.dispose();
        }
        // 子组件（图标、输入框）由 Swing 在 paint 阶段继续绘制
    }

    // ==================================================================
    // 委托给内部输入框
    // ==================================================================

    public String getText() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }

    public JTextField field() {
        return field;
    }

    public void addActionListener(ActionListener listener) {
        field.addActionListener(listener);
    }

    public void setPlaceholder(String text) {
        field.putClientProperty("JTextField.placeholderText", text);
        field.repaint();
    }

    /** 键盘焦点落到内部输入框，而不是外壳 */
    @Override
    public boolean requestFocusInWindow() {
        return field.requestFocusInWindow();
    }

    @Override
    public void requestFocus() {
        field.requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        field.setEnabled(enabled);
        leadingIcon.setEnabled(enabled);
        repaint();
    }
}
