/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.demo;

import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Samples the JVM heap on a daemon thread and prints a live ASCII bar graph to the terminal.
 * Everything it prints comes from {@link MemoryMXBean}; nothing is simulated.
 */
final class HeapGraph implements AutoCloseable {

    private static final int WIDTH = 50;
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private final MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
    private final PrintStream out;
    private final long intervalMillis;
    private final long started = System.nanoTime();
    private final AtomicLong peakUsed = new AtomicLong();
    private final Thread thread;
    private volatile boolean running = true;
    private volatile String phase = "starting";

    HeapGraph(PrintStream out, long intervalMillis) {
        this.out = out;
        this.intervalMillis = intervalMillis;
        this.thread = new Thread(this::loop, "heap-graph");
        this.thread.setDaemon(true);
    }

    HeapGraph start() {
        thread.start();
        return this;
    }

    void phase(String phase) {
        this.phase = phase;
    }

    long peakUsedBytes() {
        return peakUsed.get();
    }

    long maxBytes() {
        return memory.getHeapMemoryUsage().getMax();
    }

    private void loop() {
        while (running) {
            sample();
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    void sample() {
        MemoryUsage heap = memory.getHeapMemoryUsage();
        long used = heap.getUsed();
        long max = heap.getMax();
        peakUsed.accumulateAndGet(used, Math::max);
        double ratio = max > 0 ? (double) used / max : 0;
        int filled = (int) Math.round(ratio * WIDTH);
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < WIDTH; i++) {
            bar.append(i < filled ? '#' : '.');
        }
        String colour = ratio > 0.85 ? RED : ratio > 0.6 ? YELLOW : GREEN;
        double seconds = (System.nanoTime() - started) / 1e9;
        out.printf("%s[heap %6.1fs] |%s| %5d / %5d MB %3d%%  %s%s%n",
                colour, seconds, bar, used >> 20, max >> 20, (int) (ratio * 100), phase, RESET);
    }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        sample();
    }
}
