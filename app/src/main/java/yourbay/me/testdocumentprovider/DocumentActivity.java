package yourbay.me.testdocumentprovider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class DocumentActivity extends Activity implements View.OnClickListener {

    public final static String TAG = "DocumentActivity";

    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
    }

    private void initUI() {
        setContentView(R.layout.activity_main);
        tvResult = (TextView) findViewById(R.id.tv_result);
        findViewById(R.id.btn_images).setOnClickListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_create) {
            DocpUtils.createFile(this, "text/plain",
                    new StringBuilder("test-DocProvider-")//
                            .append(System.currentTimeMillis())//
                            .append(".txt")//
                            .toString());
        } else if (id == R.id.action_delete) {

        } else if (id == R.id.action_edit) {
            DocpUtils.editDocument(this);
        } else if (id == R.id.action_clear) {
            tvResult.setText("");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_images) {
            IntentUtils.pickupImages(this, 123);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        Uri uri = data.getData();
        appendResult("onActivityResult uri=" + String.valueOf(uri));
        if (requestCode == DocpUtils.CREATE_REQUEST_CODE) {
            onCreated(uri);
        } else if (requestCode == DocpUtils.EDIT_REQUEST_CODE) {
            onEdited(uri);
        }
    }


    private void onCreated(Uri uri) {

    }

    private void onEdited(Uri uri) {

    }

    private void appendResult(String msg) {
        tvResult.append("\n");
        tvResult.append(msg);
    }

}
