package gabriel.visor;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class VisorDatosGuardados extends AppCompatActivity {

    private boolean resetearGrafico = false;
    private boolean primerCicloDosDedos = true;
    private boolean primerCicloUnDedo = true;

    private float limiteInferiorX;
    private float limiteSuperiorX;
    private float limiteInferiorY;
    private float limiteSuperiorY;
    private float xAnterior = 0;
    private float yAnterior = 0;
    private float hipotenusaInicial = 0;

    private static final int Y_MIN = 0;
    private static final int Y_MAX = 1024;

    private static final String ARCHIVO_GUARDADO = "dato.txt";
    private String texto = "";

    private TextView textView;
    private View vista;
    private XYPlot plot;
    private SimpleXYSeries seriePlot;
    private BufferedReader bufferedReader;
    private LineAndPointFormatter formatoPlot;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_datos_guardados);

        vista = findViewById(R.id.Vista);
        seriePlot = new SimpleXYSeries("");
        plot = findViewById(R.id.plot);
        textView = findViewById(R.id.textView);

        leerDatos();
        seriePlot.useImplicitXVals();
        limiteInferiorX = 0 - 0.01f * (seriePlot.size()-1);
        limiteSuperiorX = (seriePlot.size()-1) * 1.01f;
        limiteInferiorY = Y_MIN * 1.01f;
        limiteSuperiorY = Y_MAX * 1.01f;

        formatoPlot = new LineAndPointFormatter(this, R.xml.formato_plot);

        plot.addSeries(seriePlot, formatoPlot);
        plot.setRangeBoundaries(Y_MIN,Y_MAX, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0,limiteSuperiorX,BoundaryMode.FIXED);

        plot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            primerCicloDosDedos = true;        // para almacenar el primer valor de la hipotenusa (para zoom)
                            primerCicloUnDedo = true;          // para scroll
                            xAnterior = event.getX();
                            yAnterior = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (primerCicloUnDedo) {
                                xAnterior = event.getX();
                                yAnterior = event.getY();
                                primerCicloUnDedo = false;
                            }
                            desplazar(event.getX(),event.getY());
                            break;
                        case MotionEvent.ACTION_UP:
                            if (resetearGrafico) {
                                plot.setDomainBoundaries(limiteInferiorX, limiteSuperiorX, BoundaryMode.FIXED);
                                plot.setRangeBoundaries(limiteInferiorY, limiteSuperiorY, BoundaryMode.FIXED);
                                resetearGrafico = false;
                            }
                            if ((plot.getBounds().getMaxX().intValue() - plot.getBounds().getMinX().intValue())>30) {
                                formatoPlot.getPointLabelFormatter().getTextPaint().setColor(Color.TRANSPARENT);
                            }
                            else{
                                formatoPlot.getPointLabelFormatter().getTextPaint().setColor(Color.CYAN);

                            }
                            plot.redraw();
                            break;
                    }
                }
                if (event.getPointerCount()== 2){
                    ampliar(event.getX(0),event.getY(0),event.getX(1),event.getY(1));
                    primerCicloUnDedo = true;
                }

                if (event.getPointerCount()== 3){
                    resetearGrafico = true;
                }
                textView.setText(texto);
                return true;
            }
        });
    }

    private void leerDatos() {
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

    public void desplazar(float x, float y){

        // ********  manejo de X *********
        float limiteSuperiorActualX = plot.getBounds().getMaxX().floatValue();
        float limiteInferiorActualX = plot.getBounds().getMinX().floatValue();
        float intervaloX = limiteSuperiorActualX - limiteInferiorActualX;
        float tamañoPlotX = plot.getWidth(); // Ancho del plot
        float tazaDesplazamientoX = intervaloX/tamañoPlotX;
        limiteInferiorActualX += (xAnterior-x) * tazaDesplazamientoX ;
        limiteSuperiorActualX += (xAnterior-x) * tazaDesplazamientoX;

        // ********  manejo de Y *********
        float limiteSuperiorActualY = plot.getBounds().getMaxY().floatValue();
        float limiteInferiorActualY = plot.getBounds().getMinY().floatValue();
        float intervaloY = limiteSuperiorActualY - limiteInferiorActualY;
        float tamañoPlotY = plot.getHeight(); // Alto del plot
        float tazaDesplazamientoY = intervaloY/tamañoPlotY;
        limiteInferiorActualY -= (yAnterior-y) * tazaDesplazamientoY ;
        limiteSuperiorActualY -= (yAnterior-y) * tazaDesplazamientoY;

        if ((limiteInferiorActualX < limiteSuperiorActualX)&&(limiteInferiorActualY < limiteSuperiorActualY)){
            if(limiteInferiorActualX >= limiteInferiorX && limiteSuperiorActualX <= limiteSuperiorX) {

                plot.setDomainBoundaries(limiteInferiorActualX, limiteSuperiorActualX, BoundaryMode.FIXED);
                plot.setRangeBoundaries(limiteInferiorActualY, limiteSuperiorActualY, BoundaryMode.FIXED);
                plot.redraw();
            }
        }
        xAnterior = x;
        yAnterior = y;
    }

    private void ampliar(float x1, float y1, float x2, float y2) {

        float limiteSuperiorActualX = plot.getBounds().getMaxX().floatValue();
        float limiteInferiorActualX = plot.getBounds().getMinX().floatValue();
        float limiteSuperiorActualY = plot.getBounds().getMaxY().floatValue();
        float limiteInferiorActualY = plot.getBounds().getMinY().floatValue();

        //***** Calculo de la distancia entre los dedos (Pitagoras)
        float cateto1 = x1 - x2;
        float cateto2 = y1 - y2;
        float hipotenusa = (float) Math.sqrt(cateto1*cateto1 + cateto2*cateto2);
        if (primerCicloDosDedos){
            hipotenusaInicial = hipotenusa;
            primerCicloDosDedos = false;
        }
        float distancia = hipotenusa - hipotenusaInicial;

        //***** Establecimiento de nuevo limite al grafico para hacer zoom
        float sensibilidad = plot.getWidth()/(limiteSuperiorActualX-limiteInferiorActualX); //Sensibilidad automatica del zoom para mayor precision
        float distanciaX=distancia/sensibilidad;
        float relacionAspecto = plot.getWidth()/plot.getHeight();  // ¡¡Para Porstrait inverso de este valor!!
        float distanciaY= distanciaX /relacionAspecto;

        limiteInferiorActualX += distanciaX;
        limiteSuperiorActualX -= distanciaX;
        limiteInferiorActualY += distanciaY;
        limiteSuperiorActualY -= distanciaY;

        if ((limiteInferiorActualX < limiteSuperiorActualX)&&(limiteInferiorActualY < limiteSuperiorActualY)){
            if(limiteInferiorActualX>=limiteInferiorX && limiteSuperiorActualX<=limiteSuperiorX) {
                plot.setDomainBoundaries(limiteInferiorActualX, limiteSuperiorActualX, BoundaryMode.FIXED);
                plot.setRangeBoundaries(limiteInferiorActualY, limiteSuperiorActualY, BoundaryMode.FIXED);
                plot.redraw();
            }
        }
        hipotenusaInicial = hipotenusa;
    }
}
