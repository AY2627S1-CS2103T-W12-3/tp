package coordimate.testutil;

import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_EMAIL_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_AMY;
import static coordimate.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_FRIEND;
import static coordimate.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import coordimate.model.CoordiMate;
import coordimate.model.person.Person;

/**
 * A utility class containing a list of {@code Person} objects to be used in tests.
 */
public class TypicalPersons {

    public static final Person ALICE = new PersonBuilder().setName("Alice Pauline")
            .setAddress("123, Jurong West Ave 6, #08-111").setEmail("alice@example.com")
            .setPhone("94351253")
            .setTags("friends").build();
    public static final Person BENSON = new PersonBuilder().setName("Benson Meier")
            .setAddress("311, Clementi Ave 2, #02-25")
            .setEmail("johnd@example.com").setPhone("98765432")
            .setTags("owesMoney", "friends").build();
    public static final Person CARL = new PersonBuilder().setName("Carl Kurz").setPhone("95352563")
            .setEmail("heinz@example.com").setAddress("wall street").build();
    public static final Person DANIEL = new PersonBuilder().setName("Daniel Meier").setPhone("87652533")
            .setEmail("cornelia@example.com").setAddress("10th street").setTags("friends").build();
    public static final Person ELLE = new PersonBuilder().setName("Elle Meyer").setPhone("9482224")
            .setEmail("werner@example.com").setAddress("michegan ave").build();
    public static final Person FIONA = new PersonBuilder().setName("Fiona Kunz").setPhone("9482427")
            .setEmail("lydia@example.com").setAddress("little tokyo").build();
    public static final Person GEORGE = new PersonBuilder().setName("George Best").setPhone("9482442")
            .setEmail("anna@example.com").setAddress("4th street").build();

    // Manually added
    public static final Person HOON = new PersonBuilder().setName("Hoon Meier").setPhone("8482424")
            .setEmail("stefan@example.com").setAddress("little india").build();
    public static final Person IDA = new PersonBuilder().setName("Ida Mueller").setPhone("8482131")
            .setEmail("hans@example.com").setAddress("chicago ave").build();

    // Manually added - Person's details found in {@code CommandTestUtil}
    public static final Person AMY = new PersonBuilder().setName(VALID_NAME_AMY).setPhone(VALID_PHONE_AMY)
            .setEmail(VALID_EMAIL_AMY).setAddress(VALID_ADDRESS_AMY).setTags(VALID_TAG_FRIEND).build();
    public static final Person BOB = new PersonBuilder().setName(VALID_NAME_BOB).setPhone(VALID_PHONE_BOB)
            .setEmail(VALID_EMAIL_BOB).setAddress(VALID_ADDRESS_BOB).setTags(VALID_TAG_HUSBAND, VALID_TAG_FRIEND)
            .build();

    public static final String KEYWORD_MATCHING_MEIER = "Meier"; // A keyword that matches MEIER

    private TypicalPersons() {} // prevents instantiation

    /**
     * Returns an {@code CoordiMate} with all the typical persons.
     */
    public static CoordiMate getTypicalCoordiMate() {
        CoordiMate coordiMate = new CoordiMate();
        for (Person person : getTypicalPersons()) {
            coordiMate.addPerson(person);
        }
        return coordiMate;
    }

    public static List<Person> getTypicalPersons() {
        return new ArrayList<>(Arrays.asList(ALICE, BENSON, CARL, DANIEL, ELLE, FIONA, GEORGE));
    }
}
