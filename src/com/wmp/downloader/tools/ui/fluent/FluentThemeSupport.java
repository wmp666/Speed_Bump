package com.wmp.downloader.tools.ui.fluent;

import com.wmp.downloader.tools.ui.DynamicConverterTask;
import com.wmp.downloader.tools.ui.ThemeChanger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 让自绘组件跟随项目主题系统的桥梁。
 *
 * <h3>复用已有的主题回调，而不是另建一套</h3>
 * <p>本项目已有 {@link ThemeChanger#addInDynamicConverter}——组件注册一个
 * {@link DynamicConverterTask}，主题变化时被回调。本类直接挂接它，
 * 于是自绘组件不需要知道主题是怎么切的。</p>
 *
 * <h3>为什么还要自己维护一张弱引用表</h3>
 * <p>{@code DynamicConverterTask} 列表是强引用的，而本项目的任务卡片、
 * 拓展列表项都是<b>反复创建与销毁</b>的。如果每个实例都往 ThemeChanger 里塞一个任务，
 * 被销毁的组件会永远留在列表里——典型的内存泄漏，而且泄漏量随下载任务数增长。</p>
 *
 * <p>所以这里只向 ThemeChanger 注册<b>一个</b>任务，真正的组件存在
 * {@link WeakHashMap} 里（键为组件、值为共享常量 {@code Boolean.TRUE}，
 * 值不会反向强引用键，条目能在组件被回收后自动过期）。</p>
 *
 * <p>另一个坑是「弱引用回调」：若把 {@code this::repaint} 这样的 lambda 单独放进弱引用容器，
 * 它除了容器之外没有任何强引用，会被立刻回收，监听器静默失效。
 * 把<b>组件本身</b>当键就绕开了这个问题。</p>
 */
public final class FluentThemeSupport {

    private FluentThemeSupport() {
    }

    /** 关心主题变化的组件 */
    public interface ThemeAware {
        /** 主题或强调色变化时调用（实现里通常只需 {@code repaint()}） */
        void onThemeChanged();
    }

    private static final Map<ThemeAware, Boolean> REGISTRY = new WeakHashMap<>();
    private static volatile boolean hooked;

    /** 注册一个组件（弱引用，无需手动注销） */
    public static void register(ThemeAware aware) {
        if (aware == null) {
            return;
        }
        synchronized (REGISTRY) {
            REGISTRY.put(aware, Boolean.TRUE);
        }
    }

    /** 立即注销（通常不需要） */
    public static void unregister(ThemeAware aware) {
        if (aware == null) {
            return;
        }
        synchronized (REGISTRY) {
            REGISTRY.remove(aware);
        }
    }

    /** 当前注册数量（调试用） */
    public static int registeredCount() {
        synchronized (REGISTRY) {
            return REGISTRY.size();
        }
    }

    /**
     * 挂接到 {@link ThemeChanger} 的动态转换机制。可重复调用。
     */
    public static void install() {
        if (hooked) {
            return;
        }
        try {
            ThemeChanger.addInDynamicConverter(FluentThemeSupport::refreshAll);
            hooked = true;
        } catch (Throwable ignored) {
            // ThemeChanger 尚未就绪时不致命：组件下一次重绘自然会用新颜色
        }
    }

    /** 通知所有注册组件刷新（由 ThemeChanger 回调） */
    public static void refreshAll() {
        List<ThemeAware> snapshot;
        synchronized (REGISTRY) {
            snapshot = new ArrayList<>(REGISTRY.keySet());
        }
        for (ThemeAware aware : snapshot) {
            if (aware == null) {
                continue;
            }
            try {
                aware.onThemeChanged();
            } catch (Throwable ignored) {
                // 单个组件刷新失败不影响其它组件
            }
        }
    }
}
