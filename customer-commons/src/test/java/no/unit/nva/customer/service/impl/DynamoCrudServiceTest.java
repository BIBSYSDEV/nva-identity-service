package no.unit.nva.customer.service.impl;

import no.unit.nva.customer.testing.LocalDynamoCrudTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.unit.nva.customer.Constants.PERSISTED_ENTITY;

class DynamoCrudServiceTest extends LocalDynamoCrudTestDatabase {

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        super.init(PERSISTED_ENTITY);
    }

    @Test
    void shouldPersist() {

    }



    @Test
    void fetch() {
    }
}