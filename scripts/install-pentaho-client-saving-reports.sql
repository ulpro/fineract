INSERT INTO stretchy_report (report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report)
SELECT 'Client Saving Transactions', 'Pentaho', NULL, 'Savings', NULL, NULL, 0, 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM stretchy_report WHERE report_name = 'Client Saving Transactions'
);

INSERT INTO stretchy_report (report_name, report_type, report_subtype, report_category, report_sql, description, core_report, use_report, self_service_user_report)
SELECT 'Client Savings Summary', 'Pentaho', NULL, NULL, NULL, NULL, 0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM stretchy_report WHERE report_name = 'Client Savings Summary'
);
