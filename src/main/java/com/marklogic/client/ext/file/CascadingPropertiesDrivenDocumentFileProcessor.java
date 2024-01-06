/*
 * Copyright (c) 2023 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.client.ext.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.Stack;

/**
 * Adds a stack to store Properties objects while traversing a directory tree. Implements {@code FileVisitor} so that
 * it can be informed when {@code DefaultDocumentFileReader} is entering and exiting a directory.
 *
 * To preserve backwards compatibility in subclasses, cascading is disabled by default. This will likely change in 5.0
 * to be enabled by default.
 *
 * @since 4.6.0
 */
abstract class CascadingPropertiesDrivenDocumentFileProcessor extends PropertiesDrivenDocumentFileProcessor implements FileVisitor<Path> {

	final private Stack<Properties> propertiesStack = new Stack<>();
	private boolean cascadingEnabled = false;

	protected CascadingPropertiesDrivenDocumentFileProcessor(String propertiesFilename) {
		super(propertiesFilename);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		// If cascading is disabled, we still use a stack to keep track of whether a directory has properties or not.
		// We just never grab properties from the stack in case a directory doesn't have properties.
		if (logger.isDebugEnabled()) {
			logger.debug(format("Visiting directory: %s", dir.toFile().getAbsolutePath()));
		}
		File propertiesFile = new File(dir.toFile(), this.getPropertiesFilename());
		if (propertiesFile.exists()) {
			if (logger.isDebugEnabled()) {
				logger.debug(format("Loading properties from file: %s", propertiesFile.getAbsolutePath()));
			}
			this.loadProperties(propertiesFile);
		} else {
			if (cascadingEnabled && !propertiesStack.isEmpty()) {
				if (logger.isDebugEnabled()) {
					logger.debug("No properties file, and cascading is enabled, so using properties from top of stack.");
				}
				this.setProperties(propertiesStack.peek());
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("No properties file, or cascading is disabled, so using empty properties.");
				}
				this.setProperties(new Properties());
			}
		}
		propertiesStack.push(this.getProperties());
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
		propertiesStack.pop();
		if (!propertiesStack.isEmpty()) {
			this.setProperties(propertiesStack.peek());
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		return FileVisitResult.CONTINUE;
	}

	public boolean isCascadingEnabled() {
		return cascadingEnabled;
	}

	public void setCascadingEnabled(boolean cascadingEnabled) {
		this.cascadingEnabled = cascadingEnabled;
	}

	/**
	 * Converts a standard POSIX Shell globbing pattern into a regular expression
	 * pattern. The result can be used with the standard {@link java.util.regex} API to
	 * recognize strings which match the glob pattern.
	 * <p/>
	 * See also, the POSIX Shell language:
	 * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
	 *
	 * @param pattern A glob pattern.
	 * @return A regex pattern to recognize the given glob pattern.
	 */
	public String convertGlobToRegex(String pattern) {
		StringBuilder sb = new StringBuilder(pattern.length());
		int inGroup = 0;
		int inClass = 0;
		int firstIndexInClass = -1;
		char[] arr = pattern.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char ch = arr[i];
			switch (ch) {
				case '\\':
					if (++i >= arr.length) {
						sb.append('\\');
					} else {
						char next = arr[i];
						switch (next) {
							case ',':
								// escape not needed
								break;
							case 'Q':
							case 'E':
								// extra escape needed
								sb.append('\\');
							default:
								sb.append('\\');
						}
						sb.append(next);
					}
					break;
				case '*':
					if (inClass == 0)
						sb.append(".*");
					else
						sb.append('*');
					break;
				case '?':
					if (inClass == 0)
						sb.append('.');
					else
						sb.append('?');
					break;
				case '[':
					inClass++;
					firstIndexInClass = i+1;
					sb.append('[');
					break;
				case ']':
					inClass--;
					sb.append(']');
					break;
				case '.':
				case '(':
				case ')':
				case '+':
				case '|':
				case '^':
				case '$':
				case '@':
				case '%':
					if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
						sb.append('\\');
					sb.append(ch);
					break;
				case '!':
					if (firstIndexInClass == i)
						sb.append('^');
					else
						sb.append('!');
					break;
				case '{':
					inGroup++;
					sb.append('(');
					break;
				case '}':
					inGroup--;
					sb.append(')');
					break;
				case ',':
					if (inGroup > 0)
						sb.append('|');
					else
						sb.append(',');
					break;
				default:
					sb.append(ch);
			}
		}
		return sb.toString();
	}
}
