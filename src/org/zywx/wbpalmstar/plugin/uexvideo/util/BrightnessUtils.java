package org.zywx.wbpalmstar.plugin.uexvideo.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by Administrator on 2018/5/10.
 */

public class BrightnessUtils {
    private static float maxBrightness = 255f;


    /*
     * 调整亮度范围
     */
    private static int adjustBrightnessNumber(int brightness) {
        if (brightness < 0) {
            brightness = 0;
        } else if (brightness > 255) {
            brightness = 255;
        }
        return brightness;
    }

    /*
     * 关闭自动调节亮度
     */
    public static void offAutoBrightness(Context context) {
        ContentResolver resolver = context.getContentResolver();
        try {
            if (Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(resolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    /*
     * 获取系统亮度
     */
    public static int getBrightness(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    /*
     * 设置系统亮度，如果有设置了自动调节，请先调用offAutoBrightness()方法关闭自动调节，否则会设置失败
     */
    public static void setSystemBrightness(Context context, int newBrightness) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS
                , adjustBrightnessNumber(newBrightness));
    }

    public static float getMaxBrightness() {
        return maxBrightness;
    }

    //设置当前Window的亮度
    public static void setWindowBrightness(Activity activity,float brightnessPercent) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.screenBrightness = brightnessPercent;
        window.setAttributes(layoutParams);
    }

    //获取当前Window的亮度
    public static float getWindowBrightness(Activity activity) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        return layoutParams.screenBrightness;
    }
}
