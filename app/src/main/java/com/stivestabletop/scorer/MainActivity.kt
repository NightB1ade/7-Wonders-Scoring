package com.stivestabletop.scorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

const val PLAYER_MESSAGE = "com.example.test.PLAYERS"
const val PLAYER_LIST = "playerlist"

// Simple class for storing strings between config changes!
// For example - screen rotation (config change) can cause the main activity to be re-built
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

        val view = findViewById<LinearLayout>(R.id.rootLayout)
        val list = supportFragmentManager.findFragmentByTag(PLAYER_LIST)

        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.rootLayout, ListFragment.newInstance(), PLAYER_LIST)
                .commit()
        } else {
            // Re-use store of players to re-create view
            if (list is ListFragment)
                for (message in list.strings)
                    view?.addView(getText(message))
        }
        if (list is ListFragment) {
            setPlayer(list.strings.size)
        } else {
            setPlayer(0)
        }
    }

    fun setPlayer(players: Int) {
        val editText = findViewById<EditText>(R.id.editPlayer)
        val num = players + 1
        editText.setText("player " + num.toString())
        editText.selectAll()
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

        val list = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (list is ListFragment) {
            list.strings.add(message)
            setPlayer(list.strings.size)
        }
        // add new text to layout
        view?.addView(getText(message))

        // Enable done button
        val doneButton = findViewById<Button>(R.id.buttonDone)
        doneButton.isEnabled = true
    }

    fun donePlayers(view: View) {
        val list = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (list is ListFragment) {
            val message = ArrayList(list.strings)
            val intent = Intent(this, ScoreActivity::class.java).apply {
                putExtra(PLAYER_MESSAGE, message)
            }
            startActivity(intent)
        }
    }
}
