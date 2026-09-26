package com.wmp.speedbump.fluent.component;

import com.wmp.speedbump.fluent.reveal.RevealEngine;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Shape;

/**
 * Fluent 卡片。
 *
 * <p>Fluent 的层次感不靠阴影，而靠<b>半透明表面 + 1px 极淡描边</b>：
 * 卡片本身是「一层更亮的材质」，叠在窗口材质之上。这与 Material Design 用投影表示高度差别很大。</p>
 *
 * <p>卡片同时是 Reveal Highlight 的载体——指针移入整张卡片会亮起一片跟随指针的光晕，
 * 这正是 WinUI 设置页的观感。</p>
 *
 * <h3>两种用法</h3>
 * <pre>{@code
 * // 1) 带标题的卡片
 * FluentCard card = new FluentCard("下载设置", "影响所有新任务");
 * card.content().add(...);
 *
 * // 2) 设置行（标题 + 说明 + 右侧控件，自动带分隔线）
 * FluentCard card = new FluentCard();
 * card.addRow("开机自启动", "登录后自动运行", new FluentToggleSwitch());
 * }</pre>
 */
public class FluentCard extends JPanel implements FluentTheme.ThemeAware {

    private final JPanel header = new JPanel();
    private final JPanel content = new JPanel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private int radius = FluentMetrics.RADIUS_MEDIUM;
    private int padding = FluentMetrics.SPACING_L;
    private int rowCount = 0;

    public FluentCard() {
        this(null, null);
    }

    public FluentCard(String title) {
        this(title, null);
    }

    public FluentCard(String title, String subtitle) {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(new javax.swing.border.EmptyBorder(padding, padding, padding, padding));

        boolean hasTitle = title != null && !title.isEmpty();
        boolean hasSubtitle = subtitle != null && !subtitle.isEmpty();

        if (hasTitle || hasSubtitle) {
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            if (hasTitle) {
                titleLabel.setText(title);
                titleLabel.setFont(FluentTypography.bodyStrong());
                titleLabel.setForeground(FluentColors.text());
                titleLabel.setAlignmentX(LEFT_ALIGNMENT);
                header.add(titleLabel);
            }
            if (hasSubtitle) {
                subtitleLabel.setText(subtitle);
                subtitleLabel.setFont(FluentTypography.caption());
                subtitleLabel.setForeground(FluentColors.textSecondary());
                subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
                header.add(subtitleLabel);
            }
            add(header, BorderLayout.NORTH);
        }

        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        add(content, BorderLayout.CENTER);

        RevealEngine.register(this);
        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        titleLabel.setForeground(FluentColors.text());
        subtitleLabel.setForeground(FluentColors.textSecondary());
        repaint();
    }

    /** 卡片内部内容容器（纵向排列） */
    public JPanel content() {
        return content;
    }

    /** 替换整个内容区 */
    public FluentCard setContent(JComponent component) {
        removeAll();
        if (header.getComponentCount() > 0) {
            add(header, BorderLayout.NORTH);
        }
        add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
        return this;
    }

    /** 追加一个自定义组件到内容区（{@code Component} 而非 {@code JComponent}，便于直接放 Box 间距） */
    public FluentCard addContent(java.awt.Component component) {
        if (component instanceof JComponent jc) {
            jc.setAlignmentX(LEFT_ALIGNMENT);
        }
        content.add(component);
        return this;
    }

    public FluentCard setPadding(int padding) {
        this.padding = padding;
        setBorder(new javax.swing.border.EmptyBorder(padding, padding, padding, padding));
        revalidate();
        return this;
    }

    public FluentCard setRadius(int radius) {
        this.radius = radius;
        repaint();
        return this;
    }

    // ==================================================================
    // 设置行
    // ==================================================================

    /**
     * 添加一行设置项：左侧「标题 + 说明」，右侧控件。
     *
     * <p>行高由控件高度与两行文字高度决定；连续调用会自动在行之间插入分隔线，
     * 与 WinUI 设置页的列表样式一致。控件为 {@code null} 时只渲染文字行。</p>
     */
    public FluentCard addRow(String title, String description, JComponent control) {
        if (rowCount > 0) {
            content.add(createDivider());
        }
        rowCount++;

        JPanel row = new JPanel(new BorderLayout(FluentMetrics.SPACING_L, 0));
        row.setOpaque(false);
        row.setBorder(new javax.swing.border.EmptyBorder(
                FluentMetrics.SPACING_M, 0, FluentMetrics.SPACING_M, 0));

        JPanel texts = new JPanel(new GridLayout(0, 1, 0, 2));
        texts.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FluentTypography.body());
        titleLabel.setForeground(FluentColors.text());
        texts.add(titleLabel);
        if (description != null && !description.isEmpty()) {
            JLabel descLabel = new JLabel(description);
            descLabel.setFont(FluentTypography.caption());
            descLabel.setForeground(FluentColors.textSecondary());
            texts.add(descLabel);
        }
        row.add(texts, BorderLayout.CENTER);

        if (control != null) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            control.setAlignmentX(RIGHT_ALIGNMENT);
            holder.add(control, BorderLayout.EAST);
            holder.setPreferredSize(new Dimension(
                    Math.max(control.getPreferredSize().width, 48),
                    Math.max(texts.getPreferredSize().height,
                            control.getPreferredSize().height)));
            row.add(holder, BorderLayout.EAST);
        }

        row.setAlignmentX(LEFT_ALIGNMENT);
        // BoxLayout 沿 Y 轴排列时，只有在 maximumSize 允许的情况下才会把行拉满宽度
        Dimension rowPref = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowPref.height));
        content.add(row);
        return this;
    }

    /** 追加一行自定义内容（也参与分隔线计数） */
    public FluentCard addRow(JComponent rowContent) {
        if (rowCount > 0) {
            content.add(createDivider());
        }
        rowCount++;
        rowContent.setAlignmentX(LEFT_ALIGNMENT);
        content.add(rowContent);
        return this;
    }

    private JComponent createDivider() {
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(FluentColors.divider());
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(1, 1));
        divider.setAlignmentX(LEFT_ALIGNMENT);
        return divider;
    }

    @Override
    public Dimension getMaximumSize() {
        // 宽度可任意拉伸（填满内容列），高度锁定为首选高度——
        // 否则放在 BoxLayout 纵向列里时卡片会被拉伸填满剩余空间
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
            Shape shape = FluentPainting.roundRect(0.5, 0.5, getWidth() - 1, getHeight() - 1, radius);
            FluentPainting.fill(g2, shape, FluentColors.card());
            RevealEngine.paint(this, g2, shape);
            FluentPainting.stroke(g2, shape, FluentColors.cardStroke(), FluentMetrics.BORDER);
        } finally {
            g2.dispose();
        }
    }

    /** 便捷：标题左对齐的小节标题组件 */
    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FluentTypography.bodyStrong());
        label.setForeground(FluentColors.text());
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    /** 便捷：说明文字组件 */
    public static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FluentTypography.caption());
        label.setForeground(FluentColors.textSecondary());
        return label;
    }

    /** 便捷：主标题组件 */
    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FluentTypography.title());
        label.setForeground(FluentColors.text());
        return label;
    }

    /** 便捷：不可见占位，把后续内容推到下方 */
    public static JComponent spacer(int height) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, height));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return p;
    }

    /** 便捷：带颜色的小标签（状态徽标） */
    public static JLabel badge(String text, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    FluentPainting.antialias(g2);
                    Shape s = FluentPainting.roundRect(0, 0, getWidth(), getHeight(),
                            FluentMetrics.RADIUS_SMALL);
                    FluentPainting.fill(g2, s, new Color(color.getRed(), color.getGreen(),
                            color.getBlue(), 40));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        label.setFont(FluentTypography.caption());
        label.setForeground(color);
        label.setBorder(new javax.swing.border.EmptyBorder(2, 8, 2, 8));
        label.setOpaque(false);
        return label;
    }
}
