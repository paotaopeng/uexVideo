package org.zywx.wbpalmstar.plugin.uexvideo.vo;


import java.io.Serializable;

public class OpenVO implements Serializable {

    public String src;

    public int startTime;

    public int endTime;

    public boolean autoStart=false;

    public boolean forceFullScreen=false;

    public boolean showCloseButton=false;

    public boolean showScaleButton=false;

    public int width=-1;

    public int height=-1;

    public int x;

    public int y;

    public boolean scrollWithWeb=true;

}
