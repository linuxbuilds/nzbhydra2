package org.nzbhydra.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nzbhydra.NzbHydra;
import org.nzbhydra.config.IndexerConfig;
import org.nzbhydra.config.SearchModuleType;
import org.nzbhydra.database.IndexerAccessResult;
import org.nzbhydra.database.IndexerApiAccessEntity;
import org.nzbhydra.database.IndexerApiAccessRepository;
import org.nzbhydra.database.IndexerEntity;
import org.nzbhydra.database.IndexerRepository;
import org.nzbhydra.database.IndexerSearchEntity;
import org.nzbhydra.database.IndexerSearchRepository;
import org.nzbhydra.database.NzbDownloadEntity;
import org.nzbhydra.database.NzbDownloadRepository;
import org.nzbhydra.database.SearchEntity;
import org.nzbhydra.database.SearchRepository;
import org.nzbhydra.searching.SearchModuleConfigProvider;
import org.nzbhydra.searching.SearchModuleProvider;
import org.nzbhydra.web.mapping.stats.AverageResponseTime;
import org.nzbhydra.web.mapping.stats.CountPerDayOfWeek;
import org.nzbhydra.web.mapping.stats.CountPerHourOfDay;
import org.nzbhydra.web.mapping.stats.IndexerApiAccessStatsEntry;
import org.nzbhydra.web.mapping.stats.IndexerSearchResultsShare;
import org.nzbhydra.web.mapping.stats.StatsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("SpringJavaAutowiringInspection")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = NzbHydra.class)
@DataJpaTest
//@TestPropertySource(locations = "classpath:/org/nzbhydra/tests/searching/application.properties")
public class StatsComponentTest {

    private IndexerEntity indexer1;
    private IndexerEntity indexer2;
    private IndexerConfig indexerConfig1;
    private IndexerConfig indexerConfig2;

    @Autowired
    private SearchModuleConfigProvider searchModuleConfigProvider;
    @Autowired
    private SearchModuleProvider searchModuleProvider;
    @Autowired
    private IndexerApiAccessRepository apiAccessRepository;
    @Autowired
    private IndexerRepository indexerRepository;
    @Autowired
    private SearchRepository searchRepository;
    @Autowired
    private NzbDownloadRepository downloadRepository;
    @Autowired
    private IndexerSearchRepository indexerSearchRepository;

    @Autowired
    private Stats stats;

    @Before
    public void setUp() {
        indexerRepository.deleteAll();
        apiAccessRepository.deleteAll();
        indexerConfig1 = new IndexerConfig();
        indexerConfig1.setName("indexer1");
        indexerConfig1.setSearchModuleType(SearchModuleType.NEWZNAB);
        indexerConfig1.setEnabled(true);
        indexerConfig2 = new IndexerConfig();
        indexerConfig2.setName("indexer2");
        indexerConfig2.setSearchModuleType(SearchModuleType.NEWZNAB);
        indexerConfig2.setEnabled(false);
        searchModuleConfigProvider.setIndexers(Arrays.asList(indexerConfig1, indexerConfig2));
        searchModuleProvider.loadIndexers(Arrays.asList(indexerConfig1, indexerConfig2));
        indexer1 = indexerRepository.findByName("indexer1");
        indexer2 = indexerRepository.findByName("indexer2");
    }

    @Test
    public void shouldCalculateAverageResponseTimes() throws Exception {
        assertEquals(2, indexerRepository.count());

        IndexerApiAccessEntity apiAccess1 = new IndexerApiAccessEntity(indexer1);
        apiAccess1.setResponseTime(1000L);
        apiAccess1.setTime(Instant.now().minus(1, ChronoUnit.DAYS));
        apiAccessRepository.save(apiAccess1);

        IndexerApiAccessEntity apiAccess2 = new IndexerApiAccessEntity(indexer1);
        apiAccess2.setResponseTime(2000L);
        apiAccess2.setTime(Instant.now().minus(1, ChronoUnit.DAYS));
        apiAccessRepository.save(apiAccess2);

        IndexerApiAccessEntity apiAccess3 = new IndexerApiAccessEntity(indexer1);
        apiAccess3.setResponseTime(4000L);
        apiAccess3.setTime(Instant.now().minus(100, ChronoUnit.DAYS));
        apiAccessRepository.save(apiAccess3);

        //Access #3 is not included
        List<AverageResponseTime> averageResponseTimes = stats.averageResponseTimes(new StatsRequest(Instant.now().minus(10, ChronoUnit.DAYS), Instant.now(), true));
        assertEquals(1, averageResponseTimes.size());
        assertEquals(1500D, averageResponseTimes.get(0).getAvgResponseTime(), 0D);

        //Access #3 is included
        averageResponseTimes = stats.averageResponseTimes(new StatsRequest(Instant.now().minus(101, ChronoUnit.DAYS), Instant.now(), true));
        assertEquals(1, averageResponseTimes.size());
        assertEquals(2333D, averageResponseTimes.get(0).getAvgResponseTime(), 0D);
    }

    @Test
    public void shouldCalculateSearchesPerDayOfWeek() throws Exception {
        SearchEntity searchFriday = new SearchEntity();
        searchFriday.setTime(Instant.ofEpochSecond(1490945310L)); //Friday
        SearchEntity searchThursday1 = new SearchEntity();
        searchThursday1.setTime(Instant.ofEpochSecond(1490858910L)); //Thursday
        SearchEntity searchThursday2 = new SearchEntity();
        searchThursday2.setTime(Instant.ofEpochSecond(1490858910L)); //Thursday
        SearchEntity searchSunday = new SearchEntity();
        searchSunday.setTime(Instant.ofEpochSecond(1490513310L)); //Sunday
        searchRepository.save(Arrays.asList(searchFriday, searchThursday1, searchThursday2, searchSunday));


        List<CountPerDayOfWeek> result = stats.countPerDayOfWeek("SEARCH", new StatsRequest(searchFriday.getTime().minus(10, ChronoUnit.DAYS), searchFriday.getTime().plus(10, ChronoUnit.DAYS), true));
        assertEquals(7, result.size());

        assertEquals("Thu", result.get(3).getDay());
        assertEquals(Integer.valueOf(2), result.get(3).getCount());

        assertEquals("Fri", result.get(4).getDay());
        assertEquals(Integer.valueOf(1), result.get(4).getCount());

        assertEquals("Sun", result.get(6).getDay());
        assertEquals(Integer.valueOf(1), result.get(6).getCount());
    }

    @Test
    public void shouldCalculateDownloadsPerDayOfWeek() throws Exception {

        NzbDownloadEntity downloadFriday = new NzbDownloadEntity();
        downloadFriday.setTime(Instant.ofEpochSecond(1490945310L)); //Friday

        NzbDownloadEntity downloadThursday1 = new NzbDownloadEntity();
        downloadThursday1.setTime(Instant.ofEpochSecond(1490858910L)); //Thursday

        NzbDownloadEntity downloadThursday2 = new NzbDownloadEntity();
        downloadThursday2.setTime(Instant.ofEpochSecond(1490858910L)); //Thursday


        NzbDownloadEntity downloadSunday = new NzbDownloadEntity();
        downloadSunday.setTime(Instant.ofEpochSecond(1490513310L)); //Sunday

        downloadRepository.save(Arrays.asList(downloadFriday, downloadSunday, downloadThursday1, downloadThursday2));


        List<CountPerDayOfWeek> result = stats.countPerDayOfWeek("INDEXERNZBDOWNLOAD", new StatsRequest(downloadFriday.getTime().minus(10, ChronoUnit.DAYS), downloadFriday.getTime().plus(10, ChronoUnit.DAYS), true));
        assertEquals(7, result.size());

        assertEquals("Thu", result.get(3).getDay());
        assertEquals(Integer.valueOf(2), result.get(3).getCount());

        assertEquals("Fri", result.get(4).getDay());
        assertEquals(Integer.valueOf(1), result.get(4).getCount());

        assertEquals("Sun", result.get(6).getDay());
        assertEquals(Integer.valueOf(1), result.get(6).getCount());
    }

    @Test
    public void shouldCalculateSearchesPerHourOfDay() throws Exception {
        SearchEntity search12 = new SearchEntity();
        search12.setTime(Instant.ofEpochSecond(1490955803L));
        SearchEntity search16a = new SearchEntity();
        search16a.setTime(Instant.ofEpochSecond(1490971572L));
        SearchEntity search16b = new SearchEntity();
        search16b.setTime(Instant.ofEpochSecond(1490971572L));
        SearchEntity search23 = new SearchEntity();
        search23.setTime(Instant.ofEpochSecond(1490996779L));
        searchRepository.save(Arrays.asList(search12, search16a, search16b, search23));


        List<CountPerHourOfDay> result = stats.countPerHourOfDay("SEARCH", new StatsRequest(search12.getTime().minus(10, ChronoUnit.DAYS), search12.getTime().plus(10, ChronoUnit.DAYS), true));
        assertEquals(24, result.size());
        assertEquals(Integer.valueOf(1), result.get(12).getCount());
        assertEquals(Integer.valueOf(2), result.get(16).getCount());
        assertEquals(Integer.valueOf(1), result.get(23).getCount());
    }

    @Test
    public void shouldCalculateIndexerApiAccessStats() throws Exception {
        IndexerApiAccessEntity apiAccess1 = new IndexerApiAccessEntity(indexer1);
        apiAccess1.setResult(IndexerAccessResult.CONNECTION_ERROR); //Counted as failed
        apiAccess1.setTime(Instant.now().minus(24, ChronoUnit.HOURS));
        apiAccessRepository.save(apiAccess1);

        IndexerApiAccessEntity apiAccess2 = new IndexerApiAccessEntity(indexer1);
        apiAccess2.setResult(IndexerAccessResult.HYDRA_ERROR); //Neither counted as successful nor as failed
        apiAccessRepository.save(apiAccess2);

        IndexerApiAccessEntity apiAccess3 = new IndexerApiAccessEntity(indexer1);
        apiAccess3.setResult(IndexerAccessResult.SUCCESSFUL); //Counted as successful
        apiAccessRepository.save(apiAccess3);

        //Initially ignored by set time period
        IndexerApiAccessEntity apiAccess4 = new IndexerApiAccessEntity(indexer1);
        apiAccess4.setResult(IndexerAccessResult.SUCCESSFUL); //Counted as successful
        apiAccess4.setTime(Instant.now().minus(14, ChronoUnit.DAYS));
        apiAccessRepository.save(apiAccess4);

        List<IndexerApiAccessStatsEntry> result = stats.indexerApiAccesses(new StatsRequest(Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS), true));
        assertEquals(1, result.size());
        //One yesterday, two today: 1.5 on average
        assertEquals(1.5D, result.get(0).getAverageAccessesPerDay(), 0D);
        //One with connection error, one with another error, one successful
        assertEquals(33D, result.get(0).getPercentSuccessful(), 1D);

        result = stats.indexerApiAccesses(new StatsRequest(Instant.now().minus(20, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS), true));
        assertEquals(1, result.size());
        //One yesterday, two today, one 14 days ago: 4/3=1.33 on average
        assertEquals(1.33D, result.get(0).getAverageAccessesPerDay(), 0.01D);
        //One with connection error, one with another error, two successful
        assertEquals(50D, result.get(0).getPercentSuccessful(), 0D);
    }

    @Test
    public void shouldCalculateSearchShares() {
        //Enable indexer 2 first
        indexerConfig2.setEnabled(true);
        searchModuleConfigProvider.setIndexers(Arrays.asList(indexerConfig1, indexerConfig2));
        searchModuleProvider.loadIndexers(Arrays.asList(indexerConfig1, indexerConfig2));
        {
            //Search with a query, two indexers involved, both returned results, one had an unsuccessful additional search
            SearchEntity search1 = new SearchEntity();
            search1.setQuery("someQuery");
            IndexerSearchEntity indexer1Search1 = new IndexerSearchEntity(indexer1, search1);
            indexer1Search1.setResultsCount(900);
            indexer1Search1.setProcessedResults(100);
            indexer1Search1.setUniqueResults(7);
            indexer1Search1.setSuccessful(true);

            //Unsuccessful so it's ignored
            IndexerSearchEntity indexer1Search2Unsuccessful = new IndexerSearchEntity(indexer1, search1);
            indexer1Search2Unsuccessful.setResultsCount(99);
            indexer1Search2Unsuccessful.setProcessedResults(99);
            indexer1Search2Unsuccessful.setUniqueResults(99);
            indexer1Search2Unsuccessful.setSuccessful(false);

            IndexerSearchEntity indexer2Search1 = new IndexerSearchEntity(indexer2, search1);
            indexer2Search1.setResultsCount(600);
            indexer2Search1.setProcessedResults(100);
            indexer2Search1.setUniqueResults(3);
            indexer2Search1.setSuccessful(true);

            searchRepository.save(search1);
            indexerSearchRepository.save(Arrays.asList(indexer1Search1, indexer1Search2Unsuccessful, indexer2Search1));
        }

        {
            //Search without query or IDs, should be ignored
            SearchEntity search2UpdateQuery = new SearchEntity();
            IndexerSearchEntity indexer1Search2 = new IndexerSearchEntity(indexer1, search2UpdateQuery);
            indexer1Search2.setResultsCount(600);
            indexer1Search2.setProcessedResults(100);
            indexer1Search2.setUniqueResults(7);
            indexer1Search2.setSuccessful(true);

            IndexerSearchEntity indexer2Search2 = new IndexerSearchEntity(indexer2, search2UpdateQuery);
            indexer2Search2.setResultsCount(300);
            indexer2Search2.setProcessedResults(100);
            indexer2Search2.setUniqueResults(3);
            indexer2Search2.setSuccessful(true);

            searchRepository.save(search2UpdateQuery);
            indexerSearchRepository.save(Arrays.asList(indexer1Search2, indexer1Search2));
        }


        List<IndexerSearchResultsShare> result = stats.getIndexerSearchShares(new StatsRequest(Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().plus(10, ChronoUnit.DAYS), true));
        assertEquals(2, result.size());
        assertEquals("indexer1", result.get(0).getIndexerName());
        assertNotNull(result.get(0).getTotalShare());
        //900 from this one, 600 from the other one:
        assertEquals(60F, result.get(0).getTotalShare(), 1F);
        assertNotNull(result.get(0).getUniqueShare());
        assertEquals(70F, result.get(0).getUniqueShare(), 0F);
    }

}