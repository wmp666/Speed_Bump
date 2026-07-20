package com.wmp.downloader.ui.settings;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.ui.FunctionDialog;
import com.wmp.downloader.ui.settings.bilibili.BiliScanCodePanel;
import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.IOException;

public class BiliSettings extends BasicSpecialSettings {

    private static final Logger logger = Logger.getLogger(BiliSettings.class);


    private JPanel mainPanel;
    private JLabel loginIconLabel;
    private JLabel loginStatusLabel;
    private JButton ScanCodeButton;
    private JButton loggedOutButton;
    private JComboBox<String> qualityComboBox;
    private JLabel UserNameLabel;
    private JPanel loginPanel;

    @Override
    public String getSettingsName() {
        return "哔哩哔哩";
    }

    @Override
    public SpecialSettingsPanel getSettings() {
        return new BiliSpecialSettingsPanel(mainPanel);
    }

    public class BiliSpecialSettingsPanel extends SpecialSettingsPanel {

        public BiliSpecialSettingsPanel(JPanel panel) {
            this.setLayout(new BorderLayout());
            this.add(panel, BorderLayout.CENTER);
            UserNameLabel.putClientProperty("FlatLaf.style", "font: bold $h3.font");
            ScanCodeButton.putClientProperty("FlatLaf.style", "font: $h3.font");
            loggedOutButton.putClientProperty("FlatLaf.style", "font: $h3.font");

            initUserInfo();

            //初始化图标
            IconControl.addInDynamicConverter(() -> {
                loginIconLabel.setIcon(IconControl.getIcon("login", loginPanel.getPreferredSize().height));
            });

            //初始化监听
            ScanCodeButton.addActionListener(e -> {
                FunctionDialog.showDialog(this, "扫码登录",
                        new BiliScanCodePanel(),
                        result -> {
                            if (result == FunctionDialog.RESULT_OK){
                                initUserInfo();
                            }
                        }, FunctionDialog.DEFAULT_BUTTONS, 0,
                        null, 0);
            });
            loggedOutButton.addActionListener(e -> {
                DataControl.put("bili_sessdata", "");
                DataControl.save();
                initUserInfo();
            });
            /*qualityComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    DataControl.put("biliQuality", qualityComboBox.getSelectedItem());
                    DataControl.save();

                }
            });*/

        }

        public void initUserInfo(){
            try {
                Connection.Response response = Jsoup.connect("https://api.bilibili.com/x/web-interface/nav")
                        .header("Cookie", "SESSDATA=" + DataControl.get("bili_sessdata", "")) // 关键：在Header中设置Cookie
                        .header("User-Agent", "Mozilla/5.0")      // 设置UA，模拟浏览器
                        .ignoreContentType(true)                  // 忽略内容类型，处理JSON
                        .method(Connection.Method.GET)
                        .execute();
                JSONObject jsonObject = JSONObject.parseObject(response.body());
                var userData = jsonObject.getJSONObject("data");

                if (!userData.getBooleanValue("isLogin", false)){
                    UserNameLabel.setText("未登录");
                    loginStatusLabel.setText("无");
                    return;
                }

                logger.debug("用户信息: " + userData);
                String userName = userData.getString("uname");
                var userId = userData.getLongValue("mid", 0);
                boolean isVip = userData.getIntValue("vipStatus",0) == 1;//0:非会员，1:会员
                UserNameLabel.setText(userName);
                loginStatusLabel.setText("UID: " + userId + " " + (isVip ? " 会员" : " 非会员"));
                loggedOutButton.setEnabled(true);
            } catch (IOException e) {
                UserNameLabel.setText("未登录");
                loginStatusLabel.setText("无");
                loggedOutButton.setEnabled(false);
                logger.error("获取用户信息失败", e);
            }
        }

        @Override
        public void setDefaultButton() {
            getRootPane().setDefaultButton(ScanCodeButton);
        }

    }

}
