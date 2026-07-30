package com.wmp.downloader.test;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;

public class FTPDownloader {

    public static void main(String[] args) {
        String server = "127.0.0.1";
        int port = 21;
        String username = "testuser";
        String password = ""; // 无密码用户传空字符串

        // 1. 创建信任所有证书的 TrustManager（仅用于本地测试）
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        FTPSClient ftpsClient = null;
        try {
            // 2. 创建自定义 SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 3. 创建 FTPSClient 并传入自定义 SSLContext
            ftpsClient = new FTPSClient(sslContext);
            // 或者使用 ftpsClient = new FTPSClient("TLS", true); // true 表示隐式模式，一般不使用

            // 4. 连接服务器
            ftpsClient.connect(server, port);
            int reply = ftpsClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                System.out.println("连接被拒绝，响应码: " + reply);
                return;
            }

            // 5. 设置被动模式和二进制传输
            ftpsClient.enterLocalPassiveMode();
            ftpsClient.setFileType(FTP.BINARY_FILE_TYPE);

            // 6. 先登录（此时控制通道已加密，但数据通道尚未设置）
            boolean loginSuccess = ftpsClient.login(username, password);
            if (!loginSuccess) {
                System.out.println("登录失败，请检查用户名/密码。");
                return;
            }
            System.out.println("FTPS 登录成功！");

            // 7. 登录成功后，设置数据通道加密（P = Private）
            //    必须先调用 execPBSZ(0)，再调用 execPROT("P")
            ftpsClient.execPBSZ(0);
            ftpsClient.execPROT("P");
            System.out.println("数据通道加密已启用（PROT P）");

            // 8. 列出根目录下的文件
            FTPFile[] files = ftpsClient.listFiles();
            if (files != null && files.length > 0) {
                System.out.println("=== 文件列表 ===");
                for (FTPFile file : files) {
                    String type = file.isDirectory() ? "[DIR]" : "[FILE]";
                    System.out.println(type + " " + file.getName() + " (大小: " + file.getSize() + " 字节)");
                }
            } else {
                System.out.println("目录为空或无法获取文件列表。");
            }

            // 9. 下载示例：如果存在某个文件，将其下载到本地
            //    假设远程根目录下有一个名为 "example.txt" 的文件
            String remoteFile = "example.txt";
            String localFile = "downloaded_" + remoteFile;
            try (OutputStream output = new FileOutputStream(localFile)) {
                boolean success = ftpsClient.retrieveFile(remoteFile, output);
                if (success) {
                    System.out.println("文件 " + remoteFile + " 下载成功，保存为 " + localFile);
                } else {
                    System.out.println("文件 " + remoteFile + " 下载失败（可能不存在或权限问题）。");
                }
            }

            // 10. 登出
            ftpsClient.logout();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 11. 断开连接
            if (ftpsClient != null && ftpsClient.isConnected()) {
                try {
                    ftpsClient.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}