package com.stivestabletop.scorer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

const val PLAYER_NAMES = "playernames"
const val PLAYER_TYPES = "playertypes"
const val GAME_NAME = "gamename"
const val PLAYER_LIST = "playerlist"

// Simple class for storing strings between config changes!
// For example - screen rotation (config change) can cause the main activity to be re-built
// TODO - Expand to store player type (colour/city etc)
class PlayerDataFragment : Fragment() {
    var gamename = ""
    var playernames = mutableListOf<String>()
    var playertypes = mutableListOf<String>()

    companion object {
        fun newInstance(): PlayerDataFragment {
            return PlayerDataFragment()
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

        // Add listener to perform add player on screen-keyboard enter
        findViewById<EditText>(R.id.editPlayer).setOnEditorActionListener { v, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    addPlayer(v)
                    true
                }
                else -> false
            }
        }

        // Read in games info
        val xmlconfig = resources.openRawResource(R.raw.games)
        val gamesconfig = GamesParser(xmlconfig)

        // Set up games spinner
        val view = findViewById<Spinner>(R.id.spinnerGame)
        val adapter = ArrayAdapter(
            this, R.layout.support_simple_spinner_dropdown_item,
            gamesconfig.getGamesList()
        )
        view.adapter = adapter

        // Set up listener for game change
        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val gamename = parent.getItemAtPosition(position).toString()
                val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
                if (data is PlayerDataFragment)
                    if (data.gamename != gamename) {
                        data.gamename = gamename
                        // TODO: Clear out players chosen on change of game!
                    }
                // Set up players spinner
                val view = findViewById<Spinner>(R.id.spinnerPlayer)
                val adapter = ArrayAdapter(
                    parent.context,
                    R.layout.support_simple_spinner_dropdown_item,
                    gamesconfig.getPlayersList(gamename)
                )
                view.adapter = adapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }

        // Set up player list from any saved fragment
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.rootLayout, PlayerDataFragment.newInstance(), PLAYER_LIST)
                .commit()
        } else {
            // Re-use store of players to re-create view
            val view = findViewById<LinearLayout>(R.id.rootLayout)
            if (data is PlayerDataFragment)
                for (message in data.playernames)
                    view?.addView(getText(message))
        }
        // Set the next player text based on the number of players so far
        var numPlayers = 0
        if (data is PlayerDataFragment)
            numPlayers = data.playernames.size
        setNextPlayer(numPlayers)
        enableDone()
    }

    // Set up the next player text based on the number of players so far
    fun setNextPlayer(players: Int) {
        val editText = findViewById<EditText>(R.id.editPlayer)
        val num = players + 1
        editText.setText("player " + num.toString())
        editText.selectAll()
        val spinner = findViewById<Spinner>(R.id.spinnerPlayer)
        spinner.setSelection(0)
    }

    // Simple textview box
    fun getText(string: String): TextView {
        val tv_dynamic = TextView(this)
        tv_dynamic.textSize = 20f
        tv_dynamic.text = string
        return tv_dynamic
    }

    // Enable done button if we have at least one player
    fun enableDone() {
        val doneButton = findViewById<Button>(R.id.buttonDone)
        val list = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (list is PlayerDataFragment) {
            if (list.playernames.size > 0) {
                doneButton.isEnabled = true
                return
            }
        }
        doneButton.isEnabled = false
    }

    // Add a player to the list
    fun addPlayer(view: View) {
        val editText = findViewById<EditText>(R.id.editPlayer)
        val playername = editText.text.toString()
        val spinner = findViewById<Spinner>(R.id.spinnerPlayer)
        var playertype = spinner.selectedItem.toString()
        // Check for no selected type
        if (playertype.endsWith("..."))
            playertype = ""

        val view = findViewById<LinearLayout>(R.id.rootLayout)

        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            data.playernames.add(playername)
            data.playertypes.add(playertype)
            setNextPlayer(data.playernames.size)
        }
        // add new text to layout
        val text = if (playertype.isBlank()) playername else "$playername is $playertype"
        view?.addView(getText(text))
        // Enable done if we have enough players
        enableDone()
    }

    // Finished entering players - move onto scoring
    fun donePlayers(view: View) {
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            val args = Bundle()
            args.putStringArrayList(PLAYER_NAMES, ArrayList(data.playernames))
            args.putStringArrayList(PLAYER_TYPES, ArrayList(data.playertypes))
            args.putString(GAME_NAME, data.gamename)
            val intent = Intent(this, ScoreActivity::class.java).apply {
                putExtras(args)
            }
            startActivity(intent)
        }
    }
}
