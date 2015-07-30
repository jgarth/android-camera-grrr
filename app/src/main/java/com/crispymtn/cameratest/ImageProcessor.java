package com.crispymtn.cameratest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by josch on 29/07/15.
 */
public class ImageProcessor {

    private Context context;
    private int rotation;
    public static final float MAX_EDGE_LENGTH = 1200.0f;


    public ImageProcessor(Context context) {
        this.context = context;
    }

    public Uri processImage(byte[] imageData) throws IOException {
        String fileBaseName = UUID.randomUUID().toString();

        // Write the image to disk (is already compressed and a JPEG)
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inDither = false;
        // TODO: Save memory by calculating sample size
//        options.inSampleSize = inSampleSize;
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
        Bitmap rotatedImage = rotateBitmap(image, imageData);
        image.recycle();
        image = null;

        rotatedImage.compress(Bitmap.CompressFormat.JPEG, 70, imageStream);
        rotatedImage.recycle();
        rotatedImage = null;

        Uri imageFileURI = writeFileFromData(imageStream, fileBaseName + ".jpg");

        return imageFileURI;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    private Bitmap rotateBitmap(Bitmap source, byte[] data) {
        int exifRotation = Exif.getOrientation(data); // Rotation data from EXIF
        int deviceRotation = this.rotation; // Rotation data from device orientation

        int imageRotation = 0; // Degrees to rotate source image by

        // Get initial rotation from device
        if (deviceRotation < (270 + 45) && deviceRotation > (270 - 45)) {
            Log.e("BLIMP", "270 degrees ...");
            imageRotation = -90;
        } else if (deviceRotation < (90 + 45) && deviceRotation > (90 - 45)) {
            Log.e("BLIMP", "90 degrees ...");
            imageRotation = 90;
        }

        // Add rotation according to EXIF data
        imageRotation += exifRotation;

        Matrix matrix = new Matrix();

        // Get scale factor for maximum edge length of MAX_EDGE_LENGTH
        float scaleFactor = getScaleFactor(source.getWidth(), source.getHeight());
        int newWidth = (int)(source.getWidth() * scaleFactor);
        int newHeight = (int)(source.getHeight() * scaleFactor);

        Log.e("BLIMP", "Scaling by " + scaleFactor + " new size: " + newWidth + "x" + newHeight);

        matrix.setScale(scaleFactor, scaleFactor);

        if (imageRotation != 0) {
            matrix.postRotate(imageRotation, newWidth / 2, newHeight / 2);
        }

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private float getScaleFactor(int width, int height) {
        float scaleRatio = (float) 1.0;


        // If image is smaller than target, don’t scale
        if(width <= MAX_EDGE_LENGTH && height <= MAX_EDGE_LENGTH) {
            return scaleRatio;
        }

        // Otherwise, compute scaling on longer edge
        if(width >= height) {
            scaleRatio = MAX_EDGE_LENGTH/(float)width;
        } else {
            scaleRatio = MAX_EDGE_LENGTH/(float)height;
        }

        return scaleRatio;
    }

    // TODO: Query free space before attempting a write
    private Uri writeFileFromData(ByteArrayOutputStream byteStream, String fileName) throws IOException {
        File tempFile = new File(context.getCacheDir(), fileName);

        if (!tempFile.createNewFile()) {
            // Abort
            throw new IOException("Could not create file!");
        }

        FileOutputStream fileStream = new FileOutputStream(tempFile);
        byteStream.writeTo(fileStream);
        byteStream.close();
        fileStream.flush();
        fileStream.close();

        return Uri.fromFile(tempFile);
    }
}
