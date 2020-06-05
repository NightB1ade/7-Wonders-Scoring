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
    var gameName = ""
    var playerNames = mutableListOf<String>()
    var playerTypes = mutableListOf<String>()

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
        val xmlConfig = resources.openRawResource(R.raw.games)
        val gamesConfig = GamesParser(xmlConfig)

        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.playersLayout, PlayerDataFragment.newInstance(), PLAYER_LIST)
                .commit()
        }
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)

        // Set up player name/type list from any saved fragment
        if (savedInstanceState != null) {
            // Re-use store of players to re-create view
            val view = findViewById<LinearLayout>(R.id.playersLayout)
            if (data is PlayerDataFragment)
                for ((idx, name) in data.playerNames.withIndex())
                    view?.addView(getPlayerText(name, data.playerTypes[idx]))
        }

        // Add listener to perform add player on screen-keyboard enter
        findViewById<AutoCompleteTextView>(R.id.autoPlayer).setOnEditorActionListener { view, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_NEXT -> {
                    addPlayer(view)
                    true
                }
                else -> false
            }
        }

        // Set up games spinner
        val gameSpinner = findViewById<Spinner>(R.id.spinnerGame)
        val adapter = ArrayAdapter(
            this, R.layout.support_simple_spinner_dropdown_item,
            gamesConfig.getGamesList()
        )
        gameSpinner.adapter = adapter
        if (data is PlayerDataFragment && data.gameName.isNotBlank()) {
            // Set up previously chosen game
            gameSpinner.setSelection(adapter.getPosition(data.gameName))
            setupPlayerTypes(
                applicationContext,
                gamesConfig.getPlayersList(data.gameName),
                data.playerTypes
            )
        }

        // Add listener to always show player type drop down
        findViewById<AutoCompleteTextView>(R.id.autoPlayer).setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus && view is AutoCompleteTextView) {
                @Suppress("NAME_SHADOWING") val data =
                    supportFragmentManager.findFragmentByTag(PLAYER_LIST)
                if (data is PlayerDataFragment)
                    setupPlayerTypes(
                        applicationContext,
                        gamesConfig.getPlayersList(data.gameName),
                        data.playerTypes
                    )
                view.showDropDown()
            }
        }

        // Set up listener for game change
        gameSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                v: View?,
                pos: Int,
                id: Long
            ) {
                val gamename = parent.getItemAtPosition(pos).toString()
                @Suppress("NAME_SHADOWING") val data =
                    supportFragmentManager.findFragmentByTag(PLAYER_LIST)
                if (data is PlayerDataFragment) {
                    if (data.gameName.isBlank()) {
                        // Initialization
                        data.gameName = gamename
                        setupPlayerTypes(parent.context, gamesConfig.getPlayersList(gamename))
                    } else {
                        if (data.gameName != gamename) {
                            if (data.playerNames.size > 0) {
                                val builder = AlertDialog.Builder(parent.context)
                                builder.apply {
                                    setPositiveButton(
                                        android.R.string.ok,
                                        DialogInterface.OnClickListener { _, _ ->
                                            // Change game and clear all data
                                            data.gameName = gamename
                                            clearPlayers()
                                            setupPlayerTypes(
                                                parent.context,
                                                gamesConfig.getPlayersList(gamename)
                                            )
                                        })
                                    setNegativeButton(
                                        android.R.string.cancel,
                                        DialogInterface.OnClickListener { _, _ ->
                                            // Reset selection
                                            gameSpinner.setSelection(adapter.getPosition(data.gameName))
                                        })
                                    setMessage(R.string.game_change)
                                }
                                // Create the AlertDialog
                                builder.create()
                                builder.show()
                            } else {
                                data.gameName = gamename
                                setupPlayerTypes(
                                    parent.context,
                                    gamesConfig.getPlayersList(gamename),
                                    data.playerTypes
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
            numPlayers = data.playerNames.size
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

    // Set up the player type list, only show those not yet selected
    fun setupPlayerTypes(
        context: Context,
        list: List<String>,
        selected: List<String> = emptyList()
    ) {
        val view = findViewById<AutoCompleteTextView>(R.id.autoPlayer)
        view.completionHint = list[0]
        view.hint = list[0]
        val show = list.filterNot { it in selected }
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_dropdown_item_1line,
            show.subList(1, show.lastIndex + 1)
        )
        view.setAdapter(adapter)
    }

    // Clear all the listed players
    fun clearPlayers() {
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            data.playerNames = mutableListOf<String>()
            data.playerTypes = mutableListOf<String>()
            setNextPlayer(0)
            enableDone()
            val view = findViewById<LinearLayout>(R.id.playersLayout)
            view.removeAllViewsInLayout()
        }
    }

    // Set up the next player text based on the number of players so far
    private fun setNextPlayer(players: Int) {
        val autoText = findViewById<AutoCompleteTextView>(R.id.autoPlayer)
        autoText.setText("")
        val editText = findViewById<EditText>(R.id.editPlayer)
        val num = players + 1
        editText.setText(getString(R.string.default_player_string, num))
        // Player name view will select all on focus, but if already focused we need to re-select
        if (editText.hasFocus()) editText.selectAll()
    }

    // Get a simple player name/type textview box
    private fun getPlayerText(name: String, type: String): TextView {
        val text = if (type.isBlank()) name else "$name is $type"
        val tv = TextView(this)
        tv.textSize = 20f
        tv.text = text
        return tv
    }

    // Enable done button if we have at least one player
    private fun enableDone() {
        val doneButton = findViewById<Button>(R.id.buttonDone)
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            if (data.playerNames.size > 0) {
                doneButton.isEnabled = true
                return
            }
        }
        doneButton.isEnabled = false
    }

    // Add a new player to the list
    fun addPlayer(view: View) {
        val editText = findViewById<EditText>(R.id.editPlayer)
        val playerName = editText.text.toString()
        val autoText: AutoCompleteTextView = if (view is AutoCompleteTextView) {
            view
        } else {
            findViewById(R.id.autoPlayer)
        }

        var playerType = autoText.text.toString()
        // Check for no selected type
        if (playerType.endsWith("..."))
            playerType = ""

        val layout = findViewById<LinearLayout>(R.id.playersLayout)

        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            data.playerNames.add(playerName)
            data.playerTypes.add(playerType)
            setNextPlayer(data.playerNames.size)
        }
        // Add player to layout
        layout?.addView(getPlayerText(playerName, playerType))
        // Enable done if we have enough players
        this.enableDone()
        // Move to next player name, otherwise focus stays on player type
        if (autoText.hasFocus()) editText.requestFocus()
    }

    // Finished entering players - move onto scoring
    @Suppress("UNUSED_PARAMETER")
    fun donePlayers(v: View) {
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            val args = Bundle()
            args.putStringArrayList(PLAYER_NAMES, ArrayList(data.playerNames))
            args.putStringArrayList(PLAYER_TYPES, ArrayList(data.playerTypes))
            args.putString(GAME_NAME, data.gameName)
            val intent = Intent(this, ScoreActivity::class.java).apply {
                putExtras(args)
            }
            // FIX - for soft keyboard display issues on return from score activity
            // Remove any current focus on this activity
            currentFocus?.clearFocus()
            startActivity(intent)
        }
    }
}
