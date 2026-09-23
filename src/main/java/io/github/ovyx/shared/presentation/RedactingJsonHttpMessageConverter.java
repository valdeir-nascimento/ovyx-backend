package io.github.ovyx.shared.presentation;

import java.io.IOException;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.json.JsonMapper;

/**
 * Conversor JSON da API que nunca repete, em mensagem de erro, o conteudo que nao conseguiu ler.
 *
 * <p>O parser do Jackson poe na mensagem o trecho invalido — "Unrecognized token 'SegredoReal2026'"
 * —, e o Spring MVC registra essa mensagem em DEBUG, duas vezes. Uma senha digitada sem aspas, o que
 * acontece a quem edita o JSON a mao no Swagger UI ou no curl, ia parar no log (FR-021). Aqui a
 * falha e relancada com texto generico e so a posicao do erro, sem a causa original, que carrega o
 * trecho.
 *
 * <p>Nao e bean: {@link JsonConverterConfiguration} o poe no lugar do conversor JSON padrao, sem
 * mudar a ordem dos demais. O {@link JsonMapper} e o do proprio Spring Boot, com toda a configuracao
 * dele, inclusive a de Problem Details.
 */
public class RedactingJsonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

    public RedactingJsonHttpMessageConverter(JsonMapper jsonMapper) {
        super(jsonMapper);
    }

    @Override
    public Object read(ResolvableType type, HttpInputMessage inputMessage, Map<String, Object> hints)
            throws IOException {
        try {
            return super.read(type, inputMessage, hints);
        } catch (HttpMessageNotReadableException exception) {
            throw redacted(exception, inputMessage);
        }
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        try {
            return super.readInternal(clazz, inputMessage);
        } catch (HttpMessageNotReadableException exception) {
            throw redacted(exception, inputMessage);
        }
    }

    private static HttpMessageNotReadableException redacted(
            HttpMessageNotReadableException exception, HttpInputMessage inputMessage) {
        return new HttpMessageNotReadableException(
                "O corpo da requisição não é um JSON válido" + positionOf(exception) + ".", inputMessage);
    }

    /** Linha e coluna do erro, que ajudam a investigar sem expor nada do que foi enviado. */
    private static String positionOf(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof JacksonException cause && cause.getLocation() != null) {
            TokenStreamLocation location = cause.getLocation();
            return " (linha " + location.getLineNr() + ", coluna " + location.getColumnNr() + ")";
        }
        return "";
    }
}
