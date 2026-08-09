package com.wmp.downloader.newArchitecture;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.Run;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.PluginParserInfo;
import com.wmp.downloader.newArchitecture.ui.task.bilibili.BiliParser;
import com.wmp.downloader.newArchitecture.ui.task.bt.BTParser;
import com.wmp.downloader.newArchitecture.ui.task.douyin.DouyinParser;
import com.wmp.downloader.newArchitecture.ui.task.ed2k.ED2KParser;
import com.wmp.downloader.newArchitecture.ui.task.github.GithubParser;
import com.wmp.downloader.newArchitecture.ui.task.gopeed.GopeedParser;
import com.wmp.downloader.newArchitecture.ui.task.http.HTTPParser;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.update.GetUpdateInfo;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TaskInfo {
    private static final ArrayList<PluginParserInfo> parserList = new ArrayList<>();
    private static final ArrayList<AbstractParser> basicParserList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TaskInfo.class);
    private static final String PARSER_JSON = "Parser.json";
    //private static final String PARSERS_DIR = "ParsersATools";
    private static final String DISABLE_ID_KEY = "disable_id";
    private static final String DELETE_ID_KEY = "delete_id";
    private static final String INFO_JSON = "info.json";
    private static final String INTRODUCTION_MD = "introduction.md";

    static {
        loadParsers();

    }

    public static void loadParsers() {

        parserList.clear();
        basicParserList.clear();

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
                            logger.error("Failed to delete JAR: " + jar.getAbsolutePath());
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
                if (disableSet.contains(id)) {
                    logger.info("Parser " + id + " is disabled, skip loading.");
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
                    if (i == JOptionPane.NO_OPTION ||
                    i == JOptionPane.CLOSED_OPTION) {
                        continue;
                    }
                }

                AbstractParser parser = loadParserFromJar(jar);
                if (parser != null) {
                    parserList.add(new PluginParserInfo(parser, info[3],
                            startVersion, lastVersion,
                            info[4], false, info[5]));
                    logger.info("Loaded parser: " + id + " from " + jar.getName());
                } else {
                    logger.warn("Failed to load parser from " + jar.getName());
                }
            }
        }


        //添加


        parserList.add(new PluginParserInfo(new DouyinParser(), "1.0.1", "+", "+", "无名牌", true,
                """
                        # 抖音分享链接解析
                        支持解析大部分抖音的分享链接,这主要取决于**遇见API**是否支持
                        ## :)
                        
                        ## 注意
                        - 不支持多线程下载"""));
        parserList.add(new PluginParserInfo(new GithubParser(), "1.0.0", "+", "+", "无名牌", true, null));
        parserList.add(new PluginParserInfo(new BiliParser(), "1.0.0", "+", "+", "无名牌", true,
                """
                        # 哔哩哔哩解析器
                        支持大部分哔哩哔哩网页链接/BV号
                        
                        ## 注意
                        - 不支持多线程下载"""));

        basicParserList.add(new BTParser());
        basicParserList.add(new ED2KParser());
        basicParserList.add(new GopeedParser());
        basicParserList.add(new HTTPParser());
    }

    // ---------- 工具方法 ----------
    private static JSONObject ensureParserJson(File file) {
        if (!file.exists()) {
            JSONObject root = new JSONObject();
            root.put(DISABLE_ID_KEY, new JSONArray());
            root.put(DELETE_ID_KEY, new JSONArray());
            try {
                Files.write(file.toPath(), root.toJSONString().getBytes(StandardCharsets.UTF_8));
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
                    info.getString("author"), introductionStr};
        } catch (IOException e) {
            logger.error("Error reading info.json from " + jar.getName(), e);
            return null;
        }
    }

    private static AbstractParser loadParserFromJar(File jar) {
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()})) {
            // 读取 info.json 获取 mainClass
            String mainClass;
            try (JarFile jarFile = new JarFile(jar)) {
                JarEntry entry = jarFile.getJarEntry(INFO_JSON);
                if (entry == null) return null;
                String content = new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                JSONObject info = JSONObject.parseObject(content);
                mainClass = info.getString("mainClass");
                if (mainClass == null || mainClass.isEmpty()) {
                    logger.error("mainClass not specified in info.json of " + jar.getName());
                    return null;
                }
            }

            // 加载类并查找方法（优先 getInstance，其次 getParser）
            Class<?> clazz = Class.forName(mainClass, true, classLoader);

            // 调用方法，安全转换为 AbstractParser（不使用强制转型，通过 instanceof 校验）
            Object result = clazz.getDeclaredConstructor().newInstance();
            if (result instanceof AbstractParser) {
                return (AbstractParser) result;
            } else {
                logger.error("Class " + clazz.getName() + " does not AbstractParser instance in " + mainClass);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error loading parser from " + jar.getName(), e);
            return null;
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
            Files.write(file.toPath(), root.toJSONString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Failed to write Parser.json", e);
        }
    }

    private static void removeParserFromList(String id) {
        Iterator<PluginParserInfo> iterator = parserList.iterator();
        while (iterator.hasNext()) {
            AbstractParser parser = iterator.next().parser();
            if (id.equals(parser.getID())) {
                iterator.remove();
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

    public static void setDeleteParser(String id) {
        editParserJSONArray(0, id, DELETE_ID_KEY);

        // 从内存中移除
        removeParserFromList(id);
    }

    public static AbstractParser.Info getInfo(String link) {
        for (PluginParserInfo parserInfo : parserList) {
            var parser = parserInfo.parser();
            if (parser.isMeetRequirements(link)) {
                return parser.setLink(link);
            }
        }
        for (AbstractParser parser : basicParserList) {
            if (parser.isMeetRequirements(link)) {
                return parser.setLink(link);
            }
        }
        return null;
    }

    public static List<AbstractParser> getParserList() {
        ArrayList<AbstractParser> tempList = new ArrayList<>();
        tempList.addAll(parserList.stream().map(PluginParserInfo::parser).toList());
        tempList.addAll(basicParserList);
        return List.of(tempList.toArray(AbstractParser[]::new));
    }

    public static List<PluginParserInfo> getPluginParserList(){
        return List.of(parserList.toArray(PluginParserInfo[]::new));
    }
}