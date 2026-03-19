package org.hibernate.envers.bugs.model;

import io.hypersistence.utils.hibernate.type.json.JsonStringType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.bugs.usertype.AddressListMyOwnJsonType;

import java.util.List;

@Audited
@Entity
@Table(name = "person")
@Data
public class Person {
    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @Type(AddressListMyOwnJsonType.class)
    @Column(name = "addresses", columnDefinition = "varchar")
    private List<AddressDto> addressesWithCustomType;

    @Type(JsonStringType.class)
    @Column(name = "addresses2", columnDefinition = "varchar")
    private List<AddressDto> addressesWithJsonStringType;
}
