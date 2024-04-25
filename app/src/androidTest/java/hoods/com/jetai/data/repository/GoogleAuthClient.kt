package hoods.com.jetai.data.repository

import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import hoods.com.jetai.test.BuildConfig
import hoods.com.jetai.utils.Response
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class GoogleAuthClient
    (private  val oneTapClient: SignInClient )

{
    companion object
    {
        const val  TAG = "google_auth"
    }

    suspend fun SignIn() : IntentSender?{
       var result = try {
        oneTapClient.beginSignIn(buildSignInRequest()).await()
       }
       catch (e: Exception)
       {
           e.printStackTrace()
           if(e is CancellationException) throw  e
           null
       }
        return result?.pendingIntent?.intentSender
    }

 fun signInWithIntent(intent: Intent)
   :Flow<Response<AuthResult?>>  =  callbackFlow{
    val credentials = oneTapClient.getSignInCredentialFromIntent(intent)
    val googleIdToken  = credentials.googleIdToken
       val googleCredential = GoogleAuthProvider.getCredential(
           googleIdToken, null
       )


        try {
            Firebase.auth.signInWithCredential(googleCredential)
                .addOnCompleteListener {
                    if(it.isSuccessful)
                    {
                        trySend(Response.Success(it.result))
                    }
                    else
                    {
                        trySend(Response.Error(it.exception))
                    }
                }
        }catch (e : Exception)
        {
        e.printStackTrace()
        }

     awaitClose {  }
    }

    private fun buildSignInRequest(): BeginSignInRequest
    {
        return BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(System.getProperty("clientId"))
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()
                }

}