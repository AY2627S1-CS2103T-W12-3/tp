package coordimate.ui;

import java.time.LocalDate;

import coordimate.model.person.Address;
import coordimate.model.person.Person;
import coordimate.model.tag.Tag;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * A UI component that displays information of a {@code Person}.
 */
public class PersonCard extends UiPart<Region> {

    private static final String FXML = "PersonListCard.fxml";

    private final Person person;

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/coordimate-level4/issues/336">The issue on CoordiMate level 4</a>
     */
    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label phone;
    @FXML
    private Label role;
    @FXML
    private Label leave;
    @FXML
    private Label address;
    @FXML
    private Label archived;
    @FXML
    private Label email;
    @FXML
    private FlowPane tags;

    /**
     * Creates a {@code PersonCard} displaying the contact's permanent member ID.
     */
    public PersonCard(Person person) {
        this(person, LocalDate.now());
    }

    /**
     * Creates a {@code PersonCard} that shows the contact's leave as of {@code today}.
     */
    public PersonCard(Person person, LocalDate today) {
        super(FXML);
        this.person = person;
        id.setText("ID: " + person.getMemberId().orElseThrow());
        name.setText(person.getName().getFullName());
        phone.setText(person.getPhone().getValue());
        role.setText(person.getRole().getValue());
        boolean isOnLeave = person.isOnLeave(today);
        leave.setText(isOnLeave ? "On leave until " + person.getLeaveDate().orElseThrow() : "");
        leave.setVisible(isOnLeave);
        leave.setManaged(isOnLeave);
        boolean hasAddress = person.getAddress().isPresent();
        address.setText(person.getAddress().map(Address::getValue).orElse(""));
        address.setVisible(hasAddress);
        address.setManaged(hasAddress);
        email.setText(person.getEmail().getValue());
        archived.setVisible(person.isArchived());
        archived.setManaged(person.isArchived());
        person.getTags().stream()
                .sorted(Tag.ALPHABETICAL_ORDER)
                .forEach(tag -> tags.getChildren().add(TagStyle.createTagLabel(tag.getTagName())));
    }

    public Person getPerson() {
        return person;
    }
}
