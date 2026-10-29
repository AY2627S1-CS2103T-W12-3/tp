package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.commons.core.index.Index;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.logic.reminder.ReminderDismissalException;
import coordimate.logic.reminder.ReminderService;
import coordimate.model.Model;
import coordimate.model.event.Event;

/**
 * Dismisses a reminder identified by its position in the upcoming events panel.
 */
public class DismissCommand extends Command {
    public static final String COMMAND_WORD = "dismiss";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Dismisses an upcoming event reminder.\n"
            + "Parameters: INDEX (a positive integer from the Upcoming events panel)\n"
            + "Example: " + COMMAND_WORD + " 1";
    public static final String MESSAGE_SUCCESS = "Reminder dismissed: %s.";
    public static final String MESSAGE_INVALID_INDEX =
            "No reminder exists at index %d. Use an index from the Upcoming events panel.";
    public static final String MESSAGE_REMINDER_ENDED = "Reminder %d is no longer upcoming, so it was not "
            + "dismissed. Check the updated Upcoming events panel.";

    private final Logger logger = LogsCenter.getLogger(DismissCommand.class);
    private final Index targetIndex;
    private final ReminderService reminderService;

    /**
     * Uses the shared reminder service so CLI and button dismissals have the same persistence behavior.
     */
    public DismissCommand(Index targetIndex, ReminderService reminderService) {
        this.targetIndex = requireNonNull(targetIndex);
        this.reminderService = requireNonNull(reminderService);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        // Resolve against the panel as shown; refreshing first could shift indexes to another reminder.
        List<Event> shownReminders = reminderService.getShownReminders();
        if (targetIndex.getZeroBased() >= shownReminders.size()) {
            throw new CommandException(String.format(MESSAGE_INVALID_INDEX, targetIndex.getOneBased()));
        }
        Event reminder = shownReminders.get(targetIndex.getZeroBased());
        if (!reminderService.getReminders().contains(reminder)) {
            throw new CommandException(String.format(MESSAGE_REMINDER_ENDED, targetIndex.getOneBased()));
        }
        try {
            reminderService.dismissReminder(reminder);
        } catch (ReminderDismissalException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return CommandResult.warning(e.getMessage());
        }
        return new CommandResult(String.format(MESSAGE_SUCCESS, reminder.getName()));
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof DismissCommand command && targetIndex.equals(command.targetIndex);
    }

    @Override
    public int hashCode() {
        return targetIndex.hashCode();
    }
}
