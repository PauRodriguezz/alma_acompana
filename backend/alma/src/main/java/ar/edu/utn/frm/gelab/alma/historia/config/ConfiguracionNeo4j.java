package ar.edu.utn.frm.gelab.alma.historia.config;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;

import jakarta.persistence.EntityManagerFactory;

/**
 * Transacciones con JPA y Neo4j a la vez (contrato del grafo, sección 2, "Transacciones").
 *
 * <p>Spring Boot define un solo bean {@code transactionManager}, con {@code @ConditionalOnMissingBean}, tanto
 * para JPA como para Neo4j. Si solo se agregara el de Neo4j, el de JPA dejaría de crearse y todos los
 * {@code @Transactional} sin calificar del proyecto pasarían al de Neo4j sin avisar. Por eso se declaran
 * <b>los dos</b>: el de JPA con el nombre {@code transactionManager} y {@code @Primary} (el resto del proyecto
 * sigue usándolo sin cambios) y el de Neo4j con el nombre {@code neo4jTransactionManager}, que usa solo el
 * módulo de historia con {@code @Transactional("neo4jTransactionManager")}.
 *
 * <p>{@code @EnableNeo4jRepositories} fija el manejador y el paquete de los repositorios de Neo4j. Al
 * declararlo, Spring Boot deja de configurar esos repositorios por su cuenta.
 */
@Configuration
@EnableNeo4jRepositories(
        basePackages = "ar.edu.utn.frm.gelab.alma.historia.repository",
        transactionManagerRef = ConfiguracionNeo4j.MANEJADOR_NEO4J)
public class ConfiguracionNeo4j {

    public static final String MANEJADOR_NEO4J = "neo4jTransactionManager";

    /** El de siempre: el que usa todo {@code @Transactional} sin calificar. */
    @Bean
    @Primary
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(MANEJADOR_NEO4J)
    public Neo4jTransactionManager neo4jTransactionManager(Driver driver,
            DatabaseSelectionProvider databaseSelectionProvider) {
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}
