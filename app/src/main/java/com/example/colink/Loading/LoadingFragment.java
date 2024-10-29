package com.example.colink.Loading;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.colink.R;
import com.example.colink.Tabs.MusicPlayerFragment;

import java.io.File;
import java.util.HashMap;

public class LoadingFragment extends Fragment {

    private HashMap<File, Boolean> allSongs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_loading_fragement, container, false);

        System.out.println("Entered the loading Fragment");
        loadData();
        return view;
    }

    private void loadData() {
        new Thread(() -> {
            allSongs = loadFiles(Environment.getExternalStorageDirectory(), new String[]{".mp3"});
            requireActivity().runOnUiThread(this::onDataLoaded);
        }).start();
    }

    private HashMap<File, Boolean> loadFiles(File directory, String[] extensions) {
        HashMap<File, Boolean> fileBooleanMap = new HashMap<>();
        File[] folder = directory.listFiles();
        if (folder != null) {
            for (File file : folder) {
                if (!file.isHidden() && file.isDirectory()) {
                    fileBooleanMap.putAll(loadFiles(file, extensions));
                } else {
                    for (String ext : extensions) {
                        if (file.getName().endsWith(ext) && !file.getName().startsWith(".")) {
                            fileBooleanMap.put(file, false);
                            break;
                        }
                    }
                }
            }
        }
        return fileBooleanMap;
    }

    private void onDataLoaded() {
        if (allSongs != null && !allSongs.isEmpty()) {
            Bundle result = new Bundle();
            result.putSerializable("allSongs", allSongs);
            getParentFragmentManager().setFragmentResult("dataFromLoader", result);
        }

        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.FrameLayout, new MusicPlayerFragment())
                    .commit();
        }
    }
}