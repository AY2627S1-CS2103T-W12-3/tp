package coordimate.logic.commands;

import static java.util.Objects.requireNonNull;

import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.event.Event;
import coordimate.model.event.EventMembersPredicate;

/**
 * Filters the contact list to the members of the event identified by its name, ignoring case.
 */
public class MembersCommand extends Command {
    public static final String COMMAND_WORD = "members";
    public static final String MESSAGE_USAGE = "members evn/EVENT_NAME";
    public static final String MESSAGE_SUCCESS = "Listed %d member(s) of %s.";
    public static final String MESSAGE_NO_MEMBERS = "%s has no members assigned.";

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
        model.updateFilteredPersonList(new EventMembersPredicate(target.getName(),
                model.getCoordiMate().getEventList()));

        int memberCount = model.getFilteredPersonList().size();
        String message = memberCount == 0
                ? String.format(MESSAGE_NO_MEMBERS, target.getName())
                : String.format(MESSAGE_SUCCESS, memberCount, target.getName());
        return new CommandResult(message);
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
