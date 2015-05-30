# AndroidSPAAM
A Single Point Active Alignment Method implementation created to calibrate the left and right eye perspective projections for the Epson Moverio BT-200 Optical See-Through Head-Mounted Display

The intention of this program is to allow a user to calibrate the left and right eye view of the Epson Moverio, save the calibration result to the device, and be able to use other programs which read those calibration files to produce correctly registered AR content.

USAGE: This software is free to use and modify by anyone. It is hoped that the accessibilty of this software will aid and encourage others interested in see-through display calibration to further the work and ease the development of other projects.

CONTENTS: There are 3 main parts to this software
    1: The source code - This project is intended for use on the Epson Moverio BT-200, and therefore the source code is written in Java for use on the Android platform.
    2: Third Party libraries - This project requires hat several third party libraries be inclded which provide necessary functionality. These libraries are: Vuforia (used for marker tracking), JAMA (used for linear algebra solving), Moverio SDK (used to control some screen settings of the Moverio device)
    3: 
