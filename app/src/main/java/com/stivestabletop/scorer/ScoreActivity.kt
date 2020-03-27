package com.stivestabletop.scorer

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment

const val TRACKS_LIST = "trackslist"

@RequiresApi(Build.VERSION_CODES.O)
fun getContrastTextColour(bg: Int): Int {
    // Calculate the perceptive luminance (aka luma) - human eye favors green color...
    val luma: Double =
        ((0.299 * Color.red(bg)) + (0.587 * Color.green(bg)) + (0.114 * Color.blue(bg))) / 255
    return (if (luma > 0.5) Color.BLACK else Color.WHITE)
}

class TrackFragment : Fragment() {
    companion object {
        fun newInstance(
            players: Array<String>, name: String, colour: String,
            first: Boolean, total: Boolean
        ): TrackFragment {
            val f = TrackFragment()
            val args = Bundle()
            args.putStringArray("players", players)
            args.putString("name", name)
            args.putString("colour", colour)
            args.putBoolean("first", first) // First visible track to get focus
            args.putBoolean("total", total)
            f.setArguments(args)
            return f
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = arguments
        var players: Array<String> = arrayOf()
        var tname = ""
        var tcol = ""
        var first = false
        var total = false
        if (args != null) {
            players = args.getStringArray("players") as Array<String>
            tname = args.getString("name", "track")
            tcol = "#" + args.getString("colour", "FFFFFF")
            first = args.getBoolean("first", false)
            total = args.getBoolean("total", false)
        }
        // Create new track fragment
        val track = inflater.inflate(R.layout.fragment_line, container, false)
        if (total)
            track.id = R.id.total_track
        // Update the track name/colour
        val name = track.findViewById<Button>(R.id.trackName)
        name.text = tname
        val bgcol = Color.parseColor(tcol)
        name.setBackgroundColor(bgcol)
        name.setTextColor(getContrastTextColour(bgcol))
        name.setOnClickListener(View.OnClickListener { v ->
            // TODO: Do something more useful here... with special calcs
            if (v is Button)
                v.text = "Woot"
        })

        // Sort out score boxes
        val view = track.findViewById<LinearLayout>(R.id.scoreLayout)
        for (num in 1..players.size) {
            val sv = getScoreView(num, tname, players[num - 1], !total)
            view?.addView(sv)
            if (num == 1 && first)
                sv.requestFocus()
        }

        return track
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // TODO - do this for every hide?
        calculateScores()
    }

    fun getScoreEntry(): EditText {
        val view = EditText(requireContext())
        // Signed integer numbers only
        view.inputType = InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_VARIATION_NORMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        // Right aligned
        view.gravity = Gravity.END
        // Move cursor onto next box to right on screen-keyboard enter and force no full screen
        view.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_FULLSCREEN
        view.maxLines = 1
        // Set maximum input length (including sign)
        view.filters = arrayOf<InputFilter>(LengthFilter(5))
        return view
    }

    // Player score edit text
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getScoreView(id: Int, trackname: String, playername: String, editable: Boolean): View {
        val layoutparams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        val view = if (editable) getScoreEntry() else TextView(requireContext())
        view.id = id
        view.tag = trackname
        view.textSize = 12f
        view.layoutParams = layoutparams
        if (editable) {
            view.setOnLongClickListener(View.OnLongClickListener { v ->
                val builder = AlertDialog.Builder(v.context)
                builder.apply {
                    setTitle(playername)
                    setMessage("Enter $trackname score...")
                    val entry = getScoreEntry()
                    setView(entry)
                    setPositiveButton(android.R.string.ok,
                        DialogInterface.OnClickListener { _, _ ->
                            if (v is EditText)
                                v.setText(entry.text)
                        })
                    setNegativeButton(android.R.string.cancel, null)
                }
                // Create the AlertDialog
                builder.create()
                builder.show()
                true
            })

            view.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable) {}
                override fun beforeTextChanged(
                    s: CharSequence, start: Int,
                    count: Int, after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    calculateScores()
                }
            })
        } else {
            // Total text
            view.text = "0"
            view.height = 100
            view.gravity = Gravity.CENTER_HORIZONTAL or Gravity.END
        }
        return view
    }

    // Collect all the scores for all the players in all the tracks
    // TODO: Do we have to re-calculate everything?
    fun calculateScores() {
        // Map of scores - key = player number, value = running total
        val scores = mutableMapOf<Int, Int>()
        // List of track fragments
        val fragments = activity?.supportFragmentManager?.fragments
        if (fragments != null) {
            for (fragment in fragments) {
                // Ignore the total track and track data, update totals later
                if (fragment is TrackFragment && fragment.isVisible) {
                    // Find the score layout in the fragment
                    val view = fragment.view
                    if (view != null && view.id != R.id.total_track) {
                        val layout = view.findViewById<LinearLayout>(R.id.scoreLayout)
                        val children = layout?.children
                        if (children != null) {
                            // Look at the edit text boxes in the fragment
                            for (child in children) {
                                if (child is EditText) {
                                    // Retrieve value and convert to number
                                    val text = child.text.toString()
                                    var value: Int
                                    value = try {
                                        text.toInt()
                                    } catch (e: NumberFormatException) {
                                        // Blank or a negative symbol without number
                                        0
                                    }
                                    // Store number in score map based on player number
                                    if (scores.containsKey(child.id)) {
                                        scores[child.id] = scores.getValue(child.id) + value
                                    } else {
                                        scores[child.id] = value
                                    }
                                }
                            } // end for children
                        }
                    }
                }
            } // end for fragments
        }
        // Update the totals in the total track
        val track = activity?.findViewById<ConstraintLayout>(R.id.total_track)
        val layout = track?.findViewById<LinearLayout>(R.id.scoreLayout)
        val children = layout?.children
        if (children != null) {
            for (child in children) {
                if (child is TextView) {
                    child.text = scores[child.id].toString()
                }
            }
        }
    }
}

// Simple class for storing strings between config changes!
class TracksDataFragment : Fragment() {
    var players = 0

    // TODO - probably doesn't need to be mutable!
    private var tracks = mutableListOf<GamesParser.Track>()
    private var actives = mutableListOf<Boolean>()

    companion object {
        fun newInstance(): TracksDataFragment {
            return TracksDataFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Will get saved between config changes!
        retainInstance = true
    }

    fun setTracks(list: MutableList<GamesParser.Track>) {
        tracks = list
        for (track in tracks)
            actives.add(track.default)
    }

    fun getTrackNames(): Array<CharSequence> {
        val list = ArrayList<CharSequence>()
        for (track in tracks)
            list.add(track.name)
        return list.toTypedArray()
    }

    fun getActives(): BooleanArray {
        return actives.toBooleanArray()
    }

    fun setActives(selected: BooleanArray) {
        actives = selected.toMutableList()
    }
}

class ScoreActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // Get the Intent that started this activity and extract the players
        val bundle = intent.extras
        var playernames = bundle?.getStringArrayList(PLAYER_NAMES)
        if (playernames == null)
            playernames = ArrayList(listOf("fail"))
        var playertypes = bundle?.getStringArrayList(PLAYER_TYPES)
        if (playertypes == null)
            playertypes = ArrayList(listOf("fail"))
        val numplayers = playernames.size

        // Add the player names to the names list
        val view = findViewById<LinearLayout>(R.id.playersLayout)
        for (player in 0 until numplayers) {
            val name = playernames[player] + "\n" + playertypes[player]
            view?.addView(getPlayerTitle(name))
        }

        val xmlconfig = resources.openRawResource(R.raw.games)
        // TODO: Is it worth passing this from main activity, rather than re-read?
        val gamesconfig = GamesParser(xmlconfig)

        val gamename = bundle?.getString(GAME_NAME)
        val tracks = gamesconfig.getTracksList(gamename)

        if (savedInstanceState == null) {
            if (tracks != null) {
                val datatrans = supportFragmentManager.beginTransaction()
                val frag = TracksDataFragment.newInstance()
                datatrans.add(R.id.trackScroll, frag, TRACKS_LIST)
                datatrans.commit()

                // Save the tracks and players for later in the data fragment
                frag.setTracks(tracks as MutableList<GamesParser.Track>)
                frag.players = numplayers

                // Set up initial tracks
                val trans = supportFragmentManager.beginTransaction()
                var first = true
                for (track in tracks) {
                    val tf = TrackFragment.newInstance(
                        playernames.toTypedArray(), track.name, track.colour,
                        (first && track.default), false
                    )
                    trans.add(R.id.trackLayout, tf)
                    if (!track.default) {
                        trans.hide(tf)
                    } else {
                        first = false
                    }
                }
                val tf = TrackFragment.newInstance(
                    playernames.toTypedArray(),
                    "Total",
                    "000000",
                    false,
                    true
                )
                trans.add(R.id.trackLayout, tf)
                trans.commit()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        // Not sure why this is an error in IDE but it compiles and works!
        inflater.inflate(R.menu.menu_score, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_config -> {
                doConfigure()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun doConfigure() {
        val data = supportFragmentManager.findFragmentByTag(TRACKS_LIST)
        if (data is TracksDataFragment) {
            // Simple multi-select dialog
            val actives = data.getActives()
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.score_conf)
            builder.setMultiChoiceItems(
                data.getTrackNames(), actives
            ) { _, which, isChecked ->
                actives[which] = isChecked
            }
            // TODO: Add cancel?
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                data.setActives(actives)
                // Update tracks
                val trans = supportFragmentManager.beginTransaction()
                var num = 0
                for (fragment in supportFragmentManager.fragments) {
                    // Assumes the fragments are in the order we added them!
                    if (fragment is TrackFragment) {
                        val view = fragment.view
                        if (view != null && view.id != R.id.total_track) {
                            if (actives[num] && fragment.isHidden)
                                trans.show(fragment)
                            if (!actives[num] && fragment.isVisible)
                                trans.hide(fragment)
                            num++
                        }
                    }
                }
                trans.commit()
            }
            // Show the configuration dialog
            builder.show()
        }
    }

    // Player name view box
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getPlayerTitle(string: String): TextView {
        val layoutparams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        val view = TextView(this)
        view.textSize = 14f
        view.text = string
        view.layoutParams = layoutparams
        view.gravity = Gravity.CENTER
        return view
    }
}
