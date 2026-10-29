---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# CoordiMate Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

### Architecture

<puml src="diagrams/ArchitectureDiagram.puml" width="280" />

The ***Architecture Diagram*** given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/MainApp.java)) is in charge of the app launch and shut down.
* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The *Sequence Diagram* below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its *API* in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<puml src="diagrams/ComponentManagers.puml" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/Ui.java).

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

The first diagram shows the parts created or used by `MainWindow`, including `CommandBox`, `ResultDisplay`, `PersonListPanel`, `EventListPanel`, `ReminderPanel`, `StatusBarFooter`, and `TutorialWindow`. `MainWindow` also uses `TagListViewModel` to keep the Tags view synchronized with contact and tag data.

The inheritance diagram shows all ten subclasses of the abstract `UiPart`, which captures behavior shared by visible GUI parts. Each `UiPart` box represents the same superclass, repeated to keep the arrows clear. `TagListViewModel` and `WindowBounds` are helpers rather than `UiPart` subclasses.

<puml src="diagrams/UiPartInheritanceDiagram.puml" alt="UI component inheritance from UiPart"/>

The `HelpWindow` provides an in-app command quick reference. Its read-only, scrollable command table is populated by
`HelpWindow` using the command words defined by the corresponding command classes, while its FXML file controls the
resizable layout. The User Guide URL remains available for more detailed instructions.
The window's **Close** button is marked as the JavaFX cancel button, allowing users to dismiss it by selecting the
button or pressing <kbd>Esc</kbd> without exiting the application.
`MainWindow` hosts the guided `TutorialWindow` as an overlay above the main application scene. The sixteen-step tour
highlights the Contacts, Events, and Tags views, contact commands and input, lists, details, the event member list, and
Help. It also introduces keyboard navigation. It selects an existing contact, event, or tag when needed, without
changing saved data; empty lists show their existing hints.
The event details pane scrolls to the member list for its step, and highlights inside a scroll pane are clipped to the
visible viewport. The dim backdrop leaves the highlighted area clear while a transparent blocker and disabled command
box prevent commands from running during the tour. `UiManager` opens the overlay on first launch. `MainWindow` records
the tutorial preference when the user closes or finishes it. The `tutorial` command, the Tutorial item beside Help in
the top bar, and the <kbd>F2</kbd> shortcut can open the overlay again at any time. `TutorialCommand` returns a
`CommandResult` whose `shouldShowTutorial()` flag asks `MainWindow` to open the overlay, in the same way that
`shouldShowHelp()` opens the Help window. The overlay handles Left and Right Arrow navigation, Escape dismissal, and
Tab focus movement before key events can reach the command box or the main window's navigation shortcuts. Tab and
Shift+Tab cycle through the card's enabled buttons so that keyboard focus cannot leave the card.

`CommandBox` keeps submitted, nonblank commands in a session-only list. The list has no configured cap or eviction;
its practical limit is the JVM heap, and it is discarded when the app closes. It records an attempt before execution
so commands that fail validation can be recalled for correction. Up and Down move through the list, with the current
input saved as a draft and restored after the newest entry. Browsing does not execute a command or modify stored
entries. While `Logic` awaits delete confirmation, arrow-key navigation is paused and the response is not added
to history. Inserting a command from the sidebar resets history navigation.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/MainWindow.java).
`ResultDisplay` sizes its scroll pane from the message's wrapped height, capped at 220 pixels, so delete confirmation
details remain visible at the minimum window size while longer feedback shows
several lines without displacing the workspace or command box. Its icon and status label stay outside the scroll pane.
The status column reserves space for the overflow hint even while it is hidden, keeping the message width stable.
The hint appears only when the current message is taller than the visible viewport.

At launch, `MainWindow` obtains the saved size, position, and maximized state from `GuiSettings`. It selects the screen
containing the saved position, or the primary screen if that display is no longer connected. `WindowBounds` then clamps
the saved dimensions and coordinates to the screen's visual bounds, which exclude operating-system areas such as the
taskbar or dock. The preferred minimum size is 1200 × 800 pixels, capped to the available area so it cannot force the
window beyond a smaller display. `MainWindow` tracks the latest non-maximized bounds separately, allowing a maximized
window to be restored without replacing its normal dimensions. If a maximized window is moved to another monitor
before closing, the normal bounds retain their size and relative position on that monitor so the next launch maximizes
there. Screen fallback is logged at `WARNING`, bounds adjustments at `INFO`, and maximized-state restoration and saving
at `FINE`.

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data. The Contacts view
  displays the filtered contact list beside a details pane, follows a selected contact by permanent member ID
  across edits and reordering, and clears the selection when that ID leaves the displayed list.
  Contact cards, contact details, event rosters, and tag member lists display `ID: <number>` from the saved contact,
  independently of the current contact filter. The details pane displays all saved contact
  fields, showing a placeholder for unset optional fields and no tag chips for an empty tag set.
  It also derives assigned-event rows from the live event list. The Events view resolves each assigned
  member to contact role, organisation, and tags, and refreshes when contact data changes. Contact and
  event views keep their details pane above a minimum width, then stack the list above details when the
  window is narrow. The Events view stacks below 900 pixels of workspace width so its timing card does not extend
  beyond the details viewport. Sidebar command rows insert only the command word into the command box and show its
  format and an example; rows also support keyboard activation. Selecting a tag shows all saved contacts
  assigned to it, independent of the current contact filter, and refreshes names, roles, organisations, and tags
  when contact data changes.
  A window-level key filter owns Navigation-mode input: `J` and `K` move within the active view, `S` cycles views,
  and `I` returns to Command mode. Other unmodified keys are consumed before focused controls can act on them.
  Entering Navigation mode transfers focus to the mode indicator, preventing controls such as reminder dismissal
  buttons from retaining keyboard activation.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
  Commands such as `members` and the tag commands can return a view selection request that `MainWindow` applies
  through the same view controls used by the sidebar.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below follows `execute("delete 1")` and the subsequent confirmation response. The first call resolves and stores the contact without deleting it. If the response is `y` or `Y` after removing surrounding whitespace, the confirmed command deletes the contact on a candidate model through the atomic save path; any other response cancels the deletion without changing data.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

Both delete diagrams use a reference to the detailed atomic-save sequence in
[Saving data atomically](#saving-data-atomically). The confirmed command deletes the resolved contact from a candidate
model; `LogicManager` replaces the live data only after saving the candidate succeeds.


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `CoordiMateParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`). For `delete`, `LogicManager` first calls `resolvePerson` and returns a confirmation prompt without executing the deletion.
1. If the next input is `y` or `Y` after removing surrounding whitespace, `LogicManager` creates a command for the resolved `Person` and executes it on a candidate model. That command deletes the contact from the candidate, which is saved before the live model changes. Any other input cancels the deletion.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `CoordiMateParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `CoordiMateParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

Command error messages follow these conventions:

* Use sentence case and full stops for explanations, without an `Error!` prefix or exclamation marks.
* For malformed command syntax, use `ParserUtil.createInvalidFormatException(Command.MESSAGE_USAGE)`.
  Usage text has a command description, `Parameters:`, and `Example:` on separate lines.
* Every command error includes the relevant command's usage once. `CoordiMateParser` appends it to parse errors;
  `LogicManager` appends it to execution and storage errors. `Messages.withUsage` avoids duplicating usage.
* For unknown or repeated parameters, name the offending prefix: `Unknown parameter: x/.` or
  `Parameter evn/ may only be specified once.`
* For invalid field values, explain the constraint and provide an `Example:` using the failing parameter's prefix.
* Distinguish missing parameters, which show command-format guidance, from supplied but empty values, which use
  `<Field> must not be empty. Example: <prefix><value>`. Optional contact fields can still be cleared intentionally.
* Storage-loading errors use `Saved data could not be loaded. Check the local data file.` for every data type.
* For execution errors, explain what failed and how to correct it. Reuse the templates in `Messages` for missing
  contacts, events, and tags, and for unmatched or ambiguous contact identifiers.
* Add `No changes were made.` when an attempted change is rejected without modifying the saved data.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="575" />


The `Model` component,

* stores CoordiMate data, including all `Person` objects (which are contained in a `UniquePersonList` object).
* assigns each saved `Person` an immutable `MemberId`. `CoordiMate` maintains the next ID to allocate, and
  `UniquePersonList` rejects duplicate IDs. Replacing or editing a contact preserves its ID.
* stores a case-insensitively unique global tag list. Every tag assigned to a contact is also registered in this list;
  adding, editing, or replacing contacts registers any missing tags while preserving existing unused tags.
* stores all `Event` objects. Each `Event` records its assigned members as an ordered list of contact `Name`s. `CoordiMate` keeps these lists in sync with the contacts: deleting a contact removes their name from every event, and renaming a contact updates it.
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* keeps an archive view, which decides whether the filtered contact list and the filtered event list show active or [archived](#archiving-contacts-and-events) items.
* stores a `UserPrefs` object that represents the user’s preferences, including window size and position, maximized
  state, and whether the first-launch tutorial has been completed or dismissed. This is exposed to the outside as a
  `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** `CoordiMate` already keeps a global `UniqueTagList`, but each `Person` holds its own `Tag` objects, which are compared with those in the list by value and are not necessarily the same objects. The alternative, arguably more object-oriented, design below has each `Person` reference the `Tag` objects in that list instead, so `CoordiMate` maintains exactly one `Tag` object per unique tag.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="700" />

The `Storage` component,
* can save both CoordiMate data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonCoordiMateStorage` and `JsonUserPrefsStorage` (one class per data file).
* converts CoordiMate data through `JsonSerializableCoordiMate`, which stores contacts (`JsonAdaptedPerson`), events (`JsonAdaptedEvent`), and the global tag list (`JsonAdaptedTag`) as separate lists. Each contact also stores its own tags, and each event records its members by contact name rather than by member ID.
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

If the data file cannot be loaded during startup, `MainApp` creates an empty in-memory model only for initialization
and passes a recovery flag to `UiManager`. If model conversion raised `IllegalValueException`, it also passes the
validation message. After the main window is ready, `UiManager` shows a warning that names the unchanged data file,
includes any available validation message, and asks the user to repair or restore it. Dismissing the warning requests
a normal JavaFX shutdown before the tutorial or command interface can be used. When preferences also fail to load,
`MainApp` passes that warning separately so the same recovery dialog names both files and their problems.

The tutorial flag is saved with the other user preferences. Preference files without that field are read with the
tutorial flag set to `false`, so the tutorial is offered until the user closes or finishes it.

Before Jackson binds saved data to Java fields, `SavedJsonTypeValidator` checks the JSON types of known contact,
event, and preference fields. This prevents Jackson from converting values such as numeric phone numbers or numeric
window maximization flags. Existing validation for `archived`, member IDs, and model values still runs afterward.
The validator reports a field path for type errors, while unknown fields remain ignored for compatibility.
For preferences, it removes explicit `null` scalar fields after validation so Jackson retains the defaults for
`hasSeenTutorial`, window width, window height, and maximized state.
If preferences cannot be loaded, `MainApp` uses defaults and shows a warning. It skips preference writes during that
session, including shutdown, so the original file remains available for repair. If the data file is also invalid,
the warning instead appears in the data recovery dialog before the app closes.

Previously saved digit-only phone numbers with at least three digits, and 7–15-digit numbers whose brackets do not
match, remain valid model data, so older JSON files can still load. The command parser applies the current rule,
`Phone#isValidInputPhone`, whenever a user supplies a phone number for `add` or `edit`: 7–15 digits, an optional `+`
or bracketed country code such as `(+65)` at the start, and brackets in non-nested pairs that each enclose a digit.
Editing another field does not require replacing a legacy phone number.

When loading JSON data, explicitly saved tags are validated and restored before contacts. If a contact uses a tag
that is missing from the saved global tag list, CoordiMate registers it automatically. This also migrates older files
without an explicit tag list: default tags are retained and tags used by contacts are added. Existing saved tag order,
capitalization, and unused tags are preserved.

### Common classes

Classes used by multiple components are in the `coordimate.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Permanent member IDs

Each saved contact has a unique `MemberId` in the range `1` to `2147483647`. The displayed label is
`ID: <number>`; search results, filters, and list order never determine its value.

#### Allocation and editing

`Person` holds an immutable ID. A new contact can be an unassigned draft until `CoordiMate#addPerson` accepts it.
`CoordiMate` allocates from its `long nextMemberId` counter, advances the counter after successful additions,
and preserves imported IDs. `UniquePersonList` rejects duplicate assigned IDs and prevents replacements from
changing a contact's ID. Bulk replacement reserves all supplied IDs before assigning any missing ones.
Copies and resets preserve both IDs and allocation state, including when the highest ID has been deleted.
Deleting contacts does not recycle IDs. `clear` creates a new empty dataset and resets allocation to `1`.
The counter value `2147483648` represents exhaustion; further automatic allocation is rejected.
`AddCommand` translates the allocation failure into a `CommandException` with the message
`No more member IDs are available.` so the UI displays normal command feedback. Contacts, tags, the
allocation counter, and the saved file remain unchanged.

#### Command targeting

`ParserUtil#parseMemberId` parses a positive integer without converting it to a list position.
`MemberIdResolver` searches `Model#getCoordiMate().getPersonList()`, including contacts hidden by the active
view. `edit` and `delete` resolve one ID; `assign` and `unassign` resolve every ID before changing an event.
An unknown ID reports `No contact exists with member ID: <number>. Use an existing member ID.` without
changing data or falling back to a position. Event membership and attendance still use contact names internally;
the commands translate IDs to those names after resolution.

Deletion confirmation retains the resolved contact. On confirmation, the command resolves its ID again and
rejects the deletion if that contact was removed or edited. Reordering or filtering cannot redirect it.

#### Storage and migration

`JsonAdaptedPerson` writes a numeric `memberId`, and `JsonSerializableCoordiMate` writes a numeric
`nextMemberId`. Loading first parses all contacts and finds the highest supplied ID. It then validates the
saved counter, or derives one greater than the highest ID when no counter exists. Contacts with missing or
null IDs receive consecutive IDs in file order starting from that counter. An entirely legacy file therefore
receives IDs starting at `1`; mixed files preserve all existing IDs before allocating new ones.

Loading does not rewrite the file. The next successful save records migrated IDs and the counter, so future
launches retain IDs and do not reuse deleted ones. Duplicate IDs, non-integral or out-of-range values, a counter
not greater than every saved ID, and exhausted allocation for a contact without an ID reject the file through
the normal storage-loading error path. Numeric strings are not accepted as IDs or counters.

#### Design considerations and verification

A saved counter avoids reusing an ID after deleting the highest-numbered contact. Deriving the next ID from
the current list on every launch would lose that history. Full-list lookup keeps command behavior independent
of the UI and is adequate for the expected contact count.

`MemberIdPersistenceTest` covers saved counters, legacy and mixed-file migration, invalid data, and exhaustion.
`PermanentMemberIdCommandsTest` covers targeting across searches, tag filters, event-member views, empty
results, reordering, and restarts. It also runs a legacy migration through editing, event assignment and
unassignment, deletion, saving, and restart, then checks that a deleted ID cannot target another contact.
UI tests verify the same labels and selection behavior in Contacts, Events, and Tags.

### Saving data atomically

Commands that edit contacts, event details, tags, or attendance either take effect in full, both in the app
and in the data file, or not at all.
This keeps the displayed data and the saved data identical even when the data file cannot be read or written,
and meets the requirement that saved data is not lost if the app terminates unexpectedly.
Reminder dismissal uses a separate save path with a session-only fallback, described in
[Dismissing reminders](#dismissing-reminders).

#### Implementation

Two mechanisms work together.

**1. Commands run on a copy of the model.** [`LogicManager#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/LogicManager.java)
passes commands that edit contacts, event details, tags, or attendance to `LogicManager#executeAtomicCommand`.
These are `AddCommand`, `EditCommand`,
`DeleteCommand`, `ClearCommand`, `AddEventCommand`, `EditEventCommand`, `DeleteEventCommand`, `AssignCommand`,
`UnassignCommand`, `MarkAttendanceCommand`, and the tag commands that extend `MutatingTagCommand`.

`executeAtomicCommand` does the following:

1. It calls `Storage#readCoordiMate()` to check that the existing data file can still be loaded. If it cannot,
   the command is rejected with a load error and the file is left untouched, so that the user can repair it.
   A missing data file is not an error.
1. It creates a second `ModelManager`, the _candidate_, from a copy of the live model's data, including member
   IDs and allocation state. Its filtered contact list initially shows the same contacts as the live view.
   Commands resolve member IDs against the complete saved contact list in either model.
1. It executes the command on the candidate. If the command fails, the candidate is discarded.
1. It saves the candidate's data with `Storage#saveCoordiMateIfUnchanged`. This compares the file with the revision
   loaded at startup or last saved by this window while holding a lock shared by CoordiMate instances. If another
   window changed the file, the command is rejected and the candidate is discarded. Other save failures still
   report an error such as `Event could not be saved. No changes were made.`
1. Only then does it call `Model#setCoordiMate` to replace the live model's data with the candidate's data,
   which updates the UI.

The live model is not touched until step 5, so a failure at any earlier step leaves the app exactly as it was.

The sequence diagram below shows a successful call to `executeAtomicCommand(c)` with an `AddEventCommand` as `c`. Parsing is omitted.

<puml src="diagrams/AtomicCommandSequenceDiagram.puml" alt="Interactions when a command that changes data is executed" />

The activity diagram below summarises the possible outcomes.

<puml src="diagrams/AtomicCommandActivityDiagram.puml" width="450" />

After committing, `LogicManager` adjusts the displayed contact list where needed: `add` and `edit` show all
contacts again so that the changed contact is visible, and renaming an event whose members are being shown keeps
that view on the renamed event (see [Keeping events in sync with contacts](#keeping-events-in-sync-with-contacts)).

**2. The data file is replaced in one step.**
[`JsonCoordiMateStorage#saveCoordiMate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonCoordiMateStorage.java)
writes the new data to a temporary file named `coordimate-*.tmp` in the same folder as the data file, then moves it
over the data file using `StandardCopyOption.ATOMIC_MOVE`. The data file therefore always holds either the complete
old data or the complete new data. The temporary file is deleted if anything fails.

**Deleting a contact** adds a confirmation step. For a `delete` command, `LogicManager` checks that the data file
can be loaded, resolves the contact, stores it in its `pendingDelete` field, and returns a confirmation prompt.
The next input is treated as the answer, not as a command: `y` or `Y` runs the deletion through
`executeAtomicCommand`, and any other input cancels it.

**Commands that do not change data**, such as `list`, `search`, `filter`, and `members`, are executed
directly on the live model without saving. They cannot overwrite a newer or invalid data file. A window that has
become stale can still show its loaded data, but its next save is rejected until it is reopened. Reminder dismissal
uses the same conditional save and retains its session-only fallback when saving fails.

`CommandResult.View` carries one requested view, or `NONE` when a command does not request a view change.
`MainWindow` applies a requested view through the same view controls used by the sidebar.

Commands that produce contact, event, or tag results request the corresponding `CommandResult.View`, which
`MainWindow` applies after successful execution. A failed command does not return a view request, so the current
tab remains selected. Contact deletion requests Contacts when its confirmation prompt is displayed. `assign` and
`unassign` deliberately return `View.NONE`; the current tab remains selected while its observable contact or event
details refresh.

#### Design considerations

**Aspect: How a command is prevented from taking effect when saving fails**

* **Alternative 1 (current choice):** Execute the command on a copy of the model, save the copy, then replace the
  live model's data.
  * Pros: Works for the editing commands listed above without command-specific rollback code. The UI never shows
    an edit that was not saved.
  * Cons: Copies all data for each command, which is acceptable for the expected 250 contacts. Each new command
    using this save path must be added to the list in `LogicManager#execute`.

* **Alternative 2:** Execute the command on the live model, and restore a snapshot if saving fails.
  * Pros: No copy is needed when saving succeeds.
  * Cons: The UI observes the model, so it briefly shows a change that is then reverted.

* **Alternative 3:** Have each command undo its own change if saving fails.
  * Pros: Uses the least memory.
  * Cons: Every command needs correct undo logic, which is easy to get wrong.

### Dismissing reminders

`LogicManager` creates a shared `ReminderService` and supplies it to `CoordiMateParser`, which passes it to
`DismissCommandParser` and `DismissCommand`. The CLI command and the panel's **Dismiss** button therefore use
the same dismissal history and storage behavior.

1. `DismissCommandParser` uses `ParserUtil#parseIndex` to accept one positive, 1-based index. Missing input
   produces a format error; invalid integers and extra arguments are rejected before execution.
1. `DismissCommand#execute` obtains the refreshed reminder list from `ReminderService#getReminders` and checks
   the index against its size. `UpcomingEventFinder` orders this list by start time and then by event name,
   ignoring case; date-only starts count as midnight for sorting. `ReminderPanel` renders the same list with
   1-based indexes and regenerates the rows whenever it changes. An out-of-range index, including an empty
   list, produces a command error without reading or writing storage.
1. The command passes the selected `Event` to `ReminderService#dismissReminder`. The service creates a copy
   with updated dismissal history, checks that the existing data can be loaded, and saves the candidate
   before updating the live event. Event details, members, attendance, and the contact filter are preserved.
1. On success, the command returns `Reminder dismissed: EVENT_NAME.` without requesting a view change.
   If reading or saving fails, the service keeps the dismissal in its session map, refreshes the list, and
   throws `ReminderDismissalException`. The command logs the failure and returns `CommandResult.warning`.
   `MainWindow` checks `CommandResult#isWarning` and uses `ResultDisplay#setWarningFeedback` to show the
   existing **Not saved** status. The persisted history is unchanged, so the reminder may return after restart.

`LogicManager` returns the dismissal result directly, bypassing both `executeAtomicCommand` and the generic
post-command save. `ReminderService` already performs the save; a second save would be redundant and could
replace the warning with an unrelated error. The normal final reminder refresh still runs after execution.

`ReminderLogicIntegrationTest` covers ordering, edits, persistence, event data, and read/write failures using
a controlled clock and temporary JSON files. `MainWindowReminderTest` covers immediate panel updates,
renumbering, feedback, selected tabs, and command focus.

### Event attendance

Attendance records whether each member of an event was present or absent.

#### Implementation

<puml src="diagrams/EventClassDiagram.puml" width="450" />

An [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java) is immutable. It stores:

* its members as a `List<Name>` of contact names, in the order they were assigned.
* its attendance as a `Map<Name, AttendanceStatus>`, where `AttendanceStatus` is an enumeration of `PRESENT` and
  `ABSENT`. A member with no entry in the map has not been marked.

The `Event` constructor rejects an attendance record that contains a name which is not a member, so attendance can
never exist for someone who is not assigned to the event.

The `markattendance` command works as follows:

1. `MarkAttendanceCommandParser` checks that `evn/`, `mem/`, and `att/` each appear exactly once, converts the
   status with `AttendanceStatus#fromString`, and validates the member name with `ParserUtil#parseName`.
1. `MarkAttendanceCommand#execute` finds the event and then the member, both ignoring case. It uses the member's
   name as stored in the event, so that feedback and the attendance record use the saved capitalisation.
1. If the member already has the requested status, the command reports that no changes were made.
1. Otherwise it calls `Event#withAttendance`, which returns a copy of the event with the member's status set, and
   replaces the event with `Model#setEvent`.

The command changes data, so it runs through `executeAtomicCommand` as described above.

Other commands keep the attendance record consistent with the members:

| Change | Effect on attendance |
|---|---|
| `assign` adds members | Existing attendance is kept. New members are not marked. |
| `unassign` removes members | The removed members' attendance is removed. |
| `editevent` renames or reschedules the event | Attendance is kept. |
| `edit` renames a contact | The contact's attendance moves to the new name in every event. |
| `delete` deletes a contact | The contact's attendance is removed from every event. |

In the data file, [`JsonAdaptedEvent`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonAdaptedEvent.java)
stores attendance as an `attendance` object that maps each member's name to `"present"` or `"absent"`. A file with
no `attendance` field loads with no attendance recorded, so files saved by earlier versions still load. A file is
rejected as invalid if a status is neither value or if a name in `attendance` is not in the event's `members`.

`EventListPanel` shows **Present**, **Absent**, or **Not marked** beside each member of the selected event, and
the number of members with each status.

#### Design considerations

**Aspect: Where attendance is stored**

* **Alternative 1 (current choice):** Each `Event` stores a map from member name to status.
  * Pros: An event and its attendance are saved, copied, and replaced as one unit, so they cannot disagree.
    Removing a member and their attendance is a single change.
  * Cons: A contact's attendance across events can only be found by looking through every event.

* **Alternative 2:** Store attendance records in a separate list, each referring to an event and a contact.
  * Pros: Makes it easy to query one contact's attendance across all events.
  * Cons: Every change to an event or contact must also update the separate list, or records are left behind.

**Aspect: How "not marked" is represented**

* **Alternative 1 (current choice):** A member with no entry in the map is not marked.
  * Pros: Newly assigned members need no extra data, and older data files load without conversion.
  * Cons: Code that counts statuses must derive the not-marked count from the members.

* **Alternative 2:** Add a third `NOT_MARKED` status and give every member an entry.
  * Pros: Every member has an explicit status.
  * Cons: Every command that adds a member must also add an entry, and older data files would need converting.

### Filtering contacts

The `filter` command lists the contacts that match every given `FIELD/VALUE` pair, where the field is `tag`,
`event`, or `organisation`.

#### Implementation

Filtering is split into parsing, which does not need saved data, and resolving, which does.

**Parsing.** [`FilterCommandParser`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/parser/FilterCommandParser.java)
turns the arguments into a list of [`FilterCriterion`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterCriterion.java)
records, each holding a [`FilterField`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterField.java)
and the value as typed.

1. It finds every `FIELD/` token, which is a run of letters followed by `/` at the start of the arguments or after
   a space. A token is ignored if an odd number of quotation marks comes before it, since it is then inside a
   quoted value.
1. The value of each field is the text up to the next token. Surrounding quotation marks are removed.
1. The field name is matched to a `FilterField`, ignoring case.

An unclosed quotation mark is rejected only when a `FIELD/` token follows it, because it is then unclear where the
value ends. With no token after it, the quotation mark is kept as part of the value, since saved names may contain
one. For example, `filter event/"Cali` matches an event named `"Cali`.

**Resolving.** [`FilterCommand#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/commands/FilterCommand.java)
turns each criterion into a `Predicate<Person>` using the model:

| Field | The value must match, ignoring case | A contact matches if |
|---|---|---|
| `tag` | a tag in `Model#getTagList()` | it has that tag |
| `event` | the name of a saved event | it is a member of the event with that name |
| `organisation` | the organisation of at least one saved contact | its organisation is that value |

If a value matches nothing, the command throws a `CommandException` with
`No saved tag, event, or organisation matches that value. Use a value from the saved data.` before the displayed
list is changed. This tells the user about a mistyped value instead of showing an empty list. Otherwise
the predicates are combined into a [`ContactFilterPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/ContactFilterPredicate.java),
which a contact matches only if it matches every predicate, and passed to `Model#updateFilteredPersonList`.

`filter` does not change data, so it is executed directly on the live model.

<box type="info" seamless>

**Note:** The `event` predicate is the same `EventMembersPredicate` that the `members` command uses. It holds the
event's name and looks the event up each time it is tested, so the filtered list follows later changes to that
event, as described in [Keeping events in sync with contacts](#keeping-events-in-sync-with-contacts). The existence
check above is made only when `filter` is executed: if the event is deleted afterwards, the list becomes empty.
</box>

To add a filter field, add a constant to `FilterField`, add a case to `FilterCommand#resolvePredicate`, and update
`FilterCommandParser.MESSAGE_INVALID_FIELD`.

The `search` command is implemented separately. `SearchCommand` uses `ContactMatchesKeywordPredicate`, which tests
whether a contact's name, phone, email, address, or organisation contains the keyword, ignoring case. It compares
substrings with `String#regionMatches(true, ...)`, using the same case comparison as duplicate-name detection.
A phone number also matches when it contains the keyword after `Phone#normalize` removes spaces, hyphens, and
brackets from both, the same normalisation that `delete p/`, `edit target/`, and duplicate detection use.

#### Design considerations

**Aspect: Syntax of a filter**

* **Alternative 1 (current choice):** Full field names, as in `filter tag/EXCO event/Final Concert`.
  * Pros: The fields are self-explanatory, and a field is added by extending one enumeration.
  * Cons: Differs from the short prefixes of other commands, such as `t/`.

* **Alternative 2:** Reuse the existing prefixes, as in `filter t/EXCO evn/Final Concert`.
  * Pros: Consistent with other commands.
  * Cons: Some prefixes would mean different things in different commands, and organisation would need `o/`,
    which is easy to confuse with other single-letter prefixes.

**Aspect: What happens when a value does not exist**

* **Alternative 1 (current choice):** Reject the command with
  `No saved tag, event, or organisation matches that value. Use a value from the saved data.`
  * Pros: Distinguishes a mistyped value from a valid value that no contact has.
  * Cons: The command must look the value up in the model before filtering.

* **Alternative 2:** Show an empty list.
  * Pros: Simpler to implement.
  * Cons: The user cannot tell whether the value was mistyped.

### Keeping events in sync with contacts

An event refers to its members by contact `Name`, not by `Person` object. Contacts are replaced with new `Person`
objects whenever they are edited, so a name is the stable way to refer to one. In exchange, the app must keep the
names in events consistent with the saved contacts.

#### Implementation

**Renaming and deleting contacts.** [`CoordiMate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/CoordiMate.java)
updates events whenever a contact changes:

* `CoordiMate#setPerson` checks whether the contact's name has changed. If so, every event that lists the old name
  is replaced with a copy that lists the new name in the same position and carries the contact's attendance over.
* `CoordiMate#removePerson` replaces every event that lists the contact with a copy that has neither the name nor
  its attendance.

Both use `CoordiMate#getEventsWithReplacedMember`, which builds all the updated events before anything is changed.
As a safeguard, if an event already has a member with the new name, it throws `MemberNameConflictException` and no data is changed.
`EditCommand` reports this as `Cannot rename this contact because an event already has a member with that name.
No changes were made.`

**Loading data.** `JsonSerializableCoordiMate#toModelType` rejects a data file in which an event lists a member
who is not a saved contact, with `Event members must refer to existing contacts.`

**Showing an event's members.** `MembersCommand` filters the contact list with
[`EventMembersPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/EventMembersPredicate.java).
The predicate holds the event's name and the model's live event list, and looks the event up each time it is tested.
The displayed members therefore follow later changes:

* After `assign` or `unassign`, an added member matches and a removed member no longer does.
* After `deleteevent`, no event has that name, so the list becomes empty.
* After `editevent` renames the event, the old name would match nothing, so
  `LogicManager#keepEventViewsAfterRename` replaces the predicate with one for the new name.

`filter event/...` uses the same predicate as one of the criteria of a `ContactFilterPredicate`, so a filtered list
follows the same changes. For a rename, `keepEventViewsAfterRename` uses
`ContactFilterPredicate#withCriteriaReplaced` to replace only the event criteria for the renamed event, keeping the
other criteria unchanged.

A `filter tag/...` criterion is a `TagFilterPredicate`, which also identifies its tag by name. After `edittag` renames
that tag, `LogicManager#keepTagFiltersAfterRename` replaces it with a predicate for the new name in the same way, so
the filtered list keeps showing the contacts that have the renamed tag.

A filtered list re-tests its predicate only when the contact list changes. For this reason `CoordiMate#resetData`
replaces the events **before** the contacts: when the contacts are replaced and the predicate is tested again, it
already sees the new events.

#### Design considerations

**Aspect: How an event refers to its members**

* **Alternative 1 (current choice):** By contact name.
  * Pros: Readable in the data file, where `members` is a list of names. Names are already unique among contacts.
  * Cons: Renaming or deleting a contact must update every event, as described above.

* **Alternative 2:** By a reference to the `Person` object.
  * Pros: No lookup is needed to get a member's details.
  * Cons: `Person` is immutable and is replaced on every edit, so every event would need updating on any edit, not
    only on a rename.

* **Alternative 3:** By the contact's existing permanent member ID.
  * Pros: Renaming a contact would not affect events.
  * Cons: Requires migrating event member and attendance references from names to IDs in the data file, and
    looking up contacts to display member names.

### Marking contacts as on leave

A contact can be marked as on leave until a given date. The status ends by itself once that date has passed.

#### Implementation

[`LeaveDate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/LeaveDate.java)
holds the **last day** of a contact's leave, in `dd-MM-yyyy` format. `Person` stores it as an optional field, like
birthday and note, and exposes:

* `Person#getLeaveDate()`, which returns the saved date, even if it has passed.
* `Person#isOnLeave(LocalDate today)`, which returns true if a date is saved and `today` is not after it.

`LeaveDate` has two validity rules, in the same way that `Phone` separates saved values from newly entered ones:

* `LeaveDate#isValidLeaveDate` accepts any real date, including past dates, so that leave which has ended can still
  be loaded from the data file.
* `LeaveDate#isValidInputLeaveDate` also requires the date not to be before today. `ParserUtil#parseLeaveDate`
  applies this rule to dates entered with `lv/`, so new leave cannot already have ended.

The `lv/` prefix is handled like the other optional contact fields:

1. `AddCommandParser` sets the leave date if `lv/` has a value, and leaves it unset if `lv/` is empty or omitted.
1. `EditCommandParser` records the new date in `EditPersonDescriptor#setLeaveDate`. An empty `lv/` records
   `null`, which clears the leave. `EditPersonDescriptor#isLeaveDateEdited` distinguishes clearing from omitting.
1. `EditCommand#createEditedPerson` keeps the existing leave date unless it was edited.

**How leave ends.** Nothing is changed when a leave date passes. Instead, the UI asks each contact whether it is
on leave on the current date:

* `PersonCard` shows an **On leave until** badge only if `Person#isOnLeave(today)` is true.
* `PersonListPanel` shows the date in the details pane in the same case, and `— (not on leave)` otherwise.

`PersonListPanel` reads `today` from a `Clock`, which tests replace to control the date. A window left open past
midnight still needs redrawing, so `MainWindow`'s existing one-minute reminder timer also calls
`PersonListPanel#refreshIfDateChanged()`. If the date differs from the one last shown, the panel refreshes its list
cells, which rebuilds each `PersonCard`, and redraws the selected contact's details. Otherwise it does nothing, so
the list is redrawn at most once a day.

The ended date stays in the contact and in the data file until the user replaces or clears it. Code that copies a
contact, such as `CoordiMate#setTag` and `CoordiMate#removeTag`, passes the leave date on, so it is never dropped.

In the data file, [`JsonAdaptedPerson`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonAdaptedPerson.java)
stores the date in a `leaveUntil` field. Files saved before this feature have no such field, and their contacts
load as not on leave.

**Assertions and logging.** `ParserUtil#parseLeaveDate` asserts that the parsed leave covers today, which checks
that the input rule was applied. `AddCommand` and `EditCommand` log at `INFO` level when a contact's leave is set or
cleared.

#### Design considerations

**Aspect: How leave ends once its date has passed**

* **Alternative 1 (current choice):** Keep the date, and check it against the current date whenever a contact is
  displayed.
  * Pros: No data needs changing when leave ends, so no save is needed at launch or at midnight. The behaviour is
    easy to test by passing a fixed date to `Person#isOnLeave`.
  * Cons: Ended dates remain in the data file. A window left open past midnight must be redrawn, which needs the
    date check described below.

* **Alternative 2:** Remove ended leave dates from the data, for example at launch.
  * Pros: The data file only contains leave that is still active.
  * Cons: Requires saving at launch, which no other feature does, and still needs a check for leave that ends while
    the app is open.

**Aspect: How an open window notices that the date has changed**

* **Alternative 1 (current choice):** Check the date on each tick of the existing one-minute reminder timer.
  * Pros: Adds no new timer. Still works if the computer sleeps through midnight or its clock or time zone changes,
    because each tick compares the current date with the date last shown.
  * Cons: The badge can stay visible for up to a minute after midnight.

* **Alternative 2:** Schedule a timer for the next midnight, then reschedule it each day.
  * Pros: Updates exactly at midnight.
  * Cons: A second timer to start and stop with the window. A timer scheduled before a sleep, or a clock or time
    zone change, can fire at the wrong time.

**Aspect: How leave is entered**

* **Alternative 1 (current choice):** An optional `lv/` field in `add` and `edit`.
  * Pros: Consistent with the other optional contact fields, including clearing with an empty prefix.
  * Cons: Users must know that leave is set through `edit`.

* **Alternative 2:** A separate command, such as `leave MEMBER_ID until/DATE`.
  * Pros: Easier to discover.
  * Cons: Duplicates the contact lookup and validation already in `edit`, and adds a second way to change a contact.

### Archiving contacts and events

`archive`, `unarchive`, `archiveevent`, and `unarchiveevent` hide a contact or an event from the normal lists without
deleting it. `list archived` shows only archived items, and `list` returns to the active ones.

#### Implementation

**The archive flag.** [`Person`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/Person.java)
and [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java)
each have a boolean `archived` field, which is part of `equals` and `hashCode`. Both are immutable, so archiving
returns a copy from `withArchived(boolean)`:

* `Event#withArchived` builds the copy through the full constructor. Every other `Event` copy method, such as
  `withAttendance` and `withDetails`, passes the flag on.
* `Person` takes `archived` as the last parameter of its full constructor, the one that also takes a member ID.
  Every copy of an existing contact uses that constructor, so the compiler makes each copy pass the flag on:
  `Person#withArchived`, `Person#withMemberId`, `EditCommand#createEditedPerson`, and the tag updates in
  `CoordiMate#setTag` and `CoordiMate#deleteTag`. The shorter constructors create new contacts, which are never
  archived. `withMemberId` matters when loading a data file saved before member IDs existed: `CoordiMate#addPerson`
  gives each contact an ID, and the contact must stay archived.

Archiving changes nothing else: an archived contact keeps its name in event member lists, and an archived event
keeps its members and attendance.

**Hiding archived items.** [`ModelManager`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/ModelManager.java)
keeps the archive view, a `BooleanProperty` that is true while archived items are shown, separately from the
contact filter set by commands:

* The filtered contact list shows a contact only if its `archived` value matches the view **and** it matches the
  latest predicate passed to `updateFilteredPersonList`. `getFilteredPersonPredicate` still returns that predicate,
  so code such as `LogicManager#keepEventViewsAfterRename` sees the command's filter and not the combined test.
* A filtered event list, `Model#getFilteredEventList`, shows the events whose `archived` value matches the view.
  The **Events** tab and its count use this list. A contact's details pane uses the full event list instead, so that
  archived events still appear in the contact's history.
* `Model#setShowingArchived` switches both lists. `MainWindow` observes `showingArchivedProperty` to:
  * change the tab titles to **Archived contacts** and **Archived events**,
  * tell `PersonListPanel` and `EventListPanel` to show `No archived contacts.` or `No archived events.` for an empty
    list, and the event list heading to read **Archived events**, and
  * show short sidebar command lists, `archivedContactsCommands` and `archivedEventsCommands` in `MainWindow.fxml`,
    which list only the restore command and `list`, in place of the full lists for the **Contacts** and **Events**
    tabs.

Because the view is applied inside the model, `search`, `filter`, and `members` hide archived contacts without any
change to those commands.

The sequence diagram below shows `archive 12`. `ArchiveCommand` finds the contact by its permanent member ID with
`MemberIdResolver`, which searches all saved contacts, so the command works whatever the displayed list shows. Like
`edit`, it is one of the commands that `LogicManager` executes on a copy of the model, as described in
[Saving data atomically](#saving-data-atomically). `LogicManager` copies the current archive view onto that copy, so
that commands which read the displayed list see the same contacts on the copy as on the screen.

<puml src="diagrams/ArchiveSequenceDiagram.puml" width="700" />

**Effects on other features.**

* `ReminderService#refreshReminders` skips archived events, so archiving an event removes its reminder at once.
* `ReadOnlyCoordiMate#findEvent(String)` returns the saved event with a given name, ignoring case and including
  archived events. `ArchiveEventCommand`, `AddEventCommand`, and `EventMembersCommand` use it.
* The instance method `EventMembersCommand#findEvent`, used by `assign` and `unassign`, rejects an archived event.
  `MembersCommand`, `MarkAttendanceCommand`, `EditEventCommand`, and `DeleteEventCommand` search the full event list,
  so they still work on archived events.
* `AssignCommand` resolves its member IDs with `EventMembersCommand#resolvePersons` and rejects the whole command if
  any of the contacts is archived. `UnassignCommand` does not check, so an archived contact can still be removed from
  an event.
* The **Tags** member list in `MainWindow` and the event member list in `EventListPanel` mark archived contacts as
  `(archived)`, and `PersonListPanel` marks archived events in a contact's event list.
* `AddEventCommand` reports an archived duplicate with a hint to unarchive it, since the user cannot see the
  existing event.
* `ModelManager#addPerson` sets the view back to active, like the filter it already clears, and `LogicManager` does
  the same on the displayed model after `add` and `addevent`, so that the new item is visible. The first is needed
  because `AddCommand` looks up the new contact in the displayed list of the model copy to select it.

**Storage.** [`JsonAdaptedEvent`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonAdaptedEvent.java)
and
[`JsonAdaptedPerson`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonAdaptedPerson.java)
both read `archived` as a `JsonNode` creator parameter and reject non-boolean values during model conversion.
The rejection message names the contact or event when its saved name is valid and uses a fallback otherwise. Quoted
boolean strings are rejected. A missing or `null` value is read as `false`, so files saved before archiving existed
still load.

#### Design considerations

**Aspect: How archived items are hidden**

* **Alternative 1 (current choice):** The model combines the archive view with every contact filter.
  * Pros: Every command that filters contacts respects the view without changes. Future filters also respect it.
  * Cons: The filtered list's predicate is no longer exactly the one a command set, so `ModelManager` must keep the
    command's predicate separately for `getFilteredPersonPredicate`.

* **Alternative 2:** Each command adds `!person.isArchived()` to its own predicate.
  * Pros: The model stays unchanged.
  * Cons: Easy to forget in a new command, and `list archived` would have to change every command's behaviour.

**Aspect: How `archive` and `unarchive` identify a contact**

* **Alternative 1 (current choice):** By permanent member ID, as `edit`, `delete`, and `assign` do.
  * Pros: Consistent with the other contact commands. A contact can be restored from any view, and a search or
    filter cannot change which contact an ID refers to.
  * Cons: The user needs to know the ID, which is shown on each contact card.

* **Alternative 2:** By index in the displayed list.
  * Pros: Easy to use right after a search.
  * Cons: Restoring a contact would first need `list archived`, and indexes shift when the list changes.

**Aspect: Where archived items are stored**

* **Alternative 1 (current choice):** In the same lists, with an `archived` flag.
  * Pros: Names stay unique across active and archived items, events keep referring to archived contacts by name,
    and unarchiving only flips the flag.
  * Cons: Every list that should hide archived items must filter them out.

* **Alternative 2:** In separate archive lists.
  * Pros: The normal lists never contain archived items.
  * Cons: Duplicate checks, event member lookups, and the data file format would all have to handle two lists, and
    moving an item between lists could break the links between events and contacts.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* is a university CCA EXCO member managing information for one organisation
* needs to manage the organisation's contacts, events, and attendance records
* needs to preserve useful information for future committee members
* prefers desktop apps over other types of applications
* can type fast
* prefers typing to mouse interactions
* is reasonably comfortable using CLI apps

**Value proposition**: CoordiMate helps university CCA EXCO members organise contacts, coordinate events, track attendance, and preserve important information to support smooth committee handovers.


### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a … | I want to … | So that I can …                                                                                 |
|----------|--------|-------------|-------------------------------------------------------------------------------------------------|
| `* * *` | first-time user | view a tutorial | learn how to navigate CoordiMate                                                                |
| `* *` | returning user | access the tutorial again | refresh my memory when I forget how to use CoordiMate                                           |
| `* * *` | Exco member | save a contact's details | keep track of the people I know                                                                 |
| `* * *` | Exco member | edit a saved contact | correct or update the contact's information                                                     |
| `* * *` | Exco member | delete a contact | remove an entry that is no longer needed                                                        |
| `* * *` | Exco member | save a contact's email address | keep a complete record of the contact's information                                             |
| `* *` | Exco member | record additional details about a contact | keep relevant background information at hand                                                    |
| `* *` | Exco member | record a contact's birthday | remember to wish the contact well                                                               |
| `*` | Exco member | attach multiple phone numbers or email addresses to a contact | record the contact's different personal and work details                                        |
| `*` | Exco member | attach a photo to a contact | associate the contact's face with their name                                                    |
| `*` | Exco member entering data quickly | enter phone numbers containing extra whitespace or dashes | avoid manually reformatting phone numbers                                                       |
| `*` | Exco member entering data quickly | enter Telegram handles with or without the `@` symbol | avoid manually reformatting handles                                                             |
| `* * *` | event lead | search and filter contacts by name, tag, or role | retrieve specific contact information quickly                                                   |
| `* *` | Exco member | filter contacts by their assigned colour | search contacts in a particular group quickly                                                   |
| `*` | Exco member | have urgent contacts appear at the top of the contact list | identify them quickly                                                                           |
| `* *` | experienced user | colour-code contacts by role or group | identify their roles or groups quickly                                                          |
| `* *` | Exco member | assign a colour to a contact | distinguish that contact visually                                                               |
| `*` | Exco member | choose from a set of default colours | assign colours without configuring them myself                                                  |
| `*` | Exco member | change a contact's assigned colour | keep the colour accurate when the contact's role or group changes                               |
| `* * *` | Exco member | assign tags to contacts | categorize contacts for later retrieval                                                         |
| `* *` | Exco member | choose from a set of common default tags | categorize contacts quickly                                                                     |
| `* *` | Exco member | create custom tags | categorize contacts in ways not covered by the default tags                                     |
| `* * *` | Exco member | create an event with a name and date | keep a record around which I can organize contacts and attendance                               |
| `* * *` | Exco member | assign a member to an event | track who is involved in that event                                                             |
| `* * *` | Exco member | remove a member from an event | correct the assignment when someone is no longer involved                                       |
| `* * *` | Exco member | view the members assigned to an event | see the committee allocation at a glance                                                        |
| `* *` | Exco member | create future events and assign members to them | plan committee allocation ahead of time                                                         |
| `*` | user with limited time | have the most frequently used event appear first | access it without extra navigation                                                              |
| `*` | forgetful Exco member | receive reminders for upcoming events | avoid missing them                                                                              |
| `*` | forgetful Exco member | see everyone assigned to an event | avoid overlooking a participant                                                                 |
| `* * *` | Exco member who organizes events | track attendance for an event | monitor participation                                                                           |
| `* * *` | Exco member | mark a member as present or absent for an event | keep an accurate attendance record                                                              |
| `* *` | Exco member | view the counts of present and absent members for an event | gauge overall turnout quickly                                                                   |
| `* *` | Exco member | view the members marked absent for an event | follow up with them individually                                                                |
| `*` | Exco member | view the edit history of attendance records | see when a member's status was changed and avoid disputes                                       |
| `*` | Exco member | export attendance records | use them for reporting or record-keeping                                                        |
| `*` | Exco member | archive contacts | declutter my active contact list without permanently deleting entries                           |
| `*` | Exco member | archive past events | keep my event list focused on current and upcoming events                                       |
| `*` | Exco member | access archived contacts and events | retrieve historical information when needed                                                     |
| `*` | Exco member | mark a contact as on leave until a specified date | remember that the contact is temporarily unavailable without manually clearing the status later |
| `* *` | Exco member | switch between the Contacts and Events views | navigate between contact and event information easily                                           |
| `*` | user who values a pleasant interface | switch between light and dark modes | adjust the display to suit my environment                                                       |

### Use cases

(For all use cases below, the **System** is the `CoordiMate` and the **Actor** is the `user`, unless specified otherwise)

**Use case: Add a contact**

**System:** CoordiMate

**Actor:** University CCA EXCO member

**Preconditions:** CoordiMate is running.

**Main success scenario (MSS)**

1. EXCO member requests to add a contact, providing the contact's name, phone number, email address, role, and any applicable optional details.
2. CoordiMate validates the supplied fields, registers any new custom tags, saves the contact, and confirms
   that the contact was saved.

    Use case ends.

**Extensions**

* 1a. A required detail is missing, empty, repeated, or invalid, or the EXCO member provides an unknown field.

  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member corrects the contact details.

    Steps 1a1-1a2 are repeated until the contact details are valid.<br>
    Use case resumes from step 2.

* 1b. A contact with the same name (ignoring case and surrounding spaces), normalised phone number,
  or email address already exists.

  * 1b1. CoordiMate informs the EXCO member that the contact already exists and makes no changes.

    Use case ends.

* 2a. CoordiMate cannot write to the local data file.

  * 2a1. CoordiMate informs the EXCO member that the contact could not be saved and makes no changes.

    Use case ends.

* 2b. CoordiMate cannot load the local data file because it is invalid or corrupted.

  * 2b1. CoordiMate informs the EXCO member that the contact data could not be loaded and makes no changes.

    Use case ends.

**Use case: Search contacts**

**System:** CoordiMate

**Actor:** Event lead

**Preconditions:** CoordiMate has been launched and contains at least one contact.

**MSS**

1. Event lead requests to search contacts using one keyword or phrase.
2. CoordiMate displays contacts whose name, phone number, email address, address, or organisation contains that
   keyword, ignoring case. Spaces, hyphens, and brackets in phone numbers are ignored when matching.
   Extra spaces in the keyword and in addresses are treated as one space.

   Use case ends.

**Extensions**

* 1a. Event lead provides an empty keyword, including a blank quoted phrase.

  * 1a1. CoordiMate displays an error message.

    Use case ends.

* 2a. No contact matches the keyword.

  * 2a1. CoordiMate informs the event lead that no matching contacts were found.

    Use case ends.

**Use case: Delete a contact**

**System:** CoordiMate

**Actor:** University CCA EXCO member

**Preconditions:** CoordiMate is running and at least one contact has been saved.

**Main success scenario (MSS)**

1. EXCO member requests to delete a contact, identifying the contact by its permanent member ID, exact name, phone number, or email address.
2. CoordiMate displays the matching contact and requests confirmation.
3. EXCO member confirms the deletion.
4. CoordiMate saves a model without the selected contact, their assignments in any event roster, or their
   associated attendance records, then updates the live model and confirms the deletion. Events and other contacts
   remain unchanged.

   Use case ends.

**Extensions**

* 1a. The EXCO member provides no identifier, more than one identifier, an unknown field, or an invalid member ID.

  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member provides one valid identifier.

    Use case resumes from step 2.

* 1b. No contact matches the provided identifier.

  * 1b1. CoordiMate informs the EXCO member that no matching contact was found.

    Use case ends.

* 1c. More than one contact matches the provided name.

  * 1c1. CoordiMate requests a phone number, email address, or member ID to identify the contact.
  * 1c2. EXCO member provides one of the requested identifiers.

    Use case resumes from step 2.

* 3a. EXCO member declines the deletion.

  * 3a1. CoordiMate leaves the contact unchanged. Only `y` or `Y` confirms; any other input cancels and is consumed.

    Use case ends.

**Use case: Create an event**

**Preconditions:** CoordiMate is running.

**MSS**

1. The Exco member creates an event by entering its name, start date/time, and end date/time using the `addevent` command.
2. CoordiMate validates the details and creates the event.
3. CoordiMate displays a confirmation with the event name and its start and end times.

   Use case ends.

**Extensions**

* 1a. The event name is empty, a date/time is missing or incorrectly formatted, or a required parameter is repeated.
  * 1a1. CoordiMate displays the relevant error.
  * 1a2. The Exco member corrects the details and retries step 1.

* 1b. An event with that name already exists.
  * 1b1. CoordiMate reports that duplicate event names are not allowed.
  * 1b2. The Exco member chooses a different name and retries step 1.

* 2a. CoordiMate cannot save the event because the local data file cannot be written or is invalid.
  * 2a1. CoordiMate displays the relevant error and does not create the event.

    Use case ends.

**Use case: Assign members to an event**

**Preconditions:** CoordiMate is running. The event and the contacts to be assigned already exist.

**MSS**

1. The Exco member assigns one or more contacts to an event using the `assign` command, specifying the event name and the permanent member IDs of the contacts.
2. CoordiMate verifies the event and contacts, then associates the contacts with the event.
3. CoordiMate confirms how many members were assigned.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name, or a member ID is invalid or unknown.
  * 1a1. CoordiMate displays the relevant error and assigns no contacts.
  * 1a2. The Exco member corrects the event name or member ID and retries step 1.

* 1b. No member IDs are provided.
  * 1b1. CoordiMate reports that at least one contact must be specified.
  * 1b2. The Exco member provides one or more member IDs and retries step 1.

* 2a. A contact is already assigned, or the same member ID appears more than once.
  * 2a1. CoordiMate avoids creating duplicate assignments and reports the number of newly assigned members.

* 2b. Every specified contact is already assigned.
  * 2b1. CoordiMate reports that no changes were made.

    Use case ends.

* 2c. CoordiMate cannot save the assignment to the local data file.
  * 2c1. CoordiMate displays the relevant error and assigns no contacts.

    Use case ends.

**Use case: Remove members from an event**

**Preconditions:** CoordiMate is running. The event exists and has at least one member.

**MSS**

1. The Exco member removes one or more members from an event using the `unassign` command, specifying the event name and the permanent member IDs of the contacts.
2. CoordiMate verifies the event and contacts, then removes the contacts from the event's members, together with their attendance records for that event.
3. CoordiMate confirms how many members were removed.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name, or a member ID is invalid or unknown.
  * 1a1. CoordiMate displays the relevant error and removes no members.
  * 1a2. The Exco member corrects the event name or member ID and retries step 1.

* 1b. No member IDs are provided.
  * 1b1. CoordiMate reports that at least one contact must be specified.
  * 1b2. The Exco member provides one or more member IDs and retries step 1.

* 2a. A specified contact is not assigned to the event, or the same member ID appears more than once.
  * 2a1. CoordiMate skips those contacts and reports the number of members removed.

* 2b. None of the specified contacts are assigned to the event.
  * 2b1. CoordiMate reports that no changes were made.

    Use case ends.

* 2c. CoordiMate cannot save the change to the local data file.
  * 2c1. CoordiMate displays the relevant error and removes no members.

    Use case ends.

**Use case: View members of an event**

**Preconditions:** CoordiMate is running. The event exists.

**MSS**

1. The Exco member requests to view the members of an event using the `members` command, specifying the event name.
2. CoordiMate switches to the contact list and shows only the event's members, with their contact details.
3. CoordiMate confirms how many members are shown.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name, or the command format is invalid.
  * 1a1. CoordiMate displays the relevant error and leaves the contact list unchanged.
  * 1a2. The Exco member corrects the command and retries step 1.

* 2a. The event has no members.
  * 2a1. CoordiMate shows an empty contact list and reports that the event has no members.

    Use case ends.

* 3a. The Exco member removes a shown member from the event, or renames the event.
  * 3a1. CoordiMate updates the shown list to the event's current members.

    Use case resumes from step 3.

* 3b. The Exco member wants to see all contacts again.
  * 3b1. The Exco member uses the `list` command, and CoordiMate shows all contacts.

    Use case ends.

**Use case: Record event attendance**

**Preconditions:** CoordiMate is running. The event exists and has at least one member assigned to it.

**MSS**

1. The Exco member requests to view the list of members assigned to an event (use case: View members of an event).
2. CoordiMate displays the list of members for that event.
3. The Exco member marks a member's attendance status as present or absent using the `markattendance` command.
4. CoordiMate records the attendance status and confirms the update.
5. Steps 3-4 are repeated for each member whose attendance is to be recorded.

   Use case ends.

**Extensions**

* 1a. The event does not exist.
  * 1a1. CoordiMate displays the relevant error.

    Use case ends.

* 2a. No members are assigned to the event.
  * 2a1. CoordiMate informs the Exco member that the event has no members.

    Use case ends.

* 3a. The specified member is not assigned to the event.
  * 3a1. CoordiMate displays the relevant error.
  * The Exco member retries step 3.

* 3b. The attendance status given is neither "present" nor "absent".
  * 3b1. CoordiMate displays an error listing the valid status values.
  * The Exco member retries step 3.

* 3c. The member already has the specified attendance status recorded.
  * 3c1. CoordiMate informs the Exco member that no change was made.

    Use case resumes from step 5.

* 4a. CoordiMate cannot save the updated attendance record to the local data file.
  * 4a1. CoordiMate displays the relevant error and does not update the displayed status.

    Use case resumes from step 5.

### Non-Functional Requirements

1.  Should work on any _mainstream OS_ (Windows, macOS, Linux) as long as it has Java `25` or above installed.
2.  Should be able to hold up to 250 contacts and 6 events per semester without a noticeable sluggishness in performance for typical usage.
3.  A user with above average typing speed for regular English text (i.e. not code, not system admin commands) should be able to accomplish most of the tasks faster using commands than using the mouse.
4.  Should respond to any user command within 2 seconds under typical load (up to 250 contacts, 6 events).
5.  Should not require an internet connection to operate, since all contact, event, and attendance data is stored and managed locally.
6.  Should not lose previously saved data if the application terminates unexpectedly (e.g. crash, power loss) mid-operation.
7.  Data should be stored in a human-editable, non-proprietary file format (not a DBMS), so users can back up, inspect, or migrate their data manually.
8.  Should be shipped as a single JAR file that does not require a separate installer.
9.  A first-time user with no prior experience should be able to complete the in-app tutorial and add their first contact within 10 minutes.
10. The colour-coding feature should not be the sole means of distinguishing contact categories, so the app remains usable by colour-blind users (e.g. tags are also shown as text).

### Glossary

* **Archive**: To move a contact or event out of the active list without permanently deleting it, so that it can still be retrieved later.
* **Assigned member**: A contact who has been assigned to a particular event. Also referred to as a *member* of that event.
* **Attendance status**: Whether an assigned member was `present` or `absent` at an event.
* **CCA (Co-Curricular Activity)**: A student club or society in a university, such as a band, sports team or interest group.
* **CLI (Command Line Interface)**: A way of using the app by typing text commands, as opposed to clicking buttons in a graphical interface.
* **Colour-code**: To assign a colour to a contact so that contacts of the same role or group can be recognised visually.
* **Committee handover**: The transfer of responsibilities and information from an outgoing EXCO to the incoming EXCO.
* **Contact**: A person saved in CoordiMate, with details such as name, phone number, email address and role.
* **Custom tag**: A tag created by the user, to categorise contacts in ways not covered by the default tags.
* **Default tag**: A tag that is available without the user creating it: `EXCO`, `Sponsor`, `UniversityStaff` and `Logistics`.
* **Member ID**: A unique, permanent positive integer assigned to a contact, displayed as `ID: <number>`, and used to identify them in commands across searches, filters, and restarts.
* **Duplicate contact**: A contact with the same name (ignoring case and surrounding spaces), normalised phone number, or email address as another saved contact.
* **Event**: An activity organised by the CCA, such as a concert or fair, with a unique name, a start date/time and an end date/time.
* **Event lead**: An EXCO member who is in charge of organising a particular event.
* **EXCO (Executive Committee)**: The group of students elected to lead and run a CCA.
* **External contact**: A contact from outside the CCA, such as a sponsor, university staff member, or EXCO member of a collaborating CCA.
* **Future event**: An event whose start date/time is later than the current date/time.
* **Internal contact**: A contact from within the CCA, such as a member, fellow EXCO member or advisor.
* **JAR file**: A single file that packages a Java application so that it can be run without installation.
* **Local data file**: The file on the user's computer where CoordiMate automatically saves all contacts, events and attendance records.
* **Mainstream OS**: Windows, Linux, Unix, or macOS
* **Normalised phone number**: A phone number with spaces, hyphens and brackets removed, used to detect duplicates. For example, `+65 9123 4567` and `+6591234567` are the same.
* **On leave**: A temporary status marking a contact as unavailable until a specified date, after which it is cleared automatically.
* **Organisation**: The external company or body that a contact belongs to, such as a sponsor company or university office.
* **Prefix**: The short label before a value in a command that tells CoordiMate which field it belongs to, such as `n/` for name or `t/` for tag.
* **Role**: A contact's responsibility or position in relation to the CCA, such as `Logistics Lead` or `Vice-President`.
* **Tag**: A short label attached to a contact to categorise it, such as `EXCO` or `Sponsor`. A contact can have multiple tags.
* **Telegram handle**: A user's username on the Telegram messaging app, such as `@aishatan`.
* **Tutorial**: An in-app guide that introduces a first-time user to CoordiMate's key features.
* **Typical usage**: Using CoordiMate with up to 250 contacts and 6 events, as described in the NFRs.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<box type="info" seamless>

**Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens maximized with a set of sample contacts and a sample event. The first-launch tutorial
      can show the event's assigned members and attendance information.

1. Saving window preferences

   1. Unmaximize the window. Resize it to a distinctive size, move it to a different location, and close it.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The app opens unmaximized with the most recent window size and location.

1. Shutting down

   1. Test case: `exit`<br>
      Expected: The app closes. Relaunching it shows the same data as before.

<box type="info" seamless>

**Note:** Several tests below start from the sample contacts. To restore them, close the app, delete
`data/coordimate.json` from the folder containing the JAR file, and relaunch the app.
</box>

### Deleting a contact

1. Deleting a contact while all contacts are being shown

   1. Prerequisites: Restore the sample contacts and run `list`. Alex Yeoh has member ID `1`.

   1. Test case: `delete 1`, then `n`<br>
      Expected: `ID: 1` and Alex Yeoh's name, phone, email, and role are shown, followed by `Confirm deletion? [y/N]`. After `n`, the status message shows `Deletion cancelled. No changes were made.` and no contact is deleted.

   1. Test case: `delete 1`, then `y ` (with a trailing space)<br>
      Expected: Alex Yeoh (member ID `1`) is deleted. Other contacts keep their IDs. The status message shows `Contact deleted successfully: ` followed by the contact's name.

   1. Test case: `delete 0`<br>
      Expected: An error is shown. No confirmation is requested and no contact is deleted.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `delete 99` (where member ID `99` does not exist)<br>
      Expected: An error is shown. No confirmation is requested and no contact is deleted.

### Searching contacts

1. Searching the sample contacts

   1. Prerequisites: The sample contacts are loaded.

   1. Test case: `search alex`<br>
      Expected: Only `Alex Yeoh` is listed. The status message shows `1 contact(s) found matching "alex".`

   1. Test case: `search serangoon`<br>
      Expected: `Bernice Yu` and `David Li` are listed, matched by their addresses.

   1. Test case: `search "Serangoon Gardens"`<br>
      Expected: The same two contacts are listed.

   1. Test case: `search 9927`<br>
      Expected: Only `Bernice Yu` is listed, matched by phone number.

   1. Test case: `search example.com`<br>
      Expected: All six contacts are listed, matched by email address.

   1. Test case: `search zzz`<br>
      Expected: The list is empty. The status message shows `No contacts found matching "zzz".`

   1. Test case: `search`<br>
      Expected: An error is shown and the list is unchanged.

1. Searching by organisation

   1. Prerequisites: Run `add n/Mr Lim p/90801110 e/lim@example.com o/NUS Student Affairs r/University Staff t/UniversityStaff`.

   1. Test case: `search student affairs`<br>
      Expected: Only `Mr Lim` is listed.

### Filtering contacts

1. Filtering by tag, event, and organisation

   1. Prerequisites: Restore the sample data (contact IDs `1` to `6`), with no additional contacts, events, or custom
      tags. Run these commands in order:<br>
      `add n/Aisha Tan p/+6591234567 e/aisha@example.com r/Logistics t/EXCO`<br>
      `add n/Mr Lim p/90801110 e/lim@example.com o/NUS Student Affairs r/University Staff t/UniversityStaff`<br>
      `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
      `assign evn/Final Concert c/1 7`<br>
      `addtag t/Publicity`

   1. Test case: `filter tag/exco`<br>
      Expected: `Alex Yeoh`, `Bernice Yu`, `Irfan Ibrahim`, and `Aisha Tan` are listed, in that order. The status
      message shows `4 contact(s) found.`

   1. Test case: `filter event/"Final Concert"`<br>
      Expected: `Alex Yeoh` and `Aisha Tan` are listed.

   1. Test case: `filter event/Final Concert tag/EXCO`<br>
      Expected: `Alex Yeoh` and `Aisha Tan` are listed, as both have the `EXCO` tag and belong to `Final Concert`.

   1. Test case: `filter organisation/NUS Student Affairs`<br>
      Expected: Only `Mr Lim` is listed.

   1. Test case: `filter organisation/NUS`<br>
      Expected: An error is shown because no saved organisation matches. Only `Mr Lim` remains listed.

   1. Test case: `filter tag/Publicity`<br>
      Expected: The list is empty, as the tag exists but no contact has it. The status message shows `No contacts match the specified filters.`

   1. Test case: `filter role/Logistics`<br>
      Expected: An error is shown for the unsupported field. The list stays empty.

   1. Other incorrect filter commands to try: `filter`, `filter tag/`, `filter event/"Final Concert tag/EXCO`<br>
      Expected: An error is shown and the list is unchanged.

### Adding an event

1. Adding events

   1. Prerequisites: No event named `Final Concert` or `Fair` exists.

   1. Test case: `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
      Expected: The **Events** tab opens with the new event selected. The status message shows `Created Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 18:00.`

   1. Test case: `addevent evn/final concert st/09-10-2026 et/10-10-2026`<br>
      Expected: An error is shown for the duplicate name. No event is added.

   1. Test case: `addevent evn/Fair st/10-10-2026 et/09-10-2026`<br>
      Expected: An error is shown because the start is after the end. No event is added.

   1. Test case: `addevent evn/Fair st/10/10/2026 et/11-10-2026`<br>
      Expected: An error shows the required date and time format. No event is added.

   1. Other incorrect addevent commands to try: `addevent`, `addevent evn/Fair st/10-10-2026`, `addevent evn/Fair st/10-10-2026 st/10-10-2026 et/11-10-2026`<br>
      Expected: An error is shown and no event is added.

### Editing an event

1. Editing an existing event

   1. Prerequisites: Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`. Select it in the **Events** tab.

   1. Test case: `editevent evn/final concert st/08-08-2026 16:00 et/08-08-2026 19:00`<br>
      Expected: The event's details show the new times. The status message shows `Edited Event Final Concert. Start Time: 08-08-2026 16:00. End Time: 08-08-2026 19:00.`

   1. Test case: `editevent evn/Final Concert nevn/Grand Concert`<br>
      Expected: The event is renamed to `Grand Concert` and keeps its times.

   1. Test case: `editevent evn/Grand Concert st/09-08-2026`<br>
      Expected: An error is shown because the start is after the end. The event is unchanged.

   1. Test case: `editevent evn/Gala nevn/Ball`<br>
      Expected: An error is shown for the unknown event. No data changes.

   1. Other incorrect editevent commands to try: `editevent evn/Grand Concert`, `editevent evn/Grand Concert nevn/`<br>
      Expected: An error is shown and no data changes.

### Deleting an event

1. Deleting an existing event

   1. Prerequisites: An event named `Grand Concert` exists, as created in the previous section.

   1. Test case: `deleteevent evn/Gala`<br>
      Expected: An error is shown for the unknown event. No event is deleted.

   1. Test case: `deleteevent evn/grand concert`<br>
      Expected: The event is removed from the **Events** tab. The status message shows `Deleted Event Grand Concert.`

   1. Other incorrect deleteevent commands to try: `deleteevent`, `deleteevent evn/`<br>
      Expected: An error is shown and no event is deleted.

### Dismissing a reminder

1. Selecting by reminder order and saving the dismissal

   1. Prerequisites: Use test data with no active reminders. Replace `TOMORROW` below with tomorrow's date in
      `dd-MM-yyyy` format, using the computer's local time zone. Create `Zulu Reminder` first with
      `addevent evn/Zulu Reminder st/TOMORROW et/TOMORROW`, then create `alpha Reminder` with the same dates.
      The panel should show `alpha Reminder` at index 1 and `Zulu Reminder` at index 2.

   1. Test case: Select the **Tags** view, then enter `dismiss 1`.<br>
      Expected: `alpha Reminder` disappears, `Zulu Reminder` becomes index 1, and feedback shows
      `Reminder dismissed: alpha Reminder.` with **OK** status. The **Tags** tab stays selected and the
      command input keeps focus. Both events remain in the **Events** view with unchanged details.

   1. Test case: Close and reopen the app while the test events still qualify for reminders.<br>
      Expected: Only `Zulu Reminder` appears in the panel, at index 1. `alpha Reminder` remains saved.

   1. Test case: `dismiss 2`<br>
      Expected: An error is shown and no reminder is dismissed.

   1. Test case: `dismiss 1`, then `dismiss 1` again.<br>
      Expected: The first command dismisses `Zulu Reminder` and hides the empty panel. The second command
      reports that no reminder exists at index 1. Both events remain saved.

1. Rejecting invalid input

   1. Prerequisites: At least one reminder is shown.

   1. Test cases: `dismiss`, `dismiss 0`, `dismiss -1`, `dismiss abc`, `dismiss 1.5`, `dismiss 1 2`,
      `dismiss 2147483648`<br>
      Expected: Feedback shows **Rejected** with an error and `dismiss` usage. No reminders or saved events change.

### Assigning members to an event

1. Assigning contacts while all persons are being shown

   1. Prerequisites: Restore the sample contacts and run `list`; Alex Yeoh, Bernice Yu, and Charlotte Oliveiro have IDs `1`, `2`, and `3`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`. Select it in the **Events** tab.

   1. Test case: `assign evn/Final Concert c/1 2`<br>
      Expected: The status message shows `Assigned 2 member(s) to Final Concert.` The event's details list Alex Yeoh (`ID: 1`) and Bernice Yu (`ID: 2`) under **Members (2)**.

   1. Test case: `assign evn/final concert c/2 3`<br>
      Expected: Only Charlotte Oliveiro (`ID: 3`) is added. The status message also shows `1 contact(s) were already assigned.`

   1. Test case: `assign evn/Final Concert c/1`<br>
      Expected: No change. The status message shows that all specified contacts are already assigned.

   1. Test case: `assign evn/Gala c/1`<br>
      Expected: An error is shown for the unknown event. No data changes.

   1. Other incorrect assign commands to try: `assign evn/Final Concert c/0`, `assign evn/Final Concert c/99` (where member ID `99` does not exist), `assign evn/Final Concert c/1 c/2`, `assign evn/Final Concert`<br>
      Expected: An error is shown and no data changes.

1. Assigning contacts from a filtered list

   1. Prerequisites: The sample contacts are loaded. Create an empty event using `addevent evn/Filtered Fair st/08-08-2026 et/08-08-2026`. Run `search Charlotte`, showing only `ID: 3`.

   1. Test case: `assign evn/Filtered Fair c/3`<br>
      Expected: Charlotte Oliveiro is assigned with `ID: 3` unchanged, even though she is the first result.

   1. Test case: `assign evn/Filtered Fair c/1`<br>
      Expected: Alex Yeoh (`ID: 1`) is assigned despite being hidden by the search. Charlotte stays assigned.

### Removing members from an event

1. Removing members while all persons are being shown

   1. Prerequisites: Restore the sample contacts (IDs `1` to `6`) and run `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/1 2 3`. Select the event in the **Events** tab.

   1. Test case: `unassign evn/final concert c/2 4`<br>
      Expected: The status message shows `Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.` The event's details list member IDs `1` and `3` under **Members (2)**, in that order.

   1. Test case: `unassign evn/Final Concert c/4`<br>
      Expected: No change. The status message shows that none of the specified contacts are assigned.

   1. Test case: `unassign evn/Gala c/1`<br>
      Expected: An error is shown for the unknown event. No data changes.

   1. Other incorrect unassign commands to try: `unassign evn/Final Concert c/0`, `unassign evn/Final Concert c/1 99` (where member ID `99` does not exist), `unassign evn/Final Concert c/1 c/3`, `unassign evn/Final Concert`<br>
      Expected: An error is shown and no data changes.

1. Removing members from a filtered list

   1. Prerequisites: The `Final Concert` event has member IDs `1` and `3` assigned. Run `search Charlotte`, showing only `ID: 3`.

   1. Test case: `unassign evn/Final Concert c/3`<br>
      Expected: Charlotte Oliveiro (`ID: 3`) is removed. Alex Yeoh (`ID: 1`) remains assigned.

### Viewing members of an event

1. Viewing the members of an event

   1. Prerequisites: Restore the sample contacts (IDs `1` to `6`) and run `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/3 1`. Switch to the **Events** view.

   1. Test case: `members evn/final concert`<br>
      Expected: CoordiMate switches to the **Contacts** view and shows Alex Yeoh (`ID: 1`) and Charlotte Oliveiro (`ID: 3`), in contact list order. The status message shows `Listed 2 member(s) of Final Concert. Use list to show all contacts.`

   1. Test case: `members evn/Gala`<br>
      Expected: An error is shown for the unknown event. The contact list is unchanged.

   1. Other incorrect members commands to try: `members`, `members evn/`, `members evn/Final Concert c/1`, `members evn/Final Concert evn/Fair`<br>
      Expected: An error is shown and the contact list is unchanged.

1. Keeping the shown members up to date

   1. Prerequisites: The members of `Final Concert` are shown, as above.

   1. Test case: `unassign evn/Final Concert c/1`<br>
      Expected: Alex Yeoh disappears from the list straight away. Charlotte Oliveiro keeps `ID: 3` as the only result.

   1. Test case: `editevent evn/Final Concert nevn/Grand Concert`<br>
      Expected: The list still shows the event's members.

   1. Test case: `deleteevent evn/Grand Concert`, then `list`<br>
      Expected: The list becomes empty after the event is deleted, then shows all persons after `list`.

### Marking attendance

1. Marking and changing attendance

   1. Prerequisites: The sample contacts are loaded and listed using `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/1 2`. Select the event in the **Events** tab.

   1. Test case: `markattendance evn/Final Concert mem/Alex Yeoh att/present`<br>
      Expected: `Alex Yeoh` shows **Present**. The counts show 1 present, 0 absent, and 1 not marked. The status message shows `Marked Alex Yeoh as present for Event Final Concert.`

   1. Test case: `markattendance evn/final concert mem/alex yeoh att/ABSENT`<br>
      Expected: `Alex Yeoh` shows **Absent**. The counts show 0 present, 1 absent, and 1 not marked.

   1. Test case: Repeat the previous command.<br>
      Expected: No change. Feedback reports that `Alex Yeoh` is already marked absent.

   1. Test case: `markattendance evn/Final Concert mem/David Li att/present`<br>
      Expected: An error is shown because David is not assigned. Attendance is unchanged.

   1. Test case: `markattendance evn/Gala mem/Alex Yeoh att/present`<br>
      Expected: An error is shown for the unknown event. Attendance is unchanged.

   1. Other incorrect markattendance commands to try: `markattendance evn/Final Concert mem/Alex Yeoh att/late`, `markattendance evn/Final Concert mem/Alex Yeoh`, `markattendance evn/Final Concert mem/Alex Yeoh att/present att/absent`<br>
      Expected: An error is shown and attendance is unchanged.

1. Keeping members and attendance in sync with contacts

   1. Prerequisites: Continue from the previous test, with `Alex Yeoh` marked absent. List all contacts using `list`.

   1. Test case: `assign evn/Final Concert c/3`<br>
      Expected: Charlotte Oliveiro (`ID: 3`) is added as **Not marked**. `Alex Yeoh` is still **Absent**.

   1. Test case: `editevent evn/Final Concert et/08-08-2026 20:00`<br>
      Expected: The end time changes. `Alex Yeoh` is still **Absent**.

   1. Test case: `edit 1 n/Alex Tan`<br>
      Expected: The event's members show `Alex Tan` in the same position, still **Absent**.

   1. Test case: `unassign evn/Final Concert c/1`, then `assign evn/Final Concert c/1`<br>
      Expected: `Alex Tan` is removed, then added again at the end of the members as **Not marked**.

   1. Test case: `markattendance evn/Final Concert mem/Alex Tan att/present`, then close and relaunch the app.<br>
      Expected: `Alex Tan` is still **Present**.

   1. Test case: `delete 1`, then `y`. Close and relaunch the app.<br>
      Expected: `Alex Tan` and his attendance are removed from the event. Other members are unchanged.

### Marking a contact as on leave

<box type="info" seamless>

**Note:** In these tests, `FUTURE_DATE` is any date after today in `dd-MM-yyyy` format, `TODAY` is today's date,
and `PAST_DATE` is any date before today, such as `01-01-2020`.
</box>

1. Setting and clearing leave

   1. Prerequisites: The sample contacts are loaded and listed using `list`.

   1. Test case: `edit 1 lv/FUTURE_DATE`<br>
      Expected: The card of the contact with member ID `1` shows **On leave until FUTURE_DATE**. Selecting the contact shows `FUTURE_DATE` under **On leave until**.

   1. Test case: `edit 1 lv/TODAY`<br>
      Expected: The contact is still shown as on leave, now until today.

   1. Test case: `edit 1 lv/`<br>
      Expected: The badge disappears and the details pane shows `— (not on leave)`.

   1. Test case: `add n/Ben Ong p/91112222 e/ben@example.com r/Publicity lv/FUTURE_DATE`<br>
      Expected: The new contact is added and shown as on leave.

   1. Test case: `edit 1 lv/PAST_DATE`<br>
      Expected: An error is shown for the past date. The contact is unchanged.

   1. Other incorrect commands to try: `edit 1 lv/31-11-2030`, `edit 1 lv/next week`, `edit 1 lv/FUTURE_DATE lv/FUTURE_DATE`<br>
      Expected: An error is shown and the contact is unchanged.

1. Leave ending by itself

   1. Prerequisites: Run `edit 1 lv/FUTURE_DATE`, then close the app.

   1. Test case: In `data/coordimate.json`, change that contact's `leaveUntil` value to `PAST_DATE`, save, then launch the app.<br>
      Expected: The app loads the data. The contact is not shown as on leave, and the details pane shows `— (not on leave)`. The `leaveUntil` value is still in the data file.

   1. Test case: Close the app, change `leaveUntil` to `31-02-2030`, save, then launch the app.<br>
      Expected: CoordiMate warns that the saved data could not be loaded and that the original file is unchanged.
      The warning shows the file path. Selecting **OK** closes the app. Restore the file before launching again.

   1. Test case: Run `edit 1 lv/TODAY` and select that contact. Leave the app open past midnight, or set your
      computer's date to the next day.<br>
      Expected: Within a minute, without running a command, the badge disappears and the details pane shows
      `— (not on leave)`.

### Managing tags

1. Adding, renaming, and deleting tags

   1. Prerequisites: The sample contacts are loaded and no additional custom tags have been created.

   1. Test case: Press <kbd>Esc</kbd>, then <kbd>S</kbd> until the Tags view is active.<br>
      Expected: The Tags view lists `EXCO, Sponsor, UniversityStaff, Logistics, friends, neighbours`. Selecting
      `EXCO` shows Alex Yeoh, Bernice Yu, and Irfan Ibrahim.

   1. Test case: `addtag t/Publicity`<br>
      Expected: `Publicity` appears in the Tags view. The status message shows `Created tag: Publicity.`

   1. Test case: `addtag t/publicity`<br>
      Expected: An error is shown for the duplicate name. No tag is added.

   1. Test case: `addtag t/Event Crew`<br>
      Expected: An error is shown for the invalid name. No tag is added.

   1. Test case: `edittag Publicity t/Media`<br>
      Expected: The tag is renamed. The status message shows `Publicity successfully renamed to Media.`

   1. Test case: `edit 1 at/Media`, then `edittag media t/Press`<br>
      Expected: The details of member ID `1` show the tag `Press`.

   1. Test case: `deletetag t/press`<br>
      Expected: The tag is removed from the Tags view and from member ID `1`. The status message shows `Press successfully deleted.`

   1. Test case: `deletetag t/Nope`<br>
      Expected: An error is shown for the unknown tag. No tag is deleted.

   1. Other incorrect tag commands to try: `addtag`, `addtag Publicity`, `edittag EXCO`, `deletetag EXCO`<br>
      Expected: An error is shown and no data changes.

### Archiving contacts and events

1. Archiving and restoring a contact

   1. Prerequisites: The sample contacts are loaded. Run `list`. Note the member ID shown on the 2nd contact; the steps
      below assume it is `2`.

   1. Test case: `archive 2`<br>
      Expected: The contact disappears from the contact list, and the **Contacts** tab count drops by 1. The status
      message shows `Archived contact: {NAME}.`

   1. Test case: `list archived`<br>
      Expected: The tabs are titled **Archived contacts (1)** and **Archived events (0)**. Only the archived contact
      is shown, with an **Archived** badge. The sidebar lists only `unarchive` and `list`.

   1. Test case: `unarchive 2`<br>
      Expected: The contact leaves the archived view, which shows `No archived contacts.` After `list`, the contact is
      back in the contact list and the sidebar shows all contact commands again.

   1. Test case: `search` for another contact, then `archive 2` and `unarchive 2`<br>
      Expected: Both succeed although the contact is not displayed.

   1. Other incorrect commands to try: `archive`, `archive 0`, `archive x`, `archive 999`, `unarchive 2` on an active
      contact, `list archive`<br>
      Expected: An error is shown and no data changes.

1. Archiving and restoring an event

   1. Prerequisites: Run `addevent evn/Orientation st/DATE et/DATE`, where `DATE` is tomorrow in `dd-MM-yyyy`
      format. Assign the 1st contact with `assign evn/Orientation c/ID`, using its member ID. A reminder for
      `Orientation` is shown.

   1. Test case: `archiveevent evn/orientation`<br>
      Expected: The **Events** tab opens, even if another tab was selected. The event disappears from it, and its
      reminder disappears. The status message shows `Archived Event Orientation.` In the **Contacts** tab, the
      contact's details still list `Orientation (archived)`.

   1. Test case: `assign evn/Orientation c/ID` with another contact's ID<br>
      Expected: No change. The status message says the event is archived and suggests `unarchiveevent`.

   1. Test case: `addevent evn/Orientation st/DATE et/DATE`<br>
      Expected: No event is added. The status message says an archived event with that name already exists.

   1. Test case: `list archived`, then open the **Events** tab<br>
      Expected: The tab and list heading read **Archived events (1)**. The sidebar lists only `unarchiveevent` and
      `list`.

   1. Test case: `unarchiveevent evn/Orientation`<br>
      Expected: The archived events list shows `No archived events.` After `list`, the event is back in the **Events**
      tab with its members, and its reminder reappears.

1. Archived contacts in events and tags

   1. Prerequisites: The 1st contact is a member of `Orientation`, and has the tag `friends`. Archive the contact with
      `archive ID`.

   1. Test case: Select `Orientation` in the **Events** tab, then the `friends` tag in the **Tags** tab<br>
      Expected: Both member lists show the contact's name followed by `(archived)`.

   1. Test case: `assign evn/Orientation c/ID`, using the archived contact's ID<br>
      Expected: No change. The status message says the contact is archived and suggests `unarchive`.

   1. Test case: `unassign evn/Orientation c/ID`<br>
      Expected: The archived contact is removed from the event.

1. Keeping the archive status after restarting

   1. Prerequisites: Archive a contact and an event as above.

   1. Test case: Close and relaunch the app.<br>
      Expected: The contact and event are still hidden from the normal lists, and appear in `list archived`.

### Saving data

<box type="warning" seamless>

**Caution:** These tests modify or remove the data file. Back up `data/coordimate.json` first if it holds data you
want to keep.
</box>

1. Dealing with a missing data file

   1. Prerequisites: The app is closed and `data/coordimate.json` exists.

   1. Test case: Delete `data/coordimate.json`, then launch the app.<br>
      Expected: The app starts with the sample contacts and the `Freshmen Welcome Night` sample event. The event
      has two assigned contacts, with Alex Yeoh marked present.

   1. Test case: Run `list`, then check the `data` folder.<br>
      Expected: `data/coordimate.json` has not been created, because commands that do not change data do not save.

   1. Test case: Run `addtag t/Test`, then check the `data` folder.<br>
      Expected: `data/coordimate.json` has been created again, containing the sample data and the `Test` tag.

1. Migrating legacy member IDs

   1. Prerequisites: With the app closed, use a saved copy of the sample data in `data/coordimate.json`. Remove
      every contact's `memberId` and the top-level `nextMemberId`, keeping valid JSON.

   1. Test case: Relaunch the app.<br>
      Expected: Contacts receive IDs `1` to `6` in file order. Cards and details show the same IDs. The file is unchanged.

   1. Test case: Run `search Charlotte`, then `edit 3 r/Treasurer`. Delete IDs `2` and `6`, confirming each with `y`.
      Close and relaunch, then run `add n/ID Test p/91234567 e/idtest@example.com r/Member`.<br>
      Expected: Charlotte keeps `ID: 3` and the new role; Bernice and Roy are absent. The new contact receives
      `ID: 7`, so deleted IDs are not reused. Saved contacts include their IDs.

   1. Test case: `edit 2 r/President`, then `delete 2`.<br>
      Expected: Both show an error, request no confirmation, and leave contacts and the file unchanged.

1. Dealing with a corrupted data file

   1. Prerequisites: The app is closed and `data/coordimate.json` contains at least one contact. Keep a copy of the file.

   1. Test case: Open `data/coordimate.json` in a text editor, delete the first `{`, save, then launch the app.<br>
      Expected: CoordiMate warns that the saved data could not be loaded and that the original file is unchanged.
      The warning shows the path to `data/coordimate.json` and says that CoordiMate will close.

   1. Test case: Select **OK**, then inspect `data/coordimate.json`.<br>
      Expected: CoordiMate closes. The file still contains the same corrupted contents and has not been replaced with
      an empty dataset.

   1. Test case: Restore the copy of the file, then launch the app again.<br>
      Expected: The recovery warning does not appear. The original contacts and events are shown again.

1. Dealing with an event member who is not a contact

   1. Prerequisites: The app is closed and `data/coordimate.json` contains an event with at least one member. Keep a copy of the file.

   1. Test case: In the data file, change one name in an event's `members` list to a name that no contact has, save, then launch the app.<br>
      Expected: CoordiMate shows the recovery warning and closes after **OK** is selected. The invalid file remains
      unchanged. Restore the copy before launching again.

1. Dealing with a failed save

   1. Prerequisites: The app is running and `data/coordimate.json` exists. Make the data impossible to save: on Windows, open the properties of `data/coordimate.json` and tick **Read-only**; on macOS or Linux, run `chmod a-w data` in the folder containing the JAR file.

   1. Test case: `addevent evn/Test st/01-01-2027 et/01-01-2027`<br>
      Expected: An error is shown. No event is added and the **Events** tab is unchanged.

   1. Test case: `add n/Test Person p/91234567 e/testperson@example.com r/Member`<br>
      Expected: An error is shown and no contact is added.

   1. Test case: Undo the change made in the prerequisites (untick **Read-only**, or run `chmod u+w data`), then repeat the `addevent` command.<br>
      Expected: The event is added.
