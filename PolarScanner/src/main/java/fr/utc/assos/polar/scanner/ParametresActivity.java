package fr.utc.assos.polar.scanner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;

/**
 * Created by florent on 11/10/13.
 */
public class ParametresActivity extends Activity {

    public static final String DEFAULT_URL = "http://192.168.0.1";
    public static final int DEFAULT_PORT = 8000;

    private View mParametresFormView;
    private SharedPreferences preferences;
    private TextView serverUrlView;
    private TextView serverPortView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_parametres);

        mParametresFormView = findViewById(R.id.parametres_view);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        serverUrlView = (TextView) findViewById(R.id.server_url);
        serverUrlView.setText(preferences.getString("server_url", DEFAULT_URL));

        serverPortView = (TextView) findViewById(R.id.server_port);
        serverPortView.setText(String.valueOf(preferences.getInt("server_port", DEFAULT_PORT)));
    }

    private void saveParametres() {
        SharedPreferences.Editor editor = preferences.edit();

        int serverPort =  Integer.parseInt(serverPortView.getText().toString());
        editor.putInt("server_port", serverPort);

        String serverUrl = serverUrlView.getText().toString();
        editor.putString("server_url", serverUrl);
        editor.commit();

        CharSequence text = getText(R.string.parametres_enregistres_msg);
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        saveParametres();
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                // On sauvegarde les données
                saveParametres();
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}