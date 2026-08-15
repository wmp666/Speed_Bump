package com.wmp.downloader.test;


import static com.wmp.downloader.tools.platform.FileAssociation.register;

public class FileAssociationTest {
    // 测试示例
    public static void main(String[] args) throws Exception {
        // 示例参数（请根据实际情况修改）
        String extension = "abcd";
        String description = "abcdFile";
        String icon = "C:\\Users\\21348\\Desktop\\吴鹤轩\\Java\\DownLoader\\src\\icon\\speedbump_file.ico";      // Windows: .ico, macOS: .icns, Linux: .png
        String app = "E:\\Windows\\System32\\notepad.exe";   // Windows: exe或脚本, macOS: .app目录, Linux: 可执行文件
        register(extension, description, icon, app);
    }
}
