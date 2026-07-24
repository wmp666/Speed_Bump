package com.wmp.downloader.ui.task.http;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import org.apache.log4j.Logger;

import java.awt.event.ActionEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wmp.downloader.tools.download.URLDownloadTool.extractFileName;
import static com.wmp.downloader.tools.download.URLDownloadTool.getFileSize;

public class HTTPParser extends Parser {

    private static final Logger logger = Logger.getLogger(HTTPParser.class);
    @Override
    public LinkFileInfoPanel parse(String link) {
        if (DataControl.get("is_use_github_accelerate", false) && link.contains("github.com")){
            link = "https://" + DataControl.get("github_accelerate_link", "gh-proxy.org") + "/" + link;
        }
        try {
            var fileName = extractFileName(link);
            var fileSizeNum = getFileSize(link);
            return LinkFileInfoPanel
                    .createBasicLinkFileInfoPanel(fileName, fileSizeNum, "HTTP", link);
        } catch (Exception e) {
            logger.error("Error parsing HTTP link: "+link, e);
        }
        return null;
    }




}
