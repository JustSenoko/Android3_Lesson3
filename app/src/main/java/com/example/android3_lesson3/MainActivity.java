package com.example.android3_lesson3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 42;
    Completable filenameSubject;
    Disposable disposable;
    AlertDialog alert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        final Button button = findViewById(R.id.button);
        Observable<String> buttonObservable = Observable.create(emitter -> button.setOnClickListener(view -> emitter.onNext("")));
        buttonObservable.subscribe((s) -> selectImageDialog());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != SELECT_PICTURE || resultCode != RESULT_OK) {
            return;
        }
        filenameSubject = Completable.create(emitter -> {
            Bitmap bitmapSelectedImage = getImage(intent.getData());
            if (bitmapSelectedImage != null) {
                saveAsPNG(bitmapSelectedImage);
            }
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());

        disposable = filenameSubject.subscribe(
                ()-> {
                    if(alert.isShowing()) alert.cancel();
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.success),Toast.LENGTH_LONG).show();
                },
                (e)->{
                    if(alert.isShowing()) alert.cancel();
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.error) + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                }
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.in_progress))
                .setMessage(getResources().getString(R.string.msg_in_progress))
                .setCancelable(false)
                .setNegativeButton(getResources().getString(R.string.cansel),(dialog, id)->{
                    if(!disposable.isDisposed()) disposable.dispose();
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.interrupted),Toast.LENGTH_LONG).show();
                    dialog.cancel();
                });
        alert = builder.create();
        alert.show();
    }

    private void selectImageDialog() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,
                "Select Picture"), SELECT_PICTURE);
    }

    private Bitmap getImage(Uri selectedImage) {
        InputStream imageStream;
        try {
            assert selectedImage != null;
            imageStream = getContentResolver().openInputStream(selectedImage);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        try {
            Thread.sleep(10000);
        }
        catch (InterruptedException e){
            return null;
        }
        return BitmapFactory.decodeStream(imageStream);
    }

    private void saveAsPNG(Bitmap bitmapSelectedImage) {
        File file = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "savedBitmap.png"
        );

        try {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                bitmapSelectedImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } finally {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
