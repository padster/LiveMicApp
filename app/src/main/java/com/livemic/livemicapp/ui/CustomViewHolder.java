package com.livemic.livemicapp.ui;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

// Wrap a view holder with something that lets you manipulate its binder.
public class CustomViewHolder<T extends ViewDataBinding> extends RecyclerView.ViewHolder {
  private final T binding;

  public CustomViewHolder(T binding) {
    super(binding.getRoot());
    this.binding = binding;
  }

  public T getBinding() {
    return this.binding;
  }
}
