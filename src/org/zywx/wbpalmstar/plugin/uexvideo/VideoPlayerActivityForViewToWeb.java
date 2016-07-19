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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.AbsoluteLayout;

/**
 * TODO
 * 1.scrollWithWeb
 * 2.添加close  Button
*
 */
public class VideoPlayerActivityForViewToWeb extends Activity implements OnPreparedListener, OnClickListener,
		OnSeekBarChangeListener, OnCompletionListener, OnErrorListener, OnVideoSizeChangedListener,
		OnBufferingUpdateListener {

	public static final String TAG = "VideoPlayerActivity";
	private final static int ACTION_UPDATE_PASS_TIME = 1;
	private final static int ACTION_HIDE_CONTROLLER = 2;
	private final static int MODE_FULL_SCEEN = 2;// 全屏
	private final static int MODE_SCALE= 1;// 正常
	private final static int STATE_INIT = 0;
	private final static int STATE_PREPARED = 1;
	private final static int STATE_PLAYING = 2;
	private final static int STATE_PAUSE = 3;
	private final static int STATE_STOP = 4;
	private final static int STATE_RELEASED = 5;
	private final static int CONTROLLERS_HIDE_DURATION = 5000;
	private int curerntState = STATE_INIT;
	private ProgressDialog progressDialog;
	private int lastPlayPostion;
	private SurfaceView m_display;
	private SurfaceHolder surfaceHolder;
	private ImageView ivClose;
    private ImageView ivProgress;

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
    private int startTime;
	private boolean autoStart; //是否自动播放
    private boolean forceFullScreen; //是否强制全屏显示
	private boolean showCloseButton; //是否显示 关闭 按钮
    private boolean showScaleButton; //是否显示 缩放 按钮
    private boolean scrollWithWeb;

	private int passTime;
	private int totalTime;
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

	private GestureDetector gestureDetector = new GestureDetector(new SimpleOnGestureListener() {

        public boolean onSingleTapConfirmed(MotionEvent event) {
			switchControllersVisiblity();
			notifyHideControllers();
			return true;
		};

	});

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
                ivClose.setVisibility(View.GONE);
                ivClose.setAnimation(fadeOutAnim);
				break;
			}
		};
	};

	private int x_activity = 0;
	private int y_activity = 0;
	private int w_activity = 0;
	private int h_activity = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finder = ResoureFinder.getInstance(this);
		getWindow().getDecorView().setBackgroundDrawable(null);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final Intent intent = getIntent();
		if (intent == null ||  ((VideoPlayerConfig)intent.getSerializableExtra("playerConfig")) == null) {// 路径不存在
            alertMessage("invalid params", true);
			return;
		}
        VideoPlayerConfig config = (VideoPlayerConfig)intent.getSerializableExtra("playerConfig");
        videoPath = config.getSrc();
        if (TextUtils.isEmpty(videoPath)) {
            Log.i(TAG, "[invalid params]: videoPath can not be null");
            alertMessage("invalid params", true);
        }

        x_activity = config.getX();
		y_activity = config.getY();
        w_activity = config.getWidth();
        h_activity = config.getHeight();
		mUexBaseObj = intent.getParcelableExtra("EUExVideo");
        startTime = config.getStartTime();
        autoStart = config.getAutoStart();
        showScaleButton = config.getShowScaleButton();
        forceFullScreen = config.getForceFullScreen();
		showCloseButton = config.getShowCloseButton();
        scrollWithWeb = config.getScrollWithWeb();

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		screenHeight = metrics.heightPixels;
		screenWidth = metrics.widthPixels;
		setContentView(finder.getLayoutId("plugin_video_player_main2"));
		initViews();
		m_display.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		m_display.getHolder().setKeepScreenOn(true);
		m_display.getHolder().addCallback(callback);
	}

	private void initViews() {
		initAnimation();

		m_display = (SurfaceView) findViewById(finder.getId("plugin_video_player_sv_diaplay"));
        ivClose = (ImageView) findViewById(finder.getId("plugin_video_player_iv_close"));
        ivProgress = (ImageView)  findViewById(finder.getId("plugin_video_iv_progress_bar"));
		m_ivPlayPause = (ImageView) findViewById(finder.getId("plugin_video_player_play_pause"));
		m_ivScreenAdjust = (ImageView) findViewById(finder.getId("plugin_video_player_iv_screen_adjust"));
		m_bottomLayer = (RelativeLayout) findViewById(finder.getId("plugin_video_player_bottom_layout"));
		m_tvPassTime = (TextView) findViewById(finder.getId("plugin_video_player_tv_pass_time"));
		m_sbTimeLine = (SeekBar) findViewById(finder.getId("plugin_video_player_sb_timeline"));
		m_ivPlayPause.setOnClickListener(this);
        if (forceFullScreen) {
            m_ivScreenAdjust.setVisibility(View.GONE);
        } else {
            m_ivScreenAdjust.setOnClickListener(this);
        }
        //如果显示缩放按钮
        if (showScaleButton) {
            m_ivScreenAdjust.setVisibility(View.VISIBLE);
            m_ivScreenAdjust.setOnClickListener(this);
        } else {
            m_ivScreenAdjust.setVisibility(View.GONE);
        }
		m_sbTimeLine.setOnSeekBarChangeListener(this);
		m_display.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
		m_bottomLayer.setOnClickListener(this);
		if (showCloseButton) {
			ivClose.setVisibility(View.VISIBLE);
			ivClose.setOnClickListener(this);
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

		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			if (path.startsWith(BUtility.F_HTTP_PATH) || path.startsWith(BUtility.F_FILE_SCHEMA)
					|| path.startsWith(BUtility.F_RTSP_PATH) || path.startsWith("/")) {// 直接设置路径
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
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		cancelProgressDialog();// 取消进度框
		curerntState = STATE_PREPARED;
        m_sbTimeLine.setMax((int) (Math.ceil(mediaPlayer.getDuration() /1000.0) * 1000));
		notifyStopMusicPlay();
        //初始尺寸为用户传进来的宽高
        if(forceFullScreen) {
            setVideoDisplayMode(MODE_FULL_SCEEN);
        } else {
            m_display.getHolder().setFixedSize(w_activity, h_activity);
        }
        //如果是自动播放
		if (autoStart) {
			try {
				if (lastPlayPostion != 0) {
					mediaPlayer.seekTo(lastPlayPostion);
					lastPlayPostion = 0;
				} else {
                    mediaPlayer.seekTo(startTime * 1000);
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
			m_ivPlayPause.setBackgroundDrawable(finder.getDrawable("plugin_video_pause_selector"));
			handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
		} else {
			curerntState = STATE_PAUSE;
            mediaPlayer.seekTo(startTime * 1000);
            startTime = 0;
            m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play_selector"));
		}
        passTime = mediaPlayer.getCurrentPosition();
        totalTime = mediaPlayer.getDuration();
        m_tvPassTime.setText(formatTime(passTime) + "/" + formatTime(totalTime));
        m_sbTimeLine.setProgress(passTime);
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
        cancelProgressDialog();
		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.dismiss();
		}
        int progressOnClose = (int)Math.ceil(passTime / 1000.0);
        releaseMediaPlayer();
        mUexBaseObj.closePlayerCallBack(videoPath, progressOnClose);
	}

    /**
     * 改变屏幕的尺寸，只是针对最外层，并不会去改surfaceview的尺寸
     */
	private void toogleFullScreen() {

        if (scrollWithWeb) {
            AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) getWindow().getDecorView().getLayoutParams();
            AbsoluteLayout.LayoutParams newLayout;
            //如果之前状态是全屏，则恢复到非全屏的状态
            if (lp.width == AbsoluteLayout.LayoutParams.MATCH_PARENT
                    && lp.height == AbsoluteLayout.LayoutParams.MATCH_PARENT) {
                newLayout =  new android.widget.AbsoluteLayout.LayoutParams(w_activity, h_activity, x_activity, y_activity);
            } else { //如果之前的状态不是全屏，则恢复到全屏状态。
                newLayout =  new android.widget.AbsoluteLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 0, 0);
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
            mUexBaseObj.closePlayer(null);
			//this.finish();
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
					mediaPlayer.pause();
					curerntState = STATE_PAUSE;
					m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play_selector"));
                    onPlayerStatusChange(PLAYER_STATUS_PAUSE);
                    break;
				case STATE_PAUSE:
                    if(startTime != 0) {
                        mediaPlayer.seekTo(startTime * 1000);
                        startTime = 0;
                    }
					mediaPlayer.start();
					curerntState = STATE_PLAYING;
                    onPlayerStatusChange(PLAYER_STATUS_PLAYING);
                    m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_pause_selector"));
                    if(!autoStart) {
                        handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
                    }
					break;
				}
			} catch (IllegalStateException e) {
				alertMessage(finder.getString("plugin_video_player_player_state_call_error"), true);
			}
		}
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
			mediaPlayer.seekTo(seekBar.getProgress());
            isUserSeekingBar = false;
		}
		notifyHideControllers();
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		final float timePercent = 1f * totalTime * percent / 100;
		m_sbTimeLine.setSecondaryProgress((int) timePercent);
        BDebug.log("onBufferingUpdate  percent:" + percent);
        onPlayerStatusChange(PLAYER_STATUS_BUFFERING);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
        //播放完成后设置成暂停模式，下次点击时可以重新播放。
        curerntState = STATE_PAUSE;
        mediaPlayer.seekTo(1);//回到第一帧
        startTime = 0;
        passTime = 0;
        m_sbTimeLine.setProgress(passTime);//重新显示控制条
        switchControllersVisiblity();
        m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play_selector"));
        mUexBaseObj.callBackPluginJs(EUExVideo.F_CALLBACK_ON_COMPLETION, "");
    }


	public void setVideoDisplayMode(int mode) {
        if (mode == MODE_FULL_SCEEN) { //全屏
            if (videoHeight != 0 && videoWidth != 0) {
                // 计算屏幕与视频的缩放比
                final float widthScaleRate = (float) screenWidth / (float) videoWidth;
                final LayoutParams lp = m_display.getLayoutParams();
                lp.height = (int) (widthScaleRate * (float) videoHeight);
                lp.width = screenWidth;
                m_display.setLayoutParams(lp);
                m_display.getHolder().setFixedSize(lp.width, lp.height);
                displayMode = mode;
                m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_fullscreen_selector"));
            }
        } else {
            final LayoutParams lp = m_display.getLayoutParams();
            lp.height = h_activity;
            lp.width = w_activity;
            m_display.setLayoutParams(lp);
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
        if (videoWidth == 0 || videoWidth == 0) {// 第一次进入
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
        ivProgress.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(this, finder.getAnimId("plugin_video_rotate_loading"));
        ivProgress.startAnimation(animation);

    }

	// 取消进度框
	private void cancelProgressDialog() {
		ivProgress.clearAnimation();
        ivProgress.setVisibility(View.GONE);
	}

	private String formatTime(int ms) {
		if (ms >= 0) {
            int totalSeconds = ms / 1000;
            //如果设置的不是回到第一帧,需要向上取整，防止。防止如果视屏有6997ms,但还是显示总时长为6秒钟。
            if (ms != 1) {
                totalSeconds =(int) Math.ceil(ms / 1000.0);
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
            ivClose.setVisibility(View.VISIBLE);
            ivClose.setAnimation(fadeInAnim);
		} else {
			m_bottomLayer.setVisibility(View.GONE);
			m_bottomLayer.startAnimation(fadeOutAnim);
            ivClose.setVisibility(View.GONE);
            ivClose.setAnimation(fadeOutAnim);
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
}
