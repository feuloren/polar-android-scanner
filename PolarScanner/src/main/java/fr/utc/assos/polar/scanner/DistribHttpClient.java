package fr.utc.assos.polar.scanner;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import org.json.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by florent on 12/10/13.
 */
public class DistribHttpClient {

    public final int SUCCESS = 0;
    public final int NOT_FOUND = 1;
    public final int FORBIDDEN = 2;

    private String server_url;
    private int server_port;
    private String complete_url;


    public DistribHttpClient(String url, int port) {
        server_url = url;
        server_port = port;
        complete_url = url + ":" + String.valueOf(port);

        CookieHandler.setDefault( new CookieManager( null, CookiePolicy.ACCEPT_ALL ) );
    }

    public void get(final String urlPart, final HashMap<String, String> params, final Handler handler) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    StringBuilder donnees = new StringBuilder();
                    donnees.append(complete_url);
                    donnees.append(urlPart);
                    if (params != null) {
                        donnees.append("?");
                        int c = 0;
                        Iterator it = params.entrySet().iterator();
                        while (it.hasNext()) {
                            if (c > 0)
                                donnees.append("&");
                            c++;

                            Map.Entry pairs = (Map.Entry)it.next();
                            donnees.append(URLEncoder.encode((String) pairs.getKey(), "UTF-8"));
                            donnees.append("=");
                            donnees.append(URLEncoder.encode((String) pairs.getValue(), "UTF-8"));
                        }
                    }

                    URL url = new URL(donnees.toString());

                    // Ouverture de la connexion
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                    // Si le serveur nous répond avec un code OK
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        StringBuilder response = new StringBuilder();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String ligne;

                        // Tant que « ligne » n'est pas null, c'est que le flux n'a pas terminé d'envoyer des informations
                        while ((ligne = reader.readLine()) != null) {
                            response.append(ligne);
                        }

                        JSONObject parsed = new JSONObject(response.toString());
                        Message msg = handler.obtainMessage(SUCCESS, parsed);
                        handler.sendMessage(msg);
                    } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        Log.d("CLIENT", "!! 404 !!");
                    } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        Log.d("CLIENT", "!! 403 !!");
                    }
                    urlConnection.disconnect();
                } catch (Exception e) {
                    Log.d("CLIENT", "exception = " + e.toString());
                }

            }
        }).start();
    }

    public void getInfos(String barcode, Handler handler) {
        get("/commande/infos/" + barcode, null, handler);
    }

    public void recherche(String query, Handler handler) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("q", query);
        get("/recherche", params, handler);
        //return new JSONArray();
    }

    public void retirer(int id, String login, Handler handler) {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("login", login);
        params.put("id", String.valueOf(id));
        get("/commande/retirer/" + String.valueOf(id), params, handler);
    }

}