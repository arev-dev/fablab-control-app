package com.example.thefablabcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle


//aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
//aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
const val REQUEST_ENABLE_BT = 1
//bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

class MainActivity : AppCompatActivity() {
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null
    var isBluetoothPermissed = false
    var isLocationPermissed = false

    companion object {
        const val MY_PERMISSIONS_REQUEST_BLUETOOTH = 123
        const val MY_PERMISSIONS_REQUEST_LOCATION = 124
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private var m_bluetoothSocket: BluetoothSocket? = null

        var m_isConnected: Boolean = false
        lateinit var m_address: String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        disableButtons()
        // Checar y solicitar permisos
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Solicitar permisos al usuario
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                MY_PERMISSIONS_REQUEST_BLUETOOTH
            )
        } else {
            // Los permisos ya están concedidos
            isBluetoothPermissed = true
            isLocationPermissed = true
            enableButtons()
        }

        if (!isBluetoothPermissed || !isLocationPermissed)
        {
            Toast.makeText(this, "Debes conceder los permisos para usar la app", Toast.LENGTH_LONG).show()
        }

        mAddressDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        mNameDevices = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        val idBtnOnBT = findViewById<Button>(R.id.btnBlueOn)
        val idBtnOffBT = findViewById<Button>(R.id.btnBlueDes)
        val idBtnConect = findViewById<Button>(R.id.btnBlueConnect)
        val idBtnEnviar = findViewById<Button>(R.id.btnSendM)
        val idBtnA = findViewById<Button>(R.id.btnBlueA)
        val idBtnB = findViewById<Button>(R.id.btnBlueB)

        val idBtnDispBT = findViewById<Button>(R.id.btnBlueDevices)
        val idSpinDisp = findViewById<Spinner>(R.id.spBlueDevices)
        val idTextOut = findViewById<EditText>(R.id.txtSend)

        val someActivityResultLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == REQUEST_ENABLE_BT) {
                Log.i("MainActivity", "ACTIVIDAD REGISTRADA")
            }
        }
        //Inicializacion del bluetooth adapter
        mBtAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

        //Checar si esta encendido o apagado
        /*if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth no está encendido en este dipositivo", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Bluetooth está encendido en este dispositivo", Toast.LENGTH_LONG).show()
        }*/


        idBtnOnBT.setOnClickListener {
            if (mBtAdapter.isEnabled) {
                //Si ya está activado
                Toast.makeText(this, "Bluetooth ya se encuentra activado", Toast.LENGTH_LONG).show()
            } else {
                //Encender Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("MainActivity", "ActivityCompat#requestPermissions")
                }
                someActivityResultLauncher.launch(enableBtIntent)
            }
        }

        //Boton apagar bluetooth
        idBtnOffBT.setOnClickListener {
            if (!mBtAdapter.isEnabled) {
                //Si ya está desactivado
                Toast.makeText(this, "Bluetooth ya se encuentra desactivado", Toast.LENGTH_LONG).show()
            } else {
                //Encender Bluetooth
                mBtAdapter.disable()
                Toast.makeText(this, "Se ha desactivado el bluetooth", Toast.LENGTH_LONG).show()
            }
        }

        //Boton dispositivos emparejados
        idBtnDispBT.setOnClickListener {
            if (mBtAdapter.isEnabled) {

                val pairedDevices: Set<BluetoothDevice>? = mBtAdapter?.bondedDevices
                mAddressDevices!!.clear()
                mNameDevices!!.clear()

                pairedDevices?.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    mAddressDevices!!.add(deviceHardwareAddress)
                    //........... EN ESTE PUNTO GUARDO LOS NOMBRE A MOSTRARSE EN EL COMBO BOX
                    mNameDevices!!.add(deviceName)
                }

                //ACTUALIZO LOS DISPOSITIVOS
                idSpinDisp.setAdapter(mNameDevices)
            } else {
                val noDevices = "Ningun dispositivo pudo ser emparejado"
                mAddressDevices!!.add(noDevices)
                mNameDevices!!.add(noDevices)
                Toast.makeText(this, "Primero vincule un dispositivo bluetooth", Toast.LENGTH_LONG).show()
            }
        }


        idBtnConect.setOnClickListener {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    Toast.makeText(this,"Conectando...",Toast.LENGTH_LONG).show()
                    val IntValSpin = idSpinDisp.selectedItemPosition
                    m_address = mAddressDevices!!.getItem(IntValSpin).toString()
                    //Toast.makeText(this,m_address,Toast.LENGTH_LONG).show()
                    // Cancel discovery because it otherwise slows down the connection.
                    try{
                        mBtAdapter?.cancelDiscovery()
                        val device: BluetoothDevice = mBtAdapter.getRemoteDevice(m_address)
                        m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                        m_bluetoothSocket!!.connect()
                    }catch(e: IOException)
                    {
                        Toast.makeText(this,"error"+e.toString(),Toast.LENGTH_LONG).show()
                        Log.i("MainActivity", "error"+e.toString())
                    }

                }
                Toast.makeText(this,"CONEXION EXITOSA",Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "CONEXION EXITOSA")

            } catch (e: IOException) {
                //connectSuccess = false
                e.printStackTrace()
                Toast.makeText(this,"ERROR DE CONEXION",Toast.LENGTH_LONG).show()
                Log.i("MainActivity", "ERROR DE CONEXION")
            }
        }

        idBtnA.setOnClickListener {
            sendCommand("a")
        }

        idBtnB.setOnClickListener {
            sendCommand("b")
        }

        idBtnEnviar.setOnClickListener {

            if(idTextOut.text.toString().isEmpty()){
                Toast.makeText(this, "El campo no puede estar vacío", Toast.LENGTH_SHORT)
            }else{
                var mensaje_out: String = idTextOut.text.toString()
                sendCommand(mensaje_out)
            }
        }


    }


    // Función para verificar permisos de Bluetooth
    private fun checkBluetoothPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Función para solicitar permisos de Bluetooth
    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            MY_PERMISSIONS_REQUEST_BLUETOOTH
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_BLUETOOTH -> {
                // Si la solicitud es cancelada, el array de resultados estará vacío
                isBluetoothPermissed = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                enableButtons()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun sendCommand(input: String) {
        if (m_bluetoothSocket != null) {
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch(e: IOException) {
                e.printStackTrace()
            }
        }
        else{
            Toast.makeText(this, "Debes conectarte a un dispositivo primero", Toast.LENGTH_LONG).show()
        }
    }

    private fun enableButtons() {
        //val idBtnOnBT = findViewById<Button>(R.id.btnBlueOn)
        val idBtnOffBT = findViewById<Button>(R.id.btnBlueDes)
        val idBtnConect = findViewById<Button>(R.id.btnBlueConnect)
        val idBtnEnviar = findViewById<Button>(R.id.btnSendM)
        val idBtnA = findViewById<Button>(R.id.btnBlueA)
        val idBtnB = findViewById<Button>(R.id.btnBlueB)

        // Habilitar/deshabilitar botones según el estado de los permisos
        //idBtnOnBT.isEnabled = isBluetoothPermissed
        idBtnOffBT.isEnabled = isBluetoothPermissed
        idBtnConect.isEnabled = isBluetoothPermissed
        idBtnEnviar.isEnabled = isBluetoothPermissed
        idBtnA.isEnabled = isBluetoothPermissed
        idBtnB.isEnabled = isBluetoothPermissed
    }

    private fun disableButtons() {
        //val idBtnOnBT = findViewById<Button>(R.id.btnBlueOn)
        val idBtnOffBT = findViewById<Button>(R.id.btnBlueDes)
        val idBtnConect = findViewById<Button>(R.id.btnBlueConnect)
        val idBtnEnviar = findViewById<Button>(R.id.btnSendM)
        val idBtnA = findViewById<Button>(R.id.btnBlueA)
        val idBtnB = findViewById<Button>(R.id.btnBlueB)

        // Habilitar/deshabilitar botones según el estado de los permisos
        //idBtnOnBT.isEnabled = isBluetoothPermissed
        idBtnOffBT.isEnabled = false
        idBtnConect.isEnabled = false
        idBtnEnviar.isEnabled = false
        idBtnA.isEnabled = false
        idBtnB.isEnabled = false
    }

}