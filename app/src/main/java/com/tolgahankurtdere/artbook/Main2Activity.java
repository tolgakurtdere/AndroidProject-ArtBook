package com.tolgahankurtdere.artbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Main2Activity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView_selectedImage;
    EditText editText_artName,editText_artistName,editText_year;
    Button button_save;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView_selectedImage = findViewById(R.id.imageView_selectImage);
        editText_artName = findViewById(R.id.editText_artName);
        editText_artistName = findViewById(R.id.editText_artistName);
        editText_year = findViewById(R.id.editText_year);
        button_save = findViewById(R.id.button_save);
        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        if(intent.getStringExtra("info").matches("newData")){ //if this activity is opened for adding new data
            button_save.setVisibility(View.VISIBLE);
        }
        else{
            button_save.setVisibility(View.INVISIBLE);
            int artId = intent.getIntExtra("artId",1);
            try{
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[]{String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    editText_artName.setText(cursor.getString(artNameIx));
                    editText_artistName.setText(cursor.getString(artistNameIx));
                    editText_year.setText(cursor.getString(yearIx));

                    byte[] byteArr = cursor.getBlob(imageIx); //byte dizisini gorsele cevirmek icin
                    Bitmap bitmap = BitmapFactory.decodeByteArray(byteArr,0,byteArr.length);
                    imageView_selectedImage.setImageBitmap(bitmap);
                }

            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public void click_selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);

        }
        else{
            Intent intentGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentGallery,2);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if(requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intentGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentGallery,2);
            }
            else{
                Toast.makeText(this,"Permission Denied!",Toast.LENGTH_LONG).show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { //when photo is selected

        if(requestCode == 2 && resultCode == RESULT_OK && data != null){
            Uri imageData = data.getData();

            try {
                if(Build.VERSION.SDK_INT >= 28){
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                }
                else{
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(),imageData);
                }
                imageView_selectedImage.setImageBitmap(selectedImage);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void click_save(View view){
        String artName = editText_artName.getText().toString();
        String artistName = editText_artistName.getText().toString();
        String year = editText_year.getText().toString();

        Bitmap smallImage = makeImageSmaller(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); //convert image to byte[] to save this in sqlLite
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArr = outputStream.toByteArray();

        try{
            //database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)");

            String sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString); //sql can understand string in this way

            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArr);
            sqLiteStatement.execute();

        } catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(Main2Activity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeImageSmaller(Bitmap image,int maxSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float ratio = (float)width / height;

        if(ratio > 1){
            width = maxSize;
            height = (int)(width / ratio);
        }
        else{
            height = maxSize;
            width = (int)(height * ratio);
        }
        return Bitmap.createScaledBitmap(image,width,height,true); //return a new scaled bitmap
    }
}
