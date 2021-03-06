package vn.ssdc.vnpt;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.Path;
import java.util.Set;

/**
 * Created by vietnq on 10/21/16.
 */
@Component
public class UmpResourceConfig extends ResourceConfig {
    private static final Logger logger = LoggerFactory.getLogger(UmpResourceConfig.class);

    @Value("${spring.jersey.application-path:/}")
    private String apiPath;

    public UmpResourceConfig() {
        registerEndpoints();
    }

    //find all class annotated with javax.ws.rs.Path and register
    private void registerEndpoints() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("vn.ssdc"))
                .setScanners(new TypeAnnotationsScanner()));

        Set<Class<?>> endpoints = reflections.getTypesAnnotatedWith(Path.class,true);

        for(Class<?> endpoint : endpoints) {
            logger.info("Registering endpoint: {}",endpoint.getName());
            register(endpoint);
        }
    }

    @PostConstruct
    public void init() {
        this.configureSwagger();
    }

    private void configureSwagger() {
        this.register(ApiListingResource.class);
        this.register(SwaggerSerializers.class);

        BeanConfig config = new BeanConfig();
        config.setConfigId("ump-jersey");
        config.setTitle("UMP Rest API");
        config.setDescription("Rest API for Unified Device Management");
        config.setVersion("v1");
        config.setContact("SSDC");
        config.setSchemes(new String[] { "http", "https" });
        config.setBasePath(this.apiPath);
        config.setResourcePackage("vn.ssdc.vnpt");
        config.setPrettyPrint(true);
        config.setScan(true);
    }
}
