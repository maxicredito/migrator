package br.eti.clairton.migrator;

import static javax.enterprise.inject.spi.CDI.current;
import static liquibase.database.DatabaseFactory.getInstance;

import java.sql.Connection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.ResourceAccessor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class Migrator implements javax.enterprise.inject.spi.Extension {
	private final Logger logger = LogManager.getLogger(getClass().getName());

	private Liquibase liquibase;

	public void init(final @Observes AfterDeploymentValidation adv)
			throws Exception {
		logger.info("Iniciando migração do banco de dados");
		final Connection connection = current().select(Connection.class).get();
		Config config = current().select(Config.class).get();
		migrate(connection, config);
		new Inserter().run(connection, config);
	}

	public void migrate(final Connection connection, final Config config) {
		try {
			final DatabaseConnection jdbcConnection = new JdbcConnection(
					connection);
			final Database database = getInstance()
					.findCorrectDatabaseImplementation(jdbcConnection);
			final ResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor(
					getClass().getClassLoader());
			liquibase = new Liquibase(config.getChangelogPath(),
					resourceAccessor, database);
			connection.setAutoCommit(false);
			if (config.isDropAll()) {
				logger.info("Deletando objetos");
				liquibase.dropAll();
			}
			final String context = "";
			logger.info("Rodando changesets");
			liquibase.update(context);
			logger.info("Aplicado changesets");
			connection.commit();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
