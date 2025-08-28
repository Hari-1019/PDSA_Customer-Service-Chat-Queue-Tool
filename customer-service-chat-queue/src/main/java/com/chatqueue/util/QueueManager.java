package com.chatqueue.util;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueManager<T> {
    private final Queue<T> q = new ConcurrentLinkedQueue<>();

    public void add(T item) { q.add(item); }
    public T poll() { return q.poll(); }
    public int size() { return q.size(); }

    // For position calculation when T has a tempId getter-like shape,
    // callers can wrap T or compute separately; here we support a generic index scan
    public int indexOf(Object match) {
        int i = 0;
        for (T t : q) {
            if (t != null && t.equals(match)) return i;
            i++;
        }
        return -1;
    }

    // Overload: when T has a "tempId()" method; we try reflectively to find position by tempId
    public int indexOf(String tempId) {
        int idx = 0;
        try {
            for (T t : q) {
                if (t == null) { idx++; continue; }
                var m = t.getClass().getMethod("tempId");
                Object val = m.invoke(t);
                if (tempId != null && tempId.equals(val)) return idx;
                idx++;
            }
        } catch (Exception ignored) { /* fallback to not found */ }
        return -1;
    }

    public Iterator<T> iterator() { return q.iterator(); }
}
