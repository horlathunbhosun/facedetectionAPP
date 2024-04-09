package com.olatunbosun.facedetectionapp;



import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.Manifest;

import org.opencv.android.Utils;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.media.MediaPlayer;


import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private MediaPlayer mediaPlayer;

    private static final double EYE_OPEN_THRESHOLD =0.3 ;
    JavaCameraView javaCameraView;

//    File cascFile;
    File cascFileFace, cascFileEye;

    CascadeClassifier faceDetector, eyeDetector;
    private Mat mRgba,mGrey;

    //hereeeeeeeeee
    private static String TAG = "MainActivity";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_FRONT;
//    int activeCameraBack = CameraBridgeViewBase.CAMERA_ID_BACK
//////////////////////////////////////////////////





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();
        javaCameraView =(JavaCameraView) findViewById(R.id.JavaCamView);


//////////////////////////////here
        // checking if the permission has already been granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permissions granted");
            initializeCamera(javaCameraView, activeCamera);
        } else {
            // prompt system dialog
            Log.d(TAG, "Permission prompt");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        ////////to here

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
            try {
                loadCascadeClassifierAndEnableView();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }




        javaCameraView.setCvCameraViewListener(this);


        if(OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(), "OpenCV loaded successfully :)", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), "Could not load OpenCV!!", Toast.LENGTH_LONG).show();
        }



    }



    //////////////////////////////////////////
    // callback to be executed after the user has givenapproval or rejection via system prompt
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // camera can be turned on
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                initializeCamera(javaCameraView, activeCamera);
            } else {
                // camera will stay off
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeCamera(JavaCameraView javaCameraView, int activeCamera){
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(activeCamera);
        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }
    ///////////////////////////////

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba =new Mat();
        mGrey =new Mat();
    }

    @Override
    public void onCameraViewStopped() {

        mRgba.release();
        mGrey.release();
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)  {
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();

        boolean eyesDetected = false;

        // Detect faces
        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(mGrey, faceDetections);

        // Detect eyes
        MatOfRect eyeDetections = new MatOfRect();
        for (Rect rect : faceDetections.toArray()) {
            Mat faceROI = new Mat(mGrey, rect);
            eyeDetector.detectMultiScale(faceROI, eyeDetections);
            for (Rect eyeRect : eyeDetections.toArray()) {
                // Draw a rectangle around each detected eye
                Imgproc.rectangle(mRgba,
                        new Point(rect.x + eyeRect.x, rect.y + eyeRect.y),
                        new Point(rect.x + eyeRect.x + eyeRect.width, rect.y + eyeRect.y + eyeRect.height),
                        new Scalar(0, 255, 0), 2);
                eyesDetected = true;
            }
        }

        // Draw rectangles around faces
        int thickness = 2;
        for (Rect rect : faceDetections.toArray()) {
            Imgproc.rectangle(mRgba,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(255, 0, 0), thickness);

        }

        // If eyes are not detected and at least one face is detected, play the sound
        if (!eyesDetected && !faceDetections.empty()) {
            playSound();
        }

        return mRgba;
    }

    // Method to overlay an image onto another image
    private void overlayImage(Mat background, Mat foreground, int x, int y, int width, int height) {
        Mat submat = background.submat(y, y + height, x, x + width);
        foreground.copyTo(submat, foreground);
    }


    // Method to play sound indicating closed eye
    private void playSound() {
        // Release any previously created MediaPlayer instance
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Create a new MediaPlayer instance
        mediaPlayer = MediaPlayer.create(this, R.raw.danger);
        Log.d("O1", "Playing sound"+mediaPlayer);
        // Set a listener to release the MediaPlayer once the sound finishes playing
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });

        // Start playing the sound
        mediaPlayer.start();
    }







    private void loadCascadeClassifierAndEnableView() throws IOException {
        InputStream isFace = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
        File cascadeDirFace = getDir("cascade", Context.MODE_PRIVATE);
        cascFileFace = new File(cascadeDirFace, "haarcascade_frontalface_alt2.xml");
        FileOutputStream fosFace = new FileOutputStream(cascFileFace);

        InputStream isEye = getResources().openRawResource(R.raw.haarcascade_eye);
        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
        cascFileEye = new File(cascadeDirEye, "haarcascade_eye.xml");
        FileOutputStream fosEye = new FileOutputStream(cascFileEye);

        byte[] buffer = new byte[4096];
        int bytesRead;

        while ((bytesRead = isFace.read(buffer)) != -1) {
            fosFace.write(buffer, 0, bytesRead);
        }
        isFace.close();
        fosFace.close();

        while ((bytesRead = isEye.read(buffer)) != -1) {
            fosEye.write(buffer, 0, bytesRead);
        }
        isEye.close();
        fosEye.close();

        faceDetector = new CascadeClassifier(cascFileFace.getAbsolutePath());
        eyeDetector = new CascadeClassifier(cascFileEye.getAbsolutePath());

        if (faceDetector.empty() || eyeDetector.empty()) {
            faceDetector = null;
            eyeDetector = null;
        } else {
            cascadeDirFace.delete();
            cascadeDirEye.delete();
        }
        javaCameraView.enableView();
    }




    // Method to calculate Eye Aspect Ratio (EAR)
    private double calculateEAR(Rect eyeRect) {
        // Define the coordinates of the eye landmarks (assuming a rectangular eye region)
        Point p1 = new Point(eyeRect.x, eyeRect.y + eyeRect.height / 2); // leftmost point of the eye
        Point p2 = new Point(eyeRect.x + eyeRect.width, eyeRect.y + eyeRect.height / 2); // rightmost point of the eye
        Point p3 = new Point(eyeRect.x + eyeRect.width / 2, eyeRect.y); // top point of the eye
        Point p4 = new Point(eyeRect.x + eyeRect.width / 2, eyeRect.y + eyeRect.height); // bottom point of the eye

        // Calculate distances between landmarks
        double d1 = euclideanDistance(p1, p2); // Horizontal distance
        double d2 = euclideanDistance(p3, p4); // Vertical distance

        // Calculate EAR
        double ear = d2 / (2.0 * d1);

        return ear;
    }

    // Method to calculate Euclidean distance between two points
    private double euclideanDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
    }







}