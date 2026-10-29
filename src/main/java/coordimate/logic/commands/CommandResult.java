package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import coordimate.commons.util.ToStringBuilder;
import coordimate.model.person.Person;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    /** The view to display after a command, if any. */
    public enum View {
        NONE(""), CONTACTS("Contacts"), EVENTS("Events"), TAGS("Tags");

        private final String displayName;

        View(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean shouldShowHelp;

    /** The application should exit. */
    private final boolean shouldExit;

    /** The guided tutorial should be shown to the user. */
    private final boolean shouldShowTutorial;

    /** The view requested by the command. */
    private final View viewToShow;

    private final boolean isWarning;

    private final Person contactToFocus;

    private final int contactIndexToFocus;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit) {
        this(feedbackToUser, shouldShowHelp, shouldExit, View.NONE);
    }

    /**
     * Constructs a {@code CommandResult} that requests the specified view.
     */
    public CommandResult(String feedbackToUser, View viewToShow) {
        this(feedbackToUser, false, false, viewToShow);
    }

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit, View viewToShow) {
        this(feedbackToUser, shouldShowHelp, shouldExit, false, viewToShow, false, null, -1);
    }

    private CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit,
            boolean shouldShowTutorial, View viewToShow, boolean isWarning, Person contactToFocus,
            int contactIndexToFocus) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.shouldShowHelp = shouldShowHelp;
        this.shouldExit = shouldExit;
        this.shouldShowTutorial = shouldShowTutorial;
        this.viewToShow = requireNonNull(viewToShow);
        this.isWarning = isWarning;
        this.contactToFocus = contactToFocus;
        this.contactIndexToFocus = contactIndexToFocus;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
    }

    /** Reports an operation that took effect for this session but could not be saved. */
    public static CommandResult warning(String feedbackToUser) {
        return new CommandResult(feedbackToUser, false, false, false, View.NONE, true, null, -1);
    }

    /** Returns a command result that asks the UI to focus a saved contact. */
    public static CommandResult withContactToFocus(String feedbackToUser, View viewToShow, Person contact) {
        return new CommandResult(feedbackToUser, false, false, false, viewToShow, false, requireNonNull(contact),
                -1);
    }

    /** Returns a command result that asks the UI to focus the contact now at a removed contact's index. */
    public static CommandResult withContactIndexToFocus(String feedbackToUser, View viewToShow, int contactIndex) {
        if (contactIndex < 0) {
            throw new IllegalArgumentException("Contact index to focus must be non-negative.");
        }
        return new CommandResult(feedbackToUser, false, false, false, viewToShow, false, null, contactIndex);
    }

    /** Returns a command result that asks the UI to open the guided tutorial. */
    public static CommandResult showingTutorial(String feedbackToUser) {
        return new CommandResult(feedbackToUser, false, false, true, View.NONE, false, null, -1);
    }

    public boolean isWarning() {
        return isWarning;
    }

    public Optional<Person> getContactToFocus() {
        return Optional.ofNullable(contactToFocus);
    }

    /** Returns the zero-based displayed index to focus, if a deleted contact occupied that position. */
    public OptionalInt getContactIndexToFocus() {
        return contactIndexToFocus < 0 ? OptionalInt.empty() : OptionalInt.of(contactIndexToFocus);
    }

    public String getFeedbackToUser() {
        return feedbackToUser;
    }

    /**
     * Returns true if the command requests that help be shown.
     */
    public boolean shouldShowHelp() {
        return shouldShowHelp;
    }

    /**
     * Returns true if the command requests that the application exit.
     */
    public boolean shouldExit() {
        return shouldExit;
    }

    /**
     * Returns true if the command requests that the guided tutorial be shown.
     */
    public boolean shouldShowTutorial() {
        return shouldShowTutorial;
    }

    /**
     * Returns the view requested by the command, or {@code View.NONE} if it does not change views.
     */
    public View getViewToShow() {
        return viewToShow;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof CommandResult otherCommandResult)) {
            return false;
        }

        return feedbackToUser.equals(otherCommandResult.feedbackToUser)
                && shouldShowHelp == otherCommandResult.shouldShowHelp
                && shouldExit == otherCommandResult.shouldExit
                && shouldShowTutorial == otherCommandResult.shouldShowTutorial
                && viewToShow == otherCommandResult.viewToShow
                && isWarning == otherCommandResult.isWarning
                && Objects.equals(contactToFocus, otherCommandResult.contactToFocus)
                && contactIndexToFocus == otherCommandResult.contactIndexToFocus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, shouldShowHelp, shouldExit, shouldShowTutorial, viewToShow, isWarning,
                contactToFocus, contactIndexToFocus);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", shouldShowHelp)
                .add("exit", shouldExit)
                .add("showTutorial", shouldShowTutorial)
                .add("viewToShow", viewToShow)
                .add("isWarning", isWarning)
                .add("contactToFocus", contactToFocus)
                .add("contactIndexToFocus", contactIndexToFocus)
                .toString();
    }

}
