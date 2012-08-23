package org.garrapeta.commons;

import org.garrapeta.commons.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

/**
 * Activity to test the AnimatedToggleButton component
 *
 */
public class AnimatedToggleButtonActivity extends Activity {

    private AnimatedToggleButton mAnimatedToggleButton;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animated_toggle_button);
        mAnimatedToggleButton = (AnimatedToggleButton) findViewById(R.id.animated_toggle_button); 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_animated_toggle_button, menu);
        return true;
    }
    
    
    // Listeners to the buttons in the XML
    
    public void commitStateChange(View view) {
        try {
            mAnimatedToggleButton.commitCheckedChange();
        } catch (IllegalStateException ise) {
            handle(ise);
        }
    }

    public void cancelStateChange(View view) {
        try {
            mAnimatedToggleButton.cancelCheckedChange();
        } catch (IllegalStateException ise) {
            handle(ise);
        }
    }

    private void handle(Throwable t) {
        Toast.makeText(this, t.getMessage(), Toast.LENGTH_LONG).show();
    }
}
