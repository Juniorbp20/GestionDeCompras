package com.example.gestiondecompras.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gestiondecompras.databinding.ActivityAyudaBinding;

public class AyudaActivity extends AppCompatActivity {

    private ActivityAyudaBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAyudaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        setupZoomControls();
    }

    private void setupZoomControls() {
        binding.btnVerTutorial.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, TutorialActivity.class)));
        binding.fabZoomIn.setOnClickListener(v -> adjustTextSize(1.1f));
        binding.fabZoomOut.setOnClickListener(v -> adjustTextSize(0.9f));
    }

    private void adjustTextSize(float multiplier) {
        int childCount = binding.contentLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            android.view.View child = binding.contentLayout.getChildAt(i);
            if (child instanceof android.widget.TextView) {
                android.widget.TextView textView = (android.widget.TextView) child;
                float currentSizePx = textView.getTextSize();
                float newSizePx = currentSizePx * multiplier;
                
                // Optional: Min/Max constraints
                float density = getResources().getDisplayMetrics().scaledDensity;
                float currentSizeSp = currentSizePx / density;
                
                if (multiplier > 1 && currentSizeSp > 40) continue; // Max limit 
                if (multiplier < 1 && currentSizeSp < 10) continue; // Min limit

                textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, newSizePx);
            }
        }
    }
}
