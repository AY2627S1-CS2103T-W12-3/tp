package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.logging.Logger;

import coordimate.commons.core.LogsCenter;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.event.EventMembersPredicate;

/**
 * Filters the contact list to the members of the event identified by its name, ignoring case.
 * Only members in the current archive view are listed; the message says how many members the other view holds.
 */
public class MembersCommand extends Command {
    public static final String COMMAND_WORD = "members";
    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Lists the members of an event.\n"
            + "Parameters: evn/EVENT_NAME\n"
            + "Example: " + COMMAND_WORD + " evn/Final Concert";
    public static final String MESSAGE_SHOW_ALL_HINT = " Use list to show all contacts.";
    public static final String MESSAGE_SHOW_ARCHIVED_HINT = " Use list archived to show archived contacts.";
    public static final String MESSAGE_LISTED = "Listed %d member(s) of %s.";
    public static final String MESSAGE_SUCCESS = MESSAGE_LISTED + MESSAGE_SHOW_ALL_HINT;
    public static final String MESSAGE_NO_MEMBERS = "%s has no members assigned." + MESSAGE_SHOW_ALL_HINT;
    public static final String MESSAGE_NONE_SHOWN = "No %s members of %s are shown.";
    public static final String MESSAGE_HIDDEN_ARCHIVED = " %d member(s) are archived." + MESSAGE_SHOW_ARCHIVED_HINT;
    public static final String MESSAGE_HIDDEN_ACTIVE = " %d member(s) are active." + MESSAGE_SHOW_ALL_HINT;

    private static final Logger logger = LogsCenter.getLogger(MembersCommand.class);

    private final String eventName;

    /**
     * Creates a command that lists the members of the named event.
     */
    public MembersCommand(String eventName) {
        this.eventName = requireNonNull(eventName).strip();
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Event target = EventMembersCommand.findEvent(model, eventName);
        EventMembersPredicate predicate = new EventMembersPredicate(target.getName(),
                model.getCoordiMate().getEventList());
        model.updateFilteredPersonList(predicate);

        int shownCount = model.getFilteredPersonList().size();
        // Counted across all saved contacts, since the filtered list hides those outside the current archive view.
        int memberCount = (int) model.getCoordiMate().getPersonList().stream().filter(predicate).count();
        assert shownCount <= memberCount : "Shown members must be a subset of the event's members.";
        int hiddenCount = memberCount - shownCount;
        if (hiddenCount > 0) {
            logger.fine(hiddenCount + " member(s) of " + target.getName() + " are outside the current archive view");
        }
        return new CommandResult(buildMessage(model.isShowingArchived(), target.getName(), shownCount, hiddenCount),
                CommandResult.View.CONTACTS);
    }

    private static String buildMessage(boolean isShowingArchived, String name, int shownCount, int hiddenCount) {
        if (hiddenCount == 0) {
            return shownCount == 0
                    ? String.format(MESSAGE_NO_MEMBERS, name)
                    : String.format(MESSAGE_SUCCESS, shownCount, name);
        }
        String shown = shownCount == 0
                ? String.format(MESSAGE_NONE_SHOWN, isShowingArchived ? "archived" : "active", name)
                : String.format(MESSAGE_LISTED, shownCount, name);
        // Contacts hidden from the archived view are active, and those hidden from the normal view are archived.
        return shown + String.format(isShowingArchived ? MESSAGE_HIDDEN_ACTIVE : MESSAGE_HIDDEN_ARCHIVED,
                hiddenCount);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MembersCommand otherCommand && eventName.equals(otherCommand.eventName);
    }

    @Override
    public int hashCode() {
        return eventName.hashCode();
    }
}
