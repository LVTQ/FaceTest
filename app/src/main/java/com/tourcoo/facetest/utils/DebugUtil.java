package com.tourcoo.facetest.utils;

import android.os.Looper;
import android.util.Log;

/**
 * Created by Administrator on 2018/4/23.
 */

public class DebugUtil {

    private static final String TAG = "DebugUtil";

    public static void getCurrentThread(){

        if( Looper.getMainLooper().getThread() == Thread.currentThread()){
            Log.e(TAG, "onPreview: 当前运行在主线程之中");
        }else {
            Log.e(TAG, "onPreview: 当前运行在子线程之中");
        }
    }

}
