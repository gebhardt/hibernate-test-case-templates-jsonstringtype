/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.bugs;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.bugs.model.AddressDto;
import org.hibernate.envers.bugs.model.Person;
import org.junit.Test;

import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This template demonstrates how to develop a test case for Hibernate Envers, using its built-in unit test framework.
 */
public class EnversUnitTestCase extends AbstractEnversTestCase {

    // Add your entities here.
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] { Person.class };
    }

    // If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
    @Override
    protected String[] getMappings() {
        return new String[] {
                //				"Foo.hbm.xml",
                //				"Bar.hbm.xml"
        };
    }

    // If those mappings reside somewhere other than resources/org/hibernate/test, change this.
    @Override
    protected String getBaseForMappings() {
        return "org/hibernate/test/";
    }

    // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);

        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
    }

    // Add your tests, using standard JUnit.
    @Test
    public void auditHistoryCanReadAddressesWithCustomType() {

        Long personId = createPersonWithTwoRevisions();

        // --- Verify Audit-History --

        AuditReader reader = getAuditReader();

        // load all revisions
        List<Number> revisions = reader.getRevisions(Person.class, personId);
        assertEquals(2, revisions.size());

        // verify addressesWithJsonStringType in Revision 1
        Person rev1 = reader.find(Person.class, personId, revisions.get(0));
        assertNotNull(rev1.getAddressesWithCustomType());
        assertEquals("Stuttgart", rev1.getAddressesWithCustomType().get(0).getCity());
        assertEquals("Musterstr. 1", rev1.getAddressesWithCustomType().get(0).getStreet());
        assertEquals("70173", rev1.getAddressesWithCustomType().get(0).getZip());

        // verify Revision 2
        Person rev2 = reader.find(Person.class, personId, revisions.get(1));
        assertNotNull(rev2.getAddressesWithCustomType());
        assertEquals("Berlin", rev2.getAddressesWithCustomType().get(0).getCity());
        assertEquals("Neue Str. 5", rev2.getAddressesWithCustomType().get(0).getStreet());
        assertEquals("10115", rev2.getAddressesWithCustomType().get(0).getZip());
    }

    @Test
    public void auditHistoryFailsReadingAddressesWithJsonStringType() {

        Long personId = createPersonWithTwoRevisions();

        AuditReader reader = getAuditReader();
        List<Number> revisions = reader.getRevisions(Person.class, personId);
        assertEquals(2, revisions.size());

        // verify Revision 1 – JsonStringType deserializes to LinkedHashMap instead of AddressDto
        Person rev1 = reader.find(Person.class, personId, revisions.get(0));
        assertNotNull(rev1.getAddressesWithJsonStringType());
        assertThrows(ClassCastException.class, () -> rev1.getAddressesWithJsonStringType().get(0)
                        .getCity(), // 🐛 LinkedHashMap cannot be cast to AddressDto
                "Expected ClassCastException because JsonStringType deserializes to LinkedHashMap");

        // verify Revision 2
        Person rev2 = reader.find(Person.class, personId, revisions.get(1));
        assertNotNull(rev2.getAddressesWithJsonStringType());
        assertThrows(ClassCastException.class, () -> rev2.getAddressesWithJsonStringType().get(0)
                        .getCity(), // 🐛 LinkedHashMap cannot be cast to AddressDto
                "Expected ClassCastException because JsonStringType deserializes to LinkedHashMap");
    }

    @Test
    public void directLoadCanReadBothAddressVariants() {

        Long personId = createPersonWithTwoRevisions();

        doInJPA(this::sessionFactory, entityManager -> {
            Person person = entityManager.find(Person.class, personId);

            // addressesWithCustomType – works correctly ✅
            assertNotNull(person.getAddressesWithCustomType());
            assertEquals("Berlin", person.getAddressesWithCustomType().get(0).getCity());
            assertEquals("Neue Str. 5", person.getAddressesWithCustomType().get(0).getStreet());
            assertEquals("10115", person.getAddressesWithCustomType().get(0).getZip());

            // addressesWithJsonStringType – also works correctly on direct load ✅
            // (Bug only manifests when loading via Envers revision)
            assertNotNull(person.getAddressesWithJsonStringType());
            assertEquals("Berlin", person.getAddressesWithJsonStringType().get(0).getCity());
            assertEquals("Neue Str. 5", person.getAddressesWithJsonStringType().get(0).getStreet());
            assertEquals("10115", person.getAddressesWithJsonStringType().get(0).getZip());
        });
    }

    private Long createPersonWithTwoRevisions() {
        final Long[] personId = new Long[1];

        // --- Save (Revision 1) ---
        doInJPA(this::sessionFactory, entityManager -> {
            AddressDto address = new AddressDto("Musterstr. 1", "Stuttgart", "70173");
            Person person = new Person();
            person.setName("Max Mustermann");
            person.setAddressesWithCustomType(List.of(address));
            person.setAddressesWithJsonStringType(List.of(address));
            entityManager.persist(person);
            personId[0] = person.getId();
        });

        // --- Modify addresses (Revision 2) ---
        doInJPA(this::sessionFactory, entityManager -> {
            Person person = entityManager.find(Person.class, personId[0]);
            AddressDto address = new AddressDto("Neue Str. 5", "Berlin", "10115");
            person.setAddressesWithCustomType(List.of(address));
            person.setAddressesWithJsonStringType(List.of(address));
        });
        return personId[0];
    }
}
