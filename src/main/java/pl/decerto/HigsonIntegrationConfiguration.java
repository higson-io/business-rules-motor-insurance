package pl.decerto;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import io.higson.runtime.profiler.jdbc.proxy.DataSourceProxy;

/**
 * Higson runtime (engine, data source, dialect, dev mode, instance name, diagnostic profiler)
 * is auto-configured by {@code spring-boot-starter-higson-runtime}, driven by {@code higson.*}
 * properties loaded below.
 *
 * <p>The starter builds a plain data source, so the only piece we still wire manually is the
 * Higson {@link DataSourceProxy}: it wraps the starter's {@code higsonDataSource} bean so that
 * JDBC-level profiling statistics keep flowing to {@code /profilers/jdbc}.
 */
@Configuration
@PropertySource(value = {"classpath:higson-demo-app.properties", "file:${user.home}/conf/higson-demo-app.properties"}, ignoreResourceNotFound = true)
public class HigsonIntegrationConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(HigsonIntegrationConfiguration.class);

	private static final String HIGSON_DATASOURCE_BEAN = "higsonDataSource";

	/**
	 * Wraps the starter-provided {@code higsonDataSource} in a Higson {@link DataSourceProxy}
	 * before it is injected into the engine, preserving JDBC profiling (/profilers/jdbc).
	 * Declared {@code static} so it is registered early, before the beans it post-processes.
	 */
	@Bean
	public static BeanPostProcessor higsonJdbcProfilerDataSourceProxy() {
		return new BeanPostProcessor() {
			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
				if (HIGSON_DATASOURCE_BEAN.equals(beanName)
						&& bean instanceof DataSource dataSource
						&& !(bean instanceof DataSourceProxy)) {
					LOG.info("Wrapping Higson '{}' in DataSourceProxy to enable JDBC profiling (/profilers/jdbc)", beanName);
					DataSourceProxy proxy = new DataSourceProxy();
					proxy.setDataSource(dataSource);
					return proxy;
				}
				return bean;
			}
		};
	}

}
