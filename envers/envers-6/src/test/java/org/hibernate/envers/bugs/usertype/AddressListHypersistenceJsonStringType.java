package org.hibernate.envers.bugs.usertype;

import com.fasterxml.jackson.core.type.TypeReference;
import io.hypersistence.utils.hibernate.type.json.JsonStringType;
import org.hibernate.envers.bugs.model.AddressDto;

import java.util.List;

public class AddressListHypersistenceJsonStringType extends JsonStringType {
    public AddressListHypersistenceJsonStringType() {
        super(new TypeReference<List<AddressDto>>() {
        }.getType());
    }
}