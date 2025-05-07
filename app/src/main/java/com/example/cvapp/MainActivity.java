package com.example.cvapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

import okhttp3.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private Uri photoUri;
    private File photoFile;

    private ImageView imageView;
    private TextView resultText;
    private Button captureButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        captureButton = findViewById(R.id.captureButton);

        captureButton.setOnClickListener(v -> {
            try {
                dispatchTakePictureIntent();
            } catch (IOException e) {
                e.printStackTrace();
                resultText.setText("Error creating image file");
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

// Overwrite file with corrected image before upload
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
    }

    private void uploadImage(File file) {
        resultText.setText("Uploading...");

        // Field must be named "file" as per backend
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiService apiService = RetrofitClient.getApiService();
        Call<PredictionResponse> call = apiService.uploadImage(body);

        call.enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(Call<PredictionResponse> call, Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PredictionResponse result = response.body();

                    if ("success".equalsIgnoreCase(result.getStatus())) {
                        String building = result.getPredicted_class();
                        double confidence = result.getConfidence();
                        resultText.setText("🏢 " + building + "\n✅ Confidence: " + String.format("%.2f%%", confidence * 100));
                    } else {
                        resultText.setText("❌ Prediction failed.");
                    }
                } else {
                    resultText.setText("❌ Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<PredictionResponse> call, Throwable t) {
                resultText.setText("🚫 Network error: " + t.getMessage());
            }
        });

    }

}
