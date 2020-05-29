# BGG Scorer

## Version 13

* Simple board game scoring app which supports the following games:
  * 7 Wonders
  * Kingdomino & Queendomino
  * Underwater cities
  * Neom
 
* First screen is game chooser and player name/type entry
  * List of players names recorded and shown
  * Players/game can be cleared via menu
* Second screen is dynamic to number of players, and sets up score tracks based on chosen game
  * Tracks can be configured in second screen (hide/show)
  * Totals are tracked dynamically
  * Button press on track name opens helper dialogs for each player inc special calc as needed
  * Long press on a single score opens helper dialog including special calc as needed
  * Helper dialog supports sub-total for games that have multiple scores - such as King/Queendomino
  * Clear scores menu for allowing second game without returning to game chooser
  * Back button or Game select menu will take you back to player entry

## Next steps
Current brain dump - also see TODOs in code

Player entry:
- [x] set first letter as capital on player name/type
- [x] reduce player type list once something chosen (reset on clear)
- [x] change NEXT button to ADD
- [x] show player type on focus, rather than after first key?
- [ ] make player name recording area prettier/better...
- [ ] player name editing after entry?

Score entry:
- [x] menu entry to return to first screen
- [x] clear scores menu
- [ ] Set focus on NEXT row/box if special dialog invoked
- [ ] menu entry for help about invoking special dialog?
- [ ] tool tip text on track buttons?
- [ ] dialog at end declaring winner on final score entry?
- [ ] calculator dialog

Other:
- [ ] make the config strings be internationalized via dynamic lookup on load???

Done:
- [x] add MULTIPLE feature to special dialog
- [x] BUG: Make sure totals updated after special dialog
- [x] BUG: add auto-focus + keyboard on the special dialog
- [x] BUG: on return from score activity keyboard entry is NOT displayed!
- [x] add tip text on special dialog and score track name
- [x] clean up alignment of totals and entries on special dialog
- [x] clean up score dialog entry - make it prettier
- [x] invoke dialog (with special calc) entry on track button press (from player 1)
- [x] implement special dialog instead of just single entry on long press on score
- [x] change player type list to a look-up with editable text
- [x] BUG: on landscape layout - show player name editing!
- [x] BUG: pressing NEXT player does not select all text of the name
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