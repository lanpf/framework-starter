package com.cloud.framework.starter.persistence.jpa.naming;

import com.cloud.framework.persistence.naming.PersistenceTableNaming;
import lombok.AllArgsConstructor;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

@AllArgsConstructor
public class PersistencePhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {
    private final String tablePrefix;
    private final String tableSuffix;

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        Identifier tableName = super.toPhysicalTableName(logicalName, jdbcEnvironment);
        if (tableName == null || tableName.getText() == null) {
            return tableName;
        }
        String physicalName = PersistenceTableNaming.apply(
                tableName.getText(),
                tablePrefix,
                toJpaTableSuffix(tableSuffix)
        );
        return Identifier.toIdentifier(physicalName, tableName.isQuoted());
    }

    private static String toJpaTableSuffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return suffix;
        }
        StringBuilder result = new StringBuilder(suffix.length());
        for (int i = 0; i < suffix.length(); i++) {
            char current = suffix.charAt(i);
            if (current == '_' || current == '-' || Character.isWhitespace(current)) {
                continue;
            }
            result.append(Character.toLowerCase(current));
        }
        return result.toString();
    }
}
