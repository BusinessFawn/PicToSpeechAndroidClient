package xyz.a4tay.pictospeech;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.*;
import okhttp3.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class PutImageFile extends AppCompatActivity {

    private String uploadBucket = "gs://upload-bucket-pic-to-speech-4tay";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();
    private String functionURL = "https://us-east1-pictotext.cloudfunctions.net/pic-to-text ";
    private String mp3Path = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_put_image);
        // File or Blob
        String fileName = getIntent().getStringExtra("UPLOAD_FILE");
        Uri file = Uri.fromFile(new File(fileName));
        FirebaseStorage storage = FirebaseStorage.getInstance(uploadBucket);
        StorageReference storageRef = storage.getReference();

        // Create the file metadata
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();

        beginUploadTask(storageRef, file, metadata);
    }


    private void beginUploadTask(StorageReference storageRef, Uri file, StorageMetadata metadata) {
        // Upload file and metadata to the path 'images/mountains.jpg'
        UploadTask uploadTask = storageRef.child("" + file.getLastPathSegment()).putFile(file, metadata);

        final String fileName = file.getLastPathSegment();


        // Listen for state changes, errors, and completion of the upload.
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                System.out.println("Upload is " + progress + "% done");
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Toast.makeText(getBaseContext(), "Upload failed...", Toast.LENGTH_LONG).show();
                finish();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Handle successful uploads on complete
                System.out.println("finished upload, closing activity.");
                Toast.makeText(getBaseContext(), "Upload succeeded, retrieving audio", Toast.LENGTH_LONG).show();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("uri", fileName);
                    post(jsonObject.toString(), new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Toast.makeText(getBaseContext(), "Download failed...", Toast.LENGTH_LONG).show();
                            finish();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                System.out.println("Success??? " + response.code());
                                try {
                                    JSONObject responseJson = new JSONObject(response.body().string());
                                    mp3Path = responseJson.optString("uri");
                                    System.out.println("mp3Path: " + mp3Path);
                                    downloadAudio();
                                } catch (JSONException eJSON) {
                                    System.out.println("JSON Error from response: " + eJSON.getMessage());
                                }
                            } else {
                                System.out.println("Fail??? " + response.code());
                                finish();
                            }
                        }
                    });
                } catch (IOException ioException) {
                    System.out.println("IO Error.... " + ioException.getMessage());
                } catch (JSONException jsonException) {
                    System.out.println("JSON Error.... " + jsonException.getMessage());
                }
            }
        });
    }

    private Call post(String json, Callback callback) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(functionURL)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    private void downloadAudio() {

        String[] downloadPath = mp3Path.split("/");

        FirebaseStorage storage = FirebaseStorage.getInstance("gs://" + downloadPath[0]);

        StorageReference storageRef = storage.getReference(downloadPath[1]);

        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String audioFileName = "mp3_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            final File audio = File.createTempFile(
                    audioFileName,  /* prefix */
                    ".mp3",         /* suffix */
                    storageDir      /* directory */
            );
            storageRef.getFile(audio).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    System.out.println("Audio file downloaded! " + audio.getAbsolutePath());
                    Intent data = new Intent();
                    data.putExtra("downloadPath", audio.getAbsolutePath());
                    setResult(RESULT_OK, data);
                    finish();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    finish();
                    System.out.println("Failed download of audio file....");
                }
            });
        } catch (IOException e) {
            System.out.println("IO Error.... " + e.getMessage());
        }
    }
}
