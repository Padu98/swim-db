package one.ampadu.dsv.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @ManyToOne
    @JoinColumn(name = "process_run_id")
    private ProtocolProcessRun processRun;
}
