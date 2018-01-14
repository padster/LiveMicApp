package com.livemic.livemicapp;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.livemic.livemicapp.databinding.ActivityMainBinding;
import com.livemic.livemicapp.model.Conversation;
import com.livemic.livemicapp.model.MessageObject;
import com.livemic.livemicapp.model.MessageUtil;
import com.livemic.livemicapp.model.Participant;
import com.livemic.livemicapp.pipes.RecentSamplesBuffer;
import com.livemic.livemicapp.pipes.wifi.WiFiDirectSink;
import com.livemic.livemicapp.pipes.wifi.WiFiDirectSource;
import com.livemic.livemicapp.ui.MicPagerAdapter;
import com.livemic.livemicapp.ui.ParticipantListAdapter;
import com.livemic.livemicapp.ui.gl.GLView;

import java.util.Collection;

import static com.livemic.livemicapp.Constants.LOCAL_ONLY_TAG;
import static com.livemic.livemicapp.Constants.MSG_PUSHOUT_DATA;
import static com.livemic.livemicapp.Constants.MSG_REGISTER_ACTIVITY;

public class MainActivity extends AppCompatActivity implements TextChatLog {
  private GLView sampleView;

  // Data models
  private Conversation conversation;

  // TODO - remove:
  private WiFiDirectSource source;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean localOnly = getIntent().hasExtra(LOCAL_ONLY_TAG);
    conversation = createConversation(localOnly);

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

  // BIG HACK - this should come from network sharing stuff.
  private Conversation createConversation(boolean localOnly) {
    this.source = null;
    boolean isServer = true;
    if (!localOnly) {
      source = new WiFiDirectSource();
      isServer = ((LiveMicApp) getApplication()).mIsServer;
    }

    // HACK
    Participant p1 = new Participant("P1");
    Participant p2 = new Participant("P2");
    Participant me = isServer ? p1 : p2;

    Conversation testConversation = new Conversation(
        this,     // Owner activity
        isServer, // Whether I'm the moderator
        me,       // My identity
        "P1",     // Name of the current talker
        source    // Source of sound coming from other devices
    );

    testConversation.addParticipant(p1);
    testConversation.addParticipant(p2);

    WiFiDirectSink sink = null;
    Log.i(Constants.TAG, "Local only? " + localOnly);
    if (!localOnly) {
      sink = new WiFiDirectSink(testConversation);
      testConversation.setSink(sink);
    }
    return testConversation;
  }


  public void updateWithSamples(byte[] samples) {

  }

  @Override
  protected void onPause() {
    super.onPause();
    registerActivityToService(false);
  }

  @Override
  protected void onResume() {
    super.onResume();
    registerActivityToService(true);
  }

  private void registerActivityToService(boolean register) {
    if (ConnectionService.getInstance() != null) {
      Message msg = ConnectionService.getInstance().getHandler().obtainMessage();
      msg.what = MSG_REGISTER_ACTIVITY;
      msg.obj = this;
      msg.arg1 = register ? 1 : 0;
      ConnectionService.getInstance().getHandler().sendMessage(msg);
    }
  }

  /** Given a payload from afar, update stuff locally. */
  public void handleMessageReceived(MessageObject msg) {
    if (MessageUtil.isSamples(msg)) {
      byte[] samples = msg.getAudioData();
      if (samples != null) {
        Log.d(Constants.TAG, "RECV> " + samples.length + " bytes");
        // New samples received! HACK: Spaghetti
        if (source != null) {
          source.updateWithRemoteSamples(samples);
        }
      }
    } else {
      conversation.updateMetaFromServer(
          msg.getParticipants(),
          msg.getTalkingParticipant(),
          msg.getPastMessages()
      );
    }

  }
}
