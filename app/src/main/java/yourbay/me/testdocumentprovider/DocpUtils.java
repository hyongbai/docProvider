package yourbay.me.testdocumentprovider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

/**
 * Created by ram on 15/1/30.
 */
public class DocpUtils {
    public static final int EDIT_REQUEST_CODE = 44;
    public static final int CREATE_REQUEST_CODE = 43;

    public final static void createFile(Activity activity, String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        activity.startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    /**
     * Open a file for writing and append some text to it.
     */
    public final static void editDocument(Activity activity) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's
        // file browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only text files.
        intent.setType("text/plain");

        activity.startActivityForResult(intent, EDIT_REQUEST_CODE);
    }


    @TargetApi(19)
    public final static void delete(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT < 19) {
            return;
        }
        DocumentsContract.deleteDocument(context.getContentResolver(), uri);
    }
}
