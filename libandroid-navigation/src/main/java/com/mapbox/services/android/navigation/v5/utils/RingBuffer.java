package com.mapbox.services.android.navigation.v5.utils;

import android.support.annotation.IntRange;

import java.util.ArrayDeque;

public class LocationRingBuffer extends ArrayDeque {

  private final int maxSize;

  public LocationRingBuffer(@IntRange(from = 0) int maxSize) {
    this.maxSize = maxSize;
  }

  @Override
  public boolean add(E e) {
    boolean result = super.add(e);
    resize();
    return result;
  }

  @Override
  public void addFirst(E e) {
    super.addFirst(e);
    resize();
  }

  @Override
  public void addLast(E e) {
    super.addLast(e);
    resize();
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    boolean result = super.addAll(collection);
    resize();
    return result;
  }

  @Override
  public void push(E e) {
    super.push(e);
    resize();
  }

  private void resize() {
    while (size() > maxSize) {
      pop();
    }
  }









}
