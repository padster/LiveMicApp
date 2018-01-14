package com.livemic.livemicapp.ui.gl;

import android.content.Context;
import android.opengl.GLES20;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

// A single line in GL.
public class GLLine {
  public static int loadShader(Context ctx, int rawId, int type){
    int shader = GLES20.glCreateShader(type);
    InputStream is = ctx.getResources().openRawResource(rawId);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buf = new byte[1024];
    int len;
    try {
      while ((len = is.read(buf)) != -1) {
        baos.write(buf, 0, len);
      }
    } catch (IOException e) {
      throw new RuntimeException("WHOOPS! Could not load resource");
    }
    GLES20.glShaderSource(shader, baos.toString());
    GLES20.glCompileShader(shader);
    return shader;
  }

  static final int VERTEX_STRIDE = 12; // 3 floats per vertex * 4 bytes per float.
  private final float[] color = new float[]{1f, 1f, 1f, 1f};
  private final int programHandle;

  /**
   * Builds a program that can draw a line of a given color.
   */
  public GLLine(Context ctx) {
    // Make program, add shaders, and link:
    this.programHandle = GLES20.glCreateProgram();
    GLES20.glAttachShader(programHandle,
        loadShader(ctx, R.raw.vertex_shader, GLES20.GL_VERTEX_SHADER));
    GLES20.glAttachShader(programHandle,
        loadShader(ctx, R.raw.fragment_shader, GLES20.GL_FRAGMENT_SHADER));
    GLES20.glLinkProgram(programHandle);
  }

  // Convert the line to OPenGL commands!
  public void draw(float[] mvpMatrix, float[] samples) {

    // Add program to OpenGL ES environment
    GLES20.glUseProgram(programHandle);

    // Configure the MVP matrix parameter.
    int mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix");
    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

    // Set position information
    int mPositionHandle = GLES20.glGetAttribLocation(programHandle, "vPosition");
    GLES20.glEnableVertexAttribArray(mPositionHandle);
    GLES20.glVertexAttribPointer(
        mPositionHandle, 3 /* coords per vertex */,
        GLES20.GL_FLOAT, false,
        VERTEX_STRIDE, snapshotToBuffer(samples));

    // Set color information
    int mColorHandle = GLES20.glGetUniformLocation(programHandle, "vColor");
    GLES20.glUniform4fv(mColorHandle, 1, color, 0);

    // And finally, draw it as a line strip.
    GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, samples.length);
    GLES20.glDisableVertexAttribArray(mPositionHandle);
  }

  /**
   * Convert the timeseries snapshot into a run of (x, y, z) bytes.
   */
  private FloatBuffer snapshotToBuffer(float[] snapshot) {
    // Create buffer to store result.
    ByteBuffer bb = ByteBuffer.allocateDirect(snapshot.length * VERTEX_STRIDE);
    bb.order(ByteOrder.nativeOrder());

    // Write to buffer using floats:
    FloatBuffer vertexBuffer = bb.asFloatBuffer();
    vertexBuffer.put(snapshotToCoord(snapshot));
    vertexBuffer.position(0);
    return vertexBuffer;
  }

  /**
   * Convert the timeseries snapshot into a run of (x, y, z) floats.
   */
  private float[] snapshotToCoord(float[] snapshot) {
    int n = snapshot.length;

    double xScale = 1.0 / n;
    double yMin = -Constants.UI_AUDIO_SCALE;
    double yMax =  Constants.UI_AUDIO_SCALE;

    // Write these out to the array of (x, y, z), with z = 0 always.
    float[] result = new float[3 * n];
    for (int i = 0; i < snapshot.length; i++) {
      double x = i * xScale;
      double y = (snapshot[i] - yMin) / (yMax - yMin);
      result[3 * i    ] = 2f * (float)x - 1.0f;
      result[3 * i + 1] = 2f * (float)y - 1.0f;
      result[3 * i + 2] = 0f;
    }

    return result;
  }
}
