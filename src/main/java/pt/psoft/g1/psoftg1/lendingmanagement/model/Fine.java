package pt.psoft.g1.psoftg1.lendingmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * The {@code Fine} class models a fine applied when a lending is past its due date.
 * <p>It stores its current value, and the associated {@code Lending}.
 * @author  rmfranca*/
@Getter
@Entity
public class Fine {
    @Id
    @Column(name = "FINE_ID", length = 36, nullable = false, updatable = false)
    private String id;

    @PositiveOrZero
    @Column(updatable = false)
    private int fineValuePerDayInCents;

    /**Fine value in Euro cents*/
    @PositiveOrZero
    int centsValue;

    @Setter
    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "lending_pk", nullable = false, unique = true)
    private Lending lending;

    /**
     * Constructs a new {@code Fine} object. Sets the current value of the fine,
     * as well as the fine value per day at the time of creation.
     * @param   lending transaction which generates this fine.
     * */
    public Fine(Lending lending) {
        if(lending.getDaysDelayed() <= 0)
            throw new IllegalArgumentException("Lending is not overdue");
        fineValuePerDayInCents = lending.getFineValuePerDayInCents();
        centsValue = fineValuePerDayInCents * lending.getDaysDelayed();
        this.lending = Objects.requireNonNull(lending);
    }

    public Fine(Lending lending, int fineValuePerDayInCents, int centsValue) {
        this.lending = Objects.requireNonNull(lending);
        this.fineValuePerDayInCents = Math.max(0, fineValuePerDayInCents);
        this.centsValue = Math.max(0, centsValue);
    }

    /**Protected empty constructor for ORM only.*/
    protected Fine() {}

    public String getId() { return id; }

    public void assignId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id cannot be blank");
        this.id = id;
    }
}
