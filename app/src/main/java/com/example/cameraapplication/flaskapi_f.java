package com.example.cameraapplication;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

interface flaskapi_f {

    @Multipart
    @POST("predict")
    Call<ResponseBody> imageUpload(@Part MultipartBody.Part img);
}
