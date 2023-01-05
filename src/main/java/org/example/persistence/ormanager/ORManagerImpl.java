package org.example.persistence.ormanager;

import lombok.extern.slf4j.Slf4j;
import org.example.exceptionhandler.ExceptionHandler;
import org.example.persistence.annotations.Entity;
import org.example.persistence.annotations.Id;
import org.example.persistence.utilities.SerializationUtil;

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.example.persistence.sql.SQLDialect.*;
import static org.example.persistence.utilities.AnnotationUtils.declareColumnNamesFromEntityFields;
import static org.example.persistence.utilities.AnnotationUtils.getTableName;


@Slf4j
public class ORManagerImpl implements ORManager {
    private DataSource dataSource;

    public ORManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void register(Class... entityClasses) {
        for (Class<?> cls : entityClasses) {
            List<String> columnNames;
            String tableName = getTableName(cls);
            if (cls.isAnnotationPresent(Entity.class)) {
                columnNames = declareColumnNamesFromEntityFields(cls);
                String sqlCreateTable = String.format("%s %s%n(%n%s%n);", SQL_CREATE_TABLE, tableName,
                        String.join(",\n", columnNames));
                log.atDebug().log("table create statement: \n{}", sqlCreateTable);
                try (PreparedStatement prepStmt = dataSource.getConnection().prepareStatement(sqlCreateTable)) {
                    prepStmt.executeUpdate();
                } catch (SQLException e) {
                    ExceptionHandler.sql(e);
                }
            }
        }
    }

    @Override
    public <T> T save(T o) {
        if (checkIfObjectExists(o)) {
            return o;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlInsertStatement(o.getClass()), Statement.RETURN_GENERATED_KEYS)) {
            replacePlaceholdersInStatement(o, ps);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                setEntityGeneratedId(o, rs);
            }
            rs.close();
        } catch (SQLException e) {
            ExceptionHandler.sql(e);
        }
        return o;
    }

    private <T> boolean checkIfObjectExists(T o) {
        boolean exists = false;
        try {
            Field[] declaredFields = o.getClass().getDeclaredFields();
            if (declaredFields[0].isAnnotationPresent(Id.class)) {
                declaredFields[0].setAccessible(true);
                exists = declaredFields[0].get(o) != null;
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
        return exists;
    }

    /**
     * Replaces all placeholders ("?") in INSERT and UPDATE(except for the last one: WHERE id = ?) sql statements
     * with the names of the columns in the table extracted form the saved/updated object.
     *
     * @param o  The generic entity object which will be saved or updated in the DB.
     * @param ps Prepared statement for Insert and Update.
     * @throws SQLException
     */
    private <T> void replacePlaceholdersInStatement(T o, PreparedStatement ps) throws SQLException {
        try {
            Field[] declaredFields = o.getClass().getDeclaredFields();
            for (int i = 1; i < declaredFields.length; i++) {
                declaredFields[i].setAccessible(true);
                String fieldTypeName = declaredFields[i].getType().getSimpleName();
                switch (fieldTypeName) {
                    case "String" -> ps.setString(i, declaredFields[i].get(o).toString());
                    case "Long", "long" -> ps.setLong(i, (Long) declaredFields[i].get(o));
                    case "Integer", "int" -> ps.setInt(i, (Integer) declaredFields[i].get(o));
                    case "Boolean", "boolean" -> ps.setBoolean(i, (Boolean) declaredFields[i].get(o));
                    case "LocalDate" -> ps.setDate(i, Date.valueOf(declaredFields[i].get(o).toString()));
                    default -> {
                        Object objectField = declaredFields[i].get(o);
                        log.atError().log("Academy field of Student object: {}", objectField);
                        Object objectFiledIdValue = null;
                        if (objectField != null) {
                            Field[] declaredFields1 = declaredFields[i].getType().getDeclaredFields();
                            declaredFields1[0].setAccessible(true);
                            objectFiledIdValue = declaredFields1[0].get(objectField);
                            log.atError().log("id value of Student's Academy object field: {}", objectFiledIdValue);
                        }
                        ps.setObject(i, objectFiledIdValue);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
    }

    /**
     * Sets the ID field of generic entity object to the autogenerated one from the DB side.
     *
     * @param o            The generic entity object for which the ID will be set.
     * @param generatedKey ResultSet from INSERT sql statement with autogenerated keys from DB side
     * @throws SQLException
     */
    private <T> void setEntityGeneratedId(T o, ResultSet generatedKey) throws SQLException {
        Field[] declaredFields = o.getClass().getDeclaredFields();
        try {
            declaredFields[0].setAccessible(true);
            String fieldTypeSimpleName = declaredFields[0].getType().getSimpleName();
            if (fieldTypeSimpleName.equals("Long")) {
                declaredFields[0].set(o, generatedKey.getLong(1));
            } else {
                declaredFields[0].set(o, generatedKey.getInt(1));
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
    }

    @Override
    public <T> Optional<T> findById(Serializable id, Class<T> cls) {
        T entity = null;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlSelectStatement(cls))) {
            if (id.getClass().getSimpleName().equalsIgnoreCase("long")) {
                ps.setLong(1, (Long) id);
            } else {
                ps.setInt(1, (Integer) id);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                entity = extractEntityFromResultSet(rs, cls);
            }
            rs.close();
        } catch (SQLException e) {
            ExceptionHandler.sql(e);
        }
        return entity != null ? Optional.of(entity) : Optional.empty();
    }

    /**
     * @param rs   ResultSet from SELECT sql statement.
     * @param clss Class.
     * @return Generic entity object with field values set from the DB record value.
     * @throws SQLException
     */
    private <T> T extractEntityFromResultSet(ResultSet rs, Class<T> clss) throws SQLException {
        T entityToFind = createNewInstance(clss);
        try {
            Field[] declaredFields = clss.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                declaredFields[i].setAccessible(true);
                String fieldTypeName = declaredFields[i].getType().getSimpleName();
                int columnIndex = i + 1;
                switch (fieldTypeName) {
                    case "String" -> declaredFields[i].set(entityToFind, rs.getString(columnIndex));
                    case "Long", "long" -> declaredFields[i].set(entityToFind, rs.getLong(columnIndex));
                    case "Integer", "int" -> declaredFields[i].set(entityToFind, rs.getInt(columnIndex));
                    case "Boolean", "boolean" -> declaredFields[i].set(entityToFind, rs.getBoolean(columnIndex));
                    case "Double", "double" -> declaredFields[i].set(entityToFind, rs.getDouble(columnIndex));
                    case "LocalDate" -> declaredFields[i].set(entityToFind, rs.getDate(columnIndex).toLocalDate());
                    default -> {
                        try {
                            System.out.println("+++++++++++++++++++++");
                            Object byId = findById(rs.getLong(columnIndex), declaredFields[i].getType()).get();
                            System.out.println("------------------------");
                            declaredFields[i].set(entityToFind, byId);
                        } catch (NoSuchElementException ex) {
                            ExceptionHandler.noSuchElement(ex);
                            declaredFields[i].set(entityToFind, null);
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
        return entityToFind;
    }

    /**
     * Create and initialize a new instance from the no-args constructor of the provided class.
     *
     * @param cls Class.
     * @return New empty generic object of the given class.
     */
    private <T> T createNewInstance(Class<T> cls) {
        T newObject = null;
        try {
            Constructor<T> declaredConstructor = cls.getDeclaredConstructor();
            declaredConstructor.setAccessible(true);
            newObject = declaredConstructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            ExceptionHandler.newInstance(e);
        }
        return newObject;
    }

    @Override
    public <T> T update(T o) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlUpdateStatement(o.getClass()))) {
            replacePlaceholdersInStatement(o, ps);
            Field[] fields = o.getClass().getDeclaredFields();
            int placeholderForId = fields.length;
            fields[0].setAccessible(true);
            ps.setString(placeholderForId, fields[0].get(o).toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ExceptionHandler.sql(ex);
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
        return o;
    }

    @Override
    public <T> T refresh(T o) {
        return null;
    }

    @Override
    public <T> List<T> findAll(Class<T> cls) {
        List<T> records = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement st = connection.prepareStatement(SQL_FIND_ALL + getTableName(cls))) {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                records.add(extractEntityFromResultSet(rs, cls));
            }
            rs.close();
        } catch (SQLException e) {
            ExceptionHandler.sql(e);
        }
        return records;
    }

    @Override
    public void delete(Object... objects) {
        for (Object object : objects) {
            delete(object);
        }
    }

    @Override
    public boolean delete(Object o) {

        Field idField = o.getClass().getDeclaredFields()[0];
        idField.setAccessible(true);
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement ps = connection.prepareStatement(getTableForDelete(o.getClass()));
            ps.setString(1, idField.get(o).toString());
            ps.executeUpdate();
            idField.set(o, null);
            return true;
        } catch (Exception ex) {
            log.error("Exception has occurred: ", ex);
        }
        return false;

    }

    @Override
    public long recordsCount(Class<?> clss) {
        long count = 0;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(SQL_COUNT_ALL + getTableName(clss))) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                count = rs.getLong(1);
            }
        } catch (SQLException e) {
            ExceptionHandler.sql(e);
        }
        return count;
    }
}