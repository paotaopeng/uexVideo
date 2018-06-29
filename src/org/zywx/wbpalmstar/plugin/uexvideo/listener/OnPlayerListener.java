package org.zywx.wbpalmstar.plugin.uexvideo.listener;

/**
 * Created by Administrator on 2018/4/24.
 */

public interface OnPlayerListener {
    void onPlayerCloseWarn();

    void onPlayerSeek(int position);

    void onWordClick(String param);
}
