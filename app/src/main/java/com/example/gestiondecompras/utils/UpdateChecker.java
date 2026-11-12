package com.example.gestiondecompras.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.gestiondecompras.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateChecker {

    private static final String TAG = "UPDATER";
    private static final String REPO = "Juniorbp20/GestionDeCompras";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    public interface Callback {
        void onUpdateAvailable(String version, String apkUrl);
        void onNoUpdate();
    }

    private UpdateChecker() {
    }

    public static void check(Callback callback) {
        EXECUTOR.execute(() -> {
            String version = null;
            String apkUrl = null;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "GestionDeCompras/" + BuildConfig.VERSION_NAME);
                int code = conn.getResponseCode();
                if (code == 200) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        JSONObject json = new JSONObject(sb.toString());
                        version = normalize(json.optString("tag_name"));
                        JSONArray assets = json.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.optJSONObject(i);
                                String name = asset != null ? asset.optString("name") : "";
                                if (name.endsWith(".apk")) {
                                    apkUrl = asset.optString("browser_download_url");
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "GitHub respondio HTTP " + code);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.d(TAG, "Error consultando actualizaciones: " + e.getMessage());
            }
            String finalVersion = version;
            String finalApkUrl = apkUrl;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (finalVersion != null && finalApkUrl != null && esNuevaVersion(finalVersion)) {
                    Log.d(TAG, "Nueva version disponible: " + finalVersion);
                    callback.onUpdateAvailable(finalVersion, finalApkUrl);
                } else {
                    callback.onNoUpdate();
                }
            });
        });
    }

    public static boolean esNuevaVersion(String nueva) {
        return esNuevaVersion(BuildConfig.VERSION_NAME, nueva);
    }

    static boolean esNuevaVersion(String actual, String nueva) {
        String[] newParts = split(normalize(nueva));
        String[] curParts = split(normalize(actual));
        for (int i = 0; i < Math.max(newParts.length, curParts.length); i++) {
            int nv = i < newParts.length ? parseInt(newParts[i]) : 0;
            int cv = i < curParts.length ? parseInt(curParts[i]) : 0;
            if (nv != cv) {
                return nv > cv;
            }
        }
        return false;
    }

    private static String[] split(String v) {
        if (v == null || v.trim().isEmpty()) {
            return new String[0];
        }
        return v.trim().split("\\.");
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalize(String version) {
        if (version == null) {
            return null;
        }
        String t = version.trim();
        while (!t.isEmpty() && (t.charAt(0) == 'v' || t.charAt(0) == 'V')) {
            t = t.substring(1);
        }
        return t;
    }
}
