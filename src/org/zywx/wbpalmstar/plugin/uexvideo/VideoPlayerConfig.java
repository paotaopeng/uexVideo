package org.zywx.wbpalmstar.plugin.uexvideo;


import java.io.Serializable;

public class VideoPlayerConfig implements Serializable {

    private int x;
    private int y;
    private int width;
    private int height;
    private String src;
    private int startTime;
    private boolean autoStart;
    private boolean forceFullScreen;
    private boolean showCloseButton;
    private boolean showScaleButton;
    private boolean scrollWithWeb;

    public VideoPlayerConfig() {
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }


    public void setShowScaleButton(boolean showScaleButton) {
        this.showScaleButton = showScaleButton;
    }

    public boolean getAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean getForceFullScreen() {
        return forceFullScreen;
    }

    public void setForceFullScreen(boolean forceFullScreen) {
        this.forceFullScreen = forceFullScreen;
    }

    public boolean getShowCloseButton() {
        return showCloseButton;
    }

    public void setShowCloseButton(boolean showCloseButton) {
        this.showCloseButton = showCloseButton;
    }

    public boolean getShowScaleButton() {
        return showScaleButton;
    }

    public boolean getScrollWithWeb() {
        return scrollWithWeb;
    }

    public void setScrollWithWeb(boolean scrollWithWeb) {
        this.scrollWithWeb = scrollWithWeb;
    }

    public void setScrollWithWeb(Boolean scrollWithWeb) {
        this.scrollWithWeb = scrollWithWeb;
    }
}
