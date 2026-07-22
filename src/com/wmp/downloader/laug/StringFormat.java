package com.wmp.downloader.laug;

import com.wmp.downloader.tools.DataControl;

import java.util.Locale;
import java.util.ResourceBundle;

public class StringFormat {
    private static final String BUNDLE_PREFIX = "com.wmp.downloader.laug.";

    public static String translate(String rootLocal, String key){
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX + rootLocal, Locale.getDefault());
        return bundle.getString(key);
    }
}
