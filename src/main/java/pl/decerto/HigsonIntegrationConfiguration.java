package pl.decerto;

import com.zaxxer.hikari.HikariDataSource;

import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import io.higson.runtime.core.HigsonEngine;
import io.higson.runtime.core.HigsonEngineFactory;
import io.higson.runtime.profiler.jdbc.proxy.DataSourceProxy;
import io.higson.runtime.sql.DialectRegistry;
import io.higson.runtime.sql.DialectTemplate;

@Configuration
@PropertySource(value = {"classpath:higson-demo-app.properties", "file:${user.home}/conf/higson-demo-app.properties"}, ignoreResourceNotFound = true)
public class HigsonIntegrationConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(HigsonIntegrationConfiguration.class);

	private final Environment env;

	private final Boolean higsonDevMode;

	private final String higsonDevUser;

	@Autowired
	public HigsonIntegrationConfiguration(Environment env, @Value("${higson.dev.mode}") Boolean higsonDevMode,
										   @Value("${higson.dev.user}") String higsonDevUser) {
		this.env = env;
		this.higsonDevMode = higsonDevMode;
		this.higsonDevUser = higsonDevUser;
	}

	@Bean
	public DialectRegistry getDialectRegistry() {
		DialectRegistry registry = new DialectRegistry();
		registry.setDialect(env.getProperty("higson.database.dialect"));
		return registry;
	}

	@Bean
	public DialectTemplate getDialectTemplate() {
		return getDialectRegistry().create();
	}

	@Bean(destroyMethod = "close")
	public HikariDataSource getDataSource() {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setUsername(env.getProperty("higson.database.username"));
		dataSource.setPassword(env.getProperty("higson.database.password"));
		dataSource.setJdbcUrl(env.getProperty("higson.database.url"));
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
	public HigsonEngineFactory getHigsonEngineFactory() {
		HigsonEngineFactory higsonEngineFactory = new HigsonEngineFactory();
		higsonEngineFactory.setDataSource(getDataSourceProxy());

		if (BooleanUtils.toBoolean(higsonDevMode)) {
			LOG.info("Higson factory set in developer mode!");
			higsonEngineFactory.setDeveloperMode(true);
			String higsonDevUser = Objects.requireNonNull(this.higsonDevUser, "Higson dev user not supplied");
			higsonEngineFactory.setUsername(higsonDevUser);
		}

		return higsonEngineFactory;
	}

	@Bean
	public HigsonEngine getHigsonEngine(HigsonEngineFactory higsonEngineFactory) {
		higsonEngineFactory.setValidateFunctionArgumentsDataTypes(true);
		return higsonEngineFactory.create();
	}

}
