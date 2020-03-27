# 7-Wonders-Scoring

## Version 8

* Simple app prototype with two games - 7 Wonders and King/Queendomino
* First screen is game chooser and player name/type entry - list of players names built and shown
* Players can be cleared via menu
* Second screen is dynamic to number of players, and sets up score tracks based on chosen game
* Tracks can be configured in second screen (hide/show)
* Totals are tracked dynamically
* Long press on score opens helper dialog - to be finished with special calc
* Back button will take you back to player entry

## Next steps
Current brain dump - also see TODOs in code

Player entry:
- [ ] reduce player type list once something chosen (reset on clear)
- [ ] make player name recording area prettier/better...
- [ ] player name editing

Score entry:
- [ ] invoke dialog (with special calc) entry on track button press (from player 1)
- [ ] tool tip text on track buttons?
- [ ] implement special dialog instead of just single entry on long press on score
- [ ] clean up score dialog entry - its a bit messy
- [ ] dialog at end declaring winner on final score entry?

Other:
- [ ] make the config strings be internationalized via dynamic lookup on load???

Done:
- [x] don't allow zero players
- [x] make number players dynamic based on first screen
- [x] default player names to "player X" and pre-selected for easy edit/entry
- [x] create game config xml file and parser
- [x] make score tracks dynamic based on read in data config
- [x] add total track
- [x] make "Enter" on score entry go across and not down
- [x] player "Enter" adds player
- [x] game chooser
- [x] player type chooser
- [x] menu to configure tracks
- [x] dialog to select and change visible tracks/score
- [x] sort out players when game changes - dialog to ask if ok to clear.
- [x] allow removal of players on first screen - by clearing menu
- [x] back button takes you back to first screen