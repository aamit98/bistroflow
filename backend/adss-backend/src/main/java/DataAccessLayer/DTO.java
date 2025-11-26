package DataAccessLayer;

public abstract class DTO {

    protected boolean isPersisted;
    private Controller controller;

    public DTO(Controller controller, boolean fromDB) {
        this.isPersisted = fromDB;
        this.controller = controller;
    }

    public abstract void persist() throws Exception;

    /**
     * Inserts a new record into the database.
     *
     * @param attributesValues The attribute values for the record.
     * @throws Exception If an unexpected error occurs while inserting.
     */
    protected void insert(Object[] attributesValues) throws Exception {
        if (!controller.insert(attributesValues)) {
            throw new Exception("An unexpected error occurred while inserting");
        }
    }

    /**
     * Updates a record in the database.
     *
     * @param identifiersValues The identifier values for the record.
     * @param varToUpdate The variable to update.
     * @param valueToUpdate The new value for the variable.
     * @throws Exception If an unexpected error occurs while updating.
     */
    protected void update(Object[] identifiersValues, String varToUpdate, Object valueToUpdate) throws Exception {
        if (!controller.update(identifiersValues, varToUpdate, valueToUpdate)) {
            throw new Exception("An unexpected error occurred while updating");
        }
    }

    /**
     * Deletes a record from the database.
     *
     * @param identifiersValues The identifier values for the record.
     * @throws Exception If an unexpected error occurs while deleting.
     */
    protected void delete(Object[] identifiersValues) throws Exception {
        if (!controller.delete(identifiersValues)) {
            throw new Exception("An unexpected error occurred while deleting");
        }
    }
}