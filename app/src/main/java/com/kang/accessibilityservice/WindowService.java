package com.kang.accessibilityservice;

import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kang.accessibilityservice.utils.NavigationBar;
import com.kang.accessibilityservice.utils.RuntimeExec;
import com.orhanobut.logger.Logger;

/**
 * Created by kangren on 2018/1/5.
 */

public class WindowService extends Service {

    private static final int UNDER_SCORE_Y = 300;

    private static final double PRESS_COEFFICIENT = 1.392;

    private static final int PIECE_BASE_HEIGHT = 20;

    private static final int PIECE_BODY_WIDTH = 70;

    private static final int SWIPE_X1 = 500;

    private static final int SWIPE_Y1 = 1600;

    private static final int SWIPE_X2 = 502;

    private static final int SWIPE_Y2 = 1602;

    /**
     * 悬浮窗布局参数
     */
    private WindowManager.LayoutParams params;

    /**
     * 悬浮窗开始按钮
     */
    private Button start;

    /**
     * WindowManager用于产生悬浮窗
     */
    private WindowManager manager;

    /**
     * 用于获取屏幕
     */
    private MediaProjectionManager projectionManager;

    private MediaProjection mMediaProjection;

    private VirtualDisplay mVirtualDisplay;

    private ImageReader mImageReader;

    /**
     * 截图存储路径，调试用，正式功能不需要存储
     */
    private String path;

    /**
     * 手机屏幕参数
     */
    private int width;
    private int height;
    private int densityDpi;

    /**
     * 存储图片像素点的数组
     */
    private int[] pixels;

    private int pieceX;

    private int pieceY;

    private int boardX;

    private int boardY;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createWindow();
        initEnvironment();
    }

    private void initEnvironment() {
        Intent intent = ((MyApplication)getApplication()).getData();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mMediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, intent);
        path = Environment.getExternalStorageDirectory().getPath() + "/auto.png";

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        //如果当前存在NavigationBar
        if (NavigationBar.hasNavBar(WindowService.this))
        {
            height += NavigationBar.getNavigationBarHeight(WindowService.this);
        }
        densityDpi = metrics.densityDpi;
        Logger.d("width:" + width + " height:" + height);

        //第一次录屏初始化数组
        if (pixels == null)
        {
            pixels = new int[width * height];
        }
    }


    private void createWindow() {
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        params = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            //android 8.0适配
            params.flags = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        else
        {
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }

        //设置窗口初始停靠位置.
        params.gravity = Gravity.TOP;
        params.x = 0;
        params.y = 0;

        //设置宽度和高度
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LinearLayout linearLayout = (LinearLayout) View.inflate(this, R.layout.window_layout, null);
        start = linearLayout.findViewById(R.id.start);
        manager.addView(linearLayout, params);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    public void run() {
                        //start virtual
                        startVirtual();
                    }
                }, 500);

                Handler handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    public void run() {
                        //capture the screen
                        startCapture();
                    }
                }, 1500);
            }
        });
    }

    private void startVirtual() {
        mImageReader = ImageReader.newInstance(width, height, 0x1, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                width, height, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }

    private void startCapture()
    {
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        image.close();
        getPicturePixel(bitmap);

        double distance = Math.sqrt((pieceX - boardX) * (pieceX - boardX) + (pieceY - boardY) * (pieceY - boardY));
        jump(distance);


//        if(bitmap != null) {
//            try{
//                File fileImage = new File(path);
//                if(!fileImage.exists()){
//                    fileImage.createNewFile();
//                }
//                FileOutputStream out = new FileOutputStream(fileImage);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//                out.flush();
//                out.close();
//                Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//                Uri contentUri = Uri.fromFile(fileImage);
//                media.setData(contentUri);
//                this.sendBroadcast(media);
//            } catch (IOException e){
//                e.printStackTrace();
//            }
//        }
    }

    private void jump(double distance)
    {
        double pressTime = distance * PRESS_COEFFICIENT;
        //设置最小按压时间为200ms
        pressTime = Math.max(pressTime, 200);
        int press = (int) pressTime;
        String command = "input swipe " + SWIPE_X1 + " " + SWIPE_Y1 + " " + SWIPE_X2 + " " + SWIPE_Y2
                + " " + press;
        Logger.d("command:" + command);
        RuntimeExec.justExecute(command);
    }

    /**
     * 获得当前屏幕关键点坐标
     * @param bitmap
     */
    private void getPicturePixel(Bitmap bitmap) {
        //扫描开始的Y坐标
        int scanStartY = getScanStartY(bitmap);
        Logger.d("扫描开始Y:" + scanStartY);

        int pieceXSum = 0;
        int num1 = 0;
        int pieceYMax = 0;

        //扫描棋子时的左右边界
        int scanBorderX = width / 8;
        for (int i = scanStartY; i > height / 3; i--)
        {
            //棋子不可能靠近屏幕左右边框，因此采用scanBorderX减少扫描开销
            for (int j = scanBorderX; j <= width - scanBorderX; j++)
            {
                int temp = pixels[width * i + j];
                int red = Color.red(temp);
                int green = Color.green(temp);
                int blue = Color.blue(temp);
                if (red > 50 && red < 60 && green > 53 && green < 63 && blue > 95 && blue < 110)
                {
                    pieceXSum += j;
                    num1++;
                    pieceYMax = Math.max(i, pieceYMax);
                }
            }
        }

        pieceX = pieceXSum / num1;
        //上移棋子底盘高度的一半
        pieceY = pieceYMax + PIECE_BASE_HEIGHT;

        Logger.d("棋子X:" + pieceX + " Y:" + pieceY);

        boardX = 0;
        boardY = 0;
        //目标台子边界，默认在当前棋子右边
        int boardStartX = pieceX;
        int boardEndX = width;
        //棋子在右边，目标台子在左边
        if (pieceX > width / 2)
        {
            boardStartX = 0;
            boardEndX = pieceX;
        }

        int lastI = 0;
        for (int i = height / 3; i <= height * 2 / 3; i++)
        {
            int lastPixel = pixels[width * i];
            int red = Color.red(lastPixel);
            int green = Color.green(lastPixel);
            int blue = Color.blue(lastPixel);

            if (boardX != 0 || boardY != 0)
            {
                lastI = i - 1;
                break;
            }
            int boardXSum = 0;
            int num2 = 0;
            for (int j = boardStartX; j <= boardEndX; j++)
            {
                int temp = pixels[width * i + j];
                //解决棋子头比下一个台子还高的情况
                if (Math.abs(j - pieceX) < PIECE_BODY_WIDTH)
                {
                    continue;
                }
                int redT = Color.red(temp);
                int greenT = Color.green(temp);
                int blueT = Color.blue(temp);
                if ((Math.abs(redT - red) + Math.abs(greenT - green) + Math.abs(blueT - blue)) > 10)
                {
                    boardXSum += j;
                    num2++;
                }
            }

            if (boardXSum != 0)
            {
                boardX = boardXSum / num2;
                Logger.d("boardX:" + boardX);
            }
        }

        //从上顶点往下 +274 的位置开始向下找颜色与上顶点一样的点，为下顶点
        //该方法对所有纯色平面和部分非纯色平面有效，对高尔夫草坪面、木纹桌面、药瓶和非菱形的碟机（好像是）会判断错误
        int lastPixel = pixels[width * lastI + boardX];
        int lastK = 0;
        for (int k = lastI; k <= lastI + 274; k++)
        {
            int red = Color.red(lastPixel);
            int green = Color.green(lastPixel);
            int blue = Color.blue(lastPixel);
            int temp = pixels[width * k + boardX];
            int redT = Color.red(temp);
            int greenT = Color.green(temp);
            int blueT = Color.blue(temp);
            if ((Math.abs(redT - red) + Math.abs(greenT - green) + Math.abs(blueT - blue)) < 10)
            {
                lastK = k;
                break;
            }
        }

        boardY = (lastI + lastK) / 2;
        Logger.d("boardY:" + boardY);

        //如果上一跳命中中间，则下个目标中心会出现 r245 g245 b245 的点，利用这个属性弥补上一段代码可能存在的判断错误
        //若上一跳由于某种原因没有跳到正中间，而下一跳恰好有无法正确识别花纹，则有可能游戏失败，由于花纹面积通常比较大，失败概率较低
        for (int l = lastI; l >= lastI - 200; l--)
        {
            int temp = pixels[width * l + pieceX];
            int red = Color.red(temp);
            int green = Color.green(temp);
            int blue = Color.blue(temp);
            if ((Math.abs(red - 245) + Math.abs(green - 245) + Math.abs(blue - 245)) == 0)
            {
                boardY = l + 10;
                Logger.d("boardY修正:" + boardY);
            }
        }
    }

    /**
     * 获取扫描开始的Y坐标
     * 棋子一般在屏幕的1/3至2/3区域
     * @param bitmap
     * @return
     */
    private int getScanStartY(Bitmap bitmap)
    {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = height * 2 / 3; i >= height / 3; i = i - 50)
        {
            int lastPixels = pixels[width * i];
            for (int j = 1; j < width; j++)
            {
                int temp = pixels[width *i + j];
                //不是纯色的线，就记录scanStartY的值，跳出循环
                if (temp != lastPixels)
                {
                    Logger.d("scanY:" + i);
                    return i;
                }
            }
        }
        return 0;
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVirtual();
    }
}
