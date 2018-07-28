package com.example.android.eventtimer.utils;

import android.content.SharedPreferences;

import org.apache.commons.math3.distribution.TDistribution;

import java.util.List;

import static com.example.android.eventtimer.StatsFragment.useListStats;

public class StatsManager {
    private static final String SHORTEST_EVENT = "shortestEvent";
    private static final String AVERAGE_TIME = "averageTime";
    private static final String LONGEST_EVENT = "longestEvent";
    private static final String STD_DEV = "stdDev";
    private static final String MOE = "moe";
    public static final String SELECTED_CONFIDENCE_VALUE = "confidence_value";
    public static final String SELECTED_CONFIDENCE_ITEM = "confidence_selection";

    public static void updateEventAdded(SharedPreferences prefs, Event event) {
        int eventListSize = EventsManager.getEventListSize(prefs);

        long newTime = event.getDurationMillis();

        if(eventListSize == 1) {
            updateSingleEvent(prefs, newTime);
        } else {
            long longestEventMillis = getLongestEvent(prefs);
            long shortestEventMillis = getShortestEvent(prefs);
            long averageTimeMillis = getAverageTime(prefs);

            averageTimeMillis = (averageTimeMillis * (eventListSize - 1) + newTime) / eventListSize;
            longestEventMillis = newTime > longestEventMillis ? newTime : longestEventMillis;
            shortestEventMillis = newTime < shortestEventMillis ? newTime : shortestEventMillis;

            setShortestEvent(prefs, shortestEventMillis);
            setAverageTime(prefs, averageTimeMillis);
            setLongestEvent(prefs, longestEventMillis);
        }

        calculateAndSetStdDev(prefs);
        calculateAndSetMoe(prefs);
    }

    public static void undoRemoveEvents(SharedPreferences prefs) {
        recalculateListStats(prefs);
    }

    public static void updateEventsRemoved(SharedPreferences prefs, List<Event> removedEventsList) {
        if(useListStats) {
            int eventListSize = EventsManager.getEventListSize(prefs);

            if(eventListSize == 0) {
                setShortestEvent(prefs, 0); // todo
                setAverageTime(prefs, 0); // todo
                setLongestEvent(prefs, 0);
            } else if(eventListSize == 1) {
                updateSingleEvent(prefs, EventsManager.getAllEvents(prefs).get(0).getDurationMillis());
            } else {
                long totalRemovedTime = 0;
                long averageTimeMillis = getAverageTime(prefs);
                boolean updateLongest = false;
                boolean updateShortest = false;

                for(Event removedEvent : removedEventsList) {
                    long removedTime = removedEvent.getDurationMillis();

                    totalRemovedTime += removedTime;

                    if(removedTime == getShortestEvent(prefs)) {
                        updateShortest = true;
                    }

                    if(removedTime == getLongestEvent(prefs)) {
                        updateLongest = true;
                    }
                }

                averageTimeMillis = (averageTimeMillis * (eventListSize + removedEventsList.size()) - totalRemovedTime) / eventListSize;

                if(updateShortest) {
                    recalculateShortestTime(prefs);
                }

                if(updateLongest) {
                    recalculateLongestTime(prefs);
                }

                setAverageTime(prefs, averageTimeMillis);
            }

            calculateAndSetStdDev(prefs);
            calculateAndSetMoe(prefs);
        }
    }

    public static void resetStats(SharedPreferences prefs) {
        setShortestEvent(prefs, 0);
        setAverageTime(prefs, 0);
        setLongestEvent(prefs, 0);

        calculateAndSetMoe(prefs);
        calculateAndSetStdDev(prefs);
    }

    public static void recalculateListStats(SharedPreferences prefs) {
        List<Event> eventList = EventsManager.getAllEvents(prefs);
        long averageTimeMillis = 0;
        long longestEventMillis = 0;
        long shortestEventMillis = (long) Double.POSITIVE_INFINITY;

        if(!eventList.isEmpty()) {
            long currentDuration;

            for (Event event : eventList) {
                currentDuration = event.getDurationMillis();
                averageTimeMillis += currentDuration;
                longestEventMillis = currentDuration > longestEventMillis ? currentDuration : longestEventMillis;
                shortestEventMillis = currentDuration < shortestEventMillis ? currentDuration : shortestEventMillis;
            }

            averageTimeMillis /= eventList.size();
        }

        setShortestEvent(prefs, shortestEventMillis);
        setAverageTime(prefs, averageTimeMillis);
        setLongestEvent(prefs, longestEventMillis);
        calculateAndSetStdDev(prefs);
        calculateAndSetMoe(prefs);
    }

    private static void recalculateShortestTime(SharedPreferences prefs) {
        long shortestEventMillis = (long) Double.POSITIVE_INFINITY;

        for(Event event : EventsManager.getAllEvents(prefs)) {
            shortestEventMillis = event.getDurationMillis() < shortestEventMillis ?
                    event.getDurationMillis() : shortestEventMillis;
        }

        StatsManager.setShortestEvent(prefs, shortestEventMillis);
    }

    private static void recalculateLongestTime(SharedPreferences prefs) {
        long longestEventMillis = 0;

        for(Event event : EventsManager.getAllEvents(prefs)) {
            longestEventMillis = event.getDurationMillis() > longestEventMillis ?
                    event.getDurationMillis() : longestEventMillis;
        }

        StatsManager.setLongestEvent(prefs, longestEventMillis);
    }

    private static void updateSingleEvent(SharedPreferences prefs, long time) {
        setShortestEvent(prefs, time);
        setAverageTime(prefs, time);
        setLongestEvent(prefs, time);
    }

    public static void setShortestEvent(SharedPreferences prefs, long time) {
        prefs.edit().putLong(SHORTEST_EVENT, time).apply();
    }

    public static void setAverageTime(SharedPreferences prefs, long time) {
        prefs.edit().putLong(AVERAGE_TIME, time).apply();
    }

    public static void setLongestEvent(SharedPreferences prefs, long time) {
        prefs.edit().putLong(LONGEST_EVENT, time).apply();
    }

    private static void calculateAndSetStdDev(SharedPreferences prefs) {
        long time = 0;
        List<Event> eventList = EventsManager.getAllEvents(prefs);

        if(eventList.size() > 1) {
            long mean = getAverageTime(prefs);
            long variance = 0;

            for(Event event : eventList) {
                variance += Math.pow((event.getDurationMillis() - mean), 2);
            }

            time = (long) Math.sqrt(variance/(eventList.size() - 1));
        }

        prefs.edit().putLong(STD_DEV, time).apply();
    }

    private static void calculateAndSetMoe(SharedPreferences prefs) {
        int n = EventsManager.getEventListSize(prefs);
        float moe = 0;

        if(n > 1) {
            int df = n - 1;
            double alpha = getConfidenceValue(prefs);
            double p = 1 - (alpha / 2);

            TDistribution dist = new TDistribution(df);
            float t = (float) dist.inverseCumulativeProbability(p);
            moe = (float) (getStdDev(prefs) / Math.sqrt(n)) * t;
        }

        prefs.edit().putFloat(MOE, moe).apply();
    }

    public static void changeConfidence(SharedPreferences prefs, int selected) {
        setConfidenceValue(prefs, selected);
        calculateAndSetMoe(prefs);
        prefs.edit().putInt(SELECTED_CONFIDENCE_ITEM, selected).apply();
    }

    private static void setConfidenceValue(SharedPreferences prefs, int selection) {
        float conf;
        switch (selection) {
            case 0: conf = 0.01f;
            break;

            case 1: conf = 0.05f;
            break;

            case 2: conf = 0.1f;
            break;

            default: conf = 0.2f;
            break;
        }

        prefs.edit().putFloat(SELECTED_CONFIDENCE_VALUE, conf).apply();
    }

    public static long getShortestEvent(SharedPreferences prefs) {
        return prefs.getLong(SHORTEST_EVENT, 0);
    }

    public static long getAverageTime(SharedPreferences prefs) {
        return prefs.getLong(AVERAGE_TIME, 0);
    }

    public static long getLongestEvent(SharedPreferences prefs) {
        return prefs.getLong(LONGEST_EVENT, 0);
    }

    public static long getStdDev(SharedPreferences prefs) {
        return prefs.getLong(STD_DEV, 0);
    }

    public static long getmoe(SharedPreferences prefs) {
        return (long) prefs.getFloat(MOE, 0);
    }

    public static int getConfidence(SharedPreferences prefs) {
        return prefs.getInt(SELECTED_CONFIDENCE_ITEM, 2);
    }

    private static float getConfidenceValue(SharedPreferences prefs) {
        return prefs.getFloat(SELECTED_CONFIDENCE_VALUE, 0.1f);
    }
}
