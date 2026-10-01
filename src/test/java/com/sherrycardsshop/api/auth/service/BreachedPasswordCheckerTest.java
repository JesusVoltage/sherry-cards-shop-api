package com.sherrycardsshop.api.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BreachedPasswordCheckerTest {

    // SHA-1("password") = 5BAA6 1E4C9B93F3F0682250B6CF8331B7EE68FD8
    private static final String RANGE_URL = "https://api.pwnedpasswords.com/range/5BAA6";
    private static final String SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private MockRestServiceServer server;
    private BreachedPasswordChecker checker;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        checker = new BreachedPasswordChecker(builder.build(), true);
    }

    @Test
    void sendsOnlyTheHashPrefixAndDetectsBreachedPasswords() {
        server.expect(requestTo(RANGE_URL)).andExpect(header("Add-Padding", "true"))
                .andRespond(withSuccess("0018A45C4D1DEF81644B54AB7F969B88D65:3\r\n" + SUFFIX + ":9545824", MediaType.TEXT_PLAIN));

        assertThat(checker.isBreached("password")).isTrue();
        server.verify();
    }

    @Test
    void ignoresPaddingEntries() {
        server.expect(requestTo(RANGE_URL)).andRespond(withSuccess(SUFFIX + ":0", MediaType.TEXT_PLAIN));

        assertThat(checker.isBreached("password")).isFalse();
    }

    @Test
    void failsOpenWhenTheServiceIsUnavailable() {
        server.expect(requestTo(RANGE_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(checker.isBreached("password")).isFalse();
    }

    @Test
    void skipsTheRequestWhenDisabled() {
        assertThat(new BreachedPasswordChecker(RestClient.create(), false).isBreached("password")).isFalse();
    }
}
