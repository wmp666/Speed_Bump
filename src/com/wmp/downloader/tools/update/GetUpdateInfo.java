package com.wmp.downloader.tools.update;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.Run;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class GetUpdateInfo {

    private static final Logger logger = Logger.getLogger(GetUpdateInfo.class);

    /**
     * 获取更新信息
     *
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
                //最原始的更新内容
                var body = json.getString("body");
                if (compareVersions(Run.VERSION, urlVersion) < 0) {
                    //有新版
                    AtomicReference<String> targetUrl = new AtomicReference<>("");
                    json.getJSONArray("assets").forEach(obj -> {
                        if (obj instanceof JSONObject jsonObject) {
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
                    //获取更多
                    var infoWithinInterval = getAllUpdateInfoWithinInterval(useGithubAccelerate,
                            Run.VERSION, urlVersion);
                    body = infoWithinInterval == null ? body : infoWithinInterval;

                    return new UpdateInfo(urlVersion, body, targetUrl.get());
                } else {
                    //无新版
                    return null;
                }
            } else if (useGithubAccelerate && status == 403) {
                return getUpdateInfo(false);
            } else {
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
     *
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

    /**
     * 判断版本号是否在指定区间内（包含端点）,+表示这一端始终符合
     *
     * @param version      待检查的版本号
     * @param startVersion 区间起始版本
     * @param lastVersion  区间结束版本
     * @return true 如果 startVersion <= version <= lastVersion（若 start > last 则自动交换）
     * @throws IllegalArgumentException 如果任一参数为 null
     */
    public static boolean isVersionInRange(String version, String startVersion, String lastVersion) {
        if (version == null || startVersion == null || lastVersion == null) {
            throw new IllegalArgumentException("Version strings must not be null");
        }
        // 确保 start <= last，若反向则交换
        if (!(startVersion.equals("+") || lastVersion.equals("+")) && compareVersions(startVersion, lastVersion) > 0) {
            String temp = startVersion;
            startVersion = lastVersion;
            lastVersion = temp;
        }
        return (startVersion.equals("+") || compareVersions(version, startVersion) >= 0)
                && (lastVersion.equals("+") || compareVersions(version, lastVersion) <= 0);
    }

    // 若仍需要原来的布尔语义，可保留：
    public static boolean versionGreaterThan(String v1, String v2) {
        return compareVersions(v1, v2) > 0;
    }

    private static String getAllUpdateInfoWithinInterval(boolean useGithubAccelerate, String startVersion, String lastVersion) {
        var apiUrl = "https://api.github.com/repos/wmp666/Speed_Bump/releases";

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

            var status = 404;
            var message = "";

            InputStream is = response.bodyStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();

            if (body.startsWith("{")) {
                var json = JSONObject.parseObject(body);
                message = json.getString("message");
                status = json.getIntValue("status", 200);
            } else if (body.startsWith("[")) {
                status = 200;
            }


            if (status == 200) {
                var json = JSONArray.parseArray(body);
                ArrayList<String> versionList = new ArrayList<>();
                ArrayList<String> infoList = new ArrayList<>();

                for (var o : json) {
                    if (o instanceof JSONObject jsonObject) {
                        var version = jsonObject.getString("tag_name");
                        // 只收集处于区间内的版本
                        if (isVersionInRange(version, startVersion, lastVersion)) {
                            if (version.equals(Run.VERSION)) continue;
                            versionList.add(version);
                            infoList.add(jsonObject.getString("body"));
                        }
                    }
                }

                return IntegrationUpdateInfo(versionList.toArray(String[]::new),
                        infoList.toArray(String[]::new));

            } else if (useGithubAccelerate && status == 403) {
                return getAllUpdateInfoWithinInterval(false, startVersion, lastVersion);
            } else {
                ToastMessage.show(String.format(
                        "Status = %s message = %s",
                        status,
                        message
                ), ToastMessage.ERROR);
                logger.error("Json数据存在问题 status=" + status);
            }
        } catch (Exception e) {
            logger.error("网络数据获取失败", e);
        }

        return null;
    }

    private static String IntegrationUpdateInfo(String[] versions, String[] updateInfos) {

        //### 新增功能
        HashSet<String> newFunctionStrList = new HashSet<>();
        //### 体验优化
        HashSet<String> ExperienceOptimizationStrList = new HashSet<>();
        //### 问题修复
        HashSet<String> ProblemFixedStrList = new HashSet<>();
        //### 未知
        HashSet<String> unknownStrList = new HashSet<>();

        HashMap<Integer, HashSet<String>> infoMap = new HashMap<>();
        infoMap.put(1, newFunctionStrList);
        infoMap.put(2, ExperienceOptimizationStrList);
        infoMap.put(3, ProblemFixedStrList);
        infoMap.put(4, unknownStrList);

        //1-新增功能 2-体验优化 3-问题修复
        int status = 4;
        //格式化数据
        for (int i = 0; i < updateInfos.length; i++) {
            String s = updateInfos[i];
            String version = versions[i];
            for (var string : s.replaceAll("\\r|\\r\\n", "\n").split("\\n")) {
                if (!string.isBlank()) {
                    //判断开头 ### /-
                    if (string.startsWith("### ")) {
                        if (string.contentEquals("### 新增功能")) {
                            status = 1;
                        } else if (string.contentEquals("### 体验优化")) {
                            status = 2;
                        } else if (string.contentEquals("### 问题修复")) {
                            status = 3;
                        } else status = 4;
                    } else if (string.startsWith("- ")) {
                        if (string.substring(2).isBlank()) continue;
                        infoMap.get(status).add(string + " (Version **" + version + "**)");
                    } else infoMap.get(4).add(string + " (Version **" + version + "**)");
                }
            }

        }

        //拼接数据
        StringBuilder sb = new StringBuilder();

        if (!infoMap.get(1).isEmpty()) {
            sb.append("## 新增功能\n");
            infoMap.get(1).forEach(s -> {
                sb.append(s).append("\n");
            });
        }
        if (!infoMap.get(2).isEmpty()) {
            sb.append("\n## 体验优化\n");
            infoMap.get(2).forEach(s -> {
                sb.append(s).append("\n");
            });
        }
        if (!infoMap.get(3).isEmpty()) {
            sb.append("\n## 问题修复\n");
            infoMap.get(3).forEach(s -> {
                sb.append(s).append("\n");
            });
        }
        if (!infoMap.get(4).isEmpty()) {
            sb.append("\n## 未知\n");
            infoMap.get(4).forEach(s -> {
                sb.append(s).append("\n");
            });
        }

        return sb.toString();

    }

    public record UpdateInfo(String version, String body, String url) {
    }
}
