package com.stivestabletop.scorer

import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class TrackFragment : Fragment() {
    companion object {
        fun newInstance(players: Int): TrackFragment {
            val f = TrackFragment()
            val args = Bundle()
            args.putInt("players", players)
            f.setArguments(args)
            return f
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args = arguments
        var players = 0
        if (args != null)
            players = args.getInt("players", 0)
        val track = inflater.inflate(R.layout.fragment_line, container, false)
        val view = track.findViewById<LinearLayout>(R.id.scoreLayout)
        // We already have one player's score box, add the others
        while (players > 1) {
            view?.addView(getScore())
            players--
        }
        return track
    }

    // Player name view box
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getScore(): EditText {
        val layoutparams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        )
        val tv_dynamic = EditText(requireContext())
        tv_dynamic.textSize = 12f
        tv_dynamic.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
        tv_dynamic.layoutParams = layoutparams
        //tv_dynamic.gravity = Gravity.CENTER
        return tv_dynamic
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

        if (savedInstanceState == null) {
            // Create 4 tracks
            // TODO: Make this dynaimc somehow?
            supportFragmentManager
                .beginTransaction()
                .add(R.id.trackLayout, TrackFragment.newInstance(players))
                .add(R.id.trackLayout, TrackFragment.newInstance(players))
                .add(R.id.trackLayout, TrackFragment.newInstance(players))
                .add(R.id.trackLayout, TrackFragment.newInstance(players))
                .commit()
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
        val tv_dynamic = TextView(this)
        tv_dynamic.textSize = 14f
        tv_dynamic.text = string
        tv_dynamic.layoutParams = layoutparams
        tv_dynamic.gravity = Gravity.CENTER
        return tv_dynamic
    }
}
