package com.livemic.livemicapp.ui;


import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.view.View;

import com.livemic.livemicapp.R;

/** Adapter for the tabs in the main activity - lets us use layouts in the same file. */
public class MicPagerAdapter extends PagerAdapter {
  // Names of the tabs, can't easily use the ones in the layout :(
  private static final String[] TITLES = new String[]{"Speaker", "Audience"};

  private final Activity ctx;
  public MicPagerAdapter(Activity ctx) {
    this.ctx = ctx;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return TITLES[position];
  }

  @Override
  public Object instantiateItem(View collection, int position) {
    int resId = 0;
    switch (position) {
      case 0:
        resId = R.id.speakerPage;
        break;
      case 1:
        resId = R.id.audiencePage;
        break;
    }
    return ctx.findViewById(resId);
  }

  @Override
  public int getCount() {
    return 2;
  }

  @Override
  public boolean isViewFromObject(View arg0, Object arg1) {
    return arg0 == ((View) arg1);
  }
}