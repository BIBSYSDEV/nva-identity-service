package no.unit.identityservice.fsproxy;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

public class FsApiTest {


    @Test
    void shouldReturnFsIdNumberWhenInputIsNin() throws IOException, InterruptedException {
        var expectedFsIdNumber = new FsIdNumber(78);
        var nin = new NationalIdentityNumber("24027336201");
        FsApi fsApi = new FsApi();
        var actualIdNumber = fsApi.getFsId(nin);
        assertThat(actualIdNumber, is(equalTo(expectedFsIdNumber)));
    }

}
