package com.example.simpleimageeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int GALLERY_INTENT_REQUEST_CODE = 1, CAMERA_INTENT_REQUEST_CODE = 2,  GALLERY_PERMISSION_REQUEST_CODE = 1, CAMERA_PERMISSION_REQUEST_CODE = 2;

    ImageView imageViewMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewMain = findViewById(R.id.imageViewMain);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == GALLERY_INTENT_REQUEST_CODE && data != null)
        {
            Log.i("IMAGE_SELECTION_STATUS", "Success!");
            Uri selectedImgUri = data.getData();
            //Log.i("SELECTED_IMAGE_URI", "Uri is: " + selectedImgUri);
            try {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImgUri);
                //Log.i("BITMAP_HEIGHT", "Height is: " + bmp.getHeight());

                Intent editIntent = new Intent(this, EditActivity.class);
                editIntent.putExtra("URI_KEY_IN_STRING", selectedImgUri.toString());
                startActivityForResult(editIntent, 10);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(resultCode == RESULT_OK && requestCode == CAMERA_INTENT_REQUEST_CODE && data != null)
        {
            Log.i("CAMERA_CLICK_STATUS", "Success!");

            Bundle extras = data.getExtras();

            Intent editIntent = new Intent(this, EditActivity.class);
            editIntent.putExtra("PHOTO_BUNDLE_KEY", extras);
            startActivityForResult(editIntent, 20);
        }

        if(requestCode == 10 || requestCode == 20) {
            Log.i("RECEIVED_SAVED_PHOTO", "Success!");
            Bitmap bmp = null;
            try {
                bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(data.getStringExtra("SAVED_PHOTO_URI")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            imageViewMain.setImageBitmap(bmp);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == GALLERY_PERMISSION_REQUEST_CODE)
        {
            Log.i("PERMISSION_RESULT", "Gallery Permission Granted Now!");
            Toast.makeText(getApplicationContext(), "Gallery Permission Granted!\nPlease choose your photo!", Toast.LENGTH_SHORT).show();
        }

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == CAMERA_PERMISSION_REQUEST_CODE)
        {
            Log.i("PERMISSION_RESULT", "Camera Permission Granted Now!");
            Toast.makeText(getApplicationContext(), "Camera Permission Granted!\nPlease click your photo!", Toast.LENGTH_SHORT).show();
        }
    }

    public void open(MenuItem item)
    {
        //Log.i("MENU_ITEM_ONCLICK", "Item Clicked: " + item.getTitle() + "\n" + item.getItemId());

        if(item.getItemId() == R.id.navigation_gallery)     //OPENING GALLERY
        {
            Log.i("MENU_OPTION_SELECTED", "Gallery option selected!");
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("PERMISSION_CHECK", "Gallery Permission Not granted yet!");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_REQUEST_CODE);
            }
            else
            {
                Log.i("PERMISSION_CHECK", "Gallery Permission Granted Already!");
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, GALLERY_INTENT_REQUEST_CODE);
            }
        }
        else    //OPENING CAMERA
        {
            Log.i("MENU_OPTION_SELECTED", "Camera option selected!");
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("PERMISSION_CHECK", "Camera Permission Not granted yet!");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            }
            else
            {
                Log.i("PERMISSION_CHECK", "Camera Permission Granted Already!");
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_INTENT_REQUEST_CODE);
            }
        }
    }
}