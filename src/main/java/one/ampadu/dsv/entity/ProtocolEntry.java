package one.ampadu.dsv.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "PROTOCOL_ENTRY")
public class ProtocolEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private int poolDistance;
}
