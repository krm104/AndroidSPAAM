/**************************************************************************************
 * Author: Kenneth Moser
 * email: moserk@acm.org
 * Affiliation: Mississippi State University
 * 
 * Description: This file contains the Class OGLESRenderer which manages the display 
 * of the on-screen calibration patterns as well as the transmition of the pixel 
 * and 3D point locations (obtained from Vuforia) to the JAMA SVD operation.
 * 
 * This file contains all of the functions used by the OpenGL renderer and also the
 * tap event callback functions as well as the functions related to reading/writing 
 * the calibration files for each eye.
 *************************************************************************************/

/******Package Name******/
package com.androidspaam;

/******Static Library imports required by the openGL ES 2.0 function calls******/
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.epson.moverio.bt200.DisplayControl;
import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Environment;
import android.util.Log;

import com.androidspaam.util.ShaderHelper;
import com.androidspaam.util.TextResourceReader;
import com.spaam.util.spaamutil.SPAAM_SVD;
import com.spaam.util.spaamutil.SPAAM_SVD.Correspondence_Pair;
/******Java specific Libraries******/
/******Android Specific Libraries******/
/******Qualcomm Specific Libraries required by Vuforia******/
/******Epson Specific Libraries******/
/**********************************************************************
 * Class: OGLESRenderer
 * Extends: Activity
 * Implements: Renderer
 * Description: This class controls the display of the calibration
 * pattern as well as the transmission of the pixel and 3D point locations
 * used to generate the 3x4 SPAAM projection matrix resulting from the 
 * SVD operation (and conversion of this matrix into the 4x4 used by OpenGL).
 * The tap event callback functions are also defined within this activity
 * as well.
 *********************************************************************/
public class OGLESRenderer extends Activity implements Renderer {
	final int SCREENWIDTH = 960; //full screen width for each eye of the Moverio device//
	final int SCREENHEIGHT = 540; //full screen height for each eye of the Moverio device//
	
	/******Members for openGL ES rendering******/
	//Members Specific to the Calibration Grid//
	private static final int CROSS_POSITION_COMPONENT_COUNT = 2;
	private static final int BYTES_PER_FLOAT = 4;
	private static final String U_COLOR = "u_Color";
	private static final String A_POSITION = "a_Position";
	
	/*******************************************************************************************
	 * These are the static locations for the onscreen crosses used for the SPAAM alignments.
	 * Ideally these points would be created at run time, but for now they are hard coded to these
	 * location. There is a 5x5  (25 total) arrangement of crosshairs such that they are equally
	 * distributed vertically and horizontally aross the display screen.
	 *******************************************************************************************/
	float[] crossVertices = {		//Row One//
									-394f/960f,196.8f/540f,-374f/960f,196.8f/540f,-384f/960f,186.8f/540f,-384f/960f,206.8f/540f,
									-202f/960f,196.8f/540f,-182f/960f,196.8f/540f,-192f/960f,186.8f/540f,-192f/960f,206.8f/540f,
									-10f/960f,196.8f/540f,10f/960f,196.8f/540f,0f/960f,186.8f/540f,0f/960f,206.8f/540f,
									182f/960f,196.8f/540f,202f/960f,196.8f/540f,192f/960f,186.8f/540f,192f/960f,206.8f/540f,
									374f/960f,196.8f/540f,394f/960f,196.8f/540f,384f/960f,186.8f/540f,384f/960f,206.8f/540f,
									//Row Two//
									-394f/960f,98.4f/540f,-374f/960f,98.4f/540f,-384f/960f,88.4f/540f,-384f/960f,108.4f/540f,
									-202f/960f,98.4f/540f,-182f/960f,98.4f/540f,-192f/960f,88.4f/540f,-192f/960f,108.4f/540f,
									-10f/960f,98.4f/540f,10f/960f,98.4f/540f,0f/960f,88.4f/540f,0f/960f,108.4f/540f,
									182f/960f,98.4f/540f,202f/960f,98.4f/540f,192f/960f,88.4f/540f,192f/960f,108.4f/540f,
									374f/960f,98.4f/540f,394f/960f,98.4f/540f,384f/960f,88.4f/540f,384f/960f,108.4f/540f,
									//Row Three//
									-394f/960f,0f/540f,-374f/960f,0f/540f,-384f/960f,-10f/540f,-384f/960f,10f/540f,
									-202f/960f,0f/540f,-182f/960f,0f/540f,-192f/960f,-10f/540f,-192f/960f,10f/540f,
									-10f/960f,0f/540f,10f/960f,0f/540f,0f/960f,-10f/540f,0f/960f,10f/540f,
									182f/960f,0f/540f,202f/960f,0f/540f,192f/960f,-10f/540f,192f/960f,10f/540f,
									374f/960f,0f/540f,394f/960f,0f/540f,384f/960f,-10f/540f,384f/960f,10f/540f,
									//Row Four//
									-394f/960f,-98.4f/540f,-374f/960f,-98.4f/540f,-384f/960f,-108.4f/540f,-384f/960f,-88.4f/540f,
									-202f/960f,-98.4f/540f,-182f/960f,-98.4f/540f,-192f/960f,-108.4f/540f,-192f/960f,-88.4f/540f,
									-10f/960f,-98.4f/540f,10f/960f,-98.4f/540f,0f/960f,-108.4f/540f,0f/960f,-88.4f/540f,
									182f/960f,-98.4f/540f,202f/960f,-98.4f/540f,192f/960f,-108.4f/540f,192f/960f,-88.4f/540f,
									374f/960f,-98.4f/540f,394f/960f,-98.4f/540f,384f/960f,-108.4f/540f,384f/960f,-88.4f/540f,
									//Row Five//
									-394f/960f,-196.8f/540f,-374f/960f,-196.8f/540f,-384f/960f,-206.8f/540f,-384f/960f,-186.8f/540f,
									-202f/960f,-196.8f/540f,-182f/960f,-196.8f/540f,-192f/960f,-206.8f/540f,-192f/960f,-186.8f/540f,
									-10f/960f,-196.8f/540f,10f/960f,-196.8f/540f,0f/960f,-206.8f/540f,0f/960f,-186.8f/540f,
									182f/960f,-196.8f/540f,202f/960f,-196.8f/540f,192f/960f,-206.8f/540f,192f/960f,-186.8f/540f,
									374f/960f,-196.8f/540f,394f/960f,-196.8f/540f,384f/960f,-206.8f/540f,384f/960f,-186.8f/540f,};
	int crossNum = -4;
	int crossCount = 0;
	private final FloatBuffer crossVertexData;
	private int crossProgram;
	private int uCrossColorLocation;
	private int aCrossPositionLocation;

	//Members Specific to the Verification Square//
	private static final int SQUARE_POSITION_COMPONENT_COUNT = 3;
	private static final String U_PROJECTION = "u_Projection";
	private static final String U_TRANSFORM = "u_Transform";
	//These are the vertices describing the location of the corners of the verification square//
	//This should eventually be updated to a verification cube//
	float[] squareVertices= {//Top Edge//
									-0.10f, 0.10f, 0.0f, 0.10f, 0.10f, 0.0f,
									//Left Edge//
									-0.10f, 0.10f, 0.0f, -0.10f, -0.10f, 0.0f,
									//Right Edge//
									0.10f, 0.10f, 0.0f, 0.10f, -0.10f, 0.0f,
									//Bottom Edge//
									-0.10f, -0.10f, 0.0f, 0.10f, -0.10f, 0.0f
									};
	
	
	//Members which define the started values for the projection matrices used to render the left
	//and right eye viewpoint as well as the transformation needed to properly locate the verification
	//square (the location obtained via the Vuforia marker tracker. These values simply create identity
	//matrices in order to initial the members for later use//
	float[] u_ProjectionLeft = {1f, 0f, 0f, 0f,
							0f, 1, 0f, 0f,
							0f, 0f, 1f, 0f,
							0f, 0f, 0f, 1f};
	float[] u_ProjectionRight = {1f, 0f, 0f, 0f,
								0f, 1, 0f, 0f,
								0f, 0f, 1f, 0f,
								0f, 0f, 0f, 1f};
	float[] u_Transform = {1.0f, 0.0f, 0.0f, 0.0f,
							0.0f, 1.0f, 0.0f, 0.0f,
							0.0f, 0.0f, 1.0f, 0.0f,
							0.0f, 0.0f, 0.0f, 1.0f};
	
	//Members used to store handles to the various attributes needed by the shaders//
	private final FloatBuffer squareVertexData;
	private int squareProgram;
	private int uSquareColorLocation;
	private int aSquarePositionLocation;
	private int uProjectionLocation;
	private int uTransformLocation;
	
	//Members Specific to the Rendering Window//
	int WIDTH = 0;
	int HEIGHT = 0;
	boolean tracking = false;
	boolean eye = false;
	boolean file = false;
	
	/////////////Get Moverio Display Controller///
	DisplayControl mDisplayControl = null;

	private final Context context;
	
	//Members to store the x, y, z, location of the 3D world alignment point (center of the tracking marker)//
	float cam_x = 0f;
	float cam_y = 0f;
	float cam_z = 0f;
	
	//////////////////////////
	SPAAM_SVD svd = new SPAAM_SVD();
	File SPAAM_File = null;
	File calibFile = null;
	//////////////////////////
	
	////////Functions for Handling File Access///////
	//// Checks if external storage is available for read and write ////
	public boolean isExternalStorageWritable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	        return true;
	    }
	    return false;
	}

	//// Checks if external storage is available to at least read ////
	public boolean isExternalStorageReadable() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state) ||
	        Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        return true;
	    }
	    return false;
	}
	
	//// Function for Accessing a Public Directory File ////
	public File getAlbumStorageDir(String albumName) {
	    // Get the directory for the user's public pictures directory. 
	    File file = new File(Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_DOWNLOADS), albumName);
	    if (!file.mkdirs()) {
	        Log.e("SPAAM ACTIVITY", "Directory not created");
	    }
	    return file;
	}

	/////////////////////////////////////////////////////
	//////////////Vuforio Native Functions///////////////
	/** Native function for initializing the renderer. */
    public native void initTracking(int width, int height);

    /** Native function to update the renderer. */
    public native void updateTracking();
    //////////////////////
	
    /****************************************************************
     * @param eye - the eye chosen for calibration
     * @throws IOException - exception thrown when file cannot be accessed
     * 
     * This function checks if the calibration file for the selected
     * eye is already created and can be read from. If it is not
     * already created, then the file is created.
     ***************************************************************/
	public void SetupFileFunc( boolean eye) throws IOException
	{
		//Is the storage of the device readable//
		if (isExternalStorageReadable() && isExternalStorageWritable()) {
			File storageDirectory = getAlbumStorageDir("SPAAM_Calib");
			if ( storageDirectory != null ){
				if ( storageDirectory.listFiles() != null )
				{
					file = true;
					calibFile = null;
				
					//Check if the file for the chosen eye exists. If the file does not exist, create it//
					//Right eye//
					if ( eye ) {
						//Check if file exists//
						for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
							if ( storageDirectory.listFiles()[i].getName() == "Right.calib" ){
								calibFile = storageDirectory.listFiles()[i];
								break;
							}
						}
						//file does not exist//
						if ( calibFile == null )
						{
							calibFile = new File(storageDirectory.getAbsolutePath() + "/Right.calib");
							calibFile.createNewFile();
						}
					}//Left eye//
					else {
						//Check if file exists//
						for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
							if ( storageDirectory.listFiles()[i].getName() == "Left.calib" ){
								calibFile = storageDirectory.listFiles()[i];
								break;
							}
						}
						//file does not exist//
						if ( calibFile == null )
						{
							calibFile = new File(storageDirectory.getAbsolutePath() + "/Left.calib");
							calibFile.createNewFile();
						}
					}
					
					//Attempt to read from the file. If the file is empty (just created, nothing is read)//
					RandomAccessFile rac_file = new RandomAccessFile(calibFile.getAbsolutePath(), "r");
					if ( rac_file.length() >= 16*8 )
					{
						//Store the calibration result into the correct projection for the selected eye//
						if ( eye ){
							for ( int i = 0; i < 16; i++ )
							{
				                u_ProjectionRight[i] = (float)rac_file.readDouble();
							}
						}else{
							for ( int i = 0; i < 16; i++ )
							{
				                u_ProjectionLeft[i] = (float)rac_file.readDouble();
							}
						}
					}
					rac_file.close();
				}
			}
		}//File Storage could not be accessed
		else{
			file = false;
		}
	}

	/***************************************************************
	 * @throws IOException - exception thrown when the file cannot be accessed
	 * 
	 * This function attempts to write the calibration results to the correct
	 * file for the selected eye. The results are written as doubles in
	 * row major order (4x4 OpenGL matrix).
	 **************************************************************/
	public void WriteFileFunc( ) throws IOException{
		//Open the file for read/write access//
		RandomAccessFile rac_file = new RandomAccessFile(calibFile.getAbsolutePath(), "rw");
		rac_file.setLength(0);
		//Write the Right eye results//
		if ( eye ){
			for ( int i = 0; i < 16; i++ )
			{
				rac_file.writeDouble((double)u_ProjectionRight[i]);
			}
			rac_file.close();
		}//Write the Left eye results//
		else{
			for ( int i = 0; i < 16; i++ )
			{
				rac_file.writeDouble((double)u_ProjectionLeft[i]);
			}
			rac_file.close();
		}
	}
	
	/***************************************************************
	 * @param context
	 * 
	 * This is the constructor for the OGLESRenderer class. It initializes
	 * the arrays required for rendering and also begins the Vuforio tracking engine.
	 ***************************************************************/
	public OGLESRenderer( Context context )
	{
		this.context = context;
		
		//Load Cross Vertex Data//
		for ( int i = 0; i < crossVertices.length; i++)
		{
			crossVertices[i]*= 2.0f;
		}
		crossVertexData = ByteBuffer.allocateDirect(crossVertices.length*BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		crossVertexData.put(crossVertices);
		crossCount = crossVertices.length/2;
	
		//Load Square Vertex Data//
		squareVertexData = ByteBuffer.allocateDirect(squareVertices.length*BYTES_PER_FLOAT)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		squareVertexData.put(squareVertices);
		
		//Setup the Vuforia Tracker//
		initTracking(SCREENWIDTH, SCREENHEIGHT);	
	}
	
	/****************************************************************
	 * This function is called when the OpenGL ES surface (the object
	 * to which the graphics are rendered, basically the display buffer)
	 * is initialized. I believe this is called after the construcor for the class.
	 * 
	 * The primary fucntion of this mehtod is to initialize all of the shader
	 * and OpenGL related handels and data.
	 ***************************************************************/
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		/////////////Get Moverio Display Controller///
		mDisplayControl = new DisplayControl(context);
		
		//Setup openGL Fixed Functionality Calls//
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		///////////////////////////////////
		
		//Setup the Cross Shaders//
		String vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_vertex_shader);
		String fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.simple_fragment_shader);
		int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
		int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
		crossProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);
		glUseProgram(crossProgram);
		uCrossColorLocation = glGetUniformLocation(crossProgram, U_COLOR);	
		aCrossPositionLocation = glGetAttribLocation(crossProgram, A_POSITION);
		crossVertexData.position(0);
		glVertexAttribPointer(aCrossPositionLocation, CROSS_POSITION_COMPONENT_COUNT, GL_FLOAT, false, 0, crossVertexData);
		
		//////////////////////////////////
		//Setup the Square Shaders//
		vertexShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.square_vertex_shader);
		fragmentShaderSource = TextResourceReader.readTextFileFromResource(context, R.raw.square_fragment_shader);
		vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
		fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
		squareProgram = ShaderHelper.linkProgram(vertexShader, fragmentShader);
		glUseProgram(squareProgram);
		uSquareColorLocation = glGetUniformLocation(squareProgram, U_COLOR);
		uProjectionLocation = glGetUniformLocation(squareProgram, U_PROJECTION);
		uTransformLocation = glGetUniformLocation(squareProgram, U_TRANSFORM);
		aSquarePositionLocation = glGetAttribLocation(squareProgram, A_POSITION);
		squareVertexData.position(0);
		glVertexAttribPointer(aSquarePositionLocation, 3, GL_FLOAT, false, 0, squareVertexData);
	}

	/***************************************************************
	 * This function is basically the callback for when the surface
	 * changes (either by a pause/resume call of the activity or
	 * if the surface is resized or loses and regains context).
	 * 
	 * It basically just handles resizing for this application.
	 **************************************************************/
	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		glViewport(0, 0, width, height);
		
		WIDTH = width;
		HEIGHT = height;
		
		///Set Moverio Display Mode to be 3D///
		mDisplayControl.setMode(DisplayControl.DISPLAY_MODE_3D, false);
	}

	/****************************************************************
	 * @param cam_x
	 * @param cam_y
	 * @param cam_z
	 * 
	 * This is a callback used by the Vuforia tracking engine. It passes
	 * the 3D position of the center of the tracking marker. Vuforia returns
	 * the distance in cm, hence why the position is diveded by 100.
	 ****************************************************************/
	public void setCameraPoseNative(float cam_x,float cam_y,float cam_z) {
		this.cam_x = cam_x/100.0f;
		this.cam_y = cam_y/100.0f;
		this.cam_z = cam_z/100.0f;
	}

	/********************************************************************
	 * @param transform0	 * @param transform1	 * @param transform2	 * @param transform3
	 * @param transform4	 * @param transform5	 * @param transform6	 * @param transform7
	 * @param transform8	 * @param transform9	 * @param transform10	 * @param transform11
	 * @param transform12	 * @param transform13	 * @param transform14	 * @param transform15
	 *
	 * This functions is a callback method used by the Vuforia tracking engine to pass the
	 * 3D pose of the marker in the head relative cooridnae frame. This pose is used for positioning
	 * and orienting the verification square overlaid on the marker during calibration.
	 ********************************************************************/
	public void setCameraOrientationNative(float transform0, float transform1, float transform2, float transform3,
			float transform4, float transform5, float transform6, float transform7, float transform8, float transform9,
			float transform10, float transform11, float transform12, float transform13, float transform14, float transform15) {
	 
		u_Transform[0] = transform0; u_Transform[1] = -transform1; u_Transform[2] = -transform2; u_Transform[3] = transform3;
		u_Transform[4] = transform4; u_Transform[5] = -transform5; u_Transform[6] = -transform6; u_Transform[7] = transform7;
		u_Transform[8] = transform8; u_Transform[9] = -transform9; u_Transform[10] = -transform10; u_Transform[11] = transform11;
		u_Transform[12] = transform12/100.0f; u_Transform[13] = -transform13/100.0f; u_Transform[14] = -transform14/100.0f; u_Transform[15] = transform15;
	
	}

	/**************************************************************************
	 * @param istracked
	 * 
	 * This is a callback function used by the Vuforia traking engine to indicate when
	 * the marker is being tracked. This is used to provide feedback to the user when it is
	 * okay to take a calibration reading (only when the marker is being tracked of course).
	 *************************************************************************/
	public void setTrackedNative(boolean istracked) {
		 tracking = istracked;
	}

	/**************************************************************************
	 * This function is basically the render function. It is called every time 
	 * a frame is rendered. It draws items based upon the eye chosen for calibration
	 * and also which cross is to be displayed.
	 *************************************************************************/
	@Override
	public void onDrawFrame(GL10 gl) {
		updateTracking();
		
		/////////////////////////////////////////////////////////////////////////
		//Left Eye//
		if ( !eye )
		{
			//Draw Left Eye//
			glViewport(0, 0, WIDTH/2, HEIGHT);
			//Reset the Display Buffers//
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			//Draw the Crosses//
			glUseProgram(crossProgram);
			crossVertexData.position(0);
			glVertexAttribPointer(aCrossPositionLocation, CROSS_POSITION_COMPONENT_COUNT, GL_FLOAT, false, 0, crossVertexData);
			glEnableVertexAttribArray(aCrossPositionLocation);
			if ( tracking && file )
				glUniform4f(uCrossColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);
			else if ( tracking )
				glUniform4f(uCrossColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
			else
				glUniform4f(uCrossColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
			glDrawArrays(GL_LINES, java.lang.Math.max(crossNum, 0), crossCount);
			glDisableVertexAttribArray(aCrossPositionLocation);
			
			//Draw Square//
			glUseProgram(squareProgram);
			squareVertexData.position(0);
			glVertexAttribPointer(aSquarePositionLocation, SQUARE_POSITION_COMPONENT_COUNT, GL_FLOAT, false, 0, squareVertexData);
			glEnableVertexAttribArray(aSquarePositionLocation);
			if ( tracking && file )
				glUniform4f(uSquareColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);
			else if ( tracking )
				glUniform4f(uSquareColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
			else
				glUniform4f(uSquareColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
			
			//Send the matrix values to the shader and draw the vertex arrays//
			glUniformMatrix4fv(uProjectionLocation, 1, false, u_ProjectionLeft, 0);
			glUniformMatrix4fv(uTransformLocation, 1, false, u_Transform, 0);
			glDrawArrays(GL_LINES, 0, squareVertices.length/3);	
			glDisableVertexAttribArray(aSquarePositionLocation);
		}
		///////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////
		//Right Eye//
		if ( eye)
		{
			//Draw Right Eye//
			glViewport(WIDTH/2, 0, WIDTH/2, HEIGHT);
			//Reset the Display Buffers//
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			//Draw the Crosses//
			glUseProgram(crossProgram);
			crossVertexData.position(0);
			glVertexAttribPointer(aCrossPositionLocation, CROSS_POSITION_COMPONENT_COUNT, GL_FLOAT, false, 0, crossVertexData);
			glEnableVertexAttribArray(aCrossPositionLocation);
			if ( tracking && file )
				glUniform4f(uCrossColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);
			else if ( tracking )
				glUniform4f(uCrossColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
			else
				glUniform4f(uCrossColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);
				glDrawArrays(GL_LINES, java.lang.Math.max(crossNum, 0), crossCount);
				glDisableVertexAttribArray(aCrossPositionLocation);
			
			//Draw Square//
			glUseProgram(squareProgram);
			squareVertexData.position(0);
			glVertexAttribPointer(aSquarePositionLocation, SQUARE_POSITION_COMPONENT_COUNT, GL_FLOAT, false, 0, squareVertexData);
			glEnableVertexAttribArray(aSquarePositionLocation);
			if ( tracking && file )
				glUniform4f(uSquareColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);
			else if ( tracking )
				glUniform4f(uSquareColorLocation, 0.0f, 1.0f, 0.0f, 1.0f);
			else
				glUniform4f(uSquareColorLocation, 1.0f, 0.0f, 0.0f, 1.0f);

			//Send the matrix values to the shader and draw the vertex arrays//
			glUniformMatrix4fv(uProjectionLocation, 1, false, u_ProjectionRight, 0);
			glUniformMatrix4fv(uTransformLocation, 1, false, u_Transform, 0);
			glDrawArrays(GL_LINES, 0, squareVertices.length/3);	
			glDisableVertexAttribArray(aSquarePositionLocation);
		}
		///////////////////////////////////////////////////////////////////////////
	}

	/**************************************************************************
	 * @throws IOException
	 * 
	 * This function handles the tap event for the touchpad of the Moverio.
	 * It checks to make sure that the marker is being tracked and then checks
	 * the state (which cross) and passes the 2D pixel location of the cross
	 * and the 3D position data of the marker center to the SVD related math functions
	 * to solve for the SPAAM solution.
	 **************************************************************************/
	public void handleTouchPress() throws IOException{
		
		//Verify the marker is being tracker//
		if ( tracking )
		{
			/////This is checking if we are at the last cross (of the 25)/////
			if ( crossNum >= crossVertices.length/2-4 )
			{	//Make sure a single cross is displayed and not the full grid//
				if ( crossNum >= 0)
				{	
					//record the pixel and 3D point location data//
					svd.corr_points.add(new Correspondence_Pair(cam_x, cam_y, cam_z,
							crossVertices[crossNum*2+4]/2*960f + 480f, crossVertices[crossNum*2+1]/2f*540f + 270f));
					//Call the SVD function, a minimum of 6 points is required//
					if ( svd.projectionDLTImpl() ) {
						//Build the OpenGL 4x4 projection matrix with a near clip plane of .1 and far clip plane of 100//
						svd.BuildGLMatrix3x4(.1, 100.0, 960, 0, 540, 0);
						//write the calibration results to the proper file//
						if ( !eye ){
			                for (int i = 0; i < 16; i++) {
			                	u_ProjectionLeft[i] = (float)svd.projMat3x4[i];
			                }
		                } else {
			                for (int i = 0; i < 16; i++) {
			                	u_ProjectionRight[i] = (float)svd.projMat3x4[i];
			                }
		                }
						WriteFileFunc();
					}	
				}
				crossNum = -4;
				crossCount = crossVertices.length/2;
			}//This is any cross but the last cross//
			else {
				//Makre sure a single cross is displayed and not the full grid//
				if ( crossNum >= 0)
				{	
					//record the pixel and 3D point location data//
					svd.corr_points.add(new Correspondence_Pair(cam_x, cam_y, cam_z,
							crossVertices[crossNum*2+4]/2*960f + 480f, crossVertices[crossNum*2+1]/2f*540f + 270f));
					//Call the SVD function, a minimum of 6 points is required//
					if ( svd.projectionDLTImpl() ){
						svd.BuildGLMatrix3x4(.1, 100.0, 960, 0, 540, 0);
						//write the calibration results to the proper file//
						if ( !eye ) {
			                for (int i = 0; i < 16; i++) {
			                	u_ProjectionLeft[i] = (float)svd.projMat3x4[i];
			                }
		                } else {
			                for (int i = 0; i < 16; i++) {
			                	u_ProjectionRight[i] = (float)svd.projMat3x4[i];
			                }
		                }
						WriteFileFunc();
					}
				}
				crossNum += 4;
				crossCount = 4;
			}
		}
	}
	
	/***************************************************************************
	 * This function is needed for the touch events callback setup. It does not
	 * currently perform any meaningful function however.
	 **************************************************************************/
	public void handleTouchDrag(){
		
	}
}
