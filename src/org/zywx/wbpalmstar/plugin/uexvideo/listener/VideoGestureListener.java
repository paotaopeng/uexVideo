package org.zywx.wbpalmstar.plugin.uexvideo.listener;

import android.view.MotionEvent;

/**
 * Created by Administrator on 2018/5/10.
 */

public interface VideoGestureListener {
    //亮度手势，手指在Layout左半部上下滑动时候调用
    public void onBrightnessGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

    //音量手势，手指在Layout右半部上下滑动时候调用
    public void onVolumeGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

    //快进快退手势，手指在Layout左右滑动的时候调用
    public void onFF_REWGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);

    //单击手势，确认是单击的时候调用
    public void onSingleTapGesture(MotionEvent e);

    //双击手势，确认是双击的时候调用
    public void onDoubleTapGesture(MotionEvent e);

    //按下手势，第一根手指按下时候调用
    public void onDown(MotionEvent e);

    //快进快退执行后的松开时候调用
    public void onEndFF_REW(MotionEvent e);
}
