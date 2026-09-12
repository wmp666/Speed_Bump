package com.wmp.downloader.tools.ui;

import com.wmp.downloader.tools.file.DataControl;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;

/**
 * 拖入文件时显示的半透明遮罩面板。
 *
 * <p>使用时把它作为叠层容器的子组件，并让它的 z-order 位于最上层（{@code setComponentZOrder(overlay, 0)}），
 * 显示时覆盖在页面内容之上。</p>
 *
 * <p>该面板本身不注册任何鼠标或拖放监听器，所以不会拦截下层组件的拖放事件。</p>
 */
public class DropOverlayPanel extends JPanel {

    private static final int BOX_MARGIN = 20;
    private static final int ARC = 28;
    private static final int ICON_SIZE = 48;

    private final String iconKey;

    private String title;
    private String tip;
    private boolean accepted = true;

    /**
     * @param title   主标题，例如“松开鼠标即可安装拓展”
     * @param tip     副标题，可使用 \n 换行
     * @param iconKey 图标键，取自 tools/ui/icons.properties，可为 null
     */
    public DropOverlayPanel(String title, String tip, String iconKey) {
        this.title = title;
        this.tip = tip;
        this.iconKey = iconKey;

        // 半透明遮罩必须是非不透明的，否则会盖住底下的页面内容
        setOpaque(false);
        // 不参与容器的尺寸计算，避免遮罩把页面撑大
        setPreferredSize(new Dimension(0, 0));
        setFocusable(false);
    }

    /**
     * 更新遮罩显示内容
     *
     * @param accepted 拖入的内容是否可接受，false 时使用警示色
     */
    public void setState(boolean accepted, String title, String tip) {
        boolean changed = this.accepted != accepted
                || !Objects.equals(this.title, title)
                || !Objects.equals(this.tip, tip);

        this.accepted = accepted;
        this.title = title;
        this.tip = tip;

        if (changed) repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean dark = "dark".equals(DataControl.get("theme_type", "light"));
        Color accent = accepted ? UIManager.getColor("Component.accentColor") : UIManager.getColor("Actions.Red");
        if (accent == null) accent = accepted ? new Color(0x3D7EFF) : new Color(0xE05A4F);

        // 1.整块区域的半透明底色
        g2.setColor(withAlpha(accent, 55));
        g2.fillRect(0, 0, width, height);

        int boxWidth = Math.max(0, width - BOX_MARGIN * 2);
        int boxHeight = Math.max(0, height - BOX_MARGIN * 2);
        if (boxWidth <= 0 || boxHeight <= 0) {
            g2.dispose();
            return;
        }

        // 2.中间的投放框，使用渐变让中心更亮
        RoundRectangle2D box = new RoundRectangle2D.Float(BOX_MARGIN, BOX_MARGIN, boxWidth, boxHeight, ARC, ARC);
        g2.setPaint(new GradientPaint(
                BOX_MARGIN, BOX_MARGIN, withAlpha(accent, 35),
                BOX_MARGIN, BOX_MARGIN + boxHeight, withAlpha(accent, 115)));
        g2.fill(box);

        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10f, new float[]{10f, 8f}, 0f));
        g2.setColor(withAlpha(accent, 200));
        g2.draw(box);

        // 3.图标与文字
        drawContent(g2, width, height, dark);

        g2.dispose();
    }

    private void drawContent(Graphics2D g2, int width, int height, boolean dark) {
        Color foreground = UIManager.getColor("Label.foreground");
        if (foreground == null) foreground = dark ? Color.WHITE : Color.BLACK;

        Font baseFont = UIManager.getFont("defaultFont");
        if (baseFont == null) baseFont = getFont();
        if (baseFont == null) baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

        Font titleFont = baseFont.deriveFont(Font.BOLD, baseFont.getSize() * 1.6f);
        Font tipFont = baseFont.deriveFont(Font.PLAIN, baseFont.getSize() * 1.05f);

        Icon icon = loadIcon();
        String[] tipLines = tip == null ? new String[0] : tip.split("\\R");

        FontMetrics titleMetrics = g2.getFontMetrics(titleFont);
        FontMetrics tipMetrics = g2.getFontMetrics(tipFont);

        int iconHeight = icon == null ? 0 : icon.getIconHeight();
        int gapAfterIcon = iconHeight > 0 ? 16 : 0;
        int gapAfterTitle = tipLines.length > 0 ? 6 : 0;
        int contentHeight = iconHeight + gapAfterIcon + titleMetrics.getHeight()
                + gapAfterTitle + tipLines.length * tipMetrics.getHeight();

        int y = (height - contentHeight) / 2;
        int centerX = width / 2;

        if (icon != null) {
            icon.paintIcon(this, g2, centerX - icon.getIconWidth() / 2, y);
            y += iconHeight + gapAfterIcon;
        }

        g2.setFont(titleFont);
        g2.setColor(foreground);
        if (title != null) {
            g2.drawString(title, centerX - titleMetrics.stringWidth(title) / 2, y + titleMetrics.getAscent());
        }
        y += titleMetrics.getHeight() + gapAfterTitle;

        g2.setFont(tipFont);
        g2.setColor(withAlpha(foreground, 190));
        for (String line : tipLines) {
            g2.drawString(line, centerX - tipMetrics.stringWidth(line) / 2, y + tipMetrics.getAscent());
            y += tipMetrics.getHeight();
        }
    }

    private Icon loadIcon() {
        if (iconKey == null) return null;
        try {
            return IconControl.getIcon(iconKey, ICON_SIZE);
        } catch (Exception _) {
            return null;
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.max(0, Math.min(255, alpha)));
    }
}
