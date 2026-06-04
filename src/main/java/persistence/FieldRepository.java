package persistence;

import model.Field;

public class FieldRepository extends GenericRepository<Field> {
    public FieldRepository() {
        super(Field.class);
    }
}
