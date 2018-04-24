package org.zywx.wbpalmstar.plugin.uexvideo.vo;


import java.io.Serializable;

public class OpenVO implements Serializable {

    public String src;

    public String title;//视频标题

    public String exitMsgContent="";//提出提示内容

    public int startTime;

    public int endTime;

    public boolean autoStart = false;

    public boolean forceFullScreen = false;

    public boolean showCloseButton = false;

    public boolean showScaleButton = false;

    public boolean showCloseDialog =false;//关闭时显示确认窗口

    public int orientationAfterExit=1;//退出视频后的屏幕方向，默认竖屏

    public double width = -1;

    public double height = -1;

    public double x;

    public double y;

    public boolean scrollWithWeb = true;

    public boolean isAutoEndFullScreen = false;

    public boolean canSeek = true;//是否可以拖动进度条


}
