package com.wmp.downloader;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class WebSetter {
    /**
     * SSL控制
     *
     * @param isSSL 是否启用SSL
     */
    public static void SSLControl(boolean isSSL) {
        if (isSSL) {
            // 启用SSL

        } else {
            // 禁用SSL
            try {
                var sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                        }
                }, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void proxies(boolean isUseSystemProxy) {
        System.setProperty("java.net.useSystemProxies", String.valueOf(isUseSystemProxy));
    }

    public static void proxies(String host, int port) {
        proxies(false);
        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", String.valueOf(port));
    }

    public static void isUseProxy(boolean isUseProxy) {
        if (!isUseProxy) {
            proxies(false);
            System.setProperty("http.proxyHost", "");
            System.setProperty("http.proxyPort", "");
        }

    }
}
