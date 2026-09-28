package io.github.ovyx.production.infrastructure;

import io.github.ovyx.production.application.dailyreport.ConfirmNoMortalityCommandHandler;
import io.github.ovyx.production.application.dailyreport.CorrectDailyReportCommandHandler;
import io.github.ovyx.production.application.dailyreport.DailyReportDirectory;
import io.github.ovyx.production.application.dailyreport.FindDailyReportQueryHandler;
import io.github.ovyx.production.application.dailyreport.FindReportCageQueryHandler;
import io.github.ovyx.production.application.dailyreport.ListDailyReportsQueryHandler;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommandHandler;
import io.github.ovyx.production.application.dailyreport.RecordFeedBySuggestionCommandHandler;
import io.github.ovyx.production.application.dailyreport.RecordFeedCommandHandler;
import io.github.ovyx.production.application.dailyreport.RecordMortalityCommandHandler;
import io.github.ovyx.production.application.dailyreport.RecordProductionCommandHandler;
import io.github.ovyx.production.application.dailyreport.SuggestDailyReportQueryHandler;
import io.github.ovyx.production.application.dailyreport.SuggestFeedQueryHandler;
import io.github.ovyx.production.application.dashboard.DashboardDirectory;
import io.github.ovyx.production.application.dashboard.GetDashboardOverviewQueryHandler;
import io.github.ovyx.production.application.dashboard.GetSectorDashboardQueryHandler;
import io.github.ovyx.production.domain.port.DailyReportRepository;
import io.github.ovyx.production.domain.port.FarmStructure;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.shared.application.FarmCalendar;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fiacao dos tratadores do contexto production.
 *
 * <p>Os tratadores sao classes comuns, sem anotacao: a camada {@code application} e livre de framework
 * (principio I), e por isso quem os instancia e esta configuracao, que vive em {@code infrastructure}.
 */
@Configuration
public class ProductionBeanConfiguration {

    @Bean
    OpenDailyReportCommandHandler openDailyReportCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FarmCalendar calendar, Clock clock) {
        return new OpenDailyReportCommandHandler(repository, farmStructure, calendar, clock);
    }

    @Bean
    CorrectDailyReportCommandHandler correctDailyReportCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FarmCalendar calendar, Clock clock) {
        return new CorrectDailyReportCommandHandler(repository, farmStructure, calendar, clock);
    }

    @Bean
    RecordProductionCommandHandler recordProductionCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, Clock clock) {
        return new RecordProductionCommandHandler(repository, farmStructure, clock);
    }

    @Bean
    RecordMortalityCommandHandler recordMortalityCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, Clock clock) {
        return new RecordMortalityCommandHandler(repository, farmStructure, clock);
    }

    @Bean
    ConfirmNoMortalityCommandHandler confirmNoMortalityCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, Clock clock) {
        return new ConfirmNoMortalityCommandHandler(repository, farmStructure, clock);
    }

    @Bean
    RecordFeedBySuggestionCommandHandler recordFeedBySuggestionCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FeedCatalog feedCatalog, Clock clock) {
        return new RecordFeedBySuggestionCommandHandler(repository, farmStructure, feedCatalog, clock);
    }

    @Bean
    RecordFeedCommandHandler recordFeedCommandHandler(
            DailyReportRepository repository, FarmStructure farmStructure, FeedCatalog feedCatalog, Clock clock) {
        return new RecordFeedCommandHandler(repository, farmStructure, feedCatalog, clock);
    }

    @Bean
    SuggestFeedQueryHandler suggestFeedQueryHandler(DailyReportDirectory directory, FeedCatalog feedCatalog) {
        return new SuggestFeedQueryHandler(directory, feedCatalog);
    }

    @Bean
    ListDailyReportsQueryHandler listDailyReportsQueryHandler(DailyReportDirectory directory) {
        return new ListDailyReportsQueryHandler(directory);
    }

    @Bean
    FindDailyReportQueryHandler findDailyReportQueryHandler(DailyReportDirectory directory) {
        return new FindDailyReportQueryHandler(directory);
    }

    @Bean
    FindReportCageQueryHandler findReportCageQueryHandler(DailyReportDirectory directory) {
        return new FindReportCageQueryHandler(directory);
    }

    @Bean
    SuggestDailyReportQueryHandler suggestDailyReportQueryHandler(
            DailyReportDirectory directory, FarmCalendar calendar) {
        return new SuggestDailyReportQueryHandler(directory, calendar);
    }

    @Bean
    GetDashboardOverviewQueryHandler getDashboardOverviewQueryHandler(
            DashboardDirectory directory, FarmCalendar calendar) {
        return new GetDashboardOverviewQueryHandler(directory, calendar);
    }

    @Bean
    GetSectorDashboardQueryHandler getSectorDashboardQueryHandler(
            DashboardDirectory directory, FarmCalendar calendar) {
        return new GetSectorDashboardQueryHandler(directory, calendar);
    }
}
