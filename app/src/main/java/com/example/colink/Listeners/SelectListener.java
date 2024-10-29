package com.example.colink.Listeners;

import java.io.File;

public interface SelectListener {
    void onItemSelect(File item);

    void onItemDeSelect(File item);
}