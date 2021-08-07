package com.example.simpleimageeditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.ui.AppBarConfiguration;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class EditActivity extends AppCompatActivity {

    Bitmap rotatedBitmap, bitmap;   //rotatedBitmap - denotes 'Gallery'       bitmap - denotes 'Camera'
    ImageView imageView;

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1, PIC_CROP = 2;
    private static boolean redirectedFromGallery = true;
    private int currentShowingIndex = 0;
    private ArrayList<Bitmap> bitmapsForUndo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        setTitle("Edit Image");

        BottomNavigationView navView = findViewById(R.id.nav_view_edit);
        navView.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);

        bitmapsForUndo = new ArrayList<Bitmap>();
        imageView = findViewById(R.id.imageView);
        Log.i("INTENT_STATUS", "Coming from Gallery: " + redirectedFromGallery);

        Intent intent = getIntent();
        if(intent.hasExtra("URI_KEY_IN_STRING"))    //Receiving photo from Gallery
        {
            redirectedFromGallery = true;

            String uriString = intent.getStringExtra("URI_KEY_IN_STRING");
            Uri imageUri = Uri.parse(uriString);

            String picturePath = getPath( getApplicationContext(), imageUri );
            Log.i("PICTURE_PATH", picturePath);
            //I/PICTURE_PATH: /storage/6430-3737/Wallpaper/HDWallpaper/60446_23.%20thumb-1920-36519.jpg

            try
            {
                Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                ExifInterface ei = new ExifInterface(picturePath);
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                Log.i("ORIENTATION", "is: " + orientation);

                rotatedBitmap = null;
                switch(orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        Log.i("ORIENTATION", "Rotated 90!");
                        rotatedBitmap = rotateImage(bmp, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        Log.i("ORIENTATION", "Rotated 180!");
                        rotatedBitmap = rotateImage(bmp, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        Log.i("ORIENTATION", "Rotated 270!");
                        rotatedBitmap = rotateImage(bmp, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                        Log.i("ORIENTATION", "Not Rotated!");
                    default:
                        rotatedBitmap = bmp;
                }
                imageView.setImageBitmap(rotatedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else    //Receiving photo from camera
        {
            Log.i("REDIRECT_FROM_CAMERA", "Photo Clicked through Camera.");
            redirectedFromGallery = false;

            Bundle photoBundle = intent.getBundleExtra("PHOTO_BUNDLE_KEY");
            bitmap = photoBundle.getParcelable("data");     //HAVE TO IMPROVE CODE FOR GETTING THE PROPER QUALITY
            imageView.setImageBitmap(bitmap);
            Log.i("PHOTO_HEIGHT_WIDTH", "is: " + bitmap.getHeight() + "\t" + bitmap.getWidth());
        }
        addToUndoList();
    }

    public void menuItemSelected(MenuItem menuItem)
    {
        switch (menuItem.getItemId())
        {
            case R.id.navigation_undo:          //UNDO
                Log.i("MENU_ITEM_CLICKED", "Undo menu item clicked!");

                onUndoPressed();
                break;

            case R.id.navigation_crop:          //CROP
                Log.i("MENU_ITEM_CLICKED", "Crop menu item clicked!");

                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    Log.i("PERMISSION_CHECK", "Storage Permission NOT granted yet!");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                }
                else
                {
                    performCrop();
                    addToUndoList();
                }
                break;

            case R.id.navigation_rotate:        //ROTATE
                Log.i("MENU_ITEM_CLICKED", "Rotate menu item clicked!");
                if(redirectedFromGallery)
                {
                    Log.i("ROTATE", "Rotating Gallery pic.");
                    rotatedBitmap = rotateImage(rotatedBitmap, 90);
                    addToUndoList();
                    imageView.setImageBitmap(rotatedBitmap);
                }
                else
                {
                    Log.i("ROTATE", "Rotating Camera pic.");
                    bitmap = rotateImage(bitmap, 90);
                    addToUndoList();
                    imageView.setImageBitmap(bitmap);
                }
                break;

            case R.id.navigation_save:          //SAVE
                Log.i("MENU_ITEM_CLICKED", "Save menu item clicked!");

                if(ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                {
                    Log.i("PERMISSION_CHECK", "Storage Permission NOT granted yet!");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                }
                else
                {
                    Log.i("PERMISSION_CHECK", "Storage Permission already granted!");
                    //PROCEED TO SAVE PHOTO

                    String path = Environment.getExternalStorageDirectory().toString();
                    Log.i("PATH", "is: " + path);
                    OutputStream fOut = null;
                    File file = new File(path + "/Simple Image Editor");
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    File photoFile = new File(file, timeStamp + ".jpeg");
                    try
                    {
                        fOut = new FileOutputStream(photoFile);

                        if(redirectedFromGallery) {
                            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        }
                        else {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        }
                        fOut.flush();
                        fOut.close();

                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(Uri.fromFile(photoFile));
                        this.sendBroadcast(mediaScanIntent);

                        Log.i("SAVE", "After broadcast.");
                        Toast.makeText(getApplicationContext(), "Photo saved succesfully!", Toast.LENGTH_LONG).show();

                        //NOW HAVE TO FINISH THIS ACTIVITY & SHOW THE FINAL EDITED PHOTO IN THE MAIN ACTIVITY.

                        Intent objIntent = new Intent();
                        objIntent.putExtra("SAVED_PHOTO_URI", Uri.fromFile(photoFile).toString());
                        setResult(2, objIntent);
                        finish();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == STORAGE_PERMISSION_REQUEST_CODE)
        {
            Log.i("PERMISSION_CHECK", "Storage Permission granted now!");
            Toast.makeText(getApplicationContext(), "Storage Permission Granted!\nPlease proceed to Process/Save your photo!", Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public static String getPath(Context context, Uri uri ) {
        String result = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = context.getContentResolver( ).query( uri, proj, null, null, null );
        if(cursor != null){
            if ( cursor.moveToFirst( ) ) {
                int column_index = cursor.getColumnIndexOrThrow( proj[0] );
                result = cursor.getString( column_index );
            }
            cursor.close( );
        }
        if(result == null) {
            result = "Not found";
        }
        return result;
    }

    /*private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        //have to replace this directory with getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  *//* prefix *//*
                ".jpg",         *//* suffix *//*
                storageDir      *//* directory *//*
        );

        // Save a file: path for use with ACTION_VIEW intents
        String currentPhotoPath = image.getAbsolutePath();
        return image;
    }*/

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /*public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    *//** Create a file Uri for saving an image or video *//*
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    *//** Create a File for saving an image or video *//*
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists())
        {
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }*/

    public Uri getImageUriFromBitmap()
    {
        String tempDirStr = Environment.getExternalStorageDirectory().toString();
        File tempDir = new File(tempDirStr + "/.temp");
        //have to delete intermediate files at this location after saving the final edited photo.
        Log.i("ABSOLUTE_PATH", "tempDir.getAbsolutePath() : " + tempDir.getAbsolutePath());
        if(!tempDir.exists())
        {
            Log.i("TEMP_DIR_STATUS", "existence?: " + tempDir.exists());
            tempDir.mkdirs();
        }
        Log.i("TEMP_DIR_STATUS", "existence?: " + tempDir.exists());
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        File tempFile = null;
        //write the bytes in file
        try
        {
            tempFile = File.createTempFile("Temp-" + timeStamp, ".jpg", tempDir);
            Log.i("TEMP_FILE", "Created!");
            FileOutputStream fos = new FileOutputStream(tempFile);
            if(redirectedFromGallery) {
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
            else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            }
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("URI_TEMP_FILE", "is: " + Uri.fromFile(tempFile));
        return FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", tempFile);
    }

    public void performCrop()
    {
        Uri picUri = getImageUriFromBitmap();
        //call the standard crop action intent (the user device may not support it)
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri
        cropIntent.setDataAndType(picUri, "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 0);  //for fixed size = 1
        cropIntent.putExtra("aspectY", 0);  //for fixed size = 1
        //indicate output X and Y
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //start the activity - handle returning in onActivityResult
        Log.i("PERFORM_CROP", "Just before starting to cropIntent!");
        startActivityForResult(cropIntent, PIC_CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //AFTER COMING BACK FROM CROP
        if(resultCode == RESULT_OK && requestCode == PIC_CROP && data != null)
        {
            //Receive pic & show it to user
            //get the returned data
            Bundle extras = data.getExtras();
            //get the cropped bitmap
            Bitmap thePic = extras.getParcelable("data");
            if(redirectedFromGallery) {
                rotatedBitmap = thePic;
            }
            else {
                bitmap = thePic;
            }
            imageView.setImageBitmap(thePic);
        }
    }

    private void onUndoPressed()
    {
        if(redirectedFromGallery)
        {
            if (rotatedBitmap != null) {
                if (!rotatedBitmap.isRecycled()) {
                    rotatedBitmap.recycle();
                }
            }
            rotatedBitmap = getUndoBitmap();
            imageView.setImageBitmap(rotatedBitmap);
        }
        else
        {
            if (bitmap != null) {
                if (!bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
            bitmap = getUndoBitmap();
            imageView.setImageBitmap(bitmap);
        }
        undoAvailability();
    }

    private Bitmap getUndoBitmap()
    {
        if (currentShowingIndex - 1 >= 0)
            currentShowingIndex -= 1;
        else currentShowingIndex = 0;

        return bitmapsForUndo.get(currentShowingIndex)
                .copy(bitmapsForUndo.get(currentShowingIndex).getConfig(), true);
    }

    private void undoAvailability()
    {
        if(currentShowingIndex <= 0) {
            Toast.makeText(getApplicationContext(), "Undo Not Possible!", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToUndoList()
    {
        try{
            recycleBitmapList(++currentShowingIndex);
            if(redirectedFromGallery) {
                bitmapsForUndo.add(rotatedBitmap.copy(rotatedBitmap.getConfig(), true));
            }
            else {
                bitmapsForUndo.add(bitmap.copy(bitmap.getConfig(), true));
            }
        }
        catch (OutOfMemoryError error)
        {
            bitmapsForUndo.get(1).recycle();
            bitmapsForUndo.remove(1);
            if(redirectedFromGallery) {
                bitmapsForUndo.add(rotatedBitmap.copy(rotatedBitmap.getConfig(), true));
            }
            else {
                bitmapsForUndo.add(bitmap.copy(bitmap.getConfig(), true));
            }
        }
    }

    private void recycleBitmapList(int fromIndex)
    {
        while (fromIndex < bitmapsForUndo.size())
        {
            bitmapsForUndo.get(fromIndex).recycle();
            bitmapsForUndo.remove(fromIndex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        recycleBitmapList(0);
    }
}