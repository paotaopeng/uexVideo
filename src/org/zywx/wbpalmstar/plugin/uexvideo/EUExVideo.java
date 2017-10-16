/*
 *  Copyright (C) 2014 The AppCan Open Source Project.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.

 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.

 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.zywx.wbpalmstar.plugin.uexvideo;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexvideo.lib.VideoCaptureActivity;
import org.zywx.wbpalmstar.plugin.uexvideo.lib.configuration.CaptureConfiguration;
import org.zywx.wbpalmstar.plugin.uexvideo.lib.configuration.PredefinedCaptureConfigurations;
import org.zywx.wbpalmstar.plugin.uexvideo.vo.OpenVO;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class EUExVideo extends EUExBase implements Parcelable {

    public static final int F_ACT_REQ_CODE_UEX_VIDEO_RECORD = 5;
    private static final int REQUEST_VIDEO_PICKER = 1003;
    public static final String F_CALLBACK_NAME_VIDEO_RECORD_FINISH = "uexVideo.onRecordFinish";
    public static final String F_CALLBACK_ON_PLAYER_CLOSE = "uexVideo.onPlayerClose";
    public static final String F_CALLBACK_ON_PLAYER_STATUS_CHANGE = "uexVideo.onPlayerStatusChange";
    public static final String F_CALLBACK_ON_PLAYER_FINISH = "uexVideo.onPlayerFinish";
    public static final String F_CALLBACK_ON_PLAYER_ENDTIME = "uexVideo.onPlayerEndTime";
    public static final String CALLBACK_ON_VIDEO_PICKER_CLOSED = "uexVideo.onVideoPickerClosed";

    private ResoureFinder finder;

    private View mMapDecorView;
    private static LocalActivityManager mgr;
    private String TAG = "EUExVideo";

    private boolean scrollWithWeb = false;

    private String ViewPlayerViewTag = "Video_Player_View";

    public EUExVideo(Context context, EBrowserView inParent) {
        super(context, inParent);
        finder = ResoureFinder.getInstance(context);
    }

    public static void onActivityResume(Context context) {
        if (mgr != null) {
            mgr.dispatchResume();
        }
    }

    public static void onActivityPause(Context context) {
        if (mgr != null) {
            mgr.dispatchPause(((Activity) context).isFinishing());
        }
    }

    /**
     * 打开视频播放器
     */
    public void open(String[] params) {
        if (params.length < 1) {
            return;
        }
        Intent intent = new Intent();
        String fullPath = params[0];
        Log.i("uexVideo", fullPath);
        if (fullPath == null || fullPath.length() == 0) {
            errorCallback(0,
                    EUExCallback.F_ERROR_CODE_VIDEO_OPEN_ARGUMENTS_ERROR,
                    finder.getString("path_error"));
            Log.i("uexVideo", "path_error");
            return;
        }
        String realPath = BUtility.makeRealPath(fullPath, mBrwView);
        Uri url = Uri.parse(realPath);
        intent.setData(url);
        intent.setClass(mContext, VideoPlayerActivity.class);
        mContext.startActivity(intent);
    }

    /**
     * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
     */
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 打开视频播放器(新的view)
     */
    public void openPlayer(final String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        WindowManager wm = ((Activity) mContext).getWindowManager();

        int screenWidth = wm.getDefaultDisplay().getWidth();
        int screenHeight = wm.getDefaultDisplay().getHeight();
        Log.i(TAG, "screenWidth:" + screenWidth + "     screenHeight:" + screenHeight);
        final OpenVO openVO = DataHelper.gson.fromJson(params[0], OpenVO.class);

        String src = openVO.src;
        if (TextUtils.isEmpty(src)) {
            errorCallback(0, EUExCallback.F_ERROR_CODE_VIDEO_OPEN_ARGUMENTS_ERROR, finder.getString("path_error"));
            Log.i(TAG, "src is empty");
            return;
        }
        int startTime = openVO.startTime;
        boolean autoStart = openVO.autoStart;
        boolean forceFullScreen = openVO.forceFullScreen;
        boolean showCloseButton = openVO.showCloseButton;
        boolean showScaleButton = openVO.showScaleButton;

        final int width = (int) openVO.width;
        //final int width = px2dip(mContext, (float) openVO.width);
        final int height = (int) openVO.height;
        // final int height = px2dip(mContext, (float) openVO.height);
        final int x = (int) openVO.x;

        final int y = (int) openVO.y;


        scrollWithWeb = openVO.scrollWithWeb;

        //如果设置了强制全屏，则一定要显示"关闭"按钮，不显示scaleButton, 不允许跟随网页滑动。
        if (forceFullScreen) {
            showCloseButton = true;
            showScaleButton = false;
            scrollWithWeb = false;
            openVO.scrollWithWeb = false;
            openVO.showCloseButton = true;
            openVO.showScaleButton = false;
        }

        ((Activity) mContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (mMapDecorView != null) {
                    Log.i("corVideo", "already open");
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra("playerConfig", openVO);
                intent.putExtra("EUExVideo", EUExVideo.this);
                intent.setClass(mContext, VideoPlayerActivityForViewToWeb.class);
                //mContext.startActivity(intent);
                if (mgr == null) {
                    mgr = new LocalActivityManager((Activity) mContext, true);
                    mgr.dispatchCreate(null);
                }
                Window window = mgr.startActivity("TAG_Video", intent);
                mMapDecorView = window.getDecorView();

                RelativeLayout.LayoutParams lp;
                if (openVO.forceFullScreen) {
                    lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT);
                    lp.leftMargin = 0;
                    lp.topMargin = 0;
                } else {
                    lp = new RelativeLayout.LayoutParams(width, height);
                    lp.leftMargin = x;
                    lp.topMargin = y;
                }
                if (openVO.scrollWithWeb) {
                    AbsoluteLayout.LayoutParams layoutParams = new AbsoluteLayout.LayoutParams(width, height, x, y);
                    addViewToWebView(mMapDecorView, layoutParams, "Video_Player_View");
                } else {
                    addView2CurrentWindow(mMapDecorView, lp);
                }
            }
        });


    }

    private void addView2CurrentWindow(View child, RelativeLayout.LayoutParams params) {
        int l = params.leftMargin;
        int t = params.topMargin;
        int w = params.width;
        int h = params.height;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.gravity = Gravity.NO_GRAVITY;
        lp.leftMargin = l;
        lp.topMargin = t;
        // adptLayoutParams(params, lp);
        // Log.i(TAG, "addView2CurrentWindow");
        mBrwView.addViewToCurrentWindow(child, lp);
    }


    /**
     * 关闭播放器
     */
    public void closePlayer(String[] params) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mMapDecorView != null) {
                    if (scrollWithWeb) {
                        removeViewFromWebView(ViewPlayerViewTag);
                    } else {
                        removeViewFromCurrentWindow(mMapDecorView);
                    }
                    mMapDecorView = null;
                    mgr.destroyActivity("TAG_Video", true);
                }
            }
        });
    }

    public void videoPicker(String[] params) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_VIDEO_PICKER);
    }

    public void closePlayerCallBack(String src, int progress) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("src", src);
            jsonObject.put("currentTime", progress); //返回的时间单位为秒
            callBackPluginJs(EUExVideo.F_CALLBACK_ON_PLAYER_CLOSE, jsonObject.toString());
        } catch (JSONException e) {
            Log.i(TAG, String.valueOf(e.getMessage()));
        }
    }

    public void record(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        String json = params[0];
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(json);
            //默认无时间限制
            int maxDuration = jsonObject.optInt("maxDuration", -1);
            //默认输出的格式是mp4，Android当前支持mp4和3gp两种
            String fileType = jsonObject.optString("fileType", "mp4");
            //默认的采样频率为高采样率，录制的视屏质量高, 取值为0, 1, 2, 默认为0, 0: 高采样率, 1: 中采样率, 2: 低采样率
            int rateType = jsonObject.optInt("bitRateType", 0);
            //默认的视频尺寸 取值为0,1,2,默认为0。0:1920x1080, 1:1280x720, 2:640x480
            int qualityType = jsonObject.optInt("qualityType", 0);
            PredefinedCaptureConfigurations.CaptureResolution resolution;
            switch (qualityType) {
                case 0:
                    resolution = PredefinedCaptureConfigurations.CaptureResolution.RES_1080P;
                    break;
                case 1:
                    resolution = PredefinedCaptureConfigurations.CaptureResolution.RES_720P;
                    break;
                case 2:
                    resolution = PredefinedCaptureConfigurations.CaptureResolution.RES_480P;
                    break;
                default:
                    resolution = PredefinedCaptureConfigurations.CaptureResolution.RES_1080P;
                    break;
            }
            //控制采样率
            PredefinedCaptureConfigurations.CaptureQuality bitRate;
            switch (rateType) {
                case 0:
                    bitRate = PredefinedCaptureConfigurations.CaptureQuality.HIGH;
                    break;
                case 1:
                    bitRate = PredefinedCaptureConfigurations.CaptureQuality.MEDIUM;
                    break;
                case 2:
                    bitRate = PredefinedCaptureConfigurations.CaptureQuality.LOW;
                    break;
                default:
                    bitRate = PredefinedCaptureConfigurations.CaptureQuality.HIGH;
            }

            CaptureConfiguration config = new CaptureConfiguration(resolution,
                    bitRate, maxDuration, -1);
            config.setOutputFormat(fileType);
            String fileName = "temp.mp4";
            if ("3gp".equalsIgnoreCase(fileType)) {
                fileName = new Date().getTime() + ".3gp";
            } else {
                fileName = new Date().getTime() + ".mp4";
            }

            final Intent intent = new Intent(mContext, VideoCaptureActivity.class);
            intent.putExtra(VideoCaptureActivity.EXTRA_CAPTURE_CONFIGURATION, config);
            intent.putExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME, fileName);
            startActivityForResult(intent, F_ACT_REQ_CODE_UEX_VIDEO_RECORD);
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
    }

    private String getNmae() {
        Date date = new Date();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
        return "video/scan" + df.format(date) + ".3gp";
    }

    private void checkPath() {
        String widgetPath = mBrwView.getCurrentWidget().getWidgetPath()
                + "video";
        File temp = new File(widgetPath);
        if (!temp.exists()) {
            temp.mkdirs();
        } else {
            File[] files = temp.listFiles();
            if (files.length >= 20) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        JSONObject jsonObject = new JSONObject();
        try {
            // 视频录制
            if (requestCode == F_ACT_REQ_CODE_UEX_VIDEO_RECORD) {
                //录制成功
                if (resultCode == Activity.RESULT_OK) {
                    jsonObject.put("result", 0);
                    jsonObject.put("path", data.getStringExtra(VideoCaptureActivity.EXTRA_OUTPUT_FILENAME));
                    callBackPluginJs(F_CALLBACK_NAME_VIDEO_RECORD_FINISH, jsonObject.toString());
                    return;
                }
                //用户取消
                if (resultCode == Activity.RESULT_CANCELED) {
                    jsonObject.put("result", 1);
                    callBackPluginJs(F_CALLBACK_NAME_VIDEO_RECORD_FINISH, jsonObject.toString());
                    return;
                }
                return;
            }
            // 视频录制操作出错
            if (requestCode == VideoCaptureActivity.RESULT_ERROR) {
                jsonObject.put("result", 2);
                callBackPluginJs(F_CALLBACK_NAME_VIDEO_RECORD_FINISH, jsonObject.toString());
                return;
            }
            // 选择视频
            if (requestCode == REQUEST_VIDEO_PICKER) {
                if (resultCode == Activity.RESULT_OK) {
                    Uri selectedVideo = data.getData();
                    String[] filePathColumn = {MediaStore.Video.Media.DATA};
                    Cursor cursor = mContext.getContentResolver().query(selectedVideo,
                            filePathColumn, null, null, null);
                    if (cursor == null) {
                        errorCallback(0, 0, "uexImage 选择视频 失败");
                        return;
                    }
                    try {
                        JSONArray dataList = new JSONArray();
                        while (cursor.moveToNext()) {
                            JSONObject video = new JSONObject();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            String videoPath = cursor.getString(columnIndex);
                            video.put("src", videoPath);
                            dataList.put(video);
                        }
                        jsonObject.put("data", dataList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cursor.close();
                    jsonObject.put("isCancelled", false);
                } else {
                    jsonObject.put("isCancelled", true);
                }
                callBackPluginJsByJSON(CALLBACK_ON_VIDEO_PICKER_CLOSED, jsonObject.toString());
            }
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }
    }

    @Override
    protected boolean clean() {
        Log.i("uexVideo", "clean");
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public void callBackPluginJs(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        onCallback(js);
    }

    private void callBackPluginJsByJSON(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "(" + jsonData + ");}";
        onCallback(js);
    }

}
