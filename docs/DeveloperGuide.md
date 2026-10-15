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

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `HelpWindow` provides an in-app command quick reference. Its read-only, scrollable command table is populated by
`HelpWindow` using the command words defined by the corresponding command classes, while its FXML file controls the
resizable layout. The User Guide URL remains available for more detailed instructions.
The window's **Close** button is marked as the JavaFX cancel button, allowing users to dismiss it by selecting the
button or pressing <kbd>Esc</kbd> without exiting the application.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data. The Contacts view
  displays the filtered contact list beside a details pane, follows a selected contact when an edit replaces that
  list entry, and clears the selection when the contact leaves the list. The details pane displays all saved contact
  fields, showing a placeholder for unset optional fields and no tag chips for an empty tag set.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

<box type="info" seamless>

**Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, the lifeline continues till the end of diagram.
</box>

<box type="info" seamless>

**Note:** This diagram and the architecture sequence diagram above are simplified to show the general flow of a
command. In the actual implementation, `delete` first asks the user for confirmation, and commands that change data
are executed on a copy of the model and saved before the change is applied. Both are described in
[Saving data atomically](#saving-data-atomically).
</box>


How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to a `CoordiMateParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
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
* For unknown or repeated parameters, show the reason followed by the command's usage text.
* For invalid field values, explain the constraint and provide an `Example:` of a valid value.
* For execution errors, explain what failed and how to correct it. Reuse the templates in `Messages` for missing
  contacts, events, and tags, and for unmatched or ambiguous contact identifiers.
* Add `No changes were made.` when an attempted change is rejected without modifying the saved data.

### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="450" />


The `Model` component,

* stores CoordiMate data, including all `Person` objects (which are contained in a `UniquePersonList` object).
* stores all `Event` objects. Each `Event` records its assigned members as an ordered list of contact `Name`s. `CoordiMate` keeps these lists in sync with the contacts: deleting a contact removes their name from every event, and renaming a contact updates it.
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)


<box type="info" seamless>

**Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `CoordiMate`, and each `Person` references tags from that list. This lets `CoordiMate` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="550" />

The `Storage` component,
* can save both CoordiMate data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonCoordiMateStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

Previously saved digit-only phone numbers with at least three digits remain valid model data, so older JSON files can
still load. The command parser applies the current 7–15-digit rule whenever a user supplies a phone number for `add`
or `edit`; editing another field does not require replacing a legacy phone number.

### Common classes

Classes used by multiple components are in the `coordimate.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Saving data atomically

A command that changes data either takes effect in full, both in the app and in the data file, or not at all.
This keeps the displayed data and the saved data identical even when the data file cannot be read or written,
and meets the requirement that saved data is not lost if the app terminates unexpectedly.

#### Implementation

Two mechanisms work together.

**1. Commands run on a copy of the model.** [`LogicManager#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/LogicManager.java)
passes every command that changes data to `LogicManager#executeAtomicCommand`. These are `AddCommand`, `EditCommand`,
`DeleteCommand`, `ClearCommand`, `AddEventCommand`, `EditEventCommand`, `DeleteEventCommand`, `AssignCommand`,
`UnassignCommand`, `MarkAttendanceCommand`, and the tag commands that extend `MutatingTagCommand`.

`executeAtomicCommand` does the following:

1. It calls `Storage#readCoordiMate()` to check that the existing data file can still be loaded. If it cannot,
   the command is rejected with a load error and the file is left untouched, so that the user can repair it.
   A missing data file is not an error.
1. It creates a second `ModelManager`, the _candidate_, from a copy of the live model's data. The candidate's
   filtered contact list is set to show the same contacts as the live displayed list, so that a contact index
   typed by the user refers to the same contact in both models.
1. It executes the command on the candidate. If the command fails, the candidate is discarded.
1. It saves the candidate's data with `Storage#saveCoordiMate`. If saving fails, the command is rejected with a
   save error such as `Event could not be saved. No changes were made.` and the candidate is discarded.
1. Only then does it call `Model#setCoordiMate` to replace the live model's data with the candidate's data,
   which updates the UI.

The live model is not touched until step 5, so a failure at any earlier step leaves the app exactly as it was.

The sequence diagram below shows these steps for an `addevent` command that succeeds. Parsing is omitted.

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

**Commands that do not change data**, such as `list`, `search`, `filter`, `members`, and `listtags`, are executed
directly on the live model. As in AddressBook-Level3, `LogicManager` still saves the model afterwards. This has two
consequences that developers should be aware of:

* If the data file is invalid, such a command replaces it with the data currently in the app.
* If the data file cannot be written, such a command reports a save error even though it changed no data.

#### Design considerations

**Aspect: How a command is prevented from taking effect when saving fails**

* **Alternative 1 (current choice):** Execute the command on a copy of the model, save the copy, then replace the
  live model's data.
  * Pros: Works for every command without command-specific code. The UI never shows a change that was not saved.
  * Cons: Copies all data for each command, which is acceptable for the expected 250 contacts. Each new command
    that changes data must be added to the list in `LogicManager#execute`.

* **Alternative 2:** Execute the command on the live model, and restore a snapshot if saving fails.
  * Pros: No copy is needed when saving succeeds.
  * Cons: The UI observes the model, so it briefly shows a change that is then reverted.

* **Alternative 3:** Have each command undo its own change if saving fails.
  * Pros: Uses the least memory.
  * Cons: Every command needs correct undo logic, which is easy to get wrong.

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
| `event` | the name of a saved event | it is a member of that event |
| `organisation` | the organisation of at least one saved contact | its organisation is that value |

If a value matches nothing, the command throws a `CommandException` with `No matching value found.` before the
displayed list is changed. This tells the user about a mistyped value instead of showing an empty list. Otherwise
the predicates are combined with `Predicate#and` and passed to `Model#updateFilteredPersonList`.

`filter` does not change data, so it is executed directly on the live model.

<box type="info" seamless>

**Note:** The `event` predicate holds the `Event` found when the command was executed. Since an `Event` is
immutable, the filtered list does not follow later changes to that event's members until `filter` is run again.
The `members` command differs: it uses `EventMembersPredicate`, which looks the event up each time it is tested.
</box>

To add a filter field, add a constant to `FilterField`, add a case to `FilterCommand#resolvePredicate`, and update
`FilterCommandParser.MESSAGE_INVALID_FIELD`.

The `search` command is implemented separately. `SearchCommand` uses `ContactMatchesKeywordPredicate`, which tests
whether a contact's name, phone, email, address, or organisation contains the keyword, ignoring case.

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

* **Alternative 1 (current choice):** Reject the command with `No matching value found.`
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

* After `unassign`, the removed member no longer matches and disappears from the list.
* After `deleteevent`, no event has that name, so the list becomes empty.
* After `editevent` renames the event, the old name would match nothing, so
  `LogicManager#keepMembersViewAfterRename` replaces the predicate with one for the new name.

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

* **Alternative 3:** By a generated contact ID.
  * Pros: Renaming a contact would not affect events.
  * Cons: Needs a new ID field and a change to the data file format, and makes the data file harder to edit by hand.

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

(For all use cases below, the **System** is the `Coordimate` and the **Actor** is the `user`, unless specified otherwise)

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

1. Event lead requests to search contacts using one or more names, tags, or roles.
2. CoordiMate displays the contacts that match the given criteria.

   Use case ends.

**Extensions**

* 1a. Event lead provides no search criteria.

  * 1a1. CoordiMate displays an error message.

    Use case ends.

* 1b. One or more search criteria are invalid.

  * 1b1. CoordiMate displays an error message identifying the invalid criteria.

    Use case ends.

* 2a. No contact matches the given criteria.

  * 2a1. CoordiMate informs the event lead that no matching contacts were found.

    Use case ends.

**Use case: Delete a contact**

**System:** CoordiMate

**Actor:** University CCA EXCO member

**Preconditions:** CoordiMate is running and at least one contact has been saved.

**Main success scenario (MSS)**

1. EXCO member requests to delete a contact, identifying the contact by its displayed index, exact name, phone number, or email address.
2. CoordiMate displays the matching contact and requests confirmation.
3. EXCO member confirms the deletion.
4. Coordimate  model without the selected contact, their assignments in any event roster, or their
   associated attendance records, then updates the live model and confirms the deletion. Events and other contacts
   remain unchanged.
        
   Use case ends.

**Extensions**
  
* 1a. The EXCO member provides no identifier, more than one identifier, an unknown field, or an invalid index.

  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member provides one valid identifier.

    Use case resumes from step 2.

* 1b. No contact matches the provided identifier.

  * 1b1. CoordiMate informs the EXCO member that no matching contact was found.

    Use case ends.

* 1c. More than one contact matches the provided name.

  * 1c1. CoordiMate requests a phone number, email address, or displayed index to identify the contact.
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

1. The Exco member assigns one or more contacts to an event using the `assign` command, specifying the event name and the displayed indexes of the contacts.
2. CoordiMate verifies the event and contacts, then associates the contacts with the event.
3. CoordiMate confirms how many members were assigned.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name, or a contact index is invalid.
  * 1a1. CoordiMate displays the relevant error and assigns no contacts.
  * 1a2. The Exco member corrects the event name or index and retries step 1.

* 1b. No contact indexes are provided.
  * 1b1. CoordiMate reports that at least one contact must be specified.
  * 1b2. The Exco member provides one or more contact indexes and retries step 1.

* 2a. A contact is already assigned, or the same contact index appears more than once.
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

1. The Exco member removes one or more members from an event using the `unassign` command, specifying the event name and the displayed indexes of the contacts.
2. CoordiMate verifies the event and contacts, then removes the contacts from the event's members, together with their attendance records for that event.
3. CoordiMate confirms how many members were removed.

   Use case ends.

**Extensions**

* 1a. No saved event has the given name, or a contact index is invalid.
  * 1a1. CoordiMate displays the relevant error and removes no members.
  * 1a2. The Exco member corrects the event name or index and retries step 1.

* 1b. No contact indexes are provided.
  * 1b1. CoordiMate reports that at least one contact must be specified.
  * 1b2. The Exco member provides one or more contact indexes and retries step 1.

* 2a. A specified contact is not assigned to the event, or the same contact index appears more than once.
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
* **Displayed index**: The number shown beside a contact in the currently displayed list, used to identify that contact in commands.
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
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. Shutting down

   1. Test case: `exit`<br>
      Expected: The app closes. Relaunching it shows the same data as before.

<box type="info" seamless>

**Note:** Several tests below start from the sample contacts. To restore them, close the app, delete
`data/coordimate.json` from the folder containing the JAR file, and relaunch the app.
</box>

### Deleting a contact

1. Deleting a contact while all contacts are being shown

   1. Prerequisites: List all contacts using the `list` command, with multiple contacts in the list.

   1. Test case: `delete 1`, then `n`<br>
      Expected: The first contact's name, phone, email, and role are shown, followed by `Confirm deletion? [y/N]`. After `n`, the status message shows `Deletion cancelled. No changes were made.` and no contact is deleted.

   1. Test case: `delete 1`, then `y`<br>
      Expected: The first contact is deleted from the list. The status message shows `Contact deleted successfully: ` followed by the contact's name.

   1. Test case: `delete 0`<br>
      Expected: No confirmation is requested and no contact is deleted. The status message shows `Invalid contact index. Example: delete 3`

   1. Other incorrect delete commands to try: `delete`, `delete x`, `delete 99` (where the list has fewer than 99 contacts)<br>
      Expected: No confirmation is requested and no contact is deleted. The status message shows error details.

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
      Expected: The list is unchanged. The status message shows `Search keyword cannot be empty.` followed by the usage.

1. Searching by organisation

   1. Prerequisites: Run `add n/Mr Lim p/90801110 e/lim@example.com o/NUS Student Affairs r/University Staff t/UniversityStaff`.

   1. Test case: `search student affairs`<br>
      Expected: Only `Mr Lim` is listed.

### Filtering contacts

1. Filtering by tag, event, and organisation

   1. Prerequisites: The sample contacts are loaded. Run these commands in order:<br>
      `add n/Aisha Tan p/+6591234567 e/aisha@example.com r/Logistics t/EXCO`<br>
      `add n/Mr Lim p/90801110 e/lim@example.com o/NUS Student Affairs r/University Staff t/UniversityStaff`<br>
      `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
      `assign evn/Final Concert c/1 7`

   1. Test case: `filter tag/exco`<br>
      Expected: Only `Aisha Tan` is listed. The status message shows `1 contact(s) found.`

   1. Test case: `filter event/"Final Concert"`<br>
      Expected: `Alex Yeoh` and `Aisha Tan` are listed.

   1. Test case: `filter event/Final Concert tag/EXCO`<br>
      Expected: Only `Aisha Tan` is listed, as she is the only contact matching both filters.

   1. Test case: `filter organisation/NUS Student Affairs`<br>
      Expected: Only `Mr Lim` is listed.

   1. Test case: `filter organisation/NUS`<br>
      Expected: The list is unchanged. The status message shows `No matching value found.`

   1. Test case: `filter tag/Sponsor`<br>
      Expected: The list is empty, as the tag exists but no contact has it. The status message shows `No contacts match the specified filters.`

   1. Test case: `filter role/Logistics`<br>
      Expected: The list is unchanged. The status message shows `Invalid filter field. Supported fields: tag, event, organisation.`

   1. Other incorrect filter commands to try: `filter`, `filter tag/`, `filter event/"Final Concert tag/EXCO`<br>
      Expected: The list is unchanged. The status message shows error details.

### Adding an event

1. Adding events

   1. Prerequisites: No event named `Final Concert` or `Fair` exists.

   1. Test case: `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`<br>
      Expected: The **Events** tab opens with the new event selected. The status message shows `Created Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 18:00.`

   1. Test case: `addevent evn/final concert st/09-10-2026 et/10-10-2026`<br>
      Expected: No event is added. The status message shows `An event with this name already exists.`

   1. Test case: `addevent evn/Fair st/10-10-2026 et/09-10-2026`<br>
      Expected: No event is added. The status message shows `Start time must not be after end time.`

   1. Test case: `addevent evn/Fair st/10/10/2026 et/11-10-2026`<br>
      Expected: No event is added. The status message shows the required date and time format.

   1. Other incorrect addevent commands to try: `addevent`, `addevent evn/Fair st/10-10-2026`, `addevent evn/Fair st/10-10-2026 st/10-10-2026 et/11-10-2026`<br>
      Expected: No event is added. The status message shows error details.

### Editing an event

1. Editing an existing event

   1. Prerequisites: Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`. Select it in the **Events** tab.

   1. Test case: `editevent evn/final concert st/08-08-2026 16:00 et/08-08-2026 19:00`<br>
      Expected: The event's details show the new times. The status message shows `Edited Event Final Concert. Start Time: 08-08-2026 16:00. End Time: 08-08-2026 19:00.`

   1. Test case: `editevent evn/Final Concert nevn/Grand Concert`<br>
      Expected: The event is renamed to `Grand Concert` and keeps its times.

   1. Test case: `editevent evn/Grand Concert st/09-08-2026`<br>
      Expected: No change. The status message shows `Start time must not be after end time.`

   1. Test case: `editevent evn/Gala nevn/Ball`<br>
      Expected: No change. The status message shows `Event Gala does not exist.`

   1. Other incorrect editevent commands to try: `editevent evn/Grand Concert`, `editevent evn/Grand Concert nevn/`<br>
      Expected: No change. The status message shows error details.

### Deleting an event

1. Deleting an existing event

   1. Prerequisites: An event named `Grand Concert` exists, as created in the previous section.

   1. Test case: `deleteevent evn/Gala`<br>
      Expected: No event is deleted. The status message shows `Event Gala does not exist.`

   1. Test case: `deleteevent evn/grand concert`<br>
      Expected: The event is removed from the **Events** tab. The status message shows `Deleted Event Grand Concert.`

   1. Other incorrect deleteevent commands to try: `deleteevent`, `deleteevent evn/`<br>
      Expected: No event is deleted. The status message shows error details.

### Assigning members to an event

1. Assigning contacts while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with at least 3 persons in the list. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`. Select it in the **Events** tab.

   1. Test case: `assign evn/Final Concert c/1 2`<br>
      Expected: The status message shows `Assigned 2 member(s) to Final Concert.` The event's details list the 1st and 2nd persons under **Members (2)**.

   1. Test case: `assign evn/final concert c/2 3`<br>
      Expected: Only the 3rd person is added. The status message also shows `1 contact(s) were already assigned.`

   1. Test case: `assign evn/Final Concert c/1`<br>
      Expected: No change. The status message shows that all specified contacts are already assigned.

   1. Test case: `assign evn/Gala c/1`<br>
      Expected: No change. The status message shows `Event Gala does not exist.`

   1. Other incorrect assign commands to try: `assign evn/Final Concert c/0`, `assign evn/Final Concert c/x` (where x is larger than the list size), `assign evn/Final Concert c/1 c/2`, `assign evn/Final Concert`<br>
      Expected: No change. The status message shows error details.

1. Assigning contacts from a filtered list

   1. Prerequisites: The `Final Concert` event exists. Use `search` so that the displayed list starts with a person who is not first in the full list.

   1. Test case: `assign evn/Final Concert c/1`<br>
      Expected: The 1st person in the filtered list, not the full list, is added to the event's members.

1. Keeping members in sync with contacts

   1. Prerequisites: The `Final Concert` event has at least one member.

   1. Test case: `edit` a member to change their name, using `n/`.<br>
      Expected: The event's members show the new name in the same position.

   1. Test case: `delete` a member.<br>
      Expected: The person is removed from the event's members. Closing and relaunching the app keeps the data.

### Removing members from an event

1. Removing members while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with at least 4 persons in the list. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/1 2 3`. Select the event in the **Events** tab.

   1. Test case: `unassign evn/final concert c/2 4`<br>
      Expected: The status message shows `Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.` The event's details list the 1st and 3rd persons under **Members (2)**, in that order.

   1. Test case: `unassign evn/Final Concert c/4`<br>
      Expected: No change. The status message shows that none of the specified contacts are assigned.

   1. Test case: `unassign evn/Gala c/1`<br>
      Expected: No change. The status message shows `Event Gala does not exist.`

   1. Other incorrect unassign commands to try: `unassign evn/Final Concert c/0`, `unassign evn/Final Concert c/1 x` (where x is larger than the list size), `unassign evn/Final Concert c/1 c/3`, `unassign evn/Final Concert`<br>
      Expected: No change. The status message shows error details.

1. Removing members from a filtered list

   1. Prerequisites: The `Final Concert` event has the 1st and 3rd persons as members. Use `search` so that the displayed list shows only the 3rd person.

   1. Test case: `unassign evn/Final Concert c/1`<br>
      Expected: The 3rd person of the full list, shown 1st in the filtered list, is removed. The 1st person of the full list remains a member.

### Viewing members of an event

1. Viewing the members of an event

   1. Prerequisites: List all persons using the `list` command, with at least 3 persons in the list. Create an event using `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, then run `assign evn/Final Concert c/3 1`. Switch to the **Events** view.

   1. Test case: `members evn/final concert`<br>
      Expected: CoordiMate switches to the **Contacts** view and shows only the 1st and 3rd persons, numbered 1 and 2. The status message shows `Listed 2 member(s) of Final Concert. Use list to show all contacts.`

   1. Test case: `members evn/Gala`<br>
      Expected: The contact list is unchanged. The status message shows `Event Gala does not exist.`

   1. Other incorrect members commands to try: `members`, `members evn/`, `members evn/Final Concert c/1`, `members evn/Final Concert evn/Fair`<br>
      Expected: The contact list is unchanged. The status message shows error details.

1. Keeping the shown members up to date

   1. Prerequisites: The members of `Final Concert` are shown, as above.

   1. Test case: `unassign evn/Final Concert c/1`<br>
      Expected: The removed person disappears from the list straight away, and the remaining member is numbered 1.

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
      Expected: No change. The status message shows `Alex Yeoh is already marked as absent for Event Final Concert. No changes were made.`

   1. Test case: `markattendance evn/Final Concert mem/David Li att/present`<br>
      Expected: No change. The status message shows `Error! David Li is not assigned to event Final Concert!`

   1. Test case: `markattendance evn/Gala mem/Alex Yeoh att/present`<br>
      Expected: No change. The status message shows `Error! Event Gala does not exist!`

   1. Other incorrect markattendance commands to try: `markattendance evn/Final Concert mem/Alex Yeoh att/late`, `markattendance evn/Final Concert mem/Alex Yeoh`, `markattendance evn/Final Concert mem/Alex Yeoh att/present att/absent`<br>
      Expected: No change. The status message shows error details.

1. Keeping attendance in sync with members

   1. Prerequisites: Continue from the previous test, with `Alex Yeoh` marked absent. List all contacts using `list`.

   1. Test case: `assign evn/Final Concert c/3`<br>
      Expected: The 3rd contact is added as **Not marked**. `Alex Yeoh` is still **Absent**.

   1. Test case: `editevent evn/Final Concert et/08-08-2026 20:00`<br>
      Expected: The end time changes. `Alex Yeoh` is still **Absent**.

   1. Test case: `edit 1 n/Alex Tan`<br>
      Expected: The event's members show `Alex Tan` in the same position, still **Absent**.

   1. Test case: `unassign evn/Final Concert c/1`, then `assign evn/Final Concert c/1`<br>
      Expected: `Alex Tan` is removed, then added again at the end of the members as **Not marked**.

   1. Test case: Mark any member's attendance, then close and relaunch the app.<br>
      Expected: The attendance is retained.

### Managing tags

1. Adding, renaming, and deleting tags

   1. Prerequisites: The sample contacts are loaded and no custom tags have been created.

   1. Test case: `listtags`<br>
      Expected: The Tags view opens. The status message lists `EXCO, Sponsor, UniversityStaff, Logistics`.

   1. Test case: `addtag t/Publicity`<br>
      Expected: `Publicity` appears in the Tags view. The status message shows `Created tag: Publicity.`

   1. Test case: `addtag t/publicity`<br>
      Expected: No tag is added. The status message shows `This tag already exists.`

   1. Test case: `addtag t/Event Crew`<br>
      Expected: No tag is added. The status message shows `Tag names should be alphanumeric with no spaces.`

   1. Test case: `edittag Publicity t/Media`<br>
      Expected: The tag is renamed. The status message shows `Publicity successfully renamed to Media.`

   1. Test case: `edit 1 at/Media`, then `edittag media t/Press`<br>
      Expected: The 1st contact's details show the tag `Press`.

   1. Test case: `deletetag t/press`<br>
      Expected: The tag is removed from the Tags view and from the 1st contact. The status message shows `Press successfully deleted.`

   1. Test case: `deletetag t/Nope`<br>
      Expected: No tag is deleted. The status message shows `No such tag exists: Nope.`

   1. Other incorrect tag commands to try: `addtag`, `addtag Publicity`, `edittag EXCO`, `deletetag EXCO`, `listtags 1`<br>
      Expected: No change. The status message shows error details.

### Saving data

<box type="warning" seamless>

**Caution:** These tests modify or remove the data file. Back up `data/coordimate.json` first if it holds data you
want to keep.
</box>

1. Dealing with a missing data file

   1. Prerequisites: The app is closed and `data/coordimate.json` exists.

   1. Test case: Delete `data/coordimate.json`, then launch the app.<br>
      Expected: The app starts with the sample contacts.

   1. Test case: Run `list`, then check the `data` folder.<br>
      Expected: `data/coordimate.json` has been created again.

1. Dealing with a corrupted data file

   1. Prerequisites: The app is closed and `data/coordimate.json` contains at least one contact. Keep a copy of the file.

   1. Test case: Open `data/coordimate.json` in a text editor, delete the first `{`, save, then launch the app.<br>
      Expected: The app starts with no contacts and no events.

   1. Test case: Without closing the app, run `addevent evn/Test st/01-01-2027 et/01-01-2027`.<br>
      Expected: No event is added. The status message shows `Contact data could not be loaded. Please check the local data file.` The data file is unchanged.

   1. Test case: Close the app, put the `{` back, save, then launch the app.<br>
      Expected: The original contacts and events are shown again.

   1. Test case: Corrupt the file again as above, launch the app, then run `list`.<br>
      Expected: The corrupted file is replaced with the app's empty data. Restore the copy of the file to get the original data back.

1. Dealing with an event member who is not a contact

   1. Prerequisites: The app is closed and `data/coordimate.json` contains an event with at least one member. Keep a copy of the file.

   1. Test case: In the data file, change one name in an event's `members` list to a name that no contact has, save, then launch the app.<br>
      Expected: The file is treated as invalid, so the app starts with no contacts and no events. Restore the copy of the file afterwards.

1. Dealing with a failed save

   1. Prerequisites: The app is running and `data/coordimate.json` exists. Make the data impossible to save: on Windows, open the properties of `data/coordimate.json` and tick **Read-only**; on macOS or Linux, run `chmod a-w data` in the folder containing the JAR file.

   1. Test case: `addevent evn/Test st/01-01-2027 et/01-01-2027`<br>
      Expected: No event is added and the **Events** tab is unchanged. The status message shows `Event could not be saved. No changes were made.`

   1. Test case: `add n/Test Person p/91234567 e/testperson@example.com r/Member`<br>
      Expected: No contact is added. The status message shows `Contact could not be saved. No changes were made.`

   1. Test case: Undo the change made in the prerequisites (untick **Read-only**, or run `chmod u+w data`), then repeat the `addevent` command.<br>
      Expected: The event is added.
