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

import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.osgi.annotation.versioning.ProviderType;
import org.w3c.dom.Element;

import java.nio.file.Path;

/**
 * A post body format.
 */

@ProviderType
public interface ZBlogPostFormatType extends ZServiceType
{
  /**
   * Produce XHTML for the given body text.
   *
   * @param path The path of the original file, for error reporting
   * @param text The input body text
   *
   * @return XHTML, or a list of reasons why XHTML could not be produced
   */

  Validation<Seq<ZError>, Element> produceXHTML(
    Path path,
    String text);

  /**
   * Produce plain text for the given body text.
   *
   * @param path The path of the original file, for error reporting
   * @param text The input body text
   *
   * @return Plain text, or a list of reasons why plain text could not be
   * produced
   */

  Validation<Seq<ZError>, String> producePlain(
    Path path,
    String text);
}
