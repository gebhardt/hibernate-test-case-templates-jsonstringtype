package org.hibernate.envers.bugs.usertype;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class MyOwnJsonType<T> implements UserType<T> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final JavaType javaType;

    // Konstruktor für einfache Typen: AddressDto.class
    public MyOwnJsonType(Class<T> clazz) {
        this.javaType = MAPPER.getTypeFactory().constructType(clazz);
    }

    public MyOwnJsonType(TypeReference<T> typeReference) {
        this.javaType = MAPPER.getTypeFactory().constructType(typeReference.getType());
    }

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class<T> returnedClass() {
        return (Class<T>) javaType.getRawClass();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String json = rs.getString(position);
        if (json == null)
            return null;
        try {
            return (T) MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            throw new SQLException("Fehler beim Deserialisieren von JSON: " + json, e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement ps, T value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
            return;
        }
        try {
            ps.setObject(index, MAPPER.writeValueAsString(value), Types.VARCHAR);
        } catch (IOException e) {
            throw new SQLException("Fehler beim Serialisieren von JSON", e);
        }
    }

    @Override
    public boolean equals(T x, T y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(T x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deepCopy(T value) {
        if (value == null)
            return null;
        try {
            return (T) MAPPER.readValue(MAPPER.writeValueAsString(value), javaType);
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Deep-Copy", e);
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(T value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T assemble(Serializable cached, Object owner) {
        try {
            return (T) MAPPER.readValue((String) cached, javaType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}