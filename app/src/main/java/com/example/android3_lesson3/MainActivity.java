package com.example.android3_lesson3;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 42;
    PublishSubject<Uri> filenameSubject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        filenameSubject = PublishSubject.create();

        final Button button = findViewById(R.id.button);
        Observable<String> buttonObservable = Observable.create(emitter -> button.setOnClickListener(view -> emitter.onNext("")));
        buttonObservable.subscribe((s) -> selectImageDialog());

        filenameSubject.subscribeOn(Schedulers.io()).subscribe((filename) -> {
            Bitmap bitmapSelectedImage = getImage(filename);
            saveAsPNG(bitmapSelectedImage);});
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode != SELECT_PICTURE || resultCode != RESULT_OK) {
            return;
        }

        filenameSubject.onNext(intent.getData());
        filenameSubject.onComplete();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
