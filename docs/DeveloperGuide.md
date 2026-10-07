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

* _{This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).}_

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

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W12-3/tp/tree/master/src/main/java/coordimate/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data. The Contacts view
  displays the filtered contact list beside a details pane and clears the selection when the selected contact
  leaves that list.
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

### Common classes

Classes used by multiple components are in the `coordimate.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedCoordiMate`. It extends `CoordiMate` with an undo/redo history, stored internally as a `coordiMateStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedCoordiMate#commit()` -- Saves the current CoordiMate state in its history.
* `VersionedCoordiMate#undo()` -- Restores the previous CoordiMate state from its history.
* `VersionedCoordiMate#redo()` -- Restores a previously undone CoordiMate state from its history.

These operations are exposed in the `Model` interface as `Model#commitCoordiMate()`, `Model#undoCoordiMate()` and `Model#redoCoordiMate()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedCoordiMate` will be initialized with the initial CoordiMate state, and the `currentStatePointer` pointing to that single state.

<puml src="diagrams/UndoRedoState0.puml" alt="UndoRedoState0" />

Step 2. The user executes the `delete 5` command to delete the 5th person in CoordiMate. The `delete` command calls `Model#commitCoordiMate()`, causing the modified CoordiMate state to be saved in the `coordiMateStateList`, and the `currentStatePointer` is shifted to the newly inserted state.

<puml src="diagrams/UndoRedoState1.puml" alt="UndoRedoState1" />

Step 3. The user executes `add n/David …​` to add a new person. The `add` command also calls `Model#commitCoordiMate()`, causing another modified CoordiMate state to be saved into the `coordiMateStateList`.

<puml src="diagrams/UndoRedoState2.puml" alt="UndoRedoState2" />

<box type="info" seamless>

**Note:** If a command fails its execution, it will not call `Model#commitCoordiMate()`, so the CoordiMate state will not be saved into the `coordiMateStateList`.
</box>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoCoordiMate()`, which shifts the `currentStatePointer` once to the left and restores the previous CoordiMate state.

<puml src="diagrams/UndoRedoState3.puml" alt="UndoRedoState3" />


<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index 0, pointing to the initial CoordiMate state, then there are no previous states to restore. The `undo` command uses `Model#canUndoCoordiMate()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.
</box>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

<puml src="diagrams/UndoSequenceDiagram-Logic.puml" alt="UndoSequenceDiagram-Logic" />

<box type="info" seamless>

**Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</box>

Similarly, how an undo operation goes through the `Model` component is shown below:

<puml src="diagrams/UndoSequenceDiagram-Model.puml" alt="UndoSequenceDiagram-Model" />

The `redo` command does the opposite — it calls `Model#redoCoordiMate()`, which shifts the `currentStatePointer` once to the right and restores the previously undone CoordiMate state.

<box type="info" seamless>

**Note:** If the `currentStatePointer` is at index `coordiMateStateList.size() - 1`, pointing to the latest CoordiMate state, then there are no undone states to restore. The `redo` command uses `Model#canRedoCoordiMate()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.
</box>

Step 5. The user then decides to execute the command `list`. Commands that do not modify CoordiMate data, such as `list`, will usually not call `Model#commitCoordiMate()`, `Model#undoCoordiMate()` or `Model#redoCoordiMate()`. Thus, the `coordiMateStateList` remains unchanged.

<puml src="diagrams/UndoRedoState4.puml" alt="UndoRedoState4" />

Step 6. The user executes `clear`, which calls `Model#commitCoordiMate()`. Since the `currentStatePointer` is not pointing at the end of the `coordiMateStateList`, all states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …` command. This is the behavior that most modern desktop applications follow.

<puml src="diagrams/UndoRedoState5.puml" alt="UndoRedoState5" />

The following activity diagram summarizes what happens when a user executes a new command:

<puml src="diagrams/CommitActivityDiagram.puml" width="250" />

#### Design considerations:

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire CoordiMate state.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_


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

| Priority | As a … | I want to … | So that I can … |
|----------|--------|-------------|-----------------|
| `* * *` | first-time user | view a tutorial | learn how to navigate CoordiMate |
| `* *` | returning user | access the tutorial again | refresh my memory when I forget how to use CoordiMate |
| `* * *` | Exco member | save a contact's details | keep track of the people I know |
| `* * *` | Exco member | edit a saved contact | correct or update the contact's information |
| `* * *` | Exco member | delete a contact | remove an entry that is no longer needed |
| `* * *` | Exco member | save a contact's email address | keep a complete record of the contact's information |
| `* *` | Exco member | record additional details about a contact | keep relevant background information at hand |
| `* *` | Exco member | record a contact's birthday | remember to wish the contact well |
| `*` | Exco member | attach multiple phone numbers or email addresses to a contact | record the contact's different personal and work details |
| `*` | Exco member | attach a photo to a contact | associate the contact's face with their name |
| `*` | Exco member entering data quickly | enter phone numbers containing extra whitespace or dashes | avoid manually reformatting phone numbers |
| `*` | Exco member entering data quickly | enter Telegram handles with or without the `@` symbol | avoid manually reformatting handles |
| `* * *` | event lead | search and filter contacts by name, tag, or role | retrieve specific contact information quickly |
| `* *` | Exco member | filter contacts by their assigned colour | find contacts in a particular group quickly |
| `*` | Exco member | have urgent contacts appear at the top of the contact list | identify them quickly |
| `* *` | experienced user | colour-code contacts by role or group | identify their roles or groups quickly |
| `* *` | Exco member | assign a colour to a contact | distinguish that contact visually |
| `*` | Exco member | choose from a set of default colours | assign colours without configuring them myself |
| `*` | Exco member | change a contact's assigned colour | keep the colour accurate when the contact's role or group changes |
| `* * *` | Exco member | assign tags to contacts | categorize contacts for later retrieval |
| `* *` | Exco member | choose from a set of common default tags | categorize contacts quickly |
| `* *` | Exco member | create custom tags | categorize contacts in ways not covered by the default tags |
| `* * *` | Exco member | create an event with a name and date | keep a record around which I can organize contacts and attendance |
| `* * *` | Exco member | assign a member to an event | track who is involved in that event |
| `* * *` | Exco member | remove a member from an event | correct the assignment when someone is no longer involved |
| `* * *` | Exco member | view the members assigned to an event | see the committee allocation at a glance |
| `* *` | Exco member | create future events and assign members to them | plan committee allocation ahead of time |
| `*` | user with limited time | have the most frequently used event appear first | access it without extra navigation |
| `*` | forgetful Exco member | receive reminders for upcoming events | avoid missing them |
| `*` | forgetful Exco member | see everyone assigned to an event | avoid overlooking a participant |
| `* * *` | Exco member who organizes events | track attendance for an event | monitor participation |
| `* * *` | Exco member | mark a member as present or absent for an event | keep an accurate attendance record |
| `* *` | Exco member | view the counts of present and absent members for an event | gauge overall turnout quickly |
| `* *` | Exco member | view the members marked absent for an event | follow up with them individually |
| `*` | Exco member | view the edit history of attendance records | see when a member's status was changed and avoid disputes |
| `*` | Exco member | export attendance records | use them for reporting or record-keeping |
| `*` | Exco member | archive contacts | declutter my active contact list without permanently deleting entries |
| `*` | Exco member | archive past events | keep my event list focused on current and upcoming events |
| `*` | Exco member | access archived contacts and events | retrieve historical information when needed |
| `*` | Exco member | mark a contact as on leave until a specified date | remember that the contact is temporarily unavailable without manually clearing the status later |
| `* *` | Exco member | switch between the Contacts and Events views | navigate between contact and event information easily |
| `*` | user who values a pleasant interface | switch between light and dark modes | adjust the display to suit my environment |

### Use cases

(For all use cases below, the **System** is the `Coordimate` and the **Actor** is the `user`, unless specified otherwise)

**Use case: Add a contact**

**System:** CoordiMate

**Actor:** University CCA EXCO member

**Preconditions:** CoordiMate is running.

**Main success scenario (MSS)**

1. EXCO member requests to add a contact, providing the contact's name, phone number, email address, role, and any applicable optional details.
2. CoordiMate adds the contact and confirms that the contact was saved.

    Use case ends.

**Extensions**

* 1a. A required detail is missing, empty, repeated, or invalid, or the EXCO member provides an unknown field.

  * 1a1. CoordiMate informs the EXCO member of the error.
  * 1a2. EXCO member corrects the contact details.

    Steps 1a1-1a2 are repeated until the contact details are valid.<br>
    Use case resumes from step 2.

* 1b. A contact with the same normalised phone number or email address already exists.

  * 1b1. CoordiMate informs the EXCO member that the contact already exists and makes no changes.

    Use case ends.

* 2a. CoordiMate cannot write to the local data file.

  * 2a1. CoordiMate informs the EXCO member that the contact could not be saved and makes no changes.

    Use case ends.

**Use case: Find contacts**

**System:** CoordiMate

**Actor:** Event lead

**Preconditions:** CoordiMate has been launched and contains at least one contact.

**MSS**

1. Event lead requests to find contacts using one or more names, tags, or roles.
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
4. CoordiMate deletes the contact and confirms the deletion.

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

  * 3a1. CoordiMate leaves the contact unchanged.

    Use case ends.

* 4a. The contact is assigned to a future event.

  * 4a1. CoordiMate removes the contact from the event's active participant list and retains existing attendance records.

    Use case ends.

**Use case: Create an event**

**Preconditions:** CoordiMate is running.

**MSS**

1. The Exco member creates an event by entering its name, start date/time, and end date/time using the `addevent` command.
2. CoordiMate validates the details and creates the event.
3. CoordiMate displays a confirmation with the event name and its start and end times.
4. The use case ends.

**Extensions**

* 1a. The event name is empty, a date/time is missing or incorrectly formatted, or a required parameter is repeated.
  * 1a1. CoordiMate displays the relevant error.
  * 1a2. The Exco member corrects the details and retries step 1.

* 1b. An event with that name already exists.
  * 1b1. CoordiMate reports that duplicate event names are not allowed.
  * 1b2. The Exco member chooses a different name and retries step 1.

* 2a. CoordiMate cannot save the event because the local data file cannot be written or is invalid.
  * 2a1. CoordiMate displays the relevant error and does not create the event.
  * The use case ends.

**Use case: Assign members to an event**

**Preconditions:** CoordiMate is running. The event and the contacts to be assigned already exist.

**MSS**

1. The Exco member assigns one or more contacts to an event using the `assign` command, specifying the event name and the displayed indexes of the contacts.
2. CoordiMate verifies the event and contacts, then associates the contacts with the event.
3. CoordiMate confirms how many members were assigned.
4. The use case ends.

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
  * The use case ends.

* 2c. CoordiMate cannot save the assignment to the local data file.
  * 2c1. CoordiMate displays the relevant error and assigns no contacts.
  * The use case ends.

**Use case: Record event attendance**

**Preconditions:** CoordiMate is running. The event exists and has at least one member assigned to it.

**MSS**

1. The Exco member requests to view the list of members assigned to an event.
2. CoordiMate displays the list of members for that event.
3. The Exco member marks a member's attendance status as present or absent using the `markattendance` command.
4. CoordiMate records the attendance status and confirms the update.
5. Steps 3-4 are repeated for each member whose attendance is to be recorded.
6. The use case ends.

**Extensions**

* 1a. The event does not exist.
  * 1a1. CoordiMate displays the relevant error.
  * The use case ends.

* 2a. No members are assigned to the event.
  * 2a1. CoordiMate informs the Exco member that the event has no members.
  * The use case ends.

* 3a. The specified member is not assigned to the event.
  * 3a1. CoordiMate displays the relevant error.
  * The Exco member retries step 3.

* 3b. The attendance status given is neither "present" nor "absent".
  * 3b1. CoordiMate displays an error listing the valid status values.
  * The Exco member retries step 3.

* 3c. The member already has the specified attendance status recorded.
  * 3c1. CoordiMate informs the Exco member that no change was made.
  * The use case resumes from step 5.

* 4a. CoordiMate cannot save the updated attendance record to the local data file.
  * 4a1. CoordiMate displays the relevant error and does not update the displayed status.
  * The use case resumes from step 5.

*{More to be added}*

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
* **Duplicate contact**: A contact with the same normalised phone number or email address as another saved contact. Contacts with the same name are not considered duplicates.
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

1. _{ more test cases … }_

### Deleting a person

1. Deleting a person while all persons are being shown

   1. Prerequisites: List all persons using the `list` command, with multiple persons in the list.

   1. Test case: `delete 1`<br>
      Expected: The first contact is deleted from the list. The status message shows the deleted contact's details.

   1. Test case: `delete 0`<br>
      Expected: No person is deleted. The status message shows error details.

   1. Other incorrect delete commands to try: `delete`, `delete x`, `...` (where x is larger than the list size)<br>
      Expected: Similar to previous.

1. _{ more test cases … }_

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

   1. Prerequisites: The `Final Concert` event exists. Use `find` so that the displayed list starts with a person who is not first in the full list.

   1. Test case: `assign evn/Final Concert c/1`<br>
      Expected: The 1st person in the filtered list, not the full list, is added to the event's members.

1. Keeping members in sync with contacts

   1. Prerequisites: The `Final Concert` event has at least one member.

   1. Test case: `edit` a member to change their name, using `n/`.<br>
      Expected: The event's members show the new name in the same position.

   1. Test case: `delete` a member.<br>
      Expected: The person is removed from the event's members. Closing and relaunching the app keeps the data.

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

1. _{ more test cases … }_
