package one.ampadu.dsv.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "PROTOCOL_ENTRY")
public class ProtocolEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setAgeGroup(long ageGroup) {
        this.ageGroup = ageGroup;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public void setStroke(String stroke) {
        this.stroke = stroke;
    }

    public void setCalendarYear(int calendarYear) {
        this.calendarYear = calendarYear;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public long getAgeGroup() {
        return ageGroup;
    }

    public String getSex() {
        return sex;
    }

    public long getTime() {
        return time;
    }

    public int getDistance() {
        return distance;
    }

    public String getStroke() {
        return stroke;
    }

    public int getCalendarYear() {
        return calendarYear;
    }

    public String getPlace() {
        return place;
    }

    private String firstName;
    private String lastName;
    private long ageGroup;
    private String sex;
    private long time;
    private int distance;
    private String stroke;
    private int calendarYear;
    private String place;
    private String club;

    public void setPoolDistance(int poolDistance) {
        this.poolDistance = poolDistance;
    }

    public int getPoolDistance() {
        return poolDistance;
    }

    private int poolDistance;

    public void setClub(String club) {
        this.club = club;
    }

    public String getClub() {
        return club;
    }
}
