package com.mapbox.services.android.navigation.v5.utils;

import androidx.annotation.IntRange;

import java.util.ArrayDeque;
import java.util.Collection;

public class RingBuffer<T> extends ArrayDeque<T> {

  private final int maxSize;

  public RingBuffer(@IntRange(from = 0) int maxSize) {
    this.maxSize = maxSize;
  }

  @Override
  public boolean add(T item) {
    boolean result = super.add(item);
    resize();
    return result;
  }

  @Override
  public void addFirst(T item) {
    super.addFirst(item);
    resize();
  }

  @Override
  public void addLast(T item) {
    super.addLast(item);
    resize();
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    boolean result = super.addAll(collection);
    resize();
    return result;
  }

  @Override
  public void push(T item) {
    super.push(item);
    resize();
  }

  private void resize() {
    while (size() > maxSize) {
      pop();
    }
  }
}