package com.epam;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CacheGuavaLRU {

  private final static long CACHE_MAX_SIZE = 100000L;

  private static ExecutorService executorService = Executors.newCachedThreadPool();
  private static Random random = new Random();
  private static Cache<Long, Entry> cache = CacheBuilder.newBuilder()
      .maximumSize(CACHE_MAX_SIZE)
      .expireAfterAccess(5, TimeUnit.SECONDS)
      .concurrencyLevel(10)
      .removalListener(new Listener())
      .build();

  public static void main(String[] args) throws InterruptedException {

    // fill cache
    for (long i = 0; i < CACHE_MAX_SIZE; i++) {
      cache.put(i, new Entry("Entry " + i));
    }

    // start parallel read
    for (int i = 0; i < 20; i++) {
      executorService.submit(() -> {
        while (true) {
          long key = random.nextInt((int)CACHE_MAX_SIZE);
          Thread.sleep(1000);
          cache.getIfPresent(new Entry("Entry " + key));
        }
      });
    }
    // start threat of reading first 100 as most accessed for sure that cache works correctly
    executorService.submit(() -> {
      while (true) {
        Thread.sleep(10);
        cache.getIfPresent(new Entry("Entry " + random.nextInt(100)));
      }
    });

    // wait for randomly access
    System.out.println("waiting...");
    Thread.sleep(10000);

    // start parallel write
    for (int i = 0; i < 2; i++) {
      executorService.submit(() -> {
        while (true) {
          long key = getRandomLong();
          Thread.sleep(random.nextInt(10) * 100);
          cache.put(key, new Entry("Value" + key));
        }
      });
    }
  }

  private static long getRandomLong() {
    long r = random.nextLong();
    long exceptExisting = r > CACHE_MAX_SIZE ? r : r + CACHE_MAX_SIZE;
    if (exceptExisting < 0)
      return getRandomLong();

    return exceptExisting;
  }
}

class Listener implements RemovalListener<Long, Entry> {
  @Override
  public void onRemoval(RemovalNotification<Long, Entry> removalNotification) {
    System.out.println(removalNotification.getCause() + " " + removalNotification.getValue());
  }
}

class Entry {

  private String data;

  public Entry(String data) {
    this.data = data;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Entry entry = (Entry) o;
    return Objects.equals(data, entry.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }
}
