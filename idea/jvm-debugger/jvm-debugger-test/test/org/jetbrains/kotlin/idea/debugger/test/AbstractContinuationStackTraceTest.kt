/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.extensions.Extensions
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.ui.tree.nodes.XValuePresentationUtil
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.format
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.idea.debugger.test.util.XDebuggerTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.getSafe
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import javax.swing.Icon

abstract class AbstractContinuationStackTraceTest : KotlinDescriptorTestCaseWithStepping() {
    private companion object {
        const val MARGIN = "    "
        val ASYNC_STACKTRACE_EP_NAME = AsyncStackTraceProvider.EP.name
        val INDENT_FRAME = 1
        val INDENT_VARIABLES = 2
    }

    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        val asyncStackTraceProvider = getAsyncStackTraceProvider()

        doWhenXSessionPausedThenResume {
            printContext(debugProcess.debuggerContext)
            val suspendContext = debuggerSession.xDebugSession?.getSuspendContext()
            var executionStack = suspendContext?.getActiveExecutionStack()
            if (executionStack != null) {
                try {
                    out("Thread stack trace:")
                    val stackFrames: List<XStackFrame> = XDebuggerTestUtil.collectFrames(executionStack)
                    for (frame in stackFrames) {
                        if (frame is JavaStackFrame) {
                            out(XDebuggerTestUtil.getFramePresentation(frame))
                            val asyncstackFrames = asyncStackTraceProvider?.getAsyncStackTrace(frame, suspendContext as SuspendContextImpl)
                            if (asyncstackFrames != null) {
                                for (frame in asyncstackFrames)
                                    out(frame)
                                break
                            }
                        }
                    }
                } catch (e: Throwable) {
                    val stackTrace = e.stackTraceAsString()
                    System.err.println("Exception occurred on calculating async stack traces: $stackTrace")
                    throw e
                }
            } else {
                println("FrameProxy is 'null', can't calculate async stack trace", ProcessOutputTypes.SYSTEM)
            }
        }
    }

    private fun out(stackFrame: StackFrameItem) {
        out(INDENT_FRAME, stackFrame.format())
        outVariables(debugProcess.invokeInManagerThread { stackFrame.createFrame(debugProcess) } ?: return)
    }

    private fun out(stackFrame: JavaStackFrame) {
        out(INDENT_FRAME, stackFrame.format())
        outVariables(stackFrame)
    }

    private fun outVariables(stackFrame: XStackFrame) {
        val variables = XDebuggerTestUtil.collectChildrenWithError(stackFrame)
        val varString = variables.first.joinToString()
        out(INDENT_VARIABLES, "($varString)")
    }

    private fun out(text: String) {
        println(text, ProcessOutputTypes.SYSTEM)
    }

    private fun out(indent: Int, text: String) {
        println("\t".repeat(indent) + text, ProcessOutputTypes.SYSTEM)
        println(text)
    }

    private fun Throwable.stackTraceAsString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun getAsyncStackTraceProvider(): CoroutineAsyncStackTraceProvider? {
        val area = Extensions.getArea(null)
        if (!area.hasExtensionPoint(ASYNC_STACKTRACE_EP_NAME)) {
            System.err.println("${ASYNC_STACKTRACE_EP_NAME} extension point is not found (probably old IDE version)")
            return null
        }

        val extensionPoint = area.getExtensionPoint<Any>(ASYNC_STACKTRACE_EP_NAME)
        val provider = extensionPoint.extensions.firstIsInstanceOrNull<CoroutineAsyncStackTraceProvider>()

        if (provider == null) {
            System.err.println("Kotlin coroutine async stack trace provider is not found")
        }
        return provider
    }
}
