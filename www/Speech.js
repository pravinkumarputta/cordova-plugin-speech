var exec = require('cordova/exec');
var channel = require('cordova/channel');

const SPEECH = 'Speech'
const API_LIST = {
    GET_SUPPORTED_LANGUAGES: "getSupportedLanguages",
    GET_SUPPORTED_VOICES: "getSupportedVoices",
    GET_DEFAULT_LANGUAGE: "getDefaultLanguage",
    SPEAK_OUT: "speakOut",
    INIT_RECOGNITION: "initRecognition",
    START_RECOGNITION: "startRecognition",
    STOP_RECOGNITION: "stopRecognition",
}

function Speech() {
    this.supportedLanguages = null
    this.defaultLanguage = null

    var me = this

    channel.onCordovaReady.subscribe(function () {
        me[API_LIST.GET_SUPPORTED_LANGUAGES](supportedLanguages => {
            me.supportedLanguages = supportedLanguages
            me[API_LIST.GET_DEFAULT_LANGUAGE](defaultLanguage => {
                me.defaultLanguage = defaultLanguage
            }, err => {
                console.error(err)
            })
        }, err => {
            console.error(err)
        })
    })
}

Speech.prototype[API_LIST.GET_SUPPORTED_LANGUAGES] = function (success, error) {
    exec(success, error, SPEECH, API_LIST.GET_SUPPORTED_LANGUAGES, [])
}

Speech.prototype[API_LIST.GET_SUPPORTED_VOICES] = function (success, error) {
    exec((res) => {
        let voiceList = []
        res.forEach(element => {
            let dataParts = element.split(":")
            voiceList.push({
                language: dataParts[0],
                name: dataParts[1]
            })
        });
        success(voiceList)
    }, error, SPEECH, API_LIST.GET_SUPPORTED_VOICES, [])
}

Speech.prototype[API_LIST.GET_DEFAULT_LANGUAGE] = function (success, error) {
    exec(success, error, SPEECH, API_LIST.GET_DEFAULT_LANGUAGE, [])
}

Speech.prototype[API_LIST.SPEAK_OUT] = function (message, success, error, options) {
    let pitchRate = options && options.pitchRate ? options.pitchRate : 0.0
    let speechRate = options && options.speechRate ? options.speechRate : 0.0
    let language = options && options.language ? options.language : ""
    exec(success, error, SPEECH, API_LIST.SPEAK_OUT, [message, pitchRate, speechRate, language])
}

Speech.prototype[API_LIST.INIT_RECOGNITION] = function (success, error, options) {
    let language = options && options.language ? options.language : ""
    exec(success, error, SPEECH, API_LIST.INIT_RECOGNITION, [language])
}

Speech.prototype[API_LIST.START_RECOGNITION] = function (success, error, options) {
    let partialResultRequired = options && options.partialResultRequired ? options.partialResultRequired : false
    let offlineRecognitionRequired = options && options.offlineRecognitionRequired ? options.offlineRecognitionRequired : false
    exec(success, error, SPEECH, API_LIST.START_RECOGNITION, [partialResultRequired, offlineRecognitionRequired])
}

Speech.prototype[API_LIST.STOP_RECOGNITION] = function (success, error) {
    exec(success, error, SPEECH, API_LIST.STOP_RECOGNITION, [])
}

module.exports = new Speech();
