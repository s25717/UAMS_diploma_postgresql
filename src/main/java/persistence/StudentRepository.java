package persistence;

import model.Student;

public class StudentRepository extends GenericRepository<Student> {
    public StudentRepository() {
        super(Student.class);
    }
}
