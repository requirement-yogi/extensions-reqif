import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Tests (...or "enforce") that we only use packages that are explicitly exported by Requirement Yogi.
 *
 * Using JUnit to implement this enforcement is not very canonical, but using the Maven Enforcer Plugin
 * would require to use an external rule such as https://github.com/skuzzle/restrict-imports-enforcer-rule ,
 * and I can't evaluate whether it's a trusted source. It's just as easy to implement it ourselves.
 */
public class TestAPIRules {

    private final static List<String> EXPORTED_PACKAGES_FROM_RY = Lists.newArrayList(
                            "com.playsql.requirementyogi.ao",
                            "com.playsql.requirementyogi.api",
                            "com.playsql.requirementyogi.api.beans",
                            "com.playsql.requirementyogi.api.beans.enums",
                            "com.playsql.requirementyogi.api.beans.interfaces",
                            "com.playsql.requirementyogi.api.dev",
                            "com.playsql.requirementyogi.api.documentimporter",
                            "com.playsql.requirementyogi.api.permissions",
                            "com.playsql.requirementyogi.api.search",
                            "com.playsql.requirementyogi.ao.v33",
                            "com.playsql.requirementyogi.ao.v54",
                            "com.playsql.requirementyogi.ao.v60",
                            "com.playsql.requirementyogi.reporting.api",
                            "com.playsql.requirementyogi.exceptions"
    );

/*-
 * #%L
 * Requirement Yogi - ReqIF Import
 * %%
 * Copyright (C) 2019 - 2022 Requirement Yogi S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

    @Test
    public void testThisPluginOnlyUsesOfficiallyExposedClasses() throws IOException {
        File dir1 = new File("src/main/java/com/requirementyogi/extensions/reqif");
        File dir2 = new File("extensions-reqif/src/main/java/com/requirementyogi/extensions/reqif");
        assertTrue(dir1.isDirectory() || dir2.isDirectory());
        File dir = dir1.isDirectory() ? dir1 : dir2;
        Set<String> files = new HashSet<>();
        process(dir, files);
    }

    private void process(@Nonnull File dir, Set<String> files) throws IOException {
        for (File file : dir.listFiles()) {
            if (!files.add(file.getAbsolutePath())) {
                throw new RuntimeException("File is being processed twice: " + file.getCanonicalPath());
            } else if (file.isDirectory()) {
                process(file, files);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                process(file);
            }
        }
    }

    private void process(File file) throws IOException {
        for (String line : FileUtils.readLines(file, Charsets.UTF_8)) {
            String importedPackage = StringUtils.substringBetween(line, "import ", ";");
            if (importedPackage != null) {
                boolean found = false;
                for (String acceptableImport : EXPORTED_PACKAGES_FROM_RY) {
                    if (importedPackage.contains(acceptableImport)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new RuntimeException("Illegal import: \"" + importedPackage + "\" in file " + file.getName() + ".");
                }
            }
        }
    }
}
