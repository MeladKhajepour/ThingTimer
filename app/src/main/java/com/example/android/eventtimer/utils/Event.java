package com.example.android.eventtimer.utils;

public class Event {
    private int index;
    private long durationMillis;

    public Event(int index, long durationMillis) {
        this.index = index;
        this.durationMillis = durationMillis;
    }

    public String getLabel() {
        return "Event " + String.valueOf(index);
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getFormattedDuration() {
        return Timer.formatDuration(durationMillis);
    }
}
