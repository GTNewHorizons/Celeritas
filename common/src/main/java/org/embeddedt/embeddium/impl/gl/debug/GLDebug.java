/*
 * Copyright LWJGL. All rights reserved. Modified by IMS for use in Iris (net.coderbot.iris.gl).
 * License terms: https://www.lwjgl.org/license
 */

package org.embeddedt.embeddium.impl.gl.debug;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mitchej123.lwjgl.DebugExtension;
import com.mitchej123.lwjgl.GLExtension;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;
import org.lwjgl.opengl.AMDDebugOutput;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.KHRDebug;

import java.io.PrintStream;
import java.util.function.Consumer;

public final class GLDebug {
    static final Logger LOGGER = LogManager.getLogger("Celeritas/GLDebug");

	private static DebugState debugState = new UnsupportedDebugState();

	/**
	 * Sets up debug callbacks
	 *
	 * @return 0 for failure, 1 for success, 2 for restart required.
	 */
	public static int setupDebugMessageCallback() {
		reloadDebugState();
		return setupDebugMessageCallback(LWJGL.getDebugStream());
	}

	private static void trace(Consumer<String> output) {
		/*
		 * We can not just use a fixed stacktrace element offset, because some methods
		 * are intercepted and some are not. So, check the package name.
		 */
		StackTraceElement[] elems = filterStackTrace(new Throwable(), 4).getStackTrace();
		for (StackTraceElement ste : elems) {
			output.accept(ste.toString());
		}
	}

	public static Throwable filterStackTrace(Throwable throwable, int offset) {
		StackTraceElement[] elems = throwable.getStackTrace();
		StackTraceElement[] filtered = new StackTraceElement[elems.length];
		int j = 0;
		for (int i = offset; i < elems.length; i++) {
			filtered[j++] = elems[i];
		}
		StackTraceElement[] newElems = new StackTraceElement[j];
		System.arraycopy(filtered, 0, newElems, 0, j);
		throwable.setStackTrace(newElems);
		return throwable;
	}

	private static void printTrace(PrintStream stream) {
		trace(new Consumer<>() {
			boolean first = true;

			public void accept(String str) {
				if (first) {
					printDetail(stream, "Stacktrace", str);
					first = false;
				} else {
					printDetailLine(stream, "Stacktrace", str);
				}
			}
		});
	}

	public static int setupDebugMessageCallback(PrintStream stream) {
		return LWJGL.setupDebugCallback((source, type, id, severity, message, extension) -> {
			if (extension == DebugExtension.AMD_DEBUG_OUTPUT) {
				stream.println("[LWJGL] AMD_debug_output message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Category", getCategoryAMD(source)); // AMD uses category instead of source
				printDetail(stream, "Severity", getSeverityAMD(severity));
				printDetail(stream, "Message", message);
			} else if (extension == DebugExtension.ARB_DEBUG_OUTPUT) {
				stream.println("[LWJGL] ARB_debug_output message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Source", getSourceARB(source));
				printDetail(stream, "Type", getTypeARB(type));
				printDetail(stream, "Severity", getSeverityARB(severity));
				printDetail(stream, "Message", message);
			} else {
				stream.println("[LWJGL] OpenGL debug message");
				printDetail(stream, "ID", String.format("0x%X", id));
				printDetail(stream, "Source", getDebugSource(source));
				printDetail(stream, "Type", getDebugType(type));
				printDetail(stream, "Severity", getDebugSeverity(severity));
				printDetail(stream, "Message", message);
			}
			printTrace(stream);
		});
	}

	public static int disableDebugMessages() {
		LWJGL.disableDebugCallback();
		return 1;
	}

	private static void printDetail(PrintStream stream, String type, String message) {
		stream.printf("\t%s: %s\n", type, message);
	}

	private static void printDetailLine(PrintStream stream, String type, String message) {
		stream.append("    ");
		for (int i = 0; i < type.length(); i++) {
			stream.append(" ");
		}
		stream.append(message).append("\n");
	}

	private static String unknownToken(int token) {
		return "Unknown (0x" + Integer.toHexString(token).toUpperCase() + ")";
	}

	// ===================== GL43/KHR_debug lookups =====================

	private static String getDebugSource(int source) {
		return switch (source) {
			case GL43C.GL_DEBUG_SOURCE_API -> "API";
			case GL43C.GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "WINDOW SYSTEM";
			case GL43C.GL_DEBUG_SOURCE_SHADER_COMPILER -> "SHADER COMPILER";
			case GL43C.GL_DEBUG_SOURCE_THIRD_PARTY -> "THIRD PARTY";
			case GL43C.GL_DEBUG_SOURCE_APPLICATION -> "APPLICATION";
			case GL43C.GL_DEBUG_SOURCE_OTHER -> "OTHER";
			default -> unknownToken(source);
		};
	}

	private static String getDebugType(int type) {
		return switch (type) {
			case GL43C.GL_DEBUG_TYPE_ERROR -> "ERROR";
			case GL43C.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED BEHAVIOR";
			case GL43C.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED BEHAVIOR";
			case GL43C.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY";
			case GL43C.GL_DEBUG_TYPE_PERFORMANCE -> "PERFORMANCE";
			case GL43C.GL_DEBUG_TYPE_OTHER -> "OTHER";
			case GL43C.GL_DEBUG_TYPE_MARKER -> "MARKER";
			default -> unknownToken(type);
		};
	}

	private static String getDebugSeverity(int severity) {
		return switch (severity) {
			case GL43C.GL_DEBUG_SEVERITY_NOTIFICATION -> "NOTIFICATION";
			case GL43C.GL_DEBUG_SEVERITY_HIGH -> "HIGH";
			case GL43C.GL_DEBUG_SEVERITY_MEDIUM -> "MEDIUM";
			case GL43C.GL_DEBUG_SEVERITY_LOW -> "LOW";
			default -> unknownToken(severity);
		};
	}

	// ===================== ARB_debug_output lookups =====================

	private static String getSourceARB(int source) {
		return switch (source) {
			case ARBDebugOutput.GL_DEBUG_SOURCE_API_ARB -> "API";
			case ARBDebugOutput.GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> "WINDOW SYSTEM";
			case ARBDebugOutput.GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> "SHADER COMPILER";
			case ARBDebugOutput.GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> "THIRD PARTY";
			case ARBDebugOutput.GL_DEBUG_SOURCE_APPLICATION_ARB -> "APPLICATION";
			case ARBDebugOutput.GL_DEBUG_SOURCE_OTHER_ARB -> "OTHER";
			default -> unknownToken(source);
		};
	}

	private static String getTypeARB(int type) {
		return switch (type) {
			case ARBDebugOutput.GL_DEBUG_TYPE_ERROR_ARB -> "ERROR";
			case ARBDebugOutput.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> "DEPRECATED BEHAVIOR";
			case ARBDebugOutput.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> "UNDEFINED BEHAVIOR";
			case ARBDebugOutput.GL_DEBUG_TYPE_PORTABILITY_ARB -> "PORTABILITY";
			case ARBDebugOutput.GL_DEBUG_TYPE_PERFORMANCE_ARB -> "PERFORMANCE";
			case ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB -> "OTHER";
			default -> unknownToken(type);
		};
	}

	private static String getSeverityARB(int severity) {
		return switch (severity) {
			case ARBDebugOutput.GL_DEBUG_SEVERITY_HIGH_ARB -> "HIGH";
			case ARBDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_ARB -> "MEDIUM";
			case ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB -> "LOW";
			default -> unknownToken(severity);
		};
	}

	// ===================== AMD_debug_output lookups =====================

	private static String getCategoryAMD(int category) {
		return switch (category) {
			case AMDDebugOutput.GL_DEBUG_CATEGORY_API_ERROR_AMD -> "API ERROR";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD -> "WINDOW SYSTEM";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_DEPRECATION_AMD -> "DEPRECATION";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD -> "UNDEFINED BEHAVIOR";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_PERFORMANCE_AMD -> "PERFORMANCE";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD -> "SHADER COMPILER";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_APPLICATION_AMD -> "APPLICATION";
			case AMDDebugOutput.GL_DEBUG_CATEGORY_OTHER_AMD -> "OTHER";
			default -> unknownToken(category);
		};
	}

	private static String getSeverityAMD(int severity) {
		return switch (severity) {
			case AMDDebugOutput.GL_DEBUG_SEVERITY_HIGH_AMD -> "HIGH";
			case AMDDebugOutput.GL_DEBUG_SEVERITY_MEDIUM_AMD -> "MEDIUM";
			case AMDDebugOutput.GL_DEBUG_SEVERITY_LOW_AMD -> "LOW";
			default -> unknownToken(severity);
		};
	}

	// ===================== Debug state management =====================

	public static void reloadDebugState() {
		if (Boolean.getBoolean("celeritas.enableGLDebug") &&
				(LWJGL.isExtensionSupported(GLExtension.KHR_debug) || LWJGL.isOpenGLVersionSupported(4, 3))) {
			debugState = new KHRDebugState();
		} else {
			debugState = new UnsupportedDebugState();
		}
	}

	public static void nameObject(int id, int object, String name) {
		debugState.nameObject(id, object, name);
	}

	public static void pushGroup(int id, String name) {
		debugState.pushGroup(id, name);
	}

	public static void popGroup() {
		debugState.popGroup();
	}

	private interface DebugState {
		void nameObject(int id, int object, String name);
		void pushGroup(int id, String name);
		void popGroup();
	}

	private static class KHRDebugState implements DebugState {
		private static final boolean ENABLE_DEBUG_GROUPS = true;
		private int stackSize;

		@Override
		public void nameObject(int id, int object, String name) {
			LWJGL.glObjectLabel(id, object, name);
		}

		@Override
		public void pushGroup(int id, String name) {
			if (!ENABLE_DEBUG_GROUPS) return;
			LWJGL.glPushDebugGroup(KHRDebug.GL_DEBUG_SOURCE_APPLICATION, id, name);
			stackSize += 1;
		}

		@Override
		public void popGroup() {
			if (!ENABLE_DEBUG_GROUPS) return;
			if (stackSize != 0) {
				LWJGL.glPopDebugGroup();
				stackSize -= 1;
			}
		}
	}

	private static class UnsupportedDebugState implements DebugState {
		@Override
		public void nameObject(int id, int object, String name) {}
		@Override
		public void pushGroup(int id, String name) {}
		@Override
		public void popGroup() {}
	}
}
