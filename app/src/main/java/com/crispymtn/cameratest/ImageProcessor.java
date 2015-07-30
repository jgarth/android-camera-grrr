package com.crispymtn.cameratest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

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

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public Uri processImage(byte[] imageData) throws IOException {
        String fileBaseName = UUID.randomUUID().toString();

        // Write the image to disk (is already compressed and a JPEG)
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        image = rotateBitmap(image);
//        image = resize(image, 1200, 1200);
        image.compress(Bitmap.CompressFormat.JPEG, 100, imageStream);
        Uri imageFileURI = writeFileFromData(imageStream, fileBaseName + ".jpg");

        return imageFileURI;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    private Bitmap rotateBitmap(Bitmap source) {
        Matrix matrix = null;

        if (rotation < (270 + 45) && rotation > (270 - 45)) {
            matrix = new Matrix();
            matrix.setRotate(-90, source.getWidth() / 2, source.getHeight() / 2);
        } else if (rotation < (90 + 45) && rotation > (90 - 45)) {
            matrix = new Matrix();
            matrix.setRotate(90, source.getWidth() / 2, source.getHeight() / 2);
        }
        if (matrix != null) {
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, false);
        } else {
            return source;
        }
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
