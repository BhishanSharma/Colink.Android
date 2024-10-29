package com.example.colink.Listeners;

import java.io.File;

public interface ClickListener {
    void onClick(File file, int position, String song_name, String artist_name, byte[] image);
}
