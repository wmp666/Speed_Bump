package com.wmp.speedbump.fluent.backdrop;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * Windows 注册表只读访问（FFM 直调 {@code advapi32.dll}）。
 *
 * <h3>为什么不用 {@code reg query} 子进程</h3>
 * <p>最初的实现是 {@code new ProcessBuilder("reg", "query", ...)}。它在实测中要花
 * <b>11 秒</b>——而这段代码是为了「跟随系统主题」服务的，会在 EDT 上同步执行，
 * 也就是说用户一切到「跟随系统」，界面会毫无提示地冻结十几秒。</p>
 *
 * <p>换成直接调用 {@code RegGetValueW} 之后，同样的读取只需要<b>不到 1 毫秒</b>，
 * 而且不需要任何第三方库——与本项目调用 DWM 的方式一致，都是 FFM 直调。</p>
 *
 * <p>只实现「读一个 DWORD」这一个用途，不追求完整的注册表 API 封装。
 * 若 FFM 不可用（非 Windows 等），调用方应自行降级。</p>
 */
public final class WindowsRegistry {

    private WindowsRegistry() {
    }

    /** {@code HKEY_CURRENT_USER} 的固定句柄值 */
    public static final long HKEY_CURRENT_USER = 0x80000001L;

    /** {@code RRF_RT_REG_DWORD}：只接受 REG_DWORD 类型 */
    private static final int RRF_RT_REG_DWORD = 0x00000010;

    /** 读取失败（键或值不存在、类型不符、接口不可用）时的返回值 */
    public static final int NOT_FOUND = Integer.MIN_VALUE;

    /**
     * 读取一个 REG_DWORD。
     *
     * @param rootKey    根键（{@link #HKEY_CURRENT_USER} 等）
     * @param subKey     子键路径，例如 {@code "Software\\Microsoft\\...\\Personalize"}
     * @param valueName  值名
     * @return 值；读取失败返回 {@link #NOT_FOUND}
     */
    public static int readInt(long rootKey, String subKey, String valueName) {
        if (!Native.available || subKey == null || valueName == null) {
            return NOT_FOUND;
        }
        try (Arena arena = Arena.ofConfined()) {
            // W 系列 API 要求 UTF-16LE 字符串，不能用默认的 UTF-8 编码
            MemorySegment subKeySegment = arena.allocateFrom(subKey, StandardCharsets.UTF_16LE);
            MemorySegment valueSegment = arena.allocateFrom(valueName, StandardCharsets.UTF_16LE);
            MemorySegment data = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment size = arena.allocate(ValueLayout.JAVA_INT);
            size.set(ValueLayout.JAVA_INT, 0, 4);

            int status = (int) Native.regGetValue.invoke(
                    MemorySegment.ofAddress(rootKey),
                    subKeySegment,
                    valueSegment,
                    RRF_RT_REG_DWORD,
                    MemorySegment.NULL,   // 不关心类型
                    data,
                    size);
            if (status != 0) {
                return NOT_FOUND;
            }
            return data.get(ValueLayout.JAVA_INT, 0);
        } catch (Throwable t) {
            return NOT_FOUND;
        }
    }

    /** 原生接口是否可用 */
    public static boolean isAvailable() {
        return Native.available;
    }

    private static final class Native {
        static final boolean available;
        static final MethodHandle regGetValue;

        static {
            MethodHandle handle = null;
            boolean ok = false;
            try {
                if (System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                    Linker linker = Linker.nativeLinker();
                    SymbolLookup advapi32 = SymbolLookup.libraryLookup("advapi32.dll", Arena.ofShared());
                    // 优先用 RegGetValueW（Vista+），它自带类型过滤，比 RegQueryValueExW 省事
                    handle = linker.downcallHandle(
                            advapi32.find("RegGetValueW").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,   // HKEY
                                    ValueLayout.ADDRESS,   // lpSubKey
                                    ValueLayout.ADDRESS,   // lpValue
                                    ValueLayout.JAVA_INT,  // dwFlags
                                    ValueLayout.ADDRESS,   // pdwType
                                    ValueLayout.ADDRESS,   // pvData
                                    ValueLayout.ADDRESS)); // pcbData
                    ok = true;
                }
            } catch (Throwable t) {
                ok = false;
            }
            available = ok;
            regGetValue = handle;
        }
    }
}
