package vishnu.android.fdtect;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {
    private final int PERMISSION_ACCESS_CAMERA = 100, PHOTO_REQUEST = 200;
    private boolean cameraPermission = false, writeToExternalPermission = false;
    private Uri imageUri;
    private FaceDetector detector;
    private ImageView mImageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView2);
        if (Build.VERSION.SDK_INT >= 23)
            askPermission();

        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        ImageView imgTakeSelfie = (ImageView) findViewById(R.id.imageView);
        imgTakeSelfie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        switch (requestCode){
            case PERMISSION_ACCESS_CAMERA:
                if (grantResults.length > 0  ){
                    if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "App needs the permission", Toast.LENGTH_LONG).show();
                        cameraPermission = true;
                    }
                    if (grantResults[1] != PackageManager.PERMISSION_GRANTED){
                        writeToExternalPermission = true;
                    }
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == PHOTO_REQUEST && resultCode == RESULT_OK){
            launchMediaScanIntent();
            try{
                scanFaces();
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "Failed to load image", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void askPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_ACCESS_CAMERA);
        }
        else {
            cameraPermission = true;
            writeToExternalPermission = true;
        }
    }

    private void takePic(){
        if (cameraPermission && writeToExternalPermission){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photo = new File(Environment.getExternalStorageDirectory(), "pic.jpg");
            imageUri = Uri.fromFile(photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, PHOTO_REQUEST);
        }
        else askPermission();
    }

    private void launchMediaScanIntent(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void scanFaces() throws Exception{
        Bitmap bitmap = decodeBitmapUri(this);
        if (detector.isOperational() && bitmap != null){
            Bitmap editedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
            float scale = getResources().getDisplayMetrics().density;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.rgb(255,61,61));
            paint.setTextSize( 14*scale);
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(bitmap, 0,0, paint);
            Frame frame = new Frame.Builder().setBitmap(editedBitmap).build();
            SparseArray<Face> faces = detector.detect(frame);

            Drawable d = /*getResources().getDrawable(R.drawable.donkey);*/ContextCompat.getDrawable(this, R.drawable.donkey);
            Bitmap b = ((BitmapDrawable) d).getBitmap();

            for (int index = 0; index < faces.size(); ++index){
                Face face = faces.valueAt(index);
                /*canvas.drawRect(face.getPosition().x, face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);*/
                /*canvas.drawBitmap(b, face.getPosition().x, face.getPosition().y, paint);*/
                canvas.drawBitmap(b, null, new RectF(face.getPosition().x,face.getPosition().y,
                        face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight()), null);
                for (Landmark landmark: face.getLandmarks()
                     ) {
                    int cx = (int) landmark.getPosition().x;
                    int cy = (int) landmark.getPosition().y;
                    canvas.drawCircle(cx, cy, 5, paint);

                    /*Drawable dw = getResources().getDrawable(R.mipmap.ic_launcher);
                    Bitmap bw = ((BitmapDrawable) d).getBitmap();
                    canvas.drawBitmap(b, cx, cy, paint);*/
                }
            }
            if (faces.size() == 0){
                Toast.makeText(getApplicationContext(), "zero", Toast.LENGTH_LONG).show();
            }
            else {
                mImageView.setImageBitmap(editedBitmap);
            }
        }
    }

    private Bitmap decodeBitmapUri(Context context) throws FileNotFoundException{
        int targetWidth = 600;
        int targetHeight = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri), null, bmOptions);
        int photoWidth = bmOptions.outWidth;
        int photoHeight = bmOptions.outHeight;

        int scaleFactor = Math.min(photoWidth/targetWidth, photoHeight/targetHeight);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imageUri), null, bmOptions);
    }
}
