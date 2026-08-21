package pl.decerto;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import(HigsonIntegrationConfiguration.class)
// Każde demo ma własny pakiet i musi być tu wymienione — skan nie sięga wyżej niż podane ścieżki,
// więc dodanie kontrolera bez dopisania pakietu kończy się cichym 404, nie błędem startu.
@ComponentScan({"pl.decerto.higson.demo.motor", "pl.decerto.higson.demo.life",
	"pl.decerto.higson.demo.property"})
@EnableAutoConfiguration
public class ApplicationConfig implements WebMvcConfigurer {

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/").setViewName("forward:/index.html");
	}
}
