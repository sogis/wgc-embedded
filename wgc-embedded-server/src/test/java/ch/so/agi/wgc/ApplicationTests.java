package ch.so.agi.wgc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ApplicationTests {
    static Logger logger = LoggerFactory.getLogger(ApplicationTests.class);

    @LocalServerPort
    protected String port;

    protected TestRestTemplate restTemplate;

    protected ObjectMapper mapper;

    public ApplicationTests(TestRestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    @Test
    public void contextLoads() {
    }
    
    @Test
    public void getConfig_Ok() throws Exception {
        String serverUrl = "http://localhost:"+port+"/config";

        ResponseEntity<String> response = restTemplate.getForEntity(serverUrl, String.class);
        Map<String, Object> body = mapper.readValue(response.getBody(), Map.class);
        List<Map<String,Object>> basemaps = (List<Map<String,Object>>) body.get("basemaps");
        
        assertEquals(200, response.getStatusCode().value());
        assertEquals("https://geo.so.ch/api/wmts/1.0.0/{Layer}/default/2056/{TileMatrix}/{TileRow}/{TileCol}", basemaps.get(0).get("url"));
    }
    
    @Test
    public void index_Ok() throws Exception {
        assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/index.html", String.class))
                .contains("Web GIS Client • Kanton Solothurn");
    }
}
