package coordimate.logic;

import coordimate.commons.core.GuiSettings;
import coordimate.logic.commands.CommandResult;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.parser.exceptions.ParseException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.model.event.Event;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.ObservableList;

/**
 * API of the Logic component.
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     *
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /** Returns whether the next input will answer a contact deletion prompt. */
    boolean isAwaitingDeleteConfirmation();

    /**
     * Returns an unmodifiable view of the filtered list of persons.
     */
    ObservableList<Person> getFilteredPersonList();

    /** Returns the live, unmodifiable list of all saved persons. */
    ObservableList<Person> getPersonList();

    /**
     * Returns the live, unmodifiable event list in insertion order.
     */
    ObservableList<Event> getEventList();

    /**
     * Returns the live, unmodifiable list of events in the current archive view, in insertion order.
     */
    ObservableList<Event> getFilteredEventList();

    /**
     * Returns an observable value that is true while archived contacts and events are shown.
     */
    ReadOnlyBooleanProperty showingArchivedProperty();

    /**
     * Returns the live, unmodifiable list of active reminders in chronological order.
     * Refreshes it against the current time and saved model data.
     */
    ObservableList<Event> getReminders();

    /**
     * Refreshes reminder eligibility after time passes. Does not access storage.
     */
    void refreshReminders();

    /**
     * Saves dismissal of an active reminder. Outdated or already dismissed reminders have no effect.
     *
     * @throws ReminderDismissalException If the dismissal is only retained for this session.
     */
    void dismissReminder(Event reminder) throws ReminderDismissalException;

    /**
     * Returns the live, unmodifiable list of saved tags.
     */
    ObservableList<Tag> getTagList();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /** Returns whether the first-launch tutorial has already been shown. */
    boolean hasSeenTutorial();

    /** Records whether the first-launch tutorial has been shown. */
    void setHasSeenTutorial(boolean hasSeenTutorial);
}
