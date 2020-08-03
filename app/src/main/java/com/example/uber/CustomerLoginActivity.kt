package com.example.uber

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_driver_login.*

class CustomerLoginActivity : BaseActivity(), View.OnClickListener {
    private var firebaseAuth: FirebaseAuth? = null
    private var firebaseAuthStateListener: FirebaseAuth.AuthStateListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_login)
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuthStateListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                startActivity(Intent(this, CustomerMapActivity::class.java))
                finish()
            }
        }
    }

    override fun onClick(p0: View?) {
        val email = edtEmail.text.toString()
        val password = edtPassword.text.toString()
        when(p0?.id){
            R.id.btnLogin->{
               firebaseAuth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener {
                   if (!it.isSuccessful){
                       Toast.makeText(this, "Sign In Error", Toast.LENGTH_SHORT).show()
                   }
               }

            }
            R.id.btnRegistration->{
                firebaseAuth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener {
                    if (it.isSuccessful){
                        val uid = firebaseAuth?.uid
                        val docData = hashMapOf(
                            uid to true)
                        val db = FirebaseFirestore.getInstance()
                        db.collection("Users").document("Customers").set(docData)

                    }else{
                        Toast.makeText(this, "Sign Up Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    override fun onStart() {
        super.onStart()
        firebaseAuthStateListener?.let { firebaseAuth?.addAuthStateListener(it) }
    }

    override fun onStop() {
        super.onStop()
        firebaseAuthStateListener?.let { firebaseAuth?.removeAuthStateListener(it) }
    }
}