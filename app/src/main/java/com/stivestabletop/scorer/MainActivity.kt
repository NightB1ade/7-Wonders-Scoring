package com.stivestabletop.scorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

const val PLAYER_MESSAGE = "com.example.test.PLAYERS"

// Stupid thing for storing strings between config changes!
class ListFragment : Fragment() {
    var strings = mutableListOf<String>()

    companion object {
        fun newInstance(): ListFragment {
            return ListFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Will get saved between config changes!
        retainInstance = true
    }
}


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.rootLayout, ListFragment.newInstance(), "players")
                .commit()
        } else {
            // Re-use store of stuff to re-create view
            val view = findViewById<LinearLayout>(R.id.rootLayout)
            val list = supportFragmentManager.findFragmentByTag("players")
            if (list is ListFragment)
                for (message in list.strings)
                    view?.addView(getText(message))
        }
    }

    // Simple textview box
    fun getText(string: String): TextView {
        val tv_dynamic = TextView(this)
        tv_dynamic.textSize = 20f
        tv_dynamic.text = string
        return tv_dynamic
    }

    fun addPlayer(view: View) {
        val editText = findViewById<EditText>(R.id.editPlayer)
        val message = editText.text.toString()

        val view = findViewById<LinearLayout>(R.id.rootLayout)

        val list = supportFragmentManager.findFragmentByTag("players")
        if (list is ListFragment)
            list.strings.add(message)

        // add new text to layout
        view?.addView(getText(message))
    }

    fun donePlayers(view: View) {
        val intent = Intent(this, ScoreActivity::class.java).apply {
            putExtra(PLAYER_MESSAGE, "players")
        }
        startActivity(intent)
    }
}
