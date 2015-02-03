package yourbay.me.testdocumentprovider;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Referenced from {@link "https://developer.android.com/intl/zh-cn/guide/topics/providers/document-provider.html"}
 * Created by ram on 15/2/2.
 */
public class DocProvider extends DocumentsProvider {
    private Map<String, String> FILE_MAP = new HashMap<>();

    private static final String TAG = "DocProvider";
    private static final String[] DEFAULT_ROOT_PROJECTION =
            new String[]{Root.COLUMN_ROOT_ID,
                    Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
                    Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,
                    Root.COLUMN_AVAILABLE_BYTES,};
    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new
            String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_DISPLAY_NAME, Document.COLUMN_LAST_MODIFIED,
            Document.COLUMN_FLAGS, Document.COLUMN_SIZE,};
    private String ROOT_ID = "THIS_IS_A_ROOT_ID";

    private File mBaseDir;

    @Override
    public boolean onCreate() {
        log("onCreate");
        mBaseDir = getContext().getFilesDir();
        clearFileMap();
        return true;
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        log("queryRoots  " + Arrays.toString(projection));
        final MatrixCursor result = new MatrixCursor(DEFAULT_ROOT_PROJECTION);
        addRoot(result, getContext().getString(R.string.app_name), "just a test");
        return result;
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        if (ROOT_ID.equals(parentDocumentId)) {
            clearFileMap();
        }
        final MatrixCursor result = new MatrixCursor(DEFAULT_DOCUMENT_PROJECTION);
        final String path = getFilePath(parentDocumentId);
        final File parent = new File(path);
        log("queryChildDocuments  " + parentDocumentId + "   " + path);
        for (File f : parent.listFiles()) {
            addFile(result, f);
        }
        return result;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        log("queryDocument   documentId=" + documentId + "  projection=" + Arrays.toString(projection));
        final MatrixCursor result = new MatrixCursor(DEFAULT_DOCUMENT_PROJECTION);
        addFile(result, new File(getFilePath(documentId)), documentId);
        return result;
    }


    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, CancellationSignal signal)
            throws FileNotFoundException {
        log("openDocument, mode: " + mode);
        String path = getFilePath(documentId);
        log("openDocument, path= " + path);
        final File file = new File(path);
        log("openDocument, file= " + file);
        final boolean isWrite = (mode.indexOf('w') != -1);
        final int accessMode = ParcelFileDescriptor.MODE_READ_WRITE;
        if (isWrite) {
            try {
                Handler handler = new Handler(getContext().getMainLooper());
                return ParcelFileDescriptor.open(file, accessMode, handler,
                        new ParcelFileDescriptor.OnCloseListener() {
                            @Override
                            public void onClose(IOException e) {
                                Log.i(TAG, "A file with id "
                                        + System.currentTimeMillis()
                                        + " has been closed! Time to update the server.");
                            }

                        });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document with id "
                        + documentId + " and mode " + mode);
            }
        } else {
            return ParcelFileDescriptor.open(file, accessMode);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, CancellationSignal cancellationSignal) {
        Cursor cursor = null;
        final String docId = parseDocId(uri.getPath());
        final String path = getFilePath(docId);
        if (!TextUtils.isEmpty(path) && new File(path).exists()) {
            File f = new File(path);
            final MatrixCursor result = new MatrixCursor(DEFAULT_DOCUMENT_PROJECTION);
            addFile(result, f, docId);
            cursor = result;
        }
        if (cursor == null) {
            cursor = super.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
        }
        log("query    cursor:" + cursor + " docId=" + docId + " filePath=" + path);
        return cursor;
    }


    private String getFilePath(String documentId) {
        return FILE_MAP.get(documentId);
    }

    private void clearFileMap() {
        FILE_MAP.clear();
        FILE_MAP.put(ROOT_ID, Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    private void addFile(MatrixCursor result, File f) {
        addFile(result, f, getDocumentIdFromFile(f));
    }

    private void addFile(MatrixCursor result, File f, String docId) {
        if (f == null || f.isHidden() || TextUtils.isEmpty(docId)) {
            return;
        }
        FILE_MAP.put(docId, f.getAbsolutePath());
        String mime = getDocumentMime(f);
        result.newRow()//
                .add(Document.COLUMN_DOCUMENT_ID, docId)//
                .add(Document.COLUMN_MIME_TYPE, mime)//
                .add(Document.COLUMN_DISPLAY_NAME, f.getName())//
                .add(Document.COLUMN_SIZE, f.length())//
                .add(Document.COLUMN_LAST_MODIFIED, f.lastModified())//
                .add(Document.COLUMN_FLAGS, getDocumentFlag(f, mime))//
        ;
    }


    private void addRoot(MatrixCursor result, String name, String summary) {
        log("addRoot id=" + name);
        result.newRow()
                .add(Root.COLUMN_ROOT_ID, ROOT_ID)//
                .add(Root.COLUMN_ICON, R.drawable.ic_launcher)//
                .add(Root.COLUMN_TITLE, name)//
                .add(Root.COLUMN_SUMMARY, summary)//
                .add(Root.COLUMN_DOCUMENT_ID, ROOT_ID)//
                .add(Root.COLUMN_AVAILABLE_BYTES, mBaseDir.getFreeSpace())
                .add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE //
                        | Root.FLAG_SUPPORTS_RECENTS //
                        | Root.FLAG_SUPPORTS_SEARCH //
                )
        ;
    }


    private void log(String log) {
        Log.d(TAG, Thread.currentThread().getId() + " " + log);
    }


    //UTILS
    public static final String getDocumentMime(File f) {
        if (f.isDirectory() || !f.isFile()) {
            return Document.MIME_TYPE_DIR;
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(f.getAbsolutePath()));
    }

    public static final int getDocumentFlag(File f, String mime) {
        //flag
        int flag = 0;
        if (f.isDirectory()) {
            flag |= Document.FLAG_DIR_SUPPORTS_CREATE;
        }
        if (f.canWrite()) {
            flag |= Document.FLAG_SUPPORTS_RENAME;
            flag |= Document.FLAG_SUPPORTS_DELETE;
        }
        if (mime != null && mime.startsWith("image")) {
            //  flag |= Document.FLAG_SUPPORTS_THUMBNAIL;
        }
        return flag;
    }

    public static final String parseDocId(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        if (path.startsWith("/document/")) {
            return path.replace("/document/", "");
        }
        return null;
    }

    public static final String getDocumentIdFromFile(File f) {
        return String.valueOf(StringToMD5(f.getAbsolutePath()));
    }

    public static final String StringToMD5(String inStr) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();

        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }


}
