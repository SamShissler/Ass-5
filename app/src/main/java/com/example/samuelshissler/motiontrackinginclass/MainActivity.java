package com.example.samuelshissler.motiontrackinginclass;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPointCloudData;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements SetAdfNameDialog.CallbackListener, SaveAdfTask.SaveAdfListener{

    String TAG = "SAMUEL.L.SHISSLER";

    Switch adfSwitch;
    Switch learningSwitch;

    boolean adfBool, learningBool, learnedBool = false;

    public Coordinates coordinates = new Coordinates(-1.5, -1.20);
    public String fullUUIDString = "";


    private static final int SECS_TO_MILLISECS = 1000;
    private Tango mTango;
    private TangoConfig mConfig;

    private double mPreviousPoseTimeStamp;
    private double mTimeToNextUpdate = UPDATE_INTERVAL_MS;

    private boolean mIsRelocalized;
    private boolean mIsLearningMode = true;
    private boolean mIsConstantSpaceRelocalize;

    // Long-running task to save the ADF.
    private SaveAdfTask mSaveAdfTask;

    private static final double UPDATE_INTERVAL_MS = 100.0;

    private final Object mSharedLock = new Object();

    TextView howdy;
    TextView isLocalizedView;
    TextView poseView;
    public TangoPoseData mPose = new TangoPoseData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adfSwitch = (Switch) findViewById(R.id.LoadADF);
        learningSwitch = (Switch) findViewById(R.id.LearningMode);
        howdy = (TextView) findViewById(R.id.textView);
        isLocalizedView = (TextView) findViewById(R.id.textView2);
        poseView = (TextView) findViewById(R.id.textView3);
        howdy.setText("HOWDY");



        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //View adf = findViewById(R.id.LoadADF);
                if (adfBool) {
                    Snackbar.make(view, adfBool + "ADFBOOLOOLOOLOO", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    ArrayList<String> fullUUIDList = new ArrayList<String>();
                    // Returns a list of ADFs with their UUIDs
                    fullUUIDList = mTango.listAreaDescriptions();

                    // Load the latest ADF if ADFs are found.
                    if (fullUUIDList.size() > 0) {
                        /*try {
                            mConfig = setTangoConfig(
                                    mTango, false, true);
                            mTango.connect(mConfig);
                            startupTango();
                        } catch (TangoOutOfDateException e) {
                            Log.e(TAG, "tango_out_of_date_exception", e);
                        } catch (TangoErrorException e) {
                            Log.e(TAG, "tango_error", e);
                        } catch (TangoInvalidException e) {
                            Log.e(TAG, "tango_invalid", e);
                        } catch (SecurityException e) {
                            // Area Learning permissions are required. If they are not available,
                            // SecurityException is thrown.
                            Log.e(TAG, "no_permissions", e);
                        }*/

                        mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                                fullUUIDList.get(fullUUIDList.size() - 1));
                        Toast.makeText(MainActivity.this, fullUUIDList.get(fullUUIDList.size() - 1).toString(), Toast.LENGTH_SHORT).show();
                        mIsRelocalized = false;
                    }



                }
                else if (learningBool) {
                    Snackbar.make(view, learningBool + "LearningBool", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    if(!learnedBool) {

                        try {
                            mConfig.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, "SamsSuperCoolADF");
                        } catch (TangoErrorException e) {
                            // handle exception
                        }
                        try {
                            TangoConfig mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
                            mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);
                        } catch (TangoErrorException e) {
                            // handle exception
                        }
                        learnedBool = true;
                    }else{
                        learnedBool = false;
                        try {
                            mConfig = setTangoConfig(
                                    mTango, true, false);
                            mTango.connect(mConfig);
                            startupTango();
                        } catch (TangoOutOfDateException e) {
                            Log.e(TAG, "tango_out_of_date_exception", e);
                        } catch (TangoErrorException e) {
                            Log.e(TAG, "tango_error", e);
                        } catch (TangoInvalidException e) {
                            Log.e(TAG, "tango_invalid", e);
                        } catch (SecurityException e) {
                            // Area Learning permissions are required. If they are not available,
                            // SecurityException is thrown.
                            Log.e(TAG, "no_permissions", e);
                        }
                        saveAdf("SamsSuperCoolADF.adf");
                    }
                }
                else{
                    Log.i(TAG, "TeTSST");
                    Driver.drive(MainActivity.this);

                }
            }
        });
        adfSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adfBool = isChecked;
                //Toast.makeText(MainActivity.this, "The Switch is ", Toast.LENGTH_SHORT).show();
            }
        });
        learningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                learningBool = isChecked;
                //Toast.makeText(MainActivity.this, "The Switch is not", Toast.LENGTH_SHORT).show();
            }
        });
        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Clear the relocalization state; we don't know where the device will be since our app
        // will be paused.
        mIsRelocalized = false;
        synchronized (this) {
            try {
                mTango.disconnect();
            } catch (TangoErrorException e) {
                Log.e(TAG, "TangoErrorException", e);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        mTango = new Tango(MainActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                synchronized (MainActivity.this) {
                    try {
                        mConfig = setTangoConfig(
                                mTango, false, true);
                        mTango.connect(mConfig);
                        startupTango();
                    } catch (TangoOutOfDateException e) {
                        Log.e(TAG, "tango_out_of_date_exception", e);
                    } catch (TangoErrorException e) {
                        Log.e(TAG, "tango_error", e);
                    } catch (TangoInvalidException e) {
                        Log.e(TAG, "tango_invalid", e);
                    } catch (SecurityException e) {
                        // Area Learning permissions are required. If they are not available,
                        // SecurityException is thrown.
                        Log.e(TAG, "no_permissions", e);
                    }
                }

                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (MainActivity.this) {
                            setupTextViewsAndButtons(mTango, mIsLearningMode,
                                    mIsConstantSpaceRelocalize);
                        }
                    }
                });*/
            }
        });
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {
        // Use default configuration for Tango Service.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // Check if learning mode.
        if (isLearningMode) {
            // Set learning mode to config.
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);

        }
        // Check for Load ADF/Constant Space relocalization mode.
        if (isLoadAdf) {
            ArrayList<String> fullUuidList;
            // Returns a list of ADFs with their UUIDs.
            fullUuidList = tango.listAreaDescriptions();
            // Load the latest ADF if ADFs are found.
            if (fullUuidList.size() > 0) {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUuidList.get(fullUuidList.size() - 1));
            }
        }
        return config;
    }

    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     */
    private void startupTango() {
        // Set Tango listeners for Poses Device wrt Start of Service, Device wrt
        // ADF and Start of Service wrt ADF.
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

        ArrayList<String> fullUUID = mTango.listAreaDescriptions();
        for(String uuid : fullUUID){
            fullUUIDString = fullUUIDString + "\n" + uuid;
        }

        mTango.connectListener(framePairs, new OnTangoUpdateListener() {

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Make sure to have atomic access to Tango data so that UI loop doesn't interfere
                // while Pose call back is updating the data.

                synchronized (mSharedLock) {
                    // Check for Device wrt ADF pose, Device wrt Start of Service pose, Start of
                    // Service wrt ADF pose (this pose determines if the device is relocalized or
                    // not).
                    mPose = pose;
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
                        // Process new ADF to device pose data.
                        //Log.d(TAG, "New ADF, who dis?");
                    }
                    else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData
                            .COORDINATE_FRAME_START_OF_SERVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mIsRelocalized = true;
                        } else {
                            mIsRelocalized = false;
                            Log.d(TAG, "Not Relocalizedddddddddd");
                        }
                    }
                    /*else{
                        Log.d(TAG, "\t"+ (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION)
                                +"\n\t"+ (pose.targetFrame == TangoPoseData
                                .COORDINATE_FRAME_START_OF_SERVICE));
                    }*/
                }

                final double deltaTime = (pose.timestamp - mPreviousPoseTimeStamp) *
                        SECS_TO_MILLISECS;
                mPreviousPoseTimeStamp = pose.timestamp;
                mTimeToNextUpdate -= deltaTime;

                if (mTimeToNextUpdate < 0.0) {
                    mTimeToNextUpdate = UPDATE_INTERVAL_MS;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (mSharedLock) {
//                                mSaveAdfButton.setEnabled(mIsRelocalized);
                                //mRelocalizationTextView.setText(mIsRelocalized ?
                                //getString(R.string.localized) :
                                //getString(R.string.not_localized));
                                //Log.i(TAG, ""+mIsRelocalized);
                                howdy.setText(status);
                                poseView.setText("YAW: "+ getYaw()+"\nDistance: " +getDistanceToCoordinates(coordinates));
                                isLocalizedView.setText(fullUUIDString + "\n\n"+(mPose.statusCode == TangoPoseData.POSE_VALID) +"\n\nIs Localized = "+mIsRelocalized);
                            }
                        }
                    });
                }
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                // We are not using onXyzIjAvailable for this app.
            }

            @Override
            public void onPointCloudAvailable(TangoPointCloudData xyzij) {
                // We are not using onPointCloudAvailable for this app.
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles failed save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfFailed(String adfName) {
        Toast.makeText(this, "SaveAdfFailed :(", Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
    }

    /**
     * Handles successful save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid) {
        Toast.makeText(this, "SaveAdfSuccccessed :)", Toast.LENGTH_LONG).show();
        mSaveAdfTask = null;
        finish();
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameOk(String name, String uuid) {
        saveAdf(name);
    }

    /**
     * Implements SetAdfNameDialog.CallbackListener.
     */
    @Override
    public void onAdfNameCancelled() {
        // Continue running.
    }

    private void saveAdf(String adfName) {
        mSaveAdfTask = new SaveAdfTask(this, this, mTango, adfName);
        mSaveAdfTask.execute();
    }



    String status = "Ststss";

    // Distance and angle tolerances
    private final double DISTANCE_TOLERANCE = 0.5;
    private final double ANGLE_TOLERANCE = 10.0;

    public void driveToCoordinates(Coordinates coordinates) {
        double distance = getDistanceToCoordinates(coordinates);
        double actualAngleToCoordinates = getActualAngleToCoordinates(coordinates);

        boolean withinDistanceTolerance = distance < DISTANCE_TOLERANCE;
        boolean withinAngleTolerance = Math.abs(actualAngleToCoordinates) < ANGLE_TOLERANCE;

        if (withinDistanceTolerance) {
            // Arrived at coordinates
            status = "At " + coordinates.toString();
            return;
        }

        if (withinAngleTolerance) {
            status = "Go Straight";
            //status = "Going straight at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
            //driveForward(motorSpeed);
        } else {
            if (actualAngleToCoordinates < 0) {
                status = "Turn Left";
                //status = "Turning left at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
                //driveLeft(motorSpeed);
            } else {
                status = "Turn Right";
                //status = "Turning right at " + Double.toString(actualAngleToCoordinates) + " to get to " + coordinates.toString();
                //driveRight(motorSpeed);
            }
        }
    }

    private double getDistanceToCoordinates(Coordinates coordinates) {
        Coordinates currentPosition = new Coordinates(getXTranslation(),
                getYTranslation());

        double deltaX = coordinates.getX() - currentPosition.getX();
        double deltaY = coordinates.getY() - currentPosition.getY();

        double distance = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));

        return distance;
    }

    // Determine the angle that the robot must rotate;
    private double getActualAngleToCoordinates(Coordinates coordinates) {
        double yaw = getYaw();

        double angleToCoordinates = getAngleToCoordinates(coordinates);

        double actualAngleToCoordinates = yaw - angleToCoordinates;

        if (actualAngleToCoordinates < 0) {
            actualAngleToCoordinates += 360;
        }

        actualAngleToCoordinates %= 360;

        if (actualAngleToCoordinates > 180) {
            actualAngleToCoordinates = actualAngleToCoordinates - 360;
        }

        return actualAngleToCoordinates;
    }

    // Determine the angle between the current coordinates and the destination coordinates
    private double getAngleToCoordinates(Coordinates coordinates) {
        Coordinates currentPosition = new Coordinates(getXTranslation(),
                getYTranslation());

        double deltaX = coordinates.getX() - currentPosition.getX();
        double deltaY = coordinates.getY() - currentPosition.getY();

        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        if (angle < 0) {
            angle += 360;
        }

        angle = angle % 360;

        return angle;
    }

    public double getXTranslation(){
        return mPose.translation[0];
    }
    public double getYTranslation(){
        return mPose.translation[1];
    }

    /*public void setMPose(TangoPoseData mPose) {
        this.mPose = mPose;
        TangoPoseData convertMPose = this.mPose;
        convertCameraCoordinatesToWorldCoordinates(convertMPose);
    }*/

    // Convert the tango (camera) coordinates to world coordinates
    /*private void convertCameraCoordinatesToWorldCoordinates(TangoPoseData mPose) {
        double cameraX = mPose.translation[TangoPoseData.INDEX_TRANSLATION_X];
        double cameraY = mPose.translation[TangoPoseData.INDEX_TRANSLATION_Y];

        double rotationAngle = cameraCoordinatesToWorldCoordinatesRotation;

        Coordinates rotatedCameraCoordinates = new Coordinates((Math.cos(rotationAngle) * cameraX) - (Math.sin(rotationAngle) * cameraY),
                (Math.sin(rotationAngle) * cameraX) + (Math.cos(rotationAngle) * cameraY));

        double rotatedCameraX = rotatedCameraCoordinates.getX();
        double rotatedCameraY = rotatedCameraCoordinates.getY();

        double translatedRotatedCameraX = rotatedCameraX + tangoXTranslationAdjustment;
        double translatedRotatedCameraY = rotatedCameraY + tangoYTranslationAdjustment;

        xTranslation = translatedRotatedCameraX;
        yTranslation = translatedRotatedCameraY;
    }*/


    public double getYaw() {
        // Quaternion vector components
        double w = mPose.rotation[TangoPoseData.INDEX_ROTATION_W];
        double x = mPose.rotation[TangoPoseData.INDEX_ROTATION_X];
        double y = mPose.rotation[TangoPoseData.INDEX_ROTATION_Y];
        double z = mPose.rotation[TangoPoseData.INDEX_ROTATION_Z];

        //Log.d(TAG, "\tw\t"+w+"\tx\t"+x+"\ty\t"+y+"\tz\t"+z);

        // Extract yaw from the quaternion vector components
        double yaw = Math.toDegrees(Math.atan2((2 * ((x * y) + (z * w))),
                (Math.pow(w, 2) + Math.pow(x, 2) -
                        Math.pow(y, 2) - Math.pow(z, 2))));

        if (yaw < 0) {
            yaw += 360;
        }

        //yaw += tangoAngleAdjustment;

        yaw = yaw % 360;

        return yaw;
    }

    public boolean hasArrivedAtCoordinates(Coordinates coordinates) {
        double distance = getDistanceToCoordinates(coordinates);

        boolean withinDistanceTolerance = distance < DISTANCE_TOLERANCE;

        if (withinDistanceTolerance) {
            return true;
        } else {
            return false;
        }
    }
}