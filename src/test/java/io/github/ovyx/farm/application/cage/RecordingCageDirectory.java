package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.PageResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê do diretório de gaiolas: responde o que o teste preparou e anota o que lhe perguntaram.
 *
 * <p>O teste do tratador de consulta prova o que o tratador pede ao diretório; o que o SQL devolve
 * fica para o {@code CageDirectoryIT}.
 */
final class RecordingCageDirectory implements CageDirectory {

    /** Uma pesquisa, como o tratador a pediu; {@code pendingSince} so com o filtro de pesagem (010). */
    record Search(
            SectorId sectorId, String code, String battery, StatusFilter status, int page, int size, LocalDate pendingSince) {}

    /** Um pedido de todas as gaiolas dos filtros, sem pagina, como o tratador da exportacao o fez (007). */
    record All(SectorId sectorId, String code, String battery, StatusFilter status, LocalDate pendingSince) {}

    private final Map<SectorId, SectorSchedule> schedules = new HashMap<>();
    private final Map<CageId, CageDetail> details = new HashMap<>();
    private final List<Search> searches = new ArrayList<>();
    private final List<All> everyCage = new ArrayList<>();
    private List<CageSummary> answer = List.of();

    /** Um setor ativo, sem dia da pesagem. */
    RecordingCageDirectory withSector(SectorId sectorId) {
        return withSchedule(sectorId, new SectorSchedule(Status.ACTIVE, null));
    }

    /** Um setor com a situacao e o dia da pesagem dados (010). */
    RecordingCageDirectory withSchedule(SectorId sectorId, SectorSchedule schedule) {
        schedules.put(sectorId, schedule);
        return this;
    }

    RecordingCageDirectory holding(CageDetail detail) {
        schedules.putIfAbsent(detail.sectorId(), new SectorSchedule(Status.ACTIVE, null));
        details.put(detail.id(), detail);
        return this;
    }

    @Override
    public Optional<SectorSchedule> scheduleOf(SectorId sectorId) {
        return Optional.ofNullable(schedules.get(sectorId));
    }

    @Override
    public PageResponse<CageSummary> search(SectorId sectorId, CageFilter filter, int page, int size) {
        searches.add(new Search(
                sectorId, filter.code(), filter.battery(), filter.status(), page, size, filter.pendingSince()));
        return PageResponse.of(answer, page, size, answer.size());
    }

    @Override
    public List<CageSummary> searchAll(SectorId sectorId, CageFilter filter) {
        everyCage.add(new All(sectorId, filter.code(), filter.battery(), filter.status(), filter.pendingSince()));
        return answer;
    }

    RecordingCageDirectory answering(List<CageSummary> cages) {
        this.answer = List.copyOf(cages);
        return this;
    }

    List<All> everyCageAsked() {
        return List.copyOf(everyCage);
    }

    @Override
    public Optional<CageDetail> findDetail(SectorId sectorId, CageId cageId) {
        return Optional.ofNullable(details.get(cageId)).filter(detail -> detail.sectorId().equals(sectorId));
    }

    List<Search> searches() {
        return List.copyOf(searches);
    }
}
