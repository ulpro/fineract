package org.apache.fineract.accounting.cemac.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.fineract.accounting.cemac.data.CemacCodeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CemacCodeReadPlatformServiceImpl implements CemacCodeReadPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CemacCodeReadPlatformServiceImpl(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final class CemacCodeMapper implements RowMapper<CemacCodeData> {

        public String schema() {
            return " id, cobac_code as cobacCode, pcemf_code as pcemfCode, libelle_cobac as libelleCobac, "
                    + " classe_pcemf as classePcemf, etat_sesame as etatSesame "
                    + " from ref_cobac_pcemf ";
        }

        @Override
        public CemacCodeData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String cobacCode = rs.getString("cobacCode");
            final String pcemfCode = rs.getString("pcemfCode");
            final String libelleCobac = rs.getString("libelleCobac");
            final Integer classePcemf = rs.getInt("classePcemf");
            final String etatSesame = rs.getString("etatSesame");

            return CemacCodeData.instance(id, cobacCode, pcemfCode, libelleCobac, rs.wasNull() ? null : classePcemf, etatSesame);
        }
    }

    @Override
    public Collection<CemacCodeData> retrieveAllCemacCodes() {
        final CemacCodeMapper rm = new CemacCodeMapper();
        final String sql = "select " + rm.schema() + " order by pcemf_code";
        return this.jdbcTemplate.query(sql, rm); // NOSONAR
    }
}
