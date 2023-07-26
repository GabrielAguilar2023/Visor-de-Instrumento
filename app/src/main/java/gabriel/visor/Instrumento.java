package gabriel.visor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.ekn.gruzer.gaugelibrary.HalfGauge;

public class Instrumento extends AppCompatActivity {

    BluetoothAdapter miBluetooth = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice dispositivoBluetooth;
    private BluetoothSocket socketDeBluetooth;
    static final UUID IdentificadorUnico = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String direccionMac;
    static final byte INICIO_BUFFER = 42; //Caracter "*" indica el inicio del paquete recibido
    private Handler handlerDeBluetooth;
    final int handlerState = 0;
    private HiloConectado MyConexionBT;

    private static final int RETARDO_ANIMACION = 300;

    private static final int TAMAÑO_MUESTRAS = 50;
    private final Handler mHideHandler = new Handler();
    private String palabraEnBinario;
    private boolean estanVisiblesLosControles;
    private String unidadesDeMedicion = "mH";
    private String nombreInstrumento = "Inductómetro";

    private View pantallaPrincipal;
    private View controlesOcultables;
    private TextView texto;
    private TextView unidades;
    private XYPlot plot;
    private SimpleXYSeries seriePlot;
    private Redrawer redrawer;

    HalfGauge medidor;
    com.ekn.gruzer.gaugelibrary.Range Rango1,Rango2,Rango3;

    //-------------------------------------------------------
    private final Runnable OcultarRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            pantallaPrincipal.setSystemUiVisibility(
                      View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable MostrarRunnable = new Runnable() {
        @Override
        public void run() {
            controlesOcultables.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instrumento);
        medidor = findViewById(R.id.medidor);
        texto = findViewById(R.id.texto);
        unidades = findViewById(R.id.unidades);
        plot = findViewById(R.id.plot);

        unidades.setText(nombreInstrumento);

        Rango1= new com.ekn.gruzer.gaugelibrary.Range(); Rango1.setFrom(0); Rango1.setTo(500); Rango1.setColor(0xFF14FB27);
        Rango2= new com.ekn.gruzer.gaugelibrary.Range(); Rango2.setFrom(500); Rango2.setTo(800); Rango2.setColor(0xFFF3FB14);
        Rango3= new com.ekn.gruzer.gaugelibrary.Range(); Rango3.setFrom(800); Rango3.setTo(1024); Rango3.setColor(0xFFFB1414);

        medidor.addRange(Rango1); medidor.addRange(Rango2); medidor.addRange(Rango3);
        medidor.setMinValue(0); medidor.setMaxValue(1024);
        medidor.setValueColor(0x0014FB27);
        medidor.setMaxValueTextColor(Color.LTGRAY);
        medidor.setMinValueTextColor(Color.LTGRAY);
        medidor.setNeedleColor(Color.GRAY);
        medidor.setKeepScreenOn(true);

        seriePlot = new SimpleXYSeries("");
        seriePlot.useImplicitXVals();
        plot.addSeries(seriePlot,new LineAndPointFormatter(Color.argb(255,0,255,0),null,Color.argb(100,0,116,0),null));
        plot.setRangeBoundaries(0,1024, BoundaryMode.FIXED);
        redrawer = new Redrawer(plot,5,true);
        controlesOcultables = findViewById(R.id.controlesOcultables);
        pantallaPrincipal = findViewById(R.id.pantallaPrincipal);
        estanVisiblesLosControles = true;
        hide();
        medidor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            mostrarOcultar();}});
            conectarBluetooth();
            manejarHandlerDeBluetooth();

        MyConexionBT = new HiloConectado(socketDeBluetooth);
        MyConexionBT.start();
        MyConexionBT.write("muchos datos");
    }

    @SuppressLint("HandlerLeak")
    public synchronized void manejarHandlerDeBluetooth() {
        handlerDeBluetooth = new Handler(){
            public void handleMessage (android.os.Message msg){
                if (msg.what == handlerState) {
                    //Interacción con los datos de ingreso
                    byte[] buffer = (byte[])msg.obj;
                    int valor = convertirAInt(buffer);
                    medidor.setValue(valor);
                    texto.setText(String.valueOf(valor)+ " " + unidadesDeMedicion);
                    if (seriePlot.size() > TAMAÑO_MUESTRAS){
                        seriePlot.removeFirst();
                        seriePlot.addLast(null,valor);
                    }else{
                        seriePlot.addLast(null,valor);
                    }
                }
            }
        };
    }

    private void conectarBluetooth() {

        Intent intent = getIntent();
        direccionMac = intent.getStringExtra(Uno.DIRECCION_MAC);
        //Toast.makeText(getApplicationContext(),direccionMac,Toast.LENGTH_SHORT).show();
        dispositivoBluetooth= miBluetooth.getRemoteDevice(direccionMac);

        try {
            socketDeBluetooth = dispositivoBluetooth.createRfcommSocketToServiceRecord(IdentificadorUnico);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        try {
            socketDeBluetooth.connect();
        //    Toast.makeText(getBaseContext(), "CONEXION EXITOSA", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Fallo en la Conexion", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarOcultar() {
        if (estanVisiblesLosControles) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        controlesOcultables.setVisibility(View.GONE);
        estanVisiblesLosControles = false;
        mHideHandler.removeCallbacks(MostrarRunnable);
        mHideHandler.postDelayed(OcultarRunnable, RETARDO_ANIMACION);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        pantallaPrincipal.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        estanVisiblesLosControles = true;
        mHideHandler.removeCallbacks(OcultarRunnable);
        mHideHandler.postDelayed(MostrarRunnable, RETARDO_ANIMACION);
    }

    public void salirInstrumento (View v){
    finish();
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (!socketDeBluetooth.isConnected()){
           //Todo: mostrar indicacion de si está o no conectado el bluetooth
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        try {
          socketDeBluetooth.close();
          miBluetooth.disable();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Fallo en la desconexion", Toast.LENGTH_SHORT).show();
        }
    }

    //Crea la clase que permite crear el evento de conexion
    public class HiloConectado extends Thread
    {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public HiloConectado(BluetoothSocket socket)
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
            byte[] buffer = new byte[4];

            int numBytes;
            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (true) {
                try {
                    numBytes = mmInStream.read(buffer);
                    if (buffer[0]!=INICIO_BUFFER)
                    handlerDeBluetooth.obtainMessage(handlerState,numBytes,-1,buffer).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        //Envio de trama
        public void write(String input)
        {
            try {
                mmOutStream.write(input.getBytes());
            }
            catch (IOException e)
            {
                //si no es posible enviar datos se cierra la conexión
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public int convertirAInt (byte[] vector){
        palabraEnBinario="";
        for (int i=0; i < vector.length;i++){
            agregarByte(vector[i]);
        }
        return Integer.parseInt(palabraEnBinario,2);
    }
    private void agregarByte(byte b1){
        palabraEnBinario = palabraEnBinario + String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
    }
}