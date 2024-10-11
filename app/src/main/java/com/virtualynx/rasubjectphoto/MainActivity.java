package com.virtualynx.rasubjectphoto;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    static final int pic_id = 123;
    static final String serverHost = "http://36.88.110.134:881/biometric";
    private AutoCompleteTextView autoTextPerson;
    private ImageView imagePhoto;
    private final HashMap<String, String> doctypeMaps = new HashMap<String, String>(){
        {
            put("TTD Pemanfaatan", "sign_util");
            put("TTD Notaris", "sign_notary");
            put("Pengambilan Sertifikat", "cert_acq");
            put("Dokumentasi", "documentation");
        }
    };
    private Spinner spinnerDoctype;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        autoTextPerson = (AutoCompleteTextView)findViewById(R.id.autotxt_persons);

        autoTextPerson.setOnClickListener(v -> {
            autoTextPerson.setText("");
        });

        OkHttpClient client = new OkHttpClient();
        Request personListRequest = new Request.Builder()
                .url(serverHost+"/api/person/list.php")
                .build();

        client.newCall(personListRequest).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();

                    List<String> persons = new ArrayList<>();
                    try {
                        JSONArray jsonArr = new JSONArray(responseData);
                        for (int a=0; a<jsonArr.length(); a++) {
                            JSONObject obj = jsonArr.getJSONObject(a);

                            String nik = obj.get("nik").toString();
                            String name = obj.get("name").toString();
                            persons.add(name+" | "+nik);
                        }

                        String[] personArr = persons.toArray(new String[0]);

                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
//                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, personArr);
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.row_list, personArr);
                                autoTextPerson.setThreshold(1);
                                autoTextPerson.setAdapter(adapter);

                                int a = 1;
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        List<String> doctypes = new ArrayList<String>();
        for(Map.Entry<String, String> entry : doctypeMaps.entrySet()) {
            doctypes.add(entry.getKey());
        }
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, doctypes);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.row_list, doctypes);
        spinnerDoctype = (Spinner)findViewById(R.id.spinner_doctype);
        spinnerDoctype.setAdapter(adapter);

        Button buttonPhoto = findViewById(R.id.button_take_photo);
        Button uploadPhoto = findViewById(R.id.button_upload);
        this.imagePhoto = findViewById(R.id.image_photo);

        buttonPhoto.setOnClickListener(v -> {
            String nik = getSelectedPersonNik();
            if(nik == null){
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Select a person first !!", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            // Create the camera_intent ACTION_IMAGE_CAPTURE it will open the camera for capture the image
            Intent camera_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Start the activity with camera_intent, and request pic id
//            startActivityForResult(camera_intent, pic_id);

            startCameraIntent.launch(camera_intent);
        });

        uploadPhoto.setOnClickListener(v -> {
            String nik = getSelectedPersonNik();
            if(nik == null){
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Select a person first !!", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
            if(imagePhoto.getDrawable() == null){
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "No photo to be uploaded !!", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
            ((BitmapDrawable)imagePhoto.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, imageStream); //bitmap is required image which have to send  in Bitmap form
            byte[] imageBytes = imageStream.toByteArray();
            String encodedImage = "data:image/jpeg;base64,"+Base64.encodeToString(imageBytes, Base64.DEFAULT);

            String photoType = spinnerDoctype.getSelectedItem().toString();
            String photoTypeId = doctypeMaps.get(photoType).toString();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
//                    .addFormDataPart(
//                            "file",
//                            file.getName(),
//                            RequestBody.create(MediaType.parse("image/jpeg"), file)
//                    )
                    .addFormDataPart("nik", nik)
                    .addFormDataPart("filename", photoTypeId+System.currentTimeMillis()+".jpeg")
                    .addFormDataPart("photo_type", photoTypeId)
                    .addFormDataPart("description", photoType)
                    .addFormDataPart("is_base64", "true")
                    .addFormDataPart("photo", encodedImage)
                    .build();

            Request uploadPhotoRequest = new Request.Builder()
                    .url(serverHost+"/api/person/upload_photo.php")
                    .post(requestBody)
                    .build();

            client.newCall(uploadPhotoRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if(response.isSuccessful()) {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show();
                                imagePhoto.setImageDrawable(null);
                            }
                        });
                    }else{
                        String responseBody = response.body()==null? "Failed to upload photo": response.body().string();

                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(MainActivity.this, responseBody, Toast.LENGTH_SHORT).show();
                                imagePhoto.setImageDrawable(null);
                            }
                        });
                    }
                }
            });
        });
    }

    private final ActivityResultLauncher<Intent> startCameraIntent = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                // Set the image in imageview for display
                imagePhoto.setImageBitmap(photo);
            }else{
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, result.getResultCode(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    );

    private String getSelectedPersonNik(){
        AutoCompleteTextView autoTextPerson = findViewById(R.id.autotxt_persons);
        String nameNik = autoTextPerson.getText().toString();
        if(nameNik.isEmpty()){
            return null;
        }

        String[] nameNikArr = nameNik.split("\\|");

        return nameNikArr[1].trim();
    }
}