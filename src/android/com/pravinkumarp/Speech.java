package com.pravinkumarp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class echoes a string called from JavaScript.
 */
public class Speech extends CordovaPlugin {
    // API lists
    private final static String TAG = "Speech";
    private final static String GET_SUPPORTED_LANGUAGES = "getSupportedLanguages";
    private final static String GET_SUPPORTED_VOICES = "getSupportedVoices";
    private final static String GET_DEFAULT_LANGUAGE = "getDefaultLanguage";
    private final static String SPEAK_OUT = "speakOut";
    private final static String INIT_RECOGNITION = "initRecognition";
    private final static String START_RECOGNITION = "startRecognition";
    private final static String STOP_RECOGNITION = "stopRecognition";

    private LanguageDetailsChecker languageDetailsChecker;

    private TextToSpeech textToSpeech;
    private CallbackContext ttsCallbackContext;

    private String selectedLanguage = "en-US";
    private CallbackContext sttCallbackContext;

    private int REQUEST_RECORD_AUDIO = 1234;
    private SpeechRecognizer speechRecognizer;
    private boolean isRecognizing = false;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "echo": {
                String message = args.getString(0);
                this.echo(message, callbackContext);
                break;
            }
            case GET_SUPPORTED_LANGUAGES:
                this.getSupportedLanguages(callbackContext);
                break;
            case GET_SUPPORTED_VOICES:
                this.getSupportedVoices(callbackContext);
                break;
            case GET_DEFAULT_LANGUAGE:
                this.getDefaultLanguage(callbackContext);
                break;
            case SPEAK_OUT: {
                String message = args.getString(0);
                float pitchRate = (float) args.getDouble(1);
                if (pitchRate == 0f) {
                    pitchRate = 1f;
                }
                float speechRate = (float) args.getDouble(2);
                if (speechRate == 0f) {
                    speechRate = 1f;
                }
                String speechLanguageName = (String) args.getString(3);
                this.speakOut(message, pitchRate, speechRate, speechLanguageName, callbackContext);
                break;
            }
            case INIT_RECOGNITION:
                String language = args.getString(0);
                this.initRecognition(language, callbackContext);
                break;
            case START_RECOGNITION:
                boolean partialResultRequired = args.getBoolean(0);
                boolean offlineRecognitionRequired = args.getBoolean(1);

                cordova.getActivity().runOnUiThread(() -> Speech.this.startRecognition(partialResultRequired, offlineRecognitionRequired, callbackContext));
                break;
            case STOP_RECOGNITION:

                cordova.getActivity().runOnUiThread(() -> Speech.this.stopRecognition(callbackContext));
                break;
            default:
                return false;
        }
        return true;
    }

    private void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success("Success");
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void getSupportedLanguages(CallbackContext callbackContext) {
        if (languageDetailsChecker != null) {
            this.publishSupportedLanguagesResult(callbackContext);
            return;
        }

        this.loadLanguageDetails(() -> Speech.this.publishSupportedLanguagesResult(callbackContext));
    }

    private void getSupportedVoices(CallbackContext callbackContext) {
        this.textToSpeech = new TextToSpeech(cordova.getActivity(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                ArrayList<String> supportedVoices = new ArrayList<>();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Set<Voice> voices = textToSpeech.getVoices();
                    if (voices != null) {
                        for (Voice voice : voices) {
                            supportedVoices.add(voice.getLocale() + ":" + voice.getName());
                        }
                    }
                }
                JSONArray voicesArray = new JSONArray(supportedVoices);
                callbackContext.success(voicesArray);
            } else {
                callbackContext.error("Failed to init tts.");
            }
        });
    }

    private void publishSupportedLanguagesResult(CallbackContext callbackContext) {
        List<String> supportedLanguages = languageDetailsChecker.getSupportedLanguages();
        if (supportedLanguages != null) {
            JSONArray languages = new JSONArray(supportedLanguages);
            callbackContext.success(languages);
        } else {
            callbackContext.error("Failed to get supported languages.");
        }
    }

    private void getDefaultLanguage(CallbackContext callbackContext) {
        if (languageDetailsChecker != null) {
            this.publishDefaultLanguageResult(callbackContext);
            return;
        }

        this.loadLanguageDetails(() -> Speech.this.publishDefaultLanguageResult(callbackContext));
    }

    private void publishDefaultLanguageResult(CallbackContext callbackContext) {
        String defaultLanguage = languageDetailsChecker.getDefaultLanguage();
        if (!defaultLanguage.isEmpty()) {
            callbackContext.success(defaultLanguage);
        } else {
            callbackContext.error("Failed to get default language.");
        }
    }

    private void loadLanguageDetails(LanguageDetailsChecker.LanguageDetailsCheckerListener languageDetailsCheckerListener) {
        languageDetailsChecker = new LanguageDetailsChecker(languageDetailsCheckerListener);
        Intent detailsIntent = new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            detailsIntent.setPackage("com.google.android.googlequicksearchbox");
        }
        cordova.getActivity().sendOrderedBroadcast(detailsIntent, null, languageDetailsChecker, null, Activity.RESULT_OK, null, null);
    }

    private void speakOut(String message, float pitchRate, float speechRate, String speechLanguageName, CallbackContext callbackContext) {
        this.ttsCallbackContext = callbackContext;
        this.textToSpeech = new TextToSpeech(cordova.getActivity(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                Speech.this.textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "tts-start");
                        pluginResult.setKeepCallback(true);
                        Speech.this.ttsCallbackContext.sendPluginResult(pluginResult);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Speech.this.ttsCallbackContext.success("tts-end");
                        Speech.this.textToSpeech.stop();
                        Speech.this.textToSpeech.shutdown();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Speech.this.ttsCallbackContext.error("Failed to speak.");
                    }
                });
                Speech.this.speak(message, pitchRate, speechRate, speechLanguageName);
            } else {
                Speech.this.ttsCallbackContext.error("Failed to init tts.");
            }
        });
    }

    private void speak(String message, float pitchRate, float speechRate, String speechLanguageName) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id");
        textToSpeech.setPitch(pitchRate);
        textToSpeech.setSpeechRate(speechRate);

        if (!speechLanguageName.isEmpty() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Set<Voice> voices = textToSpeech.getVoices();
            if (voices != null) {
                for (Voice voice : voices) {
                    if (voice.getName().equals(speechLanguageName)) {
                        textToSpeech.setVoice(voice);
                        break;
                    }
                }
            }
        }
        Speech.this.textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void initRecognition(String language, CallbackContext callbackContext) {
        if (!language.isEmpty()) {
            this.selectedLanguage = language;
        }
        this.sttCallbackContext = callbackContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!cordova.hasPermission(Manifest.permission.RECORD_AUDIO)) {
                cordova.requestPermissions(this, REQUEST_RECORD_AUDIO, new String[]{Manifest.permission.RECORD_AUDIO});
                return;
            }
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("offlineRecognitionAvailable", true);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
            callbackContext.sendPluginResult(pluginResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startRecognition(boolean partialResultRequired, boolean offlineRecognitionRequired, CallbackContext callbackContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (cordova.getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                callbackContext.error("Permission denied");
                return;
            }
        }

        if (isRecognizing) {
            this.speechRecognizer.stopListening();
            speechRecognizer.destroy();
            callbackContext.error("Speech recognition was running, but now it stopped");
            return;
        }

        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(cordova.getActivity());

        this.speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int errorCode) {
                isRecognizing = false;

                String error;
                switch (errorCode) {

                    case SpeechRecognizer.ERROR_AUDIO:
                        error = "Audio recording error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        error = "Client side error";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        error = "Insufficient permissions";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        error = "Network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        error = "Network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        error = "No match";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        error = "RecognitionService busy";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        error = offlineRecognitionRequired ? "Language package not found" : "Server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        error = "No speech input";
                        break;
                    default:
                        error = "Didn't understand, please try again.";
                }

                callbackContext.error(error);
                speechRecognizer.destroy();
            }

            @Override
            public void onResults(Bundle finalResults) {
                isRecognizing = false;
                ArrayList<String> matches = finalResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("isFinal", true);
                        jsonObject.put("text", matches.get(0));

                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
                        pluginResult.setKeepCallback(false);
                        callbackContext.sendPluginResult(pluginResult);
                        speechRecognizer.destroy();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("isFinal", false);
                        jsonObject.put("text", matches.get(0));

                        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
                        pluginResult.setKeepCallback(true);
                        callbackContext.sendPluginResult(pluginResult);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });


        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, cordova.getActivity().getCallingPackage());

        // partial result
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResultRequired);

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        // offline speech to text
        if (offlineRecognitionRequired) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            }
        }

        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // selected language
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage);

        speechRecognizer.startListening(recognizerIntent);
        isRecognizing = true;
    }

    private void stopRecognition(CallbackContext callbackContext) {
        if (this.speechRecognizer != null && isRecognizing) {
            isRecognizing = false;
            this.speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, true);
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if ((grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                this.initRecognition(this.selectedLanguage, this.sttCallbackContext);
            } else {
                this.sttCallbackContext.error("Permission denied");
            }
        }
    }
}