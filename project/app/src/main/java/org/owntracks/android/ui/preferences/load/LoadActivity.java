package org.owntracks.android.ui.preferences.load;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonParseException;

import org.owntracks.android.R;
import org.owntracks.android.databinding.UiPreferencesLoadBinding;
import org.owntracks.android.ui.base.BaseActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import timber.log.Timber;

public class LoadActivity extends BaseActivity<UiPreferencesLoadBinding, LoadMvvm.ViewModel> implements LoadMvvm.View {
    public static final int REQUEST_CODE = 1;
    public static final String FLAG_IN_APP = "INAPP";
    private MenuItem saveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityComponent().inject(this);
        bindAndAttachContentView(R.layout.ui_preferences_load, savedInstanceState);

        setHasEventBus(false);
        setSupportToolbar(binding.toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setHasBack(false);
        handleIntent(intent);
    }



    private void tintMenu() {
        if(saveButton != null) {
            saveButton.setEnabled(viewModel.getConfigurationPretty() != null);
            saveButton.setVisible(viewModel.getConfigurationPretty() != null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                viewModel.saveConfiguration();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    private void setHasBack(boolean hasBackArrow) {
        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackArrow);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_load, menu);

        saveButton = menu.findItem(R.id.save);
        tintMenu();
        return true;
    }

    private void handleIntent(@Nullable Intent intent) {
        if(intent == null) {
            Timber.e("no intent provided");
            return;
        }


        setHasBack(intent.getBooleanExtra(FLAG_IN_APP, false));
        Timber.v("inApp %s", intent.getBooleanExtra(FLAG_IN_APP, false));


        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            Timber.v("uri: %s", uri);
            if (uri != null) {
                extractPreferences(uri);
            }
        } else {
            Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
            pickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
            pickerIntent.setType("*/*");

            try {
                Timber.v("loading picker");
                startActivityForResult(Intent.createChooser(pickerIntent, "Select a file"), LoadActivity.REQUEST_CODE);
            } catch (android.content.ActivityNotFoundException ex) {
                // Potentially direct the user to the Market with a Dialog
            }

        }
    }

    // Return path from file picker
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent) {
        super.onActivityResult(requestCode, resultCode, resultIntent);
        Timber.v("RequestCode: " + requestCode + " resultCode: " + resultCode);
        switch(requestCode) {
            case LoadActivity.REQUEST_CODE: {
                if(resultCode == RESULT_OK) {
                    extractPreferences(resultIntent.getData());
                } else {
                    finish();
                }


                break;
            }
        }
    }

    private void extractPreferences(Uri uri){

        try {
            BufferedReader r;
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                // Note: left here to avoid breaking compatibility.  May be removed
                // with sufficient testing. Will not work on Android >5 without granting READ_EXTERNAL_STORAGE permission
                Timber.v("using file:/ uri");
                r = new BufferedReader(new InputStreamReader(new FileInputStream(uri.getPath())));
            } else {
                Timber.v("using content:/ uri");
                InputStream stream = getContentResolver().openInputStream(uri);

                r = new BufferedReader(new InputStreamReader(stream));
            }

            StringBuilder total = new StringBuilder();

            String content;
            while ((content = r.readLine()) != null) {
                total.append(content);
            }

            viewModel.setConfiguration(total.toString());
            tintMenu();
        } catch (JsonParseException e) {
            Timber.e(e, "parse exception ");
            Toast.makeText(this, getString(R.string.errorPreferencesImportFailedParseException), Toast.LENGTH_SHORT).show();
            finish();
        } catch(OutOfMemoryError e){
            Timber.e(e, "load exception oom");
            finish();
            Toast.makeText(this, getString(R.string.errorPreferencesImportFailedMemory), Toast.LENGTH_SHORT).show();
        } catch(Exception e) {
            Timber.e(e, "load exception");
            finish();
            Toast.makeText(this, getString(R.string.errorPreferencesImportFailed), Toast.LENGTH_SHORT).show();
        } 

    }

}