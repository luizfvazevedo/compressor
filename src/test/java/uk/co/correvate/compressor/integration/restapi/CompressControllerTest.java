package uk.co.correvate.compressor.integration.restapi;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.co.correvate.compressor.CompressorApplication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CompressControllerTest} contains integration tests for the {@link CompressController} class
 *
 * @author Luiz Azevedo
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = CompressorApplication.class)
public class CompressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void compressEmptyListOfFiles() throws Exception {
        mockMvc.perform(multipart("/compress")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void compressSingleFile() throws Exception {
        final MvcResult result = mockMvc.perform(multipart("/compress")
                        .file(new MockMultipartFile("files", "foo.txt", "text/plain", "foo".getBytes(Charset.defaultCharset())))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertZipFileContent(Map.of("foo.txt", "foo"), result.getResponse().getContentAsByteArray());
    }

    @Test
    void compressMultipleFiles() throws Exception {
        final MvcResult result = mockMvc.perform(multipart("/compress")
                        .file(new MockMultipartFile("files", "foo.txt", "text/plain", "foo".getBytes(Charset.defaultCharset())))
                        .file(new MockMultipartFile("files", "bar.txt", "text/plain", "bar".getBytes(Charset.defaultCharset())))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertZipFileContent(Map.of("foo.txt", "foo", "bar.txt", "bar"), result.getResponse().getContentAsByteArray());
    }


    /**
     * Method that helps to make assertions on the downloaded zip file. It matches the expected files, along with underlying
     * content, with the zip entries read from the given byte array.
     *
     * @param expectedFiles             map with the expected file name and underlying content (as a string)
     * @param zipFileContentAsByteArray zip file byte array
     */
    void assertZipFileContent(final Map<String, String> expectedFiles, final byte[] zipFileContentAsByteArray) throws IOException {
        try (final ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipFileContentAsByteArray))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                final String fileName = zipEntry.getName();

                assertTrue(expectedFiles.containsKey(fileName));

                try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    IOUtils.copy(zipInputStream, outputStream);
                    final String fileContent = outputStream.toString(Charset.defaultCharset());
                    assertEquals(expectedFiles.get(fileName), fileContent);
                }

                zipInputStream.closeEntry();

                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
        }
    }
}
