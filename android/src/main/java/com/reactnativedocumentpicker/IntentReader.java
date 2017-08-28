/* Extracted from ShareCompat.java */

package com.reactnativedocumentpicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.IntentCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import java.util.ArrayList;

/**
 * IntentReader is a helper for reading the data contained within a sharing (ACTION_SEND)
 * Intent. It provides methods to parse standard elements included with a share
 * in addition to extra metadata about the app that shared the content.
 *
 * <p>Social sharing apps are encouraged to provide attribution for the app that shared
 * the content. IntentReader offers access to the application label, calling activity info,
 * and application icon of the app that shared the content. This data may have been provided
 * voluntarily by the calling app and should always be displayed to the user before submission
 * for manual verification. The user should be offered the option to omit this information
 * from shared posts if desired.</p>
 *
 * <p>Activities that intend to receive sharing intents should configure an intent-filter
 * to accept {@link Intent#ACTION_SEND} intents ("android.intent.action.SEND") and optionally
 * accept {@link Intent#ACTION_SEND_MULTIPLE} ("android.intent.action.SEND_MULTIPLE") if
 * the activity is equipped to handle multiple data streams.</p>
 */
public class IntentReader {
    private static final String TAG = "IntentReader";

    private Intent mIntent;
    private String mCallingPackage;
    private ComponentName mCallingActivity;

    private ArrayList<Uri> mStreams;

    /**
     * Get an IntentReader for parsing and interpreting the sharing intent
     * used to start the given activity.
     *
     * @param intent Intent
     * @return IntentReader for parsing sharing data
     */
    public static IntentReader from(Intent intent) {
        return new IntentReader(intent);
    }

    private IntentReader(Intent intent) {
        mIntent = intent;
    }

    /**
     * Returns true if the activity this reader was obtained for was
     * started with an {@link Intent#ACTION_SEND} or {@link Intent#ACTION_SEND_MULTIPLE}
     * sharing Intent.
     *
     * @return true if the activity was started with an ACTION_SEND
     *         or ACTION_SEND_MULTIPLE Intent
     */
    public boolean isShareIntent() {
        final String action = mIntent.getAction();
        return Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action);
    }

    /**
     * Returns true if the activity this reader was obtained for was started with an
     * {@link Intent#ACTION_SEND} intent and contains a single shared item.
     * The shared content should be obtained using either the {@link #getText()}
     * or {@link #getStream()} methods depending on the type of content shared.
     *
     * @return true if the activity was started with an ACTION_SEND intent
     */
    public boolean isSingleShare() {
        return Intent.ACTION_SEND.equals(mIntent.getAction());
    }

    /**
     * Returns true if the activity this reader was obtained for was started with an
     * {@link Intent#ACTION_SEND_MULTIPLE} intent. The Intent may contain more than
     * one stream item.
     *
     * @return true if the activity was started with an ACTION_SEND_MULTIPLE intent
     */
    public boolean isMultipleShare() {
        return Intent.ACTION_SEND_MULTIPLE.equals(mIntent.getAction());
    }

    /**
     * Get the mimetype of the data shared to this activity.
     *
     * @return mimetype of the shared data
     * @see Intent#getType()
     */
    public String getType() {
        return mIntent.getType();
    }

    /**
     * Get the literal text shared with the target activity.
     *
     * @return Literal shared text or null if none was supplied
     * @see Intent#EXTRA_TEXT
     */
    public CharSequence getText() {
        return mIntent.getCharSequenceExtra(Intent.EXTRA_TEXT);
    }

    /**
     * Get the styled HTML text shared with the target activity.
     * If no HTML text was supplied but {@link Intent#EXTRA_TEXT} contained
     * styled text, it will be converted to HTML if possible and returned.
     * If the text provided by {@link Intent#EXTRA_TEXT} was not styled text,
     * it will be escaped by {@link android.text.Html#escapeHtml(CharSequence)}
     * and returned. If no text was provided at all, this method will return null.
     *
     * @return Styled text provided by the sender as HTML.
     */
    public String getHtmlText() {
        String result = mIntent.getStringExtra(IntentCompat.EXTRA_HTML_TEXT);
        if (result == null) {
            CharSequence text = getText();
            if (text instanceof Spanned) {
                result = Html.toHtml((Spanned) text);
            } else if (text != null) {
                result = Html.escapeHtml(text);
            }
        }
        return result;
    }

    /**
     * Get a URI referring to a data stream shared with the target activity.
     *
     * <p>This call will fail if the share intent contains multiple stream items.
     * If {@link #isMultipleShare()} returns true the application should use
     * {@link #getStream(int)} and {@link #getStreamCount()} to retrieve the
     * included stream items.</p>
     *
     * @return A URI referring to a data stream to be shared or null if one was not supplied
     * @see Intent#EXTRA_STREAM
     */
    public Uri getStream() {
        return mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
    }

    /**
     * Get the URI of a stream item shared with the target activity.
     * Index should be in the range [0-getStreamCount()).
     *
     * @param index Index of text item to retrieve
     * @return Requested stream item URI
     * @see Intent#EXTRA_STREAM
     * @see Intent#ACTION_SEND_MULTIPLE
     */
    public Uri getStream(int index) {
        if (mStreams == null && isMultipleShare()) {
            mStreams = mIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
        if (mStreams != null) {
            return mStreams.get(index);
        }
        if (index == 0) {
            return mIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        }
        throw new IndexOutOfBoundsException("Stream items available: " + getStreamCount() +
                " index requested: " + index);
    }

    /**
     * Return the number of stream items shared. The return value will be 0 or 1 if
     * this was an {@link Intent#ACTION_SEND} intent, or 0 or more if it was an
     * {@link Intent#ACTION_SEND_MULTIPLE} intent.
     *
     * @return Count of text items contained within the Intent
     */
    public int getStreamCount() {
        if (mStreams == null && isMultipleShare()) {
            mStreams = mIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
        if (mStreams != null) {
            return mStreams.size();
        }
        return mIntent.hasExtra(Intent.EXTRA_STREAM) ? 1 : 0;
    }

    /**
     * Get an array of Strings, each an email address to share to.
     *
     * @return An array of email addresses or null if none were supplied.
     * @see Intent#EXTRA_EMAIL
     */
    public String[] getEmailTo() {
        return mIntent.getStringArrayExtra(Intent.EXTRA_EMAIL);
    }

    /**
     * Get an array of Strings, each an email address to CC on this share.
     *
     * @return An array of email addresses or null if none were supplied.
     * @see Intent#EXTRA_CC
     */
    public String[] getEmailCc() {
        return mIntent.getStringArrayExtra(Intent.EXTRA_CC);
    }

    /**
     * Get an array of Strings, each an email address to BCC on this share.
     *
     * @return An array of email addresses or null if none were supplied.
     * @see Intent#EXTRA_BCC
     */
    public String[] getEmailBcc() {
        return mIntent.getStringArrayExtra(Intent.EXTRA_BCC);
    }

    /**
     * Get a subject heading for this share; useful when sharing via email.
     *
     * @return The subject heading for this share or null if one was not supplied.
     * @see Intent#EXTRA_SUBJECT
     */
    public String getSubject() {
        return mIntent.getStringExtra(Intent.EXTRA_SUBJECT);
    }

}
