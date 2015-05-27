/* MathUtils - VuforiaJME Example
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
 * This class is based on the SampleMath.h and SampleUtils.h from the Vuforia ImageTarget example
 */

#ifndef _QCAR_MATHUTILS_H_
#define _QCAR_MATHUTILS_H_

// Includes:
#include <stdio.h>
#include <android/log.h>

// Includes:
#include <QCAR/Tool.h>

// Utility for logging:
#define LOG_TAG    "QCARJME3"
#define LOG(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


/// A utility class used by the QCAR SDK samples.
class MathUtil
{
public:

    static QCAR::Matrix44F Matrix44FIdentity();
    
    static QCAR::Matrix44F Matrix44FTranspose(QCAR::Matrix44F m);
    
    static float Matrix44FDeterminate(QCAR::Matrix44F& m);

    static QCAR::Matrix44F Matrix44FInverse(QCAR::Matrix44F& m);

    /// Set the rotation components of this 4x4 matrix.
    static void setRotationMatrix(float angle, float x, float y, float z,
        float *nMatrix);

    /// Applies a rotation.
    static void rotatePoseMatrix(float angle, float x, float y, float z,
        float* nMatrix = NULL);

    /// Multiplies the two matrices A and B and writes the result to C.
    static void multiplyMatrix(float *matrixA, float *matrixB,
        float *matrixC);
};

#endif // _QCAR_MATHUTILS_H_
