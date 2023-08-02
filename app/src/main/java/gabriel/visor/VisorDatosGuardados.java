package gabriel.visor;

import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PanZoom;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class VisorDatosGuardados extends AppCompatActivity {


    float escala=1f;
    float actual= 0.1f;

    boolean primerCicloDosDedos = true;
    boolean primerCicloUnDedo = true;
    float xAnterior = 0;
    float yAnterior=0;
    int hipotenusaInicial = 0;


    private View vista;
    private XYPlot plot;
    private SimpleXYSeries seriePlot;
    private static final int Y_MIN = 0;
    private static final int Y_MAX = 1024;
    private static final String ARCHIVO_GUARDADO = "dato.txt";
    private BufferedReader bufferedReader;
    private LineAndPointFormatter formatoPlot;
    private TextView textView;
    private GestureDetector gestos;
    Redrawer redrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.visor_datos_guardados);

        vista = findViewById(R.id.Vista);
        plot = findViewById(R.id.plot);
        gestos = new GestureDetector(this,new EscuchaGestos());

        seriePlot = new SimpleXYSeries("");

        textView = findViewById(R.id.textView);

        leerDatos();
        seriePlot.useImplicitXVals();

        plot.setRangeBoundaries(Y_MIN,Y_MAX, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0,seriePlot.size()-1,BoundaryMode.FIXED);

        formatoPlot = new LineAndPointFormatter(Color.argb(255,0,0,255),null,Color.argb(100,0,0,116),null);
        formatoPlot.getLinePaint().setStrokeWidth(2f);

        plot.addSeries(seriePlot, formatoPlot);
        //plot.getOuterLimits().set(0,seriePlot.size()-1,Y_MIN,Y_MAX);
        redrawer = new Redrawer(plot,10,false);

        plot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int hipotenusa;
                String palabra="";
                if (event.getPointerCount()==3){
                    plot.setDomainBoundaries(3f,6f, BoundaryMode.FIXED);
                    plot.redraw();
                }

                if (event.getPointerCount()==1) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            palabra ="down, ";
                            primerCicloDosDedos = true;         // para almacenar el primer valor de la hipotenusa (para zoom)
                            primerCicloUnDedo = true;          //  Indica que es el primer ciclo de motion event para ACTION_MOVE (para scroll)
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (primerCicloUnDedo) {
                                xAnterior = event.getX();
                                yAnterior = event.getY();
                                primerCicloUnDedo = false;
                            }
                            desplazar(event.getX(),event.getY());
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            palabra ="cancel, ";
                            break;
                        case MotionEvent.ACTION_UP:
                            palabra ="up,  ";
                            break;
                    }
                }
                if (event.getPointerCount()==2){

                    int y1 = (int) event.getY(0);
                    int x1 = (int) event.getX(0);
                    int y2 = (int) event.getY(1);
                    int x2 = (int) event.getX(1);

                    int cateto1 = x1 - x2;
                    int cateto2 = y1 - y2;
                    hipotenusa = (int) Math.sqrt(cateto1*cateto1+cateto2*cateto2);

                    if (primerCicloDosDedos){
                        hipotenusaInicial = hipotenusa;
                        primerCicloDosDedos = false;
                    }
                    int distancia = hipotenusa - hipotenusaInicial;
                    palabra = "Dedo 1 = " + x1 + " , "+ y1 + "\n" +
                            "Dedo 2 = " + x2 + " , "+ y2 + "\n" +
                            distancia;
                    primerCicloUnDedo = true;
                }
                textView.setText(palabra);
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

    @Override
    public boolean onTouchEvent (MotionEvent event){
        gestos.onTouchEvent(event);
        return super.onTouchEvent(event);

    }
    class EscuchaGestos extends GestureDetector.SimpleOnGestureListener{
        @Override
        public void onLongPress(MotionEvent e) {
            textView.setText("onLongPress");
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            textView.setText("onSingleTapUp");
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            textView.setText("onDoubleTap");
            escala =1f;

            plot.setScaleY(escala);
            plot.setScaleX(escala);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // e1 posicion inicial
        // e2 posicion final

            if (e2.getY()<e1.getY()){
                 //actual =  plot.getBounds().getxRegion().getMax().intValue()+5;
                escala +=actual;
                plot.setScaleX(escala) ;
                plot.setScaleY(escala);
              //  plot.getBounds().getxRegion().setMax(actual);
                textView.setText(String.valueOf(escala));
            }else{
                //actual = plot.getBounds().getxRegion().getMax().intValue()+5 ;
               // plot.getBounds().getxRegion().setMax(actual);
                escala-=actual;
                plot.setScaleY(escala);
                plot.setScaleX(escala);
                textView.setText(String.valueOf(escala));



            }

            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {



            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }

    public void desplazar(float x, float y){

        // ********  manejo de X *********
        float limiteSuperiorActualX = plot.getBounds().getMaxX().floatValue();
        float limiteInferiorActualX = plot.getBounds().getMinX().floatValue();
        float intervaloX = limiteSuperiorActualX - limiteInferiorActualX;
        float tamañoPlotX = plot.getWidth(); // Ancho del plot
        float tazaDesplazamientoX = intervaloX/tamañoPlotX;     // Porporcional a los valores numericos del grafico
        limiteInferiorActualX += (xAnterior-x)* tazaDesplazamientoX ;
        limiteSuperiorActualX += (xAnterior-x) * tazaDesplazamientoX;

        // ********  manejo de Y *********
        float limiteSuperiorActualY = plot.getBounds().getMaxY().floatValue();
        float limiteInferiorActualY = plot.getBounds().getMinY().floatValue();
        float intervaloY = limiteSuperiorActualY - limiteInferiorActualY;
        float tamañoPlotY = plot.getHeight(); // Alto del plot
        float tazaDesplazamientoY = intervaloY/tamañoPlotY;     // Porporcional a los valores numericos del grafico
        limiteInferiorActualY -= (yAnterior-y)* tazaDesplazamientoY ;
        limiteSuperiorActualY -= (yAnterior-y) * tazaDesplazamientoY;

        plot.setRangeBoundaries(limiteInferiorActualY,limiteSuperiorActualY,BoundaryMode.FIXED);
        plot.setDomainBoundaries(limiteInferiorActualX, limiteSuperiorActualX, BoundaryMode.FIXED);
        plot.redraw();

        xAnterior = x;
        yAnterior = y;
    }

}
