import Speech
/*
 * Notes: The @objc shows that this class & function should be exposed to Cordova.
 */
@objc(Speech) class Speech : CDVPlugin, AVSpeechSynthesizerDelegate {
    
    static var supportedLanguages: [String] = []
    var defaultLanugage = ""
    private var speechSynthesizer: AVSpeechSynthesizer?
    private var ttsCommand: CDVInvokedUrlCommand?
    
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private var audioEngine = AVAudioEngine()
    private var selectedLanguage: String = "en-US"
    private var sttCommand: CDVInvokedUrlCommand?
    
    // @objc(yourFunctionName:) // Declare your function name.
    // func yourFunctionName(command: CDVInvokedUrlCommand) { // write the function code.
    //   /*
    //    * Always assume that the plugin will fail.
    //    * Even if in this example, it can't.
    //    */
    //   // Set the plugin result to fail.
    //   var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
    //   // Set the plugin result to succeed.
    //       pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "The plugin succeeded");
    //   // Send the function result back to Cordova.
    //   self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
    // }
    
    @objc(echo:) // Declare your function name.
    func echo(command: CDVInvokedUrlCommand) { // write the function code.
        /*
         * Always assume that the plugin will fail.
         * Even if in this example, it can't.
         */
        // Set the plugin result to fail.
        var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "The Plugin Failed");
        // Set the plugin result to succeed.
        pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "The plugin succeeded");
        // Send the function result back to Cordova.
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
    }
    
    @objc(getSupportedLanguages:) // Declare your function name.
    func getSupportedLanguages(command: CDVInvokedUrlCommand) {
        // fetch languages
        if(Speech.supportedLanguages.count == 0) {
            self.loadLanugageDetails()
        }
        
        // prepare result
        var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "Failed to get supported languages.");
        if (Speech.supportedLanguages.count > 0) {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: Speech.supportedLanguages);
        }
        
        // publish result
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
    }
    
    @objc(getDefaultLanguage:) // Declare your function name.
    func getDefaultLanguage(command: CDVInvokedUrlCommand) {
        // fetch languages
        if(self.defaultLanugage.count == 0) {
            self.loadLanugageDetails()
        }
        
        // prepare result
        var pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: "Failed to get default language.");
        if (self.defaultLanugage.count > 0) {
            pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: self.defaultLanugage);
        }
        
        // publish result
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
    }
    
    @objc(speakOut:) // Declare your function name.
    func speakOut(command: CDVInvokedUrlCommand) {
        let message = command.arguments[0] as? String ?? ""
        var pitchRate = command.arguments[1] as? Float ?? 1.0
        if (pitchRate == 0.0) {
            pitchRate = 1.0
        }
        var speechRate = command.arguments[2] as? Float ?? 0.5
        if (speechRate == 0.0) {
            speechRate = 0.5
        }
        
        ttsCommand = command;
        // fetch languages
        let speechUtterance = AVSpeechUtterance(string: message)
        speechUtterance.pitchMultiplier = pitchRate
        speechUtterance.rate = speechRate
        
        if(speechSynthesizer == nil) {
            // initialize speechSynthesizer
            self.speechSynthesizer = AVSpeechSynthesizer()
            self.speechSynthesizer?.delegate = self
        }
        
        if(self.speechSynthesizer!.isSpeaking){
            // stop speechSynthesizer
            self.speechSynthesizer!.stopSpeaking(at: .immediate)
        }
        
        // start speechSynthesizer
        self.speechSynthesizer!.speak(speechUtterance)
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "tts-start");
        pluginResult?.setKeepCallbackAs(true)
        self.commandDelegate!.send(pluginResult, callbackId: ttsCommand?.callbackId);
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "tts-end");
        self.commandDelegate!.send(pluginResult, callbackId: ttsCommand?.callbackId);
    }
    
    private func loadLanugageDetails() {
        // fetch default language
        let speechRecognizer = SFSpeechRecognizer()
        self.defaultLanugage = speechRecognizer!.locale.identifier
        
        // fetch supported languages
        SFSpeechRecognizer.supportedLocales().forEach { (locale) in
            Speech.supportedLanguages.append(locale.identifier)
        }
    }
    
    @objc(initRecognition:) // Declare your function name.
    func initRecognition(command: CDVInvokedUrlCommand) {
        self.sttCommand = command
        self.selectedLanguage = command.arguments[0] as? String ?? ""
        if self.selectedLanguage.isEmpty {
            self.selectedLanguage = self.defaultLanugage
        }
        
        self.setupSpeechRecognition(locale: selectedLanguage)
    }
    
    @objc(startRecognition:) // Declare your function name.
    func startRecognition(command: CDVInvokedUrlCommand) {
        if !self.speechRecognizer!.isAvailable {
            self.publishSTTError(error: "Speech recognition service not available")
            return
        }
        
        if audioEngine.isRunning {
            audioEngine.stop()
            recognitionRequest?.endAudio()
            self.publishSTTError(error: "Speech recognition was running, but now it stopped")
            return
        }
        
        self.sttCommand = command
        
        let partialResultRequired = command.arguments[0] as? Bool ?? false
        let offlineRecognitionRequired = command.arguments[1] as? Bool ?? false
        
        self.commandDelegate.run(inBackground: {
            do {
                try self.startRecording(partialResultRequired: partialResultRequired, offlineRecognitionRequired: offlineRecognitionRequired)
            }catch{
                self.publishSTTError(error: "Speech recognition error")
            }
        })
        
    }
    
    @objc(stopRecognition:) // Declare your function name.
    func stopRecognition(command: CDVInvokedUrlCommand) {
        if audioEngine.isRunning {
            audioEngine.stop()
            recognitionRequest?.endAudio()
        }
        let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: true);
        self.commandDelegate!.send(pluginResult, callbackId: command.callbackId);
    }
    
    func setupSpeechRecognition(locale:String) {
        
        speechRecognizer = SFSpeechRecognizer(locale: Locale.init(identifier: locale))
        
        SFSpeechRecognizer.requestAuthorization { (authStatus) in
            switch authStatus {
            case .authorized:
                var result = ["offlineRecognitionAvailable": false] as [AnyHashable : Any]
                if #available(iOS 13, *) {
                    result = ["offlineRecognitionAvailable": self.speechRecognizer!.supportsOnDeviceRecognition] as [AnyHashable : Any]
                }
                self.publishSTTResult(result: result, keepCallback: false)
                break
                
            case .denied:
                self.publishSTTError(error: "User denied access to speech recognition")
                break
                
            case .restricted:
                self.publishSTTError(error: "Speech recognition restricted on this device")
                break
                
            case .notDetermined:
                self.publishSTTError(error: "Speech recognition not yet authorized")
                break
            @unknown default:
                self.publishSTTError(error: "Speech recognition not yet authorized")
            }
        }
    }
    
    func publishSTTError(error: String) {
        let pluginResult = CDVPluginResult (status: CDVCommandStatus_ERROR, messageAs: error);
        self.commandDelegate!.send(pluginResult, callbackId: self.sttCommand?.callbackId);
    }
    
    func publishSTTResult(result: [AnyHashable : Any], keepCallback: Bool) {
        let pluginResult = CDVPluginResult (status: CDVCommandStatus_OK, messageAs: result);
        pluginResult?.setKeepCallbackAs(keepCallback)
        self.commandDelegate!.send(pluginResult, callbackId: self.sttCommand?.callbackId);
    }
    
    func startRecording(partialResultRequired: Bool, offlineRecognitionRequired: Bool) throws {
        
        recognitionTask?.cancel()
        self.recognitionTask = nil
        
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(AVAudioSessionCategoryPlayAndRecord, mode: AVAudioSessionModeMeasurement, options: .duckOthers)
        try audioSession.setActive(true, with: .notifyOthersOnDeactivation)
        
        audioEngine = AVAudioEngine()
        let inputNode = audioEngine.inputNode
        inputNode.removeTap(onBus: 0)
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { (buffer: AVAudioPCMBuffer, when: AVAudioTime) in
            self.recognitionRequest?.append(buffer)
        }
        
        audioEngine.prepare()
        print("step tracking ==>", audioEngine.isRunning)
        try audioEngine.start()
        
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else {
            self.publishSTTError(error: "Unable to create a SFSpeechAudioBufferRecognitionRequest object")
            return
        }
        
        // allow partial results
        recognitionRequest.shouldReportPartialResults = partialResultRequired
        
        // allow offline recognition
        if offlineRecognitionRequired, #available(iOS 13, *) {
            if speechRecognizer?.supportsOnDeviceRecognition ?? false{
                recognitionRequest.requiresOnDeviceRecognition = true
            }
        }
        
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                DispatchQueue.main.async {
                    let transcribedString = result.bestTranscription.formattedString
                    let finalResult = ["isFinal":result.isFinal, "text":(transcribedString)] as [AnyHashable:Any]
                    if result.isFinal{
                        self.publishSTTResult(result: finalResult, keepCallback: false)
                    } else{
                        self.publishSTTResult(result: finalResult, keepCallback: true)
                    }
                }
            }
            if error != nil {
                self.audioEngine.stop()
                inputNode.removeTap(onBus: 0)
                self.recognitionRequest = nil
                self.recognitionTask = nil
                self.publishSTTError(error: "Failed to recognize")
            }
        }
    }
}
