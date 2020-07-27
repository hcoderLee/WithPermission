package com.lee.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lee.withpermission.PermissionManager

class MainActivity() : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // some code need some permissions and need to be initialized here
        ContactSDK.readContacts(this)
        LocationSDK.getLocation(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
}