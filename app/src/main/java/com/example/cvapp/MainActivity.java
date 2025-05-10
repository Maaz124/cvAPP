package com.example.cvapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private Uri photoUri;
    private File photoFile;

    private ImageView imageView;
    private TextView resultText;
    private Button captureButton;
    private Button uploadButton;
    private Button mapButton;

    private double currentLat = 0.0;
    private double currentLng = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        captureButton = findViewById(R.id.captureButton);
        uploadButton = findViewById(R.id.uploadButton);
        mapButton = findViewById(R.id.mapButton);
        mapButton.setVisibility(View.GONE); // Hide initially

        captureButton.setOnClickListener(v -> {
            try {
                dispatchTakePictureIntent();
            } catch (IOException e) {
                e.printStackTrace();
                resultText.setText("Error creating image file");
            }
        });

        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_IMAGE_PICK);
        });

        mapButton.setOnClickListener(v -> {
            String mapsUrl = "https://www.google.com/maps?q=" + currentLat + "," + currentLng;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl));
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                resultText.append("\n⚠️ Unable to open Google Maps.");

            }
        });
    }

    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile = File.createTempFile("photo_", ".jpg", getCacheDir());
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bitmap rotatedBitmap = ImageUtils.getCorrectlyOrientedBitmap(photoFile.getAbsolutePath());
            imageView.setImageBitmap(rotatedBitmap);
            try {
                java.io.FileOutputStream out = new java.io.FileOutputStream(photoFile);
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            uploadImage(photoFile);
        }

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    File file = FileUtils.uriToFile(this, selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    Bitmap rotated = ImageUtils.getCorrectlyOrientedBitmap(file.getAbsolutePath());
                    imageView.setImageBitmap(rotated);

                    java.io.FileOutputStream out = new java.io.FileOutputStream(file);
                    rotated.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();

                    uploadImage(file);

                } catch (IOException e) {
                    e.printStackTrace();
                    resultText.setText("Failed to load image");
                }
            }
        }
    }

    private void uploadImage(File file) {
        resultText.setText("⏳ Uploading for prediction...");

        RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiService apiService = RetrofitClient.getPredictionApi();

        Call<PredictionResponse> call = apiService.uploadImage(body);

        call.enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PredictionResponse result = response.body();
                    String building = result.getPredicted_class();
                    double confidence = result.getConfidence();
                    PredictionResponse.Coordinates coords = result.getCoordinates();

                    StringBuilder display = new StringBuilder();
                    display.append("🏢 ").append(building)
                            .append("\n✅ Confidence: ").append(String.format("%.2f%%", confidence));

                    if (coords != null) {
                        currentLat = coords.getLatitude();
                        currentLng = coords.getLongitude();
                        display.append("\n📍 Lat: ").append(currentLat)
                                .append(", Lng: ").append(currentLng);
                        mapButton.setVisibility(View.VISIBLE);
                    } else {
                        display.append("\n📍 Coordinates not available");
                        mapButton.setVisibility(View.GONE);
                    }

                    resultText.setText(display.toString());

                    // Trigger distance estimation
                    estimateDistance(file);

                } else {
                    resultText.setText("❌ Prediction API error: " + response.code());
                    mapButton.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                resultText.setText("🚫 Prediction API failed: " + t.getMessage());
                mapButton.setVisibility(View.GONE);
            }
        });
    }

    private void estimateDistance(File file) {
        ApiService apiService = RetrofitClient.getDistanceApi();

        RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        Call<DistanceResponse> call = apiService.estimateDistance(body);
        call.enqueue(new Callback<DistanceResponse>() {
            @Override
            public void onResponse(Call<DistanceResponse> call, Response<DistanceResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    double distance = response.body().getDistance_meters();
                    resultText.append("\n📏 Approx. Distance: " + String.format("%.2f m", distance));
                } else {
                    resultText.append("\n❌ Distance API error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<DistanceResponse> call, Throwable t) {
                resultText.append("\n🚫 Distance API failed: " + t.getMessage());
            }
        });
    }

}