package com.wmp.downloader.tools.ui.fluent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ItemListener;

/**
 * WinUI 风格开关（ToggleSwitch）的 UI 代理。
 *
 * <h3>为什么做成 {@code JCheckBox} 的 UI 代理，而不是新控件</h3>
 * <p>本项目 23 个面板里共 21 个 {@code JCheckBox} 是 <b>GUI Designer 的 .form</b> 声明的，
 * 手改它们的控件类型会被设计器覆盖回去。而 {@code CheckBoxUI} 是 Swing 的通用默认值——
 * 换掉它，所有复选框（含设计器生成的）立刻变成开关，<b>不需要碰任何一个 .form 文件</b>。</p>
 *
 * <h3>必须给表格的 Boolean 列留回退（这是最容易踩的坑）</h3>
 * <p>{@code JTable}/{@code JList} 渲染布尔值时会用一个 {@code JCheckBox} 当「图章」：
 * 它<b>不在组件树里</b>（{@code getParent() == null}），只是被临时画到单元格里。
 * 如果不加区分，项目里 {@code LinkFileChoosePanel} 与 {@code TestControlDialog}
 * 的勾选列会变成一排开关——那显然是错的。</p>
 *
 * <p>判定依据是「绘制时是否挂在组件树上」：真实控件的绘制一定发生在窗口层级内，
 * 而渲染器永远没有父容器。命中回退时按传统复选框绘制（圆角方框 + 对勾）。</p>
 *
 * <h3>逐个控件的退出开关</h3>
 * <p>个别位置若仍希望保留传统复选框外观，给它设
 * {@code putClientProperty(FluentSwitchUI.DISABLE_KEY, Boolean.TRUE)} 即可。</p>
 */
public class FluentSwitchUI extends BasicCheckBoxUI {

    /**
     * 必须显式定义：{@code UIDefaults.getUI()} 调用的是静态 {@code createUI}，
     * 缺省会继承 {@link BasicCheckBoxUI#createUI} 并返回普通复选框 UI，
     * 于是复选框不会变成开关，而且不报任何错。
     * 详见 {@link FluentScrollBarUI#createUI}。
     */
    public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
        return new FluentSwitchUI();
    }

    /** 设为 {@code TRUE} 时该复选框保持传统外观 */
    public static final String DISABLE_KEY = "FluentSwitch.disable";

    private static final int ANIMATION_MS = 150;

    private AbstractButton button;
    private ItemListener itemListener;

    /** 滑块位置 0..1 的动画状态 */
    private float knobFrom;
    private float knobTo;
    private float knob;
    private long animationStart;
    private Timer animationTimer;

    // ==================================================================
    // 生命周期
    // ==================================================================

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (c instanceof AbstractButton b) {
            button = b;
            knob = b.isSelected() ? 1f : 0f;
            knobTo = knob;
            itemListener = e -> animateTo(button != null && button.isSelected() ? 1f : 0f);
            b.addItemListener(itemListener);
            // 装上本 UI 即自动参与揭示高亮，不需要调用方记得注册
            RevealEngine.register(b);
        }
    }

    @Override
    public void uninstallUI(JComponent c) {
        stopAnimation();
        if (button != null) {
            RevealEngine.unregister(button);
            if (itemListener != null) {
                button.removeItemListener(itemListener);
            }
        }
        itemListener = null;
        button = null;
        super.uninstallUI(c);
    }

    // ==================================================================
    // 尺寸
    // ==================================================================

    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (!(c instanceof AbstractButton b) || useClassicStyle(b)) {
            return classicPreferredSize(c);
        }
        FontMetrics fm = b.getFontMetrics(fontOf(b));
        String text = textOf(b);
        int textWidth = text.isEmpty() ? 0 : fm.stringWidth(text);
        int gap = textWidth > 0 ? FluentMetrics.SWITCH_GAP : 0;
        int height = Math.max(FluentMetrics.CONTROL_HEIGHT, fm.getHeight() + 8);
        return new Dimension(FluentMetrics.SWITCH_WIDTH + gap + textWidth, height);
    }

    private Dimension classicPreferredSize(JComponent c) {
        if (!(c instanceof AbstractButton b)) {
            return new Dimension(20, 20);
        }
        FontMetrics fm = b.getFontMetrics(fontOf(b));
        String text = textOf(b);
        int textWidth = text.isEmpty() ? 0 : fm.stringWidth(text);
        int gap = textWidth > 0 ? FluentMetrics.SWITCH_GAP : 0;
        int height = Math.max(FluentMetrics.CONTROL_HEIGHT, fm.getHeight() + 8);
        return new Dimension(FluentMetrics.CHECKBOX_SIZE + gap + textWidth, height);
    }

    // ==================================================================
    // 动画
    // ==================================================================

    private void animateTo(float target) {
        if (Math.abs(target - knob) < 0.001f) {
            return;
        }
        knobFrom = knob;
        knobTo = target;
        animationStart = System.nanoTime();
        if (animationTimer == null) {
            animationTimer = new Timer(FluentMetrics.TICK, e -> {
                long elapsed = (System.nanoTime() - animationStart) / 1_000_000L;
                float t = Math.min(1f, elapsed / (float) ANIMATION_MS);
                // 缓出，接近 WinUI 的开关手感
                float eased = 1 - (1 - t) * (1 - t);
                knob = knobFrom + (knobTo - knobFrom) * eased;
                if (button != null) {
                    button.repaint();
                }
                if (t >= 1f) {
                    stopAnimation();
                }
            });
            animationTimer.setCoalesce(true);
        }
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    private void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        knob = knobTo;
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    @Override
    public void paint(Graphics g, JComponent c) {
        if (!(c instanceof AbstractButton b)) {
            super.paint(g, c);
            return;
        }
        if (useClassicStyle(b)) {
            paintClassicBox(g, b);
            return;
        }
        paintSwitch(g, b);
    }

    /**
     * 是否走传统复选框外观。
     *
     * <h3>判定顺序是有讲究的，第一条才是可靠的</h3>
     * <p>Swing 的表格布尔列渲染器是 {@code JTable.BooleanRenderer extends JCheckBox
     * implements TableCellRenderer}（见 JDK 源码 {@code JTable.java}），
     * 也就是说<b>渲染器对象本身就是组件</b>——用 {@code instanceof} 判断精确且零成本。</p>
     *
     * <p>曾经想当然地用「{@code getParent() == null} 说明是渲染器」来判定，
     * 查了 {@code CellRendererPane.paintComponent} 才发现是错的：它里面有
     * {@code if (c.getParent() != this) this.add(c);}，
     * 渲染器在<b>绘制期间</b>的父容器正是那个 {@code CellRendererPane}，并不为 null。
     * 只按这一条判断的话，项目里 {@code LinkFileChoosePanel} 与 {@code TestControlDialog}
     * 的勾选列会变成一排开关。</p>
     */
    private boolean useClassicStyle(AbstractButton b) {
        if (Boolean.TRUE.equals(b.getClientProperty(DISABLE_KEY))) {
            return true;
        }
        // 1) 渲染器对象自身（最可靠）
        if (b instanceof javax.swing.table.TableCellRenderer
                || b instanceof javax.swing.ListCellRenderer) {
            return true;
        }
        // 2) 兜底：挂在 CellRendererPane 下（覆盖「渲染器不是组件本身」的写法）
        for (java.awt.Container p = b.getParent(); p != null; p = p.getParent()) {
            if (p instanceof javax.swing.CellRendererPane) {
                return true;
            }
        }
        // 3) 最后的兜底：完全脱离组件树的「图章」组件
        return b.getParent() == null;
    }

    // ---- 开关样式 ----

    private void paintSwitch(Graphics g, AbstractButton b) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);

            int height = b.getHeight();
            int width = b.getWidth();
            boolean enabled = b.isEnabled();
            boolean on = b.isSelected();

            FontMetrics fm = g2.getFontMetrics(fontOf(b));
            String text = textOf(b);
            int textWidth = text.isEmpty() ? 0 : fm.stringWidth(text);
            int gap = textWidth > 0 ? FluentMetrics.SWITCH_GAP : 0;

            // 设计器可能给了固定尺寸；不能用就把轨道压缩到可用宽度，避免压住文字
            int available = Math.max(16, width - textWidth - gap);
            int trackWidth = Math.min(FluentMetrics.SWITCH_WIDTH, available);
            int trackHeight = Math.min(FluentMetrics.SWITCH_HEIGHT, Math.max(12, height - 4));
            int trackX = 0;
            int trackY = (height - trackHeight) / 2;

            java.awt.Shape track = FluentPainting.roundRect(trackX, trackY, trackWidth, trackHeight,
                    FluentMetrics.RADIUS_PILL);

            Color trackColor;
            if (!enabled) {
                trackColor = FluentColors.controlDisabled();
            } else if (on) {
                trackColor = b.getModel().isRollover() || b.getModel().isPressed()
                        ? FluentColors.accentHover() : FluentColors.accent();
            } else {
                trackColor = b.getModel().isRollover() || b.getModel().isPressed()
                        ? FluentColors.controlHover() : FluentColors.control();
            }
            FluentPainting.fill(g2, track, trackColor);

            // 关态补一圈描边，否则浅色主题下轨道和背景几乎同色
            if (!on || !enabled) {
                FluentPainting.stroke(g2,
                        FluentPainting.roundRect(trackX + 0.5, trackY + 0.5,
                                trackWidth - 1, trackHeight - 1, FluentMetrics.RADIUS_PILL),
                        FluentColors.stroke(), FluentMetrics.BORDER);
            }

            // 揭示高亮：画在轨道之上、滑块与文字之下，层级与 WinUI 一致
            RevealEngine.paint(b, g2,
                    FluentPainting.roundRect(0, 0, width, height, FluentMetrics.RADIUS_SMALL));

            // 滑块
            int knobOuter = Math.min(FluentMetrics.SWITCH_KNOB, Math.max(8, trackHeight - 8));
            float travel = trackWidth - knobOuter - 8f;
            float knobX = trackX + 4 + travel * Math.max(0f, Math.min(1f, knob));
            float knobY = trackY + (trackHeight - knobOuter) / 2f;

            Color knobColor;
            if (!enabled) {
                knobColor = FluentColors.textDisabled();
            } else if (on) {
                knobColor = FluentColors.onAccent();
            } else {
                knobColor = b.getModel().isRollover() || b.getModel().isPressed()
                        ? FluentColors.text() : FluentColors.textSecondary();
            }
            g2.setColor(knobColor);
            g2.fillOval(Math.round(knobX), Math.round(knobY), knobOuter, knobOuter);

            // 文字
            if (!text.isEmpty()) {
                Rectangle textRect = new Rectangle(trackWidth + gap,
                        (height - fm.getHeight()) / 2,
                        Math.max(0, width - trackWidth - gap),
                        fm.getHeight());
                paintText(g2, b, textRect, text);
            }

            if (b.isFocusOwner() && b.isFocusPainted()) {
                FluentPainting.focusRing(g2, b, FluentMetrics.RADIUS_SMALL);
            }
        } finally {
            g2.dispose();
        }
    }

    // ---- 传统复选框样式（渲染器回退） ----

    private void paintClassicBox(Graphics g, AbstractButton b) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);

            int height = b.getHeight();
            int size = Math.min(FluentMetrics.CHECKBOX_SIZE, Math.max(10, height - 4));
            int y = (height - size) / 2;
            boolean enabled = b.isEnabled();
            boolean checked = b.isSelected();
            int radius = FluentMetrics.cornerRadius(FluentMetrics.RADIUS_SMALL);

            java.awt.Shape box = FluentPainting.roundRect(0.5, y + 0.5, size - 1, size - 1, radius);
            Color fill;
            if (!enabled) {
                fill = FluentColors.controlDisabled();
            } else if (checked) {
                fill = FluentColors.accent();
            } else {
                fill = b.getModel().isRollover() ? FluentColors.controlHover() : FluentColors.control();
            }
            FluentPainting.fill(g2, box, fill);
            if (!checked || !enabled) {
                FluentPainting.stroke(g2, box, FluentColors.stroke(), FluentMetrics.BORDER);
            }

            if (checked && enabled) {
                // 矢量对勾，避免依赖图标字体
                g2.setColor(FluentColors.onAccent());
                g2.setStroke(new java.awt.BasicStroke(2f,
                        java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int cx = size / 2;
                g2.drawLine(Math.round(size * 0.24f), y + cx,
                        Math.round(size * 0.44f), y + Math.round(size * 0.68f));
                g2.drawLine(Math.round(size * 0.44f), y + Math.round(size * 0.68f),
                        Math.round(size * 0.78f), y + Math.round(size * 0.30f));
            }

            // 揭示高亮：方框之上、文字之下
            RevealEngine.paint(b, g2,
                    FluentPainting.roundRect(0, 0, b.getWidth(), height, FluentMetrics.RADIUS_SMALL));

            FontMetrics fm = g2.getFontMetrics(fontOf(b));
            String text = textOf(b);
            if (!text.isEmpty()) {
                Rectangle textRect = new Rectangle(size + FluentMetrics.SWITCH_GAP,
                        (height - fm.getHeight()) / 2,
                        Math.max(0, b.getWidth() - size - FluentMetrics.SWITCH_GAP),
                        fm.getHeight());
                paintText(g2, b, textRect, text);
            }
        } finally {
            g2.dispose();
        }
    }

    // ==================================================================
    // 小工具
    // ==================================================================

    private static java.awt.Font fontOf(AbstractButton b) {
        java.awt.Font f = b.getFont();
        return f != null ? f : new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12);
    }

    private static String textOf(AbstractButton b) {
        String t = b.getText();
        return t == null ? "" : t;
    }

    /** 覆盖图标相关逻辑：本 UI 完全自绘，不需要图标（也必须避免 L&F 灌进来一个） */
    @Override
    public Icon getDefaultIcon() {
        return null;
    }

    @Override
    protected void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setIcon(null);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(true);
    }
}
