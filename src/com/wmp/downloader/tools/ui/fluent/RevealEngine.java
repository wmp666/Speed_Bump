package com.wmp.downloader.tools.ui.fluent;

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
 * Reveal Highlight（揭示高亮）引擎 —— WinUI / Fluent Design 的标志性光晕效果。
 *
 * <h3>它做什么</h3>
 * <p>指针在控件上移动时，在「背景之上、内容之下」绘制一团跟随指针的柔和光斑，
 * 并带渐变淡入淡出。它暗示「这里可以交互」，是 Windows 10/11 上 Fluent 应用最容易被感知的细节。</p>
 *
 * <h3>为什么用全局指针模型，而不是给每个组件挂 MouseListener</h3>
 * <p>含子组件的容器（卡片、列表项）在指针移入其子组件时会收到 {@code mouseExited}，
 * 移回空白处又收到 {@code mouseEntered}，会造成光晕闪烁甚至熄灭。
 * 这里改为监听全局 AWT 鼠标事件，用<b>组件边界命中</b>判定指针压在哪些控件上，
 * 因此卡片与其内部按钮可以同时亮起——这正是 WinUI 的行为。</p>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * // 1) 组件构造时注册一次
 * RevealEngine.register(this);
 *
 * // 2) 在背景之后、内容之前绘制
 * Shape clip = FluentPainting.roundRect(this, 0, radius);
 * FluentPainting.fill(g2, clip, FluentColors.control());
 * RevealEngine.paint(this, g2, clip);
 * }</pre>
 *
 * <p>组件被 GC 后自动从表中消失（{@link WeakHashMap}），无需手动注销。
 * 通过 {@link #setEnabled(boolean)} 可全局关闭（例如低性能机器上）。</p>
 */
public final class RevealEngine {

    private RevealEngine() {
    }

    /** 全局开关 */
    private static boolean enabled = true;

    /** 光斑半径下限（px）；实际半径取「该值与组件高度 × 2.5」中的较大者 */
    private static float minRadius = 90f;

    /** 指针追随的插值系数：越大越跟手，越小越「拖尾」 */
    private static final float POINTER_LERP = 0.32f;

    /** 透明度插值系数 */
    private static final float ALPHA_LERP = 0.22f;

    /** 认为动画结束的阈值 */
    private static final float EPSILON = 0.01f;

    /** 单个组件的光晕状态 */
    private static final class State {
        float alpha;
        float target;
        float px;
        float py;
        boolean hasPointer;
    }

    private static final Map<JComponent, State> STATES = new WeakHashMap<>();
    private static Timer timer;
    private static AWTEventListener pointerListener;

    // ==================================================================
    // 开关
    // ==================================================================

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * 全局开关。
     *
     * <p>关闭时只把动画状态归零，<b>不清空注册表</b>：注册是「组件声明自己需要光晕」，
     * 与「此刻是否启用光晕」是两件事。早期实现里这里做了 {@code STATES.clear()}，
     * 结果全局开关一次就再也开不回来了——所有组件被静默注销。
     * 这个缺陷是离屏自检（先关引擎测基线、再开引擎测光晕）暴露出来的。</p>
     */
    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            synchronized (STATES) {
                for (State s : STATES.values()) {
                    s.alpha = 0f;
                    s.target = 0f;
                    s.hasPointer = false;
                }
            }
            stopTimer();
        }
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

    /** 已注册组件数量（调试用） */
    public static int registeredCount() {
        synchronized (STATES) {
            return STATES.size();
        }
    }

    /** 该组件是否已注册（调试与自检用） */
    public static boolean isRegistered(JComponent component) {
        if (component == null) {
            return false;
        }
        synchronized (STATES) {
            return STATES.containsKey(component);
        }
    }

    /**
     * <b>仅供离屏自检与调试使用</b>：直接置入指针状态与不透明度，跳过全局鼠标事件。
     *
     * <p>存在的理由：光晕依赖真实指针位置，而离屏渲染没有指针，
     * 于是这个效果就成了「无法自动验证」的部分。给一个明确的测试接缝，
     * 比让它在回归测试里变成盲区要好。</p>
     *
     * @param component 已注册的组件
     * @param x         组件内的指针 x
     * @param y         组件内的指针 y
     */
    public static void simulatePointer(JComponent component, int x, int y) {
        if (component == null) {
            return;
        }
        synchronized (STATES) {
            State s = STATES.get(component);
            if (s == null) {
                return;
            }
            s.px = x;
            s.py = y;
            s.hasPointer = true;
            s.target = 1f;
            s.alpha = 1f;
        }
        component.repaint();
    }

    /** <b>仅供离屏自检与调试使用</b>：清除全部指针状态 */
    public static void clearSimulatedPointer() {
        synchronized (STATES) {
            for (State s : STATES.values()) {
                s.alpha = 0f;
                s.target = 0f;
                s.hasPointer = false;
            }
        }
        stopTimer();
    }

    // ==================================================================
    // 绘制
    // ==================================================================

    /**
     * 绘制光晕。必须在组件背景填充<b>之后</b>、内容绘制<b>之前</b>调用。
     *
     * @param component 目标组件
     * @param g         可安全修改的 Graphics2D（方法内部自行 create/dispose）
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
        // 指针在组件外时把光斑中心夹到边界内，避免光晕整块消失
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
            // 半径退化等极端情况直接跳过，不影响组件本体
        } finally {
            g2.dispose();
        }
    }

    /** 当前主题下的光晕色 */
    private static Color glowColor() {
        // 浅色主题：白色光晕能提亮卡面；深色主题：极低透明度，避免「糊一层灰」
        return FluentColors.isDark()
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
            timer = new Timer(FluentMetrics.TICK, e -> tick());
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

    /** 释放引擎（应用退出前调用，让 AWT 事件监听器不再持有引用） */
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
