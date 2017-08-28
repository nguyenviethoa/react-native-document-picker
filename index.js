'use strict';

import {Platform, NativeModules, NativeEventEmitter} from "react-native";
const DocumentPicker = NativeModules.RNDocumentPicker;

/**
 * Android requires mime types, iOS is a bit more complicated:
 *
 * @see https://developer.apple.com/library/ios/documentation/Miscellaneous/Reference/UTIRef/Articles/System-DeclaredUniformTypeIdentifiers.html
 */
class DocumentPickerUtil {
  static allFiles() {
    return (Platform.OS === 'android') ? "*/*" : "public.content";
  }

  static movie() {
    return Platform.select({"ios": "public.movie", "android": "video/mp4"});
  }

  static images() {
    return (Platform.OS === 'android') ? "image/*" : "public.image";
  }

  static plainText() {
    return (Platform.OS === 'android') ? "text/plain" : "public.plain-text";
  }

  static audio() {
    return (Platform.OS === 'android') ? "audio/*" : "public.audio";
  }

  static pdf() {
    return (Platform.OS === 'android') ? "application/pdf" : "com.adobe.pdf";
  }
}

const incomingShareEmitter = new NativeEventEmitter(DocumentPicker);

class IncomingShare {
    static EVENT = "onIncomingShare";
    static addEventListener(event, callback) {
        return incomingShareEmitter.addListener(event, callback);
    }
    static removeEventListener(event, callback) {
      incomingShareEmitter.removeListener(event, callback);
    }
    static getIncomingAttachments = DocumentPicker.getIncomingAttachments;
}

module.exports = {DocumentPickerUtil, DocumentPicker, IncomingShare};


