package com.example.cvapp;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL_PREDICT = "http://159.223.36.120:8080/";
    private static final String BASE_URL_DISTANCE = "http://159.223.36.120:8000/";

    private static Retrofit retrofitPredict;
    private static Retrofit retrofitDistance;

    public static ApiService getPredictionApi() {
        if (retrofitPredict == null) {
            retrofitPredict = new Retrofit.Builder()
                    .baseUrl(BASE_URL_PREDICT)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitPredict.create(ApiService.class);
    }

    public static ApiService getDistanceApi() {
        if (retrofitDistance == null) {
            retrofitDistance = new Retrofit.Builder()
                    .baseUrl(BASE_URL_DISTANCE)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofitDistance.create(ApiService.class);
    }
}
