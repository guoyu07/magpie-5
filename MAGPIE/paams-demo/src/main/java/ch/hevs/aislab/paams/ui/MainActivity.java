package ch.hevs.aislab.paams.ui;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import ch.hevs.aislab.magpie.agent.MagpieAgent;
import ch.hevs.aislab.magpie.agent.PrologAgentMind;
import ch.hevs.aislab.magpie.android.MagpieActivity;
import ch.hevs.aislab.magpie.environment.Services;
import ch.hevs.aislab.magpie.event.LogicTupleEvent;
import ch.hevs.aislab.paams.model.DoubleValue;
import ch.hevs.aislab.paams.model.SingleValue;
import ch.hevs.aislab.paams.model.Type;
import ch.hevs.aislab.paams.model.Value;
import ch.hevs.aislab.paamsdemo.R;

public class MainActivity extends MagpieActivity implements
        AddValueFragment.OnAddedNewMeasurementListener, AlertFragment.OnFragmentInteractionListener {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupInstances();
        setNavigationView();

    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupInstances() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(MainActivity.this, drawerLayout, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(drawerToggle);
    }

    private void setNavigationView() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);

        if (navigationView != null) {
            prepareDrawer(navigationView);
            navigate(navigationView.getMenu().getItem(0));
        }
    }

    private void prepareDrawer(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        item.setChecked(true);
                        navigate(item);
                        drawerLayout.closeDrawers();
                        return true;
                    }
                }
        );
    }

    private void navigate(MenuItem itemDrawer) {
        Fragment genericFragment = null;
        FragmentManager fragmentManager = getSupportFragmentManager();
        String fragmentTag = null;

        switch (itemDrawer.getItemId()) {
            case R.id.item_glucose:
                genericFragment = PhysioParamFragment.newInstance(Type.GLUCOSE);
                fragmentTag = Type.GLUCOSE.name();
                break;
            case R.id.item_blood_pressure:
                genericFragment = PhysioParamFragment.newInstance(Type.BLOOD_PRESSURE);
                fragmentTag = Type.BLOOD_PRESSURE.name();
                break;
            case R.id.item_weight:
                genericFragment = PhysioParamFragment.newInstance(Type.WEIGHT);
                fragmentTag = Type.WEIGHT.name();
                break;
            case R.id.item_alerts:
                genericFragment = AlertFragment.newInstance("", "");
                break;
        }

        if (genericFragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.main_container, genericFragment, fragmentTag)
                    .commit();
        }

        setTitle(itemDrawer.getTitle());
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void addSingleValue(SingleValue measurement) {
        PhysioParamFragment fragment = null;
        switch (measurement.getType()) {
            case GLUCOSE:
                fragment = (PhysioParamFragment)
                        getSupportFragmentManager().findFragmentByTag(Type.GLUCOSE.name());
                break;
            case WEIGHT:
                fragment = (PhysioParamFragment)
                        getSupportFragmentManager().findFragmentByTag(Type.WEIGHT.name());
                break;
        }
        PhysioParamFragment.SectionsAdapter adapter =
                (PhysioParamFragment.SectionsAdapter) fragment.getViewPager().getAdapter();
        ListValuesFragment listValuesFragment = (ListValuesFragment) adapter.getItem(0);
        listValuesFragment.getValueAdapter().addItem(measurement);
        // Send also the measurement to MAGPIE
        sendEvent(measurement);
    }

    @Override
    public void addDoubleValue(DoubleValue measurement) {
        PhysioParamFragment fragment = (PhysioParamFragment)
                getSupportFragmentManager().findFragmentByTag(Type.BLOOD_PRESSURE.name());
        PhysioParamFragment.SectionsAdapter adapter =
                (PhysioParamFragment.SectionsAdapter) fragment.getViewPager().getAdapter();
        ListValuesFragment listValuesFragment = (ListValuesFragment) adapter.getItem(0);
        listValuesFragment.getValueAdapter().addItem(measurement);
        // Send also the measurement to MAGPIE
        sendEvent(measurement);
    }

    /**
     * MAGPIE related methods
     */
    @Override
    public void onEnvironmentConnected() {
        MagpieAgent agent = new MagpieAgent("demo-agent", Services.LOGIC_TUPLE);
        PrologAgentMind mind = new PrologAgentMind(getApplicationContext(), R.raw.demo_rules);
        agent.setMind(mind);
        registerAgent(agent);
    }

    @Override
    public void onAlertProduced(LogicTupleEvent alert) {
        Log.i("MainActivity", "Alert name: " + alert.getName());
        Log.i("MainActivity", "Alert tuple: " + alert.toTuple());
        Log.i("MainActivity" , alert.getArguments().get(0));
        Log.i("MainActivity", "Alert date: " + alert.getStringTimestamp("dd.MM.yyyy H:mm"));
    }

    private void sendEvent(Value measurement) {
        LogicTupleEvent event = null;
        long timestamp = measurement.getTimestamp();
        String name = measurement.getType().name().toLowerCase();
        if (measurement instanceof SingleValue) {
            String value = String.valueOf(((SingleValue)measurement).getValue());
            event = new LogicTupleEvent(timestamp, name, value);
        } else if (measurement instanceof DoubleValue) {
            String sys = String.valueOf(((DoubleValue)measurement).getFirstValue());
            String dias = String.valueOf(((DoubleValue)measurement).getSecondValue());
            event = new LogicTupleEvent(timestamp, name, sys, dias);
        }
        sendEvent(event);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
