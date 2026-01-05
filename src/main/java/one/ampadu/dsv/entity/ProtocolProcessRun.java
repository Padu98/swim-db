package one.ampadu.dsv.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "PROCESS_RUN")
public class ProtocolProcessRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public void setSuccess(Date success) {
        this.success = success;
    }

    public void setNumberId(int numberId) {
        this.numberId = numberId;
    }

    public Long getId() {
        return id;
    }

    public Date getSuccess() {
        return success;
    }

    public int getNumberId() {
        return numberId;
    }

    private Date success;

    @Column(unique = true)
    private int numberId;
}
