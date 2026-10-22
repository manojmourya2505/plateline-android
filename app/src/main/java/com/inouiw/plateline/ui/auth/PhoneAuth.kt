package com.inouiw.plateline.ui.auth

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

/** Firebase phone (OTP) sign-in for Indian mobile numbers (+91). */
object PhoneAuth {
    private var verificationId: String? = null
    private var sentTo: String = ""

    val displayPhone: String
        get() {
            val d = sentTo.removePrefix("+91")
            return if (d.length == 10) "+91 ${d.take(5)} ${d.drop(5)}" else sentTo
        }

    fun send(activity: Activity, phone: String, onCodeSent: () -> Unit, onError: (String) -> Unit) {
        sentTo = phone
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    onError(e.localizedMessage ?: "Could not send the code")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    onCodeSent()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verify(code: String, onDone: () -> Unit, onError: (String) -> Unit) {
        val id = verificationId ?: return onError("Ask for a new code first")
        FirebaseAuth.getInstance().signInWithCredential(PhoneAuthProvider.getCredential(id, code))
            .addOnSuccessListener { onDone() }
            .addOnFailureListener { onError("That code is not right. Try again.") }
    }
}
