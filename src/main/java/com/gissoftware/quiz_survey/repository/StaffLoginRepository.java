package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.dto.StaffLastLoginDTO;
import com.gissoftware.quiz_survey.dto.StaffLoginCountDTO;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StaffLoginRepository {

    private final JdbcTemplate jdbcTemplate;

    public Page<StaffLastLoginDTO> getLastLogins(
            String search,
            String type,
            Integer day,
            Integer month,
            Integer year,
            String date,
            int page,
            int size
    ) {

        StringBuilder where = new StringBuilder("""
            FROM user_login_table l
            JOIN (
                SELECT DISTINCT ON (emp_id)
                    emp_id,
                    name,
                    region
                FROM user_master_table
                ORDER BY emp_id, year DESC, quarter DESC
            ) u ON u.emp_id = l.emp_id

            WHERE l.login = (
                SELECT MAX(l2.login)
                FROM user_login_table l2
                WHERE l2.emp_id = l.emp_id
            )
            """);

        List<Object> params = new ArrayList<>();

        // Search
        if (search != null && !search.trim().isEmpty()) {

            where.append("""
                AND (
                    LOWER(l.emp_id) LIKE ?
                    OR LOWER(u.name) LIKE ?
                )
                """);

            String searchPattern =
                    "%" + search.trim().toLowerCase() + "%";

            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Type
        if ("CSR".equalsIgnoreCase(type)) {

            where.append(" AND u.region <> 'CCenter' ");

        } else if ("CALL_CENTER".equalsIgnoreCase(type)
                || "CALL CENTER".equalsIgnoreCase(type)) {

            where.append(" AND u.region = 'CCenter' ");
        }

        // Exact date
        if (date != null && !date.trim().isEmpty()) {

            where.append(" AND l.login::date = ?::date ");

            params.add(date.trim());
        }

        // Day
        if (day != null) {

            where.append(" AND EXTRACT(DAY FROM l.login) = ? ");

            params.add(day);
        }

        // Month
        if (month != null) {

            where.append(" AND EXTRACT(MONTH FROM l.login) = ? ");

            params.add(month);
        }

        // Year
        if (year != null) {

            where.append(" AND EXTRACT(YEAR FROM l.login) = ? ");

            params.add(year);
        }

        // Count
        String countSql = "SELECT COUNT(*) " + where;

        Long total = jdbcTemplate.queryForObject(
                countSql,
                params.toArray(),
                Long.class
        );

        // Data
        String dataSql = """
            SELECT
                l.emp_id AS staff_id,
                u.name,
                l.login::date AS date,
                l.login::time AS time
            """ + where + """
            ORDER BY l.login DESC
            LIMIT ? OFFSET ?
            """;

        List<Object> dataParams = new ArrayList<>(params);

        dataParams.add(size);
        dataParams.add(page * size);

        List<StaffLastLoginDTO> results =
                jdbcTemplate.query(
                        dataSql,
                        dataParams.toArray(),
                        (rs, rowNum) -> new StaffLastLoginDTO(
                                rs.getString("staff_id"),
                                rs.getString("name"),
                                rs.getDate("date")
                                        .toLocalDate()
                                        .toString(),
                                rs.getTime("time")
                                        .toLocalTime()
                                        .toString()
                        )
                );

        return new PageImpl<>(
                results,
                PageRequest.of(page, size),
                total != null ? total : 0
        );
    }

    public Page<StaffLoginCountDTO> getLoginCounts(
            String search,
            String type,
            Integer day,
            Integer month,
            Integer year,
            String date,
            int page,
            int size
    ) {

        StringBuilder where = new StringBuilder("""
        FROM user_login_table l
        JOIN (
            SELECT DISTINCT ON (emp_id)
                emp_id,
                name,
                region
            FROM user_master_table
            ORDER BY emp_id, year DESC, quarter DESC
        ) u ON u.emp_id = l.emp_id
        WHERE 1 = 1
        """);

        List<Object> params = new ArrayList<>();

        // Search
        if (search != null && !search.trim().isEmpty()) {
            where.append("""
            AND (
                LOWER(l.emp_id) LIKE ?
                OR LOWER(u.name) LIKE ?
            )
            """);

            String searchPattern =
                    "%" + search.trim().toLowerCase() + "%";

            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Type
        if ("CSR".equalsIgnoreCase(type)) {
            where.append(" AND u.region <> 'CCenter' ");
        } else if ("CALL_CENTER".equalsIgnoreCase(type)
                || "CALL CENTER".equalsIgnoreCase(type)) {
            where.append(" AND u.region = 'CCenter' ");
        }

        // Date
        if (date != null && !date.trim().isEmpty()) {

            where.append(" AND l.login::date = ?::date ");
            params.add(date.trim());

        } else {

            if (day != null) {
                where.append(" AND EXTRACT(DAY FROM l.login) = ? ");
                params.add(day);
            }

            if (month != null) {
                where.append(" AND EXTRACT(MONTH FROM l.login) = ? ");
                params.add(month);
            }

            if (year != null) {
                where.append(" AND EXTRACT(YEAR FROM l.login) = ? ");
                params.add(year);
            }
        }

        // Total staff count
        String countSql = """
        SELECT COUNT(*)
        FROM (
            SELECT l.emp_id
        """ + where + """
            GROUP BY l.emp_id
        ) counted_staff
        """;

        Long total = jdbcTemplate.queryForObject(
                countSql,
                params.toArray(),
                Long.class
        );

        // Login count per staff
        String dataSql = """
        SELECT
            l.emp_id AS staff_id,
            u.name AS username,
            COUNT(*) AS login_count
        """ + where + """
        GROUP BY l.emp_id, u.name
        ORDER BY login_count DESC, u.name ASC
        LIMIT ? OFFSET ?
        """;

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add(page * size);

        List<StaffLoginCountDTO> results =
                jdbcTemplate.query(
                        dataSql,
                        dataParams.toArray(),
                        (rs, rowNum) -> new StaffLoginCountDTO(
                                rs.getString("staff_id"),
                                rs.getString("username"),
                                rs.getLong("login_count")
                        )
                );

        return new PageImpl<>(
                results,
                PageRequest.of(page, size),
                total != null ? total : 0
        );
    }
}