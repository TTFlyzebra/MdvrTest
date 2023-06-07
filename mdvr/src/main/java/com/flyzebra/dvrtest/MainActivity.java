package com.flyzebra.dvrtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.TextView;

import com.flyzebra.dvrtest.databinding.ActivityMainBinding;
import com.quectel.qcarapi.stream.QCarCamera;

public class MainActivity extends AppCompatActivity {

    private SurfaceView sv01;

    // Used to load the 'dvrtest' library on application startup.
    static {
        System.loadLibrary("dvrtest");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sv01 = findViewById(R.id.sv01);

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        try {
            QCarCamera qCarCamera = new QCarCamera(1);
            int ret = qCarCamera.cameraOpen(1, 2);
            qCarCamera.startPreview(0, sv01.getHolder().getSurface(), 720, 1280, QCarCamera.YUV420_NV21);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * A native method that is implemented by the 'dvrtest' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}