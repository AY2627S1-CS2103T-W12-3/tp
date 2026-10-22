package coordimate.model;

import static coordimate.testutil.TypicalPersons.ALICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.EditCommand;
import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.model.person.MemberId;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import coordimate.testutil.PersonBuilder;

public class MemberIdLifecycleTest {
    @Test
    public void edit_allContactDetails_preservesId() throws Exception {
        Person member = new PersonBuilder(ALICE).withMemberId(12).build();
        CoordiMate data = new CoordiMate();
        data.addPerson(member);
        Model model = new ModelManager(data, new UserPrefs());
        EditPersonDescriptor descriptor = new EditPersonDescriptor();
        Person replacement = new PersonBuilder().withRole("President").withBirthday("18-06-2004")
                .withOrganisation("NUS").withNote("Chair").withTags("EXCO").build();
        descriptor.setName(replacement.getName());
        descriptor.setPhone(replacement.getPhone());
        descriptor.setEmail(replacement.getEmail());
        descriptor.setRole(replacement.getRole());
        descriptor.setBirthday(replacement.getBirthday().orElseThrow());
        descriptor.setAddress(replacement.getAddress().orElseThrow());
        descriptor.setOrganisation(replacement.getOrganisation().orElseThrow());
        descriptor.setNote(replacement.getNote().orElseThrow());
        descriptor.setTags(replacement.getTags());

        new EditCommand(new MemberId(12), descriptor).execute(model);

        assertEquals(replacement.withMemberId(new MemberId(12)), model.getFilteredPersonList().getFirst());
    }

    @Test
    public void renameAndRemoveTags_preserveId() {
        Person member = new PersonBuilder(ALICE).withMemberId(12).withTags("friends").build();
        CoordiMate data = new CoordiMate();
        data.addPerson(member);
        data.setTag(new Tag("friends"), new Tag("Committee"));
        assertEquals(new MemberId(12), data.getPersonList().getFirst().getMemberId().orElseThrow());
        assertEquals(List.of(new Tag("Committee")), data.getPersonList().getFirst().getTags().stream().toList());

        data.removeTag(new Tag("Committee"));
        assertEquals(new MemberId(12), data.getPersonList().getFirst().getMemberId().orElseThrow());
        assertEquals(0, data.getPersonList().getFirst().getTags().size());
    }

    @Test
    public void copyAndReset_preserveId() {
        CoordiMate data = new CoordiMate();
        data.addPerson(new PersonBuilder(ALICE).withMemberId(12).build());
        CoordiMate copy = new CoordiMate(data);
        CoordiMate reset = new CoordiMate();
        reset.resetData(copy);
        assertEquals(data, copy);
        assertEquals(data, reset);
        assertEquals(new MemberId(12), reset.getPersonList().getFirst().getMemberId().orElseThrow());
    }
}
