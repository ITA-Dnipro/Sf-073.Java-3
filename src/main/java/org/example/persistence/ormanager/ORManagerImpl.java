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
        SerializationUtil.serialize(o);
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

    private <T> void replacePlaceholdersInStatement(T o, PreparedStatement ps) throws SQLException {
        try {
            Field[] declaredFields = o.getClass().getDeclaredFields();
            for (int i = 1; i < declaredFields.length; i++) {
                declaredFields[i].setAccessible(true);
                String fieldTypeName = declaredFields[i].getType().getSimpleName();
                switch (fieldTypeName) {
                    case "String" -> ps.setString(i, declaredFields[i].get(o).toString());
                    case "Long" -> ps.setLong(i, (Long) declaredFields[i].get(o));
                    case "Integer" -> ps.setInt(i, (Integer) declaredFields[i].get(o));
                    case "Boolean" -> ps.setBoolean(i, (Boolean) declaredFields[i].get(o));
                    case "LocalDate" -> ps.setDate(i, Date.valueOf(declaredFields[i].get(o).toString()));
                }
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
    }

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
                    case "Long" -> declaredFields[i].set(entityToFind, rs.getLong(columnIndex));
                    case "Integer" -> declaredFields[i].set(entityToFind, rs.getInt(columnIndex));
                    case "Boolean" -> declaredFields[i].set(entityToFind, rs.getBoolean(columnIndex));
                    case "Double" -> declaredFields[i].set(entityToFind, rs.getDouble(columnIndex));
                    case "LocalDate" -> declaredFields[i].set(entityToFind, rs.getDate(columnIndex).toLocalDate());
                }
            }
        } catch (IllegalAccessException e) {
            ExceptionHandler.illegalAccess(e);
        }
        return entityToFind;
    }

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