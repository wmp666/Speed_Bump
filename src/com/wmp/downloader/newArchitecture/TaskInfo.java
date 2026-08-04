package com.wmp.downloader.newArchitecture;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.ui.task.bilibili.BiliParser;
import com.wmp.downloader.newArchitecture.ui.task.bt.BTParser;
import com.wmp.downloader.newArchitecture.ui.task.douyin.DouyinParser;
import com.wmp.downloader.newArchitecture.ui.task.ed2k.ED2KParser;
import com.wmp.downloader.newArchitecture.ui.task.github.GithubParser;
import com.wmp.downloader.newArchitecture.ui.task.gopeed.GopeedParser;
import com.wmp.downloader.newArchitecture.ui.task.http.HTTPParser;
import com.wmp.downloader.tools.DataControl;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TaskInfo {
    private static final ArrayList<AbstractParser> parserList = new ArrayList<>();
    private static final ArrayList<AbstractParser> basicParserList = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(TaskInfo.class);
    private static final String PARSER_JSON = "Parser.json";
    private static final String PARSERS_DIR = "ParsersATools";
    private static final String DISABLE_ID_KEY = "disable_id";
    private static final String DELETE_ID_KEY = "delete_id";
    private static final String INFO_JSON = "info.json";

    static {
        // 1. 初始化 Parser.json（若不存在则创建）
        File dataPath = DataControl.getDataPath();
        File parserJsonFile = new File(dataPath, PARSER_JSON);
        JSONObject root = ensureParserJson(parserJsonFile);

        // 2. 读取删除和禁用列表
        Set<String> deleteSet = readIdSet(root, DELETE_ID_KEY);
        Set<String> disableSet = readIdSet(root, DISABLE_ID_KEY);

        // 3. 准备解析器目录
        File parsersDir = new File(dataPath, PARSERS_DIR);
        if (!parsersDir.exists()) {
            parsersDir.mkdirs();
        }

        // 4. 删除匹配的 JAR 文件
        File[] jarFiles = parsersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                String id = getParserIdFromJar(jar);
                if (id != null && deleteSet.contains(id)) {
                    if (jar.delete()) {
                        logger.info("Deleted parser JAR: " + jar.getName() + " (id: " + id + ")");
                    } else {
                        logger.error("Failed to delete JAR: " + jar.getAbsolutePath());
                    }
                }
            }
        }

        // 5. 重新扫描并加载剩余的解析器
        jarFiles = parsersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles != null) {
            for (File jar : jarFiles) {
                String id = getParserIdFromJar(jar);
                if (id == null) {
                    logger.warn("No id found in info.json of " + jar.getName() + ", skip.");
                    continue;
                }
                if (disableSet.contains(id)) {
                    logger.info("Parser " + id + " is disabled, skip loading.");
                    continue;
                }
                AbstractParser parser = loadParserFromJar(jar);
                if (parser != null) {
                    parserList.add(parser);
                    logger.info("Loaded parser: " + id + " from " + jar.getName());
                } else {
                    logger.warn("Failed to load parser from " + jar.getName());
                }
            }
        }


        //添加
        parserList.add(new BiliParser());
        parserList.add(new DouyinParser());
        parserList.add(new GithubParser());

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

    private static String getParserIdFromJar(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            JarEntry entry = jarFile.getJarEntry(INFO_JSON);
            if (entry == null) return null;
            String content = new String(jarFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            JSONObject info = JSONObject.parseObject(content);
            return info.getString("id");
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
            Method method = null;
            try {
                method = clazz.getMethod("getInstance");
            } catch (NoSuchMethodException e1) {
                try {
                    method = clazz.getMethod("getParser");
                } catch (NoSuchMethodException e2) {
                    logger.error("No getInstance() or getParser() method found in " + mainClass);
                    return null;
                }
            }

            // 调用方法，安全转换为 AbstractParser（不使用强制转型，通过 instanceof 校验）
            Object result = method.invoke(null);
            if (result instanceof AbstractParser) {
                return (AbstractParser) result;
            } else {
                logger.error("Method " + method.getName() + " does not return AbstractParser instance in " + mainClass);
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
        Iterator<AbstractParser> iterator = parserList.iterator();
        while (iterator.hasNext()) {
            AbstractParser parser = iterator.next();
            if (id.equals(parser.getID())) {
                iterator.remove();
                logger.info("Removed parser: " + id);
                break;
            }
        }
    }

    // ---------- 公开 API ----------
    public static void setDisableParser(String id) {
        if (id == null || id.isEmpty()) return;
        File parserJsonFile = new File(DataControl.getDataPath(), PARSER_JSON);
        JSONObject root = readParserJson(parserJsonFile);
        if (root == null) return;

        JSONArray disableArray = root.getJSONArray(DISABLE_ID_KEY);
        if (disableArray == null) {
            disableArray = new JSONArray();
            root.put(DISABLE_ID_KEY, disableArray);
        }
        // 去重
        for (Object obj : disableArray) {
            if (obj.toString().equals(id)) return;
        }
        disableArray.add(id);
        writeParserJson(parserJsonFile, root);

        // 从内存中移除
        removeParserFromList(id);
    }

    public static void setDeleteParser(String id) {
        if (id == null || id.isEmpty()) return;
        File parserJsonFile = new File(DataControl.getDataPath(), PARSER_JSON);
        JSONObject root = readParserJson(parserJsonFile);
        if (root == null) return;

        JSONArray deleteArray = root.getJSONArray(DELETE_ID_KEY);
        if (deleteArray == null) {
            deleteArray = new JSONArray();
            root.put(DELETE_ID_KEY, deleteArray);
        }
        for (Object obj : deleteArray) {
            if (obj.toString().equals(id)) return;
        }
        deleteArray.add(id);
        writeParserJson(parserJsonFile, root);

        // 删除对应的 JAR 文件
        File parsersDir = new File(DataControl.getDataPath(), PARSERS_DIR);
        if (parsersDir.exists()) {
            File[] jarFiles = parsersDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles != null) {
                for (File jar : jarFiles) {
                    String jarId = getParserIdFromJar(jar);
                    if (id.equals(jarId)) {
                        if (jar.delete()) {
                            logger.info("Deleted parser JAR: " + jar.getName() + " for id: " + id);
                        } else {
                            logger.error("Failed to delete JAR: " + jar.getAbsolutePath());
                        }
                        break;
                    }
                }
            }
        }

        // 从内存中移除
        removeParserFromList(id);
    }

    public static AbstractParser.Info getInfo(String link) {
        for (AbstractParser parser : parserList) {
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
        tempList.addAll(parserList);
        tempList.addAll(basicParserList);
        return List.of(tempList.toArray(AbstractParser[]::new));
    }
}