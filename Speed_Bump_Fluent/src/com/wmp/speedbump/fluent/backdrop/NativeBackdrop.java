package com.wmp.speedbump.fluent.backdrop;

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
 * Windows 原生背景材质（Mica / Acrylic / Blur）与窗口装饰的调用层。
 *
 * <p>零第三方依赖：使用 JDK 25 已转正的 FFM API（{@code java.lang.foreign}）直接调用
 * {@code dwmapi.dll} / {@code user32.dll} / {@code ntdll.dll}。不需要 JNA，也不需要 C 代码。</p>
 *
 * <h3>关键约束（踩过的坑，改代码前务必读）</h3>
 * <ol>
 *   <li><b>窗口必须是逐像素透明（per-pixel translucent）的无边框窗口</b>，且
 *       {@code setUndecorated(true)} 与 {@code setBackground(透明)} 都要在 {@code setVisible(true)}
 *       <b>之前</b>完成。否则材质被不透明客户区完全盖住。</li>
 *   <li><b>逐像素透明窗口是 layered 窗口</b>。DWM 的 Mica/Acrylic 需要非 layered 窗口才会真正渲染，
 *       而在 layered 窗口上 {@code DwmSetWindowAttribute(SYSTEMBACKDROP_TYPE)} 会<b>返回成功却什么也不画</b>
 *       —— 这个「假成功」会绕过所有回退分支，最终表现为「材质开了但没效果」。
 *       因此 {@link #forceBlurInsteadOfMica} 默认为 {@code true}，强制走
 *       {@code SetWindowCompositionAttribute} 模糊路径（Win10 1803+ 与 Win11 都有效）。</li>
 *   <li>Mica 是 Windows 11 22621+ 特性；在 Windows 10 上调用返回
 *       {@code 0x80070057 (E_INVALIDARG)}。</li>
 * </ol>
 *
 * <h3>本机实测（Windows 10 19044 / JDK 25）</h3>
 * <pre>
 * DwmSetWindowAttribute(SYSTEMBACKDROP_TYPE=38) -&gt; 0x80070057  Mica 不可用
 * SetWindowCompositionAttribute(BLURBEHIND=3)   -&gt; 有效：真实模糊
 * SetWindowCompositionAttribute(ACRYLICBLUR=4)  -&gt; 仅着色，本身不产生模糊
 * </pre>
 *
 * <p>使用 FFM 需要启动参数 {@code --enable-native-access=ALL-UNNAMED}，不加只打印警告，不影响功能。</p>
 */
public final class NativeBackdrop {

    private NativeBackdrop() {
    }

    // ---- DWM / user32 常量 ----
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    private static final int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    private static final int DWMWA_SYSTEMBACKDROP_TYPE = 38;
    private static final int DWMWA_MICA_EFFECT = 1029;
    private static final int WCA_ACCENT_POLICY = 19;

    private static final Map<Window, Long> HWND_CACHE = new WeakHashMap<>();
    private static final List<Long> ENUM_RESULT = new ArrayList<>();

    /**
     * 是否跳过 DWM 的 Mica/Acrylic 而强制使用 {@code SetWindowCompositionAttribute} 模糊。
     *
     * <p><b>默认为 {@code true}</b>，原因见类注释第 2 条：layered 窗口上 DWM 材质会「假成功」。
     * 如果将来承载材质的窗口改成非 layered（例如只让 DWM 画圆角 + 自己画内容），
     * 可以设为 {@code false} 启用真正的 Mica。</p>
     */
    public static boolean forceBlurInsteadOfMica = true;

    // ==================================================================
    // 能力探测
    // ==================================================================

    /** 原生库是否可用（Windows 且加载成功） */
    public static boolean isSupported() {
        return Native.available;
    }

    /** 系统真实 build 号（如 19044 / 22621）；取不到返回 {@code -1} */
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

    /** 系统主版本：Windows 11 返回 11，Windows 10 返回 10，非 Windows 返回 {@code -1} */
    public static int windowsMajorVersion() {
        int build = windowsBuild();
        if (build <= 0) {
            return Native.available ? 10 : -1;
        }
        return build >= 22000 ? 11 : 10;
    }

    /**
     * 按当前系统挑一个「能真正出效果」的材质。
     *
     * @param requested 期望材质
     * @return 实际可用材质（可能被降级）
     */
    public static BackdropMaterial resolve(BackdropMaterial requested) {
        if (!Native.available || requested == BackdropMaterial.NONE) {
            return BackdropMaterial.NONE;
        }
        int build = windowsBuild();
        if (build < 17134) {
            // 17134 之前没有 SetWindowCompositionAttribute
            return BackdropMaterial.NONE;
        }
        if (forceBlurInsteadOfMica && (requested == BackdropMaterial.MICA
                || requested == BackdropMaterial.MICA_ALT)) {
            // 本项目用 Swing，Swing 的透明窗口必然是 layered，Mica 无法渲染
            return BackdropMaterial.BLUR;
        }
        return requested;
    }

    /** 材质是否在当前系统上真正可用（用于设置页的可用性提示） */
    public static boolean isAvailable(BackdropMaterial material) {
        return resolve(material) != BackdropMaterial.NONE || material == BackdropMaterial.NONE;
    }

    /** 一次性描述当前环境，供诊断页展示 */
    public static String describeEnvironment() {
        return "原生材质可用: " + isSupported()
                + "\nWindows 版本: " + windowsMajorVersion()
                + "\nbuild: " + windowsBuild()
                + "\n系统主题色: " + toHex(systemAccent());
    }

    // ==================================================================
    // 透明窗口准备
    // ==================================================================

    /**
     * 让窗口具备显示材质的前提：逐像素透明。
     *
     * <p>必须在 {@code setVisible(true)} 之前调用。</p>
     *
     * @return 是否成功（不支持透明时会返回 {@code false}，调用方应退化为不透明背景）
     */
    public static boolean prepareTransparent(Window window) {
        if (window == null || GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (!device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
        window.setBackground(new Color(0, 0, 0, 0));
        return true;
    }

    // ==================================================================
    // 材质应用
    // ==================================================================

    /** 应用材质（不着色） */
    public static boolean apply(Window window, BackdropMaterial material) {
        return apply(window, material, 0x00000000);
    }

    /**
     * 应用材质。
     *
     * @param window   目标窗口（须为无边框透明窗口）
     * @param material 材质
     * @param tintAbgr Win10 路径的着色，ABGR 顺序（如 {@code 0x99202020} 为 60% 深灰）
     * @return 是否至少一次原生调用成功
     */
    public static boolean apply(Window window, BackdropMaterial material, int tintAbgr) {
        if (window == null) {
            return false;
        }
        if (!Native.available) {
            // 非 Windows：透明窗口本身仍然有效，只是没有模糊
            return window.getBackground() != null && window.getBackground().getAlpha() == 0;
        }
        long hwnd = hwndOf(window);
        if (hwnd == 0) {
            return false;
        }

        boolean ok = false;
        if (material == BackdropMaterial.NONE) {
            setDwmInt(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, 1);
            setAccent(hwnd, 0, 0);
            ok = true;
        } else if (!forceBlurInsteadOfMica && material.dwmBackdropType >= 0) {
            ok = setDwmInt(hwnd, DWMWA_SYSTEMBACKDROP_TYPE, material.dwmBackdropType);
            if (!ok && material == BackdropMaterial.MICA) {
                // Windows 11 21H2 (build 22000) 用的是未公开属性
                ok = setDwmInt(hwnd, DWMWA_MICA_EFFECT, 1);
            }
        }
        if (!ok && material != BackdropMaterial.NONE) {
            int state = material.accentState >= 0 ? material.accentState : 3;
            ok = setAccent(hwnd, state, tintAbgr);
        }

        // 顺手统一下窗口装饰：圆角与深色标题栏（Win11 生效，Win10 返回失败但不影响）
        setDwmInt(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, 2);
        setDwmInt(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, isDarkTheme() ? 1 : 0);
        return ok;
    }

    /** 只更新 DWM 深色标题栏标志（主题切换时调用，代价很小） */
    public static boolean applyImmersiveDarkMode(Window window, boolean dark) {
        if (!Native.available || window == null) {
            return false;
        }
        long hwnd = hwndOf(window);
        return hwnd != 0 && setDwmInt(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, dark ? 1 : 0);
    }

    /** Windows 11 上的窗口圆角偏好：0=默认 1=不要圆角 2=圆角 3=小圆角 */
    public static boolean applyCornerPreference(Window window, int preference) {
        if (!Native.available || window == null) {
            return false;
        }
        long hwnd = hwndOf(window);
        return hwnd != 0 && setDwmInt(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, preference);
    }

    /** 读取系统主题色（DWM 着色色，可能受「透明效果」开关影响） */
    public static Color systemAccent() {
        if (!Native.available) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment out = arena.allocate(4);
            MemorySegment opaque = arena.allocate(4);
            int hr = (int) Native.dwmGetColorizationColor.invoke(out, opaque);
            if (hr != 0) {
                return null;
            }
            int abgr = out.get(ValueLayout.JAVA_INT, 0);
            // 返回的是 0xAABBGGRR
            int r = abgr & 0xFF;
            int g = (abgr >> 8) & 0xFF;
            int b = (abgr >> 16) & 0xFF;
            return new Color(r, g, b);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 强制刷新句柄缓存（窗口被销毁重建后调用） */
    public static void invalidate(Window window) {
        synchronized (HWND_CACHE) {
            HWND_CACHE.remove(window);
        }
    }

    // ==================================================================
    // HWND 获取
    // ==================================================================

    /**
     * 取 AWT 窗口对应的 Win32 HWND。
     *
     * <p>JDK 25 移除了 {@code Component.getPeer()}，反射取句柄的办法失效。
     * 这里改为 EnumWindows 按 PID 枚举本进程顶层窗口，先按标题精确匹配，再按尺寸兜底。</p>
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
        if (found == 0) {
            Rectangle bounds = window.getBounds();
            for (long h : candidates) {
                Rectangle rect = windowRect(h);
                if (rect == null) {
                    continue;
                }
                if (Math.abs(rect.width - bounds.width) <= 4 && Math.abs(rect.height - bounds.height) <= 4) {
                    found = h;
                    break;
                }
            }
        }
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
        if (Native.setWindowCompositionAttribute == null) {
            return false;
        }
        try (Arena arena = Arena.ofConfined()) {
            // struct ACCENTPOLICY { int nAccentState; int nFlags; int nColor; int nAnimationId; }
            MemorySegment accent = arena.allocate(16);
            accent.set(ValueLayout.JAVA_INT, 0, accentState);
            accent.set(ValueLayout.JAVA_INT, 4, 2);
            accent.set(ValueLayout.JAVA_INT, 8, tintAbgr);
            accent.set(ValueLayout.JAVA_INT, 12, 0);
            // struct WINCOMPATTRDATA { int nAttribute; PVOID pData; ULONG ulDataSize; }
            // 64 位下因指针对齐共占 24 字节
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

    /**
     * 主题是否深色。
     *
     * <p>刻意不直接依赖 {@code FluentTheme}：调用方通过 {@link #darkThemeSupplier} 注入，
     * 避免 backdrop 层与 theme 层形成循环依赖。</p>
     */
    private static java.util.function.BooleanSupplier darkThemeSupplier = () -> false;

    public static void setDarkThemeSupplier(java.util.function.BooleanSupplier supplier) {
        if (supplier != null) {
            darkThemeSupplier = supplier;
        }
    }

    private static boolean isDarkTheme() {
        try {
            return darkThemeSupplier.getAsBoolean();
        } catch (Throwable t) {
            return false;
        }
    }

    private static String toHex(Color c) {
        return c == null ? "未知" : String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
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
        static MethodHandle dwmGetColorizationColor;
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
                    dwmGetColorizationColor = optionalHandle(linker, dwmapi, "DwmGetColorizationColor",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));
                    // SetWindowCompositionAttribute 是 Win10 1803+ 的未公开 API，
                    // 旧系统上必须可选加载，否则会让整个类降级
                    setWindowCompositionAttribute = optionalHandle(linker, user32, "SetWindowCompositionAttribute",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS));
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
                            MethodHandles.lookup().findStatic(NativeBackdrop.class, "enumProc",
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

        /** 可选加载：找不到该导出函数时返回 {@code null}，让调用方按降级路径处理 */
        private static MethodHandle optionalHandle(Linker linker, SymbolLookup lookup,
                                                   String name, FunctionDescriptor descriptor) {
            return lookup.find(name).map(symbol -> {
                try {
                    return linker.downcallHandle(symbol, descriptor);
                } catch (Throwable t) {
                    return null;
                }
            }).orElse(null);
        }

        /**
         * RtlGetVersion 取真实 build 号。
         *
         * <p>{@code System.getProperty("os.version")} 在 Win10/11 上都返回 {@code 10.0}，不可用。</p>
         */
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
