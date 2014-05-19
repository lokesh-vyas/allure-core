package ru.yandex.qatools.allure.utils;

import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.qatools.allure.model.Attachment;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;
import static ru.yandex.qatools.allure.utils.AllureResultsUtils.deleteAttachment;
import static ru.yandex.qatools.allure.utils.AllureResultsUtils.writeAttachment;
import static ru.yandex.qatools.allure.utils.DirectoryMatcher.contains;
import static ru.yandex.qatools.allure.utils.DirectoryMatcher.notContains;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 30.04.14
 */
public class SaveAttachmentTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public File resultsDirectory;

    private static final String ATTACHMENT = "simple attachment context";

    private static final String TITLE = "title";

    @Before
    public void setUp() throws Exception {
        resultsDirectory = folder.newFolder();
        AllureResultsUtils.setResultsDirectory(resultsDirectory);
    }

    @Test
    public void saveAndDeleteTest() throws Exception {
        Attachment first = save(ATTACHMENT);
        ;
        assertNotNull(first);
        String firstSource = first.getSource();
        assertNotNull(firstSource);

        assertThat(resultsDirectory, contains(firstSource));

        Attachment second = save(ATTACHMENT);
        assertNotNull(second);
        String secondSource = second.getSource();
        assertNotNull(secondSource);

        assertEquals(firstSource, secondSource);

        assertThat(resultsDirectory, contains(secondSource));

        deleteAttachment(first);

        assertThat(resultsDirectory, notContains(firstSource));
        assertThat(resultsDirectory, notContains(secondSource));

        deleteAttachment(second);

        assertThat(resultsDirectory, notContains(firstSource));
        assertThat(resultsDirectory, notContains(secondSource));
    }

    public Attachment save(String string) throws IOException {
        String type = "text/plain";
        String source = writeAttachment(string.getBytes(Charsets.UTF_8), type);

        return new Attachment().withSource(source).withType(type).withTitle(TITLE);
    }

}
