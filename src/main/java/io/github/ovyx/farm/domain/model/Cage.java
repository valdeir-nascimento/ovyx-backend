package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.valueobject.Battery;
import io.github.ovyx.farm.domain.valueobject.BirdCount;
import io.github.ovyx.farm.domain.valueobject.CageNumber;
import java.time.Instant;
import java.util.Objects;

/**
 * Gaiola: a unidade de alojamento das aves dentro de um setor, onde as features seguintes lancam
 * producao, racao, mortalidade e peso (spec, Key Entities).
 *
 * <p>Entidade interna do agregado {@link Sector} (R-003): tem identidade propria, mas toda escrita
 * passa pelo setor, que guarda as regras entre as gaiolas dele. Por isso os metodos que a alteram sao
 * de pacote. Pertence ao mesmo setor para sempre (invariante 4).
 */
public final class Cage {

    private final CageId id;
    private final Instant createdAt;

    private Battery battery;
    private CageNumber number;
    private BirdCount birdCount;
    private Status status;
    private boolean deactivatedWithSector;
    private Instant updatedAt;

    private Cage(
            CageId id,
            Battery battery,
            CageNumber number,
            BirdCount birdCount,
            Status status,
            boolean deactivatedWithSector,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.battery = battery;
        this.number = number;
        this.birdCount = birdCount;
        this.status = status;
        this.deactivatedWithSector = deactivatedWithSector;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Uma gaiola nova: ativa, e nao inativada junto com o setor. */
    static Cage register(Battery battery, CageNumber number, BirdCount birdCount, Instant now) {
        return new Cage(CageId.generate(), battery, number, birdCount, Status.ACTIVE, false, now, now);
    }

    /**
     * Reconstroi a gaiola a partir do que foi gravado, para o adaptador de persistencia: os dados ja
     * foram validados quando entraram.
     */
    public static Cage restore(
            CageId id,
            Battery battery,
            CageNumber number,
            BirdCount birdCount,
            Status status,
            boolean deactivatedWithSector,
            Instant createdAt,
            Instant updatedAt) {
        return new Cage(id, battery, number, birdCount, status, deactivatedWithSector, createdAt, updatedAt);
    }

    /**
     * O codigo da gaiola: a bateria, um hifen e o numero com pelo menos dois digitos (FR-008). E
     * derivado, e nao gravado.
     */
    public static String codeOf(String battery, int number) {
        return battery + "-" + String.format("%02d", number);
    }

    void update(Battery battery, CageNumber number, BirdCount birdCount, Instant now) {
        this.battery = battery;
        this.number = number;
        this.birdCount = birdCount;
        this.updatedAt = now;
    }

    /**
     * Tira a gaiola de uso, sem apagar nada (FR-012).
     *
     * @param withSector se foi a inativacao do setor que a levou: so essas voltam com a reativacao dele
     *     (R-004)
     */
    void deactivate(boolean withSector, Instant now) {
        this.status = Status.INACTIVE;
        this.deactivatedWithSector = withSector;
        this.updatedAt = now;
    }

    /** Devolve a gaiola ao uso, e limpa a marca da inativacao do setor. */
    void reactivate(Instant now) {
        this.status = Status.ACTIVE;
        this.deactivatedWithSector = false;
        this.updatedAt = now;
    }

    /** Se ocupa a bateria e o numero: so conta a gaiola ativa (invariante 2). */
    boolean occupies(Battery battery, CageNumber number) {
        return isActive() && this.battery.equals(battery) && this.number.equals(number);
    }

    public String code() {
        return codeOf(battery.value(), number.value());
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public CageId id() {
        return id;
    }

    public Battery battery() {
        return battery;
    }

    public CageNumber number() {
        return number;
    }

    public BirdCount birdCount() {
        return birdCount;
    }

    public Status status() {
        return status;
    }

    /** Verdadeiro so quando a inativacao do setor inativou a gaiola (R-004). */
    public boolean deactivatedWithSector() {
        return deactivatedWithSector;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Cage cage && id.equals(cage.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
