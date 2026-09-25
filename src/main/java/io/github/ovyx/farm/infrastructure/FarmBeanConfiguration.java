package io.github.ovyx.farm.infrastructure;

import io.github.ovyx.farm.application.cage.CageDirectory;
import io.github.ovyx.farm.application.cage.DeactivateCageCommandHandler;
import io.github.ovyx.farm.application.cage.FindCageByIdQueryHandler;
import io.github.ovyx.farm.application.cage.ReactivateCageCommandHandler;
import io.github.ovyx.farm.application.cage.RegisterCageCommandHandler;
import io.github.ovyx.farm.application.cage.SearchCagesQueryHandler;
import io.github.ovyx.farm.application.cage.UpdateCageCommandHandler;
import io.github.ovyx.farm.application.sector.DeactivateSectorCommandHandler;
import io.github.ovyx.farm.application.sector.FindSectorByIdQueryHandler;
import io.github.ovyx.farm.application.sector.ListSectorsQueryHandler;
import io.github.ovyx.farm.application.sector.ReactivateSectorCommandHandler;
import io.github.ovyx.farm.application.sector.RegisterSectorCommandHandler;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.application.sector.UpdateSectorCommandHandler;
import io.github.ovyx.farm.domain.port.SectorRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiacao dos tratadores do contexto farm.
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
    FindCageByIdQueryHandler findCageByIdQueryHandler(CageDirectory cageDirectory) {
        return new FindCageByIdQueryHandler(cageDirectory);
    }
}
