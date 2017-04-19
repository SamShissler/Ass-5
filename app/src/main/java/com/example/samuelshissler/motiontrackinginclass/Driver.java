package com.example.samuelshissler.motiontrackinginclass;

import android.util.Log;
import java.lang.Runnable;

import java.util.concurrent.TimeUnit;

/**
 * Created by samuel.shissler on 4/17/17.
 */

public class Driver {
    public static void drive(final LoadADFActivity loadADFActivity){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!loadADFActivity.hasArrivedAtCoordinates(loadADFActivity.coordinates)) {
                    //synchronized (LearnADFActivity.this) {
                        //while(true) {
                        //Log.i(TAG, "YOLO");
                        loadADFActivity.driveToCoordinates(loadADFActivity.coordinates);
                        //loadADFActivity.status = loadADFActivity.getYaw() + "";
                        //loadADFActivity.howdy.setText(loadADFActivity.status);
                        //Log.i(loadADFActivity.TAG, loadADFActivity.status);
                        try {
                            TimeUnit.MILLISECONDS.sleep(20);
                        } catch (InterruptedException ie) {
                            Log.i(loadADFActivity.TAG, "INTERRUPTED");
                        }
                        //}
                    //}
                }
                loadADFActivity.status = "ARRIVED!!!";
            }
        }).start();
    }
}
