package com.wmp.downloader.tools.update;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.Run;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.concurrent.atomic.AtomicReference;

public class GetUpdateInfo {

    private static final Logger logger = Logger.getLogger(GetUpdateInfo.class);

    /**
     * 获取更新信息
     * @return 更新信息（没有时，为null）
     */
    public static UpdateInfo getUpdateInfo() {
        return getUpdateInfo(DataControl.get("use_github_accelerate", false));
    }

    public static UpdateInfo getUpdateInfo(boolean useGithubAccelerate) {
        var apiUrl = "https://api.github.com/repos/wmp666/Speed_Bump/releases/latest";

        if (useGithubAccelerate) {
            apiUrl = "https://" + DataControl.get("github_accelerate_link", "gh-proxy.org") + "/" + apiUrl;
        }


        try {
            Connection.Response response = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0")
                    .ignoreContentType(true)   // 返回 JSON，非 HTML
                    .followRedirects(false)    // 禁用重定向（可选）
                    .method(Connection.Method.GET)
                    .execute();

            var json = JSONObject.parseObject(response.body());
            var status = json.getIntValue("status", 200);
            if (status == 200) {
                var urlVersion = json.getString("tag_name");
                var body = json.getString("body");
                if (compareVersions(Run.VERSION, urlVersion) < 0) {
                    //有新版
                    AtomicReference<String> targetUrl = new AtomicReference<>("");
                    json.getJSONArray("assets").forEach(obj -> {
                        if (obj instanceof JSONObject jsonObject){
                            //获取系统名称
                            var osName = System.getProperty("os.name");
                            var name = jsonObject.getString("name");
                            if (osName.contains("Win")) {
                                if (name.startsWith("Speed_Bump_Setup") && name.endsWith(".exe")) {
                                    targetUrl.set(jsonObject.getString("browser_download_url"));
                                }
                            } else if (osName.contains("Mac")) {
                                if (name.startsWith("Speed_Bump_Setup") && name.endsWith(".dmg")) {
                                    targetUrl.set(jsonObject.getString("browser_download_url"));
                                }
                            } else if (osName.contains("Linux")) {
                                if (name.startsWith("Speed_Bump_Setup") && name.endsWith(".deb")) {
                                    targetUrl.set(jsonObject.getString("browser_download_url"));
                                }
                            }
                        }

                    });

                    return new UpdateInfo(urlVersion, body, targetUrl.get());
                } else {
                    //无新版
                    return null;
                }
            } else if (useGithubAccelerate && status == 403) {
                return getUpdateInfo(false);
            } else{
                ToastMessage.show(String.format(
                        "Status = %s message = %s",
                        status,
                        json.getString("message")
                ), ToastMessage.ERROR);
                logger.error("Json数据存在问题 status=" + status);
            }
        } catch (Exception e) {
            logger.error("网络数据获取失败", e);
        }

        return null;
    }

    /**
     * 比较两个版本号的大小（格式：数字段用点分隔，如 "1.2.3"）
     * @param v1 版本1，不能为 null
     * @param v2 版本2，不能为 null
     * @return 负数表示 v1 < v2，0 表示相等，正数表示 v1 > v2
     * @throws IllegalArgumentException 如果任一版本号为 null 或包含非数字段
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("Version strings must not be null");
        }
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2); // 或者 (num1 > num2 ? 1 : -1)
            }
        }
        return 0;
    }

    // 若仍需要原来的布尔语义，可保留：
    public static boolean versionGreaterThan(String v1, String v2) {
        return compareVersions(v1, v2) > 0;
    }


    public record UpdateInfo(String version, String body, String url) {
    }
}
