package com.example.rfid

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rscja.deviceapi.RFIDWithUHFUART
import java.io.File

class MainActivity : AppCompatActivity() {

    private var mReader: RFIDWithUHFUART? = null
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1
    private lateinit var progressDialog: ProgressDialog
    private lateinit var btnRead: Button
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRead = findViewById(R.id.btnRead)
        txtResult = findViewById(R.id.txtResult)

        // Check and request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        } else {
            initUHF()
        }

        btnRead.setOnClickListener {
            readTag()
        }
    }

    private fun initUHF() {
        try {
            mReader = RFIDWithUHFUART.getInstance()
            if (mReader != null) {
                InitTask().execute()
            } else {
                toastMessage("UHF reader instance is null")
            }
        } catch (ex: Exception) {
            toastMessage(ex.message ?: "Error initializing UHF reader")
            Log.e("UHF_Init", "Exception: ${ex.message}", ex)
        }
    }

    private fun readTag() {
        if (mReader != null) {
            try {
                val epc = mReader?.readData("00000000", 2, 0, 6)  // Read EPC data, bank 1, word offset 2
                txtResult.text = epc ?: "No tag read"
            } catch (ex: Exception) {
                toastMessage(ex.message ?: "Error reading tag")
                Log.e("UHF_Read", "Exception: ${ex.message}", ex)
            }
        } else {
            toastMessage("UHF reader not initialized")
        }
    }

    private fun toastMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initUHF()
            } else {
                toastMessage("Permission denied to read external storage")
            }
        }
    }

    private inner class InitTask : AsyncTask<Void, Void, Boolean>() {
        override fun onPreExecute() {
            progressDialog = ProgressDialog(this@MainActivity)
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog.setMessage("Initializing UHF reader...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            val configPath = "/storage/emulated/0/PDAConfig.txt"
            if (!File(configPath).exists()) {
                Log.e("UHF_Init", "Configuration file not found: $configPath")
                return false
            }
            val success = mReader?.init() ?: false
            if (!success) {
                Log.e("UHF_Init", "Failed to initialize UHF reader")
            }
            return success
        }

        override fun onPostExecute(result: Boolean) {
            progressDialog.dismiss()
            if (!result) {
                toastMessage("Failed to initialize UHF reader")
            }
        }
    }
}
