package org.zywx.wbpalmstar.plugin.uexvideo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.zywx.wbpalmstar.base.ResoureFinder;

import java.io.IOException;
import java.util.List;

public class RecordVideoActivity extends Activity implements SurfaceHolder.Callback{
    private final String TAG = "RecordVideoActivity";
    private ResoureFinder finder;

    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;


    private TextView tvCapture;// 拍摄按钮
    private MediaRecorder mediaRecorder;// 录制视频的类
    private SurfaceView surfaceview;// 显示视频的控件
    // 用来显示视频的一个接口，我靠不用还不行，也就是说用mediarecorder录制视频还得给个界面看
    private SurfaceHolder surfaceHolder;

    private boolean isOnRecording = false;
    private String filePath;

    private Camera camera;
    private int cameraId;


    private int maxDuration;
    private int qualityType;
    private int maxFileLength;
    private String fileType;

    private boolean isLandscape = false;


    private int displayOrientation;
    private int layoutOrientation;
    private List<Camera.Size> supportedVideoSizes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 选择支持半透明模式,在有surfaceview的activity中使用。
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        finder = ResoureFinder.getInstance(this);
        setContentView(finder.getLayoutId("plugin_video_activity_record_video"));
        initView();
        Intent intent = getIntent();
        maxDuration = intent.getIntExtra("maxDuration", 0);
        qualityType = intent.getIntExtra("qualityType", 0);
        maxFileLength = intent.getIntExtra("maxFileLength", 0);
        fileType = intent.getStringExtra("fileType");


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initView() {

        surfaceview = (SurfaceView) this.findViewById(finder.getId("surfaceview"));
        tvCapture = (TextView) this.findViewById(finder.getId("tv_capture"));
        tvCapture.setOnClickListener(listener);
        SurfaceHolder holder = surfaceview.getHolder();// 取得holder
        holder.addCallback(this); // holder加入回调接口
        // setType必须设置，要不出错.
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.i("TAG portrait:" , "" + (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT));
            int width =  getWindowManager().getDefaultDisplay().getWidth();
            int height =  getWindowManager().getDefaultDisplay().getHeight();
            Log.i(TAG, "height:" + height + "    width:" + width);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Log.i(TAG, "rotation:" + rotation);


            if (!isOnRecording) {
                isOnRecording = true;

                mediaRecorder = new MediaRecorder();// 创建mediarecorder对象
                camera.unlock();
                mediaRecorder.setCamera(camera);
                // Step 2: Set sources
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//                mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
                // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

                mediaRecorder.setVideoSize(640, 480);
//                if (qualityType == 1) {
//                    mediaRecorder.setVideoSize(1280, 720);
//                } else if (qualityType == 2) {
//                    mediaRecorder.setVideoSize(960, 540);
//                } else if (qualityType == 3) {
//                    mediaRecorder.setVideoSize(640, 480);
//                } else {
//                    mediaRecorder.setVideoSize(supportedVideoSizes.get(0).width, supportedVideoSizes.get(0).height);
//                }

//                mediaRecorder.setOrientationHint(getCameraRotation());

                // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
//                mediaRecorder.setVideoFrameRate(20);
                mediaRecorder.setVideoEncodingBitRate(3000 * 1000);
                mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                // 设置视频文件输出的路径
                mediaRecorder.setOutputFile("/sdcard/love.mp4");
                try {
                    // 准备录制
                    mediaRecorder.prepare();
                    // 开始录制
                    mediaRecorder.start();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tvCapture.setText("拍摄中");
            } else {
                if (mediaRecorder != null) {
                    // 停止录制
                    mediaRecorder.stop();
                    // 释放资源
                    mediaRecorder.release();

                    mediaRecorder = null;
                    if (camera != null){
                        camera.lock();
                        camera.release();        // release the camera for other applications
                        camera = null;
                    }
                    isOnRecording = false;
                    //tvCapture.setText("拍摄");
                    Intent intent = new Intent();
                    intent.putExtra("path", filePath);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }

        }
    };


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        if(camera == null) {
            camera = Camera.open();

            try {
                determineDisplayOrientation();
                setBestPreviewSize();
                camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                camera.startPreview();//开始预览
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    public void setBestPreviewSize() {
        //set up camera preview.
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedVideoSizes = parameters.getSupportedVideoSizes();
        for (Camera.Size size: supportedVideoSizes) {
            Log.i(TAG, "video size:" + size.height + "    " + size.width);
        }

        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
                surfaceview.getWidth(), surfaceview.getHeight());
        supportedVideoSizes = parameters.getSupportedVideoSizes();
        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;
        Log.i(TAG, "best preview size:" + profile.videoFrameWidth + "     " + profile.videoFrameHeight);

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        camera.setParameters(parameters);

    }

    public int getCameraRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i(TAG, "orientation:" + rotation);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 90; break;
            case Surface.ROTATION_90: degrees = 0; break;
            case Surface.ROTATION_180: degrees = 270; break;
            case Surface.ROTATION_270: degrees = 180; break;
        }
        return degrees;

    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged  width:" + width + "    height:" + height);
        surfaceHolder = holder;
    }



    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // surfaceDestroyed的时候同时对象设置为null
        surfaceview = null;
        surfaceHolder = null;
        mediaRecorder = null;
        if (camera != null){
            camera.release();
            camera = null;
        }
    }
    /**
     * Determine the current display orientation and rotate the camera preview
     * accordingly.
     */
    public void determineDisplayOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees  = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int displayOrientation;

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        this.displayOrientation = displayOrientation;
        this.layoutOrientation  = degrees;

        camera.setDisplayOrientation(displayOrientation);
    }
}
