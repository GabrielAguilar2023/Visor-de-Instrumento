package gabriel.visor;

import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VisorDatosGuardados extends AppCompatActivity {

    private View vista;
    private XYPlot plot;
    private SimpleXYSeries seriePlot;
    private static final String ARCHIVO_GUARDADO = "dato.txt";
    private BufferedReader bufferedReader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_datos_guardados);

        vista = findViewById(R.id.Vista);
        plot = findViewById(R.id.plot);
        seriePlot = new SimpleXYSeries("");

        leerDatos(seriePlot);
        seriePlot.useImplicitXVals();
        plot.setRangeBoundaries(0,1024, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0,seriePlot.size()-1,BoundaryMode.FIXED);
        plot.addSeries(seriePlot,new LineAndPointFormatter(Color.argb(255,0,255,0),null,Color.argb(100,0,116,0),null));

    }

    private void leerDatos(SimpleXYSeries seriePlot) {
        File tarjetaSD = Environment.getExternalStorageDirectory();
        File rutaArchivo = new File(tarjetaSD.getAbsolutePath(),ARCHIVO_GUARDADO);
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(rutaArchivo)));
            String texto;

            while ((texto = bufferedReader.readLine())!=null){

                seriePlot.addLast(null,Integer.parseInt(texto));

            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }




    }

    private void hide() {
        vista.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hide();
    }
}
