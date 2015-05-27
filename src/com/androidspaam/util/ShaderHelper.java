package com.androidspaam.util;


import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glValidateProgram;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;


public class ShaderHelper {
	private static final String TAG = "ShaderHelper";
	
	public static int compileVertexShader(String shaderCode){
		return compileShader(GL_VERTEX_SHADER, shaderCode);
	}
	
	public static int compileFragmentShader(String shaderCode){
		return compileShader(GL_FRAGMENT_SHADER, shaderCode);
	}
	
	private static int compileShader(int type, String shaderCode){
		final int shaderObjectId = glCreateShader(type);
		
		if ( shaderObjectId == 0){
			return 0;
		}
		
		glShaderSource(shaderObjectId, shaderCode);
		glCompileShader(shaderObjectId);
		
		final int[] compileStatus = new int[1];
		glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);
		
		if( compileStatus[0] == 0){
			glDeleteShader(shaderObjectId);
			
			return 0;
		}
		
		return shaderObjectId;
	}
	
	public static int linkProgram(int vertexShaderId, int fragmentShaderId){
		final int programObjectId = glCreateProgram();
		
		if (programObjectId == 0){
			return 0;
		}
		
		glAttachShader(programObjectId, vertexShaderId);
		glAttachShader(programObjectId, fragmentShaderId);
		glLinkProgram(programObjectId);
		
		final int[] linkStatus = new int[1];
		glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);
		
		if ( linkStatus[0] == 0)
		{
			glDeleteProgram(programObjectId);
			return 0;
		}
		
		return programObjectId;
	}

	public static boolean validateProgram(int programObjectId){
		glValidateProgram(programObjectId);
		
		final int[] validateStatus = new int[1];
		glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
		
		return validateStatus[0] != 0;
	}

	/*************************************************************
	 * @param ortho - 16 float array where the result will be stored (assumes row major order)
	 * @param left - left most pixel of the viewport
	 * @param right - right most pixel of the viewport
	 * @param top - top most pixel of the viewport
	 * @param bottom - bottom most pixel of the viewport
	 * @param near - value representing the near plane
	 * @param far - value representing the far plane
	 * @return - dummy boolean
	 ***********************************************************/
	public static float[] createOrthoMatrix( float[] ortho, int left, int right, int top, int bottom, float near, float far){
		ortho[0] = 2.0f/(right - left); ortho[1] = 0.0f; ortho[2] = 0.0f; ortho[3] = (right + left)/(left - right);
		ortho[4] = 0.0f; ortho[5] = 2.0f/(top - bottom); ortho[6] = 0.0f; ortho[7] = (top + bottom)/(bottom - top);
		ortho[8] = 0.0f; ortho[9] = 0.0f; ortho[10] = 2.0f/(near - far); ortho[11] = (far + near)/(near - far);
		ortho[12] = 0.0f; ortho[13] = 0.0f; ortho[14] = 0.0f; ortho[15] = 1.0f;
		
		return ortho;
	}
	
	/***********************************************************
	 * @param proj3x4 - 12 float array with the initial 3x4 projection matrix (assumes row major order)
	 * @param proj4x4 - 16 float array where the result will be stored (assumes row major order)
	 * @param near - value representing the near plane
	 * @param far - value representing the far plane
	 * @return - dummy boolean
	 **********************************************************/
	public static float[] create4x4Projection( float[] proj3x4, float[] proj4x4, float near, float far ){
		//Create Needed Extra Values to expand 3x4 to 4x4//
		double norm = java.lang.Math.sqrt(proj3x4[0]*proj3x4[0] + proj3x4[1]*proj3x4[1] + proj3x4[2]*proj3x4[2]);
		double add = near*far*norm;
		double mult = (-far - near);
		
		//Copy Unchanged Values//
		proj4x4[0] = proj3x4[0]; proj4x4[1] = proj3x4[1]; proj4x4[2] = proj3x4[2]; proj4x4[3] = proj3x4[3];
		proj4x4[4] = proj3x4[4]; proj4x4[5] = proj3x4[5]; proj4x4[6] = proj3x4[6]; proj4x4[7] = proj3x4[7];
		proj4x4[8] = proj3x4[8]; proj4x4[9] = proj3x4[9]; proj4x4[10] = proj3x4[10]; proj4x4[11] = proj3x4[11];
		proj4x4[12] = proj3x4[8]; proj4x4[13] = proj3x4[9]; proj4x4[14] = proj3x4[10]; proj4x4[15] = proj3x4[11];
		
		//Modify New Values;
		proj4x4[8] *= mult; proj4x4[9] *= mult; proj4x4[10] *= mult; proj4x4[11] *= mult; proj4x4[11] += add;
		
		return proj4x4;
	}
	
	public static float[] transposeMatrix(float [] m){
        float[] temp = new float[m.length];
        temp[0] = m[0]; temp[1] = m[4]; temp[2] = m[8]; temp[3] = m[12];
		temp[4] = m[1]; temp[5] = m[5]; temp[6] = m[9]; temp[7] = m[13];
		temp[8] = m[2]; temp[9] = m[6]; temp[10] = m[10]; temp[11] = m[14];
		temp[12] = m[3]; temp[13] = m[7]; temp[14] = m[11]; temp[15] = m[15];
        return temp;
    }
}
