package com.livemic.livemicapp.ui.gl;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

// Does all the OpenGL stuff needed to render a line.
public class GLRenderer implements GLSurfaceView.Renderer {
  private final Context ctx;
  private GLLine line;

  private final float[] mMVPMatrix = new float[16];
  private RecentSamplesBuffer buffer;

  public GLRenderer(Context ctx) {
    this.ctx = ctx;

    // Set up the view matrix, projection, then combine to make MVP matrix
    float[] mViewMatrix = new float[16];
    float[] mProjectionMatrix = new float[16];
    Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 7);
    Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
  }

  // Connect to data source
  public void attachBuffer(RecentSamplesBuffer buffer) {
    this.buffer = buffer;
  }
  
  @Override
  public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
    // Set the background frame color
    GLES20.glClearColor(0f, 0f, 0f, 1f);
    GLES20.glEnable(GL10.GL_LINE_SMOOTH);

    // IMPORTANT: This must be created here, it can't be moved to the constructor.
    this.line = new GLLine(ctx);
  }

  @Override
  public void onSurfaceChanged(GL10 gl10, int width, int height) {
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 gl10) {
    // Redraw background color
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    if (this.buffer == null) {
      return;
    }
    float[] snapshot = this.buffer.getSamples();
    line.draw(mMVPMatrix, snapshot);
  }
}
