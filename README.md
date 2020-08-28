# cordova-plugin-speech

[![npm](https://img.shields.io/npm/v/cordova-plugin-speech.svg)](https://www.npmjs.com/package/cordova-plugin-speech)
![Platform](https://img.shields.io/badge/platform-android%20%7C%20ios-lightgrey.svg)

This is cordova plugin for Speech Recognition and Text to Speech.


## Installation

```
cordova plugin add cordova-plugin-speech
```

## Supported Platforms

- Android
- iOS

## Usage

This plugin works with internet connection and without internet if language package available on device.

### supportedLanguages

```js
Speech.supportedLanguages
```

`[]String` - Returns list of supported Speech to Text language models.

### getSupportedVoices()

```js
Speech.getSupportedVoices(Function successCallback, Function errorCallback)
```

Result of success callback is an Array of supported voices.

### defaultLanguage

```js
Speech.supportedLanguages
```

`String` - Returns systems default Speech to Text language model.

### speakOut()

```js

let options = {
  Number pitchRate,
  Number speechRate,
  String language
}

Speech.speakOut(String message, Function successCallback, Function errorCallback, Object options)
```

`message` - {String} message for speaking.

Result of success callback is an `String` of TTS states as below:

- `tts-start` States that TTS started.
- `tts-end` States that TTS ended.

This method has an options parameter with the following optional values:

- `pitchRate` {Number} used for TTS pitch rate.
1. `iOS` - The default pitch is 1.0. Allowed values are in the range from 0.5 (for lower pitch) to 2.0 (for higher pitch).
1. `Android` - The default pitch is 1.0, lower values lower the tone of the synthesized voice, greater values increase it.
- `speechRate`  {Number} used for TTS speech rate.
1. `iOS` - The default speech rate is 0.5. Lower values correspond to slower speech, and vice versa.
1. `Android` - The default speech rate is 1.0. Lower values correspond to slower speech, and vice versa.
- `language`  {String} used for TTS speech voice.
1. `iOS` - Use `Speech.getSupportedVoices()` to get voices and use `selectedVoice.language` as input.
1. `Android` - Use `Speech.getSupportedVoices()` to get voices and use `selectedVoice.name` as input.

### initRecognition()

```js

let options = {
  String language
}

Speech.initRecognition(
  Function successCallback, Function errorCallback, Object options)
```

Result of success callback is an `Object` of recognition features as below:
- `offlineRecognitionAvailable` {Boolean} states that offline speech recognition is available or not. For Android it's values is always true.

This method has an options parameter with the following optional values:

- `language` {String} used language for recognition (default is systems default lanuguage model).

### startRecognition()

```js

let options = {
  Boolean partialResultRequired,
  Boolean offlineRecognitionRequired
}

Speech.startRecognition(
  Function successCallback, Function errorCallback, Object options)
```

Result of success callback is an `Object` of recognized terms:

- `isFinal` {Boolean} In case of partial result it is `false` otherwise it's `true`.
- `text` {String} Recognized text.

This method has an options parameter with the following optional values:

- `partialResultRequired` {Boolean} Allow partial results to be returned (default `false`)
- `offlineRecognitionRequired` {Boolean} Enables offline speech recognition if language model available (default `false`)

There is a difference between Android and iOS platforms. On Android speech recognition stops when the speaker finishes speaking (at end of sentence). On iOS the user has to stop manually the recognition process by calling stopRecognition() method.


### stopRecognition()

```js
Speech.stopRecognition(
  Function successCallback, Function errorCallback)
```

Stop the recognition process. Returns `true`.


## Android Quirks

### Requirements

- cordova-android v5.0.0
- Android API level 14
- [RECORD_AUDIO](https://developer.android.com/reference/android/Manifest.permission.html#RECORD_AUDIO) permission

### Further readings

- https://developer.android.com/reference/android/speech/package-summary.html
- https://developer.android.com/reference/android/speech/SpeechRecognizer.html

## iOS Quirks

### Requirements

- XCode 8.0 (requires 10.12+ macOS Sierra or 10.11.5+ OS X El Capitan)
- iOS 10
- [NSMicrophoneUsageDescription](https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW25) permission
- [NSSpeechRecognitionUsageDescription](https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW52) permission

### Further readings

- https://developer.apple.com/reference/speech?language=swift
- https://developer.apple.com/reference/speech/sfspeechrecognizer?language=swift

## Author

### Pravinkumar Putta

- https://github.com/pravinkumarputta


## LICENSE

**cordova-plugin-speech** is licensed under the MIT Open Source license. For more information, see the LICENSE file in this repository.