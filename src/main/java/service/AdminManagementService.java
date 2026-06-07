package service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.persistence.EntityManager;
import model.Administrator;
import model.Attendance;
import model.ClassMeeting;
import model.Field;
import model.Notification;
import model.Person;
import model.ScheduledNotificationTask;
import model.Semester;
import model.SemesterFieldSubject;
import model.Student;
import model.StudentGroup;
import model.Subject;
import model.Teacher;
import model.WeeklyScheduleEntry;
import model.value.BirthDate;
import persistence.JpaTransactionManager;
import persistence.PersonRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class AdminManagementService {
    private final JpaTransactionManager transactionManager = new JpaTransactionManager();
    private final PersonRepository personRepository = new PersonRepository();
    private final PasswordService passwordService = new PasswordService();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public Student createStudent(String name, String surname, LocalDate birthDate, String email,
                                 String rawPassword, String studentNumber, Long groupId) {
        validateEmailAvailable(email, null);
        return transactionManager.execute(em -> {
            StudentGroup group = require(em.find(StudentGroup.class, groupId), "Group is required.");
            assertGroupHasCapacity(em, group.getId());
            Student student = new Student(name, surname, new BirthDate(birthDate), Set.of(email), studentNumber);
            student.setPasswordHash(passwordService.hash(rawPassword));
            student.setPrimaryEmail(email);
            student.setGroup(group);
            validateEntity(student);
            em.persist(student);
            return student;
        });
    }

    public Teacher createTeacher(String name, String surname, LocalDate birthDate, String email,
                                 String rawPassword, String employeeNumber, Set<Long> subjectIds) {
        validateEmailAvailable(email, null);
        return transactionManager.execute(em -> {
            Teacher teacher = new Teacher(name, surname, new BirthDate(birthDate), Set.of(email), employeeNumber);
            teacher.setPasswordHash(passwordService.hash(rawPassword));
            teacher.setPrimaryEmail(email);
            for (Long subjectId : subjectIds) {
                teacher.addQualifiedSubject(require(em.find(Subject.class, subjectId), "Subject not found."));
            }
            validateEntity(teacher);
            em.persist(teacher);
            return teacher;
        });
    }

    public Administrator createAdministrator(String name, String surname, LocalDate birthDate, String email,
                                             String rawPassword, String employeeNumber) {
        validateEmailAvailable(email, null);
        return transactionManager.execute(em -> {
            Administrator administrator = new Administrator(name, surname, new BirthDate(birthDate), Set.of(email), employeeNumber);
            administrator.setPasswordHash(passwordService.hash(rawPassword));
            administrator.setPrimaryEmail(email);
            validateEntity(administrator);
            em.persist(administrator);
            return administrator;
        });
    }

    public void updatePersonBasics(Long personId, String name, String surname, String email, String rawPassword) {
        transactionManager.executeVoid(em -> {
            Person person = require(em.find(Person.class, personId), "Select a user first.");
            person.setName(name);
            person.setSurname(surname);
            String normalizedEmail = email == null ? null : email.trim();
            if (normalizedEmail != null && !normalizedEmail.isBlank()) {
                validateEmailAvailable(normalizedEmail, personId);
                String storedEmail = person.getEmails().stream()
                        .filter(existing -> existing.equalsIgnoreCase(normalizedEmail))
                        .findFirst()
                        .orElse(null);
                if (storedEmail == null) {
                    if (person.getEmails().size() >= 3) {
                        throw new IllegalArgumentException("Each person can have at most 3 emails.");
                    }
                    person.getEmails().add(normalizedEmail);
                    storedEmail = normalizedEmail;
                }
                person.setPrimaryEmail(storedEmail);
            }
            if (rawPassword != null && !rawPassword.isBlank()) {
                person.setPasswordHash(passwordService.hash(rawPassword));
            }
            validateEntity(person);
        });
    }

    public void updatePersonBasics(Long personId, String name, String surname, String rawPassword) {
        updatePersonBasics(personId, name, surname, null, rawPassword);
    }

    public void deletePerson(Long personId, Long currentAdminId) {
        if (personId != null && personId.equals(currentAdminId)) {
            throw new IllegalArgumentException("You cannot delete your own logged-in administrator account.");
        }
        transactionManager.executeVoid(em -> {
            Person person = require(em.find(Person.class, personId), "Select a user first.");
            assertNoPersonHistory(em, personId);
            if (person instanceof Teacher teacher) {
                teacher.getQualifiedSubjects().forEach(subject -> subject.getQualifiedTeachers().remove(teacher));
                teacher.getQualifiedSubjects().clear();
            }
            if (person instanceof Student student) {
                student.setGroup(null);
            }
            em.remove(person);
        });
    }

    public Field createField(String name) {
        return transactionManager.execute(em -> {
            Field field = new Field(name);
            validateEntity(field);
            em.persist(field);
            return field;
        });
    }

    public void updateField(Long id, String name) {
        transactionManager.executeVoid(em -> {
            Field field = require(em.find(Field.class, id), "Select a field first.");
            field.setName(name);
            validateEntity(field);
        });
    }

    public void deleteField(Long id) {
        transactionManager.executeVoid(em -> {
            Field field = require(em.find(Field.class, id), "Select a field first.");
            Long semesterCount = em.createQuery("""
                    select count(sem)
                    from Semester sem
                    join sem.fields field
                    where field.id = :fieldId
                    """, Long.class)
                    .setParameter("fieldId", id)
                    .getSingleResult();
            if (semesterCount > 0 || count(em, StudentGroup.class, "field.id", id) > 0) {
                throw new IllegalArgumentException("Cannot delete a field used by semesters or groups.");
            }
            em.remove(field);
        });
    }

    public Semester createSemester(int number, LocalDate startDate, LocalDate endDate, Set<Long> fieldIds) {
        return transactionManager.execute(em -> {
            Set<Field> fields = requireFields(em, fieldIds);
            Semester semester = new Semester(number);
            semester.setStartDate(startDate);
            semester.setEndDate(endDate);
            fields.forEach(semester::addField);
            validateDates(startDate, endDate);
            validateEntity(semester);
            em.persist(semester);
            return semester;
        });
    }

    public void updateSemester(Long id, int number, LocalDate startDate, LocalDate endDate, Set<Long> fieldIds) {
        transactionManager.executeVoid(em -> {
            Semester semester = require(em.find(Semester.class, id), "Select a semester first.");
            Set<Field> fields = requireFields(em, fieldIds);
            Set<Long> removedFieldIds = new HashSet<>();
            for (Field existing : semester.getFields()) {
                if (fields.stream().noneMatch(field -> field.getId().equals(existing.getId()))) {
                    removedFieldIds.add(existing.getId());
                }
            }
            if (!removedFieldIds.isEmpty()) {
                Long usages = em.createQuery("""
                        select count(g)
                        from StudentGroup g
                        where g.semester.id = :semesterId
                        and g.field.id in :fieldIds
                        """, Long.class)
                        .setParameter("semesterId", id)
                        .setParameter("fieldIds", removedFieldIds)
                        .getSingleResult();
                Long curriculumUsages = em.createQuery("""
                        select count(c)
                        from SemesterFieldSubject c
                        where c.semester.id = :semesterId
                        and c.field.id in :fieldIds
                        """, Long.class)
                        .setParameter("semesterId", id)
                        .setParameter("fieldIds", removedFieldIds)
                        .getSingleResult();
                if (usages > 0 || curriculumUsages > 0) {
                    throw new IllegalArgumentException("Cannot remove a field used by this semester's groups or curriculum.");
                }
            }
            semester.setNumber(number);
            semester.setStartDate(startDate);
            semester.setEndDate(endDate);
            new HashSet<>(semester.getFields()).forEach(semester::removeField);
            fields.forEach(semester::addField);
            validateDates(startDate, endDate);
            validateEntity(semester);
        });
    }

    public void deleteSemester(Long id) {
        transactionManager.executeVoid(em -> {
            Semester semester = require(em.find(Semester.class, id), "Select a semester first.");
            if (count(em, StudentGroup.class, "semester.id", id) > 0
                    || count(em, WeeklyScheduleEntry.class, "semester.id", id) > 0) {
                throw new IllegalArgumentException("Cannot delete a semester with groups or weekly schedules.");
            }
            em.remove(semester);
        });
    }

    public StudentGroup createGroup(String code, Long semesterId, Long fieldId, int maxSize) {
        return transactionManager.execute(em -> {
            Semester semester = require(em.find(Semester.class, semesterId), "Semester is required.");
            Field field = require(em.find(Field.class, fieldId), "Field is required.");
            assertSemesterContainsField(em, semesterId, fieldId);
            StudentGroup group = new StudentGroup(code, maxSize);
            group.setSemester(semester);
            group.setField(field);
            validateEntity(group);
            em.persist(group);
            return group;
        });
    }

    public void updateGroup(Long id, String code, Long semesterId, Long fieldId, int maxSize) {
        transactionManager.executeVoid(em -> {
            StudentGroup group = require(em.find(StudentGroup.class, id), "Select a group first.");
            assertSemesterContainsField(em, semesterId, fieldId);
            group.setCode(code);
            group.setSemester(require(em.find(Semester.class, semesterId), "Semester is required."));
            group.setField(require(em.find(Field.class, fieldId), "Field is required."));
            group.setMaxSize(maxSize);
            assertGroupSizeWithinLimit(em, id, maxSize);
            validateEntity(group);
        });
    }

    public void deleteGroup(Long id) {
        transactionManager.executeVoid(em -> {
            StudentGroup group = require(em.find(StudentGroup.class, id), "Select a group first.");
            if (count(em, Student.class, "group.id", id) > 0
                    || count(em, ClassMeeting.class, "group.id", id) > 0
                    || count(em, WeeklyScheduleEntry.class, "group.id", id) > 0) {
                throw new IllegalArgumentException("Cannot delete a group with students, class meetings, or weekly schedules.");
            }
            group.getSubjects().forEach(subject -> subject.getGroups().remove(group));
            group.getSubjects().clear();
            em.remove(group);
        });
    }

    public Subject createSubject(String name) {
        return transactionManager.execute(em -> {
            Subject subject = new Subject(name);
            validateEntity(subject);
            em.persist(subject);
            return subject;
        });
    }

    public void updateSubject(Long id, String name) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, id), "Select a subject first.");
            subject.setName(name);
            validateEntity(subject);
        });
    }

    public void deleteSubject(Long id) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, id), "Select a subject first.");
            if (count(em, ClassMeeting.class, "subject.id", id) > 0
                    || count(em, WeeklyScheduleEntry.class, "subject.id", id) > 0
                    || count(em, SemesterFieldSubject.class, "subject.id", id) > 0) {
                throw new IllegalArgumentException("Cannot delete a subject used by curriculum, class meetings, or weekly schedules.");
            }
            subject.getGroups().forEach(group -> group.getSubjects().remove(subject));
            subject.getGroups().clear();
            subject.getQualifiedTeachers().forEach(teacher -> teacher.getQualifiedSubjects().remove(subject));
            subject.getQualifiedTeachers().clear();
            em.remove(subject);
        });
    }

    public void assignSubjectToGroup(Long subjectId, Long groupId) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, subjectId), "Subject is required.");
            StudentGroup group = require(em.find(StudentGroup.class, groupId), "Group is required.");
            assertSubjectHasQualifiedTeacher(em, subjectId);
            if (!isSubjectAvailableInSemesterField(em, subjectId, group.getSemester().getId(), group.getField().getId())) {
                throw new IllegalArgumentException("Assign the subject to the group's semester and field before assigning it to the group.");
            }
            subject.addGroup(group);
        });
    }

    public void removeSubjectFromGroup(Long subjectId, Long groupId) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, subjectId), "Subject is required.");
            StudentGroup group = require(em.find(StudentGroup.class, groupId), "Group is required.");
            subject.removeGroup(group);
        });
    }

    public void assignSubjectToSemesterField(Long subjectId, Long semesterId, Long fieldId) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, subjectId), "Subject is required.");
            Semester semester = require(em.find(Semester.class, semesterId), "Semester is required.");
            Field field = require(em.find(Field.class, fieldId), "Field is required.");
            assertSemesterContainsField(em, semesterId, fieldId);
            assertSubjectHasQualifiedTeacher(em, subjectId);
            em.persist(semester.assignSubject(field, subject));
        });
    }

    public void removeSubjectFromSemesterField(Long subjectId, Long semesterId, Long fieldId) {
        transactionManager.executeVoid(em -> {
            Subject subject = require(em.find(Subject.class, subjectId), "Subject is required.");
            Semester semester = require(em.find(Semester.class, semesterId), "Semester is required.");
            Field field = require(em.find(Field.class, fieldId), "Field is required.");
            Long groupAssignments = em.createQuery("""
                    select count(g)
                    from StudentGroup g
                    join g.subjects s
                    where g.semester.id = :semesterId
                    and g.field.id = :fieldId
                    and s.id = :subjectId
                    """, Long.class)
                    .setParameter("semesterId", semesterId)
                    .setParameter("fieldId", fieldId)
                    .setParameter("subjectId", subjectId)
                    .getSingleResult();
            if (groupAssignments > 0) {
                throw new IllegalArgumentException("Cannot remove a curriculum subject while it is assigned to groups in that semester and field.");
            }
            semester.removeSubject(field, subject);
        });
    }

    private void assertNoPersonHistory(EntityManager em, Long personId) {
        if (count(em, Attendance.class, "student.id", personId) > 0
                || count(em, ClassMeeting.class, "teacher.id", personId) > 0
                || count(em, WeeklyScheduleEntry.class, "teacher.id", personId) > 0
                || count(em, Notification.class, "recipient.id", personId) > 0
                || count(em, ScheduledNotificationTask.class, "recipient.id", personId) > 0) {
            throw new IllegalArgumentException("Cannot delete a user with attendance, meetings, schedules, or notifications.");
        }
    }

    private void validateEmailAvailable(String email, Long personId) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (personRepository.emailExistsForAnotherPerson(email, personId)) {
            throw new IllegalArgumentException("Email is already used by another person: " + email);
        }
    }

    private void assertGroupHasCapacity(EntityManager em, Long groupId) {
        StudentGroup group = require(em.find(StudentGroup.class, groupId), "Group is required.");
        long currentSize = count(em, Student.class, "group.id", groupId);
        if (currentSize >= group.getMaxSize()) {
            throw new IllegalArgumentException("Group " + group.getCode() + " is full.");
        }
    }

    private void assertGroupSizeWithinLimit(EntityManager em, Long groupId, int maxSize) {
        long currentSize = count(em, Student.class, "group.id", groupId);
        if (currentSize > maxSize) {
            throw new IllegalArgumentException("Group already has " + currentSize + " students.");
        }
    }

    private boolean isSubjectAvailableInSemesterField(EntityManager em, Long subjectId, Long semesterId, Long fieldId) {
        Long count = em.createQuery("""
                select count(c)
                from SemesterFieldSubject c
                where c.semester.id = :semesterId
                and c.field.id = :fieldId
                and c.subject.id = :subjectId
                """, Long.class)
                .setParameter("semesterId", semesterId)
                .setParameter("fieldId", fieldId)
                .setParameter("subjectId", subjectId)
                .getSingleResult();
        return count > 0;
    }

    private Set<Field> requireFields(EntityManager em, Set<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            throw new IllegalArgumentException("Select at least one field for the semester.");
        }
        Set<Field> fields = new HashSet<>();
        for (Long fieldId : fieldIds) {
            fields.add(require(em.find(Field.class, fieldId), "Field not found: " + fieldId));
        }
        return fields;
    }

    private void assertSemesterContainsField(EntityManager em, Long semesterId, Long fieldId) {
        if (semesterId == null || fieldId == null) {
            throw new IllegalArgumentException("Semester and field are required.");
        }
        Long count = em.createQuery("""
                select count(sem)
                from Semester sem
                join sem.fields field
                where sem.id = :semesterId
                and field.id = :fieldId
                """, Long.class)
                .setParameter("semesterId", semesterId)
                .setParameter("fieldId", fieldId)
                .getSingleResult();
        if (count == 0) {
            throw new IllegalArgumentException("The selected field does not belong to the selected semester.");
        }
    }

    private void assertSubjectHasQualifiedTeacher(EntityManager em, Long subjectId) {
        Long count = em.createQuery("""
                select count(t)
                from Teacher t
                join t.qualifiedSubjects subject
                where subject.id = :subjectId
                """, Long.class)
                .setParameter("subjectId", subjectId)
                .getSingleResult();
        if (count < 1) {
            throw new IllegalArgumentException("Subject must have at least one qualified teacher before it can be used in a semester or group.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("Semester start date must be before end date.");
        }
    }

    private <T> T require(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private long count(EntityManager em, Class<?> entityClass, String propertyPath, Long id) {
        return em.createQuery("select count(e) from " + entityClass.getSimpleName() + " e where e." + propertyPath + " = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    private <T> void validateEntity(T entity) {
        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            StringBuilder message = new StringBuilder("Entity validation failed: ");
            for (ConstraintViolation<T> violation : violations) {
                message.append(violation.getPropertyPath())
                        .append(" ")
                        .append(violation.getMessage())
                        .append("; ");
            }
            throw new IllegalArgumentException(message.toString());
        }
    }
}
