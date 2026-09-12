package com.wmp.downloader.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.tools.file.DataControl;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 微软翻译（Azure Translator 官方 REST API v3）。
 * <p>
 * 密钥与区域分别保存在配置项 {@code translate.azure_key} / {@code translate.azure_region} 中
 * （调用前通过 {@link DataControl} 读取），未配置密钥时调用会抛出明确错误。
 * <p>
 * 用法：{@link #translate(String, String)} 将简体中文翻译为指定目标语言代码；
 * 目标语言见 {@link #languages()}，默认目标语言见 {@link #defaultLanguage()}。
 */
public class MicrosoftTranslator {

    private static final Logger logger = Logger.getLogger(MicrosoftTranslator.class);

    public static final String KEY_DATA = "translate.azure_key";
    public static final String REGION_DATA = "translate.azure_region";

    private static final String TRANSLATE_URL = "https://api.cognitive.microsofttranslator.com/translate";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 一种可翻译的语言。
     *
     * @param label 用于界面展示的语言名（本地化原名）
     * @param code  翻译接口的语言代码（如 "en"、"zh-Hans"）
     */
    public record Language(String label, String code) {
        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * 供用户选择的目标语言列表。
     */
    public static List<Language> languages() {
        return List.of(
                new Language("English", "en"),
                new Language("简体中文", "zh-Hans"),
                new Language("繁體中文", "zh-Hant"),
                new Language("日本語", "ja"),
                new Language("한국어", "ko"),
                new Language("Русский", "ru"),
                new Language("Français", "fr"),
                new Language("Deutsch", "de"),
                new Language("Español", "es"),
                new Language("Português", "pt"),
                new Language("Tiếng Việt", "vi"),
                new Language("ไทย", "th"),
                new Language("Bahasa Indonesia", "id"),
                new Language("Türkçe", "tr")
        );
    }

    /**
     * 根据当前界面语言返回默认的目标翻译语言。
     * 界面为中文时（源语言同为目标语言，翻译无意义）默认英文；
     * 其它情况下默认与当前界面语言一致。
     */
    public static Language defaultLanguage() {
        List<Language> list = languages();
        String laug = String.valueOf(DataControl.get("laug", "zh_cn")).toLowerCase(Locale.ROOT);
        String want;
        if (laug.startsWith("zh")) want = "en";
        else if (laug.startsWith("en")) want = "en";
        else if (laug.startsWith("ja")) want = "ja";
        else if (laug.startsWith("ko")) want = "ko";
        else if (laug.startsWith("ru")) want = "ru";
        else want = "en";
        for (var l : list) {
            if (l.code().equalsIgnoreCase(want)) return l;
        }
        return list.getFirst();
    }

    /**
     * 是否已配置 Azure 翻译密钥。
     */
    public static boolean hasKey() {
        String key = String.valueOf(DataControl.get(KEY_DATA, "")).trim();
        return !key.isEmpty();
    }

    /**
     * 将简体中文文本翻译为目标语言。
     *
     * @param text 简体中文源文本
     * @param to   目标语言代码（见 {@link #languages()}）
     * @return 翻译后的文本
     * @throws Exception 未配置密钥、网络或解析失败时抛出
     */
    public static String translate(String text, String to) throws Exception {
        if (text == null || text.isBlank()) return text;

        String key = DataControl.get(KEY_DATA, "").trim();
        if (key.isEmpty()) {
            throw new IllegalStateException("尚未配置微软翻译(Azure Translator)密钥");
        }
        String region = DataControl.get(REGION_DATA, "").trim();

        String url = TRANSLATE_URL + "?api-version=3.0&from=zh-Hans&to=" + to;
        String payload = JSON.toJSONString(List.of(Map.of("Text", text)));

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Ocp-Apim-Subscription-Key", key)
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0");
        if (!region.isEmpty()) {
            builder.header("Ocp-Apim-Subscription-Region", region);
        }
        HttpRequest request = builder
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("翻译请求失败，HTTP " + response.statusCode() + "：" + response.body());
        }

        String translated = extractTranslated(response.body());
        if (translated == null) {
            throw new IllegalStateException("翻译响应中未找到译文：" + response.body());
        }
        logger.info("翻译成功：" + text.length() + " 字符 -> " + to);
        return translated;
    }

    /**
     * 从响应中提取译文。响应形如：[{"translations":[{"text":"...","to":"..."}]}]
     */
    private static String extractTranslated(String body) {
        Object parsed;
        try {
            parsed = JSON.parse(body);
        } catch (Exception e) {
            return null;
        }
        if (parsed instanceof JSONArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item == null) continue;
                JSONArray trs = item.getJSONArray("translations");
                if (trs != null && !trs.isEmpty()) {
                    String s = trs.getJSONObject(0).getString("text");
                    if (s != null) return s;
                }
            }
            if (!arr.isEmpty()) {
                return arr.getJSONObject(0).getString("text");
            }
        } else if (parsed instanceof JSONObject obj) {
            JSONArray trs = obj.getJSONArray("translations");
            if (trs != null && !trs.isEmpty()) {
                return trs.getJSONObject(0).getString("text");
            }
            return obj.getString("text");
        }
        return null;
    }
}
