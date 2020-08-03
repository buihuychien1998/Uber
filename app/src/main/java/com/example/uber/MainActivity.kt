package com.example.uber

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.btnDriver->{
                startActivity(Intent(this, DriverLoginActivity::class.java))
                finish()
            }
            R.id.btnCustomer->{
                startActivity(Intent(this, CustomerLoginActivity::class.java))
                finish()
            }

        }
    }
}