package com.wmp.speedbump.fluent.ui;

import com.wmp.speedbump.fluent.component.FluentScrollPane;
import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentMetrics;
import com.wmp.speedbump.fluent.theme.FluentTheme;
import com.wmp.speedbump.fluent.theme.FluentTypography;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Fluent 页面基类。
 *
 * <p>一个页面 = 「标题区 + 可滚动的内容列」。标题区固定在顶部不随滚动移动（WinUI 的页面结构），
 * 内容列宽度被限制在 {@link FluentMetrics#PAGE_MAX_WIDTH} 以内并水平居中——
 * 这是 WinUI 设置页在超宽屏上的做法：正文不会被拉成一行几百字的长条。</p>
 *
 * <h3>子类只需要往 {@link #body()} 里塞内容</h3>
 * <pre>{@code
 * public class SettingsPage extends FluentPage {
 *     public SettingsPage() {
 *         super("设置", "外观、下载与拓展");
 *         body().add(new FluentCard("外观")...);
 *     }
 * }
 * }</pre>
 */
public abstract class FluentPage extends JPanel implements FluentTheme.ThemeAware {

    private final JPanel body = new JPanel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();
    private final JPanel header = new JPanel();
    private final CenteringPanel centering;

    protected FluentPage(String title, String subtitle) {
        setLayout(new BorderLayout());
        setOpaque(false);

        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new javax.swing.border.EmptyBorder(
                FluentMetrics.PAGE_PADDING, FluentMetrics.PAGE_PADDING,
                FluentMetrics.SPACING_M, FluentMetrics.PAGE_PADDING));

        titleLabel.setText(title);
        titleLabel.setFont(FluentTypography.title());
        titleLabel.setForeground(FluentColors.text());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            subtitleLabel.setText(subtitle);
            subtitleLabel.setFont(FluentTypography.caption());
            subtitleLabel.setForeground(FluentColors.textSecondary());
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(subtitleLabel);
        }
        add(header, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        centering = new CenteringPanel(body, FluentMetrics.PAGE_MAX_WIDTH);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new javax.swing.border.EmptyBorder(
                0, FluentMetrics.PAGE_PADDING, FluentMetrics.PAGE_PADDING, FluentMetrics.PAGE_PADDING));
        wrapper.add(centering, BorderLayout.CENTER);

        FluentScrollPane scroll = new FluentScrollPane(wrapper);
        add(scroll, BorderLayout.CENTER);

        FluentTheme.register(this);
    }

    @Override
    public void onThemeChanged() {
        titleLabel.setForeground(FluentColors.text());
        subtitleLabel.setForeground(FluentColors.textSecondary());
    }

    /** 页面内容容器（纵向 BoxLayout），子类往这里添加卡片与控件 */
    protected JPanel body() {
        return body;
    }

    /** 追加一个组件并置于顶部对齐（接受 {@code Component}，便于直接放 Box 间距） */
    public FluentPage add(java.awt.Component component) {
        if (component instanceof JComponent jc) {
            jc.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        body.add(component);
        return this;
    }

    /** 追加垂直间距 */
    public FluentPage gap(int height) {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(1, height));
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        spacer.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(spacer);
        return this;
    }

    /** 设置/更新页面标题 */
    public void setPageTitle(String title) {
        titleLabel.setText(title);
    }

    public void setPageSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    /**
     * 居中容器：把子组件水平居中，但宽度不超过上限。
     *
     * <p>用 {@code GridBagLayout} 做这件事很别扭（要么被拉伸到满宽，要么宽度退化成首选宽度），
     * 所以直接重写 {@code doLayout}——五行代码解决问题，且行为完全可控。</p>
     */
    static class CenteringPanel extends JPanel {

        private final JComponent child;
        private final int maxWidth;

        CenteringPanel(JComponent child, int maxWidth) {
            this.child = child;
            this.maxWidth = maxWidth;
            setLayout(null);
            setOpaque(false);
            add(child);
            // 子组件尺寸变化时重新布局（例如卡片被折叠）
            child.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    revalidate();
                }
            });
        }

        @Override
        public void doLayout() {
            int available = getWidth();
            int width = Math.min(available, maxWidth);
            int x = Math.max(0, (available - width) / 2);
            // 高度严格等于内容的首选高度：不能让内容被拉伸，
            // 否则 BoxLayout 会把它能长大的子组件（例如卡片）拉高
            int height = child.getPreferredSize().height;
            child.setBounds(new Rectangle(x, 0, width, height));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = child.getPreferredSize();
            return new Dimension(Math.min(d.width, maxWidth), d.height);
        }

        @Override
        public void paint(Graphics g) {
            // 不绘制自身（透明容器），只负责定位
            super.paintChildren(g);
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            doLayout();
        }

        @Override
        public void addNotify() {
            super.addNotify();
            revalidate();
        }

        /** 让 BoxLayout 之外也能访问内容容器 */
        public Container content() {
            return child;
        }
    }
}
