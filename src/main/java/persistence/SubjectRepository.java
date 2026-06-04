package persistence;

import model.Subject;

public class SubjectRepository extends GenericRepository<Subject> {
    public SubjectRepository() {
        super(Subject.class);
    }
}
