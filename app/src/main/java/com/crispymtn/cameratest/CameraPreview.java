package com.crispymtn.cameratest;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.crispymtn.cameratest.MainActivity;
import net.bozho.easycamera.EasyCamera;

import java.io.IOException;
import java.util.List;


/**
 * Created by josch on 18/07/15.
 *
 * TAKEN STRAIGHT FROM THE ANDROID-16 SDK SAMPLES
 */
@SuppressWarnings("deprecation")
/**
* A simple wrapper around a Camera and a SurfaceView that renders a centered preview of the Camera
* to the surface. We need to center the SurfaceView because not all devices have cameras that
* support preview sizes at the same aspect ratio as the device's display.
*/
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {
    private final String TAG = "Preview";

    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    Camera.Size mPreviewSize;
    List<Camera.Size> mSupportedPreviewSizes;
    EasyCamera mCamera;
    MainActivity cameraActivity;

    boolean isOrientationPortrait = true;

    public void setCameraActivity(MainActivity cameraActivity) {
        this.cameraActivity = cameraActivity;
    }

    public CameraPreview(Context context) {
        super(context);
        initializeAttributes(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeAttributes(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeAttributes(context);
    }

    private void initializeAttributes(Context context) {

        int orientation = getResources().getConfiguration().orientation;

        if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
            isOrientationPortrait = true;
        } else {
            isOrientationPortrait = false;
        }

        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(EasyCamera camera) {
        if (mCamera != null) {
            mCamera.stopPreview();
        }

        mCamera = camera;

        if (mCamera != null) {
            mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            Log.e(TAG, "SUPPORTED PREVIEW SIZES: " + mSupportedPreviewSizes);

            requestLayout();

            switchCamera(camera);
        }
    }

    public void switchCamera(EasyCamera camera) {

        try {

            Log.e(TAG, "Setting display orientation to 90 degrees");
            camera.setDisplayOrientation(90);
            this.cameraActivity.setCameraActions(camera.startPreview(mHolder));

        } catch (IOException exception) {
            Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
        }

        if(mPreviewSize != null) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            camera.setParameters(parameters);
        }

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // We purposely disregard child measurements because act as a
        // wrapper to a SurfaceView that centers the camera preview instead
        // of stretching it.
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(width, height);

        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            int width = r - l;
            int height = b - t;

            int previewWidth = width;
            int previewHeight = height;

            if (mPreviewSize != null && isOrientationPortrait) {
                previewWidth = mPreviewSize.height;
                previewHeight = mPreviewSize.width;

            } else if(mPreviewSize != null && !isOrientationPortrait) {
                previewWidth = mPreviewSize.width;
                previewHeight = mPreviewSize.height;
            }

            Log.e(TAG, "Preview Size: " + previewWidth + "x" + previewHeight);
            Log.e(TAG, "Screen Size: " + width + "x" + height);

            int previewLeft = 0;
            int previewTop = 0;
            int previewRight = 0;
            int previewBottom = 0;

            final float scaleFactor;
            final int finalHeight;
            final int finalWidth;

            if(height >= width) {
                scaleFactor = (float) height / (float) previewHeight;
                Log.e(TAG, "1st case – scaleFactor: " + scaleFactor);

                finalHeight = (int) (previewHeight * scaleFactor);
                finalWidth = (int) (previewWidth * scaleFactor);

                previewLeft = (width - finalWidth) / 2;
                previewRight = previewLeft + finalWidth;
                previewTop = 0;
                previewBottom = finalHeight;

            } else {
                scaleFactor = (float) width / (float) previewWidth;
                Log.e(TAG, "2nd case – scaleFactor: " + scaleFactor);

                finalHeight = (int) (previewHeight * scaleFactor);
                finalWidth = (int) (previewWidth * scaleFactor);

                previewLeft = 0;
                previewRight = finalWidth;
                previewTop = (height - finalHeight) / 2;
                previewBottom = previewTop + finalHeight;

            }

            Log.e(TAG, "final size: " + finalWidth + "x" + finalHeight);
            Log.e(TAG, "SurfaceView Origin: (" + previewLeft + "," + previewTop + ") Size: (" + (previewRight - previewLeft) + "x" + (previewBottom - previewTop) + ")");

            child.layout(previewLeft, previewTop, previewRight, previewBottom);
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            if (mCamera != null) {
                mCamera.setDisplayOrientation(90);
                this.cameraActivity.setCameraActions(mCamera.startPreview(holder));
            }

        } catch (IOException exception) {
            Log.e(TAG, "IOException caused in surfaceCreated()", exception);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Now that the size is known, set up the camera parameters and begin
        // the preview.
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        requestLayout();

        mCamera.setParameters(parameters);
        mCamera.stopPreview();
        try {
            mCamera.setDisplayOrientation(90);
            this.cameraActivity.setCameraActions(mCamera.startPreview(mHolder));

        } catch (IOException exception) {
            Log.e(TAG, "IOException caused in surfaceChanged()", exception);
        }

        Log.e(TAG, "Surface changed to size: " + w + "x" + h);
    }

}
