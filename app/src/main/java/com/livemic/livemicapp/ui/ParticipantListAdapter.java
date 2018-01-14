package com.livemic.livemicapp.ui;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.livemic.livemicapp.Constants;
import com.livemic.livemicapp.R;
import com.livemic.livemicapp.databinding.ListitemParticipantBinding;
import com.livemic.livemicapp.model.Conversation;

// Makes a list data-bind-able in Android
public class ParticipantListAdapter extends RecyclerView.Adapter<CustomViewHolder<ListitemParticipantBinding>> {
  private final Context ctx;
  private final Conversation conversation;

  public ParticipantListAdapter(final Context ctx, Conversation conversation) {
      this.ctx = ctx;
      this.conversation = conversation;
    }

  @Override
  public CustomViewHolder<ListitemParticipantBinding> onCreateViewHolder(
      ViewGroup parent, int viewType) {
    ListitemParticipantBinding binding = DataBindingUtil.inflate(
        LayoutInflater.from(this.ctx), R.layout.listitem_participant, parent, false);
    return new CustomViewHolder<>(binding);
  }

  @Override
  public void onBindViewHolder(CustomViewHolder<ListitemParticipantBinding> holder, int position) {
    holder.getBinding().setConversation(conversation);
    holder.getBinding().setParticipant(conversation.getParticipant(position));
  }

  @Override
  public int getItemCount() {
    return conversation.participantCount();
  }
}
