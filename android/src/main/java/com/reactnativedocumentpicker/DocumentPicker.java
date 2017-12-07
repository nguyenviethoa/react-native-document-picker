package com.reactnativedocumentpicker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://developer.android.com/guide/topics/providers/document-provider.html">android documentation</a>
 */
public class DocumentPicker extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final String NAME = "RNDocumentPicker";
    private static final int READ_REQUEST_CODE = 41;

    private final ReactApplicationContext mReactContext;

    private static class Fields {
        private static final String FILE_SIZE = "fileSize";
        private static final String FILE_NAME = "fileName";
        private static final String TYPE = "type";
    }

    private Callback callback;
    private boolean isMultiple;

    public DocumentPicker(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void show(ReadableMap args, Callback callback) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            if (args.hasKey("multiple") && args.getBoolean("multiple")) {
                this.isMultiple = true;
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, this.isMultiple);
            }
        }

        if (!args.isNull("filetype")) {
            ReadableArray filetypes = args.getArray("filetype");
            if (filetypes.size() > 1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                intent.setType("*/*");
                final String[] types = new String[filetypes.size()];
                for (int i = 0; i < filetypes.size(); i++) {
                    types[i] = filetypes.getString(i);
                }
                intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
            } else if (filetypes.size() > 0) {
                intent.setType(filetypes.getString(0));
            }
        }

        this.callback = callback;

        getReactApplicationContext().startActivityForResult(intent, READ_REQUEST_CODE, Bundle.EMPTY);
    }

    // removed @Override temporarily just to get it working on RN0.33 and RN0.32 - will remove
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode != READ_REQUEST_CODE)
            return;

        if (resultCode != Activity.RESULT_OK) {
            callback.invoke("Bad result code: " + resultCode, null);
            return;
        }

        if (data == null) {
            callback.invoke("No data", null);
            return;
        }

        try {
            if (this.isMultiple) {
                List<Uri> uris = new ArrayList<>();
                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        if (item.getUri() != null) {
                            Uri uri = item.getUri();
                            uris.add(uri);
                        }
                    }
                } else {
                    uris.add(data.getData());
                }
                callback.invoke(null, toListWithMetadata(activity, uris));
            } else {
                callback.invoke(null, toMapWithMetadata(activity, data.getData()));
            }
        } catch (Exception e) {
            Log.e(NAME, "Failed to read", e);
            callback.invoke(e.getMessage(), null);
        }
    }

    private WritableArray toListWithMetadata(Activity activity, List<Uri> uris) {
        WritableArray list = Arguments.createArray();

        for (Uri uri : uris) {
            list.pushMap(toMapWithMetadata(activity, uri));
        }

        return list;
    }

    private WritableMap toMapWithMetadata(Activity activity, Uri uri) {
        WritableMap map;
        if (uri.toString().startsWith("/")) {
            map = metaDataFromFile(new File(uri.toString()));
        } else if (uri.toString().startsWith("http")) {
            map = metaDataFromUri(uri);
        } else {
            map = metaDataFromContentResolver(uri);
            activity.grantUriPermission(activity.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        map.putString("uri", uri.toString());

        return map;
    }

    private WritableMap metaDataFromUri(Uri uri) {
        WritableMap map = Arguments.createMap();
        map.putString(Fields.TYPE, mimeTypeFromName(uri.toString()));

        return map;
    }

    private WritableMap metaDataFromFile(File file) {
        WritableMap map = Arguments.createMap();

        if (!file.exists())
            return map;

        map.putInt(Fields.FILE_SIZE, (int) file.length());
        map.putString(Fields.FILE_NAME, file.getName());
        map.putString(Fields.TYPE, mimeTypeFromName(file.getAbsolutePath()));

        return map;
    }

    private WritableMap metaDataFromContentResolver(Uri uri) {
        WritableMap map = Arguments.createMap();

        ContentResolver contentResolver = getReactApplicationContext().getContentResolver();

        map.putString(Fields.TYPE, contentResolver.getType(uri));

        Cursor cursor = contentResolver.query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {

                map.putString(Fields.FILE_NAME, cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    String size = cursor.getString(sizeIndex);
                    if (size != null)
                        map.putInt(Fields.FILE_SIZE, Integer.valueOf(size));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return map;
    }

    private static File download(Uri uri, File outputDir) throws IOException {
        File file = File.createTempFile("prefix", "extension", outputDir);

        URL url = new URL(uri.toString());

        ReadableByteChannel channel = Channels.newChannel(url.openStream());
        try {
            FileOutputStream stream = new FileOutputStream(file);

            try {
                stream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);
                return file;
            } finally {
                stream.close();
            }
        } finally {
            channel.close();
        }
    }

    private static String mimeTypeFromName(String absolutePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(absolutePath);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        } else {
            return null;
        }
    }

    private void sendEvent(String eventName, WritableMap params) {
        mReactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @Override
    public void onNewIntent(Intent intent) {
        WritableMap params = getUrisFromIntent(intent);
        if (params == null) return;

        sendEvent("onIncomingShare", params);
    }

    private WritableMap getUrisFromIntent(Intent intent) {
        IntentReader intentReader = IntentReader.from(intent);

        if (!intentReader.isShareIntent()) {
            return null;
        }

        WritableMap params = Arguments.createMap();

        if (intentReader.getText() != null) {
            params.putString("text", intentReader.getText().toString());
        }
        params.putString("mimeType", intentReader.getType());
        params.putString("html", intentReader.getHtmlText());

        List<Uri> uris = new ArrayList<>();
        for (int i = 0; i < intentReader.getStreamCount(); i++) {
            uris.add(intentReader.getStream(i));
        }
        params.putArray("attachments", toListWithMetadata(mReactContext.getCurrentActivity(), uris));
        return params;
    }

    /**
     * Return the URL the activity was started with
     *
     * @param promise a promise which is resolved with the initial URL
     */
    @ReactMethod
    public void getIncomingAttachments(Promise promise) {
        try {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity != null) {
                WritableMap params = getUrisFromIntent(currentActivity.getIntent());
                if (params != null) {
                    promise.resolve(params);
                    return;
                }
            }
            promise.reject("ERROR_NO_ATTACHMENTS", "Not a incoming share intent");
        } catch (Exception e) {
            promise.reject(new JSApplicationIllegalArgumentException(
                    "Could not get the initial URL : " + e.getMessage()));
        }
    }
}
