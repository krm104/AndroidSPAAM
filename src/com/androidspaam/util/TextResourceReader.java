/**************************************************************************
 * This file contains an interface for reading in a text resource.
 * This is primarily used for reading in shader files.
 *************************************************************************/
package com.androidspaam.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources;

/****************************************************************************
 * Interface for reading in text resources (shader files)
 ***************************************************************************/
public class TextResourceReader {
	public static String readTextFileFromResource(Context context, int resourceID){
		StringBuilder body = new StringBuilder();
		
		try{
			InputStream inputStream = context.getResources().openRawResource(resourceID);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			
			String nextLine;
			
			while((nextLine = bufferedReader.readLine()) != null){
				body.append(nextLine);
				body.append('\n');
			}
		} catch (IOException e){
			throw new RuntimeException("Could not open resource: " + resourceID, e);
			
		}catch (Resources.NotFoundException nfe){
			throw new RuntimeException("Resource not found: " + resourceID, nfe);
		}
		
		return body.toString();
	}
}
