package xyz.a4tay.pictospeech;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_TAKE_PHOTO = 5;
    private static final int REQUEST_DOWNLOAD_AUDIO = 10;
    private ImageView mImageView;
    private String mCurrentPhotoPath;
    private String mp3FilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button takePhotoButton = findViewById(R.id.takePhoto);
        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });


        Button showPhotoButton = findViewById(R.id.showPhoto);
        showPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPhotoPath != null && !mCurrentPhotoPath.equals("")) {
                    setPic();
                    Intent intent = new Intent(getBaseContext(), PutImageFile.class);
                    intent.putExtra("UPLOAD_FILE", mCurrentPhotoPath);
                    startActivityForResult(intent, REQUEST_DOWNLOAD_AUDIO);
                } else {
                    Toast.makeText(getBaseContext(), "You must take a photo first", Toast.LENGTH_LONG).show();
                }
            }
        });
        Button playAudioButton = findViewById(R.id.playAudio);
        playAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mp3FilePath != null && !mp3FilePath.equals("")) {
                    playAudio();
                } else {
                    Toast.makeText(getBaseContext(),
                            "you must take and upload a photo first", Toast.LENGTH_LONG).show();
                }
            }
        });
        mImageView = findViewById(R.id.photoView);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentPhotoPath != null && !mCurrentPhotoPath.equals("")) {
                    File file = new File(mCurrentPhotoPath);
                    final Intent intent = new Intent(Intent.ACTION_VIEW)//
                            .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                                            android.support.v4.content.FileProvider.getUriForFile(getBaseContext(),
                                                    "com.example.android.fileprovider", file) : Uri.fromFile(file),
                                    "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    Toast.makeText(getBaseContext(), "no photo to view....", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
            galleryAddPic();
            setPic();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            System.out.println("data: " + data.toString());
            try {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                mImageView.setImageBitmap(imageBitmap);
            } catch (NullPointerException e) {
                System.out.println("Error... " + e.getMessage());
            }
        }
        if (requestCode == REQUEST_DOWNLOAD_AUDIO && resultCode == RESULT_OK) {
            System.out.println("data: " + data.toString());
            try {
                Bundle extras = data.getExtras();
                mp3FilePath = extras.getString("downloadPath");
                playAudio();
            } catch (NullPointerException e) {
                System.out.println("Error... " + e.getMessage());
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(bitmap);
    }

    private void playAudio() {
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(mp3FilePath);
            mp.prepare();
            mp.start();

        } catch (IOException ioException) {
            System.out.println("Error with audio... " + ioException.getMessage());
        }
    }
}
