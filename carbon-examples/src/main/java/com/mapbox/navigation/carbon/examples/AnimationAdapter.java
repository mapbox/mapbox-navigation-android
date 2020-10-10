package com.mapbox.navigation.carbon.examples;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.navigation.carbon.examples.AnimationType.*;

public class AnimationAdapter extends RecyclerView.Adapter<AnimationAdapter.AnimationsViewHolder> {

  private List<AnimationType> animationList = new ArrayList<>();
  private OnAnimationButtonClicked callback;
  private LayoutInflater inflater;

  public AnimationAdapter(Context context, OnAnimationButtonClicked callback) {
    this.inflater = LayoutInflater.from(context);
    this.callback = callback;
    animationList.add(Following);
    animationList.add(Overview);
    animationList.add(Recenter);
  }

  @NonNull @Override public AnimationsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = inflater.inflate(R.layout.item_animation_list, parent, false);
    return new AnimationsViewHolder(view);
  }

  @Override public void onBindViewHolder(@NonNull AnimationsViewHolder holder, int position) {
    AnimationType item = animationList.get(position);
    holder.bindAnimations(item);
  }

  @Override public int getItemCount() {
    return 3;
  }

  class AnimationsViewHolder extends RecyclerView.ViewHolder {

    Button animationButton;
    public AnimationsViewHolder(@NonNull View itemView) {
      super(itemView);
      this.animationButton = itemView.findViewById(R.id.animationButton);
    }

    public void bindAnimations(AnimationType item) {
      animationButton.setText(animationList.get(getAdapterPosition()).name());
      animationButton.setOnClickListener(v -> callback.onButtonClicked(item));
    }
  }

  interface OnAnimationButtonClicked {
    void onButtonClicked(@NonNull AnimationType animationType);
  }
}
