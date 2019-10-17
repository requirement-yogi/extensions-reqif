import com.atlassian.confluence.content.render.xhtml.RenderedContentCleaner;
import com.google.common.collect.Lists;
import com.playsql.extensions.reqif.managers.ReqifDocumentManager;
import com.playsql.extensions.reqif.managers.ReqifDocumentManager.ParseException;
import com.playsql.extensions.reqif.ui.UIReqifDocument;
import com.playsql.extensions.reqif.ui.UIRequirement;
import com.playsql.extensions.reqif.ui.UIRequirementValue;
import com.playsql.requirementyogi.ao.Ref;
import com.playsql.requirementyogi.ao.Requirement;
import com.playsql.requirementyogi.ao.RequirementRef;
import com.playsql.utils.BaseUtils.Clock;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class TestReqifOverview {
    private final static String REQIF_FILE_LOCATION = "test.reqif";
    private ReqifDocumentManager reqifDocumentManager;

    @Mock RenderedContentCleaner antisamy = Mockito.mock(RenderedContentCleaner.class);

    @Before
    public void setup() {
        reqifDocumentManager = new ReqifDocumentManager(null, antisamy, null, null, null, null);
    }

    public Requirement createRequirement(String spaceKey, String key, Ref origin, List<RequirementRef> dependencies) {
        Requirement requirement = new Requirement();
        requirement.setSpaceKey(spaceKey);
        requirement.setKey(key);
        requirement.setOrigin(origin);
        requirement.setDependencies(dependencies);
        return requirement;
    }

    @Test
    public void testParseDefaultFile() throws IOException, URISyntaxException, ParseException {
        InputStream inputStream = new ByteArrayInputStream(getFileContent(REQIF_FILE_LOCATION).getBytes());

        UIReqifDocument reqifDocument = reqifDocumentManager.parse(inputStream);
        List<UIRequirement> requirements = reqifDocument.getRequirements();
        assertThat(requirements, hasSize(13));
        UIRequirement uiRequirement = requirements.get(0);
        UIRequirementValue xhtmlAttribute = uiRequirement.get("10000004");
        assertThat(xhtmlAttribute.isXhtml(), is(true));
        assertThat(xhtmlAttribute.getValue(), is("<div><p style=\"text-align: left\">hello</p><p>world</p></div>"));
    }

    @Test
    public void testParseAllFiles() throws IOException, URISyntaxException, ParseException {
        File testResourcesFolder = searchForRoot();
        List<File> files = Lists.newArrayList(testResourcesFolder.listFiles((dir, name) -> name.endsWith(".reqif")));
        File samplesFolder = new File(testResourcesFolder, "samples");
        if (samplesFolder.isDirectory()) {
            files.addAll(Lists.newArrayList(samplesFolder.listFiles((dir, name) -> name.endsWith(".reqif"))));
        }

        boolean hasFile = false;
        for (File file : files) {

            InputStream inputStream = new FileInputStream(file);

            try {
                long start = System.nanoTime();
                UIReqifDocument reqifDocument = reqifDocumentManager.parse(inputStream);
                List<UIRequirement> requirements = reqifDocument.getRequirements();

                System.out.print(requirements.size() + " requirements \tin " +
                    reqifDocument.getSpecObjectTypes().size() + " type(s) \tin " +
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + "ms");

                assertThat("File " + file.getName(), requirements, is(not(empty())));
                hasFile = true;
            } finally {
                System.out.println("\t\tfor file " + file.getName());
            }
        }
        assertTrue("Files were parsed", hasFile);
    }

    protected static File searchForRoot() {
        for (String path : new String[] { ".", "extensions-reqif/src/test/resources", "src/test/resources" }) {
            File file = new File(path);
            if (file.isDirectory() && StringUtils.endsWith(file.getAbsolutePath(), "src/test/resources")) {
                return file;
            }
        }
        throw new RuntimeException("We're not executing in a directory that we expect: " + new File(".").getAbsolutePath());
    }

    private String getFileContent(String path) throws URISyntaxException, IOException {
        URI xmlFileUri = this.getClass().getClassLoader().getResource(path).toURI();
        return new String(Files.readAllBytes(Paths.get(xmlFileUri)));
    }
}
