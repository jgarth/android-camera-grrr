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

    private class Size {
        int width;
        int height;
        float scaleRatio;

        public Size() {
        }

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public float getScaleRatio() {
            return scaleRatio;
        }

        public void setScaleRatio(float scaleRatio) {
            this.scaleRatio = scaleRatio;
        }
    }

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public Uri processImage(byte[] imageData) throws IOException {
        String fileBaseName = UUID.randomUUID().toString();

        // Write the image to disk (is already compressed and a JPEG)
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        image = rotateBitmap(image, imageData);
//        image = resize(image, 1200, 1200);
//        image = resize(image, 1200, 1200);
        image.compress(Bitmap.CompressFormat.JPEG, 100, imageStream);
        Uri imageFileURI = writeFileFromData(imageStream, fileBaseName + ".jpg");

        return imageFileURI;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    private Bitmap rotateBitmap(Bitmap source, byte[] data) {
        int exifOffset = Exif.getOrientation(data);
        int rotation = this.rotation;
        int rotate = 0;

        if ((rotation < (270 + 45) && rotation > (270 - 45))) {
            Log.e("BLIMP", "270 degrees ...");
            rotate = -90;
        } else if (rotation < (90 + 45) && rotation > (90 - 45)) {
            Log.e("BLIMP", "90 degrees ...");
            rotate = 90;
        }
        rotate += exifOffset;
        if (rotate != 0) {
            Matrix matrix = new Matrix();
            Size size = resize(source.getWidth(), source.getHeight(), 1200, 1200);
            matrix.setScale(size.getScaleRatio(), size.getScaleRatio());
            matrix.postRotate(rotate, size.getWidth() / 2, size.getHeight() / 2);
            return Bitmap.createBitmap(source, 0, 0, size.getWidth(), size.getHeight(), matrix, false);
        } else {
            return source;
        }
    }

    private Size resize(int width, int height, int maxWidth, int maxHeight) {
        Size size = new Size();
        if (maxHeight > 0 && maxWidth > 0) {
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float)maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float)maxWidth / ratioBitmap);
            }
            Log.e("BLIMP", "Scale " + finalWidth + "," + finalHeight);
            size.setWidth(finalWidth);
            size.setHeight(finalHeight);

            int maxEdgeLength = 1200;
            float scaleRatio = (float) 1.0;

            size.setScaleRatio(scaleRatio);
// If image is smaller than target, don’t scale
            if(width <= maxEdgeLength && height <= maxEdgeLength) {
                return size;
            }

// Otherwise, compute scaling on longer edge
            if(width >= height) {
                scaleRatio = maxEdgeLength/width;
            } else {
                scaleRatio = maxEdgeLength/height;
            }
            size.setScaleRatio(scaleRatio);

            return size;
        }
        return null;
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
