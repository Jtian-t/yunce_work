package com.recruit.platform.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class LegacySchemaPatchRunner implements ApplicationRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("candidate", "jd_summary", "alter table candidate add column jd_summary text null");
        ensureTextColumn("candidate", "skills_summary", "alter table candidate modify column skills_summary text null");
        ensureTextColumn("candidate", "project_summary", "alter table candidate modify column project_summary text null");
        ensureTextColumn("candidate", "jd_summary", "alter table candidate modify column jd_summary text null");
        ensureTextColumn("agent_result", "parsed_education", "alter table agent_result modify column parsed_education text null");
        ensureTextColumn("agent_result", "parsed_experience", "alter table agent_result modify column parsed_experience text null");
        ensureTextColumn("agent_result", "parsed_skills_summary", "alter table agent_result modify column parsed_skills_summary longtext null");
        ensureTextColumn("agent_result", "parsed_project_summary", "alter table agent_result modify column parsed_project_summary longtext null");
        ensureTextColumn("agent_job", "request_payload_json", "alter table agent_job modify column request_payload_json longtext not null");
        ensureTextColumn("agent_job", "last_error", "alter table agent_job modify column last_error text null");
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        try (Connection connection = dataSource.getConnection()) {
            if (hasColumn(connection, tableName, columnName)) {
                return;
            }
            log.info("Missing column {}.{} detected, applying schema patch.", tableName, columnName);
            jdbcTemplate.execute(alterSql);
        } catch (Exception exception) {
            log.warn("Failed to apply schema patch for {}.{}: {}", tableName, columnName, exception.getMessage());
        }
    }

    private void ensureTextColumn(String tableName, String columnName, String alterSql) {
        try (Connection connection = dataSource.getConnection()) {
            if (!hasColumn(connection, tableName, columnName)) {
                return;
            }
            log.info("Ensuring {}.{} uses TEXT-compatible storage.", tableName, columnName);
            jdbcTemplate.execute(alterSql);
        } catch (Exception exception) {
            log.warn("Failed to normalize column {}.{}: {}", tableName, columnName, exception.getMessage());
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet upperCaseColumns = metaData.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return upperCaseColumns.next();
        }
    }
}
