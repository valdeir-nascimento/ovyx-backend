package io.github.ovyx.shared.presentation;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Monta o corpo de erro no mesmo formato do {@link ResultHttpMapper}.
 *
 * <p>Existe porque nem toda recusa passa pelo Result: as da cadeia do Spring Security — sessao
 * ausente, acesso negado, token de protecao invalido — sao escritas direto no {@code
 * HttpServletResponse}, antes de qualquer controller. O cliente nao deveria perceber a diferenca,
 * entao o corpo e o mesmo: {@code status}, {@code title}, {@code detail}, {@code instance} e o
 * {@code code} estavel.
 *
 * <p>O {@code instance} precisa ser preenchido aqui: o Spring o acrescenta sozinho ao
 * {@code ProblemDetail} que volta de um controller, mas nao ao que e escrito direto na resposta —
 * e sem isso dois 401 do mesmo endpoint saiam com formatos diferentes.
 */
public final class ProblemResponses {

    private ProblemResponses() {}

    public static ProblemDetail of(HttpStatus status, String code, String title, String detail, URI instance) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        // Titulo em portugues, e nao o reason phrase do HTTP (principio VII).
        body.setTitle(title);
        body.setInstance(instance);
        body.setProperty("code", code);
        return body;
    }
}
