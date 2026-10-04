package coordimate.testutil;

import coordimate.model.CoordiMate;
import coordimate.model.person.Person;

/**
 * A utility class to help with building CoordiMate objects.
 * Example usage: <br>
 *     {@code CoordiMate coordiMate = new CoordiMateBuilder().withPerson("John", "Doe").build();}
 */
public class CoordiMateBuilder {

    private CoordiMate coordiMate;

    public CoordiMateBuilder() {
        coordiMate = new CoordiMate();
    }

    public CoordiMateBuilder(CoordiMate coordiMate) {
        this.coordiMate = coordiMate;
    }

    /**
     * Adds a new {@code Person} to the {@code CoordiMate} that we are building.
     */
    public CoordiMateBuilder withPerson(Person person) {
        coordiMate.addPerson(person);
        return this;
    }

    public CoordiMate build() {
        return coordiMate;
    }
}
