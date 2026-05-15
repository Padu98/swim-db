package one.ampadu.dsv.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "PROCESS_RUN")
public class ProtocolProcessRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date success;

    @Column(unique = true)
    private int numberId;
}
