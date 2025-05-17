package application;

/**
 * Data model representing a person in the attendance system.
 */
public class Person {
    private final int personId;
    private final String personCode;
    private final String fullName;
    private final String department;
    private final String position;

    public Person(int personId, String personCode, String fullName, String department, String position) {
        this.personId = personId;
        this.personCode = personCode;
        this.fullName = fullName;
        this.department = department;
        this.position = position;
    }

    public int getPersonId() {
        return personId;
    }

    public String getPersonCode() {
        return personCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return String.format("Person[id=%d, code='%s', name='%s']", personId, personCode, fullName);
    }
}
