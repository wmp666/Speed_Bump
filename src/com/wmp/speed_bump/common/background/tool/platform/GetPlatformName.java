package com.wmp.speed_bump.common.background.tool.platform;

import java.util.Locale;

public class GetPlatformName {
    private static final String OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);

    public static boolean isWindows(){
        return OS.contains("win");
    }

    public static boolean isMac(){
        return OS.contains("mac");
    }

    public static boolean isLinux(){
        return !isAndroid() && (OS.contains("nix") || OS.contains("nux"));
    }

    public static boolean isAndroid(){
        if (!OS.contains("linux")) {
            return false;
        }

        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            var vmName = System.getProperty("java.vm.name", "").toLowerCase();
            return vmName.contains("dalvik") || vmName.contains("art");
        }
    }

    public static String getOSName(){
        if (isWindows()) {
            return "win";
        }
        if (isAndroid()) {
            return "android";
        }
        if (isLinux()) {
            return "linux";
        }
        if (isMac()) {
            return "mac";
        }
        return "null";
    }
}
