package com.stivestabletop.scorer

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream

private const val TAG = "GameParser"

// NOTE based on code from "Parse XML data" in:
// https://developer.android.com/training/basics/network-ops/xml#kotlin

class GamesParser(xmlconfig: InputStream) {
    // We don't use namespaces
    private val ns: String? = null

    // Data class stores
    data class Game(val name: String, val players: List<String>?, val tracks: List<Track>?)
    data class Track(
        val name: String,
        val colour: String,
        val default: Boolean,
        val special: Special?
    )

    data class Special(val variables: List<String>, val calculation: String)

    // Public access to the list of games
    var games: List<Game>

    init {
        games = parse(xmlconfig)
    }

    fun getGamesList(): List<String> {
        val list = mutableListOf<String>()
        for (game in games)
            list.add(game.name)
        return list
    }

    fun getPlayersList(gamename: String): List<String> {
        for (game in games) {
            if (game.name == gamename)
                if (game.players != null)
                    return game.players
        }
        return listOf("")
    }

    fun getTracksList(gamename: String?): List<Track>? {
        if (gamename != null)
            for (game in games) {
                if (game.name == gamename)
                    return game.tracks
            }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parse(inputStream: InputStream): List<Game> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readGames(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readGames(parser: XmlPullParser): List<Game> {
        val entries = mutableListOf<Game>()

        parser.require(XmlPullParser.START_TAG, ns, "root")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            if (parser.name == "games") {
                entries.add(readGame(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an game
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readGame(parser: XmlPullParser): Game {
        parser.require(XmlPullParser.START_TAG, ns, "games")
        var name = ""
        val players = mutableListOf<String>()
        val tracks = mutableListOf<Track>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "name" -> name = readName(parser)
                "players" -> players.add(readPlayer(parser))
                "tracks" -> tracks.add(readTrack(parser))
                else -> skip(parser)
            }
        }
        return Game(name, players, tracks)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readName(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "name")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "name")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readPlayer(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "players")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "players")
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        parser.require(XmlPullParser.START_TAG, ns, "tracks")
        var name = ""
        var colour = "FFFFFF"
        var default = false
        var special: Special? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "name" -> name = readName(parser)
                "colour" -> colour = readColour(parser)
                "default" -> default = readDefault(parser)
                // TODO: Add reading of speical!
                else -> skip(parser)
            }
        }
        return Track(name, colour, default, special)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readColour(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "colour")
        var result = readText(parser)
        if (result.length != 6) {
            Log.w(TAG, "Ignoring colour '$result'")
            result = "FF00FF"
        }
        parser.require(XmlPullParser.END_TAG, ns, "colour")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readDefault(parser: XmlPullParser): Boolean {
        parser.require(XmlPullParser.START_TAG, ns, "default")
        val result = readBoolean(parser)
        parser.require(XmlPullParser.END_TAG, ns, "default")
        return result
    }

    // For reading all the strings from the XML
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    // For reading all the booleans from the XML
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readBoolean(parser: XmlPullParser): Boolean {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return (result == "true")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}