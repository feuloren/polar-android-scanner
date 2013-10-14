package fr.utc.assos.polar.scanner;

/**
 * Created by florent on 11/10/13.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class ScanActivity extends Activity {

    public static final String LOGIN = "fr.utc.assos.polar.scanner.login";

    private SharedPreferences preferences;

    private View succesView;
    private View warningView;
    private View waitView;
    private View invalideView;
    private TextView queryView;
    private String login;

    private DistribHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);
        // variable utiles pour plus tard
        succesView = findViewById(R.id.success);
        warningView = findViewById(R.id.warning);
        waitView = findViewById(R.id.wait);
        invalideView = findViewById(R.id.invalide);
        queryView = (TextView) findViewById(R.id.query);

        // on réagit à l'appui sur la touche entrée pour lancer la recherche
        queryView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE)// touche entrée
                    searchCommande();
                return false;
            }
        });

        // on cache le clavier par défaut
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askScan();
            }
        });
        findViewById(R.id.go_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchCommande();
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Intent intent = getIntent();
        login = intent.getStringExtra(LOGIN);
        String name = (String) getText(R.string.activity_name_format);
        setTitle(String.format(name, login));

        String url = preferences.getString("server_url", ParametresActivity.DEFAULT_URL);
        int port = preferences.getInt("server_port", ParametresActivity.DEFAULT_PORT);
        client = new DistribHttpClient(url, port);
    }

    protected void askScan() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_FORMATS", "CODE_128");
            intent.putExtra("RESULT_DISPLAY_DURATION_MS", 0L);
            startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException anfe) {
            Log.e("onCreate", "Scanner Not Found", anfe);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                setWait();
                String contents = intent.getStringExtra("SCAN_RESULT");

                client.getInfos(contents, new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        JSONObject obj = (JSONObject) msg.obj;
                        JSONObject erreur = obj.optJSONObject("erreur");
                        if (erreur != null) {
                            setFailure(erreur.optString("message"));
                        } else {
                            processCommande(obj);
                        }
                    }
                });
            }
            // else continue with any other code you need in the method
        }
    }

    public void loginSuccess() {
        /*CharSequence text = getText(R.string.login_success);
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();*/
    }

    public void setFailure(String message) {
        invalideView.setVisibility(View.VISIBLE);
        succesView.setVisibility(View.GONE);
        warningView.setVisibility(View.GONE);
        waitView.setVisibility(View.GONE);
        ((TextView) invalideView).setText(message);
    }

    public void setSuccess(String message) {
        succesView.setVisibility(View.VISIBLE);
        invalideView.setVisibility(View.GONE);
        warningView.setVisibility(View.GONE);
        waitView.setVisibility(View.GONE);
        ((TextView) succesView).setText(message);
    }

    public void setWarning(String message) {
        warningView.setVisibility(View.VISIBLE);
        invalideView.setVisibility(View.GONE);
        succesView.setVisibility(View.GONE);
        waitView.setVisibility(View.GONE);
        ((TextView) warningView).setText(message);
    }

    public void setWait() {
        warningView.setVisibility(View.GONE);
        invalideView.setVisibility(View.GONE);
        succesView.setVisibility(View.GONE);
        waitView.setVisibility(View.VISIBLE);
    }

    public void endWait() {
        warningView.setVisibility(View.GONE);
        invalideView.setVisibility(View.GONE);
        succesView.setVisibility(View.GONE);
        waitView.setVisibility(View.GONE);
    }

    @Override
    public Object onRetainNonConfigurationInstance () {
        List<Object> list = new ArrayList<Object>();
        list.add(client);
        return list;
    }

    protected void processCommande(JSONObject obj) {
        String date_retrait = obj.optString("date_retrait");
        String date_paiement = obj.optString("date_paiement");

        if (date_paiement == "null") {
            setFailure("Commande non payée !");
            return;
        }
        if (date_retrait == "null") {
            StringBuilder message = new StringBuilder();
            message.append("Commande validée\n");
            message.append(obj.optString("prenom"));
            message.append(" ");
            message.append(obj.optString("nom"));

            setSuccess(message.toString());
            client.retirer(obj.optInt("id"), login, new Handler() {});
        } else {
            setWarning("Commande déjà retirée");
        }
    }

    protected void searchCommande() {
        String query = queryView.getText().toString();
        // on ne fait pas de recherche vide
        if (query.length() == 0)
            return;

        setWait();

        final Activity me = this;
        client.recherche(query, new Handler() {
            @Override
            public void handleMessage(Message msg) {
                JSONObject obj = (JSONObject) msg.obj;
                try {
                    final JSONArray commandes = obj.getJSONArray("commandes");

                    CharSequence[] items = new CharSequence[commandes.length()];
                    for (int i = 0; i < commandes.length(); i++) {
                        JSONObject commande = commandes.optJSONObject(i);
                        StringBuilder b = new StringBuilder();
                        b.append(commande.getString("prenom"));
                        b.append(" ");
                        b.append(commande.getString("nom"));
                        b.append(" (");
                        b.append(String.valueOf(commande.getInt("id")));
                        b.append(")");
                        items[i] = b.toString();
                    }

                    AlertDialog.Builder b = new AlertDialog.Builder(me)
                            .setCancelable(true)
                            .setTitle(getText(R.string.choose_commande))
                            .setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int index) {
                                    d.dismiss();
                                    queryView.setText("");
                                    processCommande(commandes.optJSONObject(index));
                                }
                            });

                    endWait();
                    b.show();
                } catch(Exception e) {
                    return;
                }
            }
        });
    }
}