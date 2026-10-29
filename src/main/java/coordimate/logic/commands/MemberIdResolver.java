package coordimate.logic.commands;

import coordimate.logic.Messages;
import coordimate.logic.commands.exceptions.CommandException;
import coordimate.model.Model;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;

/**
 * Resolves permanent IDs across all saved contacts, including contacts hidden by the current view.
 */
final class MemberIdResolver {
    private MemberIdResolver() {
    }

    /**
     * Returns the saved contact with the permanent ID, or throws if no contact has that ID.
     */
    static Person resolve(Model model, MemberId memberId) throws CommandException {
        return model.getCoordiMate().getPersonList().stream()
                .filter(person -> person.getMemberId().orElseThrow().equals(memberId))
                .findFirst().orElseThrow(() -> new CommandException(
                        String.format(Messages.MESSAGE_MEMBER_ID_NOT_FOUND, memberId.getValue())));
    }
}
