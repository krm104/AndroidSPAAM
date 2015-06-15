/*************************************************************************
This code is taken almost directly from the Ubitrack library. I have simply
converted it from C++ and Boost to Java and JAMA:

Ubitrack - Library for Ubiquitous Tracking
 * Copyright 2006, Technische Universitaet Muenchen,
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.

The source code is open source under the GLPL license. Please feel free
to use and modfiy the source code with proper citation where applicable
*************************************************************************/

package com.spaam.util;

//Jaba imports//
import java.util.ArrayList;
import java.util.List;

//Import needed for the Linear Algebra and Matrix related math functions//
import Jama.*;

/*******************************************************************************
 * This class provides an interface and related sub classes for recording
 * 2D - 3D   screen - world correspondence pairs needed for the SPAAM calibration.
 * 
 * It also provides a class for storing a list of correspondence pairs and then performing
 * the SVD calculation on those pairs.
 *******************************************************************************/
public class spaamutil {

		/***************************************************************************
		 * This class is the interface by which a list of correspondence pairs can be
		 * recorded and the SVD calculations performed on those pairs. The list
		 * corr_points stores all of the screen-world alignment pairs and the function
		 * projectionDLTImpl( ) performs the SVD calculations producing the final
		 * 3x4 projection Matrix.
		 **************************************************************************/
		static public class SPAAM_SVD{
			
			//Default Constructor that does nothing//
			public SPAAM_SVD()
			{			}
			
			/********************************************************************
			 * This class is used to easily store a 2D Pixel and 3D world point
			 * correspondence pair. This makes it easy to keep related values
			 * for each pair together.
			 *******************************************************************/
			static public class Correspondence_Pair {
				public Correspondence_Pair( )
				{  }
				
				public Correspondence_Pair( double x1, double y1, double z1, double x2, double y2 )
				{ worldPoint.set(0, 0, x1); worldPoint.set(0, 1, y1); worldPoint.set(0, 2, z1);
					screenPoint.set(0, 0, x2); screenPoint.set(0, 1, y2); }

				//Correspondence Points//
				public Matrix worldPoint = new Matrix(1, 3);
				public Matrix screenPoint = new Matrix(1, 2);
			}

			/////////////////////////////////////////////////////////////////////////////////////////////////////////
			/////////////////////////////////////////////////////////////////////////////////////////////////////////
			////Correspondence Points////
			public List<Correspondence_Pair> corr_points = new ArrayList<Correspondence_Pair>();
				
			////Normalization Components for World Points////
			private Matrix fromShift = new Matrix(1, 3);
			private Matrix fromScale = new Matrix(1, 3);
			////Normalization Components for Screen Points////
			private Matrix toShift = new Matrix(1, 2);
			private Matrix toScale = new Matrix(1, 2);
			////Normalization Matrix for World Points////
			private Matrix modMatrixWorld = new Matrix(4, 4);
			////Normalization Matrix for Screen Points////
			private Matrix modMatrixScreen = new Matrix(3, 3);
			
			////Final 3 x 4 Projection Matrix////
			public Matrix Proj3x4 = new Matrix(3, 4);	
			public double[] projMat3x4 = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};

			///////////////////////////////////////////////////////////////////////////////////////////////

			//A helper function to perform an element wise divide of 2 matrices (or vectors)
			private Matrix element_div(Matrix m1, Matrix m2)
			{
				Matrix result = null;
				if ( m1.getColumnDimension() == m1.getColumnDimension() )
					if ( m1.getRowDimension() == m2.getRowDimension() )
					{
						result = new Matrix(m1.getRowDimension(), m1.getColumnDimension());
						for ( int i = 0; i < m1.getRowDimension(); i++)
							for( int j = 0; j < m1.getColumnDimension(); j++ )
							{
								result.set(i,  j, m1.get(i,  j)/m2.get(i,  j));
							}
					}
				
				return result;
			}
			///////////////////////////////////////////////////////////////////////////////////////////////
			
			//This is a normalization function that normalizes all of the 2D and 3D correpsondence values//
			//Normalization is required since the 2D and 3D point values come over different ranges of values//
			private void estimateNormalizationParameters( )
			{
				////determine the number of points to be normalized////
				double n_pts = corr_points.size();

				////start with all 0's////
				fromShift = new Matrix(1, 3); 
				fromScale = new Matrix(1, 3); 
				toShift = new Matrix(1, 2);
				toScale = new Matrix(1, 2);

				////compute mean and mean of square////
				for ( int i = 0; i < corr_points.size(); ++i ){	
					fromShift = fromShift.plus(corr_points.get(i).worldPoint);
					Matrix tempscale = new Matrix(1, 3);
					tempscale.set(0, 0, corr_points.get(i).worldPoint.get(0, 0)*corr_points.get(i).worldPoint.get(0, 0));
					tempscale.set(0, 1, corr_points.get(i).worldPoint.get(0, 1)*corr_points.get(i).worldPoint.get(0, 1));
					tempscale.set(0, 2, corr_points.get(i).worldPoint.get(0, 2)*corr_points.get(i).worldPoint.get(0, 2));
					fromScale = fromScale.plus(tempscale);

					toShift = toShift.plus(corr_points.get(i).screenPoint);
					Matrix temptscale = new Matrix(1, 2);
					temptscale.set(0, 0, corr_points.get(i).screenPoint.get(0, 0)*corr_points.get(i).screenPoint.get(0, 0));
					temptscale.set(0, 1, corr_points.get(i).screenPoint.get(0, 1)*corr_points.get(i).screenPoint.get(0, 1));
					toScale = toScale.plus(temptscale);
				}
				fromShift = fromShift.times(( 1.0 ) / n_pts);
				fromScale = fromScale.times(( 1.0 ) / n_pts);
				toShift = toShift.times(( 1.0 ) / n_pts);
				toScale = toScale.times(( 1.0 ) / n_pts);

				////compute standard deviation////
				for ( int i = 0; i < 3; i++ ){
					fromScale.set(0, i, Math.sqrt( fromScale.get(0, i ) - ( fromShift.get(0, i ) * fromShift.get(0, i ) ) ));
				}
				for ( int i = 0; i < 2; i++ ){
					toScale.set(0, i, Math.sqrt( toScale.get(0, i ) - ( toShift.get(0, i ) * toShift.get(0, i )) ) );
				}
				////end of function////
			}

			//This function produces the correction values needed to transform the result of the SVD function//
			//back into the proper range of values//
			private void generateNormalizationMatrix( )
			{
				////compute correction matrix////
				////Start with All 0's////
				modMatrixWorld = new Matrix(4, 4);
				modMatrixScreen = new Matrix(3, 3);

				////create homogeneous matrix////
				modMatrixWorld.set( 3, 3, 1.0 );
				modMatrixScreen.set( 2, 2, 1.0 );

				////honestly I'm not sure what this is for////
				//if ( true )
				{
					for ( int i = 0; i < 2; i++ )
					{
						modMatrixScreen.set( i, i, toScale.get(0, i ));
						modMatrixScreen.set( i, 2, toShift.get(0, i ));
					}
				}//else
				{
					for ( int i = 0; i < 3; i++ )
					{
						modMatrixWorld.set( i, i, ( 1.0 ) / fromScale.get(0, i ));
						modMatrixWorld.set( i, 3, -modMatrixWorld.get( i, i ) * fromShift.get(0, i ));
					}
				}
			}
			
			//This function should be called to perform the Singular Value Decomposition operation//
			//on the correspondence pairs in the corr_points list. The result is the 3x4 projection matrix//
			//stored in the Proj3x4 object.//
			public boolean projectionDLTImpl( )
			{
				////minimum of 6 correspondence points required to solve////
				if( corr_points.size() < 6 )
					return false;

				// normalize input points
				estimateNormalizationParameters( );

				// construct equation system
				Matrix A = new Matrix(2*corr_points.size(), 12);
			
				for ( int i = 0; i < corr_points.size(); i++ )
				{
					Matrix to = element_div( corr_points.get(i).screenPoint.minus(toShift), toScale );
					Matrix from = element_div( corr_points.get(i).worldPoint.minus(fromShift), fromScale );

					A.set( i * 2,  0, 0 );
					A.set( i * 2, 1, 0 );
					A.set( i * 2, 2, 0 );
					A.set( i * 2, 3, 0 );
					A.set( i * 2,  4, -from.get(0, 0 ));
					A.set( i * 2,  5, -from.get(0, 1 ));
					A.set( i * 2,  6, -from.get(0, 2 ));
					A.set( i * 2,  7, -1);
					A.set( i * 2,  8, to.get(0, 1 ) * from.get(0, 0 ));
					A.set( i * 2,  9, to.get(0, 1 ) * from.get(0, 1 ));
					A.set( i * 2, 10, to.get(0, 1 ) * from.get(0, 2 ));
					A.set( i * 2, 11, to.get(0, 1 ));
					A.set( i * 2 + 1,  0, from.get(0, 0 ));
					A.set( i * 2 + 1,  1, from.get(0, 1 ));
					A.set( i * 2 + 1,  2, from.get(0, 2 ));
					A.set( i * 2 + 1,  3, 1);
					A.set( i * 2 + 1,  4, 0);
					A.set( i * 2 + 1, 5, 0 );
					A.set( i * 2 + 1, 6, 0 );
					A.set( i * 2 + 1, 7, 0);
					A.set( i * 2 + 1,  8, -to.get(0, 0 ) * from.get(0, 0 ));
					A.set( i * 2 + 1,  9, -to.get(0, 0 ) * from.get(0, 1 ));
					A.set( i * 2 + 1, 10, -to.get(0, 0 ) * from.get(0, 2 ));
					A.set( i * 2 + 1, 11, -to.get(0, 0 ));
				}

				// solve using SVD
				//Matrix s = new Matrix(1, 12);
				//Matrix U = new Matrix( 2 * corr_points.size(), 2 * corr_points.size() );
				Matrix Vt = new Matrix(12, 12);
				Vt = A.svd().getV().transpose();
				
				// copy result to 3x4 matrix
				Proj3x4.set( 0, 0, Vt.get( 11, 0 )); Proj3x4.set( 0, 1, Vt.get( 11, 1 )); Proj3x4.set( 0, 2, Vt.get( 11,  2 )); Proj3x4.set( 0, 3, Vt.get( 11,  3 ));
				Proj3x4.set( 1, 0, Vt.get( 11, 4 )); Proj3x4.set( 1, 1, Vt.get( 11, 5 )); Proj3x4.set( 1, 2, Vt.get( 11,  6 )); Proj3x4.set( 1, 3, Vt.get( 11,  7 ));
				Proj3x4.set( 2, 0, Vt.get( 11, 8 )); Proj3x4.set( 2, 1, Vt.get( 11, 9 )); Proj3x4.set( 2, 2, Vt.get( 11, 10 )); Proj3x4.set( 2, 3, Vt.get( 11, 11 ));

				// reverse normalization
				generateNormalizationMatrix( );
				Matrix toCorrect = new Matrix(( modMatrixScreen.getArray() ));
				Matrix Ptemp = new Matrix(3, 4); Ptemp = toCorrect.times(Proj3x4);
				Matrix fromCorrect = new Matrix(( modMatrixWorld.getArray() ));
				Proj3x4 = Ptemp.times(fromCorrect);
				
				// normalize result to have a viewing direction of length 1 (optional)
				double fViewDirLen = Math.sqrt( Proj3x4.get( 2, 0 ) * Proj3x4.get( 2, 0 ) + Proj3x4.get( 2, 1 ) * Proj3x4.get( 2, 1 ) + Proj3x4.get( 2, 2 ) * Proj3x4.get( 2, 2 ) );

				// if first point is projected onto a negative z value, negate matrix
				Matrix p1st = new Matrix(corr_points.get(0).worldPoint.getArray());
				if ( Proj3x4.get( 2, 0 ) * p1st.get(0, 0 ) + Proj3x4.get( 2, 1 ) * p1st.get(0, 1 ) + Proj3x4.get( 2, 2 ) * p1st.get(0, 2 ) + Proj3x4.get( 2, 3 ) < 0 )
					fViewDirLen = -fViewDirLen;

				Proj3x4 = Proj3x4.times(( 1.0 ) / fViewDirLen);

				Proj3x4.print(0,  3);
				
				return true;
			}
		
			//This function transforms the 3x4 projection matrix produced by the SVD operation into a//
			//4x4 matrix matrix usable by OpenGL. The parameters are the near, far clip planes, and screen resolution//
			public void BuildGLMatrix3x4(double ne, double fr, int right, int left, int top, int bottom){
				projMat3x4[0] = Proj3x4.get(0, 0); projMat3x4[1] = Proj3x4.get(0, 1); projMat3x4[2] = Proj3x4.get(0, 2); projMat3x4[3] = Proj3x4.get(0, 3);
				projMat3x4[4] = Proj3x4.get(1, 0); projMat3x4[5] = Proj3x4.get(1, 1); projMat3x4[6] = Proj3x4.get(1, 2); projMat3x4[7] = Proj3x4.get(1, 3);
				projMat3x4[8] = Proj3x4.get(2, 0); projMat3x4[9] = Proj3x4.get(2, 1); projMat3x4[10] = Proj3x4.get(2, 2); projMat3x4[11] = Proj3x4.get(2, 3);

				constructProjectionMatrix4x4_( ne, fr, right, left, top, bottom);
			}
			
			//This function creates an orthogonal matrix that is then multiplied by the 3x4 SPAAM result//
			//creating a 4x4 matrix (column major order) usable by OpenGL//
			private void constructProjectionMatrix4x4_( double ne, double fr, int right, int left, int top, int bottom)
			{
				double[] proj4x4 = new double[16];

				//Copy base 3x4 values//
				System.arraycopy(projMat3x4, 0, proj4x4, 0, 12);
				//Duplicate third row into the fourth//
				System.arraycopy(projMat3x4, 8, proj4x4, 12, 4);
			
				//calculate extra parameters//
				double norm = Math.sqrt(proj4x4[8] * proj4x4[8] + proj4x4[9] * proj4x4[9] + proj4x4[10] * proj4x4[10]);
				double add = fr*ne*norm;

				//Begin adjusting the 3x4 values for 4x4 use//
				proj4x4[8] *= (-fr - ne);
				proj4x4[9] *= (-fr - ne);
				proj4x4[10] *= (-fr - ne);
				proj4x4[11] *= (-fr - ne);
				proj4x4[11] += add;	

				//Create Orthographic projection matrix//
				double[] ortho = new double[16];
				ortho[0] = 2.0f / (right - left);
				ortho[1] = 0.0f;
				ortho[2] = 0.0f;
				ortho[3] = (right + left) / (left - right);
				ortho[4] = 0.0f;
				ortho[5] = 2.0f / (top - bottom);
				ortho[6] = 0.0f;
				ortho[7] = (top + bottom) / (bottom - top);
				ortho[8] = 0.0f;
				ortho[9] = 0.0f;
				ortho[10] = 2.0f / (ne - fr);
				ortho[11] = (fr + ne) / (ne - fr);
				ortho[12] = 0.0f;
				ortho[13] = 0.0f;
				ortho[14] = 0.0f;
				ortho[15] = 1.0f;

				//Multiply the 4x4 projection by the orthographic projection//
				projMat3x4[0] = ortho[0]*proj4x4[0] + ortho[1]*proj4x4[4] + ortho[2]*proj4x4[8] + ortho[3]*proj4x4[12];
				projMat3x4[1] = ortho[0]*proj4x4[1] + ortho[1]*proj4x4[5] + ortho[2]*proj4x4[9] + ortho[3]*proj4x4[13];
				projMat3x4[2] = ortho[0]*proj4x4[2] + ortho[1]*proj4x4[6] + ortho[2]*proj4x4[10] + ortho[3]*proj4x4[14];
				projMat3x4[3] = ortho[0]*proj4x4[3] + ortho[1]*proj4x4[7] + ortho[2]*proj4x4[11] + ortho[3]*proj4x4[15];

				projMat3x4[4] = ortho[4]*proj4x4[0] + ortho[5]*proj4x4[4] + ortho[6]*proj4x4[8] + ortho[7]*proj4x4[12];
				projMat3x4[5] = ortho[4]*proj4x4[1] + ortho[5]*proj4x4[5] + ortho[6]*proj4x4[9] + ortho[7]*proj4x4[13];
				projMat3x4[6] = ortho[4]*proj4x4[2] + ortho[5]*proj4x4[6] + ortho[6]*proj4x4[10] + ortho[7]*proj4x4[14];
				projMat3x4[7] = ortho[4]*proj4x4[3] + ortho[5]*proj4x4[7] + ortho[6]*proj4x4[11] + ortho[7]*proj4x4[15];

				projMat3x4[8] = ortho[8]*proj4x4[0] + ortho[9]*proj4x4[4] + ortho[10]*proj4x4[8] + ortho[11]*proj4x4[12];
				projMat3x4[9] = ortho[8]*proj4x4[1] + ortho[9]*proj4x4[5] + ortho[10]*proj4x4[9] + ortho[11]*proj4x4[13];
				projMat3x4[10] = ortho[8]*proj4x4[2] + ortho[9]*proj4x4[6] + ortho[10]*proj4x4[10] + ortho[11]*proj4x4[14];
				projMat3x4[11] = ortho[8]*proj4x4[3] + ortho[9]*proj4x4[7] + ortho[10]*proj4x4[11] + ortho[11]*proj4x4[15];

				projMat3x4[12] = ortho[12]*proj4x4[0] + ortho[13]*proj4x4[4] + ortho[14]*proj4x4[8] + ortho[15]*proj4x4[12];
				projMat3x4[13] = ortho[12]*proj4x4[1] + ortho[13]*proj4x4[5] + ortho[14]*proj4x4[9] + ortho[15]*proj4x4[13];
				projMat3x4[14] = ortho[12]*proj4x4[2] + ortho[13]*proj4x4[6] + ortho[14]*proj4x4[10] + ortho[15]*proj4x4[14];
				projMat3x4[15] = ortho[12]*proj4x4[3] + ortho[13]*proj4x4[7] + ortho[14]*proj4x4[11] + ortho[15]*proj4x4[15];

				proj4x4[0] = projMat3x4[0]; proj4x4[1] = projMat3x4[4]; proj4x4[2] = projMat3x4[8]; proj4x4[3] = projMat3x4[12];
				proj4x4[4] = projMat3x4[1]; proj4x4[5] = projMat3x4[5]; proj4x4[6] = projMat3x4[9]; proj4x4[7] = projMat3x4[13];
				proj4x4[8] = projMat3x4[2]; proj4x4[9] = projMat3x4[6]; proj4x4[10] = projMat3x4[10]; proj4x4[11] = projMat3x4[14];
				proj4x4[12] = projMat3x4[3]; proj4x4[13] = projMat3x4[7]; proj4x4[14] = projMat3x4[11]; proj4x4[15] = projMat3x4[15];
				
				for (int i = 0; i < 16; i++)
				{
					projMat3x4[i] = proj4x4[i];
				}
			}
		}
}
