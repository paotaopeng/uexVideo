package org.zywx.wbpalmstar.plugin.uexvideo.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by Administrator on 2018/5/11.
 */

public class NetworkUtils {
    private static final int F_JV_CONNECT_UNREACHABLE = -1;
    private static final int F_JV_CONNECT_WIFI = 0;
    private static final int F_JV_CONNECT_3G = 1;
    private static final int F_JV_CONNECT_GPRS = 2;
    private static final int F_JV_CONNECT_4G = 3;
    private static final int F_JV_CONNECT_UNKNOWN = 4;
    private static final String TAG = "NetworkUtils";

    public static int getNetworkStatus(Context context) {
        int status = F_JV_CONNECT_UNREACHABLE;
        try {
            ConnectivityManager cm = (ConnectivityManager) context
                    .getApplicationContext().getSystemService(
                            Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo info = cm.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    switch (info.getType()) {
                        case ConnectivityManager.TYPE_MOBILE:
                            TelephonyManager telephonyManager = (TelephonyManager) context
                                    .getSystemService(Context.TELEPHONY_SERVICE);
                            switch (telephonyManager.getNetworkType()) {
                                case TelephonyManager.NETWORK_TYPE_1xRTT:
                                case TelephonyManager.NETWORK_TYPE_CDMA:
                                case TelephonyManager.NETWORK_TYPE_EDGE:
                                case TelephonyManager.NETWORK_TYPE_GPRS:
                                    status = F_JV_CONNECT_GPRS;
                                    break;
                                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                                case TelephonyManager.NETWORK_TYPE_HSDPA:
                                case TelephonyManager.NETWORK_TYPE_HSPA:
                                case TelephonyManager.NETWORK_TYPE_HSUPA:
                                case TelephonyManager.NETWORK_TYPE_UMTS:
                                    status = F_JV_CONNECT_3G;
                                    break;
                                case TelephonyManager.NETWORK_TYPE_LTE:
                                case TelephonyManager.NETWORK_TYPE_EHRPD:
                                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                                case TelephonyManager.NETWORK_TYPE_HSPAP:
                                case TelephonyManager.NETWORK_TYPE_IDEN:
                                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                                    status = F_JV_CONNECT_4G;
                                    break;
                                default:
                                    status = F_JV_CONNECT_UNKNOWN;
                                    break;
                            }
                            break;
                        case ConnectivityManager.TYPE_WIFI:
                            status = F_JV_CONNECT_WIFI;
                            break;
                    }
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "no_permisson_declare");
        }
        return status;
    }
}
