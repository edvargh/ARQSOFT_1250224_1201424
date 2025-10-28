package pt.psoft.g1.psoftg1.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import lombok.Getter;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Getter
@MappedSuperclass
public abstract class EntityWithPhoto {
    @Nullable
    @OneToOne(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        orphanRemoval = true
    )
    @JoinColumn(name="photo_id")
    protected Photo photo;

    //This method is used by the mapper in order to set the photo. This will call the setPhotoInternal method that
    //will contain all the logic to set the photo
    public void setPhoto(String photoUri) {
        this.setPhotoInternal(photoUri);
    }

    public void setPhotoEntity(Photo photo) {
        this.photo = photo;
    }

    protected void setPhotoInternal(String photoURI) {
        if (photoURI == null) {
            this.photo = null;
        } else {
            try {
                Photo p = new Photo(Path.of(photoURI));
                //If the Path object instantiation succeeds, it means that we have a valid Path
                p.assignIdIfAbsent(java.util.UUID.randomUUID().toString());
                this.photo = p;
            } catch (InvalidPathException e) {
                //For some reason it failed, let's set to null to avoid invalid references to photos
                this.photo = null;
            }
        }
    }

    @JsonIgnore
    public String getPhotoId() {
        return (photo == null ? null : photo.getId());
    }

    @JsonIgnore
    public void setPhotoId(@Nullable String id) {
        if (id == null) {
            this.photo = null;
        } else {
            Photo ref = new Photo();
            ref.assignIdIfAbsent(id);
            this.photo = ref;
        }
    }
}
