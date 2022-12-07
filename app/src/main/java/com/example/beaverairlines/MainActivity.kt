package com.example.beaverairlines

import android.app.Dialog
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.diaolog_progress.*

class MainActivity : AppCompatActivity() {

    private lateinit var mProgressDialog : Dialog


    //KEYBOARD BEHAVIOUR
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //REMOVING ACTION BAR:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            )
        }
    }


    //SHOWING PROGRESS DIALOG. NOT IN USE.
    fun showProgressDialog(text : String) {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.diaolog_progress)
        mProgressDialog.show()
    }

    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }



    //METHOD TO SHOW TOAST MSG. NOT IN USE.
    fun showErrorSnackBar(message :String) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message,
            Snackbar.LENGTH_LONG)

        val snackBarView = snackbar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.red_700))

        snackbar.show()
    }
}