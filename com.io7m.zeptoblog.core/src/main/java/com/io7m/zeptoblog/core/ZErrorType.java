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
import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The type of errors.
 */

@ZImmutableStyleType
@Value.Immutable
@VavrEncodingEnabled
public interface ZErrorType
{
  /**
   * @return The error message
   */

  @Value.Parameter
  String message();

  /**
   * @return The error position
   */

  @Value.Parameter
  LexicalPosition<Path> position();

  /**
   * @return The exception raised, if any
   */

  @Value.Parameter
  Optional<Exception> error();

  /**
   * @return A formatted error message
   */

  default String show()
  {
    final Optional<Exception> error_opt = this.error();
    final Optional<Path> file_opt = this.position().file();
    if (file_opt.isPresent()) {
      if (error_opt.isPresent()) {
        final Exception ex = error_opt.get();
        return String.format(
          "%s:%d:%d: %s (%s: %s)",
          file_opt.get(),
          Integer.valueOf(this.position().line()),
          Integer.valueOf(this.position().column()),
          this.message(),
          ex.getClass().getCanonicalName(),
          ex.getMessage());
      }
      return String.format(
        "%s:%d:%d: %s",
        file_opt.get(),
        Integer.valueOf(this.position().line()),
        Integer.valueOf(this.position().column()),
        this.message());
    }

    if (error_opt.isPresent()) {
      final Exception ex = error_opt.get();
      return String.format(
        "%d:%d: %s (%s: %s)",
        Integer.valueOf(this.position().line()),
        Integer.valueOf(this.position().column()),
        this.message(),
        ex.getClass().getCanonicalName(),
        ex.getMessage());
    }

    return String.format(
      "%d:%d: %s",
      Integer.valueOf(this.position().line()),
      Integer.valueOf(this.position().column()),
      this.message());
  }
}
