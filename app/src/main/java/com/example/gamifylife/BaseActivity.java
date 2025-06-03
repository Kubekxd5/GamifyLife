package com.example.gamifylife;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gamifylife.helpers.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Before super.attachBaseContext, update the context with the selected locale
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    // You can add other common functionality for your activities here
}