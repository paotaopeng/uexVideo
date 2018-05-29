package org.zywx.wbpalmstar.plugin.uexvideo.listener;

import android.support.annotation.IntDef;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Administrator on 2018/5/10.
 */

public class VideoPlayerOnGestureListener extends SimpleOnGestureListener {
    private static final String TAG = "GestureListener";
    private static final int NONE = 0, VOLUME = 1, BRIGHTNESS = 2, FF_REW = 3;
    private VideoGestureListener mVideoGestureListener;
    //横向偏移检测，让快进快退不那么敏感
    private int offsetX = 1;
    private boolean hasFF_REW = false;
    private
    @ScrollMode
    int mScrollMode = NONE;
    private View view;

    @IntDef({NONE, VOLUME, BRIGHTNESS, FF_REW})
    @Retention(RetentionPolicy.SOURCE)
    private @interface ScrollMode {
    }


    public VideoPlayerOnGestureListener(View view, VideoGestureListener videoGestureListener) {
        this.view = view;
        this.mVideoGestureListener = videoGestureListener;
    }

    public boolean isHasFF_REW() {
        return hasFF_REW;
    }

    public void setHasFF_REW(boolean hasFF_REW) {
        this.hasFF_REW = hasFF_REW;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.d(TAG, "onDown: ");
        hasFF_REW = false;
        //每次按下都重置为NONE
        mScrollMode = NONE;
        if (mVideoGestureListener != null) {
            mVideoGestureListener.onDown(e);
        }
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "onScroll: e1:" + e1.getX() + "," + e1.getY());
        Log.d(TAG, "onScroll: e2:" + e2.getX() + "," + e2.getY());
        Log.d(TAG, "onScroll: X:" + distanceX + "  Y:" + distanceY);
        switch (mScrollMode) {
            case NONE:
                Log.d(TAG, "NONE: ");
                //offset是让快进快退不要那么敏感的值
                if (Math.abs(distanceX) - Math.abs(distanceY) > offsetX) {
                    mScrollMode = FF_REW;
                } else {
                    if (e1.getX() < view.getWidth() / 2) {
                        mScrollMode = BRIGHTNESS;
                    } else {
                        mScrollMode = VOLUME;
                    }
                }
                break;
            case VOLUME:
                if (mVideoGestureListener != null) {
                    mVideoGestureListener.onVolumeGesture(e1, e2, distanceX, distanceY);
                }
                Log.d(TAG, "VOLUME: ");
                break;
            case BRIGHTNESS:
                if (mVideoGestureListener != null) {
                    mVideoGestureListener.onBrightnessGesture(e1, e2, distanceX, distanceY);
                }
                Log.d(TAG, "BRIGHTNESS: ");
                break;
            case FF_REW:
                if (mVideoGestureListener != null) {
                    mVideoGestureListener.onFF_REWGesture(e1, e2, distanceX, distanceY);
                }
                hasFF_REW = true;
                Log.d(TAG, "FF_REW: ");
                break;
        }
        return true;
    }


    @Override
    public boolean onContextClick(MotionEvent e) {
        Log.d(TAG, "onContextClick: ");
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.d(TAG, "onDoubleTap: ");
        if (mVideoGestureListener != null) {
            mVideoGestureListener.onDoubleTapGesture(e);
        }
        return super.onDoubleTap(e);
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(TAG, "onLongPress: ");
        super.onLongPress(e);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d(TAG, "onDoubleTapEvent: ");
        return super.onDoubleTapEvent(e);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d(TAG, "onSingleTapUp: ");
        return super.onSingleTapUp(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.d(TAG, "onFling: ");
        return super.onFling(e1, e2, velocityX, velocityY);
    }


    @Override
    public void onShowPress(MotionEvent e) {
        Log.d(TAG, "onShowPress: ");
        super.onShowPress(e);
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        Log.d(TAG, "onSingleTapConfirmed: ");
        if (mVideoGestureListener != null) {
            mVideoGestureListener.onSingleTapGesture(e);
        }
        return super.onSingleTapConfirmed(e);
    }
}
