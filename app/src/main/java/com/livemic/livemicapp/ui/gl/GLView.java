package com.livemic.livemicapp.ui.gl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.livemic.livemicapp.pipes.RecentSamplesBuffer;

// Android View that draws samples to a GL Surface.
public class GLView extends GLSurfaceView {
  private final GLRenderer renderer;

  public GLView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setEGLContextClientVersion(2);

    this.renderer = new GLRenderer(this.getContext());
    setRenderer(this.renderer);
  }

  // Connect to data sounrce
  public void attachBuffer(RecentSamplesBuffer buffer) {
    this.renderer.attachBuffer(buffer);
  }
}
