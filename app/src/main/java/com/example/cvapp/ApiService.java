package com.example.cvapp;

import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.MultipartBody;

public interface ApiService {
    @Multipart
    @POST("predict")
    Call<PredictionResponse> uploadImage(
            @Part MultipartBody.Part file // 🔥 Must be named "file"
    );
}
