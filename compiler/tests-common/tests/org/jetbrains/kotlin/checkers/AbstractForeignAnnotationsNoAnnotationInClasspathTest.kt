/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractForeignAnnotationsNoAnnotationInClasspathTest : AbstractForeignAnnotationsTest() {
    // This should be executed after setUp runs, since setUp changes the root folder
    // for temporary files.
    private val compiledJavaPath by lazy {
        KotlinTestUtils.tmpDir("java-compiled-files")
    }

    override fun getExtraClasspath(): List<File> {
        val foreignAnnotations = createJarWithForeignAnnotations()
        val testAnnotations = compileTestAnnotations(foreignAnnotations)

        val additionalClasspath = (foreignAnnotations + testAnnotations).map { it.path }
        CodegenTestUtil.compileJava(
                CodegenTestUtil.findJavaSourcesInDirectory(javaFilesDir),
                additionalClasspath, emptyList(),
                compiledJavaPath
        )

        return listOf(compiledJavaPath) + testAnnotations
    }

    override fun shouldSkipTest(wholeFile: File, files: List<TestFile>): Boolean {
        return files.any { file -> InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SOURCE_RETENTION_ANNOTATIONS") }
    }

    override fun isJavaSourceRootNeeded() = false
    override fun skipDescriptorsValidation() = true
}
