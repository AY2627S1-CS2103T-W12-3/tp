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

* CoordiMate is based on [AddressBook-Level3](https://se-education.org/addressbook-level3/), created by the [SE-EDU initiative](https://se-education.org).
* [JavaFX](https://openjfx.io/) provides the application's graphical user interface.
* [Jackson](https://github.com/FasterXML/jackson) handles reading and writing JSON data and preferences.
* [JUnit 5](https://github.com/junit-team/junit5) supports automated testing.
* [MarkBind](https://markbind.org/) generates the documentation website.
* [PlantUML](https://plantuml.com/) generates the UML diagrams in this guide.
* AI Coding Agents are used throughout the development of this project. 

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

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/MainWindow.java).

Besides the main window, the `UI` component provides:

* `HelpWindow`, a command quick reference built from the command words of the command classes.
* `TutorialWindow`, a guided tour shown as an overlay over the main window. `UiManager` opens it on first launch, and
  the `tutorial` command or <kbd>F2</kbd> opens it again.
* Command history in `CommandBox`, which keeps the commands entered in the current session for recall with the Up
  and Down arrow keys.

At launch, `MainWindow` restores the window size, position, and maximized state saved in `GuiSettings`, using
`WindowBounds` to keep the window within the available screen area.

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data. For example, the Contacts,
  Events, and Tags views refresh when a contact they show is edited.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
  A command can ask `MainWindow` to show the Help window, the tutorial, or a particular view through its
  `CommandResult`.
* depends on some classes in the `Model` component because it displays `Person` and `Event` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `CoordiMateParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`), which is executed by the `LogicManager`. Most commands that change data are executed on a copy of the model, which replaces the live data only after it is saved, as described in [Saving data atomically](#saving-data-atomically).
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

The sequence diagram below shows the interactions within the `Logic` component for `execute("delete 1")`. `delete`
asks for confirmation first: the first call only finds the contact and returns a confirmation prompt, and the contact
is deleted only if the next input is `y` or `Y`. Any other input cancels the deletion.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<puml src="diagrams/ParserClasses.puml" width="600"/>

How the parsing works:
* When called upon to parse a user command, the `CoordiMateParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `CoordiMateParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

Every error message is followed by the usage of the command that failed. `CoordiMateParser` adds it to parse errors
and `LogicManager` adds it to execution errors, using `Messages.withUsage`.

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

Saved data is validated when it is loaded, and a file that fails validation is never overwritten. If the data file
cannot be loaded, CoordiMate shows a warning naming the file and then closes, so that the user can repair or restore
it. If only the preferences file cannot be loaded, CoordiMate starts with default preferences and does not save them
for that session. Data files saved by earlier versions, such as those without member IDs or a saved tag list, are
still accepted and are updated the next time the data is saved.

### Common classes

Classes used by multiple components are in the `coordimate.commons` package. These include `GuiSettings`,
`LogsCenter`, the shared exceptions such as `DataLoadingException`, and utilities such as `StringUtil` and `JsonUtil`.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Permanent member IDs

Each saved contact has a unique, permanent `MemberId`, displayed as `ID: <number>`. Commands such as `edit`,
`delete`, `assign`, and `unassign` identify contacts by this ID, so search results, filters, and list order never
change which contact an ID refers to.

#### Allocation and editing

`Person` holds an immutable ID. `CoordiMate` allocates new IDs from a `nextMemberId` counter, which only ever
increases, so deleted IDs are never reused. `UniquePersonList` rejects duplicate IDs and prevents an edit from
changing a contact's ID. `clear` starts a new empty dataset and resets allocation to `1`.

#### Command targeting

`ParserUtil#parseMemberId` parses a positive integer, and `MemberIdResolver` looks it up among **all** saved
contacts, including contacts hidden by the current search, filter, or archive view. An unknown ID is rejected
without changing data. Event membership and attendance still use contact names internally, so `assign` and
`unassign` translate IDs to names after resolving every ID.

`delete` keeps the resolved contact while waiting for confirmation. On confirmation, it resolves the ID again and
rejects the deletion if that contact has since been removed or edited.

#### Storage and migration

The data file stores each contact's `memberId` and the `nextMemberId` counter. When a file saved before member IDs
existed is loaded, contacts without an ID receive consecutive IDs in file order, after any IDs already in the file.
Loading does not rewrite the file; the IDs are recorded on the next successful save. A file with duplicate or
invalid IDs is rejected like any other invalid data file.

#### Design considerations

**Aspect: How the next ID is chosen**

* **Alternative 1 (current choice):** Save a counter in the data file.
  * Pros: An ID is never reused, even after the highest-numbered contact is deleted.
  * Cons: The counter must be validated and migrated along with the contacts.

* **Alternative 2:** Use one more than the highest saved ID on every launch.
  * Pros: Nothing extra to save.
  * Cons: Deleting the highest-numbered contact and restarting would reuse its ID for a different contact.

### Saving data atomically

Commands that edit contacts, event details, tags, or attendance either take effect in full, both in the app
and in the data file, or not at all. This keeps the displayed data and the saved data identical even when the
data file cannot be read or written.

#### Implementation

Two mechanisms work together.

**1. Commands run on a copy of the model.** [`LogicManager#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/LogicManager.java)
passes commands that change data, such as `AddCommand`, `EditCommand`, and `AssignCommand`, to
`LogicManager#executeAtomicCommand`, which does the following:

1. It checks that the existing data file can still be loaded. If it cannot, the command is rejected and the file
   is left untouched, so that the user can repair it. A missing data file is not an error.
1. It creates a second `ModelManager`, the _candidate_, from a copy of the live model's data. It also copies the
   current [archive view](#archiving-contacts-and-events) onto the candidate, so that commands which read the
   displayed list see the same contacts on the candidate as on the screen.
1. It executes the command on the candidate. If the command fails, the candidate is discarded.
1. It saves the candidate's data. If the file was changed by another CoordiMate window since it was loaded, or the
   save fails, the command is rejected and the candidate is discarded.
1. Only then does it replace the live model's data with the candidate's data, which updates the UI.

The live model is not touched until step 5, so a failure at any earlier step leaves the app exactly as it was.
Afterwards, `add` and `edit` show all contacts again, so that the changed contact is visible.

<box type="warning" seamless>

**Note:** `LogicManager#execute` chooses which commands use this path from a fixed list of command classes. A new
command that changes data must be added to that list, or its changes will not be saved atomically.
</box>

The sequence diagram below shows a successful call to `executeAtomicCommand(c)` with an `AddEventCommand` as `c`. Parsing is omitted.

<puml src="diagrams/AtomicCommandSequenceDiagram.puml" alt="Interactions when a command that changes data is executed" />

The activity diagram below summarises the possible outcomes.

<puml src="diagrams/AtomicCommandActivityDiagram.puml" width="450" />

**2. The data file is replaced in one step.**
[`JsonCoordiMateStorage#saveCoordiMate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonCoordiMateStorage.java)
writes the new data to a temporary file in the same folder, then moves it over the data file with an atomic move.
The data file therefore always holds either the complete old data or the complete new data.

A confirmed `delete` runs through `executeAtomicCommand` like any other command. Commands that do not change data,
such as `list`, `search`, `filter`, and `members`, run directly on the live model without saving. Reminder
dismissal uses its own save path, described in [Dismissing reminders](#dismissing-reminders).

#### Design considerations

**Aspect: How a command is prevented from taking effect when saving fails**

* **Alternative 1 (current choice):** Execute the command on a copy of the model, save the copy, then replace the
  live model's data.
  * Pros: Works for every command that changes data without command-specific rollback code. The UI never shows
    an edit that was not saved.
  * Cons: Copies all data for each command, which is acceptable for the expected 250 contacts.

* **Alternative 2:** Execute the command on the live model, and restore a snapshot if saving fails.
  * Pros: No copy is needed when saving succeeds.
  * Cons: The UI observes the model, so it briefly shows a change that is then reverted.

### Dismissing reminders

The **Upcoming events** panel lists reminders for events starting soon, ordered by start time and then by name.
`dismiss INDEX` and the panel's **Dismiss** button both hide a reminder through the same `ReminderService`, which
`LogicManager` creates and passes to `DismissCommand`.

`DismissCommand` checks the index against the current reminder list, then asks `ReminderService` to dismiss that
event's reminder. Like other commands that change data, the service saves an updated copy of the event before
changing the live event, and keeps the event's details, members, and attendance unchanged.

If the data file cannot be read or saved, the dismissal is still kept for the current session, so the reminder
disappears, and the command shows a **Not saved** warning instead of an error. The reminder may then return after a
restart. Because `ReminderService` handles the save itself, `LogicManager` does not run `dismiss` through
`executeAtomicCommand`.

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

`MarkAttendanceCommand` finds the event and the member, ignoring case, and reports that no changes were made if the
member already has the requested status. Otherwise it replaces the event with a copy from `Event#withAttendance`.
The command changes data, so it runs through `executeAtomicCommand` as described above.

Other commands keep the attendance record consistent with the members:

| Change | Effect on attendance |
|---|---|
| `assign` adds members | Existing attendance is kept. New members are not marked. |
| `unassign` removes members | The removed members' attendance is removed. |
| `editevent` renames or reschedules the event | Attendance is kept. |
| `edit` renames a contact | The contact's attendance moves to the new name in every event. |
| `delete` deletes a contact | The contact's attendance is removed from every event. |

In the data file, each event stores attendance as an `attendance` object that maps each member's name to
`"present"` or `"absent"`. A file without this field loads with no attendance recorded.

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
splits the arguments at each `FIELD/` token into a list of [`FilterCriterion`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterCriterion.java)
records, each holding a [`FilterField`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterField.java)
and its value. A value may be wrapped in quotation marks, so that it can contain text that looks like a `FIELD/`
token.

**Resolving.** [`FilterCommand#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/commands/FilterCommand.java)
turns each criterion into a `Predicate<Person>` using the model:

| Field | The value must match, ignoring case | A contact matches if |
|---|---|---|
| `tag` | a tag in `Model#getTagList()` | it has that tag |
| `event` | the name of a saved event | it is a member of the event with that name |
| `organisation` | the organisation of at least one saved contact | its organisation is that value |

If a value matches nothing, the command is rejected before the displayed list is changed. Otherwise the predicates
are combined into a [`ContactFilterPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/ContactFilterPredicate.java),
which a contact matches only if it matches every predicate, and passed to `Model#updateFilteredPersonList`.
`filter` does not change data, so it is executed directly on the live model.

The `event` and `tag` predicates identify their event or tag by name. How a filtered list follows later changes to
that event or tag is described in [Keeping events in sync with contacts](#keeping-events-in-sync-with-contacts).

To add a filter field, add a constant to `FilterField`, a case to `FilterCommand#resolvePredicate`, and a case to the
example-selection switch in `FilterCommandParser#parse`.

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

* **Alternative 1 (current choice):** Reject the command with an error.
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

* When a contact is renamed, every event that lists the old name is replaced with a copy that lists the new name in
  the same position, with the contact's attendance carried over.
* When a contact is deleted, every event that lists the contact is replaced with a copy without the name or its
  attendance.

All the updated events are built before anything is changed. If an event already has a member with the new name,
the rename is rejected and no data is changed. When loading, a data file in which an event lists a member who is not
a saved contact is rejected.

**Showing an event's members.** `MembersCommand` filters the contact list with
[`EventMembersPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/EventMembersPredicate.java),
which holds the event's name and looks the event up each time it is tested. The displayed members therefore follow
later changes:

* After `assign` or `unassign`, an added member matches and a removed member no longer does.
* After `deleteevent`, no event has that name, so the list becomes empty.
* After `editevent` renames the event, `LogicManager#keepEventViewsAfterRename` replaces the predicate with one for
  the new name.

`filter event/...` uses the same predicate, so a filtered list follows the same changes. In the same way, after
`edittag` renames a tag, `LogicManager#keepTagFiltersAfterRename` updates any `filter tag/...` criterion to the new
name.

<box type="warning" seamless>

**Note:** A filtered list re-tests its predicate only when the contact list changes. For this reason
`CoordiMate#resetData` replaces the events **before** the contacts, so that the predicate already sees the new
events when it is tested again. Reversing this order would leave event member views out of date.
</box>

#### Design considerations

**Aspect: How an event refers to its members**

* **Alternative 1 (current choice):** By contact name.
  * Pros: Readable in the data file, where `members` is a list of names. Names are already unique among contacts.
  * Cons: Renaming or deleting a contact must update every event, as described above.

* **Alternative 2:** By the contact's permanent member ID.
  * Pros: Renaming a contact would not affect events.
  * Cons: Requires migrating event member and attendance references from names to IDs in the data file, and
    looking up contacts to display member names.

### Marking contacts as on leave

A contact can be marked as on leave until a given date. The status ends by itself once that date has passed.

#### Implementation

[`LeaveDate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/LeaveDate.java)
holds the **last day** of a contact's leave. `Person` stores it as an optional field, like birthday and note, which
is set with `lv/` in `add` and `edit`, and cleared with an empty `lv/`. `Person#isOnLeave(LocalDate today)` returns
true if a date is saved and `today` is not after it.

A date entered with `lv/` must not be in the past, but any real date is accepted when loading the data file, so
that leave which has ended can still be loaded.

**How leave ends.** Nothing is changed when a leave date passes. Instead, the UI asks each contact whether it is on
leave on the current date, and shows the **On leave until** badge only if it is. The ended date stays in the data
file until the user replaces or clears it. Code that copies a contact, such as `CoordiMate#setTag` and
`CoordiMate#removeTag`, must pass the leave date on so that it is not dropped.

`PersonListPanel` reads the current date from a `Clock`, which tests replace to control the date. A window left open
past midnight still needs redrawing, so `MainWindow`'s existing one-minute reminder timer also calls
`PersonListPanel#refreshIfDateChanged()`, which redraws the contacts only when the date has changed.

In the data file, the date is stored in a `leaveUntil` field. Contacts without this field load as not on leave.

#### Design considerations

**Aspect: How leave ends once its date has passed**

* **Alternative 1 (current choice):** Keep the date, and check it against the current date whenever a contact is
  displayed.
  * Pros: No data needs changing when leave ends, so no save is needed at launch or at midnight. The behaviour is
    easy to test by passing a fixed date to `Person#isOnLeave`.
  * Cons: Ended dates remain in the data file, and a window left open past midnight must be redrawn.

* **Alternative 2:** Remove ended leave dates from the data, for example at launch.
  * Pros: The data file only contains leave that is still active.
  * Cons: Requires saving at launch, which no other feature does, and still needs a check for leave that ends while
    the app is open.

**Aspect: How an open window notices that the date has changed**

* **Alternative 1 (current choice):** Check the date on each tick of the existing one-minute reminder timer.
  * Pros: Adds no new timer. Still works if the computer sleeps through midnight or its clock changes.
  * Cons: The badge can stay visible for up to a minute after midnight.

* **Alternative 2:** Schedule a timer for the next midnight, then reschedule it each day.
  * Pros: Updates exactly at midnight.
  * Cons: A second timer to manage, which can fire at the wrong time after a sleep or clock change.

### Archiving contacts and events

`archive`, `unarchive`, `archiveevent`, and `unarchiveevent` hide a contact or an event from the normal lists without
deleting it. `list archived` shows only archived items, and `list` returns to the active ones.

#### Implementation

**The archive flag.** [`Person`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/Person.java)
and [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java)
each have a boolean `archived` field. Both are immutable, so archiving returns a copy from `withArchived(boolean)`,
and every other copy method passes the flag on. `Person` takes `archived` as a parameter of its full constructor,
which every copy of an existing contact uses, so the compiler makes each copy, such as `EditCommand` and the tag
updates in `CoordiMate`, pass the flag on. Archiving changes nothing else: an archived contact keeps its name in event
member lists, and an archived event keeps its members and attendance.

**Hiding archived items.** [`ModelManager`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/ModelManager.java)
keeps an archive view, which is true while archived items are shown, separately from the contact filter set by
commands. The filtered contact list shows a contact only if its `archived` value matches the view **and** it
matches the command's filter. The filtered event list shows the events whose `archived` value matches the view.
`MainWindow` observes the view to update the tab titles and sidebar commands.

Because the view is applied inside the model, `search`, `filter`, and `members` hide archived contacts without any
change to those commands.

The sequence diagram below shows `archive 12`. `ArchiveCommand` finds the contact by its permanent member ID, so the
command works whatever the displayed list shows. Like `edit`, it runs on a copy of the model, as described in
[Saving data atomically](#saving-data-atomically).

<puml src="diagrams/ArchiveSequenceDiagram.puml" width="700" />

**Effects on other features.**

| Feature | Effect of archiving |
|---|---|
| Reminders | Archived events have no reminders. |
| `assign` | Rejected for an archived event or an archived contact. |
| `unassign` | Rejected for an archived event, but can still remove an archived contact. |
| `members`, `markattendance`, `editevent`, `deleteevent` | Still work on archived events. |
| `addevent` | Rejected if an archived event has the same name, with a hint to unarchive it. |
| `add`, `addevent` | Switch the view back to active items, so that the new item is visible. |
| Event and tag member lists, a contact's event list | Archived items are marked `(archived)`. |

In the data file, contacts and events store an `archived` field. A file without this field loads with nothing
archived.

#### Design considerations

**Aspect: How archived items are hidden**

* **Alternative 1 (current choice):** The model combines the archive view with every contact filter.
  * Pros: Every command that filters contacts respects the view without changes. Future filters also respect it.
  * Cons: The filtered list's predicate is no longer exactly the one a command set, so `ModelManager` must keep the
    command's predicate separately.

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

**Note:** Use these tests as a starting point for further *exploratory* testing.

For every command error below, check that the feedback also includes the command's usage, with its parameters
and an example.
</box>

<box type="info" seamless>

**Note:** Many tests below start from the sample data and expect specific member IDs. Data created in earlier
sections is not always cleaned up, so when a section's prerequisites say **Restore the sample data**, do so before
continuing:

1. Close the app.
1. Delete `data/coordimate.json` from the folder containing the JAR file.
1. Relaunch the app.

The app then starts with the six sample contacts (member IDs `1` to `6`), the `Freshmen Welcome Night` sample
event, and no custom tags. Deleting contacts by hand is not enough, because member IDs are never reused.
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

### Deleting a contact

1. Prerequisites: Restore the sample data and run `list`. Alex Yeoh has member ID `1`.

1. Test case: `delete 1`, then `n`<br>
   Expected: `ID: 1` and Alex Yeoh's name, phone, email, and role are shown, followed by `Confirm deletion? [y/N]`. After `n`, the status message shows `Deletion cancelled. No changes were made.` and no contact is deleted.

1. Test case: `delete 1`, then `y ` (with a trailing space)<br>
   Expected: Alex Yeoh (member ID `1`) is deleted. Other contacts keep their IDs. The status message shows `Contact deleted successfully: ` followed by the contact's name.

1. Test case: `delete 0`<br>
   Expected: No confirmation is requested and no contact is deleted. The status message shows `Invalid member ID. Example: delete 12`.

1. Other incorrect delete commands to try: `delete`, `delete x`, `delete 99` (where member ID `99` does not exist)<br>
   Expected: No confirmation is requested and no contact is deleted. The status message shows error details.

### Searching contacts

1. Prerequisites: Restore the sample data.

1. Test case: `search alex`<br>
   Expected: Only `Alex Yeoh` is listed. The status message shows `1 contact(s) found matching "alex".`

1. Test case: `search zzz`<br>
   Expected: The list is empty. The status message shows `No contacts found matching "zzz".`

1. Test case: `search`<br>
   Expected: The list is unchanged. The status message shows `Invalid command format.`

### Filtering contacts

1. Prerequisites: Restore the sample data (contact IDs `1` to `6`), with no additional contacts, events, or custom
   tags. Run these commands in order:<br>
   `edit 3 o/NUS Student Affairs`<br>
   `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
   `assign evn/Final Concert c/1 2`<br>
   `addtag t/Publicity`

1. Test case: `filter tag/exco`<br>
   Expected: `Alex Yeoh`, `Bernice Yu`, and `Irfan Ibrahim` are listed, in that order. The status
   message shows `3 contact(s) found.`

1. Test case: `filter event/Final Concert tag/EXCO`<br>
   Expected: `Alex Yeoh` and `Bernice Yu` are listed; both belong to `Final Concert` and have the `EXCO` tag.

1. Test case: `filter organisation/NUS Student Affairs`<br>
   Expected: Only `Charlotte Oliveiro` is listed.

1. Test case: `filter organisation/NUS`<br>
   Expected: Only `Charlotte Oliveiro` remains listed. The status message shows
   `No saved tag, event, or organisation matches that value. Use a value from the saved data.`

1. Test case: `filter tag/Publicity`<br>
   Expected: The list is empty, as the tag exists but no contact has it. The status message shows `No contacts match the specified filters.`

1. Test case: `filter role/Logistics`<br>
   Expected: The list stays empty. The status message shows `Unknown parameter: role/.`

1. Other incorrect filter commands to try: `filter`, `filter tag/`<br>
   Expected: The list is unchanged. The status message shows error details.

1. Cleanup: Run `deletetag t/Publicity`, then `deleteevent evn/Final Concert`.<br>
   Expected: The `Publicity` tag and the `Final Concert` event are removed, as later sections create them again.

### Adding an event

1. Prerequisites: No event named `Final Concert` or `Fair` exists.

1. Test case: `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
   Expected: The **Events** tab opens with the new event selected. The status message shows `Created Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 18:00.`

1. Test case: `addevent evn/final concert st/09-10-2026 et/10-10-2026`<br>
   Expected: No event is added. The status message shows `An event with this name already exists.`

1. Test case: `addevent evn/Fair st/10-10-2026 et/09-10-2026`<br>
   Expected: No event is added. The status message shows `Start time must not be after end time.`

1. Test case: `addevent evn/Fair st/10/10/2026 et/11-10-2026`<br>
   Expected: No event is added. The status message shows the required date and time format.

1. Other incorrect addevent commands to try: `addevent`, `addevent evn/Fair st/10-10-2026`<br>
   Expected: No event is added. The status message shows error details.

### Editing an event

1. Prerequisites: An event named `Final Concert` exists from 08-08-2026 15:00 to 08-08-2026 18:00, as created in
   the previous section. If it does not, create it using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`.
   Select it in the **Events** tab.

1. Test case: `editevent evn/final concert st/08-08-2026 16:00 et/08-08-2026 19:00`<br>
   Expected: The event's details show the new times. The status message shows `Edited Event Final Concert. Start Time: 08-08-2026 16:00. End Time: 08-08-2026 19:00.`

1. Test case: `editevent evn/Final Concert nevn/Grand Concert`<br>
   Expected: The event is renamed to `Grand Concert` and keeps its times.

1. Test case: `editevent evn/Grand Concert st/09-08-2026`<br>
   Expected: No change. The status message shows `Start time must not be after end time.`

1. Other incorrect editevent commands to try: `editevent evn/Grand Concert`, `editevent evn/Grand Concert nevn/`<br>
   Expected: No change. The status message shows error details.

### Deleting an event

1. Prerequisites: An event named `Grand Concert` exists, as created in the previous section.

1. Test case: `deleteevent evn/Gala`<br>
   Expected: No event is deleted. The status message shows `Event Gala does not exist.`

1. Test case: `deleteevent evn/grand concert`<br>
   Expected: The event is removed from the **Events** tab. The status message shows `Deleted Event Grand Concert.`

1. Other incorrect deleteevent commands to try: `deleteevent`, `deleteevent evn/`<br>
   Expected: No event is deleted. The status message shows error details.

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
      Expected: No reminder is dismissed. Feedback shows
      `No reminder exists at index 2. Use an index from the Upcoming events panel.`

   1. Test case: `dismiss 1`, then `dismiss 1` again.<br>
      Expected: The first command dismisses `Zulu Reminder` and hides the empty panel. The second command
      reports that no reminder exists at index 1. Both events remain saved.

1. Rejecting invalid input

   1. Prerequisites: At least one reminder is shown.

   1. Test cases: `dismiss`, `dismiss 0`<br>
      Expected: No reminders or saved events change. Feedback shows **Rejected** and an explanation.
      A missing index gives `Invalid command format.`; `dismiss 0` gives
      `Index must be a positive integer. Example: 1`.

1. Cleaning up

   1. Test case: `deleteevent evn/Zulu Reminder`, then `deleteevent evn/alpha Reminder`<br>
      Expected: Both events are removed from the **Events** tab, along with any reminders for them.

### Assigning members to an event

1. Assigning contacts while all persons are being shown

   1. Prerequisites: Restore the sample data and run `list`; Alex Yeoh, Bernice Yu, and Charlotte Oliveiro have IDs `1`, `2`, and `3`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`. Select it in the **Events** tab.

   1. Test case: `assign evn/Final Concert c/1 2`<br>
      Expected: The status message shows `Assigned 2 member(s) to Final Concert.` The event's details list Alex Yeoh (`ID: 1`) and Bernice Yu (`ID: 2`) under **Members (2)**.

   1. Test case: `assign evn/final concert c/2 3`<br>
      Expected: Only Charlotte Oliveiro (`ID: 3`) is added. The status message also shows `1 contact(s) were already assigned.`

   1. Other incorrect assign commands to try: `assign evn/Final Concert c/0`, `assign evn/Final Concert c/99` (where member ID `99` does not exist)<br>
      Expected: No change. The status message shows error details.

1. Assigning contacts from a filtered list

   1. Prerequisites: The sample contacts are loaded. Create an empty event using `addevent evn/Filtered Fair st/08-08-2026 et/08-08-2026`. Run `search Charlotte`, showing only `ID: 3`.

   1. Test case: `assign evn/Filtered Fair c/3 1`<br>
      Expected: Charlotte Oliveiro (`ID: 3`) and Alex Yeoh (`ID: 1`) are assigned, in that order. Their IDs stay
      unchanged although Charlotte is the first search result and Alex is hidden.

### Removing members from an event

1. Removing members while all persons are being shown

   1. Prerequisites: Restore the sample data and run `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/1 2 3`. Select the event in the **Events** tab.

   1. Test case: `unassign evn/final concert c/2 4`<br>
      Expected: The status message shows `Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.` The event's details list member IDs `1` and `3` under **Members (2)**, in that order.

   1. Other incorrect unassign commands to try: `unassign evn/Final Concert c/0`, `unassign evn/Final Concert c/1 99` (where member ID `99` does not exist)<br>
      Expected: No change. The status message shows error details.

1. Removing members from a filtered list

   1. Prerequisites: The `Final Concert` event has member IDs `1` and `3` assigned. Run `search Charlotte`, showing only `ID: 3`.

   1. Test case: `unassign evn/Final Concert c/3`<br>
      Expected: Charlotte Oliveiro (`ID: 3`) is removed. Alex Yeoh (`ID: 1`) remains assigned.

### Viewing members of an event

1. Viewing the members of an event

   1. Prerequisites: Restore the sample data and run `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/3 1`. Switch to the **Events** view.

   1. Test case: `members evn/final concert`<br>
      Expected: CoordiMate switches to the **Contacts** view and shows Alex Yeoh (`ID: 1`) and Charlotte Oliveiro (`ID: 3`), in contact list order. The status message shows `Listed 2 member(s) of Final Concert. Use list to show all contacts.`

   1. Other incorrect members commands to try: `members`, `members evn/Final Concert c/1`<br>
      Expected: The contact list is unchanged. The status message shows error details.

1. Keeping the shown members up to date

   1. Prerequisites: The members of `Final Concert` are shown, as above.

   1. Test case: `unassign evn/Final Concert c/1`, then `editevent evn/Final Concert nevn/Grand Concert`<br>
      Expected: Alex Yeoh disappears after `unassign`. Charlotte Oliveiro keeps `ID: 3` as the only result
      after both commands.

   1. Test case: `deleteevent evn/Grand Concert`, then `list`<br>
      Expected: The list becomes empty after the event is deleted, then shows all persons after `list`.

### Marking attendance

1. Marking and changing attendance

   1. Prerequisites: Restore the sample data and run `list`. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/1 2`. Select the event in the **Events** tab.

   1. Test case: `markattendance evn/Final Concert mem/Alex Yeoh att/present`<br>
      Expected: `Alex Yeoh` shows **Present**. The counts show 1 present, 0 absent, and 1 not marked. The status message shows `Marked Alex Yeoh as present for Event Final Concert.`

   1. Test case: `markattendance evn/final concert mem/alex yeoh att/ABSENT`<br>
      Expected: `Alex Yeoh` shows **Absent**. The counts show 0 present, 1 absent, and 1 not marked.

   1. Test case: `markattendance evn/Final Concert mem/David Li att/present`<br>
      Expected: No change. The status message shows `David Li is not assigned to event Final Concert. Assign the contact to the event before marking attendance.`

   1. Other incorrect markattendance commands to try: `markattendance evn/Final Concert mem/Alex Yeoh att/late`, `markattendance evn/Final Concert mem/Alex Yeoh`<br>
      Expected: No change. The status message shows error details.

1. Keeping members and attendance in sync with contacts

   1. Prerequisites: Continue from the previous test, with `Alex Yeoh` marked absent. List all contacts using `list`.

   1. Test case: `edit 1 n/Alex Tan`<br>
      Expected: The event's members show `Alex Tan` in the same position, still **Absent**.

   1. Test case: `unassign evn/Final Concert c/1`, then `assign evn/Final Concert c/1`<br>
      Expected: `Alex Tan` is removed, then added again at the end of the members as **Not marked**.

   1. Test case: `markattendance evn/Final Concert mem/Alex Tan att/present`, then close and relaunch the app.<br>
      Expected: `Alex Tan` is still **Present**.

   1. Test case: `delete 1`, then `y`.<br>
      Expected: `Alex Tan` and his attendance are removed from the event. Other members are unchanged.

### Marking a contact as on leave

<box type="info" seamless>

**Note:** In these tests, `FUTURE_DATE` is any date after today in `dd-MM-yyyy` format, `TODAY` is today's date,
and `PAST_DATE` is any date before today, such as `01-01-2020`.
</box>

1. Setting and clearing leave

   1. Prerequisites: Restore the sample data and run `list`.

   1. Test case: `edit 1 lv/FUTURE_DATE`<br>
      Expected: The card of the contact with member ID `1` shows **On leave until FUTURE_DATE**. Selecting the contact shows `FUTURE_DATE` under **On leave until**.

   1. Test case: `edit 1 lv/`<br>
      Expected: The badge disappears and the details pane shows `— (not on leave)`.

   1. Test case: `edit 1 lv/PAST_DATE`<br>
      Expected: No change. The status message shows `Invalid leave end date. Use a real date in dd-MM-yyyy format that is not in the past. Example: lv/30-11-2026`

   1. Other incorrect commands to try: `edit 1 lv/31-11-2030`, `edit 1 lv/next week`<br>
      Expected: No change. The status message shows error details.

1. Leave ending by itself

   1. Prerequisites: Run `edit 1 lv/FUTURE_DATE`, then close the app.

   1. Test case: In `data/coordimate.json`, change that contact's `leaveUntil` value to `PAST_DATE`, save, then launch the app.<br>
      Expected: The app loads the data. The contact is not shown as on leave, and the details pane shows `— (not on leave)`. The `leaveUntil` value is still in the data file.

   1. Test case: Run `edit 1 lv/TODAY` and select that contact. Leave the app open past midnight, or set your
      computer's date to the next day.<br>
      Expected: The contact initially shows **On leave until TODAY**. Within a minute after midnight, without
      running a command, the badge disappears and the details pane shows `— (not on leave)`.

### Managing tags

1. Prerequisites: Restore the sample data.

1. Test case: Press <kbd>Esc</kbd>, then <kbd>S</kbd> until the Tags view is active.<br>
   Expected: The Tags view lists `EXCO, Sponsor, UniversityStaff, Logistics, friends, neighbours`. Selecting
   `EXCO` shows Alex Yeoh, Bernice Yu, and Irfan Ibrahim.

1. Test case: `addtag t/Publicity`<br>
   Expected: `Publicity` appears in the Tags view. The status message shows `Created tag: Publicity.`

1. Test case: `addtag t/publicity`<br>
   Expected: No tag is added. The status message shows `A tag with this name already exists. No changes were made.`

1. Test case: `addtag t/Event Crew`<br>
   Expected: No tag is added. The status message shows `Invalid tag name. Use letters and numbers without spaces. Example: t/Publicity`.

1. Test case: `edit 1 at/Publicity`, then `edittag publicity t/Media`<br>
   Expected: The Tags view and member ID `1`'s details show `Media`. The status message shows
   `Publicity successfully renamed to Media.`

1. Test case: `deletetag t/media`<br>
   Expected: The tag is removed from the Tags view and from member ID `1`. The status message shows `Media successfully deleted.`

1. Test case: `deletetag t/Nope`<br>
   Expected: No tag is deleted. The status message shows `Tag Nope does not exist. Use a saved tag name.`

1. Other incorrect tag commands to try: `addtag`, `edittag EXCO`, `deletetag EXCO`<br>
   Expected: No change. The status message shows error details.

### Archiving contacts and events

1. Archiving and restoring a contact

   1. Prerequisites: Restore the sample data and run `list`. Bernice Yu has member ID `2`.

   1. Test case: `archive 2`<br>
      Expected: The contact disappears from the contact list, and the **Contacts** tab count drops by 1. The status
      message shows `Archived contact: {NAME}.`

   1. Test case: Close and relaunch the app, then run `list archived`.<br>
      Expected: The tabs are titled **Archived contacts (1)** and **Archived events (0)**. Only the archived contact
      is shown, with an **Archived** badge. The sidebar lists only `unarchive` and `list`.

   1. Test case: `unarchive 2`<br>
      Expected: The contact leaves the archived view, which shows `No archived contacts.` After `list`, the contact is
      back in the contact list and the sidebar shows all contact commands again.

   1. Other incorrect commands to try: `archive`, `archive 0`, `unarchive 2` on an active
      contact, `list archive`<br>
      Expected: No change. The status message shows error details.

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

   1. Test case: Close and relaunch the app, run `list archived`, then open the **Events** tab.<br>
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

### Saving data

<box type="warning" seamless>

**Caution:** These tests modify or remove the data file. Back up `data/coordimate.json` first if it holds data you
want to keep.
</box>

1. Dealing with a missing data file

   1. Prerequisites: The app is closed and `data/coordimate.json` exists.

   1. Test case: Delete `data/coordimate.json`, launch the app, then run `list`.<br>
      Expected: The app shows the sample contacts and `Freshmen Welcome Night`, with two assigned contacts and
      Alex Yeoh marked present. `data/coordimate.json` is not created by launching or running `list`.

   1. Test case: Run `addtag t/Test`, then check the `data` folder.<br>
      Expected: `data/coordimate.json` has been created again, containing the sample data and the `Test` tag.

1. Migrating legacy member IDs

   1. Prerequisites: Restore the sample data, then run `edit 1 r/President` so that
      `data/coordimate.json` is created (Alex Yeoh is already President, so the data is unchanged). Close the app.
      In `data/coordimate.json`,
      remove every contact's `memberId` property and the top-level `nextMemberId` property, keeping valid JSON.

   1. Test case: Relaunch the app.<br>
      Expected: Contacts receive IDs `1` to `6` in file order. Cards and details show the same IDs. The file is unchanged.

   1. Test case: Run `search Charlotte`, then `edit 3 r/Treasurer`. Delete IDs `2` and `6`, confirming each with `y`.
      Close and relaunch, then run `add n/ID Test p/91234567 e/idtest@example.com r/Member`.<br>
      Expected: Charlotte keeps `ID: 3` and the new role; Bernice and Roy are absent. The new contact receives
      `ID: 7`, so deleted IDs are not reused. Saved contacts include their IDs.

   1. Test case: `edit 2 r/President`, then `delete 2`.<br>
      Expected: Both report `No contact exists with member ID: 2. Use an existing member ID.`
      No confirmation is requested, and contacts and the file remain unchanged.

1. Dealing with invalid saved data

   1. Prerequisites: Close the app and keep a copy of `data/coordimate.json` containing sample contacts and an
      event with at least one member. Restore the copy before trying each corruption below.

   1. Test cases: Make one of these changes in a text editor, save, then launch the app:
      * Delete the first `{`.
      * Change an event's `members` entry to a name that no contact has.
      * Add `"leaveUntil": "31-02-2030"` to a contact.

      Expected: CoordiMate warns that the saved data could not be loaded and that the original file is unchanged.
      The warning shows the path to `data/coordimate.json` and says that CoordiMate will close.

   1. Test case: Select **OK**, then inspect `data/coordimate.json`.<br>
      Expected: CoordiMate closes. The file still contains the same corrupted contents and has not been replaced with
      an empty dataset.

   1. Test case: Restore the copy of the file, then launch the app again.<br>
      Expected: The recovery warning does not appear. The original contacts and events are shown again.

1. Dealing with a failed save

   1. Prerequisites: The app is running and `data/coordimate.json` exists. Make the data impossible to save: on Windows, open the properties of `data/coordimate.json` and tick **Read-only**; on macOS or Linux, run `chmod a-w data` in the folder containing the JAR file.

   1. Test case: `addevent evn/Test st/01-01-2027 et/01-01-2027`<br>
      Expected: No event is added and the **Events** tab is unchanged. The status message shows `Event could not be saved. No changes were made.`

   1. Test case: `add n/Test Person p/91234567 e/testperson@example.com r/Member`<br>
      Expected: No contact is added. The status message shows `Contact could not be saved. No changes were made.`

   1. Test case: Undo the change made in the prerequisites (untick **Read-only**, or run `chmod u+w data`), then repeat the `addevent` command.<br>
      Expected: The event is added.
