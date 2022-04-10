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

package com.io7m.zeptoblog.core;

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Convenience functions for constructing errors.
 */

public final class ZErrors
{
  private ZErrors()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Construct an error from the given exception.
   *
   * @param e The exception
   *
   * @return An error
   */

  public static ZError ofException(
    final Exception e)
  {
    return ZError.of(
      e.getMessage(),
      LexicalPosition.of(0, 0, Optional.empty()),
      Optional.of(e));
  }

  /**
   * Construct an error from the given message.
   *
   * @param m The message
   *
   * @return An error
   */

  public static ZError ofMessage(
    final String m)
  {
    return ZError.of(
      m,
      LexicalPosition.of(0, 0, Optional.empty()),
      Optional.empty());
  }

  /**
   * Construct an error from the given message.
   *
   * @param m The message
   * @param p The associated path
   *
   * @return An error
   */

  public static ZError ofMessagePath(
    final String m,
    final Path p)
  {
    return ZError.of(
      m,
      LexicalPosition.of(0, 0, Optional.of(p)),
      Optional.empty());
  }

  /**
   * Construct an error from the given exception.
   *
   * @param e The exception
   * @param p The associated path
   *
   * @return An error
   */

  public static ZError ofExceptionPath(
    final Exception e,
    final Path p)
  {
    return ZError.of(
      e.getMessage(),
      LexicalPosition.of(0, 0, Optional.of(p)),
      Optional.of(e));
  }
}
