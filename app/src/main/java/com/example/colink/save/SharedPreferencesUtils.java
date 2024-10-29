package com.example.colink.save;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class SharedPreferencesUtils {

  private static final String PREF_NAME = "MySharedPrefs";
  private static final String KEY_FILE_PATHS = "FilePaths";

  public static void saveFiles(Context context, ArrayList<File> files) {
    SharedPreferences sharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPreferences.edit();

    Gson gson = new Gson();
    String json = gson.toJson(files);
    editor.putString(KEY_FILE_PATHS, json);
    editor.apply();
  }

  public static ArrayList<File> getFiles(Context context) {
    SharedPreferences sharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    String json = sharedPreferences.getString(KEY_FILE_PATHS, null);

    Gson gson = new Gson();
    Type type = new TypeToken<ArrayList<File>>() {}.getType();
    return gson.fromJson(json, type);
  }
}
