package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.List;

/** Lista os setores com os totais das gaiolas ativas; sem situacao pedida, so os ativos (FR-005). */
public class ListSectorsQueryHandler implements QueryHandler<ListSectorsQuery, List<SectorSummary>> {

    private final SectorDirectory sectorDirectory;

    public ListSectorsQueryHandler(SectorDirectory sectorDirectory) {
        this.sectorDirectory = sectorDirectory;
    }

    @Override
    public Result<List<SectorSummary>> handle(ListSectorsQuery query) {
        StatusFilter status = query.status() == null ? StatusFilter.ACTIVE : query.status();
        return Result.success(sectorDirectory.list(status));
    }
}
