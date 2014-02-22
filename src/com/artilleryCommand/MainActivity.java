package com.artilleryCommand;

import java.lang.Math;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(3)
public class MainActivity extends Activity {
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Note:  TODO This will eventually turn into a splash screen/menu options...etc currently just working on the guts of this app hence the
        //Empty main class
        
        Intent intent = new Intent(getApplicationContext(), com.artilleryCommand.MapActivity.class);
        startActivity(intent);
       
    }
}