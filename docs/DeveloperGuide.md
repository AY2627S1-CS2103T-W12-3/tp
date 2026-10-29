---
  layout: default.md
  title: "Developer Guide"
  pageNav: 3
---

# CoordiMate Developer Guide

<!-- * Table of Contents -->
<page-nav-print />

Domain terms used throughout this guide are defined in the [glossary](#glossary).

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
* `TutorialWindow`, which renders the [tutorial](#glossary) overlay. `UiManager` opens it on first launch, and
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

The [member ID](#glossary) implementation uses `MemberId`, `MemberIdResolver`, and the persisted `nextMemberId` counter.

#### Allocation and editing

`Person` holds an immutable ID. `CoordiMate` allocates new IDs from a `nextMemberId` counter, which only ever
increases, so deleted IDs are never reused. `UniquePersonList` rejects duplicate IDs and prevents an edit from
changing a contact's ID. `clear` starts a new empty dataset and resets allocation to `1`.

The object diagram below shows the contacts after three contacts were added, the contact with ID `3` was deleted, and
another contact was added. The new contact received ID `4`, and `nextMemberId` moved on to `5`, so ID `3` stays unused.

<puml src="diagrams/MemberIdObjectDiagram.puml" width="520" />

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
The move requests `ATOMIC_MOVE` and `REPLACE_EXISTING`. Under the
[Java API contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...)),
replacing an existing target with an atomic move depends on the filesystem provider. An unsupported atomic move
throws `AtomicMoveNotSupportedException`; this code has no fallback to a non-atomic overwrite.

When atomic replacement is supported, a process-only interruption leaves the existing target at either the old
or new complete version while the OS remains running. An interruption can leave a temporary file behind.
The save path does not explicitly force file contents or directory metadata to durable storage, so this mechanism
does not establish recovery guarantees after an OS crash or power loss. User preferences use a separate, direct
JSON write through `JsonUserPrefsStorage` and do not have this atomic-replacement protection.

A confirmed `delete` runs through `executeAtomicCommand` like any other command. Commands that do not change data,
such as `list`, `search`, `filter`, and `members`, run directly on the live model without saving. Reminder
dismissal uses its own save path, described in [Event reminders](#event-reminders).

#### Design considerations

**Aspect: How a command is prevented from taking effect when saving fails**

* **Alternative 1 (current choice):** Execute the command on a copy of the model, save the copy, then replace the
  live model's data.
  * Pros: Works for every command that changes data without command-specific rollback code. The UI never shows
    an edit that was not saved.
  * Cons: Copies all data for each command, which is acceptable for the expected [typical load](#glossary).

* **Alternative 2:** Execute the command on the live model, and restore a snapshot if saving fails.
  * Pros: No copy is needed when saving succeeds.
  * Cons: The UI observes the model, so it briefly shows a change that is then reverted.

### Event reminders

The **Upcoming events** panel lists [reminders](#glossary) for events starting soon, ordered by start time and then by name.

#### Implementation

Reminders are worked out from the saved events whenever they are needed, rather than being stored.

**Finding upcoming events.** [`UpcomingEventFinder`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/reminder/UpcomingEventFinder.java)
decides which events qualify, using a `Clock` so that tests can fix the current time:

* An event with a start time qualifies from 24 hours before its start until the start itself, inclusive.
* An event with only a start date qualifies throughout the day before that date and the date itself.

It returns the qualifying events ordered by start, then by name ignoring case. A date-only start counts as midnight
for ordering only.

**Keeping the reminder list current.** [`ReminderService`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/reminder/ReminderService.java)
holds the observable list that `ReminderPanel` displays. `ReminderService#refreshReminders()` rebuilds it from the
finder's results, leaving out archived events and events whose reminder has been dismissed. The list is refreshed:

* when `LogicManager` is created, so reminders are ready at launch.
* after every command, in a `finally` block of `LogicManager#execute`, so that adding, editing, archiving, or deleting
  an event updates the panel straight away, even if the command fails.
* once a minute by a `Timeline` in `MainWindow`, so that reminders appear and expire as time passes.
* when the main window is shown again after being hidden.

The list is only replaced when its contents change, which avoids redrawing the panel every minute.

**Remembering dismissals.** Each `Event` stores the start values whose reminders were dismissed, in
`dismissedReminderStarts`. `Event#isReminderDismissed()` checks whether the current start is among them, so a
dismissal applies to one particular start:

* Renaming the event or changing only its end keeps the dismissal, because the start is unchanged.
* `Event#withDetails`, used by `editevent`, clears the history whenever the start changes, including a change back
  to an earlier start. The reminder for the new start can then appear.

The history is saved with the event, so a dismissal survives restarting the app.

**Dismissing a reminder.** `dismiss INDEX` and the panel's **Dismiss** button both hide a reminder through the same
`ReminderService`, which `LogicManager` creates and passes to `DismissCommand`.

The sequence diagram below shows `dismiss 1`, both when the dismissal is saved and when saving fails.

<puml src="diagrams/DismissSequenceDiagram.puml" width="700" alt="Interactions when a reminder is dismissed" />

`DismissCommand` checks the index against the reminder list as last shown, so that the index matches the panel, and
rejects it if that reminder is no longer upcoming. It then asks `ReminderService` to dismiss that event's reminder.
Like other commands that change data, the service saves an updated copy of the event before changing the live event,
and keeps the event's details, members, and attendance unchanged. It saves with
`Storage#saveCoordiMateIfUnchanged`, which refuses to overwrite a data file that another window has changed.

If the data file cannot be read or saved, or another window has changed it, the dismissal is still kept for the current session, so the reminder
disappears, and the command shows a **Not saved** warning instead of an error. The reminder may then return after a
restart. Because `ReminderService` handles the save itself, `LogicManager` does not run `dismiss` through
`executeAtomicCommand`.

#### Design considerations

**Aspect: How reminders follow the passage of time**

* **Alternative 1 (current choice):** Recompute the reminders once a minute, and after every command.
  * Pros: Simple, and correct after the computer sleeps or its clock changes, since each refresh reads the current
    time. Adding, editing, and deleting events need no extra code to update reminders.
  * Cons: A reminder can appear or disappear up to a minute late.

* **Alternative 2:** Schedule a timer for each event's next eligibility change.
  * Pros: Reminders change at exactly the right moment.
  * Cons: Every event change must cancel and reschedule its timers, and timers can fire at the wrong moment after a
    sleep or clock change.

**Aspect: How a dismissal is remembered**

* **Alternative 1 (current choice):** Store the dismissed start values in the event.
  * Pros: The dismissal is saved with the event, and stops applying as soon as the start changes, because
    `Event#isReminderDismissed()` compares against the current start. Code that changes the start without going
    through `Event#withDetails` therefore cannot leave the moved event dismissed by mistake.
  * Cons: Adds a field to every event in the data file.

* **Alternative 2:** Store a single "dismissed" flag in the event.
  * Pros: Simpler data.
  * Cons: Every code path that changes the start must remember to clear the flag. One that forgets would leave the
    moved event silently dismissed.

### Event attendance

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

The class diagram below shows how these classes relate. The `organisation` criterion is a plain lambda, so it is not
shown.

<puml src="diagrams/FilterClassDiagram.puml" width="700" />

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

All the updated events are built before anything is changed. If an event that lists the contact already has a
member with the new name, the rename is rejected and no data is changed. When loading, a data file in which an event lists a member who is not
a saved contact is rejected.

The activity diagram below summarises how `CoordiMate` handles a renamed or deleted contact.

<puml src="diagrams/EventSyncActivityDiagram.puml" width="380" />

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

#### Implementation

[`LeaveDate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/LeaveDate.java)
implements the date check for the [on leave](#glossary) status. `Person` stores it as an optional field, like
birthday and note, which is set with `lv/` in `add` and `edit`, and cleared with an empty `lv/`.
`Person#isOnLeave(LocalDate today)` delegates the date comparison to `LeaveDate#covers`.

A date entered with `lv/` must not be in the past, but any real date is accepted when loading the data file, so
that leave which has ended can still be loaded.

**How leave ends.** Nothing is changed when a leave date passes. Instead, the UI asks each contact whether it is on
leave on the current date, and shows the **On leave until** badge only if it is. The ended date stays in the data
file until the user replaces or clears it. Code that copies a contact, such as `CoordiMate#setTag` and
`CoordiMate#removeTag`, must pass the leave date on so that it is not dropped.

The state diagram below shows how a contact's leave changes. **Leave ended** looks the same as **Not on leave** in the
app; the difference is only that the old date is still saved.

<puml src="diagrams/LeaveStateDiagram.puml" width="560" />

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

`archive`, `unarchive`, `archiveevent`, and `unarchiveevent` implement [archiving](#glossary).
`list archived` selects the archive view, and `list` returns to the active view.

#### Implementation

**The archive flag.** [`Person`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/Person.java)
and [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java)
each have a boolean `archived` field. Both are immutable, so archiving returns a copy from `withArchived(boolean)`,
and every other copy method passes the flag on. `Person` takes `archived` as a parameter of its full constructor,
which every copy of an existing contact uses, so the compiler makes each copy, such as `EditCommand` and the tag
updates in `CoordiMate`, pass the flag on.

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

### First-launch tutorial

The tutorial is a step-by-step tour of the main window, shown automatically on the first launch.

#### Implementation

[`TutorialWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/TutorialWindow.java)
is an overlay inside the main window, not a separate window. It holds a fixed list of `TutorialStep` records, each
with a title, a description, where to place the step's card, and a `TutorialTarget` naming the part of the main window
to highlight.

`TutorialWindow` handles the tour itself, and `MainWindow` connects it to the rest of the window:

* **Changing steps.** When the step changes, `TutorialWindow` calls the step-change handler set by `MainWindow`.
  `MainWindow#showTutorialStep` switches to the tab that the step needs, finds the node for its `TutorialTarget`, and
  passes it to `TutorialWindow#highlight`. The overlay then shades everything except that node and places the step's
  card beside it.
* **Blocking the app.** While the tutorial is showing, event filters on the overlay and on the main window consume
  every mouse and key event that is not aimed at the tutorial card. `MainWindow#showTutorial` also disables the command
  box. This prevents commands from changing the lists that the tour points at.
* **Keyboard control.** `TutorialWindow#handleShortcut` moves between steps with the Left and Right arrow keys and
  closes the tour with Esc. Tab cycles focus through the card's buttons without leaving the card.
* **Normal view.** `MainWindow#showTutorial` calls `Logic#showActiveItems()` first, because the archived view hides
  the lists and commands that the tour describes.

**Showing it once.** `UserPrefs` stores whether the tutorial has been seen. On launch,
`MainWindow#showTutorialOnFirstLaunch()` opens the tutorial only if it has not. Finishing or closing the tour calls
`Logic#setHasSeenTutorial(true)`, and the preference is saved with the other user preferences when the app exits.
If the preferences file could not be read at launch, `MainApp` does not overwrite it, so the tutorial is shown again
on the next launch.
The tutorial can be opened again at any time with the `tutorial` command, the **Tutorial** item in the top bar, or
F2. `TutorialCommand` returns a `CommandResult` that asks `MainWindow` to show the tutorial.

#### Design considerations

**Aspect: How the tutorial is displayed**

* **Alternative 1 (current choice):** An overlay inside the main window that highlights the real controls.
  * Pros: Users see the actual tabs, cards, and command box that each step describes, and the overlay can never be
    separated from the window it explains.
  * Cons: Each step needs a target node, and the overlay must block input to the window underneath.

* **Alternative 2:** A separate window with screenshots and text.
  * Pros: Independent of the main window's layout.
  * Cons: Screenshots go out of date whenever the UI changes, and users must map each picture onto the real window.

### Keyboard navigation and command history

CoordiMate can be used without a mouse. Besides typing commands, users can move between cards and views with single
keys, and recall earlier commands.

#### Implementation

**Interaction modes.** `MainWindow` keeps track of one of two `InteractionMode`s, shown in the label beside the command
box:

* **Command mode**, the default, in which keys go to the command box.
* **Navigation mode**, in which the command box is disabled and single keys act on the selected tab: J and K select the
  next and previous card, and S switches to the next view.

The state diagram below shows how the user moves between the two modes.

<puml src="diagrams/InteractionModeStateDiagram.puml" width="520" />

`MainWindow#handleKeyboardInput` is a key filter on the whole window. In Command mode it only reacts to Esc, which
enters Navigation mode. In Navigation mode it handles J, K, and S, and consumes every other unmodified key so that it
does nothing. Navigation mode returns to Command mode when I is **released**, rather than pressed, so that the I does
not also appear in the command box. Clicking a card list enters Navigation mode, and clicking the command box or a
command row in the sidebar returns to Command mode.

Mode changes are ignored while the tutorial is showing, and while a `delete` is waiting for confirmation, so that the
confirmation stays in the command box.

**Moving between cards.** `CardNavigation` moves the selection of a card list by one card and scrolls it into view.
With nothing selected, J selects the first card and K the last. At either end of the list, the selection stays where
it is, rather than wrapping around. `PersonListPanel`, `EventListPanel`, and the tag list all use it, so the three
tabs behave the same way.

**Command history.** [`CommandBox`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/CommandBox.java)
keeps every submitted command in a list, including commands that failed, so that they can be corrected and run again.
The Up and Down keys move through the list. The text being typed before browsing is kept as a draft and restored by
pressing Down past the newest command. Submitting a command, or inserting one from the sidebar, resets browsing to the
end of the list. Answers to a `delete` confirmation are not recorded, and Up and Down do nothing while one is pending,
so that `y` is never recalled as a command. The history is kept only for the current session.

#### Design considerations

**Aspect: How single-key navigation coexists with typing**

* **Alternative 1 (current choice):** A separate Navigation mode, entered with Esc.
  * Pros: Single keys such as J can move between cards without a modifier, as in many keyboard-driven tools, while
    the same keys still type normally in Command mode.
  * Cons: Users must know which mode they are in, so the current mode is always shown beside the command box.

* **Alternative 2:** Shortcuts with a modifier key, such as Ctrl+J, available at all times.
  * Pros: No modes to learn.
  * Cons: Common modifier shortcuts already have meanings in text fields and on different operating systems.

**Aspect: Whether command history is saved**

* **Alternative 1 (current choice):** Keep the history only for the current session.
  * Pros: Nothing extra is written to disk, and the history cannot be corrupted or grow without limit.
  * Cons: The history is lost when the app closes.

* **Alternative 2:** Save the history with the user preferences.
  * Pros: Commands can be recalled across sessions.
  * Cons: Adds another file format to maintain, and saves commands that may contain contacts' personal details.

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

Priority expresses the importance of a story, not its implementation status. The tables below separate current
features from ideas that are not implemented.

#### Current features

| Priority | As a … | I want to … | So that I can … |
|----------|--------|-------------|----------------|
| `* * *` | first-time user | view a tutorial | learn how to navigate CoordiMate |
| `* *` | returning user | access the tutorial again | refresh my memory when I forget how to use CoordiMate |
| `* *` | EXCO member | view command formats and examples in Help | check how to enter an unfamiliar command |
| `* * *` | EXCO member | save a contact's name, phone number, email address, and role | keep track of the people I know |
| `* * *` | EXCO member | edit a saved contact | correct or update the contact's information |
| `* * *` | EXCO member | confirm before deleting a contact | remove an unwanted entry after checking that I selected the right person |
| `* *` | EXCO member | record a contact's address, organisation, and note | keep relevant background information at hand |
| `* *` | EXCO member | record a contact's birthday | remember to wish the contact well |
| `* * *` | EXCO member | have data changes saved automatically | retain my work across restarts without a separate save command |
| `*` | EXCO member entering data quickly | enter phone numbers containing spaces, hyphens, or brackets | avoid manually removing accepted phone formatting |
| `* * *` | event lead | search contacts by name, phone number, email address, address, or organisation | retrieve specific contact information quickly |
| `* * *` | event lead | filter contacts by tag, event membership, or organisation, including combined criteria | retrieve contacts in the groups I need |
| `* *` | experienced user | recognise tags by their text labels and built-in colours | identify contact categories at a glance |
| `* * *` | EXCO member | assign tags to contacts | categorise contacts for later retrieval |
| `* *` | EXCO member | choose from a set of common default tags | categorise contacts quickly |
| `* *` | EXCO member | create custom tags | categorise contacts in ways not covered by the default tags |
| `* *` | EXCO member | rename or delete a saved tag | keep the tag list and affected contacts up to date |
| `* *` | EXCO member | select a tag and view its contacts | see who belongs to that category |
| `* * *` | EXCO member | create an event with a name, start date/time, and end date/time | organise contacts and attendance around a scheduled activity |
| `* *` | EXCO member | edit an event's name or schedule | keep event details accurate when plans change |
| `* *` | EXCO member | delete an event | remove an event and its assignments and attendance records when no longer needed |
| `* * *` | EXCO member | assign one or more contacts to an event using permanent member IDs | track who is involved in that event |
| `* * *` | EXCO member | remove one or more contacts from an event | correct assignments when someone is no longer involved |
| `* * *` | EXCO member | view all members assigned to an event | see the committee allocation without overlooking a participant |
| `* *` | EXCO member | create future events and assign members to them | plan committee allocation ahead of time |
| `*` | forgetful EXCO member | see upcoming event reminders while CoordiMate is running | avoid missing events |
| `*` | EXCO member | dismiss an upcoming event reminder | hide a reminder I have already acknowledged |
| `* * *` | EXCO member | mark an assigned member as present or absent, and correct that status later | keep an accurate attendance record |
| `* *` | EXCO member | view the counts of present, absent, and unmarked members for an event | gauge turnout and identify incomplete attendance records |
| `* *` | EXCO member | see each member's attendance status in the event roster | identify absent members to follow up with |
| `*` | EXCO member | archive contacts | declutter my active contact list without permanently deleting entries |
| `*` | EXCO member | archive events | keep my event list focused on current work |
| `*` | EXCO member | view and restore archived contacts and events | retrieve historical information and return items to active use |
| `*` | EXCO member | mark a contact as on leave through a specified date | see their temporary unavailability until the leave period ends |
| `* *` | EXCO member | switch between the Contacts, Events, and Tags views | navigate between related information easily |
| `* *` | user who prefers the keyboard | browse cards and switch views using keyboard shortcuts | inspect saved information without reaching for the mouse |
| `* *` | returning user | recall earlier commands from the current session | reuse commands without retyping them |
| `*` | EXCO member | clear all contacts, events, and custom tags | start a new dataset with the default tags |

#### Future ideas (not implemented)

| Priority | As a … | I want to … | So that I can … |
|----------|--------|-------------|----------------|
| `*` | EXCO member | attach multiple phone numbers or email addresses to a contact | record the contact's different personal and work details |
| `*` | EXCO member | attach a photo to a contact | associate the contact's face with their name |
| `*` | EXCO member entering data quickly | enter Telegram handles with or without the `@` symbol | avoid manually reformatting handles |
| `* * *` | event lead | filter contacts by role | retrieve contacts with a particular responsibility |
| `* *` | EXCO member | filter contacts by an assigned colour | find contacts in a particular colour group quickly |
| `*` | EXCO member | have urgent contacts appear at the top of the contact list | identify them quickly |
| `* *` | EXCO member | assign and change a contact's colour, including choosing a default colour | keep visual groupings accurate when roles or groups change |
| `*` | user with limited time | have the most frequently used event appear first | access it without extra navigation |
| `*` | EXCO member | view the edit history of attendance records | see when a member's status was changed and avoid disputes |
| `*` | EXCO member | export attendance records | use them for reporting or record-keeping |
| `*` | user who values a pleasant interface | switch between light and dark modes | adjust the display to suit my environment |

### Use cases

(For all use cases below, the **System** is `CoordiMate` and the **Actor** is an EXCO member, unless specified otherwise.)

<box type="info" seamless>

**Note:** In every use case that changes data, if CoordiMate cannot load or save the local data file, it informs
the EXCO member and makes no changes. This extension is not repeated in each use case below. The only exception is
dismissing a reminder ([UC12](#uc12-dismiss-an-event-reminder)).
</box>

#### UC01: Add a contact

**MSS**

1. EXCO member requests to add a contact, providing the contact's name, phone number, email address, role, and any
   optional details or tags.
2. CoordiMate saves the contact, creating any new tags, and confirms that the contact was saved.

   Use case ends.

**Extensions**

* 1a. A required detail is missing, a detail is invalid or repeated, or an unknown field is given.
  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member corrects the contact details.

    Steps 1a1-1a2 are repeated until the details are valid.<br>
    Use case resumes from step 2.

* 1b. More than 15 tags are given.
  * 1b1. CoordiMate informs the EXCO member of the tag limit.

    Use case resumes from step 1.

* 1c. The supplied details would create a [duplicate contact](#glossary).
  * 1c1. CoordiMate informs the EXCO member that the contact already exists and makes no changes.

    Use case ends.

#### UC02: Edit a contact

**MSS**

1. EXCO member requests to edit a contact, identifying it by its member ID or by its exact name, phone number, or
   email address, and providing the new details.
2. CoordiMate updates the contact, updates its name in every event it is a member of, and shows the updated
   details.

   Use case ends.

**Extensions**

* 1a. No new details are given, a detail is invalid or repeated, or an unknown field is given.
  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member corrects the details.

    Steps 1a1-1a2 are repeated until the details are valid.<br>
    Use case resumes from step 2.

* 1b. No contact matches the given identifier.
  * 1b1. CoordiMate informs the EXCO member that no matching contact was found.

    Use case ends.

* 1c. More than one contact matches the given name.
  * 1c1. CoordiMate lists the matching contacts with their member IDs, and requests a member ID, phone number, or
    email address instead.
  * 1c2. EXCO member provides one of the requested identifiers.

    Use case resumes from step 2.

* 1d. The proposed changes would create a [duplicate contact](#glossary).
  * 1d1. CoordiMate informs the EXCO member of the conflict and makes no changes.

    Use case ends.

* 1e. The tag changes would give the contact more than 15 tags.
  * 1e1. CoordiMate informs the EXCO member of the tag limit and makes no changes.

    Use case resumes from step 1.

#### UC03: Delete a contact

**MSS**

1. EXCO member requests to delete a contact, identifying it by its member ID or by its exact name, phone number, or
   email address.
2. CoordiMate shows the matching contact and requests confirmation.
3. EXCO member confirms the deletion by entering `y` or `Y`, ignoring surrounding whitespace.
4. CoordiMate deletes the contact, removes it from every event it is a member of along with its attendance records,
   and confirms the deletion.

   Use case ends.

**Extensions**

* 1a. No identifier, more than one identifier, or an invalid member ID is given.
  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member provides one valid identifier.

    Use case resumes from step 2.

* 1b. No contact matches the given identifier.
  * 1b1. CoordiMate informs the EXCO member that no matching contact was found.

    Use case ends.

* 1c. More than one contact matches the given name.
  * 1c1. CoordiMate lists the matching contacts with their member IDs, and requests a member ID, phone number, or
    email address instead.
  * 1c2. EXCO member provides one of the requested identifiers.

    Use case resumes from step 2.

* 3a. EXCO member enters anything other than `y` or `Y`, ignoring surrounding whitespace.
  * 3a1. CoordiMate cancels the deletion and leaves the contact unchanged. The input is consumed and is not executed as a command.

    Use case ends.

#### UC04: Search contacts

**MSS**

1. EXCO member requests to search contacts using a keyword or phrase.
2. CoordiMate shows the contacts whose name, phone number, email address, address, or organisation contains the
   keyword, ignoring case, and the number found. Phone matching uses [normalised phone numbers](#glossary).
   Extra spaces in the keyword and in addresses are treated as one space.

   Use case ends.

**Extensions**

* 1a. The keyword is empty or blank, including a blank quoted phrase such as `search ""` or `search "   "`.
  * 1a1. CoordiMate informs the EXCO member of the error.

    Use case ends.

* 2a. No contact matches the keyword.
  * 2a1. CoordiMate shows an empty list and informs the EXCO member that no matching contacts were found.

    Use case ends.

#### UC05: Filter contacts

**MSS**

1. EXCO member requests to list the contacts that match one or more criteria, each a tag, an event, or an
   organisation.
2. CoordiMate shows the contacts that match every criterion, and the number found.

   Use case ends.

**Extensions**

* 1a. No criteria are given, a criterion is empty, or a criterion is not a tag, event, or organisation.
  * 1a1. CoordiMate informs the EXCO member of the error and leaves the displayed list unchanged.

    Use case ends.

* 1b. A criterion's value does not match any saved tag, event, or organisation.
  * 1b1. CoordiMate informs the EXCO member that the value was not found and leaves the displayed list unchanged.

    Use case ends.

* 2a. No contact matches every criterion.
  * 2a1. CoordiMate shows an empty list and informs the EXCO member that no contacts match.

    Use case ends.

#### UC06: Mark a contact as on leave

**MSS**

1. EXCO member requests to mark a contact as on leave until a given date, while adding the contact (UC01) or
   editing it (UC02).
2. CoordiMate saves the date and shows the contact as on leave until that date.
3. After the date has passed, CoordiMate stops showing the contact as on leave, without any action from the EXCO
   member.

   Use case ends.

**Extensions**

* 1a. The date is not a real date, or is in the past.
  * 1a1. CoordiMate informs the EXCO member of the error and makes no changes.

    Use case resumes from step 1.

* 2a. EXCO member requests to clear the contact's leave before the date has passed.
  * 2a1. CoordiMate removes the leave date and stops showing the contact as on leave.

    Use case ends.

#### UC07: Create an event

**MSS**

1. EXCO member requests to create an event, providing its name, start date/time, and end date/time.
2. CoordiMate creates the event and confirms its name, start, and end.

   Use case ends.

**Extensions**

* 1a. A detail is missing, empty, repeated, or incorrectly formatted, or an unknown field is given.
  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member corrects the details.

    Steps 1a1-1a2 are repeated until the details are valid.<br>
    Use case resumes from step 2.

* 1b. The start is after the end.
  * 1b1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 1.

* 1c. An event with the same name already exists, ignoring case.
  * 1c1. CoordiMate informs the EXCO member that the event already exists, and how to restore it if it is
    [archived](#uc13-archive-and-restore-a-contact).

    Use case ends.

#### UC08: Edit an event

**MSS**

1. EXCO member requests to change an event's name, start, or end.
2. CoordiMate updates the event, keeping its members and attendance records, and confirms the new details.

   Use case ends.

**Extensions**

* 1a. No new details are given, or a detail is empty or incorrectly formatted.
  * 1a1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 1.

* 1b. No saved event has the given name.
  * 1b1. CoordiMate informs the EXCO member that the event does not exist.

    Use case ends.

* 1c. Another event already has the new name, ignoring case.
  * 1c1. CoordiMate informs the EXCO member of the conflict and makes no changes.

    Use case ends.

* 1d. The new start would be after the end.
  * 1d1. CoordiMate informs the EXCO member of the error and makes no changes.

    Use case ends.

Deleting an event is similar: the EXCO member identifies the event by name, and CoordiMate deletes it together with
its member list and attendance records, without asking for confirmation. The contacts themselves are kept.
Extension 1b also applies.

#### UC09: Assign members to an event

**MSS**

1. EXCO member requests to assign one or more contacts, identified by their member IDs, to an event.
2. CoordiMate adds the contacts to the event's members, with no attendance marked, and confirms how many were
   assigned.

   Use case ends.

**Extensions**

* 1a. No member IDs are given, or a member ID is invalid.
  * 1a1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 1.

* 1b. No saved event has the given name, or no contact has one of the member IDs.
  * 1b1. CoordiMate informs the EXCO member of the error and assigns no contacts.

    Use case resumes from step 1.

* 1c. The event or one of the contacts is archived.
  * 1c1. CoordiMate informs the EXCO member that it must be restored first, and assigns no contacts.

    Use case ends.

* 1d. The same member ID is given more than once.
  * 1d1. CoordiMate processes each distinct member ID once, without reporting an error for repeated IDs.

    Use case resumes from step 2.

* 2a. Some of the contacts are already members of the event.
  * 2a1. CoordiMate assigns only the other contacts, and reports how many were already assigned.

    Use case ends.

* 2b. All of the contacts are already members of the event.
  * 2b1. CoordiMate informs the EXCO member that no changes were made.

    Use case ends.

Removing members from an event is similar, with these differences:

* In step 2, CoordiMate removes the contacts from the event's members, together with their attendance records for
  that event.
* In extensions 2a and 2b, the contacts are not members of the event, instead of already being members.
* Extension 1c applies only to an archived event. An archived contact can still be removed.

#### UC10: View members of an event

**MSS**

1. EXCO member requests to view the members of an event.
2. CoordiMate shows only the event's members in the contact list, and confirms how many are shown.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name.
  * 1a1. CoordiMate informs the EXCO member that the event does not exist and leaves the contact list unchanged.

    Use case ends.

* 2a. The event has no members.
  * 2a1. CoordiMate shows an empty contact list and informs the EXCO member that the event has no members.

    Use case ends.

* 2b. Some of the event's members are outside the current archive view (archived members in the normal view, or active members in the archived view).
  * 2b1. CoordiMate shows only the members in the current archive view, and informs the EXCO member how many members are hidden.

    Use case ends.

* 2c. While the members are shown, a member is assigned or removed, or the event is renamed.
  * 2c1. CoordiMate updates the shown list to the event's current members.

    Use case ends.

#### UC11: Record event attendance

**MSS**

1. EXCO member views the members of an event (UC10).
2. EXCO member marks a member of the event as present or absent.
3. CoordiMate records the attendance status and confirms the update.
4. Steps 2-3 are repeated for each member whose attendance is to be recorded.

   Use case ends.

**Extensions**

* 2a. No saved event has the given name, or the contact is not a member of the event.
  * 2a1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 2.

* 2b. The status is neither present nor absent.
  * 2b1. CoordiMate informs the EXCO member of the valid statuses.

    Use case resumes from step 2.

* 2c. The member already has the given status.
  * 2c1. CoordiMate informs the EXCO member that no changes were made.

    Use case resumes from step 4.

#### UC12: Dismiss an event reminder

**Preconditions:** At least one event has an eligible, undismissed [reminder](#glossary).

**MSS**

1. CoordiMate shows numbered reminders in the **Upcoming events** panel, in order of start time.
2. EXCO member requests to dismiss a reminder, identifying it by its number.
3. CoordiMate hides the reminder, saves the dismissal, and renumbers the remaining reminders.

   Use case ends.

**Extensions**

* 2a. No reminder has the given number.
  * 2a1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 2.

* 3a. CoordiMate cannot load or save the local data file.
  * 3a1. CoordiMate hides the reminder for the current session only, and warns the EXCO member that it may reappear
    after restarting.

    Use case ends.

* 3b. The EXCO member later changes the event's start.
  * 3b1. CoordiMate shows the reminder again if the new start qualifies for a [reminder](#glossary).

    Use case ends.

#### UC13: Archive and restore a contact

**MSS**

1. EXCO member requests to archive a contact, identifying it by its member ID.
2. CoordiMate hides the contact from the contact list, keeping its event memberships and attendance records, and
   confirms the archiving.
3. EXCO member requests to view archived contacts and events.
4. CoordiMate shows only the archived contacts and events.
5. EXCO member requests to restore the contact, identifying it by its member ID.
6. CoordiMate returns the contact to the contact list and confirms the restoration.

   Use case ends.

**Extensions**

* 1a. No contact has the given member ID.
  * 1a1. CoordiMate informs the EXCO member of the error.

    Use case ends.

* 1b. The contact is already archived.
  * 1b1. CoordiMate informs the EXCO member that no changes were made.

    Use case ends.

* 5a. The contact is not archived.
  * 5a1. CoordiMate informs the EXCO member that no changes were made.

    Use case ends.

Archiving and restoring an event is similar, with the event identified by its name. While an event is archived, it
has no reminder, its members cannot be changed ([UC09](#uc09-assign-members-to-an-event)), and no new event can
use its name ([UC07](#uc07-create-an-event)).

#### UC14: Manage tags

**MSS**

1. EXCO member requests to create a tag.
2. CoordiMate adds the tag to the tag list and confirms its creation.
3. EXCO member requests to rename a tag.
4. CoordiMate renames the tag, including on every contact that has it, and confirms the change.
5. EXCO member requests to delete a tag.
6. CoordiMate deletes the tag, removes it from every contact that has it, and confirms the deletion.

   Use case ends.

**Extensions**

* 1a. The tag name is invalid, for example because it contains spaces or is longer than 30 characters.
  * 1a1. CoordiMate informs the EXCO member of the error.

    Use case resumes from step 1.

* 1b. A tag with the same name already exists, ignoring case.
  * 1b1. CoordiMate informs the EXCO member that the tag already exists and makes no changes.

    Use case ends.

* 3a. The tag to rename does not exist.
  * 3a1. CoordiMate informs the EXCO member that the tag does not exist.

    Use case ends.

* 3b. The new name is invalid, or another tag already has it.
  * 3b1. CoordiMate informs the EXCO member of the error and makes no changes.

    Use case resumes from step 3.

* 5a. The tag to delete does not exist.
  * 5a1. CoordiMate informs the EXCO member that the tag does not exist.

    Use case ends.

### Non-Functional Requirements

These are acceptance targets, not measured results. Performance tests should use one running CoordiMate instance
and a valid data file on writable local storage. Record the OS, Java version, processor, memory, and dataset with
the results. Speed and onboarding targets require timed trials with users from the target user profile.

1.  Should run on the [mainstream OS platforms](#glossary) with Java `25`, targeting the OS and Java configurations
    used by the project's CI workflow. The packaged JAR should also receive a manual launch check on each target
    platform before release compatibility is claimed.
2.  Should store and reload a dataset at the maximum [typical load](#glossary). Successfully saved contact details,
    member IDs, tags, event schedules, assignments, attendance, archive flags, leave dates, and reminder dismissals
    should survive a normal restart. Performance at this load is assessed using NFR 4.
    This is a capacity target for the whole local dataset, not an enforced data limit. The app does not partition
    data by semester.
3.  For users familiar with CoordiMate's keyboard shortcuts, a short sequence of switching views and selecting
    adjacent cards should take less time on average using shortcuts than using the mouse. Compare repeated
    trials by the same user from the same starting view and selection, including keyboard mode changes.
4.  Under the [typical load](#glossary), every command that reads or changes contact, tag, event, attendance, or reminder data should have a median response time of at most 2 seconds over repeated trials on the recorded test machine. This includes `delete`, `unassign`, `archiveevent`, `unarchiveevent`, `dismiss`, and `clear`. Measure from pressing Enter to visible feedback and completion of the resulting view update, including any save. Exclude application startup and time waiting for user confirmation; measure the prompt and the response after confirmation separately when applicable.
5.  Contact, tag, event, attendance, archive, and reminder features, including built-in Help and the tutorial,
    should work without an internet connection. Opening the online User Guide requires internet access.
6.  If only the application process terminates during a save while the OS remains running, an existing valid
    local data file should retain either the previous complete dataset or the new complete dataset. This assumes
    that the filesystem and Java provider support atomic replacement, as described in
    [Saving data atomically](#saving-data-atomically). It covers application data, excluding user preferences,
    OS crashes, power loss, storage hardware failure, and external file modification.
7.  Application data should be stored in human-readable JSON that users can inspect and back up with ordinary
    file tools. Manual edits should be made with the app closed and must satisfy the saved-data constraints.
8.  Should be distributed as a single application JAR with no CoordiMate installer, assuming the required Java
    environment is already installed.
9.  After completing or closing the tutorial, a first-time user from the target user profile should be able to
    add one contact within 10 minutes without assistance from another person. Supply valid, unique name, phone,
    email, and role values as task input. Start timing when the command box is ready for input and stop when
    the saved contact is visible with those details and a success message. Consulting built-in Help, command
    hints, or the User Guide and correcting commands count towards the limit; installation, launch, and tutorial
    reading time do not.
10. Every [colour-coded tag](#glossary) should display its name as text so that contact categories can be
    distinguished without relying on colour.

### Glossary

* **Archive**: To mark a contact or event as archived, hiding it from the active lists while preserving its details, event assignments, and attendance records.
* **Assigned member**: A contact who has been assigned to a particular event. Also referred to as a *member* of that event.
* **Attendance status**: A recorded value of `present` or `absent` for an assigned member at an event. A member with no attendance record is shown as unmarked, not automatically absent.
* **CCA (Co-Curricular Activity)**: A student club or society in a university, such as a band, sports team or interest group.
* **CLI (Command Line Interface)**: An interface operated by typing text commands. CoordiMate accepts these commands in a command box within its graphical window.
* **Colour-coded tag**: A tag whose display colour is assigned by the app based on its name. `EXCO`, `Logistics`, `Sponsor`, and `UniversityStaff` have distinct built-in colours; other tags share a default colour. Users cannot choose tag or contact colours.
* **Command mode**: The keyboard mode for entering commands in the command box.
* **Committee handover**: The transfer of responsibilities and information from an outgoing EXCO to the incoming EXCO.
* **Contact**: A person whose details are saved in CoordiMate and identified by a member ID.
* **Custom tag**: A tag created by the user, to categorise contacts in ways not covered by the default tags.
* **Default tag**: A tag that is available without the user creating it: `EXCO`, `Sponsor`, `UniversityStaff` and `Logistics`.
* **Duplicate contact**: A contact with the same name (ignoring case and normalising spaces), normalised phone number, or email address (ignoring case) as another saved contact, including an archived one.
* **Event**: A CCA activity recorded in CoordiMate, such as a concert or fair, identified by a name that is unique ignoring case and extra spaces.
* **Event lead**: An EXCO member who is in charge of organising a particular event.
* **EXCO (Executive Committee)**: The group of students elected to lead and run a CCA.
* **External contact**: A contact from outside the CCA, such as a sponsor, university staff member, or EXCO member of a collaborating CCA.
* **Future event**: An event whose start date/time is later than the current date/time.
* **Internal contact**: A contact from within the CCA, such as a member, fellow EXCO member or advisor.
* **JAR file**: A single file that packages a Java application so that it can be run without installation.
* **Local data file**: The JSON file on the user's computer where CoordiMate saves the application data listed in NFR 2 under [Non-Functional Requirements](#non-functional-requirements). Its default path is `data/coordimate.json`, relative to the application's working directory.
* **Mainstream OS**: Windows, macOS, or Linux.
* **Member ID**: A unique, permanent positive integer assigned to a contact, displayed as `ID: <number>`, and used in commands across searches, filters, and restarts.
* **Navigation mode**: The keyboard mode in which key presses browse cards and switch views.
* **Normalised phone number**: A phone number with spaces, hyphens and brackets removed, used to detect duplicates. For example, `+65 9123 4567` and `+6591234567` are the same.
* **On leave**: A contact with a saved leave end date is unavailable through that date, inclusive, using the computer's local date.
* **Organisation**: The company or body that a contact belongs to, such as a CCA, sponsor company, or university office.
* **Prefix**: The short label before a value in a command that tells CoordiMate which field it belongs to, such as `n/` for name or `t/` for tag.
* **Reminder**: An in-app notice for an upcoming, non-archived event, shown only while CoordiMate is running. Timed starts qualify from 24 hours before the start through the start time; date-only starts qualify on the preceding day and the start date. Reminders refresh at startup, after commands, and once per minute.
* **Role**: A contact's responsibility or position in relation to the CCA, such as `Logistics Lead` or `Vice-President`.
* **Tag**: A short label attached to a contact to categorise it, such as `EXCO` or `Sponsor`. A contact can have multiple tags.
* **Tutorial**: An in-app introduction to CoordiMate's views, commands, and keyboard navigation.
* **Typical load**: A local dataset of up to 250 contacts and 6 events in total, including archived items, with up to 250 assigned members and their attendance records per event.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

All relative file paths below are relative to the application's working directory. When launching with `java -jar`, this is the terminal's current directory. Use the same working directory when relaunching the app to reload the same data.

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
1. Delete `data/coordimate.json` from the application's working directory.
1. Relaunch the app.

The app then starts with the six sample contacts (member IDs `1` to `6`), the `Freshmen Welcome Night` sample
event, and no custom tags. Deleting contacts by hand is not enough, because member IDs are never reused.
</box>

<box type="info" seamless>

**Note:** Date placeholders use the computer's local time zone and `dd-MM-yyyy` format. `TODAY` is today's date,
`TOMORROW` is tomorrow's date, `FUTURE_DATE` is any date after today, and `PAST_DATE` is any date before today.
</box>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens maximized with a set of sample contacts and a sample event. The first-launch tutorial
      opens automatically and can show the event's assigned members and attendance information. Close it before continuing.

1. Saving window preferences

   1. Unmaximize the window. Resize it to a distinctive size, move it to a different location, and close it.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The app opens unmaximized with the most recent window size and location.

1. Shutting down

   1. Test case: `exit`<br>
      Expected: The app closes. Relaunching it shows the same data as before.

### Tutorial and Help

Closing the tutorial with Esc or **Close** also marks it as seen. Restoring the sample data does not reset this preference, which is stored in `preferences.json` in the application's working directory.

1. Test case: `tutorial`, then press Right Arrow, Left Arrow, Tab, and Shift+Tab.<br>
   Expected: The overlay starts at **Step 1 of 16**. Arrow keys change steps and Tab moves focus between the
   tutorial's enabled buttons. The highlighted area stays bright, and the command box is disabled. Press Esc to close it;
   F2 (Fn+F2 if required) reopens it at step 1. Close it with Esc again.

1. Test case: Close the app. In `preferences.json`, set `hasSeenTutorial` to `false`, leaving the window settings
   unchanged. Relaunch the app and complete the automatically opened tutorial by advancing to step 16 and selecting
   **Finish**, without closing it another way. Close and relaunch the app again.<br>
   Expected: The tutorial opens automatically after resetting the flag, visits Contacts, Events, Tags, and Help,
   and closes after Finish. On the final relaunch, it does not open automatically. Contacts, events, and tags are unchanged.

1. Test case: `help`, then resize the window and scroll its command table.<br>
   Expected: The Help window shows keyboard navigation, command formats, and the User Guide URL. Press Esc to
   close it; CoordiMate stays open. F1 (Fn+F1 if required) opens Help again; select **Close** to dismiss it.

1. Other incorrect commands to try: `tutorial extra`, `help extra`<br>
   Expected: Neither window opens. The status message shows `Invalid command format.`

### Keyboard navigation

1. Prerequisites: Restore the sample data. Run `list`, select Alex Yeoh,
   then click the command box and type `search Bernice` without submitting it.

1. Test case: Press Esc, J, K, then K again.<br>
   Expected: The indicator shows **NAVIGATION**. Bernice is selected after J, then Alex after K. The extra K
   keeps Alex selected at the start of the list. The unfinished command remains in the disabled command box.

1. Test case: Press S three times, using J and K in the Events and Tags views.<br>
   Expected: S cycles Contacts, Events, Tags, then Contacts. J and K navigate the active list; the corresponding
   details update. Returning to Contacts keeps Alex selected. Enter and Up do not submit or replace the draft.

1. Test case: Press I, then Enter.<br>
   Expected: The indicator shows **COMMAND**, the command box regains focus with `search Bernice` unchanged,
   and Enter executes it. Only Bernice Yu is listed.

### Command history

1. Prerequisites: Restore the sample data. Run `list`, then `search alex`.

1. Test case: Type `search Bernice` without submitting it. Press Up twice, then Down twice.<br>
   Expected: The input shows `search alex`, `list`, `search alex`, then the unfinished `search Bernice`.
   No command executes while browsing; Alex remains the only result.

1. Test case: Press Up, change the recalled command to `search Charlotte`, and press Enter. Press Up twice.<br>
   Expected: Charlotte becomes the only result. Up recalls `search Charlotte`, then the original `search alex`;
   editing a recalled command has not overwritten its earlier entry.

1. Test case: Close and relaunch the app, then press Up in the empty command box.<br>
   Expected: The box stays empty because commands from the previous session are not retained.

### Adding a contact

1. Prerequisites: Restore the sample data and run `list`.

1. Test case: `add n/Aisha Tan p/+6591234567 e/aisha@example.com r/Logistics`<br>
   Expected: The Contacts view selects Aisha Tan with `ID: 7` and the supplied details. The status message shows
   `Contact saved successfully: Aisha Tan`.

1. Test case: Repeat the previous command.<br>
   Expected: No contact is added. The status message shows
   `A contact with this name, phone number, or email already exists. No changes were made.`

1. Other incorrect add commands to try: `add n/Ben Ong p/91112222 e/ben@example.com` (missing role),
   `add n/Ben Ong p/abc e/ben@example.com r/Member`<br>
   Expected: No contact is added. The missing-role command reports `Invalid command format.`; the invalid-phone
   command explains the phone-number constraints.

### Editing a contact

1. Prerequisites: Restore the sample data and run `search Charlotte`, showing only member ID `3`.

1. Test case: `edit 3 r/Treasurer o/NUS Student Affairs`<br>
   Expected: All six contacts are listed and Charlotte is selected with the new role and organisation. Her ID
   stays `3`, and other fields are unchanged. Feedback shows `Contact updated successfully:` and her details.

1. Test case: `edit 3 o/`<br>
   Expected: Charlotte's organisation is cleared. Her other details are unchanged.

1. Test case: `edit 3 n/Bernice Yu`<br>
   Expected: No contact changes. The status message shows
   `This update conflicts with another saved contact. No changes were made.`

1. Other incorrect edit commands to try: `edit 3`, `edit 99 r/Treasurer` (where member ID `99` does not exist)<br>
   Expected: No contact changes. The first shows `Provide at least one field or tag operation to edit.`;
   the second shows `No contact exists with member ID: 99. Use an existing member ID.`

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

### Listing contacts and events

1. Prerequisites: Restore the sample data and run `search alex`.

1. Test case: `list`<br>
   Expected: The Contacts view shows all six active contacts again, and Events contains the sample event.
   The status message shows `Listed all contacts.`

1. Test case: `list archived`, then `list`<br>
   Expected: The archived contact and event lists are empty, since nothing is archived. `list` restores the active
   lists. Archiving and restoring entries is tested separately below.

1. Test case: `list all`<br>
   Expected: The lists are unchanged. The status message shows
   `Unknown list option: all. Use list, or list archived to show archived contacts and events.`

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

   1. Prerequisites: Use test data with no active reminders. Create `Zulu Reminder` first with
      `addevent evn/Zulu Reminder st/TOMORROW et/TOMORROW`, then create `alpha Reminder` with the same dates.

   1. Test case: Switch between the Contacts, Events, and Tags views.<br>
      Expected: The Upcoming events panel appears and stays visible, showing `alpha Reminder` at index 1 and
      `Zulu Reminder` at index 2, with their saved dates.

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

   1. Prerequisites: Run `addevent evn/Orientation st/TOMORROW et/TOMORROW`.
      Assign the 1st contact with `assign evn/Orientation c/ID`, using its member ID. A reminder for
      `Orientation` is shown.

   1. Test case: `archiveevent evn/orientation`<br>
      Expected: The **Events** tab opens, even if another tab was selected. The event disappears from it, and its
      reminder disappears. The status message shows `Archived Event Orientation.` In the **Contacts** tab, the
      contact's details still list `Orientation (archived)`.

   1. Test case: `assign evn/Orientation c/ID` with another contact's ID<br>
      Expected: No change. The status message says the event is archived and suggests `unarchiveevent`.

   1. Test case: `addevent evn/Orientation st/TOMORROW et/TOMORROW`<br>
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

### Clearing all data

<box type="warning" seamless>

**Caution:** `clear` deletes saved data immediately and cannot be undone. Back up `data/coordimate.json` first
if it contains data you want to keep.
</box>

1. Prerequisites: Restore the sample data. Run `archive 2`, `archiveevent evn/Freshmen Welcome Night`,
   `addtag t/ClearTest`, and `addevent evn/ClearTest st/TOMORROW et/TOMORROW`.
   Bernice Yu and `Freshmen Welcome Night` are archived; `ClearTest` stays active and has a reminder.

1. Test case: `clear extra`<br>
   Expected: The status message shows `Invalid command format.` No data changes.

1. Test case: `clear`, then `list archived`<br>
   Expected: `clear` reports `CoordiMate has been cleared!` without confirmation. Active and archived contacts
   and events are removed, and the reminder panel hides. Tags contains only `EXCO`, `Sponsor`, `UniversityStaff`,
   and `Logistics`; `ClearTest` is removed. `list archived` shows no archived contacts or events.

1. Test case: Close and relaunch the app, run `list`, then
   `add n/ID Reset p/91234567 e/idreset@example.com r/Member`.<br>
   Expected: The contact and event lists are still empty before the add. ID Reset is then added with `ID: 1`.

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

   1. Prerequisites: The app is running and `data/coordimate.json` exists. Make the data impossible to save: on Windows, open the properties of `data/coordimate.json` and tick **Read-only**; on macOS or Linux, run `chmod a-w data` in the application's working directory.

   1. Test case: `addevent evn/Test st/01-01-2027 et/01-01-2027`<br>
      Expected: No event is added and the **Events** tab is unchanged. The status message shows `Event could not be saved. No changes were made.`

   1. Test case: `add n/Test Person p/91234567 e/testperson@example.com r/Member`<br>
      Expected: No contact is added. The status message shows `Contact could not be saved. No changes were made.`

   1. Test case: Undo the change made in the prerequisites (untick **Read-only**, or run `chmod u+w data`), then repeat the `addevent` command.<br>
      Expected: The event is added.
