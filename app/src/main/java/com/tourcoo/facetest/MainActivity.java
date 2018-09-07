package com.tourcoo.facetest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.guo.android_extend.image.ImageConverter;
import com.tourcoo.facetest.utils.FaceDetect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.camera_surfv)
    SurfaceView surfaceView;
    @BindView(R.id.btn_take_photo)
    Button btnTakePhoto;
    @BindView(R.id.btn_choose_image)
    Button btnChooseImage;
    @BindView(R.id.img_photo)
    ImageView imgPhoto;

    private SurfaceCallback surfaceCallback = null;
    private Camera camera;
    private Camera.Parameters parameters = null;
    private boolean isPreview = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initSurface();


    }

    @OnClick({R.id.btn_take_photo, R.id.btn_choose_image})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_take_photo:
                if (camera != null) {
                    //启动拍照、人脸识别线程
                    takePhoto();
                }

                break;
            case R.id.btn_choose_image:

                //获取系统选择图片intent
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                //开启选择图片功能响应码为1
                startActivityForResult(intent, 1);

                break;
        }
    }



    private String ImagePath;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (data != null) {
                //获取图片路径
                //获取所有图片资源
                Uri uri = data.getData();
                //设置指针获得一个ContentResolver的实例
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                //返回索引项位置
                int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                //返回索引项路径
                ImagePath = cursor.getString(index);
                cursor.close();
                //这个jar包要求请求的图片大小不得超过3m所以要进行一个压缩图片操作
                resizePhoto();
//                myPhoto.setImageBitmap(myBitmapImage);
//                tip.setText("Click Detect==>");
            }
        }
    }


    private void resizePhoto() {
        //得到BitmapFactory的操作权
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 如果设置为 true ，不获取图片，不分配内存，但会返回图片的高宽度信息。
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ImagePath, options);
        //计算宽高要尽可能小于1024
        double ratio = Math.max(options.outWidth * 1.0d / 1024f, options.outHeight * 1.0d / 1024f);
        //设置图片缩放的倍数。假如设为 4 ，则宽和高都为原来的 1/4 ，则图是原来的 1/16 。
        options.inSampleSize = (int) Math.ceil(ratio);
        //我们这里并想让他显示图片所以这里要置为false
        options.inJustDecodeBounds = false;
        //利用Options的这些值就可以高效的得到一幅缩略图。
        Bitmap myBitmapImage = BitmapFactory.decodeFile(ImagePath, options);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        myBitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] datas = baos.toByteArray();

        // 人脸识别
//        FaceDetect.DetectThread.setPhotoData(datas);
//        FaceDetect.DetectThread.setBizTag(2);

    }



    /**
     * 初始化Surface
     */
    protected void initSurface() {
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);//获取WM对象
        DisplayMetrics dm = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(dm);
        surfaceView.getHolder().setFixedSize(dm.widthPixels, dm.heightPixels);    //设置Surface分辨率
        surfaceView.getHolder().setSizeFromLayout();
        surfaceView.getHolder().setKeepScreenOn(true);// 屏幕常亮
        surfaceCallback = new SurfaceCallback();
        surfaceView.getHolder().addCallback(surfaceCallback);//为SurfaceView的句柄添加一个回调函数
    }


    @Override
    public void onDestroy() {

        if (camera != null) {
            if (isPreview) {
                camera.stopPreview();
            }

            camera.release(); // 释放照相机
            camera = null;
        }

        surfaceCallback = null;


        super.onDestroy();
    }


    /**
     * 拍照
     */
    public void takePhoto() {
        if (camera != null && isPreview) {
            // 拍照
            camera.takePicture(null, null, new MyPictureCallback());
        }
    }

    private final class MyPictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                isPreview = false;//设置是否预览参数为假
//                Toast.makeText(getApplicationContext(), "Take photo Success",
//                        Toast.LENGTH_SHORT).show();
                Log.i("DetectFace", "图片大小：" + data.length);

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                bitmap = rotateToDegrees(bitmap, 0 - getPreviewDegree());
                imgPhoto.setImageBitmap(bitmap);

                // 人脸检测
                FaceDetect detect = new FaceDetect();
                byte[] formatData = new byte[bitmap.getWidth()*bitmap.getHeight()*3/2];
                ImageConverter converter = new ImageConverter();
                converter.initial(bitmap.getWidth(),bitmap.getHeight(),ImageConverter.CP_PAF_NV21);
                if(converter.convert(bitmap,formatData)){
                    Log.e(TAG, "onPictureTaken: convert  ok!!!" );
                }
                converter.destroy();

                detect.ft_process(formatData,bitmap.getWidth(),bitmap.getHeight());
                detect.releaseAge_Gender_Engine();
                // 保存图片到sd卡中
                saveToSDCard(data);

                camera.startPreview(); // 拍完照后，重新开始预览
                isPreview = true;//设置是否预览参数为真
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * SurfaceView的句柄添加一个回调函数
     *
     * @author boss
     */
    private final class SurfaceCallback implements SurfaceHolder.Callback {

        // 拍照状态变化时调用该方法
        @SuppressLint("NewApi")
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            //实现自动对焦
            if (camera != null && !isPreview) {
                parameters = camera.getParameters(); // 获取各项参数
                parameters.setPreviewSize(width, height); // 设置预览大小
                // 设置预览照片时每秒显示多少帧的最小值和最大值
                parameters.setPreviewFpsRange(4, 10);
                parameters.setPictureFormat(ImageFormat.JPEG); // 设置图片格式
                parameters.set("jpeg-quality", 100); // 设置JPG照片的质量
                parameters.setPictureSize(width, height); // 设置保存的图片尺寸
                parameters.setPreviewFormat(ImageFormat.NV21);//设置数据格式

//                parameters.setRotation(90);//设置拍照之后图片的方向

                camera.startPreview(); // 开始预览
                isPreview = true;//设置是否预览参数为真
            }
        }

        // 开始拍照时调用该方法
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (camera == null) {
                try {
                    camera = Camera.open(1); // 打开摄像头
                    camera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
                    camera.setDisplayOrientation(getPreviewDegree());
                    //自动对焦
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            if (success) {
//                                initSurface();//实现相机的参数初始化
                                camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                            }
                        }

                    });
                    camera.startPreview(); // 开始预览
                    isPreview = true;//设置是否预览参数为真
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 停止拍照时调用该方法
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                if (isPreview) {
                    camera.stopPreview();
                }

                camera.release(); // 释放照相机
                camera = null;
            }
        }
    }


    /**
     * 把Bitmap转Byte
     *
     * @Author HEH
     * @EditTime 2010-07-19 上午11:45:56
     */
    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return tmpBitmap =
                Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                        true);
    }


    /**
     * 将拍下来的照片存放在SD卡中
     *
     * @param data
     * @throws IOException
     */
    public static void saveToSDCard(byte[] data) throws IOException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss"); // 格式化时间
        String filename = format.format(date) + ".jpg";
        File fileFolder = new File(Environment.getExternalStorageDirectory()
                + "/rebot/cache/");
        if (!fileFolder.exists()) { // 如果目录不存在，则创建一个名为"cache"的目录
            fileFolder.mkdirs();
        }
        File jpgFile = new File(fileFolder, filename);
        FileOutputStream outputStream = new FileOutputStream(jpgFile); // 文件输出流
        outputStream.write(data); // 写入sd卡中
        outputStream.close(); // 关闭输出流
    }


    private static final String TAG = "BodyInductionFragment";

    /**
     * 根据手机方向获得相机预览画面旋转的角度
     */
    public int getPreviewDegree() {
        // 获得手机的方向
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        Log.e(TAG, "getPreviewDegree: 旋转角度：rotation : " + rotation + "   |  degree:" + degree);
        return degree;
    }







}
