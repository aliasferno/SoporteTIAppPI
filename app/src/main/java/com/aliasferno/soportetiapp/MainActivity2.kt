package com.aliasferno.soportetiapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class ProviderType {
    BASIC,
    GOOGLE
}
class MainActivity2 : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        credentialManager = CredentialManager.create(baseContext)

        //setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")

        setup(email?:"", provider?:"")

        //Guardado de datos

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()
    }

    private fun setup(email:String, provider:String){
        title = "Inicio"

        val lblEmail : TextView = findViewById(R.id.lblUsuarioLoggeado)
        val lblProvider : TextView = findViewById(R.id.lblProvider)
        val btnLogout : Button = findViewById(R.id.btnSalirLogeado)
        lblEmail.text = email
        lblProvider.text = provider

        btnLogout.setOnClickListener {

            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            credentialManager = CredentialManager.create(baseContext)
            prefs.clear()
            prefs.apply()
            auth.signOut()

            lifecycleScope.launch {
                try {
                    val clearRequest = ClearCredentialStateRequest()
                    credentialManager.clearCredentialState(clearRequest)
                    FirebaseAuth.getInstance().signOut()
                } catch (e: ClearCredentialException) {
                    Log.e("TAG", "Couldn't clear user credentials: ${e.localizedMessage}")
                }
            }
            onBackPressed()
        }


    }
}