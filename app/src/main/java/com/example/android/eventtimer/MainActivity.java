package com.example.android.eventtimer;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import static com.example.android.eventtimer.utils.Constants.EVENTS_FRAGMENT;
import static com.example.android.eventtimer.utils.Constants.STATS_EXPANSION;
import static com.example.android.eventtimer.utils.Constants.STATS_FRAGMENT;
import static com.example.android.eventtimer.utils.Constants.TIMER_FRAGMENT;
import static com.example.android.eventtimer.utils.EventsManager.PREFS;

public class MainActivity extends AppCompatActivity {

    private TimerFragment timerFragment;
    private StatsFragment statsFragment;
    private EventsFragment eventsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("onCreate");
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setupFragments();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_reset:
                onMenuReset();
                return true;

            case R.id.action_more:
                //todo start more activity
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupFragments() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        boolean isExpanded = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(STATS_EXPANSION, false);
        Bundle args = new Bundle();
        args.putBoolean(STATS_EXPANSION, isExpanded);

        timerFragment = new TimerFragment();
        eventsFragment = new EventsFragment(); //todo here
        statsFragment = new StatsFragment();
        statsFragment.setArguments(args);

        ft.add(timerFragment, TIMER_FRAGMENT)
                .add(eventsFragment, EVENTS_FRAGMENT)
                .add(statsFragment, STATS_FRAGMENT)
        .commit();
    }

    private void onMenuReset() {
        eventsFragment.clearEvents();
        statsFragment.resetStats();
        timerFragment.clearTimer();
    }

    public void onEventAdded() {
        eventsFragment.refreshList();
        statsFragment.refreshStats();
    }

    public void updateStatsFragment() {
        statsFragment.refreshStats();
    }

    public void undo() {
        statsFragment.refreshStats();
        timerFragment.undoResetIndex();
    }
}
