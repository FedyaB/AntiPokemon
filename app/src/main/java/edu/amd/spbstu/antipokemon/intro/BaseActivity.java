package edu.amd.spbstu.antipokemon.intro;


import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;

import java.util.Locale;

public abstract class BaseActivity extends FragmentActivity implements View.OnTouchListener {
    private static final String SAVED_BUNDLE_KEY_CURRENT_VIEW_MODE = "CURRENT_VIEW_MODE";
    private static final String SAVED_BUNDLE_KEY_CHEAT_MODE = "CHEAT_MODE";

    public enum ViewMode {
        INTRO,
        APPLICATION
    }

    protected ViewMode mViewMode;
    protected boolean cheatMode;

    private AppIntro mAppIntro;
    private ViewIntro mViewIntro;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cheatMode = false;
        createAppIntro();
        initializeMembers(savedInstanceState);
    }

    private void initializeMembers(@Nullable Bundle savedInstanceState) {
        //Initialize with defaults at first
        mViewMode = null;

        //Assign data from bundle if it exists
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(SAVED_BUNDLE_KEY_CURRENT_VIEW_MODE))
                setView((ViewMode)savedInstanceState.getSerializable(SAVED_BUNDLE_KEY_CURRENT_VIEW_MODE));
            if (savedInstanceState.keySet().contains(SAVED_BUNDLE_KEY_CHEAT_MODE)) {
                mAppIntro.cheatMode = savedInstanceState.getBoolean(SAVED_BUNDLE_KEY_CHEAT_MODE);
                cheatMode = mAppIntro.cheatMode;
            }
            else
                setInitialMode();
        } else {
            setInitialMode();
        }
    }

    protected void setInitialMode() {
            setView(ViewMode.INTRO);
    }

    private void createAppIntro() {
        final String RUSSIAN_LABEL = "russian";
        final String ENGLISH_LABEL = "english";
        String strLang = Locale.getDefault().getDisplayLanguage();
        int language;
        if (strLang.equalsIgnoreCase(ENGLISH_LABEL))
            language = AppIntro.LANGUAGE_ENG;
        else if (strLang.equalsIgnoreCase(RUSSIAN_LABEL))
            language = AppIntro.LANGUAGE_RUS;
        else
            language = AppIntro.LANGUAGE_UNKNOWN;
        mAppIntro = new AppIntro(this, language);
    }

    public boolean onTouch(View v, MotionEvent evt) {
        int x = (int)evt.getX();
        int y = (int)evt.getY();
        int touchType = AppIntro.TOUCH_DOWN;

        if (evt.getAction() == MotionEvent.ACTION_MOVE)
            touchType = AppIntro.TOUCH_MOVE;
        if (evt.getAction() == MotionEvent.ACTION_UP)
            touchType = AppIntro.TOUCH_UP;

        switch (mViewMode) {
            case INTRO:
                boolean res = mAppIntro.onTouch( x, y, touchType);
                cheatMode = mAppIntro.cheatMode;
                return res;
            case APPLICATION:
                return true;
        }
        return true;
    }

    public void setView(ViewMode viewID)
    {
        if (mViewMode == viewID)
            return;
        mViewMode = viewID;
        switch (mViewMode) {
            case INTRO:
                mViewIntro = new ViewIntro(this);
                setContentView(mViewIntro);
                break;
            case APPLICATION:
                break;
        }
    }

    public AppIntro getApp() {
        return mAppIntro;
    }

    @Override
    protected void onResume() {
        super.onResume();
        switch (mViewMode) {
            case INTRO:
                mViewIntro.start();
                break;
            case APPLICATION:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        switch (mViewMode) {
            case INTRO:
                mViewIntro.stop();
                break;
            case APPLICATION:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mViewIntro.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        initializeMembers(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putSerializable(SAVED_BUNDLE_KEY_CURRENT_VIEW_MODE, mViewMode);
            outState.putBoolean(SAVED_BUNDLE_KEY_CHEAT_MODE, cheatMode);
        }
    }
}
