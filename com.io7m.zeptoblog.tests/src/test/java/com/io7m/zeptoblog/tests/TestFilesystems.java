/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.zeptoblog.tests;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;
import com.github.marschall.memoryfilesystem.StringTransformers;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;

public final class TestFilesystems
{
  private TestFilesystems()
  {
    throw new UnreachableCodeException();
  }

  static FileSystem makeEmptyUnixFilesystem()
  {
    try {
      final String user = "root";
      final MemoryFileSystemBuilder base = MemoryFileSystemBuilder.newEmpty();
      base.addRoot("/");
      base.setCurrentWorkingDirectory("/");
      base.setSeparator("/");
      base.addUser(user);
      base.addGroup(user);
      base.addFileAttributeView(PosixFileAttributeView.class);
      base.setStoreTransformer(StringTransformers.IDENTIY);
      base.setCaseSensitive(true);
      base.addForbiddenCharacter((char) 0);
      base.addFileAttributeView(PosixFileAttributeView.class);
      return base.build("data");
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  static FileSystem makeEmptyDOSFilesystem()
  {
    try {
      final String user = "Administrator";
      final MemoryFileSystemBuilder base = MemoryFileSystemBuilder.newEmpty();
      base.addRoot("C:\\");
      base.setSeparator("\\");
      base.addUser(user);
      base.addGroup(user);
      base.addFileAttributeView(DosFileAttributeView.class);
      base.setStoreTransformer(StringTransformers.IDENTIY);
      base.setCaseSensitive(false);
      base.addForbiddenCharacter('\\');
      base.addForbiddenCharacter('/');
      base.addForbiddenCharacter(':');
      base.addForbiddenCharacter('*');
      base.addForbiddenCharacter('?');
      base.addForbiddenCharacter('"');
      base.addForbiddenCharacter('<');
      base.addForbiddenCharacter('>');
      base.addForbiddenCharacter('|');
      return base.build("data");
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }
}
