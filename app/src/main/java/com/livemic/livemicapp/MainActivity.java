package com.livemic.livemicapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.livemic.livemicapp.databinding.ActivityMainBinding;
import com.livemic.livemicapp.model.Conversation;
import com.livemic.livemicapp.model.Participant;
import com.livemic.livemicapp.pipes.MicSource;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;
import com.livemic.livemicapp.ui.MicPagerAdapter;
import com.livemic.livemicapp.ui.ParticipantListAdapter;
import com.livemic.livemicapp.ui.gl.GLView;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements TextChatLog {
  private GLView sampleView;

  // Data models
  private Conversation conversation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    maybeRequestRecordAudioPermission();

    conversation = hackTestConversation();
    ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    binding.setConversation(conversation);

    MicPagerAdapter pagerAdapter = new MicPagerAdapter(this);
    ViewPager pager = (ViewPager) findViewById(R.id.mainPager);
    pager.setAdapter(pagerAdapter);

    sampleView = (GLView) findViewById(R.id.sampleView);

    RecyclerView listView = (RecyclerView) findViewById(R.id.participantList);
    listView.setLayoutManager(new LinearLayoutManager(this));
    listView.setAdapter(new ParticipantListAdapter(this, conversation));

    SoundRewriter rewriter = new SoundRewriter();
    rewriter.start(this, conversation, new Runnable() {
      @Override
      public void run() {
        conversation.notifyChange();
      }
    });
  }


  @Override
  public void handleChatText(String currentMessage, Collection<String> previousMessages) {
    conversation.updateMessages(currentMessage, previousMessages);
  }

  // HACK - hook up local audio sink to UI
  public void attachLocalBuffer(RecentSamplesBuffer buffer) {
    sampleView.attachBuffer(buffer);
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

  // BIG HACK - this should come from network sharing stuff.
  private Conversation hackTestConversation() {
    // HACK
    Participant p1 = new Participant("P1");
    Participant p2 = new Participant("P2");
    Conversation testConversation = new Conversation(
        this, // Owner activity
        true, // Whether I'm the moderator
        p1,   // My identity
        "P1"  // Name of the current talker
    );
    testConversation.addParticipant(p1);
    testConversation.addParticipant(p2);
    return testConversation;
  }

}
