package com.example.cameraapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ImageCaptureActivity extends AppCompatActivity{
    public ImageView image;
    public Button button;
    static final int CAPTURE_IMAGE_REQUEST = 1;
    private File photoFile;
    private String theCurrentPhotoPath="";
    private Bitmap bitmap;
    private Uri photoUri;
    public String[] pred_r = new String[4];
    public List<String> n = new ArrayList<>();
    public List<Float> cv = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imagecapture);
        Intent intent = getIntent();
        image = findViewById(R.id.image2);
        button = findViewById(R.id.button2);
        doImageCapture();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Bitmap[] imgs = divImg(photoFile);

                String url1 = "http://172.20.10.4:5000/";
                String url2 = "http://172.20.10.5:5000/";
                String url3 = "http://172.20.10.6:5000/";
                String url4 = "http://172.20.10.7:5000/";

                doImageUpload(url1, imgs[0], 0);
                doImageUpload(url2, imgs[1], 1);
                doImageUpload(url3, imgs[2], 2);
                doImageUpload(url4, imgs[3], 3);

                //finish();

            }
        });
    }
    public void determineNum(){

        AlertDialog diag = new AlertDialog.Builder(ImageCaptureActivity.this)
                .setTitle("Prediction")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //dialogInterface.dismiss();
                        finish();
                    }
                })
                .create();

        List<Float> sl = new ArrayList<>(cv);

        Collections.sort(sl);
        int idx = cv.indexOf(sl.get(3));
        String pred_num = n.get(idx);

        String fname = "Image"+String.valueOf(System.currentTimeMillis())+".jpg";
        File mypath = new File("/data/user/0/com.example.cameraapplication/"+pred_num, fname);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        n.clear();
        cv.clear();

        diag.setMessage("Predicted: "+ pred_num +"\nSaved to Mobile!");
        diag.show();

    }
    public void setLists(String s) throws IOException {
        String[] parts = s.split(":");
        n.add(parts[0]);
        cv.add(Float.parseFloat(parts[1]));
        Log.i("test1",String.valueOf(n.size()));
    }

    private void doImageUpload(String burl, Bitmap im, int num){

        Retrofit.Builder builder = new Retrofit.Builder()
                .baseUrl(burl)
                .addConverterFactory(GsonConverterFactory.create());
        Retrofit rf = builder.build();

        MultipartBody.Part img = MultipartBody.Part.createFormData(photoFile.getName(), photoFile.getName(),
                RequestBody.create(MediaType.parse("*/*"),photoFile));

        flaskapi_f cl_f = rf.create(flaskapi_f.class);

        Call<ResponseBody> call = cl_f.imageUpload(img);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String pred = "0";

                try {
                    pred = response.body().string();
                    setLists(pred);
                    if(n.size()==4){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                determineNum();
                            }
                        });

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                //String txt = "Predicted Number: "+pred+ " \nUploaded Successfully!";

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                String txt = "Error: Couldn't upload";
                Toast.makeText(ImageCaptureActivity.this, txt,Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private Bitmap[] divImg(File pic){

        Bitmap picture = BitmapFactory.decodeFile(pic.getAbsolutePath());

        Bitmap[] imgs = new Bitmap[4];

        imgs[0] = Bitmap.createBitmap(picture, 0, 0, picture.getWidth()/2, picture.getHeight()/2);
        imgs[1] = Bitmap.createBitmap(picture, picture.getWidth()/2, 0, picture.getWidth()/2, picture.getHeight()/2);
        imgs[2] = Bitmap.createBitmap(picture, 0, picture.getHeight()/2, picture.getWidth()/2, picture.getHeight()/2);
        imgs[3] = Bitmap.createBitmap(picture, picture.getWidth()/2, picture.getHeight()/2, picture.getWidth()/2, picture.getHeight()/2);

        return imgs;
    }


    private void doImageCapture() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
        else {
            Intent doPictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(doPictureIntent.resolveActivity(getPackageManager()) != null){
                try {
                    photoFile = doCreateImageFile();
                    displayMessage(getBaseContext(), photoFile.getAbsolutePath());
                    Log.i("Madhu",photoFile.getAbsolutePath());

                    if(photoFile != null){
                        photoUri = FileProvider.getUriForFile(this,"com.example.cameraapplication.fileprovider",photoFile);
                        doPictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                        startActivityForResult(doPictureIntent,CAPTURE_IMAGE_REQUEST);
                    }
                }catch (Exception e){
                    displayMessage(getBaseContext(),e.getMessage().toString());
                }
            }
        }

    }

    public void onRequestPermissionsResult(int requestCode, String[] permission, int[] grantResults){
        if(requestCode == 0){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                doImageCapture();
            }
            else
            {
                displayMessage(getBaseContext(), "PERMISSION IS DENIED");
            }
        }
    }
    private void displayMessage(Context context, String message){
        //Toast.makeText(context, message,Toast.LENGTH_LONG).show();
    }
    private File doCreateImageFile() throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + ts + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg",storageDir);
        theCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    protected void onActivityResult(int reqCode, int resCode, Intent Info){
        if(reqCode == CAPTURE_IMAGE_REQUEST && resCode == RESULT_OK){
            bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            image.setImageBitmap(bitmap);
        }
    }

}
