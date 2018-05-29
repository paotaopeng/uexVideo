package org.zywx.wbpalmstar.plugin.uexvideo.vo;

/**
 * Created by Administrator on 2018/4/27.
 * 歌词字幕类
 */
public class Lyric {
    /**
     * 歌词开始时间
     */
    private String lyricStartTime;
    /**
     * 歌词结束时间
     */
    private String lyricEndTime;
    /**
     * 歌词中文文本
     */
    private String lyricChineseText;
    /**
     * 歌词英文文本
     */
    private String lyricEnglishText;
    /**
     * 歌词UUID属性
     */
    private String lyricUuidProperty;
    /**
     * 歌词recordAble属性
     */
    private String lyricRecordAbleProperty;

    public String getLyricStartTime() {
        return lyricStartTime;
    }

    public void setLyricStartTime(String lyricStartTime) {
        this.lyricStartTime = lyricStartTime;
    }

    public String getLyricEndTime() {
        return lyricEndTime;
    }

    public void setLyricEndTime(String lyricEndTime) {
        this.lyricEndTime = lyricEndTime;
    }

    public String getLyricChineseText() {
        return lyricChineseText;
    }

    public void setLyricChineseText(String lyricChineseText) {
        this.lyricChineseText = lyricChineseText;
    }

    public String getLyricEnglishText() {
        return lyricEnglishText;
    }

    public void setLyricEnglishText(String lyricEnglishText) {
        this.lyricEnglishText = lyricEnglishText;
    }

    public String getLyricUuidProperty() {
        return lyricUuidProperty;
    }

    public void setLyricUuidProperty(String lyricUuidProperty) {
        this.lyricUuidProperty = lyricUuidProperty;
    }

    public String getLyricRecordAbleProperty() {
        return lyricRecordAbleProperty;
    }

    public void setLyricRecordAbleProperty(String lyricRecordAbleProperty) {
        this.lyricRecordAbleProperty = lyricRecordAbleProperty;
    }
}
