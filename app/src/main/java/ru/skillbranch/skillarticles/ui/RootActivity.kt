package ru.skillbranch.skillarticles.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toolbar
import kotlinx.android.synthetic.main.activity_root.*
import ru.skillbranch.skillarticles.R

class RootActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_root)
        setupToolbar()

    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val logo = if (toolbar.childCount > 2) {
            toolbar.getChildAt(2) as ImageView
        } else {
            null
        }
        val lp = logo?.layoutParams as Toolbar.LayoutParams
        //lp?
    }


}