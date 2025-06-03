package com.example.gamifylife.ui.home; // Lub odpowiedni pakiet

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.example.gamifylife.R; // Upewnij się, że R jest z Twojego pakietu
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

@SuppressLint("ViewConstructor") // Jeśli konstruktor ma tylko Context i layoutId
public class AchievementCountMarkerView extends MarkerView {

    private final TextView tvContent;

    public AchievementCountMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvMarkerContent);
    }

    // Wywoływane za każdym razem, gdy MarkerView jest rysowany
    @SuppressLint("SetTextI18n")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // e.getY() to wartość (liczba ukończonych osiągnięć)
        tvContent.setText(getContext().getString(R.string.marker_achievements_completed, (int) e.getY()));
        super.refreshContent(e, highlight);
    }

    private MPPointF mOffset;

    @Override
    public MPPointF getOffset() {
        if(mOffset == null) {
            // Wyśrodkuj poziomo, przesuń w górę
            mOffset = new MPPointF(-(getWidth() / 2f), -getHeight() - 10f); // -10f to mały margines
        }
        return mOffset;
    }
}