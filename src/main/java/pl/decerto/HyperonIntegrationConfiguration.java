package pl.decerto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import pl.decerto.hyperon.runtime.core.HyperonEngine;
import pl.decerto.hyperon.runtime.core.HyperonEngineFactory;
import pl.decerto.hyperon.runtime.profiler.jdbc.proxy.DataSourceProxy;
import pl.decerto.hyperon.runtime.sql.DialectRegistry;
import pl.decerto.hyperon.runtime.sql.DialectTemplate;

import static java.util.stream.Stream.of;

@Configuration
@PropertySource(value = {"classpath:hyperon-demo-app.properties", "file:${user.home}/conf/hyperon-demo-app.properties"}, ignoreResourceNotFound = true)
public class HyperonIntegrationConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(HyperonIntegrationConfiguration.class);

	private final Environment env;

	private final Boolean hyperonDevMode;

	private final String hyperonDevUser;

	@Autowired
	public HyperonIntegrationConfiguration(Environment env, @Value("${hyperon.dev.mode}") Boolean hyperonDevMode,
										   @Value("${hyperon.dev.user}") String hyperonDevUser) {
		this.env = env;
		this.hyperonDevMode = hyperonDevMode;
		this.hyperonDevUser = hyperonDevUser;
	}

	@Bean
	public DialectRegistry getDialectRegistry() {
		DialectRegistry registry = new DialectRegistry();
		registry.setDialect(env.getProperty("hyperon.database.dialect"));
		return registry;
	}

	@Bean
	public DialectTemplate getDialectTemplate() {
		return getDialectRegistry().create();
	}

	@Bean(destroyMethod = "close")
	public HikariDataSource getDataSource() {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setUsername(env.getProperty("hyperon.database.username"));
		dataSource.setPassword(env.getProperty("hyperon.database.password"));
		dataSource.setJdbcUrl(env.getProperty("hyperon.database.url"));
		dataSource.setDriverClassName(getDialectTemplate().getJdbcDriverClassName());
		return dataSource;
	}

	@Bean
	public DataSource getDataSourceProxy() {
		DataSourceProxy dsp = new DataSourceProxy();
		dsp.setDataSource(getDataSource());
		return dsp;
	}

	@Bean(destroyMethod = "destroy")
	public HyperonEngineFactory getHyperonEngineFactory() {
		HyperonEngineFactory hyperonEngineFactory = new HyperonEngineFactory();
		hyperonEngineFactory.setDataSource(getDataSourceProxy());

		if (BooleanUtils.toBoolean(hyperonDevMode)) {
			LOG.info("Hyperon factory set in developer mode!");
			hyperonEngineFactory.setDeveloperMode(true);
			String hyperonDevUser = Objects.requireNonNull(this.hyperonDevUser, "Hyperon dev user not supplied");
			hyperonEngineFactory.setUsername(hyperonDevUser);
		}

		List<String> paramsToPrefetch = getParametersToPrefetch();
		hyperonEngineFactory.setParamsToPrefetch(paramsToPrefetch);

		return hyperonEngineFactory;
	}

	@Bean
	public HyperonEngine getHyperonEngine(HyperonEngineFactory hyperonEngineFactory) {
		return hyperonEngineFactory.create();
	}

	private List<String> getParametersToPrefetch() {
		return of(PrefetchParameters.values())
				.map(PrefetchParameters::getCode)
				.collect(Collectors.toList());
	}

}
