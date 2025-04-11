package com.aliasferno.soportetiapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var tvEmail: TextView
    private lateinit var tvProvider: TextView
    private lateinit var tvEmailVerified: TextView
    private lateinit var etDisplayName: TextInputEditText
    private lateinit var spinnerUserRole: AutoCompleteTextView
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        tvEmail = view.findViewById(R.id.tvEmail)
        tvProvider = view.findViewById(R.id.tvProvider)
        tvEmailVerified = view.findViewById(R.id.tvEmailVerified)
        etDisplayName = view.findViewById(R.id.etDisplayName)
        spinnerUserRole = view.findViewById(R.id.spinnerUserRole)
        btnSaveProfile = view.findViewById(R.id.btnSaveProfile)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configurar el spinner de roles
        setupRoleSpinner()

        // Mostrar información del usuario
        displayUserInfo()

        // Configurar botones
        btnSaveProfile.setOnClickListener {
            updateProfile()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun setupRoleSpinner() {
        val roles = arrayOf("Operador", "Administrador")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        spinnerUserRole.setAdapter(adapter)
    }

    private fun displayUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            etDisplayName.setText(user.displayName ?: "")
            tvEmail.text = "Email: ${user.email}"
            tvProvider.text = "Proveedor: ${getProviderName(user)}"
            tvEmailVerified.text = "Email verificado: ${if (user.isEmailVerified) "Sí" else "No"}"

            // Mostrar/ocultar botón de cambio de contraseña según el proveedor
            btnChangePassword.visibility = if (isPasswordProvider(user)) View.VISIBLE else View.GONE

            // Obtener el rol del usuario desde Firestore
            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role") ?: "Operador"
                        spinnerUserRole.setText(role, false)
                    } else {
                        // Si no existe el documento, crear uno nuevo con rol por defecto
                        firestore.collection("users")
                            .document(user.uid)
                            .set(mapOf("role" to "Operador"))
                            .addOnSuccessListener {
                                spinnerUserRole.setText("Operador", false)
                            }
                    }
                }
        } else {
            // Si no hay usuario, volver a la pantalla de login
            navigateToLogin()
        }
    }

    private fun isPasswordProvider(user: FirebaseUser): Boolean {
        return user.providerData.any { it.providerId == "password" }
    }

    private fun getProviderName(user: FirebaseUser): String {
        return when {
            user.providerData.any { it.providerId == "google.com" } -> "Google"
            user.providerData.any { it.providerId == "password" } -> "Email/Contraseña"
            else -> "Desconocido"
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Cambiar Contraseña")
            .setView(dialogView)
            .setPositiveButton("Cambiar") { dialog, _ ->
                val currentPassword = etCurrentPassword.text?.toString() ?: ""
                val newPassword = etNewPassword.text?.toString() ?: ""
                val confirmPassword = etConfirmPassword.text?.toString() ?: ""

                if (newPassword == confirmPassword) {
                    changePassword(currentPassword, newPassword)
                } else {
                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            // Primero reautenticar al usuario
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Luego cambiar la contraseña
                        user.updatePassword(newPassword)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al actualizar la contraseña: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(context, "Contraseña actual incorrecta", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun updateProfile() {
        val user = auth.currentUser
        if (user != null) {
            val displayName = etDisplayName.text?.toString()?.trim() ?: ""
            val role = spinnerUserRole.text.toString()

            // Actualizar el perfil en Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Actualizar el rol en Firestore
                        firestore.collection("users")
                            .document(user.uid)
                            .set(mapOf("role" to role))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                                // Recargar la información del usuario
                                displayUserInfo()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error al actualizar el rol: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Error al actualizar el perfil: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun logout() {
        // Limpiar preferencias compartidas
        val prefs = requireActivity().getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()

        // Cerrar sesión de Firebase
        auth.signOut()

        // Navegar a MainActivity
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
} 