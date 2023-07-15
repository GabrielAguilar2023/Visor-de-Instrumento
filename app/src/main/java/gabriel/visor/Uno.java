package gabriel.visor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Uno extends AppCompatActivity {

    //Variables necesarias para pedir habilitar el BT
    public int codigoActivacion = 0;
    private String direccionMAC;
    public static String DIRECCION_MAC = "direccionMac";

    public Intent pedidoHabilitacionBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    BluetoothAdapter miBlueTooth = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket ConexionBT;
    static final UUID IdentificadorUnico = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private TextView textoDialogo;
    private ListView listaDispositivos;
    private ImageButton conexionBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ventana_uno);
        textoDialogo = findViewById(R.id.textoInfo);
        conexionBluetooth = (ImageButton) findViewById(R.id.botonBT);
        listaDispositivos = findViewById(R.id.lista);
        //ActivarBT();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivarBT();
        listaDispositivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               String Seleccion = ((TextView) view).getText().toString();
               direccionMAC = Seleccion.substring(Seleccion.length()-17);
               //Toast.makeText(getApplicationContext(), direccionMAC, Toast.LENGTH_LONG).show();

               // Realiza un intent para iniciar la siguiente actividad
               Intent intend = new Intent(getApplicationContext(),Instrumento.class);
               intend.putExtra(DIRECCION_MAC, direccionMAC);
               startActivity(intend);

           }
        }
        );





    }

    private void ActivarBT() {
        if (miBlueTooth.isEnabled())
            activado();
        else
            desactivado();

        conexionBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (miBlueTooth == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth no es soportado en este dispositivo", Toast.LENGTH_LONG).show();
                } else {
                    if (miBlueTooth.isEnabled()) {
                        miBlueTooth.disable();
                        listaDispositivos.setVisibility(View.INVISIBLE);
                        conexionBluetooth.setImageResource(R.drawable.btoff);
                    }else if (!miBlueTooth.isEnabled()) {
                            startActivityForResult(pedidoHabilitacionBluetooth, codigoActivacion);
                            listaDispositivos.setVisibility(View.VISIBLE);
                            }
                    }
                }
        });
    }

    private void desactivado() {
        conexionBluetooth.setImageResource(R.drawable.btoff);
        listaDispositivos.setVisibility(View.INVISIBLE);
    }

    private void activado() {
        conexionBluetooth.setImageResource(R.drawable.bton);
        listaDispositivos.setVisibility(View.VISIBLE);
        try {
            TimeUnit.SECONDS.sleep(1);
            listarDispositivosConectados();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult (int requestCode,int resultCode, Intent data){
        //   Toast.makeText(this,"Cualquiera",Toast.LENGTH_SHORT).show();
        if(requestCode== codigoActivacion){
            if(resultCode==RESULT_OK){
                Toast.makeText(getApplicationContext(),"Activando Bluetooth",Toast.LENGTH_LONG).show();
                conexionBluetooth.setImageResource(R.drawable.bton);
            }else if (resultCode==RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"Cancelando la activacion del Bluetooth",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void listarDispositivosConectados() {
        Set<BluetoothDevice> dispositivosConectados = miBlueTooth.getBondedDevices();
        textoDialogo.setText(Integer.toString(dispositivosConectados.size())+" Dispositivos conectados");
        String [] dispositivos = new String[dispositivosConectados.size()];
        int indice=0;
        if (dispositivosConectados.size()>0){
            for ( BluetoothDevice nombre:dispositivosConectados){
                dispositivos[indice] = nombre.getName()+"\n"+nombre.getAddress();
                indice++;
            }
        }
        ArrayAdapter <String> adaptador = new ArrayAdapter<>(getApplicationContext(),android.R.layout.simple_list_item_1,dispositivos);
        listaDispositivos.setAdapter(adaptador);

    }


    public void irAInstrumento(View v){
        Intent intentoIrInstrumento = new Intent(this, Instrumento.class);
        startActivity(intentoIrInstrumento);


    }
}