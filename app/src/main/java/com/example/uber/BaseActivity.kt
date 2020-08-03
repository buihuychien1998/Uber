package com.example.uber

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


abstract class BaseActivity: AppCompatActivity() {
    protected var TAG: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TAG = this.javaClass.simpleName
    }
}