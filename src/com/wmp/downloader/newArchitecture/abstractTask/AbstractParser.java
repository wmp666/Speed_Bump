package com.wmp.downloader.newArchitecture.abstractTask;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;

public abstract class AbstractParser {

    public abstract String getID();

    public abstract String getSupportTip();

    public Info getParserInfo(String link) {
        return new Info(link);
    }


    /**
     * 需要设置 isMeetRequirements ，infos
     * @param link
     */
    protected abstract void updateLinkInfo(String link);

    protected abstract AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info);


    public abstract boolean isMeetRequirements(String link);


    protected abstract AbstractTask getTask(String link, JSONObject infoJson);

    /**
     * 获取设置页
     *
     * @return 设置页，没有返回null
     */
    public abstract AbstractSpecialSettingsPage getSettingsPage();

    public class Info{
        private final String link;

        public Info(String link) {
            this.link = link;
            updateLinkInfo(link);
        }

        public String getParserID(){
            return AbstractParser.this.getID();
        }


        /**
         * 需要设置 isMeetRequirements ，infos
         * @param link
         */
        protected void updateLinkInfo(String link){
            AbstractParser.this.updateLinkInfo(link);
        };

        public String getLink() {
            return link;
        }

        public AbstractLinkInfoPanel getLinkedInfoPanel(){
            return AbstractParser.this.getLinkedInfoPanel(link, this);
        }



        public AbstractTask getTask(JSONObject infoJson){
            return AbstractParser.this.getTask(link, infoJson);
        }

        @Override
        public String toString() {
            return "Info{" +
                    "link='" + link + '\'' +
                    '}';
        }
    }
}
