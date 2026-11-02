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

[`Main`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/Main.java) and
[`MainApp`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/MainApp.java)
initialize and connect the components at launch, then shut them down and run cleanup on exit.

| Component | Responsibility |
|---|---|
| [UI](#ui-component) | Display data and accept input |
| [Logic](#logic-component) | Parse and execute commands |
| [Model](#model-component) | Hold application data in memory |
| [Storage](#storage-component) | Read and write local files |
| [Commons](#common-classes) | Provide shared classes |

The sequence below follows `delete 1` through the four main components.

<puml src="diagrams/ArchitectureSequenceDiagram.puml" width="574" />

Each main component exposes an interface and implements it in a `{Component Name}Manager` class, for example
`Logic` and `LogicManager`. Components communicate through these interfaces to avoid coupling to implementations.

<puml src="diagrams/ComponentManagers.puml" width="300" />

### UI component

**API:** [`Ui.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/Ui.java)

<puml src="diagrams/UiClassDiagram.puml" alt="Structure of the UI Component"/>

`MainWindow` creates or uses the parts above. `TagListViewModel` synchronizes the Tags view with contact and tag data.
The inheritance diagram below shows all ten subclasses of the abstract `UiPart`; its repeated boxes represent one
superclass. `TagListViewModel` and `WindowBounds` are helpers, not `UiPart` subclasses.

<puml src="diagrams/UiPartInheritanceDiagram.puml" alt="UI component inheritance from UiPart"/>

The UI uses JavaFX, with matching `.fxml` layouts in `src/main/resources/view`. For example,
[`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/resources/view/MainWindow.fxml)
defines [`MainWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/MainWindow.java)'s layout.

* `HelpWindow` builds a quick reference from command classes' command words.
* `TutorialWindow` renders the [tutorial](#glossary) overlay. `UiManager` opens it on first launch;
  `tutorial` or <kbd>F2</kbd> reopens it.
* `CommandBox` keeps session history for recall with Up and Down.

At launch, `MainWindow` restores size, position, and maximized state from `GuiSettings`; `WindowBounds` keeps it
within the available screen area.

The UI holds `Logic` to execute commands and observes `Model` data to refresh the Contacts, Events, and Tags views.
It depends on model classes such as `Person` and `Event` for display. A `CommandResult` can request Help, the tutorial,
a view, or contact selection. After switching views, `MainWindow` completes CSS and layout, then reveals the selected
contact again to account for list resizing when feedback changes height.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/Logic.java)

<puml src="diagrams/LogicClassDiagram.puml" width="550"/>

1. `CoordiMateParser` selects a command parser, such as `DeleteCommandParser`, to produce a `Command` subclass.
1. `LogicManager` executes it. Most data changes run on a model copy, committed only after saving
   [atomically](#saving-data-atomically).
1. `Logic` returns the resulting `CommandResult`.

For `execute("delete 1")`, the first call resolves the contact and requests confirmation. The next input deletes it
only if it is `y` or `Y`; anything else cancels.

<puml src="diagrams/DeleteSequenceDiagram.puml" alt="Interactions Inside the Logic Component for the `delete 1` Command" />

The parsing classes omitted from the partial Logic diagram are:

<puml src="diagrams/ParserClasses.puml" width="600"/>

`XYZCommandParser` and `XYZCommand` stand for matching classes, such as `AddCommandParser` and `AddCommand`.
Command parsers implement `Parser`, allowing common handling, including in tests.

Every error message is followed by the usage of the command that failed. `CoordiMateParser` adds it to parse errors
and `LogicManager` adds it to execution errors, using `Messages.withUsage`.

#### Parameter detection

Command parsers detect [parameter-like text](#glossary) before validating field values. Consequently, a field's
allowed characters alone do not determine which values can be entered through the CLI.
Prefix matching is case-insensitive.

[`ParameterSyntax`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/commons/util/ParameterSyntax.java)
defines the shared parameter boundary: the start of the input or a position immediately after whitespace, using
`StringUtil.WHITESPACE_CHARACTERS` for Java whitespace and Unicode `White_Space`. It also defines the event-style
pattern. [`PrefixParser`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/parser/PrefixParser.java)
reuses that boundary and pattern, and defines the contact-style pattern with the relationship-word exception:

| Detection rule | Parameter-like text at a boundary | Used by |
|---|---|---|
| Contact-style | `letters/`, except standalone `s/o` and `d/o`, ignoring case | Contact add/edit/delete, tag commands, and `markattendance` |
| Event-style | `letters/`, without the relationship-word exception | Event add/edit/delete/archive and the shared `EventContactsParser` used by `assign`, `unassign`, and `members` |

A supported prefix starts a parameter; an unsupported token is rejected as an unknown parameter. Repeated and
empty parameters are then handled according to the command. Contact and tag parsers, along with the shared
`EventContactsParser` used by `assign`, `unassign`, and `members`, use `ArgumentTokenizer` to extract values after
rejecting unknown parameters. `PrefixParser#parseUniqueParameters` also splits values and rejects repeated
parameters for its callers.

The examples below are argument fragments; other required arguments must still be supplied:

| Fragment | Result |
|---|---|
| `r/Lead/Deputy` | The internal slash stays in the role value, which passes role validation. |
| `r/Lead Deputy/Assistant` | `Deputy/` follows whitespace and is rejected as an unknown parameter. |
| `m/Discuss r/Lead` | `r/` starts a role parameter rather than remaining in the note. |
| `n/Ravi s/o Kumar` | The standalone relationship word is exempt from contact-style detection and is allowed by name validation. |
| `evn/Concert/Workshop` | The internal slash stays in the event name. |
| `evn/Concert Workshop/Music` | `Workshop/` follows whitespace and is rejected as an unknown parameter. |

The `s/o` and `d/o` exemption applies throughout contact-style argument scanning, not only inside name fields.
It does not change field validation: names allow slashes only in these standalone relationship words and need
at least one letter outside them, whereas roles and organisations allow other slashes. Phones, emails, tags,
and date fields reject slashes even when parameter detection leaves them in the value.

`markattendance` currently uses contact-style detection for both its event and member names. Its event-name
parsing therefore accepts `evn/Concert s/o Kumar`, which event-style commands reject.
Passing parsing does not guarantee execution: the event and member must still exist. This describes the current
parser difference rather than a design requirement for event names.

Quotation marks do not disable parameter detection in contact, tag, or event commands. For example,
`r/"Lead Deputy/Assistant"` still detects `Deputy/`; these commands provide no general quoting or escaping mechanism
for parameter-like text inside values.

Both detection styles retain slash-leading words such as `/st` and `/Workshop`: `evn/Concert /Workshop` names an event
`Concert /Workshop`. `/st` and `/et` are not aliases for `st/` and `et/`: they fail date validation in date fields
or cause missing-parameter errors when replacing required prefixes, but can remain literal in names.
Startup cleanup shares `ParameterSyntax`'s pattern and preserves these names.

`filter` and `search` use separate parsers:

* `FilterCommandParser` detects `letters/` at the same boundaries, with no `s/o` or `d/o` exemption. Tokens inside
  double quotes are ignored during detection, so `filter organisation/"NUS docs/team"` keeps `docs/team` in the
  value, while the unquoted form rejects `docs/`. Filter values are checked for non-emptiness and later resolved
  against saved data, rather than passed through the corresponding model field validators. See
  [Filtering contacts](#filtering-contacts).
* `SearchCommandParser` treats the remaining arguments as a keyword, optionally removes surrounding double
  quotes, and normalises whitespace. It does not detect parameters, so slash-containing text remains keyword text.
### Model component
**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/Model.java)

<puml src="diagrams/ModelClassDiagram.puml" width="575" />


`Model` has no dependencies on UI, Logic, or Storage. It holds:

* contacts in `UniquePersonList`, with immutable `MemberId`s. `CoordiMate` allocates IDs; `UniquePersonList` rejects
  duplicates and preserves IDs on replacement or editing.
* a global tag list, unique ignoring case. Adding, editing, or replacing contacts registers missing tags and keeps
  unused tags.
* events, whose ordered member lists contain contact `Name`s. `CoordiMate` updates these names when contacts are
  renamed and removes them when contacts are deleted.
* an unmodifiable `ObservableList<Person>` of filtered contacts, such as search results, for UI observation and binding.
* an [archive view](#archiving-contacts-and-events) controlling whether contact and event lists show active or archived items.
* `UserPrefs`, exposed as `ReadOnlyUserPrefs`: window size, position, maximized state, and tutorial completion or dismissal.


<box type="info" seamless>

**Note:** Each `Person` holds `Tag` objects compared by value with the global `UniqueTagList`, not necessarily shared
instances. The alternative below shares the global objects, keeping one `Tag` instance per unique tag.<br>

<puml src="diagrams/BetterModelClassDiagram.puml" width="450" />
</box>


### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/Storage.java)

<puml src="diagrams/StorageClassDiagram.puml" width="700" />

`StorageManager` reads and writes model data as JSON through `JsonCoordiMateStorage` and `JsonUserPrefsStorage`.
`JsonSerializableCoordiMate` stores separate contact (`JsonAdaptedPerson`), event (`JsonAdaptedEvent`), and global
tag (`JsonAdaptedTag`) lists. Contacts also store their tags; events identify members by contact name, not ID.

`MainApp` resolves data and preferences paths, and `LogsCenter` resolves the log path, against
`FileUtil#getHomeFolder()`: the running JAR's folder, or the working directory when run from an IDE or Gradle.
Files therefore stay in the same [home folder](#glossary) regardless of the JAR's launch directory.

Loading validates saved data without overwriting invalid files. An unreadable data file triggers a warning naming
the file, then closes the app for repair or restoration. Unreadable preferences alone cause default preferences
to be used without saving them that session. Legacy data, including files without member IDs or a tag list,
loads and is migrated on the next save.

#### Startup event cleanup

Before loading the model, `MainApp` calls `StorageManager#cleanUpEventNames()` to migrate saved events,
including archived events. The cleanup:

* deletes events whose names contain whitespace followed by parameter-like text, such as `c/review`, because
  those names cannot be used in event commands;
* renames later events whose names were distinct under legacy whitespace rules but now match, ignoring case, with
  an available suffix such as ` (2)`. Long names are shortened to fit the 100-character limit. Duplicates under
  the legacy rules remain invalid;
* deletes events with a start or end date in year `0000`, and removes year-`0000` reminder dismissal history from
  events that remain.

Renamed events keep their members, attendance, reminder dismissal history (except year-`0000` entries), and archive
status. The remaining data is validated before the cleanup is saved using `saveCoordiMateIfUnchanged`, under a
`DataFileGuard` lock. If validation or saving fails, the original file is left unchanged. If no cleanup is needed,
the file is not rewritten. A startup warning reports deleted and renamed events after a successful save.

### Common classes

`coordimate.commons` contains shared classes: `GuiSettings`, `LogsCenter`, exceptions such as `DataLoadingException`,
and utilities such as `StringUtil` and `JsonUtil`.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

| Area | Sections |
|---|---|
| Data integrity | [Member IDs](#permanent-member-ids), [atomic saves](#saving-data-atomically), [event synchronization](#keeping-events-in-sync-with-contacts) |
| Contacts and views | [Filtering](#filtering-contacts), [leave](#marking-contacts-as-on-leave), [archiving](#archiving-contacts-and-events) |
| Events | [Reminders](#event-reminders), [attendance](#event-attendance) |
| Interaction | [Tutorial](#first-launch-tutorial), [keyboard navigation and history](#keyboard-navigation-and-command-history) |

### Permanent member IDs

The [member ID](#glossary) implementation uses `MemberId`, `MemberIdResolver`, and the persisted `nextMemberId` counter.

#### Allocation and editing

`CoordiMate` allocates immutable IDs from the increasing `nextMemberId` counter. `UniquePersonList` rejects
duplicates and ID changes on editing. Deleted IDs are never reused; `clear` starts a new dataset at `1`.

Below, three contacts were added, ID `3` was deleted, and a fourth was added with ID `4`. `nextMemberId` is now `5`.

<puml src="diagrams/MemberIdObjectDiagram.puml" width="520" />

#### Command targeting

`ParserUtil#parseMemberId` parses a positive integer; `MemberIdResolver` searches all saved contacts, including those
hidden by searches, filters, or the archive view. Unknown IDs are rejected without changes. Since events use names
internally, `assign` and `unassign` resolve every ID before translating them to names.

`delete` retains the resolved contact during confirmation, then resolves its ID again and rejects removed or edited contacts.

#### Storage and migration

The data file stores `memberId` and `nextMemberId`. Legacy contacts without IDs receive consecutive IDs in file order,
after existing IDs. Allocation alone does not rewrite the file; the next successful save, possibly
[startup cleanup](#startup-event-cleanup), records the IDs. Duplicate or invalid IDs make the file invalid.

#### Design considerations

| ID allocation | Benefit | Cost |
|---|---|---|
| Saved counter (chosen) | Never reuses deleted IDs, including the highest | Requires validation and migration |
| Highest saved ID plus one at launch | No extra saved state | Reuses the highest ID after deletion and restart |

### Saving data atomically

Contact, event-detail, tag, and attendance changes take effect in both the app and data file, or neither.
Read or write failures leave both unchanged.

#### Implementation

**1. Execute on a model copy.** [`LogicManager#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/LogicManager.java)
routes data changes such as `AddCommand`, `EditCommand`, and `AssignCommand` through `LogicManager#executeAtomicCommand`:

1. Check that the data file remains loadable. Reject unreadable data without overwriting it; a missing file is allowed.
1. Copy live data into a _candidate_ `ModelManager`. Preserve the [archive view](#archiving-contacts-and-events) and
   contact filter using `candidate.updateFilteredPersonList(model.getFilteredPersonList()::contains)`, so commands
   reading the displayed list see the same contacts.
1. Execute the command on the candidate; discard it on failure.
1. Save the candidate; reject and discard it if saving fails or another window has changed the file since loading.
1. Replace live data with the saved candidate, updating the UI.

The live model changes only at step 5. Afterwards, `add` and `edit` clear the contact filter to reveal the changed contact.

<box type="warning" seamless>

**Note:** Add new data-changing command classes to `LogicManager#execute`'s fixed list to use this atomic path.
</box>

Successful `executeAtomicCommand(c)` for an `AddEventCommand`, omitting parsing:

<puml src="diagrams/AtomicCommandSequenceDiagram.puml" alt="Interactions when a command that changes data is executed" />

<puml src="diagrams/AtomicCommandActivityDiagram.puml" width="450" />

**2. The data file is replaced in one step.**
[`JsonCoordiMateStorage#saveCoordiMate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/storage/JsonCoordiMateStorage.java)
writes to a temporary file in the same folder, then replaces the data file using `ATOMIC_MOVE` and `REPLACE_EXISTING`.
Under the
[Java API contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/Files.html#move(java.nio.file.Path,java.nio.file.Path,java.nio.file.CopyOption...)),
atomic replacement depends on the filesystem provider. Unsupported moves throw `AtomicMoveNotSupportedException`;
there is no non-atomic fallback.

Where supported, process termination while the OS remains running leaves the old or new complete target, possibly
with a leftover temporary file. Saves do not force file contents or directory metadata to durable storage, so OS
crashes and power loss have no recovery guarantee. `JsonUserPrefsStorage` writes preferences directly, without atomic replacement.

Confirmed `delete` uses this path. Read-only commands (`list`, `search`, `filter`, `members`) run on the live model
without saving. [Reminder dismissal](#event-reminders) has its own save path.

#### Design considerations

| Commit strategy | Benefit | Cost |
|---|---|---|
| Copy, save, then replace (chosen) | No command-specific rollback; UI never shows unsaved edits | Copies all data per command, acceptable at [typical load](#glossary) |
| Execute live; restore a snapshot on failure | No copy on successful saves | UI briefly shows changes that are then reverted |

### Event reminders

The **Upcoming events** panel lists [reminders](#glossary) for events starting soon, ordered by start time and then by name.

#### Implementation

Reminders are derived from saved events, not stored separately.

**Finding upcoming events.** [`UpcomingEventFinder`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/reminder/UpcomingEventFinder.java)
uses a testable `Clock` to select eligible events:

* Timed starts qualify from 24 hours before the start through the start itself, inclusive.
* Date-only starts qualify throughout the preceding day and start date.

Results are ordered by start, then name ignoring case. Date-only starts count as midnight only for ordering.

**Keeping the reminder list current.** [`ReminderService`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/reminder/ReminderService.java)
holds `ReminderPanel`'s observable list. `ReminderService#refreshReminders()` excludes archived and dismissed events
from the finder's results. It runs at `LogicManager` creation, after every command (including failures, through
`LogicManager#execute`'s `finally` block), every minute via `MainWindow`'s `Timeline`, and when the main window is
shown after being hidden. The list is replaced only when its contents change, avoiding unnecessary redraws.

**Remembering dismissals.** `Event#isReminderDismissed()` checks the current start against `dismissedReminderStarts`,
saved with each event so dismissals survive restarts. Renaming or changing only the end preserves dismissal.
`Event#withDetails`, used by `editevent`, clears history on any start change, including a return to an earlier start,
allowing the new reminder to appear.

**Dismissing a reminder.** `dismiss INDEX` and **Dismiss** use the same `ReminderService`, created by `LogicManager`
and passed to `DismissCommand`. The sequence shows `dismiss 1` on save success and failure.

<puml src="diagrams/DismissSequenceDiagram.puml" width="700" alt="Interactions when a reminder is dismissed" />

`DismissCommand` resolves the index against the last displayed list, rejects reminders no longer upcoming, and asks
the service to dismiss it. The service saves a copy before updating the live event, preserving details, members,
and attendance. `Storage#saveCoordiMateIfUnchanged` rejects files changed by another window.

On read or save failure, including conflicts, dismissal still hides the reminder for this session and returns a
**Not saved** warning; it may reappear after restart. Since the service saves directly, `dismiss` bypasses `executeAtomicCommand`.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Refresh every minute and after commands (chosen) | Simple; reads current time after sleep or clock changes; event edits need no extra refresh code | Reminders can appear or expire up to a minute late |
| Timer per eligibility change | Exact update time | Event edits must reschedule timers; sleep or clock changes can mistime them |
| Store dismissed starts (chosen) | Saved with the event; a different start no longer matches, even if code bypasses `Event#withDetails` | Extra field per event |
| Store a dismissed flag | Simpler data | Every start-changing path must clear it or leave a moved event dismissed |

### Event attendance

#### Implementation

<puml src="diagrams/EventClassDiagram.puml" width="450" />

An [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java) is immutable. It stores:

* `List<Name>` members in assignment order.
* `Map<Name, AttendanceStatus>` attendance: `PRESENT` or `ABSENT`; no entry means unmarked.

The constructor rejects attendance for non-members. `MarkAttendanceCommand` resolves the event and member ignoring
case, rejects an unchanged status, or replaces the event using `Event#withAttendance`. It runs through `executeAtomicCommand`.

Its parser uses contact-style [parameter detection](#parameter-detection), including for the event name; see that
section for the difference from other event commands.

| Change | Effect on attendance |
|---|---|
| `assign` adds members | Existing attendance is kept. New members are not marked. |
| `unassign` removes members | The removed members' attendance is removed. |
| `editevent` renames or reschedules the event | Attendance is kept. |
| `edit` renames a contact | The contact's attendance moves to the new name in every event. |
| `delete` deletes a contact | The contact's attendance is removed from every event. |

JSON `attendance` maps member names to `"present"` or `"absent"`. A missing field loads as no recorded attendance.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Attendance map in each event (chosen) | Event, membership, and attendance changes are saved, copied, and replaced together | Cross-event attendance queries scan every event |
| Separate event/contact attendance list | Easy cross-event queries for a contact | Every event/contact change must update the list to avoid orphaned records |
| Missing map entry means unmarked (chosen) | No new-member data or legacy conversion | Derive unmarked counts from members |
| Explicit `NOT_MARKED` status | Every member has a status | Every member addition needs an entry; legacy files need conversion |

### Filtering contacts

The `filter` command lists the contacts that match every given `FIELD/VALUE` pair, where the field is `tag`,
`event`, or `organisation`.

#### Implementation

Parsing needs no saved data; resolving does.

**Parsing.** [`FilterCommandParser`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/parser/FilterCommandParser.java)
splits `FIELD/` tokens into [`FilterCriterion`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterCriterion.java)
records containing a [`FilterField`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/FilterField.java)
and value. Quoted values can contain literal `FIELD/` text, unlike contact and event commands; see
[Parameter detection](#parameter-detection) for parser-specific rules.

**Resolving.** [`FilterCommand#execute`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/logic/commands/FilterCommand.java)
turns each criterion into a `Predicate<Person>` using the model:

`TagFilterPredicate` handles tag criteria; `EventMembersPredicate` handles event criteria. Both implement
`Predicate<Person>`; organisation criteria use a plain lambda.

| Field | The value must match, ignoring case | A contact matches if |
|---|---|---|
| `tag` | a tag in `Model#getTagList()` | it has that tag |
| `event` | the name of a saved event | it is a member of the event with that name |
| `organisation` | the organisation of at least one saved contact | its organisation is that value |

Unknown values are rejected before changing the display. Otherwise,
[`ContactFilterPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/ContactFilterPredicate.java)
implements `Predicate<Person>` and holds other predicates, combining them with AND (the composite pattern).
`Model#updateFilteredPersonList` receives this composite. `filter` runs on the live model without saving.
The diagram omits the organisation lambda.

<puml src="diagrams/FilterClassDiagram.puml" width="700" />

The `event` and `tag` predicates follow names, including [later renames](#keeping-events-in-sync-with-contacts).

To add a filter field, add a constant to `FilterField`, a case to `FilterCommand#resolvePredicate`, and a case to the
example-selection switch in `FilterCommandParser#parse`.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Full fields: `filter tag/EXCO event/Final Concert` (chosen) | Self-explanatory; field names come from one enumeration | Differs from short prefixes such as `t/` |
| Reuse prefixes: `filter t/EXCO evn/Final Concert` | Consistent with other commands | Prefix meanings may differ by command; `o/` is easy to confuse |
| Reject unknown values (chosen) | Distinguishes typos from valid groups with no contacts | Requires model lookup before filtering |
| Show an empty list for unknown values | Simpler | Conceals mistyped values |

### Keeping events in sync with contacts

Events refer to contact `Name`s because editing replaces `Person` objects. These references must stay synchronized
with saved contacts.

#### Implementation

**Renaming and deleting contacts.** [`CoordiMate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/CoordiMate.java)
updates events whenever a contact changes:

* Renaming replaces the old name in every affected event, preserving position and attendance.
* Deleting removes the name and attendance from every affected event.

Updated events are built before any changes. An existing member with the new name causes rejection without changes.
Loading rejects event members absent from saved contacts.

<puml src="diagrams/EventSyncActivityDiagram.puml" width="380" />

**Showing an event's members.** `MembersCommand` filters the contact list with
[`EventMembersPredicate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/EventMembersPredicate.java),
which looks up its event by name on each test. `assign` adds matches; `unassign` removes them; `deleteevent` empties
the list. After `editevent` renames an event, `LogicManager#keepEventViewsAfterRename` replaces the predicate with
the new name. `filter event/...` uses the same predicate and follows these changes.

Similarly, `LogicManager#keepTagFiltersAfterRename` updates `filter tag/...` criteria after `edittag`.

<box type="warning" seamless>

**Note:** Contact-list changes trigger predicate re-testing. `CoordiMate#resetData` must therefore replace events
before contacts, or event-member views would use stale events.
</box>

#### Design considerations

| Member reference | Benefit | Cost |
|---|---|---|
| Contact name (chosen) | Unique and readable in JSON `members` | Renaming or deleting contacts must update events |
| Permanent member ID | Renaming does not affect events | Requires migration of saved membership and attendance, plus name lookups for display |

### Marking contacts as on leave

#### Implementation

[`LeaveDate`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/LeaveDate.java)
checks [leave](#glossary) dates through `Person#isOnLeave(LocalDate today)` and `LeaveDate#covers`.
`Person` stores the optional date, set by `lv/` in `add` or `edit` and cleared by an empty `lv/`.
Input dates cannot be past; loading accepts past dates so ended leave remains readable.

The UI shows **On leave until** only while leave covers today. Passing the date changes no data: it remains saved
until replaced or cleared. Copies made by `CoordiMate#setTag`, `CoordiMate#removeTag`, and similar methods must retain it.
The activity diagram shows how the UI determines leave status; expired dates remain saved but display as not on leave.

<puml src="diagrams/LeaveActivityDiagram.puml" width="560" alt="Activity for determining a contact's displayed leave status" />

`PersonListPanel` and `EventListPanel` use testable `Clock`s. `MainWindow`'s one-minute reminder timer calls
`PersonListPanel#refreshIfDateChanged()` and `EventListPanel#refreshIfDateChanged()` to redraw Contacts and Events
only after a date change. JSON stores `leaveUntil`; missing fields mean no leave.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Retain dates; compare when displayed (chosen) | No save at launch or midnight; fixed dates make `Person#isOnLeave` easy to test | Old dates remain saved; redraw needed after midnight |
| Remove ended dates at launch | Only active leave remains saved | Requires a startup save and checks while open |
| Reuse the one-minute reminder timer (chosen) | No new timer; handles sleep and clock changes | Badge can persist up to a minute after midnight |
| Reschedule a midnight timer daily | Updates exactly at midnight | Another timer to manage; sleep or clock changes can mistime it |

### Archiving contacts and events

`archive`, `unarchive`, `archiveevent`, and `unarchiveevent` implement [archiving](#glossary).
`list archived` selects the archive view, and `list` returns to the active view.

#### Implementation

**The archive flag.** [`Person`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/person/Person.java)
and [`Event`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/event/Event.java)
are immutable and store a boolean `archived`. `withArchived(boolean)` returns a copy; all other copies retain the flag.
`Person`'s full constructor requires it, forcing callers such as `EditCommand` and `CoordiMate`'s tag updates to supply it.

**Hiding archived items.** [`ModelManager`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/model/ModelManager.java)
keeps the archive view (`true` for archived items) separate from command filters. Contacts must match both;
events must match the archive view. `MainWindow` observes it to update tab titles and sidebar commands.
Model-level filtering makes `search`, `filter`, and `members` respect the view without command changes.

Below, `ArchiveCommand` resolves permanent ID `12` regardless of the display and runs on a
[model copy](#saving-data-atomically), like `edit`.

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

JSON contacts and events store `archived`; missing fields default to false.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Apply the archive view in the model (chosen) | Existing and future filters respect it automatically | `ModelManager` must retain command predicates separately |
| Add `!person.isArchived()` in each command | No model change | Easy to omit; `list archived` requires changes in every command |
| Identify contacts by permanent ID (chosen) | Consistent with `edit`, `delete`, and `assign`; works across views and filters | Users need the ID shown on the card |
| Identify by displayed index | Convenient after searching | Requires the archived view to restore; indexes shift |
| Shared lists with an archive flag (chosen) | Preserves unique names and event references; restoration only flips a flag | Every relevant list must filter archived items |
| Separate archive lists | Normal lists contain no archived items | Duplicate checks, member lookups, and JSON must handle both lists; moving items risks broken references |

### First-launch tutorial

The tutorial is a step-by-step tour of the main window, shown automatically on the first launch.

#### Implementation

[`TutorialWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/TutorialWindow.java)
overlays the main window. Its fixed `TutorialStep` records specify a title, description, card placement, and
`TutorialTarget` to highlight.

| Responsibility | Implementation |
|---|---|
| Change steps | The step-change handler calls `MainWindow#showTutorialStep`, selects the required tab, and supplies `TutorialWindow#highlight` with a target supplier. The overlay shades other areas and places the card beside the target. |
| Block underlying controls | Overlay and window filters consume mouse/key events outside the card; `MainWindow#showTutorial` disables the command box, preventing changes to highlighted lists. |
| Handle keys | `TutorialWindow#handleShortcut` uses Left/Right for steps and Esc to close. Tab stays within enabled card buttons; Up/Down are consumed to prevent focus traversal. |
| Restore normal lists | `MainWindow#showTutorial` first calls `Logic#showActiveItems()`, since archived views hide tour targets. |

**Keeping highlights aligned.** A scene post-layout listener refreshes the highlight and card position from the
target's final bounds, including after resizing without a step change. It is removed when hidden and transferred
when the scene changes. `TutorialWindow#highlight` takes a `Supplier<Node>`, re-resolved on every refresh so contact
and event targets do not retain reused or hidden virtualized cells. Card and backdrop layout is skipped unless
target bounds or window size change. After resizing, synchronous card layout re-wraps its text immediately.

Target resolution must be safe on every refresh. `EventListPanel#selectTutorialEventIfNeeded` selects the first
event only if none is selected; other targets resolve without changing selection or scroll position.
`MainWindow` calls `PersonListPanel#revealTutorialContactCard` to select and reveal the first contact, and
`EventListPanel#revealTutorialMemberList` to scroll the member list into view, only when a step opens or the overlay
resizes through `TutorialWindow#setResizeHandler`. Revealing on resize keeps targets positioned after size changes.
Doing so every refresh would cause repeated layout requests: `ListView#scrollTo` and `ScrollPane#setVvalue` request
layout even when nothing moves.

`MainWindow#showTutorialOnFirstLaunch()` opens the tour only if `UserPrefs` says it is unseen. Finishing or closing
calls `Logic#setHasSeenTutorial(true)`; preferences save on exit. If preferences were unreadable, `MainApp` preserves
the file, so the tour appears again next launch.

`tutorial`, the top-bar **Tutorial** item, or F2 reopens it. `TutorialCommand` requests this through `CommandResult`.

#### Design considerations

| Tutorial display | Benefit | Cost |
|---|---|---|
| Overlay highlighting real controls (chosen) | Shows actual tabs, cards, and input; cannot detach from the main window | Each step needs a target; underlying input must be blocked |
| Separate window with screenshots and text | Independent of main-window layout | Screenshots become stale; users must map them to the real window |

### Keyboard navigation and command history

Single-key navigation and command recall allow mouse-free use.

#### Implementation

`MainWindow` displays its `InteractionMode` beside the command box:

| Mode | Keys |
|---|---|
| Command (default) | Type in the command box; Esc enters Navigation mode |
| Navigation | Command box disabled; J/K select next/previous card; S selects the next view; releasing I returns to Command mode |

The activity diagram shows how inputs select the mode during normal use. The app starts in Command mode;
browsing command history or navigating cards and views does not change the mode.

<puml src="diagrams/InteractionModeActivityDiagram.puml" width="520" alt="Activity for selecting the interaction mode from a keyboard or mouse input" />

`MainWindow#handleKeyboardInput` filters keys window-wide: only Esc in Command mode; J/K/S and consumption of other
unmodified keys in Navigation mode. I acts on release so it does not enter the input. Clicking a card list enters
Navigation mode; clicking the command box or a sidebar command enters Command mode. Mode changes are ignored during
the tutorial or pending deletion, keeping confirmation in the command box.

`CardNavigation` moves one card and scrolls it into view, without wrapping at either end. With no selection, J
selects first and K selects last. `PersonListPanel`, `EventListPanel`, and the tag list share this behavior.

**Command history.** [`CommandBox`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/CommandBox.java)
keeps submitted commands, including failures, for the current session. Up/Down browse history; Down past the newest
entry restores the draft typed before browsing. Submitting or inserting a sidebar command resets browsing to the end.
Delete-confirmation responses are excluded, and Up/Down do nothing while confirmation is pending.

#### Design considerations

| Decision | Benefit | Cost |
|---|---|---|
| Separate Navigation mode (chosen) | Single keys navigate without modifiers and type normally in Command mode | Users must track the mode, so it is always displayed |
| Always-available modifier shortcuts, such as Ctrl+J | No modes to learn | Can conflict with text-field and OS shortcuts |
| Session-only history (chosen) | No disk writes or accumulation across sessions | Lost on exit |
| Save history with preferences | Recall across sessions | Extra format maintenance; persists contacts' personal details in commands |

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target users:** University CCA EXCO members managing one organisation's contacts, events, and attendance for
current work and future committees. They prefer desktop apps and typing over mouse interaction, type quickly,
and are comfortable with CLI apps.

**Value proposition:** Manage CCA contacts, events, and attendance while preserving information for [committee handovers](#glossary).


### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

Priority measures importance; the tables distinguish implemented features from future ideas.

#### Current features

| Priority | As a … | I want to … | So that I can … |
|----------|--------|-------------|----------------|
| `* * *` | first-time user | view a tutorial | learn how to navigate CoordiMate |
| `* *` | returning user | reopen the tutorial | refresh my memory |
| `* *` | EXCO member | view formats and examples in Help | enter unfamiliar commands |
| `* * *` | EXCO member | save a contact's name, phone number, email address, and role | keep track of the people I know |
| `* * *` | EXCO member | edit a saved contact | update their information |
| `* * *` | EXCO member | confirm contact deletion | check the selected person before removing them |
| `* *` | EXCO member | record a contact's address, organisation, and note | retain relevant background |
| `* *` | EXCO member | record a contact's birthday | remember to wish the contact well |
| `* * *` | EXCO member | save changes automatically | retain work across restarts without a save command |
| `*` | EXCO member entering data quickly | enter phones with spaces, hyphens, or brackets | avoid removing accepted formatting |
| `* * *` | event lead | search contacts by name, phone number, email address, address, or organisation | retrieve specific contact information quickly |
| `* * *` | event lead | filter by tag, event, organisation, or combined criteria | retrieve relevant contact groups |
| `* *` | experienced user | recognise tag labels and built-in colours | identify categories at a glance |
| `* * *` | EXCO member | assign tags to contacts | categorise contacts for later retrieval |
| `* *` | EXCO member | choose common default tags | categorise contacts quickly |
| `* *` | EXCO member | create custom tags | use categories beyond the defaults |
| `* *` | EXCO member | rename or delete tags | update tags and affected contacts |
| `* *` | EXCO member | select a tag and view its contacts | see who belongs to that category |
| `* * *` | EXCO member | create an event with a name and start/end date/time | organise contacts and attendance around an activity |
| `* *` | EXCO member | edit an event's name or schedule | reflect changed plans |
| `* *` | EXCO member | delete an event | remove unneeded events, assignments, and attendance |
| `* * *` | EXCO member | assign contacts to events by permanent ID | track participants |
| `* * *` | EXCO member | remove contacts from events | correct assignments |
| `* * *` | EXCO member | view all event members | check committee allocation without overlooking participants |
| `* *` | EXCO member | create future events and assign members to them | plan committee allocation ahead of time |
| `*` | forgetful EXCO member | see upcoming reminders while the app runs | avoid missing events |
| `*` | EXCO member | dismiss event reminders | hide acknowledged reminders |
| `* * *` | EXCO member | mark assigned members present or absent and correct statuses | keep accurate attendance |
| `* *` | EXCO member | view present, absent, and unmarked counts | gauge turnout and incomplete attendance |
| `* *` | EXCO member | see attendance in the event roster | follow up with absent members |
| `*` | EXCO member | archive contacts | declutter active lists without deletion |
| `*` | EXCO member | archive events | focus on current work |
| `*` | EXCO member | view and restore archived contacts and events | retrieve history and resume active use |
| `*` | EXCO member | mark leave through a date | see temporary unavailability until leave ends |
| `* *` | EXCO member | switch Contacts, Events, and Tags views | navigate related information |
| `* *` | user who prefers the keyboard | browse cards and switch views with shortcuts | inspect data without a mouse |
| `* *` | returning user | recall session commands | reuse commands without retyping |
| `*` | EXCO member | clear all contacts, events, and custom tags | start a new dataset with the default tags |

#### Future ideas (not implemented)

| Priority | As a … | I want to … | So that I can … |
|----------|--------|-------------|----------------|
| `*` | EXCO member | save multiple phones/emails per contact | retain personal and work details |
| `*` | EXCO member | attach contact photos | match faces to names |
| `*` | EXCO member entering data quickly | enter Telegram handles with or without `@` | avoid reformatting |
| `* * *` | event lead | filter contacts by role | find contacts with a responsibility |
| `* *` | EXCO member | filter by assigned colour | find colour groups quickly |
| `*` | EXCO member | put urgent contacts first | identify them quickly |
| `* *` | EXCO member | assign/change contact colours and choose a default | update visual groups as roles or groups change |
| `*` | user with limited time | put the most-used event first | avoid extra navigation |
| `*` | EXCO member | view attendance edit history | trace status changes and avoid disputes |
| `*` | EXCO member | export attendance | support reporting and record-keeping |
| `*` | user who values a pleasant interface | switch light/dark modes | suit my environment |

### Use cases

**System:** `CoordiMate`. **Actor:** an EXCO member.

<box type="info" seamless>

**Note:** Read or save failures notify the user and leave data unchanged in every data-changing use case except
[reminder dismissal (UC12)](#uc12-dismiss-an-event-reminder).
</box>

#### UC01: Add a contact

**MSS**

1. EXCO member supplies a contact's name, phone, email, role, and any optional details or tags.
2. CoordiMate saves the contact and new tags, then confirms the save.

   Use case ends.

**Extensions**

* 1a. A required field is missing, a field is invalid or unknown, or a non-tag field is repeated.
  * 1a1. CoordiMate reports the error.
  * 1a2. EXCO member corrects the details.

    Repeat steps 1a1-1a2 until valid.<br>
    Use case resumes from step 2.

* 1b. More than 15 distinct tags remain after `t/` processing. Case variants count once; empty `t/` clears earlier tags.
  * 1b1. CoordiMate reports the tag limit.

    Use case resumes from step 1.

* 1c. The details create a [duplicate contact](#glossary).
  * 1c1. CoordiMate reports the duplicate and makes no changes.

    Use case ends.

#### UC02: Edit a contact

**MSS**

1. EXCO member identifies a contact by member ID or exact name, phone, or email, and supplies changes.
2. CoordiMate updates the contact and its name in every event, then shows the updated details.

   Use case ends.

A saved contact already above the 15-tag limit can keep or reduce its tag count using `at/` and `rt/`, or be edited
without changing its tags. A `t/` replacement must still contain at most 15 tags.

**Extensions**

* 1a. No changes are supplied, a detail is invalid, a non-tag field is repeated, or a field is unknown.
  * 1a1. CoordiMate reports the error.
  * 1a2. EXCO member corrects the details.

    Repeat steps 1a1-1a2 until valid.<br>
    Use case resumes from step 2.

* 1b. No contact matches.
  * 1b1. CoordiMate reports no match.

    Use case ends.

* 1c. Multiple contacts match the name.
  * 1c1. CoordiMate lists matches and IDs, and requests an ID, phone, or email.
  * 1c2. EXCO member supplies one.

    Use case resumes from step 2.

* 1d. Changes create a [duplicate contact](#glossary).
  * 1d1. CoordiMate reports the conflict and makes no changes.

    Use case ends.

* 1e. Replacement tags exceed 15, or tag operations increase the count above 15.
  * 1e1. CoordiMate reports the limit and makes no changes.

    Use case resumes from step 1.

#### UC03: Delete a contact

**MSS**

1. EXCO member identifies a contact by member ID or exact name, phone, or email.
2. CoordiMate shows the contact and requests confirmation.
3. EXCO member enters `y` or `Y`, ignoring surrounding whitespace.
4. CoordiMate deletes the contact, its event memberships, and attendance, then confirms deletion.

   Use case ends.

**Extensions**

* 1a. The identifier is missing, multiple identifiers are given, or the member ID is invalid.
  * 1a1. CoordiMate reports the error.
  * 1a2. EXCO member supplies one valid identifier.

    Use case resumes from step 2.

* 1b. No contact matches.
  * 1b1. CoordiMate reports no match.

    Use case ends.

* 1c. Multiple contacts match the name.
  * 1c1. CoordiMate lists matches and IDs, and requests an ID, phone, or email.
  * 1c2. EXCO member supplies one.

    Use case resumes from step 2.

* 3a. The response is not `y` or `Y`, ignoring surrounding whitespace.
  * 3a1. CoordiMate cancels without changes and consumes the input without executing it as a command.

    Use case ends.

#### UC04: Search contacts

**MSS**

1. EXCO member supplies a keyword or phrase.
2. CoordiMate lists and counts contacts whose name, phone, email, address, or organisation contains it, ignoring case.
   Phones use [normalised matching](#glossary); extra spaces in keywords and addresses count as one.

   Use case ends.

**Extensions**

* 1a. The keyword is empty or blank, including `search ""` or `search "   "`.
  * 1a1. CoordiMate reports the error.

    Use case ends.

* 2a. No contacts match.
  * 2a1. CoordiMate shows an empty list and reports no matches.

    Use case ends.

#### UC05: Filter contacts

**MSS**

1. EXCO member supplies one or more tag, event, or organisation criteria.
2. CoordiMate lists and counts contacts matching every criterion.

   Use case ends.

**Extensions**

* 1a. Criteria are missing, empty, or unsupported.
  * 1a1. CoordiMate reports the error and leaves the list unchanged.

    Use case ends.

* 1b. A value matches no saved tag, event, or organisation.
  * 1b1. CoordiMate reports the unknown value and leaves the list unchanged.

    Use case ends.

* 2a. No contact matches all criteria.
  * 2a1. CoordiMate shows an empty list and reports no matches.

    Use case ends.

#### UC06: Mark a contact as on leave

**MSS**

1. EXCO member supplies a leave end date when adding (UC01) or editing (UC02) a contact.
2. CoordiMate saves the date and shows the contact as on leave through that date.
3. After the date passes, CoordiMate stops showing leave automatically.

   Use case ends.

**Extensions**

* 1a. The date is invalid or past.
  * 1a1. CoordiMate reports the error and makes no changes.

    Use case resumes from step 1.

* 2a. EXCO member clears leave before it ends.
  * 2a1. CoordiMate removes the date and stops showing leave.

    Use case ends.

#### UC07: Create an event

**MSS**

1. EXCO member supplies an event name, start date/time, and end date/time.
2. CoordiMate creates the event and confirms its name, start, and end.

   Use case ends.

**Extensions**

* 1a. A field is missing, empty, repeated, malformed, or unknown.
  * 1a1. CoordiMate reports the error.
  * 1a2. EXCO member corrects the details.

    Repeat steps 1a1-1a2 until valid.<br>
    Use case resumes from step 2.

* 1b. Start is after end.
  * 1b1. CoordiMate reports the error.

    Use case resumes from step 1.

* 1c. The name already exists, ignoring case and extra spaces.
  * 1c1. CoordiMate reports the duplicate and how to restore it if [archived](#uc13-archive-and-restore-a-contact).

    Use case ends.

#### UC08: Edit an event

**MSS**

1. EXCO member supplies changes to an event's name, start, or end.
2. CoordiMate updates the event, preserves members and attendance, and confirms the details.

   Use case ends.

**Extensions**

* 1a. No changes are supplied, or a detail is empty or malformed.
  * 1a1. CoordiMate reports the error.

    Use case resumes from step 1.

* 1b. The event name is unknown.
  * 1b1. CoordiMate reports the missing event.

    Use case ends.

* 1c. Another event has the new name, ignoring case and extra spaces.
  * 1c1. CoordiMate reports the conflict and makes no changes.

    Use case ends.

* 1d. The new start is after end.
  * 1d1. CoordiMate reports the error and makes no changes.

    Use case ends.

Deleting an event is similar: identify it by name, then delete it, its memberships, and attendance without
confirmation. Contacts remain. Extension 1b also applies.

#### UC09: Assign members to an event

**MSS**

1. EXCO member supplies an event and one or more contact member IDs.
2. CoordiMate assigns the contacts with unmarked attendance and confirms the number added.

   Use case ends.

**Extensions**

* 1a. Member IDs are missing or invalid.
  * 1a1. CoordiMate reports the error.

    Use case resumes from step 1.

* 1b. The event or any member ID is unknown.
  * 1b1. CoordiMate reports the error and assigns nobody.

    Use case resumes from step 1.

* 1c. The event or any contact is archived.
  * 1c1. CoordiMate requests restoration first and assigns nobody.

    Use case ends.

* 1d. A member ID is repeated.
  * 1d1. CoordiMate processes each distinct ID once without error.

    Use case resumes from step 2.

* 2a. Some contacts are already assigned.
  * 2a1. CoordiMate adds only the others and reports the already-assigned count.

    Use case ends.

* 2b. All contacts are already assigned.
  * 2b1. CoordiMate reports no changes.

    Use case ends.

For removal, step 2 removes memberships and their attendance. In 2a/2b, substitute contacts not assigned for those
already assigned. Extension 1c applies only to archived events; archived contacts can still be removed.

#### UC10: View members of an event

**MSS**

1. EXCO member requests an event's members.
2. CoordiMate filters the contact list to those members and reports the count.

   Use case ends.

**Extensions**

* 1a. The event is unknown.
  * 1a1. CoordiMate reports the missing event and leaves the contact list unchanged.

    Use case ends.

* 2a. The event has no members.
  * 2a1. CoordiMate shows an empty list and reports no members.

    Use case ends.

* 2b. Some members are outside the view: archived members in normal view, or active members in archived view.
  * 2b1. CoordiMate shows only members matching the view and reports the hidden count.

    Use case ends.

* 2c. A member is assigned or removed, or the event is renamed, while members are shown.
  * 2c1. CoordiMate updates the list to current members.

    Use case ends.

#### UC11: Record event attendance

**MSS**

1. EXCO member views an event's members and attendance.
2. EXCO member marks a member present or absent.
3. CoordiMate records and confirms the status.
4. Repeat steps 2-3 for each member to mark.

   Use case ends.

**Extensions**

* 2a. The event is unknown or the contact is not assigned.
  * 2a1. CoordiMate reports the error.

    Use case resumes from step 2.

* 2b. The status is neither present nor absent.
  * 2b1. CoordiMate reports valid statuses.

    Use case resumes from step 2.

* 2c. The status is unchanged.
  * 2c1. CoordiMate reports no changes.

    Use case resumes from step 4.

#### UC12: Dismiss an event reminder

**Preconditions:** At least one event has an eligible, undismissed [reminder](#glossary).

**MSS**

1. CoordiMate shows numbered **Upcoming events** reminders in start order.
2. EXCO member supplies a reminder number to dismiss.
3. CoordiMate hides it, saves the dismissal, and renumbers the remaining reminders.

   Use case ends.

**Extensions**

* 2a. The reminder number is unknown.
  * 2a1. CoordiMate reports the error.

    Use case resumes from step 2.

* 3a. Data cannot be loaded or saved.
  * 3a1. CoordiMate hides the reminder for this session and warns that it may return after restart.

    Use case ends.

* 3b. EXCO member later changes the event's start.
  * 3b1. CoordiMate shows a reminder if the new start [qualifies](#glossary).

    Use case ends.

#### UC13: Archive and restore a contact

**MSS**

1. EXCO member requests archiving by member ID.
2. CoordiMate hides the contact, preserves memberships and attendance, and confirms archiving.
3. EXCO member requests archived contacts and events.
4. CoordiMate shows only archived items.
5. EXCO member requests restoration by member ID.
6. CoordiMate confirms restoration. The contact leaves the archived results and is available among active contacts.

   Use case ends.

**Extensions**

* 1a. The member ID is unknown.
  * 1a1. CoordiMate reports the error.

    Use case ends.

* 1b. The contact is already archived.
  * 1b1. CoordiMate reports no changes.

    Use case ends.

* 5a. The contact is not archived.
  * 5a1. CoordiMate reports no changes.

    Use case ends.

Events work similarly, identified by name. Archived events have no reminders, reject assignment/removal
([UC09](#uc09-assign-members-to-an-event)), and retain their names ([UC07](#uc07-create-an-event)).

#### UC14: Manage tags

**MSS**

1. EXCO member requests a new tag.
2. CoordiMate adds it to the tag list and confirms.
3. EXCO member requests a rename.
4. CoordiMate renames it globally and on every affected contact, then confirms.
5. EXCO member requests deletion.
6. CoordiMate removes it globally and from every affected contact, then confirms.

   Use case ends.

**Extensions**

* 1a. The name is invalid, for example because it contains spaces or exceeds 30 characters.
  * 1a1. CoordiMate reports the error.

    Use case resumes from step 1.

* 1b. The name exists, ignoring case.
  * 1b1. CoordiMate reports the duplicate and makes no changes.

    Use case ends.

* 3a. The tag to rename does not exist.
  * 3a1. CoordiMate reports the missing tag.

    Use case ends.

* 3b. The new name is invalid or conflicts with another tag.
  * 3b1. CoordiMate reports the error and makes no changes.

    Use case resumes from step 3.

* 5a. The tag to delete does not exist.
  * 5a1. CoordiMate reports the missing tag.

    Use case ends.

### Non-Functional Requirements

These are acceptance targets, not measured results. Test performance with one CoordiMate instance and valid data
on writable local storage; record OS, Java version, processor, memory, and dataset. Use timed trials with target
users for speed and onboarding targets.

1. Run on [mainstream OS platforms](#glossary) with Java `25`, targeting CI configurations. Manually launch the
   packaged JAR on each target platform before claiming release compatibility.
2. Store and reload the maximum [typical load](#glossary). Saved contact details, IDs, tags, schedules, assignments,
   attendance, archive flags, leave dates, and reminder dismissals should survive normal restarts. Assess performance
   under NFR 4. This is a whole-dataset capacity target, not an enforced limit; data is not partitioned by semester.
3. For users familiar with shortcuts, keyboard navigation should be faster on average than mouse navigation in
   this benchmark. Use sample data, starting in Contacts with Alex Yeoh selected and the command box focused in
   Command mode. Select Bernice, select Alex again, switch to Events, Tags, then Contacts, and return command-box
   focus. After two practice trials per method, alternate methods for 10 timed trials each, resetting the starting
   state before every trial. Time from a start signal until command-box focus returns, including mode changes.
   Compare arithmetic means for the same user and machine; the keyboard mean should be lower.
4. At [typical load](#glossary), every contact, tag, event, attendance, or reminder command should have a median
   response time of at most 2 seconds over repeated trials on the recorded machine, including `delete`, `unassign`,
   `archiveevent`, `unarchiveevent`, `dismiss`, and `clear`. Measure Enter to feedback and completed view update,
   including saving. Exclude startup and confirmation waiting; time the prompt and confirmed response separately.
5. Contact, tag, event, attendance, archive, reminder, Help, and tutorial features should work offline. The online
   User Guide requires internet access.
6. If the app process terminates during saving while the OS runs, an existing valid data file should retain the old
   or new complete dataset, assuming [atomic replacement support](#saving-data-atomically). Exclude preferences,
   OS crashes, power loss, hardware failure, and external file changes.
7. Store readable JSON that ordinary tools can inspect and back up. Manual edits require the app to be closed
   and must meet saved-data constraints.
8. Distribute one application JAR without a CoordiMate installer, assuming Java is installed.
9. After completing or closing the tutorial, a first-time target user should add a contact within 10 minutes,
   without another person's help. Supply valid, unique name, phone, email, and role values. Time from input-ready
   command box to the saved contact's visible details and success message. Include Help, command hints, User Guide
   consultation, and corrections; exclude installation, launch, and tutorial reading.
10. Every [colour-coded tag](#glossary) should show its name so categories are distinguishable without colour.

### Glossary

* **Archive**: Hide a contact or event from active lists while preserving details, assignments, and attendance.
* **Assigned member**: A contact assigned to an event, also called its *member*.
* **Attendance status**: An assigned member's recorded `present` or `absent` value. No record means unmarked, not absent.
* **CCA (Co-Curricular Activity)**: A university club or society, such as a band, sports team, or interest group.
* **CLI (Command Line Interface)**: A text-command interface; CoordiMate provides a command box in its graphical window.
* **Colour-coded tag**: A tag coloured by name. `EXCO`, `Logistics`, `Sponsor`, and `UniversityStaff` have distinct built-in colours; others share a default. Users cannot choose tag or contact colours.
* **Command mode**: The keyboard mode for entering commands in the command box.
* **Committee handover**: Transfer of responsibilities and information between outgoing and incoming EXCOs.
* **Contact**: A saved person identified by member ID.
* **Custom tag**: A user-created contact category beyond the default tags.
* **Default tag**: A built-in tag: `EXCO`, `Sponsor`, `UniversityStaff`, or `Logistics`.
* **Duplicate contact**: A contact with the same name (ignoring case and normalising spaces), normalised phone number, or email address (ignoring case) as another saved contact, including an archived one.
* **Event**: A saved CCA activity, such as a concert or fair, with a unique name ignoring case and extra spaces.
* **Event lead**: An EXCO member organising an event.
* **EXCO (Executive Committee)**: Students elected to lead and run a CCA.
* **Future event**: An event starting after the current date/time.
* **Home folder**: The JAR's folder, containing data, `preferences.json`, and logs regardless of launch directory. IDE or Gradle runs use the working directory.
* **JAR file**: A Java application packaged to run without installation.
* **Local data file**: JSON storing the application data in [NFR 2](#non-functional-requirements), at `data/coordimate.json` by default, relative to the [home folder](#glossary).
* **Mainstream OS**: Windows, macOS, or Linux.
* **Member ID**: A contact's unique, permanent positive integer, displayed as `ID: <number>` and used across searches, filters, and restarts.
* **Navigation mode**: The keyboard mode in which key presses browse cards and switch views.
* **Normalised phone number**: A phone number with the optional `+`, spaces, hyphens and brackets removed, used to detect duplicates. For example, `+65 9123 4567` and `6591234567` are the same.
* **On leave**: Unavailable through the saved leave end date, inclusive, using the computer's local date.
* **Organisation**: A contact's company or body, such as a CCA, sponsor, or university office.
* **Parameter-like text**: Text shaped like `[whitespace][letters]/` (e.g. ` r/`) that a parser may treat as a parameter,
  not part of a value. The brackets are descriptive: whitespace includes spaces, tabs, Java whitespace and Unicode
  `White_Space`; letters are consecutive ASCII letters (`A-Z` or `a-z`) adjacent to the slash. Preceding whitespace
  is unnecessary at the start of the arguments; whitespace after the slash is not required. See [Parameter detection](#parameter-detection)
  for command-specific rules and exceptions.
* **Prefix**: A command field label, such as `n/` for name or `t/` for tag.
* **Reminder**: An in-app notice for an upcoming, active event. Timed starts qualify from 24 hours before start through start time; date-only starts qualify on the preceding day and start date. Refreshes at startup, after commands, and every minute while the app runs.
* **Role**: A contact's responsibility or position in relation to the CCA, such as `Logistics Lead` or `Vice-President`.
* **Tag**: A contact category such as `EXCO` or `Sponsor`; contacts can have multiple tags.
* **Tutorial**: An in-app introduction to CoordiMate's views, commands, and keyboard navigation.
* **Typical load**: A local dataset of up to 250 contacts and 6 events in total, including archived items, with up to 250 assigned members and their attendance records per event.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Run each section's tests in order, following its setup. Enter commands in Command mode; after browsing cards in
Navigation mode, press <kbd>I</kbd> or click the command box before typing. Relative paths use the
[home folder](#glossary), regardless of where `java -jar` runs.

<box type="info" seamless>

Use these cases as a starting point for exploratory testing. For every command error, also check that feedback
includes usage, parameters, and an example. Quoted feedback below is in addition to that usage.
</box>

<box type="info" seamless>

**Restore the sample data** whenever instructed; earlier tests may leave data behind:

1. Close the app.
1. Delete `data/coordimate.json` from the home folder.
1. Relaunch the app.

This restores six contacts (IDs `1` to `6`), `Freshmen Welcome Night`, default tags, and custom tags `friends` and
`neighbours`. Deleting contacts alone does not reset member IDs, which are never reused.
</box>

<box type="info" seamless>

Date placeholders use the local time zone and `dd-MM-yyyy`: `TODAY`, `TOMORROW`, any `FUTURE_DATE` after today, and any
`PAST_DATE` before today.
</box>

### Launch and shutdown

| Test case | Expected |
|---|---|
| Download the JAR into an empty folder; open a terminal there and run `java -jar coordimate.jar` | Maximized GUI with sample contacts and event. The tutorial opens and can show assigned members and attendance. Close it before continuing. |
| Unmaximize, resize distinctively, move, close, and run `java -jar coordimate.jar` again | Restores the unmaximized size and position. |
| `exit`, then relaunch | App closes; data survives relaunch. |

### Tutorial and Help

Esc or **Close** marks the tutorial as seen in `preferences.json`; restoring sample data does not reset this flag.

| Test case | Expected |
|---|---|
| `tutorial`; press Right, Left, Tab, and Shift+Tab | Starts at **Step 1 of 16**. Arrows change steps; Tab cycles enabled buttons. Target stays bright; command box is disabled. Esc closes; F2 (Fn+F2 if needed) reopens step 1. Close with Esc again. |
| Close the app; set `hasSeenTutorial` to `false` in `preferences.json` without changing window settings. Relaunch, advance through step 16, select **Finish** without otherwise closing, then close and relaunch again | Tour opens automatically after reset, visits Contacts, Events, Tags, and Help, then closes on Finish. It stays closed on final relaunch; contacts, events, and tags are unchanged. |
| `help`; resize and scroll the command table | Shows keyboard navigation, command formats, and the User Guide URL. Esc closes Help without closing CoordiMate; F1 (Fn+F1 if needed) reopens it. Dismiss with **Close**. |
| `tutorial extra`, `help extra` | Neither opens; `Invalid command format.` |

### Keyboard navigation

**Setup:** Restore sample data, run `list`, select Alex Yeoh, then click the command box and type `search Bernice`
without submitting.

| Test case | Expected |
|---|---|
| Esc, J, K, K | **NAVIGATION**; J selects Bernice, K selects Alex, another K stays at Alex. Draft remains in the disabled command box. |
| S three times; use J/K in Events and Tags; try Enter and Up | Cycles Contacts, Events, Tags, Contacts. J/K update selection and details. Alex stays selected on return. Enter/Up neither submit nor replace the draft. |
| I, then Enter | **COMMAND**; input regains focus with `search Bernice` unchanged. Enter executes it; only Bernice Yu remains. |

### Command history

**Setup:** Restore sample data; run `list`, then `search alex`.

| Test case | Expected |
|---|---|
| Type `search Bernice` without submitting; Up twice, Down twice | Input shows `search alex`, `list`, `search alex`, then draft `search Bernice`. Nothing executes; only Alex remains listed. |
| Up; change to `search Charlotte`; Enter; Up twice | Only Charlotte is listed. History recalls `search Charlotte`, then original `search alex`; editing did not overwrite the earlier entry. |
| Close, relaunch, press Up in empty input | Input stays empty; history was not retained. |

### Adding a contact

**Setup:** Restore sample data; run `list`.

| Test case | Expected |
|---|---|
| `add n/Aisha Tan p/+6591234567 e/aisha@example.com r/Logistics` | Contacts selects Aisha Tan, `ID: 7`, with supplied details. `Contact saved successfully: Aisha Tan` |
| Repeat the add | No addition. `A contact with this name, phone number, or email already exists. No changes were made.` |
| `add n/Ben Ong p/91112222 e/ben@example.com` | No addition; missing role gives `Invalid command format.` |
| `add n/Ben Ong p/abc e/ben@example.com r/Member` | No addition; feedback explains phone constraints. |

### Editing a contact

**Setup:** Restore sample data; `search Charlotte` shows only ID `3`.

| Test case | Expected |
|---|---|
| `edit 3 r/Treasurer o/NUS Student Affairs` | Lists all six contacts; selects Charlotte with new role/organisation, ID `3`, and other fields unchanged. `Contact updated successfully:` followed by details. |
| `edit 3 o/` | Clears only Charlotte's organisation. |
| `edit 3 n/Bernice Yu` | No changes. `This update conflicts with another saved contact. No changes were made.` |
| `edit 3` | No changes. `Provide at least one field or tag operation to edit.` |
| `edit 99 r/Treasurer`, with no ID `99` saved | No changes. `No contact exists with member ID: 99. Use an existing member ID.` |

### Deleting a contact

**Setup:** Restore sample data; run `list`. Alex Yeoh has ID `1`.

| Test case | Expected |
|---|---|
| `delete 1`, then `n` | Shows `ID: 1`, Alex's name, phone, email, role, and `Confirm deletion? [y/N]`. `n` cancels without deletion: `Deletion cancelled. No changes were made.` |
| `delete 1`, then `y ` (trailing space) | Deletes Alex (ID `1`); other IDs stay unchanged. `Contact deleted successfully: ` followed by his name. |
| `delete 0` | No confirmation or deletion. `Invalid member ID. Example: delete 12` |
| `delete`, `delete x`, `delete 99` (ID `99` absent) | No confirmation or deletion; error details shown. |

### Searching contacts

**Setup:** Restore sample data.

| Test case | Expected |
|---|---|
| `search alex` | Only `Alex Yeoh`. `1 contact(s) found matching "alex".` |
| `search zzz` | Empty list. `No contacts found matching "zzz".` |
| `search` | List unchanged. `Invalid command format.` |

### Listing contacts and events

**Setup:** Restore sample data; run `search alex`.

| Test case | Expected |
|---|---|
| `list` | Contacts shows all six active contacts; Events contains the sample event. `Listed all contacts.` |
| `list archived`, then `list` | Archived lists are empty; `list` restores active lists. Archive changes are tested below. |
| `list all` | Lists unchanged. `Unknown list option: all. Use list, or list archived to show archived contacts and events.` |

### Filtering contacts

**Setup:** Restore only sample data (IDs `1` to `6`), then run in order:

1. `edit 3 o/NUS Student Affairs`
1. `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`
1. `assign evn/Final Concert c/1 2`
1. `addtag t/Publicity`

| Test case | Expected |
|---|---|
| `filter tag/exco` | `Alex Yeoh`, `Bernice Yu`, `Irfan Ibrahim`, in order. `3 contact(s) found.` |
| `filter event/Final Concert tag/EXCO` | `Alex Yeoh` and `Bernice Yu`; both match `Final Concert` and `EXCO`. |
| `filter organisation/NUS Student Affairs` | Only `Charlotte Oliveiro`. |
| `filter organisation/NUS` | Only `Charlotte Oliveiro` remains. `No saved tag, event, or organisation matches that value. Use a value from the saved data.` |
| `filter tag/Publicity` | Empty list: the saved tag has no contacts. `No contacts match the specified filters.` |
| `filter role/Logistics` | List stays empty. `Unknown parameter: role/.` |
| `filter`, `filter tag/` | List unchanged; error details shown. |
| Cleanup: `deletetag t/Publicity`, then `deleteevent evn/Final Concert` | Removes `Publicity` and `Final Concert` so later sections can recreate them. |

### Adding an event

**Setup:** No `Final Concert` or `Fair` event exists.

| Test case | Expected |
|---|---|
| `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00` | Events opens and selects the new event. `Created Event Final Concert. Start Time: 08-08-2026 15:00. End Time: 08-08-2026 18:00.` |
| `addevent evn/final concert st/09-10-2026 et/10-10-2026` | No addition. `An event with this name already exists. No changes were made.` |
| `addevent evn/Fair st/10-10-2026 et/09-10-2026` | No addition. `Start time must not be after end time. Example: st/17-09-2026 16:30 et/17-09-2026 18:30` |
| `addevent evn/Fair st/10/10/2026 et/11-10-2026` | No addition; required date/time format shown. |
| `addevent`, `addevent evn/Fair st/10-10-2026` | No addition; error details shown. |

### Editing an event

**Setup:** Select the previous section's `Final Concert` in Events (08-08-2026, 15:00 to 18:00). If absent, create it
with `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`.

| Test case | Expected |
|---|---|
| `editevent evn/final concert st/08-08-2026 16:00 et/08-08-2026 19:00` | Details show new times. `Edited Event Final Concert. Start Time: 08-08-2026 16:00. End Time: 08-08-2026 19:00.` |
| `editevent evn/Final Concert nevn/Grand Concert` | Renamed to `Grand Concert`; times unchanged. |
| `editevent evn/Grand Concert st/09-08-2026` | No change. `Start time must not be after end time. Example: st/17-09-2026 16:30 et/17-09-2026 18:30` |
| `editevent evn/Grand Concert`, `editevent evn/Grand Concert nevn/` | No change; error details shown. |

### Deleting an event

**Setup:** `Grand Concert` exists from the previous section.

| Test case | Expected |
|---|---|
| `deleteevent evn/Gala` | No deletion. `Event Gala does not exist. Use a saved event name.` |
| `deleteevent evn/grand concert` | Removed from Events. `Deleted Event Grand Concert.` |
| `deleteevent`, `deleteevent evn/` | No deletion; error details shown. |

### Dismissing a reminder

#### Reminder order and persistence

**Setup:** Start with no active reminders. Create `Zulu Reminder` using
`addevent evn/Zulu Reminder st/TOMORROW et/TOMORROW`, then `alpha Reminder` with the same dates.

| Test case | Expected |
|---|---|
| Switch Contacts, Events, and Tags | Upcoming events stays visible: `alpha Reminder` at 1, `Zulu Reminder` at 2, with saved dates. |
| In Tags, run `dismiss 1` | `alpha Reminder` disappears; `Zulu Reminder` becomes 1. **OK**: `Reminder dismissed: alpha Reminder.` Tags stays selected; input keeps focus; both events retain their details in Events. |
| Close/reopen while events still qualify | Only `Zulu Reminder` appears, at 1; `alpha Reminder` stays saved. |
| `dismiss 2` | Nothing dismissed. `No reminder exists at index 2. Use an index from the Upcoming events panel.` |
| `dismiss 1`, then `dismiss 1` again | First hides `Zulu Reminder` and the empty panel; second reports no reminder at 1. Both events remain saved. |

#### Invalid input and cleanup

**Setup:** Show at least one reminder before testing invalid input.

| Test case | Expected |
|---|---|
| `dismiss`, `dismiss 0` | No reminder/event changes. **Rejected** with explanation: `Invalid command format.` for missing index; `Index must be a positive integer. Example: 1` for `dismiss 0`. |
| `deleteevent evn/Zulu Reminder`, then `deleteevent evn/alpha Reminder` | Removes both events from Events and their reminders. |

### Assigning members to an event

#### All contacts shown

**Setup:** Restore sample data; run `list`. Alex, Bernice, and Charlotte have IDs `1`, `2`, `3`. Create
`addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00` and select it in Events.

| Test case | Expected |
|---|---|
| `assign evn/Final Concert c/1 2` | **Members (2)** lists Alex Yeoh (`ID: 1`) and Bernice Yu (`ID: 2`). `Assigned 2 member(s) to Final Concert.` |
| `assign evn/final concert c/2 3` | Adds only Charlotte Oliveiro (`ID: 3`). Feedback includes `1 contact(s) were already assigned.` |
| `assign evn/Final Concert c/0`, `assign evn/Final Concert c/99` (ID `99` absent) | No changes; error details shown. |

#### Filtered contacts

**Setup:** Keep sample contacts. Create `addevent evn/Filtered Fair st/08-08-2026 et/08-08-2026`;
`search Charlotte` shows only `ID: 3`.

| Test case | Expected |
|---|---|
| `assign evn/Filtered Fair c/3 1` | Assigns Charlotte Oliveiro (`ID: 3`), then hidden Alex Yeoh (`ID: 1`). IDs stay unchanged despite the search order. |

### Removing members from an event

#### All contacts shown

**Setup:** Restore sample data; run `list`. Create
`addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, run `assign evn/Final Concert c/1 2 3`, and select it in Events.

| Test case | Expected |
|---|---|
| `unassign evn/final concert c/2 4` | **Members (2)** lists IDs `1`, `3` in order. `Removed 1 member(s) from Final Concert. 1 contact(s) were not assigned.` |
| `unassign evn/Final Concert c/0`, `unassign evn/Final Concert c/1 99` (ID `99` absent) | No changes; error details shown. |

#### Filtered contacts

**Setup:** Keep IDs `1` and `3` assigned to `Final Concert`; run `search Charlotte` to show only `ID: 3`.

| Test case | Expected |
|---|---|
| `unassign evn/Final Concert c/3` | Removes Charlotte Oliveiro (`ID: 3`); Alex Yeoh (`ID: 1`) stays assigned. |

### Viewing members of an event

**Setup:** Restore sample data; run `list`. Create
`addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, run `assign evn/Final Concert c/3 1`, and switch to Events.

| Test case | Expected |
|---|---|
| `members evn/final concert` | Contacts opens with Alex Yeoh (`ID: 1`), then Charlotte Oliveiro (`ID: 3`), in contact-list order. `Listed 2 member(s) of Final Concert. Use list to show all contacts.` |
| `members`, `members evn/Final Concert c/1` | Contact list unchanged; error details shown. |
| With `Final Concert` members shown, `unassign evn/Final Concert c/1`, then `editevent evn/Final Concert nevn/Grand Concert` | Alex disappears after `unassign`; Charlotte stays the only result, `ID: 3`, after both commands. |
| `deleteevent evn/Grand Concert`, then `list` | Event deletion empties the contact list; `list` restores all contacts. |

### Marking attendance

#### Marking and changing attendance

**Setup:** Restore sample data; run `list`. Create
`addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`, run `assign evn/Final Concert c/1 2`, and select it in Events.

| Test case | Expected |
|---|---|
| `markattendance evn/Final Concert mem/Alex Yeoh att/present` | `Alex Yeoh`: `● present`; counts 1 present, 0 absent, 1 not marked. `Marked Alex Yeoh as present for Event Final Concert.` |
| `markattendance evn/final concert mem/alex yeoh att/ABSENT` | `Alex Yeoh`: `● absent`; counts 0 present, 1 absent, 1 not marked. |
| `markattendance evn/Final Concert mem/David Li att/present` | No change. `David Li is not assigned to event Final Concert. Assign the contact to the event before marking attendance.` |
| `markattendance evn/Final Concert mem/Alex Yeoh att/late`, `markattendance evn/Final Concert mem/Alex Yeoh` | No change; error details shown. |

#### Contact changes and persistence

**Setup:** Continue with `Alex Yeoh` absent; run `list`.

| Test case | Expected |
|---|---|
| `edit 1 n/Alex Tan`; select `Final Concert` in Events | `Alex Tan` stays in the same member position, `● absent`. |
| `unassign evn/Final Concert c/1`, then `assign evn/Final Concert c/1` | Removes `Alex Tan`, then appends him as `● unmarked`. |
| `markattendance evn/Final Concert mem/Alex Tan att/present`; close/relaunch; select `Final Concert` in Events | `Alex Tan` is still `● present`. |
| `delete 1`, then `y`; select `Final Concert` in Events | Removes `Alex Tan` and his attendance; other members stay unchanged. |

### Marking a contact as on leave

#### Setting and clearing leave

**Setup:** Restore sample data; run `list`.

| Test case | Expected |
|---|---|
| `edit 1 lv/FUTURE_DATE` | ID `1`'s card shows **On leave until FUTURE_DATE**; selected details show `FUTURE_DATE` under **On leave until**. |
| `edit 1 lv/` | Badge disappears; details show `— (not on leave)`. |
| `edit 1 lv/PAST_DATE` | No change. `Invalid leave end date. Use a real date in dd-MM-yyyy format that is not in the past. Example: lv/30-11-2026` |
| `edit 1 lv/31-11-2030`, `edit 1 lv/next week` | No change; error details shown. |

#### Leave ending automatically

**Setup:** Run `edit 1 lv/FUTURE_DATE`, then close.

| Test case | Expected |
|---|---|
| Change that contact's `leaveUntil` in `data/coordimate.json` to `PAST_DATE`; save and launch | Loads successfully; no leave badge; details show `— (not on leave)`. `leaveUntil` remains saved. |
| `edit 1 lv/TODAY`; select the contact; wait past midnight or advance the computer date one day | Initially **On leave until TODAY**. Within a minute after midnight, without commands, badge disappears and details show `— (not on leave)`. |

### Managing tags

**Setup:** Restore sample data.

| Test case | Expected |
|---|---|
| <kbd>Esc</kbd>, then <kbd>S</kbd> until Tags opens; select `EXCO` | Tags lists `EXCO, Sponsor, UniversityStaff, Logistics, friends, neighbours`. `EXCO` shows Alex Yeoh, Bernice Yu, and Irfan Ibrahim. |
| <kbd>I</kbd> to Command mode; `addtag t/Publicity` | `Publicity` appears in Tags. `Created tag: Publicity.` |
| `addtag t/publicity` | No addition. `A tag with this name already exists. No changes were made.` |
| `addtag t/Event Crew` | No addition. `Invalid tag name. Use letters and numbers without spaces. Example: t/Publicity` |
| `edit 1 at/Publicity`, then `edittag publicity t/Media` | Tags and ID `1`'s details show `Media`. `Publicity successfully renamed to Media.` |
| `deletetag t/media` | Removed from Tags and ID `1`. `Media successfully deleted.` |
| `deletetag t/Nope` | No deletion. `Tag Nope does not exist. Use a saved tag name.` |
| `addtag`, `edittag EXCO`, `deletetag EXCO` | No changes; error details shown. |

### Archiving contacts and events

#### Contacts

**Setup:** Restore sample data; run `list`. Bernice Yu has ID `2`.

| Test case | Expected |
|---|---|
| `archive 2` | Contact disappears; Contacts count drops by 1. `Archived contact: {NAME}.` |
| Close/relaunch; `list archived` | **Archived contacts (1)** and **Archived events (0)**. Only Bernice appears, with an **Archived** badge; sidebar contains only `unarchive` and `list`. |
| `unarchive 2`, then `list` | Contact leaves the archived list, which shows `No archived contacts.`; `list` restores her to Contacts and restores all contact sidebar commands. |
| `archive`, `archive 0`, `unarchive 2` on an active contact, `list archive` | No change; error details shown. |

#### Events

**Setup:** Create `addevent evn/Orientation st/TOMORROW et/TOMORROW`. Assign the first contact using its member ID
in `assign evn/Orientation c/ID`. `Orientation` has a reminder.

| Test case | Expected |
|---|---|
| `archiveevent evn/orientation` | Events opens from any tab; event and reminder disappear. `Archived Event Orientation.` Contact details still list `Orientation (archived)`. |
| `assign evn/Orientation c/ID` with another ID | No change; feedback explains the event is archived and suggests `unarchiveevent`. |
| `addevent evn/Orientation st/TOMORROW et/TOMORROW` | No addition; feedback reports an archived event with that name. |
| Close/relaunch; `list archived`; open Events | Tab and list heading show **Archived events (1)**; sidebar contains only `unarchiveevent` and `list`. |
| `unarchiveevent evn/Orientation`, then `list` | Archived list shows `No archived events.`; after `list`, Events contains the event and members, and its reminder returns. |

#### Archived contacts in events and tags

**Setup:** First contact belongs to `Orientation` and has `friends`; archive it with `archive ID`.

| Test case | Expected |
|---|---|
| Select `Orientation` in Events, then `friends` in Tags | Both member lists append `(archived)` to the contact's name. |
| `assign evn/Orientation c/ID` using the archived ID | No change; feedback explains the contact is archived and suggests `unarchive`. |
| `unassign evn/Orientation c/ID` | Removes the archived contact from the event. |

### Clearing all data

<box type="warning" seamless>

**Caution:** `clear` immediately deletes saved data without undo. Back up `data/coordimate.json` first if needed.
</box>

**Setup:** Restore sample data; run `archive 2`, `archiveevent evn/Freshmen Welcome Night`, `addtag t/ClearTest`,
and `addevent evn/ClearTest st/TOMORROW et/TOMORROW`. Bernice and `Freshmen Welcome Night` are archived;
`ClearTest` is active with a reminder.

| Test case | Expected |
|---|---|
| `clear extra` | No changes. `Invalid command format.` |
| `clear`, then `list archived` | No confirmation; `CoordiMate has been cleared!` Removes active/archived contacts and events; reminder panel hides. Tags contains only `EXCO`, `Sponsor`, `UniversityStaff`, `Logistics`; `ClearTest` is gone. Archived lists are empty. |
| Close/relaunch; `list`; `add n/ID Reset p/91234567 e/idreset@example.com r/Member` | Contact/event lists remain empty before the add; ID Reset receives `ID: 1`. |

### Saving data

<box type="warning" seamless>

**Caution:** These tests modify or remove data. Back up `data/coordimate.json` first if needed.
</box>

#### Missing data file

**Setup:** Close the app; `data/coordimate.json` exists.

| Test case | Expected |
|---|---|
| Delete `data/coordimate.json`; launch; `list` | Sample contacts and `Freshmen Welcome Night`, with two members and Alex present. Launch and `list` do not create `data/coordimate.json`. |
| `addtag t/Test`; inspect `data` | Recreates `data/coordimate.json` with sample data and `Test`. |

#### Legacy member IDs

**Setup:** Restore sample data; `edit 1 r/President` creates `data/coordimate.json` without changing Alex's existing
role. Close the app; remove every contact's `memberId` and top-level `nextMemberId`, keeping valid JSON.

| Test case | Expected |
|---|---|
| Relaunch | IDs `1` to `6` allocated in file order and shown consistently on cards/details. File unchanged. |
| `search Charlotte`; `edit 3 r/Treasurer`; delete IDs `2` and `6`, confirming each with `y`; close/relaunch; `add n/ID Test p/91234567 e/idtest@example.com r/Member` | Charlotte keeps `ID: 3` and new role; Bernice and Roy are absent. New contact receives `ID: 7`; deleted IDs are not reused. Saved contacts include IDs. |
| `edit 2 r/President`, then `delete 2` | Both report `No contact exists with member ID: 2. Use an existing member ID.` No confirmation; contacts/file unchanged. |

#### Invalid saved data

**Setup:** Close the app; copy `data/coordimate.json` with sample contacts and an event with members. Restore the
copy before each corruption. In a text editor, make one change, save, then launch:

* Delete the first `{`.
* Change an event's `members` entry to an unknown contact name.
* Add `"leaveUntil": "31-02-2030"` to a contact.

| Test case | Expected |
|---|---|
| Launch after each corruption | Warning says data cannot load, original file is unchanged, and CoordiMate will close; includes the `data/coordimate.json` path. |
| Select **OK**; inspect `data/coordimate.json` | App closes; corrupted contents remain, without replacement by an empty dataset. |
| Restore the copy and launch | No recovery warning; original contacts/events return. |

#### Failed save

**Setup:** App running; `data/coordimate.json` exists. On Windows, tick **Read-only** in its properties. On macOS/Linux,
run `chmod a-w data` in the home folder.

| Test case | Expected |
|---|---|
| `addevent evn/Test st/01-01-2027 et/01-01-2027` | No addition; Events unchanged. `Event could not be saved. No changes were made.` |
| `add n/Test Person p/91234567 e/testperson@example.com r/Member` | No addition. `Contact could not be saved. No changes were made.` |
| Untick **Read-only** or run `chmod u+w data`; repeat `addevent` | Event is added. |

#### Startup event cleanup

**Setup:** Restore sample data and run:

1. `addevent evn/Planning Meeting st/01-01-2027 et/01-01-2027`
1. `addevent evn/Planning Draft st/01-01-2027 et/01-01-2027`
1. `addevent evn/Legacy Parameter st/01-01-2027 et/01-01-2027`
1. `addevent evn/Legacy Date st/01-01-2027 et/01-01-2027`
1. `assign evn/Planning Draft c/1`
1. `markattendance evn/Planning Draft mem/Alex Yeoh att/absent`
1. `archiveevent evn/Planning Draft`

Close the app. In `data/coordimate.json`, keep all other fields unchanged and:

* Rename `Planning Draft` to `"Planning\u00a0Meeting"`, using the JSON escape for a non-breaking space.
  Set its `dismissedReminderStarts` to `["01-01-0000"]`.
* Rename `Legacy Parameter` to `"Legacy c/review"`.
* Change `Legacy Date`'s `startTime` and `endTime` to `"01-01-0000"`.

| Test case | Expected |
|---|---|
| Launch and acknowledge the startup warning | Warning reports the rename to `Planning Meeting (2)` and deletion of `Legacy c/review` and `Legacy Date`. The original `Planning Meeting` remains active; sample contacts and event are preserved. |
| `list archived`; select `Planning Meeting (2)` in Events; inspect the saved JSON | Event remains archived with its dates, Alex's membership, and `● absent` attendance preserved. Invalid reminder history is removed. The rename and deletions are saved. |
| Close and relaunch | No cleanup warning; migrated data remains unchanged. |

#### Concurrent-window save conflicts

**Setup:** Restore sample data; run `edit 1 r/President` to create the data file, then close the app.
Launch two instances, A and B, from the same JAR folder before running either test command.

| Test case | Expected |
|---|---|
| In A, run `addtag t/ConcurrentA`; then in B, run `addtag t/ConcurrentB` | A saves successfully. B rejects its change: `The data file changed in another window. Reopen this window before saving. No changes were made.` B's data stays unchanged; the file contains `ConcurrentA`, not `ConcurrentB`. |
| Close and reopen B; run `addtag t/ConcurrentB` | B loads `ConcurrentA` and saves `ConcurrentB`; the file contains both tags. Close both instances before continuing. |

#### Reminder dismissal when saving fails

**Setup:** Use data with no active reminders. Run `addevent evn/Unsaved Reminder st/TOMORROW et/TOMORROW`,
then block saving as described under [Failed save](#failed-save).

| Test case | Expected |
|---|---|
| `dismiss 1` | Reminder disappears with **Not saved** status: `Reminder dismissal could not be saved and may reappear after restarting.` The event and data file remain unchanged. |
| Restore write access; run `list` without restarting | Reminder stays hidden for this session; the event remains saved. |
| Close and relaunch while the event still qualifies | `Unsaved Reminder` reappears: its dismissal was not saved. |

Cleanup: `deleteevent evn/Unsaved Reminder`.
