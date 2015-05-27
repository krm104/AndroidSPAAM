#version 100
//Projection Matrices//
uniform mat4 u_Projection;

//Transformation Matrices//
uniform mat4 u_Transform;

//Vertex Coordinates//
attribute vec4 a_Position;

void main()
{      
	gl_Position = u_Projection*u_Transform*a_Position;
	gl_PointSize = 10.0;
}