package com.stivestabletop.scorer

import android.app.Activity
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
import android.view.inputmethod.InputMethodManager
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
                .add(R.id.playersLayout, PlayerDataFragment.newInstance(), PLAYER_LIST)
                .commit()
        }
        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)

        // Set up player name/type list from any saved fragment
        if (savedInstanceState != null) {
            // Re-use store of players to re-create view
            val view = findViewById<LinearLayout>(R.id.playersLayout)
            if (data is PlayerDataFragment)
                for ((idx, name) in data.playernames.withIndex())
                    view?.addView(getPlayerText(name, data.playertypes[idx]))
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
            gamesconfig.getGamesList()
        )
        gameSpinner.adapter = adapter
        if (data is PlayerDataFragment && data.gamename.isNotBlank()) {
            // Set up previously chosen game
            gameSpinner.setSelection(adapter.getPosition(data.gamename))
            setupPlayerTypes(
                applicationContext,
                gamesconfig.getPlayersList(data.gamename),
                data.playertypes
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
                        gamesconfig.getPlayersList(data.gamename),
                        data.playertypes
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
                    if (data.gamename.isBlank()) {
                        // Initialization
                        data.gamename = gamename
                        setupPlayerTypes(parent.context, gamesconfig.getPlayersList(gamename))
                    } else {
                        if (data.gamename != gamename) {
                            if (data.playernames.size > 0) {
                                val builder = AlertDialog.Builder(parent.context)
                                builder.apply {
                                    setPositiveButton(
                                        android.R.string.ok,
                                        DialogInterface.OnClickListener { _, _ ->
                                            // Change game and clear all data
                                            data.gamename = gamename
                                            clearPlayers()
                                            setupPlayerTypes(
                                                parent.context,
                                                gamesconfig.getPlayersList(gamename)
                                            )
                                        })
                                    setNegativeButton(
                                        android.R.string.cancel,
                                        DialogInterface.OnClickListener { _, _ ->
                                            // Reset selection
                                            gameSpinner.setSelection(adapter.getPosition(data.gamename))
                                        })
                                    setMessage(R.string.game_change)
                                }
                                // Create the AlertDialog
                                builder.create()
                                builder.show()
                            } else {
                                data.gamename = gamename
                                setupPlayerTypes(
                                    parent.context,
                                    gamesconfig.getPlayersList(gamename),
                                    data.playertypes
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

    // FIX: for keyboard not showing up when you either hit back from score activity or continue app
    // from background
    override fun onResume() {
        super.onResume()
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    // FIX: for keyboard not showing when the score activity finishes (rather than back)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )
        }
    }

    // FIX: Part of keyboard fix - see onResume
    override fun onPause() {
        super.onPause()
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
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
            data.playernames = mutableListOf<String>()
            data.playertypes = mutableListOf<String>()
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
        editText.requestFocus()
        editText.selectAll()
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
        val autoText: AutoCompleteTextView = if (view is AutoCompleteTextView) {
            view
        } else {
            findViewById(R.id.autoPlayer)
        }
        var playertype = autoText.text.toString()
        // Check for no selected type
        if (playertype.endsWith("..."))
            playertype = ""

        val layout = findViewById<LinearLayout>(R.id.playersLayout)

        val data = supportFragmentManager.findFragmentByTag(PLAYER_LIST)
        if (data is PlayerDataFragment) {
            data.playernames.add(playername)
            data.playertypes.add(playertype)
            setNextPlayer(data.playernames.size)
        }
        // add player to layout
        layout?.addView(getPlayerText(playername, playertype))
        // Enable done if we have enough players
        this.enableDone()
    }

    // Finished entering players - move onto scoring
    @Suppress("UNUSED_PARAMETER")
    fun donePlayers(v: View) {
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
