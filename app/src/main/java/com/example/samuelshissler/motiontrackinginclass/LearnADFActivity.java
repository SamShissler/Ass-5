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
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import java.util.ArrayList;

public class LearnADFActivity extends AppCompatActivity implements SetAdfNameDialog.CallbackListener, SaveAdfTask.SaveAdfListener, View.OnClickListener{

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

    public TangoPoseData mPose = new TangoPoseData();

    // Long-running task to save the ADF.
    private SaveAdfTask mSaveAdfTask;

    private static final double UPDATE_INTERVAL_MS = 100.0;

    private final Object mSharedLock = new Object();

    TextView isLocalizedView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adfSwitch = (Switch) findViewById(R.id.LoadADF);
        learningSwitch = (Switch) findViewById(R.id.LearningMode);
        isLocalizedView = (TextView) findViewById(R.id.textView2);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
        adfSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                adfBool = isChecked;
                //Toast.makeText(LearnADFActivity.this, "The Switch is ", Toast.LENGTH_SHORT).show();
            }
        });
        learningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                learningBool = isChecked;
                //Toast.makeText(LearnADFActivity.this, "The Switch is not", Toast.LENGTH_SHORT).show();
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
        mTango = new Tango(LearnADFActivity.this, new Runnable() {
            // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
            // will be running on a new thread.
            // When Tango is ready, we can call Tango functions safely here only when there are no
            // UI thread changes involved.
            @Override
            public void run() {
                synchronized (LearnADFActivity.this) {
                    try {
                        mConfig = setTangoConfig(
                                mTango, mIsRelocalized, mIsConstantSpaceRelocalize);
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
                        synchronized (LearnADFActivity.this) {
                            setupTextViewsAndButtons(mTango, mIsLearningMode,
                                    mIsConstantSpaceRelocalize);
                        }
                    }
                });*/
            }
        });
    }

    @Override
    public void onClick(View view) {
        //View adf = findViewById(R.id.LoadADF);
        if (adfBool) {
            Intent myIntent = new Intent(this, LoadADFActivity.class);
            startActivity(myIntent);
        }
        else if (learningBool) {
            Snackbar.make(view, learningBool + "LearningBool", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            if (!learnedBool) {

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
            } else {
                learnedBool = false;
                saveAdf("SamsSuperCoolADF.adf");
            }
        }
    }

    /**
     * Sets up the Tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {
        // Use default configuration for Tango Service.
        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        // Check if learning mode.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);

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
        //framePairs.add(new TangoCoordinateFramePair(
          //      TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
            //    TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

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
                                //howdy.setText(status);
                                //poseView.setText("YAW: "+ getYaw()+"\nDistance: " +getDistanceToCoordinates(coordinates));
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




}