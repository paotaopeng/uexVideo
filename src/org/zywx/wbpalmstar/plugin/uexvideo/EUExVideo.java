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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import android.app.Activity;
import android.app.LocalActivityManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class EUExVideo extends EUExBase implements Parcelable{

    public static final int F_ACT_REQ_CODE_UEX_VIDEO_RECORD = 5;
    public static final String F_CALLBACK_NAME_VIDEO_RECORD = "uexVideo.cbRecord";
    public static final String F_CALLBACK_NAME_VIDEO_ONCOMPLETION = "uexVideo.onCompletion";

    private ResoureFinder finder;

    private File m_tempPath;
    private boolean mWillCompress;
    
    private View mMapDecorView;
	private static LocalActivityManager mgr;
	private int x = 0;
	private int y = 0;
	private int w = 0;
	private int h = 0;

    public EUExVideo(Context context, EBrowserView inParent) {
        super(context, inParent);
        finder = ResoureFinder.getInstance(context);
    }

	public static void onActivityResume(Context context){
		if(mgr != null){
			mgr.dispatchResume();
		}
	}
	
	public static void onActivityPause(Context context){
		if(mgr != null){
			mgr.dispatchPause(((Activity)context).isFinishing());
		}
	}
	
	/**
	 * 打开视频播放器
	 * 
	 * @param inPath
	 *            文件所在路径
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
	 * 打开视频播放器(新的view)
	 * 
	 * @param parm
	 */
	public void createPlay(final String[] params){
		
		if (params.length < 5) {
			return;
		}
		try {
			x = (int) Float.parseFloat(params[0]);
			y = (int) Float.parseFloat(params[1]);
			w = (int) Float.parseFloat(params[2]);
			h = (int) Float.parseFloat(params[3]);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		((Activity)mContext).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if(mMapDecorView != null){
					Log.i("uexVideo", "already open");
					return;
				}
				Intent intent = new Intent();
				String fullPath = params[4];
				Log.i("uexVideo", fullPath);
				if (fullPath == null || fullPath.length() == 0) {
					errorCallback(0,
							EUExCallback.F_ERROR_CODE_VIDEO_OPEN_ARGUMENTS_ERROR,
							finder.getString("path_error"));
					Log.i("uexVideo", "path_error");
					return;
				}
				//fullPath = "http://mp4.mtvxz.cc/troublemaker-troublemaker%5Bj-star%5D%5B%E9%9F%A9%5D%5Bwww.mtvxz.cn%5D.mp4";
				//fullPath = "http://live.cqnews.net/live-m/manifest.m3u8";
				String realPath = BUtility.makeRealPath(fullPath, mBrwView);
		        Uri url = Uri.parse(realPath);
				intent.setData(url);
		        intent.putExtra("x", x);
		        intent.putExtra("y", y);
		        intent.putExtra("w", w);
		        intent.putExtra("h", h);
		        intent.putExtra("EUExVideo", EUExVideo.this);
				intent.setClass(mContext, VideoPlayerActivityForViewToWeb.class);
				//mContext.startActivity(intent);
				if (mgr == null) {
					mgr = new LocalActivityManager((Activity) mContext, true);
					mgr.dispatchCreate(null);
				}
				Window window = mgr.startActivity("TAG_Video", intent);
				mMapDecorView = window.getDecorView();
				RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
						w,h
						//RelativeLayout.LayoutParams.MATCH_PARENT,
						//RelativeLayout.LayoutParams.MATCH_PARENT
						);
				lp.leftMargin = x;
				lp.topMargin = y;
				addView2CurrentWindow(mMapDecorView, lp);
			}
		});
	}
	
	/**
	 * @param child
	 * @param parms
	 */
	private void addView2CurrentWindow(View child,
			RelativeLayout.LayoutParams parms) {
		int l = (int) (parms.leftMargin);
		int t = (int) (parms.topMargin);
		int w = parms.width;
		int h = parms.height;
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
		lp.gravity = Gravity.NO_GRAVITY;
		lp.leftMargin = l;
		lp.topMargin = t;
		// adptLayoutParams(parms, lp);
		// Log.i(TAG, "addView2CurrentWindow");
		mBrwView.addViewToCurrentWindow(child, lp);
	}
	
	public void removePlay(String[] parm){
		((Activity)mContext).runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if(mMapDecorView != null){
					removeViewFromCurrentWindow(mMapDecorView);
					mMapDecorView = null;
					mgr.destroyActivity("TAG_Video", true);
				}
			}
		});
	}

    public void record(String[] params) {
        if (!mWillCompress) {
            String path = mBrwView.getCurrentWidget().getWidgetPath()
                    + getNmae();
            String sdPath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
            if (path.indexOf(sdPath) == -1)
                path = path.replace("/sdcard", sdPath);
            m_tempPath = new File(path);
        } else {
            m_tempPath = new File(BUtility.getSdCardRootPath() + "demo.3gp");
        }
        if (m_tempPath != null && !m_tempPath.exists()) {
            try {
                m_tempPath.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        checkPath();
        if (Build.VERSION.SDK_INT > 8) {
            Uri fileUri = Uri.fromFile(m_tempPath);
            // 创建保存视频的文件
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            // 设置视频文件名
        }
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);// high quality
        try {
            startActivityForResult(intent, F_ACT_REQ_CODE_UEX_VIDEO_RECORD);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(
                    mContext,
                    finder.getString("can_not_find_suitable_app_perform_this_operation"),
                    Toast.LENGTH_SHORT).show();
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
        if (requestCode == F_ACT_REQ_CODE_UEX_VIDEO_RECORD) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            String path = "";
            if (null != data) {
                path = data.getDataString();
                Cursor c = ((Activity) mContext).managedQuery(data.getData(),
                        null, null, null, null);
                if (c != null) {
                    c.moveToFirst();
                    path = c.getString(c
                            .getColumnIndex(MediaStore.Video.VideoColumns.DATA));
                }
            } else {
                path = m_tempPath.getAbsolutePath();
            }
            if (path.startsWith(BUtility.F_FILE_SCHEMA)) {
                path = path.substring(BUtility.F_FILE_SCHEMA.length());
            }
            jsCallback(EUExVideo.F_CALLBACK_NAME_VIDEO_RECORD, 0,
                    EUExCallback.F_C_TEXT, path);
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

}
