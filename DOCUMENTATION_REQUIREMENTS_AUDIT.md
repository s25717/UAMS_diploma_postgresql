# MAS Documentation Requirements Audit

The SharePoint document link requires Microsoft login, so the original online document could not be inspected directly in this session.

Instead, a complete corrected documentation draft was generated locally:

`MAS_University_Academic_Management_System_Documentation.docx`

## Requirement Coverage

| Requirement | Status | Where covered |
|---|---:|---|
| 4.1.2 User stories | Covered | Section 2 |
| 4.1.3 Functional requirements/use cases | Covered | Sections 3 and 5 |
| 4.1.3 Non-functional requirements | Covered | Section 4 |
| 4.1.4 Analytical class diagram | Covered | Section 9 |
| 4.1.5 Non-trivial use case with referenced use case | Covered | Section 6.1, Generate and export attendance report |
| Additional non-trivial use case | Covered | Section 6.2, Mark attendance |
| 4.1.5 Activity diagram for selected use case | Covered | Sections 7 and 8 |
| 4.1.6 GUI design | Covered | Section 12 |
| 4.1.7 Dynamic analysis with activity/state diagrams | Covered | Sections 7, 8, 9, and 13 |
| 4.1.8 Design and implementation decisions | Covered | Section 14 |
| 4.1.9 Design class diagram | Covered | Section 10 |
| 4.1.10 Readable/labeled diagrams | Covered | Diagrams embedded as rendered images; review visually before final PDF export |
| 4.1.12 Required documentation elements | Covered | Sections 2-14 and checklist section |
| 4.2.1 Complete class structure | Covered | Sections 9, 10, 16 |
| 4.2.2 Methods for selected use case | Covered | Sections 6, 13, 16 |
| 4.2.3 GUI for selected use case | Covered | Section 12 |
| 4.2.4 GUI association with target multiplicity many | Covered | Section 12 and analytical diagram: StudentGroup 1 -> 0..* Student |
| 4.2.5 Sample data | Covered | Sections 16 and 17 |
| 4.2.6 GUI usability | Covered | Sections 12 and 16 |
| 4.2.7 Persistence | Covered | Sections 14, 15, 16 |
| 4.2.8 Avoid obvious comments | Covered | Section 16 |
| 4.2.10 Java implementation | Covered | Title metadata and Sections 15-16 |

## Important Defence-Safe Points Added

- The actor model uses only `Student`, `Teacher`, `Administrator`, and `SystemScheduler`.
- MAS requirement 4.2.4 is explicitly connected to `GroupStudentsAssociationView`.
- The analytical class diagram includes:
  - inheritance
  - multi-value attribute
  - many-to-many association
  - association with target multiplicity many
  - association class
  - composition
  - qualified association
- The first non-trivial use case is `Generate and export attendance report`.
- The second non-trivial use case is `Mark attendance`.
- The use case relation `Export attendance report extends Generate attendance report` is explicitly described.
- The documentation now includes richer role-grouped user stories and an explicit actor table.
- The scope section now explains what is inside and outside the project scope.
- The GUI section now documents role-based navigation, class meeting details, attendance marking, reports, and MAS 4.2.4.
- A defence demonstration scenario was added for Student, Teacher, and Administrator.
- Dynamic analysis implications are mapped back to implementation/design changes.
- The implementation section states JavaFX, JPA/Hibernate, Maven, H2, manual repositories/services, and no Spring.

## Remaining Manual Step

Open the generated DOCX in Word, export it to PDF, and save it using the required course filename:

`MAS_Group_Lastname_Firstname_StudentNo.pdf`

The local renderer could not complete visual QA because LibreOffice/`soffice` is not installed in this environment. The DOCX was structurally validated instead.
