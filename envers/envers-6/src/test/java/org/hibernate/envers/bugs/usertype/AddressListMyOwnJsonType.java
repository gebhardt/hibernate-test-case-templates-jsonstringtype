package org.hibernate.envers.bugs.usertype;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hibernate.envers.bugs.model.AddressDto;

import java.util.List;

public class AddressListMyOwnJsonType extends MyOwnJsonType<List<AddressDto>> {
    public AddressListMyOwnJsonType() {
        super(new TypeReference<List<AddressDto>>() {
        });
    }
}