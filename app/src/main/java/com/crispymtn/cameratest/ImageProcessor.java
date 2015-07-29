package com.crispymtn.cameratest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    public ImageProcessor(Context context) {
        this.context = context;
    }

    public Uri processImage(byte[] imageData) throws IOException {
        String fileBaseName = UUID.randomUUID().toString();

        // Write the image to disk (is already compressed and a JPEG)
        ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
        Bitmap image = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
//        image = resize(image, 1200, 1200);
        image.compress(Bitmap.CompressFormat.JPEG, 100, imageStream);
        Uri imageFileURI = writeFileFromData(imageStream, fileBaseName + ".jpg");

        return imageFileURI;
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
