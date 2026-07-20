package com.wmp.downloader.ui.task.bilibili;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.wmp.downloader.tools.DataControl;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel.formatFileSize;

public class BiliTaskFolderEditPanel extends JPanel {

    private static final Logger logger = Logger.getLogger(BiliTaskFolderEditPanel.class);

    private JTextField folderNameTextField;
    private JPanel mainPanel;
    private JComboBox<String> QualityComboBox;
    private int[] quality_int = new int[0];



    public BiliTaskFolderEditPanel(String name, int quality, int[] quality_int, String[] quality_str) {
        this.setLayout(new BorderLayout());
        this.add(mainPanel);


        Map<Integer, String> qualityMap = new HashMap<>();
        for (int i = 0; i < quality_int.length; i++) {
            qualityMap.put(quality_int[i], quality_str[i]);
        }
        logger.info("画质数据：" + qualityMap);

        this.quality_int = quality_int;
        QualityComboBox.removeAllItems();
        for (var s : quality_str) {
            QualityComboBox.addItem(s);
        }

        folderNameTextField.setText(name);

        var qualityInt = new ArrayList<>();
        for (int i : quality_int) {
            qualityInt.add(i);
        }
        QualityComboBox.setSelectedItem(quality_str[qualityInt.indexOf(quality)]);

    }
    public String getFileName() {
        return folderNameTextField.getText();
    }

    public void setFileName(String name) {
        folderNameTextField.setText(name);
    }

    public int getQuality() {
        return quality_int[QualityComboBox.getSelectedIndex()];
    }
}
