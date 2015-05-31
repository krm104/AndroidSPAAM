/* Author: Kenneth Moser
 * Email: moserk@acm.org
 * Affiliation: Mississippi State University
 * 
 * Description: This is the main starting activity for the SPAAM calibration app
 * for the Epson Moverio BT-200.
 * 
 * In order to perform the calibration, the tracking marker (included with the software
 * as both a Microsoft Power Point and pdf files (letter and A4 size) must be printed.
 * The Calibration marker must be printed so that the black border of the marker is
 * 20cm x 20cm in size.
 * 
 * This project requires 3 external libraries:
 * 	Vuforia.java - this library facilitates the marker tracking algorithms that are required
 * in order to produce location data for the 3D world point used by the SPAAM calibration.
 * 
 * 	JAMA.java - this is a Java math library which facilitates the Singular Value Decomposition
 * operation to solve the linear equation system to obtain the SPAAM result. It also facilitates
 * basic Matrix operation functions.
 * 
 *   BT200Ctrl.java - this library provides access to some of the basic Moverio functions, such as 
 *  setting Stereo Mode, needed by this application.
 *  
 * All 3 third party libraries should be included with this project in a .zip folder.
 * 
 * The Java Run Time library (rt.java) is also required but should already be available on your
 * local development machine. If it is not please visit java.com to download a runtime environment. 
 *  
 * The SPAAM activity creates an OGLESRenderer instance and waits for the user to select
 * the eye which they desire to calibrate (left or right). Once the eye is chosen the
 * activity then calls the rendering function to begin displaying the cross-hair pattern to the user.
 *******************************************************************************/

/******package name******/
package com.androidspaam;

/******Java Specific Libraries******/
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

/******Qualcomm AR library for Vuforia Useage******/
import com.qualcomm.QCAR.QCAR;

/******Android specific Libraries******/
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import android.view.Window;
import android.view.WindowManager; 

/******Project Specific Import calls******/
import com.spaam.util.*;

/*******************************************************************************
 * Class: SPAAM
 * Extends: Activity
 * 
 * Description: This class initializes the main activity and awaits user selection
 * of the eye to calbrate (left or right). Once an eye is chosen, an instance
 * of the openGL renderer class (OGLESRenderer) is created and rendering of the
 * calibration pattern commences.
 * 
 * This class uses several C++ native functions specific to use of the Vuforia
 * marker tracking SDK. The source code for the Vuforia native functions can 
 * be found in the "jni" folder in the VuforiaNative.cpp file.
 ******************************************************************************/
public class SPAAM extends Activity {
	
	////////////////////////////////////////////////////////////////
	//////////////Members used in the Vuforia Functions/////////////
	//Simple Name to identify the class//
	private static final String TAG = "SPAAM Activity";
	
	// Focus mode constants//
	private static final int FOCUS_MODE_NORMAL = 0;
	private static final int FOCUS_MODE_CONTINUOUS_AUTO = 1;
	
	// Application status constants//
	private static final int APPSTATUS_UNINITED         = -1;
	private static final int APPSTATUS_INIT_APP         = 0;
	private static final int APPSTATUS_INIT_QCAR        = 1;
	private static final int APPSTATUS_INIT_TRACKER     = 2;
	private static final int APPSTATUS_INIT_APP_AR      = 3;
	private static final int APPSTATUS_LOAD_TRACKER     = 4;
	private static final int APPSTATUS_INITED           = 5;
	private static final int APPSTATUS_CAMERA_STOPPED   = 6;
	private static final int APPSTATUS_CAMERA_RUNNING   = 7;
	
	// Name of the native dynamic libraries to load//
	private static final String NATIVE_LIB_SAMPLE = "VuforiaNative";
	private static final String NATIVE_LIB_QCAR = "Vuforia";
	
	// Display size of the device//
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	
	// Constant representing invalid screen orientation to trigger a query//
	private static final int INVALID_SCREEN_ROTATION = -1;
	
	// Last detected screen rotation//
	private int mLastScreenRotation = INVALID_SCREEN_ROTATION;
	
	// The current application status//
	private int mAppStatus = APPSTATUS_UNINITED;
	
	// The async tasks to initialize the QCAR SDK//
	private InitQCARTask mInitQCARTask;
	private LoadTrackerTask mLoadTrackerTask;
	
	// An object used for synchronizing QCAR initialization, dataset loading and
	// the Android onDestroy() life cycle event. If the application is destroyed
	// while a data set is still being loaded, then we wait for the loading
	// operation to finish before shutting down QCAR:
	private Object mShutdownLock = new Object();
	
	// QCAR initialization flags//
	private int mQCARFlags = 0;
	
	// Contextual Menu Options for Camera Flash - Autofocus//
	private boolean mFlash = false;
	private boolean mContAutofocus = false;
	
	// The menu item for swapping data sets//
	MenuItem mDataSetMenuItem = null;
	boolean mIsStonesAndChipsDataSetActive  = false;
	
	private RelativeLayout mUILayout;
	
	////////////////////////////////////////////////////
	//////////Vuforia Native Function Declarations//////
	/** Native tracker initialization and deinitialization. */
	public native int initTracker();
	public native void deinitTracker();
	
	/** Native functions to load and destroy tracking data. */
	public native int loadTrackerData();
	public native void destroyTrackerData();
	
	/** Native sample initialization. */
	public native void onQCARInitializedNative();
	
	/** Native methods for starting and stopping the camera. */
	private native void startCamera();
	private native void stopCamera();
	
	/** Native method for setting / updating the projection matrix
	* for AR content rendering */
	private native void setProjectionMatrix();
	
	/** Native function to initialize the application. */
	private native void initApplicationNative(int width, int height);
	
	/** Native function to deinitialize the application.*/
	private native void deinitApplicationNative();    
	
	/** Tells native code whether we are in portait or landscape mode */
	private native void setActivityPortraitMode(boolean isPortrait);
	
	/** Tells native code to switch dataset as soon as possible*/
	private native void switchDatasetAsap();
	
	private native boolean autofocus();
	private native boolean setFocusMode(int mode);
	
	/** Activates the Flash */
	private native boolean activateFlash(boolean flash);
	///////////////////////////////////////////////////////////

	/** A helper for loading native libraries stored in "libs/armeabi*". */
	public static boolean loadLibrary(String nLibName)
	{
		try
		{
			System.loadLibrary(nLibName);
			Log.d(TAG, "Native library lib" + nLibName + ".so loaded");
			return true;
		}
		catch (UnsatisfiedLinkError ulee)
		{
			Log.d(TAG, "The library lib" + nLibName +
			".so could not be loaded");
		}
		catch (SecurityException se)
		{
			Log.d(TAG, "The library lib" + nLibName +
			".so was not allowed to be loaded");
		}
		
		return false;
	}
	
	/** Static initializer block to load native libraries on start-up. */
	static
	{
		loadLibrary(NATIVE_LIB_QCAR);
		loadLibrary(NATIVE_LIB_SAMPLE);
	}

	/** An async task to initialize QCAR asynchronously. */
	private class InitQCARTask extends AsyncTask<Void, Integer, Boolean>
	{
		// Initialize with invalid value:
		private int mProgressValue = -1;
		
		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap with initialization:
			synchronized (mShutdownLock)
			{
				QCAR.setInitParameters(SPAAM.this, mQCARFlags);
				
				do
				{
					// QCAR.init() blocks until an initialization step is
					// complete, then it proceeds to the next step and reports
					// progress in percents (0 ... 100%).
					// If QCAR.init() returns -1, it indicates an error.
					// Initialization is done when progress has reached 100%.
					mProgressValue = QCAR.init();
					
					// Publish the progress value:
					publishProgress(mProgressValue);
					
					// We check whether the task has been canceled in the
					// meantime (by calling AsyncTask.cancel(true)).
					// and bail out if it has, thus stopping this thread.
					// This is necessary as the AsyncTask will run to completion
					// regardless of the status of the component that
					// started is.
				} while (!isCancelled() && mProgressValue >= 0
					&& mProgressValue < 100);
				
				return (mProgressValue > 0);
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			// Done initializing QCAR, proceed to next application
			// initialization status:
			if (result)
			{
				Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
				"initialization successful");
				
				updateApplicationStatus(APPSTATUS_INIT_TRACKER);
			}
			else
			{
				Log.d(TAG,"InitQCARTask::onPostExecute: QCAR " +
				"initialization failed");
			}
		}
	}

	/** An async task to load the tracker data asynchronously. */
	private class LoadTrackerTask extends AsyncTask<Void, Integer, Boolean>
	{
		protected Boolean doInBackground(Void... params)
		{
			// Prevent the onDestroy() method to overlap:
			synchronized (mShutdownLock)
			{
				// Load the tracker data set:
				return (loadTrackerData() > 0);
			}
		}
		
		protected void onPostExecute(Boolean result)
		{
			Log.d(TAG,"LoadTrackerTask::onPostExecute: execution " +
			(result ? "successful" : "failed"));
			
			if (result)
			{
				// The stones and chips data set is now active:
				mIsStonesAndChipsDataSetActive = true;
				
				// Done loading the tracker, update application status:
				updateApplicationStatus(APPSTATUS_INITED);
			}
			else
			{
			
			}
		}
	}

	/** Stores screen dimensions */
	private void storeScreenDimensions()
	{
		// Query display dimensions:
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
	}

	private void updateActivityOrientation()
	{
		Configuration config = getResources().getConfiguration();
		
		boolean isPortrait = false;
		
		switch (config.orientation)
		{
			case Configuration.ORIENTATION_PORTRAIT:
				isPortrait = true;
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				isPortrait = false;
				break;
			case Configuration.ORIENTATION_UNDEFINED:
				default:
				break;
		}
	
		Log.d(TAG,"Activity is in "
		+ (isPortrait ? "PORTRAIT" : "LANDSCAPE"));
		setActivityPortraitMode(isPortrait);
	}

	/** Updates projection matrix and viewport after a screen rotation
	* change was detected. *******************************************/
	public void updateRenderView()
	{
		int currentScreenRotation = getWindowManager().getDefaultDisplay().getRotation();
		if (currentScreenRotation != mLastScreenRotation)
		{
			// Set projection matrix if there is already a valid one:
			
			if ( (QCAR.isInitialized() && mAppStatus == APPSTATUS_CAMERA_RUNNING))
			{
				Log.d(TAG,"VuforiaJMEActivity::updateRenderView");
				
				// Query display dimensions:
				storeScreenDimensions();
				
				// Update projection matrix:
				setProjectionMatrix();
				
				// Cache last rotation used for setting projection matrix:
				mLastScreenRotation = currentScreenRotation;
			}
		}
	}

	/** NOTE: this method is synchronized because of a potential concurrent
	* access by VuforiaJMEActivity::onResume() and InitQCARTask::onPostExecute(). */
	private synchronized void updateApplicationStatus(int appStatus)
	{
		// Exit if there is no change in status:
		if (mAppStatus == appStatus)
			return;
		
		// Store new status value:
		mAppStatus = appStatus;
		
		// Execute application state-specific actions:
		switch (mAppStatus)
		{
			case APPSTATUS_INIT_APP:
				// Initialize application elements that do not rely on QCAR
				// initialization:
				initApplication();
				
				// Proceed to next application initialization status:
				updateApplicationStatus(APPSTATUS_INIT_QCAR);
				break;
			
			case APPSTATUS_INIT_QCAR:
				// Initialize QCAR SDK asynchronously to avoid blocking the
				// main (UI) thread.
				//
				// NOTE: This task instance must be created and invoked on the
				// UI thread and it can be executed only once!
				try
				{
					mInitQCARTask = new InitQCARTask();
					mInitQCARTask.execute();
				}
				catch (Exception e)
				{
					Log.d(TAG,"Initializing QCAR SDK failed");
				}
				break;
			
			case APPSTATUS_INIT_TRACKER:
				// Initialize the ImageTracker:
				if (initTracker() > 0)
				{
					// Proceed to next application initialization status:
					updateApplicationStatus(APPSTATUS_INIT_APP_AR);
				}
				break;
			
			case APPSTATUS_INIT_APP_AR:
				// Initialize Augmented Reality-specific application elements
				// that may rely on the fact that the QCAR SDK has been
				// already initialized:
				initApplicationAR();
				
				// Proceed to next application initialization status:
				updateApplicationStatus(APPSTATUS_LOAD_TRACKER);
				break;
			
			case APPSTATUS_LOAD_TRACKER:
				// Load the tracking data set:
				//
				// NOTE: This task instance must be created and invoked on the
				// UI thread and it can be executed only once!
				try
				{
					mLoadTrackerTask = new LoadTrackerTask();
					mLoadTrackerTask.execute();
				}
				catch (Exception e)
				{
					Log.d(TAG,"Loading tracking data set failed");
				}
				break;
			
			case APPSTATUS_INITED:
				// Hint to the virtual machine that it would be a good time to
				// run the garbage collector:
				//
				// NOTE: This is only a hint. There is no guarantee that the
				// garbage collector will actually be run.
				System.gc();
				
				// Native post initialization:
				onQCARInitializedNative();

				// Start the camera:
				updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
				
				break;
			
			case APPSTATUS_CAMERA_STOPPED:
				// Call the native function to stop the camera:
				stopCamera();
				break;
			
			case APPSTATUS_CAMERA_RUNNING:
				// Call the native function to start the camera:
				startCamera();
				
				// Set continuous auto-focus if supported by the device,
				// otherwise default back to regular auto-focus mode.
				// This will be activated by a tap to the screen in this
				// application.
				if (!setFocusMode(FOCUS_MODE_CONTINUOUS_AUTO))
				{
				mContAutofocus = false;
				setFocusMode(FOCUS_MODE_NORMAL);
				}
				else
				{
				mContAutofocus = true;
				}
				break;
			
			default:
				throw new RuntimeException("Invalid application state");
		}
	}

	/** Initialize application GUI elements that are not related to AR. */
	private void initApplication()
	{
		// Set the screen orientation:
		// NOTE: Use SCREEN_ORIENTATION_LANDSCAPE or SCREEN_ORIENTATION_PORTRAIT
		//       to lock the screen orientation for this activity.
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		
		// This is necessary for enabling AutoRotation in the Augmented View
		if (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
		{
			// NOTE: We use reflection here to see if the current platform
			// supports the full sensor mode (available only on Gingerbread
			// and above.
			try
			{
				// SCREEN_ORIENTATION_FULL_SENSOR is required to allow all 
				// 4 screen rotations if API level >= 9:
				Field fullSensorField = ActivityInfo.class
				.getField("SCREEN_ORIENTATION_FULL_SENSOR");
				screenOrientation = fullSensorField.getInt(null);
			}
			catch (NoSuchFieldException e)
			{
				// App is running on API level < 9, do nothing.
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// Apply screen orientation
		setRequestedOrientation(screenOrientation);
		
		updateActivityOrientation();
		
		// Query display dimensions:
		storeScreenDimensions();
		
		// As long as this window is visible to the user, keep the device's
		// screen turned on and bright:
		getWindow().setFlags(
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
		WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/** Initializes AR application components. */
	private void initApplicationAR()
	{
		// Do application initialization in native code (e.g. registering
		// callbacks, etc.):
		initApplicationNative(mScreenWidth, mScreenHeight);
	}

	private boolean mCreatedBefore = false;
		
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	/////////////////Non Vuforia Related Members////////////////////

	///////////////////////////////////////////////////////////////
	///////////////////////DATA MEMBERS////////////////////////////
	
	/** OpenGL ES rendering related objects **/
	private GLSurfaceView glSurfaceView;
	private boolean renderSet = false;
	OGLESRenderer oglRenderer = null;
	
	///////////////////////////////////////////////////////////////
	
	///////////////////////////////////////////////////////////////
	//////////////////////////Classes//////////////////////////////
	
	//////////////////////////////////////////////////////////////
	///////////////////////Other Methods/////////////////////////
	/** This function is called if the Left Eye is Selected for Calibration **/
	public void setLeftEye(View view) throws IOException {
			//simple flag denoting left eye is the chosen eye//
			oglRenderer.eye = false;
			
			//function to prepare the correct calibration file for reading/writing//
			oglRenderer.SetupFileFunc(false);
			
			//set the OpenGL renderer to be the active content view (makes it visible)//
			setContentView(glSurfaceView);
	 }
	
	/** This function is called if the Right Eye is Selected for Calibration **/
	public void setRightEye(View view) throws IOException {
		{
			//simple flag denoting right eye is the chosen eye//
			oglRenderer.eye = true;
			
			//function to prepare the correct calibration file for reading/writing//
			oglRenderer.SetupFileFunc(true);
			
			//set theOpenGL renderer to be the active content view (makes it visible)
   	    	 setContentView(glSurfaceView);
		}
	 }
	///////////////////////////////////////////////////////////////
	
	static boolean firstTimeGetImage=true;
	
	/*********************************************************************************
	 * Method called at the start of the program when the SPAAM activity is created.
	 * The main purpose of this activity is to configure the necessary attributes
	 * and parameters so that OpenGL ES 2.0 can be used and also to set some specific
	 * device settings (such as hiding the lower menu of the Moverio device).
	 * 
	 * Callbacks to handle tap events on the OpenGL rendering window are also set in this
	 * method as well, and the Vuforio libraries loaded and marker tracking enabled.
	 *********************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /////////////////////////////////
        ///////App Setup///////////////
        ///////////////////////////////////
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= 0x80000000;
        win.setAttributes(winParams); 

        setContentView(R.layout.activity_spaam);
        //////////////////////////////////
        
        //////////////////////////////////
        ///////////////Vuforia////////////
        //////////////////////////////////
        updateApplicationStatus(APPSTATUS_INIT_APP);
        //////////////////////////////////
        
        /////////////////////////////////
        ///////////////OGL///////////////
        /////////////////////////////////
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    	final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
    	final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;
        ///////////////
        glSurfaceView = new GLSurfaceView(this);
		//////////////////////////
		if ( supportsEs2 ){
			oglRenderer = new OGLESRenderer(this);
			glSurfaceView.setEGLContextClientVersion(2);
			glSurfaceView.setRenderer(oglRenderer);
			renderSet = true;
			//Set tap event listener handler//
			glSurfaceView.setOnTouchListener(new OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent event){
					if (event != null){
						if ( event.getAction() == MotionEvent.ACTION_DOWN){
							glSurfaceView.queueEvent(new Runnable(){
								@Override
								public void run(){
									try {
										oglRenderer.handleTouchPress();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							});
						} else if (event.getAction() == MotionEvent.ACTION_MOVE){
							glSurfaceView.queueEvent(new Runnable(){
								@Override
								public void run(){
									oglRenderer.handleTouchDrag();
								}
							});
						}
						return true;
					} else {
						return false;
					}
				}
			});
		} else {
			Toast.makeText(this, "ÖpenGL ES2.0 Not Supported" , Toast.LENGTH_LONG).show();
		}
		//////////////////////////	
    }

    /***********************************************************************
     * This function is currently not used since an options menu is not
     * currently provided for this application. It is left in since perhaps
     * an options menu will be added at a later date.
     **********************************************************************/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.spaam, menu);
        return true;
    }

    /***********************************************************************
     * This function is currently not used since an options menu is not 
     * currently included with this application. It is left in since perhaps
     * an options menu will be added at a later date.
     **********************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();
        //if (id == R.id.action_settings) {
        //    return true;
        //}
        return super.onOptionsItemSelected(item);
    }

    /***********************************************************************
     * This function is called whenever the main activity is resumed from the
     * paused state (including when the application is initialy started.
     * 
     * The primary function is to ensure that the Vuforia (QCAR) marker tracking
     * engine is running and that the OpenGL renderer is also drawing.
     **********************************************************************/
    public void onResume(){
    	super.onResume();

    	///////////////////////////////////
    	////////////Vuforia///////////////
    	// QCAR-specific resume operation
        QCAR.onResume();

        // We may start the camera only if the QCAR SDK has already been
        // initialized
        if (mAppStatus == APPSTATUS_CAMERA_STOPPED)
        {
            updateApplicationStatus(APPSTATUS_CAMERA_RUNNING);
        }
        
        firstTimeGetImage=true;
    	/////////////////////////////////

    	/////////////////////////////////////
    	///////////////OGL//////////////////
    	///////////////////////////////////
    	if (renderSet){
    		glSurfaceView.onResume();
    	}
    	////////////////////////////////////
    }
    
    /***********************************************************************
     * This function is called whenever the main activity is paused (by loosing
     * focus to another activity or when the user closes the activity).
     * 
     * The primary function is to ensure that the Vuforia (QCAR) engine is
     * paused so that the camera resources are freed and that the OpenGL ES
     * renderer is no longer drawing.
     **********************************************************************/
    public void onPause(){
    	super.onPause();
    	
    	/////////////////////////////////
    	/////////////Vuforia/////////////
    	/////////////////////////////////
		if (mAppStatus == APPSTATUS_CAMERA_RUNNING)
		{
		    updateApplicationStatus(APPSTATUS_CAMERA_STOPPED);
		}
				 // Disable flash when paused
		if (mFlash)
		{
		    mFlash = false;
		    activateFlash(mFlash);
		}
		
		// QCAR-specific pause operation
		QCAR.onPause();
		 
		firstTimeGetImage=true;
    	///////////////////////////////
		
		/////////////////////////////////
		////////////OGL//////////////////
		if ( renderSet){
			glSurfaceView.onPause();
		}
		/////////////////////////////////
    }

    /**********************************************************************
     * This function is called when the main activity is closed.
     * 
     * The primary function of this method is to free all of the system
     * resources acquired by the application, particularly the Vuforia
     * related resources.
     *********************************************************************/
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        
        // Cancel potentially running tasks
        if (mInitQCARTask != null &&
            mInitQCARTask.getStatus() != InitQCARTask.Status.FINISHED)
        {
            mInitQCARTask.cancel(true);
            mInitQCARTask = null;
        }

        if (mLoadTrackerTask != null &&
            mLoadTrackerTask.getStatus() != LoadTrackerTask.Status.FINISHED)
        {
            mLoadTrackerTask.cancel(true);
            mLoadTrackerTask = null;
        }

        // Ensure that all asynchronous operations to initialize QCAR
        // and loading the tracker datasets do not overlap:
        synchronized (mShutdownLock) {

            // Do application deinitialization in native code:
            deinitApplicationNative();

            // Destroy the tracking data set:
            destroyTrackerData();

            // Deinit the tracker:
            deinitTracker();

            // Deinitialize QCAR SDK:
            QCAR.deinit();
        }

        System.gc();
        
    }

    /**********************************************************************
     * This function is called when the device configuration (layout) changes.
     * 
     * This only happens on the Moverio at the start of the activity and
     * shouldn't really ever be called again since the screen layout
     * doesn't normally change on the Moverio (it's usually always landscape)
     *********************************************************************/
	@Override
    public void onConfigurationChanged(Configuration config)
    {
        super.onConfigurationChanged(config);

        updateActivityOrientation();

        storeScreenDimensions();

        // Invalidate screen rotation to trigger query upon next render call:
        mLastScreenRotation = INVALID_SCREEN_ROTATION;
    }
}