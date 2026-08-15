package com.wmp.downloader.tools.platform;

import java.util.Locale;

public class GetPlatform {
    private static final String OS = System.getProperty("os.name").toLowerCase(Locale.ROOT);

    public static boolean isWindows(){
        return OS.contains("win");
    }

    public static boolean isMac(){
        return OS.contains("mac");
    }

    public static boolean isLinux(){
        return OS.contains("nix") || OS.contains("nux");
    }

    public static String getOSName(){
        return isWindows()?
                "win" : (isLinux()?
                        "linux" : (isMac()?"mac":"none"));
    }
}
