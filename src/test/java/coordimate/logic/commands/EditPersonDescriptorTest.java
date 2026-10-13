package coordimate.logic.commands;

import static coordimate.logic.commands.CommandTestUtil.DESC_AMY;
import static coordimate.logic.commands.CommandTestUtil.DESC_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import coordimate.logic.commands.EditCommand.EditPersonDescriptor;
import coordimate.testutil.EditPersonDescriptorBuilder;

public class EditPersonDescriptorTest {

    @Test
    public void checkEquals() {
        // same values -> returns true
        EditPersonDescriptor descriptorWithSameValues = new EditPersonDescriptor(DESC_AMY);
        assertTrue(DESC_AMY.equals(descriptorWithSameValues));

        // same object -> returns true
        assertTrue(DESC_AMY.equals(DESC_AMY));

        // null -> returns false
        assertFalse(DESC_AMY.equals(null));

        // different types -> returns false
        assertFalse(DESC_AMY.equals(5));

        // different values -> returns false
        assertFalse(DESC_AMY.equals(DESC_BOB));

        // different name -> returns false
        EditPersonDescriptor editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setName(VALID_NAME_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different phone -> returns false
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setPhone(VALID_PHONE_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different email -> returns false
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setEmail(VALID_EMAIL_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different role or optional-field edit -> returns false
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setRole("Logistics").build();
        assertFalse(DESC_AMY.equals(editedAmy));
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setBirthday("18-06-2004").build();
        assertFalse(DESC_AMY.equals(editedAmy));
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setOrganisation("NUS Student Affairs").build();
        assertFalse(DESC_AMY.equals(editedAmy));
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setNote("Handles venue bookings").build();
        assertFalse(DESC_AMY.equals(editedAmy));
        assertFalse(new EditPersonDescriptor().equals(new EditPersonDescriptorBuilder().clearNote().build()));

        // different address -> returns false
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setAddress(VALID_ADDRESS_BOB).build();
        assertFalse(DESC_AMY.equals(editedAmy));

        // different tags -> returns false
        editedAmy = new EditPersonDescriptorBuilder(DESC_AMY).setTags(VALID_TAG_HUSBAND).build();
        assertFalse(DESC_AMY.equals(editedAmy));
    }

    @Test
    public void checkToStringMethod() {
        EditPersonDescriptor editPersonDescriptor = new EditPersonDescriptor();
        String expected = EditPersonDescriptor.class.getCanonicalName() + "{name="
                + editPersonDescriptor.getName().orElse(null) + ", phone="
                + editPersonDescriptor.getPhone().orElse(null) + ", email="
                + editPersonDescriptor.getEmail().orElse(null) + ", role="
                + editPersonDescriptor.getRole().orElse(null) + ", birthdayEdited="
                + editPersonDescriptor.isBirthdayEdited() + ", birthday="
                + editPersonDescriptor.getBirthday().orElse(null) + ", addressEdited="
                + editPersonDescriptor.isAddressEdited() + ", address="
                + editPersonDescriptor.getAddress().orElse(null) + ", organisationEdited="
                + editPersonDescriptor.isOrganisationEdited() + ", organisation="
                + editPersonDescriptor.getOrganisation().orElse(null) + ", noteEdited="
                + editPersonDescriptor.isNoteEdited() + ", note="
                + editPersonDescriptor.getNote().orElse(null) + ", tags="
                + editPersonDescriptor.getTags().orElse(null) + ", tagOperations=[]}";
        assertEquals(expected, editPersonDescriptor.toString());
    }
}
