package com.tourcoo.facetest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.tourcoo.facetest.utils.FaceDB;
import com.tourcoo.facetest.utils.FaceDetect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zsn10 on 2017/7/11.
 */

public class DetecterActivity extends Activity implements CameraSurfaceView.OnCameraListener, View.OnTouchListener,
        Camera.AutoFocusCallback {


    private final String TAG = this.getClass().getSimpleName();

    private int mWidth, mHeight, mFormat;
    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private Camera mCamera;

    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    byte[] mImageNV21 = null;
    FRAbsLoop mFRAbsLoop = null;
    AFT_FSDKFace mAFT_FSDKFace = null;

//    AgeAndGenderAbsLoop mAgeAndGenderAbsLoop;

    Handler mDetectHandler;

    Runnable hide = new Runnable() {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void run() {
            mFaceNum.setAlpha(0.5f);
            mImageView.setImageAlpha(128);
        }
    };


    private int mCameraId;
    private boolean isUseFront;


    class FRAbsLoop extends AbsLoop {

        //定义锁对象
        private ReentrantLock lock;
        private AFR_FSDKFace mAFR_FSDKFace;
        private AFR_FSDKEngine engine;
        private AFR_FSDKError error;

        List<FaceDB.FaceRegist> mResgist = ((MyApplication)
                getApplicationContext()).mFaceDB.mRegister;

        @Override
        public void setup() {
            Log.e(TAG, "test  setup: 识别线程开始工作............");
            lock = new ReentrantLock();
            faceDetect.initAge_Gender_Engine();
            engine = new AFR_FSDKEngine();
            error = engine.AFR_FSDK_InitialEngine(FaceDB.APP_ID, FaceDB.FR_KEY);
            Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error.getCode());

        }

        @Override
        public void loop() {
            try {
                if (mImageNV21 != null && ft_result.size() >= 1) {
                    //加锁
                    lock.lock();
                    List<AFT_FSDKFace> copyFT_Result = ft_result;

                    for (int i = 0; i < copyFT_Result.size(); i++) {

                        mAFT_FSDKFace = copyFT_Result.get(i).clone();

                        byte[] data = new byte[mImageNV21.length];
                        System.arraycopy(mImageNV21, 0, data, 0, data.length);

                        gender_result = faceDetect.gender_process(data, mWidth, mHeight, gender_input);

                        final StringBuffer sb_age = new StringBuffer();
                        final StringBuffer sb_gender = new StringBuffer();

                        for (ASGE_FSDKGender gender : gender_result) {

                            switch (gender.getGender()) {
                                case ASGE_FSDKGender.FEMALE:
                                    sb_gender.append("女性  ");
                                    break;
                                case ASGE_FSDKGender.MALE:
                                    sb_gender.append("男性  ");
                                    break;
                                case ASGE_FSDKGender.UNKNOWN:
                                    sb_gender.append("无法识别  ");
                                    break;
                            }
                        }

                        age_result = faceDetect.age_process(mImageNV21, mWidth, mHeight, age_input);

                        for (ASAE_FSDKAge age : age_result) {
                            sb_age.append(age.getAge() + "  ");
                        }


                        mDetectHandler.removeCallbacks(hide);
                        mDetectHandler.post(new Runnable() {
                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                            @Override
                            public void run() {
                                if (!sb_age.toString().equals("")) {
                                    mAge.setText(sb_age.toString());
                                }


                                if (!sb_gender.toString().equals("")) {
                                    mGender.setText(sb_gender.toString());
                                }

                            }
                        });


                        //定义一个人脸识别结果待存储list中的人脸
                        final AFR_FSDKFace fr_result = new AFR_FSDKFace();

                        //检测输入图像中的人脸特征信息，输出结果保存在 AFR_FSDKFace feature
                        error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), fr_result);
                        Log.d(TAG, "Face=" + fr_result.getFeatureData()[0] + "," + fr_result.getFeatureData()[1] + "," + fr_result.getFeatureData()[2] + "," + error.getCode());

                        //这个类用来保存特征信息匹配度
                        AFR_FSDKMatching score = new AFR_FSDKMatching();
                        float max = 0.0f;
                        String name = null;

                        Log.e(TAG, "save_face:  当前注册的人脸数： " + mResgist.size());
                        for (FaceDB.FaceRegist fr : mResgist) {
                            for (AFR_FSDKFace face : fr.mFaceList) {
                                error = engine.AFR_FSDK_FacePairMatching(fr_result, face, score);
                                Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                                if (max < score.getScore()) {
                                    max = score.getScore();
                                    name = fr.mName;
                                }
                            }
                        }

                        //当前人脸识别 置信度大于0.6  则证明从人脸库中寻找到了相匹配的人脸特征，识别成功
                        if (max > 0.6f) {
                            //fr success.
                            Log.e(TAG, "save_face: ----------    识别人脸成功 ");
                            mDetectHandler.removeCallbacks(hide);
                            break;
                        }
                        //从人脸库中无法识别-------》 注册到人脸库中
                        else {
                            Log.e(TAG, "save_face: ----------    从人脸库中无法识别-------》 注册到人脸库中" + copyFT_Result.size());
                            if (copyFT_Result.size() != 0 && copyFT_Result.size() == age_result.size() && copyFT_Result.size() == gender_result.size()
                                    && (age_result.get(i).getAge() != 0
                                    && gender_result.get(i).getGender() != -1)) {


                                if (i == copyFT_Result.size() || mResgist.size() == 0) {
                                    //只有当与数据库中的所有人脸都识别完成之后 还没有识别出来 则证明识别失败，此人是第一次使用
                                    Log.e(TAG, "save_face: ----------    应该添加人脸信息");
                                }


                                Log.e(TAG, "save_face: ----------    识别人脸失败");

                            }


                            //如果人脸检测引擎返回的结果不为空
                            if (!copyFT_Result.isEmpty()) {

                                //fr_result保存了人脸特征信息
                                if (error.getCode() == error.MOK
                                        && copyFT_Result.size() == age_result.size() && copyFT_Result.size() == gender_result.size()
                                        && (age_result.get(i).getAge() != 0
                                        && gender_result.get(i).getGender() != -1)
                                        ) {
                                    //多个人脸特征时会导致同时操作一个变量出现错误
                                    mAFR_FSDKFace = fr_result.clone();

                                    boolean isAdded = ((MyApplication) getApplicationContext()).mFaceDB.addFace(mResgist.size() + "", mAFR_FSDKFace);

                                    if (isAdded) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(DetecterActivity.this, "添加人脸信息成功", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        Log.e(TAG, "save_face: 添加人脸信息成功");
                                    }


                                }
                            }

                        }

                    }
                    mImageNV21 = null;
                    lock.unlock();
                }
            } catch (Exception e) {
                Log.e(TAG, "save_face:  --- error --- " + e);
            }


        }

        @Override
        public void over() {
            Log.e(TAG, "test  setup: 关闭人脸识别线程中..............");
            AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
            Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error.getCode());

            faceDetect.releaseAge_Gender_Engine();
        }
    }


//    class AgeAndGenderAbsLoop extends Thread {
//
//        volatile boolean isExist = false;
//
//        @Override
//        public void run() {
//            super.run();
//
//            while (!isExist) {
//                try {
//                    if (mImageNV21 != null) {
//
//                        gender_result = faceDetect.gender_process(mImageNV21, mWidth, mHeight, gender_input);
//
//                        final StringBuffer sb_age = new StringBuffer();
//                        final StringBuffer sb_gender = new StringBuffer();
//
//                        for (ASGE_FSDKGender gender : gender_result) {
//
//                            switch (gender.getGender()) {
//                                case ASGE_FSDKGender.FEMALE:
//                                    sb_gender.append("女性  ");
//                                    break;
//                                case ASGE_FSDKGender.MALE:
//                                    sb_gender.append("男性  ");
//                                    break;
//                                case ASGE_FSDKGender.UNKNOWN:
//                                    sb_gender.append("无法识别  ");
//                                    break;
//                            }
//                        }
//
//                        age_result = faceDetect.age_process(mImageNV21, mWidth, mHeight, age_input);
//
//                        for (ASAE_FSDKAge age : age_result) {
//                            sb_age.append(age.getAge() + "  ");
//                        }
//
//
//                        mDetectHandler.removeCallbacks(hide);
//                        mDetectHandler.post(new Runnable() {
//                            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
//                            @Override
//                            public void run() {
//                                if (!sb_age.toString().equals("")) {
//                                    mAge.setText(sb_age.toString());
//                                }
//
//
//                                if (!sb_gender.toString().equals("")) {
//                                    mGender.setText(sb_gender.toString());
//                                }
//
//                            }
//                        });
//                    }
//                } catch (Exception e) {
//                    //防止某个变量获取失败致使空指针使程序崩溃
//                }
//            }
//
//
//        }
//
//    }

    private TextView mFaceNum;  //检测到的人数
    private TextView mGender;   //性别
    private TextView mAge;      //年龄
    private TextView mTextView; //置信度
    private ImageView mImageView;
    private Button btn_save;

    private FaceDetect faceDetect;
    private List<AFT_FSDKFace> ft_result = new ArrayList<>();
    private List<ASAE_FSDKAge> age_result = new ArrayList<>();
    private List<ASGE_FSDKGender> gender_result = new ArrayList<>();

    private List<ASGE_FSDKFace> gender_input = new ArrayList<ASGE_FSDKFace>();
    private List<ASAE_FSDKFace> age_input = new ArrayList<ASAE_FSDKFace>();

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_camera);
        mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);

        mSurfaceView.debug_print_fps(true, false);

        //snap
        mFaceNum = (TextView) findViewById(R.id.textView);
        mFaceNum.setText("");
        mFaceNum.setTextColor(Color.RED);

        mGender = (TextView) findViewById(R.id.textView1);
        mGender.setText("");
        mGender.setTextColor(Color.RED);

        mAge = (TextView) findViewById(R.id.textView2);
        mAge.setText("");
        mAge.setTextColor(Color.RED);

        mTextView = (TextView) findViewById(R.id.textView3);
        mTextView.setText("");
        mTextView.setTextColor(Color.RED);

        mImageView = (ImageView) findViewById(R.id.imageView);

        btn_save = findViewById(R.id.btn_save);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mDetectHandler = new Handler();
        mWidth = 1024;
        mHeight = 768;
        mFormat = ImageFormat.NV21;

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.APP_ID, FaceDB.FT_KEY, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        faceDetect = new FaceDetect();

//        mAgeAndGenderAbsLoop = new AgeAndGenderAbsLoop();
//        mAgeAndGenderAbsLoop.start();

        mFRAbsLoop = new FRAbsLoop();
        mFRAbsLoop.start();
    }


    /* (non-Javadoc)
         * @see android.app.Activity#onDestroy()
         */
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
//        mAgeAndGenderAbsLoop.isExist = true;
        faceDetect.releaseAge_Gender_Engine();
    }

    @Override
    public Camera setupCamera() {
        // TODO Auto-generated method stub
        // 只有一个摄相头，打开后置
//		if (Camera.getNumberOfCameras() == 1) {
//			isUseFront=false;
//			mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
//		}else{
//			isUseFront=true;
//			mCameraId= Camera.CameraInfo.CAMERA_FACING_FRONT;
//		}
        mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        mCamera = Camera.open(0);
        int orientation = setCameraDisplayOrientation(this, 0, mCamera);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, false, orientation);


        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);
            Log.e(TAG, "setupCamera: " + mWidth + "    |    " + mHeight + "   |   " + mFormat);
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                //非常罕见的情况
                //个别机型在SupportPreviewSizes里汇报了支持某种预览尺寸，但实际是不支持的，设置进去就会抛出RuntimeException.
                e.printStackTrace();
                Log.e(TAG, "setupCamera: 不支持某种预览尺寸");
                try {
                    //遇到上面所说的情况，只能设置一个最小的预览尺寸
                    parameters.setPreviewSize(1024, 768);
                    mCamera.setParameters(parameters);

                } catch (Exception e1) {
                    //到这里还有问题，就是拍照尺寸的锅了，同样只能设置一个最小的拍照尺寸
                    e1.printStackTrace();
                    Log.e(TAG, "setupCamera: 拍照尺寸不支持");
                    try {
                        parameters.setPictureSize(1024, 768);
                        mCamera.setParameters(parameters);
                    } catch (Exception ignored) {

                    }
                }
            }


            //获取摄像头支持的各种分辨率
            List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();

            for (Camera.Size size : supportedPictureSizes) {
                Log.d(TAG, "摄像头支持的分辨率SIZE:" + size.width + "x" + size.height);
            }

            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "预览分辨率SIZE:" + size.width + "x" + size.height);
            }

            for (Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }

            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for (int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            //parameters.setPreviewFpsRange(15000, 30000);
            //parameters.setExposureCompensation(parameters.getMaxExposureCompensation());
            //parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            //parameters.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
            //parmeters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            //parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            //parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);
            //mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
    }

    /**
     * 旋转镜头成像
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    public int setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
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

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
            isUseFront = true;
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
            isUseFront = false;
        }

        camera.setDisplayOrientation(result);
        return result;
    }

    @Override
    public void setupChanged(int format, int width, int height) {
    }

    @Override
    public boolean startPreviewLater() {
        // TODO Auto-generated method stub


        return false;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        if (isUseFront) {
            Mirror(data, width, height);
        }
        ft_result = new ArrayList<>();
        ft_result = faceDetect.ft_process(data, width, height);

        mFaceNum.setText("检测到的人数：" + ft_result.size());

        gender_input.clear();
        age_input.clear();
        for (AFT_FSDKFace face : ft_result) {
            //这里人脸框和角度，请根据实际对应图片中的人脸框和角度填写
            gender_input.add(new ASGE_FSDKFace(face.getRect(), face.getDegree()));
            //这里人脸框和角度，请根据实际对应图片中的人脸框和角度填写
            age_input.add(new ASAE_FSDKFace(face.getRect(), face.getDegree()));
        }


        mImageNV21 = new byte[data.length];
        System.arraycopy(data, 0, mImageNV21, 0, mImageNV21.length);


        //copy rects
        Rect[] rects = new Rect[ft_result.size()];
        for (int i = 0; i < ft_result.size(); i++) {
            rects[i] = new Rect(ft_result.get(i).getRect());
        }

        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
        //绘制人脸框图
        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
    }


    /**
     * nv21的镜像水平翻转处理
     *
     * @param src
     * @param w
     * @param h
     */
    private void Mirror(byte[] src, int w, int h) { //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
                b--;
            }
        }

        // mirror u and v
        index = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w;
            b = (i + 1) * w - 2;
            while (a < b) {
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;

                temp = src[a + index + 1];
                src[a + index + 1] = src[b + index + 1];
                src[b + index + 1] = temp;
                a += 2;
                b -= 2;
            }
        }
    }


}
