package com.crispymtn.cameratest;

/**
 * Created by josch on 29/07/15.
 */
import retrofit.Callback;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedString;

public interface S3Service {

    @Multipart
    @POST("/")
    void createPhoto(
            @Part("file") TypedFile imageData,
            Callback<Object> callback
    );
}
