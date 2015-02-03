package yourbay.me.testdocumentprovider;

import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class DemoProvider extends DocumentsProvider {
    private static final String TAG = DemoProvider.class.getSimpleName();

    private static final String[] SUPPORTED_ROOT_PROJECTION = new String[]{
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_TITLE, Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_ICON
    };
    private static final String[] SUPPORTED_DOCUMENT_PROJECTION = new String[]{
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_FLAGS
    };
    private static final String ROOT_ID = "thisIsMyRoot";
    // private static final String ROOT_DOCUMENT_ID = "thisCannotBeEmpty";
    private static final String DELIMETER = "/";
    private AssetManager assets;

    @Override
    public boolean onCreate() {
        assets = getContext().getAssets();

        return (true);
    }

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        String[] netProjection = netProjection(projection, SUPPORTED_ROOT_PROJECTION);
        MatrixCursor result = new MatrixCursor(netProjection);
        MatrixCursor.RowBuilder row = result.newRow();

        row.add(Root.COLUMN_ROOT_ID, ROOT_ID);
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher);
        row.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY);
        row.add(Root.COLUMN_TITLE, getContext().getString(R.string.app_name));
        row.add(Root.COLUMN_SUMMARY, "This is a summary");
        row.add(Root.COLUMN_DOCUMENT_ID, fixUpDocumentId(ROOT_ID));

        return (result);
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder)
            throws FileNotFoundException {
        String[] netProjection = netProjection(projection, SUPPORTED_DOCUMENT_PROJECTION);
        MatrixCursor result = new MatrixCursor(netProjection);

        parentDocumentId = fixUpDocumentId(parentDocumentId);

        try {
            String[] children = assets.list(getAssetPathFromDocId(parentDocumentId));

            for (String child : children) {
                addDocumentRow(result, child, parentDocumentId + DELIMETER + child);
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception reading asset dir", e);
        }

        return (result);
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection)
            throws FileNotFoundException {
        String[] netProjection = netProjection(projection, SUPPORTED_DOCUMENT_PROJECTION);
        MatrixCursor result = new MatrixCursor(netProjection);

        documentId = fixUpDocumentId(documentId);

        try {
            addDocumentRow(result, Uri.parse(documentId).getLastPathSegment(), documentId);
        } catch (IOException e) {
            Log.e(TAG, "Exception reading asset dir", e);
        }

        return (result);
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
                                             CancellationSignal signal) throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;

        try {
            pipe = ParcelFileDescriptor.createPipe();
            AssetManager assets = getContext().getResources().getAssets();

            new TransferThread(assets.open(getAssetPathFromDocId(documentId)),
                    new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();
        } catch (IOException e) {
            Log.e(TAG, "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: " + documentId);
        }

        return (pipe[0]);
    }

    private void addDocumentRow(MatrixCursor result, String child, String documentId)
            throws IOException {
        MatrixCursor.RowBuilder row = result.newRow();

        row.add(Document.COLUMN_DOCUMENT_ID, documentId);

        if (isDirectory(documentId)) {
            row.add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR);
        } else {
            String mimeType = MimeTypeMap.getFileExtensionFromUrl(documentId);
            row.add(Document.COLUMN_MIME_TYPE, mimeType);
            row.add(Document.COLUMN_SIZE, lastModified(documentId));
        }

        row.add(Document.COLUMN_DISPLAY_NAME, child);
        row.add(Document.COLUMN_FLAGS, 0);
    }

    private boolean isDirectory(String documentId) throws IOException {
        return assets.list(getAssetPathFromDocId(documentId)).length > 0;
    }

    private long lastModified(String documentId) throws IOException {
        return (assets.openFd(getAssetPathFromDocId(documentId)).getLength());
    }

    private String fixUpDocumentId(String documentId) {
        if (ROOT_ID.equals(documentId)) {
            return documentId;
        }
        return documentId;
    }

    private String getAssetPathFromDocId(String documentId) throws FileNotFoundException {
        if (ROOT_ID.equals(documentId)) {
            return "";
        }

        final int splitIndex = documentId.indexOf(DELIMETER, 1);
        if (splitIndex < 0) {
            throw new FileNotFoundException("Missing root for " + documentId);
        } else {
            return documentId.substring(splitIndex + 1);
        }
    }

    private static String[] netProjection(String[] requested, String[] supported) {
        if (requested == null) {
            return (supported);
        }

        ArrayList<String> result = new ArrayList<String>();

        for (String request : requested) {
            for (String support : supported) {
                if (request.equals(support)) {
                    result.add(request);
                    break;
                }
            }
        }

        return (result.toArray(new String[0]));
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;

        TransferThread(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            int len;

            try {
                while ((len = in.read(buf)) >= 0) {
                    out.write(buf, 0, len);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception transferring file", e);
            }
        }
    }
}