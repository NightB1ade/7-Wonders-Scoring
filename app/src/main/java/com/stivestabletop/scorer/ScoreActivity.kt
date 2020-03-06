package com.stivestabletop.scorer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class TrackFragment : Fragment() {
    companion object {
        fun newInstance(): TrackFragment {
            return TrackFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_line, container, false)
    }
}

class ScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        if (savedInstanceState == null) {
            // Create initial store of stuff
            supportFragmentManager
                .beginTransaction()
                .add(R.id.trackLayout, TrackFragment.newInstance())
                .add(R.id.trackLayout, TrackFragment.newInstance())
                .add(R.id.trackLayout, TrackFragment.newInstance())
                .add(R.id.trackLayout, TrackFragment.newInstance())
                .commit()
        }
    }
}
