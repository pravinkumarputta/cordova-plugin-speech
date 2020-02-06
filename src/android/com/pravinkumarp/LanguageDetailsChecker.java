package com.pravinkumarp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;

import java.util.ArrayList;
import java.util.List;

public class LanguageDetailsChecker extends BroadcastReceiver {
    
    private List<String> supportedLanguages = new ArrayList<>();
    private String defaultLanguage = "";
    private LanguageDetailsCheckerListener languageDetailsCheckerListener;

    public LanguageDetailsChecker(LanguageDetailsCheckerListener languageDetailsCheckerListener) {
        super();
        this.languageDetailsCheckerListener = languageDetailsCheckerListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle results = getResultExtras(true);

        if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
            defaultLanguage = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE);
        }

        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
            supportedLanguages = results.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
        }

        languageDetailsCheckerListener.onDetailsReceived();
    }

    public List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public interface LanguageDetailsCheckerListener {
        void onDetailsReceived();
    }
}