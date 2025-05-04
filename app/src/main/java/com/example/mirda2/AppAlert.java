package com.example.mirda2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class AppAlert extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_app);

        Button btnAcknowledge = findViewById(R.id.btnAcknowledge);
        btnAcknowledge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Regresa a MainActivity al pulsar el botón
                Intent intent = new Intent(AppAlert.this, MainActivity.class);
                startActivity(intent);
                finish(); // Opcional: cierra esta activity para liberar recursos
            }
        });
    }
}
