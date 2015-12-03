package com.example.okulyk.cameracolordetector;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Camera mCamera;
    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    View mColorStub;
    TextView mColorText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mColorStub = findViewById(R.id.colorStub);
        mColorText = (TextView) findViewById(R.id.colorText);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            // open the camera
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
        } catch (RuntimeException e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
        Camera.Parameters param;
        param = mCamera.getParameters();
//
//        // modify parameter
        param.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(param);
        try {
            // The Surface has been created, now tell the camera where to draw
            // the preview.
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("surfaceChanged", "called");
        refreshCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    public void refreshCamera() {
        if (mSurfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception e) {

        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        int frameHeight = camera.getParameters().getPreviewSize().height;
        int frameWidth = camera.getParameters().getPreviewSize().width;
        int rgb[] = new int[frameWidth * frameHeight];
        int pixels[] = decodeYUV420SP(rgb, data, frameWidth, frameHeight);
        int centerPixel = pixels[frameWidth * frameHeight/2 + frameWidth/2];
        String strColor = String.format("#%06X", 0xFFFFFF & centerPixel);
        mColorStub.setBackgroundColor(centerPixel);
        mColorText.setText("Color: " + strColor);
    }

    public int[] decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

        // here we're using our own internal PImage attributes
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                // use interal buffer instead of pixels for UX reasons
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }

        return rgb;
    }

}
