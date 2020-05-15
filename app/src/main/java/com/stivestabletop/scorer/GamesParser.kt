package com.stivestabletop.scorer

import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.*

private const val TAG = "GameParser"
private const val MAX_PLAYER_LEN = 15

// NOTE based on code from "Parse XML data" in:
// https://developer.android.com/training/basics/network-ops/xml#kotlin

class GamesParser(xmlconfig: InputStream) {
    // We don't use namespaces in xml
    private val ns: String? = null

    // Data class stores - matching dictionaries in XML
    data class Game(val name: String, val players: List<String>?, val tracks: List<Track>?)
    data class Track(
        val name: String,
        val colour: String,
        val default: Boolean,
        val hint: String,
        val special: Special?
    )

    data class Special(val variables: List<String>, val calculation: String, val multiple: String)

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

    @Suppress("NAME_SHADOWING")
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
        if (name.isBlank())
            Log.e(TAG, "Missing name on a game")
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
        var result = readText(parser)
        if (result.length > MAX_PLAYER_LEN) {
            Log.w(TAG, "Player type '$result' is too long, truncated to $MAX_PLAYER_LEN chars")
            result = result.substring(0, MAX_PLAYER_LEN)
        }
        parser.require(XmlPullParser.END_TAG, ns, "players")
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readTrack(parser: XmlPullParser): Track {
        parser.require(XmlPullParser.START_TAG, ns, "tracks")
        var name = ""
        var colour = "FFFFFF"
        var default = false
        var hint = ""   // Optional
        var special: Special? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "name" -> name = readName(parser)
                "colour" -> colour = readColour(parser)
                "default" -> default = readDefault(parser)
                "hint" -> hint = readHint(parser)
                "special" -> special = readSpecial(parser)
                else -> skip(parser)
            }
        }
        if (name.isBlank())
            Log.e(TAG, "Missing name on a track")
        return Track(name, colour, default, hint, special)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readColour(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "colour")
        var result = readText(parser).toUpperCase(Locale.ROOT)
        // Check that colour is a 6 character hexadecimal
        var ok: Boolean
        if (result.length == 6) {
            ok = true
            for (char in result.asSequence()) {
                when (char) {
                    in '0'..'9' -> ok = true
                    in 'A'..'F' -> ok = true
                    else -> ok = false
                }
                if (!ok) break
            }
        } else {
            ok = false
        }
        if (!ok) {
            Log.w(TAG, "Ignoring invalid colour '$result'")
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

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readHint(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "hint")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "hint")
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSpecial(parser: XmlPullParser): Special {
        parser.require(XmlPullParser.START_TAG, ns, "special")
        var calculation = ""
        var multiple = ""   // Optional
        val variables = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "calculation" -> calculation = readCalculation(parser)
                "multiple" -> multiple = readMultiple(parser)
                "variables" -> variables.add(readVariables(parser))
                else -> skip(parser)
            }
        }
        if (variables.size == 0)
            Log.e(TAG, "No variables specified for special calculation: $calculation")
        return Special(variables, calculation, multiple)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readCalculation(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "calculation")
        val result = readText(parser)
        // Very simple check for invalid chars
        var ok: Boolean
        for (char in result.asSequence()) {
            when (char) {
                in '0'..'9' -> ok = true
                in 'A'..'Z' -> ok = true
                in "+-*/nx" -> ok = true
                else -> ok = false
            }
            if (!ok)
                Log.e(TAG, "Found invalid character '$char' in calculation: $result")
        }
        parser.require(XmlPullParser.END_TAG, ns, "calculation")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readMultiple(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "multiple")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "multiple")
        return result
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readVariables(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "variables")
        val result = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "variables")
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