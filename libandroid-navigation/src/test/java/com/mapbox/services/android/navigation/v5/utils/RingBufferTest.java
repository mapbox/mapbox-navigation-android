package com.mapbox.services.android.navigation.v5.utils;

import static junit.framework.Assert.assertEquals;

import com.google.common.collect.Lists;
import com.mapbox.services.android.navigation.v5.BaseTest;

import org.junit.Test;

public class RingBufferTest extends BaseTest {

  @Test
  public void testBounds() {
    RingBuffer<Integer> buffer = new RingBuffer<>(1);
    buffer.add(1);
    buffer.addFirst(2);
    buffer.addLast(3);
    buffer.addAll(Lists.newArrayList(4));
    buffer.push(5);
    buffer.add(6);

    assertEquals(1, buffer.size());
  }

  @Test
  public void testLifoOrder() {
    RingBuffer<Integer> buffer = new RingBuffer<>(1);
    buffer.add(1);
    buffer.add(2);

    assertEquals(1, buffer.size());
    assertEquals(2, buffer.pop(), DELTA);
  }

  @Test
  public void testFifo() throws Exception {
    RingBuffer<Integer> buffer = new RingBuffer<>(2);
    buffer.add(1);
    buffer.add(2);

    assertEquals(2, buffer.size());
    assertEquals(1, buffer.pop(), DELTA);
  }

  @Test
  public void testPeek() {
    RingBuffer<Integer> buffer = new RingBuffer<>(2);
    buffer.add(1);
    buffer.add(2);
    buffer.add(3);
    assertEquals(2, buffer.size());
    assertEquals(2, buffer.peekFirst(), DELTA);
    assertEquals(3, buffer.peekLast(), DELTA);
  }
}