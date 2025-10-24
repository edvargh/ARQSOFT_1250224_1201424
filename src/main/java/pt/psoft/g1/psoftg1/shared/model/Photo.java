package pt.psoft.g1.psoftg1.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

@Entity
public class Photo {
    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String pk;

    @NotNull
    @Setter
    @Getter
    private String photoFile;

    protected Photo (){}

    public Photo (Path photoPath){
        setPhotoFile(photoPath.toString());
    }

    public void assignIdIfAbsent(String id) {
        if (this.pk == null) this.pk = id;
    }

    public String getId() { return pk; }
}

