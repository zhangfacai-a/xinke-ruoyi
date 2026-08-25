package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.xinke.common.exception.ServiceException;

class AudienceRankUploadAuthenticatorTest
{
    @Test
    void rejectsEveryRequestWhenServerKeyIsMissing()
    {
        AudienceRankUploadAuthenticator authenticator = new AudienceRankUploadAuthenticator("  ");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> authenticator.authenticate("any-client-key"));

        assertEquals(503, exception.getCode());
    }

    @Test
    void acceptsOnlyTheExactConfiguredKey()
    {
        AudienceRankUploadAuthenticator authenticator = new AudienceRankUploadAuthenticator("configured-secret");

        assertDoesNotThrow(() -> authenticator.authenticate("configured-secret"));
        ServiceException wrong = assertThrows(ServiceException.class,
                () -> authenticator.authenticate("configured-secret "));
        ServiceException missing = assertThrows(ServiceException.class,
                () -> authenticator.authenticate(null));
        assertEquals(401, wrong.getCode());
        assertEquals(401, missing.getCode());
    }
}
