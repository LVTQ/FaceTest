package com.tourcoo.facetest.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/4/20.
 */

public class FaceDetect {

//    private AFD_FSDKEngine face_engine;
//    private AFD_FSDKError face_err;
//    private List<AFD_FSDKFace> face_result;

    private ASGE_FSDKEngine gender_engine;
    private ASGE_FSDKError gender_err;
    private List<ASGE_FSDKGender> gender_result;


    private ASAE_FSDKEngine age_engine;
    private ASAE_FSDKError age_err;
    private List<ASAE_FSDKAge> age_result;


    private AFT_FSDKEngine ft_engine;
    private List<AFT_FSDKFace> ft_result;
    private AFT_FSDKError ft_err;


    public FaceDetect() {


//----------------------------------人脸追踪--------------------------------------------
        ft_engine = new AFT_FSDKEngine();

        // 用来存放检测到的人脸信息列表
        ft_result = new ArrayList<>();

        //初始化人脸跟踪引擎，使用时请替换申请的APPID和SDKKEY
        ft_err = ft_engine.AFT_FSDK_InitialFaceEngine(FaceDB.APP_ID, FaceDB.FT_KEY, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d("com.arcsoft", "AFT_FSDK_InitialFaceEngine =" + ft_err.getCode());


////----------------------------------人脸检测--------------------------------------------
//        face_engine = new AFD_FSDKEngine();
//
//        //初始化人脸检测引擎，使用时请替换申请的APPID和SDKKEY
//        face_err = face_engine.AFD_FSDK_InitialFaceEngine(FaceDB.APP_ID, FaceDB.FD_KEY, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
//        Log.i("com.arcsoft", "AFD_FSDK_InitialFaceEngine = " + face_err.getCode());
//
//        // 用来存放检测到的人脸信息列表
//        face_result = new ArrayList<AFD_FSDKFace>();


//----------------------------------性别检测--------------------------------------------
        gender_engine = new ASGE_FSDKEngine();
        gender_result = new ArrayList<ASGE_FSDKGender>();


        //初始化性别检测引擎，使用时请替换申请的APPID和SDKKEY
        gender_err = gender_engine.ASGE_FSDK_InitgGenderEngine(FaceDB.APP_ID, FaceDB.GENDER_KEY);
        Log.i("com.arcsoft", "ASGE_FSDK_InitgGenderEngine = " + gender_err.getCode());


//----------------------------------年龄检测--------------------------------------------
        age_engine = new ASAE_FSDKEngine();
        // 用来存放检测到的人脸信息列表
        age_result = new ArrayList<ASAE_FSDKAge>();


        //初始化年龄检测引擎，使用时请替换申请的APPID和SDKKEY
        age_err = age_engine.ASAE_FSDK_InitAgeEngine(FaceDB.APP_ID, FaceDB.AGE_KEY);
        Log.i("com.arcsoft", "ASAE_FSDK_InitAgeEngine = " + age_err.getCode());

    }


//    public List<AFD_FSDKFace> fd_process(byte[] data, int width, int height){
//
//        //输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
//        face_err = face_engine.AFD_FSDK_StillImageFaceDetection(data, width, height, AFD_FSDKEngine.CP_PAF_NV21, face_result);
//        Log.i("com.arcsoft", "AFD_FSDK_StillImageFaceDetection =" + face_err.getCode());
//        Log.i("com.arcsoft", "Face=" + face_result.size());
//
//        for (AFD_FSDKFace face : face_result) {
//            Log.e("com.arcsoft", "Face:" + face.toString());
//        }
//
//        return face_result;
//    }


    /**
     * @param data
     * @param width
     * @param height
     * @return 返回人脸追踪时存放的人脸信息列表
     */
    public List<AFT_FSDKFace> ft_process(byte[] data, int width, int height) {

        //输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸跟踪返回结果保存在result。
        ft_err = ft_engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, ft_result);

        return ft_result;
    }


    public List<ASGE_FSDKGender> gender_process(byte[] data, int width, int height, List<ASGE_FSDKFace> gender_input) {
        //输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
        gender_err = gender_engine.ASGE_FSDK_GenderEstimation_Image(data, width, height, ASGE_FSDKEngine.CP_PAF_NV21, gender_input, gender_result);

        return gender_result;
    }


    public List<ASAE_FSDKAge> age_process(byte[] data, int width, int height, List<ASAE_FSDKFace> age_input) {

        //输入的data数据为NV21格式（如Camera里NV21格式的preview数据），其中height不能为奇数，人脸检测返回结果保存在result。
        age_err = age_engine.ASAE_FSDK_AgeEstimation_Image(data, width, height, ASAE_FSDKEngine.CP_PAF_NV21, age_input, age_result);

        return age_result;
    }


    public void releaseEngine() {
        //销毁人脸检测引擎
        ft_err = ft_engine.AFT_FSDK_UninitialFaceEngine();
        Log.i("com.arcsoft", "AFT_FSDK_UninitialFaceEngine =" + ft_err.getCode());

//        //销毁人脸检测引擎
//        face_err = face_engine.AFD_FSDK_UninitialFaceEngine();
//        Log.i("com.arcsoft", "AFD_FSDK_UninitialFaceEngine =" + face_err.getCode());

        //销毁人脸检测引擎
        gender_err = gender_engine.ASGE_FSDK_UninitGenderEngine();
        Log.i("com.arcsoft", "ASGE_FSDK_UninitGenderEngine =" + gender_err.getCode());

        //销毁年龄检测引擎
        age_err = age_engine.ASAE_FSDK_UninitAgeEngine();
        Log.i("com.arcsoft", "ASAE_FSDK_UninitAgeEngine =" + age_err.getCode());
    }

}
