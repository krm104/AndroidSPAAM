/**************************************************************************************
 * Author: Kenneth Moser
 * email: moserk@acm.org
 * Affiliation: Mississippi State University
 * 
 * Description: This file contains the Class OGLESRenderer which manages the display 
 * of the on-screen calibration patterns as well as the transmition of the pixel 
 * and 3D point locations (obtained from Vuforia) to the Ubitrack program running on
 * a separate machine.
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
 * used to generate the 3x4 SPAAM projection matrix.
 *********************************************************************/
public class OGLESRenderer extends Activity implements Renderer {
	final int SCREENWIDTH = 960;
	final int SCREENHEIGHT = 540;
	
	
	/******Members for openGL ES rendering******/
	//Members Specific to the Calibration Grid//
	private static final int CROSS_POSITION_COMPONENT_COUNT = 2;
	private static final int BYTES_PER_FLOAT = 4;
	private static final String U_COLOR = "u_Color";
	private static final String A_POSITION = "a_Position";
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
	float[] squareVertices= {//Top Edge//
									-0.10f, 0.10f, 0.0f, 0.10f, 0.10f, 0.0f,
									//Left Edge//
									-0.10f, 0.10f, 0.0f, -0.10f, -0.10f, 0.0f,
									//Right Edge//
									0.10f, 0.10f, 0.0f, 0.10f, -0.10f, 0.0f,
									//Bottom Edge//
									-0.10f, -0.10f, 0.0f, 0.10f, -0.10f, 0.0f
									};
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

	///////////////////////////////////////////////////////
	///////////////////////////////////////////////////////
	/** Native function for initializing the renderer. */
    public native void initTracking(int width, int height);

    /** Native function to update the renderer. */
    public native void updateTracking();
    //////////////////////
	
	public void SetupFileFunc( boolean eye) throws IOException
	{
		if (isExternalStorageReadable() && isExternalStorageWritable()) {
			File storageDirectory = getAlbumStorageDir("SPAAM_Calib");
			if ( storageDirectory != null ){
				if ( storageDirectory.listFiles() != null )
				{
					file = true;
					calibFile = null;
				
					if ( eye ) {
						for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
							if ( storageDirectory.listFiles()[i].getName() == "Right.calib" ){
								calibFile = storageDirectory.listFiles()[i];
								break;
							}
						}
						if ( calibFile == null )
						{
							calibFile = new File(storageDirectory.getAbsolutePath() + "/Right.calib");
							calibFile.createNewFile();
						}
					}else {
						for ( int i = 0; i < storageDirectory.listFiles().length; i++ ){
							if ( storageDirectory.listFiles()[i].getName() == "Left.calib" ){
								calibFile = storageDirectory.listFiles()[i];
								break;
							}
						}
						if ( calibFile == null )
						{
							calibFile = new File(storageDirectory.getAbsolutePath() + "/Left.calib");
							calibFile.createNewFile();
						}
					}
					
					RandomAccessFile rac_file = new RandomAccessFile(calibFile.getAbsolutePath(), "r");
					if ( rac_file.length() >= 16*8 )
					{
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
		}else{
			file = false;
		}
	}

	public void WriteFileFunc( ) throws IOException{
		RandomAccessFile rac_file = new RandomAccessFile(calibFile.getAbsolutePath(), "rw");
		rac_file.setLength(0);
		if ( eye ){
			for ( int i = 0; i < 16; i++ )
			{
				rac_file.writeDouble((double)u_ProjectionRight[i]);
			}
			rac_file.close();
		}else{
			for ( int i = 0; i < 16; i++ )
			{
				rac_file.writeDouble((double)u_ProjectionLeft[i]);
			}
			rac_file.close();
		}
	}
	
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
		//glEnableVertexAttribArray(aCrossPositionLocation);
		
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
		//glEnableVertexAttribArray(aSquarePositionLocation);	
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		glViewport(0, 0, width, height);
		
		WIDTH = width;
		HEIGHT = height;
		
		///Set Moverio Display Mode to be 3D///
		mDisplayControl.setMode(DisplayControl.DISPLAY_MODE_3D, false);
	}

	public void setCameraPoseNative(float cam_x,float cam_y,float cam_z) {
		// u_Transform[12] =
		this.cam_x = cam_x/100.0f;
		// u_Transform[13] =
		this.cam_y = cam_y/100.0f;
		// u_Transform[14] =
		this.cam_z = cam_z/100.0f;
	}

	public void setCameraOrientationNative(float transform0, float transform1, float transform2, float transform3,
			float transform4, float transform5, float transform6, float transform7, float transform8, float transform9,
			float transform10, float transform11, float transform12, float transform13, float transform14, float transform15) {
	 
		u_Transform[0] = transform0; u_Transform[1] = -transform1; u_Transform[2] = -transform2; u_Transform[3] = transform3;
		u_Transform[4] = transform4; u_Transform[5] = -transform5; u_Transform[6] = -transform6; u_Transform[7] = transform7;
		u_Transform[8] = transform8; u_Transform[9] = -transform9; u_Transform[10] = -transform10; u_Transform[11] = transform11;
		u_Transform[12] = transform12/100.0f; u_Transform[13] = -transform13/100.0f; u_Transform[14] = -transform14/100.0f; u_Transform[15] = transform15;
	
	}

	public void setTrackedNative(boolean istracked) {
		 tracking = istracked;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		updateTracking();
		
		/////////////////////////////////////////////////////////////////////////
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
			
			glUniformMatrix4fv(uProjectionLocation, 1, false, u_ProjectionLeft, 0);
			glUniformMatrix4fv(uTransformLocation, 1, false, u_Transform, 0);
			glDrawArrays(GL_LINES, 0, squareVertices.length/3);	
			glDisableVertexAttribArray(aSquarePositionLocation);
		}
		///////////////////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////////////////
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

			glUniformMatrix4fv(uProjectionLocation, 1, false, u_ProjectionRight, 0);
			glUniformMatrix4fv(uTransformLocation, 1, false, u_Transform, 0);
			glDrawArrays(GL_LINES, 0, squareVertices.length/3);	
			glDisableVertexAttribArray(aSquarePositionLocation);
		}
		///////////////////////////////////////////////////////////////////////////
	}

	public void handleTouchPress() throws IOException{
		
		if ( tracking )
		{
			/////Display Cross/////
			if ( crossNum >= crossVertices.length/2-4 )
			{
				if ( crossNum >= 0)
				{	
					///////Write Pixels//////
					svd.corr_points.add(new Correspondence_Pair(cam_x, cam_y, cam_z,
							crossVertices[crossNum*2+4]/2*960f + 480f, crossVertices[crossNum*2+1]/2f*540f + 270f));
					if ( svd.projectionDLTImpl() ) {
						svd.BuildGLMatrix3x4(.1, 100.0, 960, 0, 540, 0);
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
			}else {
				if ( crossNum >= 0)
				{	
					svd.corr_points.add(new Correspondence_Pair(cam_x, cam_y, cam_z,
							crossVertices[crossNum*2+4]/2*960f + 480f, crossVertices[crossNum*2+1]/2f*540f + 270f));
					if ( svd.projectionDLTImpl() ){
						svd.BuildGLMatrix3x4(.1, 100.0, 960, 0, 540, 0);
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
	
	public void handleTouchDrag(){
		
	}
}
