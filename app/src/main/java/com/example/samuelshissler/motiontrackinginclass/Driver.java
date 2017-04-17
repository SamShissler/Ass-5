package com.example.samuelshissler.motiontrackinginclass;

import android.util.Log;
import java.lang.Runnable;

import java.util.concurrent.TimeUnit;

/**
 * Created by samuel.shissler on 4/17/17.
 */

public class Driver {
    public static void drive(final MainActivity mainActivity){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mainActivity.hasArrivedAtCoordinates(mainActivity.coordinates)) {
                    //synchronized (MainActivity.this) {
                        //while(true) {
                        //Log.i(TAG, "YOLO");
                        mainActivity.driveToCoordinates(mainActivity.coordinates);
                        //mainActivity.status = mainActivity.getYaw() + "";
                        //mainActivity.howdy.setText(mainActivity.status);
                        //Log.i(mainActivity.TAG, mainActivity.status);
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException ie) {
                            Log.i(mainActivity.TAG, "INTERRUPTED");
                        }
                        //}
                    //}
                }
                mainActivity.status = "ARRIVED!!!";
            }
        }).start();
    }
}
