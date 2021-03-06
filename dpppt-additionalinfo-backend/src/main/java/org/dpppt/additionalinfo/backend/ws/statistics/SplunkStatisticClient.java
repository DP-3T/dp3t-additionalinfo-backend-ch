package org.dpppt.additionalinfo.backend.ws.statistics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.dpppt.additionalinfo.backend.ws.data.HistoryDataService;
import org.dpppt.additionalinfo.backend.ws.model.statistics.History;
import org.dpppt.additionalinfo.backend.ws.model.statistics.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class SplunkStatisticClient implements StatisticClient {

    private final HistoryDataService historyDataService;
    private final String url;
    private final String username;
    private final String password;
    private final String activeAppsQuery;
    private final String usedAuthCodeCountQuery;
    private final String positiveTestCountQuery;
    private final String queryCovidCodesEnteredAfterXDaysOnsetOfSymptoms;
    private final LocalDate queryStartDate;
    private final Integer queryEndDaysBack;
    private final Integer overrideActiveAppsCount;

    private final RestTemplate rt;

    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int SOCKET_TIMEOUT = 30_000;

    private static final Logger logger = LoggerFactory.getLogger(SplunkStatisticClient.class);

    public SplunkStatisticClient(
            HistoryDataService historyDataService,
            String splunkUrl,
            String splunkUsername,
            String splunkpassword,
            String activeAppsQuery,
            String usedAuthCodeCountQuery,
            String positiveTestCountQuery,
            String queryCovidCodesEnteredAfterXDaysOnsetOfSymptoms,
            LocalDate queryStartDate,
            Integer queryEndDaysBack,
            Integer overrideActiveAppsCount) {
        this.historyDataService = historyDataService;
        this.url = splunkUrl;
        this.username = splunkUsername;
        this.password = splunkpassword;
        this.activeAppsQuery = activeAppsQuery;
        this.usedAuthCodeCountQuery = usedAuthCodeCountQuery;
        this.positiveTestCountQuery = positiveTestCountQuery;
        this.queryCovidCodesEnteredAfterXDaysOnsetOfSymptoms =
                queryCovidCodesEnteredAfterXDaysOnsetOfSymptoms;
        this.queryStartDate = queryStartDate;
        this.queryEndDaysBack = queryEndDaysBack;
        this.overrideActiveAppsCount = overrideActiveAppsCount;

        // Setup rest template for making http requests to Splunk. This configures a
        // custom HTTP client with some good defaults and a custom user agent.
        HttpClientBuilder builder =
                HttpClients.custom()
                        .useSystemProperties()
                        .setUserAgent("dp3t-additional-info-backend");
        builder.disableCookieManagement()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(CONNECT_TIMEOUT)
                                .setSocketTimeout(SOCKET_TIMEOUT)
                                .build());

        CloseableHttpClient httpClient = builder.build();
        this.rt = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Override
    public Statistics getStatistics() {
        long start = System.currentTimeMillis();
        logger.info("Loading statistics from Splunk: " + this.url);

        Statistics statistics = new Statistics();
        LocalDate today = LocalDate.now();
        statistics.setLastUpdated(today);
        fillDays(today, statistics);

        try {
            loadActiveApps(statistics);
            loadUsedAuthCodeCount(statistics);
            loadPositiveTestCount(statistics);
            loadCovidcodesEntered0to2dPrevWeek(statistics);
        } catch (Exception e) {
            logger.error("Could not load statistics from Splunk: " + e);
            throw new RuntimeException(e);
        }

        long end = System.currentTimeMillis();
        logger.info("Statistics loaded from Spunk in: " + (end - start) + " [ms]");
        return statistics;
    }

    private void fillDays(LocalDate today, Statistics statistics) {
        LocalDate dayDate = queryStartDate;
        LocalDate endDate = today.minusDays(queryEndDaysBack);
        logger.info("Setup statistics result history. Start: " + dayDate + " End: " + endDate);
        while (dayDate.isBefore(endDate)) {
            History history = new History();
            history.setDate(dayDate);
            statistics.getHistory().add(history);
            dayDate = dayDate.plusDays(1);
        }
    }

    private void loadCovidcodesEntered0to2dPrevWeek(Statistics statistics) throws Exception {
        logger.info("Loading covid codes entered within 0 to 2 days for last 7 days");
        RequestEntity<MultiValueMap<String, String>> request =
                RequestEntity.post(new URI(url))
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(createHeaders())
                        .body(
                                createRequestParamsForLastXDays(
                                        queryCovidCodesEnteredAfterXDaysOnsetOfSymptoms, 7));
        logger.debug("Request entity: " + request.toString());
        ResponseEntity<String> response = rt.exchange(request, String.class);
        logger.info("Result: Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            List<SplunkResult> resultList = extractResultFromSplunkApiString(response.getBody());
            if (!resultList.isEmpty()) {
                int within0To2Days = 0;
                int total = 0;
                for (SplunkResult splunkResult : resultList) {
                    within0To2Days +=
                            splunkResult.getAfterZeroDays()
                                    + splunkResult.getAfterOneDays()
                                    + splunkResult.getAfterTwoDays();
                    total += splunkResult.getTotal();
                }
                if (total == 0) {
                    statistics.setCovidcodesEntered0to2dPrevWeek(1.0);
                } else {
                    statistics.setCovidcodesEntered0to2dPrevWeek(within0To2Days / (double) total);
                }
            }
        }
        logger.info("Covid codes entered within 0 to 2 days for last 7 days loaded");
    }

    private void loadActiveApps(Statistics statistics) throws Exception {
        logger.info("Loading active apps");
        RequestEntity<MultiValueMap<String, String>> request =
                RequestEntity.post(new URI(url))
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(createHeaders())
                        .body(createRequestParamsForLastXDays(activeAppsQuery, 10));
        logger.debug("Request entity: " + request.toString());
        ResponseEntity<String> response = rt.exchange(request, String.class);
        logger.info("Result: Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            List<SplunkResult> resultList = extractResultFromSplunkApiString(response.getBody());
            if (!resultList.isEmpty()) {
                // get latest result
                Optional<SplunkResult> latestCount =
                        resultList.stream().filter(r -> r.getActiveApps() != null).findFirst();
                if (latestCount.isPresent()) {
                    statistics.setTotalActiveUsers(latestCount.get().getActiveApps());
                } else {
                    statistics.setTotalActiveUsers(null);
                }
            }
        }
        if (overrideActiveAppsCount != null) {
            logger.info(
                    "Override active app count. From query: "
                            + statistics.getTotalActiveUsers()
                            + " override with: "
                            + overrideActiveAppsCount);
            statistics.setTotalActiveUsers(overrideActiveAppsCount);
        }
        logger.info("Active apps loaded");
    }

    private void loadUsedAuthCodeCount(Statistics statistics) throws Exception {
        logger.info("Loading used auth code count");
        RequestEntity<MultiValueMap<String, String>> request =
                RequestEntity.post(new URI(url))
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(createHeaders())
                        .body(createRequestParams(usedAuthCodeCountQuery));
        logger.debug("Request entity: " + request.toString());
        ResponseEntity<String> response = rt.exchange(request, String.class);
        logger.info("Result: Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            List<SplunkResult> resultList = extractResultFromSplunkApiString(response.getBody());
            int totalCovidcodesEntered = 0;
            for (SplunkResult r : resultList) {
                for (History h : statistics.getHistory()) {
                    if (h.getDate().isEqual(r.getTime().toLocalDate())) {
                        h.setCovidcodesEntered(r.getUsedAuthorizationCodesCount());
                    }
                }
                totalCovidcodesEntered += r.getUsedAuthorizationCodesCount();
            }
            statistics.setTotalCovidcodesEntered(totalCovidcodesEntered);
        }
        logger.info("Used auth code count loaded");
    }

    private void loadPositiveTestCount(Statistics statistics) throws Exception {
        logger.info("Loading positive test count");
        RequestEntity<MultiValueMap<String, String>> request =
                RequestEntity.post(new URI(url))
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(createHeaders())
                        .body(createRequestParams(positiveTestCountQuery));
        logger.debug("Request entity: " + request.toString());
        ResponseEntity<String> response = rt.exchange(request, String.class);
        logger.info("Result: Status: " + response.getStatusCode() + " Body: " + response.getBody());
        if (response.getStatusCode() == HttpStatus.OK) {
            List<SplunkResult> resultList = extractResultFromSplunkApiString(response.getBody());
            for (SplunkResult r : resultList) {
                for (History h : statistics.getHistory()) {
                    if (h.getDate().isEqual(r.getTime().toLocalDate())) {
                        h.setNewInfections(r.getPositiveTestCount());
                    }
                }
            }
        }
        StatisticHelper.calculateRollingAverage(statistics);

        Integer latestSevenDayAverage = null;
        Integer prevWeekSevenDayAverage = null;
        for (int i = statistics.getHistory().size() - 1; i > 0; i--) {
            latestSevenDayAverage =
                    statistics.getHistory().get(i).getNewInfectionsSevenDayAverage();
            if (latestSevenDayAverage != null) {
                LocalDate day = statistics.getHistory().get(i).getDate();
                historyDataService.upsertLatestSevenDayAvgForDay(latestSevenDayAverage, day);
                prevWeekSevenDayAverage =
                        historyDataService.findLatestSevenDayAvgForDay(day.minusDays(7));
                if (prevWeekSevenDayAverage == null) {
                    logger.warn(
                            "no seven day avg history for {}. using current data as fallback", day);
                    prevWeekSevenDayAverage =
                            statistics.getHistory().get(i - 7).getNewInfectionsSevenDayAverage();
                }
                break;
            }
        }
        statistics.setNewInfectionsSevenDayAvg(latestSevenDayAverage);
        statistics.setNewInfectionsSevenDayAvgRelPrevWeek(
                (latestSevenDayAverage / (double) prevWeekSevenDayAverage) - 1);

        logger.info("Positive test count loaded");
    }

    private MultiValueMap<String, String> createRequestParams(String query) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("search", query);
        long daysBack = ChronoUnit.DAYS.between(queryStartDate, LocalDate.now());
        params.add("earliest_time", "-" + daysBack + "d@d");
        params.add("latest_time", "-" + queryEndDaysBack + "d@d");
        params.add("output_mode", "json");
        return params;
    }

    /**
     * Use for queries where only data for the last x days is needed
     *
     * @param query
     * @param daysBack
     * @return
     */
    private MultiValueMap<String, String> createRequestParamsForLastXDays(
            String query, int daysBack) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("search", query);
        params.add("earliest_time", "-" + daysBack + "d@d");
        params.add("latest_time", "now");
        params.add("output_mode", "json");
        return params;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        return headers;
    }

    /**
     * Reads the response from the splunk api (not fully valid json, but single json objects, line
     * by line) and returns a list of {@link SplunkResult} in descending order by time.
     *
     * @param splunkApiResponse
     * @return
     * @throws JsonMappingException
     * @throws JsonProcessingException
     */
    private List<SplunkResult> extractResultFromSplunkApiString(String splunkApiResponse)
            throws JsonMappingException, JsonProcessingException {
        String sanitizedSplunkApiStrint = splunkApiResponse.replaceAll("\"NO_DATA\"", "null");
        ObjectMapper om = new ObjectMapper();
        List<SplunkResult> results = new ArrayList<>();
        String[] lines = sanitizedSplunkApiStrint.split("\\n");
        for (String line : lines) {
            SplunkResponse response = om.readValue(line, SplunkResponse.class);
            if (!Boolean.TRUE.equals(response.getPreview())) {
                results.add(response.getResult());
            }
        }
        Collections.sort(
                results, Collections.reverseOrder(Comparator.comparing(SplunkResult::getTime)));
        return results;
    }
}
