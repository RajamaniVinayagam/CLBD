package com.dreamerindia.clbd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        addListenerOnSpinnerItemSelection();
    }

    public void addListenerOnSpinnerItemSelection() {
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View view,
                                       int position, long row_id) {
                final Intent intent;
                switch (position) {
                    case 0:
                        // Toast.makeText(getApplicationContext(), "Choose a Train from Spinner", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        intent = new Intent(MainActivity.this, com.dreamerindia.clbd.TrainSchedule.class);
                        startActivity(intent);
                        break;
                    default:
                        Toast.makeText(getApplicationContext(),
                                String.valueOf("Schedule not available for " +spinner.getItemAtPosition(position)), Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub

            }

        });
    }

    protected void onDestroy() {
        System.exit(0);
        super.onDestroy();
    }
}