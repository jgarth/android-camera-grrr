package com.crispymtn.cameratest;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.Image;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.bozho.easycamera.DefaultEasyCamera;
import net.bozho.easycamera.EasyCamera;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;


public class MainActivity extends Activity implements EasyCamera.PictureCallback {

    public static final String TAG = MainActivity.class.getSimpleName();
    private ImageProcessor imageProcessor;
    private EasyCamera camera;

    public void setCameraActions(EasyCamera.CameraActions cameraActions) {
        this.cameraActions = cameraActions;
    }

    private EasyCamera.CameraActions cameraActions;
    private CameraPreview cameraPreview;
    private TextView orientationLabel;
    private OrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        orientationLabel = (TextView)findViewById(R.id.orientationLabel);
        cameraPreview = (CameraPreview)findViewById(R.id.cameraPreview);

        camera = DefaultEasyCamera.open();
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("orientation", "portrait");
        parameters.setRotation(90);
        camera.setParameters(parameters);
        cameraPreview.setCameraActivity(this);
        cameraPreview.setCamera(camera);

        setOrientationLabelText("Not initialized.");

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            public void onOrientationChanged(int orientation) {
                String orientationString = String.valueOf(orientation);
                setOrientationLabelText(orientationString);
            }
        };

        imageProcessor = new ImageProcessor(getApplication());
    }

    public void takePicture(View view) {
        EasyCamera.Callbacks callbackChain = EasyCamera.Callbacks.create()
                .withJpegCallback(this)
                .withRestartPreviewAfterCallbacks(true);

        cameraActions.takePicture(callbackChain);
    }

    @Override
    public void onPictureTaken(byte[] data, EasyCamera.CameraActions actions) {
        Log.e(TAG, "Picture taken");

        try {
            Uri imageFileURI = imageProcessor.processImage(data);
            uploadPhoto(imageFileURI);

        } catch (IOException e) {
            Log.e(TAG, e.getMessage());

        }
    }

    private void setOrientationLabelText(String text) {
        orientationLabel.setText("O: " + text);
    }

    @Override
    public void onResume(){
        super.onResume();
        orientationEventListener.enable();
    }

    @Override
    public void onPause(){
        super.onPause();
        orientationEventListener.disable();
        camera.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.close();
    }

    public void uploadPhoto(Uri imageUri) {
        final Context context = this;
        String endpoint = "http://fake-s3-crispy.fwd.wf/";

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .build();

        S3Service s3Service = restAdapter.create(S3Service.class);

        File imageFile = new File(imageUri.getPath());

        Log.e(TAG, "Uploading photo... ");

        s3Service.createPhoto(new TypedFile("image/jpeg", imageFile),
                new Callback<Object>() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.e(TAG, "Successfully uploaded photo!");
                        Toast.makeText(context, "Photo uploaded…", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e(TAG, "Failed to upload photo: " + error.getMessage());
                        Toast.makeText(context, "Error uploading photo…", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
