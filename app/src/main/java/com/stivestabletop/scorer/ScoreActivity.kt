package com.stivestabletop.scorer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment


const val TRACKS_LIST = "trackslist"

val LAYOUT_PARAMS_SCORE_VIEW = LinearLayout.LayoutParams(
    0,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    1.0f
)
val LAYOUT_PARAMS_SPECIAL_TEXT = LinearLayout.LayoutParams(
    0,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    0.8f
)
val LAYOUT_PARAMS_SPECIAL_VALUE = LinearLayout.LayoutParams(
    0,
    LinearLayout.LayoutParams.WRAP_CONTENT,
    0.2f
)

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
            players: Array<String>, name: String, colour: String, hint: String,
            special: GamesParser.Special?, first: Boolean, total: Boolean
        ): TrackFragment {
            val f = TrackFragment()
            val args = Bundle()
            args.putStringArray("players", players)
            args.putString("name", name)
            args.putString("colour", colour)
            args.putString("hint", hint)
            args.putBoolean("first", first) // First visible track to get focus
            args.putBoolean("total", total)
            if (special != null) {
                args.putString("calculation", special.calculation)
                args.putStringArray("variables", special.variables.toTypedArray())
                args.putString("multiple", special.multiple)
            }
            f.setArguments(args)
            return f
        }
    }

    @SuppressLint("ResourceType")
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
        var thint = ""
        var first = false
        var total = false
        var calculation = ""
        var variables: Array<String> = arrayOf()
        var multiple = ""
        if (args != null) {
            players = args.getStringArray("players") as Array<String>
            tname = args.getString("name", "track")
            tcol = "#" + args.getString("colour", "FFFFFF")
            thint = args.getString("hint", "enter a points value")
            first = args.getBoolean("first", false)
            total = args.getBoolean("total", false)
            calculation = args.getString("calculation", "")
            if (args.containsKey("variables"))
                variables = args.getStringArray("variables") as Array<String>
            multiple = args.getString("multiple", "")
        }
        // Create new track fragment
        val track = inflater.inflate(R.layout.fragment_line, container, false)
        if (total)
            track.id = R.id.total_track

        // Sort out score boxes
        val layoutview = track.findViewById<LinearLayout>(R.id.scoreLayout)
        for (num in 1..players.size) {
            val sv =
                getScoreView(
                    requireContext(),
                    num,
                    tname,
                    thint,
                    players[num - 1],
                    calculation,
                    variables,
                    multiple,
                    !total
                )
            layoutview?.addView(sv)
            if (num == 1 && first)
                sv.requestFocus()
        }

        // Update the track name/colour
        val nameview = track.findViewById<Button>(R.id.trackName)
        nameview.text = tname
        val bgcol = Color.parseColor(tcol)
        nameview.setBackgroundColor(bgcol)
        nameview.setTextColor(getContrastTextColour(bgcol))
        if (!total) {
            nameview.setOnClickListener(View.OnClickListener { v ->
                if (v is Button) {
                    val move_focus = layoutview.findViewById<EditText>(1)
                    move_focus.requestFocus()
                    // Show the dialogs for this score track, one player at a time

                    // Create dialogs in reverse so that we can link them to each other on dismiss
                    var nextDialog: AlertDialog? = null
                    for (num in players.size downTo 1) {
                        val ev = layoutview.findViewById<EditText>(num)
                        val dialog = specialDialog(
                            ev,
                            players[num - 1],
                            tname,
                            thint,
                            calculation,
                            variables,
                            multiple,
                            nextDialog
                        )
                        nextDialog = dialog
                    }
                    nextDialog?.show()
                }
            })
        }

        return track
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        calculateScores()
    }

    // Entry edit text for dialogs or track
    fun getScoreEntry(context: Context): EditText {
        val view = EditText(context)
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

    // Player score view - either edit text or total text view
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getScoreView(
        context: Context,
        id: Int,
        trackname: String,
        trackhint: String,
        playername: String,
        calculation: String,
        variables: Array<String>,
        multiple: String,
        editable: Boolean
    ): View {
        val view = if (editable) getScoreEntry(context) else TextView(context)
        view.id = id
        view.tag = trackname
        view.textSize = 12f
        view.layoutParams = LAYOUT_PARAMS_SCORE_VIEW
        if (editable) {
            view.setOnLongClickListener(View.OnLongClickListener { v ->
                if (v is EditText) {
                    v.requestFocus()
                    val dialog = specialDialog(
                        v,
                        playername,
                        trackname,
                        trackhint,
                        calculation,
                        variables,
                        multiple,
                        null
                    )
                    dialog.show()
                }
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

    // TODO: Make this into a Dialog class?
    fun specialDialog(
        scoreview: EditText,
        playername: String,
        trackname: String,
        trackhint: String,
        calculation: String,
        variables: Array<String>,
        multiple: String,
        nextdialog: AlertDialog?
    ): AlertDialog {
        val context = scoreview.context
        val builder = AlertDialog.Builder(context)
        builder.setTitle("$playername - $trackname")

        // Set up layout for entry
        val layout = LinearLayout(context)

        layout.setPadding(8, 0, 8, 0)
        layout.orientation = LinearLayout.VERTICAL

        if (trackhint.isNotBlank()) {
            val hint = TextView(context)
            hint.setPadding(0, 8, 0, 0)
            hint.setTypeface(null, Typeface.ITALIC)
            hint.gravity = Gravity.CENTER
            hint.text = trackhint
            hint.textSize = 13f
            layout.addView(hint)
        }

        var first_entry: EditText? = null
        var last_entry: EditText? = null
        // Either a single EditText or a sum TextView
        val sum: TextView

        if (variables.size > 0) {
            // Create special dialog

            // Sum
            val laysum = LinearLayout(context)
            laysum.orientation = LinearLayout.HORIZONTAL
            // TODO: Fix padding issues?
            laysum.setPadding(0, 8, 0, 0)
            val sumtext = TextView(context)
            sumtext.layoutParams = LAYOUT_PARAMS_SPECIAL_TEXT
            sumtext.text = getString(R.string.special_dialog_name_score, trackname)
            laysum.addView(sumtext)
            sum = TextView(context)
            sum.layoutParams = LAYOUT_PARAMS_SPECIAL_VALUE
            sum.text = "0"
            laysum.addView(sum)
            // Created - but don't add it to layout yet

            var laymulti: LinearLayout? = null
            var multitotal: TextView? = null
            if (multiple.isNotBlank()) {
                laymulti = LinearLayout(context)
                laymulti.orientation = LinearLayout.HORIZONTAL
                laymulti.setPadding(0, 8, 0, 0)
                val multitext = TextView(context)
                multitext.layoutParams = LAYOUT_PARAMS_SPECIAL_TEXT
                multitext.text = getString(R.string.special_dialog_multi_sub_total)
                laymulti.addView(multitext)
                multitotal = TextView(context)
                multitotal.layoutParams = LAYOUT_PARAMS_SPECIAL_VALUE
                multitotal.text = "0"
                laymulti.addView(multitotal)
            }
            // Variables & calculator
            val calculator = SpecialCalc(calculation)

            for ((idx, name) in variables.withIndex()) {
                val lay = LinearLayout(context)
                lay.orientation = LinearLayout.HORIZONTAL
                val text = TextView(context)
                text.layoutParams = LAYOUT_PARAMS_SPECIAL_TEXT
                text.text = getString(R.string.special_dialog_name_no_score, name)
                lay.addView(text)
                val entry = getScoreEntry(context)
                entry.layoutParams = LAYOUT_PARAMS_SPECIAL_VALUE
                // Set an incremental id from 1 for each variable (zero is invalid)
                entry.id = idx + 1
                entry.addTextChangedListener(object : TextWatcher {
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
                        // Look for all the other variables in this layout
                        val vars = mutableListOf<Int>()
                        for (id in variables.indices) {
                            val ev = layout.findViewById<EditText>(id + 1)
                            val textentry = ev.text.toString()
                            val value = try {
                                textentry.toInt()
                            } catch (e: NumberFormatException) {
                                // Blank or a negative symbol without number
                                0
                            }
                            vars.add(value)
                        }
                        if (multitotal == null) {
                            sum.text = calculator.calculate(vars).toString()
                        } else {
                            // Multiple sub total - add this in
                            // NOTE: Only "+" is supported for multiple operation
                            val subtotal = multitotal.text.toString().toInt()
                            sum.text = (calculator.calculate(vars) + subtotal).toString()
                        }
                    }
                })
                if (idx == 0) first_entry = entry
                if (idx == variables.lastIndex) last_entry = entry
                lay.addView(entry)
                layout.addView(lay)
            }
            if (laymulti != null && multitotal != null) {
                layout.addView(laymulti)
                layout.addView(laysum)
                val multibutt = Button(context)
                multibutt.text = getString(R.string.special_dialog_multi_button)
                multibutt.setOnClickListener(View.OnClickListener { v ->
                    // This means we copy the current sum to allow multiple inputs
                    if (sum.text.toString() != "0") {
                        // Make a not of the current sum
                        multitotal.text = sum.text
                        // Now zero all the variables (which will change the sum!)
                        for (id in variables.indices) {
                            val ev = layout.findViewById<EditText>(id + 1)
                            ev.setText("")
                            // Set focus back on to the first variable
                            if (id == 0)
                                ev.requestFocus()
                        }
                    }
                })
                layout.addView(multibutt)
            } else {
                layout.addView(laysum)
            }

        } else {
            // Nothing speical... :)
            val lay = LinearLayout(context)
            lay.orientation = LinearLayout.HORIZONTAL
            val text = TextView(context)
            text.layoutParams = LAYOUT_PARAMS_SPECIAL_TEXT
            text.text = getString(R.string.special_dialog_name_no_score, trackname)
            lay.addView(text)
            sum = getScoreEntry(context)
            sum.layoutParams = LAYOUT_PARAMS_SPECIAL_VALUE
            first_entry = sum
            last_entry = sum
            lay.addView(sum)
            layout.addView(lay)
        }

        val scroll = ScrollView(context)
        scroll.addView(layout)
        builder.setView(scroll)

        builder.setPositiveButton(android.R.string.ok,
            DialogInterface.OnClickListener { _, _ ->
                scoreview.setText(sum.text)
                calculateScores()
            })

        builder.setNegativeButton(R.string.skip, null)

        val alert = builder.create()
        alert.setOnShowListener( DialogInterface.OnShowListener { dialog ->
            // Focus on first entry on show
            if (first_entry != null) {
                first_entry.requestFocus()
            }
        })
        alert.setOnDismissListener(DialogInterface.OnDismissListener { dialog ->
            // Show next dialog if there is one
            if (nextdialog != null) {
                nextdialog.show()
            }
        })
        // Make last entry's "next" action dismiss the dialog
        if (last_entry != null) {
            last_entry.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_NEXT -> {
                        alert.dismiss()
                        scoreview.setText(sum.text)
                        calculateScores()
                        true
                    }
                    else -> false
                }
            }
        }

        // FIX: to show soft keyboard all the time for the dialog
        val window = alert.window
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        return alert
    }


    // Collect all the scores for all the players in all the tracks
    // NOTE: Currently we re-calculate everything, could be improved for perf in future if needed
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
        // NOTE: We could pass this from main activity, but it requires a lot of serialization, so
        // its probably not worth it
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
                        playernames.toTypedArray(),
                        track.name,
                        track.colour,
                        track.hint,
                        track.special,
                        (first && track.default),
                        false
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
                    "",
                    null,
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
