package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import coordimate.commons.util.ToStringBuilder;

/**
 * Represents the result of a command execution.
 */
public class CommandResult {

    private final String feedbackToUser;

    /** Help information should be shown to the user. */
    private final boolean shouldShowHelp;

    /** The application should exit. */
    private final boolean shouldExit;

    /** The tags view should be shown. */
    private final boolean shouldShowTags;

    /** The contacts view should be shown. */
    private final boolean shouldShowContacts;

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit) {
        this(feedbackToUser, shouldShowHelp, shouldExit, false);
    }

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit, boolean shouldShowTags) {
        this(feedbackToUser, shouldShowHelp, shouldExit, shouldShowTags, false);
    }

    /**
     * Constructs a {@code CommandResult} with the specified fields.
     */
    public CommandResult(String feedbackToUser, boolean shouldShowHelp, boolean shouldExit, boolean shouldShowTags,
            boolean shouldShowContacts) {
        this.feedbackToUser = requireNonNull(feedbackToUser);
        this.shouldShowHelp = shouldShowHelp;
        this.shouldExit = shouldExit;
        this.shouldShowTags = shouldShowTags;
        this.shouldShowContacts = shouldShowContacts;
    }

    /**
     * Constructs a {@code CommandResult} with the specified {@code feedbackToUser},
     * and other fields set to their default value.
     */
    public CommandResult(String feedbackToUser) {
        this(feedbackToUser, false, false);
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
     * Returns true if the command requests that the tags view be shown.
     */
    public boolean shouldShowTags() {
        return shouldShowTags;
    }

    /**
     * Returns true if the command requests that the contacts view be shown.
     */
    public boolean shouldShowContacts() {
        return shouldShowContacts;
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
                && shouldShowTags == otherCommandResult.shouldShowTags
                && shouldShowContacts == otherCommandResult.shouldShowContacts;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedbackToUser, shouldShowHelp, shouldExit, shouldShowTags, shouldShowContacts);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("feedbackToUser", feedbackToUser)
                .add("showHelp", shouldShowHelp)
                .add("exit", shouldExit)
                .add("showTags", shouldShowTags)
                .add("showContacts", shouldShowContacts)
                .toString();
    }

}
