# Attendance & LOP rules (Stage 5)

**Working week:** Mon-Fri (same as leave). Weekends and rows in `holiday` are non-working and never LOP.
**Permissions:** Employee - own check-in/out and history. Manager - record/correct direct reports (back-date limit), view team. HR - record anyone, CSV import, holidays, policy, month lock/unlock.
**Duplicates:** one row per employee per date (`uq_att_emp_date` + pre-check); imports report duplicates per row.
**Manual statuses:** PRESENT, HALF_DAY, ABSENT. No entries for weekends/holidays, future dates, dates outside employment, locked months, or dates covered by a full-day approved leave.

| Situation (working day) | Present | Leave | LOP |
|---|---|---|---|
| PRESENT record | 1 | - (leave ignored, flagged) | 0 |
| HALF_DAY record | 0.5 | other half: approved leave if any | other half if not covered by leave |
| Approved paid leave, no attendance | 0 | 1 (or 0.5) | 0 (uncovered remainder = absent) |
| Approved unpaid leave (e.g. LOP type) | 0 | unpaid | 1 (or 0.5) |
| ABSENT record, no leave | 0 | - | 1 |
| No record, past day, no leave | 0 | - | 1 if `MISSING_DAY_TREATMENT=ABSENT`, else ignored |
| Today (no record) / future | pending | - | 0 |

Approved leave always overrides an ABSENT record. Payable days = working days - LOP.

**Policy keys** (table `attendance_policy`, editable by HR): `MISSING_DAY_TREATMENT` (ABSENT/IGNORE), `LATE_AFTER_TIME`, `LATES_PER_HALF_DAY_LOP` (N late marks = 0.5 LOP, 0 = off), `STANDARD_WORK_MINUTES` (overtime threshold), `BACKDATE_LIMIT_DAYS` (managers).
**Month lock:** HR lock sets `attendance.is_locked` for the month and blocks new entries; reserved for payroll processing.
**Upgrade from v4:** re-run `database/schema.sql` then `database/seed.sql` (both idempotent).
