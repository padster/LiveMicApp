package com.livemic.livemicapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements TextChatLog {
  private SoundRewriter rewriter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    maybeRequestRecordAudioPermission();

    rewriter = new SoundRewriter();
  }

  public void start(View view) {
    rewriter.start(this, this);
  }

  @Override
  public void handleChatText(String currentMessage, Collection<String> previousMessages) {
    String all = currentMessage;
    for (String msg : previousMessages) {
      all += "\n" + previousMessages;
    }
    TextView tv = (TextView) findViewById(R.id.messageLog);
    tv.setText(all);
  }


  private void maybeRequestRecordAudioPermission() {
    //check API version, do nothing if API version < 23!
    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
          // Show an expanation to the user *asynchronously* -- don't block
          // this thread waiting for the user's response! After the user
          // sees the explanation, try again to request the permission.
        } else {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
    switch (requestCode) {
      case 1: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d("Activity", "Granted!");
        } else {
          Log.d("Activity", "Denied!");
          finish();
        }
        return;
      }
    }
  }

}
