package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Locale;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Version 2 (el nombre de clase no sigue el patron V2__ porque Checkstyle no admite guiones bajos: la version la declara getVersion): corrige el desfase entre V1 y el modelo y agrega las columnas de venta de animales (US-30).
 * <ul>
 *   <li>V1 creo {@code veterinary_assignments} con ids BIGINT y sin {@code assigned_at}, pero la entidad usa UUID.
 *       Si la tabla sigue con ese esquema antiguo se RENOMBRA a {@code veterinary_assignments_legacy_bigint} (no se
 *       borra ningun dato) y se crea la tabla correcta. Si ya tiene ids UUID no se toca.</li>
 *   <li>Si la tabla {@code animals} existe, agrega {@code sale_price} y {@code sold_on} cuando faltan. En una base
 *       nueva {@code animals} aun no existe (la crea Hibernate despues) y no hay nada que hacer.</li>
 * </ul>
 * Es una migracion Java (y no SQL) porque debe inspeccionar el esquema actual para no tocar bases ya correctas, y
 * asi corre igual en PostgreSQL y en H2.
 */
public class AlignVeterinaryAssignmentsMigration implements JavaMigration {

    private static final String ASSIGNMENTS = "veterinary_assignments";

    @Override
    public MigrationVersion getVersion() {
        return MigrationVersion.fromVersion("2");
    }

    @Override
    public String getDescription() {
        return "Align veterinary assignments and animal sale";
    }

    @Override
    public Integer getChecksum() {
        return null;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        if (hasLegacyBigintId(connection)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP INDEX IF EXISTS idx_vet_assignments_lookup");
                statement.execute("ALTER TABLE " + ASSIGNMENTS + " RENAME TO " + ASSIGNMENTS + "_legacy_bigint");
                statement.execute("CREATE TABLE " + ASSIGNMENTS + " ("
                        + "id UUID PRIMARY KEY, "
                        + "veterinarian_id UUID NOT NULL, "
                        + "client_id UUID NOT NULL, "
                        + "assigned_at TIMESTAMP WITH TIME ZONE NOT NULL, "
                        + "status VARCHAR(255) NOT NULL)");
                statement.execute("CREATE INDEX idx_vet_assignments_lookup "
                        + "ON " + ASSIGNMENTS + "(veterinarian_id, client_id, status)");
            }
        }
        if (tableExists(connection, "animals")) {
            addColumnIfMissing(connection, "animals", "sale_price", "NUMERIC(12,2)");
            addColumnIfMissing(connection, "animals", "sold_on", "DATE");
        }
    }

    private static boolean hasLegacyBigintId(Connection connection) throws SQLException {
        var metadata = connection.getMetaData();
        try (var columns = metadata.getColumns(connection.getCatalog(), connection.getSchema(),
                matchCase(metadata, ASSIGNMENTS), matchCase(metadata, "id"))) {
            if (!columns.next()) {
                return false;
            }
            var type = columns.getInt("DATA_TYPE");
            return type == Types.BIGINT || type == Types.INTEGER || type == Types.SMALLINT;
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        var metadata = connection.getMetaData();
        try (var tables = metadata.getTables(connection.getCatalog(), connection.getSchema(),
                matchCase(metadata, table), new String[] {"TABLE"})) {
            return tables.next();
        }
    }

    private static void addColumnIfMissing(Connection connection, String table, String column, String type)
            throws SQLException {
        var metadata = connection.getMetaData();
        try (var columns = metadata.getColumns(connection.getCatalog(), connection.getSchema(),
                matchCase(metadata, table), matchCase(metadata, column))) {
            if (columns.next()) {
                return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    /** PostgreSQL guarda los identificadores sin comillas en minusculas y H2 en mayusculas. */
    private static String matchCase(DatabaseMetaData metadata, String identifier) throws SQLException {
        return metadata.storesUpperCaseIdentifiers() ? identifier.toUpperCase(Locale.ROOT)
                : identifier.toLowerCase(Locale.ROOT);
    }
}
