package ch.so.agi.wgc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Resource configResource;

    public ConfigController( @Value("${app.configResource}") Resource configResource) {
        this.configResource = configResource;
    }
    
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> config() throws IOException { 
        byte[] content = configResource.getContentAsByteArray();        
        String json = new String(content, StandardCharsets.UTF_8);
        return ResponseEntity.ok(json);
    }
}
