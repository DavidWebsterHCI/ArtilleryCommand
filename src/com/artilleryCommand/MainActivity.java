package com.artilleryCommand;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Comment placeholder.
 */
public class MainActivity extends Activity {

    //Default constructor
    public MainActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //Note:  TODO This will eventually turn into a splash screen/menu options...etc currently just working on the guts of this app hence the
        //Empty main class
        
        Intent intent = new Intent(getApplicationContext(), com.artilleryCommand.MapActivity.class);
        startActivity(intent);
       
    }
}
