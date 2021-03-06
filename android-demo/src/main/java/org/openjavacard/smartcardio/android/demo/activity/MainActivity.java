package org.openjavacard.smartcardio.android.demo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.openjavacard.smartcardio.android.demo.R;
import org.openjavacard.smartcardio.android.demo.application.DemoPreferences;
import org.openjavacard.smartcardio.android.demo.fragment.TerminalInfoFragment;
import org.openjavacard.smartcardio.android.demo.fragment.TerminalListFragment;
import org.openjavacard.smartcardio.android.nfc.AndroidNfcSCIO;
import org.openjavacard.smartcardio.android.omapi.AndroidOmapiSCIO;
import org.openjavacard.smartcardio.generic.GenericCardTerminal;
import org.openjavacard.smartcardio.generic.GenericCardTerminals;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements GenericCardTerminals.Listener,
        SharedPreferences.OnSharedPreferenceChangeListener, FragmentManager.OnBackStackChangedListener{

    private static final String TAG = MainActivity.class.getName();

    private Handler mHandler;

    private SharedPreferences mPreferences;

    private FragmentManager mFragmentManager;
    private TerminalListFragment mTerminalList;
    private TerminalInfoFragment mTerminalInfo;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private Runnable mWakeTimer;

    private AndroidNfcSCIO mNfcSC;
    private AndroidOmapiSCIO mOmapiSC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        // set the layout
        setContentView(R.layout.activity_main);
        // set up action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.activity_main_title);
        // create handler
        mHandler = new Handler(getMainLooper());
        // get prefs
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        // get fragment manager and set up up navigation
        mFragmentManager = getSupportFragmentManager();
        mFragmentManager.addOnBackStackChangedListener(this);
        // create fragments
        mTerminalList = new TerminalListFragment();
        mTerminalInfo = new TerminalInfoFragment();
        // create SCIO interfaces
        mNfcSC = new AndroidNfcSCIO(this);
        mNfcSC.getTerminals().setListener(this);
        mOmapiSC = new AndroidOmapiSCIO(this);
        mOmapiSC.getTerminals().setListener(this);
        // set up wake lock
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeTimer = new Runnable() {
            @Override
            public void run() {
                releaseWakeLock();
            }
        };
        // start with the terminal list
        switchToTerminalList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected(" + item.getTitle() + ")");
        switch (item.getItemId()) {
            case R.id.action_settings:
                startSettings();
                return true;
            case R.id.action_about:
                startAbout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        mNfcSC.enable();
        mOmapiSC.enable();
        acquireWakeLock();
        updateUpEnabled();
        updateTerminals();
        updateWakeTimer();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        releaseWakeLock();
        mOmapiSC.disable();
        mNfcSC.disable();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mNfcSC = null;
        super.onDestroy();
    }

    @Override
    public void onTerminalAdded(GenericCardTerminals terminals, GenericCardTerminal terminal) {
        Log.d(TAG, "onTerminalAdded(" + terminal.getName() + ")");
        updateTerminals();
    }

    @Override
    public void onTerminalRemoved(GenericCardTerminals terminals, GenericCardTerminal terminal) {
        Log.d(TAG, "onTerminalRemoved(" + terminal.getName() + ")");
        updateTerminals();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onPreferenceChange(" + key + ")");
        switch(key) {
            case DemoPreferences.STAY_AWAKE:
                updateWakeTimer();
                break;
            case DemoPreferences.USE_NFC:
            case DemoPreferences.USE_OMAPI:
                updateTerminals();
                break;
        }
    }

    @Override
    public void onBackStackChanged() {
        Log.d(TAG, "onBackStackChanged()");
        updateUpEnabled();
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp()");
        mFragmentManager.popBackStack();
        return true;
    }

    private void acquireWakeLock() {
        Log.d(TAG, "acquireWakeLock()");
        if(!mWakeLock.isHeld()) {
            mWakeLock.acquire();
            updateWakeTimer();
        }
    }

    private void releaseWakeLock() {
        Log.d(TAG, "releaseWakeLock()");
        if(mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void updateUpEnabled() {
        boolean haveBackStack = mFragmentManager.getBackStackEntryCount() > 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(haveBackStack);
    }

    private void updateWakeTimer() {
        Log.d(TAG, "updateWakeTimer()");
        String stayAwakeString = mPreferences.getString(DemoPreferences.STAY_AWAKE, "60");
        int stayAwake = Integer.parseInt(stayAwakeString);
        mHandler.removeCallbacks(mWakeTimer);
        if(stayAwake > 0) {
            mHandler.postDelayed(mWakeTimer, stayAwake * 1000);
        }
    }

    private void startSettings() {
        Log.d(TAG, "startSettings()");
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void startAbout() {
        Log.d(TAG, "startAbout()");
        startActivity(new Intent(this, AboutActivity.class));
    }

    public void switchToTerminalList() {
        Log.d(TAG, "switchToTerminalList()");
        updateWakeTimer();
        updateTerminals();
        mFragmentManager.beginTransaction()
                .replace(R.id.main_layout, mTerminalList)
                .commit();
    }

    public void switchToTerminalInfo(GenericCardTerminal terminal) {
        Log.d(TAG, "switchToTerminalInfo(" + terminal + ")");
        updateWakeTimer();
        mTerminalInfo.setTerminal(terminal);
        mFragmentManager.beginTransaction()
                .replace(R.id.main_layout, mTerminalInfo)
                .addToBackStack(null)
                .commit();
    }

    private void updateTerminals() {
        Log.d(TAG, "updateTerminals()");
        // check preferences
        boolean useOMAPI = mPreferences.getBoolean(DemoPreferences.USE_OMAPI, false);
        boolean useNFC = mPreferences.getBoolean(DemoPreferences.USE_NFC, false);
        // collect all terminals
        List<GenericCardTerminal> terminals = new ArrayList<>();
        if(useOMAPI) {
            terminals.addAll(mOmapiSC.getTerminals().getTerminals());
        }
        if(useNFC) {
            terminals.addAll(mNfcSC.getTerminals().getTerminals());
        }
        // update the list
        mTerminalList.setTerminals(terminals);
    }

}
