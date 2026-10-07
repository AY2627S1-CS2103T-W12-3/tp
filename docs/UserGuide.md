---
  layout: default.md
  title: "User Guide"
  pageNav: 3
---

# CoordiMate User Guide

**CoordiMate** is a desktop application that helps university CCA EXCO members organise committee information and coordinate their work. It lets users manage contacts and tags, plan events, assign members, and track attendance through a Command Line Interface (CLI) while retaining the benefits of a Graphical User Interface (GUI).

<!-- * Table of Contents -->
<page-nav-print />

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/AY2627S1-CS2103T-W12-3/tp/releases).

1. Copy the file to the folder you want to use as the _home folder_ for CoordiMate.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar coordimate.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`help`** and press Enter to open the help window.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01` : Adds a contact named `John Doe` to CoordiMate.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `clear` : Deletes all contacts.

   * `exit` : Exits the app.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<box type="info" seamless>

**Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `...` can appear zero or more times.<br>
  For example, `[t/TAG]... ` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous parameters for commands that take no parameters, such as `help`, `list`, `exit`, and `clear`, are ignored.<br>
  For example, `help 123` is interpreted as `help`.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</box>

### Viewing help: `help`

Shows a message explaining how to access the help page.

![help message](images/helpMessage.png)

Format: `help`


### Adding a person: `add`

Adds a person to CoordiMate.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]... `

<box type="tip" seamless>

**Tip:** A person can have any number of tags, including zero.
</box>

Examples:
* `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01`
* `add n/Betsy Crowe t/friend e/betsycrowe@example.com a/Newgate Prison p/1234567 t/criminal`

### Adding an event: `addevent`

Creates an event and saves it to the local data file.

Format: `addevent evn/EVENT_NAME st/START_TIME et/END_TIME`

Each parameter must appear once. Event names must contain a non-whitespace character;
leading and trailing whitespace is removed. Names must be unique, ignoring case.
Different events may share the same start and end times.

Start and end times use `dd-MM-yyyy`, optionally followed by `HH:mm` in 24-hour format.
Date-only values are saved without a time of day. The `/et` spelling from the examples
is also accepted as an alternative to `et/`.

The start must not be after the end. Equal start and end values are allowed.
Dates are compared first. On the same date, times are compared only when both are
specified. Invalid ordering reports: `Start time must not be after end time.`

Examples:

* `addevent evn/Final Concert st/08-08-2026 15:00 et/08-08-2026 18:00`
* `addevent evn/Student Life Fair st/09-10-2026 /et 10-10-2026`

Success: `Created Event Student Life Fair. Start Time: 09-10-2026. End Time: 10-10-2026.`

Use the **Events** tab on the left to browse saved events. Selecting an event shows its
start time, end time, and duration. A successful `addevent` opens this tab and selects
the new event. At narrower window sizes, the list appears above the details.

Command feedback appears above the input at the bottom of the window. Rejected commands
show a red feedback band and input border. The command stays in the input so you can
correct it, then press Enter or click **Retry**.

If saving fails, no event is added. If the existing data file is invalid, the command
reports a load error and leaves the file untouched.

### Editing an event: `editevent`

Edits the name, start time, and/or end time of an existing event and saves the changes.

Format: `editevent evn/EVENT_NAME [nevn/NEW_EVENT_NAME] [st/NEW_START_TIME] [et/NEW_END_TIME]`

`EVENT_NAME` must match the saved name, ignoring case. Leading and trailing whitespace
is removed. Lookup capitalization does not change the saved display name; `nevn/`
sets the new display name using the supplied capitalization.
Provide at least one field to edit; omitted fields
keep their existing values. Each parameter may appear only once.

The new name must contain a non-whitespace character and must not duplicate another
event's name, ignoring case. Start and end times use `dd-MM-yyyy [HH:mm]`; a date is
required and a 24-hour time is optional. The updated start must not be after the
updated end, using the same ordering rules as `addevent`.

Examples:

* `editevent evn/Final Concert st/08-08-2026 16:00 et/08-08-2026 19:00`
* `editevent evn/Student Life Fair nevn/NUS Student Life Fair et/10-10-2026`
* `editevent evn/CCA MEETING nevn/CCA MEETING` changes the display name of `CCA Meeting` to `CCA MEETING`.

Success: `Edited Event NUS Student Life Fair. Start Time: 09-10-2026. End Time: 10-10-2026.`

| Condition | Error message |
|---|---|
| No saved event has the supplied name, ignoring case. | `Event {EVENT_NAME} does not exist.` |
| The new event name is empty. | `New event name must not be empty.` |
| A new time is empty, invalid, or incorrectly formatted. | `New start time and/or new end time are formatted as dd-MM-yyyy [HH:mm]. Time of day is optional. Example: 17-09-2026 16:30` |
| The new name duplicates another event. | `This update conflicts with another saved event. No changes were made.` |
| No field is provided to edit. | `Please provide at least one field to edit.` |
| A parameter is repeated. | `Each parameter may only be specified once.` |
| An unknown parameter is used. | `Unknown parameter. Example: edit 2 r/Logistics` |
| The updated start is after the updated end. | `Start time must not be after end time.` |

If saving fails, the event and existing data file remain unchanged.

### Deleting an event: `deleteevent`

Deletes the event with the supplied name, ignoring capitalization. Internal spacing
must match the existing event name.

Format: `deleteevent evn/EVENT_NAME`

Examples:

* `deleteevent evn/Final Concert`
* `deleteevent evn/Student Life Fair`

Success: `Deleted Event Final Concert.`

If the event does not exist, the command reports `Event {EVENT_NAME} does not exist.`
An unknown parameter reports `Unknown parameter. Example: deleteevent evn/Logistics Meeting`.
If saving fails, the event and existing data file remain unchanged.

### Listing all persons: `list`

Shows a list of all persons in CoordiMate.

Format: `list`

### Editing a person: `edit`

Edits an existing person in CoordiMate.

Format: `edit INDEX [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]... `

* Edits the person at the specified `INDEX`. The index refers to the index number shown in the displayed person list. The index **must be a positive integer** 1, 2, 3, ...
* At least one of the optional fields must be provided.
* Existing values will be updated to the input values.
* When editing tags, all of the person's existing tags are removed; adding tags is not cumulative.
* To remove all of a person's tags, enter `t/` without a tag after it.

Examples:
*  `edit 1 p/91234567 e/johndoe@example.com` Edits the phone number and email address of the 1st person to be `91234567` and `johndoe@example.com` respectively.
*  `edit 2 n/Betsy Crower t/` Edits the name of the 2nd person to be `Betsy Crower` and clears all existing tags.

### Locating persons by name: `find`

Finds persons whose names contain any of the given keywords.

Format: `find KEYWORD [MORE_KEYWORDS]`

* The search is case-insensitive; for example, `hans` matches `Hans`.
* Keyword order does not matter; for example, `Hans Bo` matches `Bo Hans`.
* The search considers only names.
* Only full words match; for example, `Han` does not match `Hans`.
* Persons matching at least one keyword are returned (an `OR` search); for example, `Hans Bo` returns `Hans Gruber` and `Bo Yang`.

Examples:
* `find John` returns `john` and `John Doe`
* `find alex david` returns `Alex Yeoh`, `David Li`<br>
  ![result for 'find alex david'](images/findAlexDavidResult.png)

### Deleting a person: `delete`

Deletes the specified person from CoordiMate.

Format: `delete INDEX`

* Deletes the person at the specified `INDEX`.
* The index refers to the index number shown in the displayed person list.
* The index **must be a positive integer** 1, 2, 3, ...

Examples:
* `list` followed by `delete 2` deletes the 2nd person in CoordiMate.
* `find Betsy` followed by `delete 1` deletes the 1st person in the results of the `find` command.

### Clearing all entries: `clear`

Clears all entries from CoordiMate.

Format: `clear`

### Exiting the program: `exit`

Exits the program.

Format: `exit`

### Saving the data

CoordiMate automatically saves data after every command. You do not need to save manually.

### Editing the data file

CoordiMate data is saved automatically as a JSON file `[JAR file location]/data/coordimate.json`. Advanced users are welcome to update data directly by editing that data file.

<box type="warning" seamless>

**Caution:**
If your changes make the data file invalid, CoordiMate starts with an empty dataset at the next run. The invalid file remains on disk until you run a command (CoordiMate saves after every command). Still, we recommend backing up the file before editing it.<br>
Furthermore, certain edits can cause CoordiMate to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</box>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous CoordiMate home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then run the `help` command (or use the `Help` menu, or the keyboard shortcut `F1`) again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action     | Format, Examples
-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------
**Add**    | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]... ` <br> e.g., `add n/James Ho p/22224444 e/jamesho@example.com a/123, Clementi Rd, 1234665 t/friend t/colleague`
**Clear**  | `clear`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**Edit**   | `edit INDEX [n/NAME] [p/PHONE_NUMBER] [e/EMAIL] [a/ADDRESS] [t/TAG]... `<br> e.g.,`edit 2 n/James Lee e/jameslee@example.com`
**Find**   | `find KEYWORD [MORE_KEYWORDS]`<br> e.g., `find James Jake`
**List**   | `list`
**Help**   | `help`
