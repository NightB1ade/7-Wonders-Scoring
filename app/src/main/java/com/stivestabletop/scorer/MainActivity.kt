package com.stivestabletop.scorer

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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

        // Read in games info
        val xmlconfig = resources.openRawResource(R.raw.games)
        val gamesconfig = GamesParser(xmlconfig)

        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.rootLayout, PlayerDataFragment.newInstance(), PLAYER_LIST)
                .commit()
        }
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)

        // Set up player list from any saved fragment
        if (savedInstanceState != null) {
            // Re-use store of players to re-create view
            val view = findViewById<LinearLayout>(R.id.rootLayout)
            if (data is PlayerDataFragment)
                for ((idx, name) in data.playernames.withIndex())
                    view?.addView(getPlayerText(name, data.playertypes[idx]))
        }

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

        // Set up games spinner
        val view = findViewById<Spinner>(R.id.spinnerGame)
        val adapter = ArrayAdapter(
            this, R.layout.support_simple_spinner_dropdown_item,
            gamesconfig.getGamesList()
        )
        view.adapter = adapter
        if (data is PlayerDataFragment && !data.gamename.isBlank()) {
            // Set up chosen game
            view.setSelection(adapter.getPosition(data.gamename))
            setupPlayerSpinner(applicationContext, gamesconfig.getPlayersList(data.gamename))
        }

        // Set up listener for game change
        view.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                v: View?,
                pos: Int,
                id: Long
            ) {
                val gamename = parent.getItemAtPosition(pos).toString()
                val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
                if (data is PlayerDataFragment) {
                    if (data.gamename.isBlank()) {
                        // Initialization
                        data.gamename = gamename
                        setupPlayerSpinner(parent.context, gamesconfig.getPlayersList(gamename))
                    } else {
                        if (data.gamename != gamename) {
                            if (data.playernames.size > 0) {
                                val builder = AlertDialog.Builder(parent.context)
                                builder.apply {
                                    setPositiveButton(R.string.ok,
                                        DialogInterface.OnClickListener { dialog, id ->
                                            data.gamename = gamename
                                            clearPlayers()
                                            setupPlayerSpinner(
                                                parent.context,
                                                gamesconfig.getPlayersList(gamename)
                                            )
                                        })
                                    setNegativeButton(R.string.cancel,
                                        DialogInterface.OnClickListener { dialog, id ->
                                            // Reset selection
                                            view.setSelection(adapter.getPosition(data.gamename))
                                        })
                                    setMessage(R.string.game_change)
                                }
                                // Create the AlertDialog
                                builder.create()
                                builder.show()
                            } else {
                                data.gamename = gamename
                                setupPlayerSpinner(
                                    parent.context,
                                    gamesconfig.getPlayersList(gamename)
                                )
                            }
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }
        }

        // Set the next player text based on the number of players so far
        var numPlayers = 0
        if (data is PlayerDataFragment)
            numPlayers = data.playernames.size
        setNextPlayer(numPlayers)
        enableDone()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        // Not sure why this is an error in IDE but it compiles and works!
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_clear -> {
                clearPlayers()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Set up the player type list
    fun setupPlayerSpinner(context: Context, list: List<String>) {
        // Set up players spinner
        val view = findViewById<Spinner>(R.id.spinnerPlayer)
        val adapter = ArrayAdapter(
            context,
            R.layout.support_simple_spinner_dropdown_item,
            list
        )
        view.adapter = adapter
    }

    // Clear all the listed players
    fun clearPlayers() {
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            data.playernames = mutableListOf<String>()
            data.playertypes = mutableListOf<String>()
            setNextPlayer(0)
            enableDone()
            val view = findViewById<LinearLayout>(R.id.rootLayout)
            view.removeAllViewsInLayout()
        }
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

    // Get a simple player name/type textview box
    fun getPlayerText(name: String, type: String): TextView {
        val text = if (type.isBlank()) name else "$name is $type"
        val tv_dynamic = TextView(this)
        tv_dynamic.textSize = 20f
        tv_dynamic.text = text
        return tv_dynamic
    }

    // Enable done button if we have at least one player
    fun enableDone() {
        val doneButton = findViewById<Button>(R.id.buttonDone)
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            if (data.playernames.size > 0) {
                doneButton.isEnabled = true
                return
            }
        }
        doneButton.isEnabled = false
    }

    // Add a new player to the list
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
        // add player to layout
        view?.addView(getPlayerText(playername, playertype))
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
