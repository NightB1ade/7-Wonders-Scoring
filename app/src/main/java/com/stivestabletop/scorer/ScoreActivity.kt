package com.stivestabletop.scorer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment

@RequiresApi(Build.VERSION_CODES.O)
fun getContrastTextColour(bg: Int): Int {
    // Calculate the perceptive luminance (aka luma) - human eye favors green color...
    val luma: Double =
        ((0.299 * Color.red(bg)) + (0.587 * Color.green(bg)) + (0.114 * Color.blue(bg))) / 255
    return (if (luma > 0.5) Color.BLACK else Color.WHITE)
}

class TrackFragment : Fragment() {
    companion object {
        fun newInstance(players: Int, name: String, colour: String, total: Boolean): TrackFragment {
            val f = TrackFragment()
            val args = Bundle()
            args.putInt("players", players)
            args.putString("name", name)
            args.putString("colour", colour)
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
        var players = 0
        var tname = ""
        var tcol = ""
        var total = false
        if (args != null) {
            players = args.getInt("players", 0)
            tname = args.getString("name", "track")
            tcol = "#" + args.getString("colour", "FFFFFF")
            total = args.getBoolean("total", false)
        }
        // Create new track fragment
        val track = inflater.inflate(R.layout.fragment_line, container, false)
        if (total)
            track.id = R.id.total_track
        // Update the track name/colour
        val name = track.findViewById<TextView>(R.id.trackName)
        name.setText(tname)
        val bgcol = Color.parseColor(tcol)
        name.setBackgroundColor(bgcol)
        name.setTextColor(getContrastTextColour(bgcol))

        // Sort out score boxes
        val view = track.findViewById<LinearLayout>(R.id.scoreLayout)
        for (num in 1..players)
            view?.addView(getScoreView(num, tname, !total))
        return track
    }

    // Player name view box
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getScoreView(id: Int, tag: String, editable: Boolean): View {
        val layoutparams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        val view = if (editable) EditText(requireContext()) else TextView(requireContext())
        view.id = id
        view.tag = tag
        view.textSize = 12f
        view.layoutParams = layoutparams
        if (editable) {
            view.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_NORMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            view.gravity = Gravity.END

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
                    // TODO: Do we have to re-calculate everything?
                    // TODO: Does this have to be soo ugly...
                    // TODO: Could be a function call that is called from here rather than duplicated!
                    // Collect all the scores for all the players in all the tracks
                    val scores = mutableMapOf<Int, Int>()
                    val fragments = activity?.supportFragmentManager?.fragments
                    if (fragments != null) {
                        for (fragment in fragments) {
                            if (fragment.id != R.id.total_track) {
                                val view = fragment.view
                                val layout = view?.findViewById<LinearLayout>(R.id.scoreLayout)
                                val children = layout?.children
                                if (children != null)
                                    for (child in children) {
                                        if (child is EditText) {
                                            val text = child.text.toString()
                                            var value: Int
                                            try {
                                                value = text.toInt()
                                            } catch (e: NumberFormatException) {
                                                // Blank or maybe a negative symbol
                                                value = 0
                                            }
                                            if (scores.containsKey(child.id)) {
                                                scores[child.id] = scores.getValue(child.id) + value
                                            } else {
                                                scores[child.id] = value
                                            }
                                        }
                                    }
                            }
                        }
                    }
                    // Update the totals in the total track
                    val track = activity?.findViewById<ConstraintLayout>(R.id.total_track)
                    val layout = track?.findViewById<LinearLayout>(R.id.scoreLayout)
                    val children = layout?.children
                    if (children != null)
                        for (child in children) {
                            if (child is TextView) {
                                child.text = scores[child.id].toString()
                            }
                        }
                }
            })
        } else {
            view.text = "0"
            view.height = 100
            view.gravity = Gravity.CENTER_HORIZONTAL or Gravity.END
        }
        return view
    }
}

class ScoreActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // Get the Intent that started this activity and extract the players
        var playerlist = intent.getStringArrayListExtra(PLAYER_MESSAGE)
        if (playerlist == null)
            playerlist = ArrayList(listOf("fail"))
        val players = playerlist.size

        // Add the player names to the names list
        val view = findViewById<LinearLayout>(R.id.playersLayout)
        for (message in playerlist)
            view?.addView(getTitle(message))

        // TODO: Pass this info from main activity???
        val xmlconfig = resources.openRawResource(R.raw.games)
        val games = GamesParser(xmlconfig)

        // TODO: Don't hardcode game!
        val tracks = games.games[0].tracks

        if (savedInstanceState == null) {
            if (tracks != null) {
                val trans = supportFragmentManager.beginTransaction()
                for (track in tracks) {
                    if (track.default)
                        trans.add(
                            R.id.trackLayout,
                            TrackFragment.newInstance(players, track.name, track.colour, false)
                        )
                }
                trans.add(
                    R.id.trackLayout,
                    TrackFragment.newInstance(players, "Total", "000000", true)
                )
                trans.commit()
            }
        }
    }

    // Player name view box
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getTitle(string: String): TextView {
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
