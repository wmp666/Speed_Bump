package com.wmp.downloader.newArchitecture.abstractTask;

public record PluginParserInfo(AbstractParser parser, String version, String startVersion, String lastVersion, String author, boolean isAppPlugin) {
}
