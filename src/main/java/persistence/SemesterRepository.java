package persistence;

import model.Semester;

public class SemesterRepository extends GenericRepository<Semester> {
    public SemesterRepository() {
        super(Semester.class);
    }
}
