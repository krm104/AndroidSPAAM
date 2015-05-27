/* VuforiaNative - VuforiaJME Example
 *
 * Example Chapter 5
 * accompanying the book
 * "Augmented Reality for Android Application Development", Packt Publishing, 2013.
 *
 * Copyright © 2013 Jens Grubert, Raphael Grasset / Packt Publishing.
 *
 * This code is the proprietary information of Qualcomm Connected Experiences, Inc.
 * Any use of this code is subject to the terms of the License Agreement for Vuforia Software Development Kit
 * available on the Vuforia developer website.
 *
 * https://developer.vuforia.com
 *
 * This example was built from the ImageTarget example accompanying the Vuforia SDK
 * https://developer.vuforia.com/resources/sample-apps/image-targets-sample-app
 *
 * This class is based on the ImageTarget.cpp from the Vuforia ImageTarget example
 */

#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <math.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <QCAR/QCAR.h>
#include <QCAR/CameraDevice.h>
#include <QCAR/Renderer.h>
#include <QCAR/VideoBackgroundConfig.h>
#include <QCAR/Trackable.h>
#include <QCAR/TrackableResult.h>
#include <QCAR/Tool.h>
#include <QCAR/Tracker.h>
#include <QCAR/TrackerManager.h>
#include <QCAR/ImageTracker.h>
#include <QCAR/MarkerTracker.h>
#include <QCAR/CameraCalibration.h>
#include <QCAR/UpdateCallback.h>
#include <QCAR/DataSet.h>
#include <QCAR/Image.h>


#include "MathUtils.h"

#ifdef __cplusplus
extern "C"
{
#endif

// Screen dimensions:
unsigned int screenWidth        = 0;
unsigned int screenHeight       = 0;

// Indicates whether screen is in portrait (true) or landscape (false) mode
bool isActivityInPortraitMode   = false;

// The projection matrix used for rendering virtual objects:
QCAR::Matrix44F projectionMatrix;

// Constants:
static const float kObjectScale = 3.f;

QCAR::DataSet* dataSetStonesAndChips    = 0;

bool switchDataSetAsap          = false;

//global variables
JavaVM* javaVM = 0;
jobject activityObj = 0;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm,  void* reserved) {
    LOG("JNI_OnLoad");
    javaVM = vm;
    return JNI_VERSION_1_4;
}

// Object to receive update callbacks from QCAR SDK
//1) The QCAR_onUpdate method runs in a separate thread from the renderer, so OpenGL calls will not work.
//2) The State object received in the QCAR_onUpdate method is only valid for the scope of the method.
class androidspaam_UpdateCallback : public QCAR::UpdateCallback
{   
    virtual void QCAR_onUpdate(QCAR::State& state)
    {

    	//from
        //https://developer.vuforia.com/forum/faq/android-how-can-i-access-camera-image
        QCAR::Image *imageRGB565 = NULL;
        QCAR::Frame frame = state.getFrame();

        for (int i = 0; i < frame.getNumImages(); ++i) {
              const QCAR::Image *image = frame.getImage(i);
              if (image->getFormat() == QCAR::RGB565) {
                  imageRGB565 = (QCAR::Image*)image;

                  break;
              }
        }

        if (imageRGB565) {
            JNIEnv* env = 0;

            if ((javaVM != 0) && (activityObj != 0) && (javaVM->GetEnv((void**)&env, JNI_VERSION_1_4) == JNI_OK)) {

                const short* pixels = (const short*) imageRGB565->getPixels();
                int width = imageRGB565->getWidth();
                int height = imageRGB565->getHeight();
                int numPixels = width * height;

               // LOG("Update video image...");
                jbyteArray pixelArray = env->NewByteArray(numPixels * 2);
                env->SetByteArrayRegion(pixelArray, 0, numPixels * 2, (const jbyte*) pixels);
                jclass javaClass = env->GetObjectClass(activityObj);
                //jmethodID method = env-> GetMethodID(javaClass, "setRGB565CameraImage", "([BII)V");
                //env->CallVoidMethod(activityObj, method, pixelArray, width, height);

                env->DeleteLocalRef(pixelArray);

            }
        }
    }
};

androidspaam_UpdateCallback updateCallback;



JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_setActivityPortraitMode(JNIEnv *, jobject, jboolean isPortrait)
{
    isActivityInPortraitMode = isPortrait;
}



JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_switchDatasetAsap(JNIEnv *, jobject)
{
    switchDataSetAsap = true;
}


JNIEXPORT int JNICALL
Java_com_androidspaam_SPAAM_initTracker(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_initTracker");
    
    // Initialize the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::MarkerTracker* tracker = static_cast<QCAR::MarkerTracker*> (trackerManager.initTracker(QCAR::MarkerTracker::getClassType()));
    //static_cast<QCAR::ImageTracker*> (trackerManager.initTracker(QCAR::ImageTracker::getClassType()));
    if (tracker == NULL)
    {
        LOG("Failed to initialize MarkerTracker.");
        return 0;
    }

    LOG("Successfully initialized MarkerTracker.");
    return 1;
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_deinitTracker(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_deinitTracker");

    // Deinit the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    trackerManager.deinitTracker(QCAR::MarkerTracker::getClassType());
}


JNIEXPORT int JNICALL
Java_com_androidspaam_SPAAM_loadTrackerData(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_loadTrackerData");
    
    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::MarkerTracker* markerTracker = static_cast<QCAR::MarkerTracker*>(
    		trackerManager.getTracker(QCAR::MarkerTracker::getClassType()));
	//QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
	//					trackerManager.getTracker(QCAR::ImageTracker::getClassType()));

    if (markerTracker == NULL)
    {
        LOG("Failed to load tracking data set because the MarkerTracker has not"
            " been initialized.");
        return 0;
    }

    if ( !markerTracker->createFrameMarker(0, "MarkerQ", QCAR::Vec2F(20,20)) )
    {
            LOG("Failed to create frame markers.");
            return 0;
    }
    /*
    // Create the data sets:
    dataSetStonesAndChips = markerTracker->createDataSet();
    if (dataSetStonesAndChips == 0)
    {
        LOG("Failed to create a new tracking data.");
        return 0;
    }

    // Load the data sets:
    if (!dataSetStonesAndChips->load("VuforiaJME.xml", QCAR::DataSet::STORAGE_APPRESOURCE))
    {
        LOG("Failed to load data set.");
        return 0;
    }

    // Activate the data set:
    if (!imageTracker->activateDataSet(dataSetStonesAndChips))
    {
        LOG("Failed to activate data set.");
        return 0;
    }
*/
    LOG("Successfully loaded and activated data set.");
    return 1;
}


JNIEXPORT int JNICALL
Java_com_androidspaam_SPAAM_destroyTrackerData(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_destroyTrackerData");

    // Get the image tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::MarkerTracker* markerTracker = static_cast<QCAR::MarkerTracker*>(
        		trackerManager.getTracker(QCAR::MarkerTracker::getClassType()));
    //QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(
    //   trackerManager.getTracker(QCAR::ImageTracker::getClassType()));

    if (markerTracker == NULL)
    {
        LOG("Failed to destroy the tracking data set because the MarkerTracker has not"
            " been initialized.");
        return 0;
    }
    
    if ( !markerTracker->destroyMarker(markerTracker->getMarker(0)) )
    {
    	LOG("Failed to destroy the MarkerTracker 0 ");
    	return 0;
    }
    /*
    if (dataSetStonesAndChips != 0)
    {
        if (imageTracker->getActiveDataSet() == dataSetStonesAndChips &&
            !imageTracker->deactivateDataSet(dataSetStonesAndChips))
        {
            LOG("Failed to destroy the tracking data set StonesAndChips because the data set "
                "could not be deactivated.");
            return 0;
        }

        if (!imageTracker->destroyDataSet(dataSetStonesAndChips))
        {
            LOG("Failed to destroy the tracking data set StonesAndChips.");
            return 0;
        }

        LOG("Successfully destroyed the data set StonesAndChips.");
        dataSetStonesAndChips = 0;
    }
    */
    return 1;
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_onQCARInitializedNative(JNIEnv *, jobject)
{
    // Register the update callback where we handle the data set swap:
    QCAR::registerCallback(&updateCallback);

    // Comment in to enable tracking of up to 2 targets simultaneously and
    // split the work over multiple frames:
    // QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
}

// RENDERING CALL

JNIEXPORT void JNICALL
Java_com_androidspaam_OGLESRenderer_updateTracking(JNIEnv *env, jobject obj)
{
    //LOG("Java_com_androidspaam_OGLESRenderer_GLRenderer_renderFrame");

    // Get the state from QCAR and mark the beginning of a rendering section
    QCAR::State state = QCAR::Renderer::getInstance().begin();
    

    // Explicitly render the Video Background
  //  QCAR::Renderer::getInstance().drawVideoBackground();

  //  if(QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON)

    jclass activityClass = env->GetObjectClass(obj);
    // Did we find any trackables this frame?
    for(int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
    {
        // Get the trackable:
        const QCAR::TrackableResult* result = state.getTrackableResult(tIdx);
        //const QCAR::Trackable& trackable = result->getTrackable();

        QCAR::Matrix44F modelViewMatrix =
            QCAR::Tool::convertPose2GLMatrix(result->getPose());

        //get the camera transformation
        //QCAR::Matrix44F inverseMV = MathUtil::Matrix44FInverse(modelViewMatrix);
        QCAR::Matrix44F invTranspMV = modelViewMatrix;
        //QCAR::Matrix44F invTranspMV = MathUtil::Matrix44FTranspose(inverseMV);

        //get position
        float cam_x = invTranspMV.data[12];
        float cam_y = -invTranspMV.data[13];
        float cam_z = -invTranspMV.data[14];

        //get rotation
        float transform0 = invTranspMV.data[0];
        float transform1 = invTranspMV.data[1];
        float transform2 = invTranspMV.data[2];
        float transform3 = invTranspMV.data[3];
        float transform4 = invTranspMV.data[4];
        float transform5 = invTranspMV.data[5];
        float transform6 = invTranspMV.data[6];
        float transform7 = invTranspMV.data[7];
        float transform8 = invTranspMV.data[8];
        float transform9 = invTranspMV.data[9];
        float transform10 = invTranspMV.data[10];
        float transform11 = invTranspMV.data[11];
        float transform12 = invTranspMV.data[12];
        float transform13 = invTranspMV.data[13];
        float transform14 = invTranspMV.data[14];
        float transform15 = invTranspMV.data[15];

        /*
        //get perspective transformation
        float nearPlane = 1.0f;
        float farPlane = 1000.0f;
        const QCAR::CameraCalibration& cameraCalibration =
                                    QCAR::CameraDevice::getInstance().getCameraCalibration();

        QCAR::VideoBackgroundConfig config = QCAR::Renderer::getInstance().getVideoBackgroundConfig();

        float viewportWidth = config.mSize.data[0];
        float viewportHeight = config.mSize.data[1];

        QCAR::Vec2F size = cameraCalibration.getSize();
        QCAR::Vec2F focalLength = cameraCalibration.getFocalLength();
        float fovRadians = 2 * atan(0.5f * (size.data[1] / focalLength.data[1]));
        float fovDegrees = fovRadians * 180.0f / M_PI;
        float aspectRatio=(size.data[0]/size.data[1]);

        //adjust for screen vs camera size distorsion
        float viewportDistort=1.0;

        if (viewportWidth != screenWidth)
        {
        	viewportDistort = viewportWidth / (float) screenWidth;
            fovDegrees=fovDegrees*viewportDistort;
            aspectRatio=aspectRatio/viewportDistort;
        }

        if (viewportHeight != screenHeight)
        {
        	viewportDistort = viewportHeight / (float) screenHeight;
            fovDegrees=fovDegrees/viewportDistort;
            aspectRatio=aspectRatio*viewportDistort;
        }

        //JNIEnv *env;
        //jvm->AttachCurrentThread((void **)&env, NULL);

        jclass activityClass = env->GetObjectClass(obj);
        jmethodID setCameraPerspectiveMethod = env->GetMethodID(activityClass,"setCameraPerspectiveNative", "(FF)V");
        env->CallVoidMethod(obj,setCameraPerspectiveMethod,fovDegrees,aspectRatio);

        // jclass activityClass = env->GetObjectClass(obj);
        jmethodID setCameraViewportMethod = env->GetMethodID(activityClass,"setCameraViewportNative", "(FFFF)V");
        env->CallVoidMethod(obj,setCameraViewportMethod,viewportWidth,viewportHeight,cameraCalibration.getSize().data[0],cameraCalibration.getSize().data[1]);

        //JNIEnv *env;
        //jvm->AttachCurrentThread((void **)&env, NULL);
		*/
        //jclass activityClass = env->GetObjectClass(obj);
        jmethodID setCameraPoseMethod = env->GetMethodID(activityClass,"setCameraPoseNative", "(FFF)V");
        env->CallVoidMethod(obj,setCameraPoseMethod,cam_x,cam_y,cam_z);

        //jclass activityClass = env->GetObjectClass(obj);
        jmethodID setCameraOrientationMethod = env->GetMethodID(activityClass,"setCameraOrientationNative", "(FFFFFFFFFFFFFFFF)V");
        env->CallVoidMethod(obj,setCameraOrientationMethod, transform0, transform1, transform2, transform3, transform4, transform5,
        		transform6, transform7, transform8, transform9, transform10, transform11, transform12, transform13, transform14,
        		transform15);

       // jvm->DetachCurrentThread();

       // LOG("Got tracking...");
    }

	//jclass activityClass = env->GetObjectClass( obj );
	jmethodID setTrackedMethod = env->GetMethodID( activityClass, "setTrackedNative", "(Z)V");
	env->CallVoidMethod(obj, setTrackedMethod, !(state.getNumTrackableResults() == 0) );


    QCAR::Renderer::getInstance().end();
}


void
configureVideoBackground()
{
    // Get the default video mode:
    QCAR::CameraDevice& cameraDevice = QCAR::CameraDevice::getInstance();
    QCAR::VideoMode videoMode = cameraDevice.
                                getVideoMode(QCAR::CameraDevice::MODE_DEFAULT);


    // Configure the video background
    QCAR::VideoBackgroundConfig config;

    config.mEnabled = false;

    config.mSynchronous = true;
    config.mPosition.data[0] = 0.0f;
    config.mPosition.data[1] = 0.0f;
    
    if (isActivityInPortraitMode)
    {
        //LOG("configureVideoBackground PORTRAIT");
        config.mSize.data[0] = videoMode.mHeight
                                * (screenHeight / (float)videoMode.mWidth);
        config.mSize.data[1] = screenHeight;

        if(config.mSize.data[0] < screenWidth)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenWidth;
            config.mSize.data[1] = screenWidth * 
                              (videoMode.mWidth / (float)videoMode.mHeight);
        }
    }
    else
    {
        //LOG("configureVideoBackground LANDSCAPE");
        config.mSize.data[0] = screenWidth;
        config.mSize.data[1] = videoMode.mHeight
                            * (screenWidth / (float)videoMode.mWidth);

        if(config.mSize.data[1] < screenHeight)
        {
            LOG("Correcting rendering background size to handle missmatch between screen and video aspect ratios.");
            config.mSize.data[0] = screenHeight
                                * (videoMode.mWidth / (float)videoMode.mHeight);
            config.mSize.data[1] = screenHeight;
        }
    }

    LOG("Configure Video Background : Video (%d,%d), Screen (%d,%d), mSize (%d,%d)", videoMode.mWidth, videoMode.mHeight, screenWidth, screenHeight, config.mSize.data[0], config.mSize.data[1]);

    // Set the config:
    QCAR::Renderer::getInstance().setVideoBackgroundConfig(config);
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_initApplicationNative(
                            JNIEnv* env, jobject obj, jint width, jint height)
{
    LOG("Java_com_androidspaam_SPAAM_initApplicationNative");
    
    // Store screen dimensions
    screenWidth = width;
    screenHeight = height;
        
    // Handle to the activity class:

    env->GetJavaVM(&javaVM);
    activityObj = env->NewGlobalRef(obj);
    LOG("Java_com_androidspaam_SPAAM_initApplicationNative finished");
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_deinitApplicationNative(
                                                        JNIEnv* env, jobject obj)
{
    LOG("Java_com_androidspaam_SPAAM_deinitApplicationNative");


}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_startCamera(JNIEnv *,
                                                                         jobject)
{
    LOG("Java_com_androidspaam_SPAAM_startCamera");
    
    // Select the camera to open, set this to QCAR::CameraDevice::CAMERA_FRONT 
    // to activate the front camera instead.
    QCAR::CameraDevice::CAMERA camera = QCAR::CameraDevice::CAMERA_DEFAULT;

    // Initialize the camera:
    if (!QCAR::CameraDevice::getInstance().init(camera))
        return;

    // Configure the video background
    configureVideoBackground();

    // Select the default mode:
    if (!QCAR::CameraDevice::getInstance().selectVideoMode(
                                QCAR::CameraDevice::MODE_DEFAULT))
        return;

  //  QCAR::VideoMode videoMode = cameraDevice.
   //                      getVideoMode(QCAR::CameraDevice::MODE_OPTIMIZE_QUALITY);

    // Start the camera:
    if (!QCAR::CameraDevice::getInstance().start())
        return;

    QCAR::setFrameFormat(QCAR::RGB565, true);
    // Uncomment to enable flash
    //if(QCAR::CameraDevice::getInstance().setFlashTorchMode(true))
    //	LOG("IMAGE TARGETS : enabled torch");

    // Uncomment to enable infinity focus mode, or any other supported focus mode
    // See CameraDevice.h for supported focus modes
    //if(QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_INFINITY))
    //	LOG("IMAGE TARGETS : enabled infinity focus");

    // Start the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::MarkerTracker* markerTracker = static_cast<QCAR::MarkerTracker*>(trackerManager.getTracker(QCAR::MarkerTracker::getClassType()));

    //QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    if(markerTracker != 0)
        markerTracker->start();
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_stopCamera(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_stopCamera");

    // Stop the tracker:
    QCAR::TrackerManager& trackerManager = QCAR::TrackerManager::getInstance();
    QCAR::MarkerTracker* markerTracker = static_cast<QCAR::MarkerTracker*>(trackerManager.getTracker(QCAR::MarkerTracker::getClassType()));
    //QCAR::ImageTracker* imageTracker = static_cast<QCAR::ImageTracker*>(trackerManager.getTracker(QCAR::ImageTracker::getClassType()));
    if(markerTracker != 0)
        markerTracker->stop();
    
    QCAR::CameraDevice::getInstance().stop();
    QCAR::CameraDevice::getInstance().deinit();
}


JNIEXPORT void JNICALL
Java_com_androidspaam_SPAAM_setProjectionMatrix(JNIEnv *, jobject)
{
    LOG("Java_com_androidspaam_SPAAM_setProjectionMatrix");

    // Cache the projection matrix:
    const QCAR::CameraCalibration& cameraCalibration =
                                QCAR::CameraDevice::getInstance().getCameraCalibration();
    projectionMatrix = QCAR::Tool::getProjectionGL(cameraCalibration, 2.0f, 2500.0f);
}

// ----------------------------------------------------------------------------
// Activates Camera Flash
// ----------------------------------------------------------------------------
JNIEXPORT jboolean JNICALL
Java_com_androidspaam_SPAAM_activateFlash(JNIEnv*, jobject, jboolean flash)
{
    return QCAR::CameraDevice::getInstance().setFlashTorchMode((flash==JNI_TRUE)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_androidspaam_SPAAM_autofocus(JNIEnv*, jobject)
{
    return QCAR::CameraDevice::getInstance().setFocusMode(QCAR::CameraDevice::FOCUS_MODE_TRIGGERAUTO) ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT jboolean JNICALL
Java_com_androidspaam_SPAAM_setFocusMode(JNIEnv*, jobject, jint mode)
{
    int qcarFocusMode;

    switch ((int)mode)
    {
        case 0:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_NORMAL;
            break;
        
        case 1:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_CONTINUOUSAUTO;
            break;
            
        case 2:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_INFINITY;
            break;
            
        case 3:
            qcarFocusMode = QCAR::CameraDevice::FOCUS_MODE_MACRO;
            break;
    
        default:
            return JNI_FALSE;
    }
    
    return QCAR::CameraDevice::getInstance().setFocusMode(qcarFocusMode) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_androidspaam_OGLESRenderer_initTracking(
                        JNIEnv* env, jobject obj, jint width, jint height)
{
    LOG("Java_com_androidspaam_OGLESRenderer_initTracking");

    // Update screen dimensions
    screenWidth = width;
    screenHeight = height;

    // Reconfigure the video background
    configureVideoBackground();
}


#ifdef __cplusplus
}
#endif
