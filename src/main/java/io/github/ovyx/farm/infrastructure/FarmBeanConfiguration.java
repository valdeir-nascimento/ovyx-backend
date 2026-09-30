package io.github.ovyx.farm.infrastructure;

import io.github.ovyx.farm.application.cage.CageDirectory;
import io.github.ovyx.farm.application.cage.DeactivateCageCommandHandler;
import io.github.ovyx.farm.application.cage.ExportCagesQueryHandler;
import io.github.ovyx.farm.application.cage.FindCageByIdQueryHandler;
import io.github.ovyx.farm.application.cage.ReactivateCageCommandHandler;
import io.github.ovyx.farm.application.cage.RegisterCageCommandHandler;
import io.github.ovyx.farm.application.cage.SearchCagesQueryHandler;
import io.github.ovyx.farm.application.cage.UpdateCageCommandHandler;
import io.github.ovyx.farm.application.formula.DeactivateFeedFormulaCommandHandler;
import io.github.ovyx.farm.application.formula.FeedFormulaDirectory;
import io.github.ovyx.farm.application.formula.FindFeedFormulaQueryHandler;
import io.github.ovyx.farm.application.formula.ListFeedFormulasQueryHandler;
import io.github.ovyx.farm.application.formula.ReactivateFeedFormulaCommandHandler;
import io.github.ovyx.farm.application.formula.RegisterFeedFormulaCommandHandler;
import io.github.ovyx.farm.application.formula.UpdateFeedFormulaCommandHandler;
import io.github.ovyx.farm.application.sector.DeactivateSectorCommandHandler;
import io.github.ovyx.farm.application.sector.FindSectorByIdQueryHandler;
import io.github.ovyx.farm.application.sector.ListSectorsQueryHandler;
import io.github.ovyx.farm.application.sector.ReactivateSectorCommandHandler;
import io.github.ovyx.farm.application.sector.RegisterSectorCommandHandler;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.application.sector.UpdateSectorCommandHandler;
import io.github.ovyx.farm.application.weighing.CorrectWeighingCommandHandler;
import io.github.ovyx.farm.application.weighing.FindWeighingQueryHandler;
import io.github.ovyx.farm.application.weighing.GetWeighingOverviewQueryHandler;
import io.github.ovyx.farm.application.weighing.RecordWeighingCommandHandler;
import io.github.ovyx.farm.application.weighing.VoidWeighingCommandHandler;
import io.github.ovyx.farm.application.weighing.WeighingDirectory;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetWriter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiacao dos tratadores do contexto farm: setores, gaiolas e formulas de racao.
 *
 * <p>Os tratadores sao classes comuns, sem anotacao: a camada {@code application} e livre de framework
 * (principio I), e por isso quem os instancia e esta configuracao, que vive em {@code infrastructure}.
 */
@Configuration
public class FarmBeanConfiguration {

    @Bean
    RegisterSectorCommandHandler registerSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new RegisterSectorCommandHandler(sectorRepository, clock);
    }

    @Bean
    UpdateSectorCommandHandler updateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new UpdateSectorCommandHandler(sectorRepository, clock);
    }

    @Bean
    DeactivateSectorCommandHandler deactivateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new DeactivateSectorCommandHandler(sectorRepository, clock);
    }

    @Bean
    ReactivateSectorCommandHandler reactivateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new ReactivateSectorCommandHandler(sectorRepository, clock);
    }

    @Bean
    DeactivateCageCommandHandler deactivateCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new DeactivateCageCommandHandler(sectorRepository, clock);
    }

    @Bean
    ReactivateCageCommandHandler reactivateCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new ReactivateCageCommandHandler(sectorRepository, clock);
    }

    @Bean
    ListSectorsQueryHandler listSectorsQueryHandler(SectorDirectory sectorDirectory) {
        return new ListSectorsQueryHandler(sectorDirectory);
    }

    @Bean
    FindSectorByIdQueryHandler findSectorByIdQueryHandler(SectorDirectory sectorDirectory) {
        return new FindSectorByIdQueryHandler(sectorDirectory);
    }

    @Bean
    RegisterCageCommandHandler registerCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new RegisterCageCommandHandler(sectorRepository, clock);
    }

    @Bean
    UpdateCageCommandHandler updateCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        return new UpdateCageCommandHandler(sectorRepository, clock);
    }

    @Bean
    SearchCagesQueryHandler searchCagesQueryHandler(CageDirectory cageDirectory) {
        return new SearchCagesQueryHandler(cageDirectory);
    }

    @Bean
    ExportCagesQueryHandler exportCagesQueryHandler(
            CageDirectory cageDirectory,
            SectorDirectory sectorDirectory,
            SpreadsheetWriter writer,
            FarmCalendar calendar) {
        return new ExportCagesQueryHandler(cageDirectory, sectorDirectory, writer, calendar);
    }

    @Bean
    FindCageByIdQueryHandler findCageByIdQueryHandler(CageDirectory cageDirectory) {
        return new FindCageByIdQueryHandler(cageDirectory);
    }

    @Bean
    RegisterFeedFormulaCommandHandler registerFeedFormulaCommandHandler(
            FeedFormulaRepository formulaRepository, Clock clock) {
        return new RegisterFeedFormulaCommandHandler(formulaRepository, clock);
    }

    @Bean
    UpdateFeedFormulaCommandHandler updateFeedFormulaCommandHandler(
            FeedFormulaRepository formulaRepository, Clock clock) {
        return new UpdateFeedFormulaCommandHandler(formulaRepository, clock);
    }

    @Bean
    DeactivateFeedFormulaCommandHandler deactivateFeedFormulaCommandHandler(
            FeedFormulaRepository formulaRepository, Clock clock) {
        return new DeactivateFeedFormulaCommandHandler(formulaRepository, clock);
    }

    @Bean
    ReactivateFeedFormulaCommandHandler reactivateFeedFormulaCommandHandler(
            FeedFormulaRepository formulaRepository, Clock clock) {
        return new ReactivateFeedFormulaCommandHandler(formulaRepository, clock);
    }

    @Bean
    ListFeedFormulasQueryHandler listFeedFormulasQueryHandler(FeedFormulaDirectory formulaDirectory) {
        return new ListFeedFormulasQueryHandler(formulaDirectory);
    }

    @Bean
    FindFeedFormulaQueryHandler findFeedFormulaQueryHandler(FeedFormulaDirectory formulaDirectory) {
        return new FindFeedFormulaQueryHandler(formulaDirectory);
    }

    @Bean
    RecordWeighingCommandHandler recordWeighingCommandHandler(
            SectorRepository sectorRepository,
            WeighingRepository weighingRepository,
            FarmCalendar calendar,
            Clock clock) {
        return new RecordWeighingCommandHandler(sectorRepository, weighingRepository, calendar, clock);
    }

    @Bean
    GetWeighingOverviewQueryHandler getWeighingOverviewQueryHandler(WeighingDirectory weighingDirectory) {
        return new GetWeighingOverviewQueryHandler(weighingDirectory);
    }

    @Bean
    FindWeighingQueryHandler findWeighingQueryHandler(WeighingDirectory weighingDirectory) {
        return new FindWeighingQueryHandler(weighingDirectory);
    }

    @Bean
    CorrectWeighingCommandHandler correctWeighingCommandHandler(
            SectorRepository sectorRepository,
            WeighingRepository weighingRepository,
            FarmCalendar calendar,
            Clock clock) {
        return new CorrectWeighingCommandHandler(sectorRepository, weighingRepository, calendar, clock);
    }

    @Bean
    VoidWeighingCommandHandler voidWeighingCommandHandler(
            SectorRepository sectorRepository, WeighingRepository weighingRepository, Clock clock) {
        return new VoidWeighingCommandHandler(sectorRepository, weighingRepository, clock);
    }
}
