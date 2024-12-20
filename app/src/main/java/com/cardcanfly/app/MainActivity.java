package com.cardcanfly.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.File;


public class MainActivity extends AppCompatActivity {
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;
    private Uri cameraImageUri;

    WebView myWeb;

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

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }

        myWeb = findViewById(R.id.myWeb);
        WebSettings webSettings = myWeb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Enable caching (HTTP cache)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT); // Default caching behavior, use the cache when available
        webSettings.setDatabaseEnabled(true); // Enable database for local storage
        webSettings.setDomStorageEnabled(true); // Enable DOM storage for HTML5 web apps

        myWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // Prevents WebView reload on navigation
            }
        });

        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                MainActivity.this.filePathCallback = filePathCallback;

                // Create a file for the camera image
                File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "camera_image.jpg");
                cameraImageUri = FileProvider.getUriForFile(MainActivity.this, getApplicationContext().getPackageName() + ".fileprovider", imageFile);

                // Grant URI permissions to the camera app
                grantUriPermission("com.android.camera", cameraImageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Create camera intent
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

                // Create file chooser intent
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                // Create chooser intent
                Intent chooserIntent = Intent.createChooser(contentSelectionIntent, "Image Chooser");

                // Add camera intent to chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

                startActivityForResult(chooserIntent, FILE_REQUEST_CODE);
                return true;
            }
        });

        // Hide the system navigation bar
        hideNavigationBar();

        if (savedInstanceState != null) {
            myWeb.restoreState(savedInstanceState);
        } else {
            myWeb.loadUrl("https://cardcanfly.com/");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Log.e("Permissions", "Required permissions not granted!");
            }
        }
    }

    @SuppressWarnings("deprecation")
    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null); // Replace managedQuery with getContentResolver().query()
        if (cursor != null && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String realPath = cursor.getString(column_index);
            cursor.close();
            return realPath;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == FILE_REQUEST_CODE && filePathCallback != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK) {
                if (intent == null || intent.getData() == null) {
                    // If there is not data, then we may have taken a photo
                    results = new Uri[]{cameraImageUri};
                } else {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        myWeb.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        myWeb.restoreState(savedInstanceState);
    }

    // Hide the system navigation bar (Home, Back, Overview buttons)
    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY); // Immersive mode
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideNavigationBar(); // Keep the navigation bar hidden when the app is resumed
    }
}
