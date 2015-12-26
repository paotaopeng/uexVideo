package org.zywx.wbpalmstar.plugin.uexvideo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EUExVideo extends EUExBase {

    public static final int F_ACT_REQ_CODE_UEX_VIDEO_RECORD = 5;
    public static final String F_CALLBACK_NAME_VIDEO_RECORD = "uexVideo.cbRecord";

    private ResoureFinder finder;

    private File m_tempPath;
    private boolean mWillCompress;

    public EUExVideo(Context context, EBrowserView inParent) {
        super(context, inParent);
        finder = ResoureFinder.getInstance(context);
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
        String realPath=BUtility.makeRealPath(fullPath,mBrwView);
        Uri url = Uri.parse(realPath);
        intent.setData(url);
        intent.setClass(mContext, VideoPlayerActivity.class);
        mContext.startActivity(intent);
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

        return false;
    }

}
