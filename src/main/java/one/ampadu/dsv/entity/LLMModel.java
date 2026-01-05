package one.ampadu.dsv.entity;

import jakarta.persistence.*;
import java.util.Date;


@Entity
@Table(name = "LLM_MODEL")
public class LLMModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;
    private String provider;
    private Date blocked;

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setBlocked(Date blocked) {
        this.blocked = blocked;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public Date getBlocked() {
        return blocked;
    }
}