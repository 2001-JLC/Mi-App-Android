package com.example.asb.auth

import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// Clase encargada de manejar la autenticación biométrica (huella/rostro)
class BiometricHelper(private val activity: FragmentActivity) {

    /**
     * Muestra el diálogo de autenticación biométrica
     * @param title Título del diálogo (opcional, por defecto "Iniciar con huella")
     * @param onSuccess Callback que se ejecuta cuando la autenticación es exitosa
     * @param onError Callback que se ejecuta cuando falla la autenticación (recibe mensaje de error)
     */
    fun showPrompt(
        title: String = "Iniciar con huella",  // Título personalizable
        onSuccess: () -> Unit,                 // Función a ejecutar al autenticar correctamente
        onError: (String) -> Unit              // Función que maneja errores (recibe String)
    ) {
        // Crea un BiometricPrompt configurado con:
        // 1. La actividad que lo llama (FragmentActivity)
        // 2. Un Executor para manejar callbacks en el hilo principal
        // 3. Un AuthenticationCallback para responder a eventos
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),  // Ejecuta callbacks en el hilo UI
            object : BiometricPrompt.AuthenticationCallback() {
                // Se llama cuando el usuario se autentica exitosamente
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()  // Ejecuta la función de éxito proporcionada
                }

                // Se llama cuando hay un error en la autenticación
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError("Error: $errString")  // Notifica el error al callback
                }
            })

        // Configura y muestra el diálogo biométrico
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)                     // Establece el título del diálogo
                .setNegativeButtonText("Usar contraseña")  // Texto para fallback manual
                .build()                            // Construye la configuración final
        )
    }
}