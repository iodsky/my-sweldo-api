-- Migration to rename contribution tables to rate_table
-- This provides better clarity that these tables store configuration/rate data rather than actual employee contributions

-- ============================================================================
-- 1. Rename SSS Contribution table to SSS Rate Table
-- ============================================================================

-- Rename the table
ALTER TABLE sss_contribution RENAME TO sss_rate_table;

-- Update sequence if any (not applicable for UUID primary keys)
-- No indexes to rename since we're using simple queries

COMMENT ON TABLE sss_rate_table IS 'Stores SSS contribution rate configurations and salary brackets that change over time';

-- ============================================================================
-- 2. Rename Pag-IBIG Contribution table to Pag-IBIG Rate Table
-- ============================================================================

-- Rename the table
ALTER TABLE pagibig_contribution RENAME TO pagibig_rate_table;

COMMENT ON TABLE pagibig_rate_table IS 'Stores Pag-IBIG contribution rate configurations that change over time';

-- ============================================================================
-- 3. Rename PhilHealth Contribution table to PhilHealth Rate Table
-- ============================================================================

-- Rename the table
ALTER TABLE philhealth_contribution RENAME TO philhealth_rate_table;

COMMENT ON TABLE philhealth_rate_table IS 'Stores PhilHealth contribution rate configurations that change over time';

-- ============================================================================
-- Migration Complete
-- ============================================================================
-- All data has been preserved. Only table names have been changed.
-- Application code has been updated to use new entity names:
--   - SssContribution -> SssRateTable
--   - PagibigContribution -> PagibigRateTable
--   - PhilhealthContribution -> PhilhealthRateTable
