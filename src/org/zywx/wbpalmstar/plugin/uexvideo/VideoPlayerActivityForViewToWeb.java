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
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.plugin.uexvideo.listener.OnPlayerListener;
import org.zywx.wbpalmstar.plugin.uexvideo.listener.VideoGestureListener;
import org.zywx.wbpalmstar.plugin.uexvideo.listener.VideoPlayerOnGestureListener;
import org.zywx.wbpalmstar.plugin.uexvideo.util.BrightnessUtils;
import org.zywx.wbpalmstar.plugin.uexvideo.util.JsonUtils;
import org.zywx.wbpalmstar.plugin.uexvideo.util.NetworkUtils;
import org.zywx.wbpalmstar.plugin.uexvideo.util.UIUtils;
import org.zywx.wbpalmstar.plugin.uexvideo.vo.Lyric;
import org.zywx.wbpalmstar.plugin.uexvideo.vo.OpenVO;

import java.util.List;

/**
 * TODO
 * 1.scrollWithWeb
 * 2.添加close  Button
 */
public class VideoPlayerActivityForViewToWeb extends Activity implements OnPreparedListener, OnClickListener,
        OnSeekBarChangeListener, OnCompletionListener, OnErrorListener, OnVideoSizeChangedListener,
        OnBufferingUpdateListener, OnPlayerListener,VideoGestureListener {

    public static final String TAG = "VideoPlayerActivity";

    private final static int ACTION_UPDATE_PASS_TIME = 1;
    private final static int ACTION_HIDE_CONTROLLER = 2;
    private final static int MODE_FULL_SCEEN = 2;// 全屏
    private final static int MODE_SCALE = 1;// 正常
    private final static int STATE_INIT = 0;
    private final static int STATE_PREPARED = 1;
    private final static int STATE_PLAYING = 2;
    private final static int STATE_PAUSE = 3;
    private final static int STATE_STOP = 4;
    private final static int STATE_RELEASED = 5;
    private final static int CONTROLLERS_HIDE_DURATION = 3000;
    private int curerntState = STATE_INIT;
    private int lastPlayPostion;
    private SurfaceView m_display;
    private SurfaceHolder surfaceHolder;
    private ImageView ivClose;
    private ImageView ivBufferProgress;

    private ImageView m_ivPlayPause;
    private ImageView m_ivScreenAdjust;
    private SeekBar m_sbTimeLine;
    private TextView m_tvPassTime;
    private RelativeLayout m_bottomLayer;
    private MediaPlayer mediaPlayer;

    private int screenWidth;
    private int screenHeight;

    private int videoWidth;
    private int videoHeight;
    private double startTime;
    private double endTime;//试看的结束时间
    private boolean autoStart; //是否自动播放
    private boolean forceFullScreen; //是否强制全屏显示
    private boolean showCloseButton; //是否显示 关闭 按钮
    private boolean showScaleButton; //是否显示 缩放 按钮
    private boolean showCloseDialog; //是否显示关闭确认对话框
    private boolean scrollWithWeb;
    private boolean isAutoEndFullScreen;//是否自动结束全屏播放
    private boolean canSeek;//是否允许拖动进度条
    private String title;//视频标题
    private String exitMsgContent;//退出视频标题
    private int passTime;
    private int totalTime;
    private int orientation;//屏幕方向
    private int displayMode = MODE_SCALE;
    private boolean isUserSeekingBar = false;
    private AlphaAnimation fadeInAnim;
    private AlphaAnimation fadeOutAnim;
    private String videoPath;
    private ResoureFinder finder;
    private EUExVideo mUexBaseObj;
    private static final int PLAYER_STATUS_PAUSE = 0;
    private static final int PLAYER_STATUS_BUFFERING = 1;
    private static final int PLAYER_STATUS_PLAYING = 2;
    private static final int PALYER_STATUS_ERROR = 3;
    //字幕相关
    private List<Lyric> lyricList;
    private ImageView ivPreLyric;
    private ImageView ivNextLyric;
    private RelativeLayout lyricLayout;
    private TextView tvLyric;
    private RelativeLayout pluginVideoLayout;
    private int lyricListSize;
    private RelativeLayout titleBar;
    private TextView tvTitle;
    private ImageView ivPlay;
    private TextView tvBufferProgress;
    private LinearLayout linerBufferProgress;
    private static final int deviation=5000;

    //手势控制相关
    private AudioManager mAudioManager;
    private int maxVolume = 0;
    private int oldVolume = 0;
    private float brightness = 1;
    private LinearLayout gestureProgressBar;
    private ImageView ivGestureProgress;
    private TextView tvGestureProgrss;
    private VideoPlayerOnGestureListener mOnGestureListener;
    private GestureDetector gestureDetector;
    private int seekedPosition;
    private int curPositoin;
    private LinearLayout noNetworkLayout;
    private TextView reloadBtn;
    private TextView watchInNoneWifi;
    private LinearLayout noWifiLayout;

    @Override
    public void onPlayerCloseWarn() {
        closePlayerHandler();
    }

    @Override
    public void onPlayerSeek(int position) {
        mediaPlayer.seekTo(position);
        mediaPlayer.start();
        Log.e(TAG,"position:"+position+"  curPosition:"+mediaPlayer.getCurrentPosition());
        curerntState = STATE_PLAYING;
        onPlayerStatusChange(PLAYER_STATUS_PLAYING);
        m_ivPlayPause.setBackgroundDrawable(finder.getDrawable("plugin_video_pause"));
        ivPlay.setVisibility(View.GONE);
        handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
    }



    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_UPDATE_PASS_TIME:
                    if (!isUserSeekingBar && mediaPlayer != null) {
                        passTime = mediaPlayer.getCurrentPosition(); // 播放器状态异常捕捉
                        m_tvPassTime.setText(formatTime(passTime) + "/" + formatTime(totalTime));
                        m_sbTimeLine.setProgress(passTime);
                    }
                    if (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE) {
                        handler.sendEmptyMessageDelayed(ACTION_UPDATE_PASS_TIME, 1000);
                    }
                    break;
                case ACTION_HIDE_CONTROLLER:
                    m_bottomLayer.setVisibility(View.GONE);
                    m_bottomLayer.setAnimation(fadeOutAnim);
                    titleBar.setVisibility(View.GONE);
                    titleBar.setAnimation(fadeOutAnim);
                    UIUtils.hideStatusBar(VideoPlayerActivityForViewToWeb.this);
                    break;
            }
        }

        ;
    };

    private int x_activity = 0;
    private int y_activity = 0;
    private int w_activity = 0;
    private int h_activity = 0;
    //    private int curLyricIndex=-1;
    private boolean hasLyric = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finder = ResoureFinder.getInstance(this);
        getWindow().getDecorView().setBackgroundDrawable(null);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final Intent intent = getIntent();
        if (intent == null || ((OpenVO) intent.getSerializableExtra("playerConfig")) == null) {// 路径不存在
            alertMessage("invalid params", true);
            return;
        }
        OpenVO config = (OpenVO) intent.getSerializableExtra("playerConfig");
        String lyric = config.lyric;
        if (!lyric.isEmpty()) {
            hasLyric = true;
            lyricList = JsonUtils.json2List(lyric, Lyric[].class);
            lyricListSize = lyricList.size();
        }
        videoPath = config.src;
        if (TextUtils.isEmpty(videoPath)) {
            Log.i(TAG, "[invalid params]: videoPath can not be null");
            alertMessage("invalid params", true);
        }

        x_activity = (int) config.x * getDensity();
        y_activity = (int) config.y * getDensity();
        w_activity = (int) config.width * getDensity();
        h_activity = (int) config.height * getDensity();
        mUexBaseObj = intent.getParcelableExtra("EUExVideo");
        mUexBaseObj.setOnPlayerListener(this);
        orientation = config.orientationAfterExit;
        startTime = config.startTime;
        endTime = config.endTime * 1000;
        autoStart = config.autoStart;
        showScaleButton = config.showScaleButton;
        forceFullScreen = config.forceFullScreen;
        showCloseButton = config.showCloseButton;
        showCloseDialog = config.showCloseDialog;
        scrollWithWeb = config.scrollWithWeb;
        isAutoEndFullScreen = config.isAutoEndFullScreen;
        canSeek = config.canSeek;
        title = config.title;
        exitMsgContent = config.exitMsgContent;
        setScreenSize();
        setContentView(finder.getLayoutId("plugin_video_player_main2"));
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制横屏
        initViews();
        m_display.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        m_display.getHolder().setKeepScreenOn(true);
        m_display.getHolder().addCallback(callback);
        handler.post(checkPlayTimeRunnable);
        handler.post(showLyricTextRunnable);
        UIUtils.setStatusBarTransparent(this.getParent());
        //初始化获取音量属性
        mAudioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //下面这是设置当前APP亮度的方法配置
        brightness =BrightnessUtils.getWindowBrightness(this.getParent());
    }



    private void setScreenSize() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        Log.i(TAG, "setScreenSize:" + screenWidth + "," + screenHeight);
    }

    Runnable checkPlayTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (endTime != 0 && mediaPlayer != null && mediaPlayer.getCurrentPosition() > endTime) {
                onPlayEndTime();
            }
            handler.postDelayed(this, 500);

        }
    };

    Runnable showLyricTextRunnable = new Runnable() {
        @Override
        public void run() {
                if (hasLyric) {
                    showLyricText();
                }
                handler.postDelayed(this, 500);
        }
    };

    Runnable mHideRunnable  = new Runnable() {
        @Override
        public void run() {
            gestureProgressBar.setVisibility(View.GONE);
        }
    };

    private void initViews() {
        initAnimation();
        m_display = (SurfaceView) findViewById(finder.getId("plugin_video_player_sv_diaplay"));
        mOnGestureListener = new VideoPlayerOnGestureListener(m_display,this);
        gestureDetector = new GestureDetector(this, mOnGestureListener);
        m_display.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (mOnGestureListener.isHasFF_REW()) {
                        onEndFF_REW(event);
                        mOnGestureListener.setHasFF_REW(false);
                    }
                }
                //监听触摸事件
                return gestureDetector.onTouchEvent(event);
            }
        });
        //标题栏相关
        titleBar = (RelativeLayout) findViewById(finder.getId("plugin_video_player_title_bar"));
        if (showCloseButton) {
            titleBar.setVisibility(View.VISIBLE);
        }
        ivClose = (ImageView) findViewById(finder.getId("plugin_video_player_iv_close"));
        ivClose.setOnClickListener(this);
        tvTitle = (TextView) findViewById(finder.getId("plugin_video_player_title"));
        tvTitle.setText(title);


        //缓冲进度条相关布局控件

        linerBufferProgress =(LinearLayout)findViewById(finder.getId("plugin_video_layout_buffer_progress_bar"));
        ivBufferProgress = (ImageView) findViewById(finder.getId("plugin_video_iv_buffer_progress_bar"));
        tvBufferProgress =(TextView)findViewById(finder.getId("plugin_video_tv_buffer_progress_bar"));


        //播放进度条相关布局控件
        m_bottomLayer = (RelativeLayout) findViewById(finder.getId("plugin_video_player_bottom_layout"));//视频播放进度条控制栏
        m_bottomLayer.setOnClickListener(this);
        m_tvPassTime = (TextView) findViewById(finder.getId("plugin_video_player_tv_pass_time")); //播放进度时间显示
        m_sbTimeLine = (SeekBar) findViewById(finder.getId("plugin_video_player_sb_timeline"));//播放进度条
        if (canSeek) {
            m_sbTimeLine.setOnSeekBarChangeListener(this);
        } else {
            m_sbTimeLine.setEnabled(false);
        }
        m_ivPlayPause = (ImageView) findViewById(finder.getId("plugin_video_player_play_pause"));//进度条上的播放暂停按钮
        m_ivPlayPause.setOnClickListener(this);
        //字幕相关
        lyricLayout = (RelativeLayout) findViewById(finder.getId("plugin_video_player_lyric_layout"));
        if (!hasLyric) {
            lyricLayout.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) m_bottomLayer.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, finder.getId("plugin_video_layout"));
        }
        ivPreLyric = (ImageView) findViewById(finder.getId("plugin_video_player_pre"));//上一句
        ivPreLyric.setOnClickListener(this);
        ivNextLyric = (ImageView) findViewById(finder.getId("plugin_video_player_next"));//下一句
        ivNextLyric.setOnClickListener(this);
        tvLyric = (TextView) findViewById(finder.getId("plugin_video_player_lyric_text"));//字幕

        ivPlay =(ImageView)findViewById(finder.getId("plugin_video_play_button"));//播放按钮
        ivPlay.setOnClickListener(this);

        //缩放按钮
        m_ivScreenAdjust = (ImageView) findViewById(finder.getId("plugin_video_player_iv_screen_adjust"));//缩放按钮
        //如果显示缩放按钮
        if (showScaleButton) {
            m_ivScreenAdjust.setVisibility(View.VISIBLE);
            m_ivScreenAdjust.setOnClickListener(this);
        } else {
            m_ivScreenAdjust.setVisibility(View.GONE);
        }
        if (forceFullScreen) {
            m_ivScreenAdjust.setVisibility(View.GONE);
        } else {
            m_ivScreenAdjust.setOnClickListener(this);
        }

        //手势控制相关
        gestureProgressBar=(LinearLayout)findViewById(finder.getId("plugin_video_layout_gesture_progress_bar"));
        ivGestureProgress=(ImageView)findViewById(finder.getId("plugin_video_iv_gesture_progress_bar"));
        tvGestureProgrss=(TextView)findViewById(finder.getId("plugin_video_tv_gesture_progress_bar"));

        //无网
        noNetworkLayout=(LinearLayout)findViewById(finder.getId("plugin_video_layout_none_network"));
        reloadBtn=(TextView)findViewById(finder.getId("plugin_video_tv_none_network_reload"));
        reloadBtn.setOnClickListener(this);

        //非Wifi
        noWifiLayout=(LinearLayout)findViewById(finder.getId("plugin_video_layout_none_wifi"));
        watchInNoneWifi=(TextView)findViewById(finder.getId("plugin_video_tv_none_wifi"));
        watchInNoneWifi.setOnClickListener(this);
    }

    private void closePlayerHandler() {
        if (showCloseDialog) {
            pauseVideoHandler();
            new AlertDialog.Builder(this).setTitle("确定退出?").setMessage(exitMsgContent)
                    .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            releaseMediaPlayer();
                            dialog.dismiss();
                            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                            mUexBaseObj.closePlayer(null);
                        }
                    }).setNegativeButton("继续观看", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    playVideoHandler();
                }
            }).show();
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            mUexBaseObj.closePlayer(null);
        }
    }

    private void initMediaPlayer(String path) {
        if (surfaceHolder == null) {
            return;
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnVideoSizeChangedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mediaPlayer) {
            }
        });
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if(what == MediaPlayer.MEDIA_INFO_BUFFERING_START){
                    showProgressDialog();
                }else if(what == MediaPlayer.MEDIA_INFO_BUFFERING_END){
                    //此接口每次回调完START就回调END,若不加上判断就会出现缓冲图标一闪一闪的卡顿现象
                    if(mp.isPlaying()){
                        cancelProgressDialog();
                    }
                }
                return true;

            }
        });
        mediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            @Override
            public void onTimedText(MediaPlayer mediaPlayer, TimedText timedText) {
                closePlayerHandler();
            }
        });
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            if (path.startsWith(BUtility.F_HTTP_PATH) || path.startsWith(BUtility.F_FILE_SCHEMA) ||
                    path.startsWith("https://") || path.startsWith(BUtility.F_RTSP_PATH) || path.startsWith("/")) {// 直接设置路径
                if (path.startsWith(BUtility.F_FILE_SCHEMA) || path.startsWith("/")) {
                    path = path.replace("file://", "");
                    mediaPlayer.setDataSource(path);
                } else {
                    String newUrl = path;
                    int lastLine = path.lastIndexOf("/");
                    if (lastLine != -1) {
                        String lastPart = path.substring(lastLine + 1);
                        String frontPart = path.substring(0, lastLine + 1);
                        newUrl = frontPart + Uri.encode(lastPart);
                    }
                    mediaPlayer.setDataSource(newUrl);
                }
            } else if (path.startsWith(BUtility.F_Widget_RES_SCHEMA)) {// RES协议下文件
                final AssetFileDescriptor descriptor = BUtility.getFileDescriptorByResPath(this, path);
                if (descriptor == null) {
                    alertMessage(finder.getString("error_file_does_not_exist"), true);
                } else {
                    mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(),
                            descriptor.getLength());
                }
            } else {
                alertMessage(finder.getString("plugin_file_file_path_error") + path, true);
            }

            mediaPlayer.setDisplay(surfaceHolder);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.prepareAsync();
            showProgressDialog();
            curerntState = STATE_INIT;
            BDebug.d(TAG, "curerntState:STATE_INIT");
        } catch (Exception e) {
            e.printStackTrace();
            BDebug.e(TAG, "initMediaPlayer():" + e.getMessage());
            alertMessage(finder.getString("plugin_video_video_load_fail"), true);
        }
        if(NetworkUtils.getNetworkStatus(this)==-1){
            noNetworkLayout.setVisibility(View.VISIBLE);
            cancelProgressDialog();
        }
    }

    private int getDensity() {

        return (int) getResources().getDisplayMetrics().density;
    }

    private LayoutParams setSurfaceViewParams() {

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        if (w_activity >= h_activity) {
            int w = h_activity * videoWidth / videoHeight;
            int margin = (w_activity - w) / 2;
            if (margin >= 0) {
                lp.setMargins(margin, 0, margin, 0);
            } else {
                int h = videoHeight * w_activity / videoWidth;
                int marginh = (h_activity - h) / 2;
                lp.setMargins(0, marginh, 0, marginh);
            }
        } else {
            int h = videoHeight * w_activity / videoWidth;
            int marginh = (h_activity - h) / 2;
            if (marginh >= 0) {
                lp.setMargins(0, marginh, 0, marginh);
            } else {
                int w = h_activity * videoWidth / videoHeight;
                int margin = (w_activity - w) / 2;
                lp.setMargins(margin, 0, margin, 0);
            }
        }
        m_display.setLayoutParams(lp);
        return lp;

    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        cancelProgressDialog();// 取消进度框
        curerntState = STATE_PREPARED;
        m_sbTimeLine.setMax((int) (Math.ceil(mediaPlayer.getDuration() / 1000.0) * 1000));
        notifyStopMusicPlay();
        //初始尺寸为用户传进来的宽高
        if (forceFullScreen) {
            setVideoDisplayMode(MODE_FULL_SCEEN);
        } else {
            setSurfaceViewParams();
        }
        //如果是自动播放
        if (autoStart) {
            try {
                if (lastPlayPostion != 0) {
                    Log.d(TAG, String.valueOf(lastPlayPostion));
                    mediaPlayer.seekTo(lastPlayPostion);
                    lastPlayPostion = 0;
                } else {
                    mediaPlayer.seekTo((int) (startTime * 1000));
                    startTime = 0;
                }
                mediaPlayer.start();
                curerntState = STATE_PLAYING;
                onPlayerStatusChange(PLAYER_STATUS_PLAYING);
                BDebug.log("currentState: STATE_PLAYING");
                notifyHideControllers();
            } catch (IllegalStateException e) {
                onPlayerStatusChange(PALYER_STATUS_ERROR);
                Log.i(TAG, "video player occur unkwon error:" + e.getMessage());
            }
            m_ivPlayPause.setBackgroundDrawable(finder.getDrawable("plugin_video_pause"));
            handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
        } else {
            curerntState = STATE_PAUSE;
            mediaPlayer.seekTo((int) (startTime * 1000));
            startTime = 0;
            m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play"));
        }
        passTime = mediaPlayer.getCurrentPosition();
        totalTime = mediaPlayer.getDuration();
        m_tvPassTime.setText(formatTime(passTime) + "/" + formatTime(totalTime));
        m_sbTimeLine.setProgress(passTime);
        //非WIFI下显示
        if(NetworkUtils.getNetworkStatus(this)>0){
            mediaPlayer.pause();
            noWifiLayout.setVisibility(View.VISIBLE);
        }

    }


    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                curerntState = STATE_STOP;
            }
            mediaPlayer.release();
            curerntState = STATE_RELEASED;
            mediaPlayer = null;
        }
    }

    SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceHolder = holder;
            initMediaPlayer(videoPath);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceHolder = null;
            releaseMediaPlayer();
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        if (lastPlayPostion != 0) {
            initMediaPlayer(videoPath);
        }
    }

    @Override
    protected void onPause() {
        if (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE) {
            lastPlayPostion = mediaPlayer.getCurrentPosition();
        }
        releaseMediaPlayer();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(checkPlayTimeRunnable);
            handler.removeCallbacks(showLyricTextRunnable);
        }
        cancelProgressDialog();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        int progressOnClose = (int) Math.ceil(passTime / 1000.0);
        releaseMediaPlayer();
        if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//强制竖屏
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//强制横屏
        }

        mUexBaseObj.closePlayerCallBack(videoPath, progressOnClose);
    }

    /**
     * 上一句
     */
    private void preLyric() {
        int curLyricIndex = getLyricIndexByPosition();
        if (curLyricIndex < lyricListSize - 1) {
            ivNextLyric.setVisibility(View.VISIBLE);
        }

        if (curLyricIndex <= 0) {
            ivPreLyric.setVisibility(View.GONE);
            return;
        }
        curLyricIndex--;
        playVideoByCurLyric(curLyricIndex);
    }

    private void playVideoByCurLyric(int curLyricIndex) {
        Lyric lyric = lyricList.get(curLyricIndex);
        tvLyric.setText(lyric.getLyricEnglishText());
        double lyricStartTime = Double.parseDouble(lyric.getLyricStartTime());
        int lyricSeededPosition=(int)(lyricStartTime*1000);
        onPlayerSeek(lyricSeededPosition);
    }

    /**
     * 下一句
     */
    private void nextLyric() {
        int curLyricIndex = getLyricIndexByPosition();
        if (curLyricIndex > 0) {
            ivPreLyric.setVisibility(View.VISIBLE);
        }
        if (curLyricIndex >= lyricListSize - 1) {
            ivNextLyric.setVisibility(View.GONE);
            return;
        }
        curLyricIndex++;
        Log.e(TAG,"curLyricIndex:"+curLyricIndex);
        playVideoByCurLyric(curLyricIndex);
    }

    /**
     * 根据当前播放位置，获取当前字幕下标
     */
    private int getLyricIndexByPosition() {
        int lyricIndex = 0;
        int position = mediaPlayer.getCurrentPosition();
        Log.e(TAG,"position:"+position);
        Lyric lyric = null, lyricNext = null;
        float startTime, endTime, nextStartTime;
        for (int i = 0; i < lyricListSize; i++) {
            if (i == lyricListSize - 1) {
                lyricIndex = i;
                break;
            }
            lyric = lyricList.get(i);
            startTime = Float.parseFloat(lyric.getLyricStartTime()) * 1000;
            endTime = Float.parseFloat(lyric.getLyricEndTime()) * 1000;

            lyricNext = lyricList.get(i + 1);
            nextStartTime = Float.parseFloat(lyricNext.getLyricStartTime()) * 1000;
            if (position < startTime || (startTime <= position && endTime >= position) || (position > endTime && position < nextStartTime)) {
                lyricIndex = i;
                break;
            }
        }
        return lyricIndex;
    }


    //显示手势相关View
    private void showGestrueView(){
        gestureProgressBar.setVisibility(View.VISIBLE);
        if (handler != null) {
            handler.removeCallbacks(mHideRunnable);
        }
        handler.postDelayed(mHideRunnable,1000);
    }

    /**
     * 显示歌词字幕
     */
    private void showLyricText() {
        int curLyricIndex = getLyricIndexByPosition();
        Lyric lyric = lyricList.get(curLyricIndex);
        float startTime = Float.parseFloat(lyric.getLyricStartTime()) * 1000;
        float endTime = Float.parseFloat(lyric.getLyricEndTime()) * 1000;
        int position = mediaPlayer.getCurrentPosition();
        if (startTime <= position && endTime >= position) {
            tvLyric.setText(lyric.getLyricEnglishText());
        } else {
            tvLyric.setText("");
        }
        ivNextLyric.setVisibility(View.VISIBLE);
        if (curLyricIndex > 0) {
            ivPreLyric.setVisibility(View.VISIBLE);
        }
        if (curLyricIndex >= lyricListSize - 1) {
            ivNextLyric.setVisibility(View.GONE);
        }

    }


    /**
     * 改变屏幕的尺寸，只是针对最外层，并不会去改surfaceview的尺寸
     */
    private void toogleFullScreen() {
        if (scrollWithWeb) {
            LayoutParams lp = (LayoutParams) getWindow().getDecorView().getLayoutParams();
            LayoutParams newLayout;
            //如果之前状态是全屏，则恢复到非全屏的状态
            if (lp.width == LayoutParams.MATCH_PARENT
                    && lp.height == LayoutParams.MATCH_PARENT) {
                newLayout = new android.widget.AbsoluteLayout.LayoutParams(w_activity, h_activity, x_activity, y_activity);
            } else { //如果之前的状态不是全屏，则恢复到全屏状态。
                newLayout = new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0, 0);
            }
            getWindow().getDecorView().setLayoutParams(newLayout);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getWindow()
                    .getDecorView().getLayoutParams();
            //如果之前状态是全屏，则恢复到非全屏的状态
            if (lp.width == FrameLayout.LayoutParams.MATCH_PARENT
                    && lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
                lp.width = w_activity;
                lp.height = h_activity;
                lp.leftMargin = x_activity;
                lp.topMargin = y_activity;
            } else { //如果之前的状态不是全屏，则恢复到全屏状态。
                lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
                lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
                lp.leftMargin = 0;
                lp.topMargin = 0;
            }
            getWindow().getDecorView().setLayoutParams(lp);

        }

    }

    @Override
    public void onClick(View v) {
        if (v == ivClose) {
            closePlayerHandler();
            //this.finish();
        }else if(v == ivPlay){
            //屏幕中间播放按钮
            playVideoHandler();
        }else if(v==reloadBtn){
            noNetworkLayout.setVisibility(View.GONE);
            onCreate(null);
        } else if(v==watchInNoneWifi){
            noWifiLayout.setVisibility(View.GONE);
            mediaPlayer.start();
        } else if (v == m_ivScreenAdjust) {
            if (displayMode == MODE_FULL_SCEEN) {
                setVideoDisplayMode(MODE_SCALE);
            } else {
                setVideoDisplayMode(MODE_FULL_SCEEN);
            }
            toogleFullScreen();

        } else if (v == m_ivPlayPause) {
            try {
                switch (curerntState) {
                    case STATE_PLAYING:
                        pauseVideoHandler();
                        break;
                    case STATE_PAUSE:
                        playVideoHandler();
                        break;
                }
            } catch (IllegalStateException e) {
                alertMessage(finder.getString("plugin_video_player_player_state_call_error"), true);
            }
        } else if (v == ivPreLyric) {
            //上一句
            preLyric();
        } else if (v == ivNextLyric) {
            //下一句
            nextLyric();
        }

    }

    private void pauseVideoHandler() {
        mediaPlayer.pause();
        curerntState = STATE_PAUSE;
        m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play"));
        ivPlay.setVisibility(View.VISIBLE);
        onPlayerStatusChange(PLAYER_STATUS_PAUSE);
        notifyHideControllers();
    }

    private void playVideoHandler() {
        if (startTime != 0) {
            mediaPlayer.seekTo((int) (startTime * 1000));
            Log.d(TAG, "curLyricIndex--startTime"+startTime * 1000);
            startTime = 0;
        }
        mediaPlayer.start();
        Log.d(TAG,"curLyricIndex--positon:"+mediaPlayer.getCurrentPosition());
        curerntState = STATE_PLAYING;
        ivPlay.setVisibility(View.GONE);
        onPlayerStatusChange(PLAYER_STATUS_PLAYING);
        m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_pause"));
        handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
        notifyHideControllers();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //双击事件
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isUserSeekingBar = true;
        handler.removeMessages(ACTION_HIDE_CONTROLLER);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE)) {
            passTime = progress;
            m_tvPassTime.setText(formatTime(passTime) + "/" + formatTime(totalTime));
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE) {
            if (endTime != 0 && endTime < passTime) {
                onPlayEndTime();
            } else {
                mediaPlayer.seekTo(seekBar.getProgress());
                showLyricText();
            }
            isUserSeekingBar = false;
        }
        notifyHideControllers();
    }


    private void onPlayEndTime() {
        BDebug.d("onPlayEndTime", "试看结束");
        curerntState = STATE_PAUSE;
        mediaPlayer.pause();
        mediaPlayer.seekTo(0);//重置
        startTime = 0;
        passTime = 0;
        m_sbTimeLine.setProgress(passTime);//重新显示控制条
        switchControllersVisiblity();
        m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play"));
        mUexBaseObj.callBackPluginJs(EUExVideo.F_CALLBACK_ON_PLAYER_ENDTIME, "");

    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        final float timePercent = 1f * totalTime * percent / 100;
        m_sbTimeLine.setSecondaryProgress((int) timePercent);
        tvBufferProgress.setText(percent+"%");
        Log.d(TAG,"onBufferingUpdate  percent:" + percent);
        onPlayerStatusChange(PLAYER_STATUS_BUFFERING);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //播放完成后设置成暂停模式，下次点击时可以重新播放。
        ivPreLyric.setVisibility(View.GONE);
        ivNextLyric.setVisibility(View.VISIBLE);
        curerntState = STATE_PAUSE;
        mediaPlayer.seekTo(1);//回到第一帧
        startTime = 0;
        passTime = 0;
        m_sbTimeLine.setProgress(passTime);//重新显示控制条
        switchControllersVisiblity();
        m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play"));
        mUexBaseObj.callBackPluginJs(EUExVideo.F_CALLBACK_ON_PLAYER_FINISH, "");
        if (isAutoEndFullScreen && displayMode == MODE_FULL_SCEEN && !forceFullScreen) {//切换成非全屏
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setVideoDisplayMode(MODE_SCALE);
                    toogleFullScreen();
                }
            }, 1000);
        }
    }


    public void setVideoDisplayMode(int mode) {
        if (mode == MODE_FULL_SCEEN) { //全屏
            VideoPlayerActivityForViewToWeb.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setScreenSize();
            if (videoHeight != 0 && videoWidth != 0) {
//                // 计算屏幕与视频的缩放比
//                final float widthScaleRate = (float) screenWidth / (float) videoWidth;
//                final LayoutParams lp = m_display.getLayoutParams();
//                lp.height = (int) (widthScaleRate * (float) videoHeight);
//                lp.width = screenWidth;
//                m_display.setLayoutParams(lp);
//                m_display.getHolder().setFixedSize(lp.width, lp.height);
//                displayMode = mode;
//                m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_fullscreen_selector"));
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                if (videoWidth > videoHeight) {
                    int h = screenWidth * videoHeight / videoWidth;
                    int margin = (screenHeight - h) / 2;
                    lp.setMargins(0, margin, 0, margin);
                    if (scrollWithWeb) {
                        lp.width = screenWidth;
                        lp.height = h;
                    }

                } else {
                    int w = screenHeight * videoWidth / videoHeight;
                    int margin = (screenWidth - w) / 2;
                    lp.setMargins(margin, 0, margin, 0);
                    if (scrollWithWeb) {
                        lp.width = w;
                        lp.height = screenHeight;
                    }
                }
                m_display.setLayoutParams(lp);
                m_display.getHolder().setFixedSize(lp.width, lp.height);
                displayMode = mode;
                m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_fullscreen_selector"));


            }
        } else {
            VideoPlayerActivityForViewToWeb.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            //  final LayoutParams lp = m_display.getLayoutParams();
//            lp.height = h_activity;
//            lp.width = w_activity;
            // m_display.setLayoutParams(lp);
            LayoutParams lp = setSurfaceViewParams();
            m_display.getHolder().setFixedSize(lp.width, lp.height);
            displayMode = mode;
            m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_actualsize_selector"));

        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        BDebug.log("onError------->  what: " + what + "  extra: " + extra);
        //alertMessage(finder.getString("plugin_video_can_not_support_this_format_video_playback"), true);
        onPlayerStatusChange(PALYER_STATUS_ERROR);
        return true;
    }

    private AlertDialog alertDialog;

    // 弹出消息框
    private void alertMessage(String message, final boolean exitOnConfirm) {

        alertDialog = new AlertDialog.Builder(this).setTitle(finder.getString("prompt")).setMessage(message)
                .setCancelable(false)
                .setPositiveButton(finder.getString("confirm"), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (exitOnConfirm) {
                            releaseMediaPlayer();
                            dialog.dismiss();
                            mUexBaseObj.closePlayer(null);
                        }
                    }
                }).create();

        alertDialog.show();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

        if (videoWidth == 0 || videoHeight == 0) {// 第一次进入
            if (width != 0 && height != 0) {
                videoWidth = width;
                videoHeight = height;
                BDebug.log("Screen W:" + screenWidth + "  H:" + screenHeight + "    Video W:" + videoWidth + "  H:"
                        + videoHeight);
            } else {
                BDebug.log("video width&height is not avaliable......");
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // 显示进度框
    private void showProgressDialog() {
        cancelProgressDialog();
        linerBufferProgress.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(this, finder.getAnimId("plugin_video_rotate_loading"));
        ivBufferProgress.startAnimation(animation);

    }

    // 取消进度框
    private void cancelProgressDialog() {
        ivBufferProgress.clearAnimation();
        linerBufferProgress.setVisibility(View.GONE);
    }

    private String formatTime(int ms) {
        if (ms >= 0) {
            int totalSeconds = ms / 1000;
            //如果设置的不是回到第一帧,需要向上取整，防止。防止如果视屏有6997ms,但还是显示总时长为6秒钟。
            if (ms != 1) {
                totalSeconds = (int) Math.ceil(ms / 1000.0);
            }
            final int hours = totalSeconds / 3600;
            final int minutes = (totalSeconds % 3600) / 60;
            final int second = ((totalSeconds % 3600) % 60);
            final StringBuffer sb = new StringBuffer();
            if (hours > 0) {
                if (hours <= 10) {
                    sb.append("0");
                }
                sb.append(hours).append(":");
            }
            if (minutes < 10) {
                sb.append("0");
            }
            sb.append(minutes).append(":");
            if (second < 10) {
                sb.append("0");
            }
            sb.append(second);
            return sb.toString();
        }
        return "";
    }

    private void switchControllersVisiblity() {
        if (m_bottomLayer.getVisibility() == View.GONE) {
            m_bottomLayer.setVisibility(View.VISIBLE);
            m_bottomLayer.startAnimation(fadeInAnim);
            titleBar.setVisibility(View.VISIBLE);
            titleBar.setAnimation(fadeInAnim);
            UIUtils.showStatusBar(this);
        } else {
            m_bottomLayer.setVisibility(View.GONE);
            m_bottomLayer.startAnimation(fadeOutAnim);
            titleBar.setVisibility(View.GONE);
            titleBar.setAnimation(fadeOutAnim);
            UIUtils.hideStatusBar(this);
        }
    }

    /**
     * 移除隐藏控件的消息并重新发送
     */
    private void notifyHideControllers() {
        // 取消之前发送的还未被处理的消息
        handler.removeMessages(ACTION_HIDE_CONTROLLER);
        // 播放时才发送隐藏消息
        if (curerntState == STATE_PLAYING) {
            handler.sendEmptyMessageDelayed(ACTION_HIDE_CONTROLLER, CONTROLLERS_HIDE_DURATION);
        }
    }

    private void onPlayerStatusChange(int status) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("status", status);
            mUexBaseObj.callBackPluginJs(EUExVideo.F_CALLBACK_ON_PLAYER_STATUS_CHANGE, jsonObject.toString());
        } catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }


    }

    private void notifyStopMusicPlay() {
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra("command", "pause");
        this.sendBroadcast(i);
    }

    private void initAnimation() {
        final int duration = 300;
        LinearInterpolator interpolator = new LinearInterpolator();
        fadeInAnim = new AlphaAnimation(0, 1);
        fadeInAnim.setDuration(duration);
        fadeInAnim.setInterpolator(interpolator);
        fadeOutAnim = new AlphaAnimation(1, 0);
        fadeOutAnim.setDuration(duration);
        fadeOutAnim.setInterpolator(interpolator);
    }



    @Override
    public void onBrightnessGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //下面这是设置当前Window亮度的方法
        BrightnessUtils.offAutoBrightness(this.getParent());
        Log.d(TAG, "onBrightnessGesture: old" + brightness);
        float newBrightness = (e1.getY() - e2.getY()) / m_display.getHeight() ;
        newBrightness += brightness;
        Log.d(TAG, "onBrightnessGesture: new" + newBrightness);
        if (newBrightness < 0){
            newBrightness = 0.0f;
        }else if (newBrightness > 1){
            newBrightness = 1.0f;
        }
        BrightnessUtils.setWindowBrightness(this.getParent(),newBrightness);
        int bright=(int)(newBrightness*100);
        Log.d(TAG, "bright" + bright);
        tvGestureProgrss.setText(bright+ "%");
        ivGestureProgress.setImageResource(finder.getDrawableId("brightness_w"));
        showGestrueView();
    }

    @Override
    public void onVolumeGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(TAG, "onVolumeGesture: oldVolume " + oldVolume);
        int value = m_display.getHeight()/maxVolume ;
        int newVolume = (int) ((e1.getY() - e2.getY())/value + oldVolume);

        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,newVolume,AudioManager.FLAG_PLAY_SOUND);
//        Log.d(TAG, "onVolumeGesture: value" + value);
        Log.d(TAG, "onVolumeGesture: newVolume "+ newVolume);

        //要强行转Float类型才能算出小数点，不然结果一直为0
        int volumeProgress = (int) (newVolume/Float.valueOf(maxVolume) *100);
        if(volumeProgress>100){
            volumeProgress=100;
        }
        if(volumeProgress<0){
            volumeProgress=0;
        }
        if (volumeProgress >= 50){
            ivGestureProgress.setImageResource(finder.getDrawableId("volume_higher_w"));
        }else if (volumeProgress > 0){
            ivGestureProgress.setImageResource(finder.getDrawableId("volume_lower_w"));
        }else {
            ivGestureProgress.setImageResource(finder.getDrawableId("volume_off_w"));
        }

        tvGestureProgrss.setText(volumeProgress+"%");
        showGestrueView();
    }

    @Override
    public void onFF_REWGesture(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float offset = e2.getX() - e1.getX();
        //根据移动的正负决定快进还是快退
        if (offset > 0) {
            ivGestureProgress.setImageResource(finder.getDrawableId("ff"));
        }else {
            ivGestureProgress.setImageResource(finder.getDrawableId("fr"));
        }
        int ratio=(int)(offset/m_display.getWidth() * 100);
        seekedPosition =curPositoin+ratio*1000;
        if(seekedPosition<0){
            seekedPosition=0;
        }
        if(seekedPosition >totalTime){
            seekedPosition =totalTime;
        }
        tvGestureProgrss.setText(formatTime(seekedPosition) + "/" + formatTime(totalTime));
        showGestrueView();
    }

    @Override
    public void onEndFF_REW(MotionEvent e) {
        onPlayerSeek(seekedPosition);
    }

    @Override
    public void onSingleTapGesture(MotionEvent e) {
        switchControllersVisiblity();
        notifyHideControllers();
    }

    @Override
    public void onDoubleTapGesture(MotionEvent e) {
        if(mediaPlayer.isPlaying()){
            pauseVideoHandler();
        }else{
            playVideoHandler();
        }
    }

    @Override
    public void onDown(MotionEvent e) {
        //每次按下的时候更新当前亮度和音量，播放位置
        curPositoin=mediaPlayer.getCurrentPosition();
        oldVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        brightness = BrightnessUtils.getWindowBrightness(this.getParent());
        if (brightness == -1){
            //一开始是默认亮度的时候，获取系统亮度，计算比例值
            brightness = BrightnessUtils.getBrightness(this.getParent()) / BrightnessUtils.getMaxBrightness();
        }
    }


}
