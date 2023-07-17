package gabriel.visor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

public class Instrumento extends AppCompatActivity {

    BluetoothAdapter miBluetooth = BluetoothAdapter.getDefaultAdapter();
    BluetoothDevice dispositivoBluetooth;
    private BluetoothSocket socketDeBluetooth;
    static final UUID IdentificadorUnico = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Handler handlerDeBluetooth;
    final int handlerState = 0;
    private HiloConectado MyConexionBT;

    private static final int RETARDO_ANIMACION = 300;
    private final Handler mHideHandler = new Handler();
    private View pantallaPrincipal;
    private View controlesOcultables;
    private boolean estanVisiblesLosControles;
    private TextView texto;


    private static String direccionMac;

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
        texto = findViewById(R.id.texto);
        estanVisiblesLosControles = true;
        controlesOcultables = findViewById(R.id.controlesOcultables);
        pantallaPrincipal = findViewById(R.id.pantallaPrincipal);
        hide();
        pantallaPrincipal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            mostrarOcultar();}});
        conectarBluetooth();
        manejarHandlerDeBluetooth();

        MyConexionBT = new HiloConectado(socketDeBluetooth);
        MyConexionBT.start();
MyConexionBT.write("muchos datos");



    }

    public void manejarHandlerDeBluetooth() {
        handlerDeBluetooth = new Handler(){
            public void handleMessage (android.os.Message msg){
                if (msg.what == handlerState) {
                    //Interacción con los datos de ingreso
                    byte[] valor = (byte[])msg.obj;
                    int entero1 = (int) valor[0];
                    String string = Arrays.toString(valor);
                    texto.setText(string);
                }
            }
        };
    }

    private void conectarBluetooth() {

        Intent intent = getIntent();
        direccionMac = intent.getStringExtra(Uno.DIRECCION_MAC);
        Toast.makeText(getApplicationContext(),direccionMac,Toast.LENGTH_SHORT).show();
        dispositivoBluetooth= miBluetooth.getRemoteDevice(direccionMac);

        try {

            socketDeBluetooth = dispositivoBluetooth.createRfcommSocketToServiceRecord(IdentificadorUnico);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        try {
            socketDeBluetooth.connect();
            Toast.makeText(getBaseContext(), "CONEXION EXITOSA", Toast.LENGTH_SHORT).show();

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
                    //String string = buffer.toString();
                    handlerDeBluetooth.obtainMessage(handlerState,numBytes,-1,buffer).sendToTarget();
                    //handlerDeBluetooth.obtainMessage(handlerState, ch).sendToTarget();

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

}