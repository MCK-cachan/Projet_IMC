package main;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import main.login.LoginActivity;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Playlist;

public class serveur {
    private static final String TAG = "serveur";
    public static final String BASE_URL = "https://hardcore-unifier-aging.ngrok-free.dev/";
    public static final String LOGIN_URL = BASE_URL + "user/login.php";
    public static final String UPDATE_USER_URL = BASE_URL + "user/updateUser.php";
    public static final String SEARCH_URL = BASE_URL + "music/search.php";
    public static final String GET_ALBUM_URL = BASE_URL + "music/album.php";
    public static final String GET_ARTIST_URL = BASE_URL + "music/artiste.php";
    public static final String GET_AUDIO_URL = BASE_URL + "music/audio.php";
    public static final String GET_TRACK_URL = BASE_URL + "music/track.php";
    public static final String FOREVER_URL = BASE_URL + "music/forever.php";
    public static final String SYNC_URL = BASE_URL + "sync.php";

    public interface SearchCallback {
        void onResult(List<?> results);
        void onError(String message);
    }

    public static String getAudioUrl(Long musicId) {
        if (musicId == null) return null;
        return GET_AUDIO_URL + "?id=" + musicId;
    }

    public static String formatImagePath(String path) {
        if (path == null || path.isEmpty() || path.equalsIgnoreCase("null")) {
            return "logo:ic_img_default";
        }
        if (path.startsWith("http") || path.startsWith("logo:")) {
            return path;
        }
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        return BASE_URL + relativePath;
    }

    public static void search(Activity activity, String query, String type, SearchCallback callback) {
        SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = loginPrefs.getString(LoginActivity.KEY_USER_TOKEN, "");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                URL url = new URL(SEARCH_URL + "?q=" + encodedQuery + "&type=" + type);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", token);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);

                    JSONArray array = new JSONArray(response.toString());
                    List<Object> currentResults = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        if ("track".equals(type)) {
                            Musique m = new Musique();
                            m.idMusic = obj.optLong("idMusique", obj.optLong("id", 0));
                            m.musicTitle = obj.optString("title", obj.optString("musicTitle", "Sans titre"));
                            m.artisteName = obj.optString("artist", obj.optString("artisteName", "Inconnu"));
                            m.imagePath = formatImagePath(obj.optString("image", obj.optString("imagePath", "")));
                            m.audioPath = getAudioUrl(m.idMusic);
                            currentResults.add(m);
                        } else if ("artist".equals(type)) {
                            Artiste a = new Artiste();
                            a.idArtiste = obj.optLong("idArtiste", obj.optLong("id", 0));
                            a.Pseudo = obj.optString("artist", obj.optString("name", obj.optString("Pseudo", "Inconnu")));
                            a.imagePath = formatImagePath(obj.optString("image", obj.optString("imagePath", "")));
                            currentResults.add(a);
                        } else if ("album".equals(type) || "playlist".equals(type)) {
                            Playlist p = new Playlist();
                            p.IdPlaylist = obj.optLong("idAlbum", obj.optLong("id", obj.optLong("idPlaylist", 0)));
                            p.Name = obj.optString("album", obj.optString("title", obj.optString("Name", "Sans titre")));
                            p.imagePath = formatImagePath(obj.optString("image", obj.optString("imagePath", "")));
                            currentResults.add(p);
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(currentResults));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Erreur: " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Erreur réseau"));
            }
        });
    }

    public static void getArtistTracks(Context context, Long artistId, SearchCallback callback) {
        fetchTracks(context, GET_ARTIST_URL + "?id=" + artistId, null, artistId, callback);
    }

    public static void getAlbumTracks(Context context, Long albumId, SearchCallback callback) {
        fetchTracks(context, GET_ALBUM_URL + "?id=" + albumId, albumId, null, callback);
    }

    public static void getTrack(Context context, Long trackId, SearchCallback callback) {
        fetchTracks(context, GET_TRACK_URL + "?id=" + trackId, null, null, callback);
    }

    private static void fetchTracks(Context context, String urlString, Long albumId, Long artistId, SearchCallback callback) {
        SharedPreferences loginPrefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = loginPrefs.getString(LoginActivity.KEY_USER_TOKEN, "");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", token);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);

                    String jsonResponse = response.toString();
                    JSONArray array = null;
                    try {
                        array = new JSONArray(jsonResponse);
                    } catch (Exception e) {
                        JSONObject root = new JSONObject(jsonResponse);
                        array = root.optJSONArray("tracks");
                        if (array == null) array = root.optJSONArray("data");
                    }

                    List<Musique> tracks = new ArrayList<>();
                    if (array != null) {
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject obj = array.getJSONObject(i);
                            Musique m = new Musique();
                            m.idMusic = obj.optLong("idMusique", obj.optLong("id", 0));
                            m.musicTitle = obj.optString("title", obj.optString("musicTitle", "Sans titre"));
                            m.artisteName = obj.optString("artist", obj.optString("artisteName", ""));
                            m.imagePath = formatImagePath(obj.optString("image", obj.optString("imagePath", "")));
                            m.audioPath = getAudioUrl(m.idMusic);
                            tracks.add(m);
                        }
                    }
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(tracks));
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError("Erreur: " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError("Erreur réseau"));
            }
        });
    }

    public static void addToForever(Context context, Long musicId) {
        if (musicId == null || musicId <= 0) return;
        
        SharedPreferences loginPrefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = loginPrefs.getString(LoginActivity.KEY_USER_TOKEN, "");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                URL url = new URL(FOREVER_URL + "?id=" + musicId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", token);
                conn.getResponseCode(); // Effectue la requête
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Erreur addToForever", e);
            }
        });
    }

    public static void syncDatabase(Context context, List<Playlist> playlists, List<Musique> musiques, List<Artiste> artistes, Runnable callback) {
        SharedPreferences loginPrefs = context.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = loginPrefs.getString(LoginActivity.KEY_USER_TOKEN, "");

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                JSONObject json = new JSONObject();
                
                JSONArray plArray = new JSONArray();
                for (Playlist p : playlists) {
                    JSONObject obj = new JSONObject();
                    obj.put("IdPlaylist", p.IdPlaylist);
                    obj.put("Name", p.Name);
                    plArray.put(obj);
                }
                json.put("playlists", plArray);

                JSONArray muArray = new JSONArray();
                for (Musique m : musiques) {
                    JSONObject obj = new JSONObject();
                    obj.put("IdMusic", m.idMusic);
                    obj.put("IdPlaylist", m.idPlaylist);
                    obj.put("IdArtiste", m.idArtiste);
                    muArray.put(obj);
                }
                json.put("musiques", muArray);

                JSONArray arArray = new JSONArray();
                for (Artiste a : artistes) {
                    JSONObject obj = new JSONObject();
                    obj.put("IdArtiste", a.idArtiste);
                    obj.put("Pseudo", a.Pseudo);
                    arArray.put(obj);
                }
                json.put("artistes", arArray);

                URL url = new URL(SYNC_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", token);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes("UTF-8"));
                }

                if (conn.getResponseCode() == 200) {
                    if (callback != null) new Handler(Looper.getMainLooper()).post(callback);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Erreur syncDatabase", e);
            }
        });
    }

    public static void updateName(Activity activity, String nom, String prenom) {
        try {
            JSONObject json = new JSONObject();
            if (nom != null && !nom.isEmpty()) json.put("nom", nom);
            if (prenom != null && !prenom.isEmpty()) json.put("prenom", prenom);
            sendUpdateRequest(activity, json, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateEmail(Activity activity, String email) {
        try {
            JSONObject json = new JSONObject();
            json.put("mail", email);
            sendUpdateRequest(activity, json, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updatePassword(Activity activity, String password) {
        try {
            JSONObject json = new JSONObject();
            json.put("password", password);
            sendUpdateRequest(activity, json, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void updateImage(Activity activity, String base64Image, Runnable successCallback) {
        try {
            JSONObject json = new JSONObject();
            json.put("image", base64Image);
            sendUpdateRequest(activity, json, successCallback);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void sendUpdateRequest(Activity activity, JSONObject jsonParam, Runnable successCallback) {
        SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = loginPrefs.getString(LoginActivity.KEY_USER_TOKEN, "");
        if (token.isEmpty()) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            try {
                URL url = new URL(UPDATE_USER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", token);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonParam.toString().getBytes("UTF-8"));
                }
                if (conn.getResponseCode() == 200) success = true;
                conn.disconnect();
            } catch (Exception e) { e.printStackTrace(); }
            
            final boolean finalSuccess = success;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalSuccess) {
                    if (successCallback != null) successCallback.run();
                }
            });
        });
    }
}
