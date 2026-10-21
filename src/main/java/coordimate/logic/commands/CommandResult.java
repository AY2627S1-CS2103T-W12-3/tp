package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import coordimate.commons.util.ToStringBuilder;

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

    /** The view requested by the command. */
    private final View viewToShow;

    private final boolean warning;

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
        this(feedbackToUser, shouldShowHelp, shouldExit, viewToShow, false);
    }

    private CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit,
            View viewToShow, boolean warning) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.shouldShowHelp = shouldShowHelp;
        this.shouldExit = shouldExit;
        this.viewToShow = requireNonNull(viewToShow);
        this.warning = warning;
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
        return new CommandResult(feedbackToUser, false, false, View.NONE, true);
    }

    public boolean isWarning() {
        return warning;
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
                && viewToShow == otherCommandResult.viewToShow
                && warning == otherCommandResult.warning;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, shouldShowHelp, shouldExit, viewToShow, warning);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", shouldShowHelp)
                .add("exit", shouldExit)
                .add("viewToShow", viewToShow)
                .add("warning", warning)
                .toString();
    }

}
