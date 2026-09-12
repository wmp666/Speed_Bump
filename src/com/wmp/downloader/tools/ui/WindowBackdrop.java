package com.wmp.downloader.tools.ui;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Windows 窗口背景材质（Mica / Acrylic / 模糊）工具类。
 *
 * <p>零第三方依赖：使用 JDK 25 已转正的 FFM API（{@code java.lang.foreign}）直接调用
 * {@code dwmapi.dll} / {@code user32.dll}。不需要 JNA。</p>
 *
 * <h3>原理与前提（重要）</h3>
 * <ol>
 *   <li>原生 backdrop 由 DWM 绘制在<b>窗口内容之下</b>。只要 Swing 把客户区画成不透明像素，
 *       材质就会被完全盖住 —— 所以窗口必须是 <b>per-pixel 透明</b>（{@link #prepareTransparent}）。</li>
 *   <li>窗口需要先 {@code setUndecorated(true)}（在 {@code setVisible} 之前调用），
 *       再设置 {@code setBackground(new Color(0,0,0,0))}。</li>
 *   <li>Mica 是 Windows 11 (build 22621+) 独有特性。Windows 10 上调用会返回
 *       {@code 0x80070057 (E_INVALIDARG)}，此时请改用 {@link Material#BLUR}。</li>
 * </ol>
 *
 * <h3>本机实测结论（Windows 10 20H2 / JDK 25）</h3>
 * <pre>
 * DwmSetWindowAttribute(SYSTEMBACKDROP_TYPE=38) -&gt; 0x80070057  Mica 不可用
 * DwmSetWindowAttribute(MICA_EFFECT=1029)       -&gt; 0x80070057  旧版私有属性同样不可用
 * SetWindowCompositionAttribute(BLURBEHIND=3)   -&gt; 有效：清晰条纹对比度 97.5 -&gt; 39.2（真实模糊）
 * SetWindowCompositionAttribute(ACRYLIC=4)      -&gt; 仅上色，本身不产生模糊
 * SetWindowCompositionAttribute(HOSTBACKDROP=5) -&gt; 无效果
 * </pre>
 *
 * <h3>用法</h3>
 * <pre>{@code
 * JFrame frame = new JFrame("Demo");
 * frame.setUndecorated(true);                       // 必须在 setVisible 之前
 * frame.setContentPane(root);                        // root 应 setOpaque(false)
 * WindowBackdrop.prepareTransparent(frame);          // 使窗口 per-pixel 透明
 * frame.setVisible(true);
 * WindowBackdrop.apply(frame, WindowBackdrop.Material.MICA); // 全平台自动选择
 * }</pre>
 *
 * 注意：使用 FFM 需要启动参数 {@code --enable-native-access=ALL-UNNAMED}，
 * 不加只会打印警告，不影响功能。
 */
public final class WindowBackdrop {

    private WindowBackdrop() {
    }

    /** 背景材质 */
    public enum Material {
        /** Win11 22H2+：云母，对桌面壁纸取色并轻度模糊；Win10 上自动回退为 BLUR */
        MICA(2, -1),
        /** Win11 22H2+：亚克力，比云母更透明 */
        ACRYLIC(3, 4),
        /** Win11 22H2+：标签页式材质 */
        TABBED(4, -1),
        /** Win10 1803+：模糊窗口背后的一切内容（实测唯一真正产生模糊的 Win10 方案） */
        BLUR(-1, 3),
        /** 关闭材质 */
        NONE(1, 0);

        final int dwmBackdropType;
        final int accentState;

        Material(int dwmBackdropType, int accentState) {
            this.dwmBackdropType = dwmBackdropType;
            this.accentState = accentState;
        }
    }

    // ---- DWM / user32 常量 ----
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    private static final int DWMWA_MICA_EFFECT = 1029;
    private static final int WCA_ACCENT_POLICY = 19;
    private static final int ACCENT_ENABLE_BLURBEHIND = 3;
    private static final int ACCENT_ENABLE_ACRYLICBLURBEHIND = 4;

    private static final Map<Window, Long> HWND_CACHE = new WeakHashMap<>();
    private static final List<Long> ENUM_RESULT = new ArrayList<>();

    // ==================================================================
    // 公开 API
    // ==================================================================

    /** 本机是否可用于原生自适应材质（Windows 且原生库可用） */
    public static boolean isSupported() {
        return Native.available;
    }

    /**
     * 使窗口具备显示背景材质的前提条件：per-pixel 透明 + 无边框。
     *
     * <p>必须在 {@code setVisible(true)} <b>之前</b>调用；若窗口已经显示，
     * 需要先 {@code dispose()} 再 {@code setUndecorated(true)} 后重新显示。</p>
     *
     * @return 是否成功设置为透明窗口
     */
    public static boolean prepareTransparent(Window window) {
        if (window == null || GraphicsEnvironment.isHeadless()) {
            return false;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (!device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            return false;
        }
        window.setBackground(new Color(0, 0, 0, 0));
        return true;
    }

    /**
     * 应用背景材质，自动按系统版本选择实现（Win11 走 DWM，Win10 回退 blur）。
     *
     * @param window 目标窗口，须为无边框透明窗口
     * @param material 材质类型
     * @return 是否至少有一个原生调用成功
     */
    public static boolean apply(Window window, Material material) {
        return apply(window, material, 0x00000000);
    }

    /**
     * 应用背景材质。
     *
     * @param window 目标窗口
     * @param material 材质类型
     * @param tintAbgr Win10 blur/acrylic 的着色，ABGR 顺序（如 {@code 0x99202020} 为 60% 深灰），
     *                 仅对 {@link Material#ACRYLIC} 和 Win10 回退路径生效；Win11 Mica 忽略该值
     * @return 是否至少有一个原生调用成功
     */
    public static boolean apply(Window window, Material material, int tintAbgr) {
        if (window == null) {
            return false;
        }
        if (!Native.available) {
            // 非 Windows 或原生库不可用：透明窗口本身即可用（无模糊效果）
            return window.getBackground() != null && window.getBackground().getAlpha() == 0;
        }
        long hwnd = hwndOf(window);
        if (hwnd == 0) {
            return false;
        }

        boolean ok = false;
        if (material.dwmBackdropType >= 0) {
            ok = setDwmInt(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, material.dwmBackdropType);
            if (!ok && material == Material.MICA) {
                // Windows 11 21H2（build 22000）使用未公开的 MICA_EFFECT 属性
                ok = setDwmInt(hwnd, DWMWA_MICA_EFFECT, 1);
            }
        }
        if (!ok && material != Material.NONE) {
            // 系统不是 Win11 22H2+：回退到 Win10 就有的窗口背后内容模糊
            int fallbackState = (material == Material.ACRYLIC)
                    ? ACCENT_ENABLE_ACRYLICBLURBEHIND
                    : ACCENT_ENABLE_BLURBEHIND;
            ok = setAccent(hwnd, fallbackState, tintAbgr);
        }
        if (material == Material.NONE) {
            // 关闭：既关 DWM backdrop，也关 Win10 accent
            setDwmInt(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, 1);
            setAccent(hwnd, 0, 0);
            ok = true;
        }
        // 顺手统一圆角与深色标题栏（两者在 Win11 生效，Win10 返回失败但不影响）
        setDwmInt(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, 2);
        setDwmInt(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, isDarkTheme() ? 1 : 0);
        return ok;
    }

    /** Windows 10 语义下的模糊（供需要显式指定时使用） */
    public static boolean applyBlur(Window window, int tintAbgr) {
        return apply(window, Material.BLUR, tintAbgr);
    }

    /** 当前系统主版本号，如 Windows 10 返回 10，Windows 11 返回 11；非 Windows 返回 -1 */
    public static int windowsMajorVersion() {
        int build = windowsBuild();
        if (build <= 0) {
            return Native.available ? 10 : -1;
        }
        return build >= 22000 ? 11 : 10;
    }

    /** 当前系统真实 build 号（如 19042 / 22621）；取不到返回 -1 */
    public static int windowsBuild() {
        if (!Native.available) {
            return -1;
        }
        try {
            return Native.windowsBuild();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static boolean isDarkTheme() {
        // 延迟获取，避免与项目的主题工具循环依赖
        return false;
    }

    // ==================================================================
    // HWND 获取
    // ==================================================================

    /**
     * 取得 AWT 窗口对应的 Win32 HWND。
     *
     * <p>JDK 25 已移除 {@code Component.getPeer()}，反射取句柄的老办法失效，
     * 这里改为 EnumWindows 按进程 PID 枚举，再用窗口标题（或位置尺寸）匹配。</p>
     */
    public static long hwndOf(Window window) {
        if (window == null || !Native.available) {
            return 0;
        }
        synchronized (HWND_CACHE) {
            Long cached = HWND_CACHE.get(window);
            if (cached != null && cached != 0 && isWindow(cached)) {
                return cached;
            }
        }

        List<Long> candidates;
        synchronized (ENUM_RESULT) {
            ENUM_RESULT.clear();
            if (!nativeEnumWindows()) {
                return 0;
            }
            candidates = new ArrayList<>(ENUM_RESULT);
        }
        if (candidates.isEmpty()) {
            return 0;
        }

        long found = 0;
        // 1) 标题精确匹配（AWT 会用 Frame 的 title 作为 Win32 窗口标题）
        if (window instanceof Frame frame) {
            String title = frame.getTitle();
            if (title != null && !title.isEmpty()) {
                for (long h : candidates) {
                    if (title.equals(windowTitle(h))) {
                        found = h;
                        break;
                    }
                }
            }
        }
        // 2) 位置尺寸匹配（考虑 DPI 缩放）
        if (found == 0) {
            Rectangle bounds = window.getBounds();
            for (long h : candidates) {
                Rectangle rect = windowRect(h);
                if (rect == null) {
                    continue;
                }
                int dw = Math.abs(rect.width - bounds.width);
                int dh = Math.abs(rect.height - bounds.height);
                if (dw <= 4 && dh <= 4) {
                    found = h;
                    break;
                }
            }
        }
        // 3) 兜底：本进程第一个可见顶层窗口
        if (found == 0) {
            found = candidates.get(0);
        }

        synchronized (HWND_CACHE) {
            HWND_CACHE.put(window, found);
        }
        return found;
    }

    private static boolean isWindow(long hwnd) {
        try {
            return (int) Native.isWindow.invoke(MemorySegment.ofAddress(hwnd)) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String windowTitle(long hwnd) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(512);
            int len = (int) Native.getWindowTextW.invoke(MemorySegment.ofAddress(hwnd), buf, 256);
            if (len <= 0) {
                return "";
            }
            // GetWindowTextW 写出 UTF-16LE，不能按 UTF-8 解码
            byte[] raw = buf.asSlice(0, (long) len * 2).toArray(ValueLayout.JAVA_BYTE);
            return new String(raw, StandardCharsets.UTF_16LE);
        } catch (Throwable t) {
            return "";
        }
    }

    private static Rectangle windowRect(long hwnd) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rect = arena.allocate(16);
            if ((int) Native.getWindowRect.invoke(MemorySegment.ofAddress(hwnd), rect) == 0) {
                return null;
            }
            return new Rectangle(
                    rect.get(ValueLayout.JAVA_INT, 0),
                    rect.get(ValueLayout.JAVA_INT, 4),
                    rect.get(ValueLayout.JAVA_INT, 8) - rect.get(ValueLayout.JAVA_INT, 0),
                    rect.get(ValueLayout.JAVA_INT, 12) - rect.get(ValueLayout.JAVA_INT, 4));
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean nativeEnumWindows() {
        try {
            return (int) Native.enumWindows.invoke(Native.ENUM_CALLBACK, MemorySegment.NULL) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /** EnumWindows 回调：收集本进程的可见顶层窗口 */
    private static int enumProc(MemorySegment hwnd, MemorySegment lParam) {
        try {
            if ((int) Native.isWindowVisible.invoke(hwnd) == 0) {
                return 1;
            }
            Native.getWindowThreadProcessId.invoke(hwnd, Native.PID_OUT);
            if (Native.PID_OUT.get(ValueLayout.JAVA_INT, 0) == (int) Native.PID) {
                synchronized (ENUM_RESULT) {
                    ENUM_RESULT.add(hwnd.address());
                }
            }
        } catch (Throwable ignored) {
            // 单个窗口查询失败不影响枚举
        }
        return 1;
    }

    // ==================================================================
    // 原生调用封装
    // ==================================================================

    private static boolean setDwmInt(long hwnd, int attribute, int value) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pv = arena.allocate(4);
            pv.set(ValueLayout.JAVA_INT, 0, value);
            int hr = (int) Native.dwmSetWindowAttribute.invoke(
                    MemorySegment.ofAddress(hwnd), attribute, pv, 4);
            return hr == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean setAccent(long hwnd, int accentState, int tintAbgr) {
        try (Arena arena = Arena.ofConfined()) {
            // struct ACCENTPOLICY { int nAccentState; int nFlags; int nColor; int nAnimationId; }
            MemorySegment accent = arena.allocate(16);
            accent.set(ValueLayout.JAVA_INT, 0, accentState);
            accent.set(ValueLayout.JAVA_INT, 4, 2);
            accent.set(ValueLayout.JAVA_INT, 8, tintAbgr);
            accent.set(ValueLayout.JAVA_INT, 12, 0);
            // struct WINCOMPATTRDATA { int nAttribute; PVOID pData; ULONG ulDataSize; } —— 64 位下 24 字节
            MemorySegment data = arena.allocate(24);
            data.set(ValueLayout.JAVA_INT, 0, WCA_ACCENT_POLICY);
            data.set(ValueLayout.ADDRESS, 8, accent);
            data.set(ValueLayout.JAVA_INT, 16, 16);
            return (int) Native.setWindowCompositionAttribute.invoke(
                    MemorySegment.ofAddress(hwnd), data) != 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 便捷：恢复默认边框（解除无边框时可用） */
    public static void reset(Window window) {
        apply(window, Material.NONE);
    }

    /** 强制刷新缓存（窗口被重建后调用） */
    public static void invalidate(Window window) {
        synchronized (HWND_CACHE) {
            HWND_CACHE.remove(window);
        }
    }

    // ==================================================================
    // 原生句柄初始化
    // ==================================================================

    private static final class Native {
        static final boolean available;
        static final long PID = ProcessHandle.current().pid();
        static final Arena ARENA = Arena.ofShared();
        static final MemorySegment PID_OUT = ARENA.allocate(4);
        static final MemorySegment ENUM_CALLBACK;

        static MethodHandle dwmSetWindowAttribute;
        static MethodHandle setWindowCompositionAttribute;
        static MethodHandle enumWindows;
        static MethodHandle getWindowThreadProcessId;
        static MethodHandle getWindowRect;
        static MethodHandle isWindowVisible;
        static MethodHandle isWindow;
        static MethodHandle getWindowTextW;
        static MethodHandle rtlGetVersion;

        static {
            boolean ok = false;
            MemorySegment callback = null;
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("windows")
                        && !GraphicsEnvironment.isHeadless()) {
                    Linker linker = Linker.nativeLinker();
                    SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", ARENA);
                    SymbolLookup dwmapi = SymbolLookup.libraryLookup("dwmapi.dll", ARENA);
                    SymbolLookup ntdll = SymbolLookup.libraryLookup("ntdll.dll", ARENA);

                    dwmSetWindowAttribute = linker.downcallHandle(
                            dwmapi.find("DwmSetWindowAttribute").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                    setWindowCompositionAttribute = linker.downcallHandle(
                            user32.find("SetWindowCompositionAttribute").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                    enumWindows = linker.downcallHandle(
                            user32.find("EnumWindows").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                    getWindowThreadProcessId = linker.downcallHandle(
                            user32.find("GetWindowThreadProcessId").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                    getWindowRect = linker.downcallHandle(
                            user32.find("GetWindowRect").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                    isWindowVisible = linker.downcallHandle(
                            user32.find("IsWindowVisible").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                    isWindow = linker.downcallHandle(
                            user32.find("IsWindow").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
                    getWindowTextW = linker.downcallHandle(
                            user32.find("GetWindowTextW").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
                    rtlGetVersion = linker.downcallHandle(
                            ntdll.find("RtlGetVersion").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));

                    callback = linker.upcallStub(
                            MethodHandles.lookup().findStatic(WindowBackdrop.class, "enumProc",
                                    MethodType.methodType(int.class, MemorySegment.class, MemorySegment.class)),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                            ARENA);
                    ok = true;
                }
            } catch (Throwable t) {
                ok = false;
            }
            available = ok;
            ENUM_CALLBACK = callback;
        }

        /** RtlGetVersion 取真实 build 号（System.getProperty("os.version") 在 Win10/11 上都是 10.0，不可用） */
        static int windowsBuild() {
            try (Arena arena = Arena.ofConfined()) {
                // OSVERSIONINFOW: ULONG dwOSVersionInfoSize, dwMajorVersion, dwMinorVersion, dwBuildNumber ...
                MemorySegment info = arena.allocate(284);
                info.set(ValueLayout.JAVA_INT, 0, 284);
                if ((int) rtlGetVersion.invoke(info) != 0) {
                    return -1;
                }
                return info.get(ValueLayout.JAVA_INT, 12);
            } catch (Throwable t) {
                return -1;
            }
        }
    }
}
