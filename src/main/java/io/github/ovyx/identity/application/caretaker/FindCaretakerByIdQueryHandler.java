package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

/** Consulta um responsavel pelo administrador (FR-014). */
public class FindCaretakerByIdQueryHandler implements QueryHandler<FindCaretakerByIdQuery, CaretakerDetail> {

    private final CaretakerDirectory caretakerDirectory;

    public FindCaretakerByIdQueryHandler(CaretakerDirectory caretakerDirectory) {
        this.caretakerDirectory = caretakerDirectory;
    }

    @Override
    public Result<CaretakerDetail> handle(FindCaretakerByIdQuery query) {
        return caretakerDirectory
                .findDetail(query.caretakerId())
                .map(Result::success)
                .orElseGet(() -> Result.failure(CaretakerRefusals.notFound()));
    }
}
