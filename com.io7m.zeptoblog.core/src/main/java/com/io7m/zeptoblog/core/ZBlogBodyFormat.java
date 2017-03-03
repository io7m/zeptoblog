/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
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

import com.io7m.jnull.NullCheck;

/**
 * The format of a piece of text.
 */

public enum ZBlogBodyFormat
{
  /**
   * The text is in CommonMark format.
   */

  FORMAT_COMMONMARK("commonmark"),

  /**
   * The text is in XHTML format.
   */

  FORMAT_XHTML("xhtml");

  private final String format;

  ZBlogBodyFormat(
    final String in_format)
  {
    this.format = NullCheck.notNull(in_format, "format");
  }

  /**
   * Parse a format name.
   *
   * @param name The format name
   *
   * @return A parsed format
   *
   * @throws IllegalArgumentException If the format name is unknown
   */

  public static ZBlogBodyFormat of(final String name)
    throws IllegalArgumentException
  {
    final ZBlogBodyFormat[] formats = ZBlogBodyFormat.values();
    for (final ZBlogBodyFormat v : formats) {
      if (name.equals(v.format)) {
        return v;
      }
    }

    final StringBuilder sb = new StringBuilder(128);
    sb.append("Unrecognized format.");
    sb.append("  Expected one of: ");
    for (final ZBlogBodyFormat f : formats) {
      sb.append(f.format);
      sb.append(" ");
    }
    sb.append(System.lineSeparator());
    sb.append("  Received: ");
    sb.append(name);
    sb.append(System.lineSeparator());
    throw new IllegalArgumentException(sb.toString());
  }
}
