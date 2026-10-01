package com.wmp.speed_bump.platform.ui.swing.window;

import com.wmp.downloader.tools.file.DataControl;
import com.wmp.speed_bump.common.background.tool.platform.AutoStart;
import com.wmp.downloader.tools.ui.ThemeChanger;
import org.apache.log4j.Logger;

import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import java.awt.Color;
import java.util.Locale;

/**
 * 欢迎页的配置读写收口。
 *
 * <p>各设置页只跟这里打交道，不直接拼配置键与缺省值——缺省值散在两三个类里，
 * 早晚会出现「这一页写 12、那一页读 14」这种对不上的情况。</p>
 */
final class WelcomeSettings {

    private static final Logger logger = Logger.getLogger(WelcomeSettings.class);

    static final String DEFAULT_THEME = "Mac Light";
    static final String DEFAULT_ACCENT = "05E666";
    static final String DEFAULT_FONT = "Microsoft YaHei";
    static final int DEFAULT_FONT_SIZE = 12;

    /** 字号上下限。上限只是个「别把窗口撑到屏幕外」的保险，不是产品约束 */
    static final int MIN_FONT_SIZE = 1;
    static final int MAX_FONT_SIZE = 36;

    private WelcomeSettings() {
    }

    // ==================================================================
    // 配置
    // ==================================================================

    static String theme() {
        return DataControl.get("theme", DEFAULT_THEME);
    }

    static String accent() {
        return DataControl.get("accent_color", DEFAULT_ACCENT);
    }

    static String font() {
        return DataControl.get("Font", DEFAULT_FONT);
    }

    /** 当前字体大小；负数会让 Font 构造失败，所以至少抬到 1 */
    static int fontSize() {
        Object raw = DataControl.get("FontSize", DEFAULT_FONT_SIZE);
        int size = raw instanceof Number number ? number.intValue() : DEFAULT_FONT_SIZE;
        return Math.max(MIN_FONT_SIZE, size);
    }

    static boolean squareCorners() {
        return Boolean.TRUE.equals(DataControl.get("is_use_square_component", Boolean.TRUE));
    }

    static boolean checkUpdateOnStart() {
        return Boolean.TRUE.equals(DataControl.get("is_start_check_update", Boolean.TRUE));
    }

    static String downloadPath() {
        return DataControl.getDownloadFilePath().getAbsolutePath();
    }

    /**
     * 当前生效的界面语言代码：配置里有就用配置的，否则用系统语言。
     *
     * <p>返回「解析后的代码」而不是配置原值，是为了让快照能安全回滚：
     * 新用户的配置里根本没有这个键，存 {@code null} 回滚时反而会写坏配置
     * （{@code DataControl} 的加工逻辑会对值直接调 {@code toString()}）。</p>
     */
    static String languageCode() {
        String saved = DataControl.get("laug", null);
        if (saved != null && !saved.isBlank()) {
            return saved;
        }
        Locale locale = Locale.getDefault();
        String code = locale.getLanguage();
        if (!locale.getCountry().isEmpty()) {
            code = code + "_" + locale.getCountry();
        }
        return code;
    }

    /**
     * 字号调节模型。
     *
     * <p>上限会在「配置里本来就比上限大」时临时放宽：{@link SpinnerNumberModel} 不接受
     * 超出区间的初值，若直接夹到上限，用户只是翻到这一页、然后点「跳过」，
     * 配置里的字号就被悄悄改小了。</p>
     */
    static SpinnerNumberModel fontSizeModel() {
        int current = fontSize();
        return new SpinnerNumberModel(current, MIN_FONT_SIZE, Math.max(MAX_FONT_SIZE, current), 1);
    }

    // ==================================================================
    // 外观
    // ==================================================================

    /** 重新套用当前主题（强调色、字体、字号、弧度都挂在这次重装里） */
    static void reapplyTheme() {
        ThemeChanger.easyChanger(theme());
    }

    /** 错误提示用的红色：优先取当前外观的错误色，取不到就用固定值 */
    static Color errorColor() {
        Color color = UIManager.getColor("Component.error.focusedBorderColor");
        return color != null ? color : new Color(0xC4, 0x2B, 0x1C);
    }

    // ==================================================================
    // 开机自启动
    // ==================================================================

    /**
     * 开机自启动在当前环境能不能设置。
     *
     * <p>它依赖 {@code jpackage.app-path}（由打包器写入），从 IDE 或裸 jar 启动时读不到，
     * 此时写注册表也无从下手，所以调用方应把开关置灰。</p>
     */
    static boolean autoStartUsable() {
        try {
            return DataControl.getAppPath() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 读系统里的开机自启动状态；查询本身失败时当作「未开启」 */
    static boolean autoStartEnabled() {
        try {
            return AutoStart.isAutoStart();
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================================================================
    // 颜色字符串
    // ==================================================================

    /** 去掉可能存在的 {@code #} 前缀：配置里带不带它都有人写 */
    static String normalizeHex(String hex) {
        if (hex == null) {
            return "";
        }
        return hex.startsWith("#") ? hex.substring(1) : hex;
    }

    static Color parseColor(String hex, Color fallback) {
        if (hex == null) {
            return fallback;
        }
        String value = normalizeHex(hex);
        try {
            if (value.length() == 6) {
                return new Color(Integer.parseInt(value, 16) | 0xFF000000, false);
            }
            if (value.length() == 8) {
                return new Color((int) Long.parseLong(value, 16), true);
            }
        } catch (NumberFormatException ignored) {
            //配置里写了非法色值，退回兜底色
        }
        return fallback;
    }

    // ==================================================================
    // 快照
    // ==================================================================

    /**
     * 打开欢迎页时的配置快照，「跳过」时用它回滚。
     *
     * <p>存的是「按缺省值补全后」的取值，不是原始配置里的裸值。新用户的配置里这几个键
     * 本来都不存在，回滚时会把缺省值显式写进去——效果与「键不存在」完全一致，
     * 却避开了「拿 {@code null} 回滚反而写坏配置」的问题。</p>
     *
     * <p>开机自启动不在 {@code DataControl} 里，它写的是注册表 / LaunchAgent / desktop 文件，
     * 所以单独记一份系统状态。</p>
     */
    record Snapshot(String theme, String accent, String laug, String font, int fontSize,
                    boolean square, boolean checkUpdate, String path, boolean autoStart) {
    }

    static Snapshot capture() {
        return new Snapshot(
                theme(),
                accent(),
                languageCode(),
                font(),
                fontSize(),
                squareCorners(),
                checkUpdateOnStart(),
                downloadPath(),
                autoStartUsable() && autoStartEnabled());
    }

    static void restore(Snapshot snapshot) {
        DataControl.put("theme", snapshot.theme());
        DataControl.put("accent_color", snapshot.accent());
        DataControl.put("DownloadFilePath", snapshot.path());
        DataControl.put("Font", snapshot.font());
        DataControl.put("FontSize", snapshot.fontSize());
        DataControl.put("is_use_square_component", snapshot.square());
        DataControl.put("is_start_check_update", snapshot.checkUpdate());
        //laug 会顺带重设 Locale.setDefault，必须走 put
        DataControl.put("laug", snapshot.laug());
        //开机自启动是系统层面的改动，跳过时一并还原
        if (autoStartUsable()) {
            try {
                AutoStart.setAutoStart(snapshot.autoStart());
            } catch (Throwable t) {
                logger.error("还原开机自启动失败", t);
            }
        }
        reapplyTheme();
    }
}
