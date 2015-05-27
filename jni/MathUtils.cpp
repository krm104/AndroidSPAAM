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
 * This class is based on the SampleMath.cpp and SampleUtils.cpp from the ImageTarget example
 */

#include <math.h>
#include <stdlib.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "MathUtils.h"


QCAR::Matrix44F
MathUtil::Matrix44FIdentity()
{
    QCAR::Matrix44F r;
    
    for (int i = 0; i < 16; i++)
        r.data[i] = 0.0f;
    
    r.data[0] = 1.0f;
    r.data[5] = 1.0f;
    r.data[10] = 1.0f;
    r.data[15] = 1.0f;
    
    return r;
}


QCAR::Matrix44F
MathUtil::Matrix44FTranspose(QCAR::Matrix44F m)
{
    QCAR::Matrix44F r;
    for (int i = 0; i < 4; i++)
        for (int j = 0; j < 4; j++)
            r.data[i*4+j] = m.data[i+4*j];
    return r;
}


float
MathUtil::Matrix44FDeterminate(QCAR::Matrix44F& m)
{
    return  m.data[12] * m.data[9] * m.data[6] * m.data[3] - m.data[8] * m.data[13] * m.data[6] * m.data[3] -
            m.data[12] * m.data[5] * m.data[10] * m.data[3] + m.data[4] * m.data[13] * m.data[10] * m.data[3] +
            m.data[8] * m.data[5] * m.data[14] * m.data[3] - m.data[4] * m.data[9] * m.data[14] * m.data[3] -
            m.data[12] * m.data[9] * m.data[2] * m.data[7] + m.data[8] * m.data[13] * m.data[2] * m.data[7] +
            m.data[12] * m.data[1] * m.data[10] * m.data[7] - m.data[0] * m.data[13] * m.data[10] * m.data[7] -
            m.data[8] * m.data[1] * m.data[14] * m.data[7] + m.data[0] * m.data[9] * m.data[14] * m.data[7] +
            m.data[12] * m.data[5] * m.data[2] * m.data[11] - m.data[4] * m.data[13] * m.data[2] * m.data[11] -
            m.data[12] * m.data[1] * m.data[6] * m.data[11] + m.data[0] * m.data[13] * m.data[6] * m.data[11] +
            m.data[4] * m.data[1] * m.data[14] * m.data[11] - m.data[0] * m.data[5] * m.data[14] * m.data[11] -
            m.data[8] * m.data[5] * m.data[2] * m.data[15] + m.data[4] * m.data[9] * m.data[2] * m.data[15] +
            m.data[8] * m.data[1] * m.data[6] * m.data[15] - m.data[0] * m.data[9] * m.data[6] * m.data[15] -
            m.data[4] * m.data[1] * m.data[10] * m.data[15] + m.data[0] * m.data[5] * m.data[10] * m.data[15] ;
}


QCAR::Matrix44F
MathUtil::Matrix44FInverse(QCAR::Matrix44F& m)
{
    QCAR::Matrix44F r;
    
    float det = 1.0f / Matrix44FDeterminate(m);
    
    r.data[0]   = m.data[6]*m.data[11]*m.data[13] - m.data[7]*m.data[10]*m.data[13]
                + m.data[7]*m.data[9]*m.data[14] - m.data[5]*m.data[11]*m.data[14]
                - m.data[6]*m.data[9]*m.data[15] + m.data[5]*m.data[10]*m.data[15];
    
    r.data[4]   = m.data[3]*m.data[10]*m.data[13] - m.data[2]*m.data[11]*m.data[13]
                - m.data[3]*m.data[9]*m.data[14] + m.data[1]*m.data[11]*m.data[14]
                + m.data[2]*m.data[9]*m.data[15] - m.data[1]*m.data[10]*m.data[15];
    
    r.data[8]   = m.data[2]*m.data[7]*m.data[13] - m.data[3]*m.data[6]*m.data[13]
                + m.data[3]*m.data[5]*m.data[14] - m.data[1]*m.data[7]*m.data[14]
                - m.data[2]*m.data[5]*m.data[15] + m.data[1]*m.data[6]*m.data[15];
    
    r.data[12]  = m.data[3]*m.data[6]*m.data[9] - m.data[2]*m.data[7]*m.data[9]
                - m.data[3]*m.data[5]*m.data[10] + m.data[1]*m.data[7]*m.data[10]
                + m.data[2]*m.data[5]*m.data[11] - m.data[1]*m.data[6]*m.data[11];
    
    r.data[1]   = m.data[7]*m.data[10]*m.data[12] - m.data[6]*m.data[11]*m.data[12]
                - m.data[7]*m.data[8]*m.data[14] + m.data[4]*m.data[11]*m.data[14]
                + m.data[6]*m.data[8]*m.data[15] - m.data[4]*m.data[10]*m.data[15];
    
    r.data[5]   = m.data[2]*m.data[11]*m.data[12] - m.data[3]*m.data[10]*m.data[12]
                + m.data[3]*m.data[8]*m.data[14] - m.data[0]*m.data[11]*m.data[14]
                - m.data[2]*m.data[8]*m.data[15] + m.data[0]*m.data[10]*m.data[15];
    
    r.data[9]   = m.data[3]*m.data[6]*m.data[12] - m.data[2]*m.data[7]*m.data[12]
                - m.data[3]*m.data[4]*m.data[14] + m.data[0]*m.data[7]*m.data[14]
                + m.data[2]*m.data[4]*m.data[15] - m.data[0]*m.data[6]*m.data[15];
    
    r.data[13]  = m.data[2]*m.data[7]*m.data[8] - m.data[3]*m.data[6]*m.data[8]
                + m.data[3]*m.data[4]*m.data[10] - m.data[0]*m.data[7]*m.data[10]
                - m.data[2]*m.data[4]*m.data[11] + m.data[0]*m.data[6]*m.data[11];
    
    r.data[2]   = m.data[5]*m.data[11]*m.data[12] - m.data[7]*m.data[9]*m.data[12]
                + m.data[7]*m.data[8]*m.data[13] - m.data[4]*m.data[11]*m.data[13]
                - m.data[5]*m.data[8]*m.data[15] + m.data[4]*m.data[9]*m.data[15];
    
    r.data[6]   = m.data[3]*m.data[9]*m.data[12] - m.data[1]*m.data[11]*m.data[12]
                - m.data[3]*m.data[8]*m.data[13] + m.data[0]*m.data[11]*m.data[13]
                + m.data[1]*m.data[8]*m.data[15] - m.data[0]*m.data[9]*m.data[15];
    
    r.data[10]  = m.data[1]*m.data[7]*m.data[12] - m.data[3]*m.data[5]*m.data[12]
                + m.data[3]*m.data[4]*m.data[13] - m.data[0]*m.data[7]*m.data[13]
                - m.data[1]*m.data[4]*m.data[15] + m.data[0]*m.data[5]*m.data[15];
    
    r.data[14]  = m.data[3]*m.data[5]*m.data[8] - m.data[1]*m.data[7]*m.data[8]
                - m.data[3]*m.data[4]*m.data[9] + m.data[0]*m.data[7]*m.data[9]
                + m.data[1]*m.data[4]*m.data[11] - m.data[0]*m.data[5]*m.data[11];
    
    r.data[3]   = m.data[6]*m.data[9]*m.data[12] - m.data[5]*m.data[10]*m.data[12]
                - m.data[6]*m.data[8]*m.data[13] + m.data[4]*m.data[10]*m.data[13]
                + m.data[5]*m.data[8]*m.data[14] - m.data[4]*m.data[9]*m.data[14];
    
    r.data[7]  = m.data[1]*m.data[10]*m.data[12] - m.data[2]*m.data[9]*m.data[12]
                + m.data[2]*m.data[8]*m.data[13] - m.data[0]*m.data[10]*m.data[13] 
                - m.data[1]*m.data[8]*m.data[14] + m.data[0]*m.data[9]*m.data[14];
    
    r.data[11]  = m.data[2]*m.data[5]*m.data[12] - m.data[1]*m.data[6]*m.data[12]
                - m.data[2]*m.data[4]*m.data[13] + m.data[0]*m.data[6]*m.data[13]
                + m.data[1]*m.data[4]*m.data[14] - m.data[0]*m.data[5]*m.data[14];
    
    r.data[15]  = m.data[1]*m.data[6]*m.data[8] - m.data[2]*m.data[5]*m.data[8]
                + m.data[2]*m.data[4]*m.data[9] - m.data[0]*m.data[6]*m.data[9]
                - m.data[1]*m.data[4]*m.data[10] + m.data[0]*m.data[5]*m.data[10];
    
    for (int i = 0; i < 16; i++)
        r.data[i] *= det;
    
    return r;
}

void
MathUtil::rotatePoseMatrix(float angle, float x, float y, float z,
                              float* matrix)
{
    // Sanity check
    if (!matrix)
        return;

    float rotate_matrix[16];
    MathUtil::setRotationMatrix(angle, x, y, z, rotate_matrix);

    // matrix * scale_matrix
    MathUtil::multiplyMatrix(matrix, rotate_matrix, matrix);
}



void
MathUtil::multiplyMatrix(float *matrixA, float *matrixB, float *matrixC)
{
    int i, j, k;
    float aTmp[16];

    for (i = 0; i < 4; i++)
    {
        for (j = 0; j < 4; j++)
        {
            aTmp[j * 4 + i] = 0.0;

            for (k = 0; k < 4; k++)
                aTmp[j * 4 + i] += matrixA[k * 4 + i] * matrixB[j * 4 + k];
        }
    }

    for (i = 0; i < 16; i++)
        matrixC[i] = aTmp[i];
}


void
MathUtil::setRotationMatrix(float angle, float x, float y, float z,
    float *matrix)
{
    double radians, c, s, c1, u[3], length;
    int i, j;

    radians = (angle * M_PI) / 180.0;

    c = cos(radians);
    s = sin(radians);

    c1 = 1.0 - cos(radians);

    length = sqrt(x * x + y * y + z * z);

    u[0] = x / length;
    u[1] = y / length;
    u[2] = z / length;

    for (i = 0; i < 16; i++)
        matrix[i] = 0.0;

    matrix[15] = 1.0;

    for (i = 0; i < 3; i++)
    {
        matrix[i * 4 + (i + 1) % 3] = u[(i + 2) % 3] * s;
        matrix[i * 4 + (i + 2) % 3] = -u[(i + 1) % 3] * s;
    }

    for (i = 0; i < 3; i++)
    {
        for (j = 0; j < 3; j++)
            matrix[i * 4 + j] += c1 * u[i] * u[j] + (i == j ? c : 0.0);
    }
}
