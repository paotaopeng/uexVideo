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

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class VideoPlayerActivityForViewToWeb extends Activity implements OnPreparedListener, OnClickListener,
		OnSeekBarChangeListener, OnCompletionListener, OnErrorListener, OnVideoSizeChangedListener,
		OnBufferingUpdateListener {

	public static final String TAG = "VideoPlayerActivityForViewToWeb";
	private final static int ACTION_UPDATE_PASS_TIME = 1;
	private final static int ACTION_HIDE_CONTROLLER = 2;
	private final static int MODE_HEIGHT_FIT = 2;// 全屏
	private final static int MODE_WIDTH_FIT = 1;// 正常
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
	private ImageView m_ivBack;
	private ImageView m_ivPreStep;
	private ImageView m_ivNextStep;
	private ImageView m_ivPlayPause;
	private ImageView m_ivScreenAdjust;
	private SeekBar m_sbTimeLine;
	private TextView m_tvPassTime;
	private TextView m_tvTotalTime;
	private LinearLayout m_topLayer;
	private RelativeLayout m_bottomLayer;
	private MediaPlayer mediaPlayer;

	private int screenWidth;
	private int screenHeight;

	private int videoWidth;
	private int videoHeight;

	private int passTime;
	private int totalTime;
	private int displayMode = MODE_WIDTH_FIT;
	private boolean isUserSeekingBar = false;
	private AlphaAnimation fadeInAnim;
	private AlphaAnimation fadeOutAnim;
	private String videoPath;
	private ResoureFinder finder;
	private EUExVideo mUexBaseObj;
	private GestureDetector gestureDetector = new GestureDetector(new SimpleOnGestureListener() {

		// 双击视频画面--->宽度适应或高度适应切换
		public boolean onDoubleTap(MotionEvent event) {
			if (displayMode == MODE_HEIGHT_FIT) {
				setVideoDisplayMode(MODE_WIDTH_FIT);
			} else if (displayMode == MODE_WIDTH_FIT) {
				setVideoDisplayMode(MODE_HEIGHT_FIT);
			}
			notifyHideControllers();
			return true;
		};

		/**
		 * 确保用户不是双击才触发
		 * 
		 * @param event
		 * @return
		 */
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
					m_tvPassTime.setText(formatTime(passTime));
					m_sbTimeLine.setProgress(passTime);
				}
				if (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE) {
					handler.sendEmptyMessageDelayed(ACTION_UPDATE_PASS_TIME, 1000);
				}
				break;
			case ACTION_HIDE_CONTROLLER:
				m_topLayer.setVisibility(View.GONE);
				m_bottomLayer.setVisibility(View.GONE);
				m_topLayer.startAnimation(fadeOutAnim);
				m_bottomLayer.setAnimation(fadeOutAnim);
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
		if (intent == null || intent.getData() == null) {// 路径不存在
			alertMessage(finder.getString("plugin_video_file_path_is_not_exist"), true);
			return;
		}
		x_activity = intent.getIntExtra("x", 0);
		y_activity = intent.getIntExtra("y", 0);
		w_activity = intent.getIntExtra("w", 0);
		h_activity = intent.getIntExtra("h", 0);
		mUexBaseObj = intent.getParcelableExtra("EUExVideo");
		videoPath = intent.getData().toString();
		BDebug.d(TAG, "VideoPath:" + videoPath);
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
		m_ivBack = (ImageView) findViewById(finder.getId("plugin_video_player_iv_back"));
		m_ivNextStep = (ImageView) findViewById(finder.getId("plugin_video_player_quick_next_step"));
		m_ivPlayPause = (ImageView) findViewById(finder.getId("plugin_video_player_play_pause"));
		m_ivPreStep = (ImageView) findViewById(finder.getId("plugin_video_player_quick_pre_step"));
		m_ivScreenAdjust = (ImageView) findViewById(finder.getId("plugin_video_player_iv_screen_adjust"));
		m_topLayer = (LinearLayout) findViewById(finder.getId("plugin_video_player_top_layout"));
		m_bottomLayer = (RelativeLayout) findViewById(finder.getId("plugin_video_player_bottom_layout"));
		m_tvPassTime = (TextView) findViewById(finder.getId("plugin_video_player_tv_pass_time"));
		m_tvTotalTime = (TextView) findViewById(finder.getId("plugin_video_player_tv_total_time"));
		m_sbTimeLine = (SeekBar) findViewById(finder.getId("plugin_video_player_sb_timeline"));
		m_ivBack.setOnClickListener(this);
		m_ivNextStep.setOnClickListener(this);
		m_ivPreStep.setOnClickListener(this);
		m_ivPlayPause.setOnClickListener(this);
		m_ivScreenAdjust.setOnClickListener(this);
		m_sbTimeLine.setOnSeekBarChangeListener(this);
		m_display.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}
		});
		m_topLayer.setOnClickListener(this);
		m_bottomLayer.setOnClickListener(this);
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
			showProgressDialog("Loading...");
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
		BDebug.log("MediaPlayer---------onPrepared.........");
		cancelProgressDialog();// 取消进度框
		curerntState = STATE_PREPARED;
		m_sbTimeLine.setMax(mediaPlayer.getDuration());
		BDebug.log("currentState: STATE_PREPARED");
		notifyStopMusicPlay();
		int videoWidth = mp.getVideoWidth();
		int videoHeight = mp.getVideoHeight();
		m_display.getHolder().setFixedSize(videoWidth, videoHeight);
		try {
			if (lastPlayPostion != 0) {
				mediaPlayer.seekTo(lastPlayPostion);
				lastPlayPostion = 0;
			}
			mediaPlayer.start();
			curerntState = STATE_PLAYING;
			BDebug.log("currentState: STATE_PLAYING");
			notifyHideControllers();
		} catch (IllegalStateException e) {
			alertMessage(finder.getString("plugin_video_mediaplayer_occur_unknown_error"), true);
		}
		totalTime = mediaPlayer.getDuration();
		m_tvTotalTime.setText(formatTime(totalTime));
		m_tvPassTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
		m_ivPlayPause.setBackgroundDrawable(finder.getDrawable("plugin_video_pause_selector"));
		handler.sendEmptyMessage(ACTION_UPDATE_PASS_TIME);
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
		BDebug.d(TAG, "onResume...............");
		super.onResume();
		if (lastPlayPostion != 0) {
			initMediaPlayer(videoPath);
		}
	}

	@Override
	protected void onPause() {
		BDebug.d(TAG, "onPause...............");
		if (curerntState == STATE_PLAYING || curerntState == STATE_PAUSE) {
			lastPlayPostion = mediaPlayer.getCurrentPosition();
		}
		releaseMediaPlayer();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		BDebug.d(TAG, "onDestroy.......");
		cancelProgressDialog();
		if (alertDialog != null && alertDialog.isShowing()) {
			alertDialog.dismiss();
		}
		releaseMediaPlayer();
		super.onDestroy();
	}
	
	private void setFullscreen() {
		FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getWindow()
				.getDecorView().getLayoutParams();
		if (lp.width == FrameLayout.LayoutParams.MATCH_PARENT
				&& lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
			lp.width = w_activity;
			lp.height = h_activity;
			lp.leftMargin = x_activity;
			lp.topMargin = y_activity;
		} else {
			lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
			lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
			lp.leftMargin = 0;
			lp.topMargin = 0;
		}
		getWindow().getDecorView().setLayoutParams(lp);
	}

	@Override
	public void onClick(View v) {
		if (v == m_ivBack) {
			releaseMediaPlayer();
			mUexBaseObj.removePlay(null);
			//this.finish();
		} else if (v == m_ivScreenAdjust) {
			if (displayMode == MODE_WIDTH_FIT) {
				setVideoDisplayMode(MODE_HEIGHT_FIT);
				setFullscreen();
			} else {
				setVideoDisplayMode(MODE_WIDTH_FIT);
				setFullscreen();
			}

		} else if (v == m_ivPlayPause) {
			try {
				switch (curerntState) {
				case STATE_PLAYING:
					mediaPlayer.pause();
					curerntState = STATE_PAUSE;
					m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_play_selector"));
					break;
				case STATE_PAUSE:
					mediaPlayer.start();
					curerntState = STATE_PLAYING;
					m_ivPlayPause.setBackgroundResource(finder.getDrawableId("plugin_video_pause_selector"));
					break;
				}
			} catch (IllegalStateException e) {
				alertMessage(finder.getString("plugin_video_player_player_state_call_error"), true);
			}
		} else if (v == m_ivPreStep) {
			if (curerntState == STATE_PLAYING) {
				isUserSeekingBar = true;
				passTime = mediaPlayer.getCurrentPosition() - totalTime / 100;
				m_tvPassTime.setText(formatTime(passTime));
				m_sbTimeLine.setProgress(passTime);
				mediaPlayer.seekTo(passTime);
				isUserSeekingBar = false;
			}
		} else if (v == m_ivNextStep) {
			if (curerntState == STATE_PLAYING) {
				isUserSeekingBar = true;
				passTime = mediaPlayer.getCurrentPosition() + totalTime / 100;
				m_tvPassTime.setText(formatTime(passTime));
				m_sbTimeLine.setProgress(passTime);
				mediaPlayer.seekTo(passTime);
				isUserSeekingBar = false;
			}
		}
		notifyHideControllers();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.i(TAG,"onKeyUp-->>>>>>>");
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//this.finish();
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
			m_tvPassTime.setText(formatTime(passTime));
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
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		releaseMediaPlayer();
		mUexBaseObj.removePlay(null);
		mUexBaseObj.jsCallback(EUExVideo.F_CALLBACK_NAME_VIDEO_ONCOMPLETION, 0,
                EUExCallback.F_C_TEXT, 0);
		//this.finish();
		//onClick(m_ivPlayPause);
	}

	/**
	 * 设置视频显示模式(高度适应|宽度适应)
	 * 
	 * @param mode
	 */
	public void setVideoDisplayMode(int mode) {
		switch (mode) {
		// 宽度适应，默认
		case MODE_WIDTH_FIT:
			if (videoHeight != 0 && videoWidth != 0) {
				// 计算屏幕与视频的缩放比
				final float widthScaleRate = (float) screenWidth / (float) videoWidth;
				final LayoutParams lp = m_display.getLayoutParams();
				lp.height = (int) (widthScaleRate * (float) videoHeight);
				lp.width = screenWidth;
				m_display.setLayoutParams(lp);
				displayMode = mode;
				m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_actualsize_selector"));
			}
			break;
		case MODE_HEIGHT_FIT:
			if (videoHeight != 0 && videoWidth != 0) {
				float heightScaleRate = (float) screenHeight / (float) videoHeight;
				final LayoutParams lp = m_display.getLayoutParams();
				lp.width = (int) (heightScaleRate * (float) videoWidth);
				lp.height = screenHeight;
				m_display.setLayoutParams(lp);
				displayMode = mode;
				m_ivScreenAdjust.setBackgroundResource(finder.getDrawableId("plugin_video_fullscreen_selector"));
			}
			break;
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		BDebug.log("onError------->  what: " + what + "  extra: " + extra);
		alertMessage(finder.getString("plugin_video_can_not_support_this_format_video_playback"), true);
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
							mUexBaseObj.removePlay(null);
							//VideoPlayerActivityForViewToWeb.this.finish();
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
				final float screenRatio = (float) screenWidth / (float) screenHeight;
				final float videoRatio = (float) videoWidth / (float) videoHeight;
				if (screenRatio > videoRatio) {
					setVideoDisplayMode(MODE_HEIGHT_FIT);
					BDebug.log("init setVideo--------------->HeightFit......");
				} else {
					BDebug.log("init setVideo--------------->WidthFit......");
					setVideoDisplayMode(MODE_WIDTH_FIT);
				}
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
	private void showProgressDialog(String msg) {
		cancelProgressDialog();
		progressDialog = ProgressDialog.show(this, finder.getString("prompt"), msg, false, true);
	}

	// 取消进度框
	private void cancelProgressDialog() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
	}

	private String formatTime(int ms) {
		if (ms >= 0) {
			final int totalSeconds = ms / 1000;
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
		if (m_topLayer.getVisibility() == View.GONE) {
			m_topLayer.setVisibility(View.VISIBLE);
			m_topLayer.startAnimation(fadeInAnim);
		} else {
			m_topLayer.setVisibility(View.GONE);
			m_topLayer.startAnimation(fadeOutAnim);
		}
		if (m_bottomLayer.getVisibility() == View.GONE) {
			m_bottomLayer.setVisibility(View.VISIBLE);
			m_bottomLayer.startAnimation(fadeInAnim);
		} else {
			m_bottomLayer.setVisibility(View.GONE);
			m_bottomLayer.startAnimation(fadeOutAnim);
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
