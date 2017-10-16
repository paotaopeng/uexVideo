/**
 * Copyright 2014 Jeroen Mols
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.zywx.wbpalmstar.plugin.uexvideo.lib.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import android.widget.TextView;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.universalex.EUExUtil;


public class VideoCaptureView extends FrameLayout implements OnClickListener {

	private ImageView   mDeclineBtnIv;
	private ImageView   mThumbnailIv;
	private ImageView   mFlashIv;
	private ImageView   mAcceptBtnIv;
	private ImageView   mRecordBtnIv;
	private SurfaceView mSurfaceView;
	private ImageView   mSwitchCameraIv;
	private Chronometer mTimerTv;

	private RecordingButtonInterface mRecordingInterface;
	private ResoureFinder            finder;

	public VideoCaptureView(Context context) {
		super(context);
		initialize(context);
	}

	public VideoCaptureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(context);
	}

	public VideoCaptureView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initialize(context);
	}

	private void initialize(Context context) {
		finder = ResoureFinder.getInstance(context);
		final View videoCapture = View.inflate(context, finder.getLayoutId("plugin_video_view_videocapture"), this);

		mRecordBtnIv = (ImageView) videoCapture.findViewById(finder.getId("videocapture_recordbtn_iv"));
		mAcceptBtnIv = (ImageView) videoCapture.findViewById(finder.getId("videocapture_acceptbtn_iv"));
		mDeclineBtnIv = (ImageView) videoCapture.findViewById(finder.getId("videocapture_declinebtn_iv"));
		mSwitchCameraIv = (ImageView) videoCapture.findViewById(EUExUtil.getResIdID("plugin_video_switch_btn"));
		mFlashIv= (ImageView) videoCapture.findViewById(EUExUtil.getResIdID("plugin_video_flash_btn"));
		mTimerTv= (Chronometer) videoCapture.findViewById(EUExUtil.getResIdID("plugin_video_timer_txt"));
//		mTimerTv.setFormat("H:MM:SS");
		mRecordBtnIv.setOnClickListener(this);
		mAcceptBtnIv.setOnClickListener(this);
		mDeclineBtnIv.setOnClickListener(this);
        mSwitchCameraIv.setOnClickListener(this);
        mFlashIv.setOnClickListener(this);
		mThumbnailIv = (ImageView) videoCapture.findViewById(finder.getId("videocapture_preview_iv"));
		mSurfaceView = (SurfaceView) videoCapture.findViewById(finder.getId("videocapture_preview_sv"));
	}

	public void setRecordingButtonInterface(RecordingButtonInterface mBtnInterface) {
		this.mRecordingInterface = mBtnInterface;
	}

	public SurfaceHolder getPreviewSurfaceHolder() {
		return mSurfaceView.getHolder();
	}

	public void updateUINotRecording() {
		mRecordBtnIv.setSelected(false);
		mRecordBtnIv.setVisibility(View.VISIBLE);
		mAcceptBtnIv.setVisibility(View.GONE);
		mDeclineBtnIv.setVisibility(View.GONE);
		mThumbnailIv.setVisibility(View.GONE);
		mSurfaceView.setVisibility(View.VISIBLE);
        mRecordBtnIv.setBackgroundResource(EUExUtil.getResDrawableID("plugin_video_states_btn_capture"));

    }

	public void startTimer(){
	    mTimerTv.setBase(SystemClock.elapsedRealtime());
	    mTimerTv.start();
    }

    public void stopTimer(){
	    mTimerTv.stop();
    }

	public void updateUIRecordingOngoing() {
		mRecordBtnIv.setSelected(true);
		mRecordBtnIv.setVisibility(View.VISIBLE);
		mAcceptBtnIv.setVisibility(View.GONE);
		mDeclineBtnIv.setVisibility(View.GONE);
		mThumbnailIv.setVisibility(View.GONE);
		mSurfaceView.setVisibility(View.VISIBLE);
		mRecordBtnIv.setBackgroundResource(EUExUtil.getResDrawableID("plugin_video_states_btn_recording"));
	}

	public void updateUIRecordingFinished(Bitmap videoThumbnail) {
		mRecordBtnIv.setVisibility(View.INVISIBLE);
		mAcceptBtnIv.setVisibility(View.VISIBLE);
		mDeclineBtnIv.setVisibility(View.VISIBLE);
		mThumbnailIv.setVisibility(View.VISIBLE);
		mSurfaceView.setVisibility(View.GONE);
		final Bitmap thumbnail = videoThumbnail;
		if (thumbnail != null) {
			mThumbnailIv.setScaleType(ScaleType.CENTER_CROP);
			mThumbnailIv.setImageBitmap(videoThumbnail);
		}
	}

	public void setFlashButtonBg(boolean open){
        if (open){
            mFlashIv.setBackgroundResource(EUExUtil.getResDrawableID("plugin_video_flash_open"));
        }else {
            mFlashIv.setBackgroundResource(EUExUtil.getResDrawableID("plugin_video_flash_close"));
        }
    }

    public void setFlashBtnVisible(boolean visible){
	    mFlashIv.setVisibility(visible?VISIBLE:INVISIBLE);
    }

    public void recordMode(boolean recording){
        if (recording){
            mFlashIv.setVisibility(GONE);
            mSwitchCameraIv.setVisibility(GONE);
        }else{
            mFlashIv.setVisibility(VISIBLE);
            mSwitchCameraIv.setVisibility(VISIBLE);
        }
    }

	@Override
	public void onClick(View v) {
		if (mRecordingInterface == null) return;

		if (v.getId() == mRecordBtnIv.getId()) {
			mRecordingInterface.onRecordButtonClicked();
		} else if (v.getId() == mAcceptBtnIv.getId()) {
			mRecordingInterface.onAcceptButtonClicked();
		} else if (v.getId() == mDeclineBtnIv.getId()) {
			mRecordingInterface.onDeclineButtonClicked();
		} else if (v.getId()==mFlashIv.getId()){
		    mRecordingInterface.onFlashButtonClicked();
        }else if (v.getId()==mSwitchCameraIv.getId()){
		    mRecordingInterface.onChangeCameraButtonClicked();
        }

	}

}
