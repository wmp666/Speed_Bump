package com.wmp.speedbump.fluent.reveal;

import com.wmp.speedbump.fluent.theme.FluentColors;
import com.wmp.speedbump.fluent.theme.FluentPainting;
import com.wmp.speedbump.fluent.theme.FluentTheme;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Reveal Highlight（揭示高亮）引擎 —— Fluent Design 的标志性光晕效果。
 *
 * <h3>它做什么</h3>
 * <p>指针在卡片 / 按钮 / 导航项上移动时，在控件「背景之上、内容之下」绘制一团跟随指针的柔和光斑，
 * 并带渐变淡入淡出。WinUI 用它暗示「这里可以交互」。</p>
 *
 * <h3>为什么用全局指针模型，而不是给每个组件挂 MouseListener</h3>
 * <p>含子组件的容器（卡片、列表项）在指针移入子组件时会收到 {@code mouseExited}，
 * 移回空白处又收到 {@code mouseEntered}，会造成光晕闪烁甚至熄灭。
 * 这里改为监听全局 AWT 鼠标事件，用<b>组件边界命中</b>判定指针压在哪些控件上，
 * 因此卡片与其内部按钮可以同时亮起——这正是 WinUI 的行为。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 1) 注册（组件构造时调用一次）
 * RevealEngine.register(this);
 *
 * // 2) 在背景之后、文字之前绘制
 * @Override protected void paintComponent(Graphics g) {
 *     Graphics2D g2 = (Graphics2D) g.create();
 *     Shape clip = FluentPainting.roundRect(this, 0, 4);
 *     FluentPainting.fill(g2, clip, FluentColors.card());
 *     RevealEngine.paint(this, g2, clip);   // ← 光晕
 *     g2.dispose();
 *     super.paintComponent(g);
 * }
 * }</pre>
 *
 * <p>组件被 GC 后自动从表中消失（{@link WeakHashMap}），无需手动注销；
 * 显式 {@link #unregister} 只在需要立刻停止动画时使用。</p>
 */
public final class RevealEngine {

    private RevealEngine() {
    }

    /** 全局开关。关闭后不再注册监听器，也不绘制光晕。 */
    public static boolean enabled = true;

    /** 光斑半径下限（px）；实际半径取「该值与组件高度 × 2.5」中的较大者 */
    public static float minRadius = 90f;

    /** 动画步进间隔，约 60 FPS */
    private static final int TICK = 16;

    /** 指针追随的插值系数：越大越跟手，越小越「拖尾」 */
    private static final float POINTER_LERP = 0.32f;

    /** 透明度插值系数 */
    private static final float ALPHA_LERP = 0.22f;

    /** 认为动画结束的阈值 */
    private static final float EPSILON = 0.01f;

    private static final Map<JComponent, State> STATES = new WeakHashMap<>();
    private static Timer timer;
    private static AWTEventListener pointerListener;

    /** 单个组件的光晕状态 */
    private static final class State {
        float alpha;
        float target;
        float px;
        float py;
        boolean hasPointer;
    }

    // ==================================================================
    // 注册
    // ==================================================================

    /** 让组件参与揭示高亮 */
    public static void register(JComponent component) {
        if (component == null || !enabled) {
            return;
        }
        synchronized (STATES) {
            STATES.computeIfAbsent(component, k -> new State());
        }
        installPointerListener();
    }

    /** 立刻停止该组件的光晕 */
    public static void unregister(JComponent component) {
        if (component == null) {
            return;
        }
        synchronized (STATES) {
            STATES.remove(component);
        }
    }

    /** 组件是否已注册 */
    public static boolean isRegistered(JComponent component) {
        synchronized (STATES) {
            return STATES.containsKey(component);
        }
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    /**
     * 绘制光晕。必须在组件背景填充<b>之后</b>、内容绘制<b>之前</b>调用。
     *
     * @param component 目标组件
     * @param g         已被裁剪/可安全修改的 Graphics2D（方法内部会自行 create/dispose）
     * @param clip      光晕的裁剪形状（通常是组件的圆角矩形）
     */
    public static void paint(JComponent component, Graphics2D g, Shape clip) {
        if (!enabled || component == null || clip == null) {
            return;
        }
        State state = stateOf(component);
        if (state == null || state.alpha <= EPSILON || !state.hasPointer) {
            return;
        }

        float width = Math.max(1f, component.getWidth());
        float height = Math.max(1f, component.getHeight());
        float radius = Math.max(minRadius, height * 2.5f);
        // 指针在组件外时，把光斑中心夹到边界内，避免光晕整块消失
        float cx = Math.max(0, Math.min(width, state.px));
        float cy = Math.max(0, Math.min(height, state.py));

        Color glow = glowColor();
        int a = Math.round(glow.getAlpha() * state.alpha);
        if (a <= 1) {
            return;
        }
        Color center = new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), a);
        Color edge = new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 0);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            FluentPainting.antialias(g2);
            g2.clip(clip);
            g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(cx, cy), radius,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{center, center, edge}));
            g2.fill(clip);
        } catch (Throwable ignored) {
            // 半径退化等极端情况直接跳过绘制，不影响组件本体
        } finally {
            g2.dispose();
        }
    }

    /** 当前主题下的光晕色 */
    private static Color glowColor() {
        if (FluentColors.isSolidFallback()) {
            return new Color(255, 255, 255, 0x40);
        }
        // 浅色主题：白色光晕能提亮卡面；深色主题：极低透明度，避免「糊一层灰」
        return FluentTheme.isDark()
                ? new Color(255, 255, 255, 0x1C)
                : new Color(255, 255, 255, 0xB0);
    }

    // ==================================================================
    // 指针跟踪与动画
    // ==================================================================

    private static State stateOf(JComponent component) {
        synchronized (STATES) {
            return STATES.get(component);
        }
    }

    private static void installPointerListener() {
        if (pointerListener != null) {
            return;
        }
        pointerListener = (AWTEvent event) -> {
            int id = event.getID();
            if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED
                    || id == MouseEvent.MOUSE_ENTERED || id == MouseEvent.MOUSE_EXITED) {
                updatePointer();
            }
        };
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(pointerListener,
                    AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
        } catch (Throwable t) {
            pointerListener = null;
        }
    }

    /** 依据全局指针位置更新所有已注册组件的目标状态 */
    private static void updatePointer() {
        if (!enabled) {
            return;
        }
        Point screen;
        try {
            var info = MouseInfo.getPointerInfo();
            if (info == null) {
                return;
            }
            screen = info.getLocation();
        } catch (Throwable t) {
            return;
        }

        boolean anyActive = false;
        synchronized (STATES) {
            for (Map.Entry<JComponent, State> entry : STATES.entrySet()) {
                JComponent c = entry.getKey();
                State s = entry.getValue();
                if (c == null || !c.isShowing()) {
                    s.target = 0;
                    continue;
                }
                try {
                    Point local = new Point(screen);
                    SwingUtilities.convertPointFromScreen(local, c);
                    boolean inside = local.x >= 0 && local.y >= 0
                            && local.x < c.getWidth() && local.y < c.getHeight();
                    if (inside) {
                        s.target = 1f;
                        s.px = local.x;
                        s.py = local.y;
                        s.hasPointer = true;
                    } else {
                        s.target = 0f;
                    }
                } catch (Throwable t) {
                    s.target = 0f;
                }
                if (s.target > 0 || s.alpha > EPSILON) {
                    anyActive = true;
                }
            }
        }
        if (anyActive) {
            startTimer();
        }
    }

    private static void startTimer() {
        if (timer == null) {
            timer = new Timer(TICK, e -> tick());
            timer.setCoalesce(true);
        }
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /** 推进一帧 */
    private static void tick() {
        if (!enabled) {
            stopTimer();
            return;
        }
        List<JComponent> toRepaint = new ArrayList<>();
        boolean busy = false;

        synchronized (STATES) {
            var iterator = STATES.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<JComponent, State> entry = iterator.next();
                JComponent c = entry.getKey();
                State s = entry.getValue();
                if (c == null) {
                    iterator.remove();
                    continue;
                }
                float delta = s.target - s.alpha;
                if (Math.abs(delta) <= EPSILON) {
                    s.alpha = s.target;
                } else {
                    s.alpha += delta * ALPHA_LERP;
                    busy = true;
                }
                if (s.alpha > EPSILON && c.isShowing()) {
                    toRepaint.add(c);
                    busy = true;
                }
            }
        }

        for (JComponent c : toRepaint) {
            c.repaint();
        }
        if (!busy) {
            stopTimer();
        }
    }

    private static void stopTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    /** 释放引擎（应用退出前调用，主要用于让 AWT 事件监听器不再持有引用） */
    public static void dispose() {
        stopTimer();
        if (pointerListener != null) {
            try {
                Toolkit.getDefaultToolkit().removeAWTEventListener(pointerListener);
            } catch (Throwable ignored) {
                // 退出阶段失败可忽略
            }
            pointerListener = null;
        }
        synchronized (STATES) {
            STATES.clear();
        }
    }
}
