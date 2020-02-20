package gg.com;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button openButton,sendButton,closeButton;
    TextView myLabel;
    EditText myTextbox;

    BluetoothAdapter mbluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

        }catch (Exception e){
            e.printStackTrace();
        }
        openButton = (Button) findViewById(R.id.open);
        sendButton = (Button) findViewById(R.id.send);
        closeButton = (Button) findViewById(R.id.close);

        myLabel = (TextView) findViewById(R.id.label);
        myTextbox = (EditText) findViewById(R.id.entry);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    closeBT();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendData();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    findBT();
                    openBT();
                }catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

             void openBT() throws IOException{
                 try {

                     // Standard SerialPortService ID
                     UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                     mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
                     mmSocket.connect();
                     mmOutputStream = mmSocket.getOutputStream();
                     mmInputStream = mmSocket.getInputStream();

                     beginListenForData();

                     myLabel.setText("Bluetooth Opened");

                 } catch (Exception e) {
                     e.printStackTrace();
                 }

            }

            void findBT() {
                try {
                    mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mbluetoothAdapter == null){
                        myLabel.setText("No Bluetooth adapter available");
                    }
                    if (!mbluetoothAdapter.isEnabled()){
                        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBluetooth , 0);
                    }
                    Set<BluetoothDevice> pairedDevices = mbluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0 ) {
                        for (BluetoothDevice device : pairedDevices){
                            if (device.getName().equals("PP801")){
                                mmDevice = device;
                                break;
                        }
                    }

                }
                myLabel.setText("Bluetooth diviece found.");
            }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

    }

    void closeBT() throws IOException{
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            myLabel.setText("Bluetooth Closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void sendData() throws IOException{
        try {

            // the text typed by the user
            String msg = myTextbox.getText().toString();
            msg += "\n";

            mmOutputStream.write(msg.getBytes());

            // tell the user data were sent
            myLabel.setText("Data sent.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void beginListenForData() {
         try {
             final Handler handler = new Handler();

             // this is the ASCII code for a newline character
             final byte delimiter = 10;

             stopWorker = false;
             readBufferPosition = 0;
             readBuffer = new byte[1024];

             workerThread = new Thread(new Runnable() {
                 public void run() {

                     while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                         try {

                             int bytesAvailable = mmInputStream.available();

                             if (bytesAvailable > 0) {

                                 byte[] packetBytes = new byte[bytesAvailable];
                                 mmInputStream.read(packetBytes);

                                 for (int i = 0; i < bytesAvailable; i++) {

                                     byte b = packetBytes[i];
                                     if (b == delimiter) {

                                         byte[] encodedBytes = new byte[readBufferPosition];
                                         System.arraycopy(
                                                 readBuffer, 0,
                                                 encodedBytes, 0,
                                                 encodedBytes.length
                                         );

                                         // specify US-ASCII encoding
                                         final String data = new String(encodedBytes, "US-ASCII");
                                         readBufferPosition = 0;

                                         // tell the user data were sent to bluetooth printer device
                                         handler.post(new Runnable() {
                                             public void run() {
                                                 myLabel.setText(data);
                                             }
                                         });

                                     } else {
                                         readBuffer[readBufferPosition++] = b;
                                     }
                                 }
                             }

                         } catch (IOException ex) {
                             stopWorker = true;
                         }

                     }
                 }
             });

             workerThread.start();

         } catch (Exception e) {
             e.printStackTrace();
         }
    }
}
