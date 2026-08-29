package com.wmp.downloader.newArchitecture;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.Run;
import com.wmp.downloader.newArchitecture.abstractTask.*;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.ui.task.bt.BTParser;
import com.wmp.downloader.newArchitecture.ui.task.github.GithubParser;
import com.wmp.downloader.newArchitecture.ui.task.gopeed.GopeedParser;
import com.wmp.downloader.newArchitecture.ui.task.http.HTTPParser;
import com.wmp.downloader.tools.platform.GetPlatform;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ParserTaskInfo {
    private static final ArrayList<PluginParserInfo> ENABLE_PLUGIN_PARSER_LIST = new ArrayList<>();
    private static final ArrayList<PluginParserInfo> ALL_PARSER_LIST = new ArrayList<>();
    private static final ArrayList<AbstractParser> BASIC_PARSER_LIST = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(ParserTaskInfo.class);
    private static final String PARSER_JSON = "Parser.json";
    //private static final String PARSERS_DIR = "ParsersATools";
    private static final String DISABLE_ID_KEY = "disable_id";
    private static final String DELETE_ID_KEY = "delete_id";
    private static final String INFO_JSON = "info.json";
    private static final String INTRODUCTION_MD = "introduction.md";

    private static Map<String, URLClassLoader> pluginLoaders = new ConcurrentHashMap<>();

    static {loadParsers();}



    public static void loadParsers() {

        for (URLClassLoader loader : pluginLoaders.values()) {
            try {
                loader.close();
            } catch (IOException ignored) {
            }
        }
        pluginLoaders.clear();

        ALL_PARSER_LIST.clear();
        ENABLE_PLUGIN_PARSER_LIST.clear();
        BASIC_PARSER_LIST.clear();

        // 1. 初始化 Parser.json（若不存在则创建）
        File dataPath = DataControl.getDataPath();
        File parserJsonFile = new File(dataPath, PARSER_JSON);
        JSONObject root = ensureParserJson(parserJsonFile);

        // 2. 读取删除和禁用列表
        Set<String> deleteSet = readIdSet(root, DELETE_ID_KEY);
        Set<String> disableSet = readIdSet(root, DISABLE_ID_KEY);

        // 3. 准备解析器目录
        File parsersDir = DataControl.getPATPath();
        if (!parsersDir.exists()) {
            parsersDir.mkdirs();
        }

        // 4. 删除匹配的 JAR 文件
        File[] jarFiles = null;
        try {
            jarFiles = parsersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles != null) {
                for (File jar : jarFiles) {
                    String[] info = getParserInfoFromJar(jar);
                    if (info == null) {
                        logger.warn("No info found in info.json of " + jar.getName() + ", skip.");
                        continue;
                    }
                    var id = info[0];
                    if (id != null && deleteSet.contains(id)) {
                        if (jar.delete()) {
                            editParserJSONArray(1, id, DELETE_ID_KEY);
                            logger.info("删除解析器 JAR: " + jar.getName() + " (id: " + id + ")");
                        } else {
                            logger.error("解析器删除失败 JAR: " + jar.getAbsolutePath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("删除失败");
        }

        // 5. 重新扫描并加载剩余的解析器
        jarFiles = parsersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                String[] info = getParserInfoFromJar(jar);
                if (info == null) {
                    logger.warn("No info found in info.json of " + jar.getName() + ", skip.");
                    continue;
                }
                var id = info[0];
                if (id == null) {
                    logger.warn("No id found in info.json of " + jar.getName() + ", skip.");
                    continue;
                }

                if (deleteSet.contains(id)) {
                    logger.info("解析器 " + id + " 需要被删除，已跳过");
                    continue;
                }
                //判断版本是否符合条件
                var startVersion = info[1];
                var lastVersion = info[2];
                if (!GetUpdateInfo.isVersionInRange(Run.PLUGIN_SUPPORT_VERSION,
                        startVersion, lastVersion)) {
                    var i = JOptionPane.showConfirmDialog(
                            null,
                            String.format(
                                    StringFormat.translate("load_local_parser.version_error"),
                                    jar, startVersion, lastVersion, Run.PLUGIN_SUPPORT_VERSION),
                            StringFormat.translate("warn"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (i != JOptionPane.YES_OPTION) {
                        continue;
                    }
                }

                //0-all 1-windows 2-linux 3-mac
                var supportPlatform = 0;
                if (info[6] != null) {
                    supportPlatform = switch (info[6]){
                        case "windows" -> 1;
                        case "linux" -> 2;
                        case "mac" -> 3;
                        default -> 0;
                    };
                }
                if (supportPlatform != 0) {
                    if (!((GetPlatform.isWindows() && supportPlatform == 1) ||
                            (GetPlatform.isLinux() && supportPlatform == 2) ||
                            (GetPlatform.isMac() && supportPlatform == 3))) {
                        var i = JOptionPane.showConfirmDialog(
                                null,
                                String.format(
                                        StringFormat.translate("load_local_parser.platform_error"),
                                        jar, info[6],
                                StringFormat.translate("warn"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                        ));
                        if (i != JOptionPane.YES_OPTION) {
                            continue;
                        }
                    }
                }

                try {

                        AbstractParser parser = loadParserFromJar(jar);
                        if (parser != null) {
                            var parserInfo = new PluginParserInfo(parser, info[3],
                                    startVersion, lastVersion,
                                    info[4], false, info[5]);
                            ALL_PARSER_LIST.add(parserInfo);
                            if (disableSet.contains(id)) {
                                logger.info("解析器 " + id + " 被禁用,已跳过");
                                continue;
                            }

                            ENABLE_PLUGIN_PARSER_LIST.add(parserInfo);
                            logger.info("Loaded parser: " + id + " from " + jar.getName());
                        } else {
                            logger.warn("Failed to load parser from " + jar.getName());
                        }
                } catch (Exception e) {
                    logger.error("类加载失败", e);
                }
            }
        }


        addAppPlugin();
    }

    private static void addAppPlugin() {
        //添加
        var githubParserInfo = new PluginParserInfo(new GithubParser(), "1.0.1", "+", "+", "无名牌", true, null);
        ENABLE_PLUGIN_PARSER_LIST.add(githubParserInfo);
        ALL_PARSER_LIST.add(githubParserInfo);

        BASIC_PARSER_LIST.add(new BTParser());
        BASIC_PARSER_LIST.add(new GopeedParser());
        BASIC_PARSER_LIST.add(new HTTPParser());
    }

    // ---------- 工具方法 ----------
    private static JSONObject ensureParserJson(File file) {
        if (!file.exists()) {
            JSONObject root = new JSONObject();
            root.put(DISABLE_ID_KEY, new JSONArray());
            root.put(DELETE_ID_KEY, new JSONArray());
            try {
                Files.writeString(file.toPath(), root.toJSONString());
            } catch (IOException e) {
                logger.error("Failed to create Parser.json", e);
            }
            return root;
        }
        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return JSONObject.parseObject(content);
        } catch (IOException e) {
            logger.error("Failed to read Parser.json", e);
            JSONObject root = new JSONObject();
            root.put(DISABLE_ID_KEY, new JSONArray());
            root.put(DELETE_ID_KEY, new JSONArray());
            return root;
        }
    }

    private static Set<String> readIdSet(JSONObject root, String key) {
        Set<String> set = new HashSet<>();
        JSONArray array = root.getJSONArray(key);
        if (array != null) {
            for (Object obj : array) {
                set.add(obj.toString());
            }
        }
        return set;
    }

    private static String[] getParserInfoFromJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry infoEntry = jarFile.getJarEntry(INFO_JSON);
            if (infoEntry == null) return null;
            String infoContent = new String(jarFile.getInputStream(infoEntry).readAllBytes(), StandardCharsets.UTF_8);
            JSONObject info = JSONObject.parseObject(infoContent);

            //获取介绍introduction.md
            String introductionStr = null;

            var introductionEntry = jarFile.getJarEntry(INTRODUCTION_MD);
            if (introductionEntry != null) {
                introductionStr = new String(jarFile.getInputStream(introductionEntry).readAllBytes(), StandardCharsets.UTF_8);
            }

            return new String[]{info.getString("id"),
                    info.getString("plugin_support_version_start"),
                    info.getString("plugin_support_version_last"),
                    info.getString("version"),
                    info.getString("author"), introductionStr,
                    info.getString("support_platform")};
        } catch (IOException e) {
            logger.error("Error reading info.json from " + jar.getName(), e);
            return null;
        }
    }

    private static AbstractParser loadParserFromJar(File jar) throws Exception{
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()});
        String mainClass, id;
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry(INFO_JSON);
            if (entry == null) return null;
            String content = new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            JSONObject info = JSONObject.parseObject(content);
            mainClass = info.getString("mainClass");
            id = info.getString("id");
            if (mainClass == null || mainClass.isEmpty()) {
                logger.error("mainClass not specified in info.json of " + jar.getName());
                return null;
            }
        }
        Class<?> clazz = Class.forName(mainClass, true, classLoader);
        Object result = clazz.getDeclaredConstructor().newInstance();
        if (result instanceof AbstractParser) {
            AbstractParser parser = (AbstractParser) result;
            pluginLoaders.put(id, classLoader);  // 保存加载器
            return parser;
        } else {
                //进行向下兼容
                logger.error("Class " + clazz.getName() + " does not AbstractParser instance in " + mainClass);
                logger.error("将采用向下兼容");
                return new AbstractParser(){
                    @Override
                    public String getID() {
                        try {
                            return clazz.getDeclaredMethod("getID").invoke(result).toString();
                        } catch (Exception e) {
                            return "错误";
                        }
                    }

                    @Override
                    public String getSupportTip() {
                        try {
                            return clazz.getDeclaredMethod("getSupportTip").invoke(result).toString();
                        } catch (Exception e) {
                            return "错误";
                        }
                    }

                    @Override
                    protected void updateLinkInfo(String link) {
                        try {
                            clazz.getDeclaredMethod("updateLinkInfo", String.class).invoke(result, link);
                        } catch (Exception _) {
                        }
                    }

                    @Override
                    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
                        try {
                            return (AbstractLinkInfoPanel) clazz.getDeclaredMethod("getLinkedInfoPanel", String.class, ProcessHandle.Info.class).invoke(result, new Object[]{link, info});
                        } catch (Exception e) {
                            return new AbstractLinkInfoPanel(info) {
                                @Override
                                public JSONObject getJsonInfo() {
                                    return jsonInfo;
                                }
                            };
                        }
                    }

                    @Override
                    public boolean isMeetRequirements(String link) {
                        try {
                            return (boolean) clazz.getDeclaredMethod("isMeetRequirements", String.class).invoke(result, link);
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    @Override
                    protected AbstractTask getTask(String link, JSONObject infoJson) {
                        try {
                            return (AbstractTask) clazz.getDeclaredMethod("getTask", String.class, JSONObject.class).invoke(result, link, infoJson);
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public AbstractSpecialSettingsPage getSettingsPage() {
                        try {
                            return (AbstractSpecialSettingsPage) clazz.getDeclaredMethod("getSettingsPage").invoke(result);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                };
            }
    }

    private static JSONObject readParserJson(File file) {
        try {
            if (!file.exists()) {
                JSONObject root = new JSONObject();
                root.put(DISABLE_ID_KEY, new JSONArray());
                root.put(DELETE_ID_KEY, new JSONArray());
                return root;
            }
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            return JSONObject.parseObject(content);
        } catch (IOException e) {
            logger.error("Failed to read Parser.json", e);
            return null;
        }
    }

    private static void writeParserJson(File file, JSONObject root) {
        try {
            Files.writeString(file.toPath(), root.toJSONString());
        } catch (IOException e) {
            logger.error("Failed to write Parser.json", e);
        }
    }

    private static void removeParserFromList(String id) {
        Iterator<PluginParserInfo> iterator = ENABLE_PLUGIN_PARSER_LIST.iterator();
        while (iterator.hasNext()) {
            AbstractParser parser = iterator.next().parser();
            if (id.equals(parser.getID())) {
                iterator.remove();
                logger.info("Removed parser: " + id);
                break;
            }
        }
    }

    private static void addParserFromList(String id) {
        for (PluginParserInfo pluginParserInfo : ALL_PARSER_LIST) {
            if (id.equals(pluginParserInfo.parser().getID())) {
                ENABLE_PLUGIN_PARSER_LIST.add(pluginParserInfo);
                logger.info("Removed parser: " + id);
                break;
            }
        }
    }

    /**
     * 编辑Parser.json中的JSONArray数据
     * @param operation 0-添加 其他数字表示删除
     * @param id ID
     * @param key Key
     */
    private static void editParserJSONArray(int operation, String id, String key){
        if (id == null || id.isEmpty()) return;
        File parserJsonFile = new File(DataControl.getDataPath(), PARSER_JSON);
        JSONObject root = readParserJson(parserJsonFile);
        if (root == null) return;

        JSONArray array = root.getJSONArray(key);
        if (array == null) {
            array = new JSONArray();
            root.put(key, array);
        }
        var b = operation == 0 ? array.add(id) : array.remove(id);
        root.put(key, array);
        logger.info(b);
        writeParserJson(parserJsonFile, root);
    }

    // ---------- 公开 API ----------
    public static void setDisableParser(String id) {
        editParserJSONArray(0, id, DISABLE_ID_KEY);

        // 从内存中移除
        removeParserFromList(id);
    }

    // ---------- 公开 API ----------
    public static void removeDisableParser(String id) {
        editParserJSONArray(1, id, DISABLE_ID_KEY);

        // 从内存中移除
        addParserFromList(id);
    }

    public static void setDeleteParser(String id) {
        editParserJSONArray(0, id, DELETE_ID_KEY);

        // 从内存中移除
        removeParserFromList(id);
    }

    public static AbstractParser getParser(String link) {
        for (PluginParserInfo parserInfo : ENABLE_PLUGIN_PARSER_LIST) {
            var parser = parserInfo.parser();
            if (parser.isMeetRequirements(link)) {
                return parser;
            }
        }
        for (AbstractParser parser : BASIC_PARSER_LIST) {
            if (parser.isMeetRequirements(link)) {
                return parser;
            }
        }
        return null;
    }

    public static AbstractParser.Info getInfo(String link) {
        for (PluginParserInfo parserInfo : ENABLE_PLUGIN_PARSER_LIST) {
            var parser = parserInfo.parser();
            if (parser.isMeetRequirements(link)) {
                return parser.getParserInfo(link);
            }
        }
        for (AbstractParser parser : BASIC_PARSER_LIST) {
            if (parser.isMeetRequirements(link)) {
                return parser.getParserInfo(link);
            }
        }
        return null;
    }

    public static List<AbstractParser> getEnablePluginParserList() {
        ArrayList<AbstractParser> tempList = new ArrayList<>();
        tempList.addAll(ENABLE_PLUGIN_PARSER_LIST.stream().map(PluginParserInfo::parser).toList());
        tempList.addAll(BASIC_PARSER_LIST);
        return List.of(tempList.toArray(AbstractParser[]::new));
    }

    public static List<PluginParserInfo> getPluginParserList(){
        return List.of(ENABLE_PLUGIN_PARSER_LIST.toArray(PluginParserInfo[]::new));
    }

    public static List<InstallPluginParserInfo> getInstallPluginParserInfoList(){
        return getInstallPluginParserInfoList(DataControl.get("use_github_accelerate", false));
    }

    private static List<InstallPluginParserInfo> getInstallPluginParserInfoList(boolean useGithubAccelerate) {
        var apiUrl = DataControl.PLUGIN_GITHUB_API_HEAD + "/releases";

        if (useGithubAccelerate) {
            apiUrl = "https://" + DataControl.get("github_accelerate_link", "gh-proxy.org") + "/" + apiUrl;
        }

        try {
            Connection.Response response = Jsoup.connect(apiUrl)
                    .userAgent("Mozilla/5.0")
                    .ignoreContentType(true)
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .execute();

            int status = 404;
            String message = "";

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
                var jsonArray = JSONArray.parseArray(body);
                var list = new ArrayList<InstallPluginParserInfo>();

                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject release = jsonArray.getJSONObject(i);
                    String tag = release.getString("tag_name");
                    String title = release.getString("name"); // GitHub 的 name 字段即为 Release 标题
                    String releaseBody = release.getString("body");

                    // 解析 Release Body 获取插件信息
                    PluginParserInfo info = parsePluginInfoFromBody(releaseBody, tag, title);
                    if (info == null) {
                        // 解析失败则跳过该 Release
                        logger.warn("跳过 Release: " + tag + "，因为 Body 内容不符合规范");
                        continue;
                    }

                    // 获取第一个 asset 的下载链接
                    JSONArray assets = release.getJSONArray("assets");
                    if (assets.isEmpty()) {
                        logger.warn("Release " + tag + " 没有 asset，跳过");
                        continue;
                    }
                    String downloadUrl = assets.getJSONObject(0).getString("browser_download_url");

                    list.add(new InstallPluginParserInfo(downloadUrl, info));
                }

                return list;
            } else if (useGithubAccelerate && status == 403) {
                return null;
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

    /**
     * 从 Release Body 中解析插件信息
     * @param body   Release 的正文内容
     * @param tag    Release 的 Tag（通常和插件名称一致）
     * @param title  Release 的标题（通常和插件名称一致）
     * @return PluginParserInfo 对象，若解析失败返回 null
     */
    private static PluginParserInfo parsePluginInfoFromBody(String body, String tag, String title) {
        if (body == null || body.isEmpty()) {
            return null;
        }

        // 按行分割
        String[] lines = body.split("\\n");
        String name = null;
        String author = null;
        String version = null;
        String supportVersion = null; // 形如 "start~last"
        String supportPlatform = "all";
        StringBuilder introduction = new StringBuilder();

        boolean inInfoSection = false;
        boolean inIntroSection = false;

        for (String line : lines) {
            String trimmed = line.strip();

            // 检测节标题
            if (trimmed.startsWith("### 信息")) {
                inInfoSection = true;
                inIntroSection = false;
                continue;
            } else if (trimmed.startsWith("### 介绍")) {
                inInfoSection = false;
                inIntroSection = true;
                continue;
            }

            // 在信息节中解析键值对
            if (inInfoSection) {
                if (trimmed.startsWith("name:")) {
                    name = trimmed.substring(5).strip();
                } else if (trimmed.startsWith("author:")) {
                    author = trimmed.substring(7).strip();
                } else if (trimmed.startsWith("version:")) {
                    version = trimmed.substring(8).strip();
                } else if (trimmed.startsWith("plugin_support_version:")) {
                    supportVersion = trimmed.substring("plugin_support_version: ".length()).strip();
                } else if (trimmed.startsWith("support_platform:")) {
                    supportPlatform = trimmed.substring(17).strip();
                }
            }

            // 介绍节：从 "->" 开始的内容作为介绍
            if (inIntroSection) {
                if (!trimmed.isEmpty()) {
                    // 如果已开始介绍，后续行（非空）也视为介绍的一部分（可能多行）
                    introduction.append(trimmed).append("\n");
                }
            }
        }

        // 检查必要字段是否齐全
        if (name == null || author == null || version == null || supportVersion == null) {
            return null;
        }

        // 解析 supportVersion 为 start 和 last
        String startVersion = "";
        String lastVersion = "";
        if (supportVersion.contains("~")) {
            String[] parts = supportVersion.split("~", -1);
            if (parts.length >= 1) startVersion = parts[0].strip();
            if (parts.length >= 2) lastVersion = parts[1].strip();
            // 处理 + 符号（无限）——直接保留字符串，后续在比较时特殊处理
        } else {
            // 如果没有 ~，则视为单一版本（start=last）
            startVersion = supportVersion.strip();
            lastVersion = supportVersion.strip();
        }

        // 构建 PluginParserInfo，AbstractParser 传 null

        String finalName = name;
        return new PluginParserInfo(
                new AbstractParser() {
                    @Override
                    public String getID() {
                        return finalName;
                    }

                    @Override
                    public String getSupportTip() {
                        return finalName;
                    }

                    @Override
                    protected void updateLinkInfo(String link) {

                    }

                    @Override
                    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
                        return null;
                    }

                    @Override
                    public boolean isMeetRequirements(String link) {
                        return false;
                    }

                    @Override
                    protected AbstractTask getTask(String link, JSONObject infoJson) {
                        return null;
                    }

                    @Override
                    public AbstractSpecialSettingsPage getSettingsPage() {
                        return null;
                    }
                },           // parser
                version,
                startVersion,
                lastVersion,
                author,
                false,          // isAppPlugin 默认为 false，可根据需要调整
                introduction.toString().strip()
        );
    }

    public static List<PluginParserInfo> getAllPluginParserList(){
        return List.of(ALL_PARSER_LIST.toArray(PluginParserInfo[]::new));
    }

    public static boolean isEnable(String id){
        var allIDList = ALL_PARSER_LIST.stream().map(pluginParserInfo -> pluginParserInfo.parser().getID()).toList();
        var enableIDlist = ENABLE_PLUGIN_PARSER_LIST.stream().map(pluginParserInfo -> pluginParserInfo.parser().getID()).toList();
        if (!allIDList.contains(id)) return false;
        return enableIDlist.contains(id);
    }
}