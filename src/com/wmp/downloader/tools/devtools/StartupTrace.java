package com.wmp.downloader.tools.devtools;

/**
 * 启动分步耗时追踪。
 *
 * <h3>为什么要这个</h3>
 * <p>GUI 程序卡在启动阶段时，界面上什么都没有、也不抛异常，只能靠「最后打印到哪一步」定位；
 * 而线程转储在受限环境里未必拿得到（{@code jcmd} 依赖 Attach 机制，可能被沙箱拒绝）。
 * 这个零依赖的分步日志是替代方案。</p>
 *
 * <h3>为什么不做成常开的重日志</h3>
 * <p>默认开着，但只输出十来行、每行一次 {@code println}——启动一次只看一次，
 * 换来的是「启动慢了 3 秒，慢在哪一步」这种问题不用再靠猜。
 * 需要彻底静默时用 {@code -Dspeedbump.trace.startup=false}。</p>
 */
public final class StartupTrace {

    private StartupTrace() {
    }

    private static final boolean ENABLED =
            !"false".equalsIgnoreCase(System.getProperty("speedbump.trace.startup", "true"));

    private static final long START = System.nanoTime();
    private static long last = START;

    /** 是否启用 */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /** 记一步（无附加信息） */
    public static void step(String what) {
        step(what, null);
    }

    /**
     * 记一步。
     *
     * @param what   步骤名
     * @param detail 附加信息，可为 {@code null}
     */
    public static void step(String what, Object detail) {
        long now = System.nanoTime();
        if (!ENABLED) {
            last = now;
            return;
        }
        String suffix = detail == null ? "" : "　[" + detail + "]";
        System.out.printf("[启动] %-22s 本步 %7.1f ms | 累计 %7.1f ms%s%n",
                what,
                (now - last) / 1_000_000.0,
                (now - START) / 1_000_000.0,
                suffix);
        System.out.flush();
        last = now;
    }

    /** 从此刻重新开始计时（例如进入主循环之前） */
    public static void reset() {
        last = System.nanoTime();
    }

    /** 距启动的总耗时（毫秒） */
    public static double elapsedMs() {
        return (System.nanoTime() - START) / 1_000_000.0;
    }
}
