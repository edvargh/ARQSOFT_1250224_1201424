package pt.psoft.g1.psoftg1.shared.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class ForbiddenName{

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String pk;

    @Getter
    @Setter
    @Column(nullable = false)
    @Size(min = 1)
    private String forbiddenName;

    public ForbiddenName(String id, String name) {
        this.pk = id;
        this.forbiddenName = name;
    }

    public void assignIdIfAbsent(String id) {
        if (this.pk == null) this.pk = id;
    }

    public String getId() { return pk; }
}
